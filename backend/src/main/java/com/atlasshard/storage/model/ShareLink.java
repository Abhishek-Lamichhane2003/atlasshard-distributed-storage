package com.atlasshard.storage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "share_links")
public class ShareLink {
    @Id
    private String id = UUID.randomUUID().toString();
    @Column(nullable = false)
    private String fileId;
    @Column(nullable = false)
    private String ownerId;
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public String getFileId() { return fileId; }
    public void setFileId(String value) { fileId = value; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String value) { ownerId = value; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String value) { tokenHash = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { expiresAt = value; }
    public Instant getCreatedAt() { return createdAt; }
}
