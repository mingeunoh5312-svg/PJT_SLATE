<script setup>
const categoryFilters = ['전체', '단편', '장편', '다큐', '광고', '뮤직비디오', '지역']

const deadlineItems = ['D-2', 'D-9', 'D-14', 'D-24', 'D-30', 'D-50', 'D-60']

const contests = [
  {
    id: 'c-1',
    title: '서울독립영화제 단편 부문',
    host: '서울독립영화제',
    prize: '총 4,000만원',
    deadline: '2026.08.20',
    dleft: 24,
    region: '전국',
    tags: ['단편', '독립'],
    saved: true,
    fit: 86,
    status: '접수 중'
  },
  {
    id: 'c-2',
    title: 'MISE-EN-SCENE 단편영화제',
    host: '미장센',
    prize: '총 3,500만원',
    deadline: '2026.09.15',
    dleft: 50,
    region: '전국',
    tags: ['단편', '장르'],
    saved: false,
    fit: 74,
    status: '접수 중'
  },
  {
    id: 'c-3',
    title: '부산국제광고제 영상 부문',
    host: 'AD STARS',
    prize: 'Grand Prix',
    deadline: '2026.07.05',
    dleft: 9,
    region: '전국',
    tags: ['광고', '영상'],
    saved: true,
    fit: 62,
    status: '마감 임박'
  },
  {
    id: 'c-4',
    title: '강원도 로케이션 다큐 공모',
    host: '강원영상위원회',
    prize: '제작비 2,000만원',
    deadline: '2026.06.30',
    dleft: -2,
    region: '강원',
    tags: ['다큐', '지역'],
    saved: false,
    fit: 91,
    status: '종료'
  }
]

const submitChecklist = [
  { text: '출품 신청서 작성', done: true },
  { text: '상영 본 (DCP) 준비', done: true },
  { text: '스틸 사진 5매', done: false },
  { text: '감독 소개 / 연출 의도', done: false },
  { text: '크레딧 리스트 점검', done: false }
]

const companySteps = [
  { text: '기업 계정 가입', status: '완료' },
  { text: '사업자 증빙 서류 업로드', status: '완료' },
  { text: '관리자 승인', status: '승인됨' },
  { text: '공모전 개설 요청 작성', status: '작성 중' },
  { text: '관리자 검토', status: '대기' }
]

const dueLabel = (contest) => (contest.dleft < 0 ? '마감' : `D-${contest.dleft}`)
const isUrgent = (contest) => contest.dleft >= 0 && contest.dleft <= 10
const stepTone = (status) => {
  if (status === '완료' || status === '승인됨') return 'done'
  if (status === '작성 중') return 'active'
  return 'pending'
}
</script>

<template>
  <section class="lovable-contests-page">
    <header class="lovable-contests-head">
      <div>
        <span>CONTESTS</span>
        <h1>공모전</h1>
        <p>탐색, 저장, 프로필·팀 기준 적합도 분석, 제출 체크리스트까지 한 곳에서 관리합니다.</p>
      </div>
      <button type="button" class="lovable-contest-request">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 5v14M5 12h14" />
        </svg>
        기업 공모전 개설 요청
      </button>
    </header>

    <div class="lovable-contest-filterbar">
      <label class="lovable-contest-search">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-3.5-3.5" />
        </svg>
        <input type="search" placeholder="공모전, 주최, 키워드" aria-label="공모전 검색" />
      </label>

      <button
        v-for="(filter, index) in categoryFilters"
        :key="filter"
        type="button"
        :class="{ active: index === 0 }"
      >
        {{ filter }}
      </button>
    </div>

    <div class="lovable-contests-layout">
      <section class="lovable-contests-feed" aria-label="공모전 목록">
        <article class="lovable-deadline-strip">
          <span>UPCOMING DEADLINES</span>
          <div>
            <button
              v-for="(deadline, index) in deadlineItems"
              :key="deadline"
              type="button"
              :class="{ urgent: index <= 1 }"
            >
              <strong>{{ deadline }}</strong>
              <small>공모 {{ index + 1 }}</small>
            </button>
          </div>
        </article>

        <article
          v-for="contest in contests"
          :key="contest.id"
          class="lovable-contest-card"
          :class="{ closed: contest.dleft < 0 }"
        >
          <div class="lovable-contest-day" :class="{ urgent: isUrgent(contest) }">
            <strong>{{ dueLabel(contest) }}</strong>
            <span>{{ contest.status }}</span>
          </div>

          <div class="lovable-contest-copy">
            <div>
              <h2>{{ contest.title }}</h2>
              <span v-if="contest.saved">SAVED</span>
            </div>
            <p>{{ contest.host }} · {{ contest.region }} · 마감 {{ contest.deadline }}</p>

            <ul aria-label="공모전 태그">
              <li v-for="tag in contest.tags" :key="tag">{{ tag }}</li>
              <li>{{ contest.prize }}</li>
            </ul>

            <div class="lovable-contest-fit">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="m12 3 1.8 5.5 5.7 1.7-5.7 1.8L12 18l-1.8-6-5.7-1.8 5.7-1.7L12 3Z" />
              </svg>
              <div>
                <span :style="{ width: `${contest.fit}%` }"></span>
              </div>
              <strong>{{ contest.fit }}% <small>FIT</small></strong>
            </div>
          </div>

          <div class="lovable-contest-actions">
            <button type="button">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M6 4h12v17l-6-4-6 4V4Z" />
              </svg>
              {{ contest.saved ? '저장됨' : '저장' }}
            </button>
            <button type="button">제출 준비</button>
            <button type="button">
              원문
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M7 17 17 7M9 7h8v8" />
              </svg>
            </button>
          </div>
        </article>
      </section>

      <aside class="lovable-contests-aside" aria-label="공모전 보조 정보">
        <section class="lovable-submit-prep">
          <span>제출 준비 체크리스트</span>
          <h2>서울독립영화제 단편</h2>
          <ul>
            <li v-for="item in submitChecklist" :key="item.text" :class="{ done: item.done }">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M20 6 9 17l-5-5" />
              </svg>
              <span>{{ item.text }}</span>
            </li>
          </ul>
          <textarea
            aria-label="제출 준비 메모"
          >DCP 인코딩은 7/30까지. 사운드 마스터 -23 LUFS 확인.</textarea>
        </section>

        <section class="lovable-company-flow">
          <span>기업 공모전 개설 흐름</span>
          <ol>
            <li v-for="(step, index) in companySteps" :key="step.text">
              <strong>{{ index + 1 }}</strong>
              <span>{{ step.text }}</span>
              <small :class="stepTone(step.status)">{{ step.status }}</small>
            </li>
          </ol>
        </section>
      </aside>
    </div>
  </section>
</template>
