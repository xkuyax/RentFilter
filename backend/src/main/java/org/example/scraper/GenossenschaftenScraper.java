package org.example.scraper;

import org.example.entity.Source;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GenossenschaftenScraper extends AbstractScraper {

    private static final String SEARCH_URL = "https://genossenschaften.immo/";

    @Override
    public Source getSource() {
        return Source.GENOSSENSCHAFTEN;
    }

    @Override
    public List<ListingDto> scrape() {
        log.info("Genossenschaften: not yet implemented, returning empty");
        return List.of();
    }
}
