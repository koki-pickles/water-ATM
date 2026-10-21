package ke.wateratm.adapter.out.mpesa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import ke.wateratm.application.port.out.PaymentGatewayPort.PaymentGatewayException;
import ke.wateratm.application.port.out.PaymentGatewayPort.StkPushRequest;
import ke.wateratm.application.port.out.PaymentGatewayPort.StkStatus;
import ke.wateratm.domain.model.PhoneNumber;

/** Checks we speak Daraja's language correctly, against a fake Safaricom server. */
class DarajaPaymentGatewayTest {

	static final String BASE = "https://sandbox.safaricom.co.ke";

	// 2026-10-21 09:30:00 UTC = 12:30:00 in Nairobi (UTC+3)
	static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T09:30:00Z"), ZoneOffset.UTC);

	static final DarajaProperties PROPS = new DarajaProperties(BASE, "key", "secret", "174379", "passkey",
			"CustomerPayBillOnline", "https://example.ngrok.app", "s3cret");

	MockRestServiceServer safaricom;

	DarajaPaymentGateway gateway;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		safaricom = MockRestServiceServer.bindTo(builder).build();
		gateway = new DarajaPaymentGateway(builder, PROPS, CLOCK);
	}

	void expectToken() {
		safaricom.expect(ExpectedCount.once(), requestTo(BASE + "/oauth/v1/generate?grant_type=client_credentials"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Basic " + base64("key:secret")))
			.andRespond(withSuccess("{\"access_token\":\"tok123\",\"expires_in\":\"3599\"}", MediaType.APPLICATION_JSON));
	}

	StkPushRequest request() {
		return new StkPushRequest(PhoneNumber.of("0712345678"), 120, "WaterATM", "Water order");
	}

	@Test
	void sendsCorrectStkPush() {
		expectToken();
		safaricom.expect(requestTo(BASE + "/mpesa/stkpush/v1/processrequest"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header("Authorization", "Bearer tok123"))
			.andExpect(jsonPath("$.BusinessShortCode").value("174379"))
			.andExpect(jsonPath("$.Timestamp").value("20261021123000"))
			.andExpect(jsonPath("$.Password").value(base64("174379passkey20261021123000")))
			.andExpect(jsonPath("$.TransactionType").value("CustomerPayBillOnline"))
			.andExpect(jsonPath("$.Amount").value(120))
			.andExpect(jsonPath("$.PartyA").value("254712345678"))
			.andExpect(jsonPath("$.PhoneNumber").value("254712345678"))
			.andExpect(jsonPath("$.CallBackURL").value("https://example.ngrok.app/api/mpesa/callback/s3cret"))
			.andRespond(withSuccess("""
					{"MerchantRequestID":"29115-34620561-1","CheckoutRequestID":"ws_CO_191220191020363925",
					 "ResponseCode":"0","ResponseDescription":"Success. Request accepted for processing",
					 "CustomerMessage":"Success. Request accepted for processing"}""", MediaType.APPLICATION_JSON));

		assertThat(gateway.requestStkPush(request())).isEqualTo("ws_CO_191220191020363925");
		safaricom.verify();
	}

	@Test
	void reusesTokenForSecondPayment() {
		expectToken();
		String ok = "{\"CheckoutRequestID\":\"ws_CO_1\",\"ResponseCode\":\"0\"}";
		safaricom.expect(ExpectedCount.twice(), requestTo(BASE + "/mpesa/stkpush/v1/processrequest"))
			.andRespond(withSuccess(ok, MediaType.APPLICATION_JSON));

		gateway.requestStkPush(request());
		gateway.requestStkPush(request());
		safaricom.verify();
	}

	@Test
	void refusalBecomesGatewayException() {
		expectToken();
		safaricom.expect(requestTo(BASE + "/mpesa/stkpush/v1/processrequest"))
			.andRespond(withSuccess("{\"ResponseCode\":\"1\",\"ResponseDescription\":\"Invalid Amount\"}",
					MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> gateway.requestStkPush(request())).isInstanceOf(PaymentGatewayException.class)
			.hasMessageContaining("Invalid Amount");
	}

	@Test
	void serverErrorBecomesGatewayException() {
		expectToken();
		safaricom.expect(requestTo(BASE + "/mpesa/stkpush/v1/processrequest")).andRespond(withServerError());

		assertThatThrownBy(() -> gateway.requestStkPush(request())).isInstanceOf(PaymentGatewayException.class);
	}

	void expectQueryAnswer(org.springframework.test.web.client.ResponseCreator answer) {
		expectToken();
		safaricom.expect(requestTo(BASE + "/mpesa/stkpushquery/v1/query"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(jsonPath("$.BusinessShortCode").value("174379"))
			.andExpect(jsonPath("$.Password").value(base64("174379passkey20261021123000")))
			.andExpect(jsonPath("$.CheckoutRequestID").value("ws_CO_1"))
			.andRespond(answer);
	}

	@Test
	void queryPaid() {
		expectQueryAnswer(withSuccess("""
				{"ResponseCode":"0","CheckoutRequestID":"ws_CO_1","ResultCode":"0",
				 "ResultDesc":"The service request is processed successfully."}""", MediaType.APPLICATION_JSON));

		assertThat(gateway.queryStatus("ws_CO_1").state()).isEqualTo(StkStatus.State.PAID);
	}

	@Test
	void queryNoAnswerFromCustomer() {
		// Exactly what the real sandbox answered on 21 Oct 2026
		expectQueryAnswer(withSuccess("""
				{"ResponseCode": "0","ResponseDescription":"The service request has been accepted successfully",
				 "CheckoutRequestID":"ws_CO_1","ResultCode": "1037","ResultDesc":"No response from user."}""",
				MediaType.APPLICATION_JSON));

		StkStatus status = gateway.queryStatus("ws_CO_1");
		assertThat(status.state()).isEqualTo(StkStatus.State.FAILED);
		assertThat(status.description()).isEqualTo("No response from user.");
	}

	@Test
	void queryStillProcessing() {
		expectQueryAnswer(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body("""
				{"requestId":"1","errorCode":"500.001.1001","errorMessage":"The transaction is being processed"}"""));

		assertThat(gateway.queryStatus("ws_CO_1").state()).isEqualTo(StkStatus.State.STILL_PROCESSING);
	}

	@Test
	void queryOtherErrorIsGatewayException() {
		expectQueryAnswer(withServerError());

		assertThatThrownBy(() -> gateway.queryStatus("ws_CO_1")).isInstanceOf(PaymentGatewayException.class);
	}

	static String base64(String s) {
		return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
	}

}
