package in.pipeline.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

@Schema(name = "ShortsRenderRequest", description = "Multipart form payload for local Shorts rendering")
public final class ShortsRenderRequest {
    @Schema(description = "59-second hook audio file", type = "string", format = "binary", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile mp3;

    @Schema(description = "Background image file", type = "string", format = "binary", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile image;

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

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
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
