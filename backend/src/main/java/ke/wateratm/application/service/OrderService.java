package ke.wateratm.application.service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import ke.wateratm.application.port.in.FindOrderUseCase;
import ke.wateratm.application.port.in.PlaceOrderUseCase;
import ke.wateratm.application.port.in.UpdateOrderStatusUseCase;
import ke.wateratm.application.port.out.CustomerRepositoryPort;
import ke.wateratm.application.port.out.OrderRepositoryPort;
import ke.wateratm.application.port.out.PartnerShopRepositoryPort;
import ke.wateratm.domain.exception.NoShopNearbyException;
import ke.wateratm.domain.exception.OrderNotFoundException;
import ke.wateratm.domain.model.Customer;
import ke.wateratm.domain.model.GeoPoint;
import ke.wateratm.domain.model.Order;
import ke.wateratm.domain.model.PartnerShop;
import ke.wateratm.domain.model.PhoneNumber;
import ke.wateratm.domain.service.NearestShopPolicy;

/**
 * Runs the order use cases. Both the WhatsApp bot and the app come in through these ports,
 * so the rules live here once ("one brain, two doors").
 *
 * Plain Java on purpose: no Spring, no database code. Transactions are added around it in
 * {@code config.UseCaseConfig}.
 */
public class OrderService implements PlaceOrderUseCase, UpdateOrderStatusUseCase, FindOrderUseCase {

	private final OrderRepositoryPort orders;

	private final CustomerRepositoryPort customers;

	private final PartnerShopRepositoryPort shops;

	private final int deliveryFeeKes;

	public OrderService(OrderRepositoryPort orders, CustomerRepositoryPort customers, PartnerShopRepositoryPort shops,
			int deliveryFeeKes) {
		this.orders = orders;
		this.customers = customers;
		this.shops = shops;
		this.deliveryFeeKes = deliveryFeeKes;
	}

	@Override
	public Order placeOrder(PlaceOrderCommand command) {
		PhoneNumber phone = PhoneNumber.of(command.customerPhone());
		GeoPoint location = new GeoPoint(command.lat(), command.lng());

		PartnerShop shop = NearestShopPolicy.choose(shops.findActive(), location)
			.orElseThrow(NoShopNearbyException::new);

		Customer customer = customers.findByPhone(phone).orElseGet(() -> Customer.register(phone));
		customer.moveTo(location);
		customers.save(customer);

		Order order = Order.place(customer, shop, command.jerrycans(), location, command.channel(), deliveryFeeKes);
		return orders.save(order);
	}

	@Override
	public Optional<Order> findOrder(UUID orderId) {
		return orders.findById(orderId);
	}

	@Override
	public Order markPaid(UUID orderId) {
		return update(orderId, Order::markPaid);
	}

	@Override
	public Order markPaymentFailed(UUID orderId) {
		return update(orderId, Order::cancelUnpaid);
	}

	@Override
	public Order acceptByShop(UUID orderId, UUID shopId) {
		return update(orderId, order -> order.acceptBy(shopId));
	}

	@Override
	public Order markOutForDelivery(UUID orderId) {
		return update(orderId, Order::sendOutForDelivery);
	}

	@Override
	public boolean confirmDelivery(UUID orderId, String code) {
		Order order = orders.findByIdForUpdate(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
		boolean delivered = order.confirmDelivery(code);
		if (delivered) {
			orders.save(order);
		}
		return delivered;
	}

	@Override
	public Order flagForRefund(UUID orderId) {
		return update(orderId, Order::flagForRefund);
	}

	private Order update(UUID orderId, Consumer<Order> change) {
		Order order = orders.findByIdForUpdate(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
		change.accept(order);
		return orders.save(order);
	}

}
