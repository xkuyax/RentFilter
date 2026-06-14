package org.example.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Source;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

        // Enrich from detail pages
        boolean previousCached = true;
        for (ListingDto dto : results) {
            boolean cached = enrichFromDetailPage(dto);
            if (!cached && !previousCached) Thread.sleep(requestDelayMs);
            previousCached = cached;
        }

        return results;
    }

    ListingDto parseAd(JsonNode ad) {
        try {
            Map<String, String> attrs = toAttributeMap(ad.path("attributes").path("attribute"));

            ListingDto dto = new ListingDto();
            dto.setExternalId(ad.path("id").asText());
            dto.setSelfLink(ad.path("selfLink").asText(null));
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
                if (imgUrl != null) {
                    imgUrl = imgUrl.replace("_n.", ".").replace("_hoved.", ".").replace("_thumb.", ".");
                    imageUrls.add(imgUrl);
                }
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

    boolean enrichFromDetailPage(ListingDto dto) {
        if (dto.getUrl() == null || dto.getUrl().equals(SEARCH_URL)) return true;

        try {
            FetchResult fr = fetch(dto.getUrl());
            Document doc = fr.document();

            // Extract all visible text from detail sections
            StringBuilder fullText = new StringBuilder();
            for (String heading : List.of("Objektbeschreibung", "Objektinformationen",
                    "Ausstattung und Freiflächen", "Ausstattung", "Flächen",
                    "Preis und Detailinformation", "Preisinformation",
                    "Zusatzinformationen", "Energieausweis")) {
                Element el = doc.selectFirst("h2:containsOwn(" + heading + ")");
                if (el == null) {
                    el = doc.selectFirst("h3:containsOwn(" + heading + ")");
                }
                if (el != null) {
                    Element next = el.nextElementSibling();
                    if (next != null) {
                        String text;
                        if (next.select("ul > li").size() >= 2) {
                            text = extractAttributeList(next);
                        } else {
                            text = extractStructuredText(next);
                        }
                        if (text.length() > 5) {
                            fullText.append("--- ").append(heading).append(" ---\n")
                                    .append(text).append("\n\n");
                        }
                    }
                }
            }
            if (!fullText.isEmpty()) {
                String desc = fullText.toString()
                        .replaceAll("\\n{3,}", "\n\n")
                        .replaceAll(" {2,}", " ")
                        .trim();
                if (desc.length() > 20) dto.setDescription(desc);
            }

            // Try to extract floor plans from embedded JSON
            JsonNode root = extractJson(doc);
            if (root != null) {
                JsonNode fps = root.path("advertImageList").path("floorPlans");
                // Also try walking all nodes for advertImageList
                if (!fps.isArray() || fps.isEmpty()) {
                    JsonNode walked = findInJson(root, "advertImageList");
                    if (walked != null) fps = walked.path("floorPlans");
                }
                if (fps.isArray() && !fps.isEmpty()) {
                    List<String> floorPlans = new ArrayList<>();
                    for (JsonNode fp : fps) {
                        String fpUrl = fp.path("referenceImageUrl").asText(null);
                        if (fpUrl == null) fpUrl = fp.path("mainImageUrl").asText(null);
                        if (fpUrl != null) {
                            fpUrl = fpUrl.replace("_n.", ".").replace("_hoved.", ".").replace("_thumb.", ".");
                            floorPlans.add(fpUrl);
                        }
                    }
                    if (!floorPlans.isEmpty()) {
                        List<String> all = new ArrayList<>(
                                dto.getImageUrls() != null ? dto.getImageUrls() : List.of());
                        all.addAll(floorPlans);
                        dto.setImageUrls(all);
                    }
                }
            }

            return fr.cached();
        } catch (Exception e) {
            log.warn("Willhaben: failed to enrich detail {}", dto.getUrl(), e);
            return false;
        }
    }

    private String extractStructuredText(Element el) {
        StringBuilder sb = new StringBuilder();
        for (org.jsoup.nodes.Node node : el.childNodes()) {
            if (node instanceof Element child) {
                switch (child.tagName().toLowerCase()) {
                    case "br" -> sb.append("\n");
                    case "li" -> sb.append("\n").append(child.wholeText().trim());
                    case "ul", "ol" -> {
                        for (Element li : child.select("> li")) {
                            sb.append("\n").append(li.wholeText().trim());
                        }
                    }
                    case "p", "div" -> {
                        String t = extractStructuredText(child).trim();
                        if (!t.isBlank()) sb.append("\n").append(t);
                    }
                    default -> {
                        String t = extractStructuredText(child).trim();
                        if (!t.isBlank()) sb.append(" ").append(t);
                    }
                }
            } else if (node instanceof org.jsoup.nodes.TextNode) {
                sb.append(((org.jsoup.nodes.TextNode) node).getWholeText());
            }
        }
        return sb.toString().trim();
    }

    // Parses a <ul> attribute list where each <li> has label + value children
    private String extractAttributeList(Element container) {
        StringBuilder sb = new StringBuilder();
        for (Element li : container.select("ul > li")) {
            // Try spans first, then divs as direct children
            var children = li.select("> span");
            if (children.size() < 2) children = li.select("> div");
            if (children.size() >= 2) {
                String label = children.get(0).wholeText().trim();
                String value = children.get(1).wholeText().trim();
                if (!label.isBlank() && !value.isBlank()) {
                    sb.append(label).append(": ").append(value).append("\n");
                    continue;
                }
            }
            sb.append(li.wholeText().trim()).append("\n");
        }
        return sb.toString().trim();
    }

    private JsonNode findInJson(JsonNode node, String key) {
        if (node.isObject()) {
            if (node.has(key)) return node.get(key);
            var it = node.fields();
            while (it.hasNext()) {
                JsonNode found = findInJson(it.next().getValue(), key);
                if (found != null) return found;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findInJson(child, key);
                if (found != null) return found;
            }
        }
        return null;
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
