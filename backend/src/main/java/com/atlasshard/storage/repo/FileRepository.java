package com.atlasshard.storage.repo;

import com.atlasshard.storage.model.FileEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FileRepository extends JpaRepository<FileEntry, String> {
    List<FileEntry> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    Optional<FileEntry> findFirstByOwnerIdAndLogicalPathOrderByVersionNumberDesc(
        String ownerId,
        String logicalPath);

    long countByContentHash(String contentHash);

    @Query("select coalesce(sum(f.sizeBytes), 0) from FileEntry f")
    Long logicalBytes();

    @Query("select count(distinct f.contentHash) from FileEntry f")
    long uniqueObjectCount();

    @Query(
        value = "select cast(coalesce(sum(size_bytes), 0) as bigint) "
            + "from (select content_hash, max(size_bytes) as size_bytes "
            + "from files group by content_hash) x",
        nativeQuery = true)
    Long uniqueBytes();
}
