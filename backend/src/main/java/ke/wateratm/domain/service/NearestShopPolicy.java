package ke.wateratm.domain.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import ke.wateratm.domain.model.GeoPoint;
import ke.wateratm.domain.model.PartnerShop;

/** Business rule: which shop serves a delivery location. */
public final class NearestShopPolicy {

	/** Shops further than this can't deliver in reasonable time. */
	public static final double MAX_DISTANCE_KM = 5.0;

	private NearestShopPolicy() {
	}

	public static Optional<PartnerShop> choose(Collection<PartnerShop> shops, GeoPoint deliveryLocation) {
		return shops.stream()
			.filter(PartnerShop::isActive)
			.filter(shop -> shop.location().distanceKmTo(deliveryLocation) <= MAX_DISTANCE_KM)
			.min(Comparator.comparingDouble(shop -> shop.location().distanceKmTo(deliveryLocation)));
	}

}
