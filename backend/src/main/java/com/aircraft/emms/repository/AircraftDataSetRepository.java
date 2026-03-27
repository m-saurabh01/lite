package com.aircraft.emms.repository;

import com.aircraft.emms.entity.AircraftDataSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AircraftDataSetRepository extends JpaRepository<AircraftDataSet, Long> {

    Optional<AircraftDataSet> findByAssetNum(String assetNum);

    boolean existsByAssetNum(String assetNum);

    Optional<AircraftDataSet> findByActiveTrue();

    List<AircraftDataSet> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE AircraftDataSet a SET a.active = false WHERE a.active = true")
    void deactivateAll();

    @Modifying
    @Query("UPDATE AircraftDataSet a SET a.active = true WHERE a.id = :id")
    void activateById(@Param("id") Long id);
}
