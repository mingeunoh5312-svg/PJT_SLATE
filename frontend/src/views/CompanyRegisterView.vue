<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import RegisterAccountFields from '../components/auth/RegisterAccountFields.vue'
import RegisterAgreements from '../components/auth/RegisterAgreements.vue'
import RegisterHero from '../components/auth/RegisterHero.vue'
import { slateApi } from '../services/api'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const companySearchKeyword = ref('')
const companySearchLoading = ref(false)
const companySearchResults = ref([])
const selectedPublicCompany = ref(null)
const directCompanyInput = ref(false)
const form = reactive({
  loginId: '', email: '', password: '', passwordConfirm: '', nickname: '',
  birthDate: '', phone: '', address: '', companyName: '',
  businessRegistrationNo: '', companyIntro: '', publicDataCompanyName: ''
})
const agreements = reactive({ all: false, terms: false, privacy: false })

async function searchCompany() {
  if (directCompanyInput.value || !companySearchKeyword.value.trim()) return
  companySearchLoading.value = true
  error.value = ''
  try {
    companySearchResults.value = await slateApi.publicDataSearch({ keyword: companySearchKeyword.value, itemType: 'COMPANY', limit: 5 })
  } catch (err) {
    error.value = err.message
  } finally {
    companySearchLoading.value = false
  }
}

function selectCompany(item) {
  selectedPublicCompany.value = item
  companySearchKeyword.value = item.title || ''
  form.companyName = item.title || ''
  form.companyIntro = item.description || ''
  form.publicDataCompanyName = item.title || item.externalId || ''
  companySearchResults.value = []
}

function toggleDirectCompanyInput() {
  selectedPublicCompany.value = null
  companySearchResults.value = []
  form.publicDataCompanyName = ''
  if (directCompanyInput.value) {
    form.companyName = ''
    form.companyIntro = ''
  }
}

async function submit() {
  error.value = ''
  if (form.password !== form.passwordConfirm) {
    error.value = '비밀번호 확인이 일치하지 않습니다.'
    return
  }
  if (!form.phone.trim()) {
    error.value = '기업 담당자 연락처를 입력해주세요.'
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
      accountType: 'COMPANY',
      company: {
        companyName: form.companyName,
        businessRegistrationNo: form.businessRegistrationNo,
        managerName: form.nickname,
        managerPhone: form.phone,
        companyIntro: form.companyIntro,
        publicDataCompanyName: form.publicDataCompanyName
      }
    })
    sessionStorage.setItem('slate.register.result', 'COMPANY')
    await router.replace({ name: 'register-company-pending' })
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="register-screen company-register-screen">
    <RegisterHero />
    <form class="register-card" @submit.prevent="submit">
      <div class="register-route-head">
        <RouterLink :to="{ name: 'register' }" aria-label="가입 유형 선택으로 돌아가기">←</RouterLink>
        <div><span class="eyebrow">기업 사용자</span><h2>회원가입</h2></div>
      </div>
      <p v-if="error" class="error-text" role="alert">{{ error }}</p>
      <RegisterAccountFields :model-value="form" account-type="COMPANY" />
      <section class="register-company-panel">
        <div class="register-company-head">
          <strong>회사 정보</strong>
          <label><input v-model="directCompanyInput" type="checkbox" @change="toggleDirectCompanyInput">직접 입력</label>
        </div>
        <div class="register-company-search">
          <input v-model.trim="companySearchKeyword" :disabled="directCompanyInput || companySearchLoading" placeholder="회사명을 검색하세요" @keyup.enter.prevent="searchCompany">
          <button type="button" :disabled="directCompanyInput || companySearchLoading || !companySearchKeyword.trim()" @click="searchCompany">{{ companySearchLoading ? '검색 중' : '찾기' }}</button>
        </div>
        <div v-if="companySearchResults.length && !directCompanyInput" class="register-company-results">
          <button v-for="item in companySearchResults" :key="item.publicDataSyncItemId" type="button" @click="selectCompany(item)">
            <strong>{{ item.title }}</strong><span>{{ item.description || item.externalId }}</span>
          </button>
        </div>
        <div class="register-form-grid company">
          <label class="register-field"><span>회사명</span><input v-model.trim="form.companyName" maxlength="120" :readonly="Boolean(selectedPublicCompany) && !directCompanyInput" required></label>
          <label class="register-field"><span>사업자등록번호</span><input v-model.trim="form.businessRegistrationNo" maxlength="30" required></label>
          <label class="register-field wide"><span>회사 소개</span><textarea v-model.trim="form.companyIntro" maxlength="1000" :readonly="Boolean(selectedPublicCompany) && !directCompanyInput" required /></label>
        </div>
      </section>
      <RegisterAgreements :model-value="agreements" />
      <button class="register-submit" type="submit" :disabled="loading">{{ loading ? '접수 중' : '기업 회원가입 신청' }}</button>
      <p class="register-login-link">이미 계정이 있으신가요? <RouterLink to="/login">로그인</RouterLink></p>
    </form>
  </section>
</template>
