import { createFileRoute } from "@tanstack/react-router";
import { SiteHeader, SiteFooter } from "@/components/site-chrome";
import { locations } from "@/lib/mock";
import { Sparkles, Bookmark, MapPin, AlertTriangle, Users } from "lucide-react";

export const Route = createFileRoute("/ai/location")({
  head: () => ({
    meta: [
      { title: "AI 로케이션 탐색 — Slate" },
      { name: "description", content: "장면 설명과 지역 조건으로 촬영지 후보를 찾고, 개인·팀 후보로 저장합니다." },
      { property: "og:title", content: "AI 로케이션 탐색 — Slate" },
    ],
  }),
  component: AiLocation,
});

function AiLocation() {
  return (
    <div className="min-h-screen">
      <SiteHeader />
      <main className="mx-auto max-w-[1320px] px-6 py-8">
        <header>
          <div className="font-mono-meta text-film inline-flex items-center gap-1.5"><Sparkles className="h-3 w-3"/> SLATE INTELLIGENCE</div>
          <h1 className="mt-2 font-display text-4xl">AI 로케이션 탐색</h1>
          <p className="mt-2 text-sm text-muted-foreground max-w-xl">씬 설명을 입력하면 지역 조건과 팀 맥락을 반영해 촬영지 후보를 정리합니다.</p>
        </header>

        <div className="mt-8 grid gap-8 lg:grid-cols-12">
          <section className="lg:col-span-5 space-y-5">
            <div className="rounded-lg border hairline-strong bg-card p-5">
              <label className="font-mono-meta">장면 설명</label>
              <textarea
                className="mt-2 w-full rounded-md border hairline bg-surface p-3 text-sm h-32 resize-none"
                defaultValue="1990년대 분위기의 인쇄골목 야간. 좁은 골목, 깜빡이는 네온 간판, 비 온 뒤 젖은 노면."
              />
              <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                <Field label="지역 조건" value="서울 중구 / 종로 / 영등포" />
                <Field label="추천 개수" value="6개" />
                <Field label="팀 맥락 포함" value="블루룸 픽처스 〈푸른 방〉" />
                <Field label="촬영 시간대" value="야간" />
              </div>
              <button className="mt-5 w-full rounded-md bg-film text-film-foreground py-2.5 text-sm font-medium inline-flex items-center justify-center gap-2">
                <Sparkles className="h-4 w-4" /> 추천 실행
              </button>
              <div className="font-mono-meta mt-3 text-center">예상 처리 시간 8초 · 시안 상태에서는 실제 호출 없음</div>
            </div>

            <div className="rounded-lg border hairline bg-card p-5">
              <div className="font-mono-meta">저장한 후보</div>
              <ul className="mt-3 space-y-2 text-sm">
                {locations.map((l) => (
                  <li key={l.id} className="flex items-center gap-2">
                    <Bookmark className="h-3.5 w-3.5 text-film" />
                    <span className="flex-1">{l.name}</span>
                    <span className={"font-mono-meta " + (l.saved === "팀" ? "text-film" : "")}>{l.saved}</span>
                  </li>
                ))}
              </ul>
            </div>
          </section>

          <section className="lg:col-span-7">
            <div className="rounded-lg border hairline bg-card overflow-hidden">
              <MapStub />
              <div className="divide-y hairline">
                {locations.map((l, i) => (
                  <div key={l.id} className="p-5 grid grid-cols-12 gap-4 items-start">
                    <div className="col-span-1 font-display text-3xl text-muted-foreground">{String(i + 1).padStart(2, "0")}</div>
                    <div className="col-span-7">
                      <div className="font-display text-xl">{l.name}</div>
                      <div className="font-mono-meta mt-1 inline-flex items-center gap-1.5"><MapPin className="h-3 w-3"/> {l.region}</div>
                      <p className="mt-3 text-sm text-muted-foreground">{l.reason}</p>
                      <p className="mt-2 text-xs inline-flex items-start gap-1.5 text-warning">
                        <AlertTriangle className="h-3.5 w-3.5 mt-0.5 shrink-0" /> {l.caution}
                      </p>
                    </div>
                    <div className="col-span-4 flex flex-col gap-2">
                      <button className="rounded-md border hairline-strong py-1.5 text-xs">개인 후보로 저장</button>
                      <button className="rounded-md bg-foreground text-background py-1.5 text-xs inline-flex items-center justify-center gap-1">
                        <Users className="h-3.5 w-3.5"/> 팀 후보로 저장
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </section>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border hairline bg-surface p-3">
      <div className="font-mono-meta">{label}</div>
      <div className="mt-1">{value}</div>
    </div>
  );
}

function MapStub() {
  return (
    <div className="relative h-64 border-b hairline" style={{
      backgroundImage:
        "linear-gradient(to right, oklch(0.18 0.02 70 / 6%) 1px, transparent 1px), linear-gradient(to bottom, oklch(0.18 0.02 70 / 6%) 1px, transparent 1px), radial-gradient(60% 60% at 30% 40%, oklch(0.65 0.16 65 / 14%), transparent 60%)",
      backgroundSize: "24px 24px, 24px 24px, 100% 100%",
    }}>
      <div className="absolute font-mono-meta top-3 left-4">MAP · 시안 (실제 지도 미연동)</div>
      {[
        { x: "28%", y: "44%", n: 1 },
        { x: "62%", y: "28%", n: 2 },
        { x: "78%", y: "70%", n: 3 },
      ].map((p) => (
        <div key={p.n} style={{ left: p.x, top: p.y }} className="absolute -translate-x-1/2 -translate-y-1/2">
          <div className="h-7 w-7 rounded-full bg-film text-film-foreground text-xs font-medium flex items-center justify-center ring-4 ring-background">
            {p.n}
          </div>
        </div>
      ))}
    </div>
  );
}
