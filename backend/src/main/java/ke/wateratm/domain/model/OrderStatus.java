package ke.wateratm.domain.model;

import java.util.Set;

/**
 * Life of an order (ROADMAP.md §5). Each status lists the only statuses it may move to,
 * so the code can never e.g. deliver an order that was never paid.
 */
public enum OrderStatus {

	PENDING_PAYMENT, PAID, ACCEPTED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED, REFUND_NEEDED, REFUNDED;

	public Set<OrderStatus> allowedNext() {
		return switch (this) {
			case PENDING_PAYMENT -> Set.of(PAID, CANCELLED);
			case PAID -> Set.of(ACCEPTED, REFUND_NEEDED);
			case ACCEPTED -> Set.of(OUT_FOR_DELIVERY, REFUND_NEEDED);
			case OUT_FOR_DELIVERY -> Set.of(DELIVERED, REFUND_NEEDED);
			case REFUND_NEEDED -> Set.of(REFUNDED);
			case DELIVERED, CANCELLED, REFUNDED -> Set.of();
		};
	}

	public boolean canMoveTo(OrderStatus next) {
		return allowedNext().contains(next);
	}

}
