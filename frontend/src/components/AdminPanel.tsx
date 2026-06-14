import { useState } from "react";
import { triggerScrape, triggerGeocode } from "../api";

export default function AdminPanel() {
  const [scraping, setScraping] = useState(false);
  const [geocoding, setGeocoding] = useState(false);
  const [message, setMessage] = useState("");

  const handleScrape = async () => {
    setScraping(true);
    setMessage("");
    try {
      await triggerScrape();
      setMessage("Scrape complete!");
    } catch {
      setMessage("Scrape failed");
    }
    setScraping(false);
  };

  const handleGeocode = async () => {
    setGeocoding(true);
    setMessage("");
    try {
      const result = await triggerGeocode();
      setMessage(`Geocoded ${result.filled} listings`);
    } catch {
      setMessage("Geocode failed");
    }
    setGeocoding(false);
  };

  return (
    <div className="absolute bottom-2 left-2 z-[1000] flex items-center gap-2 bg-white/90 backdrop-blur rounded-lg shadow px-3 py-2 text-xs">
      <button
        onClick={handleScrape}
        disabled={scraping}
        className="px-3 py-1 rounded bg-green-600 text-white hover:bg-green-700 disabled:opacity-50"
      >
        {scraping ? "Scraping..." : "Scrape"}
      </button>
      <button
        onClick={handleGeocode}
        disabled={geocoding}
        className="px-3 py-1 rounded bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50"
      >
        {geocoding ? "Geocoding..." : "Geocode"}
      </button>
      {message && <span className="text-gray-700 ml-1">{message}</span>}
    </div>
  );
}
