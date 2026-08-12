<script setup>
const props = defineProps({ modelValue: { type: Object, required: true } })

function toggleAll() {
  props.modelValue.terms = props.modelValue.all
  props.modelValue.privacy = props.modelValue.all
}
</script>

<template>
  <section class="register-policy">
    <strong>가입 정책 동의</strong>
    <label>
      <input v-model="modelValue.all" type="checkbox" @change="toggleAll">
      전체 동의
    </label>
    <label>
      <input v-model="modelValue.terms" type="checkbox">
      서비스 이용약관 동의 (필수)
      <span aria-hidden="true">›</span>
    </label>
    <label>
      <input v-model="modelValue.privacy" type="checkbox">
      개인정보 수집 및 이용 동의 (필수)
      <span aria-hidden="true">›</span>
    </label>
  </section>
</template>
