<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FollowListDialog from '../components/follows/FollowListDialog.vue'
import {
  defaultPortfolioImage,
  defaultProfileImage,
  defaultTeamImage,
  defaultWorkImage
} from '../constants/defaultImages'
import { setToken, slateApi } from '../services/api'

const props = defineProps({ currentUser: Object })
const emit = defineEmits(['login'])
const route = useRoute()
const router = useRouter()

const requestedCodeGroups = [
  'PROFILE_VISIBILITY',
  'ACTIVITY_STATUS',
  'EXPERIENCE_LEVEL',
  'JOIN_AVAILABILITY',
  'COLLABORATION_STATUS',
  'COLLABORATION_CONDITION',
  'TRAVEL_RANGE',
  'DURATION',
  'EQUIPMENT_STATUS'
]

const fallbackCodes = {
  PROFILE_VISIBILITY: [{ code: 'PUBLIC', displayName: '공개' }, { code: 'PRIVATE', displayName: '비공개' }],
  JOIN_AVAILABILITY: [
    { code: 'IMMEDIATE', displayName: '즉시' },
    { code: 'WITHIN_1W', displayName: '1주 이내' },
    { code: 'WITHIN_2W', displayName: '2주 이내' },
    { code: 'WITHIN_1M', displayName: '1개월 이내' },
    { code: 'NEGOTIABLE', displayName: '협의' }
  ],
  TRAVEL_RANGE: [
    { code: 'KM_30', displayName: '30km' },
    { code: 'KM_100', displayName: '100km' },
    { code: 'ANYWHERE', displayName: '전국' }
  ],
  EQUIPMENT_STATUS: [{ code: 'NOT_ENTERED', displayName: '미입력' }]
}

const ageBands = [
  { code: 'PRIVATE', displayName: '비공개' },
  { code: 'TWENTIES', displayName: '20대' },
  { code: 'THIRTIES', displayName: '30대' },
  { code: 'FORTIES_PLUS', displayName: '40대 이상' }
]

const participationModes = [
  { code: 'HYBRID', displayName: '혼합' },
  { code: 'OFFLINE', displayName: '오프라인' },
  { code: 'REMOTE', displayName: '원격' }
]

const portfolioSourceTypes = [
  { code: 'MANUAL', displayName: '직접 입력' },
  { code: 'PUBLIC_DATA_MANUAL', displayName: '공공데이터 직접 입력' }
]

const publicDataTypes = [
  { code: '', displayName: '전체' },
  { code: 'MOVIE', displayName: '영화' },
  { code: 'PERSON', displayName: '인물' },
  { code: 'COMPANY', displayName: '회사' }
]

const profile = ref(null)
const codeGroups = ref({})
const roleGroups = ref([])
const genres = ref([])
const regions = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const saved = ref('')
const referencesLoaded = ref(false)
const portfolioSaving = ref(false)
const portfolioError = ref('')
const portfolioSaved = ref('')
const editingPortfolioId = ref(null)
const pendingProfileDelete = ref(false)
const pendingPortfolioDeleteId = ref(null)
const publicDataLoading = ref(false)
const publicDataError = ref('')
const publicDataResults = ref([])
const draggedRoleId = ref(null)
const workFiles = ref([])
const workFileQuota = ref(null)
const workFileFilter = ref('ALL')
const selectedWorkFile = ref(null)
const workFileDuration = ref(null)
const filesLoading = ref(false)
const fileSaving = ref(false)
const fileError = ref('')
const fileSaved = ref('')
const fileActionId = ref(null)
const pendingFileDeleteId = ref(null)
const youtubeUrl = ref('')
const youtubePreview = ref(null)
const youtubePreviewLoading = ref(false)
const youtubeError = ref('')
const youtubeSaved = ref('')
const pendingYoutubeDeleteId = ref(null)
const followSummary = ref(null)
const followSummaryLoading = ref(false)
const followSummaryError = ref('')
const followDialogOpen = ref(false)
const followDialogMode = ref('followers')
const profileTeams = ref([])
const profileWorks = ref([])
const profileTeamsLoading = ref(false)
const profileWorksLoading = ref(false)
const profileTeamsError = ref('')
const profileWorksError = ref('')
const profileImageFile = ref(null)
const profileImagePreview = ref('')
const profileImageDelete = ref(false)
const profileImageFailed = ref(false)
const portfolioImageFile = ref(null)
const portfolioImagePreview = ref('')
const portfolioThumbnailMode = ref('NONE')
const portfolioThumbnailLoading = ref(false)
let followSummaryRequestId = 0
let profileLoadRequestId = 0

const form = reactive({
  displayName: '',
  shortIntro: '',
  detailIntro: '',
  visibility: 'PUBLIC',
  activityStatus: 'VISIBLE',
  regionId: null,
  roleIds: [],
  genreIds: [],
  experienceLevel: 'Y0_3',
  joinAvailability: 'IMMEDIATE',
  collaborationStatus: 'AVAILABLE',
  collaborationConditionCodes: [],
  travelRange: 'KM_30',
  preferredDuration: 'WITHIN_3M',
  equipmentStatus: 'NOT_ENTERED',
  ageBand: 'PRIVATE',
  participationMode: 'HYBRID'
})

const portfolioForm = reactive({
  title: '',
  roleName: '',
  description: '',
  sourceType: 'MANUAL',
  externalSourceName: '',
  externalReferenceId: '',
  url: '',
  thumbnailUrl: '',
  sortOrder: 0,
  kobisMovieCd: '',
  kobisMovieNm: '',
  kobisMovieNmEn: '',
  kobisPrdtYear: '',
  kobisOpenDt: '',
  kobisGenreAlt: '',
  creditName: ''
})

const accountForm = reactive({
  nickname: '',
  email: '',
  currentPassword: '',
  newPassword: '',
  newPasswordConfirm: '',
  withdrawalPassword: ''
})
const accountEdit = reactive({
  nickname: false,
  email: false,
  password: false
})
const accountSaving = ref(false)
const accountWithdrawing = ref(false)
const accountError = ref('')
const accountSaved = ref('')
const pendingAccountWithdrawal = ref(false)

const publicDataForm = reactive({
  keyword: '',
  itemType: ''
})

const roleOptions = computed(() => roleGroups.value.flatMap((category) =>
  (category.roles || []).map((role) => ({
    ...role,
    roleId: Number(role.roleId),
    name: roleDisplayName(role),
    categoryName: category.name
  }))
))
const selectedRoles = computed(() => form.roleIds
  .map((roleId, index) => {
    const role = roleOptions.value.find((candidate) => candidate.roleId === Number(roleId))
    return role ? { ...role, index } : null
  })
  .filter(Boolean))

const portfolioItems = computed(() => profile.value?.portfolioItems || [])
const routePortfolioId = computed(() => Number(route.params.portfolioId || 0))
const selectedPortfolioItem = computed(() => portfolioItems.value.find(
  (item) => Number(item.portfolioItemId) === routePortfolioId.value
) || null)
const youtubePortfolioItems = computed(() => portfolioItems.value.filter((item) => {
  const url = String(item.url || '').toLowerCase()
  return url.includes('youtube.com/') || url.includes('youtu.be/')
}))
const kobisMovieKeyword = ref('')
const kobisMovieLoading = ref(false)
const kobisMovieResults = ref([])
const selectedKobisMovie = ref(null)
let kobisMovieSearchTimer = null
const dashboardRoles = computed(() => (profile.value?.roles || []).filter((role) => roleDisplayName(role)))
const dashboardGenres = computed(() => profile.value?.genres || [])
const dashboardConditions = computed(() => profile.value?.collaborationConditions || [])
const dashboardTeams = computed(() => profileTeams.value.filter((team) => team.status !== 'ENDED'))
const primaryRoleName = computed(() => roleDisplayName(dashboardRoles.value[0]) || '등록된 역할 없음')
const regionName = computed(() => profile.value?.publicRegionName || '정보 없음')
const activityStatusName = computed(() => {
  const status = profile.value?.activityStatus
  if (!status) return '정보 없음'
  return codeOptions('ACTIVITY_STATUS').find((item) => item.code === status)?.displayName || status
})
const isProfileDashboard = computed(() => route.name === 'profile')
const isProfileSubRoute = computed(() => !isProfileDashboard.value)
const editorTitle = computed(() => {
  if (route.name === 'profile-privacy') return '공개 범위 설정'
  if (route.name === 'profile-account') return '계정 관리'
  if (route.name === 'profile-works') return '내 참여 작품'
  if (route.name === 'profile-recovery') return '삭제/복구 안내'
  if (route.name === 'profile-portfolio') return '포트폴리오 관리'
  if (route.name === 'profile-portfolio-new') return '포트폴리오 등록'
  if (route.name === 'profile-portfolio-detail') return '포트폴리오 상세'
  if (route.name === 'profile-portfolio-edit') return '포트폴리오 수정'
  if (route.name === 'profile-files') return '업로드 파일 관리'
  if (route.name === 'profile-youtube') return 'YouTube 포트폴리오 업로드'
  if (route.name === 'profile-public-data') return '작품 검색으로 추가'
  return profile.value?.profileId ? '프로필 수정' : '프로필 생성'
})
const editorEyebrow = computed(() => {
  if (route.name === 'profile-privacy') return '공개 설정'
  if (route.name === 'profile-account') return '계정'
  if (route.name === 'profile-works') return '작업물'
  if (route.name === 'profile-recovery') return '보호 조치'
  if (route.name === 'profile-portfolio') return '작업 이력'
  if (route.name === 'profile-portfolio-new') return '새 작업 이력'
  if (route.name === 'profile-portfolio-detail') return '작업 이력'
  if (route.name === 'profile-portfolio-edit') return '작업 이력 수정'
  if (route.name === 'profile-files') return '내 저장 공간'
  if (route.name === 'profile-youtube') return 'YouTube 포트폴리오'
  if (route.name === 'profile-public-data') return '대체 검색'
  return profile.value?.profileId ? '수정' : '생성'
})
const showsProfileForm = computed(() => ['profile-edit', 'profile-privacy'].includes(String(route.name)))
const showsPortfolioPanel = computed(() => route.name === 'profile-portfolio')
const showsPortfolioForm = computed(() => ['profile-portfolio-new', 'profile-portfolio-edit'].includes(String(route.name)))
const showsPortfolioDetail = computed(() => route.name === 'profile-portfolio-detail')
const showsPublicDataPanel = computed(() => route.name === 'profile-public-data')
const showsWorksPanel = computed(() => route.name === 'profile-works')
const accountNeedsCurrentPassword = computed(() => accountEdit.email || accountEdit.password)
const subrouteBackRoute = computed(() => {
  if (route.name === 'profile-portfolio-new') return { name: 'profile-portfolio' }
  if (route.name === 'profile-portfolio-edit' && routePortfolioId.value) {
    return { name: 'profile-portfolio-detail', params: { portfolioId: routePortfolioId.value } }
  }
  if (route.name === 'profile-portfolio-detail') return { name: 'profile-portfolio' }
  return { name: 'profile' }
})

function goProfileRoute(name) {
  router.push({ name })
}

function isVerifiedItem(item) {
  return item?.verified === true
}

function verificationStatusLabel(status) {
  return {
    VERIFIED: '검증 완료',
    NOT_VERIFIED: '검증되지 않음',
    AMBIGUOUS: '확인 필요',
    ERROR: '검증 오류'
  }[status] || '검증 정보 없음'
}

function verificationStatusTone(status) {
  if (status === 'VERIFIED') return 'verified'
  if (status === 'AMBIGUOUS') return 'ambiguous'
  if (status === 'ERROR') return 'error'
  return 'unverified'
}

