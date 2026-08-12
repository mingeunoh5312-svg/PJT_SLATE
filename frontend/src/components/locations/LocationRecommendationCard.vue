<script setup>
import { computed } from 'vue'

const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  selected: Boolean,
  saved: Boolean
})

const emit = defineEmits(['select', 'save'])

const address = computed(() => props.item.roadAddress || props.item.lotAddress || '주소 정보 없음')
const score = computed(() => {
  const value = Number(props.item.score)
  return Number.isFinite(value) ? Math.round(value * 10) / 10 : null
})
const histories = computed(() => Array.isArray(props.item.histories) ? props.item.histories : [])
const checkPoints = computed(() => Array.isArray(props.item.checkPoints) ? props.item.checkPoints.filter(Boolean) : [])
const matchedSceneTags = computed(() => [
  ...(Array.isArray(props.item.matchedRequiredSceneTags) ? props.item.matchedRequiredSceneTags : []),
  ...(Array.isArray(props.item.matchedOptionalSceneTags) ? props.item.matchedOptionalSceneTags : [])
].filter(Boolean))
const dataWarnings = computed(() => Array.isArray(props.item.dataWarnings) ? props.item.dataWarnings.filter(Boolean) : [])
const hasDetails = computed(() => (
  Boolean(props.item.recommendationBasis)
  || matchedSceneTags.value.length > 0
  || dataWarnings.value.length > 0
  || checkPoints.value.length > 0
  || histories.value.length > 0
))

function selectCard() {
  emit('select', Number(props.item.locationId))
}

function historyWarnings(history) {
  if (Array.isArray(history?.dataWarnings)) {
    return history.dataWarnings.filter(Boolean)
  }
  const text = `${history?.sceneDescription || ''} ${history?.movieTitle || ''}`
  const warnings = []
  if (/폐업|영업\s*종료|폐관|폐쇄/.test(text)) warnings.push('폐업/운영 종료 언급')
  if (/철거|멸실|현재\s*없음|없어짐/.test(text)) warnings.push('현존 여부 확인 필요')
  if (/공터|나대지/.test(text)) warnings.push('현재 용도 확인 필요')
  return warnings
}
</script>

<template>
  <article
    class="location-result-card"
    :class="{ 'is-selected': selected }"
    tabindex="0"
    :aria-current="selected ? 'true' : undefined"
    :data-location-id="item.locationId"
    @click="selectCard"
    @keydown.enter.prevent="selectCard"
    @keydown.space.prevent="selectCard"
  >
    <header class="location-result-head">
      <div class="location-rank">
        <span>{{ item.rankNo || '-' }}</span>
        <small>추천</small>
      </div>
      <div class="location-result-title">
        <div class="location-badge-row">
          <span v-if="item.fallback" class="location-status-badge is-fallback">조건 기반 추천</span>
          <span v-if="saved" class="location-status-badge is-saved">저장됨</span>
          <span v-if="dataWarnings.length" class="location-status-badge is-warning">확인 필요</span>
        </div>
        <h3>{{ item.placeName || '이름 없는 로케이션' }}</h3>
        <p>{{ address }}</p>
      </div>
      <strong v-if="score !== null" class="location-score">{{ score }}</strong>
    </header>

    <dl class="location-ai-copy">
      <div v-if="item.aiSummary">
        <dt>AI 추천 요약</dt>
        <dd>{{ item.aiSummary }}</dd>
      </div>
      <div v-if="item.matchReason">
        <dt>이 장면과 맞는 이유</dt>
        <dd>{{ item.matchReason }}</dd>
      </div>
      <div v-if="item.usageIdea">
        <dt>촬영 활용 아이디어</dt>
        <dd>{{ item.usageIdea }}</dd>
      </div>
    </dl>

    <details v-if="hasDetails" class="location-card-details" @click.stop>
      <summary>추천 상세 보기</summary>
      <section v-if="matchedSceneTags.length">
        <h4>장면 일치 요소</h4>
        <div class="location-tag-list">
          <span v-for="tag in matchedSceneTags" :key="tag">{{ tag }}</span>
        </div>
      </section>
      <section v-if="dataWarnings.length" class="location-warning-block">
        <h4>데이터 주의</h4>
        <ul>
          <li v-for="warning in dataWarnings" :key="warning">{{ warning }}</li>
        </ul>
      </section>
      <section v-if="item.recommendationBasis">
        <h4>추천 근거</h4>
        <p>{{ item.recommendationBasis }}</p>
      </section>
      <section v-if="checkPoints.length">
        <h4>현장 체크 포인트</h4>
        <ul>
          <li v-for="point in checkPoints" :key="point">{{ point }}</li>
        </ul>
      </section>
      <section v-if="histories.length">
        <h4>대표 촬영 이력</h4>
        <article
          v-for="history in histories"
          :key="history.historyId || `${history.movieTitle}-${history.productionYear}`"
          :class="{ 'has-warning': historyWarnings(history).length }"
        >
          <div>
            <strong>{{ history.movieTitle || '작품명 정보 없음' }}</strong>
            <span v-if="history.productionYear">{{ history.productionYear }}</span>
          </div>
          <p v-if="history.sceneDescription">{{ history.sceneDescription }}</p>
          <ul v-if="historyWarnings(history).length" class="location-history-warnings">
            <li v-for="warning in historyWarnings(history)" :key="warning">{{ warning }}</li>
          </ul>
          <a
            v-if="history.sourceUrl"
            :href="history.sourceUrl"
            target="_blank"
            rel="noopener noreferrer"
            @click.stop
          >
            출처 새 탭에서 보기
          </a>
        </article>
      </section>
    </details>

    <footer class="location-card-actions">
      <span>{{ item.sido || '지역 미상' }}<template v-if="item.sigungu"> · {{ item.sigungu }}</template></span>
      <button class="primary-button" type="button" @click.stop="emit('save', item)">
        {{ saved ? '저장 정보 확인' : '후보로 저장' }}
      </button>
    </footer>
  </article>
</template>
