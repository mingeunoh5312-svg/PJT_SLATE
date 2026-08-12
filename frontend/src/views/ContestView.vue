<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProtectedImage from '../components/media/ProtectedImage.vue'
import { defaultContestImage } from '../constants/defaultImages'
import { slateApi } from '../services/api'
import {
  contestDeadlineOptions,
  contestListRegionOptions,
  contestOrganizerOptions,
  contestRegionOptions,
  contestTargetOptions,
  contestTypeOptions,
  totalPrizeBands
} from '../constants/contestFilters'

const props = defineProps({ currentUser: Object })
const emit = defineEmits(['login'])
const route = useRoute()
const router = useRouter()

const contests = ref([])
const urgentContestRows = ref([])
const selected = ref(null)
const bases = ref({ profile: null, teams: [] })
const basisKey = ref('')
const fitResult = ref(null)
const fitAnalyzing = ref(false)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const saved = ref('')
const companyRequests = ref([])
const companyManagedContests = ref([])
const companyDocuments = ref([])
const creatingRequest = ref(false)
const updatingCompanyContest = ref(false)
const editingCompanyContestId = ref(null)
const uploadingCompanyDocument = ref(false)
const companyDocumentActionId = ref(null)
const selectedCompanyDocumentFile = ref(null)
const companyDocumentType = ref('BUSINESS_REGISTRATION')
const pendingCompanyDocumentDeleteId = ref(null)
const pendingCompanyContestEndId = ref(null)
const contestSearch = ref('')
const contestFilters = reactive({
  targetCodes: [],
  regionMode: '',
  totalPrizeBand: '',
  deadlineWithinDays: '',
  contestType: ''
})
const contestPageSizeOptions = [10, 20, 50]
const contestPageSize = ref(10)
const contestCurrentPage = ref(1)
const requestImageFile = ref(null)
const requestImagePreview = ref('')
const requestImageObjectUrls = ref({})
const companyContestImageFile = ref(null)
const companyContestImagePreview = ref('')
const companyContestImageDelete = ref(false)

