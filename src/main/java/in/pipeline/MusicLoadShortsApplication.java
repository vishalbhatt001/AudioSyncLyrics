package in.pipeline;

import in.pipeline.config.DotenvConfigLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Path;

@SpringBootApplication
public class MusicLoadShortsApplication {
    public static void main(String[] args) {
        DotenvConfigLoader.load(Path.of(".env"));
        SpringApplication.run(MusicLoadShortsApplication.class, args);
    }
}
