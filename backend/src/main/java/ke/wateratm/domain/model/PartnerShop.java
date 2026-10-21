package ke.wateratm.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A water ATM / refill shop that fulfils orders. */
public class PartnerShop {

	private final UUID id;

	private final String name;

	private final PhoneNumber whatsappPhone;

	private final GeoPoint location;

	private final int pricePer20lKes;

	private boolean active;

	private final Instant createdAt;

	private PartnerShop(UUID id, String name, PhoneNumber whatsappPhone, GeoPoint location, int pricePer20lKes,
			boolean active, Instant createdAt) {
		if (pricePer20lKes <= 0) {
			throw new IllegalArgumentException("Price must be positive");
		}
		this.id = id;
		this.name = name;
		this.whatsappPhone = whatsappPhone;
		this.location = location;
		this.pricePer20lKes = pricePer20lKes;
		this.active = active;
		this.createdAt = createdAt;
	}

	public static PartnerShop onboard(String name, PhoneNumber whatsappPhone, GeoPoint location, int pricePer20lKes) {
		return new PartnerShop(UUID.randomUUID(), name, whatsappPhone, location, pricePer20lKes, true, Instant.now());
	}

	public static PartnerShop restore(UUID id, String name, PhoneNumber whatsappPhone, GeoPoint location,
			int pricePer20lKes, boolean active, Instant createdAt) {
		return new PartnerShop(id, name, whatsappPhone, location, pricePer20lKes, active, createdAt);
	}

	public void deactivate() {
		active = false;
	}

	public UUID id() {
		return id;
	}

	public String name() {
		return name;
	}

	public PhoneNumber whatsappPhone() {
		return whatsappPhone;
	}

	public GeoPoint location() {
		return location;
	}

	public int pricePer20lKes() {
		return pricePer20lKes;
	}

	public boolean isActive() {
		return active;
	}

	public Instant createdAt() {
		return createdAt;
	}

}
