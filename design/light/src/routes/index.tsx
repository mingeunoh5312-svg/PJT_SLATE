import { createFileRoute, Link } from "@tanstack/react-router";
import { SiteHeader, SiteFooter } from "@/components/site-chrome";
import { CoverArt } from "@/components/cover-art";
import { teams, profiles, contests, works } from "@/lib/mock";
import {
  ArrowRight,
  Sparkles,
  Users,
  Film,
  MapPin,
  Trophy,
  CheckCircle2,
  Clapperboard,
} from "lucide-react";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Slate — 영화·영상 제작 협업 플랫폼" },
      {
        name: "description",
        content:
          "팀과 제작자가 서로를 찾고, 작업물·공모전·AI 로케이션 탐색을 한 곳에서. Slate에서 다음 프로젝트를 시작하세요.",
      },
      { property: "og:title", content: "Slate — 다음 프로젝트가 시작되는 곳" },
      {
        property: "og:description",
        content: "제작자·팀·기업·운영자를 잇는 영상 제작 협업 플랫폼.",
      },
    ],
  }),
  component: Home,
});

function Home() {
  return (
    <div className="min-h-screen">
      <SiteHeader />
      <main>
        <Hero />
        <Personas />
        <FeatureGrid />
        <SpotlightDiscover />
        <AiStrip />
        <ContestsTeaser />
        <ClosingCta />
      </main>
      <SiteFooter />
    </div>
  );
}

