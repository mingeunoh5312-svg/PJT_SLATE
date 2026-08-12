<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProtectedImage from '../components/media/ProtectedImage.vue'
import { defaultContestImage } from '../constants/defaultImages'
import { slateApi } from '../services/api'
import {
  loadSidebarAssets,
  readSidebarAssetFile,
  removeSidebarAsset,
  resetSidebarAssets,
  setSidebarAsset,
  sidebarAssetTargets,
  sidebarAssetValue
} from '../services/sidebarAssets'
import {
  contestOrganizerOptions,
  contestRegionOptions,
  contestTargetOptions
} from '../constants/contestFilters'

const props = defineProps({ currentUser: Object })
const route = useRoute()
const router = useRouter()
const applications = ref([])
const permissionCatalog = ref([])
const myPermissions = ref([])
const adminUsers = ref([])
const auditLogs = ref([])
const operationLogs = ref([])
const policyHistory = ref([])
const policyPreview = ref(null)
const notificationTemplates = ref([])
const notificationBatches = ref([])
const notificationPreview = ref(null)
const reports = ref([])
const workFiles = ref([])
const workFileStorage = ref(null)
const boardPosts = ref([])
const selectedBoardPost = ref(null)
const managedTeams = ref([])
const selectedManagedTeam = ref(null)
const managedUsers = ref([])
const selectedManagedUser = ref(null)
const regions = ref([])
const regionSummary = ref(null)
const moderationUsers = ref([])
const sanctions = ref([])
const contestRequests = ref([])
const managedContests = ref([])
const selectedManagedContestIds = ref([])
const contestCrawlerResult = ref(null)
const demoAccessCodes = ref([])
const latestDemoAccessCode = ref(null)
const error = ref('')
const noticeSaved = ref('')
const permissionSaved = ref('')
const policySaved = ref('')
const moderationSaved = ref('')
const fileSaved = ref('')
const boardPostSaved = ref('')
const teamSaved = ref('')
const userSaved = ref('')
const regionSaved = ref('')
const sanctionSaved = ref('')
const contestSaved = ref('')
const applicationSaved = ref('')
const demoAccessSaved = ref('')
const sidebarAssetSaved = ref('')
const sidebarAssetUploadKey = ref('')
const sidebarAssets = ref(loadSidebarAssets())
const sendingNotice = ref(false)
const previewingNotice = ref(false)
const loadingNotificationBatches = ref(false)
const savingPermissionUserId = ref(null)
const fileActionId = ref(null)
const boardPostActionId = ref(null)
const teamActionId = ref(null)
const userActionId = ref(null)
const regionActionId = ref(null)
const applicationActionId = ref(null)
const reportActionId = ref(null)
const contestStatusActionId = ref(null)
const contestDeleteActionId = ref(null)
const contestRequestActionId = ref(null)
const demoAccessActionId = ref(null)
const sanctionRevokeId = ref(null)
const loadingCompanyDocumentApplicationId = ref(null)
const downloadingCompanyDocumentId = ref(null)
const loadingLogs = ref(false)
const savingPolicy = ref(false)
const previewingPolicy = ref(false)
const rollingBackPolicyId = ref(null)
const loadingReports = ref(false)
const loadingWorkFiles = ref(false)
const loadingBoardPosts = ref(false)
const loadingBoardPostDetail = ref(false)
const loadingManagedTeams = ref(false)
const loadingManagedTeamDetail = ref(false)
const loadingManagedUsers = ref(false)
const loadingManagedUserDetail = ref(false)
const loadingRegions = ref(false)
const loadingSanctions = ref(false)
const loadingContestRequests = ref(false)
const loadingManagedContests = ref(false)
const runningContestCrawler = ref(false)
const loadingDemoAccessCodes = ref(false)
const creatingDemoAccessCode = ref(false)
const sanctioning = ref(false)
const creatingContest = ref(false)
const editingContestId = ref(null)
const contestImageFile = ref(null)
const contestImagePreview = ref('')
const contestImageInputKey = ref(0)
const pendingAdminAction = ref(null)
const policyRollbackReason = ref('')
const permissionDrafts = reactive({})
const reportDrafts = reactive({})
const fileReasonDrafts = reactive({})
const companyDocumentLists = reactive({})
const revokeDrafts = reactive({})
const demoAccessDrafts = reactive({})
const demoAccessRevokeDrafts = reactive({})
const regionDrafts = reactive({})
const companyDecisionDrafts = reactive({})
const contestRequestDecisionDrafts = reactive({})
const contestStatusReasonDrafts = reactive({})
const sidebarAssetInputKeys = reactive(Object.fromEntries(sidebarAssetTargets.map((target) => [target.key, 0])))
const contestDeleteReason = ref('관리자 선택 삭제')
const sectionErrors = reactive({
  applications: '',
  notifications: '',
  permissions: '',
  logs: '',
  policy: '',
  reports: '',
  files: '',
  boards: '',
  teams: '',
  users: '',
  regions: '',
  sanctions: '',
  contests: '',
  demoAccess: '',
  uiAssets: ''
})
const noticeForm = reactive({
  targetScope: 'ALL',
  accountType: 'USER',
  userIdsText: '',
  teamId: '',
  templateId: '',
  title: '',
  body: ''
})
const logFilters = reactive({
  actionType: '',
  targetType: '',
  actorUserId: '',
  logLevel: '',
  eventCode: ''
})
const reportFilters = reactive({
  status: 'PENDING',
  targetType: ''
})
const fileFilters = reactive({
  status: 'ALL',
  keyword: '',
  uploaderUserId: '',
  teamId: ''
})
const boardPostFilters = reactive({
  keyword: '',
  category: '',
  status: '',
  visibility: '',
  authorUserId: ''
})
const managedTeamFilters = reactive({
  keyword: '',
  status: '',
  regionId: '',
  leaderUserId: ''
})
const managedUserFilters = reactive({
  keyword: '',
  accountType: '',
  accountStatus: ''
})
const regionFilters = reactive({
  keyword: '',
  sidoName: '',
  activeYn: 'Y'
})
const regionSidoOptions = [
  '서울특별시',
  '부산광역시',
  '대구광역시',
  '인천광역시',
  '광주광역시',
  '대전광역시',
  '울산광역시',
  '세종특별자치시',
  '경기도',
  '강원특별자치도',
  '충청북도',
  '충청남도',
  '전라남도',
  '전북특별자치도',
  '경상북도',
  '경상남도',
  '제주특별자치도'
]
const userFilters = reactive({
  keyword: '',
  accountStatus: ''
})
const contestRequestFilters = reactive({
  status: 'PENDING'
})
const contestFilters = reactive({
  status: 'ALL',
  contestType: 'ALL'
})
const contestCrawlerForm = reactive({
  maxPages: 10,
  maxItems: 100,
  dryRun: true
})
const crawlerStatusFilterOptions = [
  { value: 'ALL', label: '전체 유형' },
  { value: 'INSERTED', label: '등록' },
  { value: 'UPDATED', label: '수정' },
  { value: 'SKIPPED', label: '건너뜀' },
  { value: 'DRY_RUN', label: 'Dry run' },
  { value: 'FAILED', label: '실패' }
]
const crawlerPageSizeOptions = [
  { value: 20, label: '20개씩' },
  { value: 50, label: '50개씩' },
  { value: 100, label: '100개씩' },
  { value: 'ALL', label: '전체' }
]
const crawlerResultFilters = reactive({
  status: 'ALL',
  pageSize: 50
})
const crawlerResultPage = ref(1)
const demoAccessForm = reactive({
  label: '',
  startsAt: '',
  expiresAt: futureDateTimeLocal(7),
  maxUses: ''
})
const policyDraft = reactive({
  policyId: null,
  policyName: '',
  description: '',
  version: null,
  changeReason: '',
  items: []
})
const sanctionForm = reactive({
  userId: '',
  nickname: '',
  sanctionType: 'TEMP_SUSPENDED',
  sanctionUntil: '',
  reason: ''
})
const contestForm = reactive({
  contestType: 'INTERNAL',
  title: '',
  summary: '',
  theme: '',
  prizeText: '',
  totalPrizeAmount: '',
  firstPrizeAmount: '',
  organizer: 'Slate 운영팀',
  organizerType: '',
  representativeImageUrl: '',
  submissionEmail: '',
  externalUrl: '',
  targetText: '',
  targetCodes: [],
  regionCodes: [],
  requiredRolesText: '',
  relatedGenresText: '',
  startAt: '',
  deadlineAt: ''
})
const boardPostForm = reactive({
  title: '',
  body: '',
  category: 'WORK',
  visibility: 'PUBLIC',
  status: 'PUBLISHED',
  reason: ''
})
const boardPostActionReason = ref('')
const managedTeamForm = reactive({
  name: '',
  description: '',
  status: 'RECRUITING',
  regionId: '',
  regionAnyYn: 'N',
  expectedDuration: '',
  maxMemberCount: 1,
  reason: ''
})
const managedTeamActionReason = ref('')
const managedTeamActionEndType = ref('NORMAL')
const managedTeamRestoreStatus = ref('RECRUITING')
const managedTeamRestoreSnapshotYn = ref('Y')
const managedUserForm = reactive({
  nickname: '',
  phone: '',
  accountType: 'USER',
  accountStatus: 'ACTIVE',
  reason: ''
})
const managedUserActionReason = ref('')

const adminPanelByRouteName = {
  'admin-users': 'users',
  'admin-users-detail': 'users',
  'admin-users-edit': 'users',
  'admin-posts': 'board-posts',
  'admin-posts-detail': 'board-posts',
  'admin-teams': 'teams',
  'admin-teams-detail': 'teams',
  'admin-companies': 'applications',
  'admin-reports': 'reports',
  'admin-files': 'files',
  'admin-contests': 'contests',
  'admin-contests-manual': 'contests',
  'admin-contests-crawler': 'contests',
  'admin-contests-list': 'contests',
  'admin-contests-requests': 'contests',
  'admin-demo-access': 'demo-access',
  'admin-notifications': 'notifications',
  'admin-ui-assets': 'ui-assets',
  'admin-regions': 'regions',
  'admin-roles': 'permissions',
  'admin-logs': 'logs',
  'admin-score-policies': 'policy'
}
const adminRouteByPanel = {
  users: 'admin-users',
  'board-posts': 'admin-posts',
  teams: 'admin-teams',
  applications: 'admin-companies',
  reports: 'admin-reports',
  files: 'admin-files',
  contests: 'admin-contests',
  'demo-access': 'admin-demo-access',
  notifications: 'admin-notifications',
  'ui-assets': 'admin-ui-assets',
  regions: 'admin-regions',
  permissions: 'admin-roles',
  logs: 'admin-logs',
  policy: 'admin-score-policies'
}
const adminContestSectionByRouteName = {
  'admin-contests': 'overview',
  'admin-contests-manual': 'manual',
  'admin-contests-crawler': 'crawler',
  'admin-contests-list': 'list',
  'admin-contests-requests': 'requests'
}
const adminContestRouteBySection = {
  overview: 'admin-contests',
  manual: 'admin-contests-manual',
  crawler: 'admin-contests-crawler',
  list: 'admin-contests-list',
  requests: 'admin-contests-requests'
}
const adminPanelPermissions = {
  applications: 'COMPANY_APPROVAL',
  reports: 'CONTENT_MODERATION',
  files: 'CONTENT_MODERATION',
  'board-posts': 'CONTENT_MODERATION',
  teams: 'CONTENT_MODERATION',
  users: 'USER_SANCTION',
  contests: 'CONTEST_MANAGE',
  'demo-access': 'DEMO_ACCESS_MANAGE',
  notifications: 'NOTIFICATION_SEND',
  regions: 'REGION_MANAGE',
  permissions: 'ADMIN_PERMISSION_MANAGE',
  logs: 'LOG_VIEW',
  policy: 'SCORE_POLICY'
}
const adminPanelPermissionNames = {
  COMPANY_APPROVAL: '회사 승인',
  CONTENT_MODERATION: '콘텐츠 운영',
  USER_SANCTION: '회원 제재',
  CONTEST_MANAGE: '공모전 관리',
  DEMO_ACCESS_MANAGE: '접근 코드 관리',
  NOTIFICATION_SEND: '알림 발송',
  REGION_MANAGE: '지역 DB 관리',
  ADMIN_PERMISSION_MANAGE: '권한 관리',
  LOG_VIEW: '로그 조회',
  SCORE_POLICY: '점수 정책'
}
const adminNavigationItems = [
  { name: 'admin', label: '대시보드' },
  { name: 'admin-users', label: '회원', panel: 'users' },
  { name: 'admin-posts', label: '게시글', panel: 'board-posts' },
  { name: 'admin-teams', label: '팀', panel: 'teams' },
  { name: 'admin-companies', label: '회사 승인', panel: 'applications' },
  { name: 'admin-reports', label: '신고', panel: 'reports' },
  { name: 'admin-files', label: '파일', panel: 'files' },
  { name: 'admin-contests', label: '공모전', panel: 'contests' },
  { name: 'admin-demo-access', label: '접근 코드', panel: 'demo-access' },
  { name: 'admin-notifications', label: '알림', panel: 'notifications' },
  { name: 'admin-ui-assets', label: '화면 자산', panel: 'ui-assets' },
  { name: 'admin-regions', label: '지역 DB', panel: 'regions' },
  { name: 'admin-roles', label: '권한', panel: 'permissions' },
  { name: 'admin-logs', label: '로그', panel: 'logs' },
  { name: 'admin-score-policies', label: '점수 정책', panel: 'policy' }
]
const activeAdminPanel = computed(() => adminPanelByRouteName[route.name] || '')
const isAdminDashboard = computed(() => route.name === 'admin')
const activeContestAdminSection = computed(() => adminContestSectionByRouteName[route.name] || 'overview')
const managedContestIds = computed(() => managedContests.value.map((contest) => Number(contest.contestId)).filter(Boolean))
const selectedManagedContestCount = computed(() => selectedManagedContestIds.value.length)
const allManagedContestsSelected = computed(() => managedContestIds.value.length > 0 && selectedManagedContestCount.value === managedContestIds.value.length)
const crawlerAllResultItems = computed(() => contestCrawlerResult.value?.itemResults || [])
const crawlerFilteredResultItems = computed(() => {
  const status = adminText(crawlerResultFilters.status).toUpperCase()
  if (!status || status === 'ALL') return crawlerAllResultItems.value
  return crawlerAllResultItems.value.filter((item) => adminText(item?.status).toUpperCase() === status)
})
const crawlerEffectivePageSize = computed(() => {
  if (crawlerResultFilters.pageSize === 'ALL') return Math.max(crawlerFilteredResultItems.value.length, 1)
  return Math.max(1, Number(crawlerResultFilters.pageSize) || 50)
})
const crawlerResultTotalPages = computed(() => Math.max(1, Math.ceil(crawlerFilteredResultItems.value.length / crawlerEffectivePageSize.value)))
const crawlerPagedResultItems = computed(() => {
  if (crawlerResultFilters.pageSize === 'ALL') return crawlerFilteredResultItems.value
  const start = (crawlerResultPage.value - 1) * crawlerEffectivePageSize.value
  return crawlerFilteredResultItems.value.slice(start, start + crawlerEffectivePageSize.value)
})
const crawlerResultPageNumbers = computed(() => {
  const total = crawlerResultTotalPages.value
  const current = crawlerResultPage.value
  const first = Math.max(1, Math.min(current - 2, total - 4))
  const last = Math.min(total, first + 4)
  return Array.from({ length: last - first + 1 }, (_, index) => first + index)
})
const isUserListRoute = computed(() => route.name === 'admin-users')
const isUserDetailRoute = computed(() => ['admin-users-detail', 'admin-users-edit'].includes(route.name))
const isUserEditRoute = computed(() => route.name === 'admin-users-edit')
const isPostListRoute = computed(() => route.name === 'admin-posts')
const isPostDetailRoute = computed(() => route.name === 'admin-posts-detail')
const isTeamListRoute = computed(() => route.name === 'admin-teams')
const isTeamDetailRoute = computed(() => route.name === 'admin-teams-detail')

const policyGroups = [
  { code: 'FINAL_RATIO', label: '최종 반영 비율' },
  { code: 'FIRST_FILTER', label: '1차 필터 가중치' },
  { code: 'INTERNAL', label: '내부 점수 가중치' }
]
const policyGroupSums = computed(() => Object.fromEntries(
  policyGroups.map((group) => [
    group.code,
    policyDraft.items
      .filter((item) => item.scoreGroup === group.code)
      .reduce((sum, item) => sum + Number(item.weight || 0), 0)
  ])
))
const selectedNotificationTemplate = computed(() => notificationTemplates.value.find((template) => template.templateId === Number(noticeForm.templateId)))
const visibleAdminNavigationItems = computed(() => adminNavigationItems.filter((item) => !item.panel || canAccessAdminPanel(item.panel)))
const canAccessActivePanel = computed(() => !activeAdminPanel.value || canAccessAdminPanel(activeAdminPanel.value))
const activePanelRequiredPermission = computed(() => adminPanelPermissions[activeAdminPanel.value] || '')
const activePanelPermissionName = computed(() => adminPanelPermissionNames[activePanelRequiredPermission.value] || activePanelRequiredPermission.value)
const adminStats = computed(() => [
  {
    key: 'applications',
    label: '승인 대기 회사',
    value: applications.value.filter((application) => application.status === 'PENDING').length,
    note: `전체 신청 ${applications.value.length}건`,
    tone: 'blue',
    icon: '▥',
    panel: 'applications'
  },
  {
    key: 'reports',
    label: '신고 대기',
    value: reports.value.filter((report) => report.status === 'PENDING').length,
    note: `긴급 ${reports.value.filter((report) => report.reasonCode === 'ABUSE' || report.reasonCode === 'ILLEGAL').length}건`,
    tone: 'orange',
    icon: '!',
    panel: 'reports'
  },
  {
    key: 'board-posts',
    label: '게시글 관리',
    value: boardPosts.value.length || 0,
    note: `숨김 ${boardPosts.value.filter((post) => post.status === 'BLINDED').length || 0}건`,
    tone: 'blue',
    icon: '▤',
    panel: 'board-posts'
  },
  {
    key: 'teams',
    label: '팀 관리',
    value: managedTeams.value.length || 0,
    note: `대기 지원 ${managedTeams.value.reduce((sum, team) => sum + Number(team.pendingApplicationCount || 0), 0)}건`,
    tone: 'green',
    icon: '♙',
    panel: 'teams'
  },
  {
    key: 'users',
    label: '회원 관리',
    value: managedUsers.value.length || 0,
    note: `비활성 ${managedUsers.value.filter((user) => user.deactivatedAt || ['PERM_SUSPENDED', 'WITHDRAWN'].includes(user.accountStatus)).length || 0}건`,
    tone: 'orange',
    icon: '♙',
    panel: 'users'
  },
  {
    key: 'files',
    label: '보관/삭제 대기 파일',
    value: workFileStorage.value?.summary?.heldCount || workFiles.value.filter((file) => file.status === 'HELD').length,
    note: `조회 파일 ${workFiles.value.length}건`,
    tone: 'green',
    icon: '□',
    panel: 'files'
  },
  {
    key: 'contests',
    label: '공모전 요청',
    value: contestRequests.value.filter((request) => request.status === 'PENDING').length,
    note: `관리 공모전 ${managedContests.value.length}건`,
    tone: 'blue',
    icon: '♕',
    panel: 'contests'
  },
  {
    key: 'demo-access',
    label: '접근 코드',
    value: demoAccessCodes.value.length || 0,
    note: `활성 ${demoAccessCodes.value.filter((code) => code.effectiveStatus === 'ACTIVE').length || 0}개`,
    tone: 'orange',
    icon: '#',
    panel: 'demo-access'
  },
  {
    key: 'notifications',
    label: '알림 발송 배치',
    value: notificationBatches.value.length,
    note: `템플릿 ${notificationTemplates.value.length}개`,
    tone: 'green',
    icon: '✈',
    panel: 'notifications'
  }
].filter((item) => canAccessAdminPanel(item.panel)))
const priorityAdminTasks = computed(() => [
  {
    key: 'company',
    title: '회사 승인 검토',
    bullets: [`검토 대기 ${applications.value.filter((application) => application.status === 'PENDING').length}건`, `서류 포함 ${applications.value.filter((application) => Number(application.documentCount || 0) > 0).length}건`],
    tone: 'blue',
    icon: '▥',
    panel: 'applications'
  },
  {
    key: 'report',
    title: '신고/제재 검토',
    bullets: [`검토 대기 ${reports.value.filter((report) => report.status === 'PENDING').length}건`, `긴급 신고 ${reports.value.filter((report) => report.reasonCode === 'ABUSE' || report.reasonCode === 'ILLEGAL').length}건`],
    tone: 'orange',
    icon: '!',
    panel: 'reports'
  },
  {
    key: 'board-posts',
    title: '게시글 노출 관리',
    bullets: [`조회 목록 ${boardPosts.value.length || 0}건`, `신고 포함 ${boardPosts.value.filter((post) => Number(post.reportCount || 0) > 0).length || 0}건`],
    tone: 'blue',
    icon: '▤',
    panel: 'board-posts'
  },
  {
    key: 'teams',
    title: '팀 운영 상태 점검',
    bullets: [`조회 팀 ${managedTeams.value.length || 0}개`, `대기 지원 ${managedTeams.value.reduce((sum, team) => sum + Number(team.pendingApplicationCount || 0), 0)}건`],
    tone: 'green',
    icon: '♙',
    panel: 'teams'
  },
  {
    key: 'users',
    title: '회원 상태 점검',
    bullets: [`조회 회원 ${managedUsers.value.length || 0}명`, `제재 중 ${managedUsers.value.filter((user) => user.activeSanctionId).length || 0}명`],
    tone: 'orange',
    icon: '♙',
    panel: 'users'
  },
  {
    key: 'file',
    title: '파일 관리 확인',
    bullets: [`운영 보관 ${workFiles.value.filter((file) => file.status === 'HELD').length}건`, `삭제 상태 ${workFiles.value.filter((file) => file.status === 'DELETED').length}건`],
    tone: 'green',
    icon: '□',
    panel: 'files'
  },
  {
    key: 'contest',
    title: '공모전 요청 검토',
    bullets: [`검토 대기 ${contestRequests.value.filter((request) => request.status === 'PENDING').length}건`, `운영 공모전 ${managedContests.value.length}건`],
    tone: 'blue',
    icon: '♕',
    panel: 'contests'
  },
  {
    key: 'demo-access',
    title: '접근 코드 관리',
    bullets: [`코드 ${demoAccessCodes.value.length || 0}개`, `활성 ${demoAccessCodes.value.filter((code) => code.effectiveStatus === 'ACTIVE').length || 0}개`],
    tone: 'orange',
    icon: '#',
    panel: 'demo-access'
  }
].filter((item) => canAccessAdminPanel(item.panel)).slice(0, 6))
const brandSidebarAssetTarget = computed(() => sidebarAssetTargets.find((target) => target.group === 'brand'))
const navSidebarAssetTargets = computed(() => sidebarAssetTargets.filter((target) => target.group === 'nav'))
const adminMenuItems = computed(() => [
  {
    key: 'applications',
    group: 'review',
    title: '회사 승인',
    body: '회사 계정 신청과 증빙 자료를 검토합니다.',
    meta: `대기 ${applications.value.filter((application) => application.status === 'PENDING').length}건`,
    tone: 'blue',
    icon: '▥',
    panel: 'applications'
  },
  {
    key: 'reports',
    group: 'review',
    title: '신고/제재',
    body: '신고 접수와 제재 이력을 관리합니다.',
    meta: `검토 ${reports.value.filter((report) => report.status === 'PENDING').length}건`,
    tone: 'orange',
    icon: '!',
    panel: 'reports'
  },
  {
    key: 'board-posts',
    group: 'content',
    title: '게시글 관리',
    body: '게시글 검색, 수정, 숨김, 삭제, 복구 처리를 수행합니다.',
    meta: `목록 ${boardPosts.value.length || 0}건`,
    tone: 'blue',
    icon: '▤',
    panel: 'board-posts'
  },
  {
    key: 'teams',
    group: 'content',
    title: '팀 관리',
    body: '팀 검색, 정보 수정, 숨김, 종료, 삭제, 복구 처리를 수행합니다.',
    meta: `팀 ${managedTeams.value.length || 0}개`,
    tone: 'green',
    icon: '♙',
    panel: 'teams'
  },
  {
    key: 'users',
    group: 'review',
    title: '회원 관리',
    body: '회원 검색, 정보 수정, 비활성화, 복구와 제재 연결을 관리합니다.',
    meta: `회원 ${managedUsers.value.length || 0}명`,
    tone: 'orange',
    icon: '♙',
    panel: 'users'
  },
  {
    key: 'files',
    group: 'content',
    title: '파일 관리',
    body: '업로드 파일 보관 정책과 검수 현황을 확인합니다.',
    meta: `보관 ${workFileStorage.value?.summary?.heldCount || 0}건`,
    tone: 'green',
    icon: '□',
    panel: 'files'
  },
  {
    key: 'contests',
    group: 'content',
    title: '공모전 관리',
    body: '공모전 등록 요청과 노출 상태를 관리합니다.',
    meta: `요청 ${contestRequests.value.filter((request) => request.status === 'PENDING').length}건`,
    tone: 'blue',
    icon: '♕',
    panel: 'contests'
  },
  {
    key: 'demo-access',
    group: 'system',
    title: '접근 코드 관리',
    body: '서비스 준비/점검 안내 접근 코드를 생성하고 폐기합니다.',
    meta: `코드 ${demoAccessCodes.value.length || 0}개`,
    tone: 'orange',
    icon: '#',
    panel: 'demo-access'
  },
  {
    key: 'notifications',
    group: 'system',
    title: '알림 발송',
    body: '공지·배치·대상자 발송을 설정합니다.',
    meta: `배치 ${notificationBatches.value.length || 0}건`,
    tone: 'green',
    icon: '✈',
    panel: 'notifications'
  },
  {
    key: 'ui-assets',
    group: 'system',
    title: '화면 자산',
    body: '사이드바 로고 보조 이미지와 메뉴 이미지를 설정합니다.',
    meta: `${navSidebarAssetTargets.value.filter((target) => sidebarAssetForKey(target.key)).length + (sidebarAssetForKey('brand') ? 1 : 0)}개 적용`,
    tone: 'blue',
    icon: 'UI',
    panel: 'ui-assets'
  },
  {
    key: 'regions',
    group: 'system',
    title: '지역 DB',
    body: '서비스 지역명과 거리 계산용 좌표를 관리합니다.',
    meta: `활성 ${regionSummary.value?.activeCount || regions.value.length || 0}개`,
    tone: 'green',
    icon: '⌖',
    panel: 'regions'
  },
  {
    key: 'permissions',
    group: 'system',
    title: '권한 관리',
    body: '관리자/운영자 역할과 접근 권한을 설정합니다.',
    meta: `역할 ${permissionCatalog.value.length || 6}종`,
    tone: 'blue',
    icon: '♙',
    panel: 'permissions'
  },
  {
    key: 'logs',
    group: 'system',
    title: '감사/운영 로그',
    body: '주요 운영 이력과 접근 로그를 확인합니다.',
    meta: `감사 ${auditLogs.value.length || 0}건`,
    tone: 'blue',
    icon: '▣',
    panel: 'logs',
    cta: '열기'
  },
  {
    key: 'policy',
    group: 'system',
    title: '점수 정책',
    body: '매칭 점수 정책과 가중치를 관리합니다.',
    meta: policyDraft.version ? `활성 v${policyDraft.version}` : '활성 정책 확인',
    tone: 'green',
    icon: '☆',
    panel: 'policy'
  }
].filter((item) => canAccessAdminPanel(item.panel)))
const adminMenuGroups = computed(() => [
  { key: 'review', title: '검수와 계정', items: adminMenuItems.value.filter((item) => item.group === 'review') },
  { key: 'content', title: '콘텐츠 운영', items: adminMenuItems.value.filter((item) => item.group === 'content') },
  { key: 'system', title: '시스템 관리', items: adminMenuItems.value.filter((item) => item.group === 'system') }
].filter((group) => group.items.length))
const currentAdminRouteLabel = computed(() => {
  const matched = visibleAdminNavigationItems.value.find((item) => isAdminRouteActive(item))
  return matched?.label || '관리자'
})
const adminUserLabel = computed(() => props.currentUser?.nickname || props.currentUser?.email || '관리자')
const visiblePermissionTags = computed(() => myPermissions.value.slice(0, 5))
const hiddenPermissionCount = computed(() => Math.max(myPermissions.value.length - visiblePermissionTags.value.length, 0))

