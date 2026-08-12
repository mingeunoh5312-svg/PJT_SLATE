<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import RegisterAccountFields from '../components/auth/RegisterAccountFields.vue'
import RegisterAgreements from '../components/auth/RegisterAgreements.vue'
import RegisterHero from '../components/auth/RegisterHero.vue'
import { setToken, slateApi } from '../services/api'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({
  loginId: '', email: '', password: '', passwordConfirm: '', nickname: '',
  birthDate: '', phone: '', address: ''
})
const agreements = reactive({ all: false, terms: false, privacy: false })

async function submit() {
  error.value = ''
  if (form.password !== form.passwordConfirm) {
    error.value = '비밀번호 확인이 일치하지 않습니다.'
    return
  }
  if (!agreements.terms || !agreements.privacy) {
    error.value = '필수 약관에 동의해주세요.'
    return
  }
  loading.value = true
  try {
    await slateApi.register({
      loginId: form.loginId,
      email: form.email,
      password: form.password,
      nickname: form.nickname,
      accountType: 'USER'
    })
    const login = await slateApi.login(form.loginId, form.password)
    setToken(login.accessToken)
    sessionStorage.setItem('slate.register.result', 'USER')
    window.dispatchEvent(new CustomEvent('slate-auth-changed'))
    await router.replace({ name: 'register-complete' })
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="register-screen">
    <RegisterHero />
    <form class="register-card" @submit.prevent="submit">
      <div class="register-route-head">
        <RouterLink :to="{ name: 'register' }" aria-label="가입 유형 선택으로 돌아가기">←</RouterLink>
        <div><span class="eyebrow">일반 사용자</span><h2>회원가입</h2></div>
      </div>
      <p v-if="error" class="error-text" role="alert">{{ error }}</p>
      <RegisterAccountFields :model-value="form" account-type="USER" />
      <RegisterAgreements :model-value="agreements" />
      <button class="register-submit" type="submit" :disabled="loading">{{ loading ? '처리 중' : '가입하기' }}</button>
      <p class="register-login-link">이미 계정이 있으신가요? <RouterLink to="/login">로그인</RouterLink></p>
    </form>
  </section>
</template>
