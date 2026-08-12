import brandDefaultImage from '../assets/defaults/icon_images/01_Slate_Logo_img.png'
import homeDefaultImage from '../assets/defaults/icon_images/02_Home_menu_img.png'
import matchingDefaultImage from '../assets/defaults/icon_images/03_Match_menu_img.png'
import teamsDefaultImage from '../assets/defaults/icon_images/04_Team_menu_img.png'
import locationsDefaultImage from '../assets/defaults/icon_images/05_Location_menu_img.png'
import boardsDefaultImage from '../assets/defaults/icon_images/06_Commu_menu_img.png'
import contestsDefaultImage from '../assets/defaults/icon_images/07_Contest_menu_img.png'
import profileDefaultImage from '../assets/defaults/icon_images/08_Profile_menu_img.png'
import adminDefaultImage from '../assets/defaults/icon_images/09_Admin_menu_img.png'

export const SIDEBAR_ASSETS_CHANGED_EVENT = 'slate-sidebar-assets-changed'

const STORAGE_KEY = 'slate.sidebar.assets.v1'
const MAX_IMAGE_BYTES = 5 * 1024 * 1024
const MAX_STORED_DATA_URL_BYTES = 180 * 1024
const SIDEBAR_IMAGE_LIMITS = {
  brand: 256,
  nav: 160
}

export const sidebarAssetTargets = [
  {
    key: 'brand',
    label: 'Slate 로고 우측',
    description: '좌측 상단 SLATE 텍스트 오른쪽에 표시됩니다.',
    group: 'brand'
  },
  { key: 'home', label: '홈', description: '홈 메뉴 왼쪽 이미지입니다.', group: 'nav' },
  { key: 'matching', label: '매칭', description: '매칭 메뉴 왼쪽 이미지입니다.', group: 'nav' },
  { key: 'teams', label: '팀', description: '팀 메뉴 왼쪽 이미지입니다.', group: 'nav' },
  { key: 'locations', label: 'AI 로케이션 탐색', description: 'AI 로케이션 탐색 메뉴 왼쪽 이미지입니다.', group: 'nav' },
  { key: 'boards', label: '게시판', description: '게시판 메뉴 왼쪽 이미지입니다.', group: 'nav' },
  { key: 'contests', label: '공모전', description: '공모전 메뉴 왼쪽 이미지입니다.', group: 'nav' },
  { key: 'profile', label: '내 정보', description: '내 정보 메뉴 왼쪽 이미지입니다.', group: 'nav' },
  { key: 'admin', label: '관리자', description: '관리자 메뉴 왼쪽 이미지입니다.', group: 'nav' }
]

const navKeys = sidebarAssetTargets
  .filter((target) => target.group === 'nav')
  .map((target) => target.key)

const DEFAULT_NAV_ASSETS = {
  home: homeDefaultImage,
  matching: matchingDefaultImage,
  teams: teamsDefaultImage,
  locations: locationsDefaultImage,
  boards: boardsDefaultImage,
  contests: contestsDefaultImage,
  profile: profileDefaultImage,
  admin: adminDefaultImage
}

function emptyStoredAssets() {
  return {
    brand: '',
    nav: Object.fromEntries(navKeys.map((key) => [key, '']))
  }
}

function defaultAssets() {
  return {
    brand: brandDefaultImage,
    nav: { ...DEFAULT_NAV_ASSETS }
  }
}

function mergeWithDefaultAssets(storedAssets) {
  const defaults = defaultAssets()
  return {
    brand: storedAssets.brand || defaults.brand,
    nav: Object.fromEntries(navKeys.map((key) => [
      key,
      storedAssets.nav?.[key] || defaults.nav[key] || ''
    ]))
  }
}

function normalizeStoredAssetValue(value) {
  if (typeof value !== 'string' || !value) return ''
  if (!value.startsWith('data:image/')) return ''
  if (dataUrlByteSize(value) > MAX_STORED_DATA_URL_BYTES * 1.5) return ''
  return value
}

function normalizeStoredAssets(source) {
  const fallback = emptyStoredAssets()
  if (!source || typeof source !== 'object') return fallback
  return {
    brand: normalizeStoredAssetValue(source.brand),
    nav: Object.fromEntries(navKeys.map((key) => [
      key,
      normalizeStoredAssetValue(source.nav?.[key])
    ]))
  }
}

function dispatchChange() {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent(SIDEBAR_ASSETS_CHANGED_EVENT))
}

export function loadSidebarAssets() {
  if (typeof window === 'undefined') return defaultAssets()
  try {
    const storedAssets = JSON.parse(window.localStorage.getItem(STORAGE_KEY) || '{}')
    return mergeWithDefaultAssets(normalizeStoredAssets(storedAssets))
  } catch {
    return defaultAssets()
  }
}

