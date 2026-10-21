package ke.wateratm.domain.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

	public OrderNotFoundException(UUID orderId) {
		super("No order " + orderId);
	}

}
