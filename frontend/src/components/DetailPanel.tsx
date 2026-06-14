import { useState } from "react";
import { GeoJsonProperties } from "../types";
import Lightbox from "./Lightbox";

interface Props {
  listing: GeoJsonProperties;
  onClose: () => void;
}

function parseJsonArray(raw: string | null): string[] {
  if (!raw) return [];
  try { return JSON.parse(raw); } catch { return []; }
}

function fmtPrice(n: number | null): string {
  if (n == null) return "—";
  return "€" + n.toLocaleString("de-AT");
}

const SOURCE_BADGE: Record<string, string> = {
  WILLHABEN: "bg-red-600",
  GENOSSENSCHAFTEN: "bg-blue-600",
  GRAWE: "bg-green-600",
};

export default function DetailPanel({ listing: p, onClose }: Props) {
  const images = parseJsonArray(p.imageUrls);
  const benefits = parseJsonArray(p.benefits);
  const [imgIdx, setImgIdx] = useState(0);
  const [lightboxOpen, setLightboxOpen] = useState(false);

  return (
    <div className="fixed right-0 top-0 h-full w-[400px] max-w-[95vw] bg-white shadow-2xl z-[2000] flex flex-col overflow-y-auto">
      <button
        onClick={onClose}
        className="absolute top-3 right-3 z-10 bg-white/80 rounded-full w-8 h-8 flex items-center justify-center text-lg leading-none shadow hover:bg-white"
      >
        ×
      </button>

      {images.length > 0 && (
        <div
          className="relative w-full h-72 bg-gray-100 flex-shrink-0 cursor-pointer"
          onClick={() => setLightboxOpen(true)}
        >
          <img
            src={images[imgIdx]}
            alt=""
            className="w-full h-full object-contain"
          />
          {images.length > 1 && (
            <>
              <button
                onClick={() => setImgIdx((i) => (i > 0 ? i - 1 : images.length - 1))}
                className="absolute left-2 top-1/2 -translate-y-1/2 bg-white/80 rounded-full w-8 h-8 flex items-center justify-center shadow hover:bg-white"
              >
                ‹
              </button>
              <button
                onClick={() => setImgIdx((i) => (i < images.length - 1 ? i + 1 : 0))}
                className="absolute right-2 top-1/2 -translate-y-1/2 bg-white/80 rounded-full w-8 h-8 flex items-center justify-center shadow hover:bg-white"
              >
                ›
              </button>
              <div className="absolute bottom-2 right-2 bg-black/60 text-white text-xs px-2 py-0.5 rounded">
                {imgIdx + 1} / {images.length}
              </div>
            </>
          )}
        </div>
      )}

      <div className="p-4 flex flex-col gap-4">
        <div>
          <h2 className="text-lg font-semibold leading-tight">{p.title}</h2>
          <p className="text-sm text-gray-500 mt-1">{p.address}</p>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-2xl font-bold">{fmtPrice(p.price)}</span>
          <span className={"px-2 py-0.5 rounded text-white text-xs " + (SOURCE_BADGE[p.source] || "bg-gray-500")}>
            {p.source}
          </span>
          {p.has360View && (
            <span className="px-2 py-0.5 rounded bg-blue-500 text-white text-xs">360°</span>
          )}
        </div>

        <div className="grid grid-cols-2 gap-2 text-sm">
          {p.rooms != null && <Row label="Rooms" value={String(p.rooms)} />}
          {p.area != null && <Row label="Area" value={p.area + " m²" + (p.pricePerSqm != null ? " (€" + p.pricePerSqm.toFixed(0) + "/m²)" : "")} />}
          {p.buildYear && <Row label="Year built" value={String(p.buildYear)} />}
          {p.availableFrom && <Row label="Available" value={p.availableFrom} />}
          {p.heatingDemand != null && <Row label="Heating" value={p.heatingDemand + " kWh/m²a"} />}
          {p.fgee != null && <Row label="fGEE" value={String(p.fgee)} />}
          {p.provision && <Row label="Commission" value={p.provision} />}
          {p.deposit != null && <Row label="Deposit" value={fmtPrice(p.deposit)} />}
        </div>

        {(p.netRent != null || p.operatingCosts != null || p.vat != null) && (
          <div className="border rounded-lg overflow-hidden text-sm">
            <div className="bg-gray-50 px-3 py-1.5 font-medium text-gray-600">Rent breakdown</div>
            {p.netRent != null && <CostRow label="Nettomiete" value={fmtPrice(p.netRent)} />}
            {p.operatingCosts != null && <CostRow label="Betriebskosten" value={fmtPrice(p.operatingCosts)} />}
            {p.vat != null && <CostRow label="MwSt." value={fmtPrice(p.vat)} />}
            {p.price != null && <CostRow label="Gesamtkosten" value={fmtPrice(p.price)} last />}
          </div>
        )}

        {benefits.length > 0 && (
          <div>
            <div className="text-xs font-medium text-gray-500 mb-1.5">Benefits</div>
            <div className="flex flex-wrap gap-1">
              {benefits.map((b, i) => (
                <span key={i} className="px-2 py-0.5 rounded-full bg-gray-100 text-gray-700 text-xs">
                  {b}
                </span>
              ))}
            </div>
          </div>
        )}

        {p.description && (
          <div>
            <div className="text-xs font-medium text-gray-500 mb-1">Description</div>
            <p className="text-sm text-gray-700 leading-relaxed">{p.description}</p>
          </div>
        )}

        <div className="flex gap-2 pt-2 border-t">
          <a
            href={p.url}
            target="_blank"
            rel="noopener noreferrer"
            className="px-4 py-1.5 rounded bg-green-600 text-white text-sm hover:bg-green-700"
          >
            View listing →
          </a>
          {p.matterportUrl && (
            <a
              href={p.matterportUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="px-4 py-1.5 rounded bg-blue-600 text-white text-sm hover:bg-blue-700"
            >
              360° Tour →
            </a>
          )}
        </div>
      </div>
      {lightboxOpen && images.length > 0 && (
        <Lightbox
          images={images}
          index={imgIdx}
          onClose={() => setLightboxOpen(false)}
          onPrev={() => setImgIdx((i) => (i > 0 ? i - 1 : images.length - 1))}
          onNext={() => setImgIdx((i) => (i < images.length - 1 ? i + 1 : 0))}
        />
      )}
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-xs text-gray-400">{label}</div>
      <div className="font-medium">{value}</div>
    </div>
  );
}

function CostRow({ label, value, last }: { label: string; value: string; last?: boolean }) {
  return (
    <div className={"flex justify-between px-3 py-1.5 " + (last ? "font-semibold border-t" : "border-t border-gray-100")}>
      <span className="text-gray-600">{label}</span>
      <span>{value}</span>
    </div>
  );
}
