import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { SiteHeader, SiteFooter } from "@/components/site-chrome";
import { CoverArt } from "@/components/cover-art";
import { profiles } from "@/lib/mock";
import { ArrowLeft, MapPin, Calendar, Film, Heart, MessageCircle, Plus, UserPlus, Sparkles } from "lucide-react";

export const Route = createFileRoute("/profiles/$profileId")({
  loader: ({ params }) => {
    const profile = profiles.find((p) => p.id === params.profileId);
    if (!profile) throw notFound();
    return { profile };
  },
  head: ({ loaderData }) => ({
    meta: [
      { title: `${loaderData?.profile.name} — Slate` },
      { name: "description", content: loaderData?.profile.bio },
      { property: "og:title", content: `${loaderData?.profile.name} · ${loaderData?.profile.role}` },
      { property: "og:description", content: loaderData?.profile.bio },
    ],
  }),
  notFoundComponent: () => <div className="min-h-screen flex items-center justify-center">프로필을 찾을 수 없습니다.</div>,
  errorComponent: () => <div className="min-h-screen flex items-center justify-center">불러오기 실패</div>,
  component: ProfilePage,
});

function ProfilePage() {
  const { profile: p } = Route.useLoaderData();
  return (
    <div className="min-h-screen">
      <SiteHeader />
      <main className="mx-auto max-w-[1320px] px-6 py-8">
        <Link to="/discover" className="font-mono-meta inline-flex items-center gap-1 hover:text-foreground">
          <ArrowLeft className="h-3 w-3" /> 탐색
        </Link>

        <header className="mt-6 grid gap-8 lg:grid-cols-12 items-start">
          <div className="lg:col-span-8">
            <div className="flex items-start gap-5">
              <div className="h-24 w-24 rounded-2xl bg-gradient-to-br from-film to-chart-2 ring-1 ring-border-strong" />
              <div className="flex-1">
                <div className="font-mono-meta">{p.handle}</div>
                <h1 className="mt-1 font-display text-5xl">{p.name}</h1>
                <div className="mt-3 flex flex-wrap gap-2">
                  <Pill>{p.role}</Pill>
                  <Pill icon={<MapPin className="h-3.5 w-3.5" />}>{p.region}</Pill>
                  <Pill icon={<Calendar className="h-3.5 w-3.5" />}>{p.available}</Pill>
                  <Pill icon={<Film className="h-3.5 w-3.5" />}>경력 {p.years}년</Pill>
                </div>
                <p className="mt-4 text-muted-foreground max-w-xl">{p.bio}</p>
              </div>
            </div>

            <section className="mt-10">
              <SectionTitle eyebrow="PORTFOLIO" title="포트폴리오" actions={
                <button className="font-mono-meta inline-flex items-center gap-1 hover:text-foreground"><Plus className="h-3 w-3"/> 추가</button>
              }/>
              <div className="mt-4 grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
                {Array.from({ length: 6 }).map((_, i) => (
                  <div key={i} className="group rounded-lg border hairline overflow-hidden bg-card">
                    <CoverArt variant={i % 2 ? "nightowl" : "blueroom"} meta={`REEL ${String(i + 1).padStart(2, "0")}`} />
                    <div className="p-4">
                      <div className="font-mono-meta">{i % 2 ? "MUSIC VIDEO" : "SHORT FILM"} · 202{4 + (i % 2)}</div>
                      <div className="mt-1 font-display text-lg">{["산책", "Owl", "푸른 방", "마지막 일요일", "검은 강", "리허설"][i]}</div>
                      <div className="mt-1 text-xs text-muted-foreground">크레딧 · {p.role}</div>
                      <div className="mt-3 flex items-center gap-4 text-xs text-muted-foreground">
                        <span className="inline-flex items-center gap-1"><Heart className="h-3 w-3" /> {120 + i * 31}</span>
                        <span className="inline-flex items-center gap-1"><MessageCircle className="h-3 w-3" /> {12 + i * 3}</span>
                        {i === 0 && <span className="ml-auto font-mono-meta text-film">YOUTUBE</span>}
                        {i === 2 && <span className="ml-auto font-mono-meta text-film">KOBIS</span>}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </section>

            <section className="mt-12">
              <SectionTitle eyebrow="CREDITS" title="공공데이터 기반 참여 이력" />
              <div className="mt-4 rounded-lg border hairline bg-card overflow-hidden">
                <table className="w-full text-sm">
                  <thead className="bg-surface text-left">
                    <tr className="[&>th]:py-2.5 [&>th]:px-4 [&>th]:font-mono-meta [&>th]:text-muted-foreground">
                      <th>연도</th><th>작품</th><th>유형</th><th>크레딧</th><th>출처</th>
                    </tr>
                  </thead>
                  <tbody className="[&>tr]:border-t [&>tr]:hairline [&>tr>td]:py-3 [&>tr>td]:px-4">
                    <tr><td>2025</td><td>산책</td><td>단편</td><td>{p.role}</td><td className="font-mono-meta">KOBIS</td></tr>
                    <tr><td>2024</td><td>마지막 일요일</td><td>장편</td><td>{p.role}</td><td className="font-mono-meta">KOBIS</td></tr>
                    <tr><td>2024</td><td>Owl</td><td>뮤직비디오</td><td>{p.role}</td><td className="font-mono-meta">YOUTUBE</td></tr>
                    <tr><td>2023</td><td>검은 강</td><td>단편</td><td>{p.role}</td><td className="font-mono-meta">DIRECT</td></tr>
                  </tbody>
                </table>
              </div>
            </section>
          </div>

          <aside className="lg:col-span-4 space-y-5 lg:sticky lg:top-20">
            <div className="rounded-lg border hairline-strong bg-card p-5">
              <button className="w-full rounded-md bg-foreground text-background py-2.5 text-sm font-medium inline-flex items-center justify-center gap-2">
                <UserPlus className="h-4 w-4" /> 팀에 초대
              </button>
              <button className="mt-2 w-full rounded-md border hairline-strong py-2.5 text-sm hover:bg-surface">팔로우</button>
              <div className="mt-5 grid grid-cols-3 gap-2 text-center">
                <Stat n={p.works} l="작업물" />
                <Stat n={p.followers} l="팔로워" />
                <Stat n={`${p.score}%`} l="평균 FIT" />
              </div>
            </div>

            <div className="rounded-lg border hairline bg-card p-5">
              <div className="font-mono-meta">협업 조건</div>
              <ul className="mt-3 space-y-2 text-sm">
                <li className="flex justify-between"><span className="text-muted-foreground">유형</span><span>유상 / 무상 협의</span></li>
                <li className="flex justify-between"><span className="text-muted-foreground">출장</span><span>국내 가능</span></li>
                <li className="flex justify-between"><span className="text-muted-foreground">장비</span><span>본인 보유</span></li>
                <li className="flex justify-between"><span className="text-muted-foreground">근무</span><span>주말 OK</span></li>
              </ul>
            </div>

            <div className="rounded-lg border hairline bg-card p-5">
              <div className="font-mono-meta inline-flex items-center gap-1.5 text-film"><Sparkles className="h-3 w-3"/> AI 추천 이유</div>
              <p className="mt-2 text-sm text-muted-foreground">{p.reason}</p>
            </div>

            <div className="rounded-lg border hairline bg-card p-5">
              <div className="font-mono-meta">태그</div>
              <div className="mt-2 flex flex-wrap gap-1.5">
                {p.tags.map((t: string) => (
                  <span key={t} className="rounded border hairline px-2 py-0.5 text-xs">{t}</span>
                ))}
              </div>
            </div>
          </aside>
        </header>
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
function Stat({ n, l }: { n: number | string; l: string }) {
  return (
    <div className="rounded-md border hairline py-2">
      <div className="font-display text-lg">{n}</div>
      <div className="font-mono-meta">{l}</div>
    </div>
  );
}
function SectionTitle({ eyebrow, title, actions }: { eyebrow: string; title: string; actions?: React.ReactNode }) {
  return (
    <div className="flex items-end justify-between">
      <div>
        <div className="font-mono-meta">{eyebrow}</div>
        <h2 className="mt-1 font-display text-2xl">{title}</h2>
      </div>
      {actions}
    </div>
  );
}
