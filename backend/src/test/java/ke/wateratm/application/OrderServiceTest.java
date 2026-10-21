package ke.wateratm.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ke.wateratm.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import ke.wateratm.application.service.OrderService;
import ke.wateratm.domain.exception.NoShopNearbyException;
import ke.wateratm.domain.exception.OrderNotFoundException;
import ke.wateratm.domain.model.GeoPoint;
import ke.wateratm.domain.model.Order;
import ke.wateratm.domain.model.OrderChannel;
import ke.wateratm.domain.model.OrderStatus;
import ke.wateratm.domain.model.PartnerShop;
import ke.wateratm.domain.model.PhoneNumber;

/**
 * Tests the use cases with in-memory fakes plugged into the ports: no database, no Spring.
 * This is the payoff of hexagonal architecture.
 */
class OrderServiceTest {

	static final double LAT = -1.1850;

	static final double LNG = 36.8960;

	final InMemoryAdapters.Orders orders = new InMemoryAdapters.Orders();

	final InMemoryAdapters.Customers customers = new InMemoryAdapters.Customers();

	final InMemoryAdapters.Shops shops = new InMemoryAdapters.Shops();

	final OrderService service = new OrderService(orders, customers, shops, 20);

	PartnerShop shop;

	@BeforeEach
	void setUp() {
		shop = shops.save(PartnerShop.onboard("Maji Safi", PhoneNumber.of("0722000111"), new GeoPoint(LAT, LNG), 50));
	}

	PlaceOrderCommand command(String phone, OrderChannel channel) {
		return new PlaceOrderCommand(phone, 2, LAT, LNG, channel);
	}

	@Test
	void placesAndStoresOrder() {
		Order order = service.placeOrder(command("0712345678", OrderChannel.WHATSAPP));

		assertThat(orders.byId).containsKey(order.id());
		assertThat(order.shopId()).isEqualTo(shop.id());
		assertThat(order.totalKes()).isEqualTo(120);
	}

	@Test
	void sameCustomerFromBothDoors() {
		service.placeOrder(command("0712345678", OrderChannel.WHATSAPP));
		service.placeOrder(command("+254 712 345 678", OrderChannel.APP));

		assertThat(customers.byPhone).hasSize(1);
	}

	@Test
	void remembersCustomerLocation() {
		service.placeOrder(command("0712345678", OrderChannel.APP));

		assertThat(customers.byPhone.get(PhoneNumber.of("0712345678")).lastLocation()).isEqualTo(new GeoPoint(LAT, LNG));
	}

	@Test
	void noShopNearbyCreatesNothing() {
		assertThatThrownBy(() -> service.placeOrder(new PlaceOrderCommand("0712345678", 1, -4.04, 39.67, OrderChannel.APP)))
			.isInstanceOf(NoShopNearbyException.class);
		assertThat(customers.byPhone).isEmpty();
		assertThat(orders.byId).isEmpty();
	}

	@Test
	void statusChangesAreSaved() {
		UUID id = service.placeOrder(command("0712345678", OrderChannel.APP)).id();

		service.markPaid(id);
		service.acceptByShop(id, shop.id());
		service.markOutForDelivery(id);
		String code = orders.byId.get(id).deliveryCode();

		assertThat(service.confirmDelivery(id, code)).isTrue();
		assertThat(orders.byId.get(id).status()).isEqualTo(OrderStatus.DELIVERED);
	}

	@Test
	void unknownOrder() {
		assertThatThrownBy(() -> service.markPaid(UUID.randomUUID())).isInstanceOf(OrderNotFoundException.class);
	}

}
