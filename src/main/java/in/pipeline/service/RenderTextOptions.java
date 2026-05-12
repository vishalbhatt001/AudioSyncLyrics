package in.pipeline.service;

public record RenderTextOptions(String introText, String ctaText) {
    private static final String DEFAULT_INTRO_TEXT = "Agar maa yaad aati hai, ruk jao...";
    private static final String DEFAULT_CTA_TEXT = "Pura gaana sunne ke liye 👇 Related Video pe click karein";

    public RenderTextOptions {
        introText = hasText(introText) ? introText.strip() : DEFAULT_INTRO_TEXT;
        ctaText = hasText(ctaText) ? ctaText.strip() : DEFAULT_CTA_TEXT;
    }

    public static RenderTextOptions defaults() {
        return new RenderTextOptions(DEFAULT_INTRO_TEXT, DEFAULT_CTA_TEXT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
