package com.aircraft.emms.repository;

import com.aircraft.emms.entity.Snag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnagRepository extends JpaRepository<Snag, Long> {

    List<Snag> findByDatasetId(Long datasetId);

    void deleteByDatasetId(Long datasetId);
}
