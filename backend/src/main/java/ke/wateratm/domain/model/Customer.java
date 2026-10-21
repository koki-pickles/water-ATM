package ke.wateratm.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Customer {

	private final UUID id;

	private final PhoneNumber phone;

	private String name;

	private GeoPoint lastLocation;

	private final Instant createdAt;

	private Customer(UUID id, PhoneNumber phone, String name, GeoPoint lastLocation, Instant createdAt) {
		this.id = id;
		this.phone = phone;
		this.name = name;
		this.lastLocation = lastLocation;
		this.createdAt = createdAt;
	}

	/** A brand-new customer, the first time we see this phone number. */
	public static Customer register(PhoneNumber phone) {
		return new Customer(UUID.randomUUID(), phone, null, null, Instant.now());
	}

	/** Rebuild a customer that already exists (used when loading from the database). */
	public static Customer restore(UUID id, PhoneNumber phone, String name, GeoPoint lastLocation, Instant createdAt) {
		return new Customer(id, phone, name, lastLocation, createdAt);
	}

	public void moveTo(GeoPoint location) {
		this.lastLocation = location;
	}

	public void rename(String name) {
		this.name = name;
	}

	public UUID id() {
		return id;
	}

	public PhoneNumber phone() {
		return phone;
	}

	public String name() {
		return name;
	}

	public GeoPoint lastLocation() {
		return lastLocation;
	}

	public Instant createdAt() {
		return createdAt;
	}

}
