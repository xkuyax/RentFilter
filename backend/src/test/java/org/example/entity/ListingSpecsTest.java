package org.example.entity;

import org.example.service.ListingSpecs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class ListingSpecsTest {

    @Autowired
    private ListingRepository repository;

    @BeforeEach
    void setUp() {
        Listing l1 = new Listing();
        l1.setSource(Source.WILLHABEN);
        l1.setTitle("Cheap small apartment");
        l1.setPrice(new BigDecimal("500"));
        l1.setRooms(1.0f);
        l1.setArea(30.0f);
        l1.setAddress("Testgasse 1, Graz");
        l1.setUrl("https://example.com/1");

        Listing l2 = new Listing();
        l2.setSource(Source.GRAWE);
        l2.setTitle("Medium apartment");
        l2.setPrice(new BigDecimal("800"));
        l2.setRooms(2.0f);
        l2.setArea(55.0f);
        l2.setAddress("Testgasse 2, Graz");
        l2.setUrl("https://example.com/2");

        Listing l3 = new Listing();
        l3.setSource(Source.GRAWE);
        l3.setTitle("Large apartment");
        l3.setPrice(new BigDecimal("1200"));
        l3.setRooms(3.0f);
        l3.setArea(80.0f);
        l3.setAddress("Testgasse 3, Graz");
        l3.setUrl("https://example.com/3");

        repository.save(l1);
        repository.save(l2);
        repository.save(l3);
    }

    @Test
    void filterBySource() {
        Specification<Listing> spec = ListingSpecs.filter(Source.GRAWE, null, null, null, null);
        Page<Listing> result = repository.findAll(spec, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void filterByMinPrice() {
        Specification<Listing> spec = ListingSpecs.filter(null, new BigDecimal("800"), null, null, null);
        Page<Listing> result = repository.findAll(spec, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void filterByMaxPrice() {
        Specification<Listing> spec = ListingSpecs.filter(null, null, new BigDecimal("600"), null, null);
        Page<Listing> result = repository.findAll(spec, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void filterByPriceRange() {
        Specification<Listing> spec = ListingSpecs.filter(null, new BigDecimal("500"), new BigDecimal("900"), null, null);
        Page<Listing> result = repository.findAll(spec, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void filterByMinRooms() {
        Specification<Listing> spec = ListingSpecs.filter(null, null, null, 2.0f, null);
        Page<Listing> result = repository.findAll(spec, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void filterByMinArea() {
        Specification<Listing> spec = ListingSpecs.filter(null, null, null, null, 55.0f);
        Page<Listing> result = repository.findAll(spec, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void filterCombined() {
        Specification<Listing> spec = ListingSpecs.filter(Source.GRAWE, null, new BigDecimal("900"), 2.0f, null);
        Page<Listing> result = repository.findAll(spec, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Medium apartment");
    }

    @Test
    void noFilterReturnsAll() {
        Specification<Listing> spec = ListingSpecs.filter(null, null, null, null, null);
        Page<Listing> result = repository.findAll(spec, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(3);
    }
}
