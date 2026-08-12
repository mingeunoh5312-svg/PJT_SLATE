<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { defaultPortfolioImage, defaultProfileImage, defaultTeamImage } from '../constants/defaultImages'
import { getToken, slateApi } from '../services/api'

const props = defineProps({
  currentUser: Object
})
const route = useRoute()
const router = useRouter()

const mode = ref('teamToMembers')
const loading = ref(false)
const error = ref('')
const notice = ref('')
const teams = ref([])
const recruitments = ref([])
const selectedTeamId = ref(null)
const selectedSlotId = ref(null)
const genres = ref([])
const regions = ref([])
const roleGroups = ref([])
const experienceLevels = ref([])
const joinAvailabilities = ref([])
const collaborationConditions = ref([])
const durations = ref([])
const selectedGenreIds = ref([])
const selectedRoleIds = ref([])
const selectedTopRegions = ref([])
const selectedRegionIds = ref([])
const selectedExperienceLevels = ref([])
const selectedJoinAvailabilities = ref([])
const selectedCollaborationConditions = ref([])
const genreKeyword = ref('')
const genreLoading = ref(false)
const regionLoading = ref(false)
const codeLoading = ref(false)
const genreError = ref('')
const regionError = ref('')
const codeError = ref('')
const collaborationConditionError = ref('')
const topRegionKeyword = ref('')
const topRegionDropdownOpen = ref(false)
const regionKeyword = ref('')
const regionDropdownOpen = ref(false)
const teamsLoading = ref(false)
const teamsError = ref('')
const recruitmentsLoading = ref(false)
const recruitmentsError = ref('')
const profile = ref(null)
const teamToMembers = ref({ primary: [], supplementary: [] })
const memberToTeams = ref({ primary: [], supplementary: [] })
const selected = ref(null)
const policy = ref(null)
const aiRecommendationLoading = ref(false)
const aiRecommendationError = ref('')
const aiRecommendations = ref([])
const activeAiRecommendationIndex = ref(0)
const aiRecommendationRequested = ref(false)
const selectedFollowStatus = ref(null)
const followStatusLoading = ref(false)
const followToggleLoading = ref(false)
const followError = ref('')
const candidateFollowLoadingIds = ref({})
const candidateInviteLoadingIds = ref({})
const teamApplicationLoadingIds = ref({})
const followedMembers = ref([])
const followedMembersLoading = ref(false)
const followedMembersError = ref('')
const sentInvitations = ref([])
const sentInvitationsLoading = ref(false)
const sentInvitationsError = ref('')
const sentInvitationActionId = ref(null)
const sentInvitationStatusFilters = ref(['PENDING', 'ACCEPTED', 'REJECTED', 'CANCELED', 'EXPIRED'])
const savedTeams = ref([])
const savedTeamsLoading = ref(false)
const savedTeamsError = ref('')
const sentApplications = ref([])
const sentApplicationsLoading = ref(false)
const sentApplicationsError = ref('')
const sentApplicationActionId = ref(null)
const sentApplicationStatusFilters = ref(['PENDING', 'ACCEPTED', 'REJECTED', 'CANCELED', 'EXPIRED'])
const savedTeamRoleSelections = ref({})
const savedTeamActionId = ref(null)
const expandedSavedTeamDescriptionIds = ref({})
const appliedTeamFilterSignature = ref('')
const teamFiltersNeedApply = ref(false)
const MATCHING_PAGE_SIZE = 5
const resultPage = ref(1)
let refreshRequestId = 0
let recruitmentRequestId = 0
let basisChangeRequestId = 0
let loadRequestId = 0
let followStatusRequestId = 0
let followedMembersRequestId = 0
let sentInvitationsRequestId = 0
let aiRecommendationRequestId = 0
let savedTeamsRequestId = 0
let sentApplicationsRequestId = 0
let componentUnmounted = false
let basisSelectionCleared = false
let pendingAiRecommendationAfterRoute = false
const internalQueryReplacements = new Set()
let loadedRouteDataSignature = ''
const SAVED_TEAM_DESCRIPTION_LIMIT = 100
const TEAM_DESCRIPTION_PREVIEW_LIMIT = 80
const REQUEST_STATUS_OPTIONS = [
  { value: 'PENDING', label: '대기 중' },
  { value: 'ACCEPTED', label: '수락됨' },
  { value: 'REJECTED', label: '거절됨' },
  { value: 'CANCELED', label: '취소됨' },
  { value: 'EXPIRED', label: '만료됨' }
]

const isMemberListRoute = computed(() => route.name === 'matching-members')
const isMemberDetailRoute = computed(() => route.name === 'matching-members-detail')
const isTeamListRoute = computed(() => route.name === 'matching-teams')
const isTeamDetailRoute = computed(() => route.name === 'matching-teams-detail')
const isCandidateListRoute = computed(() => isMemberListRoute.value || isTeamListRoute.value)
const isCandidateDetailRoute = computed(() => isMemberDetailRoute.value || isTeamDetailRoute.value)
const isAiContext = computed(() => route.query.view === 'ai')
const isAiView = computed(() => isCandidateListRoute.value && isAiContext.value)
const isFollowedMembersContext = computed(() => (isMemberListRoute.value || isMemberDetailRoute.value) && route.query.view === 'following')
const isInvitedMembersContext = computed(() => (isMemberListRoute.value || isMemberDetailRoute.value) && route.query.view === 'invited')
const isAppliedTeamsContext = computed(() => (isTeamListRoute.value || isTeamDetailRoute.value) && route.query.view === 'applied')
const isFollowedMembersView = computed(() => isMemberListRoute.value && route.query.view === 'following')
const isInvitedMembersView = computed(() => isMemberListRoute.value && route.query.view === 'invited')
const isSavedTeamsView = computed(() => isTeamListRoute.value && route.query.view === 'saved')
const isAppliedTeamsView = computed(() => isTeamListRoute.value && route.query.view === 'applied')
const isActionListView = computed(() => isSavedTeamsView.value || isAppliedTeamsView.value || isInvitedMembersView.value)
const hasAppliedTeamFilters = computed(() => (
  mode.value === 'memberToTeams'
  && route.query.applied === '1'
))
const candidateRouteId = computed(() => (
  isMemberDetailRoute.value ? route.params.userId : isTeamDetailRoute.value ? route.params.teamId : null
))

const activeResults = computed(() => mode.value === 'teamToMembers' ? teamToMembers.value : memberToTeams.value)
const allCards = computed(() => [...(activeResults.value.primary || []), ...(activeResults.value.supplementary || [])]
  .sort((left, right) => (Number(right.score) || 0) - (Number(left.score) || 0)))
const followedMemberCards = computed(() => followedMembers.value.filter(matchesFollowedMemberFilters))
const displayCards = computed(() => {
  if (isInvitedMembersContext.value) return sentInvitations.value
  if (isAppliedTeamsContext.value) return sentApplications.value
  return isFollowedMembersContext.value ? followedMemberCards.value : allCards.value
})
const filteredGenres = computed(() => {
  const keyword = genreKeyword.value.trim().toLowerCase()
  return keyword ? genres.value.filter((genre) => String(genre.name || '').toLowerCase().includes(keyword)) : genres.value
})
const topRegionOptions = computed(() => [...new Set(regions.value.map((region) => region.sidoName).filter(Boolean))]
  .sort((left, right) => left.localeCompare(right, 'ko')))
const filteredTopRegionOptions = computed(() => {
  const keyword = topRegionKeyword.value.trim().toLowerCase()
  if (!keyword) return topRegionOptions.value
  return topRegionOptions.value.filter((option) => option.toLowerCase().includes(keyword))
})
const selectedTopRegionSet = computed(() => new Set(selectedTopRegions.value))
const selectedRegionIdSet = computed(() => new Set(selectedRegionIds.value.map((value) => Number(value))))
const scopedRegions = computed(() => selectedTopRegions.value.length
  ? regions.value.filter((region) => selectedTopRegionSet.value.has(region.sidoName))
  : regions.value)
const filteredRegions = computed(() => {
  const keyword = regionKeyword.value.trim().toLowerCase()
  return keyword
    ? scopedRegions.value.filter((region) => String(region.publicDisplayName || region.name || '').toLowerCase().includes(keyword))
    : scopedRegions.value
})
const visibleRegionOptions = computed(() => filteredRegions.value)
const selectedRegions = computed(() => selectedRegionIds.value
  .map((regionId) => regions.value.find((region) => Number(region.regionId) === Number(regionId)))
  .filter(Boolean))
const topRegionSummary = computed(() => selectedTopRegions.value.length ? `${selectedTopRegions.value.length}개 선택` : '전체 지역')
const detailRegionSummary = computed(() => selectedRegions.value.length
  ? `${selectedRegions.value.length}개 선택`
  : `${selectedTopRegions.value.length ? `${selectedTopRegions.value.length}개 지역` : '전체 지역'} 기준 ${scopedRegions.value.length}건`)
const openSlotRoleOptions = computed(() => {
  const seen = new Set()
  return openSlots.value
    .map((slot) => ({ roleId: Number(slot.roleId), label: slot.roleName }))
    .filter((role) => Number.isFinite(role.roleId) && !seen.has(role.roleId) && seen.add(role.roleId))
})
const allRoleOptions = computed(() => {
  const seen = new Set()
  return roleGroups.value
    .flatMap((category) => category.roles || [])
    .map((role) => ({ roleId: Number(role.roleId), label: role.name || role.roleName }))
    .filter((role) => Number.isFinite(role.roleId) && role.label && !seen.has(role.roleId) && seen.add(role.roleId))
})
const teamMemberRoleOptions = computed(() => openSlotRoleOptions.value.length ? openSlotRoleOptions.value : allRoleOptions.value)
const profileRoleOptions = computed(() => {
  const seen = new Set()
  return (profile.value?.roles || [])
    .map((role) => ({ roleId: Number(role.roleId), label: role.roleName || role.name }))
    .filter((role) => Number.isFinite(role.roleId) && role.label && !seen.has(role.roleId) && seen.add(role.roleId))
})
const matchingRoleOptions = computed(() => mode.value === 'teamToMembers' ? teamMemberRoleOptions.value : profileRoleOptions.value)
const teamFiltersDirty = computed(() => (
  mode.value === 'memberToTeams'
  && hasAppliedTeamFilters.value
  && teamFiltersNeedApply.value
))
const listDisplayCards = computed(() => displayCards.value)
const resultCount = computed(() => listDisplayCards.value.length)
const totalResultPages = computed(() => Math.max(1, Math.ceil(resultCount.value / MATCHING_PAGE_SIZE)))
const normalizedResultPage = computed(() => Math.min(Math.max(1, resultPage.value), totalResultPages.value))
const paginatedListDisplayCards = computed(() => {
  const start = (normalizedResultPage.value - 1) * MATCHING_PAGE_SIZE
  return listDisplayCards.value.slice(start, start + MATCHING_PAGE_SIZE)
})
const resultPageStart = computed(() => resultCount.value ? (normalizedResultPage.value - 1) * MATCHING_PAGE_SIZE + 1 : 0)
const resultPageEnd = computed(() => Math.min(resultCount.value, normalizedResultPage.value * MATCHING_PAGE_SIZE))
const hasResultPagination = computed(() => isCandidateListRoute.value && !isActionListView.value && resultCount.value > MATCHING_PAGE_SIZE)
const visibleResultPages = computed(() => {
  const total = totalResultPages.value
  const current = normalizedResultPage.value
  const start = Math.max(1, Math.min(current - 2, total - 4))
  const end = Math.min(total, start + 4)
  return Array.from({ length: end - start + 1 }, (_, index) => start + index)
})
const manageableTeams = computed(() => [...teams.value]
  .filter((team) => (
    ['LEADER', 'SUB_LEADER'].includes(team.myTeamRole)
    && ['ACTIVE', 'RECRUITING', 'IN_PROGRESS', 'RECRUITMENT_CLOSED'].includes(team.status)
  ))
  .sort((left, right) => timestamp(right.createdAt) - timestamp(left.createdAt)))
const selectedTeam = computed(() => manageableTeams.value.find((team) => Number(team.teamId) === Number(selectedTeamId.value)) || null)
const openSlots = computed(() => recruitments.value.flatMap((recruitment) => {
  if (String(recruitment.status || '').toUpperCase() !== 'OPEN') return []
  return (recruitment.slots || [])
    .filter((slot) => String(slot.status || '').toUpperCase() === 'OPEN' && Number(slot.remainingCount) > 0)
    .map((slot) => ({
      ...slot,
      recruitmentTitle: recruitment.title,
      deadlineAt: slot.deadlineAt || recruitment.deadlineAt,
      workStartAt: slot.workStartAt || recruitment.workStartAt
    }))
}))
const selectedSlot = computed(() => openSlots.value.find((slot) => Number(slot.slotId) === Number(selectedSlotId.value)) || null)
const selectedCard = computed(() => selected.value || (isCandidateDetailRoute.value ? null : displayCards.value[0]) || null)
const activeAiRecommendation = computed(() => aiRecommendations.value[activeAiRecommendationIndex.value] || null)
const aiRecommendationLabel = computed(() => mode.value === 'teamToMembers' ? '추천 팀원' : '추천 팀')
const aiRecommendationButtonText = computed(() => {
  if (aiRecommendationLoading.value) return '추천 중'
  return 'AI 추천'
})
const aiRecommendationDisabledReason = computed(() => {
  if (mode.value === 'memberToTeams' && !profile.value?.profileId) return 'AI 추천을 받으려면 프로필이 필요합니다.'
  return ''
})
const aiRecommendationContextText = computed(() => {
  if (mode.value === 'teamToMembers') {
    if (selectedTeamId.value && selectedSlotId.value) return `팀 #${selectedTeamId.value} · 모집 역할 #${selectedSlotId.value}`
    if (selectedTeamId.value) return `팀 #${selectedTeamId.value} · 전체 모집 역할`
    return '전체 공개 프로필'
  }
  return `${currentProfileName.value} · ${currentProfileRole.value}`
})
const aiRecommendationDetailButtonText = computed(() => mode.value === 'teamToMembers' ? '프로필 보기' : '팀 정보 보기')
const aiRecommendationStateText = computed(() => {
  if (mode.value === 'teamToMembers') {
    if (!manageableTeams.value.length) return '소속된 팀이 없습니다.'
    if (aiRecommendationRequested.value) return '추천 팀원을 찾을 수 없습니다.'
    return ''
  }
  return aiRecommendationRequested.value ? '추천 팀을 찾을 수 없습니다.' : ''
})
const canShowFollowControl = computed(() => {
  const profileId = Number(selectedCard.value?.profileId)
  return mode.value === 'teamToMembers'
    && isMemberDetailRoute.value
    && Number.isFinite(profileId)
    && profileId > 0
})
const currentProfileName = computed(() => props.currentUser?.nickname || profile.value?.displayName || '윤서')
const currentProfileRole = computed(() => {
  const roleNames = (profile.value?.roles || []).map((role) => role.roleName).filter(Boolean)
  return roleNames.join(', ') || '역할 정보 없음'
})
const selectedRegionChips = computed(() => {
  const chips = [
    ...selectedTopRegions.value.map((sido) => ({
      group: 'regionSido',
      key: `top:${sido}`,
      type: 'top',
      sido,
      value: sido,
      label: sido
    })),
    ...selectedRegions.value.map((region) => ({
      group: 'region',
      key: `detail:${region.regionId}`,
      type: 'detail',
      sido: region.sidoName || '',
      value: Number(region.regionId),
      label: region.publicDisplayName || region.name || `지역 #${region.regionId}`
    }))
  ]
  return chips.sort((left, right) => {
    const leftIndex = topRegionSortIndex(left.sido)
    const rightIndex = topRegionSortIndex(right.sido)
    if (leftIndex !== rightIndex) return leftIndex - rightIndex
    if (left.type !== right.type) return left.type === 'top' ? -1 : 1
    return left.label.localeCompare(right.label, 'ko')
  })
})
const selectedFilterChips = computed(() => {
  const chips = []
  selectedExperienceLevels.value.forEach((code) => {
    chips.push({ group: 'experience', value: code, label: filterCodeLabel(experienceLevels.value, code) })
  })
  selectedGenreIds.value.forEach((genreId) => {
    const genre = genres.value.find((item) => Number(item.genreId) === Number(genreId))
    chips.push({ group: 'genre', value: Number(genreId), label: genre?.name || `장르 #${genreId}` })
  })
  selectedJoinAvailabilities.value.forEach((code) => {
    chips.push({ group: 'schedule', value: code, label: filterCodeLabel(joinAvailabilities.value, code) })
  })
  selectedCollaborationConditions.value.forEach((code) => {
    chips.push({ group: 'condition', value: code, label: filterCodeLabel(collaborationConditions.value, code) })
  })
  selectedRoleIds.value.forEach((roleId) => {
    const role = matchingRoleOptions.value.find((item) => Number(item.roleId) === Number(roleId))
    chips.push({ group: 'role', value: Number(roleId), label: role?.label || `역할 #${roleId}` })
  })
  return chips
})
const hasSelectedFilterChips = computed(() => selectedRegionChips.value.length > 0 || selectedFilterChips.value.length > 0)
const sentInvitationStatusOptions = computed(() => requestStatusFilterOptions(sentInvitations.value))
const sentApplicationStatusOptions = computed(() => requestStatusFilterOptions(sentApplications.value))
const filteredSentInvitations = computed(() => filterRowsByStatus(sentInvitations.value, sentInvitationStatusFilters.value))
const filteredSentApplications = computed(() => filterRowsByStatus(sentApplications.value, sentApplicationStatusFilters.value))

