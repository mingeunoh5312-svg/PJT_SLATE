<script setup>
import { ref } from 'vue'

defineProps({
  accountType: { type: String, required: true },
  modelValue: { type: Object, required: true }
})
const passwordVisible = ref(false)
const passwordConfirmVisible = ref(false)
</script>

<template>
  <div class="register-form-grid">
    <label class="register-field">
      <span>아이디</span>
      <input v-model.trim="modelValue.loginId" autocomplete="username" minlength="4" maxlength="50" required placeholder="영문, 숫자 등 4~50자">
    </label>
    <label class="register-field">
      <span>{{ accountType === 'COMPANY' ? '담당자명' : '닉네임' }}</span>
      <input v-model.trim="modelValue.nickname" maxlength="50" required :placeholder="accountType === 'COMPANY' ? '담당자명을 입력해주세요' : '닉네임을 입력해주세요'">
    </label>
    <label class="register-field">
      <span>이메일</span>
      <input v-model.trim="modelValue.email" type="email" autocomplete="email" required placeholder="이메일을 입력해주세요">
    </label>
    <label class="register-field">
      <span>생년월일</span>
      <input v-model="modelValue.birthDate" type="date">
    </label>
    <label class="register-field">
      <span>비밀번호</span>
      <span class="register-input-button">
        <input v-model="modelValue.password" :type="passwordVisible ? 'text' : 'password'" autocomplete="new-password" minlength="8" maxlength="80" required placeholder="8자 이상 입력해주세요">
        <button type="button" :aria-label="passwordVisible ? '비밀번호 숨기기' : '비밀번호 보기'" @click="passwordVisible = !passwordVisible">◉</button>
      </span>
    </label>
    <label class="register-field">
      <span>연락처</span>
      <input v-model.trim="modelValue.phone" type="tel" autocomplete="tel" maxlength="30" placeholder="전화번호를 입력해주세요">
    </label>
    <label class="register-field">
      <span>비밀번호 확인</span>
      <span class="register-input-button">
        <input v-model="modelValue.passwordConfirm" :type="passwordConfirmVisible ? 'text' : 'password'" autocomplete="new-password" minlength="8" maxlength="80" required placeholder="비밀번호를 다시 입력해주세요">
        <button type="button" :aria-label="passwordConfirmVisible ? '비밀번호 숨기기' : '비밀번호 보기'" @click="passwordConfirmVisible = !passwordConfirmVisible">◉</button>
      </span>
    </label>
    <label class="register-field">
      <span>주소</span>
      <input v-model.trim="modelValue.address" autocomplete="street-address" placeholder="주소를 입력해주세요">
    </label>
  </div>
</template>