async function openAdminPanel(panel) {
  const name = adminRouteByPanel[panel]
  if (name) await router.push({ name })
}

function adminActionKey(type, id, detail = '') {
  return `${type}:${id}:${detail}`
}

function requestAdminAction(type, id, detail = '') {
  pendingAdminAction.value = adminActionKey(type, id, detail)
}

function isPendingAdminAction(type, id, detail = '') {
  return pendingAdminAction.value === adminActionKey(type, id, detail)
}

function clearPendingAdminAction(type, id, detail = '') {
  if (isPendingAdminAction(type, id, detail)) {
    pendingAdminAction.value = null
  }
}

function cancelAdminAction() {
  pendingAdminAction.value = null
}

function sidebarAssetForKey(key) {
  return sidebarAssetValue(sidebarAssets.value, key)
}

function sidebarAssetLabel(key) {
  return sidebarAssetTargets.find((target) => target.key === key)?.label || key
}

function refreshSidebarAssets() {
  sidebarAssets.value = loadSidebarAssets()
}

async function uploadSidebarAsset(key, event) {
  const file = event?.target?.files?.[0]
  if (!file) return
  sidebarAssetUploadKey.value = key
  sidebarAssetSaved.value = ''
  sectionErrors.uiAssets = ''
  try {
    const dataUrl = await readSidebarAssetFile(file, key)
    sidebarAssets.value = setSidebarAsset(key, dataUrl)
    sidebarAssetSaved.value = `${sidebarAssetLabel(key)} 이미지를 저장했습니다.`
  } catch (err) {
    sectionErrors.uiAssets = err.message
  } finally {
    sidebarAssetUploadKey.value = ''
    sidebarAssetInputKeys[key] += 1
  }
}

function removeSidebarAssetForKey(key) {
  sidebarAssetSaved.value = ''
  sectionErrors.uiAssets = ''
  sidebarAssets.value = removeSidebarAsset(key)
  sidebarAssetInputKeys[key] += 1
  sidebarAssetSaved.value = `${sidebarAssetLabel(key)} 이미지를 제거했습니다.`
}

function resetAllSidebarAssets() {
  sidebarAssetSaved.value = ''
  sectionErrors.uiAssets = ''
  sidebarAssets.value = resetSidebarAssets()
  sidebarAssetTargets.forEach((target) => {
    sidebarAssetInputKeys[target.key] += 1
  })
  sidebarAssetSaved.value = '사이드바 이미지를 모두 초기화했습니다.'
}

function companyDecisionDefaultReason(decision) {
  return decision === 'APPROVED' ? '증빙 서류 확인 후 승인' : '보완 필요 사유를 입력해주세요.'
}

function ensureCompanyDecisionDraft(application, decision) {
  const id = application?.companyApplicationId
  if (!id) return null
  if (!companyDecisionDrafts[id]) companyDecisionDrafts[id] = {}
  if (companyDecisionDrafts[id][decision] === undefined) {
    companyDecisionDrafts[id][decision] = companyDecisionDefaultReason(decision)
  }
  return companyDecisionDrafts[id][decision]
}

function companyDecisionReason(application, decision) {
  ensureCompanyDecisionDraft(application, decision)
  return companyDecisionDrafts[application.companyApplicationId]?.[decision]?.trim() || ''
}

function requestCompanyDecision(application, decision) {
  applicationSaved.value = ''
  sectionErrors.applications = ''
  ensureCompanyDecisionDraft(application, decision)
  requestAdminAction('company', application?.companyApplicationId || '', decision)
}

function contestRequestDefaultReason(decision) {
  return decision === 'APPROVED' ? '요청 내용 검토 후 공모전 개설 승인' : '공모전 개설 요청 보완 필요'
}

function ensureContestRequestDecisionDraft(request, decision) {
  const id = request?.requestId
  if (!id) return null
  if (!contestRequestDecisionDrafts[id]) contestRequestDecisionDrafts[id] = {}
  if (contestRequestDecisionDrafts[id][decision] === undefined) {
    contestRequestDecisionDrafts[id][decision] = contestRequestDefaultReason(decision)
  }
  return contestRequestDecisionDrafts[id][decision]
}

function contestRequestDecisionReason(request, decision) {
  ensureContestRequestDecisionDraft(request, decision)
  return contestRequestDecisionDrafts[request.requestId]?.[decision]?.trim() || ''
}

function requestContestRequestDecision(request, decision) {
  contestSaved.value = ''
  sectionErrors.contests = ''
  ensureContestRequestDecisionDraft(request, decision)
  requestAdminAction('contest-request', request?.requestId || '', decision)
}

function contestStatusReasonKey(contest, status) {
  return `${contest?.contestId || ''}:${status || ''}`
}

function contestStatusDefaultReason(status) {
  return status === 'ENDED' ? '운영 일정 종료로 공모전 노출 종료' : '운영 검수 후 공모전 재개'
}

function ensureContestStatusReason(contest, status) {
  const key = contestStatusReasonKey(contest, status)
  if (!key || key === ':') return null
  if (contestStatusReasonDrafts[key] === undefined) {
    contestStatusReasonDrafts[key] = contestStatusDefaultReason(status)
  }
  return contestStatusReasonDrafts[key]
}

function contestStatusReason(contest, status) {
  const key = contestStatusReasonKey(contest, status)
  ensureContestStatusReason(contest, status)
  return contestStatusReasonDrafts[key]?.trim() || ''
}

function requestContestStatusChange(contest, status) {
  contestSaved.value = ''
  sectionErrors.contests = ''
  ensureContestStatusReason(contest, status)
  requestAdminAction('contest-status', contest?.contestId || '', status)
}

function pruneSelectedManagedContests() {
  const visibleIds = new Set(managedContestIds.value)
  selectedManagedContestIds.value = selectedManagedContestIds.value
    .map((id) => Number(id))
    .filter((id) => visibleIds.has(id))
}

function toggleAllManagedContests(event) {
  selectedManagedContestIds.value = event?.target?.checked ? [...managedContestIds.value] : []
}

function requestSelectedContestDelete() {
  contestSaved.value = ''
  sectionErrors.contests = ''
  pruneSelectedManagedContests()
  if (selectedManagedContestIds.value.length === 0) {
    sectionErrors.contests = '삭제할 공모전을 선택해주세요.'
    return
  }
  if (!contestDeleteReason.value.trim()) {
    contestDeleteReason.value = '관리자 선택 삭제'
  }
  requestAdminAction('contest-delete', 'selected')
}

function requestNoticeSend() {
  noticeSaved.value = ''
  sectionErrors.notifications = ''
  requestAdminAction('notice', 'send')
}

function requestSanctionCreate() {
  sanctionSaved.value = ''
  sectionErrors.sanctions = ''
  if (!sanctionForm.userId) {
    sectionErrors.sanctions = '제재할 사용자를 선택해주세요.'
    return
  }
  requestAdminAction('sanction-create', sanctionForm.userId)
}

function requestPolicyPublish() {
  policySaved.value = ''
  sectionErrors.policy = ''
  requestAdminAction('policy', 'publish')
}

function requestPolicyRollback(history) {
  const target = policyRollbackTarget(history)
  if (!target?.policyId) return
  policyRollbackReason.value = `v${target.version || '?'} 정책으로 롤백`
  requestAdminAction('policy-rollback', target.policyId)
}

async function load() {
  if (props.currentUser?.accountType !== 'ADMIN') return
  error.value = ''
  await loadMyPermissions()
  const panel = activeAdminPanel.value
  if (isAdminDashboard.value) {
    if (hasPermission('COMPANY_APPROVAL')) await loadApplications()
    if (hasPermission('CONTENT_MODERATION')) {
      await Promise.all([loadReports(), loadWorkFiles(), loadBoardPosts(), loadManagedTeams()])
    }
    if (hasPermission('USER_SANCTION')) await loadManagedUsers()
    if (hasPermission('NOTIFICATION_SEND')) await loadNotificationAdminData()
    if (hasPermission('CONTEST_MANAGE')) await Promise.all([loadContestRequests(), loadManagedContests()])
    if (hasPermission('DEMO_ACCESS_MANAGE')) await loadDemoAccessCodes()
    if (hasPermission('REGION_MANAGE')) await loadRegions()
    return
  }
  if (panel === 'applications' && hasPermission('COMPANY_APPROVAL')) await loadApplications()
  if (panel === 'reports' && hasPermission('CONTENT_MODERATION')) {
    await Promise.all([loadReports(), loadModerationUsers(), loadSanctions()])
  }
  if (panel === 'files' && hasPermission('CONTENT_MODERATION')) await loadWorkFiles()
  if (panel === 'contests' && hasPermission('CONTEST_MANAGE')) {
    await Promise.all([loadContestRequests(), loadManagedContests()])
  }
  if (panel === 'demo-access' && hasPermission('DEMO_ACCESS_MANAGE')) await loadDemoAccessCodes()
  if (panel === 'notifications' && hasPermission('NOTIFICATION_SEND')) await loadNotificationAdminData()
  if (panel === 'ui-assets') refreshSidebarAssets()
  if (panel === 'regions' && hasPermission('REGION_MANAGE')) await loadRegions()
  if (panel === 'permissions' && hasPermission('ADMIN_PERMISSION_MANAGE')) await loadAdminUsers()
  if (panel === 'logs' && hasPermission('LOG_VIEW')) await loadLogs()
  if (panel === 'policy' && hasPermission('SCORE_POLICY')) await loadScorePolicy()
  if (panel === 'users' && hasPermission('USER_SANCTION')) {
    if (isUserListRoute.value) await loadManagedUsers()
    if (isUserDetailRoute.value) await selectManagedUser({ userId: route.params.userId })
  }
  if (panel === 'board-posts' && hasPermission('CONTENT_MODERATION')) {
    if (isPostListRoute.value) await loadBoardPosts()
    if (isPostDetailRoute.value) await selectBoardPost({ postId: route.params.postId })
  }
  if (panel === 'teams' && hasPermission('CONTENT_MODERATION')) {
    if (isTeamListRoute.value) await loadManagedTeams()
    if (isTeamDetailRoute.value) await selectManagedTeam({ teamId: route.params.teamId })
  }
}

async function loadNotificationAdminData() {
  sectionErrors.notifications = ''
  loadingNotificationBatches.value = true
  try {
    const [templates, batches] = await Promise.all([
      slateApi.adminNotificationTemplates(),
      slateApi.adminNotificationBatches({ limit: 8 })
    ])
    notificationTemplates.value = templates
    notificationBatches.value = batches
  } catch (err) {
    sectionErrors.notifications = err.message
  } finally {
    loadingNotificationBatches.value = false
  }
}

async function loadMyPermissions() {
  try {
    const result = await slateApi.myAdminPermissions()
    myPermissions.value = asList(result.permissions)
    permissionCatalog.value = result.catalog || []
  } catch (err) {
    error.value = err.message
  }
}

async function loadApplications() {
  sectionErrors.applications = ''
  try {
    applications.value = await slateApi.companyApplications()
  } catch (err) {
    sectionErrors.applications = err.message
  }
}

async function loadAdminUsers() {
  sectionErrors.permissions = ''
  try {
    adminUsers.value = await slateApi.adminPermissionUsers()
    adminUsers.value.forEach((user) => {
      permissionDrafts[user.userId] = asList(user.permissions)
    })
  } catch (err) {
    sectionErrors.permissions = err.message
  }
}

async function loadLogs() {
  loadingLogs.value = true
  sectionErrors.logs = ''
  try {
    auditLogs.value = await slateApi.auditLogs({
      actionType: logFilters.actionType,
      targetType: logFilters.targetType,
      actorUserId: logFilters.actorUserId,
      limit: 30
    })
    operationLogs.value = await slateApi.operationLogs({
      logLevel: logFilters.logLevel,
      eventCode: logFilters.eventCode,
      limit: 30
    })
  } catch (err) {
    sectionErrors.logs = err.message
  } finally {
    loadingLogs.value = false
  }
}

async function loadReports() {
  loadingReports.value = true
  sectionErrors.reports = ''
  try {
    reports.value = await slateApi.contentReports({
      status: reportFilters.status,
      targetType: reportFilters.targetType,
      limit: 30
    })
    reports.value.forEach((report) => ensureReportDraft(report))
  } catch (err) {
    sectionErrors.reports = err.message
  } finally {
    loadingReports.value = false
  }
}

async function loadWorkFiles() {
  loadingWorkFiles.value = true
  sectionErrors.files = ''
  try {
    const [files, storage] = await Promise.all([
      slateApi.adminWorkFiles({
        status: fileFilters.status,
        keyword: fileFilters.keyword,
        uploaderUserId: fileFilters.uploaderUserId,
        teamId: fileFilters.teamId,
        limit: 50
      }),
      slateApi.adminWorkFileStorageSummary()
    ])
    workFiles.value = files
    workFileStorage.value = storage
    workFiles.value.forEach((file) => ensureFileReason(file))
  } catch (err) {
    sectionErrors.files = err.message
  } finally {
    loadingWorkFiles.value = false
  }
}

async function loadBoardPosts() {
  loadingBoardPosts.value = true
  sectionErrors.boards = ''
  try {
    boardPosts.value = await slateApi.adminBoardPosts({
      keyword: boardPostFilters.keyword,
      category: boardPostFilters.category,
      status: boardPostFilters.status,
      visibility: boardPostFilters.visibility,
      authorUserId: boardPostFilters.authorUserId,
      limit: 50
    })
    if (selectedBoardPost.value) {
      const stillVisible = boardPosts.value.some((post) => Number(post.postId) === Number(selectedBoardPost.value.postId))
      if (!stillVisible) selectedBoardPost.value = null
    }
  } catch (err) {
    sectionErrors.boards = err.message
  } finally {
    loadingBoardPosts.value = false
  }
}

async function loadManagedUsers() {
  loadingManagedUsers.value = true
  sectionErrors.users = ''
  try {
    managedUsers.value = await slateApi.adminUsers({
      keyword: managedUserFilters.keyword,
      accountType: managedUserFilters.accountType,
      accountStatus: managedUserFilters.accountStatus,
      limit: 50
    })
    if (selectedManagedUser.value) {
      const stillVisible = managedUsers.value.some((user) => Number(user.userId) === Number(selectedManagedUser.value.userId))
      if (!stillVisible) selectedManagedUser.value = null
    }
  } catch (err) {
    sectionErrors.users = err.message
  } finally {
    loadingManagedUsers.value = false
  }
}

async function loadRegions() {
  loadingRegions.value = true
  sectionErrors.regions = ''
  try {
    const [summary, items] = await Promise.all([
      slateApi.adminRegionSummary(),
      slateApi.adminRegions({
        keyword: regionFilters.keyword,
        sidoName: regionFilters.sidoName,
        activeYn: regionFilters.activeYn,
        limit: 500
      })
    ])
    regionSummary.value = summary
    regions.value = items
    regions.value.forEach((region) => ensureRegionDraft(region))
  } catch (err) {
    sectionErrors.regions = err.message
  } finally {
    loadingRegions.value = false
  }
}

function ensureRegionDraft(region) {
  if (!region?.regionId) return null
  if (!regionDrafts[region.regionId]) {
    regionDrafts[region.regionId] = {
      sidoName: region.sidoName || '',
      sigunguName: region.sigunguName || '',
      dongName: region.dongName || '',
      centerLat: Number(region.centerLat || 0),
      centerLng: Number(region.centerLng || 0),
      publicDisplayName: region.publicDisplayName || '',
      activeYn: region.activeYn || 'Y',
      reason: ''
    }
  }
  return regionDrafts[region.regionId]
}

async function updateRegion(region) {
  const draft = ensureRegionDraft(region)
  if (!region?.regionId || !draft) return
  regionSaved.value = ''
  sectionErrors.regions = ''
  if (!String(draft.reason || '').trim()) {
    sectionErrors.regions = '지역 정보 수정 사유를 입력해주세요.'
    return
  }
  regionActionId.value = region.regionId
  try {
    const updated = await slateApi.adminUpdateRegion(region.regionId, {
      sidoName: draft.sidoName,
      sigunguName: draft.sigunguName,
      dongName: draft.dongName,
      centerLat: Number(draft.centerLat),
      centerLng: Number(draft.centerLng),
      publicDisplayName: draft.publicDisplayName,
      activeYn: draft.activeYn,
      reason: draft.reason
    })
    regionSaved.value = `지역 #${updated.regionId} 정보를 저장했습니다.`
    delete regionDrafts[region.regionId]
    await loadRegions()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.regions = err.message
  } finally {
    regionActionId.value = null
  }
}

async function selectManagedUser(user) {
  if (!user?.userId) return
  if (isUserListRoute.value) {
    await router.push({ name: 'admin-users-detail', params: { userId: user.userId } })
    return
  }
  loadingManagedUserDetail.value = true
  sectionErrors.users = ''
  userSaved.value = ''
  try {
    selectedManagedUser.value = await slateApi.adminUser(user.userId)
    fillManagedUserForm(selectedManagedUser.value)
    managedUserActionReason.value = ''
    cancelAdminAction()
    ensureManagedUserRevokeDrafts()
  } catch (err) {
    sectionErrors.users = err.message
  } finally {
    loadingManagedUserDetail.value = false
  }
}

function fillManagedUserForm(user) {
  managedUserForm.nickname = user?.nickname || ''
  managedUserForm.phone = user?.phone || ''
  managedUserForm.accountType = user?.accountType || 'USER'
  managedUserForm.accountStatus = user?.accountStatus || 'ACTIVE'
  managedUserForm.reason = ''
}

async function saveManagedUser() {
  if (!selectedManagedUser.value?.userId) return
  userSaved.value = ''
  sectionErrors.users = ''
  if (!managedUserForm.reason.trim()) {
    sectionErrors.users = '회원 정보 수정 사유를 입력해주세요.'
    return
  }
  const userId = selectedManagedUser.value.userId
  userActionId.value = userId
  try {
    const updated = await slateApi.adminUpdateUser(userId, {
      nickname: managedUserForm.nickname,
      phone: managedUserForm.phone,
      accountType: managedUserForm.accountType,
      accountStatus: managedUserForm.accountStatus,
      reason: managedUserForm.reason
    })
    selectedManagedUser.value = updated
    fillManagedUserForm(updated)
    userSaved.value = `회원 #${updated.userId} 정보를 저장했습니다.`
    await loadManagedUsers()
    if (hasPermission('LOG_VIEW')) await loadLogs()
    await router.push({ name: 'admin-users-detail', params: { userId: updated.userId } })
  } catch (err) {
    sectionErrors.users = err.message
  } finally {
    userActionId.value = null
    clearPendingAdminAction('managed-user', userId, 'UPDATE')
  }
}

function requestManagedUserAction(action) {
  userSaved.value = ''
  sectionErrors.users = ''
  managedUserActionReason.value = ''
  requestAdminAction('managed-user', selectedManagedUser.value?.userId || '', action)
}

async function runManagedUserAction(action) {
  if (!selectedManagedUser.value?.userId) return
  const reason = managedUserActionReason.value.trim()
  if (!reason) {
    sectionErrors.users = '관리자 처리 사유를 입력해주세요.'
    return
  }
  const userId = selectedManagedUser.value.userId
  userActionId.value = userId
  userSaved.value = ''
  sectionErrors.users = ''
  try {
    const updated = action === 'RESTORE'
      ? await slateApi.adminRestoreUser(userId, { reason })
      : await slateApi.adminDeactivateUser(userId, { reason })
    selectedManagedUser.value = updated
    fillManagedUserForm(updated)
    managedUserActionReason.value = ''
    userSaved.value = action === 'RESTORE'
      ? `회원 #${userId} 계정을 복구했습니다.`
      : `회원 #${userId} 계정을 비활성화했습니다.`
    await loadManagedUsers()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.users = err.message
  } finally {
    userActionId.value = null
    clearPendingAdminAction('managed-user', userId, action)
  }
}

function pickManagedUserForSanction(user = selectedManagedUser.value) {
  if (!user) return
  pickSanctionUser(user)
  userSaved.value = `${user.nickname || user.loginId} 회원을 제재 적용 대상으로 선택했습니다.`
}

async function revokeManagedUserSanction(sanction) {
  if (!sanction || !selectedManagedUser.value) return
  await revokeSanction({
    ...sanction,
    nickname: selectedManagedUser.value.nickname,
    email: selectedManagedUser.value.email
  })
  await selectManagedUser(selectedManagedUser.value)
  await loadManagedUsers()
}

function ensureManagedUserRevokeDrafts() {
  ;(selectedManagedUser.value?.recentSanctions || []).forEach((sanction) => {
    if (!revokeDrafts[sanction.sanctionId]) revokeDrafts[sanction.sanctionId] = ''
  })
}

async function selectBoardPost(post) {
  if (!post?.postId) return
  if (isPostListRoute.value) {
    await router.push({ name: 'admin-posts-detail', params: { postId: post.postId } })
    return
  }
  loadingBoardPostDetail.value = true
  sectionErrors.boards = ''
  boardPostSaved.value = ''
  try {
    selectedBoardPost.value = await slateApi.adminBoardPost(post.postId)
    fillBoardPostForm(selectedBoardPost.value)
    boardPostActionReason.value = ''
    cancelAdminAction()
  } catch (err) {
    sectionErrors.boards = err.message
  } finally {
    loadingBoardPostDetail.value = false
  }
}

function fillBoardPostForm(post) {
  boardPostForm.title = post?.title || ''
  boardPostForm.body = post?.content || post?.body || ''
  boardPostForm.category = post?.category || 'WORK'
  boardPostForm.visibility = post?.visibility || 'PUBLIC'
  boardPostForm.status = post?.status || 'PUBLISHED'
  boardPostForm.reason = ''
}

