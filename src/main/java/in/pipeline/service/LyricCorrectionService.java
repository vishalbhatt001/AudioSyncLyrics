package in.pipeline.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

@Service
public final class LyricCorrectionService {
    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";

    private final Environment environment;
    private final ObjectMapper mapper = new ObjectMapper();

    public LyricCorrectionService(Environment environment) {
        this.environment = environment;
    }

    public List<TimedWord> correctForDisplay(
            String openAiApiKey,
            List<TimedWord> words,
            String lyricsHint,
            String lyricsLanguage
    ) {
        String targetLanguage = normalizedLyricsLanguage(lyricsLanguage);
        if (words.isEmpty()) {
            return words;
        }

        log("correctForDisplay:start targetLanguage=" + targetLanguage + " words=" + words.size());
        try {
            List<Map<String, Object>> timestampedWords = new ArrayList<>();
            for (int index = 0; index < words.size(); index++) {
                TimedWord word = words.get(index);
                timestampedWords.add(Map.of(
                        "index", index,
                        "text", word.word() == null ? "" : word.word(),
                        "start", word.start(),
                        "end", word.end()
                ));
            }

            Map<String, Object> payload = Map.of(
                    "model", environment.getProperty("musicload.lyrics-correction-model", "gpt-4o-mini"),
                    "temperature", 0,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", strictLyricCorrectionSystemPrompt()),
                            Map.of("role", "user", "content", mapper.writeValueAsString(Map.of(
                                    "target_language", targetLanguage,
                                    "lyrics_hint", hasText(lyricsHint) ? lyricsHint.strip() : "",
                                    "rules", List.of(
                                            "Return exactly one corrected token for each input token.",
                                            "Preserve array length and index order.",
                                            "Do not add missing lyrics.",
                                            "Do not invent words not supported by audio or hint.",
                                            "For Hinglish, output Roman Hindi only. No Devanagari characters."
                                    ),
                                    "words", timestampedWords
                            )))
                    )
            );

            JsonNode response = openAiClient(openAiApiKey)
                    .post()
                    .uri(CHAT_COMPLETIONS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            String content = response.path("choices").path(0).path("message").path("content").asText();
            JsonNode correctedWords = mapper.readTree(content).path("corrected_words");
            if (!correctedWords.isArray() || correctedWords.size() != words.size()) {
                log("correctForDisplay:invalid_shape fallback original words");
                return words;
            }

            List<TimedWord> corrected = new ArrayList<>();
            for (int index = 0; index < words.size(); index++) {
                TimedWord original = words.get(index);
                String correctedText = correctedWords.get(index).path("text").asText(original.word()).strip();
                if (correctedText.isBlank()) {
                    correctedText = original.word();
                }
                if ("hinglish".equals(targetLanguage)) {
                    correctedText = stripDevanagariFallback(correctedText, original.word());
                }

                corrected.add(new TimedWord(correctedText, original.start(), original.end()));
            }

            log("correctForDisplay:done correctedWords=" + corrected.size());
            return corrected;
        } catch (Exception exception) {
            log("correctForDisplay:failed fallback original words error=" + exception.getMessage());
            return words;
        }
    }

    public String normalizedLyricsLanguage(String lyricsLanguage) {
        if (!hasText(lyricsLanguage)) {
            return "hinglish";
        }
        String normalized = lyricsLanguage.strip().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "hi", "hindi", "devanagari" -> "hindi";
            case "en", "english" -> "english";
            default -> "hinglish";
        };
    }

    private static String strictLyricCorrectionSystemPrompt() {
        return """
                You are a strict Hindi song lyric normalization engine for timestamped karaoke subtitles.
                You receive Whisper tokens with start/end timestamps and optional expected lyrics context.
                Task:
                - Correct spelling and normalize display text for the requested target_language.
                - Preserve timing slots: output exactly one token per input token.
                - Preserve index order exactly.
                - Never add new lyric words.
                - Never remove input positions.
                - If a token is uncertain, use the closest supported token from audio/context.
                - If still uncertain, keep a conservative normalized version of the original token.
                - For target_language=hinglish, output Roman Hindi/Hinglish only. No Devanagari.
                - For target_language=hindi, output Devanagari Hindi.
                - For target_language=english, translate conservatively, one token per input token.
                - Do not explain.
                - Do not include reasoning.
                - Return JSON only: {"corrected_words":[{"index":0,"text":"..."}]}.
                """;
    }

    private RestClient openAiClient(String apiKey) {
        return RestClient.builder()
                .requestFactory(requestFactory())
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    private JdkClientHttpRequestFactory requestFactory() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
        return new JdkClientHttpRequestFactory(client);
    }

    private static String stripDevanagariFallback(String correctedText, String originalText) {
        if (!containsDevanagari(correctedText)) {
            return correctedText;
        }
        if (originalText != null && !containsDevanagari(originalText)) {
            return originalText.strip();
        }
        return correctedText.replaceAll("\\p{InDevanagari}+", "").strip();
    }

    private static boolean containsDevanagari(String value) {
        return value != null && value.matches(".*\\p{InDevanagari}.*");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void log(String message) {
        System.out.println("[MusicLoad][LyricCorrection] " + message);
    }
}
