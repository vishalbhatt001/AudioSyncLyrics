package in.pipeline.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.concurrent.Executors;

@Service
public final class ShortsFunnelService {
    private static final String WHISPER_URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final int SHORTS_WIDTH = 1080;
    private static final int SHORTS_HEIGHT = 1920;
    private static final int HOOK_SECONDS = 59;
    private static final int CTA_START_SECONDS = 54;
    private static final int CTA_DURATION_SECONDS = 5;
    private static final int LYRIC_X = SHORTS_WIDTH / 2;
    private static final int LYRIC_Y = 672;
    private static final int INTRO_Y = 430;
    private static final int CTA_Y = 1152;
    private static final int MAX_LINE_WORDS = 7;
    private static final double MAX_LINE_SECONDS = 3.8;
    private static final double LINE_BREAK_PAUSE_SECONDS = 0.45;
    private static final String INTRO_TEXT = "Agar maa yaad aati hai, ruk jao...";
    private static final String CTA_TEXT = "Pura gaana sunne ke liye 👇 Related Video pe click karein";

    private final Environment environment;
    private final LyricCorrectionService lyricCorrectionService;

    public ShortsFunnelService(Environment environment, LyricCorrectionService lyricCorrectionService) {
        this.environment = environment;
        this.lyricCorrectionService = lyricCorrectionService;
    }

    public RenderResult renderUploaded(File hookMp3, File backgroundImage, Path jobDir, String lyricsHint, String lyricsLanguage) throws Exception {
        log("renderUploaded:start mp3=" + hookMp3 + " image=" + backgroundImage + " jobDir=" + jobDir);
        validateInputFiles(hookMp3, backgroundImage);
        Files.createDirectories(jobDir);
        RenderResult result = render(hookMp3, backgroundImage, jobDir, lyricsHint, lyricsLanguage);
        log("renderUploaded:done output=" + result.outputPath());
        return result;
    }

    public RenderResult renderLocal(String mp3Path, String imagePath, Path outputDir, String lyricsHint, String lyricsLanguage) throws Exception {
        log("renderLocal:start mp3=" + mp3Path + " image=" + imagePath + " outputDir=" + outputDir);
        File hookMp3 = new File(mp3Path);
        File backgroundImage = new File(imagePath);
        validateInputFiles(hookMp3, backgroundImage);
        Files.createDirectories(outputDir);
        RenderResult result = render(hookMp3, backgroundImage, outputDir, lyricsHint, lyricsLanguage);
        log("renderLocal:done output=" + result.outputPath());
        return result;
    }

    private RenderResult render(File hookMp3, File backgroundImage, Path outputDir, String lyricsHint, String lyricsLanguage) throws Exception {
        logProgress(0, "render:start");
        Path whisperInputPath = outputDir.resolve("whisper-input.mp3").toAbsolutePath().normalize();
        Path overlayDir = outputDir.resolve("overlays").toAbsolutePath().normalize();
        Path outputPath = outputDir.resolve("shorts-output.mp4").toAbsolutePath().normalize();

        compressAudioForWhisper(hookMp3.toPath(), whisperInputPath);
        logProgress(15, "audio compressed for Whisper");
        String openAiApiKey = requiredProperty("musicload.openai-api-key");
        WhisperTranscript transcript = transcribeHook(openAiApiKey, whisperInputPath.toFile(), lyricsHint);
        logProgress(42, "Whisper transcript received words=" + transcript.words().size());
        List<TimedWord> displayWords = lyricCorrectionService.correctForDisplay(openAiApiKey, transcript.words(), lyricsHint, lyricsLanguage);
        logProgress(50, "lyrics normalized for display language=" + lyricCorrectionService.normalizedLyricsLanguage(lyricsLanguage));

        List<OverlayCue> overlayCues = writeOverlayImages(displayWords, overlayDir);
        logProgress(60, "lyric overlays generated count=" + overlayCues.size());
        runFfmpeg(hookMp3.toPath(), backgroundImage.toPath(), overlayCues, outputPath);

        logProgress(100, "render:done output=" + outputPath);
        return new RenderResult(outputPath, overlayDir, whisperInputPath, displayWords.size(), HOOK_SECONDS);
    }

