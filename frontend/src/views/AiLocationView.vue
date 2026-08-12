<script setup>
const fields = [
  { label: '지역 조건', value: '서울 중구 / 종로 / 영등포' },
  { label: '추천 개수', value: '6개' },
  { label: '팀 맥락 포함', value: '블루룸 픽처스 <푸른 방>' },
  { label: '촬영 시간대', value: '야간' }
]

const locations = [
  {
    id: 'loc-1',
    name: '을지로 4가 인쇄골목',
    region: '서울 중구',
    reason: '낡은 간판과 좁은 골목, 야간 네온이 풍부합니다.',
    caution: '야간 소음 민원 가능, 사전 협의 필요',
    saved: '팀'
  },
  {
    id: 'loc-2',
    name: '양양 죽도 해변',
    region: '강원 양양',
    reason: '오프시즌 비어 있는 해변 풍경, 다큐 정서 일치',
    caution: '11월~3월 강풍 주의',
    saved: '팀'
  },
  {
    id: 'loc-3',
    name: '수원 화서문 일대',
    region: '경기 수원',
    reason: '성벽 야경, 매직아워 광량이 좋습니다.',
    caution: '문화재 구역 - 촬영 허가 필요',
    saved: '개인'
  }
]

const mapPins = [
  { x: '28%', y: '44%', n: 1 },
  { x: '62%', y: '28%', n: 2 },
  { x: '78%', y: '70%', n: 3 }
]
</script>

<template>
  <section class="lovable-location-page">
    <header class="lovable-location-head">
      <span>
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="m12 3 1.8 5.5 5.7 1.7-5.7 1.8L12 18l-1.8-6-5.7-1.8 5.7-1.7L12 3Z" />
        </svg>
        SLATE INTELLIGENCE
      </span>
      <h1>AI 로케이션 탐색</h1>
      <p>씬 설명을 입력하면 지역 조건과 팀 맥락을 반영해 촬영지 후보를 정리합니다.</p>
    </header>

    <div class="lovable-location-layout">
      <section class="lovable-location-tool" aria-label="AI 로케이션 추천 입력">
        <article class="lovable-location-prompt">
          <label for="lovable-location-scene">장면 설명</label>
          <textarea id="lovable-location-scene">1990년대 분위기의 인쇄골목 야간. 좁은 골목, 깜빡이는 네온 간판, 비 온 뒤 젖은 노면.</textarea>

          <dl>
            <div v-for="field in fields" :key="field.label">
              <dt>{{ field.label }}</dt>
              <dd>{{ field.value }}</dd>
            </div>
          </dl>

          <button type="button">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="m12 3 1.8 5.5 5.7 1.7-5.7 1.8L12 18l-1.8-6-5.7-1.8 5.7-1.7L12 3Z" />
            </svg>
            추천 실행
          </button>
          <p>예상 처리 시간 8초 · 시안 상태에서는 실제 호출 없음</p>
        </article>

        <article class="lovable-location-saved">
          <span>저장한 후보</span>
          <ul>
            <li v-for="location in locations" :key="location.id">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M6 4h12v17l-6-4-6 4V4Z" />
              </svg>
              <strong>{{ location.name }}</strong>
              <small :class="{ team: location.saved === '팀' }">{{ location.saved }}</small>
            </li>
          </ul>
        </article>
      </section>

      <section class="lovable-location-results" aria-label="AI 로케이션 추천 결과">
        <div class="lovable-location-map">
          <span>MAP · 시안 (실제 지도 미연동)</span>
          <i
            v-for="pin in mapPins"
            :key="pin.n"
            :style="{ left: pin.x, top: pin.y }"
          >
            {{ pin.n }}
          </i>
        </div>

        <article v-for="(location, index) in locations" :key="location.id" class="lovable-location-result">
          <div>{{ String(index + 1).padStart(2, '0') }}</div>
          <section>
            <h2>{{ location.name }}</h2>
            <span>
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 21s7-5.2 7-11a7 7 0 0 0-14 0c0 5.8 7 11 7 11Z" />
                <circle cx="12" cy="10" r="2.5" />
              </svg>
              {{ location.region }}
            </span>
            <p>{{ location.reason }}</p>
            <small>
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="m12 4 9 16H3L12 4Z" />
                <path d="M12 9v5M12 17h.01" />
              </svg>
              {{ location.caution }}
            </small>
          </section>
          <footer>
            <button type="button">개인 후보로 저장</button>
            <button type="button">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
              팀 후보로 저장
            </button>
          </footer>
        </article>
      </section>
    </div>
  </section>
</template>
