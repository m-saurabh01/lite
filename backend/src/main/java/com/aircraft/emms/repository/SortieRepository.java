package com.aircraft.emms.repository;

import com.aircraft.emms.entity.Sortie;
import com.aircraft.emms.entity.SortieStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SortieRepository extends JpaRepository<Sortie, Long> {

    Optional<Sortie> findBySortieNumber(String sortieNumber);

    boolean existsBySortieNumber(String sortieNumber);

    @Query("SELECT COUNT(s) FROM Sortie s WHERE s.dataset.id = :datasetId")
    long countByDatasetId(@Param("datasetId") Long datasetId);

    List<Sortie> findByScheduledDate(LocalDate date);

    List<Sortie> findByStatus(SortieStatus status);

    List<Sortie> findByCaptainId(Long captainId);

    List<Sortie> findByPilotId(Long pilotId);

    @Query("SELECT DISTINCT s FROM Sortie s WHERE s.captain.id = :userId OR s.pilot.id = :userId")
    List<Sortie> findByCaptainIdOrPilotId(@Param("userId") Long userId);

    @Query("SELECT s FROM Sortie s WHERE s.pilot.id = :pilotId AND s.status IN :statuses")
    List<Sortie> findByPilotIdAndStatusIn(@Param("pilotId") Long pilotId,
                                           @Param("statuses") List<SortieStatus> statuses);

    @Query("SELECT s FROM Sortie s WHERE s.captain.id = :captainId AND s.scheduledDate = :date")
    List<Sortie> findByCaptainIdAndScheduledDate(@Param("captainId") Long captainId,
                                                  @Param("date") LocalDate date);

    List<Sortie> findByDatasetId(Long datasetId);

    void deleteByDatasetId(Long datasetId);

    /** Clash detection: find overlapping sorties for the same pilot on the same date */
    @Query("SELECT s FROM Sortie s WHERE s.pilot.id = :pilotId " +
           "AND s.scheduledDate = :date " +
           "AND s.status NOT IN ('CANCELLED', 'REJECTED') " +
           "AND s.scheduledStart < :endTime " +
           "AND s.scheduledEnd > :startTime " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId)")
    List<Sortie> findClashingSorties(@Param("pilotId") Long pilotId,
                                      @Param("date") LocalDate date,
                                      @Param("startTime") LocalTime startTime,
                                      @Param("endTime") LocalTime endTime,
                                      @Param("excludeId") Long excludeId);
}
