package com.aircraft.emms.repository;

import com.aircraft.emms.entity.MeterDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeterDefinitionRepository extends JpaRepository<MeterDefinition, Long> {

    List<MeterDefinition> findByAircraftTypeAndActiveTrueOrderByDisplayOrderAsc(String aircraftType);

    List<MeterDefinition> findByActiveTrueOrderByDisplayOrderAsc();
}
