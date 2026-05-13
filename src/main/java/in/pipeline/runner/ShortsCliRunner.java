package in.pipeline.runner;

import in.pipeline.service.ShortsFunnelService;
import in.pipeline.service.RenderTextOptions;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;

@Component
public final class ShortsCliRunner implements ApplicationRunner {
    private final ShortsFunnelService renderer;

    public ShortsCliRunner(ShortsFunnelService renderer) {
        this.renderer = renderer;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("mp3")) {
            return;
        }

        String mp3Path = requiredArg(args, "mp3");
        List<String> mediaPaths = requiredArgs(args, "media", 4);
        Path outputDir = Path.of(optionalArg(args, "output-dir", "outputs"));
        String lyricsHint = optionalArg(args, "lyrics-hint", "");
        String lyricsLanguage = optionalArg(args, "lyrics-language", "hinglish");
        String introText = optionalArg(args, "intro-text", "");
        String ctaText = optionalArg(args, "cta-text", "");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                renderer.renderLocal(mp3Path, mediaPaths, outputDir, lyricsHint, lyricsLanguage, new RenderTextOptions(introText, ctaText));
                return null;
            }).get();
        }
    }

    private static String requiredArg(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        if (values == null || values.isEmpty() || values.getFirst().isBlank()) {
            throw new IllegalArgumentException("Missing required arg: --" + name);
        }
        return values.getFirst();
    }

    private static List<String> requiredArgs(ApplicationArguments args, String name, int expectedCount) {
        List<String> values = args.getOptionValues(name);
        if (values == null || values.size() != expectedCount || values.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Expected exactly " + expectedCount + " values for --" + name);
        }
        return List.copyOf(values);
    }

    private static String optionalArg(ApplicationArguments args, String name, String fallback) {
        List<String> values = args.getOptionValues(name);
        if (values == null || values.isEmpty() || values.getFirst().isBlank()) {
            return fallback;
        }
        return values.getFirst();
    }
}
