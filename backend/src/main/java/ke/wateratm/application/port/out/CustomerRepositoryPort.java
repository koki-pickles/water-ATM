package ke.wateratm.application.port.out;

import java.util.Optional;
import java.util.UUID;

import ke.wateratm.domain.model.Customer;
import ke.wateratm.domain.model.PhoneNumber;

public interface CustomerRepositoryPort {

	Optional<Customer> findById(UUID id);

	Optional<Customer> findByPhone(PhoneNumber phone);

	Customer save(Customer customer);

}
