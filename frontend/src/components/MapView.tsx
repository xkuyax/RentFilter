import { useEffect, useState } from "react";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import L from "leaflet";
import { GeoJsonFeature, Filters } from "../types";
import { fetchMapListings } from "../api";
import ListingPopup from "./ListingPopup";

const GRAZ_CENTER: [number, number] = [47.0707, 15.4395];

const SOURCE_ICONS: Record<string, L.DivIcon> = {};

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

interface Props {
  filters: Filters;
}

export default function MapView({ filters }: Props) {
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
