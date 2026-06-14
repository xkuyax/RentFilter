package org.example.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.Random;

public abstract class AbstractScraper implements ListingScraper {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value("${scraping.user-agent}")
    protected String userAgent;

    @Value("${scraping.request-delay-ms}")
    protected int requestDelayMs;

    protected HtmlCache cache;

    @Autowired
    public void setCache(HtmlCache cache) {
        this.cache = cache;
    }

    private final Random random = new Random();

    protected Document fetch(String url) throws Exception {
        return fetch(url, Map.of());
    }

    protected Document fetch(String url, Map<String, String> headers) throws Exception {
        if (cache != null && cache.isEnabled()) {
            String key = url + "|" + headers.hashCode();
            String cached = cache.get(key);
            if (cached != null) {
                log.debug("Cache hit for {}", url);
                return Jsoup.parse(cached);
            }
        }

        Thread.sleep(requestDelayMs + random.nextInt(1000));
        var conn = Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(30_000);
        for (var entry : headers.entrySet()) {
            conn.header(entry.getKey(), entry.getValue());
        }
        Document doc = conn.get();

        if (cache != null && cache.isEnabled()) {
            String key = url + "|" + headers.hashCode();
            cache.put(key, doc.outerHtml());
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
