package ke.wateratm.application.port.out;

import ke.wateratm.domain.model.PhoneNumber;

/** Driven port: the mobile money provider (M-Pesa Daraja today). */
public interface PaymentGatewayPort {

	/**
	 * Pops up the PIN prompt on the customer's phone.
	 * @return the provider's id for this request; the result callback will carry the same id
	 * @throws PaymentGatewayException if the provider refused or can't be reached
	 */
	String requestStkPush(StkPushRequest request);

	record StkPushRequest(PhoneNumber phone, int amountKes, String accountReference, String description) {
	}

	class PaymentGatewayException extends RuntimeException {

		public PaymentGatewayException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
