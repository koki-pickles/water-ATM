package ke.wateratm.application.port.in;

import java.util.UUID;

import ke.wateratm.domain.model.Order;

/** Driving port: moving an order along (payment callbacks, shop, rider, admin). */
public interface UpdateOrderStatusUseCase {

	Order markPaid(UUID orderId);

	Order markPaymentFailed(UUID orderId);

	Order acceptByShop(UUID orderId, UUID shopId);

	Order markOutForDelivery(UUID orderId);

	/** @return true if the code was right and the order is now delivered */
	boolean confirmDelivery(UUID orderId, String code);

	Order flagForRefund(UUID orderId);

}
