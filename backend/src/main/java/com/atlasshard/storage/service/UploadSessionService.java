package com.atlasshard.storage.service;

import com.atlasshard.storage.config.AppProperties;
import com.atlasshard.storage.model.FileEntry;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadSessionService {
    public record Session(
            String id,
            String owner,
            String fileName,
            String logicalPath,
            int totalChunks,
            Set<Integer> received) {}

    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private final FileService files;
    private final Path base;
    private final StringRedisTemplate redis;

    public UploadSessionService(FileService files, AppProperties props, StringRedisTemplate redis) throws IOException {
        this.files = files;
        this.redis = redis;
        this.base = Path.of(props.uploadTemp());
        Files.createDirectories(base);
    }

    public Session create(String owner, String fileName, String logicalPath, int totalChunks) {
        String id = UUID.randomUUID().toString();
        String meta = metaKey(id);
        redis.opsForHash().put(meta, "owner", owner);
        redis.opsForHash().put(meta, "fileName", fileName);
        redis.opsForHash().put(meta, "logicalPath", logicalPath);
        redis.opsForHash().put(meta, "totalChunks", Integer.toString(totalChunks));
        touch(id);
        return readOwned(id, owner);
    }

    public Session readOwned(String id, String owner) {
        Session session = read(id);
        requireOwner(session, owner);
        touch(id);
        return session;
    }

    public Session add(String id, String owner, int index, MultipartFile part) throws IOException {
        Session session = readOwned(id, owner);
        if (index < 0 || index >= session.totalChunks()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid chunk index");
        if (part.isEmpty() && !(session.totalChunks() == 1 && index == 0)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chunk is empty");
        Path dir = base.resolve(id);
        Files.createDirectories(dir);
        Path target = dir.resolve(String.format("%06d.part", index));
        part.transferTo(target);
        redis.opsForSet().add(chunksKey(id), Integer.toString(index));
        touch(id);
        return readOwned(id, owner);
    }

    public FileEntry complete(String id, String owner) throws IOException {
        Session session = readOwned(id, owner);
        String lockKey = "upload-complete:" + id;
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, owner, Duration.ofMinutes(2));
        if (!Boolean.TRUE.equals(locked)) throw new ResponseStatusException(HttpStatus.CONFLICT, "upload completion already in progress");
        Path merged = base.resolve(id + ".merged");
        try {
            session = readOwned(id, owner);
            if (session.received().size() != session.totalChunks()) throw new ResponseStatusException(HttpStatus.CONFLICT, "missing chunks");
            try (OutputStream output = Files.newOutputStream(merged)) {
                for (int i = 0; i < session.totalChunks(); i++) {
                    Path chunk = base.resolve(id).resolve(String.format("%06d.part", i));
                    if (!Files.exists(chunk)) throw new ResponseStatusException(HttpStatus.CONFLICT, "missing chunk " + i);
                    Files.copy(chunk, output);
                }
            }
            FileEntry file = files.ingest(owner, session.fileName(), session.logicalPath(), merged);
            deleteSessionFiles(id, merged);
            redis.delete(List.of(metaKey(id), chunksKey(id)));
            return file;
        } finally {
            redis.delete(lockKey);
        }
    }

    @Scheduled(fixedDelayString = "PT1H")
    public void cleanupExpiredFiles() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(25));
        try (Stream<Path> paths = Files.list(base)) {
            paths.forEach(path -> {
                try {
                    FileTime modified = Files.getLastModifiedTime(path);
                    if (modified.toInstant().isAfter(cutoff)) return;
                    String name = path.getFileName().toString();
                    String id = name.endsWith(".merged") ? name.substring(0, name.length() - 7) : name;
                    if (Boolean.TRUE.equals(redis.hasKey(metaKey(id)))) return;
                    deleteRecursively(path);
                } catch (IOException | RuntimeException ignored) {
                    // A later cleanup pass can retry.
                }
            });
        } catch (IOException ignored) {
            // Cleanup must never make the API unavailable.
        }
    }

    private Session read(String id) {
        Map<Object, Object> values = redis.opsForHash().entries(metaKey(id));
        if (values.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "upload session not found");
        Set<Integer> received = new TreeSet<>();
        Set<String> raw = redis.opsForSet().members(chunksKey(id));
        if (raw != null) raw.forEach(value -> received.add(Integer.parseInt(value)));
        return new Session(
            id,
            string(values, "owner"),
            string(values, "fileName"),
            string(values, "logicalPath"),
            Integer.parseInt(string(values, "totalChunks")),
            received);
    }

    private void requireOwner(Session session, String owner) {
        if (!session.owner().equals(owner)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "upload session belongs to another user");
    }

    private void touch(String id) {
        redis.expire(metaKey(id), SESSION_TTL);
        redis.expire(chunksKey(id), SESSION_TTL);
    }

    private String string(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "upload session is incomplete");
        return value.toString();
    }

    private String metaKey(String id) { return "upload:" + id + ":meta"; }
    private String chunksKey(String id) { return "upload:" + id + ":chunks"; }

    private void deleteSessionFiles(String id, Path merged) throws IOException {
        deleteRecursively(base.resolve(id));
        Files.deleteIfExists(merged);
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        if (Files.isDirectory(path)) {
            try (Stream<Path> stream = Files.walk(path)) {
                for (Path child : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(child);
            }
        } else {
            Files.deleteIfExists(path);
        }
    }
}
