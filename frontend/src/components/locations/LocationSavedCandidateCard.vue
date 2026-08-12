<script setup>
import { computed } from 'vue'

const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  selected: Boolean,
  teamMode: Boolean
})

const emit = defineEmits(['select'])

const address = computed(() => props.item.roadAddress || props.item.lotAddress || '주소 정보 없음')
const fallback = computed(() => props.item.fallback === true || props.item.fallbackYn === 'Y')
const dataWarnings = computed(() => Array.isArray(props.item.dataWarnings) ? props.item.dataWarnings.filter(Boolean) : [])
const score = computed(() => {
  const value = Number(props.item.score)
  return Number.isFinite(value) ? value.toFixed(1) : null
})
const savedScopeLabel = computed(() => {
  if (props.teamMode) return `${props.item.savedByNickname || '팀 멤버'} 저장`
  if (props.item.teamId) return `${props.item.teamName || '팀'} 후보지`
  return '개인 후보지'
})

function formatDate(value) {
  if (!value) return '저장 시각 정보 없음'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

function selectCard() {
  emit('select', Number(props.item.locationId))
}
</script>

<template>
  <article
    class="location-result-card location-saved-card"
    :class="{ 'is-selected': selected }"
    tabindex="0"
    :aria-current="selected ? 'true' : undefined"
    :data-location-id="item.locationId"
    @click="selectCard"
    @keydown.enter.prevent="selectCard"
    @keydown.space.prevent="selectCard"
  >
    <header>
      <div>
        <div class="location-badge-row">
          <span class="location-status-badge is-saved">저장 후보</span>
          <span v-if="fallback" class="location-status-badge is-fallback">조건 기반 추천</span>
          <span v-if="dataWarnings.length" class="location-status-badge is-warning">확인 필요</span>
        </div>
        <h3>{{ item.title || item.placeName || '저장 후보' }}</h3>
        <strong>{{ item.placeName || '이름 없는 로케이션' }}</strong>
      </div>
      <span v-if="score !== null" class="location-score">{{ score }}</span>
    </header>
    <p class="location-address">{{ address }}</p>
    <ul v-if="dataWarnings.length" class="location-saved-warnings">
      <li v-for="warning in dataWarnings" :key="warning">{{ warning }}</li>
    </ul>
    <p v-if="item.memo" class="location-saved-memo">{{ item.memo }}</p>
    <p v-if="item.aiSummary" class="location-saved-summary">{{ item.aiSummary }}</p>
    <footer>
      <span>{{ savedScopeLabel }}</span>
      <time>{{ formatDate(item.createdAt) }}</time>
    </footer>
  </article>
</template>
