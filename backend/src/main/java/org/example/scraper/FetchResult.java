package org.example.scraper;

import org.jsoup.nodes.Document;

public record FetchResult(Document document, boolean cached) {
}
