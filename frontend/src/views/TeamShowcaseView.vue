<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const profiles = [
  { id: 'p-yoon', name: '윤하정', role: '촬영감독' },
  { id: 'p-kim', name: '김도윤', role: '동시녹음' },
  { id: 'p-lee', name: '이서원', role: '편집' },
  { id: 'p-park', name: '박지운', role: '프로듀서' },
  { id: 'p-han', name: '한미르', role: '색보정' },
  { id: 'p-cho', name: '조유나', role: '배우' },
  { id: 'p-choi', name: '최재민', role: '미술' }
]

const teams = [
  {
    id: 't-blueroom',
    aliases: ['1'],
    name: '블루룸 픽처스',
    title: '단편영화 <푸른 방> 제작팀',
    cover: 'blueroom',
    stage: '프리프로덕션',
    genre: '드라마',
    region: '서울',
    period: '2026.07 - 2026.10',
    capacity: 12,
    members: 7,
    leader: '박지운',
    summary: '9분 단편. 한 여성이 텅 빈 아파트에서 마주하는 기억에 대한 이야기. 16mm 필름룩 추구.',
    slots: [
      { role: '촬영감독', need: 1, deadline: '07/02', note: '야간 촬영 다수' },
      { role: '동시녹음', need: 1, deadline: '07/05', note: '주말 가능자' },
      { role: '미술', need: 1, deadline: '07/10', note: '1990s 인테리어' }
    ]
  },
  {
    id: 't-nightowl',
    aliases: ['2'],
    name: '나이트아울',
    title: '뮤직비디오 <Owl>',
    cover: 'nightowl',
    stage: '촬영 준비',
    genre: '뮤직비디오',
    region: '서울 · 경기',
    period: '2026.07.18 - 07.22',
    capacity: 9,
    members: 6,
    leader: '윤하정',
    summary: '단일 곡 뮤직비디오. 도심 야경과 옥상 시퀀스를 중심으로 한 야간 촬영 프로젝트.',
    slots: [
      { role: '조명', need: 2, deadline: '07/08', note: '야간 5일' }
    ]
  },
  {
    id: 't-doc',
    aliases: ['3'],
    name: '오프시즌 다큐',
    title: '다큐 <오프시즌>',
    cover: 'doc',
    stage: '촬영 중',
    genre: '다큐',
    region: '강원 · 경기',
    period: '2026.05 - 2026.12',
    capacity: 6,
    members: 5,
    leader: '김도윤',
    summary: '비시즌 스키장 노동자들의 삶을 따라가는 장편 다큐멘터리.',
    slots: [
      { role: '편집', need: 1, deadline: '상시', note: '원격 협업 가능' }
    ]
  },
  {
    id: 't-ad',
    aliases: ['4'],
    name: '스튜디오 스파크',
    title: '브랜드 광고 <Re:Light>',
    cover: 'ad',
    stage: '기획',
    genre: '광고',
    region: '서울',
    period: '2026.08.01 - 08.10',
    capacity: 10,
    members: 4,
    leader: '이서원',
    summary: '조명 브랜드 30초 광고 3종. 스튜디오 촬영과 외부 로케이션 1곳을 함께 준비합니다.',
    slots: [
      { role: 'VFX', need: 1, deadline: '07/15', note: 'Houdini 우대' },
      { role: '프로듀서', need: 1, deadline: '07/12', note: '' }
    ]
  }
]

const schedule = [
  { date: '07.02', title: '촬영감독 마감' },
  { date: '07.10', title: '로케이션 헌팅 (을지로)' },
  { date: '07.18', title: '리허설 · 본녹음 테스트' },
  { date: '07.25', title: '촬영 1차 · 4일' },
  { date: '08.10', title: '오프라인 편집 1차' },
  { date: '09.02', title: '색보정 · 사운드 믹스' }
]

const teamWorks = [
  { label: '로케이션 무드 보드', meta: 'PRE-PROD' },
  { label: '캐스팅 노트', meta: 'PRE-PROD' },
  { label: '스토리보드 v2', meta: 'PRE-PROD' }
]

const statusRows = [
  { label: '수락', value: 4, tone: 'success' },
  { label: '검토 중', value: 11, tone: 'warning' },
  { label: '거절/취소', value: 6, tone: 'muted' }
]

const activeTeam = computed(() => {
  const teamId = route.params.teamId ? String(route.params.teamId) : ''
  if (!teamId) return teams[0]
  return teams.find((team) => team.id === teamId || team.aliases.includes(teamId)) || teams[0]
})

const visibleProfiles = computed(() => profiles.slice(0, activeTeam.value.members))
</script>

