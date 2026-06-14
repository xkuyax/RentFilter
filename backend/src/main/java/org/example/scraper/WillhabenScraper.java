package org.example.scraper;

import org.example.entity.Source;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WillhabenScraper extends AbstractScraper {

    private static final String SEARCH_URL =
            "https://www.willhaben.at/iad/immobilien/mietwohnungen/steiermark/graz";

    @Override
    public Source getSource() {
        return Source.WILLHABEN;
    }

    @Override
    public List<ListingDto> scrape() {
        log.info("Willhaben: not yet implemented, returning empty");
        return List.of();
    }
}
