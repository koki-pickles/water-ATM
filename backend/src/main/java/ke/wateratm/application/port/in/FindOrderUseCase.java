package ke.wateratm.application.port.in;

import java.util.Optional;
import java.util.UUID;

import ke.wateratm.domain.model.Order;

public interface FindOrderUseCase {

	Optional<Order> findOrder(UUID orderId);

}
