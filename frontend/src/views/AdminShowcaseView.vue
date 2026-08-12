<script setup>
const kpis = [
  { label: '우선 처리', value: '14', sub: '신고 6, 승인 4, 기타 4', tone: 'film' },
  { label: '회원', value: '12,840', sub: '신규 +128 / 7일' },
  { label: '기업 승인 대기', value: '9', sub: '평균 1.2일' },
  { label: '공모전', value: '64', sub: '접수중 41' },
  { label: '신고 미처리', value: '6', sub: '콘텐츠 4, 리뷰 2' }
]

const adminNav = ['대시보드', '회원', '팀', '신고', '기업 승인', '공모전', '권한·로그', '매칭 정책']

const priorityTasks = [
  { kind: '기업 승인', title: '(주)스튜디오라이트 - 사업자등록증 제출', time: '12분 전' },
  { kind: '신고', title: '리허설 게시글 - 저작권 의심 (3건 누적)', time: '1시간 전' },
  { kind: '공모전 요청', title: '오로라 픽처스 - 단편 공모전 개설 요청', time: '2시간 전' },
  { kind: '회원 제재', title: '@anon42 - 반복 신고 누적', time: '어제' }
]

const users = [
  { handle: '@hajeong', name: '윤하정', role: '촬영감독', status: '활성', reports: 0 },
  { handle: '@doyoonk', name: '김도윤', role: '동시녹음', status: '활성', reports: 0 },
  { handle: '@seowonl', name: '이서원', role: '편집', status: '비활성', reports: 0 },
  { handle: '@jiwoonp', name: '박지운', role: '프로듀서', status: '활성', reports: 0 },
  { handle: '@anon42', name: '한미르', role: '색보정', status: '제재 (3일)', reports: 3 },
  { handle: '@yunac', name: '조유나', role: '배우', status: '활성', reports: 0 }
]

const companyRequests = [
  { company: '오로라 픽처스', title: '단편 영화 공모전 2026', status: '검토 중' },
  { company: '라이트하우스 미디어', title: '브랜드 영상 챌린지', status: '서류 보완 요청' },
  { company: '북악 스튜디오', title: '음악 다큐 공모', status: '승인됨' }
]

const weights = [
  { label: '역할 일치', value: 30 },
  { label: '장르 경험', value: 22 },
  { label: '지역', value: 14 },
  { label: '일정', value: 18 },
  { label: '포트폴리오 점수', value: 16 }
]

const previewProfiles = [
  { name: '윤하정', score: 92 },
  { name: '김도윤', score: 85 },
  { name: '이서원', score: 86 },
  { name: '박지운', score: 78 }
]

const auditLogs = [
  { who: 'admin@slate.kr', action: '기업 승인', time: '방금' },
  { who: 'ops@slate.kr', action: '공모전 일괄 삭제 · 3건', time: '1시간 전' },
  { who: 'admin@slate.kr', action: '회원 제재 (3일) · @anon42', time: '어제' },
  { who: 'ops@slate.kr', action: '매칭 정책 v18 배포', time: '어제' }
]

const contests = [
  { title: '서울독립영화제 단편 부문', due: 'D-24', status: '접수 중' },
  { title: 'MISE-EN-SCENE 단편영화제', due: 'D-50', status: '접수 중' },
  { title: '부산국제광고제 영상 부문', due: 'D-9', status: '마감 임박' }
]

const activeTeams = [
  { title: '단편영화 <푸른 방> 제작팀', stage: '프리프로덕션' },
  { title: '뮤직비디오 <Owl>', stage: '촬영 준비' },
  { title: '다큐 <오프시즌>', stage: '촬영 중' },
  { title: '브랜드 광고 <Re:Light>', stage: '기획' }
]

const statusTone = (status) => {
  if (['활성', '승인됨', '접수 중'].includes(status)) return 'success'
  if (['검토 중', '서류 보완 요청'].includes(status)) return 'warning'
  if (status.startsWith('제재')) return 'destructive'
  if (status === '마감 임박') return 'film'
  return 'muted'
}
</script>

