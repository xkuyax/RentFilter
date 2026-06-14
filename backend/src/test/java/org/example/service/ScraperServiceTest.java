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

import static org.assertj.core.api.Assertions.*;

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
        assertThat(repository.findAll()).isEmpty();

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

        assertThat(repository.count()).isEqualTo(1);
        Listing saved = repository.findByUrl("https://example.com/1").orElseThrow();

        assertThat(saved.getTitle()).isEqualTo("Test Apartment");
        assertThat(saved.getSource()).isEqualTo(Source.GRAWE);
        assertThat(saved.getPrice()).isEqualByComparingTo("500");
        assertThat(saved.getNetRent()).isEqualByComparingTo("300");
        assertThat(saved.getOperatingCosts()).isEqualByComparingTo("150");
        assertThat(saved.getVat()).isEqualByComparingTo("50");
        assertThat(saved.getDeposit()).isEqualByComparingTo("1500");
        assertThat(saved.getAvailableFrom()).isEqualTo("ab sofort");
        assertThat(saved.getProvision()).isEqualTo("Nein");
        assertThat(saved.getBuildYear()).isEqualTo(1990);
        assertThat(saved.getHeatingDemand()).isEqualTo(80.0f);
        assertThat(saved.getFgee()).isEqualTo(1.5f);
        assertThat(saved.getBenefits()).contains("Balkon");
        assertThat(saved.getImageUrls()).contains("img.example.com");
        assertThat(saved.getThumbnailUrl()).isEqualTo("https://img.example.com/thumb.jpg");
        assertThat(saved.isHas360View()).isTrue();
        assertThat(saved.getMatterportUrl()).isEqualTo("https://matterport.com/1");
    }

    @Test
    void deduplicatesByUrl() {
        var dto = createDto("1", "Test", new BigDecimal("500"), 2.0f, 50.0f,
                "Testgasse 1, Graz", "https://example.com/1");

        List<ListingScraper> testScrapers = List.of(new TestScraper(Source.WILLHABEN, List.of(dto, dto)));
        var svc = new ScraperService(testScrapers, repository, null);
        svc.fetchAll();

        assertThat(repository.count()).isEqualTo(1);
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

        assertThat(repository.count()).isEqualTo(1);
        Listing found = repository.findByUrl("https://example.com/1").orElseThrow();
        assertThat(found.getTitle()).isEqualTo("Already saved");
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
