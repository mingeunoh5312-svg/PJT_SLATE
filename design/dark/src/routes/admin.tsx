import { createFileRoute } from "@tanstack/react-router";
import { SiteHeader, SiteFooter } from "@/components/site-chrome";
import { profiles, teams, contests } from "@/lib/mock";
import {
  AlertTriangle,
  Users,
  Building2,
  Flag,
  Trophy,
  ShieldCheck,
  Activity,
  Settings,
  CheckCircle2,
  XCircle,
  Eye,
  RotateCcw,
} from "lucide-react";

export const Route = createFileRoute("/admin")({
  head: () => ({
    meta: [
      { title: "운영 콘솔 — Slate" },
      { name: "description", content: "회원·팀·신고·기업 승인·공모전·권한·매칭 정책을 한 곳에서 운영합니다." },
      { property: "og:title", content: "Slate 운영 콘솔" },
    ],
  }),
  component: Admin,
});

function Admin() {
  return (
    <div className="min-h-screen">
      <SiteHeader />
      <main className="mx-auto max-w-[1480px] px-6 py-8">
        <header className="flex items-end justify-between flex-wrap gap-4">
          <div>
            <div className="font-mono-meta">OPERATIONS · 관리자</div>
            <h1 className="mt-2 font-display text-4xl">운영 콘솔</h1>
          </div>
          <div className="text-xs text-muted-foreground">로그인: <span className="text-foreground">admin@slate.kr</span> · 권한 7 / 9</div>
        </header>

        <div className="mt-8 grid gap-4 md:grid-cols-5">
          <Kpi icon={<AlertTriangle className="h-4 w-4 text-film"/>} label="우선 처리" v="14" sub="신고 6, 승인 4, 기타 4" tone="film" />
          <Kpi icon={<Users className="h-4 w-4"/>} label="회원" v="12,840" sub="신규 +128 / 7일" />
          <Kpi icon={<Building2 className="h-4 w-4"/>} label="기업 승인 대기" v="9" sub="평균 1.2일" />
          <Kpi icon={<Trophy className="h-4 w-4"/>} label="공모전" v="64" sub="접수중 41" />
          <Kpi icon={<Flag className="h-4 w-4"/>} label="신고 미처리" v="6" sub="콘텐츠 4, 리뷰 2" />
        </div>

        <div className="mt-8 grid gap-8 xl:grid-cols-12">
          <aside className="xl:col-span-2">
            <nav className="rounded-lg border hairline bg-card p-2 text-sm sticky top-20">
              {[
                { i: <Activity className="h-4 w-4"/>, t: "대시보드", on: true },
                { i: <Users className="h-4 w-4"/>, t: "회원" },
                { i: <Users className="h-4 w-4"/>, t: "팀" },
                { i: <Flag className="h-4 w-4"/>, t: "신고" },
                { i: <Building2 className="h-4 w-4"/>, t: "기업 승인" },
                { i: <Trophy className="h-4 w-4"/>, t: "공모전" },
                { i: <ShieldCheck className="h-4 w-4"/>, t: "권한·로그" },
                { i: <Settings className="h-4 w-4"/>, t: "매칭 정책" },
              ].map((n) => (
                <button key={n.t} className={"w-full text-left px-3 py-2 rounded-md flex items-center gap-2 " + (n.on ? "bg-surface" : "text-muted-foreground hover:text-foreground")}>
                  {n.i} {n.t}
                </button>
              ))}
            </nav>
          </aside>

          <section className="xl:col-span-7 space-y-8">
            <Panel title="우선 처리 업무" sub="오늘 처리해야 하는 항목">
              <ul className="divide-y hairline">
                {[
                  { kind: "기업 승인", t: "(주)스튜디오라이트 — 사업자등록증 제출", time: "12분 전" },
                  { kind: "신고", t: "‘리허설’ 게시글 — 저작권 의심 (3건 누적)", time: "1시간 전" },
                  { kind: "공모전 요청", t: "오로라 픽처스 — 단편 공모전 개설 요청", time: "2시간 전" },
                  { kind: "회원 제재", t: "@anon42 — 반복 신고 누적", time: "어제" },
                ].map((r) => (
                  <li key={r.t} className="py-3 flex items-center gap-3">
                    <span className="font-mono-meta text-film w-24">{r.kind}</span>
                    <span className="flex-1 text-sm truncate">{r.t}</span>
                    <span className="font-mono-meta">{r.time}</span>
                    <button className="rounded-md border hairline px-2.5 py-1 text-xs hover:bg-surface inline-flex items-center gap-1"><Eye className="h-3 w-3"/> 보기</button>
                  </li>
                ))}
              </ul>
            </Panel>

            <Panel title="회원 관리" sub="조회 · 비활성화 · 복구 · 제재">
              <table className="w-full text-sm">
                <thead className="text-left">
                  <tr className="[&>th]:py-2 [&>th]:px-3 font-mono-meta text-muted-foreground border-b hairline">
                    <th>핸들</th><th>이름</th><th>역할</th><th>상태</th><th>신고</th><th></th>
                  </tr>
                </thead>
                <tbody className="[&>tr]:border-b [&>tr]:hairline [&>tr>td]:py-2.5 [&>tr>td]:px-3">
                  {profiles.map((p, i) => {
                    const status = ["활성", "활성", "비활성", "활성", "제재 (3일)", "활성"][i];
                    return (
                      <tr key={p.id}>
                        <td className="font-mono-meta">{p.handle}</td>
                        <td>{p.name}</td>
                        <td className="text-muted-foreground">{p.role}</td>
                        <td><Badge status={status} /></td>
                        <td>{i === 4 ? 3 : 0}</td>
                        <td className="text-right">
                          <button className="text-xs text-muted-foreground hover:text-foreground">자세히</button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </Panel>

            <Panel title="기업 공모전 개설 요청">
              <div className="divide-y hairline">
                {[
                  { c: "오로라 픽처스", t: "단편 영화 공모전 2026", s: "검토 중" },
                  { c: "라이트하우스 미디어", t: "브랜드 영상 챌린지", s: "서류 보완 요청" },
                  { c: "북악 스튜디오", t: "음악 다큐 공모", s: "승인됨" },
                ].map((r) => (
                  <div key={r.t} className="py-3 flex items-center gap-3">
                    <Building2 className="h-4 w-4 text-muted-foreground" />
                    <div className="flex-1 min-w-0">
                      <div className="text-sm truncate">{r.t}</div>
                      <div className="font-mono-meta">{r.c}</div>
                    </div>
                    <Badge status={r.s} />
                    <button className="rounded-md bg-foreground text-background px-2.5 py-1 text-xs inline-flex items-center gap-1"><CheckCircle2 className="h-3 w-3"/> 승인</button>
                    <button className="rounded-md border hairline px-2.5 py-1 text-xs inline-flex items-center gap-1"><XCircle className="h-3 w-3"/> 거절</button>
                  </div>
                ))}
              </div>
            </Panel>

            <Panel title="매칭 점수 정책" sub="가중치 미리보기 · 배포 · 롤백">
              <div className="grid sm:grid-cols-2 gap-5">
                <div>
                  <div className="font-mono-meta mb-3">현재 가중치</div>
                  <ul className="space-y-3 text-sm">
                    {[
                      { k: "역할 일치", v: 30 },
                      { k: "장르 경험", v: 22 },
                      { k: "지역", v: 14 },
                      { k: "일정", v: 18 },
                      { k: "포트폴리오 점수", v: 16 },
                    ].map((w) => (
                      <li key={w.k}>
                        <div className="flex justify-between"><span>{w.k}</span><span className="font-mono-meta">{w.v}</span></div>
                        <div className="mt-1 h-1 rounded-full bg-border overflow-hidden"><div className="h-1 bg-film" style={{width: w.v*2 + "%"}}/></div>
                      </li>
                    ))}
                  </ul>
                </div>
                <div className="rounded-md border hairline bg-surface p-4 text-sm">
                  <div className="font-mono-meta mb-2">미리보기 결과 — 〈푸른 방〉 촬영감독 slot</div>
                  <ol className="space-y-2">
                    {profiles.slice(0, 4).map((p, i) => (
                      <li key={p.id} className="flex justify-between">
                        <span>{i + 1}. {p.name}</span>
                        <span className="font-mono-meta">{p.score - i * 2}%</span>
                      </li>
                    ))}
                  </ol>
                  <div className="mt-4 flex gap-2">
                    <button className="flex-1 rounded-md bg-foreground text-background py-2 text-xs">배포</button>
                    <button className="flex-1 rounded-md border hairline-strong py-2 text-xs inline-flex items-center justify-center gap-1"><RotateCcw className="h-3 w-3"/> 롤백</button>
                  </div>
                </div>
              </div>
            </Panel>
          </section>

          <aside className="xl:col-span-3 space-y-6">
            <Panel title="감사 로그" pad>
              <ul className="text-sm divide-y hairline">
                {[
                  { who: "admin@slate.kr", a: "기업 승인", t: "방금" },
                  { who: "ops@slate.kr", a: "공모전 일괄 삭제 · 3건", t: "1시간 전" },
                  { who: "admin@slate.kr", a: "회원 제재 (3일) · @anon42", t: "어제" },
                  { who: "ops@slate.kr", a: "매칭 정책 v18 배포", t: "어제" },
                ].map((l, i) => (
                  <li key={i} className="py-2">
                    <div className="font-mono-meta">{l.t}</div>
                    <div>{l.a}</div>
                    <div className="text-xs text-muted-foreground">{l.who}</div>
                  </li>
                ))}
              </ul>
            </Panel>

            <Panel title="활성 공모전" pad>
              <ul className="text-sm space-y-3">
                {contests.slice(0, 3).map((c) => (
                  <li key={c.id} className="flex items-center gap-2">
                    <span className="font-mono-meta w-10">{c.dleft < 0 ? "—" : "D-" + c.dleft}</span>
                    <span className="flex-1 truncate">{c.title}</span>
                    <Badge status={c.status} />
                  </li>
                ))}
              </ul>
            </Panel>

            <Panel title="현재 활성 팀" pad>
              <ul className="text-sm space-y-3">
                {teams.map((t) => (
                  <li key={t.id} className="flex items-center gap-2">
                    <span className="flex-1 truncate">{t.title}</span>
                    <span className="font-mono-meta">{t.stage}</span>
                  </li>
                ))}
              </ul>
            </Panel>
          </aside>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

function Kpi({ icon, label, v, sub, tone }: { icon: React.ReactNode; label: string; v: string; sub: string; tone?: "film" }) {
  return (
    <div className="rounded-lg border hairline bg-card p-5">
      <div className="font-mono-meta inline-flex items-center gap-1.5">{icon}{label}</div>
      <div className={"mt-1 font-display text-3xl " + (tone === "film" ? "text-film" : "")}>{v}</div>
      <div className="text-xs text-muted-foreground mt-1">{sub}</div>
    </div>
  );
}

function Panel({ title, sub, children, pad }: { title: string; sub?: string; children: React.ReactNode; pad?: boolean }) {
  return (
    <div className="rounded-lg border hairline bg-card">
      <div className="px-5 py-4 border-b hairline">
        <div className="font-display text-lg">{title}</div>
        {sub && <div className="font-mono-meta mt-0.5">{sub}</div>}
      </div>
      <div className={pad ? "p-5" : "p-5"}>{children}</div>
    </div>
  );
}

function Badge({ status }: { status: string }) {
  const map: Record<string, string> = {
    "활성": "text-success",
    "비활성": "text-muted-foreground",
    "승인됨": "text-success",
    "검토 중": "text-warning",
    "서류 보완 요청": "text-warning",
    "접수 중": "text-success",
    "마감 임박": "text-film",
    "종료": "text-muted-foreground",
  };
  const tone = map[status] ?? (status.startsWith("제재") ? "text-destructive" : "text-muted-foreground");
  return <span className={"font-mono-meta " + tone}>{status}</span>;
}
