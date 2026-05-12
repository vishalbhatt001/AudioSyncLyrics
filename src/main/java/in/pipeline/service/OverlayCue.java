package in.pipeline.service;

import java.nio.file.Path;

public record OverlayCue(Path imagePath, double start, double end, int x, int y) {
}
