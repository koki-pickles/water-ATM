package ke.wateratm.adapter.out.mpesa;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import ke.wateratm.application.port.out.PaymentGatewayPort;

/**
 * M-Pesa STK push through Safaricom's Daraja API.
 * Docs: https://developer.safaricom.co.ke (APIs → M-Pesa Express).
 */
@Component
class DarajaPaymentGateway implements PaymentGatewayPort {

	private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
		.withZone(ZoneId.of("Africa/Nairobi"));

	private static final String STILL_PROCESSING_ERROR = "500.001.1001";

	private final RestClient http;

	private final DarajaProperties props;

	private final Clock clock;

	private String accessToken;

	private Instant accessTokenExpiresAt = Instant.EPOCH;

	@Autowired
	DarajaPaymentGateway(DarajaProperties props, Clock clock) {
		this(RestClient.builder(), props, clock);
	}

	DarajaPaymentGateway(RestClient.Builder builder, DarajaProperties props, Clock clock) {
		this.http = builder.baseUrl(props.baseUrl()).build();
		this.props = props;
		this.clock = clock;
	}

	@Override
	public String requestStkPush(StkPushRequest request) {
		String timestamp = TIMESTAMP.format(clock.instant());
		String phone = request.phone().value();

		StkPushBody body = new StkPushBody(props.shortcode(), password(timestamp), timestamp, props.transactionType(),
				request.amountKes(), phone, props.shortcode(), phone, props.callbackUrl(), request.accountReference(),
				request.description());
		try {
			StkPushResponse response = http.post()
				.uri("/mpesa/stkpush/v1/processrequest")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(StkPushResponse.class);
			if (response == null || !"0".equals(response.responseCode())) {
				throw new PaymentGatewayException("M-Pesa refused the request: "
						+ (response == null ? "empty response" : response.responseDescription()), null);
			}
			return response.checkoutRequestId();
		}
		catch (RestClientException ex) {
			throw new PaymentGatewayException("Could not reach M-Pesa: " + ex.getMessage(), ex);
		}
	}

	@Override
	public StkStatus queryStatus(String checkoutRequestId) {
		String timestamp = TIMESTAMP.format(clock.instant());
		StkQueryBody body = new StkQueryBody(props.shortcode(), password(timestamp), timestamp, checkoutRequestId);
		try {
			StkQueryResponse response = http.post()
				.uri("/mpesa/stkpushquery/v1/query")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(StkQueryResponse.class);
			if (response == null || response.resultCode() == null) {
				return new StkStatus(StkStatus.State.STILL_PROCESSING, "No result yet");
			}
			// ResultCode 0 = paid. Anything else is final: 1032 cancelled, 1037 no answer, 1 no balance, 2001 wrong PIN...
			return "0".equals(response.resultCode().trim())
					? new StkStatus(StkStatus.State.PAID, response.resultDesc())
					: new StkStatus(StkStatus.State.FAILED, response.resultDesc());
		}
		catch (HttpStatusCodeException ex) {
			// Daraja answers HTTP 500 + errorCode 500.001.1001 while the customer is still on the PIN prompt.
			if (ex.getResponseBodyAsString().contains(STILL_PROCESSING_ERROR)) {
				return new StkStatus(StkStatus.State.STILL_PROCESSING, "The transaction is being processed");
			}
			throw new PaymentGatewayException("M-Pesa status query failed: " + ex.getMessage(), ex);
		}
		catch (RestClientException ex) {
			throw new PaymentGatewayException("Could not reach M-Pesa: " + ex.getMessage(), ex);
		}
	}

	private String password(String timestamp) {
		return Base64.getEncoder()
			.encodeToString((props.shortcode() + props.passkey() + timestamp).getBytes(StandardCharsets.UTF_8));
	}

	/** Daraja tokens last an hour; we reuse one until a minute before it expires. */
	private synchronized String accessToken() {
		if (clock.instant().isBefore(accessTokenExpiresAt)) {
			return accessToken;
		}
		TokenResponse token = http.get()
			.uri("/oauth/v1/generate?grant_type=client_credentials")
			.headers(h -> h.setBasicAuth(props.consumerKey(), props.consumerSecret()))
			.retrieve()
			.body(TokenResponse.class);
		if (token == null || token.accessToken() == null) {
			throw new PaymentGatewayException("M-Pesa gave no access token (check consumer key/secret)", null);
		}
		accessToken = token.accessToken();
		accessTokenExpiresAt = clock.instant().plusSeconds(Long.parseLong(token.expiresIn()) - 60);
		return accessToken;
	}

	record TokenResponse(@JsonProperty("access_token") String accessToken,
			@JsonProperty("expires_in") String expiresIn) {
	}

	record StkPushBody(@JsonProperty("BusinessShortCode") String businessShortCode,
			@JsonProperty("Password") String password, @JsonProperty("Timestamp") String timestamp,
			@JsonProperty("TransactionType") String transactionType, @JsonProperty("Amount") int amount,
			@JsonProperty("PartyA") String partyA, @JsonProperty("PartyB") String partyB,
			@JsonProperty("PhoneNumber") String phoneNumber, @JsonProperty("CallBackURL") String callBackUrl,
			@JsonProperty("AccountReference") String accountReference,
			@JsonProperty("TransactionDesc") String transactionDesc) {
	}

	record StkPushResponse(@JsonProperty("CheckoutRequestID") String checkoutRequestId,
			@JsonProperty("ResponseCode") String responseCode,
			@JsonProperty("ResponseDescription") String responseDescription) {
	}

	record StkQueryBody(@JsonProperty("BusinessShortCode") String businessShortCode,
			@JsonProperty("Password") String password, @JsonProperty("Timestamp") String timestamp,
			@JsonProperty("CheckoutRequestID") String checkoutRequestId) {
	}

	record StkQueryResponse(@JsonProperty("ResultCode") String resultCode,
			@JsonProperty("ResultDesc") String resultDesc) {
	}

}
