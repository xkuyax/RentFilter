import { GeoJsonFeature } from "../types";

const SOURCE_COLORS: Record<string, string> = {
  WILLHABEN: "#e63946",
  GENOSSENSCHAFTEN: "#457b9d",
  GRAWE: "#2a9d8f",
};

const FALLBACK_COLOR = "#888888";

function formatPrice(price: number | null): string {
  if (price == null) return "—";
  return `€${price.toLocaleString("de-AT")}`;
}

function parseJsonArray(raw: string | null): string[] {
  if (!raw) return [];
  try {
    return JSON.parse(raw);
  } catch {
    return [];
  }
}

interface Props {
  feature: GeoJsonFeature;
}

export default function ListingPopup({ feature }: Props) {
  const p = feature.properties;
  const color = SOURCE_COLORS[p.source] || FALLBACK_COLOR;
  const images = parseJsonArray(p.imageUrls);

  return (
    <div className="text-sm min-w-[240px] max-w-[320px] max-h-[400px] overflow-y-auto">
      {images.length > 0 && (
        <div className="flex gap-1 mb-2 overflow-x-auto">
          {images.slice(0, 4).map((url, i) => (
            <img
              key={i}
              src={url}
              alt=""
              className="w-16 h-12 object-cover rounded flex-shrink-0"
            />
          ))}
          {images.length > 4 && (
            <div className="w-16 h-12 bg-gray-200 rounded flex-shrink-0 flex items-center justify-center text-xs text-gray-500">
              +{images.length - 4}
            </div>
          )}
        </div>
      )}

      <h3 className="font-semibold text-base mb-1">{p.title}</h3>

      <div className="space-y-0.5 text-gray-700">
        <div>
          <span className="font-medium">Price: </span>
          {formatPrice(p.price)}
        </div>
        {p.netRent != null && (
          <div className="text-xs text-gray-500">
            Net: {formatPrice(p.netRent)} | OC: {formatPrice(p.operatingCosts)} | VAT: {formatPrice(p.vat)}
          </div>
        )}
        {p.deposit != null && (
          <div>
            <span className="font-medium">Deposit: </span>
            {formatPrice(p.deposit)}
          </div>
        )}
        {p.rooms != null && (
          <div>
            <span className="font-medium">Rooms: </span>
            {p.rooms}
          </div>
        )}
        {p.area != null && (
          <div>
            <span className="font-medium">Area: </span>
            {p.area} m²
            {p.pricePerSqm != null && (
              <span className="text-gray-400"> (€{p.pricePerSqm.toFixed(0)}/m²)</span>
            )}
          </div>
        )}
        <div>
          <span className="font-medium">Address: </span>
          {p.address}
        </div>
        {p.buildYear && (
          <div>
            <span className="font-medium">Year: </span>
            {p.buildYear}
          </div>
        )}
        {p.availableFrom && (
          <div>
            <span className="font-medium">Available: </span>
            {p.availableFrom}
          </div>
        )}
        {p.provision && (
          <div>
            <span className="font-medium">Commission: </span>
            {p.provision}
          </div>
        )}
        <div>
          <span
            className="inline-block px-1.5 py-0.5 rounded text-white text-xs font-medium"
            style={{ backgroundColor: color }}
          >
            {p.source}
          </span>
          {p.has360View && (
            <span className="inline-block px-1.5 py-0.5 rounded bg-blue-500 text-white text-xs font-medium ml-1">
              360°
            </span>
          )}
        </div>
      </div>

      <div className="mt-2 flex gap-2 text-xs">
        <a
          href={p.url}
          target="_blank"
          rel="noopener noreferrer"
          className="text-blue-600 hover:underline"
        >
          Open listing →
        </a>
        {p.matterportUrl && (
          <a
            href={p.matterportUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-blue-600 hover:underline"
          >
            360° View →
          </a>
        )}
      </div>
    </div>
  );
}
