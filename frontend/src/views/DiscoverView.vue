<script setup>
import { ref } from 'vue'

const tab = ref('teams')

const roles = ['감독', '촬영감독', '조명', '동시녹음', '프로듀서', '편집', '색보정', 'VFX']
const genres = ['드라마', '스릴러', '다큐', '단편', '뮤직비디오', '광고']
const regions = ['서울', '경기', '인천', '부산', '대구', '광주']
const careers = ['주니어', '미들', '시니어']

const selectedFilters = {
  roles: ['촬영감독'],
  genres: ['드라마', '다큐'],
  regions: ['서울'],
  careers: ['미들']
}

const teams = [
  {
    id: 't-blueroom',
    title: '단편영화 〈푸른 방〉 제작팀',
    cover: 'blueroom',
    stage: '프리프로덕션',
    genre: '드라마',
    region: '서울',
    period: '2026.07 — 2026.10',
    capacity: 12,
    members: 7,
    open: 3,
    summary: '9분 단편. 한 여성이 텅 빈 아파트에서 마주하는 기억에 대한 이야기. 16mm 필름룩 추구.',
    slots: [
      { role: '촬영감독', need: 1 },
      { role: '동시녹음', need: 1 },
      { role: '미술', need: 1 }
    ]
  },
  {
    id: 't-nightowl',
    title: '뮤직비디오 〈Owl〉',
    cover: 'nightowl',
    stage: '촬영 준비',
    genre: '뮤직비디오',
    region: '서울 · 경기',
    period: '2026.07.18 — 07.22',
    capacity: 9,
    members: 6,
    open: 2,
    summary: '단일 곡 뮤직비디오. 도심 야경과 옥상 시퀀스 중심.',
    slots: [
      { role: '조명', need: 2 }
    ]
  },
  {
    id: 't-doc',
    title: '다큐 〈오프시즌〉',
    cover: 'doc',
    stage: '촬영 중',
    genre: '다큐',
    region: '강원 · 경기',
    period: '2026.05 — 2026.12',
    capacity: 6,
    members: 5,
    open: 1,
    summary: '비시즌 스키장 노동자들의 삶을 따라가는 장편 다큐.',
    slots: [
      { role: '편집', need: 1 }
    ]
  },
  {
    id: 't-ad',
    title: '브랜드 광고 〈Re:Light〉',
    cover: 'ad',
    stage: '기획',
    genre: '광고',
    region: '서울',
    period: '2026.08.01 — 08.10',
    capacity: 10,
    members: 4,
    open: 4,
    summary: '조명 브랜드 30초 광고 3종. 스튜디오 + 외부 로케 1.',
    slots: [
      { role: 'VFX', need: 1 },
      { role: '프로듀서', need: 1 }
    ]
  }
]

const profiles = [
  {
    id: 'p-yoon',
    name: '윤하정',
    handle: '@hajeong',
    role: '촬영감독',
    region: '서울',
    years: 6,
    tags: ['ARRI', '아나모픽', '야간'],
    bio: '단편·뮤직비디오 위주. 자연광과 차가운 색온도를 좋아합니다.',
    works: 17,
    score: 92,
    reason: '장르(스릴러), 지역(서울), 야간 촬영 경험 일치'
  },
  {
    id: 'p-kim',
    name: '김도윤',
    handle: '@doyoonk',
    role: '동시녹음',
    region: '경기',
    years: 4,
    tags: ['Sennheiser', '야외', '다큐'],
    bio: '야외 다큐 5년차. 험지 현장 환영.',
    works: 9,
    score: 87,
    reason: '야외 다큐 경험, 일정 일치'
  },
  {
    id: 'p-lee',
    name: '이서원',
    handle: '@seowonl',
    role: '편집',
    region: '서울',
    years: 8,
    tags: ['DaVinci', 'Premiere', '뮤직비디오'],
    bio: '리듬감 있는 컷. 단편·뮤직비디오 30편+.',
    works: 31,
    score: 90,
    reason: '포트폴리오 장르 적합도 상위'
  },
  {
    id: 'p-park',
    name: '박지운',
    handle: '@jiwoonp',
    role: '프로듀서',
    region: '서울',
    years: 10,
    tags: ['장편', '지원사업', '공모'],
    bio: '독립 장편 프로듀싱. 지원사업·공모 경험 다수.',
    works: 12,
    score: 84,
    reason: '팀 단계(프리프로덕션) 일치'
  },
  {
    id: 'p-han',
    name: '한미르',
    handle: '@mirh',
    role: '색보정',
    region: '서울',
    years: 5,
    tags: ['DaVinci', '필름룩'],
    bio: '필름룩·시네마틱 그레이딩.',
    works: 22,
    score: 88,
    reason: '톤 레퍼런스 일치'
  },
  {
    id: 'p-cho',
    name: '조유나',
    handle: '@yunac',
    role: '배우',
    region: '부산',
    years: 3,
    tags: ['단편', '독립영화'],
    bio: '독립영화·단편 위주 활동. 사투리 가능.',
    works: 7,
    score: 81,
    reason: '캐스팅 노트와 외형 일치'
  }
]

function isSelected(group, item) {
  return selectedFilters[group].includes(item)
}
</script>

