package ke.wateratm.domain.model;

public enum PaymentStatus {

	/** STK push sent, waiting for the customer to enter their PIN. */
	PENDING,
	/** M-Pesa confirmed the money arrived. */
	SUCCEEDED,
	/** Customer cancelled, wrong PIN, not enough balance... They can try again. */
	FAILED,
	/** No answer from M-Pesa in time. */
	EXPIRED,
	/** Money arrived after we gave up on it (order already cancelled): must be refunded by hand. */
	LATE_NEEDS_REFUND,
	/** Money arrived but not the amount we asked for: needs a human to look. */
	AMOUNT_MISMATCH

}
