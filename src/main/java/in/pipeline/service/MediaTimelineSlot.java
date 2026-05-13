package in.pipeline.service;

public record MediaTimelineSlot(VisualMediaAsset asset, double start, double duration) {
    public double end() {
        return start + duration;
    }
}
