package ke.wateratm.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ke.wateratm.domain.model.Order;

/** Driven port: where orders are stored. Implemented by the persistence adapter. */
public interface OrderRepositoryPort {

	Optional<Order> findById(UUID id);

	/** Locks the order until the transaction ends, so two updates can't overwrite each other. */
	Optional<Order> findByIdForUpdate(UUID id);

	List<Order> findAwaitingPaymentCreatedBefore(Instant cutoff);

	Order save(Order order);

}
