package com.atlasshard.storage.service;

import com.atlasshard.storage.config.AppProperties;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class ObjectStorageService {
    private final S3Client s3;
    private final AppProperties props;
    private final boolean local;
    private final Path localRoot;

    public ObjectStorageService(
            S3Client s3,
            AppProperties props,
            @Value("${app.storage.mode:s3}") String mode,
            @Value("${app.storage.local-root:/tmp/atlasshard-objects}") String localRoot) {
        this.s3 = s3;
        this.props = props;
        this.local = "local".equalsIgnoreCase(mode);
        this.localRoot = Path.of(localRoot);
    }

    public void put(String key, Path path) {
        if (local) {
            try {
                Path target = localPath(key);
                Files.createDirectories(target.getParent());
                Files.copy(path, target);
                return;
            } catch (IOException e) {
                throw new IllegalStateException("failed to store local object", e);
            }
        }
        s3.putObject(
            PutObjectRequest.builder().bucket(props.s3().bucket()).key(key).build(),
            RequestBody.fromFile(path));
    }

    public boolean exists(String key) {
        if (local) return Files.exists(localPath(key));
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(props.s3().bucket()).key(key).build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw e;
        }
    }

    public void delete(String key) {
        if (local) {
            try {
                Files.deleteIfExists(localPath(key));
                return;
            } catch (IOException e) {
                throw new IllegalStateException("failed to delete local object", e);
            }
        }
        s3.deleteObject(DeleteObjectRequest.builder()
            .bucket(props.s3().bucket())
            .key(key)
            .build());
    }

    public void stream(String key, OutputStream output) throws IOException {
        if (local) {
            try (var input = Files.newInputStream(localPath(key))) {
                input.transferTo(output);
            }
            return;
        }
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(props.s3().bucket())
            .key(key)
            .build();
        try (ResponseInputStream<GetObjectResponse> input = s3.getObject(request)) {
            input.transferTo(output);
        }
    }

    private Path localPath(String key) {
        Path resolved = localRoot.resolve(key).normalize();
        if (!resolved.startsWith(localRoot.normalize())) {
            throw new IllegalArgumentException("object key escapes storage root");
        }
        return resolved;
    }
}
