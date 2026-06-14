package org.example.controller;

import org.example.entity.Listing;
import org.example.entity.Source;
import org.example.service.ListingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public Page<Listing> getListings(
            @RequestParam(required = false) Source source,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Float minRooms,
            @RequestParam(required = false) Float minArea,
            Pageable pageable) {
        return listingService.findListings(source, minPrice, maxPrice, minRooms, minArea, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Listing> getListing(@PathVariable Long id) {
        return listingService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/map")
    public Map<String, Object> getMapListings() {
        List<Map<String, Object>> features = listingService.findListings(
                        null, null, null, null, null,
                        Pageable.unpaged())
                .stream()
                .filter(l -> l.getLatitude() != null && l.getLongitude() != null)
                .map(this::toGeoJsonFeature)
                .toList();

        Map<String, Object> collection = new LinkedHashMap<>();
        collection.put("type", "FeatureCollection");
        collection.put("features", features);
        return collection;
    }

    private Map<String, Object> toGeoJsonFeature(Listing listing) {
        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");

        Map<String, Double> coordinates = new LinkedHashMap<>();
        coordinates.put("lat", listing.getLatitude());
        coordinates.put("lng", listing.getLongitude());

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("id", listing.getId());
        properties.put("title", listing.getTitle());
        properties.put("price", listing.getPrice());
        properties.put("rooms", listing.getRooms());
        properties.put("area", listing.getArea());
        properties.put("address", listing.getAddress());
        properties.put("source", listing.getSource());
        properties.put("url", listing.getUrl());
        properties.put("netRent", listing.getNetRent());
        properties.put("operatingCosts", listing.getOperatingCosts());
        properties.put("vat", listing.getVat());
        properties.put("deposit", listing.getDeposit());
        properties.put("availableFrom", listing.getAvailableFrom());
        properties.put("provision", listing.getProvision());
        properties.put("buildYear", listing.getBuildYear());
        properties.put("heatingDemand", listing.getHeatingDemand());
        properties.put("fgee", listing.getFgee());
        properties.put("benefits", listing.getBenefits());
        properties.put("imageUrls", listing.getImageUrls());
        properties.put("thumbnailUrl", listing.getThumbnailUrl());
        properties.put("has360View", listing.isHas360View());
        properties.put("matterportUrl", listing.getMatterportUrl());
        properties.put("description", listing.getDescription());

        feature.put("geometry", Map.of("type", "Point", "coordinates",
                new double[]{listing.getLongitude(), listing.getLatitude()}));
        feature.put("properties", properties);
        return feature;
    }
}
