<script setup>
import registerStudio from '../../assets/auth/register-studio.png'
</script>

<template>
  <div class="register-left">
    <header class="register-brand-row">
      <RouterLink class="login-brand" to="/">
        <span aria-hidden="true">▰</span>
        SLATE
      </RouterLink>
    </header>

    <div class="register-copy">
      <span>모두가 영화인이 될 수 있다</span>
      <h1>새로운 제작 협업을<br>시작하세요</h1>
      <p>Slate는 영화와 영상을 만드는 모든 사람들이<br>함께 연결되고, 함께 완성하는 공간입니다.</p>
    </div>

    <img :src="registerStudio" alt="" class="register-background">

    <div class="register-benefits" aria-label="회원가입 장점">
      <article>
        <span class="mint" aria-hidden="true">♣</span>
        <div><strong>최고의 파트너를 만나요</strong><small>딱 맞는 팀원과 제작 파트너를 찾아보세요.</small></div>
      </article>
      <article>
        <span class="rose" aria-hidden="true">▰</span>
        <div><strong>함께 성장하세요</strong><small>포트폴리오와 프로젝트로 역량을 증명하세요.</small></div>
      </article>
      <article>
        <span class="sky" aria-hidden="true">♛</span>
        <div><strong>더 큰 작품을 만드세요</strong><small>아이디어를 프로젝트와 작품으로 완성하세요.</small></div>
      </article>
    </div>

    <button class="register-demo-card" type="button" @click="$router.push('/login')">
      <span class="login-demo-icon" aria-hidden="true">▰</span>
      <span><strong>데모 계정으로 둘러보기</strong><small>Slate의 주요 기능을 경험해보세요.</small></span>
      <i aria-hidden="true">›</i>
    </button>
  </div>
</template>
