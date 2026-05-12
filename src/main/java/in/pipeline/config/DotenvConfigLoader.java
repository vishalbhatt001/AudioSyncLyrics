package in.pipeline.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DotenvConfigLoader {
    private DotenvConfigLoader() {
    }

    public static void load(Path path) {
        if (!Files.isRegularFile(path)) {
            return;
        }

        try {
            for (String rawLine : Files.readAllLines(path)) {
                String line = rawLine.trim();
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }

                String key = line.substring(0, separator).trim();
                String value = stripQuotes(line.substring(separator + 1).trim());

                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read .env file: " + path.toAbsolutePath(), exception);
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
