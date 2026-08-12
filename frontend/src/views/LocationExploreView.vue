<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LocationMap from '../components/locations/LocationMap.vue'
import LocationRecommendationCard from '../components/locations/LocationRecommendationCard.vue'
import LocationSaveDialog from '../components/locations/LocationSaveDialog.vue'
import LocationSavedCandidateCard from '../components/locations/LocationSavedCandidateCard.vue'
import { slateApi } from '../services/api'

const route = useRoute()
const router = useRouter()

const prompt = ref('')
const recommendationLimit = ref(5)
const includeTeamContext = ref(false)
const regions = ref([])
const teams = ref([])
const selectedTopRegions = ref([])
const topRegionKeyword = ref('')
const topRegionDropdownOpen = ref(false)
const selectedRegionIds = ref([])
const regionKeyword = ref('')
const regionDropdownOpen = ref(false)
const referencesLoading = ref(false)
const referencesError = ref('')
const activeTab = ref('recommendations')
const mobilePanel = ref('list')
const recommendationLoading = ref(false)
const savedLoading = ref(false)
const error = ref('')
const notice = ref('')
const noticeLink = ref(null)
const recommendationSession = ref(null)
const recommendations = ref([])
const savedCandidates = ref([])
const savedLoaded = ref(false)
const selectedLocationId = ref(null)
const saveDialogItem = ref(null)
const mapError = ref('')
const mapRef = ref(null)
let contextRequestId = 0
let recommendationRequestId = 0
let savedRequestId = 0
const FALLBACK_NOTICE = '추천 결과를 눌러 촬영 위치를 확인해보세요.'

const isTeamMode = computed(() => route.name === 'teams-locations')
const teamId = computed(() => {
  const value = Number(route.params.teamId)
  return Number.isInteger(value) && value > 0 ? value : null
})
const teamContext = computed(() => teams.value.find((team) => Number(team.teamId) === teamId.value) || null)
const activeTeams = computed(() => teams.value.filter((team) => (
  team?.teamId
  && team.status !== 'ENDED'
  && team.status !== 'DELETED'
)))
const teamAccessError = computed(() => {
  if (!isTeamMode.value || referencesLoading.value) return ''
  if (!teamId.value) return '팀 경로가 올바르지 않습니다.'
  if (!teamContext.value) return '이 팀을 찾을 수 없거나 활성 멤버 권한이 없습니다.'
  return ''
})
const contextTitle = computed(() => isTeamMode.value
  ? `${teamContext.value?.name || '팀'} 로케이션 탐색`
  : '개인 로케이션 탐색')
const contextDescription = computed(() => isTeamMode.value
  ? '현재 팀의 장면 의도에 맞는 장소를 찾고 팀 후보지로 저장합니다.'
  : '장면 의도에 맞는 실제 촬영 장소를 찾고 개인 또는 참여 팀 후보지로 저장합니다.')
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
  if (!keyword) return scopedRegions.value
  return scopedRegions.value.filter((region) => String(region.publicDisplayName || region.name || '').toLowerCase().includes(keyword))
})
const visibleRegionOptions = computed(() => filteredRegions.value)
const selectedRegions = computed(() => selectedRegionIds.value
  .map((regionId) => regions.value.find((region) => Number(region.regionId) === Number(regionId)))
  .filter(Boolean))
const topRegionSummary = computed(() => selectedTopRegions.value.length ? `${selectedTopRegions.value.length}개 선택` : '전체 지역')
const detailRegionSummary = computed(() => selectedRegions.value.length
  ? `${selectedRegions.value.length}개 선택`
  : `${selectedTopRegions.value.length ? `${selectedTopRegions.value.length}개 지역` : '전체 지역'} 기준 ${scopedRegions.value.length}건`)
