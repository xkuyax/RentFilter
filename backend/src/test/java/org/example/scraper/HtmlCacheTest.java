package org.example.scraper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class HtmlCacheTest {

    private HtmlCache cache;

    @BeforeEach
    void setUp() throws Exception {
        cache = new HtmlCache();
        setField(cache, "enabled", true);
        overrideCacheDir(cache, Path.of("build", "test-cache"));
        Files.createDirectories(Path.of("build", "test-cache"));
    }

    @AfterEach
    void tearDown() throws Exception {
        cache.clear();
    }

    @Test
    void putAndGet_roundtripsCorrectly() {
        cache.put("https://example.com/test", "<html>hello</html>");

        String result = cache.get("https://example.com/test");
        assertThat(result).isEqualTo("<html>hello</html>");
    }

    @Test
    void getMissingUrl_returnsNull() {
        String result = cache.get("https://nonexistent.example.com/foo");
        assertThat(result).isNull();
    }

    @Test
    void put_overwritesExisting() {
        cache.put("https://example.com/test", "<html>v1</html>");
        cache.put("https://example.com/test", "<html>v2</html>");

        assertThat(cache.get("https://example.com/test")).isEqualTo("<html>v2</html>");
    }

    @Test
    void sameUrlSameHash() {
        cache.put("https://example.com/a?b=c", "data1");

        assertThat(cache.get("https://example.com/a?b=c")).isEqualTo("data1");
    }

    @Test
    void disabledCache_skipsWrites() throws Exception {
        setField(cache, "enabled", false);

        cache.put("https://example.com/disabled", "should not save");
        assertThat(cache.get("https://example.com/disabled")).isNull();
    }

    @Test
    void disabledCache_skipsReads() throws Exception {
        setField(cache, "enabled", false);

        cache.put("https://example.com/disabled-read", "data");
        assertThat(cache.get("https://example.com/disabled-read")).isNull();
    }

    @Test
    void isEnabled_respectsSetting() throws Exception {
        assertThat(cache.isEnabled()).isTrue();

        setField(cache, "enabled", false);
        assertThat(cache.isEnabled()).isFalse();
    }

    @Test
    void clear_removesAllFiles() {
        cache.put("https://example.com/1", "a");
        cache.put("https://example.com/2", "b");
        assertThat(cache.get("https://example.com/1")).isNotNull();

        cache.clear();
        assertThat(cache.get("https://example.com/1")).isNull();
        assertThat(cache.get("https://example.com/2")).isNull();
    }

    @Test
    void cacheDir_isCreatedAutomatically() throws Exception {
        setField(cache, "enabled", false);
        cache.clear();

        Path testDir = Path.of("build", "test-cache-auto");
        overrideCacheDir(cache, testDir);
        setField(cache, "enabled", true);

        cache.put("https://example.com/auto", "data");
        assertThat(cache.get("https://example.com/auto")).isEqualTo("data");
        assertThat(testDir).exists();

        // cleanup
        Files.walk(testDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> { try { Files.deleteIfExists(p); } catch (java.io.IOException ignored) {} });
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        var f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void overrideCacheDir(HtmlCache cache, Path dir) throws Exception {
        var f = HtmlCache.class.getDeclaredField("cacheDir");
        f.setAccessible(true);
        f.set(cache, dir);
    }
}