function profileInitial() {
  return String(profile.value?.displayName || props.currentUser?.nickname || '?').trim().slice(0, 1).toUpperCase()
}

function revokePreview(field) {
  if (field.value) URL.revokeObjectURL(field.value)
  field.value = ''
}

function selectImage(event, fileRef, previewRef, deleteRef = null) {
  const file = event.target.files?.[0] || null
  revokePreview(previewRef)
  fileRef.value = file
  if (deleteRef) deleteRef.value = false
  if (file) previewRef.value = URL.createObjectURL(file)
}

function selectProfileImage(event) {
  selectImage(event, profileImageFile, profileImagePreview, profileImageDelete)
}

function removeProfileImageSelection() {
  revokePreview(profileImagePreview)
  profileImageFile.value = null
  profileImageDelete.value = true
}

function selectPortfolioImage(event) {
  selectImage(event, portfolioImageFile, portfolioImagePreview)
  if (portfolioImageFile.value) portfolioThumbnailMode.value = 'UPLOAD'
}

async function usePortfolioYoutubeThumbnail() {
  if (!portfolioForm.url.trim()) {
    portfolioError.value = 'YouTube URL을 먼저 입력해주세요.'
    return
  }
  portfolioThumbnailLoading.value = true
  portfolioError.value = ''
  try {
    const metadata = await slateApi.previewYoutubeVideo(portfolioForm.url.trim())
    if (!metadata?.thumbnailUrl) throw new Error('사용할 수 있는 YouTube 썸네일이 없습니다.')
    portfolioForm.thumbnailUrl = metadata.thumbnailUrl
    portfolioThumbnailMode.value = 'YOUTUBE'
    portfolioImageFile.value = null
    revokePreview(portfolioImagePreview)
  } catch (err) {
    portfolioError.value = err.message
  } finally {
    portfolioThumbnailLoading.value = false
  }
}

function clearPortfolioThumbnail() {
  portfolioThumbnailMode.value = 'NONE'
  portfolioForm.thumbnailUrl = ''
  portfolioImageFile.value = null
  revokePreview(portfolioImagePreview)
}

function teamRoleLabel(role) {
  return { LEADER: '리더', MEMBER: '멤버' }[role] || role || '역할 정보 없음'
}

function roleDisplayName(role) {
  return role?.name || role?.roleName || role?.displayName || ''
}

function teamStatusLabel(status) {
  return {
    RECRUITING: '모집 중',
    RECRUITMENT_CLOSED: '모집 마감',
    IN_PROGRESS: '진행 중',
    CLOSING: '종료 준비',
    ENDED: '종료'
  }[status] || status || '상태 정보 없음'
}

function teamGenreNames(team) {
  return (team?.genres || []).map((genre) => genre?.name).filter(Boolean).join(', ')
}

function workThumbnail(work) {
  return work?.representativeImageUrl || work?.youtubeThumbnailUrl || defaultWorkImage
}

function portfolioThumbnail(item) {
  return item?.uploadedThumbnailUrl || item?.thumbnailUrl || defaultPortfolioImage
}

function teamThumbnail(team) {
  return team?.imageUrl || defaultTeamImage
}

function profileThumbnail() {
  return profile.value?.profileImageUrl && !profileImageFailed.value
    ? profile.value.profileImageUrl
    : defaultProfileImage
}

function workTitle(work) {
  return work?.workTitle || work?.title || '제목 없음'
}

function workDescription(work) {
  return work?.workDescription || work?.description || work?.content || '등록된 설명이 없습니다.'
}

function workMediaLabel(mediaType) {
  return { YOUTUBE: 'YouTube', SERVER_UPLOAD: '업로드 영상', NONE: '미디어 없음' }[mediaType] || mediaType || '미디어 정보 없음'
}

function kobisDirectors(movie) {
  return (movie?.directors || [])
    .map((director) => director?.peopleNm)
    .filter(Boolean)
    .join(', ')
}

function kobisMovieLabel(movie) {
  const title = movie?.movieNm || movie?.providerMovieTitle || movie?.title || ''
  const year = movie?.prdtYear || movie?.providerMovieYear || ''
  return year ? `${title} (${year})` : title
}

function clearKobisSearchTimer() {
  if (!kobisMovieSearchTimer) return
  clearTimeout(kobisMovieSearchTimer)
  kobisMovieSearchTimer = null
}

function onKobisMovieKeywordInput() {
  kobisMovieKeyword.value = portfolioForm.title.trim()
  if (selectedKobisMovie.value && portfolioForm.title !== selectedKobisMovie.value.movieNm) {
    selectedKobisMovie.value = null
    if (portfolioForm.externalSourceName === 'KOBIS') {
      portfolioForm.sourceType = 'MANUAL'
      portfolioForm.externalSourceName = ''
      portfolioForm.externalReferenceId = ''
    }
    portfolioForm.kobisMovieCd = ''
    portfolioForm.kobisMovieNm = ''
    portfolioForm.kobisMovieNmEn = ''
    portfolioForm.kobisPrdtYear = ''
    portfolioForm.kobisOpenDt = ''
    portfolioForm.kobisGenreAlt = ''
  }
  clearKobisSearchTimer()
  if (kobisMovieKeyword.value.length < 2) {
    kobisMovieResults.value = []
    kobisMovieLoading.value = false
    return
  }
  kobisMovieSearchTimer = window.setTimeout(searchKobisMovies, 240)
}

async function searchKobisMovies() {
  const keyword = kobisMovieKeyword.value.trim()
  if (keyword.length < 2) {
    kobisMovieResults.value = []
    return
  }
  kobisMovieLoading.value = true
  try {
    const results = await slateApi.kobisMovieSearch(keyword, 10)
    kobisMovieResults.value = Array.isArray(results) ? results : []
  } catch {
    kobisMovieResults.value = []
  } finally {
    kobisMovieLoading.value = false
  }
}

function selectKobisMovie(movie) {
  portfolioForm.title = movie?.movieNm || portfolioForm.title
  portfolioForm.sourceType = 'PUBLIC_DATA_MANUAL'
  portfolioForm.externalSourceName = 'KOBIS'
  portfolioForm.externalReferenceId = movie?.movieCd || ''
  portfolioForm.kobisMovieCd = movie?.movieCd || ''
  portfolioForm.kobisMovieNm = movie?.movieNm || ''
  portfolioForm.kobisMovieNmEn = movie?.movieNmEn || ''
  portfolioForm.kobisPrdtYear = movie?.prdtYear || ''
  portfolioForm.kobisOpenDt = movie?.openDt || ''
  portfolioForm.kobisGenreAlt = movie?.genreAlt || ''
  selectedKobisMovie.value = { ...movie }
  kobisMovieKeyword.value = portfolioForm.title
  kobisMovieResults.value = []
  clearKobisSearchTimer()
}

function clearSelectedKobisMovie() {
  selectedKobisMovie.value = null
  kobisMovieResults.value = []
  kobisMovieKeyword.value = ''
  if (portfolioForm.externalSourceName === 'KOBIS') {
    portfolioForm.sourceType = 'MANUAL'
    portfolioForm.externalSourceName = ''
    portfolioForm.externalReferenceId = ''
  }
  portfolioForm.kobisMovieCd = ''
  portfolioForm.kobisMovieNm = ''
  portfolioForm.kobisMovieNmEn = ''
  portfolioForm.kobisPrdtYear = ''
  portfolioForm.kobisOpenDt = ''
  portfolioForm.kobisGenreAlt = ''
}

function codeOptions(group) {
  return codeGroups.value[group]?.length ? codeGroups.value[group] : fallbackCodes[group] || []
}

function firstCode(group, fallback) {
  return codeOptions(group)[0]?.code || fallback
}

function selectedCount(items) {
  return Array.isArray(items) ? items.length : 0
}

function toggleNumber(field, value, max) {
  const numeric = Number(value)
  const selected = form[field]
  const index = selected.indexOf(numeric)
  if (index >= 0) {
    selected.splice(index, 1)
    return
  }
  if (!max || selected.length < max) selected.push(numeric)
}

function toggleRole(roleId) {
  const numeric = Number(roleId)
  const index = form.roleIds.indexOf(numeric)
  if (index >= 0) {
    form.roleIds.splice(index, 1)
    return
  }
  if (form.roleIds.length < 5) form.roleIds.push(numeric)
}

function removeRole(roleId) {
  const index = form.roleIds.indexOf(Number(roleId))
  if (index >= 0) form.roleIds.splice(index, 1)
}

function startRoleDrag(roleId) {
  draggedRoleId.value = Number(roleId)
}

function dropRole(targetRoleId) {
  const source = draggedRoleId.value
  draggedRoleId.value = null
  if (!source || source === Number(targetRoleId)) return
  const from = form.roleIds.indexOf(source)
  const to = form.roleIds.indexOf(Number(targetRoleId))
  if (from < 0 || to < 0) return
  form.roleIds.splice(from, 1)
  form.roleIds.splice(to, 0, source)
}

function toggleCode(field, value) {
  const selected = form[field]
  const index = selected.indexOf(value)
  if (index >= 0) selected.splice(index, 1)
  else selected.push(value)
}

function resetForm(data) {
  form.displayName = data?.displayName ?? props.currentUser?.nickname ?? ''
  form.shortIntro = data?.shortIntro ?? ''
  form.detailIntro = data?.detailIntro ?? ''
  form.visibility = data?.visibility ?? firstCode('PROFILE_VISIBILITY', 'PUBLIC')
  form.activityStatus = data?.activityStatus ?? firstCode('ACTIVITY_STATUS', 'VISIBLE')
  form.regionId = data?.regionId ?? null
  form.roleIds = Array.isArray(data?.roles) ? data.roles.map((role) => Number(role.roleId)) : []
  form.genreIds = Array.isArray(data?.genres) ? data.genres.map((genre) => Number(genre.genreId)) : []
  form.experienceLevel = data?.experienceLevel ?? firstCode('EXPERIENCE_LEVEL', 'Y0_3')
  form.joinAvailability = data?.joinAvailability ?? firstCode('JOIN_AVAILABILITY', 'IMMEDIATE')
  form.collaborationStatus = data?.collaborationStatus ?? firstCode('COLLABORATION_STATUS', 'AVAILABLE')
  form.collaborationConditionCodes = Array.isArray(data?.collaborationConditions)
    ? data.collaborationConditions.map((item) => item.conditionCode)
    : []
  form.travelRange = data?.travelRange ?? firstCode('TRAVEL_RANGE', 'KM_30')
  form.preferredDuration = data?.preferredDuration ?? firstCode('DURATION', 'WITHIN_3M')
  form.equipmentStatus = data?.equipmentStatus ?? firstCode('EQUIPMENT_STATUS', 'NOT_ENTERED')
  form.ageBand = data?.ageBand ?? 'PRIVATE'
  form.participationMode = data?.participationMode ?? 'HYBRID'
}

function resetAccountForm() {
  accountForm.nickname = props.currentUser?.nickname || ''
  accountForm.email = props.currentUser?.email || ''
  accountForm.currentPassword = ''
  accountForm.newPassword = ''
  accountForm.newPasswordConfirm = ''
  accountForm.withdrawalPassword = ''
  accountEdit.nickname = false
  accountEdit.email = false
  accountEdit.password = false
}

function clearAccountMessages() {
  accountError.value = ''
  accountSaved.value = ''
}

function isAccountEditActive() {
  return accountEdit.nickname || accountEdit.email || accountEdit.password
}

