import { apiBlob, isProtectedApiResourceUrl } from './api'

const objectUrlCache = new Map()

function releaseEntry(source, entry) {
  entry.references = Math.max(0, entry.references - 1)
  if (entry.references !== 0 || !entry.objectUrl) return
  URL.revokeObjectURL(entry.objectUrl)
  if (objectUrlCache.get(source) === entry) objectUrlCache.delete(source)
}

export async function acquireProtectedResource(source) {
  if (!isProtectedApiResourceUrl(source)) {
    return { url: source, release() {} }
  }

  let entry = objectUrlCache.get(source)
  if (!entry) {
    entry = {
      references: 0,
      objectUrl: '',
      promise: null
    }
    entry.promise = apiBlob(source)
      .then((blob) => {
        entry.objectUrl = URL.createObjectURL(blob)
        if (entry.references === 0) {
          URL.revokeObjectURL(entry.objectUrl)
          entry.objectUrl = ''
          if (objectUrlCache.get(source) === entry) objectUrlCache.delete(source)
        }
        return entry.objectUrl
      })
      .catch((error) => {
        if (objectUrlCache.get(source) === entry) objectUrlCache.delete(source)
        throw error
      })
    objectUrlCache.set(source, entry)
  }

  entry.references += 1
  try {
    const url = entry.objectUrl || await entry.promise
    return {
      url,
      release() {
        releaseEntry(source, entry)
      }
    }
  } catch (error) {
    entry.references = Math.max(0, entry.references - 1)
    throw error
  }
}
