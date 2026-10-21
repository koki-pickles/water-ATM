package ke.wateratm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import ke.wateratm.TestcontainersConfiguration;
import ke.wateratm.application.port.in.PlaceOrderUseCase;
import ke.wateratm.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import ke.wateratm.application.port.in.UpdateOrderStatusUseCase;
import ke.wateratm.application.port.out.OrderRepositoryPort;
import ke.wateratm.application.port.out.PartnerShopRepositoryPort;
import ke.wateratm.domain.model.GeoPoint;
import ke.wateratm.domain.model.Order;
import ke.wateratm.domain.model.OrderChannel;
import ke.wateratm.domain.model.OrderStatus;
import ke.wateratm.domain.model.PartnerShop;
import ke.wateratm.domain.model.PhoneNumber;

/** The whole hexagon wired by Spring, against a real Postgres in Docker (Testcontainers + Flyway). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OrderPersistenceAdapterTest {

	@Autowired
	PlaceOrderUseCase placeOrder;

	@Autowired
	UpdateOrderStatusUseCase updateStatus;

	@Autowired
	OrderRepositoryPort orders;

	@Autowired
	PartnerShopRepositoryPort shops;

	@Test
	void orderSurvivesRoundTripThroughPostgres() {
		GeoPoint westlands = new GeoPoint(-1.2676, 36.8108);
		PartnerShop shop = shops
			.save(PartnerShop.onboard("Westlands Water", PhoneNumber.of("0722" + randomDigits()), westlands, 40));

		UUID id = placeOrder.placeOrder(new PlaceOrderCommand("0712" + randomDigits(), 3, westlands.lat(),
				westlands.lng(), OrderChannel.APP))
			.id();
		updateStatus.markPaid(id);

		Order loaded = orders.findById(id).orElseThrow();
		assertThat(loaded.status()).isEqualTo(OrderStatus.PAID);
		assertThat(loaded.shopId()).isEqualTo(shop.id());
		assertThat(loaded.totalKes()).isEqualTo(3 * 40 + 20);
		assertThat(loaded.deliveryCode()).matches("\\d{4}");
		assertThat(loaded.deliveryLocation()).isEqualTo(westlands);
	}

	private static String randomDigits() {
		return String.format("%06d", (int) (Math.random() * 1_000_000));
	}

}
