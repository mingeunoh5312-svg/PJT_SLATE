<script setup>
const stats = [
  { value: '2,148', label: '활성 제작자' },
  { value: '312', label: '모집 중 팀' },
  { value: '64', label: '진행 중 공모전' }
]

const personas = [
  {
    tag: 'FOR CREATORS',
    title: '제작자',
    copy: '역할·장르·지역·일정을 정리하고, 포트폴리오로 신뢰를 쌓아 팀에 합류합니다.',
    cta: '프로필 만들기'
  },
  {
    tag: 'FOR TEAMS',
    title: '팀 리더',
    copy: '모집 공고와 구인 slot을 관리하고, 지원·초대·일정·작업물을 한 보드에서 관리합니다.',
    cta: '팀 워크스페이스'
  },
  {
    tag: 'FOR COMPANIES',
    title: '기업',
    copy: '승인 후 공모전을 개설하고 요청 상태와 결과를 추적합니다.',
    cta: '공모전 개설'
  },
  {
    tag: 'FOR OPERATORS',
    title: '운영자',
    copy: '회원·팀·신고·공모전·권한·로그·매칭 정책을 한 대시보드에서 운영합니다.',
    cta: '운영 콘솔'
  }
]

const features = [
  {
    icon: 'users',
    title: '양방향 매칭',
    body: '팀이 사람을 찾고, 제작자가 팀을 찾는 두 방향을 동시에 지원합니다. 역할·경력·지역·일정 기반.',
    ai: false
  },
  {
    icon: 'clapper',
    title: '팀 워크스페이스',
    body: '모집 공고, 구인 slot, 지원/초대, 일정, 작업물 승인을 하나의 보드에서.',
    ai: false
  },
  {
    icon: 'film',
    title: '포트폴리오',
    body: '직접 업로드, YouTube 링크, KOBIS·공공데이터 기반 작품 이력을 한 프로필에.',
    ai: false
  },
  {
    icon: 'pin',
    title: 'AI 로케이션 탐색',
    body: '장면 설명과 지역 조건으로 촬영지 후보를 추천. 개인·팀 후보로 저장.',
    ai: true
  },
  {
    icon: 'trophy',
    title: '공모전',
    body: '국내외 공모전 탐색, 저장, 프로필·팀 기준 적합도 분석, 제출 준비 체크리스트.',
    ai: false
  },
  {
    icon: 'check',
    title: '운영 콘솔',
    body: '회원·팀·신고·기업 승인·공모전·권한·매칭 정책을 운영자가 직접 관리.',
    ai: false
  }
]

const teams = [
  {
    id: 1,
    title: '단편영화 〈푸른 방〉 제작팀',
    cover: 'blueroom',
    stage: '프리프로덕션',
    genre: '드라마',
    region: '서울',
    period: '2026.07 — 2026.10',
    open: 3,
    summary: '9분 단편. 한 여성이 텅 빈 아파트에서 마주하는 기억에 대한 이야기. 16mm 필름룩 추구.'
  },
  {
    id: 2,
    title: '뮤직비디오 〈Owl〉',
    cover: 'nightowl',
    stage: '촬영 준비',
    genre: '뮤직비디오',
    region: '서울 · 경기',
    period: '2026.07.18 — 07.22',
    open: 2,
    summary: '단일 곡 뮤직비디오. 도심 야경과 옥상 시퀀스를 중심으로 촬영합니다.'
  },
  {
    id: 3,
    title: '다큐 〈오프시즌〉',
    cover: 'doc',
    stage: '촬영 중',
    genre: '다큐',
    region: '강원 · 경기',
    period: '2026.05 — 2026.12',
    open: 1,
    summary: '비시즌 스키장 노동자들의 삶을 따라가는 장편 다큐 프로젝트.'
  },
  {
    id: 4,
    title: '브랜드 광고 〈Re:Light〉',
    cover: 'ad',
    stage: '기획',
    genre: '광고',
    region: '서울',
    period: '2026.08.01 — 08.10',
    open: 4,
    summary: '조명 브랜드 30초 광고 3종. 스튜디오와 외부 로케이션을 함께 사용합니다.'
  }
]

