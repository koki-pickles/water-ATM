package ke.wateratm.domain.model;

import static ke.wateratm.domain.model.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

	@Test
	void happyPath() {
		assertThat(PENDING_PAYMENT.canMoveTo(PAID)).isTrue();
		assertThat(PAID.canMoveTo(ACCEPTED)).isTrue();
		assertThat(ACCEPTED.canMoveTo(OUT_FOR_DELIVERY)).isTrue();
		assertThat(OUT_FOR_DELIVERY.canMoveTo(DELIVERED)).isTrue();
	}

	@Test
	void cannotSkipPayment() {
		assertThat(PENDING_PAYMENT.canMoveTo(ACCEPTED)).isFalse();
		assertThat(PENDING_PAYMENT.canMoveTo(DELIVERED)).isFalse();
	}

	@Test
	void paidOrdersAreRefundedNotCancelled() {
		assertThat(PAID.canMoveTo(CANCELLED)).isFalse();
		assertThat(PAID.canMoveTo(REFUND_NEEDED)).isTrue();
	}

	@Test
	void finishedOrdersNeverChange() {
		for (OrderStatus end : new OrderStatus[] { DELIVERED, CANCELLED, REFUNDED }) {
			assertThat(end.allowedNext()).isEmpty();
		}
	}

}
