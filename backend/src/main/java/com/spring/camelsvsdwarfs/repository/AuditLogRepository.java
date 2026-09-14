package com.spring.camelsvsdwarfs.repository;
import com.spring.camelsvsdwarfs.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, java.util.UUID> {
    Page<AuditLog> findAll(Pageable pageable);
}