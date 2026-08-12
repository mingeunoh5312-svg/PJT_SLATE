import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { SiteHeader, SiteFooter } from "@/components/site-chrome";
import { CoverArt } from "@/components/cover-art";
import { teams, profiles } from "@/lib/mock";
import { ArrowLeft, Bookmark, Calendar, MapPin, Users, CheckCircle2, Clock, XCircle, Send } from "lucide-react";

export const Route = createFileRoute("/teams/$teamId")({
  loader: ({ params }) => {
    const team = teams.find((t) => t.id === params.teamId);
    if (!team) throw notFound();
    return { team };
  },
  head: ({ loaderData }) => ({
    meta: [
      { title: `${loaderData?.team.title} — Slate` },
      { name: "description", content: loaderData?.team.summary },
      { property: "og:title", content: `${loaderData?.team.title} · ${loaderData?.team.name}` },
      { property: "og:description", content: loaderData?.team.summary },
    ],
  }),
  notFoundComponent: () => (
    <div className="min-h-screen flex items-center justify-center">팀을 찾을 수 없습니다.</div>
  ),
  errorComponent: () => (
    <div className="min-h-screen flex items-center justify-center">팀을 불러오지 못했습니다.</div>
  ),
  component: TeamDetail,
});

function TeamDetail() {
  const { team } = Route.useLoaderData();
  return (
    <div className="min-h-screen">
      <SiteHeader />
      <main className="mx-auto max-w-[1320px] px-6 py-8">
        <Link to="/discover" className="font-mono-meta inline-flex items-center gap-1 hover:text-foreground">
          <ArrowLeft className="h-3 w-3" /> 탐색으로
        </Link>

        <div className="mt-6 grid gap-8 lg:grid-cols-12">
          <div className="lg:col-span-8 space-y-8">
            <CoverArt variant={team.cover} label={team.title} meta={`${team.stage.toUpperCase()} · ${team.genre.toUpperCase()}`} ratio="21/9" />

            <div className="flex flex-wrap items-center gap-3">
              <span className="rounded-full bg-film/15 text-film px-3 py-1 text-xs font-medium">{team.stage}</span>
              <Pill icon={<MapPin className="h-3.5 w-3.5" />}>{team.region}</Pill>
              <Pill icon={<Calendar className="h-3.5 w-3.5" />}>{team.period}</Pill>
              <Pill icon={<Users className="h-3.5 w-3.5" />}>{team.members}/{team.capacity}명</Pill>
              <span className="font-mono-meta ml-auto">팀장 · {team.leader}</span>
            </div>

            <div>
              <h1 className="font-display text-4xl">{team.title}</h1>
              <p className="mt-3 text-muted-foreground max-w-2xl leading-relaxed">{team.summary}</p>
            </div>

            <section>
              <SectionTitle>모집 중인 역할 · {team.slots.length}</SectionTitle>
              <div className="mt-4 divide-y hairline rounded-lg border hairline overflow-hidden">
                {team.slots.map((s: { role: string; need: number; deadline: string; note: string }) => (
                  <div key={s.role} className="p-4 flex items-center gap-4 bg-card">
                    <div className="font-mono-meta w-16">SLOT</div>
                    <div className="flex-1 min-w-0">
                      <div className="font-display text-lg">{s.role}</div>
                      <div className="font-mono-meta">필요 {s.need}명 · 마감 {s.deadline}{s.note ? ` · ${s.note}` : ""}</div>
                    </div>
                    <button className="rounded-md border hairline-strong px-4 py-2 text-sm hover:bg-surface inline-flex items-center gap-2">
                      <Send className="h-3.5 w-3.5" /> 이 역할로 지원
                    </button>
                  </div>
                ))}
              </div>
            </section>

            <section>
              <SectionTitle>팀 일정 — 프로덕션 캘린더</SectionTitle>
              <ol className="mt-4 relative border-l hairline pl-6 space-y-5">
                {[
                  { d: "07.02", t: "촬영감독 마감", done: false },
                  { d: "07.10", t: "로케이션 헌팅 (을지로)", done: false },
                  { d: "07.18", t: "리허설 · 본녹음 테스트", done: false },
                  { d: "07.25", t: "촬영 1차 · 4일", done: false },
                  { d: "08.10", t: "오프라인 편집 1차", done: false },
                  { d: "09.02", t: "색보정 · 사운드 믹스", done: false },
                ].map((e) => (
                  <li key={e.d} className="relative">
                    <span className="absolute -left-[29px] top-1 h-2 w-2 rounded-full bg-film ring-4 ring-background" />
                    <div className="font-mono-meta">{e.d}</div>
                    <div className="text-sm">{e.t}</div>
                  </li>
                ))}
              </ol>
            </section>

            <section>
              <SectionTitle>팀 작업물</SectionTitle>
              <div className="mt-4 grid sm:grid-cols-3 gap-4">
                {[
                  { l: "로케이션 무드 보드", m: "PRE-PROD" },
                  { l: "캐스팅 노트", m: "PRE-PROD" },
                  { l: "스토리보드 v2", m: "PRE-PROD" },
                ].map((w) => (
                  <div key={w.l} className="rounded-lg border hairline overflow-hidden">
                    <CoverArt variant="generic" meta={w.m} ratio="4/3" />
                    <div className="p-3 text-sm">{w.l}</div>
                  </div>
                ))}
              </div>
            </section>
          </div>

          <aside className="lg:col-span-4 space-y-6">
            <div className="rounded-lg border hairline-strong bg-card p-5">
              <div className="font-mono-meta">지원 / 저장</div>
              <button className="mt-3 w-full rounded-md bg-foreground text-background py-2.5 text-sm font-medium">팀 지원하기</button>
              <button className="mt-2 w-full rounded-md border hairline-strong py-2.5 text-sm inline-flex items-center justify-center gap-2 hover:bg-surface">
                <Bookmark className="h-4 w-4" /> 저장
              </button>
              <div className="font-mono-meta mt-4">현재 상태</div>
              <div className="text-sm mt-1 inline-flex items-center gap-1.5">
                <Clock className="h-3.5 w-3.5 text-warning" /> 지원 검토 중 — 평균 3일
              </div>
            </div>

            <div className="rounded-lg border hairline bg-card p-5">
              <div className="font-mono-meta">팀원 {team.members}명</div>
              <ul className="mt-3 space-y-3">
                {profiles.slice(0, team.members).map((p) => (
                  <li key={p.id} className="flex items-center gap-3">
                    <div className="h-9 w-9 rounded-full bg-gradient-to-br from-film to-chart-2 ring-1 ring-border-strong" />
                    <div className="flex-1 min-w-0">
                      <div className="text-sm truncate">{p.name}</div>
                      <div className="font-mono-meta">{p.role}</div>
                    </div>
                    {p.name === team.leader && <span className="font-mono-meta text-film">LEAD</span>}
                  </li>
                ))}
              </ul>
            </div>

            <div className="rounded-lg border hairline bg-card p-5">
              <div className="font-mono-meta">지원 현황</div>
              <ul className="mt-3 space-y-2 text-sm">
                <Status icon={<CheckCircle2 className="h-3.5 w-3.5 text-success" />} label="수락" n={4} />
                <Status icon={<Clock className="h-3.5 w-3.5 text-warning" />} label="검토 중" n={11} />
                <Status icon={<XCircle className="h-3.5 w-3.5 text-muted-foreground" />} label="거절/취소" n={6} />
              </ul>
            </div>
          </aside>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

function Pill({ children, icon }: { children: React.ReactNode; icon?: React.ReactNode }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border hairline px-3 py-1 text-xs text-muted-foreground">
      {icon}
      {children}
    </span>
  );
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return <h2 className="font-display text-2xl">{children}</h2>;
}

function Status({ icon, label, n }: { icon: React.ReactNode; label: string; n: number }) {
  return (
    <li className="flex items-center justify-between">
      <span className="inline-flex items-center gap-2">{icon}{label}</span>
      <span className="font-mono-meta">{n}</span>
    </li>
  );
}
