package com.aircraft.emms.repository;

import com.aircraft.emms.entity.FlightLogBook;
import com.aircraft.emms.entity.FlbStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightLogBookRepository extends JpaRepository<FlightLogBook, Long> {

    List<FlightLogBook> findByPilotId(Long pilotId);

    List<FlightLogBook> findBySortieId(Long sortieId);

    List<FlightLogBook> findByStatus(FlbStatus status);

    List<FlightLogBook> findByAircraftTypeAndAircraftNumber(String aircraftType, String aircraftNumber);
}
