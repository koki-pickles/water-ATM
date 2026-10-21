package ke.wateratm.application.service;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import ke.wateratm.application.port.in.ExpireStalePaymentsUseCase;
import ke.wateratm.application.port.in.HandlePaymentResultUseCase;
import ke.wateratm.application.port.in.RequestPaymentUseCase;
import ke.wateratm.application.port.out.CustomerRepositoryPort;
import ke.wateratm.application.port.out.OrderRepositoryPort;
import ke.wateratm.application.port.out.PaymentGatewayPort;
import ke.wateratm.application.port.out.PaymentGatewayPort.PaymentGatewayException;
import ke.wateratm.application.port.out.PaymentGatewayPort.StkPushRequest;
import ke.wateratm.application.port.out.PaymentGatewayPort.StkStatus;
import ke.wateratm.application.port.out.PaymentRepositoryPort;
import ke.wateratm.domain.exception.OrderNotFoundException;
import ke.wateratm.domain.exception.PaymentNotAllowedException;
import ke.wateratm.domain.model.Customer;
import ke.wateratm.domain.model.Order;
import ke.wateratm.domain.model.Payment;
import ke.wateratm.domain.model.PaymentStatus;

public class PaymentService implements RequestPaymentUseCase, HandlePaymentResultUseCase, ExpireStalePaymentsUseCase {

	/** The PIN prompt on the phone disappears after about a minute; after this we ask M-Pesa what happened. */
	static final Duration PAYMENT_TIMEOUT = Duration.ofMinutes(5);

	/** If M-Pesa still can't tell us after this long, we stop asking. */
	static final Duration PAYMENT_GIVE_UP = Duration.ofMinutes(60);

	/** Customers can retry a failed payment; after this the unpaid order is cancelled. */
	static final Duration UNPAID_ORDER_TIMEOUT = Duration.ofMinutes(30);

	private static final Logger LOG = System.getLogger(PaymentService.class.getName());

	private final OrderRepositoryPort orders;

	private final CustomerRepositoryPort customers;

	private final PaymentRepositoryPort payments;

	private final PaymentGatewayPort gateway;

	private final Clock clock;

	public PaymentService(OrderRepositoryPort orders, CustomerRepositoryPort customers, PaymentRepositoryPort payments,
			PaymentGatewayPort gateway, Clock clock) {
		this.orders = orders;
		this.customers = customers;
		this.payments = payments;
		this.gateway = gateway;
		this.clock = clock;
	}

	@Override
	public Payment requestPayment(UUID orderId) {
		// Locking the order means a double tap on "Pay" can't send two PIN prompts.
		Order order = orders.findByIdForUpdate(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
		if (!order.isAwaitingPayment()) {
			throw new PaymentNotAllowedException(orderId, order.status());
		}
		Optional<Payment> inProgress = payments.findPendingByOrderId(orderId);
		if (inProgress.isPresent()) {
			return inProgress.get();
		}
		Customer customer = customers.findById(order.customerId())
			.orElseThrow(() -> new IllegalStateException("Order " + orderId + " has no customer"));

		String checkoutRequestId = gateway
			.requestStkPush(new StkPushRequest(customer.phone(), order.totalKes(), "WaterATM", "Water order"));
		return payments.save(Payment.started(order, checkoutRequestId));
	}

	@Override
	public void handle(PaymentResult result) {
		Optional<Payment> found = payments.findByCheckoutRequestIdForUpdate(result.checkoutRequestId());
		if (found.isEmpty()) {
			LOG.log(Level.WARNING, "M-Pesa result for unknown request {0}", result.checkoutRequestId());
			return;
		}
		Payment payment = found.get();
		if (!payment.isAwaitingCallback()) {
			// M-Pesa sometimes sends the same result twice, or a status query got there first.
			if (result.successful()) {
				payment.attachLateReceipt(result.receipt());
				payments.save(payment);
			}
			return;
		}
		if (result.successful()) {
			// No amount in a "success" is suspicious: -1 makes it an AMOUNT_MISMATCH for a human to check.
			recordSuccess(payment, result.receipt(), result.amountKes() == null ? -1 : result.amountKes());
		}
		else {
			payment.recordFailure(result.description());
		}
		payments.save(payment);
	}

	private void recordSuccess(Payment payment, String receipt, int amountKes) {
		Order order = orders.findByIdForUpdate(payment.orderId())
			.orElseThrow(() -> new OrderNotFoundException(payment.orderId()));
		payment.recordSuccess(receipt, amountKes, order.isAwaitingPayment());
		if (payment.status() == PaymentStatus.SUCCEEDED) {
			order.markPaid();
			orders.save(order);
		}
		else {
			LOG.log(Level.ERROR, "Payment {0} needs a human: {1}", payment.id(), payment.resultDescription());
		}
	}

	@Override
	public int expireStale() {
		Instant now = clock.instant();
		int closed = 0;

		for (Payment stale : payments.findPendingCreatedBefore(now.minus(PAYMENT_TIMEOUT))) {
			// Re-read with a lock: a callback may have arrived since the list was loaded.
			Payment payment = payments.findByCheckoutRequestIdForUpdate(stale.checkoutRequestId()).orElseThrow();
			if (payment.isPending() && settleWithoutCallback(payment, now)) {
				payments.save(payment);
				closed++;
			}
		}

		for (Order stale : orders.findAwaitingPaymentCreatedBefore(now.minus(UNPAID_ORDER_TIMEOUT))) {
			Order order = orders.findByIdForUpdate(stale.id()).orElseThrow();
			if (order.isAwaitingPayment() && payments.findPendingByOrderId(order.id()).isEmpty()) {
				order.cancelUnpaid();
				orders.save(order);
				closed++;
			}
		}
		return closed;
	}

	/**
	 * The callback never came. Ask M-Pesa before giving up, so a lost "paid" message can
	 * never cancel an order the customer really paid for.
	 * @return true if the payment is now settled
	 */
	private boolean settleWithoutCallback(Payment payment, Instant now) {
		StkStatus status;
		try {
			status = gateway.queryStatus(payment.checkoutRequestId());
		}
		catch (PaymentGatewayException ex) {
			status = new StkStatus(StkStatus.State.STILL_PROCESSING, "M-Pesa unreachable: " + ex.getMessage());
		}
		switch (status.state()) {
			case PAID -> recordSuccess(payment, null, payment.amountKes());
			case FAILED -> payment.recordFailure(status.description());
			case STILL_PROCESSING -> {
				if (payment.createdAt().isAfter(now.minus(PAYMENT_GIVE_UP))) {
					return false; // ask again on the next run
				}
				LOG.log(Level.ERROR, "Payment {0} still unknown after {1}, giving up: {2}", payment.id(),
						PAYMENT_GIVE_UP, status.description());
				payment.expire();
			}
		}
		return true;
	}

}
