import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import DiscoverView from '../views/DiscoverView.vue'
import MatchingView from '../views/MatchingView.vue'
import TeamsView from '../views/TeamsView.vue'
import TeamShowcaseView from '../views/TeamShowcaseView.vue'
import WorkspaceShowcaseView from '../views/WorkspaceShowcaseView.vue'
import LocationExploreView from '../views/LocationExploreView.vue'
import AiLocationView from '../views/AiLocationView.vue'
import BoardView from '../views/BoardView.vue'
import WorksView from '../views/WorksView.vue'
import ContestsView from '../views/ContestsView.vue'
import ContestView from '../views/ContestView.vue'
import ProfileView from '../views/ProfileView.vue'
import PublicProfileShowcaseView from '../views/PublicProfileShowcaseView.vue'
import PublicProfileView from '../views/PublicProfileView.vue'
import AdminShowcaseView from '../views/AdminShowcaseView.vue'
import AdminView from '../views/AdminView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import UserRegisterView from '../views/UserRegisterView.vue'
import CompanyRegisterView from '../views/CompanyRegisterView.vue'
import RegisterCompleteView from '../views/RegisterCompleteView.vue'
import CompanyPendingView from '../views/CompanyPendingView.vue'
import NotFoundView from '../views/NotFoundView.vue'
import DemoAccessView from '../views/DemoAccessView.vue'
import { getDemoAccessCode, getToken, isDemoAccessGateEnabled, setToken, slateApi } from '../services/api'

const appMeta = (label, icon, navOrder, extra = {}) => ({
  label,
  icon,
  layout: 'app',
  navOrder,
  ...extra
})

const authMeta = (label, extra = {}) => ({
  label,
  layout: 'auth',
  nav: false,
  guestOnly: true,
  ...extra
})

const adminMeta = (label, extra = {}) => ({
  label,
  icon: '◇',
  layout: 'admin',
  requiresAuth: true,
  requiresAdmin: true,
  nav: false,
  ...extra
})

const positiveQueryId = (value) => {
  const candidate = Array.isArray(value) ? value[0] : value
  const parsed = Number(candidate)
  return Number.isInteger(parsed) && parsed > 0 ? String(parsed) : null
}

const redirectLegacyMatchingAi = (to) => {
  const teamId = positiveQueryId(to.query.teamId)
  const slotId = positiveQueryId(to.query.slotId)

  if (to.query.mode === 'members' || (teamId && slotId)) {
    const query = { view: 'ai' }
    if (teamId) query.teamId = teamId
    if (slotId) query.slotId = slotId
    return { name: 'matching-members', query }
  }

  if (to.query.mode === 'teams') {
    return { name: 'matching-teams', query: { view: 'ai' } }
  }

  return { name: 'matching-teams' }
}

