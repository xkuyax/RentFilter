package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=1";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${scraping.user-agent}")
    private String userAgent;

    public record Coords(double lat, double lng) {}

    public Coords geocode(String address) {
        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(NOMINATIM_URL.formatted(encoded)))
                    .header("User-Agent", userAgent)
                    .GET()
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Nominatim returned status {} for address: {}", response.statusCode(), address);
                return null;
            }

            JsonNode root = mapper.readTree(response.body());
            if (root.isArray() && !root.isEmpty()) {
                JsonNode first = root.get(0);
                double lat = first.get("lat").asDouble();
                double lng = first.get("lon").asDouble();
                return new Coords(lat, lng);
            }
        } catch (Exception e) {
            log.warn("Geocoding failed for address: {}", address, e);
        }
        return null;
    }
}