function activateAccountEdit(field) {
  clearAccountMessages()
  if (field === 'nickname') accountEdit.nickname = true
  if (field === 'email') accountEdit.email = true
  if (field === 'password') accountEdit.password = true
}

function cancelAccountEdit(field) {
  clearAccountMessages()
  if (field === 'nickname') {
    accountForm.nickname = props.currentUser?.nickname || ''
    accountEdit.nickname = false
  }
  if (field === 'email') {
    accountForm.email = props.currentUser?.email || ''
    accountEdit.email = false
  }
  if (field === 'password') {
    accountForm.newPassword = ''
    accountForm.newPasswordConfirm = ''
    accountEdit.password = false
  }
  if (!accountNeedsCurrentPassword.value) accountForm.currentPassword = ''
}

async function saveAccount() {
  clearAccountMessages()
  if (!isAccountEditActive()) {
    accountError.value = '변경할 항목을 선택해주세요.'
    return
  }
  const nickname = accountForm.nickname.trim()
  const email = accountForm.email.trim()
  const currentPassword = accountForm.currentPassword
  const newPassword = accountEdit.password ? accountForm.newPassword : ''
  const emailChanged = email.toLowerCase() !== String(props.currentUser?.email || '').toLowerCase()
  if (accountEdit.nickname && !nickname) {
    accountError.value = '닉네임을 입력해주세요.'
    return
  }
  if (accountEdit.email && !email) {
    accountError.value = '이메일을 입력해주세요.'
    return
  }
  if (accountEdit.password && !newPassword) {
    accountError.value = '새 비밀번호를 입력해주세요.'
    return
  }
  if (accountEdit.password && newPassword !== accountForm.newPasswordConfirm) {
    accountError.value = '새 비밀번호 확인이 일치하지 않습니다.'
    return
  }
  if ((accountEdit.email || accountEdit.password) && !currentPassword) {
    accountError.value = '이메일 또는 비밀번호를 변경하려면 현재 비밀번호를 입력해주세요.'
    return
  }
  accountSaving.value = true
  try {
    const updated = await slateApi.updateMe({
      nickname,
      email,
      currentPassword: currentPassword || null,
      newPassword: newPassword || null
    })
    if (updated?.accessToken) setToken(updated.accessToken)
    accountForm.currentPassword = ''
    accountForm.newPassword = ''
    accountForm.newPasswordConfirm = ''
    accountEdit.nickname = false
    accountEdit.email = false
    accountEdit.password = false
    accountSaved.value = '계정 정보를 저장했습니다.'
    window.dispatchEvent(new CustomEvent('slate-auth-changed'))
  } catch (err) {
    accountError.value = err.message
  } finally {
    accountSaving.value = false
  }
}

function requestAccountWithdrawal() {
  clearAccountMessages()
  pendingAccountWithdrawal.value = true
}

function cancelAccountWithdrawal() {
  pendingAccountWithdrawal.value = false
  accountForm.withdrawalPassword = ''
}

async function withdrawAccount() {
  clearAccountMessages()
  if (!accountForm.withdrawalPassword) {
    accountError.value = '회원 탈퇴를 진행하려면 현재 비밀번호를 입력해주세요.'
    return
  }
  accountWithdrawing.value = true
  try {
    await slateApi.withdrawMe({ currentPassword: accountForm.withdrawalPassword })
    setToken(null)
    window.dispatchEvent(new CustomEvent('slate-auth-changed'))
    await router.push({ name: 'login' })
  } catch (err) {
    accountError.value = err.message
  } finally {
    accountWithdrawing.value = false
  }
}

async function loadReferences() {
  if (referencesLoaded.value) return
  const [codes, roles, genreRows, regionRows] = await Promise.all([
    slateApi.codes(requestedCodeGroups),
    slateApi.roles(),
    slateApi.genres(),
    slateApi.regions('', 80)
  ])
  codeGroups.value = codes || {}
  roleGroups.value = roles || []
  genres.value = genreRows || []
  regions.value = regionRows || []
  referencesLoaded.value = true
}

async function loadProfile() {
  if (!props.currentUser) return
  const requestId = ++profileLoadRequestId
  loading.value = true
  error.value = ''
  saved.value = ''
  profileTeams.value = []
  profileWorks.value = []
  profileTeamsLoading.value = true
  profileWorksLoading.value = true
  profileTeamsError.value = ''
  profileWorksError.value = ''
  resetFollowState()

  const profilePromise = slateApi.myProfile()
  const referencesPromise = loadReferences()
  const teamsPromise = slateApi.myTeams()
  const worksPromise = slateApi.myBoardWorks(100)
  const supportPromise = Promise.allSettled([referencesPromise, teamsPromise, worksPromise])

  try {
    const data = await profilePromise
    if (requestId !== profileLoadRequestId) return
    profile.value = data
    void loadFollowSummary()
  } catch (err) {
    if (requestId !== profileLoadRequestId) return
    profile.value = null
    error.value = err.message || '프로필을 불러오지 못했습니다.'
  }

  const [referencesResult, teamsResult, worksResult] = await supportPromise
  if (requestId !== profileLoadRequestId) return

  if (referencesResult.status === 'rejected' && !error.value) {
    error.value = referencesResult.reason?.message || '프로필 선택 정보를 불러오지 못했습니다.'
  }
  resetForm(profile.value)

  if (teamsResult.status === 'fulfilled') {
    profileTeams.value = Array.isArray(teamsResult.value) ? teamsResult.value : []
  } else {
    profileTeamsError.value = teamsResult.reason?.message || '참여 팀을 불러오지 못했습니다.'
  }
  profileTeamsLoading.value = false

  if (worksResult.status === 'fulfilled') {
    profileWorks.value = Array.isArray(worksResult.value) ? worksResult.value : []
  } else {
    profileWorksError.value = worksResult.reason?.message || '참여 작품을 불러오지 못했습니다.'
  }
  profileWorksLoading.value = false
  syncPortfolioRoute()
  loading.value = false
}

function resetFollowState() {
  followSummaryRequestId += 1
  followSummary.value = null
  followSummaryLoading.value = false
  followSummaryError.value = ''
  followDialogOpen.value = false
  followDialogMode.value = 'followers'
}

async function loadFollowSummary() {
  const profileId = Number(profile.value?.profileId)
  if (!Number.isFinite(profileId) || profileId <= 0) {
    resetFollowState()
    return
  }
  const requestId = ++followSummaryRequestId
  followSummaryLoading.value = true
  followSummaryError.value = ''
  try {
    const data = await slateApi.followStatus(profileId)
    if (requestId !== followSummaryRequestId) return
    followSummary.value = data
  } catch (err) {
    if (requestId !== followSummaryRequestId) return
    followSummary.value = null
    followSummaryError.value = err.message || '팔로우 정보를 불러오지 못했습니다.'
  } finally {
    if (requestId === followSummaryRequestId) followSummaryLoading.value = false
  }
}

function openFollowDialog(mode) {
  if (!profile.value?.profileId) return
  followDialogMode.value = mode
  followDialogOpen.value = true
}

function closeFollowDialog() {
  followDialogOpen.value = false
}

function buildPayload() {
  return {
    displayName: form.displayName.trim(),
    shortIntro: form.shortIntro.trim(),
    detailIntro: form.detailIntro.trim(),
    visibility: form.visibility,
    activityStatus: form.activityStatus,
    regionId: Number(form.regionId),
    roleIds: form.roleIds.map(Number),
    genreIds: form.genreIds.map(Number),
    experienceLevel: form.experienceLevel,
    joinAvailability: form.joinAvailability,
    collaborationStatus: form.collaborationStatus,
    collaborationConditionCodes: form.collaborationConditionCodes,
    travelRange: form.travelRange,
    preferredDuration: form.preferredDuration,
    equipmentStatus: form.equipmentStatus,
    ageBand: form.ageBand,
    participationMode: form.participationMode
  }
}

