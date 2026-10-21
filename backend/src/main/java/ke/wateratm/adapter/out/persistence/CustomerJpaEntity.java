package ke.wateratm.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import ke.wateratm.domain.model.Customer;
import ke.wateratm.domain.model.GeoPoint;
import ke.wateratm.domain.model.PhoneNumber;

@Entity
@Table(name = "customer")
class CustomerJpaEntity {

	@Id
	UUID id;

	String phone;

	String name;

	Double lat;

	Double lng;

	Instant createdAt;

	static CustomerJpaEntity from(Customer customer) {
		CustomerJpaEntity e = new CustomerJpaEntity();
		e.id = customer.id();
		e.phone = customer.phone().value();
		e.name = customer.name();
		if (customer.lastLocation() != null) {
			e.lat = customer.lastLocation().lat();
			e.lng = customer.lastLocation().lng();
		}
		e.createdAt = customer.createdAt();
		return e;
	}

	Customer toDomain() {
		GeoPoint location = (lat == null || lng == null) ? null : new GeoPoint(lat, lng);
		return Customer.restore(id, PhoneNumber.of(phone), name, location, createdAt);
	}

}
