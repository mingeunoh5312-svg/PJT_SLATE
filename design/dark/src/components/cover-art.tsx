import { cn } from "@/lib/utils";

// Cinematic placeholder "cover" art — pure CSS, no images required.
// Each variant deterministically composes gradients + grid + slate marks.

const variants: Record<string, string> = {
  blueroom:
    "bg-[radial-gradient(120%_80%_at_20%_10%,oklch(0.75_0.10_240/28%),transparent_60%),radial-gradient(80%_60%_at_85%_90%,oklch(0.55_0.08_280/28%),transparent_60%),linear-gradient(180deg,oklch(0.94_0.01_260),oklch(0.88_0.01_260))]",
  nightowl:
    "bg-[radial-gradient(120%_80%_at_70%_20%,oklch(0.80_0.13_65/28%),transparent_60%),radial-gradient(80%_60%_at_10%_90%,oklch(0.55_0.08_30/30%),transparent_60%),linear-gradient(180deg,oklch(0.92_0.01_40),oklch(0.86_0.01_40))]",
  doc:
    "bg-[radial-gradient(120%_80%_at_50%_30%,oklch(0.72_0.08_160/24%),transparent_60%),linear-gradient(180deg,oklch(0.94_0.01_180),oklch(0.87_0.01_180))]",
  ad:
    "bg-[radial-gradient(80%_60%_at_30%_20%,oklch(0.82_0.12_30/30%),transparent_60%),radial-gradient(80%_60%_at_80%_80%,oklch(0.70_0.15_320/26%),transparent_60%),linear-gradient(180deg,oklch(0.92_0.01_20),oklch(0.86_0.01_20))]",
  generic:
    "bg-[radial-gradient(120%_80%_at_30%_20%,oklch(0.40_0.04_60/40%),transparent_60%),linear-gradient(180deg,oklch(0.90_0.01_60),oklch(0.82_0.01_60))]",
};

export function CoverArt({
  variant = "generic",
  label,
  meta,
  className,
  ratio = "16/9",
}: {
  variant?: string;
  label?: string;
  meta?: string;
  className?: string;
  ratio?: string;
}) {
  const bg = variants[variant] ?? variants.generic;
  return (
    <div
      className={cn(
        "relative overflow-hidden rounded-md border hairline",
        bg,
        className,
      )}
      style={{ aspectRatio: ratio }}
    >
      {/* film grid */}
      <div
        className="absolute inset-0 opacity-[0.10]"
        style={{
          backgroundImage:
            "linear-gradient(to right, oklch(0.18 0.02 70 / 40%) 1px, transparent 1px), linear-gradient(to bottom, oklch(0.18 0.02 70 / 40%) 1px, transparent 1px)",
          backgroundSize: "32px 32px",
        }}
      />
      {/* perforations */}
      <div className="absolute inset-y-0 left-0 w-3 flex flex-col items-center justify-around py-2">
        {Array.from({ length: 8 }).map((_, i) => (
          <span key={i} className="h-1 w-1 rounded-[1px] bg-foreground/40" />
        ))}
      </div>
      <div className="absolute inset-y-0 right-0 w-3 flex flex-col items-center justify-around py-2">
        {Array.from({ length: 8 }).map((_, i) => (
          <span key={i} className="h-1 w-1 rounded-[1px] bg-foreground/40" />
        ))}
      </div>

      <div className="absolute left-5 top-4 font-mono-meta text-foreground/70">
        {meta ?? "SCN 014 · TAKE 03"}
      </div>
      {label ? (
        <div className="absolute bottom-4 left-5 right-5">
          <div className="font-display text-2xl text-foreground/95 leading-tight">{label}</div>
        </div>
      ) : null}
    </div>
  );
}