function Hero() {
  return (
    <section className="relative overflow-hidden border-b hairline">
      <div className="absolute inset-0 film-gradient" />
      <div className="relative mx-auto max-w-[1320px] px-6 pt-20 pb-24">
        <div className="grid gap-12 lg:grid-cols-12 items-end">
          <div className="lg:col-span-7">
            <div className="font-mono-meta mb-6 flex items-center gap-3">
              <span className="inline-flex h-1.5 w-1.5 rounded-full bg-film" />
              EST. 2026 · CREW + PROJECT NETWORK
            </div>
            <h1 className="font-display text-5xl md:text-7xl leading-[0.95]">
              다음 프로젝트는,
              <br />
              <span className="text-film">맞는 사람과</span> 시작합니다.
            </h1>
            <p className="mt-6 max-w-xl text-lg text-muted-foreground leading-relaxed">
              Slate는 영화·영상 제작자, 팀, 기업, 운영자를 잇는 협업 플랫폼입니다.
              역할과 포트폴리오, 일정과 지역을 기반으로 팀과 사람을 매칭하고,
              작업물·공모전·로케이션 탐색까지 한 곳에서 정리합니다.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-3">
              <Link
                to="/discover"
                className="inline-flex items-center gap-2 rounded-md bg-foreground px-5 py-3 text-sm font-medium text-background hover:opacity-90"
              >
                팀·제작자 탐색하기 <ArrowRight className="h-4 w-4" />
              </Link>
              <Link
                to="/workspace"
                className="inline-flex items-center gap-2 rounded-md border hairline-strong px-5 py-3 text-sm hover:bg-surface"
              >
                <Clapperboard className="h-4 w-4" /> 팀 만들기
              </Link>
              <span className="font-mono-meta ml-2">
                · 가입 없이 둘러보기
              </span>
            </div>
            <dl className="mt-12 grid grid-cols-3 gap-6 max-w-lg">
              <Stat n="2,148" l="활성 제작자" />
              <Stat n="312" l="모집 중 팀" />
              <Stat n="64" l="진행 중 공모전" />
            </dl>
          </div>

          <div className="lg:col-span-5">
            <div className="relative">
              <CoverArt
                variant="blueroom"
                label="단편 〈푸른 방〉"
                meta="SLATE A · SCN 014 · TAKE 03"
                ratio="4/5"
              />
              <div className="absolute -bottom-5 -left-5 rounded-lg border hairline-strong bg-surface-2 p-4 shadow-2xl w-64">
                <div className="font-mono-meta mb-2 flex items-center gap-1.5">
                  <Sparkles className="h-3 w-3 text-film" /> AI 매칭 추천
                </div>
                <div className="text-sm">윤하정 · 촬영감독</div>
                <div className="mt-1 text-xs text-muted-foreground">
                  적합도 92% · 야간/스릴러 경험 일치
                </div>
                <div className="mt-3 h-1 w-full rounded-full bg-border">
                  <div className="h-1 rounded-full bg-film" style={{ width: "92%" }} />
                </div>
              </div>
              <div className="absolute -top-4 -right-4 rounded-lg border hairline-strong bg-surface-2 px-3 py-2 text-xs">
                <span className="font-mono-meta">D-24</span>
                <div>서울독립영화제 마감</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function Stat({ n, l }: { n: string; l: string }) {
  return (
    <div>
      <div className="font-display text-3xl">{n}</div>
      <div className="font-mono-meta mt-1">{l}</div>
    </div>
  );
}

function Personas() {
  const items = [
    {
      tag: "FOR CREATORS",
      title: "제작자",
      copy: "역할·장르·지역·일정을 정리하고, 포트폴리오로 신뢰를 쌓아 팀에 합류합니다.",
      cta: "프로필 만들기",
    },
    {
      tag: "FOR TEAMS",
      title: "팀 리더",
      copy: "모집 공고와 구인 slot을 관리하고, 지원·초대·일정·작업물을 한 보드에서.",
      cta: "팀 워크스페이스",
    },
    {
      tag: "FOR COMPANIES",
      title: "기업",
      copy: "승인 후 공모전을 개설하고 요청 상태와 결과를 추적합니다.",
      cta: "공모전 개설",
    },
    {
      tag: "FOR OPERATORS",
      title: "운영자",
      copy: "회원·팀·신고·공모전·권한·로그·매칭 정책을 한 대시보드에서 운영합니다.",
      cta: "운영 콘솔",
    },
  ];
  return (
    <section className="border-b hairline">
      <div className="mx-auto max-w-[1320px] px-6 py-16">
        <SectionHeader
          eyebrow="누구를 위한 도구인가"
          title="네 종류의 사용자, 하나의 플랫폼"
        />
        <div className="mt-10 grid gap-px bg-border border hairline rounded-lg overflow-hidden md:grid-cols-4">
          {items.map((i) => (
            <div key={i.title} className="bg-background p-6 hover:bg-surface transition-colors">
              <div className="font-mono-meta">{i.tag}</div>
              <div className="mt-3 font-display text-2xl">{i.title}</div>
              <p className="mt-2 text-sm text-muted-foreground leading-relaxed min-h-[64px]">
                {i.copy}
              </p>
              <div className="mt-4 inline-flex items-center gap-1 text-sm text-film">
                {i.cta} <ArrowRight className="h-3.5 w-3.5" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function FeatureGrid() {
  return (
    <section className="border-b hairline">
      <div className="mx-auto max-w-[1320px] px-6 py-20">
        <SectionHeader
          eyebrow="핵심 기능"
          title="흩어진 제작 정보를, 한 흐름으로"
        />
        <div className="mt-10 grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          <Feature
            icon={<Users className="h-5 w-5" />}
            title="양방향 매칭"
            body="팀이 사람을 찾고, 제작자가 팀을 찾는 두 방향을 동시에 지원합니다. 역할·경력·지역·일정 기반."
          />
          <Feature
            icon={<Clapperboard className="h-5 w-5" />}
            title="팀 워크스페이스"
            body="모집 공고, 구인 slot, 지원/초대, 일정, 작업물 승인을 하나의 보드에서."
          />
          <Feature
            icon={<Film className="h-5 w-5" />}
            title="포트폴리오"
            body="직접 업로드, YouTube 링크, KOBIS·공공데이터 기반 작품 이력을 한 프로필에."
          />
          <Feature
            icon={<MapPin className="h-5 w-5" />}
            title="AI 로케이션 탐색"
            body="장면 설명과 지역 조건으로 촬영지 후보를 추천. 개인·팀 후보로 저장."
            ai
          />
          <Feature
            icon={<Trophy className="h-5 w-5" />}
            title="공모전"
            body="국내외 공모전 탐색, 저장, 프로필·팀 기준 적합도 분석, 제출 준비 체크리스트."
          />
          <Feature
            icon={<CheckCircle2 className="h-5 w-5" />}
            title="운영 콘솔"
            body="회원·팀·신고·기업 승인·공모전·권한·매칭 정책을 운영자가 직접 관리."
          />
        </div>
      </div>
    </section>
  );
}

function Feature({
  icon,
  title,
  body,
  ai,
}: {
  icon: React.ReactNode;
  title: string;
  body: string;
  ai?: boolean;
}) {
  return (
    <div className="group rounded-lg border hairline bg-card p-6 hover:border-strong hover:bg-surface-2 transition">
      <div className="flex items-center justify-between">
        <div className="inline-flex h-9 w-9 items-center justify-center rounded-md bg-surface text-foreground">
          {icon}
        </div>
        {ai ? (
          <span className="font-mono-meta inline-flex items-center gap-1 text-film">
            <Sparkles className="h-3 w-3" /> AI
          </span>
        ) : null}
      </div>
      <div className="mt-5 font-display text-xl">{title}</div>
      <p className="mt-2 text-sm text-muted-foreground leading-relaxed">{body}</p>
    </div>
  );
}

function SpotlightDiscover() {
  return (
    <section className="border-b hairline">
      <div className="mx-auto max-w-[1320px] px-6 py-20">
        <div className="flex items-end justify-between gap-6 flex-wrap">
          <SectionHeader eyebrow="지금 모집 중" title="진행 중 프로젝트와 사람들" />
          <Link to="/discover" className="text-sm text-muted-foreground hover:text-foreground inline-flex items-center gap-1">
            전체 보기 <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
        <div className="mt-10 grid gap-6 lg:grid-cols-12">
          <div className="lg:col-span-8 grid sm:grid-cols-2 gap-6">
            {teams.slice(0, 4).map((t) => (
              <Link
                to="/teams/$teamId"
                params={{ teamId: t.id }}
                key={t.id}
                className="group rounded-lg border hairline bg-card overflow-hidden hover:border-strong"
              >
                <CoverArt variant={t.cover} meta={`${t.stage.toUpperCase()} · ${t.genre.toUpperCase()}`} />
                <div className="p-4">
                  <div className="flex items-center justify-between">
                    <div className="font-mono-meta">{t.region} · {t.period}</div>
                    <div className="font-mono-meta text-film">+{t.open} 모집</div>
                  </div>
                  <div className="mt-2 font-display text-xl">{t.title}</div>
                  <p className="mt-1 text-sm text-muted-foreground line-clamp-2">{t.summary}</p>
                </div>
              </Link>
            ))}
          </div>
          <aside className="lg:col-span-4 rounded-lg border hairline bg-card p-5">
            <div className="font-mono-meta">RECOMMENDED CREW</div>
            <div className="mt-1 font-display text-xl">팀에 맞는 제작자</div>
            <ul className="mt-5 divide-y hairline">
              {profiles.slice(0, 5).map((p) => (
                <li key={p.id} className="py-3 flex items-center gap-3">
                  <div className="h-10 w-10 rounded-full bg-gradient-to-br from-film to-chart-2 ring-1 ring-border-strong" />
                  <div className="min-w-0 flex-1">
                    <div className="text-sm">{p.name} <span className="text-muted-foreground">· {p.role}</span></div>
                    <div className="font-mono-meta">{p.region} · {p.years}yr · {p.tags[0]}</div>
                  </div>
                  <div className="text-right">
                    <div className="text-sm">{p.score}<span className="text-muted-foreground text-xs">%</span></div>
                    <div className="font-mono-meta">FIT</div>
                  </div>
                </li>
              ))}
            </ul>
          </aside>
        </div>
      </div>
    </section>
  );
}

function AiStrip() {
  return (
    <section className="border-b hairline">
      <div className="mx-auto max-w-[1320px] px-6 py-20">
        <div className="grid gap-10 lg:grid-cols-12 items-center">
          <div className="lg:col-span-5">
            <div className="font-mono-meta text-film">SLATE INTELLIGENCE</div>
            <h2 className="mt-3 font-display text-4xl md:text-5xl leading-tight">
              장면을 설명하면,
              <br />
              로케이션이 떠오릅니다.
            </h2>
            <p className="mt-4 text-muted-foreground max-w-lg">
              씬 설명과 지역 조건을 입력하면 Slate가 촬영지 후보를 정리해 보여줍니다.
              개인 또는 팀 후보로 저장하고, 팀 워크스페이스에서 함께 검토하세요.
            </p>
            <Link
              to="/ai/location"
              className="mt-6 inline-flex items-center gap-2 rounded-md bg-film px-5 py-3 text-sm font-medium text-film-foreground hover:opacity-90"
            >
              <Sparkles className="h-4 w-4" /> 로케이션 탐색 열기
            </Link>
          </div>
          <div className="lg:col-span-7 rounded-lg border hairline bg-card p-6">
            <div className="flex items-start gap-3">
              <Sparkles className="h-4 w-4 text-film mt-1" />
              <div className="flex-1">
                <div className="font-mono-meta">PROMPT</div>
                <p className="mt-1 text-sm">
                  “1990년대 인쇄골목 야간, 네온 간판이 비치는 좁은 골목 — 서울 중구 기준”
                </p>
              </div>
              <button className="font-mono-meta rounded-md border hairline px-2 py-1">RE-RUN</button>
            </div>
            <div className="mt-5 grid sm:grid-cols-3 gap-3">
              {[
                { name: "을지로 4가 인쇄골목", reason: "낡은 간판·네온", reg: "서울 중구" },
                { name: "창신동 봉제거리", reason: "좁은 골목·야경", reg: "서울 종로" },
                { name: "문래동 철공소 일대", reason: "기름때·산업톤", reg: "서울 영등포" },
              ].map((l) => (
                <div key={l.name} className="rounded-md border hairline p-3">
                  <div className="font-mono-meta">{l.reg}</div>
                  <div className="mt-1 text-sm">{l.name}</div>
                  <div className="mt-1 text-xs text-muted-foreground">{l.reason}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function ContestsTeaser() {
  return (
    <section className="border-b hairline">
      <div className="mx-auto max-w-[1320px] px-6 py-20">
        <div className="flex items-end justify-between flex-wrap gap-6">
          <SectionHeader eyebrow="공모전" title="저장한 공모전과 적합도" />
          <Link to="/contests" className="text-sm text-muted-foreground hover:text-foreground inline-flex items-center gap-1">
            공모전 전체 <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>
        <div className="mt-10 grid gap-4 md:grid-cols-2">
          {contests.slice(0, 4).map((c) => (
            <div key={c.id} className="rounded-lg border hairline bg-card p-5 flex gap-4">
              <div className="w-16 shrink-0 text-center">
                <div className={"font-display text-3xl " + (c.dleft < 0 ? "text-muted-foreground" : c.dleft <= 10 ? "text-film" : "")}>
                  {c.dleft < 0 ? "—" : "D-" + c.dleft}
                </div>
                <div className="font-mono-meta mt-1">{c.status}</div>
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <div className="font-display text-lg">{c.title}</div>
                  {c.saved && <span className="font-mono-meta text-film">SAVED</span>}
                </div>
                <div className="font-mono-meta mt-1">{c.host} · {c.region} · {c.prize}</div>
                <div className="mt-3 flex items-center gap-3">
                  <div className="h-1.5 flex-1 rounded-full bg-border overflow-hidden">
                    <div className="h-full bg-film" style={{ width: c.fit + "%" }} />
                  </div>
                  <div className="text-sm">{c.fit}% <span className="text-muted-foreground text-xs">FIT</span></div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function ClosingCta() {
  return (
    <section>
      <div className="mx-auto max-w-[1320px] px-6 py-24">
        <div className="relative overflow-hidden rounded-2xl border hairline-strong p-12 md:p-16">
          <div className="absolute inset-0 film-gradient" />
          <div className="relative max-w-2xl">
            <div className="font-mono-meta text-film">START YOUR NEXT SLATE</div>
            <h2 className="mt-4 font-display text-4xl md:text-5xl leading-tight">
              이름만 있는 프로젝트를,
              <br />
              크레딧이 있는 작품으로.
            </h2>
            <p className="mt-4 text-muted-foreground">
              가입 없이도 둘러볼 수 있습니다. 준비가 되면 프로필을 만들고 팀에 합류하거나, 직접 팀을 꾸려보세요.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/discover" className="rounded-md bg-foreground px-5 py-3 text-sm font-medium text-background">탐색 시작</Link>
              <Link to="/workspace" className="rounded-md border hairline-strong px-5 py-3 text-sm">팀 만들기</Link>
              <Link to="/works" className="rounded-md px-5 py-3 text-sm text-muted-foreground hover:text-foreground">작업물 둘러보기</Link>
            </div>
          </div>
        </div>
        <div className="mt-10 grid gap-6 md:grid-cols-3 text-sm">
          {works.slice(0, 3).map((w) => (
            <div key={w.id} className="flex items-center gap-4">
              <CoverArt variant="generic" ratio="1/1" className="w-20 shrink-0" meta="REEL" />
              <div>
                <div className="font-mono-meta">{w.kind} · {w.year}</div>
                <div className="font-display text-lg">{w.title}</div>
                <div className="text-xs text-muted-foreground">{w.by} · {w.role}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function SectionHeader({ eyebrow, title }: { eyebrow: string; title: string }) {
  return (
    <div>
      <div className="font-mono-meta">{eyebrow}</div>
      <h2 className="mt-2 font-display text-3xl md:text-4xl">{title}</h2>
    </div>
  );
}
