package ke.wateratm.adapter.in.web;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ke.wateratm.application.port.in.HandlePaymentResultUseCase;
import ke.wateratm.application.port.in.HandlePaymentResultUseCase.PaymentResult;

/**
 * Safaricom calls this when the customer finishes (or abandons) the PIN prompt.
 * Daraja doesn't sign its callbacks, so the URL contains a secret only we and Safaricom know.
 */
@RestController
class MpesaCallbackController {

	private static final Logger LOG = LoggerFactory.getLogger(MpesaCallbackController.class);

	private static final Map<String, Object> ACCEPTED = Map.of("ResultCode", 0, "ResultDesc", "Accepted");

	private final HandlePaymentResultUseCase handlePaymentResult;

	private final byte[] callbackSecret;

	MpesaCallbackController(HandlePaymentResultUseCase handlePaymentResult,
			@Value("${wateratm.mpesa.callback-secret}") String callbackSecret) {
		this.handlePaymentResult = handlePaymentResult;
		this.callbackSecret = callbackSecret.getBytes(StandardCharsets.UTF_8);
	}

	@PostMapping("/api/mpesa/callback/{secret}")
	ResponseEntity<Map<String, Object>> callback(@PathVariable String secret, @RequestBody CallbackBody body) {
		if (!MessageDigest.isEqual(callbackSecret, secret.getBytes(StandardCharsets.UTF_8))) {
			LOG.warn("M-Pesa callback with wrong secret ignored");
			return ResponseEntity.notFound().build();
		}
		StkCallback cb = body.body().stkCallback();
		try {
			handlePaymentResult.handle(toResult(cb));
		}
		catch (RuntimeException ex) {
			// Safaricom won't resend, so keep everything needed to fix this by hand.
			LOG.error("Failed to process M-Pesa result {}", cb, ex);
		}
		return ResponseEntity.ok(ACCEPTED);
	}

	private static PaymentResult toResult(StkCallback cb) {
		boolean successful = cb.resultCode() == 0;
		String receipt = null;
		Integer amount = null;
		if (successful && cb.metadata() != null) {
			for (Item item : cb.metadata().items()) {
				if ("MpesaReceiptNumber".equals(item.name())) {
					receipt = String.valueOf(item.value());
				}
				else if ("Amount".equals(item.name())) {
					amount = new BigDecimal(String.valueOf(item.value())).intValueExact();
				}
			}
		}
		return new PaymentResult(cb.checkoutRequestId(), successful, receipt, amount, cb.resultDesc());
	}

	record CallbackBody(@JsonProperty("Body") Body body) {
	}

	record Body(@JsonProperty("stkCallback") StkCallback stkCallback) {
	}

	record StkCallback(@JsonProperty("MerchantRequestID") String merchantRequestId,
			@JsonProperty("CheckoutRequestID") String checkoutRequestId, @JsonProperty("ResultCode") int resultCode,
			@JsonProperty("ResultDesc") String resultDesc, @JsonProperty("CallbackMetadata") Metadata metadata) {
	}

	record Metadata(@JsonProperty("Item") List<Item> items) {
	}

	record Item(@JsonProperty("Name") String name, @JsonProperty("Value") Object value) {
	}

}
