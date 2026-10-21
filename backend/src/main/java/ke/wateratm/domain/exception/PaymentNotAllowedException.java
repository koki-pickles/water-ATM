package ke.wateratm.domain.exception;

import java.util.UUID;

import ke.wateratm.domain.model.OrderStatus;

public class PaymentNotAllowedException extends RuntimeException {

	public PaymentNotAllowedException(UUID orderId, OrderStatus status) {
		super("Order " + orderId + " is " + status + " and can't be paid");
	}

}
