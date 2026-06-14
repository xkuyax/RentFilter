package org.example.service;

import jakarta.persistence.criteria.Predicate;
import org.example.entity.Listing;
import org.example.entity.Source;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ListingSpecs {

    public static Specification<Listing> filter(Source source, BigDecimal minPrice, BigDecimal maxPrice,
                                                 Float minRooms, Float minArea) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (minRooms != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rooms"), minRooms));
            }
            if (minArea != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), minArea));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
