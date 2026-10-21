package ke.wateratm.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import ke.wateratm.domain.model.OrderStatus;
import ke.wateratm.domain.model.PaymentStatus;

interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, UUID> {

	Optional<CustomerJpaEntity> findByPhone(String phone);

}

interface PartnerShopJpaRepository extends JpaRepository<PartnerShopJpaEntity, UUID> {

	List<PartnerShopJpaEntity> findByActiveTrue();

}

interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

	/** SELECT ... FOR UPDATE */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from OrderJpaEntity o where o.id = :id")
	Optional<OrderJpaEntity> findByIdForUpdate(UUID id);

	List<OrderJpaEntity> findByStatusAndCreatedAtBefore(OrderStatus status, Instant cutoff);

}

interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

	/** SELECT ... FOR UPDATE */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from PaymentJpaEntity p where p.checkoutRequestId = :checkoutRequestId")
	Optional<PaymentJpaEntity> findByCheckoutRequestIdForUpdate(String checkoutRequestId);

	Optional<PaymentJpaEntity> findFirstByOrderIdAndStatus(UUID orderId, PaymentStatus status);

	List<PaymentJpaEntity> findByStatusAndCreatedAtBefore(PaymentStatus status, Instant cutoff);

}
