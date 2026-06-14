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

import static org.assertj.core.api.Assertions.*;

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

        assertThat(dto).isNotNull();
        assertThat(dto.getExternalId()).isEqualTo("1070");
        assertThat(dto.getTitle()).contains("2-Zimmer-Wohnung");
        assertThat(dto.getUrl()).contains("immobilien_id=1070");
        assertThat(dto.getRooms()).isEqualTo(2.0f);
        assertThat(dto.getPrice()).isNotNull();
        assertThat(dto.getPrice().floatValue()).isPositive();
        assertThat(dto.getArea()).isNotNull().isPositive();
        assertThat(dto.getThumbnailUrl()).isNotNull().contains("properties");
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
                assertThat(dto.getTitle()).isNotNull();
                assertThat(dto.getUrl()).isNotNull();
                assertThat(dto.getExternalId()).isNotNull();
                assertThat(dto.getThumbnailUrl()).isNotNull();
                parsed++;
            }
        }
        assertThat(parsed).isEqualTo(3);
    }

    @Test
    void extractAddressFromTitle_parsesKnownFormat() {
        String title = "2-Zimmer-Wohnung mit Balkon | Friedrichgasse 3, 8010 Graz | GRAWEwohnen";
        assertThat(scraper.extractAddressFromTitle(title))
                .isEqualTo("Friedrichgasse 3, 8010 Graz");
    }

    @Test
    void extractAddressFromTitle_fromRealPage() {
        String address = scraper.extractAddressFromTitle(detailPageDoc.title());
        assertThat(address).isNotNull();
        assertThat(address).satisfiesAnyOf(
                a -> assertThat(a).contains("Friedrichgasse"),
                a -> assertThat(a).contains("8010"),
                a -> assertThat(a).contains("Graz")
        );
    }

    @Test
    void extractImages_findsGalleryImages() {
        ListingDto dto = new ListingDto();
        scraper.extractImages(detailPageDoc, dto);

        assertThat(dto.getImageUrls()).isNotNull().isNotEmpty();
        assertThat(dto.getImageUrls()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(dto.getImageUrls()).allMatch(url -> url.contains("properties/"));
    }

    @Test
    void extractBenefits_findsBenefitPills() {
        ListingDto dto = new ListingDto();
        scraper.extractBenefits(detailPageDoc, dto);

        assertThat(dto.getBenefits()).isNotNull().isNotEmpty();
        assertThat(dto.getBenefits()).contains("Balkon/Terrasse/Garten", "Altbau");
    }

    @Test
    void extractCostTable_findsAllCostRows() {
        ListingDto dto = new ListingDto();
        scraper.extractCostTable(detailPageDoc, dto);

        assertThat(dto.getNetRent()).isNotNull();
        assertThat(dto.getNetRent().floatValue()).isGreaterThan(100f);
        assertThat(dto.getOperatingCosts()).isNotNull();
        assertThat(dto.getOperatingCosts().floatValue()).isPositive();
        assertThat(dto.getVat()).isNotNull();
        assertThat(dto.getVat().floatValue()).isPositive();
        assertThat(dto.getDeposit()).isNotNull();
        assertThat(dto.getDeposit().floatValue()).isGreaterThan(100f);
        assertThat(dto.getAvailableFrom()).isEqualTo("ab sofort");
        assertThat(dto.getProvision()).isEqualTo("Nein");
        assertThat(dto.getBuildYear()).isEqualTo(1880);
    }

    @Test
    void extractEnergyInfo_findsEnergyData() {
        ListingDto dto = new ListingDto();
        scraper.extractEnergyInfo(detailPageDoc, dto);

        assertThat(dto.getHeatingDemand()).isNotNull();
        assertThat(dto.getHeatingDemand()).isCloseTo(98.2f, within(0.01f));
        assertThat(dto.getFgee()).isNotNull();
        assertThat(dto.getFgee()).isCloseTo(1.62f, within(0.01f));
    }

    @Test
    void extract360View_findsMatterport() {
        ListingDto dto = new ListingDto();
        scraper.extract360View(detailPageDoc, dto);

        assertThat(dto.isHas360View()).isTrue();
        assertThat(dto.getMatterportUrl()).isNotNull().contains("matterport");
    }

    @Test
    void roomsPattern_matchesOneDigit() {
        var pattern = java.util.regex.Pattern.compile("(\\d+)[-\\s]Zimmer");
        var m = pattern.matcher("2-Zimmer-Wohnung in Graz");
        assertThat(m.find()).isTrue();
        assertThat(m.group(1)).isEqualTo("2");
    }

    @Test
    void roomsPattern_matchesInTitle() {
        var pattern = java.util.regex.Pattern.compile("(\\d+)[-\\s]Zimmer");
        var m = pattern.matcher("3 Zimmer Wohnung mit Balkon");
        assertThat(m.find()).isTrue();
        assertThat(m.group(1)).isEqualTo("3");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        var f = target.getClass().getSuperclass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
