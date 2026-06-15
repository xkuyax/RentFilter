package org.example.debug;

import org.example.scraper.GraweScraper;
import org.example.scraper.HtmlCache;
import org.example.scraper.ListingDto;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class PopulateCache {
    public static void main(String[] args) throws Exception {
        HtmlCache cache = new HtmlCache();
        setField(cache, "enabled", true);

        GraweScraper scraper = new GraweScraper();
        setSuperField(scraper, "userAgent",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        setSuperField(scraper, "requestDelayMs", 2000);
        setSuperField(scraper, "cache", cache);

        System.out.println("Fetching all Graz listings (this will take a while)...");
        long start = System.currentTimeMillis();
        List<ListingDto> results = new ArrayList<>();
        scraper.scrape(results::add);
        long elapsed = (System.currentTimeMillis() - start) / 1000;

        System.out.println("\nDone! " + results.size() + " listings fetched in " + elapsed + "s");
        System.out.println("HTML cache populated in: data/html-cache/");

        // Summary of what we got
        int withAddress = 0, withImages = 0, withBenefits = 0, withCosts = 0, with360 = 0;
        for (ListingDto dto : results) {
            if (dto.getAddress() != null) {
                withAddress++;
            }
            if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
                withImages++;
            }
            if (dto.getBenefits() != null && !dto.getBenefits().isEmpty()) {
                withBenefits++;
            }
            if (dto.getNetRent() != null) {
                withCosts++;
            }
            if (dto.isHas360View()) {
                with360++;
            }
        }

        System.out.println("With address: " + withAddress + "/" + results.size());
        System.out.println("With images:  " + withImages + "/" + results.size());
        System.out.println("With benefits:" + withBenefits + "/" + results.size());
        System.out.println("With costs:   " + withCosts + "/" + results.size());
        System.out.println("With 360°:    " + with360 + "/" + results.size());

        // Show first 3 listings
        System.out.println("\n=== First 3 listings ===");
        for (int i = 0; i < Math.min(3, results.size()); i++) {
            ListingDto dto = results.get(i);
            System.out.println("\n#" + (i + 1));
            System.out.println("  Title:    " + dto.getTitle());
            System.out.println("  Price:    " + dto.getPrice());
            System.out.println("  Address:  " + dto.getAddress());
            System.out.println("  Rooms:    " + dto.getRooms());
            System.out.println("  Area:     " + dto.getArea() + " m²");
            System.out.println("  Deposit:  " + dto.getDeposit());
            System.out.println("  Images:   " + (dto.getImageUrls() != null ? dto.getImageUrls().size() : 0));
            System.out.println("  Benefits: " + dto.getBenefits());
            System.out.println("  360°:     " + dto.isHas360View());
        }
    }

    private static void setSuperField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getSuperclass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
