package org.example.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AddressNormalizerTest {

    private final AddressNormalizer normalizer = new AddressNormalizer();

    @Test
    void simpleAddress_returnsOnlyOriginal() {
        List<String> candidates = normalizer.normalize("Friedrichgasse 3, 8010 Graz");
        assertThat(candidates).containsExactly("Friedrichgasse 3, 8010 Graz");
    }

    @Test
    void cornerBuilding_slashBetweenStreets_returnsEachSide() {
        List<String> candidates = normalizer.normalize("Humboldtstraße 1/Grabenstraße 2, 8010 Graz");
        assertThat(candidates).containsExactly(
                "Humboldtstraße 1/Grabenstraße 2, 8010 Graz",
                "Humboldtstraße 1, 8010 Graz",
                "Grabenstraße 2, 8010 Graz"
        );
    }

    @Test
    void cornerBuilding_differentStreetsBrokenBySlash() {
        List<String> candidates = normalizer.normalize("Grieskai 10/Griesgasse 11, 8020 Graz");
        assertThat(candidates).contains(
                "Grieskai 10, 8020 Graz",
                "Griesgasse 11, 8020 Graz"
        );
    }

    @Test
    void multiNumber_commaSeparated() {
        List<String> candidates = normalizer.normalize("Mandellstraße 3, 3a, 8010 Graz");
        assertThat(candidates).contains("Mandellstraße 3, 8010 Graz");
        assertThat(candidates).contains("Mandellstraße 3a, 8010 Graz");
    }

    @Test
    void multiNumber_commaSeparatedMultiple() {
        List<String> candidates = normalizer.normalize("Lendplatz 27, 28, 8020 Graz");
        assertThat(candidates).contains("Lendplatz 27, 8020 Graz");
        assertThat(candidates).contains("Lendplatz 28, 8020 Graz");
    }

    @Test
    void multiNumber_slashBetweenNumbers() {
        List<String> candidates = normalizer.normalize("Sandgasse 43/45/45a, 8010 Graz");
        assertThat(candidates).contains("Sandgasse 43, 8010 Graz");
        assertThat(candidates).contains("Sandgasse 45, 8010 Graz");
        assertThat(candidates).contains("Sandgasse 45a, 8010 Graz");
    }

    @Test
    void noHouseNumber_appendsOne() {
        List<String> candidates = normalizer.normalize("Idlhof, 8020 Graz");
        assertThat(candidates).containsExactly(
                "Idlhof, 8020 Graz",
                "Idlhof 1, 8020 Graz"
        );
    }

    @Test
    void originalIsAlwaysFirst() {
        List<String> candidates = normalizer.normalize("Humboldtstraße 1/Grabenstraße 2, 8010 Graz");
        assertThat(candidates.get(0)).isEqualTo("Humboldtstraße 1/Grabenstraße 2, 8010 Graz");
    }

    @Test
    void griesplatzAddress_simple() {
        List<String> candidates = normalizer.normalize("Griesplatz 36, 8020 Graz");
        assertThat(candidates).containsExactly("Griesplatz 36, 8020 Graz");
    }
}
