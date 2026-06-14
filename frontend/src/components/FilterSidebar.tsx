import { Filters } from "../types";
import AddressSearch from "./AddressSearch";

interface Props {
  filters: Filters;
  onChange: (filters: Filters) => void;
  onSearchResult: (lat: number, lng: number) => void;
}

const SOURCES = ["WILLHABEN", "GENOSSENSCHAFTEN", "GRAWE"];

export default function FilterSidebar({ filters, onChange, onSearchResult }: Props) {
  return (
    <div className="w-72 bg-white shadow-lg p-4 overflow-y-auto flex flex-col gap-3">
      <h2 className="text-lg font-semibold">RentFilter</h2>

      <AddressSearch onResult={onSearchResult} />

      <hr className="border-gray-200" />

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">Source</label>
        <select
          className="w-full border rounded px-2 py-1 text-sm"
          value={filters.source || ""}
          onChange={(e) => onChange({ ...filters, source: e.target.value || undefined })}
        >
          <option value="">All</option>
          {SOURCES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">
          Min price (€)
        </label>
        <input
          type="number"
          className="w-full border rounded px-2 py-1 text-sm"
          placeholder="0"
          value={filters.minPrice ?? ""}
          onChange={(e) =>
            onChange({ ...filters, minPrice: e.target.value ? Number(e.target.value) : undefined })
          }
        />
      </div>

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">
          Max price (€)
        </label>
        <input
          type="number"
          className="w-full border rounded px-2 py-1 text-sm"
          placeholder="∞"
          value={filters.maxPrice ?? ""}
          onChange={(e) =>
            onChange({ ...filters, maxPrice: e.target.value ? Number(e.target.value) : undefined })
          }
        />
      </div>

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">
          Min rooms
        </label>
        <input
          type="number"
          step="0.5"
          className="w-full border rounded px-2 py-1 text-sm"
          placeholder="0"
          value={filters.minRooms ?? ""}
          onChange={(e) =>
            onChange({ ...filters, minRooms: e.target.value ? Number(e.target.value) : undefined })
          }
        />
      </div>

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">
          Min area (m²)
        </label>
        <input
          type="number"
          className="w-full border rounded px-2 py-1 text-sm"
          placeholder="0"
          value={filters.minArea ?? ""}
          onChange={(e) =>
            onChange({ ...filters, minArea: e.target.value ? Number(e.target.value) : undefined })
          }
        />
      </div>

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">
          Max €/m²
        </label>
        <input
          type="number"
          className="w-full border rounded px-2 py-1 text-sm"
          placeholder="∞"
          value={filters.maxPricePerSqm ?? ""}
          onChange={(e) =>
            onChange({ ...filters, maxPricePerSqm: e.target.value ? Number(e.target.value) : undefined })
          }
        />
      </div>
    </div>
  );
}
