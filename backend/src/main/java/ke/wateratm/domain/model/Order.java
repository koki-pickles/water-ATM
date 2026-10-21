package ke.wateratm.domain.model;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

import ke.wateratm.domain.exception.IllegalOrderTransitionException;

public class Order {

	public static final int MAX_JERRYCANS = 10;

	private static final SecureRandom RANDOM = new SecureRandom();

	private final UUID id;

	private final UUID customerId;

	private final UUID shopId;

	private final int jerrycans;

	private final int totalKes;

	private final OrderChannel channel;

	private final GeoPoint deliveryLocation;

	private OrderStatus status;

	private String deliveryCode;

	private final Instant createdAt;

	private Instant updatedAt;

	private Order(UUID id, UUID customerId, UUID shopId, int jerrycans, int totalKes, OrderChannel channel,
			GeoPoint deliveryLocation, OrderStatus status, String deliveryCode, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.customerId = customerId;
		this.shopId = shopId;
		this.jerrycans = jerrycans;
		this.totalKes = totalKes;
		this.channel = channel;
		this.deliveryLocation = deliveryLocation;
		this.status = status;
		this.deliveryCode = deliveryCode;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	/** A new order, waiting for M-Pesa payment. Price = jerrycans × shop price + delivery fee. */
	public static Order place(Customer customer, PartnerShop shop, int jerrycans, GeoPoint deliveryLocation,
			OrderChannel channel, int deliveryFeeKes) {
		if (jerrycans < 1 || jerrycans > MAX_JERRYCANS) {
			throw new IllegalArgumentException("Jerrycans must be between 1 and " + MAX_JERRYCANS);
		}
		int total = jerrycans * shop.pricePer20lKes() + deliveryFeeKes;
		Instant now = Instant.now();
		return new Order(UUID.randomUUID(), customer.id(), shop.id(), jerrycans, total, channel, deliveryLocation,
				OrderStatus.PENDING_PAYMENT, null, now, now);
	}

	public static Order restore(UUID id, UUID customerId, UUID shopId, int jerrycans, int totalKes,
			OrderChannel channel, GeoPoint deliveryLocation, OrderStatus status, String deliveryCode,
			Instant createdAt, Instant updatedAt) {
		return new Order(id, customerId, shopId, jerrycans, totalKes, channel, deliveryLocation, status, deliveryCode,
				createdAt, updatedAt);
	}

	public void markPaid() {
		moveTo(OrderStatus.PAID);
		// The customer gets this code once they've paid and hands it to the rider on delivery.
		deliveryCode = String.format("%04d", RANDOM.nextInt(10_000));
	}

	public boolean isAwaitingPayment() {
		return status == OrderStatus.PENDING_PAYMENT;
	}

	public void cancelUnpaid() {
		moveTo(OrderStatus.CANCELLED);
	}

	public void acceptBy(UUID shopId) {
		if (!this.shopId.equals(shopId)) {
			throw new IllegalArgumentException("Order " + id + " belongs to another shop");
		}
		moveTo(OrderStatus.ACCEPTED);
	}

	public void sendOutForDelivery() {
		moveTo(OrderStatus.OUT_FOR_DELIVERY);
	}

	/** @return true if the code was right and the order is now delivered */
	public boolean confirmDelivery(String code) {
		if (deliveryCode == null || code == null || !deliveryCode.equals(code.trim())) {
			return false;
		}
		moveTo(OrderStatus.DELIVERED);
		return true;
	}

	public void flagForRefund() {
		moveTo(OrderStatus.REFUND_NEEDED);
	}

	private void moveTo(OrderStatus next) {
		if (!status.canMoveTo(next)) {
			throw new IllegalOrderTransitionException(id, status, next);
		}
		status = next;
		updatedAt = Instant.now();
	}

	public UUID id() {
		return id;
	}

	public UUID customerId() {
		return customerId;
	}

	public UUID shopId() {
		return shopId;
	}

	public int jerrycans() {
		return jerrycans;
	}

	public int totalKes() {
		return totalKes;
	}

	public OrderChannel channel() {
		return channel;
	}

	public GeoPoint deliveryLocation() {
		return deliveryLocation;
	}

	public OrderStatus status() {
		return status;
	}

	public String deliveryCode() {
		return deliveryCode;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

}
