package in.pipeline.service;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public final class AudioPreprocessorService {
    private final FfmpegProcessRunner processRunner;

    public AudioPreprocessorService(FfmpegProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    public void compressForWhisper(Path sourceAudio, Path targetAudio, RenderProgress progress) throws Exception {
        log("compressForWhisper:start source=" + sourceAudio + " target=" + targetAudio);
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(sourceAudio.toAbsolutePath().toString());
        command.add("-t");
        command.add(String.valueOf(ShortsRenderSpec.HOOK_SECONDS));
        command.add("-vn");
        command.add("-ac");
        command.add("1");
        command.add("-ar");
        command.add("16000");
        command.add("-b:a");
        command.add("64k");
        command.add("-progress");
        command.add("pipe:1");
        command.add("-nostats");
        command.add(targetAudio.toAbsolutePath().toString());

        processRunner.run(command, "FFmpeg Whisper audio compression", ShortsRenderSpec.HOOK_SECONDS, progress);
        log("compressForWhisper:done target=" + targetAudio + " bytes=" + Files.size(targetAudio));
    }

    private static void log(String message) {
        System.out.println("[MusicLoad][AudioPreprocessor] " + message);
    }
}
