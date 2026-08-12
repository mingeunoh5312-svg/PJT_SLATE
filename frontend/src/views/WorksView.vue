<script setup>
import { computed, ref } from 'vue'

const categories = ['전체', '단편', '뮤직비디오', '다큐', '광고', '자유글']
const activeCategory = ref('전체')

const works = [
  {
    id: 'w-1',
    title: '산책',
    by: '이서원',
    role: '감독·편집',
    kind: '단편',
    minutes: 8,
    year: 2025,
    likes: 412,
    reviews: 38,
    badge: '이주의 작업물',
    cover: 'blueroom'
  },
  {
    id: 'w-2',
    title: 'Owl (Official MV)',
    by: '윤하정',
    role: '촬영감독',
    kind: '뮤직비디오',
    minutes: 4,
    year: 2025,
    likes: 1820,
    reviews: 96,
    badge: null,
    cover: 'nightowl'
  },
  {
    id: 'w-3',
    title: '오프시즌 — 티저',
    by: '김도윤',
    role: '동시녹음',
    kind: '다큐 티저',
    minutes: 2,
    year: 2026,
    likes: 230,
    reviews: 14,
    badge: null,
    cover: 'doc'
  },
  {
    id: 'w-4',
    title: '푸른 방 (스틸 컷)',
    by: '박지운',
    role: '프로듀서',
    kind: '프로덕션 스틸',
    minutes: 0,
    year: 2026,
    likes: 178,
    reviews: 9,
    badge: '신규',
    cover: 'ad'
  }
]

const profiles = [
  { name: '윤하정', role: '촬영감독', region: '서울', followers: 1284 },
  { name: '김도윤', role: '동시녹음', region: '경기', followers: 412 },
  { name: '이서원', role: '편집', region: '서울', followers: 2104 },
  { name: '박지운', role: '프로듀서', region: '서울', followers: 980 },
  { name: '한미르', role: '색보정', region: '서울', followers: 1530 }
]

const reviews = [
  {
    who: '윤하정',
    role: '촬영감독',
    text: '흔들리는 핸드헬드와 정적 컷의 대비가 좋았어요. 사운드 디자인이 특히 인상적.'
  },
  {
    who: '한미르',
    role: '색보정',
    text: '그레이딩 톤이 일관적. 미들 톤이 차가워서 인물 감정과 잘 맞았습니다.'
  },
  {
    who: '박지운',
    role: '프로듀서',
    text: '9분 안에 이만큼의 호흡을 잡아낸 게 인상적입니다. 장편 가능성 충분.'
  }
]

const visibleWorks = computed(() => {
  if (activeCategory.value === '전체') return works
  if (activeCategory.value === '다큐') return works.filter((work) => work.kind.includes('다큐'))
  return works.filter((work) => work.kind === activeCategory.value)
})

const popularWorks = computed(() => works.map((work, index) => ({
  rank: index + 1,
  title: work.title,
  sub: `${work.by} · ${work.role}`,
  count: work.likes
})))

const popularCreators = computed(() => profiles.map((profile, index) => ({
  rank: index + 1,
  title: profile.name,
  sub: `${profile.role} · ${profile.region}`,
  count: profile.followers
})))

function workMeta(work) {
  return `${work.kind.toUpperCase()} · ${work.minutes ? `${work.minutes} MIN` : 'STILL'}`
}
</script>

