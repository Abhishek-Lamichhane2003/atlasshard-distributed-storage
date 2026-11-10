package com.atlasshard.storage.repo;

import com.atlasshard.storage.model.AuditEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
    List<AuditEvent> findTop50ByOrderByCreatedAtDesc();
    long deleteByCreatedAtBefore(Instant cutoff);
}
