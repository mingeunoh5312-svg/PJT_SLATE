<script setup>
import { computed, onMounted } from 'vue'

const props = defineProps({ currentUser: Object })
const nextRoute = computed(() => props.currentUser ? { name: 'profile' } : { name: 'login' })
const nextLabel = computed(() => props.currentUser ? '내 정보 설정하기' : '로그인하기')

onMounted(() => sessionStorage.removeItem('slate.register.result'))
</script>

<template>
  <section class="auth-result-screen">
    <RouterLink class="login-brand" to="/"><span aria-hidden="true">▰</span>SLATE</RouterLink>
    <article class="auth-result-card">
      <span class="auth-result-icon" aria-hidden="true">✓</span>
      <span class="eyebrow">가입 완료</span>
      <h1>Slate에서 다음 작품을 시작하세요</h1>
      <p>회원가입이 완료되었습니다. 프로필과 포트폴리오를 설정하면 더 정확한 팀과 프로젝트를 만날 수 있습니다.</p>
      <div class="auth-result-actions">
        <RouterLink class="primary-button" :to="nextRoute">{{ nextLabel }}</RouterLink>
        <RouterLink class="ghost-button" to="/">홈으로</RouterLink>
      </div>
    </article>
  </section>
</template>