<template>
  <section class="lovable-discover-page" aria-label="탐색">
    <header class="lovable-discover-head">
      <div>
        <span>DISCOVER</span>
        <h1>탐색</h1>
        <p>
          팀이 사람을 찾고, 제작자가 팀을 찾는 두 방향을 모두 지원합니다.
          필터, 키워드, AI 추천을 함께 사용하세요.
        </p>
      </div>
      <div class="lovable-segmented" role="tablist" aria-label="탐색 대상">
        <button
          type="button"
          :class="{ active: tab === 'teams' }"
          role="tab"
          :aria-selected="tab === 'teams'"
          @click="tab = 'teams'"
        >
          모집 중 팀
        </button>
        <button
          type="button"
          :class="{ active: tab === 'people' }"
          role="tab"
          :aria-selected="tab === 'people'"
          @click="tab = 'people'"
        >
          제작자
        </button>
      </div>
    </header>

    <div class="lovable-discover-grid">
      <aside class="lovable-filter-panel" aria-label="탐색 필터">
        <section class="lovable-filter-search">
          <h2>검색</h2>
          <label>
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.5-3.5" />
            </svg>
            <input placeholder="키워드, 작품명, 팀명">
          </label>
        </section>

        <section class="lovable-filter-group">
          <h2>역할</h2>
          <div>
            <button v-for="item in roles" :key="item" type="button" :class="{ active: isSelected('roles', item) }">{{ item }}</button>
          </div>
        </section>

        <section class="lovable-filter-group">
          <h2>장르</h2>
          <div>
            <button v-for="item in genres" :key="item" type="button" :class="{ active: isSelected('genres', item) }">{{ item }}</button>
          </div>
        </section>

        <section class="lovable-filter-group">
          <h2>지역</h2>
          <div>
            <button v-for="item in regions" :key="item" type="button" :class="{ active: isSelected('regions', item) }">{{ item }}</button>
          </div>
        </section>

        <section class="lovable-filter-group">
          <h2>경력</h2>
          <div>
            <button v-for="item in careers" :key="item" type="button" :class="{ active: isSelected('careers', item) }">{{ item }}</button>
          </div>
        </section>

        <button class="lovable-advanced-filter" type="button">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 6h16M7 12h10M10 18h4" />
          </svg>
          고급 필터
        </button>
      </aside>

      <main class="lovable-discover-results">
        <section class="lovable-ai-banner">
          <i aria-hidden="true">✦</i>
          <div>
            <span>AI RECOMMENDATION</span>
            <h2>{{ tab === 'teams' ? '프로필 기준 가장 맞는 팀 12개를 골랐습니다.' : '팀의 모집 slot에 적합한 제작자 18명을 골랐습니다.' }}</h2>
            <p>필터 변경 시 자동 재계산됩니다.</p>
          </div>
          <button type="button">추천 이유 보기</button>
        </section>

        <section v-if="tab === 'teams'" class="lovable-result-team-grid" aria-label="모집 중 팀">
          <article v-for="team in teams" :key="team.id" class="lovable-result-team-card">
            <div class="slate-cover" :class="`cover-${team.cover}`">
              <div class="slate-cover-grid" />
              <div class="slate-cover-perf left"><span v-for="index in 8" :key="index" /></div>
              <div class="slate-cover-perf right"><span v-for="index in 8" :key="index" /></div>
              <p>{{ team.stage }} · {{ team.genre }}</p>
            </div>
            <div class="lovable-result-team-copy">
              <div class="lovable-result-meta">
                <span>{{ team.region }}</span>
                <b>+{{ team.open }} 모집</b>
              </div>
              <h2>{{ team.title }}</h2>
              <p>{{ team.summary }}</p>
              <div class="lovable-team-stats">
                <span>
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <circle cx="8" cy="8" r="3" />
                    <path d="M2.5 19c0-3 2.3-5 5.5-5s5.5 2 5.5 5M16 6a2.5 2.5 0 0 1 0 5M17 14c2.4.4 4 2.1 4 4.5" />
                  </svg>
                  {{ team.members }}/{{ team.capacity }}명
                </span>
                <small>{{ team.period }}</small>
              </div>
              <div class="lovable-slot-list">
                <span v-for="slot in team.slots" :key="slot.role">{{ slot.role }} · {{ slot.need }}</span>
              </div>
              <footer>
                <button type="button">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M6 4h12v16l-6-3-6 3V4Z" />
                  </svg>
                  저장
                </button>
                <RouterLink :to="{ name: 'teams-detail', params: { teamId: team.id } }">팀 보기 →</RouterLink>
              </footer>
            </div>
          </article>
        </section>

        <section v-else class="lovable-people-grid" aria-label="제작자">
          <RouterLink
            v-for="person in profiles"
            :key="person.id"
            class="lovable-person-card"
            :to="{ name: 'public-profile', params: { profileId: person.id } }"
          >
            <header>
              <i aria-hidden="true" />
              <div>
                <h2>{{ person.name }}</h2>
                <span>{{ person.handle }} · {{ person.region }}</span>
              </div>
              <strong>{{ person.score }}<small>FIT</small></strong>
            </header>
            <dl>
              <div>
                <dt>역할</dt>
                <dd>{{ person.role }}</dd>
              </div>
              <div>
                <dt>경력</dt>
                <dd>{{ person.years }}년</dd>
              </div>
              <div>
                <dt>작업물</dt>
                <dd>{{ person.works }}</dd>
              </div>
            </dl>
            <p>{{ person.bio }}</p>
            <div class="lovable-person-tags">
              <span v-for="tag in person.tags" :key="tag">{{ tag }}</span>
            </div>
            <aside>
              <span>✦</span>
              {{ person.reason }}
            </aside>
          </RouterLink>
        </section>
      </main>
    </div>
  </section>
</template>