async function saveBoardPost() {
  if (!selectedBoardPost.value?.postId) return
  boardPostSaved.value = ''
  sectionErrors.boards = ''
  if (!boardPostForm.reason.trim()) {
    sectionErrors.boards = '게시글 수정 사유를 입력해주세요.'
    return
  }
  boardPostActionId.value = selectedBoardPost.value.postId
  try {
    const updated = await slateApi.adminUpdateBoardPost(selectedBoardPost.value.postId, {
      title: boardPostForm.title,
      body: boardPostForm.body,
      category: boardPostForm.category,
      visibility: boardPostForm.visibility,
      status: boardPostForm.status,
      reason: boardPostForm.reason
    })
    selectedBoardPost.value = updated
    fillBoardPostForm(updated)
    boardPostSaved.value = `게시글 #${updated.postId} 수정 내용을 저장했습니다.`
    await loadBoardPosts()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.boards = err.message
  } finally {
    boardPostActionId.value = null
    clearPendingAdminAction('board-post', selectedBoardPost.value?.postId, 'UPDATE')
  }
}

function requestBoardPostAction(action) {
  boardPostSaved.value = ''
  sectionErrors.boards = ''
  boardPostActionReason.value = ''
  requestAdminAction('board-post', selectedBoardPost.value?.postId || '', action)
}

async function runBoardPostAction(action) {
  if (!selectedBoardPost.value?.postId) return
  const reason = boardPostActionReason.value.trim()
  if (!reason) {
    sectionErrors.boards = '관리자 처리 사유를 입력해주세요.'
    return
  }
  const postId = selectedBoardPost.value.postId
  boardPostActionId.value = postId
  boardPostSaved.value = ''
  sectionErrors.boards = ''
  try {
    let updated
    if (action === 'HIDE') {
      updated = await slateApi.adminHideBoardPost(postId, { reason })
      boardPostSaved.value = `게시글 #${postId}을 숨김 처리했습니다.`
    } else if (action === 'DELETE') {
      updated = await slateApi.adminDeleteBoardPost(postId, { reason })
      boardPostSaved.value = `게시글 #${postId}을 삭제 상태로 전환했습니다.`
    } else if (action === 'RESTORE') {
      updated = await slateApi.adminRestoreBoardPost(postId, { reason })
      boardPostSaved.value = `게시글 #${postId}을 복구했습니다.`
    }
    selectedBoardPost.value = updated
    fillBoardPostForm(updated)
    boardPostActionReason.value = ''
    await loadBoardPosts()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.boards = err.message
  } finally {
    boardPostActionId.value = null
    clearPendingAdminAction('board-post', postId, action)
  }
}

async function loadManagedTeams() {
  loadingManagedTeams.value = true
  sectionErrors.teams = ''
  try {
    managedTeams.value = await slateApi.adminTeams({
      keyword: managedTeamFilters.keyword,
      status: managedTeamFilters.status,
      regionId: managedTeamFilters.regionId,
      leaderUserId: managedTeamFilters.leaderUserId,
      limit: 50
    })
    if (selectedManagedTeam.value) {
      const stillVisible = managedTeams.value.some((team) => Number(team.teamId) === Number(selectedManagedTeam.value.teamId))
      if (!stillVisible) selectedManagedTeam.value = null
    }
  } catch (err) {
    sectionErrors.teams = err.message
  } finally {
    loadingManagedTeams.value = false
  }
}

async function selectManagedTeam(team) {
  if (!team?.teamId) return
  if (isTeamListRoute.value) {
    await router.push({ name: 'admin-teams-detail', params: { teamId: team.teamId } })
    return
  }
  loadingManagedTeamDetail.value = true
  sectionErrors.teams = ''
  teamSaved.value = ''
  try {
    selectedManagedTeam.value = await slateApi.adminTeam(team.teamId)
    fillManagedTeamForm(selectedManagedTeam.value)
    resetManagedTeamActionDrafts()
    cancelAdminAction()
  } catch (err) {
    sectionErrors.teams = err.message
  } finally {
    loadingManagedTeamDetail.value = false
  }
}

function fillManagedTeamForm(team) {
  managedTeamForm.name = team?.name || ''
  managedTeamForm.description = team?.description || ''
  managedTeamForm.status = team?.status || 'RECRUITING'
  managedTeamForm.regionId = team?.regionId || ''
  managedTeamForm.regionAnyYn = team?.regionAnyYn || 'N'
  managedTeamForm.expectedDuration = team?.expectedDuration || ''
  managedTeamForm.maxMemberCount = Number(team?.maxMemberCount || 1)
  managedTeamForm.reason = ''
  managedTeamRestoreStatus.value = managedTeamRestorableStatus(team)
}

function resetManagedTeamActionDrafts() {
  managedTeamActionReason.value = ''
  managedTeamActionEndType.value = 'NORMAL'
  managedTeamRestoreSnapshotYn.value = 'Y'
}

async function saveManagedTeam() {
  if (!selectedManagedTeam.value?.teamId) return
  teamSaved.value = ''
  sectionErrors.teams = ''
  if (!managedTeamForm.reason.trim()) {
    sectionErrors.teams = '팀 정보 수정 사유를 입력해주세요.'
    return
  }
  const teamId = selectedManagedTeam.value.teamId
  teamActionId.value = teamId
  try {
    const updated = await slateApi.adminUpdateTeam(teamId, {
      name: managedTeamForm.name,
      description: managedTeamForm.description,
      status: managedTeamForm.status,
      regionId: managedTeamForm.regionId || null,
      regionAnyYn: managedTeamForm.regionAnyYn,
      expectedDuration: managedTeamForm.expectedDuration,
      maxMemberCount: Number(managedTeamForm.maxMemberCount || 1),
      reason: managedTeamForm.reason
    })
    selectedManagedTeam.value = updated
    fillManagedTeamForm(updated)
    teamSaved.value = `팀 #${updated.teamId} 정보를 저장했습니다.`
    await loadManagedTeams()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.teams = err.message
  } finally {
    teamActionId.value = null
    clearPendingAdminAction('managed-team', teamId, 'UPDATE')
  }
}

function requestManagedTeamAction(action) {
  teamSaved.value = ''
  sectionErrors.teams = ''
  resetManagedTeamActionDrafts()
  requestAdminAction('managed-team', selectedManagedTeam.value?.teamId || '', action)
}

async function runManagedTeamAction(action) {
  if (!selectedManagedTeam.value?.teamId) return
  const reason = managedTeamActionReason.value.trim()
  if (!reason) {
    sectionErrors.teams = '관리자 처리 사유를 입력해주세요.'
    return
  }
  const teamId = selectedManagedTeam.value.teamId
  teamActionId.value = teamId
  teamSaved.value = ''
  sectionErrors.teams = ''
  try {
    let updated
    if (action === 'HIDE') {
      updated = await slateApi.adminHideTeam(teamId, { reason })
      teamSaved.value = `팀 #${teamId}을 숨김 처리했습니다.`
    } else if (action === 'CLOSE') {
      updated = await slateApi.adminCloseTeam(teamId, {
        endType: managedTeamActionEndType.value,
        reason
      })
      teamSaved.value = `팀 #${teamId}을 종료했습니다.`
    } else if (action === 'DELETE') {
      updated = await slateApi.adminDeleteTeam(teamId, { reason })
      teamSaved.value = `팀 #${teamId}을 삭제 상태로 전환했습니다.`
    } else if (action === 'RESTORE') {
      updated = await slateApi.adminRestoreTeam(teamId, {
        status: managedTeamRestoreStatus.value,
        restoreSnapshotYn: managedTeamRestoreSnapshotYn.value,
        reason
      })
      teamSaved.value = `팀 #${teamId}을 복구했습니다.`
    }
    selectedManagedTeam.value = updated
    fillManagedTeamForm(updated)
    resetManagedTeamActionDrafts()
    await loadManagedTeams()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.teams = err.message
  } finally {
    teamActionId.value = null
    clearPendingAdminAction('managed-team', teamId, action)
  }
}

async function loadModerationUsers() {
  sectionErrors.sanctions = ''
  try {
    moderationUsers.value = await slateApi.moderationUsers({
      keyword: userFilters.keyword,
      accountStatus: userFilters.accountStatus,
      limit: 30
    })
  } catch (err) {
    sectionErrors.sanctions = err.message
  }
}

async function loadSanctions() {
  loadingSanctions.value = true
  sectionErrors.sanctions = ''
  try {
    sanctions.value = await slateApi.userSanctions({ status: 'ACTIVE', limit: 30 })
    sanctions.value.forEach((sanction) => {
      if (!revokeDrafts[sanction.sanctionId]) revokeDrafts[sanction.sanctionId] = ''
    })
  } catch (err) {
    sectionErrors.sanctions = err.message
  } finally {
    loadingSanctions.value = false
  }
}

async function loadScorePolicy() {
  sectionErrors.policy = ''
  try {
    const [policy, history] = await Promise.all([
      slateApi.adminActiveScorePolicy(),
      slateApi.scorePolicyHistory(10)
    ])
    policyDraft.policyId = policy.policyId || null
    policyDraft.policyName = policy.policyName || ''
    policyDraft.description = policy.description || ''
    policyDraft.version = policy.version
    policyDraft.changeReason = ''
    policyDraft.items = (policy.items || []).map((item) => ({
      scoreGroup: item.scoreGroup,
      elementCode: item.elementCode,
      displayName: item.displayName,
      weight: Number(item.weight || 0)
    }))
    policyHistory.value = history
    policyPreview.value = null
  } catch (err) {
    sectionErrors.policy = err.message
  }
}

async function loadContestRequests() {
  loadingContestRequests.value = true
  sectionErrors.contests = ''
  try {
    contestRequests.value = await slateApi.adminContestRequests({
      status: contestRequestFilters.status,
      limit: 30
    })
  } catch (err) {
    sectionErrors.contests = err.message
  } finally {
    loadingContestRequests.value = false
  }
}

async function loadManagedContests() {
  loadingManagedContests.value = true
  sectionErrors.contests = ''
  try {
    managedContests.value = await slateApi.adminContests({
      status: contestFilters.status,
      contestType: contestFilters.contestType,
      limit: 50
    })
    pruneSelectedManagedContests()
  } catch (err) {
    sectionErrors.contests = err.message
  } finally {
    loadingManagedContests.value = false
  }
}

async function runContestKoreaCrawler() {
  runningContestCrawler.value = true
  contestSaved.value = ''
  sectionErrors.contests = ''
  try {
    const result = await slateApi.adminRunContestKoreaCrawler({
      maxPages: contestCrawlerForm.maxPages === '' ? null : Number(contestCrawlerForm.maxPages),
      maxItems: contestCrawlerForm.maxItems === '' ? null : Number(contestCrawlerForm.maxItems),
      dryRun: contestCrawlerForm.dryRun
    })
    contestCrawlerResult.value = result
    crawlerResultPage.value = 1
    contestSaved.value = `콘테스트코리아 수집 ${result.processedItems || 0}건 처리, 실패 ${result.failedCount || 0}건`
    await loadManagedContests()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.contests = err.message
  } finally {
    runningContestCrawler.value = false
  }
}

async function loadDemoAccessCodes() {
  loadingDemoAccessCodes.value = true
  sectionErrors.demoAccess = ''
  try {
    demoAccessCodes.value = await slateApi.adminDemoAccessCodes()
    demoAccessCodes.value.forEach((code) => ensureDemoAccessDraft(code))
  } catch (err) {
    sectionErrors.demoAccess = err.message
  } finally {
    loadingDemoAccessCodes.value = false
  }
}

function resetDemoAccessForm() {
  demoAccessForm.label = ''
  demoAccessForm.startsAt = ''
  demoAccessForm.expiresAt = futureDateTimeLocal(7)
  demoAccessForm.maxUses = ''
}

function ensureDemoAccessDraft(code) {
  if (!code?.codeId) return null
  if (!demoAccessDrafts[code.codeId]) {
    demoAccessDrafts[code.codeId] = {
      label: code.label || '',
      startsAt: toDateTimeLocal(code.startsAt),
      expiresAt: toDateTimeLocal(code.expiresAt),
      maxUses: code.maxUses ?? ''
    }
  }
  if (demoAccessRevokeDrafts[code.codeId] === undefined) {
    demoAccessRevokeDrafts[code.codeId] = ''
  }
  return demoAccessDrafts[code.codeId]
}

function demoAccessPayload(source) {
  return {
    label: source.label,
    startsAt: source.startsAt || null,
    expiresAt: source.expiresAt,
    maxUses: source.maxUses === '' || source.maxUses === null || source.maxUses === undefined ? null : Number(source.maxUses)
  }
}

function validateDemoAccessPayload(payload) {
  if (!payload.label?.trim()) return '표시 이름을 입력해주세요.'
  if (!payload.expiresAt) return '만료 시각을 입력해주세요.'
  if (payload.startsAt && payload.expiresAt && new Date(payload.startsAt).getTime() >= new Date(payload.expiresAt).getTime()) {
    return '시작 시각은 만료 시각보다 이전이어야 합니다.'
  }
  if (payload.maxUses !== null && (!Number.isInteger(payload.maxUses) || payload.maxUses < 1)) {
    return '최대 사용 횟수는 1 이상의 정수로 입력해주세요.'
  }
  return ''
}

async function createDemoAccessCode() {
  const payload = demoAccessPayload(demoAccessForm)
  const validation = validateDemoAccessPayload(payload)
  demoAccessSaved.value = ''
  sectionErrors.demoAccess = validation
  if (validation) return
  creatingDemoAccessCode.value = true
  try {
    const created = await slateApi.adminCreateDemoAccessCode(payload)
    latestDemoAccessCode.value = created
    demoAccessSaved.value = `접근 코드 "${created.label}"을 생성했습니다.`
    resetDemoAccessForm()
    await loadDemoAccessCodes()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.demoAccess = err.message
  } finally {
    creatingDemoAccessCode.value = false
  }
}

async function updateDemoAccessCode(code) {
  const draft = ensureDemoAccessDraft(code)
  if (!draft) return
  const payload = demoAccessPayload(draft)
  const validation = validateDemoAccessPayload(payload)
  demoAccessSaved.value = ''
  sectionErrors.demoAccess = validation
  if (validation) return
  demoAccessActionId.value = code.codeId
  try {
    await slateApi.adminUpdateDemoAccessCode(code.codeId, payload)
    demoAccessSaved.value = `접근 코드 #${code.codeId} 설정을 저장했습니다.`
    delete demoAccessDrafts[code.codeId]
    await loadDemoAccessCodes()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.demoAccess = err.message
  } finally {
    demoAccessActionId.value = null
    clearPendingAdminAction('demo-access', code.codeId, 'UPDATE')
  }
}

async function revokeDemoAccessCode(code) {
  if (!code?.codeId) return
  demoAccessActionId.value = code.codeId
  demoAccessSaved.value = ''
  sectionErrors.demoAccess = ''
  try {
    await slateApi.adminRevokeDemoAccessCode(code.codeId, {
      reason: demoAccessRevokeDrafts[code.codeId] || '관리자 폐기'
    })
    demoAccessSaved.value = `접근 코드 #${code.codeId}을 폐기했습니다.`
    await loadDemoAccessCodes()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.demoAccess = err.message
  } finally {
    demoAccessActionId.value = null
    clearPendingAdminAction('demo-access', code.codeId, 'REVOKE')
  }
}

async function copyLatestDemoAccessCode() {
  if (!latestDemoAccessCode.value?.plainCode || !navigator.clipboard) return
  await navigator.clipboard.writeText(latestDemoAccessCode.value.plainCode)
  demoAccessSaved.value = '접근 코드를 클립보드에 복사했습니다.'
}

async function decide(application, decision) {
  if (!application) return
  const reason = companyDecisionReason(application, decision)
  if (!reason) {
    sectionErrors.applications = '회사 승인 처리 사유를 입력해주세요.'
    return
  }
  applicationActionId.value = application.companyApplicationId
  applicationSaved.value = ''
  sectionErrors.applications = ''
  try {
    await slateApi.companyDecision(application.companyApplicationId, decision, reason)
    await loadApplications()
    applicationSaved.value = `${application.companyName} 신청을 ${decision === 'APPROVED' ? '승인' : '거절'}했습니다.`
    if (companyDecisionDrafts[application.companyApplicationId]) delete companyDecisionDrafts[application.companyApplicationId][decision]
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.applications = err.message
  } finally {
    applicationActionId.value = null
    clearPendingAdminAction('company', application.companyApplicationId, decision)
  }
}

async function loadCompanyDocuments(application) {
  loadingCompanyDocumentApplicationId.value = application.companyApplicationId
  sectionErrors.applications = ''
  try {
    companyDocumentLists[application.companyApplicationId] = await slateApi.companyApplicationDocuments(application.companyApplicationId)
  } catch (err) {
    sectionErrors.applications = err.message
  } finally {
    loadingCompanyDocumentApplicationId.value = null
  }
}

async function downloadCompanyDocument(document) {
  downloadingCompanyDocumentId.value = document.documentId
  sectionErrors.applications = ''
  try {
    const blob = await slateApi.downloadCompanyDocument(document.documentId)
    const url = URL.createObjectURL(blob)
    const anchor = documentCreateDownloadLink(url, document.originalName || 'company-document')
    anchor.click()
    URL.revokeObjectURL(url)
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.applications = err.message
  } finally {
    downloadingCompanyDocumentId.value = null
  }
}

function documentCreateDownloadLink(url, filename) {
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.style.display = 'none'
  document.body.appendChild(anchor)
  setTimeout(() => anchor.remove(), 0)
  return anchor
}

function ensureReportDraft(report) {
  if (reportDrafts[report.reportId]) return reportDrafts[report.reportId]
  const action = report.targetType === 'BOARD_POST' ? 'BLIND_POST' : 'BLIND_REVIEW'
  reportDrafts[report.reportId] = {
    moderationAction: action,
    note: ''
  }
  return reportDrafts[report.reportId]
}

function reportActions(report) {
  if (report.targetType === 'BOARD_POST') {
    return [
      { key: 'NONE', label: '조치 없음' },
      { key: 'BLIND_POST', label: '게시글 숨김' },
      { key: 'DELETE_POST', label: '게시글 삭제' }
    ]
  }
  return [
    { key: 'NONE', label: '조치 없음' },
    { key: 'BLIND_REVIEW', label: '리뷰 숨김' },
    { key: 'DELETE_REVIEW', label: '리뷰 삭제' }
  ]
}

async function decideContentReport(report, decision) {
  reportActionId.value = report.reportId
  moderationSaved.value = ''
  sectionErrors.reports = ''
  try {
    const draft = ensureReportDraft(report)
    await slateApi.decideContentReport(report.reportId, {
      decision,
      moderationAction: decision === 'ACCEPTED' ? draft.moderationAction : 'NONE',
      note: draft.note || (decision === 'ACCEPTED' ? '운영 정책에 따라 처리했습니다.' : '운영 정책 위반으로 보기 어렵습니다.')
    })
    moderationSaved.value = `신고 #${report.reportId} 처리 결과를 저장했습니다.`
    await loadReports()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.reports = err.message
  } finally {
    reportActionId.value = null
    clearPendingAdminAction('report', report.reportId, decision)
  }
}

function ensureFileReason(file) {
  if (fileReasonDrafts[file.fileId] === undefined) {
    fileReasonDrafts[file.fileId] = file.holdReason || ''
  }
  return fileReasonDrafts[file.fileId]
}

async function holdWorkFile(file) {
  if (!file) return
  fileActionId.value = file.fileId
  fileSaved.value = ''
  sectionErrors.files = ''
  try {
    await slateApi.adminHoldWorkFile(file.fileId, {
      reason: fileReasonDrafts[file.fileId] || '운영 정책 검토로 보관'
    })
    fileSaved.value = `파일 #${file.fileId}을 보관 처리했습니다.`
    await loadWorkFiles()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.files = err.message
  } finally {
    fileActionId.value = null
    clearPendingAdminAction('file', file.fileId, 'HOLD')
  }
}

async function restoreWorkFile(file) {
  if (!file) return
  fileActionId.value = file.fileId
  fileSaved.value = ''
  sectionErrors.files = ''
  try {
    await slateApi.adminRestoreWorkFile(file.fileId, {
      reason: fileReasonDrafts[file.fileId] || '운영자 확인 후 복구'
    })
    fileSaved.value = `파일 #${file.fileId}을 복구했습니다.`
    await loadWorkFiles()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.files = err.message
  } finally {
    fileActionId.value = null
    clearPendingAdminAction('file', file.fileId, 'RESTORE')
  }
}

async function deleteWorkFile(file) {
  if (!file) return
  fileActionId.value = file.fileId
  fileSaved.value = ''
  sectionErrors.files = ''
  try {
    await slateApi.adminDeleteWorkFile(file.fileId, {
      reason: fileReasonDrafts[file.fileId] || '관리자 삭제'
    })
    fileSaved.value = `파일 #${file.fileId}을 삭제 상태로 전환했습니다.`
    await loadWorkFiles()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.files = err.message
  } finally {
    fileActionId.value = null
    clearPendingAdminAction('file', file.fileId, 'DELETE')
  }
}

function pickSanctionUser(user) {
  sanctionForm.userId = String(user.userId)
  sanctionForm.nickname = user.nickname
  sanctionForm.sanctionType = 'TEMP_SUSPENDED'
  sanctionForm.sanctionUntil = ''
  sanctionForm.reason = ''
}

async function createSanction() {
  sanctioning.value = true
  sanctionSaved.value = ''
  sectionErrors.sanctions = ''
  try {
    if (!sanctionForm.userId) throw new Error('제재할 사용자를 선택해주세요.')
    await slateApi.createUserSanction(Number(sanctionForm.userId), {
      sanctionType: sanctionForm.sanctionType,
      sanctionUntil: sanctionForm.sanctionType === 'TEMP_SUSPENDED' ? sanctionForm.sanctionUntil : null,
      reason: sanctionForm.reason
    })
    sanctionSaved.value = `${sanctionForm.nickname || sanctionForm.userId} 계정에 제재를 적용했습니다.`
    sanctionForm.reason = ''
    sanctionForm.sanctionUntil = ''
    await loadModerationUsers()
    await loadSanctions()
    if (activeAdminPanel.value === 'users') {
      await loadManagedUsers()
      if (selectedManagedUser.value) await selectManagedUser(selectedManagedUser.value)
    }
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.sanctions = err.message
  } finally {
    sanctioning.value = false
    clearPendingAdminAction('sanction-create', sanctionForm.userId)
  }
}

async function revokeSanction(sanction) {
  sanctionRevokeId.value = sanction.sanctionId
  sanctionSaved.value = ''
  sectionErrors.sanctions = ''
  try {
    await slateApi.revokeUserSanction(sanction.sanctionId, revokeDrafts[sanction.sanctionId] || '운영자 확인 후 해제')
    sanctionSaved.value = `${sanction.nickname} 계정 제재를 해제했습니다.`
    await loadModerationUsers()
    await loadSanctions()
    if (activeAdminPanel.value === 'users') {
      await loadManagedUsers()
      if (selectedManagedUser.value) await selectManagedUser(selectedManagedUser.value)
    }
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.sanctions = err.message
  } finally {
    sanctionRevokeId.value = null
    clearPendingAdminAction('sanction-revoke', sanction.sanctionId)
  }
}

function userIds() {
  return noticeForm.userIdsText
    .split(',')
    .map((item) => Number(item.trim()))
    .filter(Boolean)
}

function applyNotificationTemplate() {
  if (!selectedNotificationTemplate.value) return
  noticeForm.title = selectedNotificationTemplate.value.titleTemplate || ''
  noticeForm.body = selectedNotificationTemplate.value.bodyTemplate || ''
}

function noticePayload() {
  const template = selectedNotificationTemplate.value
  return {
    targetScope: noticeForm.targetScope,
    accountType: noticeForm.accountType,
    userIds: noticeForm.targetScope === 'USER' ? userIds() : [],
    teamId: noticeForm.targetScope === 'TEAM' ? Number(noticeForm.teamId) : null,
    templateId: noticeForm.templateId ? Number(noticeForm.templateId) : null,
    title: noticeForm.title,
    body: noticeForm.body,
    notificationType: template?.notificationType || 'ADMIN',
    targetType: template?.targetType || 'ADMIN_NOTICE'
  }
}

async function previewNoticeRecipients() {
  previewingNotice.value = true
  sectionErrors.notifications = ''
  notificationPreview.value = null
  try {
    notificationPreview.value = await slateApi.adminNotificationRecipientPreview(noticePayload())
  } catch (err) {
    sectionErrors.notifications = err.message
  } finally {
    previewingNotice.value = false
  }
}

async function sendNotice() {
  sendingNotice.value = true
  sectionErrors.notifications = ''
  noticeSaved.value = ''
  try {
    const result = await slateApi.adminSendNotification(noticePayload())
    noticeSaved.value = `${result.sentCount}명에게 알림을 발송했습니다. 배치 #${result.batchId}, ${result.chunkCount}개 묶음`
    noticeForm.title = ''
    noticeForm.body = ''
    notificationPreview.value = null
    await loadNotificationAdminData()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.notifications = err.message
  } finally {
    sendingNotice.value = false
    clearPendingAdminAction('notice', 'send')
  }
}

async function savePermissions(user) {
  savingPermissionUserId.value = user.userId
  permissionSaved.value = ''
  sectionErrors.permissions = ''
  try {
    await slateApi.updateAdminPermissions(user.userId, permissionDrafts[user.userId] || [])
    permissionSaved.value = `${user.nickname} 권한을 저장했습니다.`
    await loadAdminUsers()
    await loadMyPermissions()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.permissions = err.message
  } finally {
    savingPermissionUserId.value = null
    clearPendingAdminAction('permission', user.userId)
  }
}

