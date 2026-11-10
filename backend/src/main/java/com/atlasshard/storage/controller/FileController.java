package com.atlasshard.storage.controller;

import com.atlasshard.storage.model.FileEntry;
import com.atlasshard.storage.service.FileService;
import com.atlasshard.storage.service.UploadSessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api")
public class FileController {
    record UploadRequest(
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank @Size(max = 1024) String logicalPath,
        @Min(1) @Max(10000) int totalChunks) {}

    private final FileService files;
    private final UploadSessionService uploads;

    public FileController(FileService files, UploadSessionService uploads) {
        this.files = files;
        this.uploads = uploads;
    }

    @GetMapping("/files")
    public List<FileEntry> list(Authentication authentication) {
        return files.list(authentication.getName());
    }

    @DeleteMapping("/files/{id}")
    public void delete(@PathVariable String id, Authentication authentication) {
        files.deleteOwned(id, authentication.getName());
    }

    @GetMapping("/files/{id}/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable String id, Authentication authentication) {
        FileEntry file = files.getOwned(id, authentication.getName());
        StreamingResponseBody body = output -> files.streamOwned(id, authentication.getName(), output);
        return ResponseEntity.ok()
            .contentLength(file.getSizeBytes())
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(file.getFileName(), StandardCharsets.UTF_8).build().toString())
            .body(body);
    }

    @PostMapping("/uploads")
    public UploadSessionService.Session create(
            @Valid @RequestBody UploadRequest request,
            Authentication authentication) {
        return uploads.create(
            authentication.getName(),
            request.fileName(),
            request.logicalPath(),
            request.totalChunks());
    }

    @GetMapping("/uploads/{id}")
    public UploadSessionService.Session session(@PathVariable String id, Authentication authentication) {
        return uploads.readOwned(id, authentication.getName());
    }

    @PutMapping("/uploads/{id}/chunks/{index}")
    public UploadSessionService.Session chunk(
            @PathVariable String id,
            @PathVariable int index,
            @RequestPart("chunk") MultipartFile chunk,
            Authentication authentication) throws IOException {
        return uploads.add(id, authentication.getName(), index, chunk);
    }

    @PostMapping("/uploads/{id}/complete")
    public FileEntry complete(@PathVariable String id, Authentication authentication) throws IOException {
        return uploads.complete(id, authentication.getName());
    }
}
