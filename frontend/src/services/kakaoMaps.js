const KAKAO_MAP_SCRIPT_ID = 'slate-kakao-map-sdk'

let kakaoMapsPromise = null

function readyMaps() {
  return globalThis.window?.kakao?.maps?.Map ? globalThis.window.kakao.maps : null
}

function sdkError(message) {
  return new Error(message)
}

export function loadKakaoMaps() {
  const ready = readyMaps()
  if (ready) return Promise.resolve(ready)
  if (kakaoMapsPromise) return kakaoMapsPromise

  const appKey = String(import.meta.env.VITE_KAKAO_MAP_APP_KEY || '').trim()
  if (!appKey || appKey === 'CHANGE_ME') {
    return Promise.reject(sdkError('Kakao 지도 JavaScript 키가 설정되지 않았습니다.'))
  }

  kakaoMapsPromise = new Promise((resolve, reject) => {
    let settled = false

    const fail = () => {
      if (settled) return
      settled = true
      document.getElementById(KAKAO_MAP_SCRIPT_ID)?.remove()
      reject(sdkError('Kakao 지도를 불러오지 못했습니다. JavaScript SDK 도메인 등록 상태를 확인해주세요.'))
    }

    const finish = () => {
      if (settled) return
      const kakao = globalThis.window?.kakao
      if (!kakao?.maps?.load) {
        fail()
        return
      }
      kakao.maps.load(() => {
        if (settled) return
        const maps = readyMaps()
        if (!maps) {
          fail()
          return
        }
        settled = true
        resolve(maps)
      })
    }

    const existingScript = document.getElementById(KAKAO_MAP_SCRIPT_ID)
    if (existingScript) {
      existingScript.addEventListener('load', finish, { once: true })
      existingScript.addEventListener('error', fail, { once: true })
      if (globalThis.window?.kakao?.maps?.load) finish()
      return
    }

    const script = document.createElement('script')
    script.id = KAKAO_MAP_SCRIPT_ID
    script.async = true
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?autoload=false&appkey=${encodeURIComponent(appKey)}`
    script.addEventListener('load', finish, { once: true })
    script.addEventListener('error', fail, { once: true })
    document.head.appendChild(script)
  }).catch((error) => {
    kakaoMapsPromise = null
    throw error
  })

  return kakaoMapsPromise
}
