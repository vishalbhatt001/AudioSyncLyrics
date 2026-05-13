package in.pipeline.service;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public final class ShortsFunnelService {
    private final Environment environment;
    private final AudioPreprocessorService audioPreprocessor;
    private final WhisperTranscriptionService whisperTranscription;
    private final LyricCorrectionService lyricCorrection;
    private final LyricOverlayService lyricOverlay;
    private final LocalVideoRenderService videoRenderer;

    public ShortsFunnelService(
            Environment environment,
            AudioPreprocessorService audioPreprocessor,
            WhisperTranscriptionService whisperTranscription,
            LyricCorrectionService lyricCorrection,
            LyricOverlayService lyricOverlay,
            LocalVideoRenderService videoRenderer
    ) {
        this.environment = environment;
        this.audioPreprocessor = audioPreprocessor;
        this.whisperTranscription = whisperTranscription;
        this.lyricCorrection = lyricCorrection;
        this.lyricOverlay = lyricOverlay;
        this.videoRenderer = videoRenderer;
    }

    public RenderResult renderUploaded(
            File hookAudio,
            List<VisualMediaAsset> mediaAssets,
            Path jobDir,
            String lyricsHint,
            String lyricsLanguage,
            RenderTextOptions textOptions
    ) throws Exception {
        log("renderUploaded:start audio=" + hookAudio + " mediaCount=" + mediaAssets.size() + " jobDir=" + jobDir);
        validateInputFiles(hookAudio, mediaAssets);
        Files.createDirectories(jobDir);
        RenderResult result = render(hookAudio, mediaAssets, jobDir, lyricsHint, lyricsLanguage, textOptions);
        log("renderUploaded:done output=" + result.outputPath());
        return result;
    }

    public RenderResult renderLocal(
            String hookAudioPath,
            List<String> mediaPaths,
            Path outputDir,
            String lyricsHint,
            String lyricsLanguage,
            RenderTextOptions textOptions
    ) throws Exception {
        log("renderLocal:start audio=" + hookAudioPath + " mediaCount=" + mediaPaths.size() + " outputDir=" + outputDir);
        File hookAudio = new File(hookAudioPath);
        List<VisualMediaAsset> mediaAssets = mediaPaths.stream()
                .map(Path::of)
                .map(VisualMediaAsset::from)
                .toList();
        validateInputFiles(hookAudio, mediaAssets);
        Files.createDirectories(outputDir);
        RenderResult result = render(hookAudio, mediaAssets, outputDir, lyricsHint, lyricsLanguage, textOptions);
        log("renderLocal:done output=" + result.outputPath());
        return result;
    }

    private RenderResult render(
            File hookAudio,
            List<VisualMediaAsset> mediaAssets,
            Path outputDir,
            String lyricsHint,
            String lyricsLanguage,
            RenderTextOptions textOptions
    ) throws Exception {
        logProgress(0, "render:start");
        RenderPaths paths = RenderPaths.in(outputDir);

        audioPreprocessor.compressForWhisper(hookAudio.toPath(), paths.whisperInputPath(), ShortsFunnelService::logProgress);
        logProgress(15, "audio compressed for Whisper");

        String openAiApiKey = requiredProperty("musicload.openai-api-key");
        List<TimedWord> transcriptWords = whisperTranscription.transcribe(openAiApiKey, paths.whisperInputPath().toFile(), lyricsHint);
        logProgress(42, "Whisper transcript received words=" + transcriptWords.size());

        List<TimedWord> displayWords = lyricCorrection.correctForDisplay(openAiApiKey, transcriptWords, lyricsHint, lyricsLanguage);
        logProgress(50, "lyrics normalized for display language=" + lyricCorrection.normalizedLyricsLanguage(lyricsLanguage));

        List<OverlayCue> overlayCues = lyricOverlay.generate(displayWords, paths.overlayDir(), textOptions);
        logProgress(60, "lyric overlays generated count=" + overlayCues.size());

        videoRenderer.render(hookAudio.toPath(), mediaAssets, overlayCues, paths.outputPath(), ShortsFunnelService::logProgress);

        logProgress(100, "render:done output=" + paths.outputPath());
        return new RenderResult(paths.outputPath(), paths.overlayDir(), paths.whisperInputPath(), displayWords.size(), ShortsRenderSpec.HOOK_SECONDS);
    }

    private String requiredProperty(String key) {
        log("requiredProperty:lookup key=" + key);
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing config property: " + key);
        }
        return value;
    }

    private static void validateInputFiles(File hookAudio, List<VisualMediaAsset> mediaAssets) {
        log("validateInputFiles:start");
        if (!hookAudio.isFile()) {
            throw new IllegalArgumentException("Missing 59-second hook audio: " + hookAudio);
        }
        MediaTimelinePlanner.validateMediaAssets(mediaAssets);
        for (VisualMediaAsset mediaAsset : mediaAssets) {
            if (!mediaAsset.path().toFile().isFile()) {
                throw new IllegalArgumentException("Missing media asset: " + mediaAsset.path());
            }
        }
        long mediaBytes = mediaAssets.stream()
                .mapToLong(mediaAsset -> mediaAsset.path().toFile().length())
                .sum();
        log("validateInputFiles:done audioBytes=" + hookAudio.length() + " mediaCount=" + mediaAssets.size() + " mediaBytes=" + mediaBytes);
    }

    private static void log(String message) {
        System.out.println("[MusicLoad][Pipeline] " + message);
    }

    private static void logProgress(int percent, String message) {
        System.out.printf("[MusicLoad][progress=%3d%%] %s%n", percent, message);
    }

    public record RenderResult(Path outputPath, Path overlayDirPath, Path whisperInputPath, int wordCount, int durationSeconds) {
    }

    private record RenderPaths(Path whisperInputPath, Path overlayDir, Path outputPath) {
        private static RenderPaths in(Path outputDir) {
            return new RenderPaths(
                    outputDir.resolve("whisper-input.mp3").toAbsolutePath().normalize(),
                    outputDir.resolve("overlays").toAbsolutePath().normalize(),
                    outputDir.resolve("shorts-output.mp4").toAbsolutePath().normalize()
            );
        }
    }
}
