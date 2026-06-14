import { useEffect, useCallback } from "react";

interface Props {
  images: string[];
  index: number;
  onClose: () => void;
  onPrev: () => void;
  onNext: () => void;
}

export default function Lightbox({ images, index, onClose, onPrev, onNext }: Props) {
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
      else if (e.key === "ArrowLeft") onPrev();
      else if (e.key === "ArrowRight") onNext();
    },
    [onClose, onPrev, onNext]
  );

  useEffect(() => {
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [handleKeyDown]);

  return (
    <div
      className="fixed inset-0 z-[3000] bg-black/95 flex items-center justify-center"
      onClick={onClose}
    >
      <button
        onClick={onClose}
        className="absolute top-4 right-4 z-10 text-white text-3xl hover:opacity-70 w-10 h-10 flex items-center justify-center"
      >
        ×
      </button>

      {images.length > 1 && (
        <>
          <button
            onClick={(e) => { e.stopPropagation(); onPrev(); }}
            className="absolute left-4 top-1/2 -translate-y-1/2 z-10 text-white text-4xl hover:opacity-70 w-12 h-12 flex items-center justify-center select-none"
          >
            ‹
          </button>
          <button
            onClick={(e) => { e.stopPropagation(); onNext(); }}
            className="absolute right-4 top-1/2 -translate-y-1/2 z-10 text-white text-4xl hover:opacity-70 w-12 h-12 flex items-center justify-center select-none"
          >
            ›
          </button>
        </>
      )}

      <div
        className="absolute bottom-4 left-1/2 -translate-x-1/2 z-10 text-white/80 text-sm bg-black/50 px-3 py-1 rounded"
      >
        {index + 1} / {images.length}
      </div>

      <img
        src={images[index]}
        alt=""
        className="max-w-[95vw] max-h-[90vh] object-contain"
        onClick={(e) => e.stopPropagation()}
      />
    </div>
  );
}
