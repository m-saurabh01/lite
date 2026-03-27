package com.aircraft.emms.repository;

import com.aircraft.emms.entity.MeterDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterDefinitionRepository extends JpaRepository<MeterDefinition, Long> {

    List<MeterDefinition> findByAircraftTypeAndActiveTrueOrderByDisplayOrderAsc(String aircraftType);

    List<MeterDefinition> findByActiveTrueOrderByDisplayOrderAsc();

    List<MeterDefinition> findByDatasetIdAndActiveTrueOrderByDisplayOrderAsc(Long datasetId);

    List<MeterDefinition> findByDatasetIdAndAssetNumAndActiveTrueOrderByDisplayOrderAsc(Long datasetId, String assetNum);

    Optional<MeterDefinition> findByMeterNumAndDatasetId(String meterNum, Long datasetId);

    void deleteByDatasetId(Long datasetId);
}
