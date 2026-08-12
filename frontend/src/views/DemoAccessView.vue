<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import demoAccessBackground from '../assets/auth/demo-access-background.webp'
import { setDemoAccessCode, slateApi } from '../services/api'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({
  code: ''
})

const redirectTarget = computed(() => {
  const redirect = String(route.query.redirect || '/')
  if (redirect.startsWith('/demo-access')) return '/'
  if (!redirect.startsWith('/') || redirect.startsWith('//') || redirect.includes('\\')) return '/'
  return redirect
})

async function submit() {
  const code = form.code.trim()
  if (!code) {
    error.value = '접근 코드를 입력해주세요.'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await slateApi.verifyDemoAccess(code)
    setDemoAccessCode(code)
    window.dispatchEvent(new CustomEvent('slate-auth-changed'))
    await router.replace(redirectTarget.value)
  } catch (err) {
    setDemoAccessCode(null)
    error.value = (err.message || '접근 코드를 확인해주세요.').replace(/접속 코드/g, '접근 코드')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="demo-access-screen">
    <img :src="demoAccessBackground" alt="" class="demo-access-background">
    <div class="demo-access-wash"></div>
    <form
      class="demo-access-panel"
      :aria-busy="loading ? 'true' : 'false'"
      aria-labelledby="demo-access-title"
      novalidate
      @submit.prevent="submit"
    >
      <div class="demo-access-brand" aria-label="SLATE">
        <span class="demo-access-mark" aria-hidden="true">▰</span>
        <strong>SLATE</strong>
      </div>
      <p class="demo-access-kicker">서비스 준비 안내</p>
      <h1 id="demo-access-title">SLATE 서비스 준비 중입니다</h1>
      <p id="demo-access-copy" class="demo-access-copy">
        사전 접근 코드가 있는 사용자만 입장할 수 있습니다. 통과 후 입력한 페이지로 이동합니다.
      </p>
      <p
        v-if="error"
        id="demo-access-error"
        class="demo-access-error"
        role="alert"
        aria-live="polite"
      >
        {{ error }}
      </p>
      <label class="demo-access-field" for="demo-access-code">
        <span>접근 코드</span>
        <input
          id="demo-access-code"
          v-model.trim="form.code"
          type="password"
          autocomplete="one-time-code"
          spellcheck="false"
          required
          aria-required="true"
          autofocus
          :aria-invalid="error ? 'true' : 'false'"
          :aria-describedby="error ? 'demo-access-copy demo-access-error' : 'demo-access-copy'"
        >
      </label>
      <button type="submit" :disabled="loading">
        {{ loading ? '확인 중' : '입장하기' }}
      </button>
    </form>
  </section>
</template>

<style scoped>
.demo-access-screen {
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px 20px;
  overflow: hidden;
  background: #111111;
  color: #f7f3ed;
}

.demo-access-background {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.demo-access-wash {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(10, 10, 10, 0.44) 0%, rgba(10, 10, 10, 0.78) 100%),
    rgba(12, 12, 12, 0.46);
}

.demo-access-panel {
  position: relative;
  z-index: 1;
  box-sizing: border-box;
  width: min(100%, 420px);
  display: grid;
  gap: 14px;
  padding: 30px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  background: rgba(18, 18, 18, 0.9);
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(10px);
}

.demo-access-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #ffffff;
  font-size: 15px;
  font-weight: 900;
  letter-spacing: 0;
}

.demo-access-mark {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: inline-grid;
  place-items: center;
  background: #f7f3ed;
  color: #121212;
  font-size: 13px;
  line-height: 1;
}

.demo-access-kicker {
  margin: 6px 0 0;
  color: #a7d8d0;
  font-size: 13px;
  font-weight: 900;
}

.demo-access-panel h1 {
  margin: 0;
  color: #ffffff;
  font-size: 27px;
  line-height: 1.25;
  letter-spacing: 0;
  word-break: keep-all;
}

.demo-access-copy {
  margin: 0;
  color: rgba(247, 243, 237, 0.82);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.6;
  word-break: keep-all;
}

.demo-access-error {
  margin: 0;
  border: 1px solid rgba(255, 179, 164, 0.34);
  border-radius: 6px;
  padding: 10px 12px;
  background: rgba(88, 31, 24, 0.46);
  color: #ffd1c8;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.45;
}

.demo-access-field {
  display: grid;
  gap: 8px;
  margin-top: 2px;
  font-size: 13px;
  font-weight: 900;
  color: rgba(247, 243, 237, 0.86);
}

.demo-access-panel input {
  box-sizing: border-box;
  width: 100%;
  height: 44px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 6px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
  font-size: 16px;
  font-weight: 800;
  outline: none;
}

.demo-access-panel input:focus-visible {
  border-color: #a7d8d0;
  box-shadow: 0 0 0 3px rgba(167, 216, 208, 0.18);
}

.demo-access-panel input[aria-invalid="true"] {
  border-color: #ffb3a4;
}

.demo-access-panel button {
  min-height: 46px;
  border: 0;
  border-radius: 6px;
  background: #f7f3ed;
  color: #121212;
  font-weight: 700;
  cursor: pointer;
  transition: 120ms ease;
}

.demo-access-panel button:hover:not(:disabled),
.demo-access-panel button:focus-visible {
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.18);
  outline: none;
}

.demo-access-panel button:disabled {
  cursor: wait;
  opacity: 0.7;
}

@media (max-width: 480px) {
  .demo-access-screen {
    padding: 20px;
  }

  .demo-access-panel {
    padding: 24px 20px;
    gap: 13px;
  }

  .demo-access-panel h1 {
    font-size: 23px;
  }

  .demo-access-copy {
    font-size: 14px;
  }

  .demo-access-panel input,
  .demo-access-panel button {
    min-height: 46px;
  }
}
</style>
