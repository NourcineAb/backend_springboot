package com.enterprise.poodle.repository;

import com.enterprise.poodle.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByCertificateCode(String certificateCode);

    List<Certificate> findByEmployeeId(Long employeeId);

    boolean existsByEmployeeIdAndCourseId(Long employeeId, Long courseId);
}