export function saveSidebarAssets(assets) {
  const normalized = normalizeStoredAssets(assets)
  if (typeof window !== 'undefined') {
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized))
    } catch (err) {
      if (isStorageQuotaExceeded(err)) {
        throw new Error('브라우저 저장 공간이 부족합니다. 기존 사이드바 이미지를 삭제하거나 더 작은 이미지를 업로드해주세요.')
      }
      throw err
    }
    dispatchChange()
  }
  return mergeWithDefaultAssets(normalized)
}

export function setSidebarAsset(key, dataUrl) {
  const assets = loadStoredSidebarAssets()
  if (key === 'brand') {
    assets.brand = dataUrl
  } else if (navKeys.includes(key)) {
    assets.nav[key] = dataUrl
  }
  return saveSidebarAssets(assets)
}

export function removeSidebarAsset(key) {
  return setSidebarAsset(key, '')
}

export function resetSidebarAssets() {
  return saveSidebarAssets(emptyStoredAssets())
}

export function sidebarAssetValue(assets, key) {
  if (key === 'brand') return assets?.brand || brandDefaultImage
  return assets?.nav?.[key] || DEFAULT_NAV_ASSETS[key] || ''
}

function loadStoredSidebarAssets() {
  if (typeof window === 'undefined') return emptyStoredAssets()
  try {
    return normalizeStoredAssets(JSON.parse(window.localStorage.getItem(STORAGE_KEY) || '{}'))
  } catch {
    return emptyStoredAssets()
  }
}

export async function readSidebarAssetFile(file, key = '') {
  if (!file) {
    throw new Error('업로드할 이미지를 선택해주세요.')
  }
  if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
    throw new Error('PNG, JPEG, WebP 이미지만 업로드할 수 있습니다.')
  }
  if (file.size > MAX_IMAGE_BYTES) {
    throw new Error('사이드바 이미지는 5MB 이하 파일만 업로드할 수 있습니다.')
  }

  const image = await loadImage(file)
  const maxDimension = key === 'brand' ? SIDEBAR_IMAGE_LIMITS.brand : SIDEBAR_IMAGE_LIMITS.nav
  return encodeSidebarImage(image, maxDimension)
}

function isStorageQuotaExceeded(err) {
  return err?.name === 'QuotaExceededError'
    || err?.name === 'NS_ERROR_DOM_QUOTA_REACHED'
    || err?.code === 22
    || err?.code === 1014
    || String(err?.message || '').toLowerCase().includes('quota')
}

function loadImage(file) {
  return new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file)
    const image = new Image()
    image.onload = () => {
      URL.revokeObjectURL(objectUrl)
      resolve(image)
    }
    image.onerror = () => {
      URL.revokeObjectURL(objectUrl)
      reject(new Error('이미지를 읽지 못했습니다.'))
    }
    image.src = objectUrl
  })
}

async function encodeSidebarImage(image, initialMaxDimension) {
  const dimensions = Array.from(new Set([
    initialMaxDimension,
    Math.min(initialMaxDimension, 128),
    96,
    72,
    56
  ].filter((value) => value > 0)))
  const qualities = [0.86, 0.76, 0.66, 0.56]
  let fallback = ''

  for (const dimension of dimensions) {
    const canvas = drawImageToCanvas(image, dimension)
    for (const quality of qualities) {
      const dataUrl = await canvasToDataUrl(canvas, 'image/webp', quality)
      fallback = dataUrl
      if (dataUrlByteSize(dataUrl) <= MAX_STORED_DATA_URL_BYTES) {
        return dataUrl
      }
    }
  }

  if (fallback && dataUrlByteSize(fallback) <= MAX_STORED_DATA_URL_BYTES * 1.5) {
    return fallback
  }
  throw new Error('이미지를 저장 가능한 크기로 줄이지 못했습니다. 더 단순하거나 작은 이미지를 업로드해주세요.')
}

function drawImageToCanvas(image, maxDimension) {
  const sourceWidth = image.naturalWidth || image.width
  const sourceHeight = image.naturalHeight || image.height
  const scale = Math.min(1, maxDimension / Math.max(sourceWidth, sourceHeight))
  const width = Math.max(1, Math.round(sourceWidth * scale))
  const height = Math.max(1, Math.round(sourceHeight * scale))
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  if (!context) {
    throw new Error('이미지를 변환하지 못했습니다.')
  }
  context.clearRect(0, 0, width, height)
  context.imageSmoothingEnabled = true
  context.imageSmoothingQuality = 'high'
  context.drawImage(image, 0, 0, width, height)
  return canvas
}

function canvasToDataUrl(canvas, type, quality) {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error('이미지를 변환하지 못했습니다.'))
        return
      }
      const reader = new FileReader()
      reader.onload = () => resolve(String(reader.result || ''))
      reader.onerror = () => reject(new Error('이미지를 변환하지 못했습니다.'))
      reader.readAsDataURL(blob)
    }, type, quality)
  })
}

function dataUrlByteSize(dataUrl) {
  if (typeof Blob === 'undefined') return dataUrl.length
  return new Blob([dataUrl]).size
}
