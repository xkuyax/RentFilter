package org.example.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:sql/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ListingMapperTest {

    @Autowired
    private ListingMapper mapper;

    @BeforeEach
    void setUp() {
        mapper.deleteAll();

        mapper.insert(makeListing("Cheap small apartment", Source.WILLHABEN, "500", 1.0f, 30.0f,
                "Testgasse 1, Graz", "https://example.com/1"));
        mapper.insert(makeListing("Medium apartment", Source.GRAWE, "800", 2.0f, 55.0f,
                "Testgasse 2, Graz", "https://example.com/2"));
        mapper.insert(makeListing("Large apartment", Source.GRAWE, "1200", 3.0f, 80.0f,
                "Testgasse 3, Graz", "https://example.com/3"));
    }

    @Test
    void filterBySource() {
        long count = mapper.countFiltered("GRAWE", null, null, null, null);
        assertThat(count).isEqualTo(2);

        List<Listing> results = mapper.findAllFiltered("GRAWE", null, null, null, null, 0, 10);
        assertThat(results).hasSize(2);
    }

    @Test
    void filterByMinPrice() {
        long count = mapper.countFiltered(null, new BigDecimal("800"), null, null, null);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void filterByMaxPrice() {
        long count = mapper.countFiltered(null, null, new BigDecimal("600"), null, null);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void filterByPriceRange() {
        long count = mapper.countFiltered(null, new BigDecimal("500"), new BigDecimal("900"), null, null);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void filterByMinRooms() {
        long count = mapper.countFiltered(null, null, null, 2.0f, null);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void filterByMinArea() {
        long count = mapper.countFiltered(null, null, null, null, 55.0f);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void filterCombined() {
        long count = mapper.countFiltered("GRAWE", null, new BigDecimal("900"), 2.0f, null);
        assertThat(count).isEqualTo(1);

        List<Listing> results = mapper.findAllFiltered("GRAWE", null, new BigDecimal("900"), 2.0f, null, 0, 10);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Medium apartment");
    }

    @Test
    void noFilterReturnsAll() {
        long count = mapper.countFiltered(null, null, null, null, null);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void findByUrl() {
        var found = mapper.findByUrl("https://example.com/1");
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Cheap small apartment");
    }

    @Test
    void existsByUrl() {
        assertThat(mapper.existsByUrl("https://example.com/1")).isTrue();
        assertThat(mapper.existsByUrl("https://nonexistent.com")).isFalse();
    }

    @Test
    void insertAndFindById() {
        mapper.insert(makeListing("New place", Source.WILLHABEN, "600", 1.5f, 40.0f,
                "Neugasse 1, Graz", "https://example.com/new"));

        var found = mapper.findByUrl("https://example.com/new");
        assertThat(found).isPresent();

        var byId = mapper.findById(found.get().getId());
        assertThat(byId).isPresent();
        assertThat(byId.get().getTitle()).isEqualTo("New place");
    }

    @Test
    void findAllWithCoords_onlyReturnsWithCoordinates() {
        Listing withCoords = makeListing("With coords", Source.GRAWE, "500", 1f, 30f,
                "A", "https://example.com/coords");
        withCoords.setLatitude(47.0);
        withCoords.setLongitude(15.0);
        mapper.insert(withCoords);

        Listing noCoords = makeListing("No coords", Source.GRAWE, "500", 1f, 30f,
                "B", "https://example.com/nocoords");
        mapper.insert(noCoords);

        var result = mapper.findAllWithCoords();
        assertThat(result).hasSize(1).anyMatch(l -> l.getTitle().equals("With coords"));
    }

    private Listing makeListing(String title, Source source, String price,
                                 Float rooms, Float area, String address, String url) {
        Listing l = new Listing();
        l.setTitle(title);
        l.setSource(source);
        l.setPrice(new BigDecimal(price));
        l.setRooms(rooms);
        l.setArea(area);
        l.setAddress(address);
        l.setUrl(url);
        return l;
    }
}