    private WhisperTranscript transcribeHook(String openAiApiKey, File hookMp3, String lyricsHint) {
        log("transcribeHook:start file=" + hookMp3 + " bytes=" + hookMp3.length() + " hasLyricsHint=" + hasText(lyricsHint));
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("file", new FileSystemResource(hookMp3));
        multipart.add("model", "whisper-1");
        multipart.add("language", environment.getProperty("musicload.whisper-language", "hi"));
        multipart.add("response_format", "verbose_json");
        multipart.add("timestamp_granularities[]", "word");
        multipart.add("temperature", "0");
        if (hasText(lyricsHint)) {
            multipart.add("prompt", lyricsHint.strip());
        }

        return openAiClient(openAiApiKey)
                .post()
                .uri(WHISPER_URL)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipart)
                .retrieve()
                .body(WhisperTranscript.class);
    }

    private List<OverlayCue> writeOverlayImages(List<TimedWord> words, Path overlayDir) throws IOException {
        log("writeOverlayImages:start overlayDir=" + overlayDir + " inputWords=" + words.size());
        Files.createDirectories(overlayDir);
        List<OverlayCue> cues = new ArrayList<>();
        int index = 0;

        Path cinematicOverlayPath = overlayDir.resolve("cinematic-vignette-glow.png").toAbsolutePath().normalize();
        writeCinematicOverlay(cinematicOverlayPath);
        cues.add(new OverlayCue(cinematicOverlayPath, 0, HOOK_SECONDS, 0, 0));

        Path introPath = overlayDir.resolve("intro-hook.png").toAbsolutePath().normalize();
        OverlayImage introOverlay = writeTextOverlay(
                introPath,
                INTRO_TEXT,
                INTRO_Y,
                70,
                7,
                900,
                new Color(255, 224, 58)
        );
        cues.add(new OverlayCue(introPath, 0, 2.4, introOverlay.x(), introOverlay.y()));

        List<LyricLine> lyricLines = buildLyricLines(words);
        log("writeOverlayImages:lyricLines=" + lyricLines.size());
        for (LyricLine lyricLine : lyricLines) {
            List<TimedWord> lineWords = lyricLine.words();
            for (int activeWordIndex = 0; activeWordIndex < lineWords.size(); activeWordIndex++) {
                TimedWord activeWord = lineWords.get(activeWordIndex);
                double start = clamp(activeWord.start(), 0, HOOK_SECONDS);
                double end = activeWordIndex + 1 < lineWords.size()
                        ? clamp(lineWords.get(activeWordIndex + 1).start(), start + 0.05, HOOK_SECONDS)
                        : clamp(activeWord.end(), start + 0.15, HOOK_SECONDS);

                Path imagePath = overlayDir.resolve("line-%04d-word-%02d.png".formatted(index++, activeWordIndex)).toAbsolutePath().normalize();
                OverlayImage overlay = writeHighlightedLyricOverlay(imagePath, lineWords, activeWordIndex, LYRIC_Y, 82, 8, 930);
                cues.add(new OverlayCue(imagePath, start, end, overlay.x(), overlay.y()));
            }
        }

        Path ctaPath = overlayDir.resolve("cta.png").toAbsolutePath().normalize();
        OverlayImage ctaOverlay = writeTextOverlay(ctaPath, CTA_TEXT, CTA_Y, 64, 7, 930, new Color(255, 255, 255));
        cues.add(new OverlayCue(ctaPath, CTA_START_SECONDS, CTA_START_SECONDS + CTA_DURATION_SECONDS, ctaOverlay.x(), ctaOverlay.y()));

        log("writeOverlayImages:done cues=" + cues.size());
        return cues;
    }

    private static List<LyricLine> buildLyricLines(List<TimedWord> words) {
        List<LyricLine> lines = new ArrayList<>();
        List<TimedWord> current = new ArrayList<>();

        for (TimedWord rawWord : words) {
            String text = rawWord.word() == null ? "" : rawWord.word().trim();
            if (text.isBlank()) {
                continue;
            }

            TimedWord word = new TimedWord(text, rawWord.start(), rawWord.end());
            if (!current.isEmpty() && shouldBreakLine(current, word)) {
                lines.add(new LyricLine(List.copyOf(current)));
                current.clear();
            }
            current.add(word);
        }

        if (!current.isEmpty()) {
            lines.add(new LyricLine(List.copyOf(current)));
        }

        return lines;
    }

    private static boolean shouldBreakLine(List<TimedWord> current, TimedWord nextWord) {
        TimedWord first = current.getFirst();
        TimedWord previous = current.getLast();
        boolean tooManyWords = current.size() >= MAX_LINE_WORDS;
        boolean tooLong = nextWord.end() - first.start() > MAX_LINE_SECONDS;
        boolean pauseBreak = nextWord.start() - previous.end() > LINE_BREAK_PAUSE_SECONDS;
        return tooManyWords || tooLong || pauseBreak;
    }

    private static void writeCinematicOverlay(Path imagePath) throws IOException {
        BufferedImage image = new BufferedImage(SHORTS_WIDTH, SHORTS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            RadialGradientPaint warmGlow = new RadialGradientPaint(
                    SHORTS_WIDTH / 2f,
                    SHORTS_HEIGHT * 0.42f,
                    SHORTS_WIDTH * 0.62f,
                    new float[]{0f, 0.45f, 1f},
                    new Color[]{
                            new Color(255, 206, 84, 72),
                            new Color(255, 126, 42, 26),
                            new Color(0, 0, 0, 0)
                    }
            );
            graphics.setPaint(warmGlow);
            graphics.fillRect(0, 0, SHORTS_WIDTH, SHORTS_HEIGHT);

            RadialGradientPaint vignette = new RadialGradientPaint(
                    SHORTS_WIDTH / 2f,
                    SHORTS_HEIGHT / 2f,
                    SHORTS_HEIGHT * 0.72f,
                    new float[]{0f, 0.72f, 1f},
                    new Color[]{
                            new Color(0, 0, 0, 0),
                            new Color(0, 0, 0, 30),
                            new Color(0, 0, 0, 155)
                    }
            );
            graphics.setPaint(vignette);
            graphics.fillRect(0, 0, SHORTS_WIDTH, SHORTS_HEIGHT);
        } finally {
            graphics.dispose();
        }

        ImageIO.write(image, "png", imagePath.toFile());
    }

    private static OverlayImage writeTextOverlay(
            Path imagePath,
            String text,
            int centerY,
            int fontSize,
            int outlineWidth,
            int maxWidth,
            Color fillColor
    ) throws IOException {
        BufferedImage measurementImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measurementGraphics = measurementImage.createGraphics();
        Font font = new Font("SansSerif", Font.BOLD, fontSize);
        List<String> lines;
        FontMetrics metrics;
        int lineHeight;
        int textWidth = 1;
        try {
            measurementGraphics.setFont(font);
            metrics = measurementGraphics.getFontMetrics();
            lines = wrapText(text, metrics, maxWidth);
            lineHeight = metrics.getHeight();
            for (String line : lines) {
                textWidth = Math.max(textWidth, metrics.stringWidth(line));
            }
        } finally {
            measurementGraphics.dispose();
        }

        int padding = outlineWidth * 8;
        int imageWidth = Math.min(SHORTS_WIDTH, textWidth + padding * 2);
        int imageHeight = Math.min(SHORTS_HEIGHT, lineHeight * lines.size() + padding * 2);
        int targetX = LYRIC_X - imageWidth / 2;
        int targetY = centerY - imageHeight / 2;

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setFont(font);
            metrics = graphics.getFontMetrics();
            int baseline = padding + metrics.getAscent();

            for (String line : lines) {
                GlyphVector glyphVector = font.createGlyphVector(graphics.getFontRenderContext(), line);
                int lineWidth = metrics.stringWidth(line);
                int x = imageWidth / 2 - lineWidth / 2;
                var shape = glyphVector.getOutline(x, baseline);

                drawNeonText(graphics, shape, fillColor, new Color(0, 220, 255), outlineWidth);

                baseline += lineHeight;
            }
        } finally {
            graphics.dispose();
        }

        ImageIO.write(image, "png", imagePath.toFile());
        return new OverlayImage(targetX, targetY);
    }

    private static OverlayImage writeHighlightedLyricOverlay(
            Path imagePath,
            List<TimedWord> lineWords,
            int activeWordIndex,
            int centerY,
            int fontSize,
            int outlineWidth,
            int maxWidth
    ) throws IOException {
        BufferedImage measurementImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measurementGraphics = measurementImage.createGraphics();
        Font font = new Font("SansSerif", Font.BOLD, fontSize);
        FontMetrics metrics;
        List<List<Integer>> visualLines;
        int lineHeight;
        int widestLine = 1;

        try {
            measurementGraphics.setFont(font);
            metrics = measurementGraphics.getFontMetrics();
            visualLines = wrapWordIndexes(lineWords, metrics, maxWidth);
            lineHeight = metrics.getHeight();
            for (List<Integer> visualLine : visualLines) {
                widestLine = Math.max(widestLine, visualLineWidth(lineWords, visualLine, metrics));
            }
        } finally {
            measurementGraphics.dispose();
        }

        int padding = outlineWidth * 8;
        int imageWidth = Math.min(SHORTS_WIDTH, widestLine + padding * 2);
        int imageHeight = Math.min(SHORTS_HEIGHT, lineHeight * visualLines.size() + padding * 2);
        int targetX = LYRIC_X - imageWidth / 2;
        int targetY = centerY - imageHeight / 2;

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setFont(font);
            metrics = graphics.getFontMetrics();

            int baseline = padding + metrics.getAscent();
            for (List<Integer> visualLine : visualLines) {
                int lineWidth = visualLineWidth(lineWords, visualLine, metrics);
                int x = imageWidth / 2 - lineWidth / 2;

                for (int tokenIndex = 0; tokenIndex < visualLine.size(); tokenIndex++) {
                    int wordIndex = visualLine.get(tokenIndex);
                    String text = lineWords.get(wordIndex).word();
                    GlyphVector glyphVector = font.createGlyphVector(graphics.getFontRenderContext(), text);
                    var shape = glyphVector.getOutline(x, baseline);

                    boolean active = wordIndex == activeWordIndex;
                    Color fill = active ? new Color(255, 244, 138) : Color.WHITE;
                    Color glow = active ? new Color(255, 56, 182) : new Color(0, 220, 255);
                    drawNeonText(graphics, shape, fill, glow, outlineWidth);

                    x += metrics.stringWidth(text);
                    if (tokenIndex < visualLine.size() - 1) {
                        x += metrics.stringWidth(" ");
                    }
                }

                baseline += lineHeight;
            }
        } finally {
            graphics.dispose();
        }

        ImageIO.write(image, "png", imagePath.toFile());
        return new OverlayImage(targetX, targetY);
    }

    private static void drawNeonText(Graphics2D graphics, java.awt.Shape shape, Color fillColor, Color glowColor, int outlineWidth) {
        graphics.setStroke(new BasicStroke(outlineWidth * 6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(withAlpha(glowColor, 42));
        graphics.draw(shape);

        graphics.setStroke(new BasicStroke(outlineWidth * 3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(withAlpha(glowColor, 78));
        graphics.draw(shape);

        graphics.setStroke(new BasicStroke(outlineWidth * 1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(withAlpha(glowColor, 135));
        graphics.draw(shape);

        graphics.setStroke(new BasicStroke(outlineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(Color.BLACK);
        graphics.draw(shape);

        graphics.setStroke(new BasicStroke(Math.max(2f, outlineWidth * 0.35f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(withAlpha(Color.WHITE, 180));
        graphics.draw(shape);

        graphics.setColor(fillColor);
        graphics.fill(shape);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static List<List<Integer>> wrapWordIndexes(List<TimedWord> words, FontMetrics metrics, int maxWidth) {
        List<List<Integer>> lines = new ArrayList<>();
        List<Integer> current = new ArrayList<>();

        for (int index = 0; index < words.size(); index++) {
            List<Integer> candidate = new ArrayList<>(current);
            candidate.add(index);

            if (!current.isEmpty() && visualLineWidth(words, candidate, metrics) > maxWidth) {
                lines.add(List.copyOf(current));
                current = new ArrayList<>();
            }
            current.add(index);
        }

        if (!current.isEmpty()) {
            lines.add(List.copyOf(current));
        }
        return lines;
    }

    private static int visualLineWidth(List<TimedWord> words, List<Integer> indexes, FontMetrics metrics) {
        int width = 0;
        for (int tokenIndex = 0; tokenIndex < indexes.size(); tokenIndex++) {
            width += metrics.stringWidth(words.get(indexes.get(tokenIndex)).word());
            if (tokenIndex < indexes.size() - 1) {
                width += metrics.stringWidth(" ");
            }
        }
        return width;
    }

    private static List<String> wrapText(String text, FontMetrics metrics, int maxWidth) {
        String[] words = text.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && metrics.stringWidth(candidate) > maxWidth) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines.isEmpty() ? List.of(text) : lines;
    }

    private void compressAudioForWhisper(Path sourceAudio, Path targetAudio) throws Exception {
        log("compressAudioForWhisper:start source=" + sourceAudio + " target=" + targetAudio);
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(sourceAudio.toAbsolutePath().toString());
        command.add("-t");
        command.add(String.valueOf(HOOK_SECONDS));
        command.add("-vn");
        command.add("-ac");
        command.add("1");
        command.add("-ar");
        command.add("16000");
        command.add("-b:a");
        command.add("64k");
        command.add("-progress");
        command.add("pipe:1");
        command.add("-nostats");
        command.add(targetAudio.toAbsolutePath().toString());

        runProcess(command, "FFmpeg Whisper audio compression", HOOK_SECONDS);
        log("compressAudioForWhisper:done target=" + targetAudio + " bytes=" + Files.size(targetAudio));
    }

    private void runFfmpeg(Path hookMp3, Path backgroundImage, List<OverlayCue> overlayCues, Path outputPath) throws Exception {
        log("runFfmpeg:start mp3=" + hookMp3 + " image=" + backgroundImage + " overlays=" + overlayCues.size());
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-loop");
        command.add("1");
        command.add("-framerate");
        command.add("30");
        command.add("-i");
        command.add(backgroundImage.toAbsolutePath().toString());
        command.add("-i");
        command.add(hookMp3.toAbsolutePath().toString());
        for (OverlayCue cue : overlayCues) {
            command.add("-loop");
            command.add("1");
            command.add("-framerate");
            command.add("30");
            command.add("-i");
            command.add(cue.imagePath().toAbsolutePath().toString());
        }
        command.add("-t");
        command.add(String.valueOf(HOOK_SECONDS));
        command.add("-filter_complex");
        command.add(buildOverlayFilter(overlayCues));
        command.add("-map");
        command.add("[video_out]");
        command.add("-map");
        command.add("1:a");
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("ultrafast");
        command.add("-crf");
        command.add("28");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("192k");
        command.add("-progress");
        command.add("pipe:1");
        command.add("-nostats");
        command.add("-shortest");
        command.add(outputPath.toAbsolutePath().toString());

        runProcess(command, "FFmpeg local render", HOOK_SECONDS);
        log("runFfmpeg:done output=" + outputPath);
    }

    private static String buildOverlayFilter(List<OverlayCue> overlayCues) {
        log("buildOverlayFilter:start overlays=" + overlayCues.size());
        StringBuilder filter = new StringBuilder();
        filter.append("[0:v]scale=1188:2112:force_original_aspect_ratio=increase,");
        filter.append("crop=1080:1920:x='54+54*sin(t*0.08)':y='96+96*cos(t*0.06)'[base]");

        String previous = "base";
        for (int index = 0; index < overlayCues.size(); index++) {
            OverlayCue cue = overlayCues.get(index);
            String output = index == overlayCues.size() - 1 ? "video_out" : "v" + index;
            int inputIndex = index + 2;

            filter.append(";[")
                    .append(previous)
                    .append("][")
                    .append(inputIndex)
                    .append(":v]overlay=")
                    .append(cue.x())
                    .append(':')
                    .append(cue.y())
                    .append(":enable='between(t,")
                    .append(formatSeconds(cue.start()))
                    .append(',')
                    .append(formatSeconds(cue.end()))
                    .append(")'[")
                    .append(output)
                    .append(']');
            previous = output;
        }

        log("buildOverlayFilter:done length=" + filter.length());
        return filter.toString();
    }

    private static void runProcess(List<String> command, String label, int durationSeconds) throws Exception {
        log(label + ":start");
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        StringBuilder output = new StringBuilder();
        int lastPercent = -1;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
                if (line.startsWith("out_time_ms=")) {
                    String rawOutTime = line.substring("out_time_ms=".length());
                    if ("N/A".equals(rawOutTime)) {
                        continue;
                    }

                    long outTimeMicros = Long.parseLong(rawOutTime);
                    int percent = (int) Math.min(100, Math.round(outTimeMicros / 1000.0 / durationSeconds));
                    if (percent >= lastPercent + 10 || percent == 100) {
                        lastPercent = percent;
                        logProgress(percent, label);
                    }
                } else if ("progress=end".equals(line)) {
                    logProgress(100, label + " complete");
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException(label + " failed with exit code " + exitCode + "\n" + output);
        }
        log(label + ":done");
    }

    private RestClient openAiClient(String apiKey) {
        log("openAiClient:create");
        return RestClient.builder()
                .requestFactory(requestFactory())
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    private JdkClientHttpRequestFactory requestFactory() {
        log("requestFactory:create");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
        return new JdkClientHttpRequestFactory(client);
    }

    private String requiredProperty(String key) {
        log("requiredProperty:lookup key=" + key);
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing config property: " + key);
        }
        return value;
    }

    private static void validateInputFiles(File hookMp3, File backgroundImage) {
        log("validateInputFiles:start");
        if (!hookMp3.isFile()) {
            throw new IllegalArgumentException("Missing 59-second hook MP3: " + hookMp3);
        }
        if (!backgroundImage.isFile()) {
            throw new IllegalArgumentException("Missing background image: " + backgroundImage);
        }
        log("validateInputFiles:done mp3Bytes=" + hookMp3.length() + " imageBytes=" + backgroundImage.length());
    }

    private static String formatSeconds(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void log(String message) {
        System.out.println("[MusicLoad] " + message);
    }

    private static void logProgress(int percent, String message) {
        System.out.printf("[MusicLoad][progress=%3d%%] %s%n", percent, message);
    }

    public record RenderResult(Path outputPath, Path overlayDirPath, Path whisperInputPath, int wordCount, int durationSeconds) {
    }

    private record OverlayCue(Path imagePath, double start, double end, int x, int y) {
    }

    private record OverlayImage(int x, int y) {
    }

    private record LyricLine(List<TimedWord> words) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WhisperTranscript(List<TimedWord> words) {
        private WhisperTranscript {
            words = words == null ? List.of() : List.copyOf(words);
        }
    }
}
