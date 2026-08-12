<script setup>
const team = {
  name: '블루룸 픽처스',
  title: '단편영화 <푸른 방> 제작팀',
  period: '2026.07 - 2026.10',
  stage: '프리프로덕션',
  region: '서울',
  members: 7,
  capacity: 12,
  slots: [
    { role: '촬영감독', need: 1, deadline: '07/02', note: '야간 촬영 다수' },
    { role: '동시녹음', need: 1, deadline: '07/05', note: '주말 가능자' },
    { role: '미술', need: 1, deadline: '07/10', note: '1990s 인테리어' }
  ]
}

const kpis = [
  { label: '모집 중 slot', value: '3', sub: '마감 임박 1' },
  { label: '검토 중 지원자', value: '11', sub: '신규 4', tone: 'film' },
  { label: '수락한 팀원', value: '7', sub: '정원 12' },
  { label: '이번 주 일정', value: '5', sub: '촬영 1, 회의 4' }
]

const applicants = [
  { name: '윤하정', role: '촬영감독', region: '서울', years: 6, works: 17, score: 92, status: '검토 중', tone: 'warning' },
  { name: '김도윤', role: '동시녹음', region: '경기', years: 4, works: 9, score: 87, status: '수락', tone: 'success' },
  { name: '이서원', role: '편집', region: '서울', years: 8, works: 31, score: 90, status: '검토 중', tone: 'warning' },
  { name: '박지운', role: '프로듀서', region: '서울', years: 10, works: 12, score: 84, status: '초대 발송', tone: 'film' },
  { name: '한미르', role: '색보정', region: '서울', years: 5, works: 22, score: 88, status: '거절', tone: 'muted' }
]

const queueTabs = ['전체', '검토', '수락', '거절', '초대']

const kanban = [
  { title: '기획', items: ['트리트먼트 v3', '캐스팅 콜 작성'] },
  { title: '진행', items: ['로케이션 헌팅 (을지로)', '장비 견적 비교'] },
  { title: '완료', items: ['스토리보드 v2', '예산안 1차'] }
]

const approvals = [
  { label: '스토리보드 v2', status: '승인됨', tone: 'success' },
  { label: '로케이션 무드 보드', status: '검토 중', tone: 'warning' },
  { label: '캐스팅 노트 v1', status: '반려 (재요청)', tone: 'destructive' }
]

const notifications = [
  { id: 1, kind: '지원', text: '<푸른 방> 팀이 지원을 수락했습니다.', time: '방금' },
  { id: 2, kind: '초대', text: '나이트아울 팀에서 조명 역할로 초대했습니다.', time: '1시간 전' },
  { id: 3, kind: '공모전', text: '저장한 공모전 서울독립영화제 마감 24일 전.', time: '오늘' },
  { id: 4, kind: '리뷰', text: '산책에 새 리뷰 3개가 달렸습니다.', time: '어제' }
]

const locations = [
  { name: '을지로 4가 인쇄골목', scope: '팀' },
  { name: '양양 죽도 해변', scope: '팀' },
  { name: '수원 화서문', scope: '개인' }
]
</script>

