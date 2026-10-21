package ke.wateratm.application.port.in;

/** M-Pesa tells us how the PIN prompt ended. Safe to call twice with the same result. */
public interface HandlePaymentResultUseCase {

	void handle(PaymentResult result);

	/**
	 * @param receipt M-Pesa receipt number, only when successful
	 * @param amountKes amount actually paid, only when successful
	 */
	record PaymentResult(String checkoutRequestId, boolean successful, String receipt, Integer amountKes,
			String description) {
	}

}
