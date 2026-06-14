package org.example.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Random;

public abstract class AbstractScraper implements ListingScraper {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value("${scraping.user-agent}")
    protected String userAgent;

    @Value("${scraping.request-delay-ms}")
    protected int requestDelayMs;

    @Autowired
    protected HtmlCache cache;

    private final Random random = new Random();

    protected Document fetch(String url) throws Exception {
        if (cache != null && cache.isEnabled()) {
            String cached = cache.get(url);
            if (cached != null) {
                log.debug("Cache hit for {}", url);
                return Jsoup.parse(cached);
            }
        }

        Thread.sleep(requestDelayMs + random.nextInt(1000));
        Document doc = Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(30_000)
                .get();

        if (cache != null && cache.isEnabled()) {
            cache.put(url, doc.outerHtml());
        }

        return doc;
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
