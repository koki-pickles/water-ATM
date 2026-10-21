package ke.wateratm.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ke.wateratm.domain.model.GeoPoint;
import ke.wateratm.domain.model.PartnerShop;
import ke.wateratm.domain.model.PhoneNumber;

class NearestShopPolicyTest {

	static final GeoPoint KAHAWA = new GeoPoint(-1.1850, 36.8960);

	static final GeoPoint MOMBASA = new GeoPoint(-4.0435, 39.6682);

	PartnerShop shopAt(double lat, double lng, String phone) {
		return PartnerShop.onboard("Shop " + phone, PhoneNumber.of(phone), new GeoPoint(lat, lng), 50);
	}

	@Test
	void picksTheClosestShop() {
		PartnerShop far = shopAt(-1.1850, 36.8960, "0722000111");
		PartnerShop near = shopAt(-1.2050, 36.8960, "0722000222");

		assertThat(NearestShopPolicy.choose(List.of(far, near), new GeoPoint(-1.2040, 36.8960))).contains(near);
	}

	@Test
	void ignoresShopsTooFarAway() {
		assertThat(NearestShopPolicy.choose(List.of(shopAt(-1.1850, 36.8960, "0722000111")), MOMBASA)).isEmpty();
	}

	@Test
	void ignoresInactiveShops() {
		PartnerShop closed = shopAt(-1.1850, 36.8960, "0722000111");
		closed.deactivate();

		assertThat(NearestShopPolicy.choose(List.of(closed), KAHAWA)).isEmpty();
	}

}
