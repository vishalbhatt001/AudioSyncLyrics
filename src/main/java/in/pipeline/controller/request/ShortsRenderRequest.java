package in.pipeline.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Schema(name = "ShortsRenderRequest", description = "Multipart form payload for local Shorts rendering")
public final class ShortsRenderRequest {
    @Schema(description = "59-second hook audio file", type = "string", format = "binary", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile mp3;

    @Schema(description = "First image/video. Upload either four images or four videos.", type = "string", format = "binary", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile media1;

    @Schema(description = "Second image/video. Upload same type as media1.", type = "string", format = "binary", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile media2;

    @Schema(description = "Third image/video. Upload same type as media1.", type = "string", format = "binary", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile media3;

    @Schema(description = "Fourth image/video. Upload same type as media1.", type = "string", format = "binary", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile media4;

    @Schema(description = "Optional lyric context for Whisper/OpenAI correction")
    private String lyricsHint;

    @Schema(description = "Display language: hinglish, hindi, or english", defaultValue = "hinglish")
    private String lyricsLanguage = "hinglish";

    @Schema(description = "Optional intro overlay text. Emoji supported.")
    private String introText;

    @Schema(description = "Optional CTA overlay text. Emoji supported.")
    private String ctaText;

    public MultipartFile getMp3() {
        return mp3;
    }

    public void setMp3(MultipartFile mp3) {
        this.mp3 = mp3;
    }

    public MultipartFile getMedia1() {
        return media1;
    }

    public void setMedia1(MultipartFile media1) {
        this.media1 = media1;
    }

    public MultipartFile getMedia2() {
        return media2;
    }

    public void setMedia2(MultipartFile media2) {
        this.media2 = media2;
    }

    public MultipartFile getMedia3() {
        return media3;
    }

    public void setMedia3(MultipartFile media3) {
        this.media3 = media3;
    }

    public MultipartFile getMedia4() {
        return media4;
    }

    public void setMedia4(MultipartFile media4) {
        this.media4 = media4;
    }

    public List<MultipartFile> mediaFiles() {
        return List.of(media1, media2, media3, media4);
    }

    public String getLyricsHint() {
        return lyricsHint;
    }

    public void setLyricsHint(String lyricsHint) {
        this.lyricsHint = lyricsHint;
    }

    public String getLyricsLanguage() {
        return lyricsLanguage;
    }

    public void setLyricsLanguage(String lyricsLanguage) {
        this.lyricsLanguage = lyricsLanguage;
    }

    public String getIntroText() {
        return introText;
    }

    public void setIntroText(String introText) {
        this.introText = introText;
    }

    public String getCtaText() {
        return ctaText;
    }

    public void setCtaText(String ctaText) {
        this.ctaText = ctaText;
    }

    public String normalizedLyricsLanguage() {
        return lyricsLanguage == null || lyricsLanguage.isBlank() ? "hinglish" : lyricsLanguage;
    }
}
