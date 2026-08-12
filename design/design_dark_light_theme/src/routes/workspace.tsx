import { createFileRoute } from "@tanstack/react-router";
import { SiteHeader, SiteFooter } from "@/components/site-chrome";
import { CoverArt } from "@/components/cover-art";
import { teams, profiles, notifications } from "@/lib/mock";
import { Plus, CheckCircle2, Clock, XCircle, Send, Calendar, Users, FileText, MapPin } from "lucide-react";

export const Route = createFileRoute("/workspace")({
  head: () => ({
    meta: [
      { title: "팀 워크스페이스 — Slate" },
      { name: "description", content: "팀 리더를 위한 모집·지원자·일정·작업물 통합 보드" },
      { property: "og:title", content: "팀 워크스페이스 — Slate" },
    ],
  }),
  component: Workspace,
});

function Workspace() {
  const team = teams[0];
  return (
    <div className="min-h-screen">
      <SiteHeader />
      <main className="mx-auto max-w-[1320px] px-6 py-8">
        <header className="flex items-end justify-between flex-wrap gap-4">
          <div>
            <div className="font-mono-meta">WORKSPACE · {team.name}</div>
            <h1 className="mt-2 font-display text-4xl">{team.title}</h1>
            <div className="mt-2 text-sm text-muted-foreground">팀 리더 보기 · {team.period}</div>
          </div>
          <div className="flex items-center gap-2">
            <button className="rounded-md border hairline-strong px-3 py-2 text-sm hover:bg-surface">팀 정보 수정</button>
            <button className="rounded-md bg-foreground text-background px-3 py-2 text-sm inline-flex items-center gap-1.5">
              <Plus className="h-4 w-4" /> 모집 공고
            </button>
          </div>
        </header>

        <div className="mt-8 grid gap-4 md:grid-cols-4">
          <Kpi label="모집 중 slot" value="3" sub="마감 임박 1" />
          <Kpi label="검토 중 지원자" value="11" sub="신규 4" tone="film" />
          <Kpi label="수락한 팀원" value="7" sub="정원 12" />
          <Kpi label="이번 주 일정" value="5" sub="촬영 1, 회의 4" />
        </div>

        <div className="mt-8 grid gap-8 lg:grid-cols-12">
          <section className="lg:col-span-8 space-y-8">
            <div>
              <H2>모집 공고 · 구인 slot</H2>
              <div className="mt-4 rounded-lg border hairline bg-card overflow-hidden">
                {team.slots.map((s, i) => (
                  <div key={s.role} className={"p-4 grid grid-cols-12 items-center gap-4 " + (i ? "border-t hairline" : "")}>
                    <div className="col-span-3">
                      <div className="font-display text-lg">{s.role}</div>
                      <div className="font-mono-meta">SLOT-{String(i + 1).padStart(2, "0")}</div>
                    </div>
                    <div className="col-span-3 text-sm">
                      <div>필요 {s.need}명 · 남음 {s.need}</div>
                      <div className="font-mono-meta">마감 {s.deadline}</div>
                    </div>
                    <div className="col-span-4 text-sm text-muted-foreground">{s.note}</div>
                    <div className="col-span-2 flex justify-end gap-2">
                      <button className="rounded-md border hairline px-2.5 py-1.5 text-xs hover:bg-surface">수정</button>
                      <button className="rounded-md border hairline px-2.5 py-1.5 text-xs hover:bg-surface">마감</button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div>
              <div className="flex items-end justify-between">
                <H2>지원자 / 초대 큐</H2>
                <div className="inline-flex rounded-md border hairline p-1 bg-surface text-xs">
                  {["전체", "검토", "수락", "거절", "초대"].map((t, i) => (
                    <button key={t} className={"px-3 py-1 rounded " + (i === 0 ? "bg-foreground text-background" : "text-muted-foreground")}>
                      {t}
                    </button>
                  ))}
                </div>
              </div>
              <div className="mt-4 rounded-lg border hairline bg-card overflow-hidden divide-y hairline">
                {profiles.slice(0, 5).map((p, i) => {
                  const status = ["검토 중", "수락", "검토 중", "초대 발송", "거절"][i];
                  const tone = status === "수락" ? "success" : status === "거절" ? "muted-foreground" : status === "초대 발송" ? "film" : "warning";
                  return (
                    <div key={p.id} className="p-4 grid grid-cols-12 items-center gap-4">
                      <div className="col-span-4 flex items-center gap-3">
                        <div className="h-9 w-9 rounded-full bg-gradient-to-br from-film to-chart-2 ring-1 ring-border-strong" />
                        <div className="min-w-0">
                          <div className="text-sm truncate">{p.name}</div>
                          <div className="font-mono-meta">{p.role} · {p.region}</div>
                        </div>
                      </div>
                      <div className="col-span-2 text-sm">
                        <div>경력 {p.years}년</div>
                        <div className="font-mono-meta">작업물 {p.works}</div>
                      </div>
                      <div className="col-span-3 text-sm">
                        <div className="flex items-center gap-2">
                          <div className="h-1 flex-1 rounded-full bg-border overflow-hidden">
                            <div className="h-full bg-film" style={{ width: p.score + "%" }} />
                          </div>
                          <span className="text-xs">{p.score}%</span>
                        </div>
                        <div className="font-mono-meta mt-1">FIT</div>
                      </div>
                      <div className="col-span-1">
                        <span className={`text-xs text-${tone}`}>{status}</span>
                      </div>
                      <div className="col-span-2 flex justify-end gap-1.5">
                        <button className="rounded-md border hairline px-2 py-1 text-xs hover:bg-surface">프로필</button>
                        <button className="rounded-md bg-foreground text-background px-2 py-1 text-xs">수락</button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            <div>
              <H2>팀 계획 · 칸반</H2>
              <div className="mt-4 grid grid-cols-3 gap-4">
                {[
                  { t: "기획", items: ["트리트먼트 v3", "캐스팅 콜 작성"] },
                  { t: "진행", items: ["로케이션 헌팅 (을지로)", "장비 견적 비교"] },
                  { t: "완료", items: ["스토리보드 v2", "예산안 1차"] },
                ].map((c) => (
                  <div key={c.t} className="rounded-lg border hairline bg-card p-4">
                    <div className="font-mono-meta mb-3 flex justify-between">{c.t}<span>{c.items.length}</span></div>
                    <ul className="space-y-2">
                      {c.items.map((i) => (
                        <li key={i} className="rounded-md border hairline bg-surface p-2.5 text-sm">{i}</li>
                      ))}
                      <li className="rounded-md border border-dashed hairline p-2.5 text-xs text-muted-foreground inline-flex items-center gap-1">
                        <Plus className="h-3 w-3" /> 추가
                      </li>
                    </ul>
                  </div>
                ))}
              </div>
            </div>

            <div>
              <H2>팀 작업물 승인</H2>
              <div className="mt-4 grid sm:grid-cols-3 gap-4">
                <ApprovalCard label="스토리보드 v2" status="승인됨" tone="success" icon={<CheckCircle2 className="h-3.5 w-3.5" />} />
                <ApprovalCard label="로케이션 무드 보드" status="검토 중" tone="warning" icon={<Clock className="h-3.5 w-3.5" />} />
                <ApprovalCard label="캐스팅 노트 v1" status="반려 (재요청)" tone="destructive" icon={<XCircle className="h-3.5 w-3.5" />} />
              </div>
            </div>
          </section>

          <aside className="lg:col-span-4 space-y-5">
            <div className="rounded-lg border hairline bg-card p-5">
              <div className="font-mono-meta">팀 정보</div>
              <ul className="mt-3 space-y-2 text-sm">
                <Row icon={<Users className="h-3.5 w-3.5" />} k="정원" v={`${team.members}/${team.capacity}`} />
                <Row icon={<Calendar className="h-3.5 w-3.5" />} k="기간" v={team.period} />
                <Row icon={<MapPin className="h-3.5 w-3.5" />} k="지역" v={team.region} />
                <Row icon={<FileText className="h-3.5 w-3.5" />} k="단계" v={team.stage} />
              </ul>
              <div className="mt-4 flex gap-2">
                <button className="flex-1 rounded-md border hairline-strong py-2 text-xs">리더 위임</button>
                <button className="flex-1 rounded-md border hairline-strong py-2 text-xs">팀 종료</button>
              </div>
            </div>

            <div className="rounded-lg border hairline bg-card p-5">
              <div className="font-mono-meta">알림</div>
              <ul className="mt-3 space-y-3">
                {notifications.map((n) => (
                  <li key={n.id} className="text-sm">
                    <div className="flex items-center gap-2">
                      <span className="font-mono-meta text-film">{n.kind}</span>
                      <span className="font-mono-meta ml-auto">{n.time}</span>
                    </div>
                    <div className="mt-1">{n.text}</div>
                  </li>
                ))}
              </ul>
            </div>

            <div className="rounded-lg border hairline bg-card p-5">
              <div className="font-mono-meta">팀 후보 로케이션</div>
              <ul className="mt-3 space-y-2 text-sm">
                <li className="flex justify-between"><span>을지로 4가 인쇄골목</span><span className="font-mono-meta text-film">팀</span></li>
                <li className="flex justify-between"><span>양양 죽도 해변</span><span className="font-mono-meta text-film">팀</span></li>
                <li className="flex justify-between"><span>수원 화서문</span><span className="font-mono-meta">개인</span></li>
              </ul>
            </div>
          </aside>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}

function Kpi({ label, value, sub, tone }: { label: string; value: string; sub: string; tone?: "film" }) {
  return (
    <div className="rounded-lg border hairline bg-card p-5">
      <div className="font-mono-meta">{label}</div>
      <div className={"mt-1 font-display text-4xl " + (tone === "film" ? "text-film" : "")}>{value}</div>
      <div className="text-xs text-muted-foreground mt-1">{sub}</div>
    </div>
  );
}
function H2({ children }: { children: React.ReactNode }) {
  return <h2 className="font-display text-2xl">{children}</h2>;
}
function ApprovalCard({ label, status, tone, icon }: { label: string; status: string; tone: string; icon: React.ReactNode }) {
  return (
    <div className="rounded-lg border hairline bg-card overflow-hidden">
      <CoverArt variant="generic" ratio="16/9" meta="DRAFT" />
      <div className="p-3">
        <div className="text-sm">{label}</div>
        <div className={`mt-1 text-xs inline-flex items-center gap-1 text-${tone}`}>{icon}{status}</div>
      </div>
    </div>
  );
}
function Row({ icon, k, v }: { icon: React.ReactNode; k: string; v: string }) {
  return (
    <li className="flex items-center gap-2">
      <span className="text-muted-foreground">{icon}</span>
      <span className="text-muted-foreground">{k}</span>
      <span className="ml-auto">{v}</span>
    </li>
  );
}
function Send2() { return <Send />; }
