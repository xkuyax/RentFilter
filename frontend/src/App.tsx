import { useState, useCallback } from "react";
import MapView from "./components/MapView";
import FilterSidebar from "./components/FilterSidebar";
import AdminPanel from "./components/AdminPanel";
import { Filters } from "./types";

function App() {
  const [filters, setFilters] = useState<Filters>({});
  const [center, setCenter] = useState<[number, number] | null>(null);

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
        <MapView filters={filters} center={center} />
        <AdminPanel />
      </div>
    </div>
  );
}

export default App;
