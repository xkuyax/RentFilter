package org.example.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class GenossenschaftenScraperTest {

    private GenossenschaftenScraper scraper;
    private Document detailPageDoc;

    @BeforeEach
    void setUp() throws Exception {
        scraper = new GenossenschaftenScraper();
        setSuperField(scraper, "userAgent", "test-agent");
        setSuperField(scraper, "requestDelayMs", 0);

        String html = Files.readString(Path.of("src/test/resources/geno-detail-page.html"));
        detailPageDoc = Jsoup.parse(html);
    }

    @Test
    void parseCard_extractsAllFields() throws Exception {
        String html = Files.readString(Path.of("src/test/resources/geno-list-page.html"));
        Document doc = Jsoup.parse(html);
        Element firstCard = doc.selectFirst(".residence-teaser");

        ListingDto dto = scraper.parseCard(firstCard);

        assertThat(dto).isNotNull();
        assertThat(dto.getTitle()).isNotNull().isNotBlank();
        assertThat(dto.getUrl()).startsWith("https://genossenschaften.immo");
        assertThat(dto.getUrl()).endsWith("/");
        assertThat(dto.getExternalId()).isNotNull().isNotBlank();
        assertThat(dto.getThumbnailUrl()).isNotNull().isNotBlank();
    }

    @Test
    void parseCard_parsesMetadata() throws Exception {
        String html = Files.readString(Path.of("src/test/resources/geno-list-page.html"));
        Document doc = Jsoup.parse(html);

        for (Element card : doc.select(".residence-teaser")) {
            ListingDto dto = scraper.parseCard(card);
            assertThat(dto).isNotNull();
            assertThat(dto.getTitle()).isNotBlank();
            assertThat(dto.getExternalId()).isNotBlank();
            assertThat(dto.getUrl()).isNotBlank();
        }
    }

    @Test
    void parseCard_extractsRoomsAndArea() throws Exception {
        String html = Files.readString(Path.of("src/test/resources/geno-list-page.html"));
        Document doc = Jsoup.parse(html);
        Element card = doc.selectFirst(".residence-teaser");

        ListingDto dto = scraper.parseCard(card);
        assertThat(dto.getRooms()).isNotNull().isPositive();
        assertThat(dto.getArea()).isNotNull().isPositive();
    }

    @Test
    void enrichFromDetailPage_extractsAddress() throws Exception {
        ListingDto dto = new ListingDto();
        dto.setUrl("https://genossenschaften.immo/immobilien/regionen/steiermark/bruck-muerzzuschlag/bruck-an-der-mur-lamingfeldsiedlung-c/137523/");

        scraper.enrichFromDetailPage(dto);

        assertThat(dto.getAddress()).isNotNull().isNotBlank();
        assertThat(dto.getAddress()).contains("8600");
    }

    @Test
    void enrichFromDetailPage_extractsCoordinates() throws Exception {
        ListingDto dto = new ListingDto();
        dto.setUrl("https://genossenschaften.immo/immobilien/regionen/steiermark/bruck-muerzzuschlag/bruck-an-der-mur-lamingfeldsiedlung-c/137523/");

        scraper.enrichFromDetailPage(dto);

        assertThat(dto.getLatitude()).isNotNull();
        assertThat(dto.getLongitude()).isNotNull();
    }

    @Test
    void enrichFromDetailPage_extractsImages() throws Exception {
        ListingDto dto = new ListingDto();
        dto.setUrl("https://genossenschaften.immo/immobilien/regionen/steiermark/bruck-muerzzuschlag/bruck-an-der-mur-lamingfeldsiedlung-c/137523/");

        scraper.enrichFromDetailPage(dto);

        assertThat(dto.getImageUrls()).isNotNull();
        assertThat(dto.getImageUrls()).isNotEmpty();
    }

    private static void setSuperField(Object target, String name, Object value) throws Exception {
        var f = target.getClass().getSuperclass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
