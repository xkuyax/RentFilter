import { useRef } from "react";

interface Props {
  onResult: (lat: number, lng: number) => void;
}

export default function AddressSearch({ onResult }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const timeoutRef = useRef<ReturnType<typeof setTimeout>>();

  const search = async (query: string) => {
    if (query.length < 3) return;
    try {
      const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(query)}`;
      const res = await fetch(url, {
        headers: { "User-Agent": "RentFilter/1.0" },
      });
      const data = await res.json();
      if (data.length > 0) {
        onResult(parseFloat(data[0].lat), parseFloat(data[0].lon));
      }
    } catch {
      // silently ignore
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      clearTimeout(timeoutRef.current);
      search(inputRef.current?.value || "");
    }
  };

  return (
    <div className="mb-3">
      <label className="block text-xs font-medium text-gray-600 mb-1">
        Search address
      </label>
      <input
        ref={inputRef}
        type="text"
        className="w-full border rounded px-2 py-1 text-sm"
        placeholder="Street, Graz..."
        onKeyDown={handleKeyDown}
      />
    </div>
  );
}
