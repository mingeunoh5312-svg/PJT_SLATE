<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import loginStudio from '../assets/auth/login-studio.webp'
import { setToken, slateApi } from '../services/api'

defineProps({
  currentUser: Object
})

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const notice = ref('')
const passwordVisible = ref(false)
const form = reactive({
  loginId: '',
  password: ''
})

const testAccounts = [
  { label: '일반/팀장', description: '매칭, 팀 관리 검증', loginId: 'leader' },
  { label: '팀원', description: '지원/초대 검증', loginId: 'camera' },
  { label: '관리자', description: '운영 화면 검증', loginId: 'admin' },
  { label: '승인 회사', description: '회사 공모전 관리', loginId: 'approved-company' },
  { label: '승인 대기 회사', description: '차단 메시지 검증', loginId: 'company' }
]

const primaryDemoAccount = testAccounts[0]

const redirectTarget = computed(() => {
  const redirect = String(route.query.redirect || '/')
  if (redirect.startsWith('/login') || redirect.startsWith('/register')) return '/'
  if (!redirect.startsWith('/') || redirect.startsWith('//') || redirect.includes('\\')) return '/'
  return redirect
})
const isCompanyPendingError = computed(() => error.value.includes('승인 검토 중'))

async function login(loginId = form.loginId, password = form.password || 'slate1234') {
  loading.value = true
  error.value = ''
  notice.value = ''
  try {
    const data = await slateApi.login(loginId, password)
    setToken(data.accessToken)
    window.dispatchEvent(new CustomEvent('slate-auth-changed'))
    await router.push(redirectTarget.value)
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function fillTestAccount(account) {
  form.loginId = account.loginId
  form.password = 'slate1234'
  login(account.loginId, 'slate1234')
}

function showRecoveryNotice(type) {
  error.value = ''
  notice.value = `${type} 찾기는 아직 프로토타입에서 제공되지 않습니다. 테스트 계정은 leader / slate1234를 사용해주세요.`
}
</script>

<template>
  <section class="login-screen">
    <img :src="loginStudio" alt="" class="login-background">
    <div class="login-wash"></div>

    <header class="login-brand-row">
      <RouterLink class="login-brand" to="/">
        <span aria-hidden="true">▰</span>
        SLATE
      </RouterLink>
      <RouterLink class="login-register-top" to="/register">회원가입</RouterLink>
    </header>

    <div class="login-content">
      <section class="login-copy" aria-labelledby="login-title">
        <span>모두가 영화인이 될 수 있다</span>
        <h1 id="login-title">함께 만들 다음 작품을 시작하세요</h1>
      </section>

      <form class="login-card" @submit.prevent="login()">
        <p v-if="error" class="error-text">{{ error }}</p>
        <RouterLink v-if="isCompanyPendingError" class="notice-text login-pending-link" :to="{ name: 'register-company-pending' }">기업 승인 대기 안내 보기</RouterLink>
        <p v-if="notice" class="notice-text">{{ notice }}</p>
        <label class="login-field">
          <span>아이디</span>
          <input v-model.trim="form.loginId" autocomplete="username" required placeholder="아이디를 입력해주세요">
        </label>
        <label class="login-field">
          <span>비밀번호</span>
          <span class="login-password-box">
            <input
              v-model="form.password"
              :type="passwordVisible ? 'text' : 'password'"
              autocomplete="current-password"
              required
              placeholder="비밀번호를 입력해주세요"
            >
            <button
              type="button"
              :aria-label="passwordVisible ? '비밀번호 숨기기' : '비밀번호 보기'"
              @click="passwordVisible = !passwordVisible"
            >
              ◉
            </button>
          </span>
        </label>
        <button class="login-submit" type="submit" :disabled="loading">
          {{ loading ? '확인 중' : '로그인' }}
        </button>
        <div class="login-join">
          <span></span>
          <small>계정이 없으신가요?</small>
          <span></span>
        </div>
        <RouterLink class="login-join-link" to="/register">
          회원가입
          <span aria-hidden="true">→</span>
        </RouterLink>
      </form>

      <nav class="login-help-links" aria-label="계정 찾기">
        <button type="button" aria-label="아이디 찾기" @click="showRecoveryNotice('아이디')">
          <span aria-hidden="true">♙</span>
          아이디 찾기
        </button>
        <i aria-hidden="true"></i>
        <button type="button" aria-label="비밀번호 찾기" @click="showRecoveryNotice('비밀번호')">
          <span aria-hidden="true">▣</span>
          비밀번호 찾기
        </button>
      </nav>

      <button class="login-demo-card" type="button" :disabled="loading" @click="fillTestAccount(primaryDemoAccount)">
        <span class="login-demo-icon" aria-hidden="true">▰</span>
        <span>
          <strong>데모 계정으로 둘러보기</strong>
          <small>슬레이트의 주요 기능을 체험해보세요.</small>
        </span>
        <i aria-hidden="true">›</i>
      </button>

      <section class="login-quick-panel" aria-label="빠른 계정 선택">
        <header>
          <strong>빠른 선택</strong>
          <small>비밀번호는 모두 slate1234입니다.</small>
        </header>
        <div class="login-quick-grid">
          <button
            v-for="account in testAccounts"
            :key="account.loginId"
            type="button"
            :disabled="loading"
            @click="fillTestAccount(account)"
          >
            <strong>{{ account.label }}</strong>
            <span>{{ account.loginId }}</span>
          </button>
        </div>
      </section>
    </div>

  </section>
</template>
