package ke.wateratm.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import ke.wateratm.domain.exception.IllegalOrderTransitionException;

class OrderTest {

	static final GeoPoint KAHAWA = new GeoPoint(-1.1850, 36.8960);

	final Customer customer = Customer.register(PhoneNumber.of("0712345678"));

	final PartnerShop shop = PartnerShop.onboard("Maji Safi", PhoneNumber.of("0722000111"), KAHAWA, 50);

	Order newOrder(int jerrycans) {
		return Order.place(customer, shop, jerrycans, KAHAWA, OrderChannel.WHATSAPP, 20);
	}

	@Test
	void priceIsJerrycansTimesShopPricePlusDelivery() {
		assertThat(newOrder(2).totalKes()).isEqualTo(2 * 50 + 20);
	}

	@Test
	void newOrderWaitsForPaymentWithoutCode() {
		Order order = newOrder(1);
		assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(order.deliveryCode()).isNull();
	}

	@Test
	void refusesSillyQuantities() {
		assertThatThrownBy(() -> newOrder(0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> newOrder(Order.MAX_JERRYCANS + 1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void fullHappyPath() {
		Order order = newOrder(2);
		order.markPaid();
		assertThat(order.deliveryCode()).matches("\\d{4}");

		order.acceptBy(shop.id());
		order.sendOutForDelivery();

		assertThat(order.confirmDelivery("wrong")).isFalse();
		assertThat(order.status()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);

		assertThat(order.confirmDelivery(" " + order.deliveryCode() + " ")).isTrue();
		assertThat(order.status()).isEqualTo(OrderStatus.DELIVERED);
	}

	@Test
	void cannotAcceptUnpaidOrder() {
		assertThatThrownBy(() -> newOrder(1).acceptBy(shop.id()))
			.isInstanceOf(IllegalOrderTransitionException.class);
	}

	@Test
	void anotherShopCannotAccept() {
		Order order = newOrder(1);
		order.markPaid();
		assertThatThrownBy(() -> order.acceptBy(UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void cannotDeliverBeforeOutForDelivery() {
		Order order = newOrder(1);
		order.markPaid();
		assertThatThrownBy(() -> order.confirmDelivery(order.deliveryCode()))
			.isInstanceOf(IllegalOrderTransitionException.class);
	}

}