<template>
  <section class="lovable-works-page" aria-label="작업물">
    <header class="lovable-works-head">
      <div>
        <span>FEED</span>
        <h1>작업물</h1>
        <p>완성된 작품과 작업 과정을 공유하고 리뷰와 좋아요로 신뢰를 쌓습니다.</p>
      </div>
      <div>
        <button type="button" class="lovable-work-tool">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 6h16M7 12h10M10 18h4" />
          </svg>
          필터
        </button>
        <button type="button" class="lovable-work-post">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          게시
        </button>
      </div>
    </header>

    <nav class="lovable-work-tabs" aria-label="작업물 분류">
      <button
        v-for="category in categories"
        :key="category"
        type="button"
        :class="{ active: activeCategory === category }"
        @click="activeCategory = category"
      >
        {{ category }}
      </button>
    </nav>

    <div class="lovable-works-layout">
      <main class="lovable-works-feed">
        <section class="lovable-featured-work">
          <div class="slate-cover cover-blueroom cover-ratio-wide">
            <div class="slate-cover-grid" />
            <div class="slate-cover-perf left"><span v-for="index in 8" :key="index" /></div>
            <div class="slate-cover-perf right"><span v-for="index in 8" :key="index" /></div>
            <p>FEATURED · 이서원 감독·편집</p>
            <strong>산책 — 단편 (8분)</strong>
          </div>
          <div>
            <button type="button">재생</button>
            <button type="button">상세 보기</button>
          </div>
        </section>

        <section class="lovable-work-card-grid" aria-label="작업물 목록">
          <article v-for="work in visibleWorks" :key="work.id" class="lovable-work-card">
            <div class="lovable-work-cover">
              <div class="slate-cover" :class="`cover-${work.cover}`">
                <div class="slate-cover-grid" />
                <div class="slate-cover-perf left"><span v-for="index in 8" :key="index" /></div>
                <div class="slate-cover-perf right"><span v-for="index in 8" :key="index" /></div>
                <p>{{ workMeta(work) }}</p>
              </div>
              <span v-if="work.badge">{{ work.badge }}</span>
            </div>
            <div class="lovable-work-copy">
              <h2>{{ work.title }}</h2>
              <p>{{ work.by }} · {{ work.role }} · {{ work.year }}</p>
              <div>
                <span>
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z" />
                  </svg>
                  {{ work.likes }}
                </span>
                <span>
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M21 12a8 8 0 0 1-8 8H6l-3 3v-7a8 8 0 1 1 18-4Z" />
                  </svg>
                  {{ work.reviews }}
                </span>
                <button type="button">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V4s-1 1-4 1-5-2-8-2-4 1-4 1v17" />
                  </svg>
                  신고
                </button>
              </div>
            </div>
          </article>
        </section>

        <section class="lovable-review-panel">
          <header>
            <div>
              <span>REVIEWS</span>
              <h2>‘산책’에 달린 리뷰</h2>
            </div>
            <small>38개</small>
          </header>
          <ul>
            <li v-for="review in reviews" :key="review.who">
              <i aria-hidden="true" />
              <div>
                <strong>{{ review.who }} <span>· {{ review.role }}</span></strong>
                <p>{{ review.text }}</p>
              </div>
            </li>
          </ul>
        </section>
      </main>

      <aside class="lovable-works-sidebar">
        <section class="lovable-rank-card">
          <h2>이번 주 인기 작업물</h2>
          <ol>
            <li v-for="item in popularWorks" :key="item.title">
              <span>{{ item.rank }}</span>
              <div>
                <strong>{{ item.title }}</strong>
                <small>{{ item.sub }}</small>
              </div>
              <b>{{ item.count.toLocaleString() }}</b>
            </li>
          </ol>
        </section>

        <section class="lovable-rank-card">
          <h2>이번 주 인기 제작자</h2>
          <ol>
            <li v-for="item in popularCreators" :key="item.title">
              <span>{{ item.rank }}</span>
              <div>
                <strong>{{ item.title }}</strong>
                <small>{{ item.sub }}</small>
              </div>
              <b>{{ item.count.toLocaleString() }}</b>
            </li>
          </ol>
        </section>

        <section class="lovable-weekly-work">
          <span>🏆 이주의 작업물</span>
          <h2>산책 — 이서원</h2>
          <p>단편 부문 추천. 38건의 리뷰.</p>
        </section>
      </aside>
    </div>
  </section>
</template>
