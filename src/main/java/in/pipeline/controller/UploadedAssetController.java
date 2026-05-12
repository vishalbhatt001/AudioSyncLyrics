package in.pipeline.controller;

import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public final class UploadedAssetController {
    private final Path uploadRoot;

    public UploadedAssetController(Environment environment) {
        this.uploadRoot = Path.of(environment.getProperty("musicload.upload-dir", "uploads")).toAbsolutePath().normalize();
    }

    @GetMapping("/assets/{jobId}/{filename:.+}")
    ResponseEntity<Resource> getAsset(
            @PathVariable String jobId,
            @PathVariable String filename
    ) throws IOException {
        Path jobDir = uploadRoot.resolve(jobId).normalize();
        Path file = jobDir.resolve(filename).normalize();
        if (!file.startsWith(jobDir) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = toResource(file);
        String contentType = Files.probeContentType(file);
        MediaType mediaType = contentType == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(contentType);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                .body(resource);
    }

    private static Resource toResource(Path file) throws MalformedURLException {
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("Unreadable asset: " + file);
        }
        return resource;
    }
}
