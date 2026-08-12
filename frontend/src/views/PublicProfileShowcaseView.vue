<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const profiles = [
  {
    id: 'p-yoon',
    aliases: ['1'],
    name: '윤하정',
    handle: '@hajeong',
    role: '촬영감독',
    region: '서울',
    years: 6,
    tags: ['ARRI', '아나모픽', '야간'],
    bio: '단편·뮤직비디오 위주. 자연광과 차가운 색온도를 좋아합니다.',
    works: 17,
    followers: 1284,
    score: 92,
    available: '2026.07 - 2026.10',
    reason: '장르(스릴러), 지역(서울), 야간 촬영 경험 일치'
  },
  {
    id: 'p-kim',
    aliases: ['2'],
    name: '김도윤',
    handle: '@doyoonk',
    role: '동시녹음',
    region: '경기',
    years: 4,
    tags: ['Sennheiser', '야외', '다큐'],
    bio: '야외 다큐 5년차. 험지 현장 환영.',
    works: 9,
    followers: 412,
    score: 87,
    available: '2026.08 부터',
    reason: '야외 다큐 경험, 일정 일치'
  },
  {
    id: 'p-lee',
    aliases: ['3'],
    name: '이서원',
    handle: '@seowonl',
    role: '편집',
    region: '서울',
    years: 8,
    tags: ['DaVinci', 'Premiere', '뮤직비디오'],
    bio: '리듬감 있는 컷. 단편·뮤직비디오 30편 이상.',
    works: 31,
    followers: 2104,
    score: 90,
    available: '상시',
    reason: '포트폴리오 장르 적합도 상위'
  },
  {
    id: 'p-park',
    aliases: ['4'],
    name: '박지운',
    handle: '@jiwoonp',
    role: '프로듀서',
    region: '서울',
    years: 10,
    tags: ['장편', '지원사업', '공모'],
    bio: '독립 장편 프로듀싱. 지원사업과 공모 경험 다수.',
    works: 12,
    followers: 980,
    score: 84,
    available: '프리프로덕션 한정',
    reason: '팀 단계(프리프로덕션) 일치'
  }
]

const portfolioTitles = ['산책', 'Owl', '푸른 방', '마지막 일요일', '검은 강', '리허설']
const credits = [
  { year: 2025, title: '산책', type: '단편', source: 'KOBIS' },
  { year: 2024, title: '마지막 일요일', type: '장편', source: 'KOBIS' },
  { year: 2024, title: 'Owl', type: '뮤직비디오', source: 'YOUTUBE' },
  { year: 2023, title: '검은 강', type: '단편', source: 'DIRECT' }
]

const activeProfile = computed(() => {
  const profileId = route.params.profileId ? String(route.params.profileId) : ''
  return profiles.find((profile) => profile.id === profileId || profile.aliases.includes(profileId)) || profiles[0]
})

const portfolioItems = computed(() => portfolioTitles.map((title, index) => ({
  title,
  meta: `${index % 2 ? 'MUSIC VIDEO' : 'SHORT FILM'} · 202${4 + (index % 2)}`,
  credit: activeProfile.value.role,
  cover: index % 2 ? 'nightowl' : 'blueroom',
  likes: 120 + index * 31,
  comments: 12 + index * 3,
  badge: index === 0 ? 'YOUTUBE' : index === 2 ? 'KOBIS' : ''
})))
</script>

