package org.example.scraper;

import org.example.entity.Source;
import java.util.List;

public interface ListingScraper {

    Source getSource();

    List<ListingDto> scrape() throws Exception;
}
