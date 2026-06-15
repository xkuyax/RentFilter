package org.example.entity;

import java.math.BigDecimal;
import java.time.Instant;

public class Listing {

    private Long id;
    private String externalId;
    private Source source;
    private String title;
    private String description;
    private BigDecimal price;
    private Float rooms;
    private Float area;
    private String address;
    private Double latitude;
    private Double longitude;
    private String url;
    private Instant createdAt;
    private Instant updatedAt;

    private BigDecimal netRent;
    private BigDecimal operatingCosts;
    private BigDecimal vat;
    private BigDecimal deposit;
    private String availableFrom;
    private String provision;
    private Integer buildYear;
    private Float heatingDemand;
    private Float fgee;
    private String benefits;
    private String imageUrls;
    private String thumbnailUrl;
    private boolean has360View;
    private String matterportUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Float getRooms() {
        return rooms;
    }

    public void setRooms(Float rooms) {
        this.rooms = rooms;
    }

    public Float getArea() {
        return area;
    }

    public void setArea(Float area) {
        this.area = area;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public BigDecimal getNetRent() {
        return netRent;
    }

    public void setNetRent(BigDecimal netRent) {
        this.netRent = netRent;
    }

    public BigDecimal getOperatingCosts() {
        return operatingCosts;
    }

    public void setOperatingCosts(BigDecimal operatingCosts) {
        this.operatingCosts = operatingCosts;
    }

    public BigDecimal getVat() {
        return vat;
    }

    public void setVat(BigDecimal vat) {
        this.vat = vat;
    }

    public BigDecimal getDeposit() {
        return deposit;
    }

    public void setDeposit(BigDecimal deposit) {
        this.deposit = deposit;
    }

    public String getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(String availableFrom) {
        this.availableFrom = availableFrom;
    }

    public String getProvision() {
        return provision;
    }

    public void setProvision(String provision) {
        this.provision = provision;
    }

    public Integer getBuildYear() {
        return buildYear;
    }

    public void setBuildYear(Integer buildYear) {
        this.buildYear = buildYear;
    }

    public Float getHeatingDemand() {
        return heatingDemand;
    }

    public void setHeatingDemand(Float heatingDemand) {
        this.heatingDemand = heatingDemand;
    }

    public Float getFgee() {
        return fgee;
    }

    public void setFgee(Float fgee) {
        this.fgee = fgee;
    }

    public String getBenefits() {
        return benefits;
    }

    public void setBenefits(String benefits) {
        this.benefits = benefits;
    }

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public boolean isHas360View() {
        return has360View;
    }

    public void setHas360View(boolean has360View) {
        this.has360View = has360View;
    }

    public String getMatterportUrl() {
        return matterportUrl;
    }

    public void setMatterportUrl(String matterportUrl) {
        this.matterportUrl = matterportUrl;
    }

    public Float getPricePerSqm() {
        if (price == null || area == null || area == 0) {
            return null;
        }
        return price.floatValue() / area;
    }
}
