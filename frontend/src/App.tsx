import { useState, useCallback } from "react";
import MapView from "./components/MapView";
import FilterSidebar from "./components/FilterSidebar";
import AdminPanel from "./components/AdminPanel";
import DetailPanel from "./components/DetailPanel";
import { Filters, GeoJsonProperties } from "./types";

function App() {
  const [filters, setFilters] = useState<Filters>({
    maxPrice: 1000,
    minRooms: 3,
    minArea: 50,
  });
  const [center, setCenter] = useState<[number, number] | null>(null);
  const [selected, setSelected] = useState<GeoJsonProperties | null>(null);

  const handleFilterChange = useCallback((newFilters: Filters) => {
    setFilters(newFilters);
  }, []);

  const handleSearchResult = useCallback((lat: number, lng: number) => {
    setCenter([lat, lng]);
  }, []);

  return (
    <div className="flex w-full h-full">
      <FilterSidebar
        filters={filters}
        onChange={handleFilterChange}
        onSearchResult={handleSearchResult}
      />
      <div className="flex-1 relative">
        <MapView
          filters={filters}
          center={center}
          onSelectListing={setSelected}
        />
        <AdminPanel />
      </div>
      {selected && (
        <DetailPanel key={selected.id} listing={selected} onClose={() => setSelected(null)} />
      )}
    </div>
  );
}

export default App;