async function saveProfile() {
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    const payload = buildPayload()
    if (!payload.regionId || payload.roleIds.length === 0 || payload.genreIds.length === 0 || payload.collaborationConditionCodes.length === 0) {
      throw new Error('필수 선택값을 확인해주세요.')
    }
    profile.value = profile.value?.profileId
      ? await slateApi.updateProfile(profile.value.profileId, payload)
      : await slateApi.createProfile(payload)
    if (profileImageFile.value) {
      const image = await slateApi.uploadEntityImage('profile', profile.value.profileId, profileImageFile.value)
      profile.value = { ...profile.value, profileImageUrl: image.imageUrl }
    } else if (profileImageDelete.value && profile.value.profileImageUrl) {
      await slateApi.deleteEntityImage('profile', profile.value.profileId)
      profile.value = { ...profile.value, profileImageUrl: null }
    }
    revokePreview(profileImagePreview)
    profileImageFile.value = null
    profileImageDelete.value = false
    resetForm(profile.value)
    pendingProfileDelete.value = false
    saved.value = '프로필이 저장되었습니다.'
    window.dispatchEvent(new CustomEvent('slate-profile-changed'))
    await loadFollowSummary()
    await router.push({ name: 'profile' })
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

function requestProfileDelete() {
  pendingProfileDelete.value = true
  error.value = ''
  saved.value = ''
}

function cancelProfileDelete() {
  pendingProfileDelete.value = false
}

async function deleteProfile() {
  if (!profile.value?.profileId) return
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.deleteMyProfile()
    profile.value = null
    resetForm(null)
    resetPortfolioForm()
    publicDataResults.value = []
    pendingProfileDelete.value = false
    pendingPortfolioDeleteId.value = null
    resetFollowState()
    saved.value = '프로필이 삭제되었습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

function resetPortfolioForm(item = null) {
  const isKobis = item?.externalSourceName === 'KOBIS' && item?.externalReferenceId
  const kobisMovieCd = item?.kobisMovieCd || item?.providerMovieCode || (isKobis ? item.externalReferenceId : '')
  const kobisMovieNm = item?.kobisMovieNm || item?.providerMovieTitle || (isKobis ? item.title : '')
  const kobisPrdtYear = item?.kobisPrdtYear || item?.providerMovieYear || ''
  editingPortfolioId.value = item?.portfolioItemId || null
  pendingPortfolioDeleteId.value = null
  clearKobisSearchTimer()
  kobisMovieKeyword.value = ''
  kobisMovieResults.value = []
  selectedKobisMovie.value = isKobis ? {
    movieCd: kobisMovieCd,
    movieNm: kobisMovieNm,
    movieNmEn: item?.kobisMovieNmEn || item?.providerMovieTitleEn || '',
    prdtYear: kobisPrdtYear,
    openDt: item?.kobisOpenDt || item?.providerOpenDate || '',
    genreAlt: item?.kobisGenreAlt || item?.providerGenres || ''
  } : null
  portfolioForm.title = item?.title || ''
  portfolioForm.roleName = item?.roleName || ''
  portfolioForm.description = item?.description || ''
  portfolioForm.sourceType = item?.sourceType === 'PUBLIC_DATA' ? 'PUBLIC_DATA_MANUAL' : (item?.sourceType || 'MANUAL')
  portfolioForm.externalSourceName = item?.externalSourceName || ''
  portfolioForm.externalReferenceId = item?.externalReferenceId || ''
  portfolioForm.url = item?.url || ''
  portfolioForm.thumbnailUrl = item?.thumbnailUrl || ''
  portfolioForm.sortOrder = item?.sortOrder || 0
  portfolioForm.kobisMovieCd = kobisMovieCd || ''
  portfolioForm.kobisMovieNm = kobisMovieNm || ''
  portfolioForm.kobisMovieNmEn = item?.kobisMovieNmEn || item?.providerMovieTitleEn || ''
  portfolioForm.kobisPrdtYear = kobisPrdtYear || ''
  portfolioForm.kobisOpenDt = item?.kobisOpenDt || item?.providerOpenDate || ''
  portfolioForm.kobisGenreAlt = item?.kobisGenreAlt || item?.providerGenres || ''
  portfolioForm.creditName = item?.creditName || ''
  portfolioImageFile.value = null
  revokePreview(portfolioImagePreview)
  portfolioThumbnailMode.value = item?.uploadedThumbnailUrl ? 'UPLOAD' : item?.thumbnailUrl ? 'YOUTUBE' : 'NONE'
}

function portfolioPayload() {
  const hasKobisMovie = Boolean(portfolioForm.kobisMovieCd)
  return {
    title: portfolioForm.title.trim(),
    roleName: portfolioForm.roleName.trim(),
    description: portfolioForm.description.trim(),
    sourceType: hasKobisMovie ? 'PUBLIC_DATA_MANUAL' : portfolioForm.sourceType,
    externalSourceName: hasKobisMovie ? 'KOBIS' : portfolioForm.externalSourceName.trim(),
    externalReferenceId: hasKobisMovie ? portfolioForm.kobisMovieCd : portfolioForm.externalReferenceId.trim(),
    url: portfolioForm.url.trim(),
    thumbnailUrl: portfolioThumbnailMode.value === 'YOUTUBE' ? portfolioForm.thumbnailUrl.trim() : '',
    sortOrder: Number(portfolioForm.sortOrder || 0),
    kobisMovieCd: portfolioForm.kobisMovieCd || null,
    kobisMovieNm: portfolioForm.kobisMovieNm || null,
    kobisMovieNmEn: portfolioForm.kobisMovieNmEn || null,
    kobisPrdtYear: portfolioForm.kobisPrdtYear || null,
    kobisOpenDt: portfolioForm.kobisOpenDt || null,
    kobisGenreAlt: portfolioForm.kobisGenreAlt || null,
    creditName: portfolioForm.creditName.trim() || null
  }
}

async function reloadPortfolio() {
  if (!profile.value?.profileId) return
  const items = await slateApi.myPortfolioItems()
  profile.value = { ...profile.value, portfolioItems: items || [] }
}

async function savePortfolioItem() {
  portfolioSaving.value = true
  portfolioError.value = ''
  portfolioSaved.value = ''
  try {
    if (!profile.value?.profileId) throw new Error('프로필을 먼저 저장해주세요.')
    const payload = portfolioPayload()
    if (!payload.title) throw new Error('포트폴리오 제목을 입력해주세요.')
    let savedItem
    const creating = !editingPortfolioId.value
    if (editingPortfolioId.value) {
      savedItem = await slateApi.updatePortfolioItem(editingPortfolioId.value, payload)
      portfolioSaved.value = '포트폴리오가 수정되었습니다.'
    } else {
      savedItem = await slateApi.createPortfolioItem(payload)
      portfolioSaved.value = '포트폴리오가 추가되었습니다.'
    }
    try {
      if (portfolioThumbnailMode.value === 'UPLOAD' && portfolioImageFile.value) {
        await slateApi.uploadEntityImage('portfolio', savedItem.portfolioItemId, portfolioImageFile.value)
      } else if (portfolioThumbnailMode.value !== 'UPLOAD' && savedItem?.uploadedThumbnailUrl) {
        await slateApi.deleteEntityImage('portfolio', savedItem.portfolioItemId)
      }
    } catch (imageError) {
      if (creating && savedItem?.portfolioItemId) await slateApi.deletePortfolioItem(savedItem.portfolioItemId).catch(() => {})
      throw imageError
    }
    await reloadPortfolio()
    const portfolioItemId = savedItem?.portfolioItemId || editingPortfolioId.value
    if (portfolioItemId) {
      await router.push({ name: 'profile-portfolio-detail', params: { portfolioId: portfolioItemId } })
    } else {
      await router.push({ name: 'profile-portfolio' })
    }
  } catch (err) {
    portfolioError.value = err.message
  } finally {
    portfolioSaving.value = false
  }
}

function editPortfolioItem(item) {
  portfolioError.value = ''
  portfolioSaved.value = ''
  router.push({ name: 'profile-portfolio-edit', params: { portfolioId: item.portfolioItemId } })
}

function requestPortfolioDelete(item) {
  pendingPortfolioDeleteId.value = item.portfolioItemId
  portfolioError.value = ''
  portfolioSaved.value = ''
}

function cancelPortfolioDelete() {
  pendingPortfolioDeleteId.value = null
}

async function removePortfolioItem(item) {
  portfolioSaving.value = true
  portfolioError.value = ''
  portfolioSaved.value = ''
  try {
    await slateApi.deletePortfolioItem(item.portfolioItemId)
    await reloadPortfolio()
    if (editingPortfolioId.value === item.portfolioItemId) resetPortfolioForm()
    pendingPortfolioDeleteId.value = null
    portfolioSaved.value = '포트폴리오가 삭제되었습니다.'
    if (route.name === 'profile-youtube') {
      pendingYoutubeDeleteId.value = null
      youtubeSaved.value = 'YouTube 연결을 삭제했습니다.'
    }
    if (route.name === 'profile-portfolio-detail' || route.name === 'profile-portfolio-edit') {
      await router.push({ name: 'profile-portfolio' })
    }
  } catch (err) {
    portfolioError.value = err.message
  } finally {
    portfolioSaving.value = false
  }
}

function syncPortfolioRoute() {
  portfolioError.value = ''
  portfolioSaved.value = ''
  pendingPortfolioDeleteId.value = null
  if (route.name === 'profile-portfolio-new') {
    resetPortfolioForm()
    return
  }
  if (['profile-portfolio-detail', 'profile-portfolio-edit'].includes(String(route.name))) {
    if (!selectedPortfolioItem.value) {
      resetPortfolioForm()
      portfolioError.value = '포트폴리오를 찾을 수 없거나 접근 권한이 없습니다.'
      return
    }
    if (route.name === 'profile-portfolio-edit') resetPortfolioForm(selectedPortfolioItem.value)
  }
}

function onWorkFileChange(event) {
  const file = event.target.files?.[0]
  selectedWorkFile.value = file || null
  workFileDuration.value = null
  fileError.value = ''
  fileSaved.value = ''
  if (!file) return
  if (file.size > 300 * 1024 * 1024) {
    fileError.value = '영상 파일은 최대 300MB까지 업로드할 수 있습니다.'
    selectedWorkFile.value = null
    event.target.value = ''
    return
  }
  readVideoDuration(file).then((duration) => {
    workFileDuration.value = duration
    if (duration > 180) {
      fileError.value = '서버 업로드 영상은 최대 3분까지 등록할 수 있습니다.'
      selectedWorkFile.value = null
      event.target.value = ''
    }
  }).catch(() => {
    workFileDuration.value = null
  })
}

function readVideoDuration(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.onloadedmetadata = () => {
      const duration = Math.ceil(video.duration || 0)
      URL.revokeObjectURL(url)
      resolve(duration)
    }
    video.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('duration unavailable'))
    }
    video.src = url
  })
}

async function loadMyFiles() {
  if (!props.currentUser) return
  filesLoading.value = true
  fileError.value = ''
  try {
    const result = await slateApi.myWorkFiles({ status: workFileFilter.value, limit: 50 })
    workFiles.value = result.files || []
    workFileQuota.value = result.quota || null
  } catch (err) {
    fileError.value = err.message
  } finally {
    filesLoading.value = false
  }
}

async function uploadSelectedWorkFile() {
  if (!selectedWorkFile.value) return
  fileSaving.value = true
  fileError.value = ''
  fileSaved.value = ''
  try {
    await slateApi.uploadWorkFile(selectedWorkFile.value, { clientDurationSeconds: workFileDuration.value })
    selectedWorkFile.value = null
    workFileDuration.value = null
    fileSaved.value = '파일을 업로드했습니다.'
    await loadMyFiles()
  } catch (err) {
    fileError.value = err.message
  } finally {
    fileSaving.value = false
  }
}

async function deleteMyFile(file) {
  fileActionId.value = file.fileId
  fileError.value = ''
  fileSaved.value = ''
  try {
    await slateApi.deleteWorkFile(file.fileId)
    pendingFileDeleteId.value = null
    fileSaved.value = '파일을 삭제 대기 상태로 전환했습니다.'
    await loadMyFiles()
  } catch (err) {
    fileError.value = err.message
  } finally {
    fileActionId.value = null
  }
}

async function restoreMyFile(file) {
  fileActionId.value = file.fileId
  fileError.value = ''
  fileSaved.value = ''
  try {
    await slateApi.restoreWorkFile(file.fileId)
    fileSaved.value = '파일을 복구했습니다.'
    await loadMyFiles()
  } catch (err) {
    fileError.value = err.message
  } finally {
    fileActionId.value = null
  }
}