const prepareForm = reactive({
  checklistText: '',
  memo: ''
})
const requestForm = reactive({
  title: '',
  summary: '',
  theme: '',
  prizeText: '',
  totalPrizeAmount: '',
  firstPrizeAmount: '',
  organizer: '',
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
const companyContestForm = reactive({
  title: '',
  summary: '',
  theme: '',
  prizeText: '',
  totalPrizeAmount: '',
  firstPrizeAmount: '',
  organizer: '',
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

const isContestListRoute = computed(() => route.name === 'contests')
const isContestDetailRoute = computed(() => route.name === 'contests-detail')
const isContestPrepareRoute = computed(() => route.name === 'contests-prepare')
const isContestEditRequestRoute = computed(() => route.name === 'contests-edit-request')
const isContestRequestCreateRoute = computed(() => ['contests-new-request', 'contests-company-new'].includes(route.name))
const isContestRequestsRoute = computed(() => route.name === 'contests-requests')
const isCompanyContestListRoute = computed(() => route.name === 'contests-company')
const isCompanyContestEditRoute = computed(() => route.name === 'contests-company-edit')
const isCompanyWorkspaceRoute = computed(() => (
  isContestRequestCreateRoute.value
  || isContestRequestsRoute.value
  || isCompanyContestListRoute.value
  || isCompanyContestEditRoute.value
))
const isCompanyUser = computed(() => props.currentUser?.accountType === 'COMPANY')
const routeContestId = computed(() => route.params.contestId)
const contestListView = computed(() => route.query.view === 'saved' ? 'saved' : 'all')
const filteredContests = computed(() => contestListView.value === 'saved'
  ? contests.value.filter((contest) => Boolean(contest.savedByCurrentUser))
  : contests.value)
const contestTotalPages = computed(() => Math.max(1, Math.ceil(filteredContests.value.length / contestPageSize.value)))
const pagedContests = computed(() => {
  const start = (contestCurrentPage.value - 1) * contestPageSize.value
  return filteredContests.value.slice(start, start + contestPageSize.value)
})
const contestPageNumbers = computed(() => {
  const total = contestTotalPages.value
  const current = contestCurrentPage.value
  const first = Math.max(1, Math.min(current - 2, total - 4))
  const last = Math.min(total, first + 4)
  return Array.from({ length: last - first + 1 }, (_, index) => first + index)
})
const activeContestFilterCount = computed(() => [
  ...contestFilters.targetCodes,
  contestFilters.regionMode,
  contestFilters.totalPrizeBand,
  contestFilters.deadlineWithinDays,
  contestFilters.contestType,
  contestSearch.value.trim()
].filter(Boolean).length)

const basis = computed(() => {
  if (!basisKey.value) return { basisType: '', basisId: '' }
  const [basisType, basisId] = basisKey.value.split(':')
  return { basisType, basisId: Number(basisId) }
})

const basisOptions = computed(() => {
  const options = []
  if (bases.value.profile?.profileId) {
    options.push({
      key: `PROFILE:${bases.value.profile.profileId}`,
      label: `프로필 · ${bases.value.profile.title}`
    })
  }
  for (const team of bases.value.teams || []) {
    options.push({
      key: `TEAM:${team.teamId}`,
      label: `팀 · ${team.title}`
    })
  }
  return options
})
function fitQuery() {
  return basis.value.basisType ? basis.value : {}
}

function contestImageSources(item) {
  return [
    item?.uploadedImageUrl,
    item?.requestImageUrl,
    item?.representativeImageUrl,
    defaultContestImage
  ].map((value) => String(value || '').trim()).filter(Boolean)
}

function contestImage(item) {
  return contestImageSources(item)[0] || defaultContestImage
}

function textOrBlank(value) {
  return value ? String(value).trim() : ''
}

function contestSourceLabel(contest) {
  return textOrBlank(contest?.sourceAttribution)
}

function normalizedUrl(value) {
  const text = textOrBlank(value)
  if (!text) return ''
  try {
    const url = new URL(text)
    if (!['http:', 'https:'].includes(url.protocol)) return ''
    url.hash = ''
    return url.href
  } catch {
    return ''
  }
}

function normalizedUrlHost(value) {
  const url = normalizedUrl(value)
  if (!url) return ''
  return new URL(url).hostname.toLowerCase().replace(/^www\./, '')
}

function isContestKoreaUrl(value) {
  const host = normalizedUrlHost(value)
  return host === 'contestkorea.com' || host.endsWith('.contestkorea.com')
}

function sameNormalizedUrl(first, second) {
  const firstUrl = normalizedUrl(first)
  const secondUrl = normalizedUrl(second)
  return Boolean(firstUrl && secondUrl && firstUrl === secondUrl)
}

function contestOfficialUrl(contest) {
  const externalUrl = normalizedUrl(contest?.externalUrl)
  if (!externalUrl) return ''
  if (sameNormalizedUrl(externalUrl, contest?.sourceUrl) || isContestKoreaUrl(externalUrl)) return ''
  return externalUrl
}

function contestSourceUrl(contest) {
  return normalizedUrl(contest?.sourceUrl)
}

function contestPrimaryExternalUrl(contest) {
  return contestOfficialUrl(contest) || contestSourceUrl(contest)
}

function contestPrimaryExternalLabel(contest) {
  if (contestOfficialUrl(contest)) return '주최사 공고 보기'
  if (!contestSourceUrl(contest)) return ''
  return isContestKoreaUrl(contestSourceUrl(contest)) ? '콘테스트코리아 원문 보기' : '수집 원문 보기'
}

function contestSourceLinkLabel(contest) {
  if (!contestSourceUrl(contest)) return ''
  return isContestKoreaUrl(contestSourceUrl(contest)) ? '콘테스트코리아 원문 보기' : '수집 원문 보기'
}

function hasContestSourceUrl(contest) {
  return Boolean(contestSourceUrl(contest))
}

function hasContestSource(contest) {
  return Boolean(contestSourceLabel(contest) || hasContestSourceUrl(contest))
}

function contestPrimaryExternalAriaLabel(contest) {
  return `${contest?.title || '공모전'} ${contestPrimaryExternalLabel(contest)}`.trim()
}

function contestSourceAriaLabel(contest) {
  return `${contest?.title || '공모전'} ${contestSourceLinkLabel(contest)}`.trim()
}

function contestSubmissionMethod(contest) {
  if (textOrBlank(contest?.submissionEmail)) return textOrBlank(contest.submissionEmail)
  const officialUrl = contestOfficialUrl(contest)
  if (officialUrl) {
    const host = normalizedUrlHost(officialUrl)
    return host ? `외부 링크 (${host})` : '외부 링크'
  }
  return '별도 안내 예정'
}

function isContestKoreaPoster(contest) {
  return textOrBlank(contest?.posterSourceType).toUpperCase() === 'CONTESTKOREA_ALLOWED'
}

function contestCollectedDate(contest) {
  return formatContestDate(contest?.sourceCollectedAt)
}

function contestPosterCollectedDate(contest) {
  return formatContestDate(contest?.posterCollectedAt)
}

function hasContestDetailSourcePanel(contest) {
  return Boolean(
    hasContestSource(contest)
    || isContestKoreaPoster(contest)
    || contestCollectedDate(contest)
    || contestPosterCollectedDate(contest)
  )
}

function editingCompanyContest() {
  return companyManagedContests.value.find((item) => Number(item.contestId) === Number(editingCompanyContestId.value))
}

function companyContestPreviewSrc() {
  return companyContestImagePreview.value || contestImage(editingCompanyContest())
}

function handleContestImageError(event) {
  if (!event?.currentTarget) return
  event.currentTarget.onerror = null
  event.currentTarget.src = defaultContestImage
}

function formatContestDate(value) {
  if (!value) return ''
  return String(value).slice(0, 10).replaceAll('-', '.')
}

function dDayLabel(value) {
  const day = Number(value)
  if (!Number.isFinite(day)) return '마감일 확인'
  if (day < 0) return `마감 +${Math.abs(day)}`
  return `마감 D-${day}`
}

async function openContestDetail(item) {
  const contestId = item?.contest?.contestId || item?.contestId || item?.id
  if (!contestId) return
  await router.push({ name: 'contests-detail', params: { contestId } })
}

function openContestRequestPanel() {
  if (!isCompanyUser.value) return
  router.push({ name: 'contests-company-new' })
}

function splitChecklist(text) {
  return text
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean)
}

function resetPrepareForm(preparation) {
  prepareForm.checklistText = (preparation?.checklistItems || []).join('\n')
  prepareForm.memo = preparation?.memo || ''
}

function resetRequestForm() {
  revokePreview(requestImagePreview)
  requestImageFile.value = null
  requestForm.title = ''
  requestForm.summary = ''
  requestForm.theme = ''
  requestForm.prizeText = ''
  requestForm.totalPrizeAmount = ''
  requestForm.firstPrizeAmount = ''
  requestForm.organizer = props.currentUser?.nickname || ''
  requestForm.organizerType = ''
  requestForm.representativeImageUrl = ''
  requestForm.submissionEmail = ''
  requestForm.externalUrl = ''
  requestForm.targetText = ''
  requestForm.targetCodes = []
  requestForm.regionCodes = []
  requestForm.requiredRolesText = ''
  requestForm.relatedGenresText = ''
  requestForm.startAt = ''
  requestForm.deadlineAt = ''
}

function resetCompanyContestForm() {
  revokePreview(companyContestImagePreview)
  companyContestImageFile.value = null
  companyContestImageDelete.value = false
  editingCompanyContestId.value = null
  pendingCompanyContestEndId.value = null
  companyContestForm.title = ''
  companyContestForm.summary = ''
  companyContestForm.theme = ''
  companyContestForm.prizeText = ''
  companyContestForm.totalPrizeAmount = ''
  companyContestForm.firstPrizeAmount = ''
  companyContestForm.organizer = ''
  companyContestForm.organizerType = ''
  companyContestForm.representativeImageUrl = ''
  companyContestForm.submissionEmail = ''
  companyContestForm.externalUrl = ''
  companyContestForm.targetText = ''
  companyContestForm.targetCodes = []
  companyContestForm.regionCodes = []
  companyContestForm.requiredRolesText = ''
  companyContestForm.relatedGenresText = ''
  companyContestForm.startAt = ''
  companyContestForm.deadlineAt = ''
}

function revokePreview(preview) {
  if (preview.value?.startsWith('blob:')) URL.revokeObjectURL(preview.value)
  preview.value = ''
}

function revokeRequestImageObjects() {
  Object.values(requestImageObjectUrls.value).forEach((url) => URL.revokeObjectURL(url))
  requestImageObjectUrls.value = {}
}

function selectImageFile(event, fileRef, previewRef) {
  const file = event.target.files?.[0] || null
  revokePreview(previewRef)
  fileRef.value = file
  if (file) previewRef.value = URL.createObjectURL(file)
}

function onRequestImageChange(event) {
  selectImageFile(event, requestImageFile, requestImagePreview)
}

function onCompanyContestImageChange(event) {
  selectImageFile(event, companyContestImageFile, companyContestImagePreview)
  companyContestImageDelete.value = false
}

function contestPayload(form) {
  return {
    contestType: 'INTERNAL',
    title: form.title,
    summary: form.summary,
    theme: form.theme,
    prizeText: form.prizeText,
    totalPrizeAmount: numberOrNull(form.totalPrizeAmount),
    firstPrizeAmount: numberOrNull(form.firstPrizeAmount),
    organizer: form.organizer || props.currentUser?.nickname,
    organizerType: form.organizerType || null,
    representativeImageUrl: form.representativeImageUrl,
    submissionEmail: form.submissionEmail,
    externalUrl: form.externalUrl,
    targetText: form.targetText,
    targetCodes: [...form.targetCodes],
    regionCodes: [...form.regionCodes],
    requiredRolesText: form.requiredRolesText,
    relatedGenresText: form.relatedGenresText,
    startAt: form.startAt,
    deadlineAt: form.deadlineAt
  }
}

function numberOrNull(value) {
  return value === '' || value === null || value === undefined ? null : Number(value)
}

function csvQuery(value) {
  return Array.isArray(value) ? value.join(',') : String(value || '')
}

function csvValues(value) {
  return String(value || '').split(',').map((item) => item.trim()).filter(Boolean)
}

function selectedPrizeBand(bands, value) {
  return bands.find((band) => band.value === value) || bands[0]
}

function optionValueOrBlank(options, value) {
  const text = String(Array.isArray(value) ? value[0] : value || '')
  return options.some((option) => option.value === text) ? text : ''
}

function contestFilterParams() {
  const totalBand = selectedPrizeBand(totalPrizeBands, contestFilters.totalPrizeBand)
  return {
    keyword: contestSearch.value.trim(),
    contestType: contestFilters.contestType,
    deadlineWithinDays: contestFilters.deadlineWithinDays,
    target: contestFilters.targetCodes,
    region: contestFilters.regionMode ? [contestFilters.regionMode] : [],
    totalPrizeMin: totalBand.min,
    totalPrizeMax: totalBand.max
  }
}

function contestFilterQuery(view = contestListView.value) {
  const query = {}
  if (view === 'saved') query.view = 'saved'
  if (contestSearch.value.trim()) query.q = contestSearch.value.trim()
  if (contestFilters.targetCodes.length) query.target = csvQuery(contestFilters.targetCodes)
  if (contestFilters.regionMode) query.region = contestFilters.regionMode
  if (contestFilters.totalPrizeBand) query.totalPrize = contestFilters.totalPrizeBand
  if (contestFilters.deadlineWithinDays) query.deadline = contestFilters.deadlineWithinDays
  if (contestFilters.contestType) query.type = contestFilters.contestType
  return query
}

function restoreContestFilters() {
  contestSearch.value = String(route.query.q || '')
  contestFilters.targetCodes = csvValues(route.query.target)
  contestFilters.regionMode = optionValueOrBlank(contestListRegionOptions, route.query.region)
  contestFilters.totalPrizeBand = optionValueOrBlank(totalPrizeBands, route.query.totalPrize)
  contestFilters.deadlineWithinDays = optionValueOrBlank(contestDeadlineOptions, route.query.deadline)
  contestFilters.contestType = optionValueOrBlank(contestTypeOptions, route.query.type)
}

async function applyContestFilters() {
  const query = contestFilterQuery()
  if (JSON.stringify(query) === JSON.stringify(route.query)) await loadContests()
  else await router.replace({ name: 'contests', query })
}

async function resetContestFilters() {
  contestSearch.value = ''
  contestFilters.targetCodes = []
  contestFilters.regionMode = ''
  contestFilters.totalPrizeBand = ''
  contestFilters.deadlineWithinDays = ''
  contestFilters.contestType = ''
  const query = contestListView.value === 'saved' ? { view: 'saved' } : {}
  if (Object.keys(route.query).length) await router.replace({ name: 'contests', query })
  else await loadContests()
}

function setContestPage(page) {
  const nextPage = Math.min(Math.max(Number(page) || 1, 1), contestTotalPages.value)
  contestCurrentPage.value = nextPage
}

async function loadBases() {
  if (!props.currentUser) {
    bases.value = { profile: null, teams: [] }
    basisKey.value = ''
    return
  }
  try {
    bases.value = await slateApi.contestBases()
    if (!basisKey.value && basisOptions.value[0]) basisKey.value = basisOptions.value[0].key
  } catch (err) {
    bases.value = { profile: null, teams: [] }
    basisKey.value = ''
    error.value = err.message
  }
}

async function loadContests(preferredContestId) {
  loading.value = true
  error.value = ''
  saved.value = ''
  try {
    const list = await slateApi.contests({
      status: 'OPEN',
      sort: 'deadline',
      ...contestFilterParams(),
      limit: 500
    })
    contests.value = list
    contestCurrentPage.value = 1
    if (preferredContestId) {
      const next = list.find((contest) => Number(contest.contestId) === Number(preferredContestId))
      if (next) await selectContest(next)
    } else {
      selected.value = null
    }
  } catch (err) {
    contests.value = []
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function loadUrgentContests() {
  try {
    urgentContestRows.value = await slateApi.urgentContests(4)
  } catch (err) {
    urgentContestRows.value = []
    error.value = err.message
  }
}

async function loadCompanyRequests() {
  if (props.currentUser?.accountType !== 'COMPANY') {
    revokeRequestImageObjects()
    companyRequests.value = []
    return
  }
  try {
    const requests = await slateApi.myContestOpenRequests()
    revokeRequestImageObjects()
    const objectUrls = {}
    await Promise.all(requests.map(async (request) => {
      if (!request.requestImageUrl) return
      try {
        const blob = await slateApi.entityImageBlob('CONTEST_REQUEST', request.requestId)
        objectUrls[request.requestId] = URL.createObjectURL(blob)
      } catch {
        // Keep the request row visible even when the stored image cannot be read.
      }
    }))
    requestImageObjectUrls.value = objectUrls
    companyRequests.value = requests
  } catch (err) {
    error.value = err.message
  }
}

async function loadCompanyManagedContests() {
  if (props.currentUser?.accountType !== 'COMPANY') {
    companyManagedContests.value = []
    return
  }
  try {
    companyManagedContests.value = await slateApi.myManagedContests()
  } catch (err) {
    error.value = err.message
  }
}

async function loadCompanyDocuments() {
  if (props.currentUser?.accountType !== 'COMPANY') {
    companyDocuments.value = []
    return
  }
  try {
    companyDocuments.value = await slateApi.myCompanyDocuments()
  } catch (err) {
    error.value = err.message
  }
}

async function selectContest(contest) {
  error.value = ''
  fitResult.value = null
  try {
    selected.value = await slateApi.contest(contest.contestId, isContestPrepareRoute.value ? fitQuery() : {})
    resetPrepareForm(selected.value.preparation)
  } catch (err) {
    error.value = err.message
  }
}

async function loadRouteContest() {
  const contestId = routeContestId.value
  if (!contestId) {
    selected.value = null
    return
  }
  loading.value = true
  try {
    await selectContest({ contestId })
  } finally {
    loading.value = false
  }
}

async function toggleSave(contest) {
  if (!props.currentUser) {
    emit('login')
    return
  }
  if (!contest?.contestId) return
  saving.value = true
  error.value = ''
  try {
    const result = await slateApi.toggleContestSave(contest.contestId)
    contest.savedByCurrentUser = result.saved ? 1 : 0
    contest.saveCount = result.saveCount
    if (selected.value?.contestId === contest.contestId) {
      selected.value.savedByCurrentUser = contest.savedByCurrentUser
      selected.value.saveCount = result.saveCount
    }
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function calculateFit(contest = selected.value) {
  if (!props.currentUser) {
    emit('login')
    return
  }
  if (!basis.value.basisType || !contest) {
    error.value = '적합도 기준을 먼저 선택해주세요.'
    return
  }
  fitAnalyzing.value = true
  error.value = ''
  saved.value = ''
  try {
    fitResult.value = await slateApi.calculateContestFit(contest.contestId, basis.value)
    saved.value = '적합도 분석이 완료되었습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    fitAnalyzing.value = false
  }
}

async function savePreparation() {
  if (!props.currentUser) {
    emit('login')
    return
  }
  if (!selected.value || !basis.value.basisType) {
    error.value = '공모전과 적합도 기준을 먼저 선택해주세요.'
    return
  }
  if (!selected.value.contestId) return
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    const preparation = await slateApi.saveContestPreparation(selected.value.contestId, {
      ...basis.value,
      checklistItems: splitChecklist(prepareForm.checklistText),
      memo: prepareForm.memo
    })
    selected.value.preparation = preparation
    resetPrepareForm(preparation)
    saved.value = '제출 준비가 저장되었습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function submitContestRequest() {
  if (props.currentUser?.accountType !== 'COMPANY') {
    emit('login')
    return
  }
  creatingRequest.value = true
  error.value = ''
  saved.value = ''
  try {
    const created = await slateApi.createContestOpenRequest({
      ...contestPayload(requestForm),
      organizer: requestForm.organizer || props.currentUser.nickname
    })
    if (requestImageFile.value) {
      await slateApi.uploadEntityImage('CONTEST_REQUEST', created.requestId, requestImageFile.value)
    }
    saved.value = '공모전 개설 요청을 접수했습니다.'
    resetRequestForm()
    await loadCompanyRequests()
    await router.push({ name: 'contests-requests' })
  } catch (err) {
    error.value = err.message
  } finally {
    creatingRequest.value = false
  }
}

function editCompanyContest(contest) {
  if (isCompanyContestListRoute.value && contest?.contestId) {
    router.push({ name: 'contests-company-edit', params: { contestId: contest.contestId } })
    return
  }
  pendingCompanyContestEndId.value = null
  revokePreview(companyContestImagePreview)
  companyContestImageFile.value = null
  companyContestImageDelete.value = false
  editingCompanyContestId.value = contest.contestId
  companyContestForm.title = contest.title || ''
  companyContestForm.summary = contest.summary || ''
  companyContestForm.theme = contest.theme || ''
  companyContestForm.prizeText = contest.prizeText || ''
  companyContestForm.totalPrizeAmount = contest.totalPrizeAmount ?? ''
  companyContestForm.firstPrizeAmount = contest.firstPrizeAmount ?? ''
  companyContestForm.organizer = contest.organizer || ''
  companyContestForm.organizerType = contest.organizerType || ''
  companyContestForm.representativeImageUrl = contest.representativeImageUrl || ''
  companyContestForm.submissionEmail = contest.submissionEmail || ''
  companyContestForm.externalUrl = contest.externalUrl || ''
  companyContestForm.targetText = contest.targetText || ''
  companyContestForm.targetCodes = [...(contest.targetCodes || [])]
  companyContestForm.regionCodes = [...(contest.regionCodes || [])]
  companyContestForm.requiredRolesText = contest.requiredRolesText || ''
  companyContestForm.relatedGenresText = contest.relatedGenresText || ''
  companyContestForm.startAt = toDateTimeLocal(contest.startAt)
  companyContestForm.deadlineAt = toDateTimeLocal(contest.deadlineAt)
}

async function updateCompanyContest() {
  if (!editingCompanyContestId.value) return
  const contestId = editingCompanyContestId.value
  updatingCompanyContest.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.updateManagedContest(contestId, contestPayload(companyContestForm))
    if (companyContestImageFile.value) {
      await slateApi.uploadEntityImage('CONTEST', contestId, companyContestImageFile.value)
    } else if (companyContestImageDelete.value) {
      await slateApi.deleteEntityImage('CONTEST', contestId)
    }
    resetCompanyContestForm()
    await loadCompanyManagedContests()
    saved.value = '승인된 공모전 정보를 수정했습니다.'
    await router.push({ name: 'contests-detail', params: { contestId } })
  } catch (err) {
    error.value = err.message
  } finally {
    updatingCompanyContest.value = false
  }
}

function cancelCompanyContestEdit() {
  resetCompanyContestForm()
  router.push({ name: 'contests-company' })
}

async function endCompanyContest(contest) {
  if (!contest) return
  updatingCompanyContest.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.updateManagedContestStatus(contest.contestId, {
      status: 'ENDED',
      reason: '회사 계정 종료 처리'
    })
    pendingCompanyContestEndId.value = null
    await loadCompanyManagedContests()
    await loadContests(selected.value?.contestId)
    saved.value = '공모전을 종료했습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    updatingCompanyContest.value = false
  }
}

function requestCompanyContestEnd(contest) {
  if (!contest || contest.status !== 'OPEN') return
  editingCompanyContestId.value = null
  pendingCompanyContestEndId.value = contest.contestId
}

function onCompanyDocumentFileChange(event) {
  selectedCompanyDocumentFile.value = event.target.files?.[0] || null
}

async function uploadCompanyDocument() {
  if (!selectedCompanyDocumentFile.value) {
    error.value = '업로드할 회사 서류 파일을 선택해주세요.'
    return
  }
  uploadingCompanyDocument.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.uploadMyCompanyDocument(selectedCompanyDocumentFile.value, companyDocumentType.value)
    selectedCompanyDocumentFile.value = null
    saved.value = '회사 서류를 업로드했습니다.'
    await loadCompanyDocuments()
  } catch (err) {
    error.value = err.message
  } finally {
    uploadingCompanyDocument.value = false
  }
}

async function deleteCompanyDocument(document) {
  if (!document) return
  companyDocumentActionId.value = document.documentId
  error.value = ''
  saved.value = ''
  try {
    await slateApi.deleteMyCompanyDocument(document.documentId)
    pendingCompanyDocumentDeleteId.value = null
    saved.value = '회사 서류를 삭제했습니다.'
    await loadCompanyDocuments()
  } catch (err) {
    error.value = err.message
  } finally {
    companyDocumentActionId.value = null
  }
}

function requestCompanyDocumentDelete(document) {
  if (!document) return
  pendingCompanyDocumentDeleteId.value = document.documentId
}

function toDateTimeLocal(value) {
  if (!value) return ''
  return String(value).replace(' ', 'T').slice(0, 16)
}

function formatBytes(value) {
  const bytes = Number(value || 0)
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)}MB`
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)}KB`
  return `${bytes}B`
}

function documentTypeLabel(value) {
  return {
    BUSINESS_REGISTRATION: '사업자등록증',
    COMPANY_PROFILE: '회사소개서',
    PORTFOLIO: '포트폴리오',
    OTHER: '기타'
  }[value] || value
}

async function loadRoute() {
  error.value = ''
  saved.value = ''
  selected.value = null
  resetCompanyContestForm()
  if (isContestListRoute.value) {
    restoreContestFilters()
    await Promise.all([loadContests(), loadUrgentContests()])
    return
  }
  if (isContestDetailRoute.value || isContestPrepareRoute.value || isContestEditRequestRoute.value) {
    await loadBases()
    await Promise.all([
      loadRouteContest(),
      isCompanyUser.value ? loadCompanyManagedContests() : Promise.resolve()
    ])
    return
  }
  if (!isCompanyUser.value) return

  if (isContestRequestCreateRoute.value) {
    if (!requestForm.organizer) resetRequestForm()
    await loadCompanyDocuments()
    return
  }
  if (isContestRequestsRoute.value) {
    await loadCompanyRequests()
    return
  }
  if (isCompanyContestListRoute.value) {
    await loadCompanyManagedContests()
    return
  }
  if (isCompanyContestEditRoute.value) {
    await loadCompanyManagedContests()
    const contest = companyManagedContests.value.find((item) => Number(item.contestId) === Number(routeContestId.value))
    if (contest) editCompanyContest(contest)
    else error.value = '수정 권한이 있는 기업 공모전을 찾을 수 없습니다.'
  }
}

watch(
  [() => props.currentUser?.userId, () => route.fullPath],
  loadRoute,
  { immediate: true }
)

watch(contestPageSize, () => {
  contestCurrentPage.value = 1
})

watch(filteredContests, () => {
  if (contestCurrentPage.value > contestTotalPages.value) {
    contestCurrentPage.value = contestTotalPages.value
  }
})

watch(basisKey, async () => {
  fitResult.value = null
  if (isContestPrepareRoute.value) await loadRouteContest()
})

onBeforeUnmount(() => {
  revokePreview(requestImagePreview)
  revokePreview(companyContestImagePreview)
  revokeRequestImageObjects()
})
</script>

<template>
  <section class="contest-page">
    <nav v-if="!isContestListRoute && (isCompanyUser || props.currentUser?.accountType === 'ADMIN')" class="contest-route-nav" aria-label="공모전 관리 메뉴">
      <RouterLink v-if="isCompanyUser" :to="{ name: 'contests-company-new' }" :class="{ active: isContestRequestCreateRoute }">개설 요청</RouterLink>
      <RouterLink v-if="isCompanyUser" :to="{ name: 'contests-requests' }" :class="{ active: isContestRequestsRoute }">요청 내역</RouterLink>
      <RouterLink v-if="isCompanyUser" :to="{ name: 'contests-company' }" :class="{ active: isCompanyContestListRoute || isCompanyContestEditRoute }">기업 공모전</RouterLink>
      <RouterLink v-if="props.currentUser?.accountType === 'ADMIN'" :to="{ name: 'admin-contests' }">관리자 관리</RouterLink>
    </nav>

    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="saved" class="notice-text">{{ saved }}</p>

    <section v-if="isContestListRoute" class="contest-data-view">
      <header class="contest-data-header">
        <div>
          <span class="eyebrow">Open Contests</span>
          <h1>공모전 목록</h1>
          <p>현재 접수 중인 공모전을 마감일이 가까운 순서로 확인하세요.</p>
        </div>
        <button v-if="isCompanyUser" class="primary-button" type="button" @click="openContestRequestPanel">공모전 개설 요청</button>
      </header>

      <nav class="contest-list-tabs" aria-label="공모전 목록 보기">
        <RouterLink :class="{ active: contestListView === 'all' }" :to="{ name: 'contests', query: contestFilterQuery('all') }">전체 공모전</RouterLink>
        <RouterLink :class="{ active: contestListView === 'saved' }" :to="{ name: 'contests', query: contestFilterQuery('saved') }">저장한 공모전</RouterLink>
      </nav>

      <details class="contest-advanced-filter" open>
        <summary>
          <span>검색 필터</span>
          <strong v-if="activeContestFilterCount">{{ activeContestFilterCount }}개 조건</strong>
        </summary>
        <form @submit.prevent="applyContestFilters">
          <div class="contest-filter-search-row">
            <label>
              <span>검색어</span>
              <input v-model="contestSearch" type="search" placeholder="공모전명, 주최기관, 내용 검색">
            </label>
          </div>
          <div class="contest-filter-table">
            <div class="contest-filter-row">
              <strong>유형</strong>
              <div class="contest-filter-options compact-options">
                <label v-for="option in contestTypeOptions" :key="option.value || 'all'">
                  <input v-model="contestFilters.contestType" type="radio" :value="option.value">
                  {{ option.label }}
                </label>
              </div>
            </div>
            <div class="contest-filter-row">
              <strong>대상</strong>
              <div class="contest-filter-options">
                <label v-for="option in contestTargetOptions" :key="option.value">
                  <input v-model="contestFilters.targetCodes" type="checkbox" :value="option.value">
                  {{ option.label }}
                </label>
              </div>
            </div>
            <div class="contest-filter-row">
              <strong>지역</strong>
              <div class="contest-filter-options compact-options">
                <label v-for="option in contestListRegionOptions" :key="option.value || 'all'">
                  <input v-model="contestFilters.regionMode" type="radio" :value="option.value">
                  {{ option.label }}
                </label>
              </div>
            </div>
            <div class="contest-filter-row">
              <strong>마감 기간</strong>
              <div class="contest-filter-options compact-options">
                <label v-for="option in contestDeadlineOptions" :key="option.value || 'all'">
                  <input v-model="contestFilters.deadlineWithinDays" type="radio" :value="option.value">
                  {{ option.label }}
                </label>
              </div>
            </div>
            <div class="contest-filter-row">
              <strong>총상금</strong>
              <div class="contest-filter-options prize-options">
                <label v-for="band in totalPrizeBands" :key="band.value || 'all'">
                  <input v-model="contestFilters.totalPrizeBand" type="radio" :value="band.value">
                  {{ band.label }}
                </label>
              </div>
            </div>
          </div>
          <div class="contest-filter-actions">
            <button class="ghost-button" type="button" @click="resetContestFilters">필터 초기화</button>
            <button class="primary-button" type="submit" :disabled="loading">{{ loading ? '검색 중' : '조건 검색' }}</button>
          </div>
        </form>
      </details>

      <section class="contest-data-section contest-urgent-data-section">
        <div class="contest-data-title">
          <h2>마감 임박 공모전</h2>
          <span>최대 4건</span>
        </div>
        <div class="contest-urgent-data-list">
          <button v-for="contest in urgentContestRows" :key="contest.contestId" type="button" class="contest-urgent-data-row" @click="openContestDetail(contest)">
            <span class="contest-urgent-thumb contest-image-frame" :class="{ placeholder: !contestImage(contest) }">
              <ProtectedImage v-if="contestImage(contest)" :src="contestImage(contest)" :sources="contestImageSources(contest)" :fallback="defaultContestImage" :alt="`${contest.title} 대표 이미지`" @error="handleContestImageError" />
              <span v-else aria-hidden="true">SLATE</span>
            </span>
            <span>
              <strong>{{ contest.title }}</strong>
              <small>
                {{ contest.organizer }} · {{ formatContestDate(contest.deadlineAt) }}
                <template v-if="contestSourceLabel(contest)"> · {{ contestSourceLabel(contest) }}</template>
              </small>
            </span>
            <b>{{ dDayLabel(contest.dDay) }}</b>
          </button>
        </div>
        <p v-if="!loading && urgentContestRows.length === 0" class="contest-empty-state">마감 예정인 공모전이 없습니다.</p>
      </section>

      <section class="contest-data-section">
        <div class="contest-data-title contest-list-data-title">
          <div class="contest-title-count">
            <h2>{{ contestListView === 'saved' ? '저장한 공모전' : '접수 중인 공모전' }}</h2>
            <span>{{ filteredContests.length }}건</span>
          </div>
          <div class="contest-list-top-controls">
            <label class="contest-page-size">
              <span>표시</span>
              <select v-model.number="contestPageSize">
                <option v-for="size in contestPageSizeOptions" :key="size" :value="size">{{ size }}개씩</option>
              </select>
            </label>
            <nav class="contest-pagination compact" aria-label="공모전 목록 상단 페이지">
              <button type="button" :disabled="contestCurrentPage === 1" @click="setContestPage(contestCurrentPage - 1)">이전</button>
              <button
                v-for="page in contestPageNumbers"
                :key="page"
                type="button"
                :class="{ active: contestCurrentPage === page }"
                :aria-current="contestCurrentPage === page ? 'page' : undefined"
                @click="setContestPage(page)"
              >
                {{ page }}
              </button>
              <button type="button" :disabled="contestCurrentPage === contestTotalPages" @click="setContestPage(contestCurrentPage + 1)">다음</button>
            </nav>
          </div>
        </div>
        <div class="contest-data-list">
          <article v-for="contest in pagedContests" :key="contest.contestId" class="contest-data-row">
            <button class="contest-data-image contest-image-frame" :class="{ placeholder: !contestImage(contest) }" type="button" @click="openContestDetail(contest)">
              <ProtectedImage v-if="contestImage(contest)" :src="contestImage(contest)" :sources="contestImageSources(contest)" :fallback="defaultContestImage" :alt="`${contest.title} 대표 이미지`" @error="handleContestImageError" />
              <span v-else aria-hidden="true">SLATE CONTEST</span>
              <span v-if="isContestKoreaPoster(contest)" class="contest-poster-source-badge">콘테스트 제공</span>
            </button>
            <div class="contest-data-content">
              <div class="contest-data-heading">
                <span>{{ contest.organizer || '주최기관 미정' }}</span>
                <strong>{{ dDayLabel(contest.dDay) }}</strong>
              </div>
              <button type="button" class="contest-data-link" @click="openContestDetail(contest)">{{ contest.title }}</button>
              <p>{{ contest.summary }}</p>
              <div class="contest-tag-row">
                <span v-if="contest.targetText">{{ contest.targetText }}</span>
                <span v-if="contest.relatedGenresText">{{ contest.relatedGenresText }}</span>
              </div>
              <div v-if="hasContestSource(contest)" class="contest-source-row">
                <span v-if="contestSourceLabel(contest)" class="contest-source-badge">{{ contestSourceLabel(contest) }}</span>
                <a
                  v-if="hasContestSourceUrl(contest)"
                  class="contest-original-link"
                  :href="contestSourceUrl(contest)"
                  target="_blank"
                  rel="noopener noreferrer"
                  :aria-label="contestSourceAriaLabel(contest)"
                  @click.stop
                >
                  수집 원문
                </a>
              </div>
            </div>
            <div class="contest-data-meta">
              <span>마감일</span>
              <strong>{{ formatContestDate(contest.deadlineAt) || '미정' }}</strong>
              <span>상금/지원</span>
              <strong>{{ contest.prizeText || '상세 공고 참고' }}</strong>
              <div class="contest-data-actions">
                <button
                  class="contest-save-heart"
                  :class="{ saved: contest.savedByCurrentUser }"
                  type="button"
                  :disabled="saving"
                  :aria-label="contest.savedByCurrentUser ? `${contest.title} 저장 취소` : `${contest.title} 저장`"
                  :aria-pressed="Boolean(contest.savedByCurrentUser)"
                  @click="toggleSave(contest)"
                >
                  <span aria-hidden="true">{{ contest.savedByCurrentUser ? '♥' : '♡' }}</span>
                </button>
                <button class="primary-button" type="button" @click="openContestDetail(contest)">상세 보기</button>
              </div>
            </div>
          </article>
        </div>
        <p v-if="!loading && filteredContests.length === 0" class="contest-empty-state">{{ contestListView === 'saved' ? '저장한 공모전이 없습니다.' : '접수 중인 공모전이 없습니다.' }}</p>
        <nav v-if="filteredContests.length > contestPageSize" class="contest-pagination contest-pagination-bottom" aria-label="공모전 목록 하단 페이지">
          <button type="button" :disabled="contestCurrentPage === 1" @click="setContestPage(contestCurrentPage - 1)">이전</button>
          <button
            v-for="page in contestPageNumbers"
            :key="page"
            type="button"
            :class="{ active: contestCurrentPage === page }"
            :aria-current="contestCurrentPage === page ? 'page' : undefined"
            @click="setContestPage(page)"
          >
            {{ page }}
          </button>
          <button type="button" :disabled="contestCurrentPage === contestTotalPages" @click="setContestPage(contestCurrentPage + 1)">다음</button>
        </nav>
      </section>
    </section>

    <section v-if="!isContestListRoute" class="contest-work-panel contest-route-panel">
    <div v-if="isContestDetailRoute || isContestPrepareRoute" class="contest-toolbar">
      <div class="filters">
        <label>
          기준
          <select v-model="basisKey" :disabled="!props.currentUser || basisOptions.length === 0">
            <option value="">조회 필요</option>
            <option v-for="option in basisOptions" :key="option.key" :value="option.key">
              {{ option.label }}
            </option>
          </select>
        </label>
      </div>
      <RouterLink v-if="!props.currentUser" class="ghost-button" :to="{ name: 'login', query: { redirect: route.fullPath } }">로그인</RouterLink>
    </div>

    <section v-if="isContestEditRequestRoute" class="form-panel contest-route-message">
      <div class="form-head">
        <div>
          <span class="eyebrow">Edit Request</span>
          <h2>공모전 수정 요청</h2>
        </div>
        <RouterLink class="ghost-button" :to="{ name: 'contests-detail', params: { contestId: routeContestId } }">상세로</RouterLink>
      </div>
      <p>현재 백엔드에는 기존 공모전을 대상으로 하는 수정 요청 API가 구현되어 있지 않습니다.</p>
      <p class="muted">새 요청으로 저장해 수정 요청인 것처럼 처리하지 않고, 기존 기능 범위만 유지했습니다.</p>
    </section>

    <section v-if="isCompanyWorkspaceRoute && !isCompanyUser" class="form-panel contest-route-message">
      <div class="form-head">
        <div>
          <span class="eyebrow">Company Only</span>
          <h2>기업 계정 전용 기능</h2>
        </div>
        <RouterLink class="ghost-button" :to="{ name: 'contests' }">목록으로</RouterLink>
      </div>
      <p>현재 공모전 개설 요청과 기업 공모전 관리는 승인된 회사 계정만 사용할 수 있습니다.</p>
    </section>

    <section v-if="isCompanyWorkspaceRoute && isCompanyUser" class="form-panel contest-company-route-panel">
      <div class="form-head">
        <div>
          <span class="eyebrow">Company Contest</span>
          <h2 v-if="isContestRequestCreateRoute">공모전 개설 요청</h2>
          <h2 v-else-if="isContestRequestsRoute">내 요청 내역</h2>
          <h2 v-else-if="isCompanyContestEditRoute">기업 공모전 수정</h2>
          <h2 v-else>승인된 기업 공모전</h2>
        </div>
        <button v-if="isContestRequestCreateRoute" class="primary-button" type="button" :disabled="creatingRequest" @click="submitContestRequest">
          {{ creatingRequest ? '접수 중' : '개설 요청' }}
        </button>
        <RouterLink v-else-if="isCompanyContestEditRoute" class="ghost-button" :to="{ name: 'contests-company' }">목록</RouterLink>
        <RouterLink v-else-if="isCompanyContestListRoute" class="primary-button" :to="{ name: 'contests-company-new' }">새 요청</RouterLink>
      </div>
      <section v-if="isContestRequestCreateRoute" class="log-list">
        <div class="row-head">
          <h3>회사 승인 서류</h3>
          <button class="ghost-button" type="button" :disabled="uploadingCompanyDocument" @click="uploadCompanyDocument">
            {{ uploadingCompanyDocument ? '업로드 중' : '서류 업로드' }}
          </button>
        </div>
        <div class="form-grid">
          <label class="field">
            <span>서류 유형</span>
            <select v-model="companyDocumentType">
              <option value="BUSINESS_REGISTRATION">사업자등록증</option>
              <option value="COMPANY_PROFILE">회사소개서</option>
              <option value="PORTFOLIO">포트폴리오</option>
              <option value="OTHER">기타</option>
            </select>
          </label>
          <label class="field">
            <span>파일</span>
            <input accept=".pdf,.png,.jpg,.jpeg,.webp,application/pdf,image/png,image/jpeg,image/webp" type="file" @change="onCompanyDocumentFileChange">
          </label>
        </div>
        <article v-for="document in companyDocuments" :key="document.documentId" class="log-row">
          <div class="row-head">
            <div>
              <strong>{{ document.originalName }}</strong>
              <div class="subline">
                <span>{{ documentTypeLabel(document.documentType) }}</span>
                <span>{{ formatBytes(document.sizeBytes) }}</span>
                <span>{{ String(document.uploadedAt).slice(0, 10) }}</span>
              </div>
            </div>
            <button
              class="ghost-button danger"
              type="button"
              :disabled="companyDocumentActionId === document.documentId"
              @click="requestCompanyDocumentDelete(document)"
            >
              {{ companyDocumentActionId === document.documentId ? '삭제 중' : '삭제' }}
            </button>
          </div>
          <div v-if="pendingCompanyDocumentDeleteId === document.documentId" class="confirm-inline danger-confirm contest-confirm">
            <span>이 회사 서류를 삭제할까요?</span>
            <button class="primary-button" type="button" :disabled="companyDocumentActionId === document.documentId" @click="deleteCompanyDocument(document)">삭제 확인</button>
            <button class="ghost-button" type="button" @click="pendingCompanyDocumentDeleteId = null">취소</button>
          </div>
        </article>
        <p v-if="companyDocuments.length === 0" class="muted">업로드한 회사 서류가 없습니다.</p>
      </section>
      <div v-if="isContestRequestCreateRoute" class="form-grid">
        <label class="field">
          <span>제목</span>
          <input v-model="requestForm.title" maxlength="200" required>
        </label>
        <label class="field">
          <span>주최/주관</span>
          <input v-model="requestForm.organizer" maxlength="120" required>
        </label>
        <label class="field">
          <span>주최 유형</span>
          <select v-model="requestForm.organizerType">
            <option value="">미분류</option>
            <option v-for="option in contestOrganizerOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
        <label class="field wide">
          <span>요약</span>
          <textarea v-model="requestForm.summary" rows="3" maxlength="500" required></textarea>
        </label>
        <label class="field">
          <span>주제</span>
          <input v-model="requestForm.theme" maxlength="150">
        </label>
        <label class="field">
          <span>상금/지원</span>
          <input v-model="requestForm.prizeText" maxlength="150">
        </label>
        <label class="field">
          <span>총상금(원)</span>
          <input v-model.number="requestForm.totalPrizeAmount" type="number" min="0" step="10000">
        </label>
        <label class="field">
          <span>1등 상금(원)</span>
          <input v-model.number="requestForm.firstPrizeAmount" type="number" min="0" step="10000">
        </label>
        <label class="field">
          <span>시작일</span>
          <input v-model="requestForm.startAt" type="datetime-local">
        </label>
        <label class="field">
          <span>마감일</span>
          <input v-model="requestForm.deadlineAt" type="datetime-local" required>
        </label>
        <label class="field">
          <span>제출 이메일</span>
          <input v-model="requestForm.submissionEmail" type="email" maxlength="255" required>
        </label>
        <label class="field wide contest-image-picker">
          <span>대표 이미지</span>
          <input type="file" accept="image/jpeg,image/png,image/webp" @change="onRequestImageChange">
          <small>JPEG, PNG, WebP · 최대 5MB</small>
          <img v-if="requestImagePreview" :src="requestImagePreview" alt="선택한 대표 이미지 미리보기">
        </label>
        <label class="field">
          <span>모집 대상</span>
          <input v-model="requestForm.targetText" maxlength="500">
        </label>
        <fieldset class="field wide contest-structured-field">
          <legend>대상 분류</legend>
          <div class="contest-form-options">
            <label v-for="option in contestTargetOptions" :key="option.value"><input v-model="requestForm.targetCodes" type="checkbox" :value="option.value">{{ option.label }}</label>
          </div>
        </fieldset>
        <fieldset class="field wide contest-structured-field">
          <legend>지역 분류</legend>
          <div class="contest-form-options">
            <label v-for="option in contestRegionOptions" :key="option.value"><input v-model="requestForm.regionCodes" type="checkbox" :value="option.value">{{ option.label }}</label>
          </div>
        </fieldset>
        <label class="field">
          <span>필요 역할</span>
          <input v-model="requestForm.requiredRolesText" maxlength="500">
        </label>
        <label class="field wide">
          <span>관련 장르</span>
          <input v-model="requestForm.relatedGenresText" maxlength="500">
        </label>
      </div>
      <div v-if="isContestRequestsRoute" class="log-list">
        <h3>내 요청</h3>
        <article v-for="request in companyRequests" :key="request.requestId" class="log-row">
          <ProtectedImage
            v-if="request.requestImageUrl"
            class="contest-management-thumb"
            :src="requestImageObjectUrls[request.requestId] || request.requestImageUrl"
            :fallback="defaultContestImage"
            :alt="`${request.title} 대표 이미지`"
            @error="handleContestImageError"
          />
          <strong>#{{ request.requestId }} · {{ request.title }}</strong>
          <div class="subline">
            <span>{{ request.status }}</span>
            <span>마감 {{ request.deadlineAt }}</span>
            <RouterLink
              v-if="request.approvedContestId"
              :to="{ name: 'contests-detail', params: { contestId: request.approvedContestId } }"
            >
              공모전 #{{ request.approvedContestId }}
            </RouterLink>
          </div>
          <p v-if="request.reviewReason" class="muted">{{ request.reviewReason }}</p>
        </article>
        <p v-if="companyRequests.length === 0" class="muted">접수한 개설 요청이 없습니다.</p>
      </div>
      <div v-if="isCompanyContestListRoute" class="log-list">
        <h3>승인된 공모전 관리</h3>
        <article v-for="contest in companyManagedContests" :key="contest.contestId" class="log-row">
          <ProtectedImage v-if="contestImage(contest)" class="contest-management-thumb" :src="contestImage(contest)" :sources="contestImageSources(contest)" :fallback="defaultContestImage" :alt="`${contest.title} 대표 이미지`" @error="handleContestImageError" />
          <span v-else class="contest-management-thumb contest-image-placeholder" aria-hidden="true">SLATE</span>
          <RouterLink :to="{ name: 'contests-detail', params: { contestId: contest.contestId } }">
            <strong>#{{ contest.contestId }} · {{ contest.title }}</strong>
          </RouterLink>
          <p>{{ contest.summary }}</p>
          <div class="subline">
            <span>{{ contest.status }}</span>
            <span>마감 {{ contest.deadlineAt }}</span>
            <span>저장 {{ contest.saveCount }}</span>
            <span>준비 {{ contest.prepareCount }}</span>
          </div>
          <div class="row-actions">
            <button class="ghost-button" type="button" @click="editCompanyContest(contest)">수정</button>
            <button
              v-if="contest.status === 'OPEN'"
              class="ghost-button"
              type="button"
              :disabled="updatingCompanyContest"
              @click="requestCompanyContestEnd(contest)"
            >
              종료
            </button>
          </div>
          <div v-if="pendingCompanyContestEndId === contest.contestId" class="confirm-inline danger-confirm contest-confirm">
            <span>이 공모전을 종료 상태로 전환할까요?</span>
            <button class="primary-button" type="button" :disabled="updatingCompanyContest" @click="endCompanyContest(contest)">종료 확인</button>
            <button class="ghost-button" type="button" @click="pendingCompanyContestEndId = null">취소</button>
          </div>
        </article>
        <p v-if="companyManagedContests.length === 0" class="muted">승인된 회사 공모전이 없습니다.</p>
      </div>
      <form v-if="isCompanyContestEditRoute && editingCompanyContestId" class="form-grid" @submit.prevent="updateCompanyContest">
        <label class="field">
          <span>제목</span>
          <input v-model="companyContestForm.title" maxlength="200" required>
        </label>
        <label class="field">
          <span>주최/주관</span>
          <input v-model="companyContestForm.organizer" maxlength="120" required>
        </label>
        <label class="field">
          <span>주최 유형</span>
          <select v-model="companyContestForm.organizerType">
            <option value="">미분류</option>
            <option v-for="option in contestOrganizerOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
        <label class="field wide">
          <span>요약</span>
          <textarea v-model="companyContestForm.summary" rows="3" maxlength="500" required></textarea>
        </label>
        <label class="field">
          <span>주제</span>
          <input v-model="companyContestForm.theme" maxlength="150">
        </label>
        <label class="field">
          <span>상금/지원</span>
          <input v-model="companyContestForm.prizeText" maxlength="150">
        </label>
        <label class="field">
          <span>총상금(원)</span>
          <input v-model.number="companyContestForm.totalPrizeAmount" type="number" min="0" step="10000">
        </label>
        <label class="field">
          <span>1등 상금(원)</span>
          <input v-model.number="companyContestForm.firstPrizeAmount" type="number" min="0" step="10000">
        </label>
        <label class="field">
          <span>시작일</span>
          <input v-model="companyContestForm.startAt" type="datetime-local">
        </label>
        <label class="field">
          <span>마감일</span>
          <input v-model="companyContestForm.deadlineAt" type="datetime-local" required>
        </label>
        <label class="field">
          <span>제출 이메일</span>
          <input v-model="companyContestForm.submissionEmail" type="email" maxlength="255" required>
        </label>
        <label class="field wide contest-image-picker">
          <span>대표 이미지</span>
          <input type="file" accept="image/jpeg,image/png,image/webp" @change="onCompanyContestImageChange">
          <small>JPEG, PNG, WebP · 최대 5MB</small>
          <ProtectedImage
            v-if="companyContestPreviewSrc()"
            :src="companyContestPreviewSrc()"
            :fallback="defaultContestImage"
            alt="공모전 대표 이미지 미리보기"
            @error="handleContestImageError"
          />
          <span v-else class="contest-image-picker-placeholder" aria-hidden="true">대표 이미지 없음</span>
          <label class="contest-image-delete-option">
            <input v-model="companyContestImageDelete" type="checkbox" :disabled="Boolean(companyContestImageFile)">
            현재 업로드 이미지 삭제
          </label>
        </label>
        <label class="field">
          <span>모집 대상</span>
          <input v-model="companyContestForm.targetText" maxlength="500">
        </label>
        <fieldset class="field wide contest-structured-field">
          <legend>대상 분류</legend>
          <div class="contest-form-options">
            <label v-for="option in contestTargetOptions" :key="option.value"><input v-model="companyContestForm.targetCodes" type="checkbox" :value="option.value">{{ option.label }}</label>
          </div>
        </fieldset>
        <fieldset class="field wide contest-structured-field">
          <legend>지역 분류</legend>
          <div class="contest-form-options">
            <label v-for="option in contestRegionOptions" :key="option.value"><input v-model="companyContestForm.regionCodes" type="checkbox" :value="option.value">{{ option.label }}</label>
          </div>
        </fieldset>
        <label class="field">
          <span>필요 역할</span>
          <input v-model="companyContestForm.requiredRolesText" maxlength="500">
        </label>
        <label class="field wide">
          <span>관련 장르</span>
          <input v-model="companyContestForm.relatedGenresText" maxlength="500">
        </label>
        <div class="row-actions">
          <button class="ghost-button" type="button" @click="cancelCompanyContestEdit">취소</button>
          <button class="primary-button" type="submit" :disabled="updatingCompanyContest">
            {{ updatingCompanyContest ? '저장 중' : '승인 공모전 수정' }}
          </button>
        </div>
      </form>
    </section>

    <div v-if="isContestDetailRoute || isContestPrepareRoute" class="contest-grid contest-detail-route">
      <aside v-if="(isContestDetailRoute || isContestPrepareRoute) && selected" class="contest-detail contest-route-detail">
        <div class="contest-route-detail-image contest-image-frame" :class="{ placeholder: !contestImage(selected) }">
          <ProtectedImage v-if="contestImage(selected)" :src="contestImage(selected)" :sources="contestImageSources(selected)" :fallback="defaultContestImage" :alt="selected.title" @error="handleContestImageError" />
          <span v-else aria-hidden="true">SLATE CONTEST</span>
          <span v-if="isContestKoreaPoster(selected)" class="contest-detail-poster-badge">포스터 출처: 콘테스트코리아</span>
        </div>
        <div class="detail-head">
          <div>
            <span class="eyebrow">{{ selected.contestType }} · {{ selected.organizer }}</span>
            <h2>{{ selected.title }}</h2>
            <p class="contest-detail-summary">{{ selected.summary || selected.theme || '공모전 상세 내용을 확인하고 제출 준비를 진행하세요.' }}</p>
            <div v-if="contestSourceLabel(selected)" class="contest-detail-source-head">
              <span>{{ contestSourceLabel(selected) }}</span>
            </div>
          </div>
          <div class="top-actions">
            <RouterLink class="ghost-button" :to="{ name: 'contests' }">목록</RouterLink>
            <a
              v-if="contestPrimaryExternalUrl(selected)"
              class="ghost-button contest-detail-original-link"
              :href="contestPrimaryExternalUrl(selected)"
              target="_blank"
              rel="noopener noreferrer"
              :aria-label="contestPrimaryExternalAriaLabel(selected)"
            >
              {{ contestPrimaryExternalLabel(selected) }}
            </a>
            <button
              class="contest-save-heart detail"
              :class="{ saved: selected.savedByCurrentUser }"
              type="button"
              :disabled="saving"
              :aria-label="selected.savedByCurrentUser ? `${selected.title} 저장 취소` : `${selected.title} 저장`"
              :aria-pressed="Boolean(selected.savedByCurrentUser)"
              @click="toggleSave(selected)"
            >
              <span aria-hidden="true">{{ selected.savedByCurrentUser ? '♥' : '♡' }}</span>
            </button>
            <button class="primary-button" type="button" :disabled="fitAnalyzing || !basis.basisType" @click="calculateFit()">
              {{ fitAnalyzing ? '분석 중' : '적합도 분석' }}
            </button>
          </div>
        </div>

        <section class="contest-detail-status-strip" aria-label="공모전 요약 상태">
          <article>
            <span>마감</span>
            <strong>{{ dDayLabel(selected.dDay) }}</strong>
          </article>
          <article>
            <span>접수 기간</span>
            <strong>{{ formatContestDate(selected.startAt) || '시작일 미정' }} - {{ formatContestDate(selected.deadlineAt) || '마감일 미정' }}</strong>
          </article>
          <article>
            <span>저장</span>
            <strong>{{ selected.saveCount || 0 }}명</strong>
          </article>
        </section>

        <div v-if="fitResult" class="contest-score-row">
          <div class="fit-ring large" :style="{ '--score': `${fitResult.fitScore || 0}%` }">
            <strong>{{ fitResult.fitScore }}</strong>
            <span>%</span>
          </div>
          <div>
            <strong>{{ fitResult.badge || '분석 결과' }}</strong>
            <p>{{ selected.theme || selected.summary }}</p>
          </div>
        </div>

        <ul v-if="fitResult" class="reason-list">
          <li v-for="reason in fitResult.reasons || []" :key="reason"><span></span>{{ reason }}</li>
        </ul>

        <section class="contest-detail-fact-grid" aria-label="공모전 핵심 정보">
          <article>
            <span>주최/주관</span>
            <strong>{{ selected.organizer }}</strong>
          </article>
          <article>
            <span>상금/지원</span>
            <strong>{{ selected.prizeText || '상세 공고 참고' }}</strong>
          </article>
          <article>
            <span>제출 방식</span>
            <strong>{{ contestSubmissionMethod(selected) }}</strong>
          </article>
          <article>
            <span>마감</span>
            <strong>{{ formatContestDate(selected.deadlineAt) || '마감일 미정' }}</strong>
          </article>
          <article>
            <span>모집 대상</span>
            <strong>{{ selected.targetText || '전체 창작자' }}</strong>
          </article>
          <article>
            <span>필요 역할/장르</span>
            <strong>{{ [selected.requiredRolesText, selected.relatedGenresText].filter(Boolean).join(' · ') || '상세 공고 참고' }}</strong>
          </article>
        </section>

        <section v-if="hasContestDetailSourcePanel(selected)" class="contest-detail-source-panel" aria-label="공모전 출처 정보">
          <div class="contest-detail-source-grid">
            <div v-if="contestSourceLabel(selected)">
              <span>출처</span>
              <strong>{{ contestSourceLabel(selected) }}</strong>
            </div>
            <div v-if="contestCollectedDate(selected)">
              <span>수집일</span>
              <strong>{{ contestCollectedDate(selected) }}</strong>
            </div>
            <div v-if="isContestKoreaPoster(selected)">
              <span>포스터</span>
              <strong>콘테스트코리아 제공</strong>
            </div>
            <div v-if="contestPosterCollectedDate(selected)">
              <span>포스터 수집일</span>
              <strong>{{ contestPosterCollectedDate(selected) }}</strong>
            </div>
          </div>
          <div v-if="hasContestSourceUrl(selected)" class="contest-detail-source-actions">
            <a
              class="contest-detail-source-link"
              :href="contestSourceUrl(selected)"
              target="_blank"
              rel="noopener noreferrer"
              :aria-label="contestSourceAriaLabel(selected)"
            >
              {{ contestSourceLinkLabel(selected) }}
            </a>
          </div>
        </section>

        <div v-if="isContestDetailRoute" class="contest-route-actions">
          <RouterLink v-if="props.currentUser" class="primary-button" :to="{ name: 'contests-prepare', params: { contestId: selected.contestId } }">제출 준비</RouterLink>
          <RouterLink v-if="props.currentUser && !isCompanyUser" class="ghost-button" :to="{ name: 'contests-edit-request', params: { contestId: selected.contestId } }">수정 요청</RouterLink>
          <RouterLink
            v-if="isCompanyUser && companyManagedContests.some((contest) => Number(contest.contestId) === Number(selected.contestId))"
            class="ghost-button"
            :to="{ name: 'contests-company-edit', params: { contestId: selected.contestId } }"
          >
            기업 공모전 수정
          </RouterLink>
        </div>

        <form v-if="isContestPrepareRoute" class="form-panel" @submit.prevent="savePreparation">
          <div class="form-head">
            <div>
              <span class="eyebrow">Prepare</span>
              <h2>제출 준비</h2>
            </div>
            <button class="primary-button" type="submit" :disabled="saving || !basis.basisType">저장</button>
          </div>
          <RouterLink class="ghost-button inline" :to="{ name: 'contests-detail', params: { contestId: selected.contestId } }">공모전 상세로</RouterLink>
          <label class="field wide">
            <span>체크리스트</span>
            <textarea v-model="prepareForm.checklistText" rows="5"></textarea>
          </label>
          <label class="field wide">
            <span>준비 메모</span>
            <textarea v-model="prepareForm.memo" rows="4" maxlength="1000"></textarea>
          </label>
        </form>
      </aside>
      <section v-else-if="(isContestDetailRoute || isContestPrepareRoute) && !loading" class="form-panel contest-route-message">
        <h2>공모전을 찾을 수 없습니다.</h2>
        <RouterLink class="ghost-button inline" :to="{ name: 'contests' }">목록으로</RouterLink>
      </section>
    </div>
  </section>
  </section>
</template>