function emptyResult() {
  return { primary: [], supplementary: [], totalCount: 0 }
}

function resetResultPage() {
  resultPage.value = 1
}

function clampResultPage() {
  if (resultPage.value > totalResultPages.value) resultPage.value = totalResultPages.value
  if (resultPage.value < 1) resultPage.value = 1
}

function goResultPage(page) {
  const nextPage = Number(page)
  if (!Number.isFinite(nextPage)) return
  resultPage.value = Math.min(Math.max(1, Math.trunc(nextPage)), totalResultPages.value)
}

function showPreviousResultPage() {
  goResultPage(normalizedResultPage.value - 1)
}

function showNextResultPage() {
  goResultPage(normalizedResultPage.value + 1)
}

function showFirstResultPage() {
  goResultPage(1)
}

function showLastResultPage() {
  goResultPage(totalResultPages.value)
}

function routeMode() {
  if (isTeamListRoute.value || isTeamDetailRoute.value) return 'memberToTeams'
  return 'teamToMembers'
}

function candidateId(item) {
  if (mode.value === 'teamToMembers') return item?.userId || item?.profileId
  return item?.teamId
}

function positiveId(value) {
  const parsed = Number(Array.isArray(value) ? value[0] : value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

function candidateKey(item, index) {
  if (mode.value === 'teamToMembers') return `member-${candidateId(item) || index}`
  return `team-${item?.teamId || index}-slot-${item?.slotId || index}`
}

function selectRouteCandidate() {
  if (!isCandidateDetailRoute.value) {
    selected.value = null
    return
  }
  const routeTeamId = positiveId(route.query.teamId)
  const routeSlotId = positiveId(route.query.slotId)
  selected.value = displayCards.value.find((item) => (
    String(candidateId(item)) === String(candidateRouteId.value)
    && (!routeTeamId || Number(item.teamId) === routeTeamId)
    && (!routeSlotId || Number(item.slotId) === routeSlotId)
  )) || null
}

async function syncMemberQuery() {
  if (!isMemberListRoute.value) return
  await replaceCurrentQuery(matchingListQuery())
}

function routeValues(value) {
  if (Array.isArray(value)) return value.flatMap((item) => String(item).split(','))
  return value ? String(value).split(',') : []
}

function restoreFilterQuery() {
  selectedGenreIds.value = routeValues(route.query.genreIds)
    .map(Number)
    .filter((value) => Number.isFinite(value) && value > 0)
  selectedRoleIds.value = routeValues(route.query.roleIds)
    .map(Number)
    .filter((value) => Number.isFinite(value) && value > 0)
  selectedTopRegions.value = routeValues(route.query.regionSidos)
    .map((value) => String(value || '').trim())
    .filter(Boolean)
  selectedRegionIds.value = routeValues(route.query.regionIds)
    .map(Number)
    .filter((value) => Number.isFinite(value) && value > 0)
  selectedExperienceLevels.value = routeValues(route.query.experienceLevel)
  selectedJoinAvailabilities.value = routeValues(route.query.joinAvailability)
  selectedCollaborationConditions.value = routeValues(route.query.collaborationCondition)
}

function sanitizeFilterSelections() {
  const genreIds = new Set(genres.value.map((item) => Number(item.genreId)))
  const regionIds = new Set(regions.value.map((item) => Number(item.regionId)))
  const sidos = new Set(topRegionOptions.value)
  const validCodes = (rows, values) => values.filter((value) => rows.some((item) => item.code === value))
  selectedGenreIds.value = selectedGenreIds.value.filter((genreId) => genreIds.has(Number(genreId)))
  selectedTopRegions.value = selectedTopRegions.value.filter((sido) => sidos.has(sido))
  selectedRegionIds.value = selectedRegionIds.value.filter((regionId) => regionIds.has(Number(regionId)))
  selectedExperienceLevels.value = validCodes(experienceLevels.value, selectedExperienceLevels.value)
  selectedJoinAvailabilities.value = validCodes(joinAvailabilities.value, selectedJoinAvailabilities.value)
  selectedCollaborationConditions.value = validCodes(collaborationConditions.value, selectedCollaborationConditions.value)
}

function filterQuery() {
  const query = {}
  if (selectedGenreIds.value.length) query.genreIds = selectedGenreIds.value.map(String)
  if (selectedRoleIds.value.length) query.roleIds = selectedRoleIds.value.map(String)
  if (selectedTopRegions.value.length) query.regionSidos = selectedTopRegions.value
  if (selectedRegionIds.value.length) query.regionIds = selectedRegionIds.value.map(String)
  if (selectedExperienceLevels.value.length) query.experienceLevel = selectedExperienceLevels.value
  if (mode.value === 'teamToMembers' && selectedJoinAvailabilities.value.length) query.joinAvailability = selectedJoinAvailabilities.value
  if (selectedCollaborationConditions.value.length) query.collaborationCondition = selectedCollaborationConditions.value
  return query
}

function selectionTarget(targetRef) {
  if (Array.isArray(targetRef)) return targetRef
  return Array.isArray(targetRef?.value) ? targetRef.value : []
}

function toggleFilterSelection(targetRef, value) {
  const target = selectionTarget(targetRef)
  const normalized = typeof value === 'number' ? Number(value) : String(value)
  const index = target.findIndex((item) => String(item) === String(normalized))
  if (index >= 0) target.splice(index, 1)
  else target.push(normalized)
}

function topRegionSortIndex(sidoName) {
  const index = topRegionOptions.value.indexOf(sidoName)
  return index === -1 ? Number.MAX_SAFE_INTEGER : index
}

function releaseRegionInputFocus() {
  nextTick(() => {
    const active = document.activeElement
    if (active && typeof active.blur === 'function') active.blur()
  })
}

function selectTopRegion(sidoName) {
  if (!sidoName) {
    clearAllRegions()
    releaseRegionInputFocus()
    return
  }
  selectedTopRegions.value = selectedTopRegionSet.value.has(sidoName)
    ? selectedTopRegions.value.filter((value) => value !== sidoName)
    : [...selectedTopRegions.value, sidoName].sort((left, right) => topRegionSortIndex(left) - topRegionSortIndex(right))
  topRegionKeyword.value = ''
  topRegionDropdownOpen.value = false
  regionKeyword.value = ''
  regionDropdownOpen.value = false
  releaseRegionInputFocus()
}

function selectRegion(region) {
  const regionId = Number(region?.regionId)
  if (!Number.isFinite(regionId)) return
  selectedRegionIds.value = selectedRegionIdSet.value.has(regionId)
    ? selectedRegionIds.value.filter((value) => Number(value) !== regionId)
    : [...selectedRegionIds.value, regionId]
  regionKeyword.value = ''
  regionDropdownOpen.value = false
  releaseRegionInputFocus()
}

function clearTopRegion(sidoName) {
  selectedTopRegions.value = selectedTopRegions.value.filter((value) => value !== sidoName)
  topRegionKeyword.value = ''
  topRegionDropdownOpen.value = false
}

function clearRegion(regionId) {
  selectedRegionIds.value = selectedRegionIds.value.filter((value) => Number(value) !== Number(regionId))
  regionKeyword.value = ''
  regionDropdownOpen.value = false
}

function clearAllRegions() {
  selectedTopRegions.value = []
  selectedRegionIds.value = []
  topRegionKeyword.value = ''
  regionKeyword.value = ''
  topRegionDropdownOpen.value = false
  regionDropdownOpen.value = false
}

function displayCodeName(item) {
  if (!item) return ''
  if (item.code === 'AFTER_1M' && item.displayName === '1개월 이후') return '6개월 이내'
  return item.publicDisplayName || item.name || item.displayName || item.code
}

function filterCodeLabel(rows, code) {
  return displayCodeName(rows.find((item) => item.code === code)) || code
}

function removeFilterChip(chip) {
  const removeFrom = (targetRef) => {
    targetRef.value = targetRef.value.filter((item) => String(item) !== String(chip.value))
  }
  if (chip.group === 'regionSido') clearTopRegion(chip.value)
  if (chip.group === 'region') clearRegion(chip.value)
  if (chip.group === 'experience') removeFrom(selectedExperienceLevels)
  if (chip.group === 'genre') removeFrom(selectedGenreIds)
  if (chip.group === 'role') removeFrom(selectedRoleIds)
  if (chip.group === 'schedule') removeFrom(selectedJoinAvailabilities)
  if (chip.group === 'condition') removeFrom(selectedCollaborationConditions)
}

function timestamp(value) {
  if (!value) return 0
  const time = new Date(value).getTime()
  return Number.isNaN(time) ? 0 : time
}
function filterSignature(query = filterQuery()) {
  return JSON.stringify(normalizedQueryEntries(query))
}

function resetMatchState() {
  savedTeamsRequestId += 1
  followedMembersRequestId += 1
  sentInvitationsRequestId += 1
  sentApplicationsRequestId += 1
  teams.value = []
  recruitments.value = []
  recruitmentsLoading.value = false
  recruitmentsError.value = ''
  selectedTeamId.value = null
  selectedSlotId.value = null
  profile.value = null
  teamToMembers.value = emptyResult()
  memberToTeams.value = emptyResult()
  selected.value = null
  policy.value = null
  resetAiRecommendations()
  resetFollowState()
  followedMembers.value = []
  followedMembersLoading.value = false
  followedMembersError.value = ''
  sentInvitations.value = []
  sentInvitationsLoading.value = false
  sentInvitationsError.value = ''
  sentInvitationActionId.value = null
  savedTeams.value = []
  savedTeamsLoading.value = false
  savedTeamsError.value = ''
  sentApplications.value = []
  sentApplicationsLoading.value = false
  sentApplicationsError.value = ''
  sentApplicationActionId.value = null
  savedTeamRoleSelections.value = {}
  savedTeamActionId.value = null
  expandedSavedTeamDescriptionIds.value = {}
  appliedTeamFilterSignature.value = ''
  teamFiltersNeedApply.value = false
  resetResultPage()
}

function normalizeFollowedMember(row) {
  return {
    ...row,
    displayName: row.displayName || row.nickname || '팔로우 회원',
    profileImageUrl: row.profileImageUrl || row.thumbnailUrl || '',
    score: null,
    scoreBadge: '팔로우한 회원',
    reasons: ['팔로우한 회원'],
    followingByCurrentUser: true
  }
}

function idsFor(rows, key) {
  return (rows || []).map((item) => Number(item?.[key])).filter((value) => Number.isFinite(value))
}

function matchesCodeAny(selectedCodes, actualCode) {
  return !selectedCodes.length || selectedCodes.some((code) => String(code) === String(actualCode))
}

function matchesSelectedRegion(item) {
  if (!selectedTopRegions.value.length && !selectedRegionIds.value.length) return true
  if (selectedRegionIdSet.value.has(Number(item?.regionId))) return true
  return selectedTopRegionSet.value.has(String(item?.sidoName || '').trim())
}

function matchesFollowedMemberFilters(item) {
  const profileRoleIds = idsFor(item.roles, 'roleId')
  if (selectedRoleIds.value.length) {
    if (!selectedRoleIds.value.some((roleId) => profileRoleIds.includes(Number(roleId)))) return false
  } else if (selectedSlot.value?.roleId && !profileRoleIds.includes(Number(selectedSlot.value.roleId))) {
    return false
  }
  if (selectedGenreIds.value.length) {
    const profileGenreIds = idsFor(item.genres, 'genreId')
    if (!selectedGenreIds.value.some((genreId) => profileGenreIds.includes(Number(genreId)))) return false
  }
  if (!matchesSelectedRegion(item)) return false
  if (selectedExperienceLevels.value.length && !selectedExperienceLevels.value.some((code) => code === item.experienceLevel)) return false
  if (!matchesCodeAny(selectedJoinAvailabilities.value, item.joinAvailability)) return false
  if (selectedCollaborationConditions.value.length) {
    const conditionCodes = (item.collaborationConditions || []).map((condition) => condition.conditionCode)
    if (!selectedCollaborationConditions.value.includes('ANY') && !conditionCodes.includes('ANY') && !selectedCollaborationConditions.value.some((code) => conditionCodes.includes(code))) {
      return false
    }
  }
  return true
}

async function loadFollowedMembers() {
  const requestId = ++followedMembersRequestId
  followedMembersLoading.value = true
  followedMembersError.value = ''
  try {
    if (!profile.value?.profileId) {
      followedMembers.value = []
      return
    }
    const data = await slateApi.profileFollowing(profile.value.profileId, { limit: 100, offset: 0 })
    const rows = Array.isArray(data?.items) ? data.items : []
    const enriched = await Promise.all(rows.map(async (row) => {
      try {
        const detail = await slateApi.publicProfile(row.profileId)
        return normalizeFollowedMember({ ...row, ...detail, followedAt: row.followedAt })
      } catch {
        return normalizeFollowedMember(row)
      }
    }))
    if (componentUnmounted || requestId !== followedMembersRequestId || !isFollowedMembersContext.value) return
    followedMembers.value = enriched
    selectRouteCandidate()
  } catch (err) {
    if (componentUnmounted || requestId !== followedMembersRequestId || !isFollowedMembersContext.value) return
    followedMembers.value = []
    followedMembersError.value = err.message || '팔로우한 회원을 불러오지 못했습니다.'
  } finally {
    if (!componentUnmounted && requestId === followedMembersRequestId) followedMembersLoading.value = false
  }
}

function normalizeSentInvitation(row) {
  return {
    ...row,
    userId: row.targetUserId,
    profileId: row.targetProfileId || row.profileId,
    displayName: row.targetDisplayName || row.targetNickname || row.displayName || row.nickname || '초대한 팀원',
    roleName: row.roleName || '모집 역할 정보 없음',
    profileImageUrl: row.profileImageUrl || row.imageUrl || '',
    publicRegionName: row.publicRegionName || row.regionName || '지역 정보 없음',
    score: null,
    scoreBadge: requestStatusLabel(row.status),
    reasons: [row.teamName, row.recruitmentTitle].filter(Boolean),
    invitedByCurrentUser: row.status === 'PENDING',
    followingByCurrentUser: false
  }
}

function normalizeSentApplication(row) {
  return {
    ...row,
    thumbnailUrl: row.thumbnailUrl || row.imageUrl || row.teamImageUrl || '',
    teamDescription: row.teamDescription || row.description || '팀 설명 정보 없음',
    roleName: row.roleName || '모집 역할 정보 없음',
    score: null,
    scoreBadge: requestStatusLabel(row.status),
    reasons: [row.recruitmentTitle, requestStatusLabel(row.status)].filter(Boolean),
    appliedByCurrentUser: row.status === 'PENDING'
  }
}

function matchingResultCards(data) {
  return [...(data?.primary || []), ...(data?.supplementary || [])]
}

function mergeActionRowWithMatchingCard(row, card) {
  if (!card) return row
  const merged = { ...row, ...card }
  if (row.applicationId !== undefined) merged.applicationId = row.applicationId
  if (row.invitationId !== undefined) merged.invitationId = row.invitationId
  if (row.status !== undefined) merged.status = row.status
  if (row.createdAt !== undefined) merged.createdAt = row.createdAt
  if (row.decidedAt !== undefined) merged.decidedAt = row.decidedAt
  if (row.message !== undefined) merged.message = row.message
  if (row.applicationId !== undefined) merged.appliedByCurrentUser = normalizedRequestStatus(row.status) === 'PENDING'
  if (row.invitationId !== undefined) merged.invitedByCurrentUser = normalizedRequestStatus(row.status) === 'PENDING'
  return merged
}

async function enrichSentInvitationsWithScores(rows = []) {
  const pairs = new Map()
  rows.forEach((row) => {
    const teamId = positiveId(row.teamId)
    const slotId = positiveId(row.slotId)
    if (teamId && slotId) pairs.set(`${teamId}-${slotId}`, { teamId, slotId })
  })
  if (!pairs.size) return rows
  const results = await Promise.allSettled([...pairs.values()].map(async ({ teamId, slotId }) => ({
    teamId,
    slotId,
    cards: matchingResultCards(await slateApi.teamToMembers({ teamId, slotId }))
  })))
  const scoreCards = new Map()
  results.forEach((result) => {
    if (result.status !== 'fulfilled') return
    const { teamId, slotId, cards } = result.value
    cards.forEach((card) => {
      const userId = candidateUserId(card)
      const profileId = candidateProfileId(card)
      if (userId) scoreCards.set(`${teamId}-${slotId}-user-${userId}`, card)
      if (profileId) scoreCards.set(`${teamId}-${slotId}-profile-${profileId}`, card)
    })
  })
  return rows.map((row) => {
    const teamId = positiveId(row.teamId)
    const slotId = positiveId(row.slotId)
    const userId = candidateUserId(row)
    const profileId = candidateProfileId(row)
    const card = (userId && scoreCards.get(`${teamId}-${slotId}-user-${userId}`))
      || (profileId && scoreCards.get(`${teamId}-${slotId}-profile-${profileId}`))
    return mergeActionRowWithMatchingCard(row, card)
  })
}

async function enrichSentApplicationsWithScores(rows = []) {
  if (!profile.value?.profileId || !rows.length) return rows
  try {
    const data = await slateApi.memberToTeams({ profileId: profile.value.profileId })
    const scoreCards = new Map()
    matchingResultCards(data).forEach((card) => {
      const teamId = positiveId(card.teamId)
      const slotId = positiveId(card.slotId)
      if (teamId && slotId) scoreCards.set(`${teamId}-${slotId}`, card)
    })
    return rows.map((row) => {
      const teamId = positiveId(row.teamId)
      const slotId = positiveId(row.slotId)
      return mergeActionRowWithMatchingCard(row, scoreCards.get(`${teamId}-${slotId}`))
    })
  } catch {
    return rows
  }
}

function hasPendingRequestStatus(item) {
  return item?.status == null || normalizedRequestStatus(item.status) === 'PENDING'
}

function markSentInvitationStatus(invitationId, status) {
  const active = normalizedRequestStatus(status) === 'PENDING'
  sentInvitations.value.forEach((row) => {
    if (Number(row.invitationId) === Number(invitationId)) {
      row.status = status
      row.invitedByCurrentUser = active
    }
  })
  if (selected.value && Number(selected.value.invitationId) === Number(invitationId)) {
    selected.value.status = status
    selected.value.invitedByCurrentUser = active
  }
}

function markSentApplicationStatus(applicationId, status) {
  const active = normalizedRequestStatus(status) === 'PENDING'
  sentApplications.value.forEach((row) => {
    if (Number(row.applicationId) === Number(applicationId)) {
      row.status = status
      row.appliedByCurrentUser = active
    }
  })
  if (selected.value && Number(selected.value.applicationId) === Number(applicationId)) {
    selected.value.status = status
    selected.value.appliedByCurrentUser = active
  }
}

async function loadSentInvitations() {
  const requestId = ++sentInvitationsRequestId
  sentInvitationsLoading.value = true
  sentInvitationsError.value = ''
  try {
    const rows = await slateApi.sentInvitations()
    if (componentUnmounted || requestId !== sentInvitationsRequestId || !isInvitedMembersContext.value) return
    const enriched = await Promise.all((rows || []).map(async (row) => {
      const profileId = row.targetProfileId || row.profileId
      if (!profileId) return row
      try {
        const detail = await slateApi.publicProfile(profileId)
        return { ...row, ...detail, status: row.status, createdAt: row.createdAt, targetProfileId: profileId }
      } catch {
        return row
      }
    }))
    if (componentUnmounted || requestId !== sentInvitationsRequestId || !isInvitedMembersContext.value) return
    const scoredRows = await enrichSentInvitationsWithScores(enriched.map(normalizeSentInvitation))
    if (componentUnmounted || requestId !== sentInvitationsRequestId || !isInvitedMembersContext.value) return
    sentInvitations.value = scoredRows
    selectRouteCandidate()
  } catch (err) {
    if (componentUnmounted || requestId !== sentInvitationsRequestId || !isInvitedMembersContext.value) return
    sentInvitations.value = []
    sentInvitationsError.value = err.message || '초대한 팀원 목록을 불러오지 못했습니다.'
  } finally {
    if (!componentUnmounted && requestId === sentInvitationsRequestId) sentInvitationsLoading.value = false
  }
}

async function loadSavedTeams() {
  const requestId = ++savedTeamsRequestId
  savedTeamsLoading.value = true
  savedTeamsError.value = ''
  try {
    const rows = await slateApi.matchingBookmarks('TEAM')
    if (componentUnmounted || requestId !== savedTeamsRequestId || !isSavedTeamsView.value) return
    savedTeams.value = rows || []
    savedTeamRoleSelections.value = {}
    expandedSavedTeamDescriptionIds.value = {}
  } catch (err) {
    if (componentUnmounted || requestId !== savedTeamsRequestId || !isSavedTeamsView.value) return
    savedTeams.value = []
    savedTeamsError.value = err.message || '저장한 팀을 불러오지 못했습니다.'
  } finally {
    if (!componentUnmounted && requestId === savedTeamsRequestId) savedTeamsLoading.value = false
  }
}

async function loadReferenceFilters() {
  genreLoading.value = true
  regionLoading.value = true
  codeLoading.value = true
  genreError.value = ''
  regionError.value = ''
  codeError.value = ''
  collaborationConditionError.value = ''
  const [genreResult, regionResult, roleResult, codeResult] = await Promise.allSettled([
    slateApi.genres(),
    slateApi.regions('', 1000),
    slateApi.roles(),
    slateApi.codes(['EXPERIENCE_LEVEL', 'JOIN_AVAILABILITY', 'COLLABORATION_CONDITION', 'DURATION'])
  ])
  if (genreResult.status === 'fulfilled') genres.value = genreResult.value || []
  else {
    genres.value = []
    genreError.value = '장르 목록을 불러오지 못했습니다.'
  }
  genreLoading.value = false
  if (regionResult.status === 'fulfilled') regions.value = regionResult.value || []
  else {
    regions.value = []
    regionError.value = '지역 목록을 불러오지 못했습니다.'
  }
  regionLoading.value = false
  roleGroups.value = roleResult.status === 'fulfilled' ? roleResult.value || [] : []
  if (codeResult.status === 'fulfilled') {
    experienceLevels.value = codeResult.value.EXPERIENCE_LEVEL || []
    joinAvailabilities.value = codeResult.value.JOIN_AVAILABILITY || []
    collaborationConditions.value = codeResult.value.COLLABORATION_CONDITION || []
    durations.value = codeResult.value.DURATION || []
  }
  else {
    experienceLevels.value = []
    joinAvailabilities.value = []
    collaborationConditions.value = []
    durations.value = []
    codeError.value = '경력·합류 일정·협업 조건을 불러오지 못했습니다.'
    collaborationConditionError.value = codeError.value
  }
  codeLoading.value = false
}

function resetFollowState() {
  followStatusRequestId += 1
  selectedFollowStatus.value = null
  followStatusLoading.value = false
  followToggleLoading.value = false
  followError.value = ''
}

async function loadSelectedFollowStatus() {
  const profileId = Number(selectedCard.value?.profileId)
  const requestId = ++followStatusRequestId
  selectedFollowStatus.value = null
  followToggleLoading.value = false
  followError.value = ''
  if (!canShowFollowControl.value) {
    followStatusLoading.value = false
    return
  }
  followStatusLoading.value = true
  try {
    const data = await slateApi.followStatus(profileId)
    if (requestId !== followStatusRequestId || Number(selectedCard.value?.profileId) !== profileId) return
    selectedFollowStatus.value = data
  } catch (err) {
    if (requestId !== followStatusRequestId || Number(selectedCard.value?.profileId) !== profileId) return
    followError.value = err.message || '프로필을 확인할 수 없습니다.'
  } finally {
    if (requestId === followStatusRequestId) followStatusLoading.value = false
  }
}

async function toggleSelectedFollow() {
  const profileId = Number(selectedCard.value?.profileId)
  if (!canShowFollowControl.value || !selectedFollowStatus.value || followToggleLoading.value) return
  followToggleLoading.value = true
  followError.value = ''
  notice.value = ''
  error.value = ''
  try {
    const data = selectedFollowStatus.value.following
      ? await slateApi.unfollowProfile(profileId)
      : await slateApi.followProfile(profileId)
    if (Number(selectedCard.value?.profileId) !== profileId) return
    selectedFollowStatus.value = { ...selectedFollowStatus.value, ...data }
    updateCandidateProfileState(profileId, { followingByCurrentUser: Boolean(data.following) })
    notice.value = data.following ? '팔로우했습니다.' : '팔로우를 취소했습니다.'
  } catch (err) {
    if (Number(selectedCard.value?.profileId) !== profileId) return
    followError.value = err.message || '팔로우 상태를 변경하지 못했습니다.'
  } finally {
    if (Number(selectedCard.value?.profileId) === profileId) followToggleLoading.value = false
  }
}

function candidateProfileId(item) {
  return positiveId(item?.profileId)
}

function candidateUserId(item) {
  return positiveId(item?.userId)
}

function setLoadingFlag(mapRef, key, loading) {
  if (!key) return
  const next = { ...mapRef.value }
  if (loading) next[key] = true
  else delete next[key]
  mapRef.value = next
}

function updateCandidateProfileState(profileId, values) {
  const numericProfileId = Number(profileId)
  const updateRows = (rows = []) => rows.forEach((candidate) => {
    if (Number(candidate?.profileId) === numericProfileId) Object.assign(candidate, values)
  })
  updateRows(teamToMembers.value.primary)
  updateRows(teamToMembers.value.supplementary)
  updateRows(followedMembers.value)
  if (selected.value && Number(selected.value.profileId) === numericProfileId) Object.assign(selected.value, values)
  if (selectedFollowStatus.value && Number(selectedCard.value?.profileId) === numericProfileId && values.followingByCurrentUser !== undefined) {
    selectedFollowStatus.value = {
      ...selectedFollowStatus.value,
      following: Boolean(values.followingByCurrentUser)
    }
  }
}

async function loadSentApplications() {
  const requestId = ++sentApplicationsRequestId
  sentApplicationsLoading.value = true
  sentApplicationsError.value = ''
  try {
    const rows = await slateApi.sentApplications()
    if (componentUnmounted || requestId !== sentApplicationsRequestId || !isAppliedTeamsContext.value) return
    const scoredRows = await enrichSentApplicationsWithScores((rows || []).map(normalizeSentApplication))
    if (componentUnmounted || requestId !== sentApplicationsRequestId || !isAppliedTeamsContext.value) return
    sentApplications.value = scoredRows
    selectRouteCandidate()
  } catch (err) {
    if (componentUnmounted || requestId !== sentApplicationsRequestId || !isAppliedTeamsContext.value) return
    sentApplications.value = []
    sentApplicationsError.value = err.message || '지원한 팀 목록을 불러오지 못했습니다.'
  } finally {
    if (!componentUnmounted && requestId === sentApplicationsRequestId) sentApplicationsLoading.value = false
  }
}

function isCandidateFollowed(item) {
  return Boolean(item?.followingByCurrentUser)
}

function isCandidateFollowLoading(item) {
  return Boolean(candidateFollowLoadingIds.value[candidateProfileId(item)])
}

function candidateFollowText(item) {
  if (isCandidateFollowLoading(item)) return '처리 중'
  return isCandidateFollowed(item) ? '팔로잉' : '팔로우'
}

async function toggleCandidateFollow(item) {
  const profileId = candidateProfileId(item)
  if (!profileId || isCandidateFollowLoading(item)) return
  setLoadingFlag(candidateFollowLoadingIds, profileId, true)
  notice.value = ''
  error.value = ''
  try {
    const data = isCandidateFollowed(item)
      ? await slateApi.unfollowProfile(profileId)
      : await slateApi.followProfile(profileId)
    updateCandidateProfileState(profileId, { followingByCurrentUser: Boolean(data?.following) })
    notice.value = data?.following ? '팔로우했습니다.' : '팔로우를 취소했습니다.'
  } catch (err) {
    error.value = err.message || '팔로우 상태를 변경하지 못했습니다.'
  } finally {
    setLoadingFlag(candidateFollowLoadingIds, profileId, false)
  }
}

function updateCandidateInvitationState(userId, invited = true) {
  const numericUserId = Number(userId)
  const updateRows = (rows = []) => rows.forEach((candidate) => {
    if (Number(candidate?.userId) === numericUserId) {
      candidate.invitedByCurrentUser = invited
      if (!invited && candidate.status == null) candidate.invitationId = null
    }
  })
  updateRows(teamToMembers.value.primary)
  updateRows(teamToMembers.value.supplementary)
  updateRows(followedMembers.value)
  updateRows(sentInvitations.value)
  if (selected.value && Number(selected.value.userId) === numericUserId) {
    selected.value.invitedByCurrentUser = invited
    if (!invited && selected.value.status == null) selected.value.invitationId = null
  }
}

function isCandidateInvited(item) {
  return hasPendingRequestStatus(item) && Boolean(item?.invitedByCurrentUser || item?.invitationId)
}

function isCandidateInviteLoading(item) {
  return Boolean(candidateInviteLoadingIds.value[candidateUserId(item)])
}

function canInviteCandidate(item) {
  return Boolean(selectedTeamId.value && selectedSlotId.value && candidateUserId(item))
}

function isCandidateInviteDisabled(item) {
  return isCandidateInviteLoading(item) || (!isCandidateInvited(item) && !canInviteCandidate(item))
}

function candidateInviteText(item) {
  if (isCandidateInviteLoading(item)) return '처리 중'
  if (!isCandidateInvited(item) && !canInviteCandidate(item)) return '팀 선택 필요'
  return isCandidateInvited(item) ? '초대됨' : '초대'
}

function isDuplicateInvitationError(err) {
  return String(err?.message || '').includes('이미 대기 중인 초대')
}

function setCandidateInvitationCreated(userId, invitationId) {
  const numericUserId = Number(userId)
  const updateRows = (rows = []) => rows.forEach((candidate) => {
    if (Number(candidate?.userId) === numericUserId) {
      candidate.invitedByCurrentUser = true
      if (candidate.status != null) candidate.status = 'PENDING'
      if (invitationId) candidate.invitationId = invitationId
    }
  })
  updateRows(teamToMembers.value.primary)
  updateRows(teamToMembers.value.supplementary)
  updateRows(followedMembers.value)
  if (selected.value && Number(selected.value.userId) === numericUserId) {
    selected.value.invitedByCurrentUser = true
    if (selected.value.status != null) selected.value.status = 'PENDING'
    if (invitationId) selected.value.invitationId = invitationId
  }
}

async function cancelCandidateInvitation(item) {
  const invitationId = Number(item?.invitationId)
  const targetUserId = candidateUserId(item)
  if (!invitationId || !targetUserId) {
    error.value = '취소할 초대 정보를 확인할 수 없습니다.'
    return
  }
  setLoadingFlag(candidateInviteLoadingIds, targetUserId, true)
  notice.value = ''
  error.value = ''
  try {
    await slateApi.cancelInvitation(invitationId)
    markSentInvitationStatus(invitationId, 'CANCELED')
    updateCandidateInvitationState(targetUserId, false)
  } catch (err) {
    error.value = err.message || '초대를 취소하지 못했습니다.'
  } finally {
    setLoadingFlag(candidateInviteLoadingIds, targetUserId, false)
  }
}

function teamApplicationKey(item) {
  const teamId = Number(item?.teamId)
  const slotId = Number(item?.slotId)
  return Number.isFinite(teamId) && Number.isFinite(slotId) ? `${teamId}-${slotId}` : ''
}

function isTeamApplicationLoading(item) {
  return Boolean(teamApplicationLoadingIds.value[teamApplicationKey(item)])
}

function isTeamApplied(item) {
  return hasPendingRequestStatus(item) && Boolean(item?.appliedByCurrentUser || item?.applicationId)
}

function teamApplyText(item) {
  if (isTeamApplicationLoading(item)) return '처리 중'
  return isTeamApplied(item) ? '지원됨' : '지원'
}

function updateTeamApplicationState(teamId, slotId, applied = true, applicationId = null) {
  const numericTeamId = Number(teamId)
  const numericSlotId = Number(slotId)
  const updateRows = (rows = []) => rows.forEach((team) => {
    if (Number(team?.teamId) === numericTeamId && Number(team?.slotId) === numericSlotId) {
      team.appliedByCurrentUser = applied
      if (applied) {
        if (team.status != null) team.status = 'PENDING'
        team.applicationId = applicationId || team.applicationId
      }
      else if (team.status == null) team.applicationId = null
    }
  })
  updateRows(memberToTeams.value.primary)
  updateRows(memberToTeams.value.supplementary)
  savedTeams.value.forEach((team) => {
    ;(team.openRoles || []).forEach((role) => {
      if (Number(team.teamId) === numericTeamId && Number(role.slotId) === numericSlotId) {
        role.appliedByCurrentUser = applied
        if (applied) {
          if (role.status != null) role.status = 'PENDING'
          role.applicationId = applicationId || role.applicationId
        }
        else if (role.status == null) role.applicationId = null
      }
    })
  })
  if (selected.value && Number(selected.value.teamId) === numericTeamId && Number(selected.value.slotId) === numericSlotId) {
    selected.value.appliedByCurrentUser = applied
    if (applied) {
      if (selected.value.status != null) selected.value.status = 'PENDING'
      selected.value.applicationId = applicationId || selected.value.applicationId
    }
    else if (selected.value.status == null) selected.value.applicationId = null
  }
}

async function cancelTeamApplication(item) {
  const applicationId = Number(item?.applicationId)
  if (!applicationId) {
    error.value = '취소할 지원 정보를 확인할 수 없습니다.'
    return
  }
  const key = teamApplicationKey(item)
  setLoadingFlag(teamApplicationLoadingIds, key, true)
  notice.value = ''
  error.value = ''
  try {
    await slateApi.cancelApplication(applicationId)
    markSentApplicationStatus(applicationId, 'CANCELED')
    updateTeamApplicationState(item.teamId, item.slotId, false)
  } catch (err) {
    error.value = err.message || '지원을 취소하지 못했습니다.'
  } finally {
    setLoadingFlag(teamApplicationLoadingIds, key, false)
  }
}

async function cancelSentInvitation(item) {
  sentInvitationActionId.value = item?.invitationId || null
  try {
    await cancelCandidateInvitation(item)
  } finally {
    if (sentInvitationActionId.value === item?.invitationId) sentInvitationActionId.value = null
  }
}

async function cancelSentApplication(item) {
  sentApplicationActionId.value = item?.applicationId || null
  try {
    await cancelTeamApplication(item)
  } finally {
    if (sentApplicationActionId.value === item?.applicationId) sentApplicationActionId.value = null
  }
}

function openSentInvitationDetail(item) {
  const userId = candidateUserId(item)
  if (!userId) return
  const query = matchingListQuery('invited')
  if (item.teamId) query.teamId = String(item.teamId)
  if (item.slotId) query.slotId = String(item.slotId)
  router.push({
    name: 'matching-members-detail',
    params: { userId },
    query
  })
}

function openSentApplicationDetail(item) {
  const teamId = positiveId(item?.teamId)
  if (!teamId) return
  const query = matchingListQuery('applied')
  if (item.slotId) query.slotId = String(item.slotId)
  router.push({
    name: 'matching-teams-detail',
    params: { teamId },
    query
  })
}

function resetAiRecommendations() {
  aiRecommendationRequestId += 1
  aiRecommendationLoading.value = false
  aiRecommendationError.value = ''
  aiRecommendations.value = []
  activeAiRecommendationIndex.value = 0
  aiRecommendationRequested.value = false
}

async function load() {
  const requestId = ++loadRequestId
  if (!getToken()) {
    resetMatchState()
    return
  }
  mode.value = routeMode()
  loading.value = true
  error.value = ''
  notice.value = ''
  resetMatchState()
  mode.value = routeMode()
  restoreFilterQuery()
  teamsLoading.value = true
  teamsError.value = ''
  try {
    const [profileResult, teamsResult, policyResult] = await Promise.allSettled([
      slateApi.myProfile(),
      slateApi.myTeams(),
      slateApi.policy()
    ])
    await loadReferenceFilters()
    if (componentUnmounted || requestId !== loadRequestId) return
    if (profileResult.status === 'fulfilled') {
      profile.value = profileResult.value
    } else {
      profile.value = null
      if (mode.value === 'memberToTeams') throw profileResult.reason
    }
    policy.value = policyResult.status === 'fulfilled' ? policyResult.value : null
    if (teamsResult.status === 'fulfilled') teams.value = teamsResult.value || []
    else {
      teams.value = []
      teamsError.value = teamsResult.reason?.message || '관리 가능한 팀을 불러오지 못했습니다.'
    }
    teamsLoading.value = false
    sanitizeFilterSelections()
    if (isSavedTeamsView.value) {
      await loadSavedTeams()
      return
    }
    if (isAppliedTeamsContext.value) {
      await normalizeCurrentQuery()
      await loadSentApplications()
      return
    }
    if (mode.value === 'memberToTeams') {
      await normalizeCurrentQuery()
      appliedTeamFilterSignature.value = hasAppliedTeamFilters.value ? filterSignature() : ''
      teamFiltersNeedApply.value = false
      await refreshMatches()
      return
    }
    if (manageableTeams.value.length) {
      const queryTeamId = Number(route.query.teamId)
      const hasValidQueryTeam = manageableTeams.value.some((team) => Number(team.teamId) === queryTeamId)
      if (hasValidQueryTeam) {
        basisSelectionCleared = false
        selectedTeamId.value = queryTeamId
        await loadRecruitments({
          teamId: selectedTeamId.value,
          preferredSlotId: route.query.slotId,
          selectFirstWhenMissing: false
        })
      } else {
        basisSelectionCleared = true
      }
    }
    if (componentUnmounted || requestId !== loadRequestId) return
    await normalizeCurrentQuery()
    if (isInvitedMembersContext.value) {
      await loadSentInvitations()
      return
    }
    if (isFollowedMembersContext.value) {
      await loadFollowedMembers()
      return
    }
    await refreshMatches()
  } catch (err) {
    if (!componentUnmounted && requestId === loadRequestId) error.value = err.message
  } finally {
    if (!componentUnmounted && requestId === loadRequestId) {
      loading.value = false
      teamsLoading.value = false
      if (pendingAiRecommendationAfterRoute && isAiView.value) {
        pendingAiRecommendationAfterRoute = false
        void runAiRecommendations()
      }
    }
  }
}

async function loadRecruitments({ teamId, preferredSlotId = null, selectFirstWhenMissing = false }) {
  const requestId = ++recruitmentRequestId
  recruitmentsLoading.value = true
  recruitmentsError.value = ''
  const requestedTeamId = Number(teamId)
  if (!requestedTeamId) {
    recruitments.value = []
    selectedSlotId.value = null
    recruitmentsLoading.value = false
    return
  }
  try {
    const rows = await slateApi.recruitments(requestedTeamId)
    if (componentUnmounted || Number(selectedTeamId.value) !== requestedTeamId) return
    recruitments.value = rows || []
    const preferredId = Number(preferredSlotId)
    const currentSlotId = Number(selectedSlotId.value)
    selectedSlotId.value = openSlots.value.some((slot) => Number(slot.slotId) === preferredId)
      ? preferredId
      : openSlots.value.some((slot) => Number(slot.slotId) === currentSlotId)
        ? currentSlotId
        : selectFirstWhenMissing ? openSlots.value[0]?.slotId || null : null
  } catch (err) {
    if (componentUnmounted || Number(selectedTeamId.value) !== requestedTeamId) return
    recruitments.value = []
    selectedSlotId.value = null
    recruitmentsError.value = err.message || '모집 역할을 불러오지 못했습니다.'
  } finally {
    if (!componentUnmounted && (requestId === recruitmentRequestId || Number(selectedTeamId.value) === requestedTeamId)) {
      recruitmentsLoading.value = false
    }
  }
}

async function refreshMatches() {
  if (mode.value === 'memberToTeams' && !profile.value) {
    error.value = '팀 찾기를 이용하려면 프로필이 필요합니다.'
    return
  }
  const requestId = ++refreshRequestId
  resetAiRecommendations()
  loading.value = true
  error.value = ''
  try {
    if (mode.value === 'teamToMembers') {
      teamToMembers.value = emptyResult()
      const query = { ...filterQuery() }
      if (selectedTeamId.value) query.teamId = selectedTeamId.value
      if (selectedTeamId.value && selectedSlotId.value) query.slotId = selectedSlotId.value
      const data = await slateApi.teamToMembers(query)
      if (requestId !== refreshRequestId) return
      teamToMembers.value = data
      resetResultPage()
    } else {
      const data = await slateApi.memberToTeams({ profileId: profile.value.profileId, ...filterQuery() })
      if (requestId !== refreshRequestId) return
      memberToTeams.value = data
      resetResultPage()
    }
    selectRouteCandidate()
    if (isTeamDetailRoute.value && selected.value && positiveId(route.query.slotId) !== Number(selected.value.slotId)) {
      await replaceCurrentQuery(matchingListQuery())
    }
  } catch (err) {
    if (requestId !== refreshRequestId) return
    error.value = err.message
  } finally {
    if (requestId === refreshRequestId) loading.value = false
  }
}

async function onTeamChange() {
  const interactionId = ++basisChangeRequestId
  basisSelectionCleared = false
  recruitmentRequestId += 1
  refreshRequestId += 1
  resetAiRecommendations()
  recruitments.value = []
  recruitmentsError.value = ''
  recruitmentsLoading.value = false
  selectedSlotId.value = null
  teamToMembers.value = emptyResult()
  selected.value = null
  loading.value = false
  if (interactionId !== basisChangeRequestId) return
  await loadRecruitments({ teamId: selectedTeamId.value, selectFirstWhenMissing: false })
  if (interactionId !== basisChangeRequestId) return
  await syncMemberQuery()
  if (interactionId !== basisChangeRequestId) return
  if (isFollowedMembersContext.value) {
    await loadFollowedMembers()
    return
  }
  await refreshMatches()
}

async function selectOnlyManageableTeam() {
  if (manageableTeams.value.length !== 1) return
  selectedTeamId.value = manageableTeams.value[0].teamId
  await onTeamChange()
}

async function onSlotChange() {
  const interactionId = ++basisChangeRequestId
  basisSelectionCleared = false
  refreshRequestId += 1
  teamToMembers.value = emptyResult()
  selected.value = null
  loading.value = false
  resetAiRecommendations()
  await syncMemberQuery()
  if (interactionId !== basisChangeRequestId) return
  if (isFollowedMembersContext.value) {
    await loadFollowedMembers()
    return
  }
  await refreshMatches()
}
async function selectSlot(slotId) {
  const numericSlotId = Number(slotId)
  if (!Number.isFinite(numericSlotId)) return
  selectedSlotId.value = Number(selectedSlotId.value) === numericSlotId ? null : numericSlotId
  await onSlotChange()
}

function hiringSlots() {
  return openSlots.value
}

function hasHiringSlots() {
  return hiringSlots().length > 0
}

function isRecruitmentLoading() {
  return recruitmentsLoading.value
}

function recruitmentErrorText() {
  return recruitmentsError.value
}

function selectedTeamIdValue() {
  return selectedTeamId.value
}

function isSlotActive(slot) {
  return Number(selectedSlotId.value) === Number(slot?.slotId)
}

function roleFilterOptions() {
  return matchingRoleOptions.value
}

function hasRoleFilterOptions() {
  return roleFilterOptions().length > 0
}

function isRoleFilterSelected(roleId) {
  return selectedRoleIds.value.includes(Number(roleId))
}

function isMatchingLoading() {
  return loading.value
}

function slotScheduleText(slot) {
  const parts = []
  const duration = codeLabel(durations, slot?.roleDuration || slot?.duration || slot?.expectedDuration)
  if (duration && duration !== '정보 없음') parts.push(duration)
  const startAt = formatDate(slot?.workStartAt || slot?.startAt)
  if (startAt && startAt !== '정보 없음') parts.push(`시작 ${startAt}`)
  const deadline = formatDate(slot?.deadlineAt || slot?.deadline)
  if (deadline && deadline !== '정보 없음') parts.push(`마감 ${deadline}`)
  return parts.join(' · ') || '일정 정보 없음'
}

async function applyFilters() {
  error.value = ''
  if (mode.value === 'memberToTeams') {
    loading.value = false
    notice.value = ''
    resetAiRecommendations()
    appliedTeamFilterSignature.value = filterSignature()
    teamFiltersNeedApply.value = false
    await replaceCurrentQuery({ ...filterQuery(), applied: '1' })
    await refreshMatches()
    return
  }
  resetAiRecommendations()
  const view = isFollowedMembersContext.value ? 'following' : ''
  await replaceCurrentQuery(matchingListQuery(view, mode.value === 'teamToMembers'))
  if (isFollowedMembersContext.value) {
    await loadFollowedMembers()
    return
  }
  await refreshMatches()
}

async function resetFilters() {
  selectedGenreIds.value = []
  selectedRoleIds.value = []
  selectedTopRegions.value = []
  selectedRegionIds.value = []
  selectedExperienceLevels.value = []
  selectedJoinAvailabilities.value = []
  selectedCollaborationConditions.value = []
  genreKeyword.value = ''
  topRegionKeyword.value = ''
  regionKeyword.value = ''
  topRegionDropdownOpen.value = false
  regionDropdownOpen.value = false
  error.value = ''
  notice.value = '필터가 초기화되었습니다. 검색 버튼을 누르면 초기화된 조건으로 다시 조회합니다.'
  resetAiRecommendations()

  if (mode.value === 'teamToMembers') {
    const view = isFollowedMembersContext.value ? 'following' : ''
    await replaceCurrentQuery(matchingListQuery(view, true))
    return
  }

  const view = isAiView.value ? 'ai' : ''
  const shouldKeepAppliedState = hasAppliedTeamFilters.value
  teamFiltersNeedApply.value = shouldKeepAppliedState
  await replaceCurrentQuery(matchingListQuery(view, true, shouldKeepAppliedState))
}

function normalizedQueryEntries(query) {
  return Object.entries(query)
    .flatMap(([key, value]) => routeValues(value).map((item) => [key, item]))
    .sort(([leftKey, leftValue], [rightKey, rightValue]) => leftKey.localeCompare(rightKey) || leftValue.localeCompare(rightValue))
}

async function replaceCurrentQuery(query) {
  if (JSON.stringify(normalizedQueryEntries(route.query)) === JSON.stringify(normalizedQueryEntries(query))) return false
  const signature = JSON.stringify(normalizedQueryEntries(query))
  internalQueryReplacements.add(signature)
  try {
    await router.replace({ query })
    await nextTick()
    setTimeout(() => internalQueryReplacements.delete(signature), 0)
  } catch (err) {
    internalQueryReplacements.delete(signature)
    throw err
  }
  return true
}

async function normalizeCurrentQuery() {
  const normalized = matchingListQuery()
  await replaceCurrentQuery(normalized)
}

function currentMatchingView() {
  if (isAiContext.value) return 'ai'
  if (isFollowedMembersContext.value) return 'following'
  if (isInvitedMembersContext.value) return 'invited'
  if (isAppliedTeamsContext.value) return 'applied'
  if (isSavedTeamsView.value) return 'saved'
  return ''
}

function matchingListQuery(view = currentMatchingView(), includeTeamSlot = true, includeApplied = hasAppliedTeamFilters.value) {
  const query = {}
  const viewName = view === true ? 'ai' : view === false ? '' : view
  if (['ai', 'following', 'invited', 'saved', 'applied'].includes(viewName)) query.view = viewName
  if (mode.value === 'teamToMembers') {
    if (selectedTeamId.value) query.teamId = String(selectedTeamId.value)
    if (selectedSlotId.value) query.slotId = String(selectedSlotId.value)
  } else if (includeTeamSlot) {
    const slotId = positiveId(selected.value?.slotId) || positiveId(route.query.slotId)
    if (slotId) query.slotId = String(slotId)
  }
  if (mode.value === 'memberToTeams' && includeApplied) query.applied = '1'
  Object.assign(query, filterQuery())
  return query
}

function matchingDataRouteSignature() {
  const queryKeys = [
    'mode',
    'view',
    'applied',
    'teamId',
    'slotId',
    'genreIds',
    'roleIds',
    'regionSidos',
    'regionIds',
    'experienceLevel',
    'joinAvailability',
    'collaborationCondition'
  ]
  const query = {}
  queryKeys.forEach((key) => {
    if (route.query[key] !== undefined) query[key] = route.query[key]
  })
  return JSON.stringify({
    userId: props.currentUser?.userId || null,
    mode: routeMode(),
    query: normalizedQueryEntries(query)
  })
}

function returnToMatchingList() {
  return router.push({
    name: mode.value === 'teamToMembers' ? 'matching-members' : 'matching-teams',
    query: matchingListQuery()
  })
}

function showGeneralResults() {
  resetAiRecommendations()
  return router.push({
    name: mode.value === 'teamToMembers' ? 'matching-members' : 'matching-teams',
    query: matchingListQuery('')
  })
}

async function requestAiRecommendations() {
  if (aiRecommendationLoading.value) return
  if (aiRecommendationDisabledReason.value) {
    aiRecommendationError.value = aiRecommendationDisabledReason.value
    return
  }

  if (!isAiView.value) {
    pendingAiRecommendationAfterRoute = true
    aiRecommendationError.value = ''
    aiRecommendationRequested.value = true
    try {
      await router.push({
        name: mode.value === 'teamToMembers' ? 'matching-members' : 'matching-teams',
        query: matchingListQuery(true)
      })
    } catch (err) {
      pendingAiRecommendationAfterRoute = false
      aiRecommendationError.value = err.message || 'AI 추천 화면으로 이동하지 못했습니다.'
    }
    return
  }

  await runAiRecommendations()
}

async function runAiRecommendations() {
  if (aiRecommendationLoading.value) return
  if (aiRecommendationDisabledReason.value) {
    aiRecommendationError.value = aiRecommendationDisabledReason.value
    return
  }
  const requestId = ++aiRecommendationRequestId
  aiRecommendationError.value = ''
  aiRecommendations.value = []
  activeAiRecommendationIndex.value = 0
  aiRecommendationRequested.value = true

  const payload = mode.value === 'teamToMembers'
    ? { type: 'TEAM_TO_MEMBER' }
    : { type: 'MEMBER_TO_TEAM', profileId: profile.value?.profileId }
  if (mode.value === 'teamToMembers' && selectedTeamId.value) payload.teamId = selectedTeamId.value
  if (mode.value === 'teamToMembers' && selectedTeamId.value && selectedSlotId.value) payload.slotId = selectedSlotId.value

  aiRecommendationLoading.value = true
  try {
    const data = await slateApi.aiMatchingRecommendations(payload)
    if (componentUnmounted || requestId !== aiRecommendationRequestId) return
    aiRecommendations.value = Array.isArray(data?.recommendations) ? data.recommendations.slice(0, 3) : []
    activeAiRecommendationIndex.value = 0
  } catch (err) {
    if (componentUnmounted || requestId !== aiRecommendationRequestId) return
    aiRecommendationError.value = err.message || 'AI 추천을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.'
  } finally {
    if (!componentUnmounted && requestId === aiRecommendationRequestId) aiRecommendationLoading.value = false
  }
}

function showPreviousAiRecommendation() {
  if (activeAiRecommendationIndex.value > 0) {
    activeAiRecommendationIndex.value -= 1
  }
}

function showNextAiRecommendation() {
  if (activeAiRecommendationIndex.value < aiRecommendations.value.length - 1) {
    activeAiRecommendationIndex.value += 1
  }
}

function selectAiRecommendation(index) {
  activeAiRecommendationIndex.value = index
}

function findAiRecommendationCard(recommendation) {
  const targetId = positiveId(recommendation?.targetId)
  if (!targetId) return null
  if (recommendation.targetType === 'TEAM') {
    return allCards.value.find((item) => Number(item.teamId) === targetId) || null
  }
  return allCards.value.find((item) => Number(item.profileId) === targetId || Number(item.userId) === targetId) || null
}

function openAiRecommendation(recommendation = activeAiRecommendation.value) {
  if (!recommendation) return
  const matchedCard = findAiRecommendationCard(recommendation)
  if (recommendation.targetType === 'TEAM') {
    const teamId = positiveId(matchedCard?.teamId) || positiveId(recommendation.targetId)
    if (!teamId) return
    const query = matchingListQuery(true)
    if (matchedCard?.slotId) query.slotId = String(matchedCard.slotId)
    router.push({ name: 'matching-teams-detail', params: { teamId }, query })
    return
  }
  const userId = positiveId(matchedCard?.userId) || positiveId(matchedCard?.profileId) || positiveId(recommendation.targetId)
  if (!userId) return
  router.push({ name: 'matching-members-detail', params: { userId }, query: matchingListQuery(true) })
}

async function save(item) {
  notice.value = ''
  error.value = ''
  try {
    const targetType = mode.value === 'teamToMembers' ? 'PROFILE' : 'TEAM'
    const targetId = mode.value === 'teamToMembers' ? item.profileId : item.teamId
    if (targetType === 'TEAM' && item.savedByCurrentUser) {
      await slateApi.deleteMatchingBookmark('TEAM', targetId)
      updateRecommendedTeamSavedState(targetId, false)
      if (!isTeamDetailRoute.value) notice.value = '저장을 취소했습니다.'
      return
    }
    const result = await slateApi.bookmark(targetType, targetId)
    if (targetType === 'TEAM') updateRecommendedTeamSavedState(targetId, true)
    if (targetType !== 'TEAM' || !isTeamDetailRoute.value) {
      notice.value = result?.alreadySaved ? '이미 저장된 팀입니다.' : '저장했습니다.'
    }
  } catch (err) {
    error.value = err.message
  }
}

function updateRecommendedTeamSavedState(teamId, saved) {
  const update = (rows) => (rows || []).forEach((item) => {
    if (Number(item.teamId) === Number(teamId)) item.savedByCurrentUser = saved
  })
  update(memberToTeams.value.primary)
  update(memberToTeams.value.supplementary)
  if (selected.value && Number(selected.value.teamId) === Number(teamId)) {
    selected.value.savedByCurrentUser = saved
  }
  if (!saved) {
    savedTeams.value = savedTeams.value.filter((item) => Number(item.teamId) !== Number(teamId))
    const selections = { ...savedTeamRoleSelections.value }
    delete selections[teamId]
    savedTeamRoleSelections.value = selections
    const descriptions = { ...expandedSavedTeamDescriptionIds.value }
    delete descriptions[teamId]
    expandedSavedTeamDescriptionIds.value = descriptions
  } else {
    savedTeams.value.forEach((item) => {
      if (Number(item.teamId) === Number(teamId)) item.savedByCurrentUser = true
    })
  }
}

function savedTeamDescription(team) {
  return team?.teamDescription || '등록된 팀 소개가 없습니다.'
}

function teamDescriptionPreview(team) {
  const description = team?.teamDescription || '팀 설명 정보 없음'
  return description.length > TEAM_DESCRIPTION_PREVIEW_LIMIT
    ? `${description.slice(0, TEAM_DESCRIPTION_PREVIEW_LIMIT).trimEnd()}...`
    : description
}

function isSavedTeamDescriptionLong(team) {
  return savedTeamDescription(team).length > SAVED_TEAM_DESCRIPTION_LIMIT
}

function isSavedTeamDescriptionExpanded(team) {
  return Boolean(expandedSavedTeamDescriptionIds.value[team?.teamId])
}

function savedTeamDescriptionPreview(team) {
  const description = savedTeamDescription(team)
  if (!isSavedTeamDescriptionLong(team) || isSavedTeamDescriptionExpanded(team)) return description
  return `${description.slice(0, SAVED_TEAM_DESCRIPTION_LIMIT).trimEnd()}...`
}

function toggleSavedTeamDescription(team) {
  const teamId = team?.teamId
  if (!teamId) return
  expandedSavedTeamDescriptionIds.value = {
    ...expandedSavedTeamDescriptionIds.value,
    [teamId]: !expandedSavedTeamDescriptionIds.value[teamId]
  }
}

function selectedSavedRole(team) {
  const slotId = Number(savedTeamRoleSelections.value[team.teamId])
  return (team.openRoles || []).find((role) => Number(role.slotId) === slotId) || null
}

function savedTeamSelectedRoleAction(team) {
  const role = selectedSavedRole(team)
  if (!role) return null
  return {
    ...role,
    teamId: team.teamId,
    teamName: team.teamName
  }
}

function savedTeamApplyReason(team) {
  if (!['RECRUITING', 'IN_PROGRESS'].includes(team.teamStatus)) return '현재 활동 중인 모집 팀이 아닙니다.'
  if (!(team.openRoles || []).length) return '현재 지원 가능한 OPEN 역할이 없습니다.'
  if (!selectedSavedRole(team)) return '지원할 역할을 먼저 선택해주세요.'
  return ''
}

function savedTeamApplyText(team) {
  const role = savedTeamSelectedRoleAction(team)
  return role ? teamApplyText(role) : '지원'
}

async function applySavedTeam(team) {
  const role = selectedSavedRole(team)
  const reason = savedTeamApplyReason(team)
  if (reason || !role) {
    error.value = reason
    return
  }
  const action = savedTeamSelectedRoleAction(team)
  if (isTeamApplied(action)) {
    await cancelTeamApplication(action)
    return
  }
  savedTeamActionId.value = team.teamId
  setLoadingFlag(teamApplicationLoadingIds, teamApplicationKey(action), true)
  notice.value = ''
  error.value = ''
  try {
    const result = await slateApi.apply({
      teamId: Number(team.teamId),
      recruitmentId: Number(role.recruitmentId),
      slotId: Number(role.slotId),
      message: '팀에 지원하고 싶습니다.'
    })
    updateTeamApplicationState(team.teamId, role.slotId, true, result?.applicationId)
  } catch (err) {
    error.value = err.message
  } finally {
    setLoadingFlag(teamApplicationLoadingIds, teamApplicationKey(action), false)
    savedTeamActionId.value = null
  }
}

async function removeSavedTeam(team) {
  savedTeamActionId.value = team.teamId
  notice.value = ''
  error.value = ''
  try {
    await slateApi.deleteMatchingBookmark('TEAM', team.teamId)
    savedTeams.value = savedTeams.value.filter((item) => Number(item.teamId) !== Number(team.teamId))
    const selections = { ...savedTeamRoleSelections.value }
    delete selections[team.teamId]
    savedTeamRoleSelections.value = selections
    const descriptions = { ...expandedSavedTeamDescriptionIds.value }
    delete descriptions[team.teamId]
    expandedSavedTeamDescriptionIds.value = descriptions
    updateRecommendedTeamSavedState(team.teamId, false)
    notice.value = '저장을 취소했습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    savedTeamActionId.value = null
  }
}

function openSavedTeamDetail(team) {
  const teamId = positiveId(team?.teamId)
  if (!teamId) return
  const role = selectedSavedRole(team) || (team?.openRoles || [])[0]
  const query = matchingListQuery('', true, false)
  if (role?.slotId) query.slotId = String(role.slotId)
  router.push({
    name: 'matching-teams-detail',
    params: { teamId },
    query
  })
}

function formatSavedAt(value) {
  if (!value) return '저장 시각 정보 없음'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

async function request(item) {
  notice.value = ''
  error.value = ''
  try {
    if (mode.value === 'teamToMembers') {
      if (isCandidateInvited(item)) {
        await cancelCandidateInvitation(item)
        return
      }
      const targetUserId = candidateUserId(item)
      const slot = recruitments.value.flatMap((recruitment) => recruitment.slots || []).find((candidate) => candidate.slotId === selectedSlotId.value)
      if (!slot?.recruitmentId || !targetUserId) throw new Error('선택한 모집 역할 정보를 확인할 수 없습니다.')
      setLoadingFlag(candidateInviteLoadingIds, targetUserId, true)
      try {
        const result = await slateApi.invite({
          teamId: Number(selectedTeamId.value),
          recruitmentId: Number(slot.recruitmentId),
          slotId: Number(selectedSlotId.value),
          targetUserId,
          message: '함께 작업해보고 싶습니다.'
        })
        setCandidateInvitationCreated(targetUserId, result?.invitationId)
      } catch (err) {
        if (isDuplicateInvitationError(err)) {
          error.value = '이미 대기 중인 초대가 있습니다.'
          return
        }
        throw err
      } finally {
        setLoadingFlag(candidateInviteLoadingIds, targetUserId, false)
      }
    } else {
      if (isTeamApplied(item)) {
        await cancelTeamApplication(item)
        return
      }
      const key = teamApplicationKey(item)
      setLoadingFlag(teamApplicationLoadingIds, key, true)
      try {
        const result = await slateApi.apply({
        teamId: item.teamId,
        recruitmentId: item.recruitmentId,
        slotId: item.slotId,
        message: '팀에 지원하고 싶습니다.'
        })
        updateTeamApplicationState(item.teamId, item.slotId, true, result?.applicationId)
      } finally {
        setLoadingFlag(teamApplicationLoadingIds, key, false)
      }
    }
  } catch (err) {
    error.value = err.message
  }
}

function roleText(item) {
  if (item.roles) return item.roles.map((role) => role.roleName).join(', ')
  return item.roleName || '역할 정보 없음'
}

function genreText(item) {
  const genres = item.genres || item.teamGenres || []
  return genres.map((genre) => genre.name).join(', ')
}

function imageFor(item, index = 0) {
  return item?.thumbnailUrl
    || item?.profileImageUrl
    || item?.imageUrl
    || item?.teamImageUrl
    || fallbackImageFor(item)
}

function fallbackImageFor(item) {
  return item?.profileId || item?.displayName ? defaultProfileImage : defaultTeamImage
}

function useDefaultImage(event, fallback) {
  event.currentTarget.onerror = null
  event.currentTarget.src = fallback
}

function reasonsFor(item) {
  return item.reasons?.length ? item.reasons.slice(0, 4) : []
}

function codeRows(rows) {
  if (Array.isArray(rows)) return rows
  if (Array.isArray(rows?.value)) return rows.value
  return []
}

function codeLabel(rows, code, fallback = '정보 없음') {
  const value = code == null ? '' : String(code)
  const item = codeRows(rows).find((candidate) => (
    String(candidate.code ?? candidate.regionId ?? candidate.genreId ?? candidate.roleId) === value
  ))
  return displayCodeName(item) || fallback
}

function scoreAvailable(item) {
  return item?.score !== null && item?.score !== undefined && Number.isFinite(Number(item.score))
}

function scoreText(item) {
  return scoreAvailable(item) ? `${Math.round(Number(item.score))}%` : '적합도를 계산할 수 없습니다.'
}

function scoreCaption(item) {
  return scoreAvailable(item) ? '적합도' : ''
}

function scoreBadgeText(item) {
  return scoreAvailable(item) ? item.scoreBadge || '적합도 계산됨' : ''
}

function strengthsFor(item) {
  return item.strengths?.length ? item.strengths : []
}

function conditionsFor(item) {
  return (item.collaborationConditions || []).map((condition) => condition.displayName || condition.conditionCode)
}

function portfolioItemsFor(item) {
  return Array.isArray(item?.portfolioItems) ? item.portfolioItems : []
}

function firstPortfolioText(value) {
  if (Array.isArray(value)) return value.map((item) => String(item || '').trim()).find(Boolean) || ''
  return String(value || '').split(',').map((item) => item.trim()).find(Boolean) || ''
}

function portfolioThumbnail(item) {
  return item?.uploadedThumbnailUrl || item?.thumbnailUrl || defaultPortfolioImage
}

function portfolioTypeLabel(item) {
  const sourceType = String(item?.sourceType || '').toUpperCase()
  const sourceLabels = {
    KOBIS: '영화',
    YOUTUBE: '영상',
    PUBLIC_DATA: '공공데이터',
    DIRECT: '직접 등록',
    UPLOAD: '직접 등록'
  }
  return firstPortfolioText(item?.externalSourceName)
    || firstPortfolioText(item?.providerGenres)
    || firstPortfolioText(item?.matchedRoleGroup)
    || firstPortfolioText(item?.roleName)
    || sourceLabels[sourceType]
    || '포트폴리오'
}

function portfolioSummariesFor(item) {
  const groups = new Map()
  portfolioItemsFor(item).forEach((portfolio) => {
    const label = portfolioTypeLabel(portfolio)
    const group = groups.get(label) || { label, count: 0, titles: [], thumbnail: '' }
    group.count += 1
    if (portfolio.title && group.titles.length < 2) group.titles.push(portfolio.title)
    if (!group.thumbnail) group.thumbnail = portfolioThumbnail(portfolio)
    groups.set(label, group)
  })
  return [...groups.values()]
    .sort((left, right) => right.count - left.count || left.label.localeCompare(right.label, 'ko-KR'))
    .map((group) => ({
      ...group,
      meta: group.titles.length ? group.titles.join(', ') : '등록된 포트폴리오'
    }))
}

function teamRegionText(item) {
  return item?.regionAnyYn === 'Y' ? '지역 무관' : item?.publicRegionName || '지역 정보 없음'
}

function teamStatusLabel(status) {
  return {
    RECRUITING: '모집 중',
    IN_PROGRESS: '진행 중',
    RECRUITMENT_CLOSED: '모집 종료',
    ENDED: '종료',
    DELETED: '삭제됨'
  }[status] || status || '상태 정보 없음'
}

function requestStatusLabel(status) {
  return {
    PENDING: '대기 중',
    ACCEPTED: '수락됨',
    REJECTED: '거절됨',
    CANCELED: '취소됨',
    EXPIRED: '만료됨'
  }[status] || status || '상태 정보 없음'
}

function normalizedRequestStatus(status) {
  return String(status || '').toUpperCase()
}

function requestStatusFilterOptions(rows = []) {
  const counts = new Map()
  ;(rows || []).forEach((row) => {
    const status = normalizedRequestStatus(row?.status)
    if (status) counts.set(status, (counts.get(status) || 0) + 1)
  })
  const knownValues = REQUEST_STATUS_OPTIONS.map((option) => option.value)
  const unknownOptions = [...counts.keys()]
    .filter((status) => !knownValues.includes(status))
    .sort((left, right) => left.localeCompare(right))
    .map((status) => ({ value: status, label: requestStatusLabel(status) }))
  return [...REQUEST_STATUS_OPTIONS, ...unknownOptions].map((option) => ({
    ...option,
    count: counts.get(option.value) || 0
  }))
}

function filterRowsByStatus(rows = [], selectedStatuses = []) {
  const selectedSet = new Set((selectedStatuses || []).map(normalizedRequestStatus))
  if (!selectedSet.size) return []
  return (rows || []).filter((row) => selectedSet.has(normalizedRequestStatus(row?.status)))
}

function collaborationConditionText(item) {
  return codeLabel(collaborationConditions.value, item?.collaborationCondition)
}

function formatDate(value) {
  if (!value) return '정보 없음'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(date)
}

function setSelected(item) {
  const id = candidateId(item)
  if (!id) return
  router.push({
    name: mode.value === 'teamToMembers' ? 'matching-members-detail' : 'matching-teams-detail',
    params: mode.value === 'teamToMembers' ? { userId: id } : { teamId: id },
    query: mode.value === 'memberToTeams'
      ? { ...matchingListQuery(), slotId: String(item.slotId) }
      : matchingListQuery()
  })
}

watch(
  [
    () => selectedGenreIds.value.join(','),
    () => selectedRoleIds.value.join(','),
    () => selectedTopRegions.value.join(','),
    () => selectedRegionIds.value.join(','),
    () => selectedExperienceLevels.value.join(','),
    () => selectedJoinAvailabilities.value.join(','),
    () => selectedCollaborationConditions.value.join(',')
  ],
  () => {
    if (mode.value !== 'memberToTeams'
        || !hasAppliedTeamFilters.value
        || !appliedTeamFilterSignature.value
        || filterSignature() === appliedTeamFilterSignature.value) return
    teamFiltersNeedApply.value = true
    loading.value = false
    error.value = ''
    notice.value = ''
    resetAiRecommendations()
  }
)

watch(
  [() => resultCount.value, () => totalResultPages.value],
  clampResultPage
)

watch(
  [
    () => props.currentUser?.userId,
    () => route.name,
    () => route.params.userId,
    () => route.params.teamId,
    () => route.query.mode,
    () => route.query.view,
    () => route.query.applied,
    () => route.query.teamId,
    () => route.query.slotId,
    () => route.query.genreIds,
    () => route.query.roleIds,
    () => route.query.regionSidos,
    () => route.query.regionIds,
    () => route.query.experienceLevel,
    () => route.query.joinAvailability,
    () => route.query.collaborationCondition
  ],
  () => {
    const signature = JSON.stringify(normalizedQueryEntries(route.query))
    if (internalQueryReplacements.has(signature)) {
      internalQueryReplacements.delete(signature)
      return
    }
    const dataSignature = matchingDataRouteSignature()
    if (loadedRouteDataSignature === dataSignature && (isCandidateListRoute.value || isCandidateDetailRoute.value)) {
      mode.value = routeMode()
      selectRouteCandidate()
      return
    }
    loadedRouteDataSignature = dataSignature
    load()
  },
  { immediate: true }
)

watch(
  [() => selectedCard.value?.profileId, () => selectedCard.value?.sample, () => mode.value, () => route.name],
  loadSelectedFollowStatus,
  { immediate: true }
)

onMounted(() => {
  componentUnmounted = false
  window.addEventListener('slate-auth-changed', load)
})
onBeforeUnmount(() => {
  loadRequestId += 1
  refreshRequestId += 1
  recruitmentRequestId += 1
  basisChangeRequestId += 1
  followStatusRequestId += 1
  followedMembersRequestId += 1
  aiRecommendationRequestId += 1
  savedTeamsRequestId += 1
  componentUnmounted = true
  window.removeEventListener('slate-auth-changed', load)
})
</script>

<template>
  <section v-if="!props.currentUser" class="login-panel">
    <h2>로그인이 필요합니다</h2>
    <p>매칭 추천은 프로필과 팀 정보를 기준으로 계산됩니다.</p>
    <RouterLink class="primary-button inline" :to="{ name: 'login', query: { redirect: route.fullPath } }">로그인</RouterLink>
  </section>

  <section v-else class="matching-board">
    <nav v-if="!isCandidateDetailRoute" class="matching-mode-tabs" aria-label="매칭 목적">
      <RouterLink :class="{ active: isMemberListRoute || isMemberDetailRoute }" :to="{ name: 'matching-members' }">
        팀원 찾기
      </RouterLink>
      <RouterLink :class="{ active: isTeamListRoute || isTeamDetailRoute }" :to="{ name: 'matching-teams' }">
        팀 찾기
      </RouterLink>
    </nav>

    <nav v-if="isTeamListRoute" class="matching-team-view-tabs" aria-label="팀 찾기 목록">
      <RouterLink :class="{ active: !isSavedTeamsView && !isAppliedTeamsView }" :to="{ name: 'matching-teams' }">추천 팀</RouterLink>
      <RouterLink :class="{ active: isSavedTeamsView }" :to="{ name: 'matching-teams', query: { view: 'saved' } }">저장한 팀</RouterLink>
      <RouterLink :class="{ active: isAppliedTeamsView }" :to="{ name: 'matching-teams', query: { view: 'applied' } }">지원한 팀</RouterLink>
    </nav>
    <nav v-if="isMemberListRoute" class="matching-team-view-tabs" aria-label="팀원 찾기 목록">
      <RouterLink :class="{ active: !isFollowedMembersView && !isInvitedMembersView }" :to="{ name: 'matching-members', query: matchingListQuery('') }">전체</RouterLink>
      <RouterLink :class="{ active: isFollowedMembersView }" :to="{ name: 'matching-members', query: matchingListQuery('following') }">팔로우</RouterLink>
      <RouterLink :class="{ active: isInvitedMembersView }" :to="{ name: 'matching-members', query: matchingListQuery('invited') }">초대한 팀원</RouterLink>
    </nav>

    <section
      v-if="isCandidateListRoute && !isActionListView"
      class="matching-filter-card"
      :class="{ 'team-search-filter': mode === 'memberToTeams' }"
      :aria-busy="teamsLoading || recruitmentsLoading || genreLoading || regionLoading || codeLoading"
    >
      <header v-if="mode === 'teamToMembers'" class="matching-filter-section-title">
        <strong>팀원 검색 정보</strong>
      </header>
      <div v-if="mode === 'teamToMembers'" class="matching-basis-field">
        <span>기준 팀</span>
        <select v-if="manageableTeams.length" v-model="selectedTeamId" aria-label="기준 팀 변경" :disabled="teamsLoading" @change="onTeamChange">
          <option :value="null">기준 팀을 선택해주세요</option>
          <option v-for="team in manageableTeams" :key="team.teamId" :value="team.teamId">{{ team.name }}</option>
        </select>
        <div v-else-if="teamsLoading" class="matching-basis-empty">관리 가능한 팀을 불러오는 중입니다.</div>
        <div v-else-if="teamsError" class="matching-basis-empty matching-state-error" role="alert">{{ teamsError }}</div>
        <div v-else class="matching-basis-empty matching-basis-inline">
          <p>팀을 만들고 작업을 함께 하세요.</p>
          <RouterLink :to="{ name: 'teams-new' }">팀 만들기</RouterLink>
        </div>
      </div>
      <div v-if="mode === 'teamToMembers'" class="matching-basis-field matching-hiring-status-field">
        <span>기준 팀 구인 현황</span>
        <div v-if="hasHiringSlots()" class="matching-hiring-grid" role="radiogroup" aria-label="기준 팀 구인 현황">
          <button
            v-for="slot in hiringSlots()"
            :key="slot.slotId"
            type="button"
            class="matching-hiring-row"
            :class="{ active: isSlotActive(slot) }"
            :aria-pressed="isSlotActive(slot)"
            @click="selectSlot(slot.slotId)"
          >
            <strong>{{ slot.roleName || '모집 역할' }}</strong>
            <span>{{ slotScheduleText(slot) }}</span>
            <small>잔여 {{ slot.remainingCount ?? 0 }}명</small>
          </button>
        </div>
        <div v-else-if="isRecruitmentLoading()" class="matching-basis-empty">구인 현황을 불러오는 중입니다.</div>
        <div v-else-if="recruitmentErrorText()" class="matching-basis-empty matching-state-error" role="alert">{{ recruitmentErrorText() }}</div>
        <div v-else class="matching-basis-empty">
          <p>{{ selectedTeamIdValue() ? '현재 모집 중인 역할이 없습니다.' : '팀을 선택하고 구인 현황을 확인하세요.' }}</p>
          <RouterLink v-if="selectedTeamIdValue()" :to="{ name: 'teams-recruitments', params: { teamId: selectedTeamIdValue() } }">팀 모집 관리</RouterLink>
        </div>
      </div>
      <header v-if="mode === 'memberToTeams'" class="matching-filter-section-title">
        <strong>팀 검색 정보</strong>
      </header>
      <div v-if="mode === 'memberToTeams'" class="matching-basis-value matching-profile-basis">
        <strong>{{ profile?.displayName || currentProfileName }}</strong>
        <small>{{ currentProfileRole }}</small>
        <small>{{ profile?.publicRegionName || '공개 지역 정보 없음' }}</small>
      </div>
      <header class="matching-filter-section-title additional">
        <strong>추가 필터</strong>
        <small>{{ isFollowedMembersView ? '팔로우한 회원 목록 안에서 조건을 좁힙니다.' : '선택한 값은 일반 매칭 결과에만 적용됩니다.' }}</small>
      </header>
      <div class="matching-filter-table">
        <div class="matching-filter-row matching-filter-row-split">
          <div class="matching-filter-cell matching-region-field">
            <div class="matching-region-columns">
              <div class="matching-region-selector">
                <div class="matching-region-subhead">
                  <span>지역</span>
                  <small>{{ topRegionSummary }}</small>
                </div>
                <div class="matching-region-combobox">
                  <input
                    v-model="topRegionKeyword"
                    type="search"
                    placeholder="지역 선택"
                    aria-label="지역 검색어"
                    autocomplete="off"
                    :disabled="regionLoading || Boolean(regionError) || !topRegionOptions.length"
                    @focus="topRegionDropdownOpen = true"
                    @input="topRegionDropdownOpen = true"
                    @blur="topRegionDropdownOpen = false"
                    @keydown.escape="topRegionDropdownOpen = false"
                  >
                  <div
                    v-if="topRegionDropdownOpen"
                    class="matching-region-dropdown"
                    role="listbox"
                    aria-label="상위 지역 검색 결과"
                  >
                    <button
                      type="button"
                      class="matching-region-option"
                      :class="{ active: !selectedTopRegions.length && !selectedRegionIds.length }"
                      role="option"
                      :aria-selected="!selectedTopRegions.length && !selectedRegionIds.length"
                      :disabled="regionLoading || Boolean(regionError)"
                      @mousedown.prevent="selectTopRegion('')"
                    >
                      전체 지역
                    </button>
                    <button
                      v-for="option in filteredTopRegionOptions"
                      :key="option"
                      type="button"
                      class="matching-region-option"
                      :class="{ active: selectedTopRegionSet.has(option) }"
                      role="option"
                      :aria-selected="selectedTopRegionSet.has(option)"
                      :disabled="regionLoading || Boolean(regionError)"
                      @mousedown.prevent="selectTopRegion(option)"
                    >
                      {{ option }}
                    </button>
                    <p v-if="!filteredTopRegionOptions.length" class="matching-filter-empty">입력한 텍스트를 포함하는 상위 지역이 없습니다.</p>
                  </div>
                </div>
              </div>

              <div class="matching-region-selector">
                <div class="matching-region-subhead">
                  <span>세부 입력</span>
                  <small>{{ detailRegionSummary }}</small>
                </div>
                <div class="matching-region-combobox">
                  <input
                    v-model="regionKeyword"
                    type="search"
                    placeholder="시·군·구 입력"
                    aria-label="지역 검색어"
                    autocomplete="off"
                    :disabled="regionLoading || Boolean(regionError) || !regions.length"
                    @focus="regionDropdownOpen = true"
                    @input="regionDropdownOpen = true"
                    @blur="regionDropdownOpen = false"
                    @keydown.escape="regionDropdownOpen = false"
                  >
                  <div
                    v-if="regionDropdownOpen"
                    class="matching-region-dropdown"
                    role="listbox"
                    aria-label="지역 검색 결과"
                  >
                    <button
                      v-for="region in visibleRegionOptions"
                      :key="region.regionId"
                      type="button"
                      class="matching-region-option"
                      :class="{ active: selectedRegionIdSet.has(Number(region.regionId)) }"
                      role="option"
                      :aria-selected="selectedRegionIdSet.has(Number(region.regionId))"
                      :disabled="regionLoading || Boolean(regionError)"
                      @mousedown.prevent="selectRegion(region)"
                    >
                      {{ region.publicDisplayName }}
                    </button>
                    <p v-if="!visibleRegionOptions.length" class="matching-filter-empty">입력한 텍스트를 포함하는 지역이 없습니다.</p>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="selectedRegionChips.length" class="matching-selected-inline matching-region-selected-filters" aria-label="선택한 지역">
              <button
                v-for="chip in selectedRegionChips"
                :key="chip.key"
                class="matching-selected-filter"
                :class="`filter-${chip.group}`"
                type="button"
                :aria-label="`${chip.label} 필터 제거`"
                @click="removeFilterChip(chip)"
              >
                <span>{{ chip.label }}</span>
                <b aria-hidden="true">×</b>
              </button>
            </div>
            <small v-if="regionError" class="matching-reference-error">{{ regionError }}</small>
            <small v-else-if="!regionLoading && !regions.length" class="matching-reference-empty">검색된 지역이 없습니다.</small>
            <small v-else-if="regionKeyword.trim() && filteredRegions.length > visibleRegionOptions.length" class="matching-reference-empty">더 구체적인 지역명을 입력하면 목록이 좁혀집니다.</small>
          </div>
          <div class="matching-filter-cell">
            <span>{{ mode === 'teamToMembers' ? '경력' : '요구 경력' }}</span>
            <div class="matching-checkbox-grid" aria-label="경력 선택">
              <label
                v-for="level in experienceLevels"
                :key="level.code"
                class="matching-checkbox-option"
                :class="{ active: selectedExperienceLevels.includes(level.code) }"
              >
                <input
                  type="checkbox"
                  :checked="selectedExperienceLevels.includes(level.code)"
                  :disabled="codeLoading || Boolean(codeError)"
                  @change="toggleFilterSelection(selectedExperienceLevels, level.code)"
                >
                <span>{{ displayCodeName(level) }}</span>
              </label>
            </div>
            <small v-if="codeError" class="matching-reference-error" role="alert">{{ codeError }}</small>
            <small v-else-if="!codeLoading && !experienceLevels.length" class="matching-reference-empty">활성 경력 조건이 없습니다.</small>
          </div>
        </div>
        <div class="matching-filter-row">
          <strong>장르</strong>
          <div class="matching-filter-cell">
            <div class="matching-checkbox-grid" aria-label="장르 선택">
              <label
                v-for="genre in filteredGenres"
                :key="genre.genreId"
                class="matching-checkbox-option"
                :class="{ active: selectedGenreIds.includes(Number(genre.genreId)) }"
              >
                <input
                  type="checkbox"
                  :checked="selectedGenreIds.includes(Number(genre.genreId))"
                  :disabled="genreLoading || Boolean(genreError)"
                  @change="toggleFilterSelection(selectedGenreIds, Number(genre.genreId))"
                >
                <span>{{ genre.name }}</span>
              </label>
            </div>
            <small v-if="genreError" class="matching-reference-error">{{ genreError }}</small>
            <small v-else-if="!genreLoading && !genres.length" class="matching-reference-empty">활성 장르가 없습니다.</small>
            <small v-else-if="!genreLoading && !filteredGenres.length" class="matching-reference-empty">검색된 장르가 없습니다.</small>
          </div>
        </div>
        <div v-if="mode === 'teamToMembers'" class="matching-filter-row">
          <strong>작업 일정</strong>
          <div class="matching-filter-cell">
            <div class="matching-checkbox-grid" aria-label="작업 일정 선택">
              <label
                v-for="availability in joinAvailabilities"
                :key="availability.code"
                class="matching-checkbox-option"
                :class="{ active: selectedJoinAvailabilities.includes(availability.code) }"
              >
                <input
                  type="checkbox"
                  :checked="selectedJoinAvailabilities.includes(availability.code)"
                  :disabled="codeLoading || Boolean(codeError)"
                  @change="toggleFilterSelection(selectedJoinAvailabilities, availability.code)"
                >
                <span>{{ displayCodeName(availability) }}</span>
              </label>
            </div>
            <small v-if="!codeLoading && !joinAvailabilities.length" class="matching-reference-empty">활성 작업 일정이 없습니다.</small>
          </div>
        </div>
        <div class="matching-filter-row">
          <strong>협업 조건</strong>
          <div class="matching-filter-cell">
            <div class="matching-checkbox-grid" aria-label="협업 조건 선택">
              <label
                v-for="condition in collaborationConditions"
                :key="condition.code"
                class="matching-checkbox-option"
                :class="{ active: selectedCollaborationConditions.includes(condition.code) }"
              >
                <input
                  type="checkbox"
                  :checked="selectedCollaborationConditions.includes(condition.code)"
                  :disabled="codeLoading || Boolean(collaborationConditionError)"
                  @change="toggleFilterSelection(selectedCollaborationConditions, condition.code)"
                >
                <span>{{ displayCodeName(condition) }}</span>
              </label>
            </div>
            <small v-if="collaborationConditionError" class="matching-reference-error">{{ collaborationConditionError }}</small>
            <small v-else-if="!codeLoading && !collaborationConditions.length" class="matching-reference-empty">활성 협업 조건이 없습니다.</small>
          </div>
        </div>
        <div class="matching-filter-row">
          <strong>모집 역할</strong>
          <div class="matching-filter-cell">
            <div class="matching-checkbox-grid" aria-label="모집 역할 선택">
              <label
                v-for="role in roleFilterOptions()"
                :key="role.roleId"
                class="matching-checkbox-option"
                :class="{ active: isRoleFilterSelected(role.roleId) }"
              >
                <input
                  type="checkbox"
                  :checked="isRoleFilterSelected(role.roleId)"
                  @change="toggleFilterSelection(selectedRoleIds, Number(role.roleId))"
                >
                <span>{{ role.label }}</span>
              </label>
            </div>
            <small v-if="mode === 'teamToMembers' && !hasRoleFilterOptions()" class="matching-reference-empty">선택 가능한 모집 역할이 없습니다.</small>
            <small v-else-if="mode === 'memberToTeams' && !hasRoleFilterOptions()" class="matching-reference-empty">프로필에 등록된 모집 역할이 없습니다.</small>
          </div>
        </div>
      </div>
      <div class="matching-filter-actions">
        <button class="matching-filter-button reset" type="button" :disabled="isMatchingLoading()" @click="resetFilters">초기화</button>
        <button class="matching-filter-button" type="button" :disabled="isMatchingLoading()" @click="applyFilters">
          <span aria-hidden="true">≡</span>
          {{ isMatchingLoading() ? '검색 중' : mode === 'teamToMembers' ? '팀원 검색' : '팀 검색' }}
        </button>
      </div>
      <div v-if="selectedFilterChips.length" class="matching-selected-filters" aria-label="선택한 필터">
        <button
          v-for="chip in selectedFilterChips"
          :key="`${chip.group}-${chip.value}`"
          class="matching-selected-filter"
          :class="`filter-${chip.group}`"
          type="button"
          :aria-label="`${chip.label} 필터 제거`"
          @click="removeFilterChip(chip)"
        >
          <span>{{ chip.label }}</span>
          <b aria-hidden="true">×</b>
        </button>
      </div>
    </section>

    <p v-if="notice" class="notice-text">{{ notice }}</p>
    <p v-if="error" class="error-text" role="alert">{{ error }}</p>
    <p v-if="followedMembersError" class="error-text" role="alert">{{ followedMembersError }}</p>
    <p v-if="sentInvitationsError" class="error-text" role="alert">{{ sentInvitationsError }}</p>
    <p v-if="sentApplicationsError" class="error-text" role="alert">{{ sentApplicationsError }}</p>
    <p v-if="loading && !isActionListView && !isFollowedMembersContext" class="muted" role="status">매칭 결과를 불러오는 중입니다.</p>
    <p v-if="followedMembersLoading" class="muted" role="status">팔로우한 회원을 불러오는 중입니다.</p>

    <section v-if="isSavedTeamsView" class="matching-saved-teams" :aria-busy="savedTeamsLoading">
      <header class="matching-section-head">
        <div>
          <h2>저장한 팀</h2>
          <p>관심 있는 팀의 현재 모집 상태를 확인하고 역할을 선택해 지원할 수 있어요.</p>
        </div>
      </header>
      <p v-if="savedTeamsError" class="error-text" role="alert">{{ savedTeamsError }}</p>
      <p v-else-if="savedTeamsLoading" class="muted" role="status">저장한 팀을 불러오는 중입니다.</p>
      <div v-else-if="savedTeams.length" class="matching-saved-team-list">
        <article v-for="team in savedTeams" :key="team.bookmarkId" class="matching-saved-team-card">
          <header>
            <div>
              <span>{{ formatSavedAt(team.savedAt) }} 저장</span>
              <h3>{{ team.teamName }}</h3>
              <small>{{ teamStatusLabel(team.teamStatus) }} · {{ teamRegionText(team) }}</small>
            </div>
            <em :class="{ closed: !['RECRUITING', 'IN_PROGRESS'].includes(team.teamStatus) }">{{ teamStatusLabel(team.teamStatus) }}</em>
          </header>
          <div class="matching-saved-team-description">
            <p>
              <span>{{ savedTeamDescriptionPreview(team) }}</span>
              <button
                v-if="isSavedTeamDescriptionLong(team)"
                type="button"
                @click="toggleSavedTeamDescription(team)"
              >
                {{ isSavedTeamDescriptionExpanded(team) ? '접기' : '더 보기' }}
              </button>
            </p>
          </div>
          <div class="matching-chip-row">
            <span v-for="genre in team.genres || []" :key="genre.genreId">{{ genre.name }}</span>
            <span v-if="!team.genres?.length">등록된 장르 없음</span>
          </div>
          <label class="matching-saved-role-field">
            <span>지원 역할</span>
            <select
              v-model="savedTeamRoleSelections[team.teamId]"
              :disabled="!['RECRUITING', 'IN_PROGRESS'].includes(team.teamStatus) || !team.openRoles?.length"
            >
              <option value="">지원할 역할을 선택해주세요</option>
              <option v-for="role in team.openRoles || []" :key="role.slotId" :value="role.slotId">
                {{ role.roleName }} · {{ role.recruitmentTitle }} · 잔여 {{ role.remainingCount }}명
              </option>
            </select>
            <small v-if="savedTeamApplyReason(team)">{{ savedTeamApplyReason(team) }}</small>
          </label>
          <footer>
            <div class="matching-saved-team-footer-left">
              <button
                class="ghost-button danger"
                type="button"
                :disabled="Number(savedTeamActionId) === Number(team.teamId)"
                @click="removeSavedTeam(team)"
              >저장 취소</button>
            </div>
            <div class="matching-saved-team-footer-right">
              <button class="matching-more-button" type="button" @click="openSavedTeamDetail(team)">
                팀 정보 보기
              </button>
              <button
                class="primary-button matching-action-apply"
                :class="{ invited: isTeamApplied(savedTeamSelectedRoleAction(team)) }"
                type="button"
                :disabled="Boolean(savedTeamApplyReason(team)) || Number(savedTeamActionId) === Number(team.teamId) || isTeamApplicationLoading(savedTeamSelectedRoleAction(team))"
                @click="applySavedTeam(team)"
              >{{ savedTeamApplyText(team) }}</button>
            </div>
          </footer>
        </article>
      </div>
      <div v-else class="matching-results-empty">
        <strong>저장한 팀이 없습니다.</strong>
        <p>추천 팀에서 관심 있는 팀을 저장하면 이곳에서 다시 확인할 수 있어요.</p>
        <RouterLink class="ghost-button" :to="{ name: 'matching-teams' }">추천 팀 보기</RouterLink>
      </div>
    </section>

    <section v-if="isInvitedMembersView" class="matching-action-list-section" :aria-busy="sentInvitationsLoading">
      <header class="matching-section-head">
        <div>
          <h2>초대한 팀원</h2>
          <p>내가 보낸 팀원 초대와 응답 상태를 확인합니다.</p>
        </div>
        <fieldset class="matching-status-filter" aria-label="초대한 팀원 상태 필터">
          <label v-for="option in sentInvitationStatusOptions" :key="`invitation-status-${option.value}`">
            <input v-model="sentInvitationStatusFilters" type="checkbox" :value="option.value">
            <span>{{ option.label }}</span>
            <b>{{ option.count }}</b>
          </label>
        </fieldset>
      </header>
      <p v-if="sentInvitationsLoading" class="muted" role="status">초대한 팀원을 불러오는 중입니다.</p>
      <div v-else-if="filteredSentInvitations.length" class="matching-action-list">
        <article
          v-for="item in filteredSentInvitations"
          :key="item.invitationId"
          class="matching-action-list-card"
          role="button"
          tabindex="0"
          @click="openSentInvitationDetail(item)"
          @keydown.enter="openSentInvitationDetail(item)"
          @keydown.space.prevent="openSentInvitationDetail(item)"
        >
          <img :src="imageFor(item)" alt="" class="matching-person-photo" @error="useDefaultImage($event, defaultProfileImage)">
          <div class="matching-action-list-main">
            <strong>{{ item.displayName }}</strong>
            <small>{{ item.roleName }} · {{ item.teamName }}</small>
            <p>{{ item.publicRegionName }} · {{ item.recruitmentTitle || '모집 공고 정보 없음' }}</p>
            <span>{{ formatSavedAt(item.createdAt) }} 초대 · {{ requestStatusLabel(item.status) }}</span>
          </div>
          <button
            v-if="item.status === 'PENDING'"
            class="matching-action-invite invited"
            type="button"
            :disabled="sentInvitationActionId === item.invitationId"
            @click.stop="cancelSentInvitation(item)"
          >초대됨</button>
        </article>
      </div>
      <div v-else-if="sentInvitations.length" class="matching-results-empty">
        <strong>선택한 상태의 초대한 팀원이 없습니다.</strong>
        <p>우측 상단 상태 체크박스를 조정해 목록을 확인할 수 있습니다.</p>
      </div>
      <div v-else class="matching-results-empty">
        <strong>초대한 팀원이 없습니다.</strong>
        <p>추천 팀원에서 초대하면 이곳에서 상태를 확인할 수 있습니다.</p>
      </div>
    </section>

    <section v-if="isAppliedTeamsView" class="matching-action-list-section" :aria-busy="sentApplicationsLoading">
      <header class="matching-section-head">
        <div>
          <h2>지원한 팀</h2>
          <p>내가 지원한 팀과 응답 상태를 확인합니다.</p>
        </div>
        <fieldset class="matching-status-filter" aria-label="지원한 팀 상태 필터">
          <label v-for="option in sentApplicationStatusOptions" :key="`application-status-${option.value}`">
            <input v-model="sentApplicationStatusFilters" type="checkbox" :value="option.value">
            <span>{{ option.label }}</span>
            <b>{{ option.count }}</b>
          </label>
        </fieldset>
      </header>
      <p v-if="sentApplicationsLoading" class="muted" role="status">지원한 팀을 불러오는 중입니다.</p>
      <div v-else-if="filteredSentApplications.length" class="matching-action-list">
        <article
          v-for="item in filteredSentApplications"
          :key="item.applicationId"
          class="matching-action-list-card"
          role="button"
          tabindex="0"
          @click="openSentApplicationDetail(item)"
          @keydown.enter="openSentApplicationDetail(item)"
          @keydown.space.prevent="openSentApplicationDetail(item)"
        >
          <img :src="imageFor(item)" alt="" class="matching-person-photo" @error="useDefaultImage($event, defaultTeamImage)">
          <div class="matching-action-list-main">
            <strong>{{ item.teamName }}</strong>
            <small>{{ item.roleName }} · {{ item.recruitmentTitle || '모집 공고 정보 없음' }}</small>
            <p>{{ teamDescriptionPreview(item) }}</p>
            <span>{{ formatSavedAt(item.createdAt) }} 지원 · {{ requestStatusLabel(item.status) }}</span>
          </div>
          <button
            v-if="item.status === 'PENDING'"
            class="matching-action-invite invited"
            type="button"
            :disabled="sentApplicationActionId === item.applicationId"
            @click.stop="cancelSentApplication(item)"
          >지원됨</button>
        </article>
      </div>
      <div v-else-if="sentApplications.length" class="matching-results-empty">
        <strong>선택한 상태의 지원한 팀이 없습니다.</strong>
        <p>우측 상단 상태 체크박스를 조정해 목록을 확인할 수 있습니다.</p>
      </div>
      <div v-else class="matching-results-empty">
        <strong>지원한 팀이 없습니다.</strong>
        <p>추천 팀에서 지원하면 이곳에서 상태를 확인할 수 있습니다.</p>
      </div>
    </section>

    <section v-if="isAiView" class="ai-recommendation-panel" aria-labelledby="ai-recommendation-title" :aria-busy="aiRecommendationLoading">
      <header class="ai-recommendation-head">
        <div>
          <h2 id="ai-recommendation-title">{{ mode === 'teamToMembers' ? 'AI 팀원 추천' : 'AI 팀 추천' }}</h2>
          <p>선택한 팀 정보를 바탕으로 AI가 팀원을 추천해드립니다.</p>
          <small>{{ aiRecommendationContextText }}</small>
        </div>
        <div class="ai-recommendation-actions">
          <button
            class="ai-recommendation-button"
            type="button"
            :disabled="aiRecommendationLoading"
            @click="requestAiRecommendations"
          >
            {{ aiRecommendationButtonText }}
          </button>
        </div>
      </header>

      <div v-if="aiRecommendationLoading" class="ai-recommendation-state">
        추천 중입니다.
      </div>
      <div v-else-if="aiRecommendationError" class="ai-recommendation-state error">
        {{ aiRecommendationError }}
      </div>
      <div v-else-if="aiRecommendations.length" class="ai-recommendation-result">
        <article
          v-for="(item, index) in aiRecommendations"
          :key="`${item.targetType}-${item.targetId}`"
          class="ai-recommendation-card"
          :class="{ active: index === activeAiRecommendationIndex }"
        >
          <span>{{ aiRecommendationLabel }} {{ index + 1 }}</span>
          <strong>{{ item.targetName }}</strong>
          <p>{{ item.reason }}</p>
          <button type="button" @click="selectAiRecommendation(index); openAiRecommendation(item)">
            {{ aiRecommendationDetailButtonText }}
          </button>
        </article>
      </div>
      <div v-else-if="aiRecommendationStateText" class="ai-recommendation-state">
        {{ aiRecommendationStateText }}
      </div>
    </section>

    <section
      v-if="(isCandidateListRoute && !isActionListView) || isCandidateDetailRoute"
      class="matching-content-grid"
      :class="{ 'matching-route-detail-grid': isCandidateDetailRoute, 'matching-list-only-grid': isCandidateListRoute }"
    >
      <div v-if="isCandidateListRoute" class="matching-list-card">
        <header class="matching-section-head">
          <div>
            <h2>{{ mode === 'teamToMembers' ? '추천 팀원' : '추천 팀' }}</h2>
            <p>{{ mode === 'teamToMembers' ? '선택된 팀을 기준으로 팀원을 조회합니다.' : '내 프로필과 활동 조건에 가장 잘 맞는 모집 팀을 추천해요.' }}</p>
          </div>
          <div class="matching-section-actions">
            <button
              class="ai-recommendation-button"
              type="button"
              :disabled="aiRecommendationLoading || Boolean(aiRecommendationDisabledReason)"
              @click="requestAiRecommendations"
            >
              {{ aiRecommendationButtonText }}
            </button>
          </div>
        </header>

        <article
          v-for="(item, index) in paginatedListDisplayCards"
          :key="candidateKey(item, resultPageStart + index)"
          class="matching-person-card"
          :class="{ selected: selectedCard === item }"
        >
          <img :src="imageFor(item, resultPageStart + index)" alt="" class="matching-person-photo" @error="useDefaultImage($event, fallbackImageFor(item))">
          <div class="matching-person-main">
            <div class="matching-name-line">
              <strong>{{ item.displayName || item.teamName }}</strong>
            </div>
            <small>{{ mode === 'teamToMembers' ? roleText(item) : item.recruitmentTitle || '모집 공고 정보 없음' }}</small>
            <p class="matching-location">⌖ {{ mode === 'teamToMembers' ? item.publicRegionName || '공개 지역 없음' : teamRegionText(item) }}</p>
            <p v-if="mode === 'memberToTeams'" class="matching-team-description">{{ teamDescriptionPreview(item) }}</p>
            <div class="matching-chip-row">
              <span v-for="genre in genreText(item).split(',').map((text) => text.trim()).filter(Boolean)" :key="genre">
                {{ genre }}
              </span>
              <span v-if="!genreText(item)">등록된 장르 없음</span>
            </div>
            <div v-if="mode === 'teamToMembers'" class="matching-profile-facts">
              <span>경력: {{ codeLabel(experienceLevels, item.experienceLevel) }}</span>
              <span>합류 가능: {{ codeLabel(joinAvailabilities, item.joinAvailability) }}</span>
              <span>협업 조건: {{ conditionsFor(item).join(', ') || '정보 없음' }}</span>
            </div>
            <div v-else class="matching-profile-facts">
              <span>모집 역할: {{ item.roleName || '정보 없음' }}</span>
              <span>잔여 인원: {{ item.remainingCount ?? '정보 없음' }}명</span>
              <span>요구 경력: {{ codeLabel(experienceLevels, item.requiredExperienceLevel) }}</span>
              <span>협업 조건: {{ collaborationConditionText(item) }}</span>
            </div>
            <div class="matching-reason-line">
              <span v-for="reason in reasonsFor(item)" :key="reason">◎ {{ reason }}</span>
            </div>
          </div>
          <div class="matching-card-side">
            <div class="matching-card-score">
              <div class="matching-score-ring" :class="{ unavailable: !scoreAvailable(item), 'no-caption': !scoreCaption(item) }" :style="{ '--score': scoreAvailable(item) ? `${Math.round(Number(item.score))}%` : '0%' }">
                <strong>{{ scoreText(item) }}</strong>
                <small v-if="scoreCaption(item)">{{ scoreCaption(item) }}</small>
              </div>
              <em v-if="scoreBadgeText(item)" class="matching-score-badge">{{ scoreBadgeText(item) }}</em>
            </div>
            <div class="matching-card-actions">
              <button
                type="button"
                :aria-label="mode === 'teamToMembers' ? `${item.displayName} 프로필 보기` : `${item.teamName} ${item.roleName || '모집 역할'} 팀 정보 보기`"
                @click.stop="setSelected(item)"
              >{{ mode === 'teamToMembers' ? '프로필 보기' : '팀 정보 보기' }}</button>
              <button
                v-if="mode === 'teamToMembers'"
                class="matching-action-follow"
                :class="{ following: isCandidateFollowed(item) }"
                type="button"
                :disabled="isCandidateFollowLoading(item)"
                :aria-pressed="isCandidateFollowed(item)"
                @click.stop="toggleCandidateFollow(item)"
              >{{ candidateFollowText(item) }}</button>
              <button
                v-else
                class="matching-action-save"
                :class="{ saved: item.savedByCurrentUser }"
                type="button"
                :aria-pressed="Boolean(item.savedByCurrentUser)"
                @click.stop="save(item)"
              >
                {{ item.savedByCurrentUser ? '저장됨' : '저장' }}
              </button>
              <button
                type="button"
                class="matching-action-invite"
                :class="{ invited: mode === 'teamToMembers' ? isCandidateInvited(item) : isTeamApplied(item) }"
                :disabled="mode === 'teamToMembers' ? isCandidateInviteDisabled(item) : isTeamApplicationLoading(item)"
                :aria-pressed="mode === 'teamToMembers' ? isCandidateInvited(item) : isTeamApplied(item)"
                @click.stop="request(item)"
              >{{ mode === 'teamToMembers' ? candidateInviteText(item) : teamApplyText(item) }} ✈</button>
            </div>
          </div>
        </article>

        <nav v-if="hasResultPagination" class="matching-result-pagination" aria-label="추천 결과 페이지">
          <div>
            <button type="button" :disabled="normalizedResultPage === 1" @click="showFirstResultPage">처음</button>
            <button type="button" :disabled="normalizedResultPage === 1" @click="showPreviousResultPage">이전</button>
            <button
              v-for="page in visibleResultPages"
              :key="page"
              type="button"
              :class="{ active: page === normalizedResultPage }"
              :aria-current="page === normalizedResultPage ? 'page' : undefined"
              @click="goResultPage(page)"
            >
              {{ page }}
            </button>
            <button type="button" :disabled="normalizedResultPage === totalResultPages" @click="showNextResultPage">다음</button>
            <button type="button" :disabled="normalizedResultPage === totalResultPages" @click="showLastResultPage">마지막</button>
          </div>
        </nav>

        <div v-if="isFollowedMembersView && !followedMembersLoading && !listDisplayCards.length" class="matching-results-empty">
          <strong>{{ followedMembers.length ? '선택한 조건에 맞는 팔로우 회원이 없습니다.' : '팔로우한 회원이 없습니다.' }}</strong>
          <p>{{ followedMembers.length ? '필터를 줄이거나 전체 탭에서 더 넓게 찾아보세요.' : '팀원 정보에서 팔로우한 회원이 이곳에 표시됩니다.' }}</p>
          <button v-if="hasSelectedFilterChips" type="button" @click="resetFilters">초기화</button>
          <RouterLink v-else class="ghost-button" :to="{ name: 'matching-members', query: matchingListQuery('') }">전체 보기</RouterLink>
        </div>
        <div v-else-if="mode === 'teamToMembers' && !loading && !displayCards.length" class="matching-results-empty">
          <strong>선택한 조건에 맞는 팀원 후보가 없습니다.</strong>
          <p>필터를 초기화하거나 후보 프로필의 공개 범위를 확인해주세요.</p>
          <button type="button" @click="resetFilters">초기화</button>
        </div>
        <div v-else-if="mode === 'memberToTeams' && !loading && teamFiltersDirty" class="matching-results-empty matching-results-unapplied">
          <strong>변경한 필터를 다시 적용해주세요.</strong>
          <p>기존 결과를 유지하고 있습니다. 팀 검색을 누르면 변경한 조건으로 다시 조회합니다.</p>
        </div>
        <div v-else-if="mode === 'memberToTeams' && !loading && !error && !listDisplayCards.length" class="matching-results-empty">
          <strong>선택한 조건에 맞는 팀이 없습니다.</strong>
          <p>필터를 초기화하거나 모집 중인 팀의 조건을 다시 확인해주세요.</p>
          <button type="button" @click="resetFilters">초기화</button>
        </div>
      </div>

      <aside v-if="isCandidateDetailRoute && selectedCard" class="matching-detail-card matching-route-detail-card">
        <div class="matching-route-detail-nav">
          <button class="ghost-button" type="button" @click="returnToMatchingList">목록</button>
        </div>
        <div class="matching-detail-top">
          <img :src="imageFor(selectedCard, displayCards.indexOf(selectedCard))" alt="" class="matching-detail-photo" @error="useDefaultImage($event, fallbackImageFor(selectedCard))">
          <div class="matching-detail-summary">
            <div class="matching-name-line detail">
              <strong>{{ selectedCard.displayName || selectedCard.teamName }}</strong>
            </div>
            <small>{{ mode === 'teamToMembers' ? roleText(selectedCard) : selectedCard.recruitmentTitle || '모집 공고 정보 없음' }}</small>
            <p class="matching-location">⌖ {{ mode === 'teamToMembers' ? selectedCard.publicRegionName || '지역 정보 없음' : teamRegionText(selectedCard) }}</p>
            <span>주요 장르</span>
            <div class="matching-chip-row">
              <span v-for="genre in genreText(selectedCard).split(',').map((text) => text.trim()).filter(Boolean)" :key="genre">
                {{ genre }}
              </span>
              <span v-if="!genreText(selectedCard)">등록된 장르 없음</span>
            </div>
            <p>{{ mode === 'teamToMembers' ? selectedCard.shortIntro || '프로필 소개 정보 없음' : selectedCard.teamDescription || '팀 설명 정보 없음' }}</p>
          </div>
          <div class="matching-detail-score">
            <div class="matching-score-ring large" :class="{ unavailable: !scoreAvailable(selectedCard), 'no-caption': !scoreCaption(selectedCard) }" :style="{ '--score': scoreAvailable(selectedCard) ? `${Math.round(Number(selectedCard.score))}%` : '0%' }">
              <strong>{{ scoreText(selectedCard) }}</strong>
              <small v-if="scoreCaption(selectedCard)">{{ scoreCaption(selectedCard) }}</small>
            </div>
            <em v-if="scoreBadgeText(selectedCard)">{{ scoreBadgeText(selectedCard) }}</em>
          </div>
        </div>

        <section v-if="mode === 'teamToMembers'" class="matching-detail-section">
          <h3>후보 조건</h3>
          <div class="matching-profile-facts detail">
            <span>지역: {{ selectedCard.publicRegionName || '정보 없음' }}</span>
            <span>경력: {{ codeLabel(experienceLevels, selectedCard.experienceLevel) }}</span>
            <span>합류 가능: {{ codeLabel(joinAvailabilities, selectedCard.joinAvailability) }}</span>
            <span>협업 조건: {{ conditionsFor(selectedCard).join(', ') || '정보 없음' }}</span>
          </div>
        </section>
        <section v-else class="matching-detail-section">
          <h3>팀·모집 정보</h3>
          <div class="matching-profile-facts detail">
            <span>모집 공고: {{ selectedCard.recruitmentTitle || '정보 없음' }}</span>
            <span>모집 역할: {{ selectedCard.roleName || '정보 없음' }}</span>
            <span>잔여 인원: {{ selectedCard.remainingCount ?? '정보 없음' }}명</span>
            <span>요구 경력: {{ codeLabel(experienceLevels, selectedCard.requiredExperienceLevel) }}</span>
            <span>협업 조건: {{ collaborationConditionText(selectedCard) }}</span>
            <span>역할 기간: {{ codeLabel(durations, selectedCard.roleDuration || selectedCard.expectedDuration) }}</span>
            <span>작업 시작일: {{ formatDate(selectedCard.workStartAt) }}</span>
            <span>모집 마감일: {{ formatDate(selectedCard.deadlineAt) }}</span>
          </div>
        </section>

        <section class="matching-detail-section">
          <h3>추천 이유</h3>
          <div class="matching-pill-row">
            <span v-for="reason in reasonsFor(selectedCard)" :key="reason">◎ {{ reason }}</span>
          </div>
        </section>

        <section v-if="mode === 'teamToMembers'" class="matching-detail-section">
          <h3>강점</h3>
          <div class="matching-strength-grid">
            <article v-for="strength in strengthsFor(selectedCard)" :key="strength">
              <span aria-hidden="true">▧</span>
              <strong>{{ strength }}</strong>
              <small>{{ strength === '감성적 화면 연출' ? '섬세한 구도와 감정' : strength === '야간 촬영 경험' ? '저조도 환경 최적화' : '원활한 커뮤니케이션' }}</small>
            </article>
          </div>
        </section>

        <section v-if="mode === 'teamToMembers'" class="matching-detail-section">
          <h3>협업 조건</h3>
          <div class="matching-pill-row neutral">
            <span v-for="condition in conditionsFor(selectedCard)" :key="condition">▣ {{ condition }}</span>
          </div>
        </section>

        <section v-if="mode === 'teamToMembers'" class="matching-detail-section">
          <header class="matching-portfolio-head">
            <h3>포트폴리오 요약</h3>
            <RouterLink :to="{ name: 'public-profile', params: { profileId: selectedCard.profileId }, hash: '#portfolio' }">전체 포트폴리오 보기 ›</RouterLink>
          </header>
          <div
            v-if="portfolioSummariesFor(selectedCard).length"
            class="matching-portfolio-summary-track"
            aria-label="포트폴리오 요약"
          >
            <article v-for="summary in portfolioSummariesFor(selectedCard)" :key="summary.label" class="matching-portfolio-summary-card">
              <img :src="summary.thumbnail || defaultPortfolioImage" alt="" @error="useDefaultImage($event, defaultPortfolioImage)">
              <strong>{{ summary.label }} {{ summary.count }}건</strong>
              <small>{{ summary.meta }}</small>
            </article>
          </div>
          <p v-else class="matching-portfolio-empty">등록된 포트폴리오가 없습니다.</p>
        </section>

        <footer class="matching-detail-actions" :class="{ 'member-detail-actions': mode === 'teamToMembers' }">
          <button
            v-if="mode === 'memberToTeams'"
            class="matching-action-save"
            :class="{ saved: selectedCard.savedByCurrentUser }"
            type="button"
            :aria-pressed="Boolean(selectedCard.savedByCurrentUser)"
            @click.stop="save(selectedCard)"
          >
            {{ selectedCard.savedByCurrentUser ? '저장됨' : '저장' }}
          </button>
          <button
            type="button"
            class="matching-action-invite"
            :class="{ invited: mode === 'teamToMembers' ? isCandidateInvited(selectedCard) : isTeamApplied(selectedCard) }"
            :disabled="mode === 'teamToMembers' ? isCandidateInviteDisabled(selectedCard) : isTeamApplicationLoading(selectedCard)"
            :aria-pressed="mode === 'teamToMembers' ? isCandidateInvited(selectedCard) : isTeamApplied(selectedCard)"
            @click.stop="request(selectedCard)"
          >✈ {{ mode === 'teamToMembers' ? candidateInviteText(selectedCard) : teamApplyText(selectedCard) }}</button>
          <div v-if="canShowFollowControl" class="matching-follow-control">
            <button
              v-if="!selectedFollowStatus?.ownProfile"
              class="matching-follow-button"
              :class="{ following: selectedFollowStatus?.following }"
              type="button"
              :disabled="followStatusLoading || followToggleLoading || !selectedFollowStatus"
              @click.stop="toggleSelectedFollow"
            >
              {{ followStatusLoading ? '불러오는 중' : followToggleLoading ? '처리 중' : selectedFollowStatus?.following ? '팔로잉' : '팔로우' }}
            </button>
          </div>
        </footer>
        <p v-if="followError" class="matching-follow-error" role="alert">{{ followError }}</p>
      </aside>
      <section v-else-if="isCandidateDetailRoute" class="matching-detail-card matching-route-detail-card matching-route-empty">
        <p v-if="loading" class="muted">후보 정보를 불러오는 중입니다.</p>
        <p v-else class="error-text">{{ error || '후보를 찾을 수 없습니다.' }}</p>
        <button class="ghost-button" type="button" @click="returnToMatchingList">목록으로</button>
      </section>
    </section>
  </section>
</template>
