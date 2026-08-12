<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { defaultProfileImage } from '../constants/defaultImages'

const props = defineProps({
  currentUser: Object,
  shellProfile: Object,
  notificationOpen: Boolean,
  notificationLoading: Boolean,
  notifications: {
    type: Array,
    default: () => []
  },
  unreadCount: {
    type: Number,
    default: 0
  },
  section: {
    type: String,
    default: 'app'
  }
})

const emit = defineEmits([
  'logout',
  'toggle-notification-panel',
  'mark-notification-read',
  'mark-all-read',
  'hide-notification',
  'close-notifications'
])

const router = useRouter()
const route = useRoute()
const notificationWrap = ref(null)
const theme = ref('dark')

const siteNavItems = computed(() => [
  { label: '홈', to: { name: 'home' }, active: route.name === 'home' },
  { label: '탐색', to: { name: 'discover' }, active: route.name === 'discover' || route.path.startsWith('/matching') },
  { label: '작업물', to: { name: 'works' }, active: route.path.startsWith('/works') || route.path.startsWith('/boards') },
  { label: '공모전', to: { name: 'contests' }, active: route.path.startsWith('/contests') },
  {
    label: 'AI 로케이션',
    to: { name: 'locations' },
    active: route.name === 'locations' || route.name === 'teams-locations'
  },
  { label: '내 팀', to: { name: 'teams' }, active: route.path.startsWith('/teams') },
  { label: '작업공간', to: { name: 'workspace' }, active: route.path.startsWith('/workspace') },
  { label: '운영', to: { name: 'admin' }, active: route.path.startsWith('/admin') }
])

const visibleNotifications = computed(() => props.notifications.filter(isVisibleNotification))
const avatarSrc = computed(() => props.shellProfile?.profileImageUrl || defaultProfileImage)
const footerColumns = [
  { title: '제작자', items: ['프로필', '포트폴리오', '팀 탐색', '내 지원/초대'] },
  { title: '팀 · 기업', items: ['팀 만들기', '모집 공고', '기업 가입', '공모전 개설'] },
  { title: '플랫폼', items: ['공모전', 'AI 로케이션', '작업물 게시판', '운영 정책'] }
]

function isVisibleNotification(notification) {
  if (!notification || props.currentUser?.accountType === 'ADMIN') return true
  const text = `${notification.notificationType || ''} ${notification.targetType || ''} ${notification.title || ''} ${notification.body || ''}`
  return notification.targetType !== 'COMPANY_APPLICATION'
    && !text.includes('회사 승인')
    && !text.includes('기업 승인')
}

function notificationRoute(notification) {
  const targetId = notification?.targetId
  if (!targetId) return null
  if (notification.targetType === 'TEAM') {
    const text = `${notification.notificationType || ''} ${notification.title || ''} ${notification.body || ''}`
    if (text.includes('추천')) return { name: 'matching-teams', query: { view: 'saved' } }
    if (text.includes('초대')) return { name: 'teams-invitations' }
    return { name: 'teams-detail', params: { teamId: targetId } }
  }
  if (notification.targetType === 'CONTEST') return { name: 'contests-detail', params: { contestId: targetId } }
  if (notification.targetType === 'BOARD_POST') return { name: 'boards-detail', params: { postId: targetId } }
  return null
}

function openNotification(notification) {
  const target = notificationRoute(notification)
  if (!target) return
  if (notification.readYn === 'N') emit('mark-notification-read', notification)
  emit('close-notifications')
  router.push(target)
}

function loginRoute(redirect = route.fullPath) {
  const target = route.path === '/login' || route.path.startsWith('/register') ? '/' : redirect
  return { name: 'login', query: { redirect: target } }
}

function openProfileFromAccount() {
  if (props.currentUser) {
    router.push({ name: 'profile' })
    return
  }
  router.push(loginRoute())
}

function applyTheme(nextTheme) {
  const root = document.documentElement
  root.classList.remove('dark', 'light')
  root.classList.add(nextTheme)
}

function toggleTheme() {
  const nextTheme = theme.value === 'dark' ? 'light' : 'dark'
  theme.value = nextTheme
  applyTheme(nextTheme)
  try {
    localStorage.setItem('slate-theme', nextTheme)
  } catch (error) {
    // Theme persistence is non-critical.
  }
}

function closeNotificationsFromOutside(event) {
  if (!props.notificationOpen) return
  const target = event.target
  if (notificationWrap.value && target instanceof Node && notificationWrap.value.contains(target)) return
  emit('close-notifications')
}

watch(() => route.fullPath, () => {
  emit('close-notifications')
})

onMounted(() => {
  try {
    const stored = localStorage.getItem('slate-theme')
    theme.value = stored === 'light' ? 'light' : 'dark'
  } catch (error) {
    theme.value = document.documentElement.classList.contains('light') ? 'light' : 'dark'
  }
  applyTheme(theme.value)
  window.addEventListener('pointerdown', closeNotificationsFromOutside, true)
})

onBeforeUnmount(() => {
  window.removeEventListener('pointerdown', closeNotificationsFromOutside, true)
})
</script>

