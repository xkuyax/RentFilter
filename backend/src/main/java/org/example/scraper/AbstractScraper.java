package org.example.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.Random;

public abstract class AbstractScraper implements ListingScraper {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value("${scraping.user-agent}")
    protected String userAgent;

    @Value("${scraping.request-delay-ms}")
    protected int requestDelayMs;

    private final Random random = new Random();

    protected Document fetch(String url) throws Exception {
        Thread.sleep(requestDelayMs + random.nextInt(1000));
        return Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(30_000)
                .get();
    }

    protected String parsePrice(String text) {
        if (text == null) return null;
        return text.replaceAll("[^0-9,.]", "")
                .replace(",", ".")
                .replaceAll("\\.(?=.*\\.)", "");
    }

    protected Float parseFloat(String text) {
        if (text == null) return null;
        try {
            String cleaned = text.replace(",", ".").replaceAll("[^0-9.]", "");
            return Float.parseFloat(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
