<script setup>
import { computed, onBeforeUnmount, ref, useAttrs, watch } from 'vue'
import { acquireProtectedResource } from '../../services/protectedResources'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  src: {
    type: String,
    default: ''
  },
  sources: {
    type: Array,
    default: () => []
  },
  fallback: {
    type: String,
    default: ''
  }
})
const emit = defineEmits(['error', 'load'])
const attrs = useAttrs()
const resolvedSrc = ref('')
const activeIndex = ref(0)
let releaseCurrent = null
let requestId = 0

const candidates = computed(() => [...new Set([
  ...(Array.isArray(props.sources) ? props.sources : []),
  props.src,
  props.fallback
].map((value) => String(value || '').trim()).filter(Boolean))])

function releaseResolvedSource() {
  if (releaseCurrent) releaseCurrent()
  releaseCurrent = null
}

async function resolveCandidate(index, failure = null) {
  const currentRequestId = ++requestId
  releaseResolvedSource()
  resolvedSrc.value = ''
  activeIndex.value = index

  const source = candidates.value[index]
  if (!source) {
    emit('error', failure)
    return
  }

  try {
    const resource = await acquireProtectedResource(source)
    if (currentRequestId !== requestId) {
      resource.release()
      return
    }
    releaseCurrent = resource.release
    resolvedSrc.value = resource.url
  } catch (error) {
    if (currentRequestId === requestId) await resolveCandidate(index + 1, error)
  }
}

function handleNativeError(event) {
  resolveCandidate(activeIndex.value + 1, event)
}

watch(
  () => candidates.value.join('\u0000'),
  () => resolveCandidate(0),
  { immediate: true }
)

onBeforeUnmount(() => {
  requestId += 1
  releaseResolvedSource()
})
</script>

<template>
  <img
    v-bind="attrs"
    :src="resolvedSrc || undefined"
    @error="handleNativeError"
    @load="emit('load', $event)"
  >
</template>
