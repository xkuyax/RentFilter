package org.example.scheduler;

import org.example.service.ScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FetcherScheduler {

    private static final Logger log = LoggerFactory.getLogger(FetcherScheduler.class);

    private final ScraperService scraperService;

    public FetcherScheduler(ScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @Scheduled(cron = "${scraping.cron:0 0 * * * *}")
    public void fetchListings() {
        log.info("Scheduled fetch starting");
        scraperService.fetchAll();
        log.info("Scheduled fetch complete");
    }
}
