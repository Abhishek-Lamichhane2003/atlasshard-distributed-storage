package com.atlasshard.storage.controller;

import com.atlasshard.storage.model.FileEntry;
import com.atlasshard.storage.model.ShareLink;
import com.atlasshard.storage.repo.ShareLinkRepository;
import com.atlasshard.storage.service.EventService;
import com.atlasshard.storage.service.FileService;
import com.atlasshard.storage.service.HashingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/shares")
public class ShareController {
    record ShareRequest(@NotBlank String fileId, @Min(1) @Max(168) int hours) {}
    public record ShareView(String id, String fileId, String fileName, Instant expiresAt, Instant createdAt) {}

    private final ShareLinkRepository shares;
    private final FileService files;
    private final HashingService hashing;
    private final EventService events;
    private final SecureRandom random = new SecureRandom();

    public ShareController(
            ShareLinkRepository shares,
            FileService files,
            HashingService hashing,
            EventService events) {
        this.shares = shares;
        this.files = files;
        this.hashing = hashing;
        this.events = events;
    }

    @PostMapping
    public Map<String, String> create(@Valid @RequestBody ShareRequest request, Authentication authentication) {
        FileEntry file = files.getOwned(request.fileId(), authentication.getName());
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        ShareLink share = new ShareLink();
        share.setFileId(file.getId());
        share.setOwnerId(authentication.getName());
        share.setTokenHash(hashing.sha256Text(token));
        share.setExpiresAt(Instant.now().plus(Duration.ofHours(request.hours())));
        shares.save(share);
        events.publish("SHARE_CREATED", share.getId() + " file=" + file.getId());
        return Map.of(
            "token", token,
            "path", "/api/shares/public/" + token,
            "expiresAt", share.getExpiresAt().toString());
    }

    @GetMapping
    public List<ShareView> list(Authentication authentication) {
        return shares.findByOwnerIdAndExpiresAtAfterOrderByCreatedAtDesc(authentication.getName(), Instant.now()).stream()
            .map(share -> {
                FileEntry file = files.getAny(share.getFileId());
                return new ShareView(
                    share.getId(),
                    share.getFileId(),
                    file.getFileName(),
                    share.getExpiresAt(),
                    share.getCreatedAt());
            })
            .toList();
    }

    @DeleteMapping("/{id}")
    public void revoke(@PathVariable String id, Authentication authentication) {
        ShareLink share = shares.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!share.getOwnerId().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        shares.delete(share);
        events.publish("SHARE_REVOKED", share.getId());
    }

    @GetMapping("/public/{token}")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable String token) {
        String tokenHash = hashing.sha256Text(token);
        ShareLink share = shares.findByTokenHash(tokenHash)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (share.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "share link expired");
        }
        FileEntry file = files.getAny(share.getFileId());
        StreamingResponseBody body = output -> files.streamAny(file.getId(), output);
        return ResponseEntity.ok()
            .contentLength(file.getSizeBytes())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(file.getFileName(), StandardCharsets.UTF_8).build().toString())
            .body(body);
    }
}
