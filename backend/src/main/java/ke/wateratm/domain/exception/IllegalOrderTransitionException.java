package ke.wateratm.domain.exception;

import java.util.UUID;

import ke.wateratm.domain.model.OrderStatus;

public class IllegalOrderTransitionException extends RuntimeException {

	public IllegalOrderTransitionException(UUID orderId, OrderStatus from, OrderStatus to) {
		super("Order " + orderId + " cannot go from " + from + " to " + to);
	}

}
