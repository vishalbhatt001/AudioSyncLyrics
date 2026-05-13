package in.pipeline.service;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public record VisualMediaAsset(Path path, VisualMediaType type) {
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "m4v", "webm");

    public static VisualMediaAsset from(Path path) {
        String extension = extension(path);
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return new VisualMediaAsset(path.toAbsolutePath().normalize(), VisualMediaType.IMAGE);
        }
        if (VIDEO_EXTENSIONS.contains(extension)) {
            return new VisualMediaAsset(path.toAbsolutePath().normalize(), VisualMediaType.VIDEO);
        }
        throw new IllegalArgumentException("Unsupported media file type: " + path.getFileName());
    }

    private static String extension(Path path) {
        String filename = path.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new IllegalArgumentException("Media file must have an extension: " + filename);
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
