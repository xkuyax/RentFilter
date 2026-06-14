package org.example.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Source;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WillhabenScraper extends AbstractScraper {

    private static final String BASE = "https://www.willhaben.at";
    private static final String SEARCH_URL = BASE + "/iad/immobilien/mietwohnungen/steiermark/graz";
    private static final Pattern JSON_PATTERN =
            Pattern.compile("<script[^>]*type=\"application/json\"[^>]*>(.*?)</script>", Pattern.DOTALL);
    private static final Pattern COORDS_PATTERN = Pattern.compile("([0-9.]+),([0-9.]+)");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Source getSource() {
        return Source.WILLHABEN;
    }

    @Override
    public List<ListingDto> scrape() throws Exception {
        List<ListingDto> results = new ArrayList<>();
        int page = 1;
        int totalPages = 1;

        while (page <= totalPages) {
            String url = SEARCH_URL + "?page=" + page + "&rows=30";
            log.info("Willhaben: fetching page {}/{}", page, totalPages);

            FetchResult fr = fetch(url);
            Document doc = fr.document();

            JsonNode root = extractJson(doc);
            if (root == null) {
                log.warn("Willhaben: no JSON data on page {}", page);
                break;
            }

            JsonNode searchResult = root.path("props").path("pageProps").path("searchResult");

            if (page == 1) {
                int rowsFound = searchResult.path("rowsFound").asInt();
                int rowsReturned = searchResult.path("rowsReturned").asInt();
                if (rowsReturned > 0) {
                    totalPages = (int) Math.ceil((double) rowsFound / rowsReturned);
                }
            }

            JsonNode ads = searchResult.path("advertSummaryList").path("advertSummary");
            if (!ads.isArray() || ads.isEmpty()) {
                log.info("Willhaben: no more ads on page {}", page);
                break;
            }
            for (JsonNode ad : ads) {
                ListingDto dto = parseAd(ad);
                if (dto != null) results.add(dto);
            }

            if (page >= totalPages) break;
            page++;
            if (!fr.cached()) Thread.sleep(requestDelayMs);
        }

        log.info("Willhaben: {} listings parsed across {} pages", results.size(), totalPages);
        return results;
    }

    ListingDto parseAd(JsonNode ad) {
        try {
            Map<String, String> attrs = toAttributeMap(ad.path("attributes").path("attribute"));

            ListingDto dto = new ListingDto();
            dto.setExternalId(ad.path("id").asText());
            dto.setTitle(attrs.getOrDefault("HEADING", ad.path("description").asText()));

            // Address: city + postal code
            String location = attrs.get("LOCATION");
            String postcode = attrs.get("POSTCODE");
            if (location != null && postcode != null) {
                dto.setAddress(postcode + " " + location);
            } else if (location != null) {
                dto.setAddress(location);
            }

            // URL
            String seoUrl = attrs.get("SEO_URL");
            if (seoUrl != null) {
                dto.setUrl(BASE + "/iad/" + seoUrl);
            } else {
                dto.setUrl(SEARCH_URL);
            }

            // Price
            String priceStr = attrs.get("PRICE");
            if (priceStr != null) {
                dto.setPrice(new BigDecimal(priceStr));
            }

            // Area
            String areaStr = attrs.get("ESTATE_SIZE/LIVING_AREA");
            if (areaStr == null) areaStr = attrs.get("ESTATE_SIZE");
            if (areaStr != null) {
                dto.setArea(Float.parseFloat(areaStr));
            }

            // Rooms
            String roomsStr = attrs.get("NUMBER_OF_ROOMS");
            if (roomsStr != null) {
                dto.setRooms(Float.parseFloat(roomsStr));
            }

            // Coordinates
            String coordsStr = attrs.get("COORDINATES");
            if (coordsStr != null) {
                Matcher m = COORDS_PATTERN.matcher(coordsStr);
                if (m.find()) {
                    dto.setLatitude(Double.parseDouble(m.group(1)));
                    dto.setLongitude(Double.parseDouble(m.group(2)));
                }
            }

            // Images
            List<String> imageUrls = new ArrayList<>();
            JsonNode imgList = ad.path("advertImageList").path("advertImage");
            for (JsonNode img : imgList) {
                String imgUrl = img.path("referenceImageUrl").asText(null);
                if (imgUrl == null) imgUrl = img.path("mainImageUrl").asText(null);
                if (imgUrl != null) imageUrls.add(imgUrl);
            }
            if (!imageUrls.isEmpty()) dto.setImageUrls(imageUrls);

            // Thumbnail
            if (!imageUrls.isEmpty()) {
                dto.setThumbnailUrl(imageUrls.get(0));
            }

            // Description
            String body = attrs.get("BODY_DYN");
            if (body != null) dto.setDescription(body);

            // Available
            String status = ad.path("advertStatus").path("id").asText();
            if ("active".equals(status)) dto.setAvailableFrom("sofort");

            // Build year
            String buildYear = attrs.get("CONSTRUCTION_YEAR");
            if (buildYear != null) {
                try { dto.setBuildYear(Integer.parseInt(buildYear)); } catch (NumberFormatException ignored) {}
            }

            // Provision
            String commission = attrs.get("COMMISSION");
            if (commission != null) dto.setProvision(commission);

            return dto;
        } catch (Exception e) {
            log.warn("Willhaben: failed to parse ad", e);
            return null;
        }
    }

    private Map<String, String> toAttributeMap(JsonNode attributes) {
        Map<String, String> map = new HashMap<>();
        if (attributes != null && attributes.isArray()) {
            for (JsonNode attr : attributes) {
                String name = attr.path("name").asText();
                JsonNode values = attr.path("values");
                if (!values.isEmpty()) {
                    map.put(name, values.get(0).asText());
                }
            }
        }
        return map;
    }

    private JsonNode extractJson(Document doc) {
        try {
            Matcher m = JSON_PATTERN.matcher(doc.outerHtml());
            if (m.find()) {
                return objectMapper.readTree(m.group(1));
            }
        } catch (Exception e) {
            log.warn("Willhaben: failed to extract JSON", e);
        }
        return null;
    }
}
