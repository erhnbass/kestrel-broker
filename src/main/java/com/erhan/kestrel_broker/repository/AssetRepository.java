package com.erhan.kestrel_broker.repository;

import com.erhan.kestrel_broker.domain.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findByCustomerIdAndAssetName(String customerId, String assetName);

    List<Asset> findByCustomerId(String customerId);

}
