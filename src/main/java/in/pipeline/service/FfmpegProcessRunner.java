package in.pipeline.service;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public final class FfmpegProcessRunner {
    public void run(List<String> command, String label, int durationSeconds, RenderProgress progress) throws Exception {
        log(label + ":start");
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        StringBuilder output = new StringBuilder();
        int lastPercent = -1;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
                if (line.startsWith("out_time_ms=")) {
                    String rawOutTime = line.substring("out_time_ms=".length());
                    if ("N/A".equals(rawOutTime)) {
                        continue;
                    }

                    long outTimeMicros = Long.parseLong(rawOutTime);
                    int percent = (int) Math.min(100, Math.round(outTimeMicros / 1000.0 / durationSeconds));
                    if (percent >= lastPercent + 10 || percent == 100) {
                        lastPercent = percent;
                        progress.report(percent, label);
                    }
                } else if ("progress=end".equals(line)) {
                    progress.report(100, label + " complete");
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException(label + " failed with exit code " + exitCode + "\n" + output);
        }
        log(label + ":done");
    }

    private static void log(String message) {
        System.out.println("[MusicLoad][FFmpeg] " + message);
    }
}
