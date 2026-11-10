package com.atlasshard.storage.service;

import com.atlasshard.storage.model.FileEntry;
import com.atlasshard.storage.repo.FileRepository;
import com.atlasshard.storage.repo.ShareLinkRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FileService {
    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
        Long.class);

    private final FileRepository repo;
    private final ObjectStorageService objects;
    private final ShareLinkRepository shares;
    private final HashingService hashing;
    private final NodeRingService nodes;
    private final EventService events;
    private final StringRedisTemplate redis;

    public FileService(
            FileRepository repo,
            ObjectStorageService objects,
            ShareLinkRepository shares,
            HashingService hashing,
            NodeRingService nodes,
            EventService events,
            StringRedisTemplate redis) {
        this.repo = repo;
        this.objects = objects;
        this.shares = shares;
        this.hashing = hashing;
        this.nodes = nodes;
        this.events = events;
        this.redis = redis;
    }

    public FileEntry ingest(String owner, String name, String logicalPath, Path temp) throws IOException {
        String normalizedPath = normalizePath(logicalPath, name);
        String lockKey = "version-lock:" + hashing.sha256Text(owner + "|" + normalizedPath);
        String lockToken = acquireLock(lockKey);
        try {
            String hash = hashing.sha256(temp);
            String key = "content/" + hash;
            if (!objects.exists(key)) {
                objects.put(key, temp);
            }
            int version = repo.findFirstByOwnerIdAndLogicalPathOrderByVersionNumberDesc(owner, normalizedPath)
                .map(f -> f.getVersionNumber() + 1)
                .orElse(1);
            FileEntry file = new FileEntry();
            file.setOwnerId(owner);
            file.setFileName(name);
            file.setLogicalPath(normalizedPath);
            file.setContentHash(hash);
            file.setObjectKey(key);
            file.setSizeBytes(Files.size(temp));
            file.setVersionNumber(version);
            FileEntry saved = repo.saveAndFlush(file);
            events.publish(
                "FILE_STORED",
                saved.getId() + " hash=" + hash + " replicas=" +
                    nodes.replicas(hash).stream().map(NodeRingService.Node::id).toList());
            return saved;
        } finally {
            redis.execute(RELEASE_LOCK, List.of(lockKey), lockToken);
        }
    }

    public List<FileEntry> list(String owner) {
        return repo.findByOwnerIdOrderByCreatedAtDesc(owner);
    }

    @Transactional
    public void deleteOwned(String id, String owner) {
        FileEntry file = getOwned(id, owner);

        String objectKey = file.getObjectKey();
        String contentHash = file.getContentHash();

        shares.deleteByFileId(file.getId());
        repo.delete(file);
        repo.flush();

        boolean removeObject = repo.countByContentHash(contentHash) == 0;

        events.publish(
            "FILE_DELETED",
            file.getId() + " hash=" + contentHash + " objectRemoved=" + removeObject);

        if (removeObject) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            objects.delete(objectKey);
                        } catch (RuntimeException e) {
                            log.warn("Object cleanup failed for {}", objectKey, e);
                        }
                    }
                }
            );
        }
    }

    public FileEntry getOwned(String id, String owner) {
        FileEntry file = repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "file not found"));
        if (!file.getOwnerId().equals(owner)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "file belongs to another user");
        }
        return file;
    }

    public FileEntry getAny(String id) {
        return repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "file not found"));
    }

    public void streamOwned(String id, String owner, OutputStream output) throws IOException {
        FileEntry file = getOwned(id, owner);
        objects.stream(file.getObjectKey(), output);
    }

    public void streamAny(String id, OutputStream output) throws IOException {
        FileEntry file = getAny(id);
        objects.stream(file.getObjectKey(), output);
    }

    public long logicalBytes() {
        Long value = repo.logicalBytes();
        return value == null ? 0L : value;
    }

    public long uniqueBytes() {
        Long value = repo.uniqueBytes();
        return value == null ? 0L : value;
    }

    public long fileCount() {
        return repo.count();
    }

    public long uniqueObjectCount() {
        return repo.uniqueObjectCount();
    }

    private String normalizePath(String path, String fileName) {
        String value = path == null || path.isBlank() ? "/" + fileName : path.trim();
        if (!value.startsWith("/")) value = "/" + value;
        return value.replaceAll("/{2,}", "/");
    }

    private String acquireLock(String key) {
        String token = UUID.randomUUID().toString();
        for (int attempt = 0; attempt < 80; attempt++) {
            Boolean acquired = redis.opsForValue().setIfAbsent(key, token, Duration.ofSeconds(30));
            if (Boolean.TRUE.equals(acquired)) return token;
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting for version lock", e);
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "file path is busy; retry upload");
    }
}