<template>
  <section class="lovable-admin-page">
    <header class="lovable-admin-head">
      <div>
        <span>OPERATIONS · 관리자</span>
        <h1>운영 콘솔</h1>
      </div>
      <p>로그인: <strong>admin@slate.kr</strong> · 권한 7 / 9</p>
    </header>

    <section class="lovable-admin-kpis" aria-label="운영 요약">
      <article v-for="kpi in kpis" :key="kpi.label">
        <span>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path v-if="kpi.label === '우선 처리'" d="m12 4 9 16H3L12 4Z" />
            <path v-else-if="kpi.label === '회원'" d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
            <path v-else-if="kpi.label === '기업 승인 대기'" d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-8h6v8" />
            <path v-else-if="kpi.label === '공모전'" d="M8 21h8M12 17v4M7 4h10v5a5 5 0 0 1-10 0V4Z" />
            <path v-else d="M4 4v16M4 5h13l-1 5 1 5H4" />
          </svg>
          {{ kpi.label }}
        </span>
        <strong :class="{ film: kpi.tone === 'film' }">{{ kpi.value }}</strong>
        <p>{{ kpi.sub }}</p>
      </article>
    </section>

    <div class="lovable-admin-layout">
      <aside class="lovable-admin-nav">
        <nav aria-label="운영 메뉴">
          <button
            v-for="(item, index) in adminNav"
            :key="item"
            type="button"
            :class="{ active: index === 0 }"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle v-if="index === 0" cx="12" cy="12" r="8" />
              <path v-else-if="index === 1" d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <path v-else-if="index === 2" d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 7a4 4 0 1 0 0-8 4 4 0 0 0 0 8" />
              <path v-else-if="index === 3" d="M4 4v16M4 5h13l-1 5 1 5H4" />
              <path v-else-if="index === 4" d="M3 21h18M5 21V7l7-4 7 4v14" />
              <path v-else-if="index === 5" d="M8 21h8M12 17v4M7 4h10v5a5 5 0 0 1-10 0V4Z" />
              <path v-else-if="index === 6" d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z" />
              <path v-else d="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" />
            </svg>
            {{ item }}
          </button>
        </nav>
      </aside>

      <main class="lovable-admin-main">
        <section class="lovable-admin-panel">
          <header>
            <div>
              <h2>우선 처리 업무</h2>
              <span>오늘 처리해야 하는 항목</span>
            </div>
          </header>
          <ul class="lovable-admin-priority">
            <li v-for="task in priorityTasks" :key="task.title">
              <span>{{ task.kind }}</span>
              <strong>{{ task.title }}</strong>
              <small>{{ task.time }}</small>
              <button type="button">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
                보기
              </button>
            </li>
          </ul>
        </section>

        <section class="lovable-admin-panel">
          <header>
            <div>
              <h2>회원 관리</h2>
              <span>조회 · 비활성화 · 복구 · 제재</span>
            </div>
          </header>
          <div class="lovable-admin-table">
            <table>
              <thead>
                <tr>
                  <th>핸들</th>
                  <th>이름</th>
                  <th>역할</th>
                  <th>상태</th>
                  <th>신고</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in users" :key="user.handle">
                  <td>{{ user.handle }}</td>
                  <td>{{ user.name }}</td>
                  <td>{{ user.role }}</td>
                  <td><span :class="statusTone(user.status)">{{ user.status }}</span></td>
                  <td>{{ user.reports }}</td>
                  <td><button type="button">자세히</button></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="lovable-admin-panel">
          <header>
            <div>
              <h2>기업 공모전 개설 요청</h2>
            </div>
          </header>
          <div class="lovable-admin-company-requests">
            <article v-for="request in companyRequests" :key="request.title">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-8h6v8" />
              </svg>
              <div>
                <strong>{{ request.title }}</strong>
                <span>{{ request.company }}</span>
              </div>
              <small :class="statusTone(request.status)">{{ request.status }}</small>
              <button type="button">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M20 6 9 17l-5-5" />
                </svg>
                승인
              </button>
              <button type="button">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <circle cx="12" cy="12" r="9" />
                  <path d="m15 9-6 6M9 9l6 6" />
                </svg>
                거절
              </button>
            </article>
          </div>
        </section>

        <section class="lovable-admin-panel">
          <header>
            <div>
              <h2>매칭 점수 정책</h2>
              <span>가중치 미리보기 · 배포 · 롤백</span>
            </div>
          </header>
          <div class="lovable-admin-policy">
            <section>
              <span>현재 가중치</span>
              <ul>
                <li v-for="weight in weights" :key="weight.label">
                  <div>
                    <strong>{{ weight.label }}</strong>
                    <small>{{ weight.value }}</small>
                  </div>
                  <div><span :style="{ width: `${weight.value * 2}%` }"></span></div>
                </li>
              </ul>
            </section>
            <section>
              <span>미리보기 결과 - &lt;푸른 방&gt; 촬영감독 slot</span>
              <ol>
                <li v-for="(profile, index) in previewProfiles" :key="profile.name">
                  <span>{{ index + 1 }}. {{ profile.name }}</span>
                  <strong>{{ profile.score - index * 2 }}%</strong>
                </li>
              </ol>
              <footer>
                <button type="button">배포</button>
                <button type="button">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M3 12a9 9 0 1 0 3-6.7L3 8" />
                    <path d="M3 3v5h5" />
                  </svg>
                  롤백
                </button>
              </footer>
            </section>
          </div>
        </section>
      </main>

      <aside class="lovable-admin-side">
        <section class="lovable-admin-panel">
          <header><h2>감사 로그</h2></header>
          <ul class="lovable-admin-audit">
            <li v-for="log in auditLogs" :key="`${log.time}-${log.action}`">
              <span>{{ log.time }}</span>
              <strong>{{ log.action }}</strong>
              <small>{{ log.who }}</small>
            </li>
          </ul>
        </section>

        <section class="lovable-admin-panel">
          <header><h2>활성 공모전</h2></header>
          <ul class="lovable-admin-compact-list">
            <li v-for="contest in contests" :key="contest.title">
              <span>{{ contest.due }}</span>
              <strong>{{ contest.title }}</strong>
              <small :class="statusTone(contest.status)">{{ contest.status }}</small>
            </li>
          </ul>
        </section>

        <section class="lovable-admin-panel">
          <header><h2>현재 활성 팀</h2></header>
          <ul class="lovable-admin-compact-list">
            <li v-for="team in activeTeams" :key="team.title">
              <strong>{{ team.title }}</strong>
              <small>{{ team.stage }}</small>
            </li>
          </ul>
        </section>
      </aside>
    </div>
  </section>
</template>
