package ke.wateratm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ke.wateratm.application.InMemoryAdapters;
import ke.wateratm.application.port.out.PartnerShopRepositoryPort;
import ke.wateratm.domain.model.GeoPoint;
import ke.wateratm.domain.model.PartnerShop;
import ke.wateratm.domain.model.PhoneNumber;

/**
 * The whole journey over HTTP with a real Postgres: order → PIN prompt → M-Pesa callback → paid.
 * Only M-Pesa itself is faked.
 */
@Import({ TestcontainersConfiguration.class, OrderAndPaymentFlowTest.FakeMpesa.class })
@SpringBootTest(properties = "wateratm.mpesa.callback-secret=s3cret")
@AutoConfigureMockMvc
class OrderAndPaymentFlowTest {

	@TestConfiguration
	static class FakeMpesa {

		@Bean
		@Primary
		InMemoryAdapters.Gateway fakeGateway() {
			return new InMemoryAdapters.Gateway();
		}

	}

	@Autowired
	MockMvc mvc;

	@Autowired
	InMemoryAdapters.Gateway mpesa;

	@Autowired
	PartnerShopRepositoryPort shops;

	@Test
	void orderPayAndGetDeliveryCode() throws Exception {
		GeoPoint kilimani = new GeoPoint(-1.2921, 36.7836);
		shops.save(PartnerShop.onboard("Kilimani Water", PhoneNumber.of("0722111333"), kilimani, 45));

		String created = mvc
			.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
					{"phone":"0712 999 888","jerrycans":2,"lat":-1.2921,"lng":36.7836}"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
			.andExpect(jsonPath("$.totalKes").value(2 * 45 + 20))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String orderId = JsonPath.read(created, "$.id");

		mvc.perform(post("/api/orders/{id}/payment", orderId)).andExpect(status().isAccepted());
		assertThat(mpesa.requests).last().satisfies(r -> assertThat(r.phone().value()).isEqualTo("254712999888"));
		String checkoutId = "ws_CO_" + mpesa.requests.size();

		mvc.perform(post("/api/mpesa/callback/s3cret").contentType(MediaType.APPLICATION_JSON).content("""
				{"Body":{"stkCallback":{"MerchantRequestID":"1","CheckoutRequestID":"%s","ResultCode":0,
				  "ResultDesc":"ok","CallbackMetadata":{"Item":[{"Name":"Amount","Value":110},
				  {"Name":"MpesaReceiptNumber","Value":"TEST%s"}]}}}}""".formatted(checkoutId, mpesa.requests.size())))
			.andExpect(status().isOk());

		mvc.perform(get("/api/orders/{id}", orderId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PAID"))
			.andExpect(jsonPath("$.deliveryCode").isString());
	}

	@Test
	void noShopNearby() throws Exception {
		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"phone":"0712999888","jerrycans":1,"lat":3.1190,"lng":35.5973}"""))
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	void invalidRequest() throws Exception {
		mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"phone":"0712999888","jerrycans":0,"lat":-1.29,"lng":36.78}"""))
			.andExpect(status().isBadRequest());
	}

}
