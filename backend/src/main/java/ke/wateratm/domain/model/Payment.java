package ke.wateratm.domain.model;

import java.time.Instant;
import java.util.UUID;

/** One M-Pesa STK push attempt for an order. An order can have several if the customer retries. */
public class Payment {

	private final UUID id;

	private final UUID orderId;

	private final String checkoutRequestId;

	private final int amountKes;

	private PaymentStatus status;

	private String mpesaReceipt;

	private String resultDescription;

	private final Instant createdAt;

	private Instant updatedAt;

	private Payment(UUID id, UUID orderId, String checkoutRequestId, int amountKes, PaymentStatus status,
			String mpesaReceipt, String resultDescription, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.orderId = orderId;
		this.checkoutRequestId = checkoutRequestId;
		this.amountKes = amountKes;
		this.status = status;
		this.mpesaReceipt = mpesaReceipt;
		this.resultDescription = resultDescription;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	/** The STK push was accepted by M-Pesa; now we wait for the callback. */
	public static Payment started(Order order, String checkoutRequestId) {
		Instant now = Instant.now();
		return new Payment(UUID.randomUUID(), order.id(), checkoutRequestId, order.totalKes(), PaymentStatus.PENDING,
				null, null, now, now);
	}

	public static Payment restore(UUID id, UUID orderId, String checkoutRequestId, int amountKes, PaymentStatus status,
			String mpesaReceipt, String resultDescription, Instant createdAt, Instant updatedAt) {
		return new Payment(id, orderId, checkoutRequestId, amountKes, status, mpesaReceipt, resultDescription,
				createdAt, updatedAt);
	}

	public boolean isPending() {
		return status == PaymentStatus.PENDING;
	}

	/**
	 * M-Pesa can answer after we've given up (EXPIRED). We still record that answer:
	 * if the money is real, it must not be lost.
	 */
	public boolean isAwaitingCallback() {
		return status == PaymentStatus.PENDING || status == PaymentStatus.EXPIRED;
	}

	/**
	 * M-Pesa says the money arrived.
	 * @param orderStillAwaitingPayment false if the order was cancelled meanwhile
	 */
	public void recordSuccess(String receipt, int paidKes, boolean orderStillAwaitingPayment) {
		requireAwaitingCallback();
		mpesaReceipt = receipt;
		if (paidKes != amountKes) {
			finish(PaymentStatus.AMOUNT_MISMATCH, "Paid " + paidKes + " KES, expected " + amountKes);
		}
		else if (!orderStillAwaitingPayment) {
			finish(PaymentStatus.LATE_NEEDS_REFUND, "Paid after order was closed");
		}
		else {
			finish(PaymentStatus.SUCCEEDED, "Paid");
		}
	}

	public void recordFailure(String reason) {
		requireAwaitingCallback();
		finish(PaymentStatus.FAILED, reason);
	}

	/**
	 * A payment confirmed by a status query has no receipt number (the query doesn't return one).
	 * If M-Pesa's callback turns up later, keep its receipt for the records.
	 */
	public void attachLateReceipt(String receipt) {
		if (status == PaymentStatus.SUCCEEDED && mpesaReceipt == null && receipt != null) {
			mpesaReceipt = receipt;
			updatedAt = Instant.now();
		}
	}

	public void expire() {
		requirePending();
		finish(PaymentStatus.EXPIRED, "No answer from M-Pesa in time");
	}

	private void requirePending() {
		if (status != PaymentStatus.PENDING) {
			throw new IllegalStateException("Payment " + id + " is already " + status);
		}
	}

	private void requireAwaitingCallback() {
		if (!isAwaitingCallback()) {
			throw new IllegalStateException("Payment " + id + " is already " + status);
		}
	}

	private void finish(PaymentStatus next, String description) {
		status = next;
		resultDescription = description;
		updatedAt = Instant.now();
	}

	public UUID id() {
		return id;
	}

	public UUID orderId() {
		return orderId;
	}

	public String checkoutRequestId() {
		return checkoutRequestId;
	}

	public int amountKes() {
		return amountKes;
	}

	public PaymentStatus status() {
		return status;
	}

	public String mpesaReceipt() {
		return mpesaReceipt;
	}

	public String resultDescription() {
		return resultDescription;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

}