async function publishPolicy() {
  savingPolicy.value = true
  policySaved.value = ''
  sectionErrors.policy = ''
  try {
    const published = await slateApi.publishScorePolicy({
      policyName: policyDraft.policyName,
      description: policyDraft.description,
      changeReason: policyDraft.changeReason,
      items: policyItemPayload()
    })
    policySaved.value = `v${published.version} 정책을 발행했습니다.`
    await loadScorePolicy()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.policy = err.message
  } finally {
    savingPolicy.value = false
    clearPendingAdminAction('policy', 'publish')
  }
}

async function previewPolicy() {
  previewingPolicy.value = true
  policySaved.value = ''
  sectionErrors.policy = ''
  try {
    policyPreview.value = await slateApi.previewScorePolicy({
      items: policyItemPayload(),
      limit: 10
    })
  } catch (err) {
    sectionErrors.policy = err.message
  } finally {
    previewingPolicy.value = false
  }
}

async function rollbackPolicy(history) {
  const target = policyRollbackTarget(history)
  if (!target?.policyId) return
  const reason = policyRollbackReason.value.trim() || `v${target.version || '?'} 정책으로 롤백`
  rollingBackPolicyId.value = target.policyId
  policySaved.value = ''
  sectionErrors.policy = ''
  try {
    const rolledBack = await slateApi.rollbackScorePolicy(target.policyId, {
      reason: reason.trim() || `v${target.version || '?'} 정책으로 롤백`
    })
    policySaved.value = `v${rolledBack.version} 정책으로 롤백했습니다.`
    await loadScorePolicy()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.policy = err.message
  } finally {
    rollingBackPolicyId.value = null
    policyRollbackReason.value = ''
    clearPendingAdminAction('policy-rollback', target.policyId)
  }
}

function revokeContestImagePreview() {
  if (contestImagePreview.value) {
    URL.revokeObjectURL(contestImagePreview.value)
    contestImagePreview.value = ''
  }
}

function resetContestImageInput() {
  revokeContestImagePreview()
  contestImageFile.value = null
  contestImageInputKey.value += 1
}

function onContestImageChange(event) {
  const file = Array.from(event.target.files || [])[0] || null
  revokeContestImagePreview()
  contestImageFile.value = file
  contestImagePreview.value = file ? URL.createObjectURL(file) : ''
}

function adminContestImageSources() {
  return [
    contestImagePreview.value,
    editingContestId.value ? `/api/media/images/contest/${editingContestId.value}` : '',
    contestForm.representativeImageUrl,
    defaultContestImage
  ].map((value) => String(value || '').trim()).filter(Boolean)
}

function adminContestImagePreview() {
  return adminContestImageSources()[0] || defaultContestImage
}

async function createContest() {
  creatingContest.value = true
  contestSaved.value = ''
  sectionErrors.contests = ''
  try {
    const payload = {
      ...contestForm,
      totalPrizeAmount: contestForm.totalPrizeAmount === '' ? null : Number(contestForm.totalPrizeAmount),
      firstPrizeAmount: contestForm.firstPrizeAmount === '' ? null : Number(contestForm.firstPrizeAmount),
      targetCodes: [...contestForm.targetCodes],
      regionCodes: [...contestForm.regionCodes]
    }
    const savedContest = editingContestId.value
      ? await slateApi.adminUpdateContest(editingContestId.value, payload)
      : await slateApi.adminCreateContest(payload)
    if (contestImageFile.value) {
      await slateApi.uploadEntityImage('CONTEST', savedContest.contestId, contestImageFile.value)
    }
    contestSaved.value = `공모전 #${savedContest.contestId} ${editingContestId.value ? '수정' : '등록'}을 완료했습니다.`
    resetContestForm()
    await loadManagedContests()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.contests = err.message
  } finally {
    creatingContest.value = false
  }
}

function resetContestForm() {
  editingContestId.value = null
  resetContestImageInput()
  contestForm.contestType = 'INTERNAL'
  contestForm.title = ''
  contestForm.summary = ''
  contestForm.theme = ''
  contestForm.prizeText = ''
  contestForm.totalPrizeAmount = ''
  contestForm.firstPrizeAmount = ''
  contestForm.organizer = 'Slate 운영팀'
  contestForm.organizerType = ''
  contestForm.representativeImageUrl = ''
  contestForm.submissionEmail = ''
  contestForm.externalUrl = ''
  contestForm.targetText = ''
  contestForm.targetCodes = []
  contestForm.regionCodes = []
  contestForm.requiredRolesText = ''
  contestForm.relatedGenresText = ''
  contestForm.startAt = ''
  contestForm.deadlineAt = ''
}

function editContest(contest) {
  resetContestImageInput()
  editingContestId.value = contest.contestId
  contestForm.contestType = contest.contestType || 'INTERNAL'
  contestForm.title = contest.title || ''
  contestForm.summary = contest.summary || ''
  contestForm.theme = contest.theme || ''
  contestForm.prizeText = contest.prizeText || ''
  contestForm.totalPrizeAmount = contest.totalPrizeAmount ?? ''
  contestForm.firstPrizeAmount = contest.firstPrizeAmount ?? ''
  contestForm.organizer = contest.organizer || ''
  contestForm.organizerType = contest.organizerType || ''
  contestForm.representativeImageUrl = contest.representativeImageUrl || ''
  contestForm.submissionEmail = contest.submissionEmail || ''
  contestForm.externalUrl = contest.externalUrl || ''
  contestForm.targetText = contest.targetText || ''
  contestForm.targetCodes = [...(contest.targetCodes || [])]
  contestForm.regionCodes = [...(contest.regionCodes || [])]
  contestForm.requiredRolesText = contest.requiredRolesText || ''
  contestForm.relatedGenresText = contest.relatedGenresText || ''
  contestForm.startAt = toDateTimeLocal(contest.startAt)
  contestForm.deadlineAt = toDateTimeLocal(contest.deadlineAt)
}

async function updateContestStatus(contest, status) {
  const reason = contestStatusReason(contest, status)
  if (!reason) {
    sectionErrors.contests = '상태 변경 사유를 입력해주세요.'
    return
  }
  contestStatusActionId.value = contest.contestId
  contestSaved.value = ''
  sectionErrors.contests = ''
  try {
    await slateApi.adminUpdateContestStatus(contest.contestId, {
      status,
      reason
    })
    contestSaved.value = `공모전 #${contest.contestId} 상태를 변경했습니다.`
    delete contestStatusReasonDrafts[contestStatusReasonKey(contest, status)]
    await loadManagedContests()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.contests = err.message
  } finally {
    contestStatusActionId.value = null
    clearPendingAdminAction('contest-status', contest.contestId, status)
  }
}

async function deleteSelectedContests() {
  pruneSelectedManagedContests()
  const contestIds = [...selectedManagedContestIds.value]
  if (contestIds.length === 0) {
    sectionErrors.contests = '삭제할 공모전을 선택해주세요.'
    clearPendingAdminAction('contest-delete', 'selected')
    return
  }
  contestDeleteActionId.value = 'selected'
  contestSaved.value = ''
  sectionErrors.contests = ''
  try {
    const result = await slateApi.adminDeleteContests({
      contestIds,
      reason: contestDeleteReason.value.trim() || '관리자 선택 삭제'
    })
    contestSaved.value = `선택한 공모전 ${result.deletedCount || contestIds.length}건을 삭제했습니다.`
    selectedManagedContestIds.value = []
    await loadManagedContests()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.contests = err.message
  } finally {
    contestDeleteActionId.value = null
    clearPendingAdminAction('contest-delete', 'selected')
  }
}

async function decideContestRequest(request, decision) {
  const reason = contestRequestDecisionReason(request, decision)
  if (!reason) {
    sectionErrors.contests = '공모전 요청 처리 사유를 입력해주세요.'
    return
  }
  contestRequestActionId.value = request.requestId
  contestSaved.value = ''
  sectionErrors.contests = ''
  try {
    await slateApi.decideContestRequest(request.requestId, {
      decision,
      reason
    })
    contestSaved.value = `개설 요청 #${request.requestId} 처리 결과를 저장했습니다.`
    if (contestRequestDecisionDrafts[request.requestId]) delete contestRequestDecisionDrafts[request.requestId][decision]
    await loadContestRequests()
    await loadManagedContests()
    if (hasPermission('LOG_VIEW')) await loadLogs()
  } catch (err) {
    sectionErrors.contests = err.message
  } finally {
    contestRequestActionId.value = null
    clearPendingAdminAction('contest-request', request.requestId, decision)
  }
}

function hasPermission(code) {
  return myPermissions.value.includes(code)
}

function canAccessAdminPanel(panel) {
  const requiredPermission = adminPanelPermissions[panel]
  return !requiredPermission || hasPermission(requiredPermission)
}

function isAdminRouteActive(item) {
  return route.name === item.name || (item.name !== 'admin' && String(route.name).startsWith(`${item.name}-`))
}

function policyItems(groupCode) {
  return policyDraft.items.filter((item) => item.scoreGroup === groupCode)
}

function policyItemPayload() {
  return policyDraft.items.map((item) => ({
    scoreGroup: item.scoreGroup,
    elementCode: item.elementCode,
    displayName: item.displayName,
    weight: Number(item.weight)
  }))
}

function scoreDelta(value) {
  const number = Number(value || 0)
  return `${number > 0 ? '+' : ''}${number.toFixed(1)}`
}

function nextContestStatus(contest) {
  return contest?.status === 'OPEN' ? 'ENDED' : 'OPEN'
}

function fileStatusLabel(status) {
  return {
    ACTIVE: '활성',
    HELD: '운영 보관',
    DELETED: '삭제 대기'
  }[status] || status
}

function boardPostStatusLabel(status) {
  return {
    PUBLISHED: '게시 중',
    BLINDED: '숨김',
    ADMIN_DELETED: '관리자 삭제',
    AUTHOR_DELETED: '작성자 삭제'
  }[status] || status
}

function boardPostCategoryLabel(category) {
  return {
    WORK: '작업물',
    FREE: '자유'
  }[category] || category
}

function boardPostVisibilityLabel(visibility) {
  return {
    PUBLIC: '공개',
    COMPANY: '회사',
    PRIVATE: '비공개'
  }[visibility] || visibility
}

function boardPostActionLabel(action) {
  return {
    HIDE: '숨김',
    DELETE: '삭제',
    RESTORE: '복구'
  }[action] || action
}

function boardPostCanEdit(post) {
  return post && !post.deletedAt && !['ADMIN_DELETED', 'AUTHOR_DELETED'].includes(post.status)
}

function teamStatusLabel(status) {
  return {
    RECRUITING: '모집 중',
    IN_PROGRESS: '진행 중',
    RECRUITMENT_CLOSED: '모집 중단',
    CLOSING: '종료 준비',
    ENDED: '종료',
    DELETED: '삭제'
  }[status] || status
}

function teamActionLabel(action) {
  return {
    HIDE: '숨김',
    CLOSE: '종료',
    DELETE: '삭제',
    RESTORE: '복구'
  }[action] || action
}

function managedTeamCanEdit(team) {
  return team?.status !== 'ENDED' && team?.status !== 'DELETED'
}

function managedTeamCanHide(team) {
  return managedTeamCanEdit(team) && team?.status !== 'RECRUITMENT_CLOSED'
}

function managedTeamCanClose(team) {
  return managedTeamCanEdit(team)
}

function managedTeamCanDelete(team) {
  return team?.status !== 'DELETED'
}

function managedTeamCanRestore(team) {
  return ['ENDED', 'DELETED', 'RECRUITMENT_CLOSED'].includes(team?.status)
}

function managedTeamRestorableStatus(team) {
  return ['RECRUITING', 'IN_PROGRESS', 'RECRUITMENT_CLOSED', 'CLOSING'].includes(team?.status)
    ? team.status
    : 'RECRUITING'
}

function accountTypeLabel(accountType) {
  return {
    USER: '일반',
    COMPANY: '회사',
    ADMIN: '관리자'
  }[accountType] || accountType
}

function accountStatusLabel(status) {
  return {
    ACTIVE: '정상',
    PENDING_APPROVAL: '승인 대기',
    TEMP_SUSPENDED: '임시 정지',
    PERM_SUSPENDED: '영구 정지',
    WITHDRAWN: '회원 탈퇴'
  }[status] || status
}

function regionActiveLabel(activeYn) {
  return activeYn === 'N' ? '비활성' : '활성'
}

function managedUserCanEdit(user) {
  return user?.accountType !== 'ADMIN'
}

function managedUserIsInactive(user) {
  return Boolean(user?.deactivatedAt) || ['PERM_SUSPENDED', 'WITHDRAWN'].includes(user?.accountStatus)
}

function documentTypeLabel(value) {
  return {
    BUSINESS_REGISTRATION: '사업자등록증',
    COMPANY_PROFILE: '회사소개서',
    PORTFOLIO: '포트폴리오',
    OTHER: '기타'
  }[value] || value
}