<template>
  <div
    class="app-shell site-shell"
    :class="{
      'route-home': route.name === 'home',
      'route-matching': route.path.startsWith('/matching'),
      'route-locations': route.name === 'locations' || route.name === 'teams-locations',
      'admin-shell': section === 'admin'
    }"
  >
    <header class="site-header">
      <div class="site-header-inner">
        <RouterLink class="site-brand" to="/">
          <span class="site-mark" aria-hidden="true">
            <span>S</span>
            <i />
          </span>
          <span class="site-brand-name">Slate</span>
          <span class="site-version">v0 · design draft</span>
        </RouterLink>

        <nav class="site-nav" aria-label="주요 메뉴">
          <RouterLink
            v-for="item in siteNavItems"
            :key="item.label"
            :to="item.to"
            class="site-nav-link"
            :class="{ active: item.active }"
            :aria-current="item.active ? 'page' : undefined"
          >
            {{ item.label }}
          </RouterLink>
        </nav>

        <div class="site-actions">
          <button class="site-search-button" type="button" @click="router.push({ name: 'boards-search' })">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.5-3.5" />
            </svg>
            <span>팀, 제작자, 작업물 검색</span>
            <small>⌘K</small>
          </button>

          <div ref="notificationWrap" class="notification-wrap site-notification-wrap">
            <button
              class="site-icon-button bell-button"
              type="button"
              title="알림"
              aria-label="알림"
              @click="emit('toggle-notification-panel')"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M18 16v-5a6 6 0 0 0-12 0v5l-2 2h16l-2-2Z" />
                <path d="M9.5 20a2.7 2.7 0 0 0 5 0" />
              </svg>
              <span v-if="unreadCount">{{ unreadCount }}</span>
            </button>

            <section v-if="notificationOpen" class="notification-panel site-notification-panel">
              <div class="notification-head">
                <strong>알림</strong>
                <button class="ghost-button" type="button" @click="emit('mark-all-read')">모두 읽음</button>
              </div>
              <p v-if="notificationLoading" class="muted">불러오는 중입니다.</p>
              <article
                v-for="notification in visibleNotifications"
                :key="notification.notificationId"
                class="notification-row"
                :class="{ unread: notification.readYn === 'N' }"
              >
                <div>
                  <span>{{ notification.notificationType }}</span>
                  <strong>{{ notification.title }}</strong>
                  <p>{{ notification.body }}</p>
                </div>
                <div class="row-actions">
                  <button
                    v-if="notificationRoute(notification)"
                    class="ghost-button"
                    type="button"
                    @click="openNotification(notification)"
                  >
                    확인
                  </button>
                  <button
                    v-if="notification.readYn === 'N'"
                    class="ghost-button"
                    type="button"
                    @click="emit('mark-notification-read', notification)"
                  >
                    읽음
                  </button>
                  <button class="icon-button" type="button" title="숨김" @click="emit('hide-notification', notification)">×</button>
                </div>
              </article>
              <p v-if="!notificationLoading && visibleNotifications.length === 0" class="muted">최근 30일 알림이 없습니다.</p>
            </section>
          </div>

          <button
            class="site-icon-button"
            type="button"
            :aria-label="theme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환'"
            :title="theme === 'dark' ? '라이트 모드' : '다크 모드'"
            @click="toggleTheme"
          >
            <svg v-if="theme === 'dark'" viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="12" cy="12" r="4" />
              <path d="M12 2v2M12 20v2M4.93 4.93l1.42 1.42M17.65 17.65l1.42 1.42M2 12h2M20 12h2M4.93 19.07l1.42-1.42M17.65 6.35l1.42-1.42" />
            </svg>
            <svg v-else viewBox="0 0 24 24" aria-hidden="true">
              <path d="M20 15.5A8.5 8.5 0 0 1 8.5 4 7 7 0 1 0 20 15.5Z" />
            </svg>
          </button>

          <RouterLink class="site-create-action" :to="{ name: 'teams-new' }">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 3l1.6 5.1L19 10l-5.4 1.9L12 17l-1.6-5.1L5 10l5.4-1.9L12 3Z" />
              <path d="M19 16l.7 2.3L22 19l-2.3.7L19 22l-.7-2.3L16 19l2.3-.7L19 16Z" />
            </svg>
            팀 만들기
          </RouterLink>

          <button class="site-avatar" type="button" :aria-label="currentUser ? '내 프로필' : '로그인'" @click="openProfileFromAccount">
            <img v-if="currentUser" :src="avatarSrc" :alt="`${currentUser.nickname} 프로필 이미지`" @error="$event.currentTarget.src = defaultProfileImage">
          </button>
        </div>
      </div>
    </header>

    <main class="main-stage">
      <slot />
    </main>

    <footer class="site-footer">
      <div class="site-footer-inner">
        <section class="site-footer-brand">
          <strong>Slate</strong>
          <p>영화·영상 제작자, 팀, 기업, 운영자를 잇는 제작 협업 플랫폼.</p>
          <small>© 2026 Slate Studio</small>
        </section>
        <section v-for="column in footerColumns" :key="column.title" class="site-footer-column">
          <h2>{{ column.title }}</h2>
          <ul>
            <li v-for="item in column.items" :key="item">{{ item }}</li>
          </ul>
        </section>
      </div>
    </footer>
  </div>
</template>
