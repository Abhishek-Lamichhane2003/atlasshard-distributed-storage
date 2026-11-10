package com.atlasshard.storage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "files")
public class FileEntry {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String ownerId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String logicalPath;

    @Column(nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false)
    private String objectKey;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private int versionNumber;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String value) { ownerId = value; }
    public String getFileName() { return fileName; }
    public void setFileName(String value) { fileName = value; }
    public String getLogicalPath() { return logicalPath; }
    public void setLogicalPath(String value) { logicalPath = value; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String value) { contentHash = value; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String value) { objectKey = value; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long value) { sizeBytes = value; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int value) { versionNumber = value; }
    public Instant getCreatedAt() { return createdAt; }
}
