import { createFileRoute, Link } from "@tanstack/react-router";
import { SiteHeader, SiteFooter } from "@/components/site-chrome";
import { CoverArt } from "@/components/cover-art";
import { teams, profiles, roles, genres, regions } from "@/lib/mock";
import { Search, SlidersHorizontal, Sparkles, Users, Bookmark, ArrowRight } from "lucide-react";
import { useState } from "react";

export const Route = createFileRoute("/discover")({
  head: () => ({
    meta: [
      { title: "탐색 — Slate" },
      { name: "description", content: "팀과 제작자를 양방향으로 매칭. 역할·장르·지역·일정 기반으로 필터링하고 AI 추천을 확인하세요." },
      { property: "og:title", content: "Slate 탐색 — 팀과 사람을 잇다" },
    ],
  }),
  component: Discover,
});

function Discover() {
  const [tab, setTab] = useState<"teams" | "people">("teams");
  return (
    <div className="min-h-screen">
      <SiteHeader />
      <main className="mx-auto max-w-[1320px] px-6 py-10">
        <header className="flex items-end justify-between flex-wrap gap-4">
          <div>
            <div className="font-mono-meta">DISCOVER</div>
            <h1 className="mt-2 font-display text-4xl">탐색</h1>
            <p className="mt-2 text-muted-foreground max-w-xl text-sm">
              팀이 사람을 찾고, 제작자가 팀을 찾는 두 방향을 모두 지원합니다. 필터, 키워드, AI 추천을 함께 사용하세요.
            </p>
          </div>
          <div className="inline-flex rounded-md border hairline p-1 bg-surface">
            <button
              onClick={() => setTab("teams")}
              className={"px-4 py-1.5 text-sm rounded " + (tab === "teams" ? "bg-foreground text-background" : "text-muted-foreground")}
            >
              모집 중 팀
            </button>
            <button
              onClick={() => setTab("people")}
              className={"px-4 py-1.5 text-sm rounded " + (tab === "people" ? "bg-foreground text-background" : "text-muted-foreground")}
            >
              제작자
            </button>
          </div>
        </header>

        <div className="mt-8 grid gap-8 lg:grid-cols-12">
          <aside className="lg:col-span-3 space-y-6">
            <FilterSearch />
            <FilterGroup title="역할" items={roles.slice(0, 8)} selected={["촬영감독"]} />
            <FilterGroup title="장르" items={genres.slice(0, 6)} selected={["드라마", "다큐"]} />
            <FilterGroup title="지역" items={regions.slice(0, 6)} selected={["서울"]} />
            <FilterGroup title="경력" items={["주니어", "미들", "시니어"]} selected={["미들"]} />
            <button className="w-full rounded-md border hairline-strong py-2 text-sm hover:bg-surface inline-flex items-center justify-center gap-2">
              <SlidersHorizontal className="h-4 w-4" /> 고급 필터
            </button>
          </aside>

          <section className="lg:col-span-9 space-y-6">
            <AiBanner kind={tab} />
            {tab === "teams" ? <TeamGrid /> : <PeopleGrid />}
          </section>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

function FilterSearch() {
  return (
    <div>
      <div className="font-mono-meta mb-2">검색</div>
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <input
          placeholder="키워드, 작품명, 팀명"
          className="w-full rounded-md border hairline bg-surface pl-9 pr-3 py-2 text-sm placeholder:text-muted-foreground focus:outline-none focus:border-strong"
        />
      </div>
    </div>
  );
}

function FilterGroup({ title, items, selected }: { title: string; items: string[]; selected: string[] }) {
  return (
    <div>
      <div className="font-mono-meta mb-2">{title}</div>
      <div className="flex flex-wrap gap-1.5">
        {items.map((i) => {
          const on = selected.includes(i);
          return (
            <span
              key={i}
              className={
                "rounded-full border px-3 py-1 text-xs cursor-pointer " +
                (on
                  ? "bg-foreground text-background border-foreground"
                  : "hairline text-muted-foreground hover:text-foreground")
              }
            >
              {i}
            </span>
          );
        })}
      </div>
    </div>
  );
}

function AiBanner({ kind }: { kind: "teams" | "people" }) {
  return (
    <div className="rounded-lg border hairline bg-card p-5 flex items-start gap-4">
      <div className="inline-flex h-10 w-10 items-center justify-center rounded-md bg-film/15 text-film">
        <Sparkles className="h-5 w-5" />
      </div>
      <div className="flex-1">
        <div className="font-mono-meta text-film">AI RECOMMENDATION</div>
        <div className="font-display text-lg mt-1">
          {kind === "teams" ? "프로필 기준 가장 맞는 팀 12개를 골랐습니다." : "팀의 모집 slot에 적합한 제작자 18명을 골랐습니다."}
        </div>
        <div className="text-xs text-muted-foreground mt-1">필터 변경 시 자동 재계산됩니다.</div>
      </div>
      <button className="rounded-md border hairline-strong px-3 py-1.5 text-xs hover:bg-surface">추천 이유 보기</button>
    </div>
  );
}

function TeamGrid() {
  return (
    <div className="grid gap-5 sm:grid-cols-2">
      {teams.map((t) => (
        <Link
          to="/teams/$teamId"
          params={{ teamId: t.id }}
          key={t.id}
          className="group rounded-lg border hairline bg-card overflow-hidden hover:border-strong"
        >
          <CoverArt variant={t.cover} meta={`${t.stage.toUpperCase()} · ${t.genre.toUpperCase()}`} />
          <div className="p-5">
            <div className="flex items-center justify-between">
              <div className="font-mono-meta">{t.region}</div>
              <div className="font-mono-meta text-film">+{t.open} 모집</div>
            </div>
            <div className="mt-2 font-display text-xl">{t.title}</div>
            <p className="mt-1 text-sm text-muted-foreground line-clamp-2">{t.summary}</p>
            <div className="mt-4 flex items-center justify-between">
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <Users className="h-3.5 w-3.5" /> {t.members}/{t.capacity}명
              </div>
              <div className="font-mono-meta">{t.period}</div>
            </div>
            <div className="mt-4 flex flex-wrap gap-1.5">
              {t.slots.map((s) => (
                <span key={s.role} className="rounded border hairline px-2 py-0.5 text-xs">
                  {s.role} · {s.need}
                </span>
              ))}
            </div>
            <div className="mt-4 flex items-center justify-between border-t hairline pt-3">
              <button className="text-xs text-muted-foreground hover:text-foreground inline-flex items-center gap-1">
                <Bookmark className="h-3.5 w-3.5" /> 저장
              </button>
              <span className="text-xs inline-flex items-center gap-1 text-film">
                팀 보기 <ArrowRight className="h-3.5 w-3.5" />
              </span>
            </div>
          </div>
        </Link>
      ))}
    </div>
  );
}

function PeopleGrid() {
  return (
    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      {profiles.map((p) => (
        <Link
          to="/profiles/$profileId"
          params={{ profileId: p.id }}
          key={p.id}
          className="rounded-lg border hairline bg-card p-5 hover:border-strong"
        >
          <div className="flex items-center gap-3">
            <div className="h-12 w-12 rounded-full bg-gradient-to-br from-film to-chart-2 ring-1 ring-border-strong" />
            <div className="min-w-0 flex-1">
              <div className="font-display text-lg leading-tight truncate">{p.name}</div>
              <div className="font-mono-meta">{p.handle} · {p.region}</div>
            </div>
            <div className="text-right">
              <div className="font-display text-2xl text-film leading-none">{p.score}</div>
              <div className="font-mono-meta">FIT</div>
            </div>
          </div>
          <div className="mt-4 grid grid-cols-3 gap-2 text-center">
            <Mini label="역할" value={p.role} />
            <Mini label="경력" value={`${p.years}년`} />
            <Mini label="작업물" value={String(p.works)} />
          </div>
          <p className="mt-4 text-sm text-muted-foreground line-clamp-2">{p.bio}</p>
          <div className="mt-3 flex flex-wrap gap-1.5">
            {p.tags.map((t) => (
              <span key={t} className="rounded border hairline px-2 py-0.5 text-xs">{t}</span>
            ))}
          </div>
          <div className="mt-4 rounded-md bg-surface p-3 text-xs text-muted-foreground flex items-start gap-2">
            <Sparkles className="h-3.5 w-3.5 text-film mt-0.5" />
            <span>{p.reason}</span>
          </div>
        </Link>
      ))}
    </div>
  );
}

function Mini({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border hairline py-2">
      <div className="font-mono-meta">{label}</div>
      <div className="text-sm mt-0.5">{value}</div>
    </div>
  );
}
