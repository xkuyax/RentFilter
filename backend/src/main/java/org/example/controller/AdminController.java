package org.example.controller;

import org.example.entity.ListingMapper;
import org.example.service.GeocodingService;
import org.example.service.ScraperService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ScraperService scraperService;
    private final GeocodingService geocodingService;
    private final ListingMapper listingMapper;

    public AdminController(ScraperService scraperService,
                           GeocodingService geocodingService,
                           ListingMapper listingMapper) {
        this.scraperService = scraperService;
        this.geocodingService = geocodingService;
        this.listingMapper = listingMapper;
    }

    @PostMapping("/scrape")
    public Map<String, Object> triggerScrape() {
        scraperService.fetchAll();
        return Map.of("status", "ok", "message", "scrape triggered");
    }

    @PostMapping("/geocode")
    public Map<String, Object> triggerGeocode() {
        int filled = geocodingService.fillMissingCoordinates(listingMapper);
        return Map.of("status", "ok", "filled", filled);
    }
}
