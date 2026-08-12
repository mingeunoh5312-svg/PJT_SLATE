<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { loadKakaoMaps } from '../../services/kakaoMaps'

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  selectedLocationId: {
    type: [Number, String],
    default: null
  },
  contextMode: {
    type: String,
    default: 'recommendations'
  },
  loading: Boolean
})

const emit = defineEmits(['select-location', 'map-error'])

const mapContainer = ref(null)
const sdkLoading = ref(true)
const mapError = ref('')
let kakaoMaps = null
let map = null
let overlays = []
let resizeObserver = null
let unmounted = false
const SELECTED_ZOOM_LEVEL = 4

const validItems = computed(() => props.items
  .map((item) => ({
    item,
    locationId: Number(item?.locationId),
    latitude: Number(item?.latitude),
    longitude: Number(item?.longitude)
  }))
  .filter((entry) => (
    Number.isFinite(entry.locationId)
    && entry.locationId > 0
    && Number.isFinite(entry.latitude)
    && entry.latitude >= -90
    && entry.latitude <= 90
    && Number.isFinite(entry.longitude)
    && entry.longitude >= -180
    && entry.longitude <= 180
  )))

function selectedId() {
  const value = Number(props.selectedLocationId)
  return Number.isFinite(value) && value > 0 ? value : null
}

function clearOverlays() {
  overlays.forEach(({ overlay, element, clickHandler }) => {
    element.removeEventListener('click', clickHandler)
    overlay.setMap(null)
  })
  overlays = []
}

function markerLabel(entry, index) {
  if (props.contextMode === 'saved') return '저장'
  const rank = Number(entry.item?.rankNo)
  return Number.isFinite(rank) && rank > 0 ? String(rank) : String(index + 1)
}

function markerAriaLabel(entry, index) {
  const placeName = entry.item?.placeName || '로케이션'
  if (props.contextMode === 'saved') return `${placeName} 저장 후보 선택`
  return `${markerLabel(entry, index)}순위 ${placeName} 선택`
}

function buildOverlay(entry, index) {
  const element = document.createElement('button')
  element.type = 'button'
  element.className = `location-map-marker ${props.contextMode === 'saved' ? 'is-saved' : 'is-recommendation'}`
  element.textContent = markerLabel(entry, index)
  element.setAttribute('aria-label', markerAriaLabel(entry, index))
  element.dataset.locationId = String(entry.locationId)
  const clickHandler = () => emit('select-location', entry.locationId)
  element.addEventListener('click', clickHandler)

  const overlay = new kakaoMaps.CustomOverlay({
    position: new kakaoMaps.LatLng(entry.latitude, entry.longitude),
    content: element,
    xAnchor: 0.5,
    yAnchor: 1.1,
    zIndex: entry.locationId === selectedId() ? 20 : 2
  })
  overlay.setMap(map)
  return { overlay, element, clickHandler, entry }
}

function fitItems() {
  if (!map || !kakaoMaps) return
  if (!validItems.value.length) {
    map.setCenter(new kakaoMaps.LatLng(36.2683, 127.6358))
    map.setLevel(13)
    return
  }
  if (validItems.value.length === 1) {
    const [entry] = validItems.value
    map.setCenter(new kakaoMaps.LatLng(entry.latitude, entry.longitude))
    map.setLevel(5)
    return
  }
  const bounds = new kakaoMaps.LatLngBounds()
  validItems.value.forEach((entry) => {
    bounds.extend(new kakaoMaps.LatLng(entry.latitude, entry.longitude))
  })
  map.setBounds(bounds, 64, 64, 64, 64)
}

function renderOverlays() {
  if (!map || !kakaoMaps) return
  clearOverlays()
  overlays = validItems.value.map(buildOverlay)
  fitItems()
  updateSelectedOverlay(false)
}

function updateSelectedOverlay(move = true) {
  if (!map || !kakaoMaps) return
  const currentId = selectedId()
  let selectedEntry = null
  overlays.forEach((record) => {
    const isSelected = record.entry.locationId === currentId
    record.element.classList.toggle('is-selected', isSelected)
    record.element.setAttribute('aria-pressed', isSelected ? 'true' : 'false')
    record.overlay.setZIndex(isSelected ? 20 : 2)
    if (isSelected) selectedEntry = record.entry
  })
  if (move && selectedEntry) {
    map.panTo(new kakaoMaps.LatLng(selectedEntry.latitude, selectedEntry.longitude))
  }
}

async function relayout() {
  await nextTick()
  if (!map) return
  map.relayout()
  const current = validItems.value.find((entry) => entry.locationId === selectedId())
  if (current) map.panTo(new kakaoMaps.LatLng(current.latitude, current.longitude))
  else fitItems()
}

function focusLocation(locationId = props.selectedLocationId) {
  if (!map || !kakaoMaps) return
  const targetId = Number(locationId)
  if (!Number.isFinite(targetId) || targetId <= 0) return
  const current = validItems.value.find((entry) => entry.locationId === targetId)
  if (!current) return
  const currentLevel = Number(map.getLevel?.())
  map.setLevel(Number.isFinite(currentLevel) ? Math.min(currentLevel, SELECTED_ZOOM_LEVEL) : SELECTED_ZOOM_LEVEL)
  map.panTo(new kakaoMaps.LatLng(current.latitude, current.longitude))
}

async function initializeMap() {
  sdkLoading.value = true
  mapError.value = ''
  try {
    kakaoMaps = await loadKakaoMaps()
    if (unmounted || !mapContainer.value) return
    map = new kakaoMaps.Map(mapContainer.value, {
      center: new kakaoMaps.LatLng(36.2683, 127.6358),
      level: 13
    })
    map.addControl(new kakaoMaps.ZoomControl(), kakaoMaps.ControlPosition.RIGHT)
    renderOverlays()
    resizeObserver = new ResizeObserver(() => {
      if (map) map.relayout()
    })
    resizeObserver.observe(mapContainer.value)
  } catch (error) {
    if (unmounted) return
    mapError.value = error.message || '지도를 불러오지 못했습니다.'
    emit('map-error', mapError.value)
  } finally {
    if (!unmounted) sdkLoading.value = false
  }
}

watch(validItems, () => renderOverlays(), { deep: true })
watch(() => props.selectedLocationId, () => updateSelectedOverlay(false))
watch(() => props.contextMode, () => renderOverlays())

onMounted(initializeMap)

onBeforeUnmount(() => {
  unmounted = true
  resizeObserver?.disconnect()
  resizeObserver = null
  clearOverlays()
  map = null
  kakaoMaps = null
})

defineExpose({ relayout, focusLocation })
</script>

<template>
  <section class="location-map-shell" aria-label="로케이션 지도">
    <div ref="mapContainer" class="location-map-canvas" />
    <div v-if="sdkLoading" class="location-map-state" role="status">
      <strong>지도를 불러오는 중입니다.</strong>
    </div>
    <div v-else-if="mapError" class="location-map-state is-error" role="status">
      <strong>지도를 표시할 수 없습니다.</strong>
      <p>{{ mapError }}</p>
      <small>추천 카드와 후보 저장은 계속 사용할 수 있습니다.</small>
    </div>
    <div v-else-if="!validItems.length" class="location-map-state is-empty">
      <strong>표시할 후보가 없습니다.</strong>
      <p>추천 또는 저장 후보가 준비되면 DB 좌표를 지도에 표시합니다.</p>
    </div>
    <div v-if="loading && !mapError" class="location-map-loading" role="status">
      새 결과를 지도에 반영하고 있습니다.
    </div>
  </section>
</template>
