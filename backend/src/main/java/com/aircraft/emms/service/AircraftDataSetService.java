package com.aircraft.emms.service;

import com.aircraft.emms.entity.AircraftDataSet;
import com.aircraft.emms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Central service for aircraft dataset management.
 * Enforces: only ONE active aircraft at a time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AircraftDataSetService {

    private final AircraftDataSetRepository datasetRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final MeterDefinitionRepository meterDefRepository;
    private final SortieRepository sortieRepository;
    private final FlightLogBookRepository flbRepository;
    private final SnagRepository snagRepository;
    private final MeterEntryRepository meterEntryRepository;
    private final AuditService auditService;

    /**
     * Returns the currently active aircraft dataset, or empty if none active.
     */
    @Transactional(readOnly = true)
    public Optional<AircraftDataSet> getActiveDataset() {
        return datasetRepository.findByActiveTrue();
    }

    /**
     * Returns the active dataset or throws if none is active.
     */
    @Transactional(readOnly = true)
    public AircraftDataSet requireActiveDataset() {
        return datasetRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("No active aircraft. Please activate an aircraft dataset first."));
    }

    /**
     * Activate a dataset. Deactivates any previously active one.
     */
    @Transactional
    public AircraftDataSet activate(Long datasetId, String activatedBy) {
        AircraftDataSet dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft dataset not found: " + datasetId));

        datasetRepository.deactivateAll();
        datasetRepository.activateById(datasetId);
        datasetRepository.flush();

        dataset.setActive(true);

        auditService.log(activatedBy, "ACTIVATE_AIRCRAFT", "AircraftDataSet", datasetId,
                "Activated aircraft: " + dataset.getAssetNum() + " (" + dataset.getAircraftName() + ")");

        log.info("Aircraft activated: {} ({})", dataset.getAssetNum(), dataset.getAircraftName());
        return dataset;
    }

    @Transactional(readOnly = true)
    public List<AircraftDataSet> getAllDatasets() {
        return datasetRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public AircraftDataSet getDatasetById(Long id) {
        return datasetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft dataset not found: " + id));
    }

    /**
     * Truncate ALL data linked to a dataset. Irreversible.
     */
    @Transactional
    public void truncateDataset(Long datasetId, String truncatedBy) {
        AircraftDataSet dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft dataset not found: " + datasetId));

        // Delete in dependency order: meter_entries → flbs → sorties → snags → meters → assets → users
        List<Long> flbIds = flbRepository.findByDatasetId(datasetId).stream()
                .map(flb -> flb.getId()).toList();
        for (Long flbId : flbIds) {
            meterEntryRepository.deleteByFlightLogBookId(flbId);
        }
        flbRepository.deleteByDatasetId(datasetId);
        sortieRepository.deleteByDatasetId(datasetId);
        snagRepository.deleteByDatasetId(datasetId);
        meterDefRepository.deleteByDatasetId(datasetId);
        assetRepository.deleteByDatasetId(datasetId);
        userRepository.deleteByDatasetId(datasetId);
        datasetRepository.delete(dataset);

        auditService.log(truncatedBy, "TRUNCATE_AIRCRAFT", "AircraftDataSet", datasetId,
                "Truncated all data for aircraft: " + dataset.getAssetNum());

        log.info("Truncated aircraft dataset: {} ({})", dataset.getAssetNum(), dataset.getAircraftName());
    }
}