const profiles = [
  { name: '윤하정', role: '촬영감독', region: '서울', years: 6, tag: '야간', score: 92 },
  { name: '김도윤', role: '동시녹음', region: '경기', years: 4, tag: '다큐', score: 87 },
  { name: '이서원', role: '편집', region: '서울', years: 8, tag: '뮤직비디오', score: 90 },
  { name: '박지운', role: '프로듀서', region: '서울', years: 10, tag: '공모', score: 84 },
  { name: '한미르', role: '색보정', region: '서울', years: 5, tag: '필름룩', score: 88 }
]

const aiLocations = [
  { name: '을지로 4가 인쇄골목', reason: '낡은 간판·네온', region: '서울 중구' },
  { name: '창신동 봉제거리', reason: '좁은 골목·야경', region: '서울 종로' },
  { name: '문래동 철공소 일대', reason: '기름때·산업톤', region: '서울 영등포' }
]

const contests = [
  { title: '서울독립영화제 단편 경쟁', host: '서울독립영화제', region: '서울', prize: '상금 1,000만', dleft: 24, status: 'OPEN', fit: 91, saved: true },
  { title: '부산영상위원회 제작지원', host: '부산영상위원회', region: '부산', prize: '제작지원', dleft: 9, status: 'OPEN', fit: 86, saved: false },
  { title: '청년 다큐멘터리 피칭', host: 'DMZ Docs', region: '경기', prize: '멘토링', dleft: 15, status: 'OPEN', fit: 79, saved: true },
  { title: '브랜드 숏폼 필름 어워드', host: 'Slate Partners', region: '온라인', prize: '상금 500만', dleft: 32, status: 'OPEN', fit: 74, saved: false }
]

const works = [
  { title: '산책', by: '이서원', role: '감독·편집', kind: '단편', year: 2025 },
  { title: 'Owl (Official MV)', by: '윤하정', role: '촬영감독', kind: '뮤직비디오', year: 2025 },
  { title: '오프시즌 — 티저', by: '김도윤', role: '동시녹음', kind: '다큐 티저', year: 2026 }
]
</script>