const routeSections = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: appMeta('홈', '⌂', 10)
  },
  {
    path: '/discover',
    name: 'discover',
    component: DiscoverView,
    meta: appMeta('탐색', '⌕', 20)
  },
  {
    path: '/matching',
    name: 'matching',
    redirect: { name: 'matching-teams' },
    meta: appMeta('매칭', '⌕', 21, { requiresAuth: true, nav: false })
  },
  {
    path: '/matching/members',
    name: 'matching-members',
    component: MatchingView,
    meta: appMeta('팀원 매칭', '⌕', 22, { requiresAuth: true, nav: false })
  },
  {
    path: '/matching/members/:userId',
    name: 'matching-members-detail',
    component: MatchingView,
    meta: appMeta('프로필 보기', '⌕', 23, { requiresAuth: true, nav: false })
  },
  {
    path: '/matching/teams',
    name: 'matching-teams',
    component: MatchingView,
    meta: appMeta('팀 매칭', '⌕', 24, { requiresAuth: true, nav: false })
  },
  {
    path: '/matching/teams/:teamId',
    name: 'matching-teams-detail',
    component: MatchingView,
    meta: appMeta('팀 정보 보기', '⌕', 25, { requiresAuth: true, nav: false })
  },
  {
    path: '/matching/ai',
    name: 'matching-ai',
    redirect: redirectLegacyMatchingAi,
    meta: appMeta('AI 추천', '⌕', 26, { requiresAuth: true, nav: false })
  },
  {
    path: '/teams',
    name: 'teams',
    component: TeamShowcaseView,
    meta: appMeta('팀', '♙', 30)
  },
  {
    path: '/teams/new',
    name: 'teams-new',
    component: TeamsView,
    meta: appMeta('팀 생성', '♙', 31, { requiresAuth: true, nav: false })
  },
  {
    path: '/teams/invitations',
    name: 'teams-invitations',
    component: TeamsView,
    meta: appMeta('받은 팀 초대', '♙', 31, { requiresAuth: true, nav: false })
  },
  {
    path: '/workspace',
    name: 'workspace',
    component: WorkspaceShowcaseView,
    meta: appMeta('작업공간', '▦', 31)
  },
  {
    path: '/teams/:teamId/locations',
    name: 'teams-locations',
    component: LocationExploreView,
    meta: appMeta('팀 AI 로케이션 탐색', '⌖', 39, { requiresAuth: true, nav: false })
  },
  {
    path: '/teams/:teamId',
    name: 'teams-detail',
    component: TeamShowcaseView,
    meta: appMeta('팀 정보', '♙', 32, { nav: false })
  },
  {
    path: '/teams/:teamId/edit',
    name: 'teams-edit',
    component: TeamsView,
    meta: appMeta('팀 수정', '♙', 33, { requiresAuth: true, nav: false })
  },
  {
    path: '/teams/:teamId/close',
    name: 'teams-close',
    component: TeamsView,
    meta: appMeta('팀 종료', '♙', 34, { requiresAuth: true, nav: false })
  },
  {
    path: '/teams/:teamId/members',
    name: 'teams-members',
    component: TeamsView,
    meta: appMeta('팀 멤버', '♙', 35, { requiresAuth: true, nav: false })
  },
  {
    path: '/teams/:teamId/recruitments',
    name: 'teams-recruitments',
    component: TeamsView,
    meta: appMeta('팀 모집', '♙', 36, { requiresAuth: true, nav: false })
  },
  {
    path: '/teams/:teamId/requests',
    name: 'teams-requests',
    component: TeamsView,
    meta: appMeta('지원/초대 현황', '♙', 37, { requiresAuth: true, nav: false })
  },
  {
    path: '/teams/:teamId/plans/new',
    name: 'teams-plans-new',
    component: TeamsView,
    meta: appMeta('새 팀 계획', '♙', 39, { requiresAuth: true, nav: false })
  },
  {
    path: '/teams/:teamId/plans/:planItemId/edit',
    name: 'teams-plans-edit',
    component: TeamsView,
    meta: appMeta('팀 계획 수정', '♙', 39, { requiresAuth: true, nav: false })
  },
  {
    path: '/teams/:teamId/plans',
    name: 'teams-plans',
    component: TeamsView,
    meta: appMeta('팀 계획', '♙', 38, { requiresAuth: true, nav: false })
  },
  {
    path: '/locations',
    name: 'locations',
    component: AiLocationView,
    meta: appMeta('AI 로케이션 탐색', '⌖', 39, { mobileTab: false })
  },
  {
    path: '/works',
    name: 'works',
    component: WorksView,
    meta: appMeta('작업물', '□', 40)
  },
  {
    path: '/boards',
    name: 'boards',
    component: BoardView,
    meta: appMeta('게시판', '□', 41, { nav: false })
  },
  {
    path: '/boards/new',
    name: 'boards-new',
    component: BoardView,
    meta: appMeta('게시글 등록', '□', 41, { requiresAuth: true, nav: false })
  },
  {
    path: '/boards/search',
    name: 'boards-search',
    component: BoardView,
    meta: appMeta('게시판 검색', '□', 41, { nav: false })
  },
  {
    path: '/boards/:postId',
    name: 'boards-detail',
    component: BoardView,
    meta: appMeta('게시글 상세', '□', 42, { nav: false })
  },
  {
    path: '/boards/:postId/edit',
    name: 'boards-edit',
    component: BoardView,
    meta: appMeta('게시글 수정', '□', 43, { requiresAuth: true, nav: false })
  },
  {
    path: '/profiles/:profileId',
    name: 'public-profile',
    component: PublicProfileShowcaseView,
    meta: appMeta('공개 프로필', '○', 44, { nav: false })
  },
  {
    path: '/profiles/:profileId/portfolio/:portfolioItemId',
    name: 'public-profile-portfolio-detail',
    component: PublicProfileView,
    meta: appMeta('공개 포트폴리오', '○', 44, { nav: false })
  },
  {
    path: '/contests',
    name: 'contests',
    component: ContestsView,
    meta: appMeta('공모전', '◈', 50, { mobileTab: false })
  },
  {
    path: '/contests/new-request',
    name: 'contests-new-request',
    component: ContestView,
    meta: appMeta('공모전 등록 요청', '◈', 51, { requiresAuth: true, nav: false })
  },
  {
    path: '/contests/requests',
    name: 'contests-requests',
    component: ContestView,
    meta: appMeta('공모전 요청 내역', '◈', 52, { requiresAuth: true, nav: false })
  },
  {
    path: '/contests/company',
    name: 'contests-company',
    component: ContestView,
    meta: appMeta('기업 공모전 관리', '◈', 53, { requiresAuth: true, nav: false })
  },
  {
    path: '/contests/company/new',
    name: 'contests-company-new',
    component: ContestView,
    meta: appMeta('기업 공모전 개설 요청', '◈', 54, { requiresAuth: true, nav: false })
  },
  {
    path: '/contests/company/:contestId/edit',
    name: 'contests-company-edit',
    component: ContestView,
    meta: appMeta('기업 공모전 수정', '◈', 55, { requiresAuth: true, nav: false })
  },
  {
    path: '/contests/:contestId',
    name: 'contests-detail',
    component: ContestView,
    meta: appMeta('공모전 상세', '◈', 56, { nav: false })
  },
  {
    path: '/contests/:contestId/edit-request',
    name: 'contests-edit-request',
    component: ContestView,
    meta: appMeta('공모전 수정 요청', '◈', 57, { requiresAuth: true, nav: false })
  },
  {
    path: '/contests/:contestId/prepare',
    name: 'contests-prepare',
    component: ContestView,
    meta: appMeta('공모전 제출 준비', '◈', 58, { requiresAuth: true, nav: false })
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfileView,
    meta: appMeta('내 정보', '○', 60, { requiresAuth: true })
  },
  {
    path: '/profile/edit',
    name: 'profile-edit',
    component: ProfileView,
    meta: appMeta('프로필 수정', '○', 61, { requiresAuth: true, nav: false })
  },
  {
    path: '/profile/privacy',
    name: 'profile-privacy',
    component: ProfileView,
    meta: appMeta('공개 범위 설정', '○', 62, { requiresAuth: true, nav: false })
  },
  {
    path: '/profile/account',
    name: 'profile-account',
    component: ProfileView,
    meta: appMeta('계정 관리', '○', 63, { requiresAuth: true, nav: false })
  },
  {
    path: '/profile/works',
    name: 'profile-works',
    component: ProfileView,
    meta: appMeta('내 참여 작품', '○', 64, { requiresAuth: true, nav: false })
  },
  {
    path: '/profile/recovery',
    name: 'profile-recovery',
    component: ProfileView,
    meta: appMeta('삭제/복구 안내', '○', 65, { requiresAuth: true, nav: false })
  },
  {
    path: '/profile/portfolio',
    name: 'profile-portfolio',
    component: ProfileView,
    meta: appMeta('포트폴리오 관리', '○', 66, { requiresAuth: true, nav: false })
  },
  {
    path: '/profile/portfolio/new',
    name: 'profile-portfolio-new',
    component: ProfileView,
    meta: appMeta('포트폴리오 등록', '○', 67, { requiresAuth: true, nav: false })
  },
  {
    path: '/profile/portfolio/:portfolioId',
    name: 'profile-portfolio-detail',
    component: ProfileView,
    meta: appMeta('포트폴리오 상세', '○', 68, { requiresAuth: true, nav: false })
  },
  {
    path: '/profile/portfolio/:portfolioId/edit',
    name: 'profile-portfolio-edit',
    component: ProfileView,
    meta: appMeta('포트폴리오 수정', '○', 69, { requiresAuth: true, nav: false })
  },
  {
    path: '/profile/files',
    name: 'profile-files',
    component: ProfileView,
    meta: appMeta('내 파일', '○', 70, { requiresAuth: true, nav: false })
  },
  {
    path: '/profile/youtube',
    name: 'profile-youtube',
    component: ProfileView,
    meta: appMeta('내 YouTube', '○', 71, { requiresAuth: true, nav: false })
  },
  {
    path: '/profile/public-data',
    name: 'profile-public-data',
    component: ProfileView,
    meta: appMeta('작품 검색으로 추가', '○', 71, { requiresAuth: true, nav: false })
  }
]

