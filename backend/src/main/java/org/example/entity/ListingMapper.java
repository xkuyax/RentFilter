package org.example.entity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ListingMapper {

    Optional<Listing> findByUrl(String url);

    boolean existsByUrl(String url);

    long count();

    long countFiltered(@Param("source") String source,
                       @Param("minPrice") BigDecimal minPrice,
                       @Param("maxPrice") BigDecimal maxPrice,
                       @Param("minRooms") Float minRooms,
                       @Param("minArea") Float minArea);

    List<Listing> findAllFiltered(@Param("source") String source,
                                  @Param("minPrice") BigDecimal minPrice,
                                  @Param("maxPrice") BigDecimal maxPrice,
                                  @Param("minRooms") Float minRooms,
                                  @Param("minArea") Float minArea,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    Optional<Listing> findById(Long id);

    List<Listing> findAllWithCoords();

    List<Listing> findAllFilteredWithCoords(@Param("source") String source,
                                            @Param("minPrice") BigDecimal minPrice,
                                            @Param("maxPrice") BigDecimal maxPrice,
                                            @Param("minRooms") Float minRooms,
                                            @Param("minArea") Float minArea);

    int insert(Listing listing);

    int updateCoordinates(Listing listing);

    void deleteAll();
}
