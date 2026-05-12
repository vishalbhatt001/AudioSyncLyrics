package in.pipeline.service;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public final class LocalVideoRenderService {
    private final FfmpegProcessRunner processRunner;

    public LocalVideoRenderService(FfmpegProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    public void render(Path hookAudio, Path backgroundImage, List<OverlayCue> overlayCues,
                       Path outputPath, RenderProgress progress) throws Exception {
        log("render:start audio=" + hookAudio + " image=" + backgroundImage + " overlays=" + overlayCues.size());
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-loop");
        command.add("1");
        command.add("-framerate");
        command.add("30");
        command.add("-i");
        command.add(backgroundImage.toAbsolutePath().toString());
        command.add("-i");
        command.add(hookAudio.toAbsolutePath().toString());
        for (OverlayCue cue : overlayCues) {
            command.add("-loop");
            command.add("1");
            command.add("-framerate");
            command.add("30");
            command.add("-i");
            command.add(cue.imagePath().toAbsolutePath().toString());
        }
        command.add("-t");
        command.add(String.valueOf(ShortsRenderSpec.HOOK_SECONDS));
        command.add("-filter_complex");
        command.add(buildOverlayFilter(overlayCues));
        command.add("-map");
        command.add("[video_out]");
        command.add("-af");
        command.add("afade=t=out:st=57:d=2");
        command.add("-map");
        command.add("1:a");
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("ultrafast");
        command.add("-crf");
        command.add("28");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("192k");
        command.add("-progress");
        command.add("pipe:1");
        command.add("-nostats");
        command.add("-shortest");
        command.add(outputPath.toAbsolutePath().toString());

        processRunner.run(command, "FFmpeg local render", ShortsRenderSpec.HOOK_SECONDS, progress);
        log("render:done output=" + outputPath);
    }

    private static String buildOverlayFilter(List<OverlayCue> overlayCues) {
        log("buildOverlayFilter:start overlays=" + overlayCues.size());
        StringBuilder filter = new StringBuilder();
        filter.append("[0:v]scale=1188:2112:force_original_aspect_ratio=increase,");
        filter.append("crop=1080:1920:x='54+54*sin(t*0.08)':y='96+96*cos(t*0.06)'[base]");

        String previous = "base";
        for (int index = 0; index < overlayCues.size(); index++) {
            OverlayCue cue = overlayCues.get(index);
            String output = index == overlayCues.size() - 1 ? "video_out" : "v" + index;
            int inputIndex = index + 2;

            filter.append(";[")
                    .append(previous)
                    .append("][")
                    .append(inputIndex)
                    .append(":v]overlay=")
                    .append(cue.x())
                    .append(':')
                    .append(cue.y())
                    .append(":enable='between(t,")
                    .append(formatSeconds(cue.start()))
                    .append(',')
                    .append(formatSeconds(cue.end()))
                    .append(")'[")
                    .append(output)
                    .append(']');
            previous = output;
        }

        log("buildOverlayFilter:done length=" + filter.length());
        return filter.toString();
    }

    private static String formatSeconds(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private static void log(String message) {
        System.out.println("[MusicLoad][VideoRender] " + message);
    }
}
