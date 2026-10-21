package ke.wateratm.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ke.wateratm.application.port.in.HandlePaymentResultUseCase.PaymentResult;
import ke.wateratm.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import ke.wateratm.application.port.out.PaymentGatewayPort.PaymentGatewayException;
import ke.wateratm.application.service.OrderService;
import ke.wateratm.application.service.PaymentService;
import ke.wateratm.domain.exception.PaymentNotAllowedException;
import ke.wateratm.domain.model.GeoPoint;
import ke.wateratm.domain.model.Order;
import ke.wateratm.domain.model.OrderChannel;
import ke.wateratm.domain.model.OrderStatus;
import ke.wateratm.domain.model.PartnerShop;
import ke.wateratm.domain.model.Payment;
import ke.wateratm.domain.model.PaymentStatus;
import ke.wateratm.domain.model.PhoneNumber;

class PaymentServiceTest {

	static final GeoPoint CBD = new GeoPoint(-1.2864, 36.8172);

	final InMemoryAdapters.Orders orders = new InMemoryAdapters.Orders();

	final InMemoryAdapters.Customers customers = new InMemoryAdapters.Customers();

	final InMemoryAdapters.Shops shops = new InMemoryAdapters.Shops();

	final InMemoryAdapters.Payments payments = new InMemoryAdapters.Payments();

	final InMemoryAdapters.Gateway mpesa = new InMemoryAdapters.Gateway();

	final OrderService orderService = new OrderService(orders, customers, shops, 20);

	Order order;

	@BeforeEach
	void setUp() {
		shops.save(PartnerShop.onboard("Shop", PhoneNumber.of("0722000111"), CBD, 50));
		order = orderService.placeOrder(new PlaceOrderCommand("0712345678", 2, CBD.lat(), CBD.lng(), OrderChannel.APP));
	}

	PaymentService serviceAt(Instant now) {
		return new PaymentService(orders, customers, payments, mpesa, Clock.fixed(now, ZoneOffset.UTC));
	}

	PaymentService service() {
		return serviceAt(Instant.now());
	}

	PaymentResult success(String checkoutId, int amount) {
		return new PaymentResult(checkoutId, true, "SJK3ABC123", amount, "The service request is processed successfully.");
	}

	OrderStatus orderStatus() {
		return orders.byId.get(order.id()).status();
	}

	@Test
	void sendsPinPromptToCustomerForOrderTotal() {
		Payment payment = service().requestPayment(order.id());

		assertThat(mpesa.requests).hasSize(1);
		assertThat(mpesa.requests.get(0).phone()).isEqualTo(PhoneNumber.of("0712345678"));
		assertThat(mpesa.requests.get(0).amountKes()).isEqualTo(120);
		assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
	}

	@Test
	void doubleTapDoesNotSendTwoPrompts() {
		service().requestPayment(order.id());
		service().requestPayment(order.id());

		assertThat(mpesa.requests).hasSize(1);
	}

	@Test
	void successfulCallbackPaysOrder() {
		Payment payment = service().requestPayment(order.id());

		service().handle(success(payment.checkoutRequestId(), 120));

		assertThat(orderStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(orders.byId.get(order.id()).deliveryCode()).matches("\\d{4}");
	}

	@Test
	void duplicateCallbackIsIgnored() {
		Payment payment = service().requestPayment(order.id());
		service().handle(success(payment.checkoutRequestId(), 120));
		String code = orders.byId.get(order.id()).deliveryCode();

		service().handle(success(payment.checkoutRequestId(), 120));

		assertThat(orderStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(orders.byId.get(order.id()).deliveryCode()).isEqualTo(code);
	}

	@Test
	void cancelledPromptLetsCustomerRetry() {
		Payment first = service().requestPayment(order.id());
		service().handle(new PaymentResult(first.checkoutRequestId(), false, null, null, "Request cancelled by user"));

		assertThat(orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(payments.byId.get(first.id()).status()).isEqualTo(PaymentStatus.FAILED);

		service().requestPayment(order.id());
		assertThat(mpesa.requests).hasSize(2);
	}

	@Test
	void wrongAmountDoesNotPayOrder() {
		Payment payment = service().requestPayment(order.id());

		service().handle(success(payment.checkoutRequestId(), 1));

		assertThat(orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(payments.byId.get(payment.id()).status()).isEqualTo(PaymentStatus.AMOUNT_MISMATCH);
	}

	@Test
	void unknownCallbackIsIgnored() {
		service().handle(success("ws_CO_unknown", 120));
		assertThat(orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
	}

	@Test
	void cannotPayTwice() {
		Payment payment = service().requestPayment(order.id());
		service().handle(success(payment.checkoutRequestId(), 120));

		assertThatThrownBy(() -> service().requestPayment(order.id())).isInstanceOf(PaymentNotAllowedException.class);
	}

	@Test
	void mpesaDownSavesNothing() {
		mpesa.down = true;
		assertThatThrownBy(() -> service().requestPayment(order.id())).isInstanceOf(PaymentGatewayException.class);
		assertThat(payments.byId).isEmpty();
	}

	@Test
	void expiresUnansweredPaymentsThenCancelsUnpaidOrders() {
		Payment payment = service().requestPayment(order.id());

		assertThat(serviceAt(Instant.now().plus(Duration.ofMinutes(1))).expireStale()).isZero();

		serviceAt(Instant.now().plus(Duration.ofMinutes(6))).expireStale();
		assertThat(payments.byId.get(payment.id()).status()).isEqualTo(PaymentStatus.EXPIRED);
		assertThat(orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

		serviceAt(Instant.now().plus(Duration.ofMinutes(31))).expireStale();
		assertThat(orderStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	void moneyArrivingAfterOrderCancelledIsFlaggedForRefund() {
		Payment payment = service().requestPayment(order.id());
		serviceAt(Instant.now().plus(Duration.ofMinutes(31))).expireStale();
		assertThat(orderStatus()).isEqualTo(OrderStatus.CANCELLED);

		service().handle(success(payment.checkoutRequestId(), 120));

		assertThat(payments.byId.get(payment.id()).status()).isEqualTo(PaymentStatus.LATE_NEEDS_REFUND);
		assertThat(orderStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	void unknownOrderCannotBePaid() {
		assertThatThrownBy(() -> service().requestPayment(UUID.randomUUID())).isInstanceOf(RuntimeException.class);
	}

}
