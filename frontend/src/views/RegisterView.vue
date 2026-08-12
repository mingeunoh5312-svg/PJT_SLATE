<script setup>
import RegisterHero from '../components/auth/RegisterHero.vue'
</script>

<template>
  <section class="register-screen register-type-screen">
    <RegisterHero />
    <section class="register-card register-type-card" aria-labelledby="register-type-title">
      <span class="eyebrow">회원가입</span>
      <h2 id="register-type-title">가입 유형을 선택해주세요</h2>
      <div class="register-type-grid">
        <RouterLink :to="{ name: 'register-user' }">
          <span class="register-type-icon" aria-hidden="true">●</span>
          <strong>일반 사용자</strong>
          <small>프로필과 포트폴리오를 만들고 팀을 찾아보세요.</small>
          <i aria-hidden="true">›</i>
        </RouterLink>
        <RouterLink :to="{ name: 'register-company' }">
          <span class="register-type-icon company" aria-hidden="true">▥</span>
          <strong>기업 사용자</strong>
          <small>기업 승인 후 공모전과 모집 정보를 관리하세요.</small>
          <i aria-hidden="true">›</i>
        </RouterLink>
      </div>
      <p class="register-login-link">이미 계정이 있으신가요? <RouterLink to="/login">로그인</RouterLink></p>
    </section>
  </section>
</template>