function formatBytes(value) {
  const bytes = Number(value || 0)
  if (bytes >= 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024 / 1024).toFixed(1)}GB`
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)}MB`
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)}KB`
  return `${bytes}B`
}

function fileReferenceCount(file) {
  return Number(file.workReferenceCount || 0) + Number(file.requestReferenceCount || 0)
}

function asList(value) {
  if (Array.isArray(value)) return value.filter(Boolean)
  if (!value) return []
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)
      return Array.isArray(parsed) ? parsed.filter(Boolean) : []
    } catch {
      return value.split(',').map((item) => item.trim()).filter(Boolean)
    }
  }
  return []
}

function formatDate(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 19)
}

function toDateTimeLocal(value) {
  if (!value) return ''
  return String(value).replace(' ', 'T').slice(0, 16)
}

function futureDateTimeLocal(days) {
  const date = new Date()
  date.setDate(date.getDate() + Number(days || 0))
  date.setSeconds(0, 0)
  const offset = date.getTimezoneOffset()
  const localDate = new Date(date.getTime() - offset * 60 * 1000)
  return localDate.toISOString().slice(0, 16)
}

function adminText(value) {
  return value === undefined || value === null ? '' : String(value).trim()
}

function crawlerRunModeLabel(result) {
  return result?.dryRun ? 'Dry run' : '저장 실행'
}

function crawlerStatusLabel(status) {
  return {
    INSERTED: '등록',
    UPDATED: '수정',
    DRY_RUN: 'Dry run',
    FAILED: '실패',
    SKIPPED: '건너뜀'
  }[adminText(status).toUpperCase()] || adminText(status) || '확인 필요'
}

function crawlerStatusClass(status) {
  return `status-${adminText(status).toLowerCase().replaceAll('_', '-') || 'unknown'}`
}

function setCrawlerResultPage(page) {
  const nextPage = Math.min(Math.max(Number(page) || 1, 1), crawlerResultTotalPages.value)
  crawlerResultPage.value = nextPage
}

function formatCrawlerDate(value) {
  return formatDate(value) || '-'
}

function crawlerDetailAriaLabel(item) {
  return `콘테스트코리아 원문 ${adminText(item?.sourceExternalId) || '보기'}`
}

function crawlerResultMetrics(result) {
  if (!result) return []
  return [
    { label: '모드', value: crawlerRunModeLabel(result), accent: result.dryRun ? 'neutral' : 'primary' },
    { label: '페이지', value: `${result.fetchedPages ?? 0}/${result.requestedMaxPages ?? 0}` },
    { label: '발견', value: result.discoveredItems ?? 0 },
    { label: '중복 제거', value: result.deduplicatedItems ?? 0 },
    { label: '처리', value: result.processedItems ?? 0 },
    { label: '등록', value: result.insertedCount ?? 0, accent: 'success' },
    { label: '수정', value: result.updatedCount ?? 0, accent: 'primary' },
    { label: '건너뜀', value: result.skippedCount ?? 0 },
    { label: '실패', value: result.failedCount ?? 0, accent: result.failedCount ? 'danger' : 'success' },
    { label: '포스터', value: result.posterStoredCount ?? 0 },
    { label: '시작', value: formatCrawlerDate(result.startedAt) },
    { label: '종료', value: formatCrawlerDate(result.finishedAt) }
  ]
}

function contestSourceLabel(contest) {
  return adminText(contest?.sourceAttribution)
}

function contestOriginalUrl(contest) {
  return adminText(contest?.sourceUrl)
}

function hasContestOriginalUrl(contest) {
  return Boolean(contestOriginalUrl(contest))
}

function isContestKoreaPoster(contest) {
  return adminText(contest?.posterSourceType).toUpperCase() === 'CONTESTKOREA_ALLOWED'
}

function contestOriginalAriaLabel(contest) {
  return `공모전 ${contest?.contestId || ''} 원문 보기`.trim()
}

function demoAccessStatusLabel(status) {
  return {
    ACTIVE: '활성',
    SCHEDULED: '시작 전',
    EXPIRED: '만료',
    EXHAUSTED: '사용 한도 초과',
    REVOKED: '폐기'
  }[status] || status || '-'
}

function demoAccessStatusClass(status) {
  return `status-${String(status || 'unknown').toLowerCase().replaceAll('_', '-')}`
}

function formatJson(value) {
  if (!value) return ''
  if (typeof value !== 'string') return JSON.stringify(value, null, 2)
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function parseHistoryJson(value) {
  if (!value) return null
  if (typeof value !== 'string') return value
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

function policyRollbackTarget(history) {
  const before = parseHistoryJson(history?.beforeJson)
  if (before?.policyId && Number(before.policyId) !== Number(policyDraft.policyId)) {
    return before
  }
  if (history?.policyId && Number(history.policyId) !== Number(policyDraft.policyId)) {
    return history
  }
  return null
}

watch(
  [() => crawlerResultFilters.status, () => crawlerResultFilters.pageSize, () => contestCrawlerResult.value],
  () => {
    crawlerResultPage.value = 1
  }
)

watch(crawlerFilteredResultItems, () => {
  if (crawlerResultPage.value > crawlerResultTotalPages.value) {
    crawlerResultPage.value = crawlerResultTotalPages.value
  }
})

watch(
  [() => props.currentUser?.userId, () => route.fullPath],
  load,
  { immediate: true }
)

onBeforeUnmount(() => {
  revokeContestImagePreview()
})
</script>

<template>
  <section v-if="props.currentUser?.accountType !== 'ADMIN'" class="login-panel">
    <h2>관리자 권한 필요</h2>
    <p>관리자 계정으로 로그인해야 운영 화면을 사용할 수 있습니다.</p>
    <RouterLink class="primary-button inline" :to="{ name: 'login', query: { redirect: route.fullPath } }">로그인</RouterLink>
  </section>
  <section v-else class="admin-page admin-console">
    <header class="admin-console-header">
      <div>
        <span class="eyebrow">Admin console</span>
        <h2>{{ currentAdminRouteLabel }}</h2>
        <p>{{ adminUserLabel }} · 권한 {{ myPermissions.length }}개</p>
      </div>
      <RouterLink class="ghost-button" :to="{ name: 'admin-logs' }">로그</RouterLink>
    </header>

    <div class="admin-console-layout">
      <aside class="admin-console-rail" aria-label="관리자 업무 메뉴">
        <RouterLink
          :to="{ name: 'admin' }"
          class="admin-rail-link dashboard"
          :class="{ active: isAdminDashboard }"
        >
          <span>◇</span>
          <strong>대시보드</strong>
        </RouterLink>
        <section v-for="group in adminMenuGroups" :key="group.key" class="admin-rail-group">
          <h3>{{ group.title }}</h3>
          <RouterLink
            v-for="item in group.items"
            :key="item.key"
            :to="{ name: adminRouteByPanel[item.panel] }"
            class="admin-rail-link"
            :class="{ active: activeAdminPanel === item.panel }"
          >
            <span>{{ item.icon }}</span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.meta }}</small>
          </RouterLink>
        </section>
        <section class="admin-rail-permissions">
          <h3>권한</h3>
          <div class="permission-tags compact">
            <span v-for="permission in visiblePermissionTags" :key="permission" class="permission-tag">{{ permission }}</span>
            <span v-if="hiddenPermissionCount" class="permission-tag muted-tag">+{{ hiddenPermissionCount }}</span>
          </div>
        </section>
      </aside>

      <main class="admin-console-main">
        <p v-if="error" class="error-text">{{ error }}</p>

        <section v-if="isAdminDashboard" class="admin-overview-strip" aria-label="관리 요약">
          <button
            v-for="item in adminStats"
            :key="item.key"
            class="admin-stat-card"
            :class="item.tone"
            type="button"
            @click="openAdminPanel(item.panel)"
          >
            <span class="admin-card-icon">{{ item.icon }}</span>
            <span>
              <small>{{ item.label }}</small>
              <strong>{{ item.value }}</strong>
              <b>{{ item.note }}</b>
            </span>
          </button>
        </section>

        <section v-if="isAdminDashboard" class="admin-dashboard-grid">
          <section class="admin-dashboard-panel">
            <div class="admin-section-head">
              <h2>우선 처리</h2>
              <button type="button" @click="openAdminPanel('logs')">로그 보기</button>
            </div>
            <div class="admin-task-list">
              <button v-for="task in priorityAdminTasks" :key="task.key" class="admin-task-row" type="button" @click="openAdminPanel(task.panel)">
                <span class="admin-card-icon">{{ task.icon }}</span>
                <strong>{{ task.title }}</strong>
                <small>{{ task.bullets.join(' · ') }}</small>
                <i aria-hidden="true">›</i>
              </button>
            </div>
          </section>

          <section class="admin-dashboard-panel">
            <div class="admin-section-head">
              <h2>업무 메뉴</h2>
            </div>
            <div class="admin-module-groups">
              <section v-for="group in adminMenuGroups" :key="group.key" class="admin-module-group">
                <h3>{{ group.title }}</h3>
                <div class="admin-module-grid">
                  <button v-for="item in group.items" :key="item.key" class="admin-module-row" type="button" @click="openAdminPanel(item.panel)">
                    <span>{{ item.icon }}</span>
                    <strong>{{ item.title }}</strong>
                    <small>{{ item.meta }}</small>
                  </button>
                </div>
              </section>
            </div>
          </section>
        </section>

        <section v-if="activeAdminPanel" class="admin-work-panel">

    <section v-if="activeAdminPanel && !canAccessActivePanel" class="form-panel admin-permission-empty">
      <div class="form-head">
        <div>
          <span class="eyebrow">Access</span>
          <h2>권한이 필요한 관리 영역</h2>
        </div>
      </div>
      <p>{{ activePanelPermissionName }} 권한이 없어 이 관리 기능을 사용할 수 없습니다.</p>
      <RouterLink class="ghost-button inline" :to="{ name: 'admin' }">대시보드로 이동</RouterLink>
    </section>

    <section v-if="activeAdminPanel === 'ui-assets'" class="form-panel sidebar-asset-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">Interface assets</span>
          <h2>사이드바 이미지 설정</h2>
        </div>
        <button class="ghost-button" type="button" @click="resetAllSidebarAssets">전체 초기화</button>
      </div>
      <p class="muted">투명 배경 PNG 이미지를 권장합니다. 저장 즉시 좌측바와 모바일 하단 메뉴에 반영됩니다.</p>
      <p v-if="sidebarAssetSaved" class="notice-text">{{ sidebarAssetSaved }}</p>
      <p v-if="sectionErrors.uiAssets" class="error-text">{{ sectionErrors.uiAssets }}</p>

      <section v-if="brandSidebarAssetTarget" class="sidebar-asset-grid single">
        <article class="sidebar-asset-card">
          <div class="sidebar-asset-preview">
            <img v-if="sidebarAssetForKey(brandSidebarAssetTarget.key)" :src="sidebarAssetForKey(brandSidebarAssetTarget.key)" alt="">
            <span v-else>SLATE</span>
          </div>
          <div class="sidebar-asset-body">
            <strong>{{ brandSidebarAssetTarget.label }}</strong>
            <p>{{ brandSidebarAssetTarget.description }}</p>
            <div class="sidebar-asset-actions">
              <label class="ghost-button">
                업로드
                <input
                  :key="`${brandSidebarAssetTarget.key}-${sidebarAssetInputKeys[brandSidebarAssetTarget.key]}`"
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  @change="uploadSidebarAsset(brandSidebarAssetTarget.key, $event)"
                >
              </label>
              <button
                class="ghost-button"
                type="button"
                :disabled="!sidebarAssetForKey(brandSidebarAssetTarget.key) || sidebarAssetUploadKey === brandSidebarAssetTarget.key"
                @click="removeSidebarAssetForKey(brandSidebarAssetTarget.key)"
              >
                삭제
              </button>
            </div>
          </div>
        </article>
      </section>

      <section class="sidebar-asset-grid">
        <article v-for="target in navSidebarAssetTargets" :key="target.key" class="sidebar-asset-card">
          <div class="sidebar-asset-preview small">
            <img v-if="sidebarAssetForKey(target.key)" :src="sidebarAssetForKey(target.key)" alt="">
            <span v-else>{{ target.label.slice(0, 2) }}</span>
          </div>
          <div class="sidebar-asset-body">
            <strong>{{ target.label }}</strong>
            <p>{{ target.description }}</p>
            <div class="sidebar-asset-actions">
              <label class="ghost-button">
                업로드
                <input
                  :key="`${target.key}-${sidebarAssetInputKeys[target.key]}`"
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  @change="uploadSidebarAsset(target.key, $event)"
                >
              </label>
              <button
                class="ghost-button"
                type="button"
                :disabled="!sidebarAssetForKey(target.key) || sidebarAssetUploadKey === target.key"
                @click="removeSidebarAssetForKey(target.key)"
              >
                삭제
              </button>
            </div>
          </div>
        </article>
      </section>
    </section>

    <section v-if="activeAdminPanel === 'regions' && hasPermission('REGION_MANAGE')" class="form-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">Regions</span>
          <h2>지역 DB 관리</h2>
        </div>
        <button class="ghost-button" type="button" :disabled="loadingRegions" @click="loadRegions">
          {{ loadingRegions ? '조회 중' : '새로고침' }}
        </button>
      </div>
      <p v-if="regionSaved" class="notice-text">{{ regionSaved }}</p>
      <p v-if="sectionErrors.regions" class="error-text">{{ sectionErrors.regions }}</p>
      <div v-if="regionSummary" class="metric-strip">
        <div class="metric">
          <span>활성 지역</span>
          <strong>{{ regionSummary.activeCount || 0 }}</strong>
        </div>
        <div class="metric">
          <span>시도</span>
          <strong>{{ regionSummary.sidoCount || 0 }}</strong>
        </div>
        <div class="metric">
          <span>좌표 범위</span>
          <strong>{{ regionSummary.minLat }}-{{ regionSummary.maxLat }}</strong>
        </div>
      </div>
      <div class="form-grid">
        <label class="field">
          <span>검색어</span>
          <input v-model="regionFilters.keyword" placeholder="지역명 또는 코드">
        </label>
        <label class="field">
          <span>시도</span>
          <select v-model="regionFilters.sidoName" @change="loadRegions">
            <option value="">전체</option>
            <option v-for="sido in regionSidoOptions" :key="sido" :value="sido">{{ sido }}</option>
          </select>
        </label>
        <label class="field">
          <span>상태</span>
          <select v-model="regionFilters.activeYn" @change="loadRegions">
            <option value="Y">활성</option>
            <option value="N">비활성</option>
            <option value="ALL">전체</option>
          </select>
        </label>
        <button class="ghost-button field-button" type="button" :disabled="loadingRegions" @click="loadRegions">
          검색
        </button>
      </div>
      <div class="policy-table-wrap">
        <table class="policy-table admin-region-table">
          <thead>
            <tr>
              <th>코드</th>
              <th>표시명</th>
              <th>시도</th>
              <th>시군구</th>
              <th>위도</th>
              <th>경도</th>
              <th>상태</th>
              <th>참조</th>
              <th>사유</th>
              <th>저장</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="region in regions" :key="region.regionId">
              <td>
                <strong>{{ region.regionCode }}</strong>
                <small>#{{ region.regionId }}</small>
              </td>
              <td>
                <input v-if="regionDrafts[region.regionId]" v-model="regionDrafts[region.regionId].publicDisplayName" maxlength="150">
              </td>
              <td>
                <input v-if="regionDrafts[region.regionId]" v-model="regionDrafts[region.regionId].sidoName" maxlength="50">
              </td>
              <td>
                <input v-if="regionDrafts[region.regionId]" v-model="regionDrafts[region.regionId].sigunguName" maxlength="80">
              </td>
              <td>
                <input v-if="regionDrafts[region.regionId]" v-model.number="regionDrafts[region.regionId].centerLat" step="0.0000001" type="number">
              </td>
              <td>
                <input v-if="regionDrafts[region.regionId]" v-model.number="regionDrafts[region.regionId].centerLng" step="0.0000001" type="number">
              </td>
              <td>
                <select v-if="regionDrafts[region.regionId]" v-model="regionDrafts[region.regionId].activeYn">
                  <option value="Y">활성</option>
                  <option value="N">비활성</option>
                </select>
                <small>{{ regionActiveLabel(region.activeYn) }}</small>
              </td>
              <td>
                <small>프로필 {{ region.profileCount || 0 }}</small>
                <small>팀 {{ region.teamCount || 0 }}</small>
              </td>
              <td>
                <input v-if="regionDrafts[region.regionId]" v-model="regionDrafts[region.regionId].reason" maxlength="500" placeholder="좌표 보정 등">
              </td>
              <td>
                <button class="primary-button" type="button" :disabled="regionActionId === region.regionId" @click="updateRegion(region)">
                  {{ regionActionId === region.regionId ? '저장 중' : '저장' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="!loadingRegions && regions.length === 0" class="muted">조건에 맞는 지역이 없습니다.</p>
    </section>

    <section v-if="activeAdminPanel === 'teams' && hasPermission('CONTENT_MODERATION')" class="form-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">Teams</span>
          <h2>{{ isTeamDetailRoute ? '팀 상세 및 관리' : '팀 관리' }}</h2>
        </div>
        <RouterLink v-if="isTeamDetailRoute" class="ghost-button" :to="{ name: 'admin-teams' }">목록</RouterLink>
        <button v-else class="ghost-button" type="button" :disabled="loadingManagedTeams" @click="loadManagedTeams">
          {{ loadingManagedTeams ? '조회 중' : '목록 새로고침' }}
        </button>
      </div>
      <p v-if="teamSaved" class="notice-text">{{ teamSaved }}</p>
      <p v-if="sectionErrors.teams" class="error-text">{{ sectionErrors.teams }}</p>
      <div v-if="isTeamListRoute" class="form-grid">
        <label class="field">
          <span>검색어</span>
          <input v-model="managedTeamFilters.keyword" placeholder="팀명, 설명, 팀장 닉네임">
        </label>
        <label class="field">
          <span>팀 상태</span>
          <select v-model="managedTeamFilters.status" @change="loadManagedTeams">
            <option value="">전체</option>
            <option value="RECRUITING">모집 중</option>
            <option value="IN_PROGRESS">진행 중</option>
            <option value="RECRUITMENT_CLOSED">모집 중단</option>
            <option value="CLOSING">종료 준비</option>
            <option value="ENDED">종료</option>
            <option value="DELETED">삭제</option>
          </select>
        </label>
        <label class="field">
          <span>지역 ID</span>
          <input v-model="managedTeamFilters.regionId" min="1" type="number">
        </label>
        <label class="field">
          <span>팀장 userId</span>
          <input v-model="managedTeamFilters.leaderUserId" min="1" type="number">
        </label>
        <button class="ghost-button field-button" type="button" :disabled="loadingManagedTeams" @click="loadManagedTeams">
          검색
        </button>
      </div>
      <div class="admin-columns" :class="{ 'admin-detail-only': isTeamDetailRoute, 'admin-list-only': isTeamListRoute }">
        <div v-if="isTeamListRoute" class="log-list">
          <h3>팀 목록</h3>
          <article
            v-for="team in managedTeams"
            :key="team.teamId"
            class="log-row admin-board-row"
            :class="{ selected: selectedManagedTeam?.teamId === team.teamId }"
            role="button"
            tabindex="0"
            @click="selectManagedTeam(team)"
            @keydown.enter.prevent="selectManagedTeam(team)"
          >
            <div class="row-head">
              <div>
                <strong>#{{ team.teamId }} · {{ team.name }}</strong>
                <div class="subline">
                  <span>{{ teamStatusLabel(team.status) }}</span>
                  <span>{{ team.leaderNickname || `팀장 #${team.leaderUserId}` }}</span>
                  <span>{{ team.regionAnyYn === 'Y' ? '전국/무관' : team.regionDisplayName || team.publicRegionName || `지역 #${team.regionId || '-'}` }}</span>
                </div>
              </div>
            </div>
            <div class="subline">
              <span>{{ team.expectedDuration || '기간 미정' }}</span>
              <span>인원 {{ team.currentMemberCount || 0 }}/{{ team.maxMemberCount || 0 }}</span>
              <span>모집 {{ team.activeRecruitmentCount || 0 }}</span>
              <span>대기 지원 {{ team.pendingApplicationCount || 0 }}</span>
            </div>
            <div class="subline">
              <span>생성 {{ formatDate(team.createdAt) }}</span>
              <span v-if="team.updatedAt">수정 {{ formatDate(team.updatedAt) }}</span>
              <span v-if="team.endedAt">종료 {{ formatDate(team.endedAt) }}</span>
              <span v-if="team.deletedAt">삭제 {{ formatDate(team.deletedAt) }}</span>
            </div>
          </article>
          <p v-if="loadingManagedTeams" class="muted">팀을 조회하고 있습니다.</p>
          <p v-if="!loadingManagedTeams && managedTeams.length === 0" class="muted">조건에 맞는 팀이 없습니다.</p>
        </div>
        <form v-if="isTeamDetailRoute" class="log-list" @submit.prevent="saveManagedTeam">
          <div class="row-head">
            <h3>상세/수정</h3>
            <span v-if="loadingManagedTeamDetail" class="muted">상세 조회 중</span>
          </div>
          <template v-if="selectedManagedTeam">
            <div class="log-row">
              <strong>#{{ selectedManagedTeam.teamId }} · {{ selectedManagedTeam.name }}</strong>
              <div class="subline">
                <span>{{ teamStatusLabel(selectedManagedTeam.status) }}</span>
                <span>{{ selectedManagedTeam.leaderNickname || `팀장 #${selectedManagedTeam.leaderUserId}` }}</span>
                <span>인원 {{ selectedManagedTeam.currentMemberCount || 0 }}/{{ selectedManagedTeam.maxMemberCount || 0 }}</span>
              </div>
              <div class="subline">
                <span>{{ selectedManagedTeam.regionAnyYn === 'Y' ? '전국/무관' : selectedManagedTeam.regionDisplayName || selectedManagedTeam.publicRegionName || `지역 #${selectedManagedTeam.regionId || '-'}` }}</span>
                <span>모집 {{ selectedManagedTeam.activeRecruitmentCount || 0 }}</span>
                <span>대기 지원 {{ selectedManagedTeam.pendingApplicationCount || 0 }}</span>
              </div>
            </div>
            <div class="form-grid">
              <label class="field">
                <span>팀명</span>
                <input v-model="managedTeamForm.name" maxlength="100" required :readonly="!managedTeamCanEdit(selectedManagedTeam)">
              </label>
              <label class="field">
                <span>상태</span>
                <select v-model="managedTeamForm.status" :disabled="!managedTeamCanEdit(selectedManagedTeam)">
                  <option value="RECRUITING">모집 중</option>
                  <option value="IN_PROGRESS">진행 중</option>
                  <option value="RECRUITMENT_CLOSED">모집 중단</option>
                  <option value="CLOSING">종료 준비</option>
                </select>
              </label>
              <label class="field">
                <span>지역 무관</span>
                <select v-model="managedTeamForm.regionAnyYn" :disabled="!managedTeamCanEdit(selectedManagedTeam)">
                  <option value="N">지역 지정</option>
                  <option value="Y">전국/무관</option>
                </select>
              </label>
              <label class="field">
                <span>지역 ID</span>
                <input v-model="managedTeamForm.regionId" min="1" type="number" :readonly="!managedTeamCanEdit(selectedManagedTeam) || managedTeamForm.regionAnyYn === 'Y'">
              </label>
              <label class="field">
                <span>예상 기간</span>
                <input v-model="managedTeamForm.expectedDuration" maxlength="50" required :readonly="!managedTeamCanEdit(selectedManagedTeam)">
              </label>
              <label class="field">
                <span>최대 인원</span>
                <input v-model="managedTeamForm.maxMemberCount" min="1" type="number" required :readonly="!managedTeamCanEdit(selectedManagedTeam)">
              </label>
              <label class="field wide">
                <span>팀 설명</span>
                <textarea v-model="managedTeamForm.description" rows="5" maxlength="2000" required :readonly="!managedTeamCanEdit(selectedManagedTeam)"></textarea>
              </label>
              <label class="field wide">
                <span>수정 사유</span>
                <input v-model="managedTeamForm.reason" maxlength="1000" required placeholder="관리자 처리 사유" :readonly="!managedTeamCanEdit(selectedManagedTeam)">
              </label>
            </div>
            <div class="row-actions">
              <button
                class="primary-button"
                type="submit"
                :disabled="!managedTeamCanEdit(selectedManagedTeam) || teamActionId === selectedManagedTeam.teamId"
              >
                {{ teamActionId === selectedManagedTeam.teamId ? '처리 중' : '수정 저장' }}
              </button>
              <button
                v-if="managedTeamCanHide(selectedManagedTeam)"
                class="ghost-button"
                type="button"
                :disabled="teamActionId === selectedManagedTeam.teamId"
                @click="requestManagedTeamAction('HIDE')"
              >
                숨김
              </button>
              <button
                v-if="managedTeamCanClose(selectedManagedTeam)"
                class="ghost-button"
                type="button"
                :disabled="teamActionId === selectedManagedTeam.teamId"
                @click="requestManagedTeamAction('CLOSE')"
              >
                종료
              </button>
              <button
                v-if="managedTeamCanDelete(selectedManagedTeam)"
                class="ghost-button danger"
                type="button"
                :disabled="teamActionId === selectedManagedTeam.teamId"
                @click="requestManagedTeamAction('DELETE')"
              >
                삭제
              </button>
              <button
                v-if="managedTeamCanRestore(selectedManagedTeam)"
                class="ghost-button"
                type="button"
                :disabled="teamActionId === selectedManagedTeam.teamId"
                @click="requestManagedTeamAction('RESTORE')"
              >
                복구
              </button>
            </div>
            <div
              v-for="action in ['HIDE', 'CLOSE', 'DELETE', 'RESTORE']"
              :key="action"
              v-show="isPendingAdminAction('managed-team', selectedManagedTeam.teamId, action)"
              class="confirm-inline admin-confirm"
              :class="{ 'danger-confirm': ['HIDE', 'CLOSE', 'DELETE'].includes(action), 'success-confirm': action === 'RESTORE' }"
            >
              <span>팀 #{{ selectedManagedTeam.teamId }}을 {{ teamActionLabel(action) }} 처리합니다.</span>
              <label v-if="action === 'CLOSE'" class="field">
                <span>종료 유형</span>
                <select v-model="managedTeamActionEndType">
                  <option value="NORMAL">정상 종료</option>
                  <option value="DISSOLUTION">해산</option>
                </select>
              </label>
              <label v-if="action === 'RESTORE'" class="field">
                <span>복구 상태</span>
                <select v-model="managedTeamRestoreStatus">
                  <option value="RECRUITING">모집 중</option>
                  <option value="IN_PROGRESS">진행 중</option>
                  <option value="RECRUITMENT_CLOSED">모집 중단</option>
                  <option value="CLOSING">종료 준비</option>
                </select>
              </label>
              <label v-if="action === 'RESTORE'" class="field">
                <span>스냅샷 복구</span>
                <select v-model="managedTeamRestoreSnapshotYn">
                  <option value="Y">사용</option>
                  <option value="N">사용 안 함</option>
                </select>
              </label>
              <label class="field">
                <span>처리 사유</span>
                <input v-model="managedTeamActionReason" maxlength="1000" placeholder="관리자 처리 사유">
              </label>
              <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
              <button class="primary-button" type="button" :disabled="teamActionId === selectedManagedTeam.teamId" @click="runManagedTeamAction(action)">
                {{ teamActionLabel(action) }} 확정
              </button>
            </div>
            <section class="log-list">
              <div class="row-head">
                <h3>운영 요약</h3>
              </div>
              <article class="log-row">
                <strong>팀원 {{ (selectedManagedTeam.members || []).length }}명</strong>
                <div class="subline">
                  <span v-for="member in (selectedManagedTeam.members || []).slice(0, 6)" :key="member.teamMemberId || member.userId">
                    {{ member.nickname || `#${member.userId}` }} · {{ member.teamRole }} · {{ member.status }}
                  </span>
                </div>
              </article>
              <article class="log-row">
                <strong>모집 공고 {{ (selectedManagedTeam.recruitments || []).length }}건</strong>
                <div class="subline">
                  <span v-for="recruitment in (selectedManagedTeam.recruitments || []).slice(0, 4)" :key="recruitment.recruitmentId">
                    #{{ recruitment.recruitmentId }} {{ recruitment.title }} · {{ recruitment.status }}
                  </span>
                </div>
              </article>
              <article v-if="(selectedManagedTeam.closureSnapshots || []).length" class="log-row">
                <strong>종료 스냅샷 {{ selectedManagedTeam.closureSnapshots.length }}건</strong>
                <div class="subline">
                  <span v-for="snapshot in selectedManagedTeam.closureSnapshots.slice(0, 3)" :key="snapshot.closureSnapshotId">
                    #{{ snapshot.closureSnapshotId }} · {{ snapshot.endType }} · {{ formatDate(snapshot.createdAt) }}
                  </span>
                </div>
              </article>
            </section>
          </template>
          <p v-else class="muted">목록에서 팀을 선택하세요.</p>
        </form>
      </div>
    </section>

    <section v-if="activeAdminPanel === 'users' && hasPermission('USER_SANCTION')" class="form-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">Users</span>
          <h2>{{ isUserEditRoute ? '회원 정보 수정' : isUserDetailRoute ? '회원 상세' : '회원 관리' }}</h2>
        </div>
        <div v-if="isUserDetailRoute" class="row-actions">
          <RouterLink class="ghost-button" :to="{ name: 'admin-users' }">목록</RouterLink>
          <RouterLink
            v-if="!isUserEditRoute && selectedManagedUser && managedUserCanEdit(selectedManagedUser)"
            class="primary-button"
            :to="{ name: 'admin-users-edit', params: { userId: selectedManagedUser.userId } }"
          >
            수정
          </RouterLink>
        </div>
        <button v-else class="ghost-button" type="button" :disabled="loadingManagedUsers" @click="loadManagedUsers">
          {{ loadingManagedUsers ? '조회 중' : '목록 새로고침' }}
        </button>
      </div>
      <p v-if="userSaved" class="notice-text">{{ userSaved }}</p>
      <p v-if="sanctionSaved" class="notice-text">{{ sanctionSaved }}</p>
      <p v-if="sectionErrors.users" class="error-text">{{ sectionErrors.users }}</p>
      <p v-if="sectionErrors.sanctions" class="error-text">{{ sectionErrors.sanctions }}</p>
      <div v-if="isUserListRoute" class="form-grid">
        <label class="field">
          <span>검색어</span>
          <input v-model="managedUserFilters.keyword" placeholder="아이디, 닉네임, 전화번호">
        </label>
        <label class="field">
          <span>계정 유형</span>
          <select v-model="managedUserFilters.accountType" @change="loadManagedUsers">
            <option value="">전체</option>
            <option value="USER">일반</option>
            <option value="COMPANY">회사</option>
            <option value="ADMIN">관리자</option>
          </select>
        </label>
        <label class="field">
          <span>계정 상태</span>
          <select v-model="managedUserFilters.accountStatus" @change="loadManagedUsers">
            <option value="">전체</option>
            <option value="ACTIVE">정상</option>
            <option value="PENDING_APPROVAL">승인 대기</option>
            <option value="TEMP_SUSPENDED">임시 정지</option>
            <option value="PERM_SUSPENDED">영구 정지</option>
            <option value="WITHDRAWN">회원 탈퇴</option>
          </select>
        </label>
        <button class="ghost-button field-button" type="button" :disabled="loadingManagedUsers" @click="loadManagedUsers">
          검색
        </button>
      </div>
      <div class="admin-columns" :class="{ 'admin-detail-only': isUserDetailRoute, 'admin-list-only': isUserListRoute }">
        <div v-if="isUserListRoute" class="log-list">
          <h3>회원 목록</h3>
          <article
            v-for="user in managedUsers"
            :key="user.userId"
            class="log-row admin-board-row"
            :class="{ selected: selectedManagedUser?.userId === user.userId }"
            role="button"
            tabindex="0"
            @click="selectManagedUser(user)"
            @keydown.enter.prevent="selectManagedUser(user)"
          >
            <div class="row-head">
              <div>
                <strong>#{{ user.userId }} · {{ user.loginId }}</strong>
                <div class="subline">
                  <span>{{ user.nickname }}</span>
                  <span>{{ accountTypeLabel(user.accountType) }}</span>
                  <span>{{ accountStatusLabel(user.accountStatus) }}</span>
                </div>
              </div>
            </div>
            <div class="subline">
              <span>{{ user.phone || '전화번호 없음' }}</span>
              <span v-if="user.activeSanctionId">제재 #{{ user.activeSanctionId }}</span>
              <span v-if="user.lastLoginAt">최근 로그인 {{ formatDate(user.lastLoginAt) }}</span>
            </div>
            <div class="subline">
              <span>가입 {{ formatDate(user.createdAt) }}</span>
              <span v-if="user.deactivatedAt">비활성 {{ formatDate(user.deactivatedAt) }}</span>
            </div>
          </article>
          <p v-if="loadingManagedUsers" class="muted">회원을 조회하고 있습니다.</p>
          <p v-if="!loadingManagedUsers && managedUsers.length === 0" class="muted">조건에 맞는 회원이 없습니다.</p>
        </div>
        <form v-if="isUserDetailRoute" class="log-list" @submit.prevent="saveManagedUser">
          <div class="row-head">
            <h3>상세/수정</h3>
            <span v-if="loadingManagedUserDetail" class="muted">상세 조회 중</span>
          </div>
          <template v-if="selectedManagedUser">
            <div class="log-row">
              <strong>#{{ selectedManagedUser.userId }} · {{ selectedManagedUser.loginId }}</strong>
              <div class="subline">
                <span>{{ selectedManagedUser.nickname }}</span>
                <span>{{ accountTypeLabel(selectedManagedUser.accountType) }}</span>
                <span>{{ accountStatusLabel(selectedManagedUser.accountStatus) }}</span>
              </div>
              <div class="subline">
                <span>{{ selectedManagedUser.phone || '전화번호 없음' }}</span>
                <span v-if="selectedManagedUser.email">이메일 {{ selectedManagedUser.email }}</span>
                <span v-if="selectedManagedUser.lastLoginAt">최근 로그인 {{ formatDate(selectedManagedUser.lastLoginAt) }}</span>
              </div>
            </div>
            <div class="form-grid">
              <label class="field">
                <span>로그인 ID</span>
                <input :value="selectedManagedUser.loginId" readonly>
              </label>
              <label v-if="selectedManagedUser.email" class="field">
                <span>이메일</span>
                <input :value="selectedManagedUser.email" readonly>
              </label>
              <label class="field">
                <span>닉네임</span>
                <input v-model="managedUserForm.nickname" maxlength="50" required :readonly="!isUserEditRoute || !managedUserCanEdit(selectedManagedUser)">
              </label>
              <label class="field">
                <span>전화번호</span>
                <input v-model="managedUserForm.phone" maxlength="30" :readonly="!isUserEditRoute || !managedUserCanEdit(selectedManagedUser)">
              </label>
              <label class="field">
                <span>계정 유형</span>
                <select v-model="managedUserForm.accountType" :disabled="!isUserEditRoute || !managedUserCanEdit(selectedManagedUser)">
                  <option value="USER">일반</option>
                  <option value="COMPANY">회사</option>
                  <option value="ADMIN" disabled>관리자</option>
                </select>
              </label>
              <label class="field">
                <span>계정 상태</span>
                <select v-model="managedUserForm.accountStatus" :disabled="!isUserEditRoute || !managedUserCanEdit(selectedManagedUser)">
                  <option value="ACTIVE">정상</option>
                  <option value="PENDING_APPROVAL">승인 대기</option>
                  <option value="TEMP_SUSPENDED">임시 정지</option>
                  <option value="PERM_SUSPENDED">영구 정지</option>
                  <option value="WITHDRAWN">회원 탈퇴</option>
                </select>
              </label>
              <label v-if="isUserEditRoute" class="field wide">
                <span>수정 사유</span>
                <input v-model="managedUserForm.reason" maxlength="1000" required placeholder="관리자 처리 사유" :readonly="!managedUserCanEdit(selectedManagedUser)">
              </label>
            </div>
            <div class="row-actions">
              <button
                v-if="isUserEditRoute"
                class="primary-button"
                type="submit"
                :disabled="!managedUserCanEdit(selectedManagedUser) || userActionId === selectedManagedUser.userId"
              >
                {{ userActionId === selectedManagedUser.userId ? '처리 중' : '수정 저장' }}
              </button>
              <RouterLink
                v-if="isUserEditRoute"
                class="ghost-button"
                :to="{ name: 'admin-users-detail', params: { userId: selectedManagedUser.userId } }"
              >
                취소
              </RouterLink>
              <button
                v-if="managedUserCanEdit(selectedManagedUser) && !managedUserIsInactive(selectedManagedUser)"
                class="ghost-button danger"
                type="button"
                :disabled="userActionId === selectedManagedUser.userId"
                @click="requestManagedUserAction('DEACTIVATE')"
              >
                비활성화
              </button>
              <button
                v-if="managedUserCanEdit(selectedManagedUser) && managedUserIsInactive(selectedManagedUser)"
                class="ghost-button"
                type="button"
                :disabled="userActionId === selectedManagedUser.userId"
                @click="requestManagedUserAction('RESTORE')"
              >
                복구
              </button>
              <button
                v-if="managedUserCanEdit(selectedManagedUser)"
                class="ghost-button"
                type="button"
                @click="pickManagedUserForSanction(selectedManagedUser)"
              >
                제재 대상으로 선택
              </button>
            </div>
            <div
              v-for="action in ['DEACTIVATE', 'RESTORE']"
              :key="action"
              v-show="isPendingAdminAction('managed-user', selectedManagedUser.userId, action)"
              class="confirm-inline admin-confirm"
              :class="{ 'danger-confirm': action === 'DEACTIVATE', 'success-confirm': action === 'RESTORE' }"
            >
              <span>회원 #{{ selectedManagedUser.userId }} 계정을 {{ action === 'RESTORE' ? '복구' : '비활성화' }}합니다.</span>
              <label class="field">
                <span>처리 사유</span>
                <input v-model="managedUserActionReason" maxlength="1000" placeholder="관리자 처리 사유">
              </label>
              <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
              <button class="primary-button" type="button" :disabled="userActionId === selectedManagedUser.userId" @click="runManagedUserAction(action)">
                {{ action === 'RESTORE' ? '복구 확정' : '비활성화 확정' }}
              </button>
            </div>
            <section class="log-list">
              <div class="row-head">
                <h3>제재 연결</h3>
              </div>
              <div v-if="sanctionForm.userId === String(selectedManagedUser.userId)" class="report-compose">
                <div class="form-grid">
                  <label class="field">
                    <span>제재 유형</span>
                    <select v-model="sanctionForm.sanctionType">
                      <option value="TEMP_SUSPENDED">임시 정지</option>
                      <option value="PERM_SUSPENDED">영구 정지</option>
                    </select>
                  </label>
                  <label v-if="sanctionForm.sanctionType === 'TEMP_SUSPENDED'" class="field">
                    <span>종료 시각</span>
                    <input v-model="sanctionForm.sanctionUntil" type="datetime-local">
                  </label>
                  <label class="field wide">
                    <span>제재 사유</span>
                    <textarea v-model="sanctionForm.reason" rows="3" maxlength="1000"></textarea>
                  </label>
                </div>
                <div class="row-actions">
                  <button class="ghost-button" type="button" :disabled="sanctioning" @click="requestSanctionCreate">
                    {{ sanctioning ? '적용 중' : '제재 적용' }}
                  </button>
                </div>
                <div v-if="isPendingAdminAction('sanction-create', sanctionForm.userId)" class="confirm-inline danger-confirm admin-confirm">
                  <span>{{ sanctionForm.nickname || `#${sanctionForm.userId}` }} 계정에 제재를 적용합니다.</span>
                  <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
                  <button class="primary-button" type="button" @click="createSanction">제재 확정</button>
                </div>
              </div>
              <article v-if="selectedManagedUser.activeSanctionId" class="log-row">
                <strong>활성 제재 #{{ selectedManagedUser.activeSanctionId }} · {{ selectedManagedUser.activeSanctionType }}</strong>
                <p>{{ selectedManagedUser.activeSanctionReason }}</p>
                <div class="subline">
                  <span v-if="selectedManagedUser.activeSanctionUntil">종료 {{ formatDate(selectedManagedUser.activeSanctionUntil) }}</span>
                  <span v-if="selectedManagedUser.activeSanctionCreatedAt">적용 {{ formatDate(selectedManagedUser.activeSanctionCreatedAt) }}</span>
                </div>
              </article>
              <article v-for="sanction in selectedManagedUser.recentSanctions || []" :key="sanction.sanctionId" class="log-row">
                <strong>#{{ sanction.sanctionId }} · {{ sanction.sanctionType }} · {{ sanction.status }}</strong>
                <p>{{ sanction.reason }}</p>
                <div class="subline">
                  <span>{{ sanction.createdByNickname || `관리자 #${sanction.createdBy}` }}</span>
                  <span>{{ formatDate(sanction.createdAt) }}</span>
                  <span v-if="sanction.revokedAt">해제 {{ formatDate(sanction.revokedAt) }}</span>
                </div>
                <template v-if="sanction.status === 'ACTIVE'">
                  <label class="field">
                    <span>해제 사유</span>
                    <input v-model="revokeDrafts[sanction.sanctionId]" maxlength="1000" placeholder="운영자 확인 후 해제">
                  </label>
                  <button
                    class="ghost-button"
                    type="button"
                    :disabled="sanctionRevokeId === sanction.sanctionId"
                    @click="requestAdminAction('sanction-revoke', sanction.sanctionId)"
                  >
                    {{ sanctionRevokeId === sanction.sanctionId ? '해제 중' : '제재 해제' }}
                  </button>
                  <div v-if="isPendingAdminAction('sanction-revoke', sanction.sanctionId)" class="confirm-inline success-confirm admin-confirm">
                    <span>{{ selectedManagedUser.nickname }} 계정의 활성 제재를 해제합니다.</span>
                    <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
                    <button class="primary-button" type="button" @click="revokeManagedUserSanction(sanction)">해제 확정</button>
                  </div>
                </template>
              </article>
              <p v-if="!(selectedManagedUser.recentSanctions || []).length" class="muted">최근 제재 이력이 없습니다.</p>
            </section>
          </template>
          <p v-else class="muted">목록에서 회원을 선택하세요.</p>
        </form>
      </div>
    </section>

    <section v-if="activeAdminPanel === 'board-posts' && hasPermission('CONTENT_MODERATION')" class="form-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">Board Posts</span>
          <h2>{{ isPostDetailRoute ? '게시글 상세 및 관리' : '게시글 관리' }}</h2>
        </div>
        <RouterLink v-if="isPostDetailRoute" class="ghost-button" :to="{ name: 'admin-posts' }">목록</RouterLink>
        <button v-else class="ghost-button" type="button" :disabled="loadingBoardPosts" @click="loadBoardPosts">
          {{ loadingBoardPosts ? '조회 중' : '목록 새로고침' }}
        </button>
      </div>
      <p v-if="boardPostSaved" class="notice-text">{{ boardPostSaved }}</p>
      <p v-if="sectionErrors.boards" class="error-text">{{ sectionErrors.boards }}</p>
      <div v-if="isPostListRoute" class="form-grid">
        <label class="field">
          <span>검색어</span>
          <input v-model="boardPostFilters.keyword" placeholder="제목, 본문, 작성자">
        </label>
        <label class="field">
          <span>카테고리</span>
          <select v-model="boardPostFilters.category" @change="loadBoardPosts">
            <option value="">전체</option>
            <option value="WORK">작업물</option>
            <option value="FREE">자유</option>
          </select>
        </label>
        <label class="field">
          <span>상태</span>
          <select v-model="boardPostFilters.status" @change="loadBoardPosts">
            <option value="">전체</option>
            <option value="PUBLISHED">게시 중</option>
            <option value="BLINDED">숨김</option>
            <option value="ADMIN_DELETED">관리자 삭제</option>
            <option value="AUTHOR_DELETED">작성자 삭제</option>
          </select>
        </label>
        <label class="field">
          <span>공개 범위</span>
          <select v-model="boardPostFilters.visibility" @change="loadBoardPosts">
            <option value="">전체</option>
            <option value="PUBLIC">공개</option>
            <option value="COMPANY">회사</option>
            <option value="PRIVATE">비공개</option>
          </select>
        </label>
        <label class="field">
          <span>작성자 ID</span>
          <input v-model="boardPostFilters.authorUserId" min="1" type="number">
        </label>
        <button class="ghost-button field-button" type="button" :disabled="loadingBoardPosts" @click="loadBoardPosts">
          검색
        </button>
      </div>
      <div class="admin-columns" :class="{ 'admin-detail-only': isPostDetailRoute, 'admin-list-only': isPostListRoute }">
        <div v-if="isPostListRoute" class="log-list">
          <h3>게시글 목록</h3>
          <article
            v-for="post in boardPosts"
            :key="post.postId"
            class="log-row admin-board-row"
            :class="{ selected: selectedBoardPost?.postId === post.postId }"
            role="button"
            tabindex="0"
            @click="selectBoardPost(post)"
            @keydown.enter.prevent="selectBoardPost(post)"
          >
            <div class="row-head">
              <div>
                <strong>#{{ post.postId }} · {{ post.title }}</strong>
                <div class="subline">
                  <span>{{ boardPostCategoryLabel(post.category) }}</span>
                  <span>{{ boardPostStatusLabel(post.status) }}</span>
                  <span>{{ boardPostVisibilityLabel(post.visibility) }}</span>
                </div>
              </div>
            </div>
            <div class="subline">
              <span>{{ post.authorNickname || `작성자 #${post.authorUserId}` }}</span>
              <span>조회 {{ post.viewCount || 0 }}</span>
              <span>좋아요 {{ post.likeCount || 0 }}</span>
              <span>리뷰 {{ post.reviewCount || 0 }}</span>
              <span v-if="post.reportCount !== undefined">신고 {{ post.reportCount || 0 }}</span>
            </div>
            <div class="subline">
              <span>생성 {{ formatDate(post.createdAt) }}</span>
              <span v-if="post.updatedAt">수정 {{ formatDate(post.updatedAt) }}</span>
              <span v-if="post.deletedAt">삭제 {{ formatDate(post.deletedAt) }}</span>
            </div>
          </article>
          <p v-if="loadingBoardPosts" class="muted">게시글을 조회하고 있습니다.</p>
          <p v-if="!loadingBoardPosts && boardPosts.length === 0" class="muted">조건에 맞는 게시글이 없습니다.</p>
        </div>
        <form v-if="isPostDetailRoute" class="log-list" @submit.prevent="saveBoardPost">
          <div class="row-head">
            <h3>상세/수정</h3>
            <span v-if="loadingBoardPostDetail" class="muted">상세 조회 중</span>
          </div>
          <template v-if="selectedBoardPost">
            <div class="log-row">
              <strong>#{{ selectedBoardPost.postId }} · {{ selectedBoardPost.title }}</strong>
              <div class="subline">
                <span>{{ selectedBoardPost.authorNickname || `작성자 #${selectedBoardPost.authorUserId}` }}</span>
                <span>{{ boardPostCategoryLabel(selectedBoardPost.category) }}</span>
                <span>{{ boardPostStatusLabel(selectedBoardPost.status) }}</span>
                <span>{{ boardPostVisibilityLabel(selectedBoardPost.visibility) }}</span>
              </div>
              <div class="subline">
                <span>조회 {{ selectedBoardPost.viewCount || 0 }}</span>
                <span>좋아요 {{ selectedBoardPost.likeCount || 0 }}</span>
                <span>리뷰 {{ selectedBoardPost.reviewCount || selectedBoardPost.publishedReviewCount || 0 }}</span>
                <span>신고 {{ selectedBoardPost.reportCount || 0 }}</span>
              </div>
            </div>
            <label class="field">
              <span>제목</span>
              <input v-model="boardPostForm.title" maxlength="150" required :readonly="!boardPostCanEdit(selectedBoardPost)">
            </label>
            <label class="field">
              <span>본문</span>
              <textarea v-model="boardPostForm.body" rows="7" maxlength="10000" required :readonly="!boardPostCanEdit(selectedBoardPost)"></textarea>
            </label>
            <div class="form-grid">
              <label class="field">
                <span>카테고리</span>
                <select v-model="boardPostForm.category" :disabled="!boardPostCanEdit(selectedBoardPost)">
                  <option value="WORK">작업물</option>
                  <option value="FREE">자유</option>
                </select>
              </label>
              <label class="field">
                <span>공개 범위</span>
                <select v-model="boardPostForm.visibility" :disabled="!boardPostCanEdit(selectedBoardPost)">
                  <option value="PUBLIC">공개</option>
                  <option value="COMPANY">회사</option>
                  <option value="PRIVATE">비공개</option>
                </select>
              </label>
              <label class="field">
                <span>상태</span>
                <select v-model="boardPostForm.status" :disabled="!boardPostCanEdit(selectedBoardPost)">
                  <option v-if="['ADMIN_DELETED', 'AUTHOR_DELETED'].includes(boardPostForm.status)" :value="boardPostForm.status" disabled>
                    {{ boardPostStatusLabel(boardPostForm.status) }}
                  </option>
                  <option value="PUBLISHED">게시 중</option>
                  <option value="BLINDED">숨김</option>
                </select>
              </label>
              <label class="field">
                <span>수정 사유</span>
                <input v-model="boardPostForm.reason" maxlength="1000" required placeholder="관리자 처리 사유" :readonly="!boardPostCanEdit(selectedBoardPost)">
              </label>
            </div>
            <div class="row-actions">
              <button class="primary-button" type="submit" :disabled="!boardPostCanEdit(selectedBoardPost) || boardPostActionId === selectedBoardPost.postId">
                {{ boardPostActionId === selectedBoardPost.postId ? '처리 중' : '수정 저장' }}
              </button>
              <button
                v-if="selectedBoardPost.status !== 'BLINDED' && !selectedBoardPost.deletedAt"
                class="ghost-button"
                type="button"
                :disabled="boardPostActionId === selectedBoardPost.postId"
                @click="requestBoardPostAction('HIDE')"
              >
                숨김
              </button>
              <button
                v-if="selectedBoardPost.status !== 'ADMIN_DELETED' && !selectedBoardPost.deletedAt"
                class="ghost-button danger"
                type="button"
                :disabled="boardPostActionId === selectedBoardPost.postId"
                @click="requestBoardPostAction('DELETE')"
              >
                삭제
              </button>
              <button
                v-if="selectedBoardPost.status !== 'PUBLISHED' || selectedBoardPost.deletedAt"
                class="ghost-button"
                type="button"
                :disabled="boardPostActionId === selectedBoardPost.postId"
                @click="requestBoardPostAction('RESTORE')"
              >
                복구
              </button>
            </div>
            <div
              v-for="action in ['HIDE', 'DELETE', 'RESTORE']"
              :key="action"
              v-show="isPendingAdminAction('board-post', selectedBoardPost.postId, action)"
              class="confirm-inline admin-confirm"
              :class="{ 'danger-confirm': action !== 'RESTORE', 'success-confirm': action === 'RESTORE' }"
            >
              <span>게시글 #{{ selectedBoardPost.postId }}을 {{ boardPostActionLabel(action) }} 처리합니다.</span>
              <label class="field">
                <span>처리 사유</span>
                <input v-model="boardPostActionReason" maxlength="1000" placeholder="관리자 처리 사유">
              </label>
              <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
              <button class="primary-button" type="button" :disabled="boardPostActionId === selectedBoardPost.postId" @click="runBoardPostAction(action)">
                {{ boardPostActionLabel(action) }} 확정
              </button>
            </div>
          </template>
          <p v-else class="muted">목록에서 게시글을 선택하세요.</p>
        </form>
      </div>
    </section>

    <template v-if="activeAdminPanel === 'contests' && hasPermission('CONTEST_MANAGE')">
      <nav class="admin-contest-tabs" aria-label="공모전 관리 메뉴">
        <RouterLink :to="{ name: adminContestRouteBySection.overview }" :class="{ active: activeContestAdminSection === 'overview' }">개요</RouterLink>
        <RouterLink :to="{ name: adminContestRouteBySection.manual }" :class="{ active: activeContestAdminSection === 'manual' }">직접 등록</RouterLink>
        <RouterLink :to="{ name: adminContestRouteBySection.crawler }" :class="{ active: activeContestAdminSection === 'crawler' }">외부 크롤링</RouterLink>
        <RouterLink :to="{ name: adminContestRouteBySection.list }" :class="{ active: activeContestAdminSection === 'list' }">등록/수집 목록</RouterLink>
        <RouterLink :to="{ name: adminContestRouteBySection.requests }" :class="{ active: activeContestAdminSection === 'requests' }">회사 요청</RouterLink>
      </nav>

      <p v-if="contestSaved" class="notice-text">{{ contestSaved }}</p>
      <p v-if="sectionErrors.contests" class="error-text">{{ sectionErrors.contests }}</p>

      <section v-if="activeContestAdminSection === 'overview'" class="form-panel admin-contest-overview">
        <div class="form-head">
          <div>
            <span class="eyebrow">Contest</span>
            <h2>공모전 관리</h2>
          </div>
          <button class="ghost-button" type="button" :disabled="loadingManagedContests" @click="loadManagedContests">
            {{ loadingManagedContests ? '조회 중' : '목록 새로고침' }}
          </button>
        </div>
        <div class="admin-contest-overview-grid">
          <RouterLink class="admin-contest-overview-card" :to="{ name: adminContestRouteBySection.manual }">
            <strong>공모전 직접 등록</strong>
            <span>운영자가 내부/외부 공모전을 직접 등록합니다.</span>
          </RouterLink>
          <RouterLink class="admin-contest-overview-card" :to="{ name: adminContestRouteBySection.crawler }">
            <strong>외부 공모전 크롤링</strong>
            <span>콘테스트코리아 수집 실행과 결과를 확인합니다.</span>
          </RouterLink>
          <RouterLink class="admin-contest-overview-card" :to="{ name: adminContestRouteBySection.list }">
            <strong>등록/수집 공모전 목록</strong>
            <span>직접 등록 및 수집 공모전의 상태를 관리합니다.</span>
          </RouterLink>
          <RouterLink class="admin-contest-overview-card" :to="{ name: adminContestRouteBySection.requests }">
            <strong>회사 개설 요청</strong>
            <span>회사 계정의 공모전 개설 요청을 검토합니다.</span>
          </RouterLink>
        </div>
      </section>

      <section v-if="activeContestAdminSection === 'manual'" class="form-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">Contest</span>
          <h2>공모전 관리</h2>
        </div>
        <button v-if="editingContestId" class="ghost-button" type="button" @click="resetContestForm">수정 취소</button>
        <button class="ghost-button" type="button" :disabled="loadingManagedContests" @click="loadManagedContests">
          {{ loadingManagedContests ? '조회 중' : '목록 새로고침' }}
        </button>
      </div>
      <div class="row-head compact">
        <h3>{{ editingContestId ? '수동 공모전 수정' : '수동 공모전 등록' }}</h3>
        <span>운영자가 직접 등록하는 공모전 정보입니다.</span>
      </div>
      <form class="form-grid" @submit.prevent="createContest">
        <label class="field">
          <span>유형</span>
          <select v-model="contestForm.contestType">
            <option value="INTERNAL">자체</option>
            <option value="EXTERNAL">외부</option>
          </select>
        </label>
        <label class="field">
          <span>주최/주관</span>
          <input v-model="contestForm.organizer" maxlength="120" required>
        </label>
        <label class="field">
          <span>주최 유형</span>
          <select v-model="contestForm.organizerType">
            <option value="">미분류</option>
            <option v-for="option in contestOrganizerOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
        <label class="field wide">
          <span>제목</span>
          <input v-model="contestForm.title" maxlength="200" required>
        </label>
        <label class="field wide">
          <span>요약</span>
          <textarea v-model="contestForm.summary" rows="3" maxlength="500" required></textarea>
        </label>
        <label class="field">
          <span>주제</span>
          <input v-model="contestForm.theme" maxlength="150">
        </label>
        <label class="field">
          <span>상금/지원</span>
          <input v-model="contestForm.prizeText" maxlength="150">
        </label>
        <label class="field">
          <span>총상금(원)</span>
          <input v-model.number="contestForm.totalPrizeAmount" type="number" min="0" step="10000">
        </label>
        <label class="field">
          <span>1등 상금(원)</span>
          <input v-model.number="contestForm.firstPrizeAmount" type="number" min="0" step="10000">
        </label>
        <label class="field">
          <span>시작일</span>
          <input v-model="contestForm.startAt" type="datetime-local">
        </label>
        <label class="field">
          <span>마감일</span>
          <input v-model="contestForm.deadlineAt" type="datetime-local" required>
        </label>
        <label class="field">
          <span>제출 이메일</span>
          <input v-model="contestForm.submissionEmail" type="email" maxlength="255">
        </label>
        <label class="field">
          <span>외부 링크</span>
          <input v-model="contestForm.externalUrl" maxlength="500">
        </label>
        <label class="field wide contest-image-picker">
          <span>대표 이미지</span>
          <input :key="contestImageInputKey" type="file" accept="image/jpeg,image/png,image/webp" @change="onContestImageChange">
          <small>JPEG, PNG, WebP · 최대 5MB</small>
          <ProtectedImage
            :src="adminContestImagePreview()"
            :sources="adminContestImageSources()"
            :fallback="defaultContestImage"
            alt="공모전 대표 이미지 미리보기"
          />
        </label>
        <label class="field">
          <span>모집 대상</span>
          <input v-model="contestForm.targetText" maxlength="500">
        </label>
        <fieldset class="field wide contest-structured-field">
          <legend>대상 분류</legend>
          <div class="contest-form-options">
            <label v-for="option in contestTargetOptions" :key="option.value"><input v-model="contestForm.targetCodes" type="checkbox" :value="option.value">{{ option.label }}</label>
          </div>
        </fieldset>
        <fieldset class="field wide contest-structured-field">
          <legend>지역 분류</legend>
          <div class="contest-form-options">
            <label v-for="option in contestRegionOptions" :key="option.value"><input v-model="contestForm.regionCodes" type="checkbox" :value="option.value">{{ option.label }}</label>
          </div>
        </fieldset>
        <label class="field">
          <span>필요 역할</span>
          <input v-model="contestForm.requiredRolesText" maxlength="500">
        </label>
        <label class="field wide">
          <span>관련 장르</span>
          <input v-model="contestForm.relatedGenresText" maxlength="500">
        </label>
        <button class="primary-button field-button" type="submit" :disabled="creatingContest">
          {{ creatingContest ? '저장 중' : editingContestId ? '공모전 수정' : '공모전 등록' }}
        </button>
      </form>
      </section>

      <section v-if="activeContestAdminSection === 'crawler'" class="form-panel">
      <section class="admin-crawler-panel" aria-label="콘테스트코리아 수집 운영">
        <div class="row-head">
          <div>
            <h3>외부 공모전 크롤링</h3>
            <p class="muted">콘테스트코리아에서 외부 공모전 정보를 수집하고 DB에 반영합니다.</p>
          </div>
          <div class="row-actions">
            <label class="compact-filter">
              <span>페이지</span>
              <input v-model.number="contestCrawlerForm.maxPages" type="number" min="1" max="10">
            </label>
            <label class="compact-filter">
              <span>건수</span>
              <input v-model.number="contestCrawlerForm.maxItems" type="number" min="1" max="100">
            </label>
            <label class="compact-filter inline-filter">
              <input v-model="contestCrawlerForm.dryRun" type="checkbox">
              <span>Dry run</span>
            </label>
            <button class="primary-button" type="button" :disabled="runningContestCrawler" @click="runContestKoreaCrawler">
              {{ runningContestCrawler ? '실행 중' : '수집 실행' }}
            </button>
          </div>
        </div>
        <div class="admin-crawler-current">
          <span>요청 범위 {{ contestCrawlerForm.maxPages || 1 }}페이지 · {{ contestCrawlerForm.maxItems || 1 }}건</span>
          <strong>{{ contestCrawlerForm.dryRun ? 'Dry run: DB 저장과 포스터 저장 없음' : '저장 실행: DB upsert와 허용 포스터 저장' }}</strong>
        </div>
        <div class="admin-crawler-policy">
          <span>출처 표기: 출처: 콘테스트코리아</span>
          <span>원문 상세 URL 저장·제공</span>
          <span>허용 문구 확인 포스터만 저장</span>
          <span>포스터 권한 문구와 수집일 보존</span>
        </div>
        <div v-if="contestCrawlerResult" class="admin-crawler-metrics">
          <div
            v-for="metric in crawlerResultMetrics(contestCrawlerResult)"
            :key="metric.label"
            :class="['admin-crawler-metric', metric.accent ? `metric-${metric.accent}` : '']"
          >
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
          </div>
        </div>
        <div v-if="crawlerAllResultItems.length" class="admin-crawler-items">
          <div class="row-head compact admin-crawler-result-head">
            <div>
              <h3>실행 결과 전체 목록</h3>
              <span>{{ crawlerFilteredResultItems.length }} / {{ crawlerAllResultItems.length }}건</span>
            </div>
            <div class="admin-crawler-result-controls">
              <label class="compact-filter">
                <span>유형</span>
                <select v-model="crawlerResultFilters.status">
                  <option v-for="option in crawlerStatusFilterOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </label>
              <label class="compact-filter">
                <span>개수</span>
                <select v-model="crawlerResultFilters.pageSize">
                  <option v-for="option in crawlerPageSizeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </label>
              <nav v-if="crawlerResultTotalPages > 1" class="admin-crawler-pagination compact" aria-label="크롤링 결과 상단 페이지">
                <button type="button" :disabled="crawlerResultPage === 1" @click="setCrawlerResultPage(crawlerResultPage - 1)">이전</button>
                <button
                  v-for="page in crawlerResultPageNumbers"
                  :key="page"
                  type="button"
                  :class="{ active: crawlerResultPage === page }"
                  :aria-current="crawlerResultPage === page ? 'page' : undefined"
                  @click="setCrawlerResultPage(page)"
                >
                  {{ page }}
                </button>
                <button type="button" :disabled="crawlerResultPage === crawlerResultTotalPages" @click="setCrawlerResultPage(crawlerResultPage + 1)">다음</button>
              </nav>
            </div>
          </div>
          <div class="admin-crawler-item-list">
            <article
              v-for="(item, index) in crawlerPagedResultItems"
              :key="`${index}-${item.sourceExternalId || item.detailUrl || item.status}`"
              :class="['admin-crawler-item', { failed: item.status === 'FAILED' }]"
            >
              <div class="admin-crawler-item-head">
                <span :class="['admin-crawler-status', crawlerStatusClass(item.status)]">{{ crawlerStatusLabel(item.status) }}</span>
                <strong>{{ item.sourceExternalId || '외부 ID 없음' }}</strong>
                <small v-if="item.contestId">공모전 #{{ item.contestId }}</small>
                <small v-if="item.posterStored">포스터 저장</small>
              </div>
              <p v-if="item.stage || item.message">
                <b v-if="item.stage">{{ item.stage }}</b>
                <span>{{ item.message || '처리 메시지 없음' }}</span>
              </p>
              <a
                v-if="item.detailUrl"
                class="admin-crawler-source-link"
                :href="item.detailUrl"
                target="_blank"
                rel="noopener noreferrer"
                :aria-label="crawlerDetailAriaLabel(item)"
              >
                원문
              </a>
            </article>
          </div>
          <p v-if="crawlerFilteredResultItems.length === 0" class="muted">선택한 유형의 실행 결과가 없습니다.</p>
          <nav v-if="crawlerResultTotalPages > 1" class="admin-crawler-pagination bottom" aria-label="크롤링 결과 하단 페이지">
            <button type="button" :disabled="crawlerResultPage === 1" @click="setCrawlerResultPage(crawlerResultPage - 1)">이전</button>
            <button
              v-for="page in crawlerResultPageNumbers"
              :key="page"
              type="button"
              :class="{ active: crawlerResultPage === page }"
              :aria-current="crawlerResultPage === page ? 'page' : undefined"
              @click="setCrawlerResultPage(page)"
            >
              {{ page }}
            </button>
            <button type="button" :disabled="crawlerResultPage === crawlerResultTotalPages" @click="setCrawlerResultPage(crawlerResultPage + 1)">다음</button>
          </nav>
        </div>
      </section>
      </section>

      <section v-if="activeContestAdminSection === 'list'" class="form-panel">
      <div class="row-head">
        <h3>등록/수집 공모전 목록</h3>
        <div class="row-actions">
          <label class="admin-contest-select-all">
            <input
              type="checkbox"
              :checked="allManagedContestsSelected"
              :disabled="managedContestIds.length === 0"
              @change="toggleAllManagedContests"
            >
            <span>전체 선택</span>
          </label>
          <button
            class="ghost-button danger-action"
            type="button"
            :disabled="selectedManagedContestCount === 0 || contestDeleteActionId === 'selected'"
            @click="requestSelectedContestDelete"
          >
            {{ contestDeleteActionId === 'selected' ? '삭제 중' : `선택 삭제 ${selectedManagedContestCount || ''}` }}
          </button>
          <label class="compact-filter">
            <span>상태</span>
            <select v-model="contestFilters.status" @change="loadManagedContests">
              <option value="ALL">전체</option>
              <option value="OPEN">진행 중</option>
              <option value="ENDED">종료됨</option>
            </select>
          </label>
          <label class="compact-filter">
            <span>유형</span>
            <select v-model="contestFilters.contestType" @change="loadManagedContests">
              <option value="ALL">전체</option>
              <option value="INTERNAL">자체</option>
              <option value="EXTERNAL">외부</option>
            </select>
          </label>
        </div>
      </div>
      <div
        v-if="isPendingAdminAction('contest-delete', 'selected')"
        class="confirm-inline danger-confirm admin-confirm admin-contest-delete-confirm"
      >
        <span>선택한 공모전 {{ selectedManagedContestCount }}건을 삭제합니다. 저장, 제출 준비, 적합도 캐시도 함께 삭제됩니다.</span>
        <label class="field">
          <span>삭제 사유</span>
          <input v-model="contestDeleteReason" maxlength="500" required>
        </label>
        <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
        <button class="primary-button" type="button" :disabled="contestDeleteActionId === 'selected'" @click="deleteSelectedContests">
          {{ contestDeleteActionId === 'selected' ? '삭제 중' : '삭제 확정' }}
        </button>
      </div>
      <div class="log-list">
        <article v-for="contest in managedContests" :key="contest.contestId" class="log-row admin-contest-row" :class="{ selected: selectedManagedContestIds.includes(Number(contest.contestId)) }">
          <label class="admin-contest-select">
            <input v-model="selectedManagedContestIds" type="checkbox" :value="Number(contest.contestId)">
            <span class="sr-only">공모전 #{{ contest.contestId }} 선택</span>
          </label>
          <strong>#{{ contest.contestId }} · {{ contest.title }}</strong>
          <p>{{ contest.summary }}</p>
          <div class="subline">
            <span>{{ contest.contestType }}</span>
            <span>{{ contest.status }}</span>
            <span>{{ contest.organizer }}</span>
            <span>마감 {{ formatDate(contest.deadlineAt) }}</span>
            <span v-if="contest.requesterCompanyUserId">회사 #{{ contest.requesterCompanyUserId }}</span>
          </div>
          <div v-if="contestSourceLabel(contest) || hasContestOriginalUrl(contest) || isContestKoreaPoster(contest)" class="subline admin-contest-source-line">
            <span v-if="contestSourceLabel(contest)">{{ contestSourceLabel(contest) }}</span>
            <span v-if="isContestKoreaPoster(contest)">포스터 허용</span>
            <a
              v-if="hasContestOriginalUrl(contest)"
              class="admin-crawler-source-link"
              :href="contestOriginalUrl(contest)"
              target="_blank"
              rel="noopener noreferrer"
              :aria-label="contestOriginalAriaLabel(contest)"
            >
              원문
            </a>
          </div>
          <div class="row-actions">
            <button class="ghost-button" type="button" @click="editContest(contest)">수정</button>
            <button
              v-if="contest.status === 'OPEN'"
              class="ghost-button"
              type="button"
              :disabled="contestStatusActionId === contest.contestId"
              @click="requestContestStatusChange(contest, 'ENDED')"
            >
              {{ contestStatusActionId === contest.contestId ? '처리 중' : '종료' }}
            </button>
            <button
              v-else
              class="primary-button"
              type="button"
              :disabled="contestStatusActionId === contest.contestId"
              @click="requestContestStatusChange(contest, 'OPEN')"
            >
              {{ contestStatusActionId === contest.contestId ? '처리 중' : '재개' }}
            </button>
          </div>
          <div
            v-if="isPendingAdminAction('contest-status', contest.contestId, nextContestStatus(contest)) && ensureContestStatusReason(contest, nextContestStatus(contest)) !== null"
            class="confirm-inline admin-confirm"
            :class="{ 'danger-confirm': nextContestStatus(contest) === 'ENDED', 'success-confirm': nextContestStatus(contest) !== 'ENDED' }"
          >
            <span>공모전 #{{ contest.contestId }} 상태를 {{ nextContestStatus(contest) === 'ENDED' ? '종료' : '재개' }}합니다. 변경 내역은 감사 로그에 남습니다.</span>
            <label class="field">
              <span>처리 사유</span>
              <input v-model="contestStatusReasonDrafts[contestStatusReasonKey(contest, nextContestStatus(contest))]" maxlength="500" required>
            </label>
            <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
            <button class="primary-button" type="button" @click="updateContestStatus(contest, nextContestStatus(contest))">
              {{ nextContestStatus(contest) === 'ENDED' ? '종료 확정' : '재개 확정' }}
            </button>
          </div>
        </article>
        <p v-if="!loadingManagedContests && managedContests.length === 0" class="muted">관리할 공모전이 없습니다.</p>
      </div>
      </section>

      <section v-if="activeContestAdminSection === 'requests'" class="form-panel">
      <div class="row-head">
        <h3>회사 개설 요청</h3>
        <label class="compact-filter">
          <span>상태</span>
          <select v-model="contestRequestFilters.status" @change="loadContestRequests">
            <option value="PENDING">검토 대기</option>
            <option value="APPROVED">승인</option>
            <option value="REJECTED">거절</option>
            <option value="ALL">전체</option>
          </select>
        </label>
      </div>
      <div class="log-list">
        <article v-for="request in contestRequests" :key="request.requestId" class="log-row">
          <strong>#{{ request.requestId }} · {{ request.title }}</strong>
          <p>{{ request.summary }}</p>
          <div class="subline">
            <span>{{ request.companyName || request.requesterNickname }}</span>
            <span>{{ request.status }}</span>
            <span>마감 {{ formatDate(request.deadlineAt) }}</span>
            <span v-if="request.approvedContestId">공모전 #{{ request.approvedContestId }}</span>
          </div>
          <div class="subline">
            <span>{{ request.targetText || '모집 대상 미입력' }}</span>
            <span>{{ request.requiredRolesText || '필요 역할 미입력' }}</span>
            <span>{{ request.relatedGenresText || '관련 장르 미입력' }}</span>
          </div>
          <template v-if="request.status === 'PENDING'">
            <div class="row-actions">
              <button
                class="ghost-button"
                type="button"
                :disabled="contestRequestActionId === request.requestId"
                @click="requestContestRequestDecision(request, 'REJECTED')"
              >
                거절
              </button>
              <button
                class="primary-button"
                type="button"
                :disabled="contestRequestActionId === request.requestId"
                @click="requestContestRequestDecision(request, 'APPROVED')"
              >
                승인
              </button>
            </div>
            <div
              v-if="isPendingAdminAction('contest-request', request.requestId, 'REJECTED') && ensureContestRequestDecisionDraft(request, 'REJECTED') !== null"
              class="confirm-inline danger-confirm admin-confirm"
            >
              <span>회사 개설 요청 #{{ request.requestId }}을 거절합니다.</span>
              <label class="field">
                <span>처리 사유</span>
                <input v-model="contestRequestDecisionDrafts[request.requestId].REJECTED" maxlength="500" required>
              </label>
              <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
              <button class="primary-button" type="button" @click="decideContestRequest(request, 'REJECTED')">거절 확정</button>
            </div>
            <div
              v-if="isPendingAdminAction('contest-request', request.requestId, 'APPROVED') && ensureContestRequestDecisionDraft(request, 'APPROVED') !== null"
              class="confirm-inline success-confirm admin-confirm"
            >
              <span>회사 개설 요청 #{{ request.requestId }}을 승인하고 공모전으로 공개합니다.</span>
              <label class="field">
                <span>처리 사유</span>
                <input v-model="contestRequestDecisionDrafts[request.requestId].APPROVED" maxlength="500" required>
              </label>
              <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
              <button class="primary-button" type="button" @click="decideContestRequest(request, 'APPROVED')">승인 확정</button>
            </div>
          </template>
          <p v-else class="muted">{{ request.reviewReason || '처리 메모 없음' }}</p>
        </article>
        <p v-if="!loadingContestRequests && contestRequests.length === 0" class="muted">공모전 개설 요청이 없습니다.</p>
      </div>
      </section>
    </template>

    <section v-if="activeAdminPanel === 'reports' && hasPermission('CONTENT_MODERATION')" class="form-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">Moderation</span>
          <h2>콘텐츠 신고 처리</h2>
        </div>
        <button class="ghost-button" type="button" :disabled="loadingReports" @click="loadReports">
          {{ loadingReports ? '조회 중' : '새로고침' }}
        </button>
      </div>
      <p v-if="moderationSaved" class="notice-text">{{ moderationSaved }}</p>
      <p v-if="sectionErrors.reports" class="error-text">{{ sectionErrors.reports }}</p>
      <div class="form-grid">
        <label class="field">
          <span>처리 상태</span>
          <select v-model="reportFilters.status" @change="loadReports">
            <option value="">전체</option>
            <option value="PENDING">검토 대기</option>
            <option value="ACCEPTED">처리 완료</option>
            <option value="REJECTED">반려</option>
          </select>
        </label>
        <label class="field">
          <span>대상</span>
          <select v-model="reportFilters.targetType" @change="loadReports">
            <option value="">전체</option>
            <option value="BOARD_POST">게시글</option>
            <option value="BOARD_REVIEW">리뷰</option>
          </select>
        </label>
      </div>
      <div class="report-list">
        <article v-for="report in reports" :key="report.reportId" class="report-card">
          <div class="row-head">
            <div>
              <strong>#{{ report.reportId }} · {{ report.targetTitle }}</strong>
              <div class="subline">
                <span>{{ report.targetType }}</span>
                <span>{{ report.reasonCode }}</span>
                <span>{{ report.status }}</span>
                <span>{{ formatDate(report.createdAt) }}</span>
              </div>
            </div>
          </div>
          <p>{{ report.detail || report.targetContent }}</p>
          <div class="subline">
            <span>신고자 {{ report.reporterNickname || report.reporterEmail }}</span>
            <span>작성자 {{ report.targetAuthorNickname || report.targetAuthorEmail }}</span>
            <span>콘텐츠 상태 {{ report.targetStatus }}</span>
          </div>
          <div v-if="report.status === 'PENDING'" class="moderation-actions">
            <label class="field">
              <span>조치</span>
              <select v-model="ensureReportDraft(report).moderationAction">
                <option v-for="action in reportActions(report)" :key="action.key" :value="action.key">{{ action.label }}</option>
              </select>
            </label>
            <label class="field wide">
              <span>처리 메모</span>
              <textarea v-model="ensureReportDraft(report).note" rows="2" maxlength="1000"></textarea>
            </label>
            <div class="row-actions">
              <button
                class="ghost-button"
                type="button"
                :disabled="reportActionId === report.reportId"
                @click="requestAdminAction('report', report.reportId, 'REJECTED')"
              >
                반려
              </button>
              <button
                class="primary-button"
                type="button"
                :disabled="reportActionId === report.reportId"
                @click="requestAdminAction('report', report.reportId, 'ACCEPTED')"
              >
                처리
              </button>
            </div>
            <div v-if="isPendingAdminAction('report', report.reportId, 'REJECTED')" class="confirm-inline admin-confirm">
              <span>신고 #{{ report.reportId }}을 반려합니다. 대상 콘텐츠는 변경되지 않습니다.</span>
              <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
              <button class="primary-button" type="button" @click="decideContentReport(report, 'REJECTED')">반려 확정</button>
            </div>
            <div v-if="isPendingAdminAction('report', report.reportId, 'ACCEPTED')" class="confirm-inline danger-confirm admin-confirm">
              <span>신고 #{{ report.reportId }}을 처리하고 선택한 조치를 적용합니다.</span>
              <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
              <button class="primary-button" type="button" @click="decideContentReport(report, 'ACCEPTED')">처리 확정</button>
            </div>
          </div>
          <p v-else class="muted">{{ report.moderationAction }} · {{ report.resolutionNote }}</p>
        </article>
        <p v-if="!loadingReports && reports.length === 0" class="muted">신고 내역이 없습니다.</p>
      </div>
    </section>

    <section v-if="activeAdminPanel === 'files' && hasPermission('CONTENT_MODERATION')" class="form-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">Storage</span>
          <h2>작업물 파일 관리</h2>
        </div>
        <button class="ghost-button" type="button" :disabled="loadingWorkFiles" @click="loadWorkFiles">
          {{ loadingWorkFiles ? '조회 중' : '새로고침' }}
        </button>
      </div>
      <p v-if="fileSaved" class="notice-text">{{ fileSaved }}</p>
      <p v-if="sectionErrors.files" class="error-text">{{ sectionErrors.files }}</p>
      <div v-if="workFileStorage?.summary" class="metric-strip">
        <div class="metric">
          <span>활성 파일</span>
          <strong>{{ formatBytes(workFileStorage.summary.activeBytes) }}</strong>
          <small>{{ workFileStorage.summary.activeCount || 0 }}개</small>
        </div>
        <div class="metric">
          <span>운영 보관</span>
          <strong>{{ formatBytes(workFileStorage.summary.heldBytes) }}</strong>
          <small>{{ workFileStorage.summary.heldCount || 0 }}개</small>
        </div>
        <div class="metric">
          <span>삭제 대기</span>
          <strong>{{ formatBytes(workFileStorage.summary.deletedBytes) }}</strong>
          <small>{{ workFileStorage.summary.deletedCount || 0 }}개</small>
        </div>
      </div>
      <div class="form-grid">
        <label class="field">
          <span>상태</span>
          <select v-model="fileFilters.status" @change="loadWorkFiles">
            <option value="ALL">전체</option>
            <option value="ACTIVE">활성</option>
            <option value="HELD">운영 보관</option>
            <option value="DELETED">삭제 대기</option>
          </select>
        </label>
        <label class="field">
          <span>검색어</span>
          <input v-model="fileFilters.keyword" placeholder="파일명, 업로더, 팀">
        </label>
        <label class="field">
          <span>업로더 ID</span>
          <input v-model="fileFilters.uploaderUserId" min="1" type="number">
        </label>
        <label class="field">
          <span>팀 ID</span>
          <input v-model="fileFilters.teamId" min="1" type="number">
        </label>
        <button class="ghost-button field-button" type="button" @click="loadWorkFiles">검색</button>
      </div>
      <div class="admin-columns">
        <div class="log-list">
          <h3>파일 목록</h3>
          <article v-for="file in workFiles" :key="file.fileId" class="log-row">
            <div class="row-head">
              <div>
                <strong>#{{ file.fileId }} · {{ file.originalName }}</strong>
                <div class="subline">
                  <span>{{ fileStatusLabel(file.status) }}</span>
                  <span>{{ formatBytes(file.sizeBytes) }}</span>
                  <span>{{ file.uploaderNickname || file.uploaderEmail }}</span>
                  <span v-if="file.teamName">{{ file.teamName }}</span>
                </div>
              </div>
            </div>
            <div class="subline">
              <span>작업물 {{ file.workReferenceCount || 0 }}</span>
              <span>승인요청 {{ file.requestReferenceCount || 0 }}</span>
              <span>참조 합계 {{ fileReferenceCount(file) }}</span>
              <span v-if="file.physicalDeleteDueAt">물리 삭제 예정 {{ formatDate(file.physicalDeleteDueAt).slice(0, 10) }}</span>
            </div>
            <label class="field">
              <span>처리 사유</span>
              <input v-model="fileReasonDrafts[file.fileId]" maxlength="500" placeholder="운영 정책 검토">
            </label>
            <div class="row-actions">
              <button
                v-if="file.status !== 'HELD'"
                class="ghost-button"
                type="button"
                :disabled="fileActionId === file.fileId"
                @click="requestAdminAction('file', file.fileId, 'HOLD')"
              >
                {{ fileActionId === file.fileId ? '처리 중' : '보관' }}
              </button>
              <button
                v-if="file.status !== 'ACTIVE'"
                class="ghost-button"
                type="button"
                :disabled="fileActionId === file.fileId"
                @click="requestAdminAction('file', file.fileId, 'RESTORE')"
              >
                {{ fileActionId === file.fileId ? '처리 중' : '복구' }}
              </button>
              <button
                v-if="file.status !== 'DELETED'"
                class="ghost-button danger"
                type="button"
                :disabled="fileActionId === file.fileId"
                @click="requestAdminAction('file', file.fileId, 'DELETE')"
              >
                {{ fileActionId === file.fileId ? '처리 중' : '삭제' }}
              </button>
            </div>
            <div v-if="isPendingAdminAction('file', file.fileId, 'HOLD')" class="confirm-inline danger-confirm admin-confirm">
              <span>파일 #{{ file.fileId }}을 운영 보관 상태로 전환합니다.</span>
              <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
              <button class="primary-button" type="button" @click="holdWorkFile(file)">보관 확정</button>
            </div>
            <div v-if="isPendingAdminAction('file', file.fileId, 'RESTORE')" class="confirm-inline success-confirm admin-confirm">
              <span>파일 #{{ file.fileId }}을 활성 상태로 복구합니다.</span>
              <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
              <button class="primary-button" type="button" @click="restoreWorkFile(file)">복구 확정</button>
            </div>
            <div v-if="isPendingAdminAction('file', file.fileId, 'DELETE')" class="confirm-inline danger-confirm admin-confirm">
              <span>파일 #{{ file.fileId }}을 삭제 대기 상태로 전환합니다.</span>
              <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
              <button class="primary-button" type="button" @click="deleteWorkFile(file)">삭제 확정</button>
            </div>
          </article>
          <p v-if="!loadingWorkFiles && workFiles.length === 0" class="muted">파일이 없습니다.</p>
        </div>
        <div class="log-list">
          <h3>용량 상위</h3>
          <article v-for="user in workFileStorage?.topUsers || []" :key="user.userId" class="log-row">
            <strong>{{ user.nickname || user.email }} · #{{ user.userId }}</strong>
            <div class="subline">
              <span>{{ formatBytes(user.activeBytes) }}</span>
              <span>{{ user.fileCount }}개</span>
              <span>한도 {{ formatBytes(workFileStorage.userQuotaBytes) }}</span>
            </div>
          </article>
          <article v-for="team in workFileStorage?.topTeams || []" :key="team.teamId" class="log-row">
            <strong>{{ team.teamName }} · 팀 #{{ team.teamId }}</strong>
            <div class="subline">
              <span>{{ formatBytes(team.activeBytes) }}</span>
              <span>{{ team.fileCount }}개</span>
              <span>한도 {{ formatBytes(workFileStorage.teamQuotaBytes) }}</span>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section v-if="['reports', 'sanctions'].includes(activeAdminPanel) && hasPermission('USER_SANCTION')" class="form-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">Sanction</span>
          <h2>사용자 제재</h2>
        </div>
        <button class="ghost-button" type="button" :disabled="loadingSanctions" @click="loadSanctions">
          {{ loadingSanctions ? '조회 중' : '제재 새로고침' }}
        </button>
      </div>
      <p v-if="sanctionSaved" class="notice-text">{{ sanctionSaved }}</p>
      <p v-if="sectionErrors.sanctions" class="error-text">{{ sectionErrors.sanctions }}</p>
      <div class="form-grid">
        <label class="field">
          <span>사용자 검색</span>
          <input v-model="userFilters.keyword" placeholder="이메일, 닉네임, ID">
        </label>
        <label class="field">
          <span>계정 상태</span>
          <select v-model="userFilters.accountStatus">
            <option value="">전체</option>
            <option value="ACTIVE">정상</option>
            <option value="PENDING_APPROVAL">승인 대기</option>
            <option value="TEMP_SUSPENDED">임시 정지</option>
            <option value="PERM_SUSPENDED">영구 정지</option>
            <option value="WITHDRAWN">회원 탈퇴</option>
          </select>
        </label>
        <button class="ghost-button field-button" type="button" @click="loadModerationUsers">검색</button>
      </div>
      <div class="admin-columns">
        <div class="log-list">
          <h3>사용자</h3>
          <article v-for="user in moderationUsers" :key="user.userId" class="log-row">
            <strong>{{ user.nickname }} · #{{ user.userId }}</strong>
            <div class="subline">
              <span>{{ user.email }}</span>
              <span>{{ user.accountType }}</span>
              <span>{{ user.accountStatus }}</span>
            </div>
            <p v-if="user.activeSanctionId" class="muted">{{ user.activeSanctionType }} · {{ user.activeSanctionReason }}</p>
            <button
              v-if="user.accountType !== 'ADMIN'"
              class="ghost-button"
              type="button"
              @click="pickSanctionUser(user)"
            >
              제재 대상
            </button>
          </article>
        </div>
        <form class="log-list" @submit.prevent="requestSanctionCreate">
          <h3>제재 적용</h3>
          <label class="field">
            <span>대상</span>
            <input :value="sanctionForm.nickname ? `${sanctionForm.nickname} (#${sanctionForm.userId})` : ''" disabled>
          </label>
          <label class="field">
            <span>유형</span>
            <select v-model="sanctionForm.sanctionType">
              <option value="TEMP_SUSPENDED">임시 정지</option>
              <option value="PERM_SUSPENDED">영구 정지</option>
            </select>
          </label>
          <label v-if="sanctionForm.sanctionType === 'TEMP_SUSPENDED'" class="field">
            <span>종료 시각</span>
            <input v-model="sanctionForm.sanctionUntil" type="datetime-local">
          </label>
          <label class="field">
            <span>사유</span>
            <textarea v-model="sanctionForm.reason" rows="4" maxlength="1000" required></textarea>
          </label>
          <button class="primary-button" type="submit" :disabled="sanctioning">
            {{ sanctioning ? '적용 중' : '제재 적용' }}
          </button>
          <div v-if="isPendingAdminAction('sanction-create', sanctionForm.userId)" class="confirm-inline danger-confirm admin-confirm">
            <span>{{ sanctionForm.nickname || `#${sanctionForm.userId}` }} 계정에 제재를 적용합니다.</span>
            <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
            <button class="primary-button" type="button" @click="createSanction">제재 확정</button>
          </div>
        </form>
      </div>
      <div class="log-list">
        <h3>활성 제재</h3>
        <article v-for="sanction in sanctions" :key="sanction.sanctionId" class="log-row">
          <strong>{{ sanction.nickname }} · {{ sanction.sanctionType }}</strong>
          <p>{{ sanction.reason }}</p>
          <div class="subline">
            <span>{{ sanction.email }}</span>
            <span v-if="sanction.sanctionUntil">종료 {{ formatDate(sanction.sanctionUntil) }}</span>
            <span>{{ formatDate(sanction.createdAt) }}</span>
          </div>
          <label class="field">
            <span>해제 사유</span>
            <input v-model="revokeDrafts[sanction.sanctionId]" maxlength="1000" placeholder="운영자 확인 후 해제">
          </label>
          <button
            class="ghost-button"
            type="button"
            :disabled="sanctionRevokeId === sanction.sanctionId"
            @click="requestAdminAction('sanction-revoke', sanction.sanctionId)"
          >
            {{ sanctionRevokeId === sanction.sanctionId ? '해제 중' : '제재 해제' }}
          </button>
          <div v-if="isPendingAdminAction('sanction-revoke', sanction.sanctionId)" class="confirm-inline success-confirm admin-confirm">
            <span>{{ sanction.nickname }} 계정의 활성 제재를 해제합니다.</span>
            <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
            <button class="primary-button" type="button" @click="revokeSanction(sanction)">해제 확정</button>
          </div>
        </article>
        <p v-if="!loadingSanctions && sanctions.length === 0" class="muted">활성 제재가 없습니다.</p>
      </div>
    </section>

    <form v-if="activeAdminPanel === 'notifications' && hasPermission('NOTIFICATION_SEND')" class="form-panel" @submit.prevent="requestNoticeSend">
      <div class="form-head">
        <div>
          <span class="eyebrow">Notification</span>
          <h2>관리자 알림 발송</h2>
        </div>
        <button class="primary-button" type="submit" :disabled="sendingNotice">
          {{ sendingNotice ? '발송 중' : '발송' }}
        </button>
      </div>
      <p v-if="noticeSaved" class="notice-text">{{ noticeSaved }}</p>
      <p v-if="sectionErrors.notifications" class="error-text">{{ sectionErrors.notifications }}</p>
      <div v-if="isPendingAdminAction('notice', 'send')" class="confirm-inline admin-confirm">
        <span>{{ notificationPreview ? `${notificationPreview.recipientCount}명에게` : '선택한 대상에게' }} 관리자 알림을 발송합니다.</span>
        <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
        <button class="primary-button" type="button" :disabled="sendingNotice" @click="sendNotice">
          {{ sendingNotice ? '발송 중' : '발송 확정' }}
        </button>
      </div>
      <div class="form-grid">
        <label class="field">
          <span>대상</span>
          <select v-model="noticeForm.targetScope">
            <option value="ALL">전체</option>
            <option value="ACCOUNT_TYPE">계정 유형</option>
            <option value="USER">특정 사용자</option>
            <option value="TEAM">팀</option>
          </select>
        </label>
        <label v-if="noticeForm.targetScope === 'ACCOUNT_TYPE'" class="field">
          <span>계정 유형</span>
          <select v-model="noticeForm.accountType">
            <option value="USER">일반 사용자</option>
            <option value="COMPANY">회사</option>
            <option value="ADMIN">관리자</option>
          </select>
        </label>
        <label v-if="noticeForm.targetScope === 'USER'" class="field">
          <span>사용자 ID</span>
          <input v-model="noticeForm.userIdsText" placeholder="1, 3, 99">
        </label>
        <label v-if="noticeForm.targetScope === 'TEAM'" class="field">
          <span>팀 ID</span>
          <input v-model="noticeForm.teamId" min="1" type="number">
        </label>
        <label class="field">
          <span>템플릿</span>
          <select v-model="noticeForm.templateId" @change="applyNotificationTemplate">
            <option value="">직접 입력</option>
            <option v-for="template in notificationTemplates" :key="template.templateId" :value="template.templateId">
              {{ template.displayName }}
            </option>
          </select>
        </label>
        <div class="field action-field">
          <span>대상 확인</span>
          <button class="ghost-button" type="button" :disabled="previewingNotice" @click="previewNoticeRecipients">
            {{ previewingNotice ? '확인 중' : '대상 수 확인' }}
          </button>
        </div>
        <label class="field wide">
          <span>제목</span>
          <input v-model="noticeForm.title" maxlength="150" required>
        </label>
        <label class="field wide">
          <span>본문</span>
          <textarea v-model="noticeForm.body" rows="4" maxlength="500" required></textarea>
        </label>
      </div>
      <div v-if="notificationPreview" class="notice-preview">
        <strong>{{ notificationPreview.recipientCount }}명 대상</strong>
        <span v-if="notificationPreview.templateName">{{ notificationPreview.templateName }}</span>
        <span v-if="notificationPreview.sampleRecipientIds?.length">샘플 ID {{ notificationPreview.sampleRecipientIds.join(', ') }}</span>
      </div>
      <div class="compact-list">
        <div class="row-head">
          <h3>최근 발송 배치</h3>
          <button class="ghost-button" type="button" :disabled="loadingNotificationBatches" @click="loadNotificationAdminData">
            {{ loadingNotificationBatches ? '조회 중' : '새로고침' }}
          </button>
        </div>
        <article v-for="batch in notificationBatches" :key="batch.batchId" class="log-row">
          <strong>#{{ batch.batchId }} · {{ batch.title }}</strong>
          <div class="subline">
            <span>{{ batch.targetScope }}</span>
            <span>{{ batch.sentCount }}/{{ batch.recipientCount }}명</span>
            <span>{{ batch.chunkCount }}개 묶음</span>
            <span>{{ batch.templateName || '직접 입력' }}</span>
            <span>{{ formatDate(batch.createdAt) }}</span>
          </div>
        </article>
        <p v-if="!loadingNotificationBatches && notificationBatches.length === 0" class="muted">발송 배치가 없습니다.</p>
      </div>
    </form>

    <section v-if="activeAdminPanel === 'permissions' && hasPermission('ADMIN_PERMISSION_MANAGE')" class="form-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">Admin Access</span>
          <h2>관리자 세부 권한</h2>
        </div>
      </div>
      <p v-if="permissionSaved" class="notice-text">{{ permissionSaved }}</p>
      <p v-if="sectionErrors.permissions" class="error-text">{{ sectionErrors.permissions }}</p>
      <article v-for="user in adminUsers" :key="user.userId" class="permission-card">
        <div class="row-head">
          <div>
            <strong>{{ user.nickname }}</strong>
            <div class="subline">
              <span>{{ user.email }}</span>
              <span>{{ user.accountStatus }}</span>
            </div>
          </div>
          <button
            class="primary-button"
            type="button"
            :disabled="savingPermissionUserId === user.userId"
            @click="requestAdminAction('permission', user.userId)"
          >
            {{ savingPermissionUserId === user.userId ? '저장 중' : '저장' }}
          </button>
        </div>
        <div class="permission-grid">
          <label v-for="permission in permissionCatalog" :key="permission.code" class="check-row">
            <input v-model="permissionDrafts[user.userId]" type="checkbox" :value="permission.code">
            <span>
              <strong>{{ permission.label }}</strong>
              <small>{{ permission.description }}</small>
            </span>
          </label>
        </div>
        <div v-if="isPendingAdminAction('permission', user.userId)" class="confirm-inline admin-confirm">
          <span>{{ user.nickname }} 관리자 권한 구성을 저장합니다.</span>
          <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
          <button class="primary-button" type="button" @click="savePermissions(user)">저장 확정</button>
        </div>
      </article>
    </section>

    <section v-if="activeAdminPanel === 'demo-access' && !hasPermission('DEMO_ACCESS_MANAGE')" class="form-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">Demo Access</span>
          <h2>접근 코드 관리</h2>
        </div>
      </div>
      <p class="muted">접근 코드 관리 권한이 없습니다.</p>
    </section>

    <section v-if="activeAdminPanel === 'demo-access' && hasPermission('DEMO_ACCESS_MANAGE')" class="form-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">Demo Access</span>
          <h2>접근 코드 관리</h2>
        </div>
        <button class="ghost-button" type="button" :disabled="loadingDemoAccessCodes" @click="loadDemoAccessCodes">
          {{ loadingDemoAccessCodes ? '조회 중' : '목록 새로고침' }}
        </button>
      </div>
      <p v-if="demoAccessSaved" class="notice-text">{{ demoAccessSaved }}</p>
      <p v-if="sectionErrors.demoAccess" class="error-text">{{ sectionErrors.demoAccess }}</p>

      <form class="admin-inline-form" @submit.prevent="createDemoAccessCode">
        <div class="form-grid">
          <label class="field">
            <span>표시 이름</span>
            <input v-model="demoAccessForm.label" maxlength="100" placeholder="시연 초대, 점검 허용 등" required>
          </label>
          <label class="field">
            <span>시작 시각</span>
            <input v-model="demoAccessForm.startsAt" type="datetime-local">
          </label>
          <label class="field">
            <span>만료 시각</span>
            <input v-model="demoAccessForm.expiresAt" type="datetime-local" required>
          </label>
          <label class="field">
            <span>최대 사용 횟수</span>
            <input v-model="demoAccessForm.maxUses" min="1" type="number" placeholder="제한 없음">
          </label>
        </div>
        <div class="row-actions">
          <button class="ghost-button" type="button" @click="resetDemoAccessForm">초기화</button>
          <button class="primary-button" type="submit" :disabled="creatingDemoAccessCode">
            {{ creatingDemoAccessCode ? '생성 중' : '새 접근 코드 생성' }}
          </button>
        </div>
      </form>

      <div v-if="latestDemoAccessCode?.plainCode" class="log-row">
        <div class="row-head">
          <div>
            <strong>생성된 접근 코드</strong>
            <div class="subline">
              <span>{{ latestDemoAccessCode.label }}</span>
              <span>이 코드는 지금만 표시됩니다</span>
            </div>
          </div>
          <button class="ghost-button" type="button" @click="copyLatestDemoAccessCode">복사</button>
        </div>
        <input class="code-display-input" :value="latestDemoAccessCode.plainCode" readonly>
      </div>

      <div class="log-list">
        <h3>접근 코드 목록</h3>
        <article v-for="code in demoAccessCodes" :key="code.codeId" class="log-row">
          <div class="row-head">
            <div>
              <strong>#{{ code.codeId }} · {{ code.label }}</strong>
              <div class="subline">
                <span :class="['permission-tag', demoAccessStatusClass(code.effectiveStatus)]">{{ demoAccessStatusLabel(code.effectiveStatus) }}</span>
                <span>{{ formatDate(code.startsAt) || '즉시 시작' }}</span>
                <span>{{ formatDate(code.expiresAt) }}</span>
                <span>{{ code.usedCount || 0 }}/{{ code.maxUses || '무제한' }}</span>
                <span>생성 {{ code.createdByNickname || code.createdByEmail || `#${code.createdBy || '-'}` }}</span>
              </div>
            </div>
            <div class="row-actions">
              <button
                class="ghost-button"
                type="button"
                :disabled="demoAccessActionId === code.codeId || code.status === 'REVOKED'"
                @click="requestAdminAction('demo-access', code.codeId, 'UPDATE')"
              >
                수정
              </button>
              <button
                class="ghost-button danger"
                type="button"
                :disabled="demoAccessActionId === code.codeId || code.status === 'REVOKED'"
                @click="requestAdminAction('demo-access', code.codeId, 'REVOKE')"
              >
                폐기
              </button>
            </div>
          </div>

          <template v-if="ensureDemoAccessDraft(code)">
            <div v-if="isPendingAdminAction('demo-access', code.codeId, 'UPDATE')" class="confirm-inline admin-confirm">
              <label class="field">
                <span>표시 이름</span>
                <input v-model="demoAccessDrafts[code.codeId].label" maxlength="100">
              </label>
              <label class="field">
                <span>시작 시각</span>
                <input v-model="demoAccessDrafts[code.codeId].startsAt" type="datetime-local">
              </label>
              <label class="field">
                <span>만료 시각</span>
                <input v-model="demoAccessDrafts[code.codeId].expiresAt" type="datetime-local">
              </label>
              <label class="field">
                <span>최대 사용 횟수</span>
                <input v-model="demoAccessDrafts[code.codeId].maxUses" min="1" type="number">
              </label>
              <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
              <button class="primary-button" type="button" :disabled="demoAccessActionId === code.codeId" @click="updateDemoAccessCode(code)">
                {{ demoAccessActionId === code.codeId ? '저장 중' : '저장 확정' }}
              </button>
            </div>

            <div v-if="isPendingAdminAction('demo-access', code.codeId, 'REVOKE')" class="confirm-inline danger-confirm admin-confirm">
              <span>이 접근 코드를 폐기합니다.</span>
              <label class="field">
                <span>폐기 사유</span>
                <input v-model="demoAccessRevokeDrafts[code.codeId]" maxlength="500" placeholder="점검 종료">
              </label>
              <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
              <button class="primary-button" type="button" :disabled="demoAccessActionId === code.codeId" @click="revokeDemoAccessCode(code)">
                {{ demoAccessActionId === code.codeId ? '폐기 중' : '폐기 확정' }}
              </button>
            </div>
          </template>

          <div v-if="code.revokedAt" class="subline">
            <span>폐기 {{ formatDate(code.revokedAt) }}</span>
            <span>{{ code.revokeReason || '사유 없음' }}</span>
          </div>
        </article>
        <p v-if="!loadingDemoAccessCodes && demoAccessCodes.length === 0" class="muted">등록된 접근 코드가 없습니다.</p>
      </div>
    </section>

    <form v-if="activeAdminPanel === 'policy' && hasPermission('SCORE_POLICY')" class="form-panel" @submit.prevent="requestPolicyPublish">
      <div class="form-head">
        <div>
          <span class="eyebrow">Matching Score</span>
          <h2>매칭 점수 정책</h2>
        </div>
        <div class="row-actions">
          <button class="ghost-button" type="button" :disabled="previewingPolicy || savingPolicy" @click="previewPolicy">
            {{ previewingPolicy ? '계산 중' : '영향 미리보기' }}
          </button>
          <button class="primary-button" type="submit" :disabled="savingPolicy || previewingPolicy">
            {{ savingPolicy ? '발행 중' : '새 버전 발행' }}
          </button>
        </div>
      </div>
      <p v-if="policySaved" class="notice-text">{{ policySaved }}</p>
      <p v-if="sectionErrors.policy" class="error-text">{{ sectionErrors.policy }}</p>
      <div v-if="isPendingAdminAction('policy', 'publish')" class="confirm-inline admin-confirm">
        <span>매칭 점수 정책을 새 버전으로 발행합니다. 점수 미리보기와 변경 사유를 확인해주세요.</span>
        <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
        <button class="primary-button" type="button" :disabled="savingPolicy || previewingPolicy" @click="publishPolicy">
          {{ savingPolicy ? '발행 중' : '발행 확정' }}
        </button>
      </div>
      <div class="form-grid">
        <label class="field">
          <span>정책명</span>
          <input v-model="policyDraft.policyName" maxlength="100" required>
        </label>
        <label class="field">
          <span>현재 버전</span>
          <input :value="policyDraft.version ? `v${policyDraft.version}` : ''" disabled>
        </label>
        <label class="field wide">
          <span>설명</span>
          <input v-model="policyDraft.description" maxlength="255">
        </label>
        <label class="field wide">
          <span>변경 사유</span>
          <textarea v-model="policyDraft.changeReason" rows="3" maxlength="500" required></textarea>
        </label>
      </div>
      <section v-for="group in policyGroups" :key="group.code" class="policy-group">
        <div class="row-head">
          <strong>{{ group.label }}</strong>
          <span :class="['policy-sum', Math.abs(policyGroupSums[group.code] - 100) > 0.01 ? 'warn' : '']">
            합계 {{ policyGroupSums[group.code].toFixed(2) }}
          </span>
        </div>
        <div class="policy-table-wrap">
          <table class="policy-table">
            <thead>
              <tr>
                <th>항목</th>
                <th>코드</th>
                <th>가중치</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in policyItems(group.code)" :key="`${item.scoreGroup}-${item.elementCode}`">
                <td>{{ item.displayName }}</td>
                <td>{{ item.elementCode }}</td>
                <td>
                  <input v-model.number="item.weight" min="0" max="100" step="0.01" type="number">
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
      <section v-if="policyPreview" class="log-list">
        <h3>정책 영향 미리보기</h3>
        <div class="metric-strip">
          <div class="metric">
            <span>평균 점수</span>
            <strong>{{ policyPreview.summary?.beforeAverage }} → {{ policyPreview.summary?.afterAverage }}</strong>
          </div>
          <div class="metric">
            <span>PRIMARY</span>
            <strong>{{ policyPreview.summary?.beforePrimary }} → {{ policyPreview.summary?.afterPrimary }}</strong>
          </div>
          <div class="metric">
            <span>상향/하향</span>
            <strong>+{{ policyPreview.summary?.promotedToPrimary || 0 }} / -{{ policyPreview.summary?.demotedFromPrimary || 0 }}</strong>
          </div>
        </div>
        <div class="policy-table-wrap">
          <table class="policy-table">
            <thead>
              <tr>
                <th>후보</th>
                <th>맥락</th>
                <th>현재</th>
                <th>변경 후</th>
                <th>차이</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="sample in policyPreview.samples || []" :key="`${sample.slotId}-${sample.profileId}`">
                <td>{{ sample.profileName }}</td>
                <td>{{ sample.teamName }} · {{ sample.roleName }}</td>
                <td>{{ sample.beforeScore }} · {{ sample.beforeExposureType }}</td>
                <td>{{ sample.afterScore }} · {{ sample.afterExposureType }}</td>
                <td>{{ scoreDelta(sample.delta) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="(policyPreview.samples || []).length === 0" class="muted">비교할 모집 후보가 없습니다.</p>
      </section>
      <section class="log-list">
        <h3>정책 변경 이력</h3>
        <article v-for="history in policyHistory" :key="history.historyId" class="log-row">
          <div class="row-head">
            <strong>v{{ history.version }} · {{ history.policyName }}</strong>
            <button
              v-if="policyRollbackTarget(history)"
              class="ghost-button"
              type="button"
              :disabled="rollingBackPolicyId === policyRollbackTarget(history).policyId"
              @click="requestPolicyRollback(history)"
            >
              {{ rollingBackPolicyId === policyRollbackTarget(history).policyId ? '롤백 중' : '롤백' }}
            </button>
          </div>
          <p>{{ history.changeReason }}</p>
          <div class="subline">
            <span>{{ history.changedByNickname || history.changedByEmail }}</span>
            <span>{{ formatDate(history.createdAt) }}</span>
          </div>
          <details>
            <summary>변경 JSON</summary>
            <pre class="json-block">{{ formatJson(history.afterJson) }}</pre>
          </details>
          <div v-if="policyRollbackTarget(history) && isPendingAdminAction('policy-rollback', policyRollbackTarget(history).policyId)" class="confirm-inline danger-confirm admin-confirm policy-confirm">
            <span>v{{ policyRollbackTarget(history).version || '?' }} 정책 기준으로 새 활성 버전을 발행합니다.</span>
            <label class="field">
              <span>롤백 사유</span>
              <input v-model="policyRollbackReason" maxlength="500">
            </label>
            <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
            <button class="primary-button" type="button" @click="rollbackPolicy(history)">롤백 확정</button>
          </div>
        </article>
      </section>
    </form>

    <section v-if="activeAdminPanel === 'logs' && hasPermission('LOG_VIEW')" class="form-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">Logs</span>
          <h2>감사/운영 로그</h2>
        </div>
        <button class="ghost-button" type="button" :disabled="loadingLogs" @click="loadLogs">
          {{ loadingLogs ? '조회 중' : '새로고침' }}
        </button>
      </div>
      <p v-if="sectionErrors.logs" class="error-text">{{ sectionErrors.logs }}</p>
      <div class="form-grid">
        <label class="field">
          <span>감사 액션</span>
          <input v-model="logFilters.actionType" placeholder="ADMIN_NOTIFICATION_SENT">
        </label>
        <label class="field">
          <span>감사 대상</span>
          <input v-model="logFilters.targetType" placeholder="BOARD_POST">
        </label>
        <label class="field">
          <span>행위자 ID</span>
          <input v-model="logFilters.actorUserId" min="1" type="number">
        </label>
        <label class="field">
          <span>운영 레벨</span>
          <select v-model="logFilters.logLevel">
            <option value="">전체</option>
            <option value="INFO">INFO</option>
            <option value="WARN">WARN</option>
            <option value="ERROR">ERROR</option>
          </select>
        </label>
        <label class="field wide">
          <span>운영 이벤트</span>
          <input v-model="logFilters.eventCode" placeholder="CONTENT_MODERATED">
        </label>
      </div>
      <div class="admin-columns">
        <div class="log-list">
          <h3>감사 로그</h3>
          <article v-for="log in auditLogs" :key="log.auditLogId" class="log-row">
            <strong>{{ log.actionType }}</strong>
            <div class="subline">
              <span>{{ log.actorNickname || log.actorEmail || 'system' }}</span>
              <span>{{ log.targetType }} #{{ log.targetId || '-' }}</span>
              <span>{{ formatDate(log.createdAt) }}</span>
            </div>
            <details v-if="log.beforeJson || log.afterJson">
              <summary>JSON</summary>
              <pre v-if="log.beforeJson" class="json-block">{{ formatJson(log.beforeJson) }}</pre>
              <pre v-if="log.afterJson" class="json-block">{{ formatJson(log.afterJson) }}</pre>
            </details>
          </article>
        </div>
        <div class="log-list">
          <h3>운영 로그</h3>
          <article v-for="log in operationLogs" :key="log.operationLogId" class="log-row">
            <strong>{{ log.logLevel }} · {{ log.eventCode }}</strong>
            <p>{{ log.message }}</p>
            <div class="subline">
              <span>{{ formatDate(log.createdAt) }}</span>
            </div>
            <details v-if="log.contextJson">
              <summary>JSON</summary>
              <pre class="json-block">{{ formatJson(log.contextJson) }}</pre>
            </details>
          </article>
        </div>
      </div>
    </section>

    <section v-if="activeAdminPanel === 'applications' && hasPermission('COMPANY_APPROVAL')" class="stack">
      <div class="form-head">
        <div>
          <span class="eyebrow">Company</span>
          <h2>회사 승인 신청</h2>
        </div>
      </div>
      <p v-if="applicationSaved" class="notice-text">{{ applicationSaved }}</p>
      <p v-if="sectionErrors.applications" class="error-text">{{ sectionErrors.applications }}</p>
      <article v-for="application in applications" :key="application.companyApplicationId" class="list-panel">
        <div>
          <strong>{{ application.companyName }}</strong>
          <p>{{ application.companyIntro }}</p>
          <div class="subline">
            <span>{{ application.status }}</span>
            <span>{{ application.email }}</span>
            <span>{{ application.managerName }}</span>
            <span>서류 {{ application.documentCount || 0 }}개</span>
            <span v-if="application.latestDocumentAt">최근 {{ formatDate(application.latestDocumentAt).slice(0, 10) }}</span>
          </div>
          <div class="row-actions">
            <button
              class="ghost-button"
              type="button"
              :disabled="loadingCompanyDocumentApplicationId === application.companyApplicationId"
              @click="loadCompanyDocuments(application)"
            >
              {{ loadingCompanyDocumentApplicationId === application.companyApplicationId ? '조회 중' : '서류 조회' }}
            </button>
          </div>
          <div v-if="companyDocumentLists[application.companyApplicationId]" class="document-list">
            <div
              v-for="document in companyDocumentLists[application.companyApplicationId]"
              :key="document.documentId"
              class="document-row"
            >
              <div class="row-head">
                <div>
                  <strong>{{ document.originalName }}</strong>
                  <div class="subline">
                    <span>{{ documentTypeLabel(document.documentType) }}</span>
                    <span>{{ formatBytes(document.sizeBytes) }}</span>
                    <span>{{ formatDate(document.uploadedAt) }}</span>
                  </div>
                </div>
                <button
                  class="ghost-button"
                  type="button"
                  :disabled="downloadingCompanyDocumentId === document.documentId"
                  @click="downloadCompanyDocument(document)"
                >
                  {{ downloadingCompanyDocumentId === document.documentId ? '다운로드 중' : '다운로드' }}
                </button>
              </div>
            </div>
            <p v-if="companyDocumentLists[application.companyApplicationId].length === 0" class="muted">업로드된 서류가 없습니다.</p>
          </div>
        </div>
        <div class="row-actions">
          <button
            class="ghost-button"
            type="button"
            :disabled="applicationActionId === application.companyApplicationId"
            @click="requestCompanyDecision(application, 'REJECTED')"
          >
            거절
          </button>
          <button
            class="primary-button"
            type="button"
            :disabled="applicationActionId === application.companyApplicationId"
            @click="requestCompanyDecision(application, 'APPROVED')"
          >
            승인
          </button>
        </div>
        <div
          v-if="isPendingAdminAction('company', application.companyApplicationId, 'REJECTED') && ensureCompanyDecisionDraft(application, 'REJECTED') !== null"
          class="confirm-inline danger-confirm admin-confirm"
        >
          <span>{{ application.companyName }} 회사 승인 신청을 거절합니다.</span>
          <label class="field">
            <span>처리 사유</span>
            <input v-model="companyDecisionDrafts[application.companyApplicationId].REJECTED" maxlength="500" required>
          </label>
          <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
          <button class="primary-button" type="button" @click="decide(application, 'REJECTED')">거절 확정</button>
        </div>
        <div
          v-if="isPendingAdminAction('company', application.companyApplicationId, 'APPROVED') && ensureCompanyDecisionDraft(application, 'APPROVED') !== null"
          class="confirm-inline success-confirm admin-confirm"
        >
          <span>{{ application.companyName }} 회사 계정을 승인합니다.</span>
          <label class="field">
            <span>처리 사유</span>
            <input v-model="companyDecisionDrafts[application.companyApplicationId].APPROVED" maxlength="500" required>
          </label>
          <button class="ghost-button" type="button" @click="cancelAdminAction">취소</button>
          <button class="primary-button" type="button" @click="decide(application, 'APPROVED')">승인 확정</button>
        </div>
      </article>
    </section>
        </section>
      </main>
    </div>
  </section>
</template>
