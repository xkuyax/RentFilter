export interface Listing {
  id: number;
  title: string;
  description: string | null;
  price: number | null;
  rooms: number | null;
  area: number | null;
  address: string;
  latitude: number | null;
  longitude: number | null;
  source: string;
  url: string;
  createdAt: string;
  updatedAt: string;
  netRent: number | null;
  operatingCosts: number | null;
  vat: number | null;
  deposit: number | null;
  availableFrom: string | null;
  provision: string | null;
  buildYear: number | null;
  heatingDemand: number | null;
  fgee: number | null;
  benefits: string | null;
  imageUrls: string | null;
  thumbnailUrl: string | null;
  has360View: boolean;
  matterportUrl: string | null;
}

export interface GeoJsonProperties {
  id: number;
  title: string;
  price: number | null;
  rooms: number | null;
  area: number | null;
  address: string;
  source: string;
  url: string;
  netRent: number | null;
  operatingCosts: number | null;
  vat: number | null;
  deposit: number | null;
  availableFrom: string | null;
  provision: string | null;
  buildYear: number | null;
  heatingDemand: number | null;
  fgee: number | null;
  benefits: string | null;
  imageUrls: string | null;
  thumbnailUrl: string | null;
  has360View: boolean;
  matterportUrl: string | null;
  description: string | null;
  pricePerSqm: number | null;
}

export interface Filters {
  source?: string;
  minPrice?: number;
  maxPrice?: number;
  minRooms?: number;
  minArea?: number;
  maxPricePerSqm?: number;
}

export interface GeoJsonFeature {
  type: "Feature";
  geometry: {
    type: "Point";
    coordinates: [number, number];
  };
  properties: GeoJsonProperties;
}

export interface GeoJsonCollection {
  type: "FeatureCollection";
  features: GeoJsonFeature[];
}
