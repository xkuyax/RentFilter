package org.example.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Source;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GraweScraper extends AbstractScraper {

    private static final String AJAX_URL = "https://www.grawewohnen.at/wp-admin/admin-ajax.php";
    private static final Pattern ROOMS_PATTERN = Pattern.compile("(\\d+)[-\\s]Zimmer");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("\\|\\s*([^|]+?)\\s*\\|\\s*GRAWEwohnen");
    private static final Pattern MONEY_PATTERN = Pattern.compile("[€]\\s*([0-9.,]+)");
    private static final Pattern FLOAT_PATTERN = Pattern.compile("([0-9.,]+)\\s*(?:m²|kWh)");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Source getSource() {
        return Source.GRAWE;
    }

    @Override
    public List<ListingDto> scrape() throws Exception {
        List<ListingDto> results = new ArrayList<>();

        int page = 1;
        int totalPages = 1;

        while (page <= totalPages) {
            log.info("Grawe: fetching page {}/{}", page, totalPages);

            String formBody = buildFormBody(page);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AJAX_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", userAgent)
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            if (!json.path("success").asBoolean()) {
                log.warn("Grawe: API returned success=false on page {}", page);
                break;
            }

            JsonNode data = json.get("data");
            totalPages = data.get("total_pages").asInt();
            String htmlOutput = data.get("output").asText();

            Document html = Jsoup.parse(htmlOutput);
            for (Element card : html.select(".similar-property-item")) {
                ListingDto dto = parseCard(card);
                if (dto != null) {
                    results.add(dto);
                }
            }

            if (page >= totalPages) break;
            page++;
            Thread.sleep(requestDelayMs);
        }

        log.info("Grawe: {} listings found across {} pages", results.size(), totalPages);

        for (ListingDto dto : results) {
            enrichFromDetailPage(dto);
            Thread.sleep(requestDelayMs);
        }

        return results;
    }

    private String buildFormBody(int page) {
        return "action=" + enc("filter_properties")
                + "&property_type=" + enc("Wohnung")
                + "&locations=" + enc("[{\"city\":\"Graz\"}]")
                + "&paged=" + page
                + "&posts_per_page=18";
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    ListingDto parseCard(Element card) {
        try {
            Element link = card.selectFirst("a[href]");
            if (link == null) return null;

            String url = link.attr("href");
            if (!url.startsWith("http")) {
                url = "https://www.grawewohnen.at" + url;
            }

            String extId = url.replaceAll(".*immobilien_id=(\\d+).*", "$1");

            Element titleEl = card.selectFirst(".property-title div.mt-2");
            String title = titleEl != null ? titleEl.text().trim() : "";

            Element img = card.selectFirst("img.estate-img");
            String thumbnailUrl = img != null ? img.attr("src") : null;

            ListingDto dto = new ListingDto();
            dto.setExternalId(extId);
            dto.setTitle(title);
            dto.setUrl(url);
            dto.setThumbnailUrl(thumbnailUrl);

            String cardText = card.text();

            Matcher rm = ROOMS_PATTERN.matcher(title);
            if (rm.find()) {
                dto.setRooms(Float.parseFloat(rm.group(1)));
            }

            dto.setPrice(extractMoney(cardText, "Bruttomiete"));
            dto.setArea(extractFloat(cardText, "Fläche"));

            return dto;
        } catch (Exception e) {
            log.warn("Grawe: failed to parse card", e);
            return null;
        }
    }

    void enrichFromDetailPage(ListingDto dto) {
        try {
            Document detail = fetch(dto.getUrl());

            dto.setAddress(extractAddressFromTitle(detail.title()));
            extractImages(detail, dto);
            extractBenefits(detail, dto);
            extractDescription(detail, dto);
            extractCostTable(detail, dto);
            extractEnergyInfo(detail, dto);
            extract360View(detail, dto);

        } catch (Exception e) {
            log.warn("Grawe: failed to enrich detail page {}", dto.getUrl(), e);
        }
    }

    String extractAddressFromTitle(String pageTitle) {
        Matcher m = ADDRESS_PATTERN.matcher(pageTitle);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    // --- detail page parsers (package-private for testing) ---

    void extractImages(Document detail, ListingDto dto) {
        List<String> urls = new ArrayList<>();
        for (Element a : detail.select(".image-gallery a.image-popup[data-lightbox=gallery]")) {
            String href = a.attr("href");
            if (!href.isBlank()) {
                urls.add(href);
            }
        }
        if (!urls.isEmpty()) {
            dto.setImageUrls(urls);
        }
    }

    void extractBenefits(Document detail, ListingDto dto) {
        Elements pills = detail.select(".benefits > div");
        if (!pills.isEmpty()) {
            List<String> list = pills.eachText().stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
            dto.setBenefits(list);
        }
    }

    void extractDescription(Document detail, ListingDto dto) {
        Element descBlock = detail.selectFirst(".mb-12 > .border-primary.border-t-1\\.5.py-2:not(.flex)");
        if (descBlock == null) {
            descBlock = detail.selectFirst(
                    ".border-primary.border-t-1\\5 .border-t-1\\.5.py-2");
        }
        if (descBlock != null) {
            String text = descBlock.ownText().trim();
            if (!text.isBlank()) {
                dto.setDescription(text);
            }
        }
    }

    void extractCostTable(Document detail, ListingDto dto) {
        for (Element row : detail.select(".flex.justify-between")) {
            String cls = row.attr("class");
            if (!(cls.contains("border-t-1") || cls.contains("border-b-1")) || !cls.contains("py-2")) continue;

            Elements divs = row.children();
            if (divs.size() < 2) continue;

            String label = divs.get(0).wholeText().trim().toLowerCase();
            String value = divs.get(1).wholeText().trim();

            if (label.contains("nettomiete")) {
                dto.setNetRent(parseMoney(value));
            } else if (label.contains("betriebskosten")) {
                dto.setOperatingCosts(parseMoney(value));
            } else if (label.equals("mwst.")) {
                dto.setVat(parseMoney(value));
            } else if (label.contains("gesamtkosten")) {
                if (dto.getPrice() == null) dto.setPrice(parseMoney(value));
            } else if (label.contains("kaution")) {
                dto.setDeposit(parseMoney(value));
            } else if (label.contains("verfügbar")) {
                dto.setAvailableFrom(value);
            } else if (label.contains("provision")) {
                dto.setProvision(value);
            } else if (label.equals("baujahr")) {
                try { dto.setBuildYear(Integer.parseInt(value)); } catch (NumberFormatException ignored) {}
            }
        }
    }

    void extractEnergyInfo(Document detail, ListingDto dto) {
        Elements rows = detail.select(
                ".border-primary.border-b-1\\.5.py-2 div, "
                        + ".border-primary.border-b-1\\5 .py-2 div");

        String fullText = rows.text();
        Matcher m = Pattern.compile("Heizbedarf:\\s*([0-9.,]+)\\s*kWh").matcher(fullText);
        if (m.find()) {
            try {
                dto.setHeatingDemand(Float.parseFloat(m.group(1).replace(",", ".")));
            } catch (NumberFormatException ignored) {}
        }
        m = Pattern.compile("fGEE-Wert:\\s*([0-9.,]+)").matcher(fullText);
        if (m.find()) {
            try {
                dto.setFgee(Float.parseFloat(m.group(1).replace(",", ".")));
            } catch (NumberFormatException ignored) {}
        }
    }

    void extract360View(Document detail, ListingDto dto) {
        Element threeSixty = detail.selectFirst("a[href*=\"matterport\"]");
        if (threeSixty != null) {
            dto.setHas360View(true);
            dto.setMatterportUrl(threeSixty.attr("href"));
        }
    }

    // --- helpers ---

    private BigDecimal parseMoney(String text) {
        if (text == null) return null;
        Matcher m = MONEY_PATTERN.matcher(text);
        if (m.find()) {
            String cleaned = m.group(1).replace(".", "").replace(",", ".");
            try {
                return new BigDecimal(cleaned);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Float extractFloat(String text, String keyword) {
        int idx = text.indexOf(keyword);
        if (idx < 0) return null;
        Matcher m = FLOAT_PATTERN.matcher(text.substring(idx));
        if (m.find()) {
            try {
                return Float.parseFloat(m.group(1).replace(",", "."));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal extractMoney(String text, String keyword) {
        int idx = text.indexOf(keyword);
        if (idx < 0) return null;
        Matcher m = MONEY_PATTERN.matcher(text.substring(idx));
        if (m.find()) {
            String cleaned = m.group(1).replace(".", "").replace(",", ".");
            try {
                return new BigDecimal(cleaned);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
