package org.example.scraper;

import org.example.entity.Source;

import java.util.function.Consumer;

public interface ListingScraper {

    Source getSource();

    void scrape(Consumer<ListingDto> onListing) throws Exception;
}
