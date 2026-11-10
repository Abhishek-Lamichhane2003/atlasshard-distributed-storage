package com.atlasshard.storage.service;

import com.atlasshard.storage.config.AppProperties;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
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

    public ObjectStorageService(S3Client s3, AppProperties props) {
        this.s3 = s3;
        this.props = props;
    }

    public void put(String key, Path path) {
        s3.putObject(
            PutObjectRequest.builder().bucket(props.s3().bucket()).key(key).build(),
            RequestBody.fromFile(path));
    }

    public boolean exists(String key) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(props.s3().bucket()).key(key).build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw e;
        }
    }

    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder()
            .bucket(props.s3().bucket())
            .key(key)
            .build());
    }

    public void stream(String key, OutputStream output) throws IOException {
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(props.s3().bucket())
            .key(key)
            .build();
        try (ResponseInputStream<GetObjectResponse> input = s3.getObject(request)) {
            input.transferTo(output);
        }
    }
}
