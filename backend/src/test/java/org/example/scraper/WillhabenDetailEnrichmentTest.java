package org.example.scraper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class WillhabenDetailEnrichmentTest {

    private WillhabenScraper scraper;
    private String detailHtml;

    @BeforeEach
    void setUp() throws Exception {
        scraper = new WillhabenScraper();
        setSuperField(scraper, "userAgent", "test-agent");
        setSuperField(scraper, "requestDelayMs", 0);

        // Create and configure cache with the detail page fixture
        HtmlCache cache = new HtmlCache();
        setField(cache, "enabled", true);
        setCacheDir(cache, Path.of("build", "test-cache-detail"));
        Files.createDirectories(Path.of("build", "test-cache-detail"));
        cache.clear();

        detailHtml = Files.readString(Path.of("src/test/resources/willhaben-detail-page.html"));
        String detailUrl = "https://www.willhaben.at/iad/immobilien/d/mietwohnungen/steiermark/graz/gries-66-5-qm-2-5-zimmer-wohnung-ab-sofort-1680988080/";
        String cacheKey = detailUrl + "|" + java.util.Map.of().hashCode();
        cache.put(cacheKey, detailHtml);

        setSuperField(scraper, "cache", cache);
    }

    @Test
    void enrichFromDetailPage_extractsDescriptionText() {
        ListingDto dto = new ListingDto();
        dto.setUrl("https://www.willhaben.at/iad/immobilien/d/mietwohnungen/steiermark/graz/gries-66-5-qm-2-5-zimmer-wohnung-ab-sofort-1680988080/");

        boolean cached = scraper.enrichFromDetailPage(dto);

        assertThat(cached).isTrue();
        assertThat(dto.getDescription()).isNotNull().isNotBlank();
        assertThat(dto.getDescription()).contains("INFO:", "Offene Türen");
        // Section header should be present as delimiter
        assertThat(dto.getDescription()).contains("--- Objektbeschreibung ---");
    }

    @Test
    void enrichFromDetailPage_extractsContentText() {
        ListingDto dto = new ListingDto();
        dto.setUrl("https://www.willhaben.at/iad/immobilien/d/mietwohnungen/steiermark/graz/gries-66-5-qm-2-5-zimmer-wohnung-ab-sofort-1680988080/");

        scraper.enrichFromDetailPage(dto);

        // Should contain multiple section headers
        String desc = dto.getDescription();
        assertThat(desc).contains("--- Objektinformationen ---");
        // Objektinformationen is an attribute list with label: value format
        assertThat(desc).contains("--- Ausstattung und Freiflächen ---");
    }

    @Test
    void extractStructuredText_preservesLineBreaksForListItems() {
        ListingDto dto = new ListingDto();
        dto.setUrl("https://www.willhaben.at/iad/immobilien/d/mietwohnungen/steiermark/graz/gries-66-5-qm-2-5-zimmer-wohnung-ab-sofort-1680988080/");

        scraper.enrichFromDetailPage(dto);

        String desc = dto.getDescription();
        assertThat(desc).contains("\n");

        // Section headers
        assertThat(desc).contains("--- Objektinformationen ---");
        assertThat(desc).contains("--- Objektbeschreibung ---");
        assertThat(desc).contains("--- Preis und Detailinformation ---");

        // Objektinformationen should have label: value format (uses divs, not spans)
        assertThat(desc).contains("Objekttyp:", "Wohnung", "Wohnfläche:");

        // Preis section items
        assertThat(desc).contains("Betriebskosten", "Kaution");
    }

    @Test
    void extractStructuredText_handlesBrElements() {
        ListingDto dto = new ListingDto();
        dto.setUrl("https://www.willhaben.at/iad/immobilien/d/mietwohnungen/steiermark/graz/gries-66-5-qm-2-5-zimmer-wohnung-ab-sofort-1680988080/");

        scraper.enrichFromDetailPage(dto);

        // <br> elements should become newlines (visible in the INFO description)
        assertThat(dto.getDescription()).contains("Offene Türen");
    }

    @Test
    void enrichFromDetailPage_handlesMissingUrl() {
        ListingDto dto = new ListingDto();
        dto.setUrl("https://www.willhaben.at/iad/immobilien/mietwohnungen/steiermark/graz");

        boolean cached = scraper.enrichFromDetailPage(dto);

        assertThat(cached).isTrue(); // returns true if skipped (no fetch needed)
    }

    private static void setSuperField(Object target, String name, Object value) throws Exception {
        var f = target.getClass().getSuperclass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        var f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void setCacheDir(HtmlCache cache, Path dir) throws Exception {
        var f = HtmlCache.class.getDeclaredField("cacheDir");
        f.setAccessible(true);
        f.set(cache, dir);
    }
}
