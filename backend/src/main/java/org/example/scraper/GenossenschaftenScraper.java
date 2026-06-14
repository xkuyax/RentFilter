package org.example.scraper;

import org.example.entity.Source;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GenossenschaftenScraper extends AbstractScraper {

    private static final String SEARCH_URL = "https://genossenschaften.immo/";

    @Override
    public Source getSource() {
        return Source.GENOSSENSCHAFTEN;
    }

    @Override
    public List<ListingDto> scrape() throws Exception {
        List<ListingDto> results = new ArrayList<>();
        var doc = fetch(SEARCH_URL);

        // TODO: Parse listing cards from genossenschaften.immo
        // This site may require JavaScript rendering or an API call
        // Addresses here lack geolocation — will need Nominatim fallback

        log.info("Genossenschaften: fetched page, listings found: {}", results.size());
        return results;
    }
}
