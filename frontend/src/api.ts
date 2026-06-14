import { Filters, GeoJsonCollection } from "./types";

const BASE = "/api";

export async function fetchMapListings(filters?: Filters): Promise<GeoJsonCollection> {
  const params = new URLSearchParams();

  if (filters?.source) params.set("source", filters.source);
  if (filters?.minPrice != null) params.set("minPrice", String(filters.minPrice));
  if (filters?.maxPrice != null) params.set("maxPrice", String(filters.maxPrice));
  if (filters?.minRooms != null) params.set("minRooms", String(filters.minRooms));
  if (filters?.minArea != null) params.set("minArea", String(filters.minArea));
  if (filters?.maxPricePerSqm != null) params.set("maxPricePerSqm", String(filters.maxPricePerSqm));

  const url = `${BASE}/listings/map${params.toString() ? "?" + params.toString() : ""}`;
  const response = await fetch(url);
  if (!response.ok) throw new Error("Failed to fetch listings");
  return response.json();
}

export async function triggerScrape(): Promise<{ status: string }> {
  const response = await fetch(`${BASE}/admin/scrape`, { method: "POST" });
  return response.json();
}

export async function triggerGeocode(): Promise<{ status: string; filled: number }> {
  const response = await fetch(`${BASE}/admin/geocode`, { method: "POST" });
  return response.json();
}
