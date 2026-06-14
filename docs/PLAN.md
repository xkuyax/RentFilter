# RentFilter — Implementation Plan

## Architecture Overview

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot (Web, JPA, Scheduler) |
| Database | PostgreSQL via Docker Compose |
| Frontend | React (Vite + TypeScript) |
| Map | Leaflet.js |
| Scraping | Jsoup |
| Scheduling | Spring `@Scheduled` |
| Project | Gradle multi-module monorepo |

---

## Tasks

### 1. Multi-Module Gradle Setup
- Restructure `build.gradle.kts` with `backend` and `frontend` submodules
- Add Spring Boot plugin + dependencies (Web, JPA, PostgreSQL driver, Scheduler, Actuator)
- Add Jsoup dependency for HTML scraping
- Set up Vite for the frontend module

### 2. Database Infrastructure
- `docker-compose.yml` with PostgreSQL service
- Configure `application.yml` with datasource settings
- Set up Flyway for schema migrations
- Define JPA entities: `Listing`, `Source`, `ListingSnapshot` (for dedup/diff)

### 3. Scraper Framework
- Define a `ListingScraper` interface (fetch → parse → yield `Listing` DTOs)
- Abstract base class with Jsoup HTTP fetch, retries, user-agent rotation
- Implement concrete scrapers:
  - `WillhabenScraper` (willhaben.at)
  - `GenossenschaftenScraper` (genossenschaften.immo)
  - `GraweScraper` (grawewohnen.at)
- Geocoding service (address → lat/lng) using a free provider (e.g. Nominatim)

### 4. Scheduled Fetching
- `FetcherScheduler` with `@Scheduled` cron (e.g. hourly)
- Deduplication: compare by external ID + hash to skip unchanged listings
- Store fetched listings in DB

### 5. REST API
- `GET /api/listings` — paginated, filterable by price, rooms, area, source
- `GET /api/listings/{id}` — single listing details
- `GET /api/listings/map` — lightweight GeoJSON response for map rendering
- CORS config for frontend dev server

### 6. Frontend — React App Setup
- Initialize React + TypeScript with Vite
- Add Leaflet + react-leaflet for the map
- Add a simple CSS framework (Tailwind or MUI)
- Configure dev proxy to `localhost:8080`

### 7. Frontend — Map View
- Full-screen Leaflet map centered on user's area (configurable default, e.g. Vienna)
- Markers/clusters for listings, color-coded by source
- Click marker → popup with key details (price, rooms, area, link to source)
- Sidebar/drawer with filter controls (price range, rooms, source toggle)

### 8. Integration & Polish
- Wire frontend filters to backend API
- Add a detail page or expanded card view
- Loading/error/empty states

### 9. Deployment
- Dockerfile for the Spring Boot app
- Production `docker-compose.yml` (app + postgres)
- Reverse proxy config for existing nginx on VPS
- Health check endpoint for monitoring

---

## Resolved Decisions
- **Geocoding**: Nominatim (free, rate-limited). Scrapers should also try to extract coordinates from listing pages when available.
- **React UI kit**: Tailwind CSS.
- **Map**: OpenStreetMap via Leaflet (tile.openstreetmap.org). Default center: Graz, Austria. Configurable via `application.yml` for potential multi-city use later.
- **Requirements**: `REQUIREMENTS.md` will be defined later.
