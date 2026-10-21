package ke.wateratm.application.port.out;

import java.util.List;

import ke.wateratm.domain.model.PartnerShop;

public interface PartnerShopRepositoryPort {

	List<PartnerShop> findActive();

	PartnerShop save(PartnerShop shop);

}
