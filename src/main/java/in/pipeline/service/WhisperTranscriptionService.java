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

import java.io.File;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;

@Service
public final class WhisperTranscriptionService {
    private static final String WHISPER_URL = "https://api.openai.com/v1/audio/transcriptions";

    private final Environment environment;

    public WhisperTranscriptionService(Environment environment) {
        this.environment = environment;
    }

    public List<TimedWord> transcribe(String openAiApiKey, File audioFile, String lyricsHint) {
        log("transcribe:start file=" + audioFile + " bytes=" + audioFile.length() + " hasLyricsHint=" + hasText(lyricsHint));
        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("file", new FileSystemResource(audioFile));
        multipart.add("model", "whisper-1");
        multipart.add("language", environment.getProperty("musicload.whisper-language", "hi"));
        multipart.add("response_format", "verbose_json");
        multipart.add("timestamp_granularities[]", "word");
        multipart.add("temperature", "0");
        if (hasText(lyricsHint)) {
            multipart.add("prompt", lyricsHint.strip());
        }

        WhisperTranscript transcript = openAiClient(openAiApiKey)
                .post()
                .uri(WHISPER_URL)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipart)
                .retrieve()
                .body(WhisperTranscript.class);

        List<TimedWord> words = transcript == null ? List.of() : transcript.words();
        log("transcribe:done words=" + words.size());
        return words;
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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void log(String message) {
        System.out.println("[MusicLoad][Whisper] " + message);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WhisperTranscript(List<TimedWord> words) {
        private WhisperTranscript {
            words = words == null ? List.of() : List.copyOf(words);
        }
    }
}
