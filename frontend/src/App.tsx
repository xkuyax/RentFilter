import { useState, useCallback } from "react";
import MapView from "./components/MapView";
import FilterSidebar from "./components/FilterSidebar";
import { Filters } from "./types";

function App() {
  const [filters, setFilters] = useState<Filters>({});

  const handleFilterChange = useCallback((newFilters: Filters) => {
    setFilters(newFilters);
  }, []);

  return (
    <div className="flex w-full h-full">
      <FilterSidebar filters={filters} onChange={handleFilterChange} />
      <div className="flex-1">
        <MapView filters={filters} />
      </div>
    </div>
  );
}

export default App;