<template>
  <section class="lovable-workspace-page">
    <header class="lovable-workspace-head">
      <div>
        <span>WORKSPACE · {{ team.name }}</span>
        <h1>{{ team.title }}</h1>
        <p>팀 리더 보기 · {{ team.period }}</p>
      </div>
      <div>
        <button type="button">팀 정보 수정</button>
        <button type="button">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          모집 공고
        </button>
      </div>
    </header>

    <section class="lovable-workspace-kpis" aria-label="워크스페이스 요약">
      <article v-for="kpi in kpis" :key="kpi.label">
        <span>{{ kpi.label }}</span>
        <strong :class="{ film: kpi.tone === 'film' }">{{ kpi.value }}</strong>
        <p>{{ kpi.sub }}</p>
      </article>
    </section>

    <div class="lovable-workspace-layout">
      <main class="lovable-workspace-main">
        <section class="lovable-workspace-section">
          <h2>모집 공고 · 구인 slot</h2>
          <div class="lovable-workspace-slots">
            <article v-for="(slot, index) in team.slots" :key="slot.role">
              <div>
                <h3>{{ slot.role }}</h3>
                <span>SLOT-{{ String(index + 1).padStart(2, '0') }}</span>
              </div>
              <div>
                <p>필요 {{ slot.need }}명 · 남음 {{ slot.need }}</p>
                <span>마감 {{ slot.deadline }}</span>
              </div>
              <p>{{ slot.note }}</p>
              <footer>
                <button type="button">수정</button>
                <button type="button">마감</button>
              </footer>
            </article>
          </div>
        </section>

        <section class="lovable-workspace-section">
          <header>
            <h2>지원자 / 초대 큐</h2>
            <div>
              <button
                v-for="(tab, index) in queueTabs"
                :key="tab"
                type="button"
                :class="{ active: index === 0 }"
              >
                {{ tab }}
              </button>
            </div>
          </header>

          <div class="lovable-workspace-applicants">
            <article v-for="applicant in applicants" :key="applicant.name">
              <div class="lovable-workspace-applicant-profile">
                <i></i>
                <div>
                  <strong>{{ applicant.name }}</strong>
                  <span>{{ applicant.role }} · {{ applicant.region }}</span>
                </div>
              </div>
              <div class="lovable-workspace-applicant-meta">
                <p>경력 {{ applicant.years }}년</p>
                <span>작업물 {{ applicant.works }}</span>
              </div>
              <div class="lovable-workspace-fit">
                <div>
                  <span :style="{ width: `${applicant.score}%` }"></span>
                </div>
                <strong>{{ applicant.score }}%</strong>
                <small>FIT</small>
              </div>
              <span class="lovable-workspace-status" :class="applicant.tone">{{ applicant.status }}</span>
              <footer>
                <button type="button">프로필</button>
                <button type="button">수락</button>
              </footer>
            </article>
          </div>
        </section>

        <section class="lovable-workspace-section">
          <h2>팀 계획 · 칸반</h2>
          <div class="lovable-workspace-kanban">
            <article v-for="column in kanban" :key="column.title">
              <header>
                <span>{{ column.title }}</span>
                <small>{{ column.items.length }}</small>
              </header>
              <ul>
                <li v-for="item in column.items" :key="item">{{ item }}</li>
                <li>
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M12 5v14M5 12h14" />
                  </svg>
                  추가
                </li>
              </ul>
            </article>
          </div>
        </section>

        <section class="lovable-workspace-section">
          <h2>팀 작업물 승인</h2>
          <div class="lovable-workspace-approval-grid">
            <article v-for="item in approvals" :key="item.label">
              <div class="slate-cover cover-ratio-wide cover-generic">
                <div class="slate-cover-grid"></div>
                <p>DRAFT</p>
              </div>
              <div>
                <strong>{{ item.label }}</strong>
                <span :class="item.tone">
                  <svg v-if="item.tone === 'success'" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M20 6 9 17l-5-5" />
                  </svg>
                  <svg v-else-if="item.tone === 'warning'" viewBox="0 0 24 24" aria-hidden="true">
                    <circle cx="12" cy="12" r="9" />
                    <path d="M12 7v5l3 2" />
                  </svg>
                  <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                    <circle cx="12" cy="12" r="9" />
                    <path d="m15 9-6 6M9 9l6 6" />
                  </svg>
                  {{ item.status }}
                </span>
              </div>
            </article>
          </div>
        </section>
      </main>

      <aside class="lovable-workspace-aside">
        <section>
          <span>팀 정보</span>
          <ul class="lovable-workspace-team-info">
            <li>
              <span>정원</span>
              <strong>{{ team.members }}/{{ team.capacity }}</strong>
            </li>
            <li>
              <span>기간</span>
              <strong>{{ team.period }}</strong>
            </li>
            <li>
              <span>지역</span>
              <strong>{{ team.region }}</strong>
            </li>
            <li>
              <span>단계</span>
              <strong>{{ team.stage }}</strong>
            </li>
          </ul>
          <div>
            <button type="button">리더 위임</button>
            <button type="button">팀 종료</button>
          </div>
        </section>

        <section>
          <span>알림</span>
          <ul class="lovable-workspace-notifications">
            <li v-for="notification in notifications" :key="notification.id">
              <div>
                <span>{{ notification.kind }}</span>
                <small>{{ notification.time }}</small>
              </div>
              <p>{{ notification.text }}</p>
            </li>
          </ul>
        </section>

        <section>
          <span>팀 후보 로케이션</span>
          <ul class="lovable-workspace-locations">
            <li v-for="location in locations" :key="location.name">
              <strong>{{ location.name }}</strong>
              <small :class="{ team: location.scope === '팀' }">{{ location.scope }}</small>
            </li>
          </ul>
        </section>
      </aside>
    </div>
  </section>
</template>
