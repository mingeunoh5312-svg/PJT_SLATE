<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { defaultTeamImage } from '../constants/defaultImages'
import { slateApi } from '../services/api'

const props = defineProps({ currentUser: Object })
const emit = defineEmits(['login'])
const route = useRoute()
const router = useRouter()

const codeGroups = ref({})
const roleGroups = ref([])
const genres = ref([])
const regions = ref([])
const teams = ref([])
const recruitments = ref([])
const applications = ref([])
const teamInvitations = ref([])
const myInvitations = ref([])
const plans = ref([])
const closureSnapshots = ref([])
const selectedTeamId = ref(null)
const selectedRecruitmentId = ref(null)
const selectedSlotId = ref(null)
const selectedPlanId = ref(null)
const referencesLoaded = ref(false)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const saved = ref('')
const recruitmentFormError = ref('')
const pendingTeamDelete = ref(false)
const pendingRecruitmentDelete = ref(false)
const pendingSlotDelete = ref(false)
const pendingLeaderTransfer = ref(false)
const newSlotFormActive = ref(false)
const selectedApplicantApplication = ref(null)
const applicantProfile = ref(null)
const applicantProfileState = ref('idle')
const applicantProfileError = ref('')
let applicantProfileRequestId = 0
const teamImageFile = ref(null)
const teamImagePreview = ref('')
const teamImageDelete = ref(false)
const imagePreview = ref(null)
const teamDescriptionExpanded = ref(false)
const pendingTeamClose = ref(false)
const recruitmentDateTimeMin = ref('')
const teamCloseConfirmMode = ref('popover')
const endedTeamSort = ref('latest')
const scheduleDrag = reactive({
  active: false,
  startX: 0,
  scrollLeft: 0
})

const isTeamListRoute = computed(() => route.name === 'teams')
const isTeamCreateRoute = computed(() => route.name === 'teams-new')
const isTeamInvitationsRoute = computed(() => route.name === 'teams-invitations')
const isTeamDetailRoute = computed(() => route.name === 'teams-detail')
const isTeamEditRoute = computed(() => route.name === 'teams-edit')
const isTeamCloseRoute = computed(() => route.name === 'teams-close')
const isTeamMembersRoute = computed(() => route.name === 'teams-members')
const isTeamRecruitmentsRoute = computed(() => route.name === 'teams-recruitments')
const isTeamRequestsRoute = computed(() => route.name === 'teams-requests')
const isTeamPlansRoute = computed(() => route.name === 'teams-plans')
const isTeamPlanCreateRoute = computed(() => route.name === 'teams-plans-new')
const isTeamPlanEditRoute = computed(() => route.name === 'teams-plans-edit')
const isPlanScheduleView = computed(() => isTeamPlansRoute.value && route.query.view === 'schedule')
const isTeamManagementRoute = computed(() => (
  isTeamEditRoute.value || isTeamCloseRoute.value || isTeamMembersRoute.value || isTeamRecruitmentsRoute.value || isTeamRequestsRoute.value || isTeamPlanCreateRoute.value || isTeamPlanEditRoute.value
))
const showTeamForm = computed(() => isTeamCreateRoute.value || isTeamEditRoute.value)
const showTeamEditor = computed(() => (
  showTeamForm.value
  || isTeamCloseRoute.value
  || isTeamMembersRoute.value
  || isTeamRecruitmentsRoute.value
  || isTeamRequestsRoute.value
  || isTeamPlansRoute.value
  || isTeamPlanCreateRoute.value
  || isTeamPlanEditRoute.value
))

const requestedCodeGroups = [
  'TEAM_STATUS',
  'TEAM_MEMBER_ROLE',
  'DURATION',
  'RECRUITMENT_STATUS',
  'SLOT_STATUS',
  'EXPERIENCE_LEVEL',
  'COLLABORATION_CONDITION',
  'REQUEST_STATUS',
  'PLAN_STATUS',
  'TEAM_END_TYPE'
]

const ACTIVE_TEAM_STATUSES = new Set(['RECRUITING', 'IN_PROGRESS', 'RECRUITMENT_CLOSED', 'CLOSING'])

const teamForm = reactive({
  teamId: null,
  name: '',
  description: '',
  genreIds: [],
  regionId: null,
  regionAnyYn: 'N',
  expectedDuration: 'WITHIN_3M',
  maxMemberCount: 6,
  status: 'RECRUITING'
})

const recruitmentForm = reactive({
  recruitmentId: null,
  title: '',
  status: 'OPEN',
  deadlineAt: '',
  workStartAt: ''
})

const slotForm = reactive({
  slotId: null,
  roleId: null,
  requiredCount: 1,
  requiredExperienceLevel: 'Y0_3',
  collaborationCondition: 'NEGOTIABLE',
  requiredYn: 'Y',
  roleDuration: 'WITHIN_3M',
  equipmentRequiredYn: 'N',
  status: 'OPEN'
})

const planForm = reactive({
  planItemId: null,
  title: '',
  description: '',
  assigneeUserId: '',
  roleId: '',
  dueAt: '',
  status: 'TODO'
})
const closureForm = reactive({
  endType: 'NORMAL',
  reason: ''
})
const transferForm = reactive({
  newLeaderUserId: '',
  reason: ''
})
const reopenForm = reactive({
  closureSnapshotId: '',
  restoreSnapshotYn: 'Y',
  reason: ''
})

const roleOptions = computed(() => roleGroups.value.flatMap((category) =>
  (category.roles || []).map((role) => ({
    ...role,
    roleId: Number(role.roleId),
    categoryName: category.name
  }))
))

const selectedTeam = computed(() => teams.value.find((team) => Number(team.teamId) === Number(selectedTeamId.value)))
const selectedRecruitment = computed(() => recruitments.value.find((item) => Number(item.recruitmentId) === Number(selectedRecruitmentId.value)))
const activeMembers = computed(() => (selectedTeam.value?.members || []).filter((member) => member.status === 'ACTIVE'))
const eligibleLeaderMembers = computed(() => activeMembers.value.filter((member) => member.teamRole !== 'LEADER'))
const myTeamRole = computed(() => selectedTeam.value?.myTeamRole || selectedTeam.value?.members?.find((member) => Number(member.userId) === Number(props.currentUser?.userId))?.teamRole)
const canManageTeam = computed(() => ['LEADER', 'SUB_LEADER'].includes(myTeamRole.value))
const isTeamLeader = computed(() => myTeamRole.value === 'LEADER')
const isTeamEnded = computed(() => selectedTeam.value?.status === 'ENDED')
const canEditTeam = computed(() => canManageTeam.value && !isTeamEnded.value)
const canCloseTeam = computed(() => isTeamLeader.value && selectedTeam.value?.teamId && !isTeamEnded.value)
const routeAccessDenied = computed(() => (
  isTeamManagementRoute.value
  && Boolean(selectedTeam.value)
  && !canManageTeam.value
))
const routeTeamMissing = computed(() => (
  !loading.value
  && !isTeamListRoute.value
  && !isTeamCreateRoute.value
  && !selectedTeam.value
))
const isPlanEditReady = computed(() => (
  !isTeamPlanEditRoute.value
  || Number(planForm.planItemId) === Number(route.params.planItemId)
))
const showPlanFormPanel = computed(() => (
  !routeAccessDenied.value
  && !routeTeamMissing.value
  && Boolean(teamForm.teamId)
  && canEditTeam.value
  && (isTeamPlanCreateRoute.value || (isTeamPlanEditRoute.value && isPlanEditReady.value))
))
const planFormTitle = computed(() => (planForm.planItemId ? '계획 수정' : '새 계획'))
const activeDisplayTeams = computed(() => [...teams.value]
  .filter((team) => ACTIVE_TEAM_STATUSES.has(team.status))
  .sort((left, right) => (dateTimestamp(right.createdAt) || 0) - (dateTimestamp(left.createdAt) || 0))
  .slice(0, 3))
const endedDisplayTeams = computed(() => [...teams.value]
  .filter((team) => team.status === 'ENDED')
  .sort((left, right) => {
    const leftDate = dateTimestamp(left.endedAt || left.updatedAt || left.lastActiveAt || left.createdAt) || 0
    const rightDate = dateTimestamp(right.endedAt || right.updatedAt || right.lastActiveAt || right.createdAt) || 0
    return endedTeamSort.value === 'oldest' ? leftDate - rightDate : rightDate - leftDate
  }))
const dashboardTeam = computed(() => selectedTeam.value || null)
const dashboardMembers = computed(() => activeMembers.value.length)
const dashboardMaxMembers = computed(() => dashboardTeam.value?.maxMemberCount ?? null)
const teamStatusFormOptions = computed(() => {
  const options = codeOptions('TEAM_STATUS')
  return isTeamCreateRoute.value ? options.filter((item) => item.code !== 'ENDED') : options
})
const openRecruitments = computed(() => recruitments.value.filter((item) => effectiveRecruitmentStatus(item) === 'OPEN'))
const dashboardSlots = computed(() => recruitments.value.flatMap((item) => item.slots || []))
const pendingApplications = computed(() => applications.value.filter((item) => item.status === 'PENDING'))
const pendingTeamInvitations = computed(() => teamInvitations.value.filter((item) => item.status === 'PENDING'))
const pendingMyInvitations = computed(() => myInvitations.value.filter((item) => item.status === 'PENDING'))
const decidedMyInvitations = computed(() => myInvitations.value.filter((item) => item.status !== 'PENDING').slice(0, 8))
const recruitmentSummary = computed(() => ({
  total: recruitments.value.length,
  open: openRecruitments.value.length,
  closingSoon: closingSoonRecruitments.value.length,
  slots: dashboardSlots.value.length
}))
const closingSoonRecruitments = computed(() => {
  const now = Date.now()
  const sevenDaysLater = now + (7 * 24 * 60 * 60 * 1000)
  return openRecruitments.value.filter((item) => {
    const deadline = dateTimestamp(item.deadlineAt)
    return deadline !== null && deadline >= now && deadline <= sevenDaysLater
  })
})
const latestJoinedAt = computed(() => activeMembers.value
  .map((member) => dateTimestamp(member.joinedAt))
  .filter((value) => value !== null)
  .sort((left, right) => right - left)[0] ?? null)
const activePlans = computed(() => plans.value.filter((plan) => plan.status !== 'CANCELED'))
const dashboardProgress = computed(() => {
  if (!activePlans.value.length) return 0
  const done = activePlans.value.filter((plan) => plan.status === 'DONE').length
  return Math.round((done / activePlans.value.length) * 100)
})
const nextPlan = computed(() => activePlans.value
  .filter((plan) => plan.status !== 'DONE' && dateTimestamp(plan.dueAt) !== null)
  .sort((left, right) => dateTimestamp(left.dueAt) - dateTimestamp(right.dueAt))[0] || null)
const recentPlanUpdates = computed(() => [...plans.value]
  .sort((left, right) => (dateTimestamp(right.updatedAt || right.createdAt) || 0) - (dateTimestamp(left.updatedAt || left.createdAt) || 0))
  .slice(0, 3))
const planTimeline = computed(() => {
  const grouped = new Map()
  plans.value.forEach((plan) => {
    const timestamp = dateTimestamp(plan.dueAt)
    if (timestamp === null) return
    const date = new Date(timestamp)
    date.setHours(0, 0, 0, 0)
    const dayTimestamp = date.getTime()
    if (!grouped.has(dayTimestamp)) grouped.set(dayTimestamp, [])
    grouped.get(dayTimestamp).push(plan)
  })
  const entries = [...grouped.entries()].sort(([left], [right]) => left - right)
  const min = entries[0]?.[0] ?? null
  const max = entries[entries.length - 1]?.[0] ?? null
  const range = min !== null && max !== null ? max - min : 0
  const groups = entries.map(([timestamp, groupedPlans]) => {
    const position = range === 0 ? 50 : ((timestamp - min) / range) * 100
    return {
      timestamp,
      plans: groupedPlans.sort((left, right) => Number(left.planItemId) - Number(right.planItemId)),
      position,
      alignment: position <= 10 ? 'start' : position >= 90 ? 'end' : 'center'
    }
  })
  const largestGroup = groups.reduce((largest, group) => Math.max(largest, group.plans.length), 0)
  return {
    groups,
    min,
    max,
    height: Math.max(190, 130 + (largestGroup * 44))
  }
})
const scheduleLineWidth = computed(() => {
  const count = planTimeline.value.groups.length
  if (!count) return '0px'
  return `${(count * 220) + ((count - 1) * 12)}px`
})
const undatedPlans = computed(() => plans.value
  .filter((plan) => dateTimestamp(plan.dueAt) === null)
  .sort((left, right) => Number(left.planItemId) - Number(right.planItemId)))
