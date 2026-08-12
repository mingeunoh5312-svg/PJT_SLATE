<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { defaultPortfolioImage, defaultProfileImage } from '../constants/defaultImages'
import { slateApi } from '../services/api'

const props = defineProps({ currentUser: Object })
const emit = defineEmits(['login'])
const route = useRoute()
const router = useRouter()

const profile = ref(null)
const followState = ref(null)
const loading = ref(false)
const followLoading = ref(false)
const error = ref('')
const profileImageFailed = ref(false)

const profileId = computed(() => Number(route.params.profileId || 0))
const ownProfile = computed(() => Number(profile.value?.userId) === Number(props.currentUser?.userId))
const portfolioItems = computed(() => Array.isArray(profile.value?.portfolioItems) ? profile.value.portfolioItems : [])
const portfolioItemId = computed(() => Number(route.params.portfolioItemId || 0))
const selectedPortfolioItem = computed(() => portfolioItems.value.find(
  (item) => Number(item.portfolioItemId) === portfolioItemId.value
) || null)

async function loadProfile() {
  loading.value = true
  error.value = ''
  profile.value = null
  profileImageFailed.value = false
  followState.value = null
  try {
    profile.value = await slateApi.publicProfile(profileId.value)
    if (props.currentUser) followState.value = await slateApi.followStatus(profileId.value)
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  if (!props.currentUser) {
    emit('login')
    return
  }
  if (!followState.value || ownProfile.value || followLoading.value) return
  followLoading.value = true
  error.value = ''
  try {
    followState.value = followState.value.following
      ? await slateApi.unfollowProfile(profileId.value)
      : await slateApi.followProfile(profileId.value)
  } catch (err) {
    error.value = err.message
  } finally {
    followLoading.value = false
  }
}

function portfolioThumbnail(item) {
  return item?.uploadedThumbnailUrl || item?.thumbnailUrl || defaultPortfolioImage
}

function portfolioMeta(item) {
  return [item?.roleName, item?.creditName, item?.sourceType].filter(Boolean).join(' · ') || '포트폴리오 정보'
}
watch([profileId, () => props.currentUser?.userId], loadProfile, { immediate: true })
</script>

<template>
  <section class="public-profile-page">
    <button class="ghost-button inline" type="button" @click="router.back()">뒤로</button>
    <p v-if="loading" class="muted">프로필을 불러오는 중입니다.</p>
    <p v-else-if="error && !profile" class="error-text">{{ error }}</p>
    <article v-else-if="profile" class="public-profile-card">
      <div class="public-profile-portrait">
        <img :src="profile.profileImageUrl && !profileImageFailed ? profile.profileImageUrl : defaultProfileImage" :alt="`${profile.displayName} 프로필`" @error="profileImageFailed = true">
      </div>
      <div class="public-profile-content">
        <span class="eyebrow">공개 프로필</span>
        <h1>{{ profile.displayName || profile.nickname }}</h1>
        <p>{{ profile.shortIntro || '등록된 한 줄 소개가 없습니다.' }}</p>
        <div class="subline">
          <span>{{ profile.publicRegionName || '지역 정보 없음' }}</span>
          <span>{{ profile.experienceLevel || '경력 정보 없음' }}</span>
        </div>
        <div v-if="profile.roles?.length" class="public-profile-tags">
          <span v-for="role in profile.roles" :key="role.roleId">{{ role.roleName }}</span>
        </div>
        <p class="public-profile-detail">{{ profile.detailIntro || '등록된 상세 소개가 없습니다.' }}</p>
        <button
          v-if="!ownProfile"
          class="primary-button inline public-follow-button"
          :class="{ 'is-following': followState?.following }"
          type="button"
          :aria-pressed="Boolean(followState?.following)"
          :disabled="followLoading || (props.currentUser && !followState)"
          @click="toggleFollow"
        >
          {{ followLoading ? '처리 중' : followState?.following ? '팔로우 취소' : '팔로우' }}
          <span v-if="followState">· {{ followState.followerCount }}</span>
        </button>
        <p v-if="error" class="error-text">{{ error }}</p>
      </div>
    </article>
    <section v-if="profile" id="portfolio" class="public-profile-portfolio">
      <header>
        <span class="eyebrow">Portfolio</span>
        <h2>{{ selectedPortfolioItem ? selectedPortfolioItem.title : '포트폴리오' }}</h2>
        <RouterLink
          v-if="selectedPortfolioItem"
          class="ghost-button inline"
          :to="{ name: 'public-profile', params: { profileId } , hash: '#portfolio' }"
        >
          목록
        </RouterLink>
      </header>
      <article v-if="selectedPortfolioItem" class="public-profile-portfolio-detail">
        <img :src="portfolioThumbnail(selectedPortfolioItem)" :alt="`${selectedPortfolioItem.title} 썸네일`" @error="selectedPortfolioItem.uploadedThumbnailUrl ? selectedPortfolioItem.uploadedThumbnailUrl = null : selectedPortfolioItem.thumbnailUrl = null">
        <div>
          <strong>{{ selectedPortfolioItem.title }}</strong>
          <small>{{ portfolioMeta(selectedPortfolioItem) }}</small>
          <p>{{ selectedPortfolioItem.description || '등록된 설명이 없습니다.' }}</p>
          <a v-if="selectedPortfolioItem.url" :href="selectedPortfolioItem.url" target="_blank" rel="noreferrer">작품 링크 열기 ›</a>
        </div>
      </article>
      <div v-else-if="portfolioItems.length" class="public-profile-portfolio-grid" :class="{ 'single-item': portfolioItems.length === 1 }">
        <RouterLink
          v-for="item in portfolioItems"
          :key="item.portfolioItemId"
          :to="{ name: 'public-profile-portfolio-detail', params: { profileId, portfolioItemId: item.portfolioItemId } }"
        >
          <img :src="portfolioThumbnail(item)" :alt="`${item.title} 썸네일`" @error="item.uploadedThumbnailUrl ? item.uploadedThumbnailUrl = null : item.thumbnailUrl = null">
          <strong>{{ item.title }}</strong>
          <small>{{ portfolioMeta(item) }}</small>
          <p>{{ item.description || '등록된 설명이 없습니다.' }}</p>
        </RouterLink>
      </div>
      <p v-else class="public-profile-empty">공개된 포트폴리오가 없습니다.</p>
    </section>
  </section>
</template>
