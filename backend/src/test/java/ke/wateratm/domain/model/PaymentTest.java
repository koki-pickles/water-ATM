package ke.wateratm.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PaymentTest {

	static final GeoPoint CBD = new GeoPoint(-1.2864, 36.8172);

	final Order order = Order.place(Customer.register(PhoneNumber.of("0712345678")),
			PartnerShop.onboard("Shop", PhoneNumber.of("0722000111"), CBD, 50), 2, CBD, OrderChannel.APP, 20);

	final Payment payment = Payment.started(order, "ws_CO_1");

	@Test
	void startsPendingForTheOrderTotal() {
		assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
		assertThat(payment.amountKes()).isEqualTo(120);
	}

	@Test
	void success() {
		payment.recordSuccess("SJK3ABC123", 120, true);
		assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCEEDED);
		assertThat(payment.mpesaReceipt()).isEqualTo("SJK3ABC123");
	}

	@Test
	void wrongAmountNeedsAHuman() {
		payment.recordSuccess("SJK3ABC123", 1, true);
		assertThat(payment.status()).isEqualTo(PaymentStatus.AMOUNT_MISMATCH);
	}

	@Test
	void moneyForAClosedOrderMustBeRefunded() {
		payment.recordSuccess("SJK3ABC123", 120, false);
		assertThat(payment.status()).isEqualTo(PaymentStatus.LATE_NEEDS_REFUND);
	}

	@Test
	void lateSuccessAfterExpiryIsStillRecorded() {
		payment.expire();
		payment.recordSuccess("SJK3ABC123", 120, true);
		assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCEEDED);
	}

	@Test
	void finishedPaymentCannotChangeAgain() {
		payment.recordSuccess("SJK3ABC123", 120, true);
		assertThatThrownBy(() -> payment.recordFailure("oops")).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(payment::expire).isInstanceOf(IllegalStateException.class);
	}

}
