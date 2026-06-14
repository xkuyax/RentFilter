package org.example.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

class WillhabenScraperTest {

    private static final Pattern JSON_PATTERN =
            Pattern.compile("<script[^>]*type=\"application/json\"[^>]*>(.*?)</script>", Pattern.DOTALL);

    private WillhabenScraper scraper;
    private JsonNode jsonRoot;

    @BeforeEach
    void setUp() throws Exception {
        scraper = new WillhabenScraper();
        setSuperField(scraper, "userAgent", "test-agent");
        setSuperField(scraper, "requestDelayMs", 0);

        String html = Files.readString(Path.of("src/test/resources/willhaben-list-page.html"));
        Matcher m = JSON_PATTERN.matcher(html);
        if (m.find()) {
            jsonRoot = new ObjectMapper().readTree(m.group(1));
        }
    }

    @Test
    void parsesSearchResultMetadata() {
        JsonNode sr = jsonRoot.path("props").path("pageProps").path("searchResult");
        assertThat(sr.path("rowsFound").asInt()).isPositive();
        assertThat(sr.path("rowsReturned").asInt()).isEqualTo(3);
        assertThat(sr.path("pageRequested").asInt()).isEqualTo(1);
    }

    @Test
    void parsesAllListingsFromFixture() {
        JsonNode ads = jsonRoot.path("props").path("pageProps")
                .path("searchResult").path("advertSummaryList").path("advertSummary");
        assertThat(ads).isNotEmpty();
    }

    @Test
    void parseAd_extractsAllFields() {
        JsonNode ad = jsonRoot.path("props").path("pageProps")
                .path("searchResult").path("advertSummaryList").path("advertSummary").get(0);

        ListingDto dto = scraper.parseAd(ad);

        assertThat(dto).isNotNull();
        assertThat(dto.getExternalId()).isNotBlank();
        assertThat(dto.getTitle()).isNotBlank();
        assertThat(dto.getUrl()).startsWith("https://www.willhaben.at/iad/");
        assertThat(dto.getAddress()).isNotBlank();
        assertThat(dto.getPrice()).isNotNull().isPositive();
    }

    @Test
    void parseAd_extractsCoordinates() {
        JsonNode ads = jsonRoot.path("props").path("pageProps")
                .path("searchResult").path("advertSummaryList").path("advertSummary");

        for (JsonNode ad : ads) {
            ListingDto dto = scraper.parseAd(ad);
            // Not all listings have coordinates, but most should
            if (dto.getLatitude() != null) {
                assertThat(dto.getLatitude()).isGreaterThan(46.0).isLessThan(48.0);
                assertThat(dto.getLongitude()).isGreaterThan(14.0).isLessThan(16.0);
            }
        }
    }

    @Test
    void parseAd_extractsImages() {
        JsonNode ad = jsonRoot.path("props").path("pageProps")
                .path("searchResult").path("advertSummaryList").path("advertSummary").get(0);

        ListingDto dto = scraper.parseAd(ad);

        assertThat(dto.getImageUrls()).isNotNull().isNotEmpty();
        for (String url : dto.getImageUrls()) {
            assertThat(url).contains("cache.willhaben.at");
        }
    }

    @Test
    void parseAd_imageUrlsDontContainThumbnails() {
        JsonNode ads = jsonRoot.path("props").path("pageProps")
                .path("searchResult").path("advertSummaryList").path("advertSummary");

        for (JsonNode ad : ads) {
            ListingDto dto = scraper.parseAd(ad);
            if (dto.getImageUrls() == null || dto.getImageUrls().isEmpty()) continue;

            for (String url : dto.getImageUrls()) {
                assertThat(url.toLowerCase())
                        .as("Image URL should not be a thumbnail: " + url)
                        .doesNotContain("thumb");
            }
        }
    }

    @Test
    void parseAd_extractsAreaAndRooms() {
        JsonNode ads = jsonRoot.path("props").path("pageProps")
                .path("searchResult").path("advertSummaryList").path("advertSummary");

        for (JsonNode ad : ads) {
            ListingDto dto = scraper.parseAd(ad);
            // Most Wohnung listings have area and rooms
            assertThat(dto.getArea()).isNotNull();
        }
    }

    private static void setSuperField(Object target, String name, Object value) throws Exception {
        var f = target.getClass().getSuperclass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
