package ke.wateratm.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import ke.wateratm.application.port.out.PaymentRepositoryPort;
import ke.wateratm.domain.model.Payment;
import ke.wateratm.domain.model.PaymentStatus;

@Component
class PaymentPersistenceAdapter implements PaymentRepositoryPort {

	private final PaymentJpaRepository repository;

	PaymentPersistenceAdapter(PaymentJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<Payment> findByCheckoutRequestIdForUpdate(String checkoutRequestId) {
		return repository.findByCheckoutRequestIdForUpdate(checkoutRequestId).map(PaymentJpaEntity::toDomain);
	}

	@Override
	public Optional<Payment> findPendingByOrderId(UUID orderId) {
		return repository.findFirstByOrderIdAndStatus(orderId, PaymentStatus.PENDING).map(PaymentJpaEntity::toDomain);
	}

	@Override
	public List<Payment> findPendingCreatedBefore(Instant cutoff) {
		return repository.findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, cutoff)
			.stream()
			.map(PaymentJpaEntity::toDomain)
			.toList();
	}

	@Override
	public Payment save(Payment payment) {
		return repository.save(PaymentJpaEntity.from(payment)).toDomain();
	}

}
