package ke.wateratm.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import ke.wateratm.application.port.out.CustomerRepositoryPort;
import ke.wateratm.application.port.out.OrderRepositoryPort;
import ke.wateratm.application.port.out.PartnerShopRepositoryPort;
import ke.wateratm.application.port.out.PaymentGatewayPort;
import ke.wateratm.application.port.out.PaymentRepositoryPort;
import ke.wateratm.domain.model.Customer;
import ke.wateratm.domain.model.Order;
import ke.wateratm.domain.model.OrderStatus;
import ke.wateratm.domain.model.PartnerShop;
import ke.wateratm.domain.model.Payment;
import ke.wateratm.domain.model.PaymentStatus;
import ke.wateratm.domain.model.PhoneNumber;

/** Fake adapters that keep everything in memory, for testing use cases without a database or M-Pesa. */
public final class InMemoryAdapters {

	private InMemoryAdapters() {
	}

	public static class Orders implements OrderRepositoryPort {

		public final Map<UUID, Order> byId = new HashMap<>();

		@Override
		public Optional<Order> findById(UUID id) {
			return Optional.ofNullable(byId.get(id));
		}

		@Override
		public Optional<Order> findByIdForUpdate(UUID id) {
			return findById(id);
		}

		@Override
		public List<Order> findAwaitingPaymentCreatedBefore(Instant cutoff) {
			return byId.values()
				.stream()
				.filter(o -> o.status() == OrderStatus.PENDING_PAYMENT && o.createdAt().isBefore(cutoff))
				.toList();
		}

		@Override
		public Order save(Order order) {
			byId.put(order.id(), order);
			return order;
		}

	}

	public static class Customers implements CustomerRepositoryPort {

		public final Map<PhoneNumber, Customer> byPhone = new HashMap<>();

		@Override
		public Optional<Customer> findById(UUID id) {
			return byPhone.values().stream().filter(c -> c.id().equals(id)).findFirst();
		}

		@Override
		public Optional<Customer> findByPhone(PhoneNumber phone) {
			return Optional.ofNullable(byPhone.get(phone));
		}

		@Override
		public Customer save(Customer customer) {
			byPhone.put(customer.phone(), customer);
			return customer;
		}

	}

	public static class Shops implements PartnerShopRepositoryPort {

		public final List<PartnerShop> all = new ArrayList<>();

		@Override
		public List<PartnerShop> findActive() {
			return all.stream().filter(PartnerShop::isActive).toList();
		}

		@Override
		public PartnerShop save(PartnerShop shop) {
			all.add(shop);
			return shop;
		}

	}

	public static class Payments implements PaymentRepositoryPort {

		public final Map<UUID, Payment> byId = new HashMap<>();

		@Override
		public Optional<Payment> findByCheckoutRequestIdForUpdate(String checkoutRequestId) {
			return byId.values().stream().filter(p -> p.checkoutRequestId().equals(checkoutRequestId)).findFirst();
		}

		@Override
		public Optional<Payment> findPendingByOrderId(UUID orderId) {
			return byId.values()
				.stream()
				.filter(p -> p.orderId().equals(orderId) && p.status() == PaymentStatus.PENDING)
				.findFirst();
		}

		@Override
		public List<Payment> findPendingCreatedBefore(Instant cutoff) {
			return byId.values()
				.stream()
				.filter(p -> p.status() == PaymentStatus.PENDING && p.createdAt().isBefore(cutoff))
				.toList();
		}

		@Override
		public Payment save(Payment payment) {
			byId.put(payment.id(), payment);
			return payment;
		}

	}

	/** Pretends to be M-Pesa: records every PIN prompt and returns ids ws_CO_1, ws_CO_2... */
	public static class Gateway implements PaymentGatewayPort {

		public final List<StkPushRequest> requests = new ArrayList<>();

		public boolean down;

		@Override
		public String requestStkPush(StkPushRequest request) {
			if (down) {
				throw new PaymentGatewayException("M-Pesa is down", null);
			}
			requests.add(request);
			return "ws_CO_" + requests.size();
		}

	}

}
