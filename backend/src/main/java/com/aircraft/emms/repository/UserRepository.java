package com.aircraft.emms.repository;

import com.aircraft.emms.entity.User;
import com.aircraft.emms.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByServiceId(String serviceId);

    boolean existsByServiceId(String serviceId);

    List<User> findByRole(Role role);

    List<User> findByActiveTrue();

    List<User> findByRoleAndActiveTrue(Role role);

    List<User> findByDatasetId(Long datasetId);

    List<User> findByDatasetIdAndActiveTrue(Long datasetId);

    Optional<User> findByServiceIdAndDatasetId(String serviceId, Long datasetId);

    void deleteByDatasetId(Long datasetId);
}
