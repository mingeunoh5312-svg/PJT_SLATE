<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminLayout from './layouts/AdminLayout.vue'
import AppLayout from './layouts/AppLayout.vue'
import AuthLayout from './layouts/AuthLayout.vue'
import { getDemoAccessCode, getToken, isDemoAccessGateEnabled, setToken, slateApi } from './services/api'

const router = useRouter()
const route = useRoute()
const currentUser = ref(null)
const shellProfile = ref(null)
const authReady = ref(false)
const notificationOpen = ref(false)
const notificationLoading = ref(false)
const notifications = ref([])
const unreadCount = ref(0)

const layoutComponent = computed(() => {
  if (route.meta.layout === 'auth') return AuthLayout
  if (route.meta.layout === 'admin') return AdminLayout
  return AppLayout
})

const isShellLayout = computed(() => route.meta.layout !== 'auth')

function loginRoute(redirect = route.fullPath) {
  const target = route.path === '/login' || route.path.startsWith('/register') ? '/' : redirect
  return { name: 'login', query: { redirect: target } }
}

async function loadMe() {
  authReady.value = false
  if (isDemoAccessGateEnabled() && !getDemoAccessCode()) {
    currentUser.value = null
    shellProfile.value = null
    notifications.value = []
    unreadCount.value = 0
    authReady.value = true
    return
  }
  if (!getToken()) {
    currentUser.value = null
    shellProfile.value = null
    notifications.value = []
    unreadCount.value = 0
    authReady.value = true
    if (route.meta.requiresAuth) router.replace(loginRoute())
    return
  }
  try {
    currentUser.value = await slateApi.me()
    await loadShellProfile()
    await refreshNotifications(false)
  } catch (error) {
    setToken(null)
    currentUser.value = null
    shellProfile.value = null
    notifications.value = []
    unreadCount.value = 0
    if (route.meta.requiresAuth) router.replace(loginRoute())
  } finally {
    authReady.value = true
  }
}

async function loadShellProfile() {
  shellProfile.value = null
  if (currentUser.value?.accountType !== 'USER') return
  try {
    shellProfile.value = await slateApi.myProfile()
  } catch (error) {
    shellProfile.value = null
  }
}

async function refreshNotifications(loadList = notificationOpen.value) {
  if (!currentUser.value) {
    notifications.value = []
    unreadCount.value = 0
    return
  }
  try {
    const count = await slateApi.unreadNotifications()
    unreadCount.value = count.unreadCount || 0
    if (loadList || currentUser.value.accountType !== 'ADMIN') {
      notificationLoading.value = loadList
      const rows = await slateApi.notifications({ limit: 20 })
      const visibleRows = Array.isArray(rows) ? rows.filter(isVisibleNotification) : []
      unreadCount.value = visibleRows.filter((item) => item.readYn === 'N').length
      if (loadList) notifications.value = visibleRows
    }
  } catch (error) {
    notifications.value = []
    unreadCount.value = 0
  } finally {
    notificationLoading.value = false
  }
}

function isVisibleNotification(notification) {
  if (!notification || currentUser.value?.accountType === 'ADMIN') return true
  const text = `${notification.notificationType || ''} ${notification.targetType || ''} ${notification.title || ''} ${notification.body || ''}`
  return notification.targetType !== 'COMPANY_APPLICATION'
    && !text.includes('회사 승인')
    && !text.includes('기업 승인')
}

function handleViewLogin() {
  router.push(loginRoute())
}

function logout() {
  setToken(null)
  currentUser.value = null
  shellProfile.value = null
  notificationOpen.value = false
  notifications.value = []
  unreadCount.value = 0
  window.dispatchEvent(new CustomEvent('slate-auth-changed'))
  if (route.meta.requiresAuth) router.push(loginRoute())
}

function handleDemoAccessRejected() {
  if (!isDemoAccessGateEnabled() || route.name === 'demo-access') return
  currentUser.value = null
  shellProfile.value = null
  notificationOpen.value = false
  notifications.value = []
  unreadCount.value = 0
  router.replace({
    name: 'demo-access',
    query: { redirect: route.fullPath || '/' }
  })
}

async function openNotificationPanel() {
  if (!currentUser.value) {
    await router.push(loginRoute())
    return
  }
  notificationOpen.value = true
  await refreshNotifications(true)
}

async function toggleNotificationPanel() {
  if (notificationOpen.value) {
    notificationOpen.value = false
    return
  }
  await openNotificationPanel()
}

async function markNotificationRead(notification) {
  await slateApi.markNotificationRead(notification.notificationId)
  notification.readYn = 'Y'
  await refreshNotifications(false)
}

async function markAllRead() {
  await slateApi.markAllNotificationsRead()
  await refreshNotifications(true)
}

async function hideNotification(notification) {
  await slateApi.hideNotification(notification.notificationId)
  notifications.value = notifications.value.filter((item) => item.notificationId !== notification.notificationId)
  await refreshNotifications(false)
}

watch(() => route.fullPath, () => {
  notificationOpen.value = false
})

onMounted(() => {
  loadMe()
  window.addEventListener('slate-auth-changed', loadMe)
  window.addEventListener('slate-profile-changed', loadShellProfile)
  window.addEventListener('slate-demo-access-rejected', handleDemoAccessRejected)
})

onBeforeUnmount(() => {
  window.removeEventListener('slate-auth-changed', loadMe)
  window.removeEventListener('slate-profile-changed', loadShellProfile)
  window.removeEventListener('slate-demo-access-rejected', handleDemoAccessRejected)
})
</script>

<template>
  <component
    :is="layoutComponent"
    :current-user="currentUser"
    :shell-profile="shellProfile"
    :notification-open="notificationOpen"
    :notification-loading="notificationLoading"
    :notifications="notifications"
    :unread-count="unreadCount"
    @logout="logout"
    @toggle-notification-panel="toggleNotificationPanel"
    @mark-notification-read="markNotificationRead"
    @mark-all-read="markAllRead"
    @hide-notification="hideNotification"
    @close-notifications="notificationOpen = false"
  >
    <section v-if="isShellLayout && route.meta.requiresAuth && !authReady" class="login-panel">
      <h2>인증 확인 중</h2>
      <p>계정 상태를 확인하고 있습니다.</p>
    </section>
    <RouterView
      v-else
      :current-user="currentUser"
      @login="handleViewLogin"
      @open-notifications="openNotificationPanel"
    />
  </component>
</template>
