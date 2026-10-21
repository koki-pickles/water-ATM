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
import ke.wateratm.application.port.out.PaymentGatewayPort.StkStatus;
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

	Instant minutesLater(int minutes) {
		return Instant.now().plus(Duration.ofMinutes(minutes));
	}

	@Test
	void doesNotAskMpesaBeforeTheTimeout() {
		service().requestPayment(order.id());

		assertThat(serviceAt(minutesLater(1)).expireStale()).isZero();
		assertThat(mpesa.statusQueries).isEmpty();
	}

	@Test
	void lostPaidCallbackIsRescuedByAskingMpesa() {
		Payment payment = service().requestPayment(order.id());
		mpesa.statusAnswer = new StkStatus(StkStatus.State.PAID, "The service request is processed successfully.");

		serviceAt(minutesLater(6)).expireStale();

		assertThat(mpesa.statusQueries).containsExactly(payment.checkoutRequestId());
		assertThat(payments.byId.get(payment.id()).status()).isEqualTo(PaymentStatus.SUCCEEDED);
		assertThat(orderStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(orders.byId.get(order.id()).deliveryCode()).matches("\\d{4}");
	}

	@Test
	void lateCallbackAfterQueryAddsTheReceipt() {
		Payment payment = service().requestPayment(order.id());
		mpesa.statusAnswer = new StkStatus(StkStatus.State.PAID, "ok");
		serviceAt(minutesLater(6)).expireStale();
		assertThat(payments.byId.get(payment.id()).mpesaReceipt()).isNull();

		service().handle(success(payment.checkoutRequestId(), 120));

		assertThat(payments.byId.get(payment.id()).mpesaReceipt()).isEqualTo("SJK3ABC123");
		assertThat(orderStatus()).isEqualTo(OrderStatus.PAID);
	}

	@Test
	void noPinEnteredIsClosedWithMpesasReason() {
		Payment payment = service().requestPayment(order.id());

		serviceAt(minutesLater(6)).expireStale();

		assertThat(payments.byId.get(payment.id()).status()).isEqualTo(PaymentStatus.FAILED);
		assertThat(payments.byId.get(payment.id()).resultDescription()).isEqualTo("No response from user.");
		assertThat(orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
	}

	@Test
	void stillProcessingIsAskedAgainLater() {
		Payment payment = service().requestPayment(order.id());
		mpesa.statusAnswer = new StkStatus(StkStatus.State.STILL_PROCESSING, "The transaction is being processed");

		serviceAt(minutesLater(6)).expireStale();
		serviceAt(minutesLater(7)).expireStale();

		assertThat(mpesa.statusQueries).hasSize(2);
		assertThat(payments.byId.get(payment.id()).status()).isEqualTo(PaymentStatus.PENDING);
	}

	@Test
	void mpesaDownKeepsPaymentOpenAndOrderAlive() {
		Payment payment = service().requestPayment(order.id());
		mpesa.down = true;

		serviceAt(minutesLater(31)).expireStale();

		assertThat(payments.byId.get(payment.id()).status()).isEqualTo(PaymentStatus.PENDING);
		assertThat(orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
	}

	@Test
	void givesUpAfterAnHourOfNoAnswer() {
		Payment payment = service().requestPayment(order.id());
		mpesa.down = true;

		serviceAt(minutesLater(61)).expireStale();

		assertThat(payments.byId.get(payment.id()).status()).isEqualTo(PaymentStatus.EXPIRED);
		assertThat(orderStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	void unpaidOrderIsCancelledAfterThirtyMinutes() {
		service().requestPayment(order.id());

		serviceAt(minutesLater(6)).expireStale();
		assertThat(orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

		serviceAt(minutesLater(31)).expireStale();
		assertThat(orderStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	void moneyArrivingAfterOrderCancelledIsFlaggedForRefund() {
		Payment payment = service().requestPayment(order.id());
		mpesa.down = true;
		serviceAt(minutesLater(61)).expireStale();
		assertThat(orderStatus()).isEqualTo(OrderStatus.CANCELLED);

		service().handle(success(payment.checkoutRequestId(), 120));

		assertThat(payments.byId.get(payment.id()).status()).isEqualTo(PaymentStatus.LATE_NEEDS_REFUND);
		assertThat(orderStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	void successWithoutAmountNeedsAHuman() {
		Payment payment = service().requestPayment(order.id());

		service().handle(new PaymentResult(payment.checkoutRequestId(), true, "SJK3ABC123", null, "ok"));

		assertThat(payments.byId.get(payment.id()).status()).isEqualTo(PaymentStatus.AMOUNT_MISMATCH);
		assertThat(orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
	}

	@Test
	void unknownOrderCannotBePaid() {
		assertThatThrownBy(() -> service().requestPayment(UUID.randomUUID())).isInstanceOf(RuntimeException.class);
	}

}