const adminRoutes = [
  { path: '/admin', name: 'admin', label: '관리자', nav: true, component: AdminShowcaseView },
  { path: '/admin/users', name: 'admin-users', label: '회원 관리' },
  { path: '/admin/users/:userId', name: 'admin-users-detail', label: '회원 상세' },
  { path: '/admin/users/:userId/edit', name: 'admin-users-edit', label: '회원 수정' },
  { path: '/admin/posts', name: 'admin-posts', label: '게시글 관리' },
  { path: '/admin/posts/:postId', name: 'admin-posts-detail', label: '게시글 상세' },
  { path: '/admin/teams', name: 'admin-teams', label: '팀 관리' },
  { path: '/admin/teams/:teamId', name: 'admin-teams-detail', label: '팀 상세' },
  { path: '/admin/companies', name: 'admin-companies', label: '기업 관리' },
  { path: '/admin/reports', name: 'admin-reports', label: '신고 관리' },
  { path: '/admin/files', name: 'admin-files', label: '파일 관리' },
  { path: '/admin/contests', name: 'admin-contests', label: '공모전 관리' },
  { path: '/admin/contests/manual', name: 'admin-contests-manual', label: '공모전 직접 등록' },
  { path: '/admin/contests/crawler', name: 'admin-contests-crawler', label: '외부 공모전 크롤링' },
  { path: '/admin/contests/list', name: 'admin-contests-list', label: '등록/수집 공모전 목록' },
  { path: '/admin/contests/requests', name: 'admin-contests-requests', label: '회사 개설 요청' },
  { path: '/admin/demo-access', name: 'admin-demo-access', label: '접근 코드 관리' },
  { path: '/admin/notifications', name: 'admin-notifications', label: '알림 관리' },
  { path: '/admin/ui-assets', name: 'admin-ui-assets', label: '화면 자산' },
  { path: '/admin/regions', name: 'admin-regions', label: '지역 DB 관리' },
  { path: '/admin/roles', name: 'admin-roles', label: '권한 관리' },
  { path: '/admin/logs', name: 'admin-logs', label: '로그 관리' },
  { path: '/admin/score-policies', name: 'admin-score-policies', label: '점수 정책' }
].map((route, index) => ({
  path: route.path,
  name: route.name,
  component: route.component || AdminView,
  meta: adminMeta(route.label, { nav: route.nav === true, navOrder: 900 + index })
}))