<template>
  <section class="lovable-profile-page">
    <RouterLink class="lovable-profile-back" :to="{ name: 'discover' }">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M19 12H5M12 19l-7-7 7-7" />
      </svg>
      탐색
    </RouterLink>

    <div class="lovable-profile-layout">
      <main class="lovable-profile-main">
        <header class="lovable-profile-hero">
          <i></i>
          <div>
            <span>{{ activeProfile.handle }}</span>
            <h1>{{ activeProfile.name }}</h1>
            <div class="lovable-profile-pills">
              <span>{{ activeProfile.role }}</span>
              <span>
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 21s7-5.2 7-11a7 7 0 0 0-14 0c0 5.8 7 11 7 11Z" />
                  <circle cx="12" cy="10" r="2.5" />
                </svg>
                {{ activeProfile.region }}
              </span>
              <span>
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M8 2v4M16 2v4M3 10h18" />
                  <rect x="3" y="4" width="18" height="18" rx="2" />
                </svg>
                {{ activeProfile.available }}
              </span>
              <span>
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M4 4h16v16H4z" />
                  <path d="M8 4v16M16 4v16M4 9h4M4 15h4M16 9h4M16 15h4" />
                </svg>
                경력 {{ activeProfile.years }}년
              </span>
            </div>
            <p>{{ activeProfile.bio }}</p>
          </div>
        </header>

        <section class="lovable-profile-section">
          <header>
            <div>
              <span>PORTFOLIO</span>
              <h2>포트폴리오</h2>
            </div>
            <button type="button">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 5v14M5 12h14" />
              </svg>
              추가
            </button>
          </header>

          <div class="lovable-profile-portfolio-grid">
            <article v-for="item in portfolioItems" :key="item.title">
              <div class="slate-cover" :class="`cover-${item.cover}`">
                <div class="slate-cover-grid"></div>
                <div class="slate-cover-perf left"><span v-for="index in 12" :key="`l-${item.title}-${index}`"></span></div>
                <div class="slate-cover-perf right"><span v-for="index in 12" :key="`r-${item.title}-${index}`"></span></div>
                <p>{{ item.meta.split(' · ')[0] }}</p>
                <strong>{{ item.title }}</strong>
              </div>
              <div>
                <span>{{ item.meta }}</span>
                <h3>{{ item.title }}</h3>
                <p>크레딧 · {{ item.credit }}</p>
                <footer>
                  <span>
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 1 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z" />
                    </svg>
                    {{ item.likes }}
                  </span>
                  <span>
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4v8Z" />
                    </svg>
                    {{ item.comments }}
                  </span>
                  <strong v-if="item.badge">{{ item.badge }}</strong>
                </footer>
              </div>
            </article>
          </div>
        </section>

        <section class="lovable-profile-section">
          <header>
            <div>
              <span>CREDITS</span>
              <h2>공공데이터 기반 참여 이력</h2>
            </div>
          </header>
          <div class="lovable-profile-credit-table">
            <table>
              <thead>
                <tr>
                  <th>연도</th>
                  <th>작품</th>
                  <th>유형</th>
                  <th>크레딧</th>
                  <th>출처</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="credit in credits" :key="`${credit.year}-${credit.title}`">
                  <td>{{ credit.year }}</td>
                  <td>{{ credit.title }}</td>
                  <td>{{ credit.type }}</td>
                  <td>{{ activeProfile.role }}</td>
                  <td>{{ credit.source }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </main>

      <aside class="lovable-profile-aside">
        <section class="lovable-profile-action">
          <button type="button">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M19 8v6M22 11h-6" />
            </svg>
            팀에 초대
          </button>
          <button type="button">팔로우</button>
          <dl>
            <div>
              <dt>{{ activeProfile.works }}</dt>
              <dd>작업물</dd>
            </div>
            <div>
              <dt>{{ activeProfile.followers }}</dt>
              <dd>팔로워</dd>
            </div>
            <div>
              <dt>{{ activeProfile.score }}%</dt>
              <dd>평균 FIT</dd>
            </div>
          </dl>
        </section>

        <section class="lovable-profile-info">
          <span>협업 조건</span>
          <dl>
            <div><dt>유형</dt><dd>유상 / 무상 협의</dd></div>
            <div><dt>출장</dt><dd>국내 가능</dd></div>
            <div><dt>장비</dt><dd>본인 보유</dd></div>
            <div><dt>근무</dt><dd>주말 OK</dd></div>
          </dl>
        </section>

        <section class="lovable-profile-ai">
          <span>
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="m12 3 1.8 5.5 5.7 1.7-5.7 1.8L12 18l-1.8-6-5.7-1.8 5.7-1.7L12 3Z" />
            </svg>
            AI 추천 이유
          </span>
          <p>{{ activeProfile.reason }}</p>
        </section>

        <section class="lovable-profile-tags">
          <span>태그</span>
          <div>
            <small v-for="tag in activeProfile.tags" :key="tag">{{ tag }}</small>
          </div>
        </section>
      </aside>
    </div>
  </section>
</template>
