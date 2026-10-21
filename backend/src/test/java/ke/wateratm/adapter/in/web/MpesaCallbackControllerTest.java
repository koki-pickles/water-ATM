package ke.wateratm.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ke.wateratm.application.port.in.FindOrderUseCase;
import ke.wateratm.application.port.in.HandlePaymentResultUseCase;
import ke.wateratm.application.port.in.HandlePaymentResultUseCase.PaymentResult;
import ke.wateratm.application.port.in.PlaceOrderUseCase;
import ke.wateratm.application.port.in.RequestPaymentUseCase;

@WebMvcTest(properties = "wateratm.mpesa.callback-secret=s3cret")
class MpesaCallbackControllerTest {

	// Examples taken from the shape of Safaricom's documented callbacks.
	static final String SUCCESS = """
			{"Body":{"stkCallback":{"MerchantRequestID":"29115-34620561-1",
			  "CheckoutRequestID":"ws_CO_191220191020363925","ResultCode":0,
			  "ResultDesc":"The service request is processed successfully.",
			  "CallbackMetadata":{"Item":[{"Name":"Amount","Value":120.00},
			    {"Name":"MpesaReceiptNumber","Value":"NLJ7RT61SV"},{"Name":"Balance"},
			    {"Name":"TransactionDate","Value":20191219102115},{"Name":"PhoneNumber","Value":254712345678}]}}}}""";

	static final String CANCELLED = """
			{"Body":{"stkCallback":{"MerchantRequestID":"29115-34620561-1",
			  "CheckoutRequestID":"ws_CO_191220191020363925","ResultCode":1032,
			  "ResultDesc":"Request cancelled by user"}}}""";

	@Autowired
	MockMvc mvc;

	@MockitoBean
	HandlePaymentResultUseCase handlePaymentResult;

	@MockitoBean
	PlaceOrderUseCase placeOrder;

	@MockitoBean
	FindOrderUseCase findOrder;

	@MockitoBean
	RequestPaymentUseCase requestPayment;

	@Test
	void successIsPassedOn() throws Exception {
		mvc.perform(post("/api/mpesa/callback/s3cret").contentType(MediaType.APPLICATION_JSON).content(SUCCESS))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ResultCode").value(0));

		verify(handlePaymentResult).handle(new PaymentResult("ws_CO_191220191020363925", true, "NLJ7RT61SV", 120,
				"The service request is processed successfully."));
	}

	@Test
	void cancellationIsPassedOn() throws Exception {
		mvc.perform(post("/api/mpesa/callback/s3cret").contentType(MediaType.APPLICATION_JSON).content(CANCELLED))
			.andExpect(status().isOk());

		verify(handlePaymentResult)
			.handle(new PaymentResult("ws_CO_191220191020363925", false, null, null, "Request cancelled by user"));
	}

	@Test
	void wrongSecretIsRejected() throws Exception {
		mvc.perform(post("/api/mpesa/callback/guess").contentType(MediaType.APPLICATION_JSON).content(SUCCESS))
			.andExpect(status().isNotFound());

		verify(handlePaymentResult, never()).handle(any());
	}

}
