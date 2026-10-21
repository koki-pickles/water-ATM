package ke.wateratm.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import ke.wateratm.domain.model.Payment;
import ke.wateratm.domain.model.PaymentStatus;

@Entity
@Table(name = "payment")
class PaymentJpaEntity {

	@Id
	UUID id;

	UUID orderId;

	String checkoutRequestId;

	String mpesaReceipt;

	int amountKes;

	@Enumerated(EnumType.STRING)
	PaymentStatus status;

	String resultDescription;

	Instant createdAt;

	Instant updatedAt;

	static PaymentJpaEntity from(Payment payment) {
		PaymentJpaEntity e = new PaymentJpaEntity();
		e.id = payment.id();
		e.orderId = payment.orderId();
		e.checkoutRequestId = payment.checkoutRequestId();
		e.mpesaReceipt = payment.mpesaReceipt();
		e.amountKes = payment.amountKes();
		e.status = payment.status();
		e.resultDescription = payment.resultDescription();
		e.createdAt = payment.createdAt();
		e.updatedAt = payment.updatedAt();
		return e;
	}

	Payment toDomain() {
		return Payment.restore(id, orderId, checkoutRequestId, amountKes, status, mpesaReceipt, resultDescription,
				createdAt, updatedAt);
	}

}
