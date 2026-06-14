package org.example.scraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class HtmlCache {

    private static final Logger log = LoggerFactory.getLogger(HtmlCache.class);

    @Value("${scraping.cache.enabled:true}")
    private boolean enabled;

    private final Path cacheDir = Path.of(System.getProperty("user.dir"), "data", "html-cache");

    public boolean isEnabled() {
        return enabled;
    }

    public String get(String url) {
        if (!enabled) return null;
        Path file = filename(url);
        if (Files.exists(file)) {
            try {
                return Files.readString(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.warn("Cache read failed for {}", url, e);
            }
        }
        return null;
    }

    public void put(String url, String body) {
        if (!enabled) return;
        try {
            Files.createDirectories(cacheDir);
            Path file = filename(url);
            Files.writeString(file, body, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.warn("Cache write failed for {}", url, e);
        }
    }

    public void clear() {
        try {
            if (Files.exists(cacheDir)) {
                try (var files = Files.walk(cacheDir)) {
                    files.sorted(java.util.Comparator.reverseOrder())
                            .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
                }
            }
        } catch (IOException e) {
            log.warn("Cache clear failed", e);
        }
    }

    private Path filename(String url) {
        String hash = HexFormat.of().formatHex(sha256(url));
        return cacheDir.resolve(hash + ".html");
    }

    private byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return input.getBytes(StandardCharsets.UTF_8);
        }
    }
}
