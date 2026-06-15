package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Listing;
import org.example.entity.ListingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=1";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AddressNormalizer normalizer;
    private Instant lastRequest = Instant.EPOCH;

    @Value("${scraping.user-agent}")
    private String userAgent;

    public GeocodingService(AddressNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public record Coords(double lat, double lng) {
    }

    public Coords geocode(String address) {
        List<String> candidates = normalizer.normalize(address);
        for (String candidate : candidates) {
            Coords result = tryGeocode(candidate);
            if (result != null) {
                if (!candidate.equals(address)) {
                    log.info("Geocoded via fallback: \"{}\" -> \"{}\"", address, candidate);
                }
                return result;
            }
        }
        log.warn("All geocoding candidates failed for: {}", address);
        return null;
    }

    private Coords tryGeocode(String query) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                rateLimit();
                String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
                var request = HttpRequest.newBuilder()
                        .uri(URI.create(NOMINATIM_URL.formatted(encoded)))
                        .header("User-Agent", userAgent)
                        .GET()
                        .build();

                var response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 429) {
                    log.warn("Nominatim 429, sleeping 60s...");
                    Thread.sleep(60_000);
                    continue;
                }

                if (response.statusCode() != 200) {
                    return null;
                }

                String body = response.body();
                if (body.startsWith("<")) {
                    log.warn("Nominatim returned HTML instead of JSON (likely rate limited)");
                    Thread.sleep(60_000);
                    continue;
                }

                JsonNode root = mapper.readTree(body);
                if (root.isArray() && !root.isEmpty()) {
                    JsonNode first = root.get(0);
                    double lat = first.get("lat").asDouble();
                    double lng = first.get("lon").asDouble();
                    return new Coords(lat, lng);
                }
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public int fillMissingCoordinates(ListingMapper mapper) {
        List<Listing> listings = mapper.findAllWithCoords();
        List<Long> withCoords = listings.stream().map(Listing::getId).toList();

        List<Listing> all = mapper.findAllFiltered(null, null, null, null, null, 0, Integer.MAX_VALUE);
        int filled = 0;
        for (Listing listing : all) {
            if (withCoords.contains(listing.getId())) {
                continue;
            }
            if (listing.getAddress() == null || listing.getAddress().isBlank()) {
                continue;
            }

            Coords coords = geocode(listing.getAddress());
            if (coords != null) {
                listing.setLatitude(coords.lat());
                listing.setLongitude(coords.lng());
                mapper.updateCoordinates(listing);
                filled++;
            }
        }
        log.info("Filled coordinates for {}/{} missing listings", filled, all.size() - withCoords.size());
        return filled;
    }

    private synchronized void rateLimit() {
        long elapsed = Instant.now().toEpochMilli() - lastRequest.toEpochMilli();
        if (elapsed < 1100) {
            try {
                Thread.sleep(1100 - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequest = Instant.now();
    }
}