const planScheduleRows = computed(() => [...plans.value].sort((left, right) => {
  const leftDue = dateTimestamp(left.dueAt)
  const rightDue = dateTimestamp(right.dueAt)
  if (leftDue === null && rightDue === null) return Number(left.planItemId) - Number(right.planItemId)
  if (leftDue === null) return 1
  if (rightDue === null) return -1
  return leftDue - rightDue || Number(left.planItemId) - Number(right.planItemId)
}))
const planSummary = computed(() => ({
  total: plans.value.length,
  todo: plans.value.filter((plan) => plan.status === 'TODO').length,
  inProgress: plans.value.filter((plan) => plan.status === 'IN_PROGRESS').length,
  done: plans.value.filter((plan) => plan.status === 'DONE').length,
  undated: undatedPlans.value.length
}))
const leaderMember = computed(() => activeMembers.value.find((member) => member.teamRole === 'LEADER') || null)
const leaderProfileId = computed(() => positiveNumber(leaderMember.value?.profileId))
const teamDescription = computed(() => dashboardTeam.value?.description || '등록된 팀 설명이 없습니다.')
const isTeamDescriptionLong = computed(() => teamDescription.value.length > 150)
const visibleTeamDescription = computed(() => (
  isTeamDescriptionLong.value && !teamDescriptionExpanded.value
    ? `${teamDescription.value.slice(0, 150)}...`
    : teamDescription.value
))
const nextTeamAction = computed(() => {
  const actions = []

  teams.value.forEach((team) => {
    const teamId = positiveNumber(team?.teamId)
    if (!teamId || team.status === 'ENDED') return

    const isManager = ['LEADER', 'SUB_LEADER'].includes(team.myTeamRole)
    const pendingApplicationCount = countValue(team.pendingApplicationCount)
    const pendingInvitationCount = countValue(team.pendingInvitationCount)
    const nextPlanDue = dateTimestamp(team.nextUserPlanDueAt)

    if (isManager && pendingApplicationCount > 0) {
      actions.push({
        priority: 0,
        due: dateTimestamp(team.updatedAt || team.lastActiveAt || team.createdAt) ?? Date.now(),
        label: '지원 검토',
        title: team.name || '팀',
        detail: `대기 지원 ${pendingApplicationCount}건`,
        route: teamRequestsRoute(teamId)
      })
    }

    if (isManager && pendingInvitationCount > 0) {
      actions.push({
        priority: 1,
        due: dateTimestamp(team.updatedAt || team.lastActiveAt || team.createdAt) ?? Date.now(),
        label: '초대 확인',
        title: team.name || '팀',
        detail: `대기 초대 ${pendingInvitationCount}건`,
        route: teamRequestsRoute(teamId)
      })
    }

    if (team.nextUserPlanTitle) {
      actions.push({
        priority: 2,
        due: nextPlanDue ?? Number.MAX_SAFE_INTEGER,
        label: nextPlanDue ? nextPlanDayLabel({ dueAt: team.nextUserPlanDueAt }) : '계획 확인',
        title: team.nextUserPlanTitle,
        detail: team.name || '팀 계획',
        route: {
          name: 'teams-plans',
          params: { teamId },
          query: nextPlanDue ? { view: 'schedule' } : {},
          hash: nextPlanDue ? '#team-plan-schedule' : ''
        }
      })
    }
  })

  if (actions.length) {
    return actions.sort((left, right) => (
      left.priority - right.priority
      || left.due - right.due
      || String(left.title).localeCompare(String(right.title), 'ko')
    ))[0]
  }

  const fallbackTeam = activeDisplayTeams.value[0]
  if (fallbackTeam?.teamId) {
    return {
      priority: 9,
      due: Number.MAX_SAFE_INTEGER,
      label: '팀 확인',
      title: fallbackTeam.name || '팀',
      detail: '최근 팀 현황 확인',
      route: { name: 'teams-detail', params: { teamId: fallbackTeam.teamId } }
    }
  }

  return {
    priority: 10,
    due: Number.MAX_SAFE_INTEGER,
    label: '팀 만들기',
    title: '새 팀으로 작업 시작',
    detail: '팀을 만들거나 모집 중인 팀 찾기',
    route: { name: 'teams-new' }
  }
})

function closeApplicantProfile() {
  applicantProfileRequestId += 1
  selectedApplicantApplication.value = null
  applicantProfile.value = null
  applicantProfileState.value = 'idle'
  applicantProfileError.value = ''
}

async function openApplicantProfile(application) {
  if (!selectedTeam.value || !canManageTeam.value || isTeamEnded.value) return
  const requestId = ++applicantProfileRequestId
  selectedApplicantApplication.value = application
  applicantProfile.value = null
  applicantProfileError.value = ''

  if (!application.applicantProfileId) {
    applicantProfileState.value = 'missing'
    return
  }

  applicantProfileState.value = 'loading'
  try {
    const profile = await slateApi.profile(application.applicantProfileId)
    if (requestId !== applicantProfileRequestId) return
    if (!profile) {
      applicantProfileState.value = 'missing'
      return
    }
    if (profile.visibility !== 'PUBLIC' || profile.activityStatus === 'HIDDEN') {
      applicantProfileState.value = 'private'
      return
    }
    applicantProfile.value = profile
    applicantProfileState.value = 'ready'
  } catch (err) {
    if (requestId !== applicantProfileRequestId) return
    if (String(err.message).includes('찾을 수 없')) {
      applicantProfileState.value = 'missing'
      return
    }
    applicantProfileError.value = err.message
    applicantProfileState.value = 'error'
  }
}

function applicantExperienceLabel(profile) {
  return codeOptions('EXPERIENCE_LEVEL').find((item) => item.code === profile?.experienceLevel)?.displayName
    || profile?.experienceLevel
    || '경력 정보 없음'
}

function openTeamPlans(view = '') {
  const teamId = selectedTeam.value?.teamId || teamForm.teamId
  if (!teamId) {
    router.push({ name: 'teams-new' })
    return
  }
  router.push({
    name: 'teams-plans',
    params: { teamId },
    query: view ? { view } : {},
    hash: view === 'schedule' ? '#team-plan-schedule' : ''
  })
}

function teamRequestsRoute(teamId) {
  return {
    name: 'teams-requests',
    params: { teamId }
  }
}

function goNextTeamAction() {
  if (nextTeamAction.value?.route) router.push(nextTeamAction.value.route)
}

function openLeaderProfile() {
  const profileId = leaderProfileId.value
  if (!profileId) return
  router.push({ name: 'public-profile', params: { profileId } })
}

function openNewPlanPage() {
  const teamId = selectedTeam.value?.teamId || teamForm.teamId
  if (!teamId) {
    router.push({ name: 'teams-new' })
    return
  }
  newPlan()
  router.push({
    name: 'teams-plans-new',
    params: { teamId }
  })
}

function openDateTimePicker(event) {
  try {
    event.currentTarget?.showPicker?.()
  } catch {
    // Browsers without picker support keep the native text/date input behavior.
  }
}

async function scrollToPlanSchedule() {
  if (!isPlanScheduleView.value || !teamForm.teamId) return
  await nextTick()
  document.getElementById('team-plan-schedule')?.scrollIntoView({ block: 'start' })
}

function startScheduleDrag(event) {
  const target = event.currentTarget
  if (!target) return
  scheduleDrag.active = true
  scheduleDrag.startX = event.clientX
  scheduleDrag.scrollLeft = target.scrollLeft
  target.setPointerCapture?.(event.pointerId)
}

function moveScheduleDrag(event) {
  if (!scheduleDrag.active) return
  const target = event.currentTarget
  if (!target) return
  target.scrollLeft = scheduleDrag.scrollLeft - (event.clientX - scheduleDrag.startX)
}

function endScheduleDrag(event) {
  if (!scheduleDrag.active) return
  if (event.currentTarget?.hasPointerCapture?.(event.pointerId)) {
    event.currentTarget.releasePointerCapture(event.pointerId)
  }
  scheduleDrag.active = false
}

function openTeamPanel(panel) {
  if (!teamForm.teamId && !selectedTeam.value) {
    router.push({ name: 'teams-new' })
    return
  }
  const teamId = selectedTeam.value?.teamId || teamForm.teamId
  if (panel === 'team') {
    router.push({ name: 'teams-edit', params: { teamId } })
    return
  }
  if (panel === 'closure') {
    router.push({ name: 'teams-close', params: { teamId } })
    return
  }
  if (panel === 'members') {
    router.push({ name: 'teams-members', params: { teamId } })
    return
  }
  if (panel === 'recruitment') {
    router.push({ name: 'teams-recruitments', params: { teamId } })
    return
  }
  if (panel === 'requests') {
    router.push(teamRequestsRoute(teamId))
  }
}

function selectTeamCard(team) {
  if (team?.teamId) {
    router.push({ name: 'teams-detail', params: { teamId: team.teamId } })
  }
}

function teamImage(team) {
  return team?.thumbnailUrl || team?.imageUrl || defaultTeamImage
}

function hasTeamImage(team) {
  return Boolean(team?.thumbnailUrl || team?.imageUrl)
}

function handleTeamImageError(team) {
  if (!team) return
  team.thumbnailUrl = null
  team.imageUrl = null
}

function openImagePreview(src, alt) {
  if (src) imagePreview.value = { src, alt }
}

function closeImagePreview() {
  imagePreview.value = null
}

function teamGenreText(team) {
  return (team?.genres || []).map((genre) => genre.name).join(' · ') || '등록된 장르 없음'
}

function teamRegionText(team) {
  if (team?.regionAnyYn === 'Y') return '지역 무관'
  return team?.publicRegionName || team?.regionName || '지역 정보 없음'
}

function teamStatusText(team) {
  if (team?.status === 'ENDED') return team?.endType === 'DISSOLUTION' ? '해체' : '종료'
  const referenceLabel = codeOptions('TEAM_STATUS').find((item) => item.code === team?.status)?.displayName
  if (referenceLabel) return referenceLabel
  if (team?.status === 'PLANNING') return '계획 중'
  const fallbackLabels = {
    RECRUITING: '모집 중',
    IN_PROGRESS: '진행 중',
    RECRUITMENT_CLOSED: '모집 종료',
    CLOSING: '종료 준비'
  }
  return fallbackLabels[team?.status] || '상태 정보 없음'
}

function teamEndedMessage(team) {
  return team?.endType === 'DISSOLUTION' ? '해체된 팀입니다.' : '팀 작업이 종료된 팀입니다.'
}

function teamListProgress(team) {
  const max = Number(team?.maxMemberCount)
  const current = Number(team?.currentMemberCount ?? (team?.members || []).filter((member) => member.status === 'ACTIVE').length)
  if (!Number.isFinite(max) || max <= 0) return 0
  return Math.min(100, Math.max(0, Math.round((current / max) * 100)))
}

function positiveNumber(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
}

function countValue(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0
}

function latestTeamScheduleText(team) {
  if (Number(team?.teamId) === Number(selectedTeamId.value) && recentPlanUpdates.value[0]) {
    const plan = recentPlanUpdates.value[0]
    return `${plan.title} · ${formatShortDate(plan.updatedAt || plan.createdAt)}`
  }
  return `최근 활동 ${formatKoreanDate(team?.lastActiveAt || team?.updatedAt || team?.createdAt)}`
}

function dateTimestamp(value) {
  if (!value) return null
  const timestamp = new Date(String(value).replace(' ', 'T')).getTime()
  return Number.isFinite(timestamp) ? timestamp : null
}

function isPastDeadline(value) {
  const timestamp = dateTimestamp(value)
  return timestamp !== null && timestamp < Date.now()
}

function effectiveStatusForDeadline(status, deadlineAt) {
  const normalizedStatus = status || 'OPEN'
  return normalizedStatus === 'OPEN' && isPastDeadline(deadlineAt) ? 'CLOSED' : normalizedStatus
}

function effectiveRecruitmentStatus(recruitment) {
  return effectiveStatusForDeadline(recruitment?.status, recruitment?.deadlineAt)
}

function normalizedRecruitment(recruitment) {
  if (!recruitment) return recruitment
  return {
    ...recruitment,
    status: effectiveRecruitmentStatus(recruitment)
  }
}

function formatKoreanDate(value, fallback = '정보 없음') {
  const timestamp = typeof value === 'number' ? value : dateTimestamp(value)
  if (timestamp === null || !Number.isFinite(timestamp)) return fallback
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  }).format(new Date(timestamp))
}

function formatShortDate(value) {
  const timestamp = dateTimestamp(value)
  if (timestamp === null) return '날짜 없음'
  return new Intl.DateTimeFormat('ko-KR', { month: 'numeric', day: 'numeric' }).format(new Date(timestamp))
}

function planStatusLabel(status) {
  return codeOptions('PLAN_STATUS').find((item) => item.code === status)?.displayName || status || '상태 정보 없음'
}

function requestStatusLabel(status) {
  return codeOptions('REQUEST_STATUS').find((item) => item.code === status)?.displayName || status || '상태 정보 없음'
}

function recruitmentStatusLabel(status) {
  return codeOptions('RECRUITMENT_STATUS').find((item) => item.code === status)?.displayName || status || '상태 정보 없음'
}

function slotStatusLabel(status) {
  return codeOptions('SLOT_STATUS').find((item) => item.code === status)?.displayName || status || '상태 정보 없음'
}

