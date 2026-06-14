package org.example.debug;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ScraperDebugger {
    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "https://www.grawewohnen.at/";

        System.out.println("Fetching: " + url);
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .timeout(30_000)
                .get();

        System.out.println("=== HTML ===\n");
        System.out.println(doc.html());
    }
}
