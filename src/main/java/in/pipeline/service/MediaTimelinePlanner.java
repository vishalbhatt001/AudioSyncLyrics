package in.pipeline.service;

import java.util.ArrayList;
import java.util.List;

public final class MediaTimelinePlanner {
    private static final int REQUIRED_MEDIA_COUNT = 4;
    private static final double SLOT_SECONDS = 4.0;

    private MediaTimelinePlanner() {
    }

    public static List<MediaTimelineSlot> plan(List<VisualMediaAsset> mediaAssets, int durationSeconds) {
        validateMediaAssets(mediaAssets);

        List<MediaTimelineSlot> slots = new ArrayList<>();
        double cursor = 0.0;
        int assetIndex = 0;
        while (cursor < durationSeconds) {
            double slotDuration = Math.min(SLOT_SECONDS, durationSeconds - cursor);
            slots.add(new MediaTimelineSlot(mediaAssets.get(assetIndex), cursor, slotDuration));
            cursor += slotDuration;
            assetIndex = (assetIndex + 1) % mediaAssets.size();
        }
        return List.copyOf(slots);
    }

    public static void validateMediaAssets(List<VisualMediaAsset> mediaAssets) {
        if (mediaAssets == null || mediaAssets.size() != REQUIRED_MEDIA_COUNT) {
            throw new IllegalArgumentException("Exactly four image files or exactly four video files are required.");
        }

        VisualMediaType expectedType = mediaAssets.getFirst().type();
        for (VisualMediaAsset mediaAsset : mediaAssets) {
            if (mediaAsset.type() != expectedType) {
                throw new IllegalArgumentException("Do not mix images and videos. Upload either four images or four videos.");
            }
        }
    }
}
