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

	/**
	 * Asks the provider how a PIN prompt ended, for when its callback never arrives.
	 * @throws PaymentGatewayException if the provider can't be reached
	 */
	StkStatus queryStatus(String checkoutRequestId);

	record StkPushRequest(PhoneNumber phone, int amountKes, String accountReference, String description) {
	}

	record StkStatus(State state, String description) {

		public enum State {

			/** The customer paid. */
			PAID,
			/** Definitely not paid: cancelled, wrong PIN, no balance, no answer... */
			FAILED,
			/** The provider is still working on it: ask again later. */
			STILL_PROCESSING

		}

	}

	class PaymentGatewayException extends RuntimeException {

		public PaymentGatewayException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
