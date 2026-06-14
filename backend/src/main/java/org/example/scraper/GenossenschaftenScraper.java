package org.example.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Source;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GenossenschaftenScraper extends AbstractScraper {

    private static final String BASE = "https://genossenschaften.immo";
    private static final String LIST_URL = BASE + "/immobilien/regionen/steiermark/graz-stadt/";
    private static final Pattern ROOMS_PATTERN = Pattern.compile("(\\d+)");
    private static final Pattern AREA_PATTERN = Pattern.compile("([0-9.,]+)\\s*m²");
    private static final Pattern PRICE_PATTERN = Pattern.compile("([0-9.,]+)\\s*€");
    private static final Pattern NUM_PATTERN = Pattern.compile("([0-9.,]+)");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Source getSource() {
        return Source.GENOSSENSCHAFTEN;
    }

    @Override
    public List<ListingDto> scrape() throws Exception {
        List<ListingDto> results = new ArrayList<>();
        int page = 1;
        int totalPages = 1;

        while (page <= totalPages) {
            if (page > 1) {
                Thread.sleep(requestDelayMs);
            }

            String url = page == 1 ? LIST_URL : LIST_URL + page + "/";
            log.info("Genossenschaften: fetching page {}/{}", page, totalPages);

            Document doc = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .timeout(30_000)
                    .get();

            if (page == 1) {
                Element pagination = doc.selectFirst("#residence-pagination-top .pagination");
                if (pagination != null) {
                    var pages = pagination.select("li.page-item:not(.active) a.page-link[href]");
                    for (Element p : pages) {
                        try {
                            int pn = Integer.parseInt(p.text().trim());
                            if (pn > totalPages) totalPages = pn;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            for (Element card : doc.select(".residence-teaser")) {
                ListingDto dto = parseCard(card);
                if (dto != null) {
                    results.add(dto);
                }
            }

            if (page >= totalPages) break;
            page++;
        }

        log.info("Genossenschaften: {} listings found across {} pages", results.size(), totalPages);

        for (ListingDto dto : results) {
            enrichFromDetailPage(dto);
            if (!isCacheEnabled()) Thread.sleep(requestDelayMs);
        }

        return results;
    }

    ListingDto parseCard(Element card) {
        try {
            Element link = card.selectFirst("a.residence-teaser-link");
            if (link == null) return null;

            String url = link.attr("href");
            if (!url.startsWith("http")) url = BASE + url;

            String extId = url.replaceAll(".*/(\\d+)/$", "$1");

            Element titleEl = card.selectFirst("h2[itemprop=name]");
            String title = titleEl != null ? titleEl.text().trim() : "";

            ListingDto dto = new ListingDto();
            dto.setExternalId(extId);
            dto.setTitle(title);
            dto.setUrl(url);

            // Image
            Element img = card.selectFirst("div.residence-teaser-img");
            if (img != null) {
                String style = img.attr("style");
                Matcher m = Pattern.compile("url\\('([^']+)'\\)").matcher(style);
                if (m.find()) dto.setThumbnailUrl(m.group(1));
            }

            // Extract from detail columns
            String cardText = card.text();

            Matcher rm = ROOMS_PATTERN.matcher(extractDetail(card, "Zimmer"));
            if (rm.find()) dto.setRooms(Float.parseFloat(rm.group(1)));

            Matcher am = AREA_PATTERN.matcher(cardText);
            if (am.find()) {
                dto.setArea(Float.parseFloat(am.group(1).replace(".", "").replace(",", ".")));
            }

            Matcher pm = PRICE_PATTERN.matcher(extractDetail(card, "Miete"));
            if (pm.find()) {
                String price = pm.group(1).replace(".", "").replace(",", ".");
                dto.setPrice(new BigDecimal(price));
            }

            // Eigenkapital
            String ek = extractDetail(card, "Eigenkapital");
            Matcher ekm = PRICE_PATTERN.matcher(ek);
            if (ekm.find()) {
                dto.setDeposit(new BigDecimal(ekm.group(1).replace(".", "").replace(",", ".")));
            }

            // Badges
            for (Element badge : card.select(".badge")) {
                String text = badge.text().trim();
                if (text.equals("Verfügbar")) dto.setAvailableFrom("sofort");
                else if (text.equals("Geplant")) dto.setAvailableFrom("geplant");
                else if (text.equals("In Bau")) dto.setAvailableFrom("in bau");
            }

            return dto;
        } catch (Exception e) {
            log.warn("Genossenschaften: failed to parse card", e);
            return null;
        }
    }

    private String extractDetail(Element card, String label) {
        for (Element div : card.select(".text-center")) {
            Element small = div.selectFirst("small");
            if (small != null && small.text().trim().equalsIgnoreCase(label)) {
                Element fs5 = div.selectFirst(".fs-5");
                if (fs5 != null) return fs5.text().trim();
            }
        }
        return "";
    }

    void enrichFromDetailPage(ListingDto dto) {
        try {
            Document detail = fetch(dto.getUrl());
            Element script = detail.selectFirst("script[type=\"application/ld+json\"]");
            if (script == null) return;

            JsonNode json = objectMapper.readTree(script.data());
            // Schema.org Apartment type
            JsonNode address = json.path("address");
            if (!address.isMissingNode()) {
                String street = address.path("streetAddress").asText(null);
                String zip = address.path("postalCode").asText(null);
                String city = address.path("addressLocality").asText(null);
                if (street != null && zip != null && city != null) {
                    dto.setAddress(street + ", " + zip + " " + city);
                }
            }

            if (dto.getLatitude() == null && json.has("latitude")) {
                dto.setLatitude(json.get("latitude").asDouble());
            }
            if (dto.getLongitude() == null && json.has("longitude")) {
                dto.setLongitude(json.get("longitude").asDouble());
            }

            // Fill in missing from schema
            if (dto.getRooms() == null && json.has("numberOfRooms")) {
                dto.setRooms((float) json.path("numberOfRooms").path("value").asDouble());
            }
            if (dto.getArea() == null && json.has("floorSize")) {
                dto.setArea((float) json.path("floorSize").path("value").asDouble());
            }

            // Benefits from categories
            List<String> benefits = new ArrayList<>();
            for (Element cat : detail.select(".list-group-item")) {
                String text = cat.text().trim();
                if (!text.isBlank() && !text.contains("Weitere") && text.length() < 50) {
                    benefits.add(text.split("\\d")[0].trim());
                }
            }
            if (!benefits.isEmpty()) dto.setBenefits(benefits);

            // Image gallery — check background-image and img tags
            List<String> images = new ArrayList<>();
            Pattern bgPattern = Pattern.compile("url\\('([^']+)'\\)");
            for (Element el : detail.select("[style*=\"background-image\"]")) {
                Matcher m = bgPattern.matcher(el.attr("style"));
                if (m.find() && !m.group(1).contains("favicon") && !m.group(1).contains("avatar")) {
                    images.add(m.group(1));
                }
            }
            for (Element img : detail.select("img.property-image, img[src*=\"wohn\"]")) {
                String src = img.attr("src");
                if (!src.isBlank()) images.add(src);
            }
            if (!images.isEmpty()) dto.setImageUrls(images);

        } catch (Exception e) {
            log.warn("Genossenschaften: failed to enrich {}", dto.getUrl(), e);
        }
    }

    private boolean isCacheEnabled() {
        return cache != null && cache.isEnabled();
    }
}
