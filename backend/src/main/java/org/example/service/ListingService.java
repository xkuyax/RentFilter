package org.example.service;

import org.example.entity.Listing;
import org.example.entity.ListingRepository;
import org.example.entity.Source;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ListingService {

    private final ListingRepository listingRepository;

    public ListingService(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    public Page<Listing> findListings(Source source, BigDecimal minPrice, BigDecimal maxPrice,
                                       Float minRooms, Float minArea, Pageable pageable) {
        return listingRepository.findAll(ListingSpecs.filter(source, minPrice, maxPrice, minRooms, minArea), pageable);
    }

    public Optional<Listing> findById(Long id) {
        return listingRepository.findById(id);
    }

    public long countAll() {
        return listingRepository.count();
    }
}
