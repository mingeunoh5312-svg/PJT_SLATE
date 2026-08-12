import { Link, useRouterState } from "@tanstack/react-router";
import { Bell, Search, Sparkles } from "lucide-react";
import { ThemeToggle } from "@/components/theme-toggle";

const nav = [
  { to: "/", label: "홈" },
  { to: "/discover", label: "탐색" },
  { to: "/works", label: "작업물" },
  { to: "/contests", label: "공모전" },
  { to: "/ai/location", label: "AI 로케이션" },
  { to: "/workspace", label: "내 팀" },
  { to: "/admin", label: "운영" },
] as const;

export function SiteHeader() {
  const pathname = useRouterState({ select: (s) => s.location.pathname });

  return (
    <header className="sticky top-0 z-40 border-b hairline bg-background/80 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="mx-auto flex h-14 max-w-[1320px] items-center gap-6 px-6">
        <Link to="/" className="flex items-center gap-2">
          <SlateMark />
          <span className="font-display text-xl">Slate</span>
          <span className="font-mono-meta hidden sm:inline">v0 · design draft</span>
        </Link>

        <nav className="hidden md:flex items-center gap-1 text-sm">
          {nav.map((item) => {
            const active =
              item.to === "/"
                ? pathname === "/"
                : pathname.startsWith(item.to);
            return (
              <Link
                key={item.to}
                to={item.to}
                className={
                  "px-3 py-1.5 rounded-md transition-colors " +
                  (active
                    ? "text-foreground bg-surface"
                    : "text-muted-foreground hover:text-foreground")
                }
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="ml-auto flex items-center gap-2">
          <button className="hidden md:flex items-center gap-2 rounded-md border hairline bg-surface px-3 py-1.5 text-xs text-muted-foreground hover:text-foreground">
            <Search className="h-3.5 w-3.5" />
            <span>팀, 제작자, 작업물 검색</span>
            <span className="font-mono-meta ml-6">⌘K</span>
          </button>
          <button className="rounded-md border hairline bg-surface p-2 text-muted-foreground hover:text-foreground">
            <Bell className="h-4 w-4" />
          </button>
          <ThemeToggle />
          <Link
            to="/workspace"
            className="hidden sm:flex items-center gap-1.5 rounded-md bg-film px-3 py-1.5 text-xs font-medium text-film-foreground hover:opacity-90"
          >
            <Sparkles className="h-3.5 w-3.5" />
            팀 만들기
          </Link>
          <div className="h-8 w-8 rounded-full bg-gradient-to-br from-film to-chart-2 ring-1 ring-border-strong" />
        </div>
      </div>
    </header>
  );
}

function SlateMark() {
  return (
    <span className="relative inline-flex h-7 w-7 items-center justify-center rounded-md bg-foreground text-background">
      <span className="font-display text-base leading-none">S</span>
      <span className="absolute -bottom-0.5 left-0.5 right-0.5 h-[3px] bg-film rounded-sm" />
    </span>
  );
}

export function SiteFooter() {
  return (
    <footer className="mt-24 border-t hairline">
      <div className="mx-auto max-w-[1320px] px-6 py-10 grid gap-8 md:grid-cols-4 text-sm">
        <div>
          <div className="flex items-center gap-2">
            <span className="font-display text-2xl">Slate</span>
          </div>
          <p className="mt-2 text-muted-foreground max-w-xs">
            영화·영상 제작자, 팀, 기업, 운영자를 잇는 제작 협업 플랫폼.
          </p>
          <p className="font-mono-meta mt-4">© 2026 Slate Studio</p>
        </div>
        <FooterCol title="제작자" items={["프로필", "포트폴리오", "팀 탐색", "내 지원/초대"]} />
        <FooterCol title="팀 · 기업" items={["팀 만들기", "모집 공고", "기업 가입", "공모전 개설"]} />
        <FooterCol title="플랫폼" items={["공모전", "AI 로케이션", "작업물 게시판", "운영 정책"]} />
      </div>
    </footer>
  );
}

function FooterCol({ title, items }: { title: string; items: string[] }) {
  return (
    <div>
      <div className="font-mono-meta mb-3">{title}</div>
      <ul className="space-y-2 text-muted-foreground">
        {items.map((i) => (
          <li key={i} className="hover:text-foreground cursor-pointer">
            {i}
          </li>
        ))}
      </ul>
    </div>
  );
}
