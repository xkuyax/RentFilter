package org.example.scraper;

import org.example.entity.Source;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
    public List<ListingDto> scrape() throws Exception {
        List<ListingDto> results = new ArrayList<>();
        var doc = fetch(SEARCH_URL);

        // TODO: Parse listing cards from willhaben search results
        // Each listing is typically in a div with data-testid="search-result-entry"
        // Extract: title, price, rooms, area, address, url

        log.info("Willhaben: fetched page, listings found: {}", results.size());
        return results;
    }
}
