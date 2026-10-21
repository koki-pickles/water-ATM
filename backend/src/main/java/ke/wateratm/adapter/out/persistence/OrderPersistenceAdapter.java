package ke.wateratm.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import ke.wateratm.application.port.out.OrderRepositoryPort;
import ke.wateratm.domain.model.Order;
import ke.wateratm.domain.model.OrderStatus;

@Component
class OrderPersistenceAdapter implements OrderRepositoryPort {

	private final OrderJpaRepository repository;

	OrderPersistenceAdapter(OrderJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<Order> findById(UUID id) {
		return repository.findById(id).map(OrderJpaEntity::toDomain);
	}

	@Override
	public Optional<Order> findByIdForUpdate(UUID id) {
		return repository.findByIdForUpdate(id).map(OrderJpaEntity::toDomain);
	}

	@Override
	public List<Order> findAwaitingPaymentCreatedBefore(Instant cutoff) {
		return repository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, cutoff)
			.stream()
			.map(OrderJpaEntity::toDomain)
			.toList();
	}

	@Override
	public Order save(Order order) {
		return repository.save(OrderJpaEntity.from(order)).toDomain();
	}

}