const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...routeSections,
    ...adminRoutes,
    { path: '/login', name: 'login', component: LoginView, meta: authMeta('로그인') },
    { path: '/register', name: 'register', component: RegisterView, meta: authMeta('회원가입') },
    { path: '/register/user', name: 'register-user', component: UserRegisterView, meta: authMeta('일반 회원가입') },
    { path: '/register/company', name: 'register-company', component: CompanyRegisterView, meta: authMeta('기업 회원가입') },
    { path: '/register/complete', name: 'register-complete', component: RegisterCompleteView, meta: authMeta('가입 완료', { guestOnly: false, authResult: true }) },
    { path: '/register/company/pending', name: 'register-company-pending', component: CompanyPendingView, meta: authMeta('기업 승인 대기') },
    { path: '/demo-access', name: 'demo-access', component: DemoAccessView, meta: authMeta('접근 안내', { guestOnly: false }) },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFoundView,
      meta: appMeta('페이지 없음', '!', 9999, { nav: false })
    }
  ]
})

router.beforeEach(async (to) => {
  if (isDemoAccessGateEnabled()) {
    if (to.name !== 'demo-access' && !getDemoAccessCode()) {
      return {
        name: 'demo-access',
        query: { redirect: to.fullPath }
      }
    }
  } else if (to.name === 'demo-access') {
    return { name: 'home' }
  }

  const token = getToken()

  if (to.meta.authResult && token && sessionStorage.getItem('slate.register.result') !== 'USER') {
    return { name: 'home' }
  }

  if (to.meta.guestOnly && token) {
    try {
      await slateApi.me()
      return { name: 'home' }
    } catch (error) {
      setToken(null)
    }
  }

  if (to.meta.requiresAuth && !token) {
    return {
      name: 'login',
      query: { redirect: to.fullPath }
    }
  }

  if (to.meta.requiresAdmin) {
    try {
      const me = await slateApi.me()
      if (me.accountType !== 'ADMIN') {
        return { name: 'home' }
      }
    } catch (error) {
      setToken(null)
      return {
        name: 'login',
        query: { redirect: to.fullPath }
      }
    }
  }

  return true
})

export default router
