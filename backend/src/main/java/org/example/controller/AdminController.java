package org.example.controller;

import org.example.service.ScraperService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ScraperService scraperService;

    public AdminController(ScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @PostMapping("/scrape")
    public Map<String, Object> triggerScrape() {
        scraperService.fetchAll();
        return Map.of("status", "ok", "message", "scrape triggered");
    }
}
