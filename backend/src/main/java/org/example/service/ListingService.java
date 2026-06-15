package org.example.service;

import org.example.entity.Listing;
import org.example.entity.ListingMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ListingService {

    private final ListingMapper listingMapper;

    public ListingService(ListingMapper listingMapper) {
        this.listingMapper = listingMapper;
    }

    public List<Listing> findListings(String source, BigDecimal minPrice, BigDecimal maxPrice,
                                      Float minRooms, Float minArea,
                                      int offset, int limit) {
        return listingMapper.findAllFiltered(source, minPrice, maxPrice, minRooms, minArea, offset, limit);
    }

    public long countFiltered(String source, BigDecimal minPrice, BigDecimal maxPrice,
                              Float minRooms, Float minArea) {
        return listingMapper.countFiltered(source, minPrice, maxPrice, minRooms, minArea);
    }

    public Optional<Listing> findById(Long id) {
        return listingMapper.findById(id);
    }

    public long countAll() {
        return listingMapper.count();
    }

    public List<Listing> findAllWithCoords() {
        return listingMapper.findAllWithCoords();
    }

    public List<Listing> findAllFilteredWithCoords(String source, BigDecimal minPrice,
                                                   BigDecimal maxPrice, Float minRooms,
                                                   Float minArea, Float maxPricePerSqm) {
        return listingMapper.findAllFilteredWithCoords(
                source, minPrice, maxPrice, minRooms, minArea, maxPricePerSqm);
    }
}
