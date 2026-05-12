package in.pipeline.controller;

import in.pipeline.controller.request.ShortsRenderRequest;
import in.pipeline.service.ShortsFunnelService;
import in.pipeline.service.ShortsFunnelService.RenderResult;
import in.pipeline.service.RenderTextOptions;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin
@RequestMapping("/api/shorts")
public final class ShortsRenderController {
    private final ShortsFunnelService renderer;
    private final Environment environment;
    private final Path uploadRoot;

    public ShortsRenderController(ShortsFunnelService renderer, Environment environment) {
        this.renderer = renderer;
        this.environment = environment;
        this.uploadRoot = Path.of(environment.getProperty("musicload.upload-dir", "uploads")).toAbsolutePath().normalize();
    }

    @Operation(summary = "Upload 59s MP3 hook and background image, then render a local 1080x1920 MP4")
    @PostMapping(
            value = "/renders",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<Map<String, Object>> createRender(
            @ModelAttribute ShortsRenderRequest request
    ) throws Exception {
        String jobId = UUID.randomUUID().toString();
        Path jobDir = uploadRoot.resolve(jobId).normalize();
        Files.createDirectories(jobDir);

        Path mp3Path = saveUpload(request.getMp3(), jobDir, "hook.mp3");
        Path imagePath = saveUpload(request.getImage(), jobDir, "background.jpg");

        String normalizedBaseUrl = stripTrailingSlash(currentBaseUrl());
        String mp3Url = normalizedBaseUrl + "/assets/" + jobId + "/" + urlSegment(mp3Path.getFileName().toString());
        String imageUrl = normalizedBaseUrl + "/assets/" + jobId + "/" + urlSegment(imagePath.getFileName().toString());
        String outputUrl = normalizedBaseUrl + "/assets/" + jobId + "/shorts-output.mp4";

        RenderResult renderResult = renderer.renderUploaded(
                mp3Path.toFile(),
                imagePath.toFile(),
                jobDir,
                request.getLyricsHint(),
                request.normalizedLyricsLanguage(),
                new RenderTextOptions(request.getIntroText(), request.getCtaText())
        );

        return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "mp3Url", mp3Url,
                "imageUrl", imageUrl,
                "outputUrl", outputUrl,
                "outputPath", renderResult.outputPath().toString(),
                "overlayDirPath", renderResult.overlayDirPath().toString(),
                "whisperInputPath", renderResult.whisperInputPath().toString(),
                "wordCount", renderResult.wordCount(),
                "durationSeconds", renderResult.durationSeconds()
        ));
    }

    private static Path saveUpload(MultipartFile file, Path jobDir, String fallbackName) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty upload: " + fallbackName);
        }

        String filename = safeFilename(file.getOriginalFilename(), fallbackName);
        Path target = jobDir.resolve(filename).normalize();
        if (!target.startsWith(jobDir)) {
            throw new IllegalArgumentException("Invalid upload filename: " + filename);
        }

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static String safeFilename(String originalFilename, String fallbackName) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return fallbackName;
        }

        String filename = Path.of(originalFilename).getFileName().toString();
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String stripTrailingSlash(String value) {
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String currentBaseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }

    private static String urlSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
