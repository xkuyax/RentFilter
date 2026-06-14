package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Listing;
import org.example.entity.ListingMapper;
import org.example.entity.Source;
import org.example.scraper.ListingDto;
import org.example.scraper.ListingScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);

    private final List<ListingScraper> scrapers;
    private final ListingMapper listingMapper;
    private final GeocodingService geocodingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ScraperService(List<ListingScraper> scrapers,
                          ListingMapper listingMapper,
                          GeocodingService geocodingService) {
        this.scrapers = scrapers;
        this.listingMapper = listingMapper;
        this.geocodingService = geocodingService;
    }

    public void fetchAll() {
        for (ListingScraper scraper : scrapers) {
            try {
                log.info("Running scraper: {}", scraper.getSource());
                List<ListingDto> dtos = scraper.scrape();
                int saved = 0;
                for (ListingDto dto : dtos) {
                    if (saveOrUpdate(scraper.getSource(), dto)) {
                        saved++;
                    }
                }
                log.info("{}: {} new/updated listings out of {} fetched",
                        scraper.getSource(), saved, dtos.size());
            } catch (Exception e) {
                log.error("Scraper {} failed", scraper.getSource(), e);
            }
        }
    }

    private boolean saveOrUpdate(Source source, ListingDto dto) {
        Optional<Listing> existing = listingMapper.findByUrl(dto.getUrl());
        if (existing.isPresent()) {
            Listing ex = existing.get();
            if ((ex.getLatitude() == null || ex.getLongitude() == null)
                    && geocodingService != null) {
                var coords = geocodingService.geocode(ex.getAddress());
                if (coords != null) {
                    ex.setLatitude(coords.lat());
                    ex.setLongitude(coords.lng());
                    listingMapper.updateCoordinates(ex);
                }
            }
            return false;
        }

        Listing listing = new Listing();
        listing.setExternalId(dto.getExternalId());
        listing.setSource(source);
        listing.setTitle(dto.getTitle());
        listing.setDescription(dto.getDescription());
        listing.setPrice(dto.getPrice());
        listing.setRooms(dto.getRooms());
        listing.setArea(dto.getArea());
        listing.setAddress(dto.getAddress());
        listing.setLatitude(dto.getLatitude());
        listing.setLongitude(dto.getLongitude());
        listing.setUrl(dto.getUrl());

        listing.setNetRent(dto.getNetRent());
        listing.setOperatingCosts(dto.getOperatingCosts());
        listing.setVat(dto.getVat());
        listing.setDeposit(dto.getDeposit());
        listing.setAvailableFrom(dto.getAvailableFrom());
        listing.setProvision(dto.getProvision());
        listing.setBuildYear(dto.getBuildYear());
        listing.setHeatingDemand(dto.getHeatingDemand());
        listing.setFgee(dto.getFgee());
        listing.setBenefits(toJson(dto.getBenefits()));
        listing.setImageUrls(toJson(dto.getImageUrls()));
        listing.setThumbnailUrl(dto.getThumbnailUrl());
        listing.setHas360View(dto.isHas360View());
        listing.setMatterportUrl(dto.getMatterportUrl());

        if ((listing.getLatitude() == null || listing.getLongitude() == null)
                && geocodingService != null) {
            var coords = geocodingService.geocode(dto.getAddress());
            if (coords != null) {
                listing.setLatitude(coords.lat());
                listing.setLongitude(coords.lng());
            }
        }

        listingMapper.insert(listing);
        return true;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize to JSON", e);
            return null;
        }
    }
}
