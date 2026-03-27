package com.aircraft.emms.repository;

import com.aircraft.emms.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    List<Asset> findByDatasetId(Long datasetId);

    Optional<Asset> findByAssetNumAndDatasetId(String assetNum, Long datasetId);

    boolean existsByAssetNumAndDatasetId(String assetNum, Long datasetId);

    List<Asset> findByParentAssetNumAndDatasetId(String parentAssetNum, Long datasetId);

    void deleteByDatasetId(Long datasetId);
}
