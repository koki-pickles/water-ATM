package ke.wateratm.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import ke.wateratm.application.port.out.PartnerShopRepositoryPort;
import ke.wateratm.domain.model.PartnerShop;

@Component
class PartnerShopPersistenceAdapter implements PartnerShopRepositoryPort {

	private final PartnerShopJpaRepository repository;

	PartnerShopPersistenceAdapter(PartnerShopJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public List<PartnerShop> findActive() {
		return repository.findByActiveTrue().stream().map(PartnerShopJpaEntity::toDomain).toList();
	}

	@Override
	public PartnerShop save(PartnerShop shop) {
		return repository.save(PartnerShopJpaEntity.from(shop)).toDomain();
	}

}
