package com.aircraft.emms.repository;

import com.aircraft.emms.entity.MeterEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterEntryRepository extends JpaRepository<MeterEntry, Long> {

    List<MeterEntry> findByFlightLogBookId(Long flbId);

    @Query("SELECT me FROM MeterEntry me WHERE me.meterName = :meterName " +
           "ORDER BY me.createdAt DESC LIMIT 1")
    Optional<MeterEntry> findLatestByMeterName(@Param("meterName") String meterName);

    void deleteByFlightLogBookId(Long flbId);
}
