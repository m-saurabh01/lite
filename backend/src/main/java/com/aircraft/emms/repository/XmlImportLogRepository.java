package com.aircraft.emms.repository;

import com.aircraft.emms.entity.XmlImportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface XmlImportLogRepository extends JpaRepository<XmlImportLog, Long> {

    List<XmlImportLog> findAllByOrderByImportedAtDesc();
}
