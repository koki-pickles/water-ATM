package ke.wateratm.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import ke.wateratm.application.port.out.CustomerRepositoryPort;
import ke.wateratm.domain.model.Customer;
import ke.wateratm.domain.model.PhoneNumber;

@Component
class CustomerPersistenceAdapter implements CustomerRepositoryPort {

	private final CustomerJpaRepository repository;

	CustomerPersistenceAdapter(CustomerJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<Customer> findById(UUID id) {
		return repository.findById(id).map(CustomerJpaEntity::toDomain);
	}

	@Override
	public Optional<Customer> findByPhone(PhoneNumber phone) {
		return repository.findByPhone(phone.value()).map(CustomerJpaEntity::toDomain);
	}

	@Override
	public Customer save(Customer customer) {
		return repository.save(CustomerJpaEntity.from(customer)).toDomain();
	}

}
