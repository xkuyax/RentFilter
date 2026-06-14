import { useEffect, useState } from "react";
import { MapContainer, TileLayer, Marker, Popup, useMap, Circle } from "react-leaflet";
import L from "leaflet";
import { GeoJsonFeature, Filters } from "../types";
import { fetchMapListings } from "../api";
import ListingPopup from "./ListingPopup";

const GRAZ_CENTER: [number, number] = [47.0707, 15.4395];

const SOURCE_ICONS: Record<string, L.DivIcon> = {};

const searchIcon = L.divIcon({
  html: `<div style="background:#e63946;width:20px;height:20px;border-radius:50%;border:3px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.5)"></div>`,
  className: "",
  iconSize: [20, 20],
  iconAnchor: [10, 10],
  popupAnchor: [0, -10],
});

function getIcon(source: string): L.DivIcon {
  if (SOURCE_ICONS[source]) return SOURCE_ICONS[source];

  const colors: Record<string, string> = {
    WILLHABEN: "#e63946",
    GENOSSENSCHAFTEN: "#457b9d",
    GRAWE: "#2a9d8f",
  };
  const color = colors[source] || "#888";

  SOURCE_ICONS[source] = L.divIcon({
    html: `<div style="background:${color};width:16px;height:16px;border-radius:50%;border:2px solid white;box-shadow:0 1px 3px rgba(0,0,0,0.4)"></div>`,
    className: "",
    iconSize: [16, 16],
    iconAnchor: [8, 8],
    popupAnchor: [0, -8],
  });
  return SOURCE_ICONS[source];
}

function FlyToCenter({ center }: { center: [number, number] | null }) {
  const map = useMap();
  useEffect(() => {
    if (center) {
      map.flyTo(center, 16);
    }
  }, [center, map]);
  return null;
}

interface Props {
  filters: Filters;
  center: [number, number] | null;
}

export default function MapView({ filters, center }: Props) {
  const [features, setFeatures] = useState<GeoJsonFeature[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetchMapListings(filters)
      .then((data) => {
        if (!cancelled) {
          setFeatures(data.features);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [filters]);

  return (
    <div className="relative w-full h-full">
      {loading && (
        <div className="absolute top-2 left-1/2 -translate-x-1/2 z-[1000] bg-white px-3 py-1 rounded shadow text-sm">
          Loading...
        </div>
      )}
      {error && (
        <div className="absolute top-2 left-1/2 -translate-x-1/2 z-[1000] bg-red-100 text-red-700 px-3 py-1 rounded shadow text-sm">
          {error}
        </div>
      )}
      <MapContainer
        center={GRAZ_CENTER}
        zoom={13}
        scrollWheelZoom={true}
        style={{ width: "100%", height: "100%" }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <FlyToCenter center={center} />
        {center && (
          <>
            <Circle
              center={center}
              pathOptions={{ className: "search-pulse-ring" }}
              radius={1}
            />
            <Marker position={center} icon={searchIcon}>
              <Popup>
                <div className="text-sm font-medium">Search result</div>
              </Popup>
            </Marker>
          </>
        )}
        {features.map((f) => (
          <Marker
            key={f.properties.id}
            position={[f.geometry.coordinates[1], f.geometry.coordinates[0]]}
            icon={getIcon(f.properties.source)}
          >
            <Popup>
              <ListingPopup feature={f} />
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  );
}