<template>
  <section class="lovable-team-detail-page">
    <RouterLink class="lovable-team-back" :to="{ name: 'discover' }">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M19 12H5M12 19l-7-7 7-7" />
      </svg>
      탐색으로
    </RouterLink>

    <div class="lovable-team-detail-layout">
      <main class="lovable-team-main">
        <div class="slate-cover cover-ratio-wide" :class="`cover-${activeTeam.cover}`">
          <div class="slate-cover-grid"></div>
          <div class="slate-cover-perf left"><span v-for="index in 12" :key="`l-${index}`"></span></div>
          <div class="slate-cover-perf right"><span v-for="index in 12" :key="`r-${index}`"></span></div>
          <p>{{ activeTeam.stage.toUpperCase() }} · {{ activeTeam.genre.toUpperCase() }}</p>
          <strong>{{ activeTeam.title }}</strong>
        </div>

        <div class="lovable-team-pills">
          <span class="stage">{{ activeTeam.stage }}</span>
          <span>
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 21s7-5.2 7-11a7 7 0 0 0-14 0c0 5.8 7 11 7 11Z" />
              <circle cx="12" cy="10" r="2.5" />
            </svg>
            {{ activeTeam.region }}
          </span>
          <span>
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M8 2v4M16 2v4M3 10h18" />
              <rect x="3" y="4" width="18" height="18" rx="2" />
            </svg>
            {{ activeTeam.period }}
          </span>
          <span>
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
              <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
            {{ activeTeam.members }}/{{ activeTeam.capacity }}명
          </span>
          <strong>팀장 · {{ activeTeam.leader }}</strong>
        </div>

        <header class="lovable-team-title">
          <h1>{{ activeTeam.title }}</h1>
          <p>{{ activeTeam.summary }}</p>
        </header>

        <section class="lovable-team-section">
          <h2>모집 중인 역할 · {{ activeTeam.slots.length }}</h2>
          <div class="lovable-team-slot-list">
            <article v-for="slot in activeTeam.slots" :key="slot.role">
              <span>SLOT</span>
              <div>
                <h3>{{ slot.role }}</h3>
                <p>필요 {{ slot.need }}명 · 마감 {{ slot.deadline }}<template v-if="slot.note"> · {{ slot.note }}</template></p>
              </div>
              <button type="button">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="m22 2-7 20-4-9-9-4 20-7Z" />
                  <path d="M22 2 11 13" />
                </svg>
                이 역할로 지원
              </button>
            </article>
          </div>
        </section>

        <section class="lovable-team-section">
          <h2>팀 일정 - 프로덕션 캘린더</h2>
          <ol class="lovable-team-timeline">
            <li v-for="item in schedule" :key="item.date">
              <span></span>
              <small>{{ item.date }}</small>
              <p>{{ item.title }}</p>
            </li>
          </ol>
        </section>

        <section class="lovable-team-section">
          <h2>팀 작업물</h2>
          <div class="lovable-team-work-grid">
            <article v-for="work in teamWorks" :key="work.label">
              <div class="slate-cover cover-ratio-four-three cover-generic">
                <div class="slate-cover-grid"></div>
                <p>{{ work.meta }}</p>
              </div>
              <strong>{{ work.label }}</strong>
            </article>
          </div>
        </section>
      </main>

      <aside class="lovable-team-aside">
        <section class="lovable-team-apply">
          <span>지원 / 저장</span>
          <button type="button">팀 지원하기</button>
          <button type="button">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M6 4h12v17l-6-4-6 4V4Z" />
            </svg>
            저장
          </button>
          <small>현재 상태</small>
          <p>
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 2" />
            </svg>
            지원 검토 중 - 평균 3일
          </p>
        </section>

        <section class="lovable-team-members">
          <span>팀원 {{ activeTeam.members }}명</span>
          <ul>
            <li v-for="profile in visibleProfiles" :key="profile.id">
              <i></i>
              <div>
                <strong>{{ profile.name }}</strong>
                <small>{{ profile.role }}</small>
              </div>
              <em v-if="profile.name === activeTeam.leader">LEAD</em>
            </li>
          </ul>
        </section>

        <section class="lovable-team-status">
          <span>지원 현황</span>
          <ul>
            <li v-for="row in statusRows" :key="row.label">
              <span :class="row.tone">
                <svg v-if="row.tone === 'success'" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M20 6 9 17l-5-5" />
                </svg>
                <svg v-else-if="row.tone === 'warning'" viewBox="0 0 24 24" aria-hidden="true">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M12 7v5l3 2" />
                </svg>
                <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                  <circle cx="12" cy="12" r="9" />
                  <path d="m15 9-6 6M9 9l6 6" />
                </svg>
                {{ row.label }}
              </span>
              <strong>{{ row.value }}</strong>
            </li>
          </ul>
        </section>
      </aside>
    </div>
  </section>
</template>