function experienceLevelLabel(status) {
  return codeOptions('EXPERIENCE_LEVEL').find((item) => item.code === status)?.displayName || status || '경력 정보 없음'
}

function collaborationConditionLabel(status) {
  return codeOptions('COLLABORATION_CONDITION').find((item) => item.code === status)?.displayName || status || '협업 조건 없음'
}

function nextPlanDayLabel(plan) {
  const dueAt = dateTimestamp(plan?.dueAt)
  if (dueAt === null) return '일정 없음'
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const dueDate = new Date(dueAt)
  dueDate.setHours(0, 0, 0, 0)
  const days = Math.round((dueDate.getTime() - today.getTime()) / (24 * 60 * 60 * 1000))
  if (days === 0) return 'D-Day'
  return days > 0 ? `D-${days}` : `D+${Math.abs(days)}`
}

function codeOptions(group, fallback = []) {
  return codeGroups.value[group]?.length ? codeGroups.value[group] : fallback
}

function firstCode(group, fallback) {
  return codeOptions(group)[0]?.code || fallback
}

function firstCreatableTeamStatus() {
  return codeOptions('TEAM_STATUS').find((item) => item.code !== 'ENDED')?.code || 'RECRUITING'
}

function toggleGenre(genreId) {
  const numeric = Number(genreId)
  const index = teamForm.genreIds.indexOf(numeric)
  if (index >= 0) teamForm.genreIds.splice(index, 1)
  else teamForm.genreIds.push(numeric)
}

function toInputDate(value) {
  if (!value) return ''
  return String(value).replace(' ', 'T').slice(0, 16)
}

function currentInputDateTime() {
  const now = new Date()
  now.setSeconds(0, 0)
  const offset = now.getTimezoneOffset() * 60000
  return new Date(now.getTime() - offset).toISOString().slice(0, 16)
}

function refreshRecruitmentDateTimeMin() {
  recruitmentDateTimeMin.value = currentInputDateTime()
}

function currentMinuteTimestamp() {
  const now = new Date()
  now.setSeconds(0, 0)
  return now.getTime()
}

function isBeforeCurrentMinute(value) {
  const timestamp = dateTimestamp(value)
  return timestamp !== null && timestamp < currentMinuteTimestamp()
}

function toSqlDate(value) {
  if (!value) return null
  const normalized = value.replace('T', ' ')
  return normalized.length === 16 ? `${normalized}:00` : normalized
}

function resetTeamForm(team) {
  if (teamImagePreview.value) URL.revokeObjectURL(teamImagePreview.value)
  teamImagePreview.value = ''
  teamImageFile.value = null
  teamImageDelete.value = false
  teamForm.teamId = team?.teamId || null
  teamForm.name = team?.name || ''
  teamForm.description = team?.description || ''
  teamForm.genreIds = (team?.genres || []).map((genre) => Number(genre.genreId))
  teamForm.regionAnyYn = team?.regionAnyYn || 'N'
  teamForm.regionId = team?.regionId || regions.value[0]?.regionId || null
  teamForm.expectedDuration = team?.expectedDuration || firstCode('DURATION', 'WITHIN_3M')
  teamForm.maxMemberCount = team?.maxMemberCount || 6
  teamForm.status = team?.status || firstCreatableTeamStatus()
  if (teamForm.genreIds.length === 0 && genres.value[0]) teamForm.genreIds = [Number(genres.value[0].genreId)]
}

function selectTeamImage(event) {
  if (teamImagePreview.value) URL.revokeObjectURL(teamImagePreview.value)
  teamImageFile.value = event.target.files?.[0] || null
  teamImagePreview.value = teamImageFile.value ? URL.createObjectURL(teamImageFile.value) : ''
  teamImageDelete.value = false
}

function removeTeamImage() {
  if (teamImagePreview.value) URL.revokeObjectURL(teamImagePreview.value)
  teamImagePreview.value = ''
  teamImageFile.value = null
  teamImageDelete.value = true
}

function clearPendingActions() {
  pendingTeamDelete.value = false
  pendingRecruitmentDelete.value = false
  pendingSlotDelete.value = false
  pendingLeaderTransfer.value = false
}

function resetRecruitmentForm(recruitment) {
  recruitmentFormError.value = ''
  if (!recruitment) refreshRecruitmentDateTimeMin()
  recruitmentForm.recruitmentId = recruitment?.recruitmentId || null
  recruitmentForm.title = recruitment?.title || ''
  recruitmentForm.status = recruitment ? effectiveRecruitmentStatus(recruitment) : firstCode('RECRUITMENT_STATUS', 'OPEN')
  recruitmentForm.deadlineAt = toInputDate(recruitment?.deadlineAt)
  recruitmentForm.workStartAt = recruitment ? toInputDate(recruitment.workStartAt) : recruitmentDateTimeMin.value
}

function resetSlotForm(slot) {
  slotForm.slotId = slot?.slotId || null
  slotForm.roleId = slot?.roleId || roleOptions.value[0]?.roleId || null
  slotForm.requiredCount = slot?.requiredCount || 1
  slotForm.requiredExperienceLevel = slot?.requiredExperienceLevel || firstCode('EXPERIENCE_LEVEL', 'Y0_3')
  slotForm.collaborationCondition = slot?.collaborationCondition || firstCode('COLLABORATION_CONDITION', 'NEGOTIABLE')
  slotForm.requiredYn = slot?.requiredYn || 'Y'
  slotForm.roleDuration = slot?.roleDuration || firstCode('DURATION', 'WITHIN_3M')
  slotForm.equipmentRequiredYn = slot?.equipmentRequiredYn || 'N'
  slotForm.status = slot?.status || firstCode('SLOT_STATUS', 'OPEN')
}

function resetPlanForm(plan) {
  planForm.planItemId = plan?.planItemId || null
  planForm.title = plan?.title || ''
  planForm.description = plan?.description || ''
  planForm.assigneeUserId = plan?.assigneeUserId || ''
  planForm.roleId = plan?.roleId || ''
  planForm.dueAt = toInputDate(plan?.dueAt)
  planForm.status = plan?.status || firstCode('PLAN_STATUS', 'TODO')
}

function resetClosureForm() {
  closureForm.endType = firstCode('TEAM_END_TYPE', 'NORMAL')
  closureForm.reason = ''
}

function resetTransferForm() {
  transferForm.newLeaderUserId = eligibleLeaderMembers.value[0]?.userId || ''
  transferForm.reason = ''
}

function resetReopenForm() {
  reopenForm.closureSnapshotId = closureSnapshots.value[0]?.closureSnapshotId || ''
  reopenForm.restoreSnapshotYn = 'Y'
  reopenForm.reason = ''
}

async function loadReferences() {
  if (referencesLoaded.value) return
  const [codes, roleRows, genreRows, regionRows] = await Promise.all([
    slateApi.codes(requestedCodeGroups),
    slateApi.roles(),
    slateApi.genres(),
    slateApi.regions('', 80)
  ])
  codeGroups.value = codes || {}
  roleGroups.value = roleRows || []
  genres.value = genreRows || []
  regions.value = regionRows || []
  referencesLoaded.value = true
}

async function loadRecruitments(teamId, preferredRecruitmentId) {
  const rows = teamId ? await slateApi.recruitments(teamId) : []
  recruitments.value = (rows || []).map(normalizedRecruitment)
  const next = recruitments.value.find((item) => Number(item.recruitmentId) === Number(preferredRecruitmentId)) || recruitments.value[0]
  if (next) selectRecruitment(next)
  else newRecruitment()
}

async function loadTeamWorkspace(teamId) {
  if (!teamId) {
    applications.value = []
    teamInvitations.value = []
    plans.value = []
    closureSnapshots.value = []
    resetReopenForm()
    return
  }
  const [applicationRows, invitationRows, planRows, snapshotRows] = await Promise.all([
    canManageTeam.value ? slateApi.teamApplications(teamId) : Promise.resolve([]),
    canManageTeam.value ? slateApi.teamInvitations(teamId) : Promise.resolve([]),
    slateApi.teamPlans(teamId),
    slateApi.teamClosureSnapshots(teamId)
  ])
  applications.value = applicationRows || []
  teamInvitations.value = invitationRows || []
  plans.value = planRows || []
  closureSnapshots.value = snapshotRows || []
  const nextPlan = plans.value.find((plan) => Number(plan.planItemId) === Number(selectedPlanId.value)) || plans.value[0]
  if (nextPlan) selectPlan(nextPlan)
  else newPlan()
  resetClosureForm()
  resetReopenForm()
}

async function selectTeam(team) {
  selectedTeamId.value = team?.teamId || null
  teamDescriptionExpanded.value = false
  pendingTeamClose.value = false
  clearPendingActions()
  resetTeamForm(team)
  resetTransferForm()
  selectedRecruitmentId.value = null
  selectedSlotId.value = null
  selectedPlanId.value = null
  await loadRecruitments(team?.teamId)
  await loadTeamWorkspace(team?.teamId)
}

async function loadTeams(preferredTeamId) {
  teams.value = await slateApi.myTeams()
  const next = preferredTeamId
    ? teams.value.find((team) => Number(team.teamId) === Number(preferredTeamId))
    : null
  if (next) {
    await selectTeam(next)
    return
  }
  selectedTeamId.value = null
  if (preferredTeamId) error.value = '팀을 찾을 수 없거나 접근 권한이 없습니다.'
}