<template>
  <section class="lovable-home" aria-label="홈">
    <section class="lovable-hero">
      <div class="film-gradient lovable-hero-bg" />
      <div class="lovable-home-container lovable-hero-grid">
        <div class="lovable-hero-copy">
          <div class="lovable-kicker">
            <span />
            EST. 2026 · CREW + PROJECT NETWORK
          </div>
          <h1>
            다음 프로젝트는,
            <br>
            <em>맞는 사람과</em> 시작합니다.
          </h1>
          <p>
            Slate는 영화·영상 제작자, 팀, 기업, 운영자를 잇는 협업 플랫폼입니다.
            역할과 포트폴리오, 일정과 지역을 기반으로 팀과 사람을 매칭하고,
            작업물·공모전·로케이션 탐색까지 한 곳에서 정리합니다.
          </p>
          <div class="lovable-hero-actions">
            <RouterLink class="lovable-solid-link" :to="{ name: 'discover' }">
              팀·제작자 탐색하기
              <span aria-hidden="true">→</span>
            </RouterLink>
            <RouterLink class="lovable-outline-link" :to="{ name: 'teams-new' }">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 7h16M7 3v4M17 3v4M6 11h12v8H6z" />
              </svg>
              팀 만들기
            </RouterLink>
            <small>· 가입 없이 둘러보기</small>
          </div>
          <dl class="lovable-stats">
            <div v-for="stat in stats" :key="stat.label">
              <dt>{{ stat.value }}</dt>
              <dd>{{ stat.label }}</dd>
            </div>
          </dl>
        </div>

        <div class="lovable-hero-art-wrap">
          <div class="slate-cover cover-blueroom cover-ratio-portrait">
            <div class="slate-cover-grid" />
            <div class="slate-cover-perf left"><span v-for="index in 8" :key="index" /></div>
            <div class="slate-cover-perf right"><span v-for="index in 8" :key="index" /></div>
            <p>SLATE A · SCN 014 · TAKE 03</p>
            <strong>단편 〈푸른 방〉</strong>
          </div>
          <aside class="lovable-match-card">
            <span>✦ AI 매칭 추천</span>
            <strong>윤하정 · 촬영감독</strong>
            <p>적합도 92% · 야간/스릴러 경험 일치</p>
            <i><b /></i>
          </aside>
          <aside class="lovable-deadline-card">
            <span>D-24</span>
            <strong>서울독립영화제 마감</strong>
          </aside>
        </div>
      </div>
    </section>

    <section class="lovable-section">
      <div class="lovable-home-container">
        <header class="lovable-section-head">
          <span>누구를 위한 도구인가</span>
          <h2>네 종류의 사용자, 하나의 플랫폼</h2>
        </header>
        <div class="lovable-persona-grid">
          <article v-for="item in personas" :key="item.title">
            <span>{{ item.tag }}</span>
            <h3>{{ item.title }}</h3>
            <p>{{ item.copy }}</p>
            <strong>{{ item.cta }} →</strong>
          </article>
        </div>
      </div>
    </section>

    <section class="lovable-section">
      <div class="lovable-home-container">
        <header class="lovable-section-head">
          <span>핵심 기능</span>
          <h2>흩어진 제작 정보를, 한 흐름으로</h2>
        </header>
        <div class="lovable-feature-grid">
          <article v-for="feature in features" :key="feature.title" class="lovable-feature-card">
            <div>
              <span class="lovable-feature-icon">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <template v-if="feature.icon === 'users'">
                    <circle cx="8" cy="8" r="3" />
                    <path d="M2.5 19c0-3 2.3-5 5.5-5s5.5 2 5.5 5M16 6a2.5 2.5 0 0 1 0 5M17 14c2.4.4 4 2.1 4 4.5" />
                  </template>
                  <template v-else-if="feature.icon === 'clapper'">
                    <path d="M4 7h16M7 3v4M13 3v4M19 3v4M5 11h14v8H5z" />
                  </template>
                  <template v-else-if="feature.icon === 'film'">
                    <rect x="4" y="3" width="16" height="18" rx="2" />
                    <path d="M8 3v18M16 3v18M4 8h4M4 16h4M16 8h4M16 16h4" />
                  </template>
                  <template v-else-if="feature.icon === 'pin'">
                    <path d="M12 21s6-5.3 6-11a6 6 0 1 0-12 0c0 5.7 6 11 6 11Z" />
                    <circle cx="12" cy="10" r="2" />
                  </template>
                  <template v-else-if="feature.icon === 'trophy'">
                    <path d="M8 4h8v5a4 4 0 0 1-8 0V4Z" />
                    <path d="M8 6H4v2a4 4 0 0 0 4 4M16 6h4v2a4 4 0 0 1-4 4M12 13v5M9 21h6" />
                  </template>
                  <template v-else>
                    <path d="m20 6-11 11-5-5" />
                  </template>
                </svg>
              </span>
              <small v-if="feature.ai">✦ AI</small>
            </div>
            <h3>{{ feature.title }}</h3>
            <p>{{ feature.body }}</p>
          </article>
        </div>
      </div>
    </section>

    <section class="lovable-section">
      <div class="lovable-home-container">
        <div class="lovable-section-row">
          <header class="lovable-section-head">
            <span>지금 모집 중</span>
            <h2>진행 중 프로젝트와 사람들</h2>
          </header>
          <RouterLink :to="{ name: 'discover' }">전체 보기 →</RouterLink>
        </div>

        <div class="lovable-discover-grid">
          <div class="lovable-team-grid">
            <RouterLink
              v-for="team in teams"
              :key="team.id"
              class="lovable-team-card"
              :to="{ name: 'teams-detail', params: { teamId: team.id } }"
            >
              <div class="slate-cover" :class="[`cover-${team.cover}`]">
                <div class="slate-cover-grid" />
                <div class="slate-cover-perf left"><span v-for="index in 8" :key="index" /></div>
                <div class="slate-cover-perf right"><span v-for="index in 8" :key="index" /></div>
                <p>{{ team.stage }} · {{ team.genre }}</p>
              </div>
              <div>
                <span>{{ team.region }} · {{ team.period }} <em>+{{ team.open }} 모집</em></span>
                <h3>{{ team.title }}</h3>
                <p>{{ team.summary }}</p>
              </div>
            </RouterLink>
          </div>

          <aside class="lovable-crew-panel">
            <span>RECOMMENDED CREW</span>
            <h3>팀에 맞는 제작자</h3>
            <ul>
              <li v-for="profile in profiles" :key="profile.name">
                <i />
                <div>
                  <strong>{{ profile.name }} <small>· {{ profile.role }}</small></strong>
                  <span>{{ profile.region }} · {{ profile.years }}yr · {{ profile.tag }}</span>
                </div>
                <b>{{ profile.score }}<small>%</small></b>
              </li>
            </ul>
          </aside>
        </div>
      </div>
    </section>

    <section class="lovable-section">
      <div class="lovable-home-container lovable-ai-grid">
        <div>
          <span class="lovable-ai-kicker">SLATE INTELLIGENCE</span>
          <h2>
            장면을 설명하면,
            <br>
            로케이션이 떠오릅니다.
          </h2>
          <p>
            씬 설명과 지역 조건을 입력하면 Slate가 촬영지 후보를 정리해 보여줍니다.
            개인 또는 팀 후보로 저장하고, 팀 워크스페이스에서 함께 검토하세요.
          </p>
          <RouterLink class="lovable-film-link" :to="{ name: 'locations' }">✦ 로케이션 탐색 열기</RouterLink>
        </div>
        <article class="lovable-ai-card">
          <header>
            <span>✦</span>
            <div>
              <small>PROMPT</small>
              <p>“1990년대 인쇄골목 야간, 네온 간판이 비치는 좁은 골목 — 서울 중구 기준”</p>
            </div>
            <button type="button">RE-RUN</button>
          </header>
          <div>
            <article v-for="location in aiLocations" :key="location.name">
              <span>{{ location.region }}</span>
              <strong>{{ location.name }}</strong>
              <p>{{ location.reason }}</p>
            </article>
          </div>
        </article>
      </div>
    </section>

    <section class="lovable-section">
      <div class="lovable-home-container">
        <div class="lovable-section-row">
          <header class="lovable-section-head">
            <span>공모전</span>
            <h2>저장한 공모전과 적합도</h2>
          </header>
          <RouterLink :to="{ name: 'contests' }">공모전 전체 →</RouterLink>
        </div>
        <div class="lovable-contest-grid">
          <article v-for="contest in contests" :key="contest.title">
            <div>
              <strong>D-{{ contest.dleft }}</strong>
              <span>{{ contest.status }}</span>
            </div>
            <section>
              <h3>
                {{ contest.title }}
                <small v-if="contest.saved">SAVED</small>
              </h3>
              <p>{{ contest.host }} · {{ contest.region }} · {{ contest.prize }}</p>
              <div>
                <i :style="{ width: `${contest.fit}%` }" />
              </div>
              <b>{{ contest.fit }}% <small>FIT</small></b>
            </section>
          </article>
        </div>
      </div>
    </section>

    <section class="lovable-closing">
      <div class="lovable-home-container">
        <div class="lovable-closing-card">
          <div class="film-gradient" />
          <section>
            <span>START YOUR NEXT SLATE</span>
            <h2>
              이름만 있는 프로젝트를,
              <br>
              크레딧이 있는 작품으로.
            </h2>
            <p>
              가입 없이도 둘러볼 수 있습니다. 준비가 되면 프로필을 만들고 팀에 합류하거나,
              직접 팀을 꾸려보세요.
            </p>
            <div>
              <RouterLink class="lovable-solid-link" :to="{ name: 'discover' }">탐색 시작</RouterLink>
              <RouterLink class="lovable-outline-link" :to="{ name: 'teams-new' }">팀 만들기</RouterLink>
              <RouterLink :to="{ name: 'works' }">작업물 둘러보기</RouterLink>
            </div>
          </section>
        </div>

        <div class="lovable-work-row">
          <article v-for="work in works" :key="work.title">
            <div class="slate-cover cover-generic cover-ratio-square">
              <div class="slate-cover-grid" />
              <p>REEL</p>
            </div>
            <div>
              <span>{{ work.kind }} · {{ work.year }}</span>
              <strong>{{ work.title }}</strong>
              <p>{{ work.by }} · {{ work.role }}</p>
            </div>
          </article>
        </div>
      </div>
    </section>
  </section>
</template>