const selectedAreaChips = computed(() => {
  const chips = [
    ...selectedTopRegions.value.map((sido) => ({
      key: `top:${sido}`,
      type: 'top',
      sido,
      label: sido
    })),
    ...selectedRegions.value.map((region) => ({
      key: `detail:${region.regionId}`,
      type: 'detail',
      value: Number(region.regionId),
      sido: region.sidoName || '',
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
const selectedRegionFilters = computed(() => selectedRegions.value.map((region) => ({
  sido: region.sidoName || null,
  sigungu: region.sigunguName || null
})).filter((region) => region.sido || region.sigungu))
const activeItems = computed(() => activeTab.value === 'recommendations' ? recommendations.value : savedCandidates.value)
const savedLocationIds = computed(() => new Set(savedCandidates.value.map((item) => Number(item.locationId))))
const hasCurrentResults = computed(() => recommendations.value.length > 0)
const resultStatus = computed(() => recommendationSession.value?.status || '')
const resultNotice = computed(() => activeTab.value === 'recommendations' && notice.value === FALLBACK_NOTICE ? notice.value : '')
const topNotice = computed(() => resultNotice.value ? '' : notice.value)

function normalizeLocationItem(item) {
  if (!item || typeof item !== 'object') return item
  const normalized = { ...item }
  for (const key of ['sessionId', 'recommendationId', 'candidateId', 'locationId', 'rankNo', 'teamId']) {
    if (item[key] === null || item[key] === undefined || item[key] === '') continue
    const value = Number(item[key])
    if (Number.isFinite(value)) normalized[key] = value
  }
  for (const key of ['latitude', 'longitude', 'score']) {
    if (item[key] === null || item[key] === undefined || item[key] === '') continue
    const value = Number(item[key])
    if (Number.isFinite(value)) normalized[key] = value
  }
  return normalized
}

function clearMessages() {
  error.value = ''
  notice.value = ''
  noticeLink.value = null
}

function resetContextState() {
  recommendationRequestId += 1
  savedRequestId += 1
  activeTab.value = 'recommendations'
  mobilePanel.value = 'list'
  includeTeamContext.value = isTeamMode.value
  recommendationSession.value = null
  recommendations.value = []
  savedCandidates.value = []
  savedLoaded.value = false
  selectedLocationId.value = null
  saveDialogItem.value = null
  mapError.value = ''
  clearMessages()
}

async function loadContext() {
  const requestId = ++contextRequestId
  resetContextState()
  referencesLoading.value = true
  referencesError.value = ''
  try {
    const [teamResult, regionResult] = await Promise.allSettled([
      slateApi.myTeams(),
      slateApi.regions('', 1000)
    ])
    if (requestId !== contextRequestId) return
    if (teamResult.status === 'fulfilled') {
      teams.value = Array.isArray(teamResult.value) ? teamResult.value : []
    } else {
      teams.value = []
      referencesError.value = teamResult.reason?.message || '팀 정보를 불러오지 못했습니다.'
    }
    if (regionResult.status === 'fulfilled') {
      regions.value = Array.isArray(regionResult.value) ? regionResult.value : []
    } else {
      regions.value = []
      if (!referencesError.value) referencesError.value = '지역 목록을 불러오지 못했습니다. 지역명은 직접 입력할 수 있습니다.'
    }
  } finally {
    if (requestId === contextRequestId) referencesLoading.value = false
  }
}

async function requestRecommendations() {
  const normalizedPrompt = prompt.value.trim()
  clearMessages()
  if (normalizedPrompt.length < 5) {
    error.value = '장면 설명을 5자 이상 입력해주세요.'
    return
  }
  if (teamAccessError.value) {
    error.value = teamAccessError.value
    return
  }

  const requestId = ++recommendationRequestId
  const requestContext = `${route.name}:${teamId.value || 'personal'}`
  recommendationLoading.value = true
  try {
    const result = await slateApi.recommendLocations({
      prompt: normalizedPrompt,
      teamId: isTeamMode.value ? teamId.value : null,
      sidos: selectedTopRegions.value,
      regions: selectedRegionFilters.value,
      includeTeamContext: isTeamMode.value ? includeTeamContext.value : false,
      limit: recommendationLimit.value
    })
    if (
      requestId !== recommendationRequestId
      || requestContext !== `${route.name}:${teamId.value || 'personal'}`
    ) return
    recommendationSession.value = result || null
    recommendations.value = Array.isArray(result?.recommendations)
      ? result.recommendations.map(normalizeLocationItem)
      : []
    activeTab.value = 'recommendations'
    mobilePanel.value = 'list'
    selectedLocationId.value = null
    if (result?.status === 'NO_CANDIDATE') {
      notice.value = '요청한 지역과 조건에서 추천할 장소를 찾지 못했습니다.'
    } else if (result?.status === 'FAILED') {
      error.value = result.failureReason || '추천을 완료하지 못했습니다.'
    } else if (recommendations.value.length) {
      notice.value = FALLBACK_NOTICE
    }
  } catch (requestError) {
    if (requestId !== recommendationRequestId) return
    error.value = requestError.message || '로케이션 추천을 요청하지 못했습니다.'
  } finally {
    if (requestId === recommendationRequestId) recommendationLoading.value = false
  }
}

async function loadSavedCandidates(force = false) {
  if (savedLoaded.value && !force) return
  if (teamAccessError.value) return
  const requestId = ++savedRequestId
  const requestContext = `${route.name}:${teamId.value || 'personal'}`
  savedLoading.value = true
  try {
    const rows = isTeamMode.value
      ? await slateApi.teamLocationCandidates(teamId.value)
      : await slateApi.personalLocationCandidates()
    if (
      requestId !== savedRequestId
      || requestContext !== `${route.name}:${teamId.value || 'personal'}`
    ) return
    savedCandidates.value = Array.isArray(rows) ? rows.map(normalizeLocationItem) : []
    savedLoaded.value = true
    if (activeTab.value === 'saved') {
      selectedLocationId.value = savedCandidates.value[0]?.locationId || null
    }
  } catch (requestError) {
    if (requestId !== savedRequestId) return
    error.value = requestError.message || '저장한 후보를 불러오지 못했습니다.'
  } finally {
    if (requestId === savedRequestId) savedLoading.value = false
  }
}

async function changeTab(tab) {
  activeTab.value = tab
  clearMessages()
  if (tab === 'saved') await loadSavedCandidates()
  const items = tab === 'saved' ? savedCandidates.value : recommendations.value
  selectedLocationId.value = tab === 'saved' ? items[0]?.locationId || null : null
  await nextTick()
  mapRef.value?.relayout()
}

async function changeMobilePanel(panel) {
  mobilePanel.value = panel
  if (panel === 'map') {
    await nextTick()
    mapRef.value?.relayout()
  }
}

function selectLocation(locationId, fromMap = false) {
  selectedLocationId.value = Number(locationId)
  if (fromMap && window.matchMedia('(max-width: 920px)').matches) {
    mobilePanel.value = 'list'
  }
  nextTick(() => {
    mapRef.value?.focusLocation(Number(locationId))
    document.querySelector(`[data-location-id="${Number(locationId)}"]`)?.scrollIntoView({
      behavior: 'smooth',
      block: 'nearest'
    })
  })
}

function openSaveDialog(item) {
  saveDialogItem.value = item
}

async function handleSaved({ result, teamId: savedTeamId, teamName }) {
  saveDialogItem.value = null
  const alreadySaved = result?.alreadySaved === true || result?.created === false
  notice.value = alreadySaved ? '이미 저장된 로케이션 후보입니다.' : '로케이션 후보를 저장했습니다.'
  noticeLink.value = null

  const savedToCurrentContext = (
    (!isTeamMode.value && savedTeamId === null)
    || (isTeamMode.value && Number(savedTeamId) === teamId.value)
  )
  if (savedToCurrentContext) {
    await loadSavedCandidates(true)
    return
  }
  if (savedTeamId) {
    notice.value = `${teamName || '선택한 팀'} 후보지로 저장했습니다.`
    noticeLink.value = {
      label: '팀 로케이션 보기',
      to: { name: 'teams-locations', params: { teamId: savedTeamId } }
    }
  }
}

function handlePromptKeydown(event) {
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault()
    if (!recommendationLoading.value) requestRecommendations()
  }
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

function selectRegion(region) {
  const regionId = Number(region.regionId)
  selectedRegionIds.value = selectedRegionIdSet.value.has(regionId)
    ? selectedRegionIds.value.filter((value) => Number(value) !== regionId)
    : [...selectedRegionIds.value, regionId]
  regionKeyword.value = ''
  regionDropdownOpen.value = false
  releaseRegionInputFocus()
}

function clearRegion(regionId) {
  selectedRegionIds.value = selectedRegionIds.value.filter((value) => Number(value) !== Number(regionId))
  regionKeyword.value = ''
  regionDropdownOpen.value = false
}

function selectTopRegion(sidoName) {
  if (!sidoName) {
    clearAllRegions()
    releaseRegionInputFocus()
    return
  }
  const nextTopRegions = selectedTopRegionSet.value.has(sidoName)
    ? selectedTopRegions.value.filter((value) => value !== sidoName)
    : [...selectedTopRegions.value, sidoName].sort((left, right) => topRegionSortIndex(left) - topRegionSortIndex(right))
  selectedTopRegions.value = nextTopRegions
  topRegionKeyword.value = ''
  topRegionDropdownOpen.value = false
  regionKeyword.value = ''
  regionDropdownOpen.value = false
  releaseRegionInputFocus()
}

function clearTopRegion(sidoName) {
  const nextTopRegions = selectedTopRegions.value.filter((value) => value !== sidoName)
  selectedTopRegions.value = nextTopRegions
  topRegionKeyword.value = ''
  topRegionDropdownOpen.value = false
}

function clearAllRegions() {
  selectedTopRegions.value = []
  selectedRegionIds.value = []
  topRegionKeyword.value = ''
  regionKeyword.value = ''
  topRegionDropdownOpen.value = false
  regionDropdownOpen.value = false
}

function removeRegionChip(chip) {
  if (chip.type === 'top') {
    clearTopRegion(chip.sido)
  } else {
    clearRegion(chip.value)
  }
}

function goToTeamLocation(event) {
  const value = Number(event.target.value)
  event.target.value = ''
  if (Number.isFinite(value) && value > 0) {
    router.push({ name: 'teams-locations', params: { teamId: value } })
  }
}

watch(() => [route.name, route.params.teamId], loadContext, { immediate: true })
</script>

<template>
  <main class="location-explore-page">
    <header class="location-context-head">
      <div>
        <span class="eyebrow">{{ isTeamMode ? 'Team location workspace' : 'Personal location workspace' }}</span>
        <h2>{{ contextTitle }}</h2>
        <p>{{ contextDescription }}</p>
      </div>
      <div class="location-context-actions">
        <label v-if="!isTeamMode" class="location-team-jump">
          <span>팀 탐색</span>
          <select
            :disabled="referencesLoading || !activeTeams.length"
            aria-label="팀 로케이션 탐색으로 이동"
            @change="goToTeamLocation"
          >
            <option value="">{{ activeTeams.length ? '팀 선택' : '소속 팀 없음' }}</option>
            <option v-for="team in activeTeams" :key="team.teamId" :value="team.teamId">
              {{ team.name }}
            </option>
          </select>
        </label>
        <button
          v-if="isTeamMode && teamId"
          class="ghost-button"
          type="button"
          @click="router.push({ name: 'teams-detail', params: { teamId } })"
        >
          팀 정보
        </button>
        <button
          v-if="isTeamMode"
          class="ghost-button"
          type="button"
          @click="router.push({ name: 'locations' })"
        >
          개인 탐색으로 전환
        </button>
      </div>
    </header>

    <nav class="location-view-tabs" role="tablist" aria-label="로케이션 보기">
      <button
        type="button"
        role="tab"
        :aria-selected="activeTab === 'recommendations'"
        :class="{ active: activeTab === 'recommendations' }"
        @click="changeTab('recommendations')"
      >
        AI 추천
      </button>
      <button
        type="button"
        role="tab"
        :aria-selected="activeTab === 'saved'"
        :class="{ active: activeTab === 'saved' }"
        @click="changeTab('saved')"
      >
        저장한 후보
      </button>
    </nav>

    <p v-if="teamAccessError" class="location-inline-message is-error" role="alert">{{ teamAccessError }}</p>
    <p v-else-if="referencesError" class="location-inline-message is-warning">{{ referencesError }}</p>
    <p v-if="error" class="location-inline-message is-error" role="alert">{{ error }}</p>
    <div v-if="topNotice" class="location-inline-message is-notice" role="status">
      <span>{{ topNotice }}</span>
      <RouterLink v-if="noticeLink" :to="noticeLink.to">{{ noticeLink.label }}</RouterLink>
    </div>

    <form class="location-search-tool" @submit.prevent="requestRecommendations">
      <label class="location-prompt-field">
        <span>장면 프롬프트</span>
        <textarea
          v-model="prompt"
          maxlength="2000"
          rows="4"
          :disabled="recommendationLoading || Boolean(teamAccessError)"
          placeholder="예: 비 오는 밤, 서울의 좁은 골목에서 긴장감 있는 추격 장면을 촬영하고 싶어요."
          @keydown="handlePromptKeydown"
        />
        <small>{{ prompt.length }} / 2,000 · Ctrl 또는 Cmd + Enter로 추천 요청</small>
      </label>

      <div class="location-filter-row">
        <div class="location-region-field">
          <div class="location-region-columns">
            <div class="location-region-selector">
              <div class="location-region-subhead">
                <span>지역</span>
                <small>{{ topRegionSummary }}</small>
              </div>
              <div class="location-region-combobox">
                <input
                  v-model="topRegionKeyword"
                  type="search"
                  placeholder="지역 선택"
                  aria-label="지역 검색어"
                  autocomplete="off"
                  :disabled="recommendationLoading || referencesLoading || !topRegionOptions.length"
                  @focus="topRegionDropdownOpen = true"
                  @input="topRegionDropdownOpen = true"
                  @blur="topRegionDropdownOpen = false"
                  @keydown.escape="topRegionDropdownOpen = false"
                >
                <div
                  v-if="topRegionDropdownOpen"
                  class="location-region-dropdown"
                  role="listbox"
                  aria-label="상위 지역 검색 결과"
                >
                  <button
                    type="button"
                    class="location-region-option"
                    :class="{ active: !selectedTopRegions.length && !selectedRegionIds.length }"
                    role="option"
                    :aria-selected="!selectedTopRegions.length && !selectedRegionIds.length"
                    :disabled="recommendationLoading || referencesLoading"
                    @mousedown.prevent="selectTopRegion('')"
                  >
                    전체 지역
                  </button>
                  <button
                    v-for="option in filteredTopRegionOptions"
                    :key="option"
                    type="button"
                    class="location-region-option"
                    :class="{ active: selectedTopRegionSet.has(option) }"
                    role="option"
                    :aria-selected="selectedTopRegionSet.has(option)"
                    :disabled="recommendationLoading || referencesLoading"
                    @mousedown.prevent="selectTopRegion(option)"
                  >
                    {{ option }}
                  </button>
                  <p v-if="!filteredTopRegionOptions.length" class="location-filter-empty">입력한 텍스트를 포함하는 상위 지역이 없습니다.</p>
                </div>
              </div>
            </div>

            <div class="location-region-selector">
              <div class="location-region-subhead">
                <span>세부 입력</span>
                <small>{{ detailRegionSummary }}</small>
              </div>
              <div class="location-region-combobox">
                <input
                  v-model="regionKeyword"
                  type="search"
                  placeholder="시·군·구 입력"
                  aria-label="지역 검색어"
                  autocomplete="off"
                  :disabled="recommendationLoading || referencesLoading || !regions.length"
                  @focus="regionDropdownOpen = true"
                  @input="regionDropdownOpen = true"
                  @blur="regionDropdownOpen = false"
                  @keydown.escape="regionDropdownOpen = false"
                >
                <div
                  v-if="regionDropdownOpen"
                  class="location-region-dropdown"
                  role="listbox"
                  aria-label="지역 검색 결과"
                >
                  <button
                    v-for="region in visibleRegionOptions"
                    :key="region.regionId"
                    type="button"
                    class="location-region-option"
                    :class="{ active: selectedRegionIdSet.has(Number(region.regionId)) }"
                    role="option"
                    :aria-selected="selectedRegionIdSet.has(Number(region.regionId))"
                    :disabled="recommendationLoading || referencesLoading"
                    @mousedown.prevent="selectRegion(region)"
                  >
                    {{ region.publicDisplayName }}
                  </button>
                  <p v-if="!visibleRegionOptions.length" class="location-filter-empty">입력한 텍스트를 포함하는 지역이 없습니다.</p>
                </div>
              </div>
            </div>
          </div>
          <div
            v-if="selectedAreaChips.length"
            class="location-selected-inline location-region-selection-list"
            aria-label="선택한 지역"
          >
            <button
              v-for="chip in selectedAreaChips"
              :key="chip.key"
              class="location-selected-filter"
              type="button"
              :aria-label="`${chip.label} 지역 제거`"
              @click="removeRegionChip(chip)"
            >
              <span>{{ chip.label }}</span>
              <b aria-hidden="true">×</b>
            </button>
          </div>
        </div>
        <fieldset>
          <legend>추천 수</legend>
          <div class="location-limit-control">
            <button type="button" :class="{ active: recommendationLimit === 3 }" @click="recommendationLimit = 3">3개</button>
            <button type="button" :class="{ active: recommendationLimit === 5 }" @click="recommendationLimit = 5">5개</button>
          </div>
        </fieldset>
        <label v-if="isTeamMode" class="location-context-toggle">
          <input
            v-model="includeTeamContext"
            type="checkbox"
            :disabled="recommendationLoading"
          >
          <span>팀 정보 반영</span>
        </label>
        <button
          class="primary-button location-recommend-button"
          type="submit"
          :disabled="recommendationLoading || Boolean(teamAccessError)"
        >
          {{ recommendationLoading ? '추천 중' : 'AI 추천 요청' }}
        </button>
      </div>
    </form>

    <div v-if="resultNotice" class="location-inline-message is-notice location-result-notice" role="status">
      <span>{{ resultNotice }}</span>
    </div>

    <div class="location-mobile-panel-switch" aria-label="결과 표시 방식">
      <button type="button" :class="{ active: mobilePanel === 'list' }" @click="changeMobilePanel('list')">목록</button>
      <button type="button" :class="{ active: mobilePanel === 'map' }" @click="changeMobilePanel('map')">지도</button>
    </div>

    <section class="location-workspace">
      <section
        class="location-list-panel"
        :class="{ 'is-mobile-hidden': mobilePanel !== 'list' }"
        :aria-busy="recommendationLoading || savedLoading"
      >
        <header>
          <div>
            <strong>{{ activeTab === 'recommendations' ? '추천 결과' : '저장 후보' }}</strong>
            <span>{{ activeItems.length }}건</span>
          </div>
          <small v-if="recommendationSession && activeTab === 'recommendations'">
            후보 {{ recommendationSession.candidateCount || 0 }}건 검토
          </small>
        </header>

        <div v-if="recommendationLoading && !hasCurrentResults && activeTab === 'recommendations'" class="location-list-loading" role="status">
          <span v-for="index in 3" :key="index" />
          <strong>장면 의도와 촬영 이력을 비교하고 있습니다.</strong>
        </div>
        <div v-else-if="savedLoading && !savedCandidates.length && activeTab === 'saved'" class="location-list-loading" role="status">
          <span v-for="index in 3" :key="index" />
          <strong>저장한 후보를 불러오고 있습니다.</strong>
        </div>
        <div
          v-else-if="activeTab === 'recommendations' && !recommendationSession"
          class="location-empty-state"
        >
          <strong>촬영할 장면을 설명해주세요.</strong>
          <p>실제 촬영 장소와 이력을 바탕으로 최대 5곳을 추천합니다.</p>
        </div>
        <div
          v-else-if="activeTab === 'recommendations' && resultStatus === 'NO_CANDIDATE'"
          class="location-empty-state"
        >
          <strong>조건에 맞는 후보가 없습니다.</strong>
          <p>지역 조건이나 장면 설명을 조정해 다시 요청해주세요.</p>
        </div>
        <div
          v-else-if="activeTab === 'recommendations' && resultStatus === 'FAILED' && !recommendations.length"
          class="location-empty-state is-error"
        >
          <strong>추천을 완료하지 못했습니다.</strong>
          <p>{{ recommendationSession?.failureReason || '잠시 후 다시 요청해주세요.' }}</p>
        </div>
        <div
          v-else-if="activeTab === 'saved' && savedLoaded && !savedCandidates.length"
          class="location-empty-state"
        >
          <strong>저장한 후보가 없습니다.</strong>
          <p>AI 추천에서 검토할 장소를 후보로 저장해보세요.</p>
        </div>

        <div v-else class="location-card-list">
          <div v-if="recommendationLoading && activeTab === 'recommendations'" class="location-refresh-state" role="status">
            새 추천을 준비하고 있습니다. 현재 결과는 완료될 때까지 유지됩니다.
          </div>
          <template v-if="activeTab === 'recommendations'">
            <LocationRecommendationCard
              v-for="item in recommendations"
              :key="item.locationId"
              :item="item"
              :selected="Number(selectedLocationId) === Number(item.locationId)"
              :saved="savedLocationIds.has(Number(item.locationId))"
              @select="selectLocation"
              @save="openSaveDialog"
            />
          </template>
          <template v-else>
            <LocationSavedCandidateCard
              v-for="item in savedCandidates"
              :key="item.candidateId || item.locationId"
              :item="item"
              :selected="Number(selectedLocationId) === Number(item.locationId)"
              :team-mode="isTeamMode"
              @select="selectLocation"
            />
          </template>
        </div>
      </section>

      <section class="location-map-panel" :class="{ 'is-mobile-hidden': mobilePanel !== 'map' }">
        <LocationMap
          ref="mapRef"
          :items="activeItems"
          :selected-location-id="selectedLocationId"
          :context-mode="activeTab"
          :loading="recommendationLoading || savedLoading"
          @select-location="selectLocation($event, true)"
          @map-error="mapError = $event"
        />
      </section>
    </section>

    <LocationSaveDialog
      :open="Boolean(saveDialogItem)"
      :item="saveDialogItem"
      :teams="activeTeams"
      :team-context-id="isTeamMode ? teamId : null"
      @close="saveDialogItem = null"
      @saved="handleSaved"
    />
  </main>
</template>
