package org.example.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraweScraperTest {

    private GraweScraper scraper;
    private final ObjectMapper mapper = new ObjectMapper();
    private Document detailPageDoc;

    @BeforeEach
    void setUp() throws Exception {
        scraper = new GraweScraper();
        setField(scraper, "userAgent", "test-agent");
        setField(scraper, "requestDelayMs", 0);

        String html = Files.readString(Path.of("src/test/resources/grawe-detail-page.html"));
        detailPageDoc = Jsoup.parse(html);
    }

    @Test
    void parseCard_extractsAllFields() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/grawe-ajax-response.json"));
        JsonNode root = mapper.readTree(json);
        String htmlOutput = root.get("data").get("output").asText();

        Document html = Jsoup.parse(htmlOutput);
        Element firstCard = html.selectFirst(".similar-property-item");

        ListingDto dto = scraper.parseCard(firstCard);

        assertNotNull(dto);
        assertEquals("1070", dto.getExternalId());
        assertTrue(dto.getTitle().contains("2-Zimmer-Wohnung"));
        assertTrue(dto.getUrl().contains("immobilien_id=1070"));
        assertEquals(2.0f, dto.getRooms());
        assertNotNull(dto.getPrice());
        assertTrue(dto.getPrice().floatValue() > 0);
        assertNotNull(dto.getArea());
        assertTrue(dto.getArea() > 0);
        assertNotNull(dto.getThumbnailUrl());
        assertTrue(dto.getThumbnailUrl().contains("properties"));
    }

    @Test
    void parseCard_parsesAllCards() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/grawe-ajax-response.json"));
        JsonNode root = mapper.readTree(json);
        String htmlOutput = root.get("data").get("output").asText();

        Document html = Jsoup.parse(htmlOutput);
        var cards = html.select(".similar-property-item");

        int parsed = 0;
        for (Element card : cards) {
            ListingDto dto = scraper.parseCard(card);
            if (dto != null) {
                assertNotNull(dto.getTitle());
                assertNotNull(dto.getUrl());
                assertNotNull(dto.getExternalId());
                assertNotNull(dto.getThumbnailUrl());
                parsed++;
            }
        }
        assertEquals(3, parsed);
    }

    @Test
    void extractAddressFromTitle_parsesKnownFormat() {
        String title = "2-Zimmer-Wohnung mit Balkon | Friedrichgasse 3, 8010 Graz | GRAWEwohnen";
        assertEquals("Friedrichgasse 3, 8010 Graz", scraper.extractAddressFromTitle(title));
    }

    @Test
    void extractAddressFromTitle_fromRealPage() {
        String address = scraper.extractAddressFromTitle(detailPageDoc.title());
        assertNotNull(address);
        assertTrue(address.contains("Friedrichgasse") || address.contains("8010") || address.contains("Graz"));
    }

    @Test
    void extractImages_findsGalleryImages() {
        ListingDto dto = new ListingDto();
        scraper.extractImages(detailPageDoc, dto);

        assertNotNull(dto.getImageUrls());
        assertFalse(dto.getImageUrls().isEmpty());
        assertTrue(dto.getImageUrls().size() >= 5, "Expected at least 5 gallery images");
        for (String url : dto.getImageUrls()) {
            assertTrue(url.contains("properties/"));
        }
    }

    @Test
    void extractBenefits_findsBenefitPills() {
        ListingDto dto = new ListingDto();
        scraper.extractBenefits(detailPageDoc, dto);

        assertNotNull(dto.getBenefits());
        assertFalse(dto.getBenefits().isEmpty());
        assertTrue(dto.getBenefits().contains("Balkon/Terrasse/Garten"));
        assertTrue(dto.getBenefits().contains("Altbau"));
    }

    @Test
    void extractCostTable_findsAllCostRows() {
        ListingDto dto = new ListingDto();
        scraper.extractCostTable(detailPageDoc, dto);

        assertNotNull(dto.getNetRent());
        assertTrue(dto.getNetRent().floatValue() > 100);
        assertNotNull(dto.getOperatingCosts());
        assertTrue(dto.getOperatingCosts().floatValue() > 0);
        assertNotNull(dto.getVat());
        assertTrue(dto.getVat().floatValue() > 0);
        assertNotNull(dto.getDeposit());
        assertTrue(dto.getDeposit().floatValue() > 100);
        assertEquals("ab sofort", dto.getAvailableFrom());
        assertEquals("Nein", dto.getProvision());
        assertEquals(1880, dto.getBuildYear());
    }

    @Test
    void extractEnergyInfo_findsEnergyData() {
        ListingDto dto = new ListingDto();
        scraper.extractEnergyInfo(detailPageDoc, dto);

        assertNotNull(dto.getHeatingDemand());
        assertEquals(98.2f, dto.getHeatingDemand(), 0.01);
        assertNotNull(dto.getFgee());
        assertEquals(1.62f, dto.getFgee(), 0.01);
    }

    @Test
    void extract360View_findsMatterport() {
        ListingDto dto = new ListingDto();
        scraper.extract360View(detailPageDoc, dto);

        assertTrue(dto.isHas360View());
        assertNotNull(dto.getMatterportUrl());
        assertTrue(dto.getMatterportUrl().contains("matterport"));
    }

    @Test
    void roomsPattern_matchesOneDigit() {
        var pattern = java.util.regex.Pattern.compile("(\\d+)[-\\s]Zimmer");
        var m = pattern.matcher("2-Zimmer-Wohnung in Graz");
        assertTrue(m.find());
        assertEquals("2", m.group(1));
    }

    @Test
    void roomsPattern_matchesInTitle() {
        var pattern = java.util.regex.Pattern.compile("(\\d+)[-\\s]Zimmer");
        var m = pattern.matcher("3 Zimmer Wohnung mit Balkon");
        assertTrue(m.find());
        assertEquals("3", m.group(1));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        var f = target.getClass().getSuperclass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
