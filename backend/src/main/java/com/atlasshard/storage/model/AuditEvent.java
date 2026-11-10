package com.atlasshard.storage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, length = 1000)
    private String detail;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String value) {
        type = value;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String value) {
        detail = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
