package in.pipeline.service;

@FunctionalInterface
public interface RenderProgress {
    void report(int percent, String message);
}
