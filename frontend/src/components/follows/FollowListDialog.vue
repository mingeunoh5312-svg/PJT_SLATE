<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { defaultProfileImage } from '../../constants/defaultImages'
import { slateApi } from '../../services/api'

const props = defineProps({
  open: Boolean,
  mode: {
    type: String,
    default: 'followers'
  },
  profileId: [Number, String],
  currentUserId: [Number, String],
  title: String
})

const emit = defineEmits(['close', 'counts-changed'])
const items = ref([])
const totalCount = ref(0)
const limit = ref(20)
const offset = ref(0)
const hasMore = ref(false)
const loading = ref(false)
const error = ref('')
const pendingProfileIds = ref(new Set())
const closeButton = ref(null)
let listRequestId = 0
let previousBodyOverflow = ''
let bodyLocked = false

function resetList() {
  items.value = []
  totalCount.value = 0
  limit.value = 20
  offset.value = 0
  hasMore.value = false
  loading.value = false
  error.value = ''
  pendingProfileIds.value = new Set()
}

function isValidProfileId(value) {
  return Number.isFinite(Number(value)) && Number(value) > 0
}

async function loadList(reset = false) {
  if (!props.open || !isValidProfileId(props.profileId) || loading.value) return
  const requestId = ++listRequestId
  const requestOffset = reset ? 0 : items.value.length
  loading.value = true
  error.value = ''
  try {
    const request = props.mode === 'following' ? slateApi.profileFollowing : slateApi.profileFollowers
    const data = await request(Number(props.profileId), { limit: limit.value, offset: requestOffset })
    if (requestId !== listRequestId || !props.open) return
    const nextItems = Array.isArray(data?.items) ? data.items : []
    items.value = reset ? nextItems : [...items.value, ...nextItems]
    totalCount.value = Number(data?.totalCount || 0)
    limit.value = Number(data?.limit || limit.value)
    offset.value = Number(data?.offset || requestOffset)
    hasMore.value = Boolean(data?.hasMore)
  } catch (err) {
    if (requestId !== listRequestId || !props.open) return
    error.value = err.message || '팔로우 목록을 불러오지 못했습니다.'
  } finally {
    if (requestId === listRequestId) loading.value = false
  }
}

async function toggleFollow(item) {
  const profileId = Number(item?.profileId)
  if (!isValidProfileId(profileId) || pendingProfileIds.value.has(profileId)) return
  pendingProfileIds.value = new Set([...pendingProfileIds.value, profileId])
  error.value = ''
  try {
    const data = item.followingByCurrentUser
      ? await slateApi.unfollowProfile(profileId)
      : await slateApi.followProfile(profileId)
    const target = items.value.find((candidate) => Number(candidate.profileId) === profileId)
    if (target) target.followingByCurrentUser = Boolean(data?.following)
    emit('counts-changed')
  } catch (err) {
    error.value = err.message || '팔로우 상태를 변경하지 못했습니다.'
  } finally {
    const nextPending = new Set(pendingProfileIds.value)
    nextPending.delete(profileId)
    pendingProfileIds.value = nextPending
  }
}

function close() {
  emit('close')
}

function lockBody() {
  if (bodyLocked) return
  previousBodyOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  bodyLocked = true
}

function unlockBody() {
  if (!bodyLocked) return
  document.body.style.overflow = previousBodyOverflow
  bodyLocked = false
}

function handleKeydown(event) {
  if (event.key === 'Escape' && props.open) close()
}

function displayName(item) {
  return item?.displayName || item?.nickname || '사용자'
}

function initial(item) {
  return displayName(item).trim().charAt(0) || 'S'
}

function formatFollowedAt(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' }).format(date)
}

watch(
  [() => props.open, () => props.mode, () => props.profileId],
  async ([open]) => {
    listRequestId += 1
    resetList()
    if (!open || !isValidProfileId(props.profileId)) {
      unlockBody()
      return
    }
    lockBody()
    await nextTick()
    closeButton.value?.focus()
    loadList(true)
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  listRequestId += 1
  unlockBody()
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="follow-dialog-backdrop"
      @click.self="close"
      @keydown="handleKeydown"
    >
      <section
        class="follow-dialog-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="follow-dialog-title"
      >
        <header class="follow-dialog-header">
          <div>
            <span>{{ mode === 'following' ? 'Following' : 'Followers' }}</span>
            <h2 id="follow-dialog-title">{{ title || (mode === 'following' ? '팔로잉' : '팔로워') }}</h2>
          </div>
          <button ref="closeButton" type="button" aria-label="팔로우 목록 닫기" @click="close">닫기</button>
        </header>

        <p v-if="error" class="follow-dialog-error" role="alert">{{ error }}</p>
        <p v-if="loading && items.length === 0" class="follow-dialog-state">목록을 불러오는 중입니다.</p>
        <p v-else-if="!loading && items.length === 0 && !error" class="follow-dialog-state">표시할 사용자가 없습니다.</p>

        <div v-else class="follow-dialog-list">
          <article v-for="item in items" :key="item.profileId" class="follow-dialog-row">
            <RouterLink class="follow-dialog-main" :to="{ name: 'public-profile', params: { profileId: item.profileId } }" @click="close">
              <img class="follow-initial-avatar" :src="item.profileImageUrl || defaultProfileImage" :alt="`${displayName(item)} 프로필 이미지`" @error="item.profileImageUrl = null">
              <div class="follow-dialog-copy">
                <strong>{{ displayName(item) }}</strong>
                <p>{{ item.shortIntro || '소개가 아직 없습니다.' }}</p>
                <small>
                  <span v-if="item.publicRegionName">{{ item.publicRegionName }}</span>
                  <span v-if="item.experienceLevel">{{ item.experienceLevel }}</span>
                  <span v-if="item.followedAt">{{ formatFollowedAt(item.followedAt) }}</span>
                </small>
              </div>
            </RouterLink>
            <span v-if="Number(item.userId) === Number(currentUserId)" class="follow-self-label">내 프로필</span>
            <button
              v-else
              class="follow-toggle-button"
              :class="{ following: item.followingByCurrentUser }"
              type="button"
              :disabled="pendingProfileIds.has(Number(item.profileId))"
              :aria-label="`${displayName(item)} ${item.followingByCurrentUser ? '팔로우 취소' : '팔로우'}`"
              @click.stop="toggleFollow(item)"
            >
              {{ pendingProfileIds.has(Number(item.profileId)) ? '처리 중' : item.followingByCurrentUser ? '팔로잉' : '팔로우' }}
            </button>
          </article>
        </div>

        <footer class="follow-dialog-footer">
          <span>총 {{ totalCount }}명</span>
          <button v-if="hasMore" type="button" :disabled="loading" @click="loadList(false)">
            {{ loading ? '불러오는 중' : '더 보기' }}
          </button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>
