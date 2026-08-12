<script setup>
import { onMounted } from 'vue'

onMounted(() => sessionStorage.removeItem('slate.register.result'))
</script>

<template>
  <section class="auth-result-screen company-pending-screen">
    <RouterLink class="login-brand" to="/"><span aria-hidden="true">▰</span>SLATE</RouterLink>
    <article class="auth-result-card">
      <span class="auth-result-icon pending" aria-hidden="true">◷</span>
      <span class="eyebrow">기업 신청 접수</span>
      <h1>관리자 승인 검토 중입니다</h1>
      <p>기업 정보 확인이 완료되면 기업 계정으로 로그인할 수 있습니다. 승인 전에는 기업 전용 공모전 관리 기능을 사용할 수 없습니다.</p>
      <div class="auth-result-actions">
        <RouterLink class="primary-button" to="/login">로그인 화면</RouterLink>
        <RouterLink class="ghost-button" to="/">홈으로</RouterLink>
      </div>
    </article>
  </section>
</template>