function formatBytes(value) {
  const bytes = Number(value || 0)
  if (bytes >= 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024 / 1024).toFixed(1)}GB`
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)}MB`
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)}KB`
  return `${bytes}B`
}

function fileStatusLabel(status) {
  return { ACTIVE: '활성', HELD: '운영 보관', DELETED: '삭제 대기' }[status] || status
}

function fileReferenceCount(file) {
  return Number(file.workReferenceCount || 0) + Number(file.requestReferenceCount || 0)
}

function canDeleteMyFile(file) {
  return file?.status === 'ACTIVE' && fileReferenceCount(file) === 0
}

function canRestoreMyFile(file) {
  return file?.status === 'DELETED' && file.holdReason === 'USER_DELETED'
}

function fileReasonLabel(reason) {
  if (!reason) return ''
  if (reason === 'USER_DELETED') return '직접 삭제'
  if (String(reason).startsWith('ADMIN_DELETED: ')) return String(reason).replace('ADMIN_DELETED: ', '관리자 삭제: ')
  return reason
}

function clearYoutubePreview() {
  youtubePreview.value = null
  youtubeError.value = ''
  youtubeSaved.value = ''
}

async function previewProfileYoutube() {
  const url = youtubeUrl.value.trim()
  if (!url) {
    youtubeError.value = 'YouTube URL을 입력해주세요.'
    return
  }
  youtubePreviewLoading.value = true
  youtubeError.value = ''
  youtubeSaved.value = ''
  try {
    youtubePreview.value = await slateApi.previewYoutubeVideo(url)
  } catch (err) {
    youtubePreview.value = null
    youtubeError.value = err.message
  } finally {
    youtubePreviewLoading.value = false
  }
}

async function connectYoutubeToPortfolio() {
  if (!youtubePreview.value || !profile.value?.profileId) return
  portfolioSaving.value = true
  youtubeError.value = ''
  youtubeSaved.value = ''
  try {
    await slateApi.createPortfolioItem({
      title: youtubePreview.value.title || 'YouTube 영상',
      roleName: '',
      description: youtubePreview.value.channelTitle ? `YouTube · ${youtubePreview.value.channelTitle}` : 'YouTube 영상',
      sourceType: 'MANUAL',
      externalSourceName: 'YOUTUBE',
      externalReferenceId: youtubePreview.value.videoId || '',
      url: youtubeUrl.value.trim(),
      thumbnailUrl: youtubePreview.value.thumbnailUrl || '',
      sortOrder: portfolioItems.value.length
    })
    await reloadPortfolio()
    youtubeUrl.value = ''
    youtubePreview.value = null
    youtubeSaved.value = 'YouTube 영상을 포트폴리오에 연결했습니다.'
  } catch (err) {
    youtubeError.value = err.message
  } finally {
    portfolioSaving.value = false
  }
}

function formatYoutubeDuration(value) {
  const seconds = Number(value || 0)
  const minutes = String(Math.floor(seconds / 60)).padStart(2, '0')
  const remainder = String(seconds % 60).padStart(2, '0')
  return `${minutes}:${remainder}`
}

async function searchPublicData() {
  publicDataLoading.value = true
  publicDataError.value = ''
  try {
    publicDataResults.value = await slateApi.publicDataSearch({
      keyword: publicDataForm.keyword.trim(),
      itemType: publicDataForm.itemType,
      limit: 20
    })
  } catch (err) {
    publicDataError.value = err.message
  } finally {
    publicDataLoading.value = false
  }
}

async function addPublicDataItem(item) {
  portfolioSaving.value = true
  portfolioError.value = ''
  portfolioSaved.value = ''
  try {
    if (!profile.value?.profileId) throw new Error('프로필을 먼저 저장해주세요.')
    await slateApi.createPortfolioFromPublicData({
      publicDataSyncItemId: item.publicDataSyncItemId,
      roleName: portfolioForm.roleName.trim(),
      sortOrder: Number(portfolioForm.sortOrder || 0)
    })
    await reloadPortfolio()
    pendingPortfolioDeleteId.value = null
    portfolioSaved.value = '공공데이터 항목이 추가되었습니다.'
  } catch (err) {
    portfolioError.value = err.message
  } finally {
    portfolioSaving.value = false
  }
}

watch(() => props.currentUser?.userId, () => {
  if (props.currentUser) {
    resetAccountForm()
    loadProfile()
    if (route.name === 'profile-files') loadMyFiles()
  }
  else {
    profileLoadRequestId += 1
    profile.value = null
    profileTeams.value = []
    profileWorks.value = []
    profileTeamsLoading.value = false
    profileWorksLoading.value = false
    profileTeamsError.value = ''
    profileWorksError.value = ''
    loading.value = false
    resetFollowState()
    resetPortfolioForm()
    publicDataResults.value = []
    pendingProfileDelete.value = false
    pendingAccountWithdrawal.value = false
    pendingPortfolioDeleteId.value = null
    resetAccountForm()
  }
}, { immediate: true })

watch(() => [props.currentUser?.nickname, props.currentUser?.email], () => {
  if (!accountSaving.value && !accountWithdrawing.value) resetAccountForm()
})

watch(() => route.fullPath, () => {
  if (route.name === 'profile-recovery') pendingProfileDelete.value = false
  if (route.name === 'profile-account') {
    clearAccountMessages()
    pendingAccountWithdrawal.value = false
    resetAccountForm()
  }
  syncPortfolioRoute()
  if (route.name === 'profile-files') loadMyFiles()
}, { immediate: true })

watch(workFileFilter, () => {
  if (route.name === 'profile-files') loadMyFiles()
})

onBeforeUnmount(() => {
  revokePreview(profileImagePreview)
  revokePreview(portfolioImagePreview)
})
</script>

<template>
  <section v-if="!props.currentUser" class="login-panel">
    <h2>내 정보</h2>
    <p>프로필을 관리하려면 로그인이 필요합니다.</p>
    <RouterLink class="primary-button inline" :to="{ name: 'login', query: { redirect: '/profile' } }">로그인</RouterLink>
  </section>

  <section
    v-else
    class="profile-page"
  >
    <section v-if="isProfileDashboard && profile" class="profile-hero-card">
        <div class="profile-portrait-wrap">
          <img
          :src="profileThumbnail()"
          :alt="`${profile.displayName || '프로필'} 이미지`"
          class="profile-portrait"
          @error="profileImageFailed = true"
        >
          <button type="button" aria-label="프로필 사진 변경" @click="goProfileRoute('profile-edit')">▣</button>
        </div>
      <div class="profile-hero-copy">
        <div class="profile-name-row">
          <h2>{{ profile.displayName || props.currentUser.nickname || '이름 정보 없음' }}</h2>
          <span aria-hidden="true">●</span>
          <em>{{ activityStatusName }}</em>
        </div>
        <div class="profile-meta-line">
          <span>▣ {{ primaryRoleName }}</span>
          <span>⌖ {{ regionName }}</span>
        </div>
        <p>{{ profile.shortIntro || '등록된 한 줄 소개가 없습니다.' }}</p>
      </div>
      <div class="profile-hero-actions">
        <div v-if="profile?.profileId" class="profile-follow-summary profile-hero-follow-summary" aria-label="팔로우 요약">
          <button type="button" :disabled="followSummaryLoading" @click="openFollowDialog('followers')">
            <span>팔로워</span>
            <strong>{{ followSummaryLoading ? '…' : followSummary?.followerCount ?? '-' }}</strong>
          </button>
          <button type="button" :disabled="followSummaryLoading" @click="openFollowDialog('following')">
            <span>팔로잉</span>
            <strong>{{ followSummaryLoading ? '…' : followSummary?.followingCount ?? '-' }}</strong>
          </button>
          <p v-if="followSummaryError" class="profile-follow-error" role="alert">{{ followSummaryError }}</p>
        </div>
        <button type="button" @click="goProfileRoute('profile-edit')">✎ 프로필 수정</button>
        <button type="button" @click="goProfileRoute('profile-portfolio-new')">＋ 포트폴리오 추가</button>
      </div>
    </section>

    <section v-if="isProfileDashboard" class="profile-account-summary">
      <article><span>닉네임</span><strong>{{ props.currentUser?.nickname || '-' }}</strong></article>
      <article><span>이메일</span><strong>{{ props.currentUser?.email || '-' }}</strong></article>
    </section>

    <section v-if="isProfileDashboard && loading" class="profile-dashboard-state" aria-live="polite">
      프로필 정보를 불러오는 중입니다.
    </section>

    <section v-else-if="isProfileDashboard && !profile" class="profile-dashboard-state">
      <strong>등록된 프로필이 없습니다.</strong>
      <p>{{ error || '프로필을 생성하면 참여 팀과 작품, 포트폴리오를 한곳에서 관리할 수 있습니다.' }}</p>
      <button class="primary-button inline" type="button" @click="goProfileRoute('profile-edit')">프로필 생성</button>
    </section>

    <FollowListDialog
      :open="followDialogOpen"
      :mode="followDialogMode"
      :profile-id="profile?.profileId"
      :current-user-id="props.currentUser?.userId"
      :title="followDialogMode === 'following' ? '팔로잉' : '팔로워'"
      @close="closeFollowDialog"
      @counts-changed="loadFollowSummary"
    />

    <section v-if="isProfileDashboard && profile" class="profile-tag-card">
      <div>
        <strong>역할</strong>
        <span v-for="role in dashboardRoles" :key="`dash-role-${role.roleId}`">{{ roleDisplayName(role) }}</span>
        <span v-if="dashboardRoles.length === 0">등록된 역할 없음</span>
      </div>
      <div>
        <strong>장르</strong>
        <span v-for="genre in dashboardGenres" :key="`dash-genre-${genre.genreId}`">{{ genre.name }}</span>
        <span v-if="dashboardGenres.length === 0">등록된 장르 없음</span>
      </div>
      <div>
        <strong>협업 조건</strong>
        <span v-for="condition in dashboardConditions" :key="condition.conditionCode">
          {{ condition.displayName || condition.conditionCode }}
        </span>
        <span v-if="dashboardConditions.length === 0">등록된 협업 조건 없음</span>
      </div>
    </section>

    <section v-if="isProfileDashboard && profile" class="profile-dashboard-grid">
      <article class="profile-card profile-team-card">
        <header>
          <h3>참여 중인 팀</h3>
          <RouterLink to="/teams">전체 보기 ›</RouterLink>
          </header>
          <div class="profile-team-grid">
            <RouterLink
              v-for="team in dashboardTeams"
              :key="team.teamId"
              class="profile-dashboard-link profile-team-item"
              :to="{ name: 'teams-detail', params: { teamId: team.teamId } }"
            >
            <img :src="teamThumbnail(team)" :alt="`${team.name} 대표 이미지`" @error="team.imageUrl = null">
            <div>
              <strong>{{ team.name || '팀 이름 없음' }}</strong>
              <span>♙ {{ teamRoleLabel(team.myTeamRole) }}</span>
              <span>▣ {{ teamGenreNames(team) || '등록 장르 없음' }}</span>
              <span>⌖ {{ team.currentMemberCount ?? 0 }}명</span>
              <em>{{ teamStatusLabel(team.status) }}</em>
            </div>
          </RouterLink>
        </div>
        <p v-if="profileTeamsLoading" class="profile-dashboard-message">참여 팀을 불러오는 중입니다.</p>
        <p v-else-if="profileTeamsError" class="profile-dashboard-message error-text">{{ profileTeamsError }}</p>
        <p v-else-if="dashboardTeams.length === 0" class="profile-dashboard-message">참여 중인 팀이 없습니다.</p>
      </article>

      <article class="profile-card profile-work-card">
        <header>
          <h3>참여 작품</h3>
          <RouterLink :to="{ name: 'profile-works' }">전체 보기 ›</RouterLink>
          </header>
          <div class="profile-work-grid">
            <RouterLink
              v-for="work in profileWorks"
              :key="work.postId"
              class="profile-dashboard-link profile-work-item"
              :to="{ name: 'boards-detail', params: { postId: work.postId } }"
            >
            <img :src="workThumbnail(work)" :alt="`${workTitle(work)} 대표 이미지`" @error="work.representativeImageUrl ? work.representativeImageUrl = null : work.youtubeThumbnailUrl = null">
            <strong>{{ workTitle(work) }}</strong>
            <span>{{ work.workTeamName || work.authorNickname || '작성자 정보 없음' }}</span>
            <p>{{ workDescription(work) }}</p>
            <em>{{ workMediaLabel(work.mediaType) }}</em>
          </RouterLink>
        </div>
        <p v-if="profileWorksLoading" class="profile-dashboard-message">참여 작품을 불러오는 중입니다.</p>
        <p v-else-if="profileWorksError" class="profile-dashboard-message error-text">{{ profileWorksError }}</p>
        <p v-else-if="profileWorks.length === 0" class="profile-dashboard-message">참여 작품이 없습니다.</p>
      </article>
    </section>

    <section v-if="isProfileDashboard && profile" class="profile-card profile-portfolio-dashboard">
      <header>
        <h3>포트폴리오</h3>
        <button type="button" @click="goProfileRoute('profile-portfolio')">전체 보기 ›</button>
        </header>
        <div class="profile-portfolio-grid">
          <RouterLink
            v-for="item in portfolioItems"
            :key="item.portfolioItemId"
            class="profile-dashboard-link profile-portfolio-item"
            :to="{ name: 'profile-portfolio-detail', params: { portfolioId: item.portfolioItemId } }"
          >
          <img :src="portfolioThumbnail(item)" :alt="`${item.title} 썸네일`" @error="item.uploadedThumbnailUrl ? item.uploadedThumbnailUrl = null : item.thumbnailUrl = null">
          <div>
            <div class="portfolio-title-row">
              <strong>{{ item.title }}</strong>
              <span v-if="isVerifiedItem(item)" class="verified-badge">Verified</span>
            </div>
            <span>{{ item.roleName || '역할 정보 없음' }}</span>
            <p>{{ item.description || '등록된 설명이 없습니다.' }}</p>
          </div>
        </RouterLink>
      </div>
      <p v-if="portfolioItems.length === 0" class="profile-dashboard-message">등록된 포트폴리오가 없습니다.</p>
    </section>

    <section v-if="isProfileDashboard && profile" class="profile-settings-grid">
      <RouterLink :to="{ name: 'profile-youtube' }"><span>▶</span><strong>YouTube 포트폴리오 업로드</strong><small>영상 정보를 확인하고 포트폴리오에 연결해요.</small><i>›</i></RouterLink>
      <RouterLink :to="{ name: 'profile-account' }"><span>♙</span><strong>계정 관리</strong><small>이메일, 비밀번호 등 계정 정보를 관리할 수 있어요.</small><i>›</i></RouterLink>
    </section>

    <section v-if="isProfileSubRoute" class="profile-editor-shell">
    <header class="profile-subroute-head" v-if="isProfileSubRoute">
      <div>
        <span class="eyebrow">{{ editorEyebrow }}</span>
        <h2>{{ editorTitle }}</h2>
      </div>
      <RouterLink class="ghost-button inline" :to="subrouteBackRoute">← 돌아가기</RouterLink>
    </header>
    <aside class="profile-layout">
      <div class="avatar large">{{ form.displayName?.slice(0, 1) || 'S' }}</div>
      <div>
        <span class="eyebrow">{{ profile?.publicRegionName || '프로필' }}</span>
        <h2>{{ form.displayName || props.currentUser.nickname }}</h2>
        <p>{{ form.shortIntro || '소개를 입력해주세요.' }}</p>
        <div class="reason-tags">
          <span v-for="role in selectedRoles" :key="`role-${role.roleId}`">
            {{ role.index === 0 ? '주역할' : '부역할' }} · {{ role.name }}
          </span>
          <span v-for="genreId in form.genreIds" :key="`genre-${genreId}`">
            {{ genres.find((genre) => Number(genre.genreId) === genreId)?.name || genreId }}
          </span>
        </div>
      </div>
    </aside>

    <div class="profile-main-stack">
    <form
      v-if="showsProfileForm"
      class="form-panel profile-edit-panel"
      @submit.prevent="saveProfile"
    >
      <div class="form-head profile-edit-head">
        <div>
          <span class="eyebrow">{{ editorEyebrow }}</span>
          <h2>{{ editorTitle }}</h2>
        </div>
        <div class="row-actions">
          <button v-if="profile?.profileId" class="ghost-button danger" type="button" :disabled="saving || loading" @click="requestProfileDelete">
            삭제
          </button>
          <RouterLink class="ghost-button inline" :to="{ name: 'profile' }">취소</RouterLink>
          <button class="primary-button" type="submit" :disabled="saving || loading">
            {{ saving ? '저장 중' : '저장' }}
          </button>
        </div>
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>
      <p v-if="saved" class="notice-text">{{ saved }}</p>
      <div v-if="pendingProfileDelete" class="confirm-inline danger-confirm">
        <span>프로필을 삭제할까요? 매칭과 랭킹에는 노출되지 않습니다.</span>
        <button class="ghost-button danger" type="button" :disabled="saving" @click="deleteProfile">
          삭제 확인
        </button>
        <button class="ghost-button" type="button" :disabled="saving" @click="cancelProfileDelete">
          취소
        </button>
      </div>

        <div class="form-grid">
          <div class="image-picker wide">
          <img :src="profileImagePreview || (!profileImageDelete && profile?.profileImageUrl) || defaultProfileImage" alt="프로필 이미지 미리보기">
          <div>
            <strong>프로필 이미지</strong>
            <small>JPEG, PNG, WebP · 최대 5MB</small>
            <label class="ghost-button inline">이미지 선택<input type="file" accept="image/jpeg,image/png,image/webp" @change="selectProfileImage"></label>
            <button v-if="profileImagePreview || profile?.profileImageUrl" class="ghost-button danger" type="button" @click="removeProfileImageSelection">이미지 삭제</button>
          </div>
        </div>
        <label class="field">
          <span>표시 이름</span>
          <input v-model="form.displayName" maxlength="50" required>
        </label>
        <label class="field">
          <span>지역</span>
          <select v-model="form.regionId" required>
            <option v-for="region in regions" :key="region.regionId" :value="region.regionId">
              {{ region.publicDisplayName }}
            </option>
          </select>
        </label>
        <label class="field wide">
          <span>한 줄 소개</span>
          <input v-model="form.shortIntro" maxlength="120" required>
        </label>
        <label class="field wide">
          <span>상세 소개</span>
          <textarea v-model="form.detailIntro" rows="4" maxlength="2000" />
        </label>
        <label class="field">
          <span>공개 상태</span>
          <select v-model="form.visibility">
            <option v-for="item in codeOptions('PROFILE_VISIBILITY')" :key="item.code" :value="item.code">
              {{ item.displayName }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>활동 상태</span>
          <select v-model="form.activityStatus">
            <option v-for="item in codeOptions('ACTIVITY_STATUS')" :key="item.code" :value="item.code">
              {{ item.displayName }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>경력</span>
          <select v-model="form.experienceLevel">
            <option v-for="item in codeOptions('EXPERIENCE_LEVEL')" :key="item.code" :value="item.code">
              {{ item.displayName }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>합류 시점</span>
          <select v-model="form.joinAvailability">
            <option v-for="item in codeOptions('JOIN_AVAILABILITY')" :key="item.code" :value="item.code">
              {{ item.displayName }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>협업 상태</span>
          <select v-model="form.collaborationStatus">
            <option v-for="item in codeOptions('COLLABORATION_STATUS')" :key="item.code" :value="item.code">
              {{ item.displayName }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>이동 범위</span>
          <select v-model="form.travelRange">
            <option v-for="item in codeOptions('TRAVEL_RANGE')" :key="item.code" :value="item.code">
              {{ item.displayName }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>선호 기간</span>
          <select v-model="form.preferredDuration">
            <option v-for="item in codeOptions('DURATION')" :key="item.code" :value="item.code">
              {{ item.displayName }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>장비</span>
          <select v-model="form.equipmentStatus">
            <option v-for="item in codeOptions('EQUIPMENT_STATUS')" :key="item.code" :value="item.code">
              {{ item.displayName }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>연령대</span>
          <select v-model="form.ageBand">
            <option v-for="item in ageBands" :key="item.code" :value="item.code">{{ item.displayName }}</option>
          </select>
        </label>
        <label class="field">
          <span>참여 방식</span>
          <select v-model="form.participationMode">
            <option v-for="item in participationModes" :key="item.code" :value="item.code">{{ item.displayName }}</option>
          </select>
        </label>
      </div>

      <div class="field wide">
        <span>역할 {{ selectedCount(form.roleIds) }}/5</span>
        <div class="selected-role-list">
          <button
            v-for="role in selectedRoles"
            :key="`selected-${role.roleId}`"
            class="selected-role-chip"
            type="button"
            draggable="true"
            @dragstart="startRoleDrag(role.roleId)"
            @dragover.prevent
            @drop="dropRole(role.roleId)"
          >
            <strong>{{ role.index === 0 ? '주역할' : '부역할' }}</strong>
            <span>{{ role.name }}</span>
            <i aria-hidden="true" @click.stop="removeRole(role.roleId)">×</i>
          </button>
          <p v-if="selectedRoles.length === 0" class="muted">역할을 선택하면 첫 번째 역할이 주역할로 설정됩니다.</p>
        </div>
        <div class="check-grid">
          <label v-for="role in roleOptions" :key="role.roleId" class="chip-check">
            <input
              type="checkbox"
              :checked="form.roleIds.includes(role.roleId)"
              @change="toggleRole(role.roleId)"
            >
            <span>{{ role.name }}</span>
          </label>
        </div>
      </div>

      <div class="field wide">
        <span>장르</span>
        <div class="check-grid compact">
          <label v-for="genre in genres" :key="genre.genreId" class="chip-check">
            <input
              type="checkbox"
              :checked="form.genreIds.includes(Number(genre.genreId))"
              @change="toggleNumber('genreIds', genre.genreId)"
            >
            <span>{{ genre.name }}</span>
          </label>
        </div>
      </div>

      <div class="field wide">
        <span>협업 조건</span>
        <div class="check-grid compact">
          <label v-for="item in codeOptions('COLLABORATION_CONDITION')" :key="item.code" class="chip-check">
            <input
              type="checkbox"
              :checked="form.collaborationConditionCodes.includes(item.code)"
              @change="toggleCode('collaborationConditionCodes', item.code)"
            >
            <span>{{ item.displayName }}</span>
          </label>
        </div>
      </div>
    </form>

    <section v-if="showsWorksPanel" class="form-panel profile-works-panel">
      <div class="profile-works-list">
        <RouterLink
          v-for="work in profileWorks"
          :key="work.postId"
          class="profile-dashboard-link profile-work-list-item"
          :to="{ name: 'boards-detail', params: { postId: work.postId } }"
        >
          <img :src="workThumbnail(work)" :alt="`${workTitle(work)} 대표 이미지`" @error="work.representativeImageUrl ? work.representativeImageUrl = null : work.youtubeThumbnailUrl = null">
          <div>
            <strong>{{ workTitle(work) }}</strong>
            <span>{{ work.workTeamName || work.authorNickname || '작성자 정보 없음' }}</span>
            <p>{{ workDescription(work) }}</p>
            <em>{{ workMediaLabel(work.mediaType) }}</em>
          </div>
        </RouterLink>
      </div>
      <p v-if="profileWorksLoading" class="profile-dashboard-message">참여 작품을 불러오는 중입니다.</p>
      <p v-else-if="profileWorksError" class="profile-dashboard-message error-text">{{ profileWorksError }}</p>
      <p v-else-if="profileWorks.length === 0" class="profile-dashboard-message">참여 작품이 없습니다.</p>
    </section>
    <div v-if="showsWorksPanel" class="profile-works-upload-action">
      <RouterLink class="primary-button inline" :to="{ name: 'boards-new', query: { category: 'WORK' } }">작업물 업로드</RouterLink>
    </div>

    <section v-if="route.name === 'profile-account'" class="form-panel profile-account-panel">
      <form class="profile-account-form" @submit.prevent="saveAccount">
        <p v-if="accountError" class="error-text">{{ accountError }}</p>
        <p v-if="accountSaved" class="notice-text">{{ accountSaved }}</p>

        <div class="profile-account-fields">
          <div class="profile-account-edit-row">
            <label class="field">
              <span>닉네임</span>
              <input v-model.trim="accountForm.nickname" maxlength="50" autocomplete="nickname" required :disabled="!accountEdit.nickname">
            </label>
            <button v-if="!accountEdit.nickname" class="ghost-button inline" type="button" @click="activateAccountEdit('nickname')">변경</button>
            <button v-else class="ghost-button inline" type="button" @click="cancelAccountEdit('nickname')">취소</button>
          </div>

          <div class="profile-account-edit-row">
            <label class="field">
              <span>이메일</span>
              <input v-model.trim="accountForm.email" type="email" maxlength="255" autocomplete="email" required :disabled="!accountEdit.email">
            </label>
            <button v-if="!accountEdit.email" class="ghost-button inline" type="button" @click="activateAccountEdit('email')">변경</button>
            <button v-else class="ghost-button inline" type="button" @click="cancelAccountEdit('email')">취소</button>
          </div>

          <label v-if="accountEdit.email" class="field profile-account-current-password email-current-password">
            <span>현재 비밀번호</span>
            <input v-model="accountForm.currentPassword" type="password" autocomplete="current-password" maxlength="80">
          </label>

          <div class="profile-account-password-toggle">
            <div>
              <span>비밀번호</span>
              <strong>{{ accountEdit.password ? '새 비밀번호를 입력해주세요.' : '비밀번호 변경' }}</strong>
            </div>
            <button v-if="!accountEdit.password" class="ghost-button inline" type="button" @click="activateAccountEdit('password')">비밀번호 변경</button>
            <button v-else class="ghost-button inline" type="button" @click="cancelAccountEdit('password')">취소</button>
          </div>

          <label v-if="accountEdit.password && !accountEdit.email" class="field profile-account-current-password password-current-password">
            <span>현재 비밀번호</span>
            <input v-model="accountForm.currentPassword" type="password" autocomplete="current-password" maxlength="80">
          </label>
          <label v-if="accountEdit.password" class="field">
            <span>새 비밀번호</span>
            <input v-model="accountForm.newPassword" type="password" autocomplete="new-password" minlength="8" maxlength="80" placeholder="8자 이상">
          </label>
          <label v-if="accountEdit.password" class="field">
            <span>새 비밀번호 확인</span>
            <input v-model="accountForm.newPasswordConfirm" type="password" autocomplete="new-password" minlength="8" maxlength="80">
          </label>
        </div>

        <div v-if="isAccountEditActive()" class="profile-account-actions">
          <button class="primary-button inline" type="submit" :disabled="accountSaving || accountWithdrawing">
            {{ accountSaving ? '저장 중' : '저장' }}
          </button>
        </div>
      </form>

      <div class="profile-account-danger">
        <div>
          <strong>회원 탈퇴</strong>
          <p>탈퇴 후에는 현재 계정으로 로그인할 수 없으며, 운영 복구 절차가 필요한 상태로 전환됩니다.</p>
        </div>
        <button v-if="!pendingAccountWithdrawal" class="ghost-button danger" type="button" :disabled="accountSaving || accountWithdrawing" @click="requestAccountWithdrawal">
          회원 탈퇴
        </button>
        <div v-else class="profile-account-withdrawal">
          <label class="field">
            <span>현재 비밀번호</span>
            <input v-model="accountForm.withdrawalPassword" type="password" autocomplete="current-password" maxlength="80">
          </label>
          <button class="ghost-button danger" type="button" :disabled="accountWithdrawing" @click="withdrawAccount">
            {{ accountWithdrawing ? '처리 중' : '탈퇴 확인' }}
          </button>
          <button class="ghost-button" type="button" :disabled="accountWithdrawing" @click="cancelAccountWithdrawal">
            취소
          </button>
        </div>
      </div>
    </section>

    <section v-if="route.name === 'profile-recovery'" class="form-panel profile-recovery-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">보호 조치</span>
          <h2>삭제/복구 안내</h2>
        </div>
        <button v-if="profile?.profileId" class="ghost-button danger" type="button" :disabled="saving || loading" @click="requestProfileDelete">
          프로필 삭제
        </button>
      </div>
      <p class="muted">프로필을 삭제하면 매칭과 검색 노출에서 제외됩니다. 회원 탈퇴는 계정 관리에서 현재 비밀번호 확인 후 진행할 수 있습니다.</p>
      <div v-if="pendingProfileDelete" class="confirm-inline danger-confirm">
        <span>프로필을 삭제할까요? 매칭과 검색에는 노출되지 않습니다.</span>
        <button class="ghost-button danger" type="button" :disabled="saving" @click="deleteProfile">
          삭제 확인
        </button>
        <button class="ghost-button" type="button" :disabled="saving" @click="cancelProfileDelete">
          취소
        </button>
      </div>
    </section>

    <section v-if="showsPortfolioPanel" class="form-panel portfolio-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">작업 이력</span>
          <h2>내 포트폴리오</h2>
        </div>
        <RouterLink class="primary-button inline" :to="{ name: 'profile-portfolio-new' }">새 항목</RouterLink>
      </div>

      <p v-if="portfolioError" class="error-text">{{ portfolioError }}</p>
      <p v-if="portfolioSaved" class="notice-text">{{ portfolioSaved }}</p>

      <div v-if="portfolioItems.length" class="portfolio-list">
        <article v-for="item in portfolioItems" :key="item.portfolioItemId" class="list-panel portfolio-row">
          <RouterLink class="portfolio-row-main" :to="{ name: 'profile-portfolio-detail', params: { portfolioId: item.portfolioItemId } }">
            <img class="portfolio-row-thumb" :src="portfolioThumbnail(item)" :alt="`${item.title} 썸네일`" @error="item.uploadedThumbnailUrl ? item.uploadedThumbnailUrl = null : item.thumbnailUrl = null">
            <div class="portfolio-row-copy">
              <div class="portfolio-title-row">
                <strong>{{ item.title }}</strong>
                <span v-if="isVerifiedItem(item)" class="verified-badge">Verified</span>
              </div>
              <p>{{ item.description || '설명 없음' }}</p>
              <div class="subline">
                <span>{{ item.roleName || '역할 미입력' }}</span>
                <span v-if="item.externalSourceName">{{ item.externalSourceName }}</span>
              </div>
            </div>
          </RouterLink>
          <div class="row-actions">
            <a v-if="item.url" class="ghost-button inline" :href="item.url" target="_blank" rel="noreferrer">열기</a>
            <button class="ghost-button" type="button" :disabled="portfolioSaving" @click="editPortfolioItem(item)">수정</button>
            <button class="ghost-button danger" type="button" :disabled="portfolioSaving" @click="requestPortfolioDelete(item)">삭제</button>
          </div>
          <div v-if="pendingPortfolioDeleteId === item.portfolioItemId" class="confirm-inline danger-confirm portfolio-confirm">
            <span>선택한 포트폴리오를 삭제할까요?</span>
            <button class="ghost-button danger" type="button" :disabled="portfolioSaving" @click="removePortfolioItem(item)">
              삭제 확인
            </button>
            <button class="ghost-button" type="button" :disabled="portfolioSaving" @click="cancelPortfolioDelete">
              취소
            </button>
          </div>
        </article>
      </div>
      <p v-else class="muted">등록된 포트폴리오가 없습니다.</p>
    </section>

    <section v-if="showsPortfolioDetail" class="form-panel portfolio-detail-panel">
      <p v-if="portfolioError" class="error-text">{{ portfolioError }}</p>
      <template v-if="selectedPortfolioItem">
        <div class="form-head">
          <div>
            <span class="eyebrow">포트폴리오</span>
            <div class="portfolio-title-row">
              <h2>{{ selectedPortfolioItem.title }}</h2>
              <span v-if="isVerifiedItem(selectedPortfolioItem)" class="verified-badge">Verified</span>
            </div>
          </div>
          <div class="row-actions">
            <RouterLink class="ghost-button inline" :to="{ name: 'profile-portfolio-edit', params: { portfolioId: selectedPortfolioItem.portfolioItemId } }">수정</RouterLink>
            <button class="ghost-button danger" type="button" :disabled="portfolioSaving" @click="requestPortfolioDelete(selectedPortfolioItem)">삭제</button>
          </div>
        </div>
        <figure v-if="portfolioThumbnail(selectedPortfolioItem)" class="portfolio-detail-media">
          <img :src="portfolioThumbnail(selectedPortfolioItem)" :alt="`${selectedPortfolioItem.title} 썸네일`">
        </figure>
        <div class="portfolio-detail-grid">
          <article><span>역할</span><strong>{{ selectedPortfolioItem.roleName || '-' }}</strong></article>
          <article><span>크레딧</span><strong>{{ selectedPortfolioItem.creditName || '-' }}</strong></article>
          <article><span>출처</span><strong>{{ selectedPortfolioItem.externalSourceName || '직접 등록' }}</strong></article>
          <article><span>외부 식별자</span><strong>{{ selectedPortfolioItem.externalReferenceId || '-' }}</strong></article>
          <article><span>검증 상태</span><strong>{{ verificationStatusLabel(selectedPortfolioItem.verificationStatus) }}</strong></article>
          <article><span>KOBIS 매칭 이름</span><strong>{{ selectedPortfolioItem.providerPersonName || '-' }}</strong></article>
          <article><span>KOBIS 매칭 역할</span><strong>{{ selectedPortfolioItem.providerRoleName || '-' }}</strong></article>
        </div>
        <div class="portfolio-detail-copy">
          <span>설명</span>
          <p>{{ selectedPortfolioItem.description || '등록된 설명이 없습니다.' }}</p>
        </div>
        <a v-if="selectedPortfolioItem.url" class="primary-button inline" :href="selectedPortfolioItem.url" target="_blank" rel="noreferrer">연결된 링크 열기</a>
        <div v-if="pendingPortfolioDeleteId === selectedPortfolioItem.portfolioItemId" class="confirm-inline danger-confirm portfolio-confirm">
          <span>선택한 포트폴리오를 삭제할까요?</span>
          <button class="ghost-button danger" type="button" :disabled="portfolioSaving" @click="removePortfolioItem(selectedPortfolioItem)">삭제 확인</button>
          <button class="ghost-button" type="button" :disabled="portfolioSaving" @click="cancelPortfolioDelete">취소</button>
        </div>
      </template>
    </section>

    <section v-if="showsPortfolioForm" class="form-panel portfolio-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">{{ route.name === 'profile-portfolio-edit' ? '수정' : '등록' }}</span>
          <h2>{{ route.name === 'profile-portfolio-edit' ? '포트폴리오 수정' : '새 포트폴리오' }}</h2>
        </div>
        <RouterLink class="ghost-button inline" :to="subrouteBackRoute">취소</RouterLink>
      </div>
      <p v-if="portfolioError" class="error-text">{{ portfolioError }}</p>
      <form v-if="route.name === 'profile-portfolio-new' || selectedPortfolioItem" class="portfolio-compose" @submit.prevent="savePortfolioItem">
        <div class="form-grid">
          <label class="field wide">
            <span>제목</span>
            <span class="kobis-suggest-wrap">
              <input
                v-model="portfolioForm.title"
                maxlength="150"
                :disabled="!profile?.profileId"
                required
                @input="onKobisMovieKeywordInput"
              >
              <span v-if="kobisMovieLoading" class="kobis-loading">KOBIS 검색 중</span>
              <span v-if="kobisMovieResults.length" class="kobis-suggest-list">
                <button
                  v-for="movie in kobisMovieResults"
                  :key="movie.movieCd"
                  class="kobis-suggest-item"
                  type="button"
                  @click="selectKobisMovie(movie)"
                >
                  <strong>{{ movie.movieNm }}</strong>
                  <span>
                    {{ movie.prdtYear || movie.openDt || '연도 미상' }}
                    <template v-if="movie.genreAlt"> · {{ movie.genreAlt }}</template>
                  </span>
                  <small v-if="kobisDirectors(movie)">감독 {{ kobisDirectors(movie) }}</small>
                </button>
              </span>
            </span>
          </label>
          <div v-if="selectedKobisMovie" class="kobis-selected-movie wide">
            <div>
              <strong>{{ kobisMovieLabel(selectedKobisMovie) }}</strong>
              <span>KOBIS {{ selectedKobisMovie.movieCd }}</span>
            </div>
            <button type="button" :disabled="!profile?.profileId" @click="clearSelectedKobisMovie">선택 해제</button>
          </div>
          <label class="field">
            <span>역할</span>
            <input v-model="portfolioForm.roleName" maxlength="80" placeholder="예: 촬영, 조명, 편집, 감독" :disabled="!profile?.profileId">
          </label>
          <label class="field">
            <span>크레딧 이름</span>
            <input v-model="portfolioForm.creditName" maxlength="120" placeholder="예: 이태윤" :disabled="!profile?.profileId">
          </label>
          <label class="field">
            <span>출처</span>
            <select v-model="portfolioForm.sourceType" :disabled="!profile?.profileId">
              <option v-for="item in portfolioSourceTypes" :key="item.code" :value="item.code">
                {{ item.displayName }}
              </option>
            </select>
          </label>
          <label class="field">
            <span>정렬</span>
            <input v-model.number="portfolioForm.sortOrder" type="number" :disabled="!profile?.profileId">
          </label>
          <label class="field wide">
            <span>설명</span>
            <textarea v-model="portfolioForm.description" rows="3" maxlength="1000" :disabled="!profile?.profileId"></textarea>
          </label>
          <label class="field">
            <span>외부 출처명</span>
            <input v-model="portfolioForm.externalSourceName" maxlength="80" :disabled="!profile?.profileId">
          </label>
          <label class="field">
            <span>외부 식별자</span>
            <input v-model="portfolioForm.externalReferenceId" maxlength="100" :disabled="!profile?.profileId">
          </label>
          <label class="field wide">
            <span>URL</span>
            <input v-model="portfolioForm.url" maxlength="500" placeholder="https://..." :disabled="!profile?.profileId">
            </label>
            <div class="image-picker wide portfolio-thumbnail-picker">
            <img :src="portfolioImagePreview || (portfolioThumbnailMode === 'UPLOAD' && selectedPortfolioItem?.uploadedThumbnailUrl) || (portfolioThumbnailMode === 'YOUTUBE' && portfolioForm.thumbnailUrl) || defaultPortfolioImage" alt="포트폴리오 썸네일 미리보기">
            <div>
              <strong>대표 이미지</strong>
              <small>직접 업로드하거나 위 URL의 YouTube 썸네일을 사용할 수 있습니다.</small>
              <label class="ghost-button inline">직접 이미지 선택<input type="file" accept="image/jpeg,image/png,image/webp" @change="selectPortfolioImage"></label>
              <button class="ghost-button" type="button" :disabled="portfolioThumbnailLoading || !portfolioForm.url.trim()" @click="usePortfolioYoutubeThumbnail">{{ portfolioThumbnailLoading ? '확인 중' : 'YouTube 썸네일 사용' }}</button>
              <button v-if="portfolioThumbnailMode !== 'NONE'" class="ghost-button danger" type="button" @click="clearPortfolioThumbnail">대표 이미지 제거</button>
            </div>
          </div>
        </div>
        <button class="primary-button inline" type="submit" :disabled="portfolioSaving || !profile?.profileId">
          {{ editingPortfolioId ? '포트폴리오 수정' : '포트폴리오 추가' }}
        </button>
      </form>
    </section>

    <section v-if="route.name === 'profile-files'" class="form-panel profile-files-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">내 저장 공간</span>
          <h2>업로드 파일</h2>
        </div>
        <div class="row-actions">
          <label class="compact-filter">
            상태
            <select v-model="workFileFilter">
              <option value="ALL">전체</option>
              <option value="ACTIVE">활성</option>
              <option value="HELD">운영 보관</option>
              <option value="DELETED">삭제 대기</option>
            </select>
          </label>
          <button class="ghost-button" type="button" :disabled="filesLoading" @click="loadMyFiles">새로고침</button>
        </div>
      </div>
      <p v-if="fileError" class="error-text">{{ fileError }}</p>
      <p v-if="fileSaved" class="notice-text">{{ fileSaved }}</p>
      <div v-if="workFileQuota" class="profile-file-quota">
        <strong>{{ formatBytes(workFileQuota.activeUserBytes) }} / {{ formatBytes(workFileQuota.userQuotaBytes) }}</strong>
        <span>남은 용량 {{ formatBytes(workFileQuota.userRemainingBytes) }}</span>
      </div>
      <div class="profile-file-upload">
        <label class="field">
          <span>영상 파일</span>
          <input accept=".mp4,.webm,.mov,video/mp4,video/webm,video/quicktime" type="file" @change="onWorkFileChange">
        </label>
        <div>
          <strong>{{ selectedWorkFile?.name || '선택된 파일 없음' }}</strong>
          <span v-if="workFileDuration">{{ workFileDuration }}초</span>
        </div>
        <button class="primary-button" type="button" :disabled="!selectedWorkFile || fileSaving" @click="uploadSelectedWorkFile">
          {{ fileSaving ? '업로드 중' : '업로드' }}
        </button>
      </div>
      <div class="profile-file-list">
        <article v-for="file in workFiles" :key="file.fileId" class="list-panel">
          <div>
            <strong>{{ file.originalName }}</strong>
            <p>{{ fileStatusLabel(file.status) }} · {{ formatBytes(file.sizeBytes) }} · {{ file.durationSeconds || '-' }}초</p>
            <div class="subline">
              <span v-if="file.teamName">{{ file.teamName }}</span>
              <span>연결 {{ fileReferenceCount(file) }}건</span>
              <span v-if="file.physicalDeleteDueAt">삭제 예정 {{ String(file.physicalDeleteDueAt).slice(0, 10) }}</span>
              <span v-if="file.holdReason">{{ fileReasonLabel(file.holdReason) }}</span>
            </div>
          </div>
          <div class="row-actions">
            <a v-if="file.downloadUrl" class="ghost-button inline" :href="file.downloadUrl" target="_blank" rel="noreferrer">다운로드</a>
            <button v-if="file.status === 'ACTIVE'" class="ghost-button danger" type="button" :disabled="fileActionId === file.fileId || !canDeleteMyFile(file)" @click="pendingFileDeleteId = file.fileId">삭제</button>
            <button v-if="file.status === 'DELETED'" class="ghost-button" type="button" :disabled="fileActionId === file.fileId || !canRestoreMyFile(file)" @click="restoreMyFile(file)">복구</button>
          </div>
          <div v-if="pendingFileDeleteId === file.fileId" class="confirm-inline danger-confirm portfolio-confirm">
            <span>이 파일을 삭제 대기 상태로 전환할까요?</span>
            <button class="ghost-button danger" type="button" :disabled="fileActionId === file.fileId" @click="deleteMyFile(file)">삭제 확인</button>
            <button class="ghost-button" type="button" @click="pendingFileDeleteId = null">취소</button>
          </div>
        </article>
        <p v-if="!filesLoading && workFiles.length === 0" class="muted">업로드 파일이 없습니다.</p>
      </div>
    </section>

    <section v-if="route.name === 'profile-youtube'" class="form-panel profile-youtube-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">영상 연결</span>
          <h2>YouTube 포트폴리오 업로드</h2>
        </div>
      </div>
      <p v-if="youtubeError" class="error-text">{{ youtubeError }}</p>
      <p v-if="youtubeSaved" class="notice-text">{{ youtubeSaved }}</p>
      <div class="profile-youtube-connect">
        <label class="field">
          <span>YouTube URL</span>
          <input v-model="youtubeUrl" maxlength="500" placeholder="https://www.youtube.com/watch?v=..." @input="clearYoutubePreview">
        </label>
        <button class="ghost-button" type="button" :disabled="youtubePreviewLoading || !youtubeUrl.trim()" @click="previewProfileYoutube">
          {{ youtubePreviewLoading ? '확인 중' : '메타데이터 확인' }}
        </button>
      </div>
      <article v-if="youtubePreview" class="youtube-preview-card profile-youtube-preview">
        <figure class="youtube-preview-thumb">
          <img v-if="youtubePreview.thumbnailUrl" :src="youtubePreview.thumbnailUrl" alt="">
          <span v-else>NO THUMBNAIL</span>
        </figure>
        <div class="youtube-preview-meta">
          <span class="youtube-preview-kicker">확인된 영상</span>
          <strong>{{ youtubePreview.title }}</strong>
          <p>{{ youtubePreview.channelTitle }}</p>
          <div class="subline">
            <span>{{ formatYoutubeDuration(youtubePreview.durationSeconds) }}</span>
            <span>{{ youtubePreview.videoId }}</span>
          </div>
          <button class="primary-button inline" type="button" :disabled="portfolioSaving || !profile?.profileId" @click="connectYoutubeToPortfolio">YouTube 포트폴리오에 추가</button>
        </div>
      </article>
      <div class="profile-youtube-list">
        <h3>업로드된 YouTube 포트폴리오</h3>
        <article v-for="item in youtubePortfolioItems" :key="item.portfolioItemId" class="list-panel profile-youtube-row">
          <img v-if="item.thumbnailUrl" :src="item.thumbnailUrl" alt="">
          <div>
            <RouterLink :to="{ name: 'profile-portfolio-detail', params: { portfolioId: item.portfolioItemId } }"><strong>{{ item.title }}</strong></RouterLink>
            <p>{{ item.description || 'YouTube 포트폴리오' }}</p>
          </div>
          <div class="row-actions">
            <a class="ghost-button inline" :href="item.url" target="_blank" rel="noreferrer">영상 열기</a>
            <RouterLink class="ghost-button inline" :to="{ name: 'profile-portfolio-edit', params: { portfolioId: item.portfolioItemId } }">연결 수정</RouterLink>
            <button class="ghost-button danger" type="button" :disabled="portfolioSaving" @click="pendingYoutubeDeleteId = item.portfolioItemId">연결 삭제</button>
          </div>
          <div v-if="pendingYoutubeDeleteId === item.portfolioItemId" class="confirm-inline danger-confirm portfolio-confirm">
            <span>이 YouTube 연결과 포트폴리오 항목을 삭제할까요?</span>
            <button class="ghost-button danger" type="button" :disabled="portfolioSaving" @click="removePortfolioItem(item); pendingYoutubeDeleteId = null">삭제 확인</button>
            <button class="ghost-button" type="button" @click="pendingYoutubeDeleteId = null">취소</button>
          </div>
        </article>
        <p v-if="youtubePortfolioItems.length === 0" class="muted">업로드된 YouTube 포트폴리오가 없습니다.</p>
      </div>
    </section>

    <section v-if="showsPublicDataPanel" class="form-panel public-data-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">대체 검색</span>
          <h2>공공데이터</h2>
        </div>
        <button class="ghost-button" type="button" :disabled="publicDataLoading" @click="searchPublicData">
          {{ publicDataLoading ? '검색 중' : '검색' }}
        </button>
      </div>

      <p v-if="publicDataError" class="error-text">{{ publicDataError }}</p>

      <div class="form-grid">
        <label class="field">
          <span>검색어</span>
          <input v-model="publicDataForm.keyword" maxlength="80" @keyup.enter="searchPublicData">
        </label>
        <label class="field">
          <span>유형</span>
          <select v-model="publicDataForm.itemType">
            <option v-for="item in publicDataTypes" :key="item.code" :value="item.code">{{ item.displayName }}</option>
          </select>
        </label>
      </div>

      <div v-if="publicDataResults.length" class="portfolio-list">
        <article v-for="item in publicDataResults" :key="item.publicDataSyncItemId" class="list-panel portfolio-row">
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.description || item.sourceName }}</p>
            <div class="subline">
              <span>{{ item.itemType }}</span>
              <span v-if="item.displayYear">{{ item.displayYear }}</span>
              <span v-if="item.creatorName">{{ item.creatorName }}</span>
            </div>
          </div>
          <div class="row-actions">
            <a v-if="item.providerUrl" class="ghost-button inline" :href="item.providerUrl" target="_blank" rel="noreferrer">원문</a>
            <button class="primary-button" type="button" :disabled="portfolioSaving || !profile?.profileId" @click="addPublicDataItem(item)">
              추가
            </button>
          </div>
        </article>
      </div>
      <p v-else class="muted">검색 결과가 없습니다.</p>
    </section>
    </div>
  </section>
  </section>
</template>
