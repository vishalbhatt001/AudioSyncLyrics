package in.pipeline.service;

import org.springframework.stereotype.Service;

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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public final class LyricOverlayService {
    private static final int LYRIC_X = ShortsRenderSpec.WIDTH / 2;
    private static final int LYRIC_Y = 672;
    private static final int INTRO_Y = 430;
    private static final int CTA_Y = 1152;
    private static final int MAX_LINE_WORDS = 7;
    private static final double MAX_LINE_SECONDS = 3.8;
    private static final double LINE_BREAK_PAUSE_SECONDS = 0.45;
    private static final String INTRO_TEXT = "Agar maa yaad aati hai, ruk jao...";
    private static final String CTA_TEXT = "Pura gaana sunne ke liye 👇 Related Video pe click karein";

    public List<OverlayCue> generate(List<TimedWord> words, Path overlayDir) throws IOException {
        log("generate:start overlayDir=" + overlayDir + " inputWords=" + words.size());
        Files.createDirectories(overlayDir);
        List<OverlayCue> cues = new ArrayList<>();
        int index = 0;

        Path cinematicOverlayPath = overlayDir.resolve("cinematic-vignette-glow.png").toAbsolutePath().normalize();
        writeCinematicOverlay(cinematicOverlayPath);
        cues.add(new OverlayCue(cinematicOverlayPath, 0, ShortsRenderSpec.HOOK_SECONDS, 0, 0));

        Path introPath = overlayDir.resolve("intro-hook.png").toAbsolutePath().normalize();
        OverlayImage introOverlay = writeTextOverlay(introPath, INTRO_TEXT, INTRO_Y, 70, 7, 900, new Color(255, 224, 58));
        cues.add(new OverlayCue(introPath, 0, 2.4, introOverlay.x(), introOverlay.y()));

        List<LyricLine> lyricLines = buildLyricLines(words);
        log("generate:lyricLines=" + lyricLines.size());
        for (LyricLine lyricLine : lyricLines) {
            List<TimedWord> lineWords = lyricLine.words();
            for (int activeWordIndex = 0; activeWordIndex < lineWords.size(); activeWordIndex++) {
                TimedWord activeWord = lineWords.get(activeWordIndex);
                double start = clamp(activeWord.start(), 0, ShortsRenderSpec.HOOK_SECONDS);
                double end = activeWordIndex + 1 < lineWords.size()
                        ? clamp(lineWords.get(activeWordIndex + 1).start(), start + 0.05, ShortsRenderSpec.HOOK_SECONDS)
                        : clamp(activeWord.end(), start + 0.15, ShortsRenderSpec.HOOK_SECONDS);

                Path imagePath = overlayDir.resolve("line-%04d-word-%02d.png".formatted(index++, activeWordIndex)).toAbsolutePath().normalize();
                OverlayImage overlay = writeHighlightedLyricOverlay(imagePath, lineWords, activeWordIndex, LYRIC_Y, 82, 8, 930);
                cues.add(new OverlayCue(imagePath, start, end, overlay.x(), overlay.y()));
            }
        }

        Path ctaPath = overlayDir.resolve("cta.png").toAbsolutePath().normalize();
        OverlayImage ctaOverlay = writeTextOverlay(ctaPath, CTA_TEXT, CTA_Y, 64, 7, 930, Color.WHITE);
        cues.add(new OverlayCue(ctaPath, ShortsRenderSpec.CTA_START_SECONDS,
                ShortsRenderSpec.CTA_START_SECONDS + ShortsRenderSpec.CTA_DURATION_SECONDS, ctaOverlay.x(), ctaOverlay.y()));

        log("generate:done cues=" + cues.size());
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
        return current.size() >= MAX_LINE_WORDS
                || nextWord.end() - first.start() > MAX_LINE_SECONDS
                || nextWord.start() - previous.end() > LINE_BREAK_PAUSE_SECONDS;
    }

    private static void writeCinematicOverlay(Path imagePath) throws IOException {
        BufferedImage image = new BufferedImage(ShortsRenderSpec.WIDTH, ShortsRenderSpec.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            RadialGradientPaint warmGlow = new RadialGradientPaint(
                    ShortsRenderSpec.WIDTH / 2f,
                    ShortsRenderSpec.HEIGHT * 0.42f,
                    ShortsRenderSpec.WIDTH * 0.62f,
                    new float[]{0f, 0.45f, 1f},
                    new Color[]{new Color(255, 206, 84, 72), new Color(255, 126, 42, 26), new Color(0, 0, 0, 0)}
            );
            graphics.setPaint(warmGlow);
            graphics.fillRect(0, 0, ShortsRenderSpec.WIDTH, ShortsRenderSpec.HEIGHT);

            RadialGradientPaint vignette = new RadialGradientPaint(
                    ShortsRenderSpec.WIDTH / 2f,
                    ShortsRenderSpec.HEIGHT / 2f,
                    ShortsRenderSpec.HEIGHT * 0.72f,
                    new float[]{0f, 0.72f, 1f},
                    new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 30), new Color(0, 0, 0, 155)}
            );
            graphics.setPaint(vignette);
            graphics.fillRect(0, 0, ShortsRenderSpec.WIDTH, ShortsRenderSpec.HEIGHT);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", imagePath.toFile());
    }

    private static OverlayImage writeTextOverlay(Path imagePath, String text, int centerY, int fontSize,
                                                 int outlineWidth, int maxWidth, Color fillColor) throws IOException {
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
        int imageWidth = Math.min(ShortsRenderSpec.WIDTH, textWidth + padding * 2);
        int imageHeight = Math.min(ShortsRenderSpec.HEIGHT, lineHeight * lines.size() + padding * 2);
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
                int x = imageWidth / 2 - metrics.stringWidth(line) / 2;
                drawNeonText(graphics, glyphVector.getOutline(x, baseline), fillColor, new Color(0, 220, 255), outlineWidth);
                baseline += lineHeight;
            }
        } finally {
            graphics.dispose();
        }

        ImageIO.write(image, "png", imagePath.toFile());
        return new OverlayImage(targetX, targetY);
    }

    private static OverlayImage writeHighlightedLyricOverlay(Path imagePath, List<TimedWord> lineWords,
                                                            int activeWordIndex, int centerY, int fontSize,
                                                            int outlineWidth, int maxWidth) throws IOException {
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
        int imageWidth = Math.min(ShortsRenderSpec.WIDTH, widestLine + padding * 2);
        int imageHeight = Math.min(ShortsRenderSpec.HEIGHT, lineHeight * visualLines.size() + padding * 2);
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
                int x = imageWidth / 2 - visualLineWidth(lineWords, visualLine, metrics) / 2;
                for (int tokenIndex = 0; tokenIndex < visualLine.size(); tokenIndex++) {
                    int wordIndex = visualLine.get(tokenIndex);
                    String text = lineWords.get(wordIndex).word();
                    GlyphVector glyphVector = font.createGlyphVector(graphics.getFontRenderContext(), text);
                    boolean active = wordIndex == activeWordIndex;
                    Color fill = active ? new Color(255, 244, 138) : Color.WHITE;
                    Color glow = active ? new Color(255, 56, 182) : new Color(0, 220, 255);
                    drawNeonText(graphics, glyphVector.getOutline(x, baseline), fill, glow, outlineWidth);

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

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void log(String message) {
        System.out.println("[MusicLoad][Overlay] " + message);
    }

    private record OverlayImage(int x, int y) {
    }

    private record LyricLine(List<TimedWord> words) {
    }
}
