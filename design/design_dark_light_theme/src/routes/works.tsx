import { createFileRoute } from "@tanstack/react-router";
import { SiteHeader, SiteFooter } from "@/components/site-chrome";
import { CoverArt } from "@/components/cover-art";
import { works, profiles } from "@/lib/mock";
import { Heart, MessageCircle, Flag, Trophy, Plus, Filter } from "lucide-react";

export const Route = createFileRoute("/works")({
  head: () => ({
    meta: [
      { title: "작업물 — Slate" },
      { name: "description", content: "단편, 뮤직비디오, 다큐, 광고. 제작자의 작업물을 보고 리뷰와 좋아요를 남기세요." },
      { property: "og:title", content: "작업물 게시판 — Slate" },
    ],
  }),
  component: WorksPage,
});

function WorksPage() {
  return (
    <div className="min-h-screen">
      <SiteHeader />
      <main className="mx-auto max-w-[1320px] px-6 py-8">
        <header className="flex items-end justify-between flex-wrap gap-4">
          <div>
            <div className="font-mono-meta">FEED</div>
            <h1 className="mt-2 font-display text-4xl">작업물</h1>
            <p className="mt-2 text-sm text-muted-foreground">완성된 작품과 작업 과정을 공유하고 리뷰와 좋아요로 신뢰를 쌓습니다.</p>
          </div>
          <div className="flex items-center gap-2">
            <button className="rounded-md border hairline-strong px-3 py-2 text-sm inline-flex items-center gap-1.5"><Filter className="h-4 w-4" /> 필터</button>
            <button className="rounded-md bg-foreground text-background px-3 py-2 text-sm inline-flex items-center gap-1.5"><Plus className="h-4 w-4" /> 게시</button>
          </div>
        </header>

        <div className="mt-6 inline-flex rounded-md border hairline p-1 bg-surface text-sm">
          {["전체", "단편", "뮤직비디오", "다큐", "광고", "자유글"].map((t, i) => (
            <button key={t} className={"px-3 py-1.5 rounded " + (i === 0 ? "bg-foreground text-background" : "text-muted-foreground")}>{t}</button>
          ))}
        </div>

        <div className="mt-8 grid gap-8 lg:grid-cols-12">
          <section className="lg:col-span-8 space-y-6">
            <Featured />
            <div className="grid sm:grid-cols-2 gap-6">
              {works.map((w) => (
                <article key={w.id} className="rounded-lg border hairline bg-card overflow-hidden group">
                  <div className="relative">
                    <CoverArt variant={["blueroom", "nightowl", "doc", "ad"][parseInt(w.id.slice(-1)) % 4]} meta={`${w.kind.toUpperCase()} · ${w.minutes ? w.minutes + " MIN" : "STILL"}`} />
                    {w.badge && (
                      <span className="absolute top-3 right-3 rounded-full bg-film text-film-foreground text-xs font-medium px-2.5 py-1">{w.badge}</span>
                    )}
                  </div>
                  <div className="p-5">
                    <div className="font-display text-xl">{w.title}</div>
                    <div className="font-mono-meta mt-1">{w.by} · {w.role} · {w.year}</div>
                    <div className="mt-4 flex items-center gap-4 text-sm text-muted-foreground">
                      <span className="inline-flex items-center gap-1"><Heart className="h-3.5 w-3.5" /> {w.likes}</span>
                      <span className="inline-flex items-center gap-1"><MessageCircle className="h-3.5 w-3.5" /> {w.reviews}</span>
                      <button className="ml-auto inline-flex items-center gap-1 hover:text-foreground"><Flag className="h-3.5 w-3.5" /> 신고</button>
                    </div>
                  </div>
                </article>
              ))}
            </div>

            <Reviews />
          </section>

          <aside className="lg:col-span-4 space-y-6">
            <RankCard title="이번 주 인기 작업물" items={works.map((w, i) => ({ rank: i + 1, title: w.title, sub: `${w.by} · ${w.role}`, n: w.likes }))} />
            <RankCard title="이번 주 인기 제작자" items={profiles.slice(0, 5).map((p, i) => ({ rank: i + 1, title: p.name, sub: `${p.role} · ${p.region}`, n: p.followers }))} />
            <div className="rounded-lg border hairline bg-card p-5">
              <div className="font-mono-meta inline-flex items-center gap-1.5 text-film"><Trophy className="h-3 w-3"/> 이주의 작업물</div>
              <div className="mt-3 font-display text-xl">산책 — 이서원</div>
              <p className="mt-1 text-sm text-muted-foreground">단편 부문 추천. 38건의 리뷰.</p>
            </div>
          </aside>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

function Featured() {
  return (
    <div className="relative overflow-hidden rounded-lg border hairline-strong">
      <CoverArt variant="blueroom" label="산책 — 단편 (8분)" meta="FEATURED · 이서원 감독·편집" ratio="21/9" />
      <div className="absolute right-4 bottom-4 flex gap-2">
        <button className="rounded-md bg-foreground text-background px-3 py-1.5 text-xs font-medium">재생</button>
        <button className="rounded-md border hairline-strong bg-background/60 backdrop-blur px-3 py-1.5 text-xs">상세 보기</button>
      </div>
    </div>
  );
}

function Reviews() {
  return (
    <div className="rounded-lg border hairline bg-card p-5">
      <div className="flex items-end justify-between">
        <div>
          <div className="font-mono-meta">REVIEWS</div>
          <h3 className="mt-1 font-display text-xl">‘산책’에 달린 리뷰</h3>
        </div>
        <span className="font-mono-meta">38개</span>
      </div>
      <ul className="mt-5 space-y-5">
        {[
          { who: "윤하정", role: "촬영감독", t: "흔들리는 핸드헬드와 정적 컷의 대비가 좋았어요. 사운드 디자인이 특히 인상적." },
          { who: "한미르", role: "색보정", t: "그레이딩 톤이 일관적. 미들 톤이 차가워서 인물 감정과 잘 맞았습니다." },
          { who: "박지운", role: "프로듀서", t: "9분 안에 이만큼의 호흡을 잡아낸 게 인상적입니다. 장편 가능성 충분." },
        ].map((r) => (
          <li key={r.who} className="flex gap-3">
            <div className="h-9 w-9 rounded-full bg-gradient-to-br from-film to-chart-2 ring-1 ring-border-strong shrink-0" />
            <div>
              <div className="text-sm">{r.who} <span className="text-muted-foreground">· {r.role}</span></div>
              <p className="mt-1 text-sm text-muted-foreground">{r.t}</p>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

function RankCard({ title, items }: { title: string; items: { rank: number; title: string; sub: string; n: number }[] }) {
  return (
    <div className="rounded-lg border hairline bg-card p-5">
      <div className="font-mono-meta">{title}</div>
      <ol className="mt-3 space-y-3">
        {items.map((i) => (
          <li key={i.rank} className="flex items-center gap-3">
            <span className="font-display text-2xl w-6 text-muted-foreground">{i.rank}</span>
            <div className="min-w-0 flex-1">
              <div className="text-sm truncate">{i.title}</div>
              <div className="font-mono-meta">{i.sub}</div>
            </div>
            <span className="font-mono-meta">{i.n.toLocaleString()}</span>
          </li>
        ))}
      </ol>
    </div>
  );
}
