package ke.wateratm.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ke.wateratm.domain.model.Payment;

public interface PaymentRepositoryPort {

	/** Locks the payment until the transaction ends, so two callbacks can't process it at once. */
	Optional<Payment> findByCheckoutRequestIdForUpdate(String checkoutRequestId);

	Optional<Payment> findPendingByOrderId(UUID orderId);

	List<Payment> findPendingCreatedBefore(Instant cutoff);

	Payment save(Payment payment);

}
