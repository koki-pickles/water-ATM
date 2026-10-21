package ke.wateratm.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import ke.wateratm.domain.model.GeoPoint;
import ke.wateratm.domain.model.Order;
import ke.wateratm.domain.model.OrderChannel;
import ke.wateratm.domain.model.OrderStatus;

@Entity
@Table(name = "orders")
class OrderJpaEntity {

	@Id
	UUID id;

	UUID customerId;

	UUID shopId;

	UUID subscriptionId;

	int jerrycans;

	int totalKes;

	@Enumerated(EnumType.STRING)
	OrderStatus status;

	@Enumerated(EnumType.STRING)
	OrderChannel channel;

	double deliveryLat;

	double deliveryLng;

	String deliveryCode;

	Instant createdAt;

	Instant updatedAt;

	static OrderJpaEntity from(Order order) {
		OrderJpaEntity e = new OrderJpaEntity();
		e.id = order.id();
		e.customerId = order.customerId();
		e.shopId = order.shopId();
		e.jerrycans = order.jerrycans();
		e.totalKes = order.totalKes();
		e.status = order.status();
		e.channel = order.channel();
		e.deliveryLat = order.deliveryLocation().lat();
		e.deliveryLng = order.deliveryLocation().lng();
		e.deliveryCode = order.deliveryCode();
		e.createdAt = order.createdAt();
		e.updatedAt = order.updatedAt();
		return e;
	}

	Order toDomain() {
		return Order.restore(id, customerId, shopId, jerrycans, totalKes, channel, new GeoPoint(deliveryLat, deliveryLng),
				status, deliveryCode, createdAt, updatedAt);
	}

}
