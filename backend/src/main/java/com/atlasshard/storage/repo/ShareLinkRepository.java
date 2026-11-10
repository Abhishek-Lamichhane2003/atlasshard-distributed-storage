package com.atlasshard.storage.repo;

import com.atlasshard.storage.model.ShareLink;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareLinkRepository extends JpaRepository<ShareLink, String> {
    Optional<ShareLink> findByTokenHash(String tokenHash);
    List<ShareLink> findByOwnerIdAndExpiresAtAfterOrderByCreatedAtDesc(String ownerId, Instant now);
    long deleteByExpiresAtBefore(Instant cutoff);
    long deleteByFileId(String fileId);
}
