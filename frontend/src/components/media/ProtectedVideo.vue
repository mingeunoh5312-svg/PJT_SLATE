<script setup>
import { onBeforeUnmount, ref, useAttrs, watch } from 'vue'
import { acquireProtectedResource } from '../../services/protectedResources'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  src: {
    type: String,
    default: ''
  }
})
const emit = defineEmits(['error', 'loadedmetadata'])
const attrs = useAttrs()
const resolvedSrc = ref('')
let releaseCurrent = null
let requestId = 0

function releaseResolvedSource() {
  if (releaseCurrent) releaseCurrent()
  releaseCurrent = null
}

watch(
  () => props.src,
  async (source) => {
    const currentRequestId = ++requestId
    releaseResolvedSource()
    resolvedSrc.value = ''
    if (!source) return
    try {
      const resource = await acquireProtectedResource(source)
      if (currentRequestId !== requestId) {
        resource.release()
        return
      }
      releaseCurrent = resource.release
      resolvedSrc.value = resource.url
    } catch (error) {
      if (currentRequestId === requestId) emit('error', error)
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  requestId += 1
  releaseResolvedSource()
})
</script>

<template>
  <video
    v-bind="attrs"
    :src="resolvedSrc || undefined"
    @error="emit('error', $event)"
    @loadedmetadata="emit('loadedmetadata', $event)"
  ></video>
</template>
