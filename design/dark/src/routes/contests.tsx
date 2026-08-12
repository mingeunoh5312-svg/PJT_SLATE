import { createFileRoute } from "@tanstack/react-router";
import { SiteHeader, SiteFooter } from "@/components/site-chrome";
import { contests } from "@/lib/mock";
import { Bookmark, Sparkles, ExternalLink, CheckCircle2, Plus, Search } from "lucide-react";

export const Route = createFileRoute("/contests")({
  head: () => ({
    meta: [
      { title: "공모전 — Slate" },
      { name: "description", content: "국내외 영상 공모전 탐색, 저장, 적합도 분석, 제출 준비까지." },
      { property: "og:title", content: "Slate 공모전 — 탐색부터 제출 준비까지" },
    ],
  }),
  component: Contests,
});

function Contests() {
  return (
    <div className="min-h-screen">
      <SiteHeader />
      <main className="mx-auto max-w-[1320px] px-6 py-8">
        <header className="flex items-end justify-between flex-wrap gap-4">
          <div>
            <div className="font-mono-meta">CONTESTS</div>
            <h1 className="mt-2 font-display text-4xl">공모전</h1>
            <p className="mt-2 text-sm text-muted-foreground max-w-xl">탐색, 저장, 프로필·팀 기준 적합도 분석, 제출 체크리스트까지 한 곳에서 관리합니다.</p>
          </div>
          <div className="flex items-center gap-2">
            <button className="rounded-md border hairline-strong px-3 py-2 text-sm inline-flex items-center gap-1.5"><Plus className="h-4 w-4" /> 기업 공모전 개설 요청</button>
          </div>
        </header>

        <div className="mt-6 flex flex-wrap gap-2 items-center">
          <div className="relative flex-1 min-w-[260px] max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <input placeholder="공모전, 주최, 키워드" className="w-full rounded-md border hairline bg-surface pl-9 pr-3 py-2 text-sm" />
          </div>
          {["전체", "단편", "장편", "다큐", "광고", "뮤직비디오", "지역"].map((t, i) => (
            <span key={t} className={"rounded-full border px-3 py-1 text-xs " + (i === 0 ? "bg-foreground text-background border-foreground" : "hairline text-muted-foreground")}>{t}</span>
          ))}
        </div>

        <div className="mt-8 grid gap-8 lg:grid-cols-12">
          <section className="lg:col-span-8 space-y-6">
            <DeadlineStrip />
            <div className="space-y-4">
              {contests.map((c) => (
                <article key={c.id} className="rounded-lg border hairline bg-card p-5 flex gap-5">
                  <div className="w-20 shrink-0 text-center">
                    <div className={"font-display text-4xl " + (c.dleft < 0 ? "text-muted-foreground" : c.dleft <= 10 ? "text-film" : "")}>
                      {c.dleft < 0 ? "—" : "D-" + c.dleft}
                    </div>
                    <div className="font-mono-meta mt-1">{c.status}</div>
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h3 className="font-display text-2xl">{c.title}</h3>
                      {c.saved && <span className="font-mono-meta text-film">SAVED</span>}
                    </div>
                    <div className="font-mono-meta mt-1">{c.host} · {c.region} · 마감 {c.deadline}</div>
                    <div className="mt-3 flex flex-wrap gap-1.5">
                      {c.tags.map((t) => (
                        <span key={t} className="rounded border hairline px-2 py-0.5 text-xs">{t}</span>
                      ))}
                      <span className="rounded bg-surface px-2 py-0.5 text-xs">{c.prize}</span>
                    </div>
                    <div className="mt-4 flex items-center gap-3">
                      <Sparkles className="h-3.5 w-3.5 text-film" />
                      <div className="h-1.5 flex-1 rounded-full bg-border overflow-hidden">
                        <div className="h-full bg-film" style={{ width: c.fit + "%" }} />
                      </div>
                      <div className="text-sm">{c.fit}% <span className="text-muted-foreground text-xs">FIT</span></div>
                    </div>
                  </div>
                  <div className="flex flex-col gap-2 w-32 shrink-0">
                    <button className="rounded-md border hairline-strong py-1.5 text-xs inline-flex items-center justify-center gap-1.5">
                      <Bookmark className="h-3.5 w-3.5" /> {c.saved ? "저장됨" : "저장"}
                    </button>
                    <button className="rounded-md bg-foreground text-background py-1.5 text-xs">제출 준비</button>
                    <button className="rounded-md py-1.5 text-xs text-muted-foreground inline-flex items-center justify-center gap-1">
                      원문 <ExternalLink className="h-3 w-3" />
                    </button>
                  </div>
                </article>
              ))}
            </div>
          </section>

          <aside className="lg:col-span-4 space-y-6">
            <SubmitPrep />
            <CompanyFlow />
          </aside>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

function DeadlineStrip() {
  return (
    <div className="rounded-lg border hairline bg-card p-5">
      <div className="font-mono-meta">UPCOMING DEADLINES</div>
      <div className="mt-3 grid grid-cols-7 gap-2">
        {["D-2", "D-9", "D-14", "D-24", "D-30", "D-50", "D-60"].map((d, i) => (
          <div key={d} className={"rounded-md border hairline p-3 text-center " + (i <= 1 ? "border-strong" : "")}>
            <div className={"font-display text-xl " + (i <= 1 ? "text-film" : "")}>{d}</div>
            <div className="font-mono-meta mt-1">공모 {i + 1}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

function SubmitPrep() {
  return (
    <div className="rounded-lg border hairline bg-card p-5">
      <div className="font-mono-meta">제출 준비 체크리스트</div>
      <h3 className="mt-1 font-display text-xl">서울독립영화제 단편</h3>
      <ul className="mt-4 space-y-2.5 text-sm">
        {[
          { t: "출품 신청서 작성", done: true },
          { t: "상영 본 (DCP) 준비", done: true },
          { t: "스틸 사진 5매", done: false },
          { t: "감독 소개 / 연출 의도", done: false },
          { t: "크레딧 리스트 점검", done: false },
        ].map((i) => (
          <li key={i.t} className="flex items-center gap-2">
            <CheckCircle2 className={"h-4 w-4 " + (i.done ? "text-success" : "text-muted-foreground")} />
            <span className={i.done ? "line-through text-muted-foreground" : ""}>{i.t}</span>
          </li>
        ))}
      </ul>
      <textarea
        placeholder="메모 — 출품 관련 내부 노트"
        className="mt-4 w-full rounded-md border hairline bg-surface p-3 text-sm h-20 resize-none"
        defaultValue="DCP 인코딩은 7/30까지. 사운드 마스터 -23 LUFS 확인."
      />
    </div>
  );
}

function CompanyFlow() {
  return (
    <div className="rounded-lg border hairline bg-card p-5">
      <div className="font-mono-meta">기업 공모전 개설 흐름</div>
      <ol className="mt-3 space-y-3 text-sm">
        {[
          { t: "기업 계정 가입", s: "완료" },
          { t: "사업자 증빙 서류 업로드", s: "완료" },
          { t: "관리자 승인", s: "승인됨" },
          { t: "공모전 개설 요청 작성", s: "작성 중" },
          { t: "관리자 검토", s: "대기" },
        ].map((i, idx) => (
          <li key={i.t} className="flex items-center gap-3">
            <span className="font-display text-xl w-6 text-muted-foreground">{idx + 1}</span>
            <span className="flex-1">{i.t}</span>
            <span className={
              "font-mono-meta " +
              (i.s === "승인됨" || i.s === "완료" ? "text-success" : i.s === "작성 중" ? "text-film" : "")
            }>{i.s}</span>
          </li>
        ))}
      </ol>
    </div>
  );
}
