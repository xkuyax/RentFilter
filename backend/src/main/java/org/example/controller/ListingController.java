package org.example.controller;

import org.example.entity.Listing;
import org.example.entity.Source;
import org.example.service.ListingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public Map<String, Object> getListings(
            @RequestParam(required = false) Source source,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Float minRooms,
            @RequestParam(required = false) Float minArea,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String src = source != null ? source.name() : null;
        List<Listing> content = listingService.findListings(
                src, minPrice, maxPrice, minRooms, minArea, page * size, size);
        long total = listingService.countFiltered(src, minPrice, maxPrice, minRooms, minArea);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        result.put("number", page);
        result.put("size", size);
        return result;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Listing> getListing(@PathVariable Long id) {
        return listingService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/map")
    public Map<String, Object> getMapListings(
            @RequestParam(required = false) Source source,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Float minRooms,
            @RequestParam(required = false) Float minArea,
            @RequestParam(required = false) Float maxPricePerSqm) {
        String src = source != null ? source.name() : null;
        List<Listing> listings = listingService.findAllFilteredWithCoords(
                src, minPrice, maxPrice, minRooms, minArea, maxPricePerSqm);
        List<Map<String, Object>> features = listings
                .stream()
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
        properties.put("pricePerSqm", listing.getPricePerSqm());

        feature.put("geometry", Map.of("type", "Point", "coordinates",
                new double[]{listing.getLongitude(), listing.getLatitude()}));
        feature.put("properties", properties);
        return feature;
    }
}
