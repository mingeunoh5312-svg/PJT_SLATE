<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { slateApi } from '../../services/api'

const props = defineProps({
  open: Boolean,
  item: {
    type: Object,
    default: null
  },
  teams: {
    type: Array,
    default: () => []
  },
  teamContextId: {
    type: [Number, String],
    default: null
  }
})

const emit = defineEmits(['close', 'saved'])

const titleInput = ref(null)
const destination = ref('personal')
const title = ref('')
const memo = ref('')
const saving = ref(false)
const error = ref('')

const fixedTeamId = computed(() => {
  const value = Number(props.teamContextId)
  return Number.isFinite(value) && value > 0 ? value : null
})
const fixedTeam = computed(() => props.teams.find((team) => Number(team.teamId) === fixedTeamId.value))
const selectedTeam = computed(() => {
  const teamId = Number(destination.value)
  return props.teams.find((team) => Number(team.teamId) === teamId) || null
})

function resetForm() {
  destination.value = fixedTeamId.value ? String(fixedTeamId.value) : 'personal'
  title.value = String(props.item?.placeName || '').slice(0, 150)
  memo.value = ''
  error.value = ''
  saving.value = false
}

function close() {
  if (!saving.value) emit('close')
}

async function submit() {
  const normalizedTitle = title.value.trim()
  if (!normalizedTitle) {
    error.value = '후보지 제목을 입력해주세요.'
    titleInput.value?.focus()
    return
  }
  const locationId = Number(props.item?.locationId)
  if (!Number.isFinite(locationId) || locationId <= 0) {
    error.value = '저장할 로케이션 정보가 올바르지 않습니다.'
    return
  }

  saving.value = true
  error.value = ''
  try {
    const teamId = fixedTeamId.value || (destination.value === 'personal' ? null : Number(destination.value))
    const result = await slateApi.saveLocationCandidate({
      locationId,
      sessionId: props.item?.sessionId ? Number(props.item.sessionId) : null,
      recommendationId: props.item?.recommendationId ? Number(props.item.recommendationId) : null,
      teamId,
      title: normalizedTitle,
      memo: memo.value.trim() || null
    })
    emit('saved', {
      result,
      teamId,
      teamName: fixedTeam.value?.name || selectedTeam.value?.name || null
    })
  } catch (requestError) {
    error.value = requestError.message || '후보지를 저장하지 못했습니다.'
  } finally {
    saving.value = false
  }
}

function handleKeydown(event) {
  if (event.key === 'Escape' && props.open) close()
}

watch(() => props.open, async (open) => {
  if (!open) return
  resetForm()
  await nextTick()
  titleInput.value?.focus()
})

onMounted(() => document.addEventListener('keydown', handleKeydown))
onBeforeUnmount(() => document.removeEventListener('keydown', handleKeydown))
</script>

<template>
  <Teleport to="body">
    <div v-if="open && item" class="location-dialog-backdrop" @mousedown.self="close">
      <section
        class="location-save-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="location-save-dialog-title"
      >
        <header>
          <div>
            <span class="eyebrow">Location candidate</span>
            <h2 id="location-save-dialog-title">로케이션 후보 저장</h2>
          </div>
          <button class="icon-button" type="button" aria-label="저장 창 닫기" :disabled="saving" @click="close">×</button>
        </header>

        <div class="location-dialog-place">
          <span>장소명</span>
          <strong>{{ item.placeName || '이름 없는 로케이션' }}</strong>
          <p>{{ item.roadAddress || item.lotAddress || '주소 정보 없음' }}</p>
        </div>

        <form class="location-save-form" @submit.prevent="submit">
          <fieldset class="location-destination-field">
            <legend>저장 위치</legend>
            <template v-if="fixedTeamId">
              <div class="location-fixed-destination">
                <strong>{{ fixedTeam?.name || '현재 팀' }}</strong>
                <span>팀 후보지로 저장됩니다.</span>
              </div>
            </template>
            <select v-else v-model="destination" :disabled="saving">
              <option value="personal">개인 후보지</option>
              <option v-for="team in teams" :key="team.teamId" :value="String(team.teamId)">
                {{ team.name }}
              </option>
            </select>
          </fieldset>

          <label class="field">
            <span>제목</span>
            <input ref="titleInput" v-model="title" maxlength="150" required :disabled="saving">
            <small>{{ title.length }} / 150</small>
          </label>

          <label class="field">
            <span>메모</span>
            <textarea
              v-model="memo"
              maxlength="1000"
              rows="5"
              :disabled="saving"
              placeholder="장면 의도나 현장 확인 사항을 기록하세요."
            />
            <small>{{ memo.length }} / 1,000</small>
          </label>

          <p v-if="error" class="error-text" role="alert">{{ error }}</p>

          <footer>
            <button class="ghost-button" type="button" :disabled="saving" @click="close">취소</button>
            <button class="primary-button" type="submit" :disabled="saving">
              {{ saving ? '저장 중' : '후보로 저장' }}
            </button>
          </footer>
        </form>
      </section>
    </div>
  </Teleport>
</template>
