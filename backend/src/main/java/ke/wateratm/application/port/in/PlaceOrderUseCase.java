package ke.wateratm.application.port.in;

import ke.wateratm.domain.model.Order;
import ke.wateratm.domain.model.OrderChannel;

/** Driving port: what the WhatsApp bot and the app call to create an order. */
public interface PlaceOrderUseCase {

	Order placeOrder(PlaceOrderCommand command);

	record PlaceOrderCommand(String customerPhone, int jerrycans, double lat, double lng, OrderChannel channel) {
	}

}
