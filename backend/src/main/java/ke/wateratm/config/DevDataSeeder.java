package ke.wateratm.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ke.wateratm.application.port.out.PartnerShopRepositoryPort;
import ke.wateratm.domain.model.GeoPoint;
import ke.wateratm.domain.model.PartnerShop;
import ke.wateratm.domain.model.PhoneNumber;

/** Only with {@code --spring.profiles.active=dev}: adds a demo shop so you can place test orders. */
@Configuration(proxyBeanMethods = false)
@Profile("dev")
class DevDataSeeder {

	static final GeoPoint NAIROBI_CBD = new GeoPoint(-1.2864, 36.8172);

	@Bean
	ApplicationRunner seedDemoShop(PartnerShopRepositoryPort shops) {
		return args -> {
			if (shops.findActive().isEmpty()) {
				shops.save(PartnerShop.onboard("Demo Water ATM (CBD)", PhoneNumber.of("0700000000"), NAIROBI_CBD, 50));
			}
		};
	}

}
