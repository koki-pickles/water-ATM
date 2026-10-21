package ke.wateratm.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import ke.wateratm.domain.model.GeoPoint;
import ke.wateratm.domain.model.PartnerShop;
import ke.wateratm.domain.model.PhoneNumber;

@Entity
@Table(name = "partner_shop")
class PartnerShopJpaEntity {

	@Id
	UUID id;

	String name;

	String whatsappPhone;

	double lat;

	double lng;

	@Column(name = "price_per_20l_kes")
	int pricePer20lKes;

	boolean active;

	Instant createdAt;

	static PartnerShopJpaEntity from(PartnerShop shop) {
		PartnerShopJpaEntity e = new PartnerShopJpaEntity();
		e.id = shop.id();
		e.name = shop.name();
		e.whatsappPhone = shop.whatsappPhone().value();
		e.lat = shop.location().lat();
		e.lng = shop.location().lng();
		e.pricePer20lKes = shop.pricePer20lKes();
		e.active = shop.isActive();
		e.createdAt = shop.createdAt();
		return e;
	}

	PartnerShop toDomain() {
		return PartnerShop.restore(id, name, PhoneNumber.of(whatsappPhone), new GeoPoint(lat, lng), pricePer20lKes,
				active, createdAt);
	}

}