async function load() {
  if (!props.currentUser) return
  loading.value = true
  error.value = ''
  saved.value = ''
  try {
    await loadReferences()
    myInvitations.value = await slateApi.myTeamInvitations()
    if (isTeamCreateRoute.value) {
      teams.value = await slateApi.myTeams()
      newTeam()
      return
    }
    if (isTeamListRoute.value) {
      await loadTeams()
      recruitments.value = []
      applications.value = []
      teamInvitations.value = []
      plans.value = []
      closureSnapshots.value = []
      resetTeamForm(null)
      return
    }
    if (isTeamInvitationsRoute.value) {
      teams.value = await slateApi.myTeams()
      selectedTeamId.value = null
      recruitments.value = []
      applications.value = []
      teamInvitations.value = []
      plans.value = []
      closureSnapshots.value = []
      resetTeamForm(null)
      return
    }
    await loadTeams(route.params.teamId)
    if (isTeamPlanCreateRoute.value) newPlan()
    if (isTeamPlanEditRoute.value) {
      const targetPlan = plans.value.find((plan) => Number(plan.planItemId) === Number(route.params.planItemId))
      if (targetPlan) selectPlan(targetPlan)
      else {
        selectedPlanId.value = null
        resetPlanForm(null)
        error.value = '수정할 계획을 찾을 수 없습니다.'
      }
    }
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function newTeam() {
  clearPendingActions()
  selectedTeamId.value = null
  recruitments.value = []
  applications.value = []
  teamInvitations.value = []
  plans.value = []
  closureSnapshots.value = []
  resetTeamForm(null)
  newRecruitment()
  newPlan()
  resetClosureForm()
  resetTransferForm()
  resetReopenForm()
}

function selectRecruitment(recruitment) {
  pendingRecruitmentDelete.value = false
  pendingSlotDelete.value = false
  newSlotFormActive.value = false
  selectedRecruitmentId.value = recruitment?.recruitmentId || null
  resetRecruitmentForm(recruitment)
  const firstSlot = recruitment?.slots?.[0]
  if (firstSlot) selectSlot(firstSlot)
  else {
    selectedSlotId.value = null
    resetSlotForm(null)
  }
}

function newRecruitment(options = {}) {
  const preserveSelection = Boolean(options?.preserveSelection)
  pendingRecruitmentDelete.value = false
  pendingSlotDelete.value = false
  if (!preserveSelection) {
    selectedRecruitmentId.value = null
    selectedSlotId.value = null
    newSlotFormActive.value = false
    resetSlotForm(null)
  }
  resetRecruitmentForm(null)
}

function selectSlot(slot) {
  pendingSlotDelete.value = false
  newSlotFormActive.value = false
  selectedSlotId.value = slot?.slotId || null
  resetSlotForm(slot)
}

function newSlot() {
  pendingSlotDelete.value = false
  newSlotFormActive.value = true
  selectedSlotId.value = null
  resetSlotForm(null)
}

function selectPlan(plan) {
  selectedPlanId.value = plan?.planItemId || null
  resetPlanForm(plan)
}

function newPlan() {
  selectedPlanId.value = null
  resetPlanForm(null)
}

function editPlan(plan) {
  if (!canEditTeam.value || !plan) return
  const teamId = selectedTeam.value?.teamId || teamForm.teamId
  if (!teamId || !plan.planItemId) return
  error.value = ''
  saved.value = ''
  router.push({
    name: 'teams-plans-edit',
    params: { teamId, planItemId: plan.planItemId }
  })
}

function cancelPlanForm() {
  router.push({ name: 'teams-plans', params: { teamId: teamForm.teamId } })
}

function buildTeamPayload() {
  return {
    name: teamForm.name.trim(),
    description: teamForm.description.trim(),
    genreIds: teamForm.genreIds.map(Number),
    regionId: teamForm.regionAnyYn === 'Y' ? null : Number(teamForm.regionId),
    regionAnyYn: teamForm.regionAnyYn,
    expectedDuration: teamForm.expectedDuration,
    maxMemberCount: Number(teamForm.maxMemberCount),
    status: teamForm.status
  }
}

function buildRecruitmentPayload() {
  const deadlineAt = toSqlDate(recruitmentForm.deadlineAt)
  return {
    title: recruitmentForm.title.trim(),
    status: effectiveStatusForDeadline(recruitmentForm.status, deadlineAt),
    deadlineAt,
    workStartAt: toSqlDate(recruitmentForm.workStartAt)
  }
}

function buildSlotPayload() {
  return {
    roleId: Number(slotForm.roleId),
    requiredCount: Number(slotForm.requiredCount),
    requiredExperienceLevel: slotForm.requiredExperienceLevel,
    collaborationCondition: slotForm.collaborationCondition,
    requiredYn: slotForm.requiredYn,
    roleDuration: slotForm.roleDuration,
    equipmentRequiredYn: slotForm.equipmentRequiredYn,
    status: slotForm.status
  }
}

function buildPlanPayload() {
  return {
    title: planForm.title.trim(),
    description: planForm.description.trim(),
    assigneeUserId: planForm.assigneeUserId ? Number(planForm.assigneeUserId) : null,
    roleId: planForm.roleId ? Number(planForm.roleId) : null,
    dueAt: toSqlDate(planForm.dueAt),
    status: planForm.status
  }
}

async function saveTeam() {
  if (teamForm.teamId && isTeamEnded.value) {
    error.value = '종료된 팀은 수정할 수 없습니다.'
    return
  }
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    const payload = buildTeamPayload()
    if (!payload.name || !payload.description || payload.genreIds.length === 0) throw new Error('팀 필수값을 확인해주세요.')
    if (payload.regionAnyYn !== 'Y' && !payload.regionId) throw new Error('팀 지역을 선택해주세요.')
    const wasEditing = Boolean(teamForm.teamId)
    const savedTeam = wasEditing
      ? await slateApi.updateTeam(teamForm.teamId, payload)
      : await slateApi.createTeam(payload)
    if (teamImageFile.value) await slateApi.uploadEntityImage('team', savedTeam.teamId, teamImageFile.value)
    else if (teamImageDelete.value && savedTeam.imageUrl) await slateApi.deleteEntityImage('team', savedTeam.teamId)
    pendingTeamDelete.value = false
    await loadTeams(savedTeam.teamId)
    await router.push({ name: 'teams-detail', params: { teamId: savedTeam.teamId } })
    saved.value = wasEditing ? '팀 정보가 수정되었습니다.' : '팀이 생성되었습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

function requestTeamDelete() {
  pendingTeamDelete.value = true
  error.value = ''
  saved.value = ''
}

function cancelTeamDelete() {
  pendingTeamDelete.value = false
}

async function deleteTeam() {
  if (!teamForm.teamId || !isTeamLeader.value || isTeamEnded.value) return
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.deleteTeam(teamForm.teamId)
    pendingTeamDelete.value = false
    saved.value = '팀이 삭제되었습니다.'
    await router.push({ name: 'teams' })
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function saveRecruitment() {
  recruitmentFormError.value = ''
  error.value = ''
  if (!canEditTeam.value) {
    recruitmentFormError.value = '종료된 팀은 모집 공고를 수정할 수 없습니다.'
    return
  }
  if (!teamForm.teamId) {
    recruitmentFormError.value = '팀을 먼저 저장해주세요.'
    return
  }
  saving.value = true
  saved.value = ''
  try {
    if (!recruitmentForm.recruitmentId) refreshRecruitmentDateTimeMin()
    const payload = buildRecruitmentPayload()
    if (!payload.title) throw new Error('모집 제목을 입력해주세요.')
    if (!recruitmentForm.recruitmentId && recruitmentForm.workStartAt && isBeforeCurrentMinute(recruitmentForm.workStartAt)) {
      throw new Error('시작일은 현재 일자 이전으로 설정할 수 없습니다.')
    }
    if (!recruitmentForm.recruitmentId && recruitmentForm.deadlineAt && isBeforeCurrentMinute(recruitmentForm.deadlineAt)) {
      throw new Error('마감일은 현재 일자 이전으로 설정할 수 없습니다.')
    }
    const savedRecruitment = recruitmentForm.recruitmentId
      ? await slateApi.updateRecruitment(recruitmentForm.recruitmentId, payload)
      : await slateApi.createRecruitment(teamForm.teamId, payload)
    await loadRecruitments(teamForm.teamId, savedRecruitment.recruitmentId)
  } catch (err) {
    recruitmentFormError.value = err.message
  } finally {
    saving.value = false
  }
}

function requestRecruitmentDelete() {
  pendingRecruitmentDelete.value = true
  recruitmentFormError.value = ''
  error.value = ''
  saved.value = ''
}

function cancelRecruitmentDelete() {
  pendingRecruitmentDelete.value = false
}

async function deleteRecruitment() {
  if (!canEditTeam.value || !recruitmentForm.recruitmentId) return
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.deleteRecruitment(recruitmentForm.recruitmentId)
    pendingRecruitmentDelete.value = false
    pendingSlotDelete.value = false
    await loadRecruitments(teamForm.teamId)
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function saveSlot() {
  if (!canEditTeam.value) {
    error.value = '종료된 팀은 구인 공고를 수정할 수 없습니다.'
    return
  }
  if (!selectedRecruitmentId.value) {
    error.value = '모집 공고를 먼저 저장해주세요.'
    return
  }
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    const payload = buildSlotPayload()
    if (!payload.roleId) throw new Error('모집 역할을 선택해주세요.')
    const savedSlot = slotForm.slotId
      ? await slateApi.updateSlot(slotForm.slotId, payload)
      : await slateApi.createSlot(selectedRecruitmentId.value, payload)
    await loadRecruitments(teamForm.teamId, selectedRecruitmentId.value)
    const current = recruitments.value.find((item) => Number(item.recruitmentId) === Number(selectedRecruitmentId.value))
    const nextSlot = current?.slots?.find((slot) => Number(slot.slotId) === Number(savedSlot.slotId))
    if (nextSlot) selectSlot(nextSlot)
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

function requestSlotDelete() {
  pendingSlotDelete.value = true
  error.value = ''
  saved.value = ''
}

function cancelSlotDelete() {
  pendingSlotDelete.value = false
}

async function deleteSlot() {
  if (!canEditTeam.value || !slotForm.slotId) return
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.deleteSlot(slotForm.slotId)
    pendingSlotDelete.value = false
    await loadRecruitments(teamForm.teamId, selectedRecruitmentId.value)
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function decideApplication(application, decision) {
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.decideTeamApplication(
      application.applicationId,
      decision,
      decision === 'ACCEPTED' ? '팀 합류 승인' : '팀 구성상 이번에는 함께하기 어렵습니다.'
    )
    await loadTeams(teamForm.teamId)
    closeApplicantProfile()
    saved.value = decision === 'ACCEPTED' ? '지원을 수락했습니다.' : '지원을 거절했습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function decideInvitation(invitation, decision) {
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.decideTeamInvitation(invitation.invitationId, decision)
    myInvitations.value = await slateApi.myTeamInvitations()
    await loadTeams(selectedTeamId.value)
    saved.value = decision === 'ACCEPTED' ? '초대를 수락했습니다.' : '초대를 거절했습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function saveMember(member) {
  if (isTeamEnded.value) {
    error.value = '종료된 팀은 팀원 정보를 수정할 수 없습니다.'
    return
  }
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.updateTeamMember(teamForm.teamId, member.userId, {
      teamRole: member.teamRole,
      status: member.status
    })
    await loadTeams(teamForm.teamId)
    saved.value = '팀원 권한을 저장했습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function kickMember(member) {
  if (isTeamEnded.value) {
    error.value = '종료된 팀은 팀원을 제외할 수 없습니다.'
    return
  }
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.updateTeamMember(teamForm.teamId, member.userId, {
      teamRole: member.teamRole,
      status: 'KICKED'
    })
    await loadTeams(teamForm.teamId)
    saved.value = '팀원을 제외했습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function leaveTeam() {
  if (isTeamEnded.value) {
    error.value = '종료된 팀에서는 나가기 처리를 할 수 없습니다.'
    return
  }
  if (!teamForm.teamId) return
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.leaveTeam(teamForm.teamId)
    saved.value = '팀에서 나갔습니다.'
    await router.push({ name: 'teams' })
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

function requestLeaderTransfer() {
  if (!teamForm.teamId || !isTeamLeader.value || isTeamEnded.value) return
  if (!Number(transferForm.newLeaderUserId)) {
    error.value = '새 팀장을 선택해주세요.'
    saved.value = ''
    return
  }
  pendingLeaderTransfer.value = true
  error.value = ''
  saved.value = ''
}

function cancelLeaderTransfer() {
  pendingLeaderTransfer.value = false
}

function requestTeamClose() {
  pendingTeamClose.value = true
  teamCloseConfirmMode.value = window.matchMedia('(max-width: 700px)').matches ? 'modal' : 'popover'
  error.value = ''
  saved.value = ''
}

function cancelTeamClose() {
  pendingTeamClose.value = false
  teamCloseConfirmMode.value = 'popover'
}

async function transferLeader() {
  if (!teamForm.teamId || !isTeamLeader.value || isTeamEnded.value) return
  const newLeaderUserId = Number(transferForm.newLeaderUserId)
  if (!newLeaderUserId) {
    error.value = '새 팀장을 선택해주세요.'
    return
  }
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.transferTeamLeader(teamForm.teamId, {
      newLeaderUserId,
      reason: transferForm.reason.trim() || null
    })
    await loadTeams(teamForm.teamId)
    saved.value = '팀장 권한을 이전했습니다.'
    pendingLeaderTransfer.value = false
    resetTransferForm()
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function closeTeam() {
  if (!canCloseTeam.value) return
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    if (!closureForm.reason.trim()) throw new Error('종료 사유를 입력해주세요.')
    const endType = closureForm.endType
    const closed = await slateApi.closeTeam(teamForm.teamId, {
      endType,
      reason: closureForm.reason.trim()
    })
    await loadTeams(closed.teamId)
    saved.value = endType === 'NORMAL' ? '팀을 정상 종료했습니다.' : '팀을 해체했습니다.'
    pendingTeamClose.value = false
    teamCloseConfirmMode.value = 'popover'
    resetClosureForm()
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function reopenTeam() {
  if (!teamForm.teamId || !isTeamLeader.value || !isTeamEnded.value) return
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    if (!reopenForm.reason.trim()) throw new Error('재개 사유를 입력해주세요.')
    const restoreSnapshotYn = 'Y'
    const reopened = await slateApi.reopenTeam(teamForm.teamId, {
      closureSnapshotId: closureSnapshots.value[0]?.closureSnapshotId ? Number(closureSnapshots.value[0].closureSnapshotId) : null,
      restoreSnapshotYn,
      reason: reopenForm.reason.trim()
    })
    await loadTeams(reopened.teamId)
    saved.value = '팀을 복구했습니다.'
    resetReopenForm()
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function savePlan() {
  if (!canEditTeam.value) {
    error.value = '종료된 팀은 계획을 수정할 수 없습니다.'
    return
  }
  if (!teamForm.teamId) {
    error.value = '팀을 먼저 선택해주세요.'
    return
  }
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    const shouldReturnToSchedule = isTeamPlanCreateRoute.value || isTeamPlanEditRoute.value
    const payload = buildPlanPayload()
    if (!payload.title) throw new Error('계획 제목을 입력해주세요.')
    const savedPlan = planForm.planItemId
      ? await slateApi.updateTeamPlan(planForm.planItemId, payload)
      : await slateApi.createTeamPlan(teamForm.teamId, payload)
    await loadTeamWorkspace(teamForm.teamId)
    const nextPlan = plans.value.find((plan) => Number(plan.planItemId) === Number(savedPlan.planItemId))
    if (nextPlan) selectPlan(nextPlan)
    saved.value = '팀 계획을 저장했습니다.'
    if (shouldReturnToSchedule) {
      await router.push({
        name: 'teams-plans',
        params: { teamId: teamForm.teamId },
        hash: '#team-plan-schedule'
      })
    }
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function updatePlanStatus(plan, status) {
  if (isTeamEnded.value) {
    error.value = '종료된 팀은 계획 상태를 바꿀 수 없습니다.'
    return
  }
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.updateTeamPlanStatus(plan.planItemId, status)
    await loadTeamWorkspace(teamForm.teamId)
    saved.value = '계획 상태를 저장했습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

watch([() => recruitmentForm.deadlineAt, () => recruitmentForm.status], () => {
  if (recruitmentForm.status === 'OPEN' && isPastDeadline(recruitmentForm.deadlineAt)) {
    recruitmentForm.status = 'CLOSED'
  }
})

watch([() => props.currentUser?.userId, () => route.name, () => route.params.teamId, () => route.params.planItemId], () => {
  if (props.currentUser) load()
  else {
    clearPendingActions()
    teams.value = []
    recruitments.value = []
    applications.value = []
    teamInvitations.value = []
    myInvitations.value = []
    plans.value = []
    closureSnapshots.value = []
    selectedTeamId.value = null
    resetTransferForm()
    resetReopenForm()
  }
}, { immediate: true })

watch([isPlanScheduleView, () => teamForm.teamId], scrollToPlanSchedule, { flush: 'post' })

onBeforeUnmount(() => {
  if (teamImagePreview.value) URL.revokeObjectURL(teamImagePreview.value)
})
</script>

<template>
  <section v-if="!props.currentUser" class="login-panel">
    <h2>팀 목록</h2>
    <p>팀 목록과 모집 관리는 로그인 후 사용할 수 있습니다.</p>
    <RouterLink class="primary-button inline" :to="{ name: 'login', query: { redirect: route.fullPath } }">로그인</RouterLink>
  </section>

  <section v-else class="team-page">
    <section v-if="isTeamListRoute || isTeamInvitationsRoute" class="team-dashboard-head">
      <div class="team-title-actions">
        <div>
          <span class="eyebrow">Team workspace</span>
          <h2>{{ isTeamInvitationsRoute ? '받은 팀 초대' : '내 팀 요약' }}</h2>
        </div>
        <button v-if="isTeamListRoute" class="team-create-button" type="button" @click="router.push({ name: 'teams-new' })">＋ 팀 만들기</button>
        <RouterLink v-else class="ghost-button inline" :to="{ name: 'teams' }">팀 목록</RouterLink>
      </div>
      <div v-if="isTeamListRoute" class="team-overview-strip" aria-label="팀 요약">
        <article class="team-overview-compact">
          <span>진행 중</span>
          <strong>{{ activeDisplayTeams.length }}</strong>
          <small>참여 또는 운영 중인 팀</small>
        </article>
        <article class="team-overview-compact">
          <span>종료됨</span>
          <strong>{{ endedDisplayTeams.length }}</strong>
          <small>완료된 팀 기록</small>
        </article>
        <button class="team-next-action-card" type="button" @click="goNextTeamAction">
          <span>해야 할 작업</span>
          <strong>{{ nextTeamAction.label }}</strong>
          <small>{{ nextTeamAction.title }} · {{ nextTeamAction.detail }}</small>
        </button>
      </div>
      <p v-if="error" class="error-text">{{ error }}</p>
      <p v-if="saved && !isTeamRecruitmentsRoute" class="notice-text">{{ saved }}</p>
    </section>

    <section v-if="isTeamInvitationsRoute" class="team-invitation-page">
      <section class="form-panel team-invitation-hero">
        <div>
          <span class="eyebrow">Invitations</span>
          <h2>응답이 필요한 초대</h2>
          <p>팀에서 보낸 합류 초대를 확인하고 수락 또는 거절할 수 있습니다.</p>
        </div>
        <div class="team-request-summary-strip" aria-label="받은 초대 요약">
          <article>
            <span>대기 중</span>
            <strong>{{ pendingMyInvitations.length }}건</strong>
          </article>
          <article>
            <span>처리 완료</span>
            <strong>{{ decidedMyInvitations.length }}건</strong>
          </article>
        </div>
      </section>

      <section class="team-invitation-workspace">
        <article
          v-for="invitation in pendingMyInvitations"
          :key="`my-invitation-${invitation.invitationId}`"
          class="list-panel team-invitation-card"
        >
          <div class="team-invitation-copy">
            <span class="team-status-badge">초대 대기</span>
            <strong>{{ invitation.teamName || '팀 이름 없음' }}</strong>
            <p>{{ invitation.message || '초대 메시지가 없습니다.' }}</p>
            <div class="subline">
              <span>{{ invitation.recruitmentTitle || '모집 공고' }}</span>
              <span>{{ invitation.roleName || '역할 정보 없음' }}</span>
              <span>{{ invitation.inviterNickname || '초대자 정보 없음' }}</span>
            </div>
          </div>
          <div class="row-actions team-invitation-actions">
            <button class="primary-button inline" type="button" :disabled="saving" @click="decideInvitation(invitation, 'ACCEPTED')">수락</button>
            <button class="ghost-button danger" type="button" :disabled="saving" @click="decideInvitation(invitation, 'REJECTED')">거절</button>
          </div>
        </article>
        <div v-if="!loading && pendingMyInvitations.length === 0" class="team-empty-state team-invitation-empty">
          <strong>대기 중인 팀 초대가 없습니다.</strong>
          <p>새 초대를 받으면 이 화면에서 바로 응답할 수 있습니다.</p>
          <RouterLink :to="{ name: 'matching-teams' }">팀 둘러보기</RouterLink>
        </div>
      </section>

      <section v-if="decidedMyInvitations.length" class="form-panel team-invitation-history">
        <div class="team-recruitment-panel-head">
          <div>
            <span class="eyebrow">History</span>
            <h3>최근 처리한 초대</h3>
          </div>
        </div>
        <article v-for="invitation in decidedMyInvitations" :key="`decided-invitation-${invitation.invitationId}`" class="team-request-row">
          <div>
            <strong>{{ invitation.teamName || '팀 이름 없음' }}</strong>
            <div class="subline">
              <span>{{ invitation.recruitmentTitle || '모집 공고' }}</span>
              <span>{{ invitation.roleName || '역할 정보 없음' }}</span>
              <span>{{ requestStatusLabel(invitation.status) }}</span>
            </div>
          </div>
        </article>
      </section>
    </section>

    <section
      v-if="isTeamListRoute || isTeamDetailRoute"
      class="team-dashboard-grid"
      :class="{ 'team-list-route-grid': isTeamListRoute, 'team-detail-route-grid': isTeamDetailRoute }"
    >
      <div v-if="isTeamListRoute" class="team-list-stack">
        <section class="team-list-card">
          <header>
            <h3>내 팀 목록</h3>
            <span>{{ activeDisplayTeams.length }}개</span>
          </header>
          <article
            v-for="team in activeDisplayTeams"
            :key="team.teamId"
            class="team-list-item"
            :class="{ selected: dashboardTeam === team || Number(selectedTeamId) === Number(team.teamId) }"
            @click="selectTeamCard(team)"
          >
            <button
              v-if="hasTeamImage(team)"
              class="team-list-image-button"
              type="button"
              :aria-label="`${team.name} 대표 이미지 확대`"
              @click.stop="openImagePreview(teamImage(team), `${team.name} 대표 이미지`)"
            >
              <img :src="teamImage(team)" alt="" @error="handleTeamImageError(team)">
            </button>
            <img v-else class="team-image-placeholder" :src="defaultTeamImage" alt="" aria-hidden="true">
            <div class="team-list-copy">
              <strong>{{ team.name }}</strong>
              <small>{{ teamGenreText(team) }} · {{ teamRegionText(team) }}</small>
              <small>{{ latestTeamScheduleText(team) }}</small>
              <div>
                <em>{{ teamStatusText(team) }}</em>
                <span>{{ team.currentMemberCount ?? (team.members || []).filter((member) => member.status === 'ACTIVE').length }}/{{ team.maxMemberCount ?? '정보 없음' }}명</span>
              </div>
            </div>
            <div class="team-list-progress" :style="{ '--score': `${teamListProgress(team)}%` }">
              <strong>{{ teamListProgress(team) }}%</strong>
            </div>
          </article>
          <div v-if="!loading && !activeDisplayTeams.length" class="team-empty-state">
            <strong>진행 중인 팀이 없습니다.</strong>
            <p>새 팀을 만들거나 매칭에서 모집 중인 팀을 찾아보세요.</p>
            <div>
              <button type="button" @click="router.push({ name: 'teams-new' })">팀 만들기</button>
              <RouterLink :to="{ name: 'matching-teams' }">팀 찾기</RouterLink>
            </div>
          </div>
        </section>
        <section class="team-list-card team-ended-list-card">
          <header>
            <h3>종료된 팀</h3>
            <select v-model="endedTeamSort" aria-label="종료된 팀 정렬">
              <option value="latest">최신순</option>
              <option value="oldest">오래된순</option>
            </select>
          </header>
          <article
            v-for="team in endedDisplayTeams"
            :key="`ended-${team.teamId}`"
            class="team-list-item ended"
            @click="selectTeamCard(team)"
          >
            <button
              v-if="hasTeamImage(team)"
              class="team-list-image-button"
              type="button"
              :aria-label="`${team.name} 대표 이미지 확대`"
              @click.stop="openImagePreview(teamImage(team), `${team.name} 대표 이미지`)"
            >
              <img :src="teamImage(team)" alt="" @error="handleTeamImageError(team)">
            </button>
            <img v-else class="team-image-placeholder" :src="defaultTeamImage" alt="" aria-hidden="true">
            <div class="team-list-copy">
              <strong>{{ team.name }}</strong>
              <small>{{ teamGenreText(team) }} · {{ teamRegionText(team) }}</small>
              <small>{{ formatKoreanDate(team.endedAt || team.updatedAt || team.lastActiveAt || team.createdAt) }} 종료</small>
              <em :class="{ dissolved: team.endType === 'DISSOLUTION' }">{{ teamEndedMessage(team) }}</em>
            </div>
          </article>
          <p v-if="!loading && !endedDisplayTeams.length" class="muted team-ended-empty">종료된 팀이 없습니다.</p>
        </section>
      </div>

      <div v-if="isTeamDetailRoute && selectedTeam" class="team-main-column">
        <section class="team-summary-card" :class="{ 'team-summary-card-member': !canManageTeam }">
          <div class="team-summary-copy">
            <div class="team-summary-kicker">
              <em class="team-status-badge" :class="{ dissolved: dashboardTeam?.endType === 'DISSOLUTION' }">{{ teamStatusText(dashboardTeam) }}</em>
              <span>{{ teamGenreText(dashboardTeam) }}</span>
            </div>
            <div class="team-summary-title-row">
              <div class="team-summary-actions">
                <div class="team-summary-location-action">
                  <button type="button" @click="router.push({ name: 'teams-locations', params: { teamId: dashboardTeam.teamId } })">
                    AI 로케이션 탐색
                  </button>
                </div>
                <div class="team-summary-control-actions">
                  <button type="button" @click="router.push({ name: 'teams' })">팀 목록</button>
                  <button v-if="canManageTeam" type="button" @click="openTeamPanel('team')">팀 정보 수정</button>
                </div>
              </div>
              <h2>{{ dashboardTeam?.name }}</h2>
            </div>
            <div class="team-summary-meta">
              <button
                v-if="leaderProfileId"
                class="team-summary-meta-pill"
                type="button"
                @click="openLeaderProfile"
              >
                리더 {{ leaderMember?.nickname || dashboardTeam?.leaderNickname || '리더 정보 없음' }}
              </button>
              <span v-else class="team-summary-meta-pill">리더 {{ leaderMember?.nickname || dashboardTeam?.leaderNickname || '리더 정보 없음' }}</span>
              <span class="team-summary-meta-pill">지역 {{ teamRegionText(dashboardTeam) }}</span>
              <span class="team-summary-meta-pill">정원 {{ dashboardMembers }}명 / 최대 {{ dashboardMaxMembers ?? '정보 없음' }}명</span>
            </div>
            <p class="team-description-text">{{ visibleTeamDescription }}</p>
            <button
              v-if="isTeamDescriptionLong"
              class="team-description-toggle"
              type="button"
              @click="teamDescriptionExpanded = !teamDescriptionExpanded"
            >
              {{ teamDescriptionExpanded ? '접기' : '더 보기' }}
            </button>
          </div>
          <div class="team-summary-media">
            <button
              v-if="hasTeamImage(dashboardTeam)"
              class="team-summary-image-button"
              type="button"
              :aria-label="`${dashboardTeam?.name} 대표 이미지 확대`"
              @click="openImagePreview(teamImage(dashboardTeam), `${dashboardTeam?.name} 대표 이미지`)"
            >
              <img :src="teamImage(dashboardTeam)" alt="" class="team-summary-image" @error="handleTeamImageError(dashboardTeam)">
            </button>
            <img v-else class="team-summary-image team-image-placeholder" :src="defaultTeamImage" alt="" aria-hidden="true">
          </div>
        </section>

        <section class="team-detail-stats" aria-label="팀 핵심 정보">
          <button class="team-detail-stat-card" type="button" @click="openTeamPanel('members')">
            <span>멤버</span>
            <strong class="team-member-count">
              {{ dashboardMembers }}명
              <span>({{ dashboardMaxMembers !== null ? `최대 ${dashboardMaxMembers}명` : '최대 정보 없음' }})</span>
            </strong>
            <small>최근 합류 {{ formatKoreanDate(latestJoinedAt, '정보 없음') }}</small>
            <i aria-hidden="true">›</i>
          </button>
          <button class="team-detail-stat-card" type="button" @click="openTeamPanel('recruitment')">
            <span>모집 공고</span>
            <strong>{{ openRecruitments.length }}건</strong>
            <small>마감 임박 {{ closingSoonRecruitments.length }}건 · 구인 공고 {{ dashboardSlots.length }}개</small>
            <i aria-hidden="true">›</i>
          </button>
          <button
            v-if="canManageTeam && !isTeamEnded"
            class="team-detail-stat-card"
            type="button"
            @click="openTeamPanel('requests')"
          >
            <span>지원/초대 현황</span>
            <strong>{{ pendingApplications.length + pendingTeamInvitations.length }}건</strong>
            <small>지원 대기 {{ pendingApplications.length }}건 · 초대 대기 {{ pendingTeamInvitations.length }}건</small>
            <i aria-hidden="true">›</i>
          </button>
          <button class="team-detail-stat-card" type="button" @click="openTeamPlans('schedule')">
            <span>다음 일정</span>
            <strong>{{ nextPlan ? nextPlanDayLabel(nextPlan) : '없음' }}</strong>
            <small>{{ nextPlan?.title || '등록된 다음 일정이 없습니다.' }}</small>
            <i aria-hidden="true">›</i>
          </button>
        </section>

        <section class="team-lower-grid">
          <article class="team-progress-card">
            <h3>계획 진행률</h3>
            <div class="team-progress-body">
              <div class="team-progress-ring" :style="{ '--score': `${dashboardProgress}%` }">
                <strong>{{ dashboardProgress }}%</strong>
                <small>전체 진행률</small>
              </div>
              <div class="team-progress-next">
                <template v-if="nextPlan">
                  <em>{{ nextPlanDayLabel(nextPlan) }}</em>
                  <p>{{ nextPlan.title }}</p>
                  <small>{{ formatKoreanDate(nextPlan.dueAt) }}</small>
                </template>
                <p v-else>등록된 다음 일정이 없습니다.</p>
              </div>
            </div>
            <div class="team-update-list">
              <strong>최근 업데이트</strong>
              <span v-for="plan in recentPlanUpdates" :key="`update-${plan.planItemId}`">
                {{ plan.status === 'DONE' ? '◎' : '○' }} {{ plan.title }}
                <em>{{ formatShortDate(plan.updatedAt || plan.createdAt) }}</em>
              </span>
              <p v-if="!recentPlanUpdates.length" class="muted">최근 업데이트가 없습니다.</p>
            </div>
          </article>

          <article class="team-schedule-card">
            <header>
              <h3>일정 안내</h3>
              <div class="team-schedule-actions">
                <button v-if="canEditTeam" class="primary-button" type="button" @click="openNewPlanPage">새 계획</button>
                <button type="button" @click="openTeamPlans('schedule')">전체 일정 보기 ›</button>
              </div>
            </header>
            <div v-if="plans.length" class="team-point-schedule">
              <section v-if="planTimeline.groups.length" class="team-point-axis">
                <div class="team-point-range">
                  <span>{{ formatKoreanDate(planTimeline.min) }}</span>
                  <span v-if="planTimeline.max !== planTimeline.min">{{ formatKoreanDate(planTimeline.max) }}</span>
                </div>
                <div
                  class="team-schedule-strip"
                  :class="{ dragging: scheduleDrag.active }"
                  :style="{ '--schedule-line-width': scheduleLineWidth }"
                  aria-label="일정 목록"
                  @pointerdown="startScheduleDrag"
                  @pointermove="moveScheduleDrag"
                  @pointerup="endScheduleDrag"
                  @pointerleave="endScheduleDrag"
                  @pointercancel="endScheduleDrag"
                >
                  <article
                    v-for="group in planTimeline.groups"
                    :key="`schedule-day-${group.timestamp}`"
                    class="team-schedule-day-card"
                  >
                    <time>{{ formatKoreanDate(group.timestamp) }}</time>
                    <div v-for="plan in group.plans" :key="`schedule-${plan.planItemId}`" class="team-point-event">
                      <strong>{{ plan.title }}</strong>
                      <em>{{ planStatusLabel(plan.status) }}</em>
                    </div>
                  </article>
                </div>
              </section>
              <section v-if="undatedPlans.length" class="team-undated-plans">
                <h4>마감일 미정</h4>
                <article v-for="plan in undatedPlans" :key="`undated-${plan.planItemId}`">
                  <strong>{{ plan.title }}</strong>
                  <em>{{ planStatusLabel(plan.status) }}</em>
                </article>
              </section>
            </div>
            <p v-else class="team-schedule-empty muted">등록된 계획 일정이 없습니다.</p>
          </article>
        </section>
        <div v-if="canCloseTeam" class="team-detail-close-actions">
          <button class="ghost-button danger" type="button" @click="openTeamPanel('closure')">팀 작업 종료하기</button>
        </div>
      </div>
      <section v-else-if="isTeamDetailRoute" class="form-panel team-route-state">
        <p v-if="loading" class="muted">팀 정보를 불러오는 중입니다.</p>
        <p v-else class="error-text">{{ error || '팀을 찾을 수 없거나 접근 권한이 없습니다.' }}</p>
        <button class="ghost-button" type="button" @click="router.push({ name: 'teams' })">팀 목록으로</button>
      </section>
    </section>

    <section v-if="showTeamEditor" class="team-editor-shell team-admin-grid team-route-editor">
    <aside v-if="isTeamListRoute" class="tool-surface team-sidebar">
      <div class="form-head compact-head">
        <div>
          <span class="eyebrow">Teams</span>
          <h2>내 팀</h2>
        </div>
        <button class="ghost-button" type="button" @click="newTeam">새 팀</button>
      </div>
      <button
        v-for="team in teams"
        :key="team.teamId"
        class="select-row"
        :class="{ active: Number(team.teamId) === Number(selectedTeamId) }"
        type="button"
        @click="selectTeam(team)"
      >
        <strong>{{ team.name }}</strong>
        <span>{{ team.status }} · {{ team.publicRegionName || '지역 무관' }} · {{ team.currentMemberCount }}/{{ team.maxMemberCount }}</span>
      </button>
      <p v-if="!loading && teams.length === 0" class="muted">저장된 팀이 없습니다.</p>
    </aside>

    <div class="stack">
      <section
        v-if="!showTeamForm && !isTeamRecruitmentsRoute && !isTeamRequestsRoute"
        class="form-panel team-route-toolbar"
        :class="{ 'team-close-merged-panel': isTeamCloseRoute, 'team-plan-create-merged-panel': isTeamPlanCreateRoute || isTeamPlanEditRoute, 'team-plan-merged-panel': isTeamPlansRoute }"
      >
        <div class="team-route-toolbar-main">
          <div>
            <span class="eyebrow">{{ isTeamCloseRoute ? 'Team Closure' : isTeamRequestsRoute ? 'Team Requests' : isTeamPlansRoute ? 'Team Plans' : isTeamPlanCreateRoute ? 'New Plan' : isTeamPlanEditRoute ? 'Edit Plan' : 'Teams' }}</span>
            <h2>{{ isTeamCreateRoute ? '팀 생성' : isTeamEditRoute ? '팀 정보 수정' : isTeamCloseRoute ? '팀 종료' : isTeamMembersRoute ? '팀원 관리' : isTeamRecruitmentsRoute ? '모집 공고' : isTeamRequestsRoute ? '지원/초대 현황' : isTeamPlansRoute ? '전체 일정' : isTeamPlanCreateRoute ? '새 계획' : isTeamPlanEditRoute ? '계획 수정' : '팀 계획' }}</h2>
          </div>
          <div class="row-actions" :class="{ 'team-recruitment-route-actions': isTeamRecruitmentsRoute }">
            <button v-if="!isTeamCloseRoute && !isTeamPlanCreateRoute && !isTeamPlanEditRoute && !isTeamPlansRoute && !isTeamMembersRoute && !isTeamRecruitmentsRoute" class="ghost-button" type="button" @click="router.push({ name: 'teams' })">팀 목록</button>
            <button v-if="selectedTeam" class="ghost-button" type="button" @click="router.push({ name: 'teams-detail', params: { teamId: selectedTeam.teamId } })">
              {{ isTeamCloseRoute || isTeamMembersRoute || isTeamRecruitmentsRoute ? '돌아가기' : (isTeamPlansRoute || isTeamPlanCreateRoute || isTeamPlanEditRoute) ? '팀 정보' : '상세' }}
            </button>
            <button v-if="isTeamPlansRoute && canEditTeam" class="primary-button" type="button" @click="openNewPlanPage">
              새 계획
            </button>
            <button v-if="(isTeamPlanCreateRoute || isTeamPlanEditRoute) && teamForm.teamId" class="ghost-button" type="button" @click="router.push({ name: 'teams-plans', params: { teamId: teamForm.teamId } })">
              전체 일정
            </button>
          </div>
        </div>
        <div v-if="!routeAccessDenied && !routeTeamMissing && isTeamCloseRoute && teamForm.teamId" class="team-close-panel">
          <div v-if="!isTeamEnded && canCloseTeam" class="form-grid">
            <label class="field">
              <span>종료 유형</span>
              <select v-model="closureForm.endType">
                <option v-for="item in codeOptions('TEAM_END_TYPE')" :key="item.code" :value="item.code">
                  {{ item.code === 'DISSOLUTION' || item.displayName === '해체' ? '팀 해체' : item.displayName }}
                </option>
              </select>
            </label>
            <label class="field wide">
              <span>종료 사유</span>
              <textarea v-model="closureForm.reason" rows="3" maxlength="1000"></textarea>
            </label>
            <div class="team-close-action">
              <button class="ghost-button danger inline" type="button" :disabled="saving" @click="requestTeamClose">
                팀 작업 종료하기
              </button>
              <div v-if="pendingTeamClose && teamCloseConfirmMode === 'popover'" class="team-close-popover" role="dialog" aria-modal="false">
                <span>팀 작업을 종료하시겠습니까?</span>
                <div class="row-actions">
                  <button class="ghost-button" type="button" :disabled="saving" @click="cancelTeamClose">취소</button>
                  <button class="ghost-button danger" type="button" :disabled="saving" @click="closeTeam">작업 종료</button>
                </div>
              </div>
            </div>
          </div>
          <p v-else-if="!isTeamEnded" class="muted">팀 종료는 팀장만 실행할 수 있습니다.</p>
          <div v-else class="team-reopen-panel">
            <p class="notice-text" :class="{ danger: selectedTeam?.endType === 'DISSOLUTION' }">{{ teamEndedMessage(selectedTeam) }}</p>
            <div v-if="isTeamLeader" class="form-grid">
              <label class="field wide">
                <span>복구 사유</span>
                <textarea v-model="reopenForm.reason" rows="3" maxlength="1000"></textarea>
              </label>
              <button class="primary-button inline" type="button" :disabled="saving" @click="reopenTeam">
                팀 복구
              </button>
            </div>
          </div>
        </div>
        <form
          v-if="showPlanFormPanel"
          id="team-plan-form"
          class="form-grid team-plan-create-form"
          @submit.prevent="savePlan"
        >
          <div class="form-head compact-head team-plan-form-head">
            <div>
              <span class="eyebrow">Team Plan</span>
              <h3>{{ planFormTitle }}</h3>
            </div>
          </div>
          <label class="field wide">
            <span>계획 제목</span>
            <input v-model="planForm.title" maxlength="150">
          </label>
          <label class="field">
            <span>담당자</span>
            <select v-model="planForm.assigneeUserId">
              <option value="">공통</option>
              <option v-for="member in activeMembers" :key="member.userId" :value="member.userId">
                {{ member.nickname }}
              </option>
            </select>
          </label>
          <label class="field">
            <span>역할 기준</span>
            <select v-model="planForm.roleId">
              <option value="">없음</option>
              <option v-for="role in roleOptions" :key="role.roleId" :value="role.roleId">
                {{ role.name }}
              </option>
            </select>
          </label>
          <label class="field">
            <span>마감일</span>
            <input
              v-model="planForm.dueAt"
              type="datetime-local"
              @click="openDateTimePicker"
            >
          </label>
          <label class="field">
            <span>상태</span>
            <select v-model="planForm.status">
              <option v-for="item in codeOptions('PLAN_STATUS')" :key="item.code" :value="item.code">
                {{ item.displayName }}
              </option>
            </select>
          </label>
          <label class="field wide">
            <span>설명</span>
            <textarea v-model="planForm.description" rows="4" maxlength="1000"></textarea>
          </label>
          <div class="form-submit-row">
            <button class="ghost-button" type="button" @click="cancelPlanForm">취소</button>
            <button class="primary-button" type="submit" :disabled="saving">{{ planForm.planItemId ? '계획 저장' : '계획 생성' }}</button>
          </div>
        </form>
        <p v-else-if="!routeAccessDenied && !routeTeamMissing && (isTeamPlanCreateRoute || isTeamPlanEditRoute) && teamForm.teamId" class="muted">종료된 팀에서는 계획을 만들거나 수정할 수 없습니다.</p>
        <div v-if="!routeAccessDenied && !routeTeamMissing && isTeamPlansRoute && teamForm.teamId" class="team-plan-overview-content">
          <p class="team-plan-page-note">등록된 계획을 마감일 순서로 확인하고 상태를 관리합니다.</p>

          <div class="team-plan-summary-grid">
            <article class="team-plan-summary-card">
              <span>전체</span>
              <strong>{{ planSummary.total }}개</strong>
              <small>등록된 계획</small>
            </article>
            <article class="team-plan-summary-card">
              <span>진행 중</span>
              <strong>{{ planSummary.inProgress }}개</strong>
              <small>현재 작업 중</small>
            </article>
            <article class="team-plan-summary-card">
              <span>완료</span>
              <strong>{{ planSummary.done }}개</strong>
              <small>{{ dashboardProgress }}% 완료</small>
            </article>
            <article class="team-plan-summary-card">
              <span>다음 일정</span>
              <strong>{{ nextPlan ? nextPlanDayLabel(nextPlan) : '없음' }}</strong>
              <small>{{ nextPlan?.title || '등록된 다음 일정이 없습니다.' }}</small>
            </article>
          </div>

          <div id="team-plan-schedule" class="team-plan-schedule-board" :class="{ 'team-plan-schedule-view': isPlanScheduleView }">
            <div class="team-plan-board-head">
              <div>
                <h3>전체 일정</h3>
                <p>마감일이 없는 계획은 목록 하단에 표시됩니다.</p>
              </div>
              <span>{{ planSummary.total }}개</span>
            </div>
            <div v-if="planScheduleRows.length" class="team-plan-timeline-list">
              <article v-for="plan in planScheduleRows" :key="`status-${plan.planItemId}`" class="team-plan-row">
                <time>{{ formatKoreanDate(plan.dueAt, '마감 없음') }}</time>
                <div class="team-plan-row-body">
                  <strong>{{ plan.title }}</strong>
                  <p>{{ plan.description || '설명 없음' }}</p>
                  <div class="subline">
                    <span>{{ plan.assigneeNickname || plan.roleName || '공통' }}</span>
                    <span>{{ plan.dueAt || '마감 없음' }}</span>
                  </div>
                </div>
                <div class="team-plan-status-actions">
                  <span class="plan-status-pill">{{ planStatusLabel(plan.status) }}</span>
                  <select
                    v-if="canEditTeam"
                    class="team-plan-status-select"
                    :value="plan.status"
                    :disabled="saving || isTeamEnded"
                    @change="updatePlanStatus(plan, $event.target.value)"
                  >
                    <option v-for="item in codeOptions('PLAN_STATUS')" :key="item.code" :value="item.code">
                      {{ item.displayName }}
                    </option>
                  </select>
                  <button
                    v-if="canEditTeam"
                    class="ghost-button team-plan-edit-button"
                    type="button"
                    :disabled="saving || isTeamEnded"
                    @click="editPlan(plan)"
                  >
                    수정
                  </button>
                </div>
              </article>
            </div>
            <p v-else class="muted">등록된 계획이 없습니다.</p>
          </div>
        </div>
      </section>
      <p v-if="error" class="error-text">{{ error }}</p>
      <p v-if="saved && !isTeamRecruitmentsRoute" class="notice-text">{{ saved }}</p>

      <section v-if="routeTeamMissing" class="form-panel team-route-state">
        <p class="error-text">{{ error || '팀을 찾을 수 없거나 접근 권한이 없습니다.' }}</p>
        <button class="ghost-button" type="button" @click="router.push({ name: 'teams' })">팀 목록으로</button>
      </section>

      <section v-else-if="routeAccessDenied" class="form-panel team-route-state">
        <p class="error-text">이 팀의 관리 화면에 접근할 권한이 없습니다.</p>
        <button class="ghost-button" type="button" @click="router.push({ name: 'teams-detail', params: { teamId: selectedTeam.teamId } })">팀 정보로</button>
      </section>

      <form v-if="!routeAccessDenied && !routeTeamMissing && showTeamForm && (isTeamCreateRoute || selectedTeam)" class="form-panel team-info-form" @submit.prevent="saveTeam">
        <div class="form-head">
          <div>
            <span class="eyebrow">Teams</span>
            <h2>{{ teamForm.teamId ? '팀 정보 수정' : '팀 생성' }}</h2>
          </div>
          <div class="row-actions">
            <button class="ghost-button" type="button" @click="router.push({ name: 'teams' })">팀 목록</button>
            <button
              v-if="teamForm.teamId && isTeamLeader && !isTeamEnded"
              class="ghost-button danger"
              type="button"
              :disabled="saving || loading"
              @click="requestTeamDelete"
            >
              삭제
            </button>
            <button class="primary-button" type="submit" :disabled="saving || loading || (teamForm.teamId && isTeamEnded)">
              {{ saving ? '저장 중' : '팀 저장' }}
            </button>
          </div>
        </div>

        <div class="form-grid">
          <label class="field team-status-field">
            <span>상태</span>
            <select v-model="teamForm.status" :disabled="isTeamEnded">
              <option v-for="item in teamStatusFormOptions" :key="item.code" :value="item.code">
                {{ item.displayName }}
              </option>
            </select>
          </label>
          <label class="field team-name-field">
            <span>팀 이름</span>
            <input v-model="teamForm.name" maxlength="100" required>
          </label>
          <div class="image-picker team-form-image-row wide">
            <img :src="teamImagePreview || (!teamImageDelete && selectedTeam?.imageUrl) || defaultTeamImage" alt="팀 대표 이미지 미리보기">
            <div>
              <strong>팀 대표 이미지</strong><small>JPEG, PNG, WebP · 최대 5MB</small>
              <label class="ghost-button inline">이미지 선택<input type="file" accept="image/jpeg,image/png,image/webp" @change="selectTeamImage"></label>
              <button v-if="teamImagePreview || selectedTeam?.imageUrl" class="ghost-button danger" type="button" @click="removeTeamImage">이미지 삭제</button>
            </div>
          </div>
          <div class="team-form-paired-fields wide">
            <div class="team-form-field-pair">
              <label class="field">
                <span>지역 설정</span>
                <select v-model="teamForm.regionAnyYn">
                  <option value="N">지역 선택</option>
                  <option value="Y">지역 무관</option>
                </select>
              </label>
              <label class="field">
                <span>지역</span>
                <select v-model="teamForm.regionId" :disabled="teamForm.regionAnyYn === 'Y'">
                  <option v-for="region in regions" :key="region.regionId" :value="region.regionId">
                    {{ region.publicDisplayName }}
                  </option>
                </select>
              </label>
            </div>
            <div class="team-form-field-pair">
              <label class="field">
                <span>예상 기간</span>
                <select v-model="teamForm.expectedDuration">
                  <option v-for="item in codeOptions('DURATION')" :key="item.code" :value="item.code">
                    {{ item.displayName }}
                  </option>
                </select>
              </label>
              <label class="field">
                <span>최대 인원</span>
                <input v-model.number="teamForm.maxMemberCount" min="1" type="number">
              </label>
            </div>
          </div>
          <label class="field wide team-description-field">
            <span>설명</span>
            <textarea v-model="teamForm.description" rows="4" maxlength="2000" required></textarea>
          </label>
        </div>

        <div class="field wide">
          <span>장르</span>
          <div class="check-grid compact">
            <button
              v-for="genre in genres"
              :key="genre.genreId"
              class="chip-check"
              :class="{ active: teamForm.genreIds.includes(Number(genre.genreId)) }"
              type="button"
              :aria-pressed="teamForm.genreIds.includes(Number(genre.genreId))"
              @click="toggleGenre(genre.genreId)"
            >
              {{ genre.name }}
            </button>
          </div>
        </div>
        <div v-if="pendingTeamDelete" class="confirm-inline danger-confirm">
          <span>팀을 삭제할까요? 모집과 대기 중인 지원/초대가 함께 정리됩니다.</span>
          <button class="ghost-button danger" type="button" :disabled="saving" @click="deleteTeam">
            삭제 확인
          </button>
          <button class="ghost-button" type="button" :disabled="saving" @click="cancelTeamDelete">
            취소
          </button>
        </div>
      </form>

      <div
        v-if="pendingTeamClose && teamCloseConfirmMode === 'modal'"
        class="team-close-modal-backdrop"
        role="presentation"
        @click.self="cancelTeamClose"
      >
        <section class="team-close-modal" role="dialog" aria-modal="true" aria-labelledby="team-close-modal-title">
          <h2 id="team-close-modal-title">팀 작업을 종료하시겠습니까?</h2>
          <div class="row-actions">
            <button class="ghost-button" type="button" :disabled="saving" @click="cancelTeamClose">취소</button>
            <button class="ghost-button danger" type="button" :disabled="saving" @click="closeTeam">작업 종료</button>
          </div>
        </section>
      </div>

      <section v-if="!routeAccessDenied && !routeTeamMissing && isTeamMembersRoute && teamForm.teamId" class="form-panel">
        <div class="form-head">
          <div>
            <span class="eyebrow">Members</span>
            <h2>팀 멤버</h2>
          </div>
          <button v-if="myTeamRole !== 'LEADER' && !isTeamEnded" class="ghost-button" type="button" @click="leaveTeam">팀 나가기</button>
        </div>
        <article v-for="member in selectedTeam?.members || []" :key="member.teamMemberId" class="list-panel">
          <div>
            <strong>{{ member.nickname }}</strong>
            <div class="subline">
              <span>{{ member.email }}</span>
              <span>{{ member.status }}</span>
              <span>{{ member.joinedAt }}</span>
            </div>
          </div>
          <div class="row-actions member-actions">
            <select v-model="member.teamRole" :disabled="isTeamEnded || !isTeamLeader || member.teamRole === 'LEADER' || member.status !== 'ACTIVE'">
              <option v-for="item in codeOptions('TEAM_MEMBER_ROLE')" :key="item.code" :value="item.code">
                {{ item.displayName }}
              </option>
            </select>
            <button
              v-if="isTeamLeader && !isTeamEnded && member.teamRole !== 'LEADER' && member.status === 'ACTIVE'"
              class="ghost-button"
              type="button"
              @click="saveMember(member)"
            >
              저장
            </button>
            <button
              v-if="isTeamLeader && !isTeamEnded && member.teamRole !== 'LEADER' && member.status === 'ACTIVE'"
              class="ghost-button danger"
              type="button"
              @click="kickMember(member)"
            >
              제외
            </button>
          </div>
        </article>
        <div v-if="isTeamLeader && !isTeamEnded && eligibleLeaderMembers.length" class="form-grid">
          <label class="field">
            <span>새 팀장</span>
            <select v-model="transferForm.newLeaderUserId">
              <option v-for="member in eligibleLeaderMembers" :key="member.userId" :value="member.userId">
                {{ member.nickname }} · {{ member.teamRole }}
              </option>
            </select>
          </label>
          <label class="field wide">
            <span>이전 사유</span>
            <textarea v-model="transferForm.reason" rows="2" maxlength="500"></textarea>
          </label>
          <button class="ghost-button inline" type="button" :disabled="saving" @click="requestLeaderTransfer">
            팀장 이전
          </button>
          <div v-if="pendingLeaderTransfer" class="confirm-inline danger-confirm wide">
            <span>팀장 권한을 이전할까요? 이전 후 현재 계정은 부팀장이 됩니다.</span>
            <button class="ghost-button danger" type="button" :disabled="saving" @click="transferLeader">
              이전 확인
            </button>
            <button class="ghost-button" type="button" :disabled="saving" @click="cancelLeaderTransfer">
              취소
            </button>
          </div>
        </div>
      </section>

      <section
        v-if="!routeAccessDenied && !routeTeamMissing && isTeamRequestsRoute && teamForm.teamId && canManageTeam"
        id="team-request-status"
        class="team-request-status-page"
      >
        <section class="form-panel team-recruitment-hero team-request-status-hero">
          <div class="team-recruitment-hero-head">
            <div>
              <span class="eyebrow">Requests</span>
              <h2>{{ selectedTeam?.name }} 지원/초대 현황</h2>
            </div>
            <button v-if="selectedTeam" class="ghost-button" type="button" @click="router.push({ name: 'teams-detail', params: { teamId: selectedTeam.teamId } })">
              돌아가기
            </button>
          </div>
          <div>
            <p>팀 합류 요청과 보낸 초대 중 아직 응답이 필요한 항목을 확인합니다.</p>
          </div>
          <div class="team-request-summary-strip" aria-label="지원/초대 현황 요약">
            <article>
              <span>지원 대기</span>
              <strong>{{ pendingApplications.length }}건</strong>
            </article>
            <article>
              <span>초대 대기</span>
              <strong>{{ pendingTeamInvitations.length }}건</strong>
            </article>
          </div>
        </section>

        <section class="team-request-workspace">
          <section class="form-panel team-request-board">
            <div class="team-recruitment-panel-head">
              <div>
                <span class="eyebrow">Applications</span>
                <h3>지원 대기 목록</h3>
              </div>
              <div class="team-recruitment-panel-actions">
                <span>{{ pendingApplications.length }}건</span>
              </div>
            </div>
            <div class="team-request-list-scroll" aria-label="지원 대기 목록">
              <article
                v-for="application in pendingApplications"
                :key="`application-${application.applicationId}`"
                class="list-panel team-request-row"
              >
                <div>
                  <strong>{{ application.applicantNickname || '지원자' }}</strong>
                  <p>{{ application.message || '지원 메시지가 없습니다.' }}</p>
                  <div class="subline">
                    <span>{{ application.recruitmentTitle || '모집 공고' }}</span>
                    <span>{{ application.roleName || '역할 정보 없음' }}</span>
                    <span>{{ requestStatusLabel(application.status) }}</span>
                  </div>
                </div>
                <div class="row-actions team-request-row-actions">
                  <button
                    class="ghost-button"
                    type="button"
                    :disabled="!application.applicantProfileId"
                    @click="router.push({ name: 'public-profile', params: { profileId: application.applicantProfileId } })"
                  >
                    프로필 보기
                  </button>
                  <button
                    v-if="!isTeamEnded"
                    class="ghost-button"
                    type="button"
                    :disabled="saving"
                    @click="decideApplication(application, 'ACCEPTED')"
                  >
                    수락
                  </button>
                  <button
                    v-if="!isTeamEnded"
                    class="ghost-button danger"
                    type="button"
                    :disabled="saving"
                    @click="decideApplication(application, 'REJECTED')"
                  >
                    거절
                  </button>
                </div>
              </article>
              <p v-if="!pendingApplications.length" class="team-request-empty">대기 중인 지원이 없습니다.</p>
            </div>
          </section>

          <section class="form-panel team-request-board">
            <div class="team-recruitment-panel-head">
              <div>
                <span class="eyebrow">Invitations</span>
                <h3>초대 대기 목록</h3>
              </div>
              <div class="team-recruitment-panel-actions">
                <span>{{ pendingTeamInvitations.length }}건</span>
              </div>
            </div>
            <div class="team-request-list-scroll" aria-label="초대 대기 목록">
              <article
                v-for="invitation in pendingTeamInvitations"
                :key="`invitation-${invitation.invitationId}`"
                class="list-panel team-request-row"
              >
                <div>
                  <strong>{{ invitation.targetNickname || '초대 대상자' }}</strong>
                  <p>{{ invitation.message || '초대 메시지가 없습니다.' }}</p>
                  <div class="subline">
                    <span>{{ invitation.recruitmentTitle || '모집 공고' }}</span>
                    <span>{{ invitation.roleName || '역할 정보 없음' }}</span>
                    <span>{{ requestStatusLabel(invitation.status) }}</span>
                  </div>
                </div>
              </article>
              <p v-if="!pendingTeamInvitations.length" class="team-request-empty">대기 중인 초대가 없습니다.</p>
            </div>
          </section>
        </section>
      </section>

      <section v-if="!routeAccessDenied && !routeTeamMissing && isTeamRecruitmentsRoute && teamForm.teamId" class="team-recruitment-page">
        <section class="form-panel team-recruitment-hero">
          <div class="team-recruitment-hero-head">
            <div>
              <span class="eyebrow">Recruitment</span>
              <h2>{{ selectedTeam?.name }} 모집 공고</h2>
            </div>
            <button v-if="selectedTeam" class="ghost-button" type="button" @click="router.push({ name: 'teams-detail', params: { teamId: selectedTeam.teamId } })">
              돌아가기
            </button>
          </div>
          <div>
            <p>모집 공고를 만들고 필요한 팀원을 찾으세요.</p>
          </div>
          <div class="team-recruitment-summary-strip" aria-label="모집 공고 요약">
            <article>
              <span>전체 공고</span>
              <strong>{{ recruitmentSummary.total }}건</strong>
            </article>
            <article>
              <span>진행 중</span>
              <strong>{{ recruitmentSummary.open }}건</strong>
            </article>
            <article>
              <span>마감 임박</span>
              <strong>{{ recruitmentSummary.closingSoon }}건</strong>
            </article>
            <article>
              <span>구인 공고</span>
              <strong>{{ recruitmentSummary.slots }}개</strong>
            </article>
          </div>
        </section>

        <section class="team-recruitment-workspace">
          <aside class="form-panel team-recruitment-list-panel">
            <div class="team-recruitment-panel-head">
              <div>
                <span class="eyebrow">List</span>
                <h3>모집 공고 목록</h3>
              </div>
              <div class="team-recruitment-panel-actions">
                <span>{{ recruitments.length }}건</span>
                <button
                  v-if="canEditTeam && recruitmentForm.recruitmentId"
                  class="primary-button"
                  type="button"
                  @click="newRecruitment({ preserveSelection: true })"
                >
                  공고 +
                </button>
              </div>
            </div>
            <div class="team-recruitment-list-scroll" aria-label="모집 공고 목록">
              <button
                v-for="recruitment in recruitments"
                :key="recruitment.recruitmentId"
                class="team-recruitment-card"
                :class="{ active: Number(recruitment.recruitmentId) === Number(selectedRecruitmentId) }"
                type="button"
                @click="selectRecruitment(recruitment)"
              >
                <strong>{{ recruitment.title }}</strong>
                <em>{{ recruitmentStatusLabel(recruitment.status) }}</em>
                <small>마감 {{ formatKoreanDate(recruitment.deadlineAt, '미정') }}</small>
                <small>구인 공고 {{ recruitment.slots?.length || 0 }}개 · 시작 {{ formatKoreanDate(recruitment.workStartAt, '미정') }}</small>
              </button>
              <p v-if="!recruitments.length" class="team-request-empty">등록된 모집 공고가 없습니다.</p>
            </div>
          </aside>

          <section class="form-panel team-recruitment-editor-panel">
            <div class="form-head">
              <div>
                <span class="eyebrow">{{ recruitmentForm.recruitmentId ? 'Edit' : 'New' }}</span>
                <h2>{{ recruitmentForm.recruitmentId ? '모집 공고 수정' : '새 모집 공고' }}</h2>
                <p v-if="recruitmentFormError" class="error-text team-recruitment-form-error">{{ recruitmentFormError }}</p>
              </div>
              <div v-if="canEditTeam" class="row-actions">
                <button
                  v-if="recruitmentForm.recruitmentId"
                  class="ghost-button danger"
                  type="button"
                  :disabled="saving"
                  @click="requestRecruitmentDelete"
                >
                  삭제
                </button>
                <button
                  class="primary-button"
                  :class="{ 'ai-recommendation-button': !recruitmentForm.recruitmentId }"
                  type="button"
                  :disabled="saving"
                  @click="saveRecruitment"
                >
                  {{ recruitmentForm.recruitmentId ? '저장' : '모집 공고 생성' }}
                </button>
              </div>
            </div>
            <div class="team-recruitment-editor-scroll">
              <div v-if="pendingRecruitmentDelete" class="confirm-inline danger-confirm">
                <span>모집 공고를 삭제할까요? 하위 구인 공고와 대기 중인 지원/초대가 함께 정리됩니다.</span>
                <button class="ghost-button danger" type="button" :disabled="saving" @click="deleteRecruitment">
                  삭제 확인
                </button>
                <button class="ghost-button" type="button" :disabled="saving" @click="cancelRecruitmentDelete">
                  취소
                </button>
              </div>
              <div class="form-grid team-recruitment-form-grid">
                <label class="field wide">
                  <span>모집 제목</span>
                  <input v-model="recruitmentForm.title" maxlength="120">
                </label>
                <label class="field">
                  <span>시작일</span>
                  <input
                    v-model="recruitmentForm.workStartAt"
                    type="datetime-local"
                    :min="recruitmentForm.recruitmentId ? null : recruitmentDateTimeMin"
                    @focus="refreshRecruitmentDateTimeMin"
                  >
                </label>
                <label class="field">
                  <span>마감일</span>
                  <input
                    v-model="recruitmentForm.deadlineAt"
                    type="datetime-local"
                    :min="recruitmentForm.recruitmentId ? null : recruitmentDateTimeMin"
                    @focus="refreshRecruitmentDateTimeMin"
                  >
                </label>
                <label v-if="recruitmentForm.recruitmentId" class="field">
                  <span>상태</span>
                  <select v-model="recruitmentForm.status">
                    <option v-for="item in codeOptions('RECRUITMENT_STATUS')" :key="item.code" :value="item.code">
                      {{ item.displayName }}
                    </option>
                  </select>
                </label>
              </div>
            </div>
          </section>
        </section>

        <section v-if="selectedRecruitmentId" class="team-slot-workspace">
          <aside class="form-panel team-slot-list-panel">
            <div class="team-recruitment-panel-head">
              <div>
                <span class="eyebrow">{{ selectedRecruitment?.title }}</span>
                <h3>구인 공고 목록</h3>
              </div>
              <div class="team-recruitment-panel-actions">
                <span>{{ selectedRecruitment?.slots?.length || 0 }}건</span>
                <button
                  v-if="canEditTeam && !newSlotFormActive"
                  class="primary-button"
                  type="button"
                  @click="newSlot"
                >
                  공고 +
                </button>
              </div>
            </div>
            <div class="team-slot-list-scroll" aria-label="구인 공고 목록">
              <button
                v-for="slot in selectedRecruitment?.slots || []"
                :key="slot.slotId"
                class="team-slot-card"
                :class="{ active: Number(slot.slotId) === Number(selectedSlotId) }"
                type="button"
                @click="selectSlot(slot)"
              >
                <strong>{{ slot.roleName }}</strong>
                <em>{{ slotStatusLabel(slot.status) }}</em>
                <small>{{ slot.remainingCount }}명 남음 · 필요 {{ slot.requiredCount }}명</small>
                <small>{{ experienceLevelLabel(slot.requiredExperienceLevel) }} · {{ collaborationConditionLabel(slot.collaborationCondition) }}</small>
              </button>
              <p v-if="!(selectedRecruitment?.slots || []).length" class="team-request-empty">등록된 구인 공고가 없습니다.</p>
            </div>
          </aside>

          <section class="form-panel team-slot-editor-panel">
            <div class="form-head">
              <div>
                <span class="eyebrow">{{ selectedRecruitment?.title }}</span>
                <h2>{{ slotForm.slotId ? '구인 공고 수정' : '새 구인 공고' }}</h2>
              </div>
              <div v-if="canEditTeam" class="row-actions">
                <button
                  v-if="slotForm.slotId"
                  class="ghost-button danger"
                  type="button"
                  :disabled="saving"
                  @click="requestSlotDelete"
                >
                  삭제
                </button>
                <button
                  class="primary-button"
                  :class="{ 'ai-recommendation-button': !slotForm.slotId }"
                  type="button"
                  :disabled="saving"
                  @click="saveSlot"
                >
                  {{ slotForm.slotId ? '저장' : '구인 공고 생성' }}
                </button>
              </div>
            </div>
            <div class="team-slot-editor-scroll">
              <div v-if="pendingSlotDelete" class="confirm-inline danger-confirm">
                <span>구인 공고를 삭제할까요? 대기 중인 지원/초대가 함께 취소됩니다.</span>
                <button class="ghost-button danger" type="button" :disabled="saving" @click="deleteSlot">
                  삭제 확인
                </button>
                <button class="ghost-button" type="button" :disabled="saving" @click="cancelSlotDelete">
                  취소
                </button>
              </div>
              <div class="form-grid team-slot-form-grid">
                <label class="field">
                  <span>역할</span>
                  <select v-model="slotForm.roleId">
                    <option v-for="role in roleOptions" :key="role.roleId" :value="role.roleId">
                      {{ role.name }}
                    </option>
                  </select>
                </label>
                <label class="field">
                  <span>필요 인원</span>
                  <input v-model.number="slotForm.requiredCount" min="1" type="number">
                </label>
                <label class="field">
                  <span>경력</span>
                  <select v-model="slotForm.requiredExperienceLevel">
                    <option v-for="item in codeOptions('EXPERIENCE_LEVEL')" :key="item.code" :value="item.code">
                      {{ item.displayName }}
                    </option>
                  </select>
                </label>
                <label class="field">
                  <span>협업 조건</span>
                  <select v-model="slotForm.collaborationCondition">
                    <option v-for="item in codeOptions('COLLABORATION_CONDITION')" :key="item.code" :value="item.code">
                      {{ item.displayName }}
                    </option>
                  </select>
                </label>
                <label class="field">
                  <span>기간</span>
                  <select v-model="slotForm.roleDuration">
                    <option v-for="item in codeOptions('DURATION')" :key="item.code" :value="item.code">
                      {{ item.displayName }}
                    </option>
                  </select>
                </label>
                <label class="field">
                  <span>장비 필요</span>
                  <select v-model="slotForm.equipmentRequiredYn">
                    <option value="Y">필요</option>
                    <option value="N">무관</option>
                  </select>
                </label>
              </div>
            </div>
          </section>
        </section>

      </section>

    </div>
  </section>

  <div v-if="imagePreview" class="image-preview-modal" role="dialog" aria-modal="true" @click.self="closeImagePreview">
    <button class="image-preview-close" type="button" aria-label="이미지 미리보기 닫기" @click="closeImagePreview">×</button>
    <img :src="imagePreview.src" :alt="imagePreview.alt">
  </div>
  </section>
</template>
