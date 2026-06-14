package org.example.service;

import org.example.entity.Listing;
import org.example.entity.ListingRepository;
import org.example.entity.Source;
import org.example.scraper.ListingDto;
import org.example.scraper.ListingScraper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ScraperServiceTest {

    @Autowired
    private ListingRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void savesNewListings() {
        assertTrue(repository.findAll().isEmpty());

        var dto = createDto("1", "Test Apartment", new BigDecimal("500"), 2.0f, 50.0f,
                "Testgasse 1, Graz", "https://example.com/1");
        dto.setNetRent(new BigDecimal("300"));
        dto.setOperatingCosts(new BigDecimal("150"));
        dto.setVat(new BigDecimal("50"));
        dto.setDeposit(new BigDecimal("1500"));
        dto.setAvailableFrom("ab sofort");
        dto.setProvision("Nein");
        dto.setBuildYear(1990);
        dto.setHeatingDemand(80.0f);
        dto.setFgee(1.5f);
        dto.setBenefits(List.of("Balkon", "Aufzug"));
        dto.setImageUrls(List.of("https://img.example.com/1.jpg", "https://img.example.com/2.jpg"));
        dto.setThumbnailUrl("https://img.example.com/thumb.jpg");
        dto.setHas360View(true);
        dto.setMatterportUrl("https://matterport.com/1");

        List<ListingScraper> testScrapers = List.of(new TestScraper(Source.GRAWE, List.of(dto)));
        var svc = new ScraperService(testScrapers, repository, null);
        svc.fetchAll();

        assertEquals(1, repository.count());
        Listing saved = repository.findByUrl("https://example.com/1").orElseThrow();
        assertEquals("Test Apartment", saved.getTitle());
        assertEquals(Source.GRAWE, saved.getSource());
        assertEquals(new BigDecimal("500"), saved.getPrice());
        assertEquals(new BigDecimal("300"), saved.getNetRent());
        assertEquals(new BigDecimal("150"), saved.getOperatingCosts());
        assertEquals(new BigDecimal("50"), saved.getVat());
        assertEquals(new BigDecimal("1500"), saved.getDeposit());
        assertEquals("ab sofort", saved.getAvailableFrom());
        assertEquals("Nein", saved.getProvision());
        assertEquals(1990, saved.getBuildYear());
        assertEquals(80.0f, saved.getHeatingDemand());
        assertEquals(1.5f, saved.getFgee());
        assertTrue(saved.getBenefits().contains("Balkon"));
        assertTrue(saved.getImageUrls().contains("img.example.com"));
        assertEquals("https://img.example.com/thumb.jpg", saved.getThumbnailUrl());
        assertTrue(saved.isHas360View());
        assertEquals("https://matterport.com/1", saved.getMatterportUrl());
    }

    @Test
    void deduplicatesByUrl() {
        var dto = createDto("1", "Test", new BigDecimal("500"), 2.0f, 50.0f,
                "Testgasse 1, Graz", "https://example.com/1");

        List<ListingScraper> testScrapers = List.of(new TestScraper(Source.WILLHABEN, List.of(dto, dto)));
        var svc = new ScraperService(testScrapers, repository, null);
        svc.fetchAll();

        assertEquals(1, repository.count());
    }

    @Test
    void skipsExistingUrls() {
        Listing existing = new Listing();
        existing.setSource(Source.GRAWE);
        existing.setTitle("Already saved");
        existing.setAddress("Testgasse 1");
        existing.setUrl("https://example.com/1");
        repository.save(existing);

        var dto = createDto("1", "Updated title", new BigDecimal("600"), 3.0f, 60.0f,
                "Testgasse 1, Graz", "https://example.com/1");

        List<ListingScraper> testScrapers = List.of(new TestScraper(Source.GRAWE, List.of(dto)));
        var svc = new ScraperService(testScrapers, repository, null);
        svc.fetchAll();

        assertEquals(1, repository.count());
        Listing found = repository.findByUrl("https://example.com/1").orElseThrow();
        assertEquals("Already saved", found.getTitle());
    }

    private ListingDto createDto(String extId, String title, BigDecimal price,
                                  Float rooms, Float area, String address, String url) {
        ListingDto dto = new ListingDto();
        dto.setExternalId(extId);
        dto.setTitle(title);
        dto.setPrice(price);
        dto.setRooms(rooms);
        dto.setArea(area);
        dto.setAddress(address);
        dto.setUrl(url);
        return dto;
    }

    private record TestScraper(Source source, List<ListingDto> dtos) implements ListingScraper {
        @Override
        public Source getSource() { return source; }
        @Override
        public List<ListingDto> scrape() { return dtos; }
    }
}
