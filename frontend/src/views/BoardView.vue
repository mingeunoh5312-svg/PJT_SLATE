<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { defaultProfileImage, defaultWorkImage } from '../constants/defaultImages'
import { slateApi } from '../services/api'

const props = defineProps({ currentUser: Object })
const emit = defineEmits(['login'])
const route = useRoute()
const router = useRouter()

const freeCategories = [
  { key: '', label: '전체' },
  { key: 'NOTICE', label: '공지' },
  { key: 'QUESTION', label: '질문' },
  { key: 'INFO', label: '정보' },
  { key: 'REVIEW', label: '후기' },
  { key: 'FREE', label: '자유' }
]

const workTypes = [
  { key: '', label: '전체 종류' },
  { key: 'SHORT_FILM', label: '단편영화' },
  { key: 'FEATURE_FILM', label: '장편영화' },
  { key: 'MUSIC_VIDEO', label: '뮤직비디오' },
  { key: 'ADVERTISEMENT', label: '광고' },
  { key: 'DOCUMENTARY', label: '다큐멘터리' },
  { key: 'WEB_CONTENT', label: '웹 콘텐츠' },
  { key: 'OTHER', label: '기타' }
]

const reportReasons = [
  { key: 'SPAM', label: '스팸/홍보' },
  { key: 'ABUSE', label: '욕설/비방' },
  { key: 'ILLEGAL', label: '불법/권리 침해' },
  { key: 'PRIVACY', label: '개인정보 노출' },
  { key: 'OTHER', label: '기타' }
]

const activeTab = ref('HOME')
const sort = ref('latest')
const workSearchInput = ref('')
const workSearchKeyword = ref('')
const freeCategory = ref('')
const workType = ref('')
const genreId = ref('')
const popularWorkType = ref('')
const popularGenreId = ref('')
const popularPeriod = ref('ALL')
const genres = ref([])
const posts = ref([])
const boardCurrentPage = ref(1)
const rankings = ref([])
const popularProfiles = ref([])
const popularProfilesLoadedLimit = ref(0)
const homeWorks = ref([])
const homeFreePosts = ref([])
const homeSearchInput = ref('')
const homeSearchKeyword = ref('')
const homeSearchScope = ref('ALL')
const homeSearchScopeOpen = ref(false)
const routeSearchScopeOpen = ref(false)
const homeSearchResults = ref([])
const homeSearchExecuted = ref(false)
const homeSearchLoading = ref(false)
const followSavingProfileId = ref(null)
const failedProfileImages = ref(new Set())
const selected = ref(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const saved = ref('')
const reviewDraft = ref('')
const editingReviewId = ref(null)
const editingReviewContent = ref('')
const editingPostId = ref(null)
const pendingPostId = ref(null)
const reportTarget = ref(null)
const selectedWorkFile = ref(null)
const uploadedWorkFile = ref(null)
const workFileDuration = ref(null)
const uploadingFile = ref(false)
const teams = ref([])
const myTeamWorkRequests = ref([])
const teamWorkRequests = ref([])
const approvalTeamId = ref('')
const decisionReasons = reactive({})
const requestSaving = ref(false)
const pendingPostDelete = ref(false)
const pendingReviewDeleteId = ref(null)
const activeBoardPanel = ref('')
const youtubePreview = ref(null)
const youtubePreviewLoading = ref(false)
const youtubePreviewError = ref('')
const youtubePreviewUrl = ref('')
const originalYoutubeUrl = ref('')
let postsRequestId = 0
let homeRequestId = 0
const BOARD_PAGE_SIZE = 6

const isBoardListRoute = computed(() => route.name === 'boards')
const isBoardSearchRoute = computed(() => route.name === 'boards-search')
const isBoardCreateRoute = computed(() => route.name === 'boards-new')
const isBoardDetailRoute = computed(() => route.name === 'boards-detail')
const isBoardEditRoute = computed(() => route.name === 'boards-edit')
const isStandaloneBoardRoute = computed(() => isBoardCreateRoute.value || isBoardDetailRoute.value || isBoardEditRoute.value)
const showPostForm = computed(() => isBoardCreateRoute.value || isBoardEditRoute.value)
const showBoardListPanel = computed(() => (
  (isBoardListRoute.value || isBoardSearchRoute.value)
  && !activeBoardPanel.value
  && ['ALL', 'WORK', 'FREE'].includes(activeTab.value)
))
const showTeamApprovalTools = computed(() => (
  props.currentUser
  && activeBoardPanel.value === 'approvals'
))
const boardTotalPages = computed(() => Math.max(1, Math.ceil(posts.value.length / BOARD_PAGE_SIZE)))
const paginatedPosts = computed(() => {
  const start = (boardCurrentPage.value - 1) * BOARD_PAGE_SIZE
  return posts.value.slice(start, start + BOARD_PAGE_SIZE)
})
const boardPageNumbers = computed(() => Array.from({ length: boardTotalPages.value }, (_, index) => index + 1))

const dashboardTabs = [
  { key: 'HOME', label: '홈' },
  { key: 'WORK', label: '작업물' },
  { key: 'FREE', label: '자유' },
  { key: 'POPULAR', label: '인기' }
]

const boardSearchScopeOptions = [
  { key: 'ALL', label: '전체' },
  { key: 'WORK', label: '작업물' },
  { key: 'FREE', label: '자유게시판' }
]

const postForm = reactive({
  category: 'WORK',
  freeCategory: 'FREE',
  workType: 'OTHER',
  genreIds: [],
  title: '',
  content: '',
  visibility: 'PUBLIC',
  youtubeUrl: '',
  teamId: '',
  fileId: null
})
const workImageFile = ref(null)
const workImagePreview = ref('')
const workImageDelete = ref(false)
const reportForm = reactive({
  reasonCode: 'SPAM',
  detail: ''
})

const canManagePost = computed(() => {
  if (!props.currentUser || !selected.value) return false
  return props.currentUser.accountType === 'ADMIN' || Number(selected.value.authorUserId) === Number(props.currentUser.userId)
})
const canReportPost = computed(() => {
  if (!props.currentUser || !selected.value) return false
  return Number(selected.value.authorUserId) !== Number(props.currentUser.userId)
})
const workTeams = computed(() => teams.value.filter((team) => team.status !== 'ENDED'))
const managedTeams = computed(() => workTeams.value.filter((team) => ['LEADER', 'SUB_LEADER'].includes(team.myTeamRole)))
const selectedWorkTeam = computed(() => workTeams.value.find((team) => Number(team.teamId) === Number(postForm.teamId)))
const selectedWorkTeamManager = computed(() => ['LEADER', 'SUB_LEADER'].includes(selectedWorkTeam.value?.myTeamRole))
const teamWorkNeedsApproval = computed(() => postForm.category === 'WORK' && Boolean(postForm.teamId) && !selectedWorkTeamManager.value)
const canPreviewYoutube = computed(() => (
  postForm.category === 'WORK'
  && Boolean(postForm.youtubeUrl.trim())
  && !selectedWorkFile.value
  && !postForm.fileId
  && !youtubePreviewLoading.value
))
const youtubePreviewMatchesCurrentUrl = computed(() => (
  Boolean(youtubePreview.value)
  && Boolean(postForm.youtubeUrl.trim())
  && postForm.youtubeUrl.trim() === youtubePreviewUrl.value
))
const youtubeSaveBlockedMessage = computed(() => {
  if (postForm.category !== 'WORK') return ''
  if (!postForm.youtubeUrl.trim() || postForm.fileId || selectedWorkFile.value) return ''
  if (youtubePreviewMatchesCurrentUrl.value) return ''
  if (editingPostId.value && originalYoutubeUrl.value && postForm.youtubeUrl.trim() !== originalYoutubeUrl.value) {
    return 'YouTube URL이 변경되었습니다. 다시 미리보기를 확인해주세요.'
  }
  if (editingPostId.value && originalYoutubeUrl.value && postForm.youtubeUrl.trim() === originalYoutubeUrl.value) {
    return '기존 YouTube 메타데이터가 없어 미리보기를 확인해주세요.'
  }
  return '유튜브 URL 미리보기를 확인해야 저장할 수 있습니다.'
})
const youtubeRegistrationBlocked = computed(() => Boolean(youtubeSaveBlockedMessage.value))
const youtubePreviewCardLabel = computed(() => {
  if (!editingPostId.value) return '미리보기'
  if (originalYoutubeUrl.value && postForm.youtubeUrl.trim() === originalYoutubeUrl.value) return '현재 등록된 영상'
  return '새 미리보기'
})
const youtubePreviewSuccessMessage = computed(() => {
  if (!editingPostId.value) return '미리보기 확인 완료. 이 영상으로 작업물을 등록할 수 있습니다.'
  if (originalYoutubeUrl.value && postForm.youtubeUrl.trim() === originalYoutubeUrl.value) {
    return '현재 등록된 영상 정보입니다. URL을 바꾸면 다시 미리보기를 확인해야 합니다.'
  }
  return '미리보기 확인 완료. 이 영상으로 작업물을 수정할 수 있습니다.'
})
const selectedDetailTeam = computed(() => {
  const teamId = selected.value?.work?.teamId || selected.value?.workTeamId
  return workTeams.value.find((team) => Number(team.teamId) === Number(teamId))
})
const canEditPost = computed(() => {
  if (!canManagePost.value) return false
  const teamId = selected.value?.work?.teamId || selected.value?.workTeamId
  if (!teamId) return true
  return ['LEADER', 'SUB_LEADER'].includes(selectedDetailTeam.value?.myTeamRole)
})
const selectedIsYoutubeWork = computed(() => selected.value?.category === 'WORK' && hasYoutubeWork(selected.value))
const deleteConfirmMessage = computed(() => (
  selectedIsYoutubeWork.value
    ? '이 YouTube 작업물 게시글을 삭제할까요? 삭제 후 게시판 목록에서 보이지 않으며, YouTube 원본 영상은 삭제되지 않습니다.'
    : '이 게시글을 삭제할까요? 삭제 후 게시판 목록에서 보이지 않습니다.'
))
const hasActiveWorkSearch = computed(() => Boolean(workSearchKeyword.value.trim()))
const dashboardWorks = computed(() => {
  return homeWorks.value.map((post) => {
    const youtube = youtubeMeta(post)
    return {
      id: post.postId,
      post,
      raw: post,
      title: post.title,
      youtubeTitle: youtube.title,
      channelTitle: youtube.channelTitle,
      tag: workTypeLabel(post.workType),
      author: post.authorNickname || '작성자 정보 없음',
      likes: post.likeCount ?? 0,
      comments: post.reviewCount ?? 0,
      liked: isPostLiked(post),
      duration: workDurationLabel(post),
      image: workImage(post),
      mediaType: post.work?.mediaType || post.mediaType
    }
  })
})
const dashboardFreePosts = computed(() => {
  return homeFreePosts.value.map((post) => ({
    type: freeCategoryLabel(post.freeCategory),
    tone: String(post.freeCategory || 'FREE').toLowerCase(),
    title: post.title,
    content: post.content,
    author: post.authorNickname || '작성자 정보 없음',
    time: post.createdAt ? String(post.createdAt).slice(0, 10) : '',
    likes: post.likeCount ?? 0,
    comments: post.reviewCount ?? 0,
    liked: isPostLiked(post),
    post
  }))
})

const periodPopularWorks = computed(() => {
  return rankings.value.map((item) => {
    const youtube = youtubeMeta(item)
    return {
      id: item.postId,
      postId: item.postId,
      raw: item,
      title: item.title || item.workTitle || '제목 없음',
      youtubeTitle: youtube.title,
      channelTitle: youtube.channelTitle,
      tag: workTypeLabel(item.workType),
      likes: item.likeCount ?? 0,
      liked: isPostLiked(item),
      duration: workDurationLabel(item),
      image: workImage(item),
      author: item.authorNickname || item.displayName || '작성자 정보 없음',
      views: item.viewCount ?? 0
    }
  })
})

function openDashboardTab(tab) {
  activeBoardPanel.value = ''
  router.replace({ name: 'boards', query: tab === 'HOME' ? {} : { tab } })
}

function openBoardCompose(category = 'WORK') {
  router.push({
    name: 'boards-new',
    query: category === 'FREE' ? { category: 'FREE' } : {}
  })
}

function boardListLocation(tab = activeTab.value) {
  const target = ['WORK', 'FREE', 'POPULAR'].includes(tab) ? tab : 'HOME'
  return { name: 'boards', query: target === 'HOME' ? {} : { tab: target } }
}

function submitWorkSearch() {
  const nextKeyword = workSearchInput.value.trim()
  workSearchInput.value = nextKeyword
  workSearchKeyword.value = nextKeyword
  if (isBoardSearchRoute.value) syncSearchRoute()
  else loadPosts()
}

function resetWorkSearch() {
  if (!workSearchInput.value && !workSearchKeyword.value) return
  workSearchInput.value = ''
  workSearchKeyword.value = ''
  if (activeTab.value === 'WORK' || activeTab.value === 'FREE') loadPosts()
}

async function submitHomeSearch() {
  const keyword = homeSearchInput.value.trim()
  homeSearchInput.value = keyword
  if (!keyword) return
  await router.push({
    name: 'boards-search',
    query: { scope: homeSearchScope.value, keyword, sort: 'latest' }
  })
}

function resetHomeSearch() {
  homeSearchInput.value = ''
  homeSearchKeyword.value = ''
  homeSearchResults.value = []
  homeSearchExecuted.value = false
}

function normalizedScope(value) {
  const scope = String(value || '').toUpperCase()
  if (scope === 'FREE' || scope === 'WORK') return scope
  return 'ALL'
}

function setSearchScope(scope) {
  const nextScope = normalizedScope(scope)
  homeSearchScopeOpen.value = false
  routeSearchScopeOpen.value = false
  if (isBoardSearchRoute.value) {
    activeTab.value = nextScope
    freeCategory.value = ''
    workType.value = ''
    genreId.value = ''
    syncSearchRoute()
  } else {
    homeSearchScope.value = nextScope
  }
}

function syncSearchRoute() {
  if (!isBoardSearchRoute.value) return
  const query = {
    scope: activeTab.value,
    keyword: workSearchInput.value.trim(),
    sort: sort.value
  }
  if (activeTab.value === 'FREE' && freeCategory.value) query.freeCategory = freeCategory.value
  if (activeTab.value === 'WORK' && workType.value) query.workType = workType.value
  if (activeTab.value === 'WORK' && genreId.value) query.genreId = String(genreId.value)
  router.replace({ name: 'boards-search', query })
}

function postCategory(item) {
  return String(item?.category || item?.post?.category || item?.raw?.category || '').toUpperCase()
}

function isPostLiked(item) {
  const value = item?.likedByCurrentUser ?? item?.post?.likedByCurrentUser ?? item?.raw?.likedByCurrentUser
  return value === true || value === 'true' || Number(value) === 1
}

function sortBoardRows(rows, sortKey = sort.value) {
  return [...rows].sort((left, right) => {
    if (sortKey === 'latest') {
      return String(right.createdAt || '').localeCompare(String(left.createdAt || '')) || Number(right.postId || 0) - Number(left.postId || 0)
    }
    if (sortKey === 'likes') {
      return Number(right.likeCount || 0) - Number(left.likeCount || 0) || String(right.createdAt || '').localeCompare(String(left.createdAt || ''))
    }
    if (sortKey === 'views') {
      return Number(right.viewCount || 0) - Number(left.viewCount || 0) || String(right.createdAt || '').localeCompare(String(left.createdAt || ''))
    }
    return Number(right.reactionScore || 0) - Number(left.reactionScore || 0) || String(right.createdAt || '').localeCompare(String(left.createdAt || ''))
  })
}

function rankingTypeForPeriod(period = popularPeriod.value) {
  return { WEEKLY: 'WEEKLY_WORK', MONTHLY: 'MONTHLY_WORK', ALL: 'POPULAR_WORK' }[period] || 'POPULAR_WORK'
}

function setPopularPeriod(period) {
  popularPeriod.value = ['WEEKLY', 'MONTHLY', 'ALL'].includes(period) ? period : 'ALL'
  syncPopularRoute()
}

function syncPopularRoute() {
  const query = { tab: 'POPULAR', period: popularPeriod.value }
  if (popularWorkType.value) query.workType = popularWorkType.value
  if (popularGenreId.value) query.genreId = String(popularGenreId.value)
  router.replace({ name: 'boards', query })
}

function openTeamApprovalPanel() {
  activeTab.value = 'WORK'
  activeBoardPanel.value = 'approvals'
  loadBoardTeams()
  loadTeamWorkRequests()
}

function openDashboardPost(item, fallbackTab = 'WORK') {
  if (item?.post) {
    router.push({ name: 'boards-detail', params: { postId: item.post.postId }, query: { from: fallbackTab } })
    return
  }
  if (item?.postId) {
    router.push({ name: 'boards-detail', params: { postId: item.postId }, query: { from: fallbackTab } })
    return
  }
  openDashboardTab(fallbackTab)
}

function canManageReview(review) {
  if (!props.currentUser || !review) return false
  return props.currentUser.accountType === 'ADMIN' || Number(review.authorUserId) === Number(props.currentUser.userId)
}

function applyLikeState(row, postId, likeCount, likedByCurrentUser) {
  if (!row || Number(row.postId) !== Number(postId)) return
  row.likeCount = likeCount
  row.likedByCurrentUser = likedByCurrentUser ? 1 : 0
}

function syncPostLikeState(postId, result) {
  const likeCount = result.likeCount
  const liked = Boolean(result.active)
  if (selected.value && Number(selected.value.postId) === Number(postId)) {
    selected.value.likeCount = likeCount
    selected.value.likedByCurrentUser = liked ? 1 : 0
  }
  posts.value.forEach((post) => applyLikeState(post, postId, likeCount, liked))
  homeWorks.value.forEach((post) => applyLikeState(post, postId, likeCount, liked))
  homeFreePosts.value.forEach((post) => applyLikeState(post, postId, likeCount, liked))
  rankings.value.forEach((post) => applyLikeState(post, postId, likeCount, liked))
}

function canReportReview(review) {
  if (!props.currentUser || !review || review.status !== 'PUBLISHED') return false
  return Number(review.authorUserId) !== Number(props.currentUser.userId)
}

function clearPendingActions() {
  pendingPostDelete.value = false
  pendingReviewDeleteId.value = null
}

function requestPostDelete() {
  if (!selected.value || !canManagePost.value) return
  pendingPostDelete.value = true
  pendingReviewDeleteId.value = null
  reportTarget.value = null
}

function requestReviewDelete(review) {
  if (!canManageReview(review)) return
  pendingReviewDeleteId.value = review.reviewId
  pendingPostDelete.value = false
  reportTarget.value = null
}

function resetPostForm(category = activeTab.value === 'FREE' ? 'FREE' : 'WORK') {
  if (workImagePreview.value) URL.revokeObjectURL(workImagePreview.value)
  workImagePreview.value = ''
  workImageFile.value = null
  workImageDelete.value = false
  clearPendingActions()
  clearYoutubePreview()
  originalYoutubeUrl.value = ''
  editingPostId.value = null
  postForm.category = category
  postForm.freeCategory = 'FREE'
  postForm.workType = 'OTHER'
  postForm.genreIds = []
  postForm.title = ''
  postForm.content = ''
  postForm.visibility = 'PUBLIC'
  postForm.youtubeUrl = ''
  postForm.teamId = ''
  postForm.fileId = null
  selectedWorkFile.value = null
  uploadedWorkFile.value = null
  workFileDuration.value = null
}

function selectWorkImage(event) {
  if (workImagePreview.value) URL.revokeObjectURL(workImagePreview.value)
  workImageFile.value = event.target.files?.[0] || null
  workImagePreview.value = workImageFile.value ? URL.createObjectURL(workImageFile.value) : ''
  workImageDelete.value = false
}

function removeWorkImage() {
  if (workImagePreview.value) URL.revokeObjectURL(workImagePreview.value)
  workImagePreview.value = ''
  workImageFile.value = null
  workImageDelete.value = true
}

function editSelectedPost() {
  if (!selected.value) return
  router.push({ name: 'boards-edit', params: { postId: selected.value.postId } })
}

function populateSelectedPostForm() {
  if (!selected.value) return
  clearPendingActions()
  clearYoutubePreview()
  editingPostId.value = selected.value.postId
  postForm.category = selected.value.category
  postForm.freeCategory = selected.value.freeCategory || 'FREE'
  postForm.workType = selected.value.work?.workType || selected.value.workType || 'OTHER'
  postForm.genreIds = (selected.value.work?.genres || []).map((genre) => Number(genre.genreId))
  postForm.title = selected.value.title
  postForm.content = selected.value.content
  postForm.visibility = selected.value.visibility || 'PUBLIC'
  postForm.youtubeUrl = selected.value.work?.youtubeUrl || selected.value.youtubeUrl || ''
  originalYoutubeUrl.value = postForm.youtubeUrl.trim()
  postForm.teamId = selected.value.work?.teamId || selected.value.workTeamId || ''
  postForm.fileId = selected.value.work?.fileId || null
  selectedWorkFile.value = null
  workFileDuration.value = null
  uploadedWorkFile.value = postForm.fileId
    ? {
        fileId: selected.value.work.fileId,
        originalName: selected.value.work.fileOriginalName,
        durationSeconds: selected.value.work.fileDurationSeconds
      }
    : null
  hydrateYoutubePreviewFromWork(selected.value.work)
}

function postPayload() {
  if (postForm.youtubeUrl.trim() && postForm.fileId) throw new Error('유튜브 URL과 서버 업로드 파일 중 하나만 선택해주세요.')
  if (selectedWorkFile.value && !postForm.fileId) throw new Error('선택한 서버 파일을 먼저 업로드해주세요.')
  if (youtubeRegistrationBlocked.value) throw new Error(youtubeSaveBlockedMessage.value || '유튜브 URL 미리보기를 먼저 확인해주세요.')
  const payload = {
    category: postForm.category,
    freeCategory: postForm.category === 'FREE' ? postForm.freeCategory : null,
    title: postForm.title.trim(),
    content: postForm.content.trim(),
    visibility: postForm.visibility
  }
  if (postForm.category === 'WORK') {
    payload.work = {
      teamId: postForm.teamId ? Number(postForm.teamId) : null,
      title: postForm.title.trim(),
      description: postForm.content.trim(),
      mediaType: postForm.fileId ? 'SERVER_UPLOAD' : postForm.youtubeUrl.trim() ? 'YOUTUBE' : 'MANUAL',
      workType: postForm.workType,
      genreIds: postForm.genreIds.map(Number),
      youtubeUrl: postForm.fileId ? '' : postForm.youtubeUrl.trim(),
      fileId: postForm.fileId,
      visibility: postForm.visibility
    }
  }
  return payload
}

function workFileStreamUrl(fileId) {
  return `/api/boards/work-files/${fileId}/stream`
}

function clearYoutubePreview() {
  youtubePreview.value = null
  youtubePreviewLoading.value = false
  youtubePreviewError.value = ''
  youtubePreviewUrl.value = ''
}

function hasYoutubeMetadata(work) {
  return Boolean(
    work?.youtubeVideoId
    || work?.youtubeTitle
    || work?.youtubeChannelTitle
    || work?.youtubeThumbnailUrl
    || work?.youtubeDurationSeconds
  )
}

function hydrateYoutubePreviewFromWork(work) {
  if (!work?.youtubeUrl) return
  if (!hasYoutubeMetadata(work)) return
  youtubePreview.value = {
    videoId: work.youtubeVideoId || String(work.youtubeUrl).split('/').filter(Boolean).pop() || '',
    embedUrl: work.youtubeUrl,
    youtubeUrl: work.youtubeUrl,
    title: work.youtubeTitle || postForm.title || '등록된 유튜브 영상',
    channelTitle: work.youtubeChannelTitle || '채널 정보 없음',
    thumbnailUrl: work.youtubeThumbnailUrl || '',
    durationSeconds: work.youtubeDurationSeconds || 0
  }
  youtubePreviewUrl.value = postForm.youtubeUrl.trim()
}

function handleYoutubeUrlInput() {
  youtubePreviewError.value = ''
  if (youtubePreview.value && postForm.youtubeUrl.trim() !== youtubePreviewUrl.value) {
    youtubePreview.value = null
  }
}

function clearLinkedWorkFile() {
  postForm.fileId = null
  selectedWorkFile.value = null
  uploadedWorkFile.value = null
  workFileDuration.value = null
  if (editingPostId.value) {
    saved.value = '서버 파일 연결을 해제했습니다. 저장해야 수정 사항이 반영됩니다.'
  }
}

async function previewYoutubeVideo() {
  if (!props.currentUser) {
    emit('login')
    return
  }
  const youtubeUrl = postForm.youtubeUrl.trim()
  if (!youtubeUrl) {
    youtubePreviewError.value = '유튜브 URL을 입력해주세요.'
    return
  }
  if (selectedWorkFile.value || postForm.fileId) {
    youtubePreviewError.value = '서버 업로드 파일과 유튜브 URL 중 하나만 선택해주세요.'
    return
  }
  youtubePreviewLoading.value = true
  youtubePreviewError.value = ''
  saved.value = ''
  try {
    youtubePreview.value = await slateApi.previewYoutubeVideo(youtubeUrl)
    youtubePreviewUrl.value = youtubeUrl
  } catch (err) {
    youtubePreview.value = null
    youtubePreviewError.value = err.message
  } finally {
    youtubePreviewLoading.value = false
  }
}

function formatYoutubeDuration(value) {
  const seconds = Number(value || 0)
  const minutes = String(Math.floor(seconds / 60)).padStart(2, '0')
  const remainder = String(seconds % 60).padStart(2, '0')
  return `${minutes}:${remainder}`
}

async function onWorkFileChange(event) {
  const file = event.target.files?.[0]
  selectedWorkFile.value = file || null
  uploadedWorkFile.value = null
  postForm.fileId = null
  workFileDuration.value = null
  if (file) {
    postForm.youtubeUrl = ''
    clearYoutubePreview()
  }
  if (!file) return
  if (file.size > 300 * 1024 * 1024) {
    error.value = '영상 파일은 최대 300MB까지 업로드할 수 있습니다.'
    selectedWorkFile.value = null
    return
  }
  try {
    workFileDuration.value = await readVideoDuration(file)
    if (workFileDuration.value > 180) {
      error.value = '서버 업로드 영상은 최대 3분까지 등록할 수 있습니다.'
      selectedWorkFile.value = null
    }
  } catch {
    workFileDuration.value = null
  }
}

function readVideoDuration(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.onloadedmetadata = () => {
      const duration = Math.ceil(video.duration || 0)
      URL.revokeObjectURL(url)
      resolve(duration)
    }
    video.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('duration unavailable'))
    }
    video.src = url
  })
}

async function uploadSelectedWorkFile() {
  if (!props.currentUser) {
    emit('login')
    return
  }
  if (!selectedWorkFile.value) return
  uploadingFile.value = true
  error.value = ''
  saved.value = ''
  try {
    const uploaded = await slateApi.uploadWorkFile(selectedWorkFile.value, {
      teamId: postForm.teamId || '',
      clientDurationSeconds: workFileDuration.value
    })
    uploadedWorkFile.value = uploaded
    postForm.fileId = uploaded.fileId
    postForm.youtubeUrl = ''
    clearYoutubePreview()
    saved.value = '서버 파일을 업로드했습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    uploadingFile.value = false
  }
}

async function loadBoardTeams() {
  if (!props.currentUser) {
    teams.value = []
    myTeamWorkRequests.value = []
    teamWorkRequests.value = []
    approvalTeamId.value = ''
    return
  }
  try {
    teams.value = await slateApi.myTeams()
    if (!managedTeams.value.some((team) => Number(team.teamId) === Number(approvalTeamId.value))) {
      approvalTeamId.value = managedTeams.value[0]?.teamId || ''
    }
    await loadMyTeamWorkRequests()
    await loadTeamWorkRequests()
  } catch (err) {
    error.value = err.message
  }
}

async function loadMyTeamWorkRequests() {
  if (!props.currentUser) {
    myTeamWorkRequests.value = []
    return
  }
  try {
    myTeamWorkRequests.value = await slateApi.myTeamWorkRequests()
  } catch (err) {
    myTeamWorkRequests.value = []
    error.value = err.message
  }
}

async function loadTeamWorkRequests() {
  if (!props.currentUser || !approvalTeamId.value) {
    teamWorkRequests.value = []
    return
  }
  try {
    teamWorkRequests.value = await slateApi.teamWorkRequests(approvalTeamId.value)
  } catch (err) {
    teamWorkRequests.value = []
    error.value = err.message
  }
}

async function decideTeamWorkRequest(request, decision) {
  if (!request || request.status !== 'PENDING') return
  requestSaving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.decideTeamWorkRequest(request.requestId, {
      decision,
      reason: decisionReasons[request.requestId] || ''
    })
    decisionReasons[request.requestId] = ''
    await loadMyTeamWorkRequests()
    await loadTeamWorkRequests()
    await loadPosts()
    saved.value = decision === 'APPROVED' ? '팀 작업물을 승인했습니다.' : '팀 작업물을 거절했습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    requestSaving.value = false
  }
}

async function loadPosts(preferredPostId) {
  const requestId = ++postsRequestId
  loading.value = true
  error.value = ''
  saved.value = ''
  try {
    if (activeTab.value === 'HOME') {
      await loadHome()
      return
    }
    if (activeTab.value === 'POPULAR') {
      posts.value = []
      selected.value = null
      const profilesPromise = popularProfilesLoadedLimit.value >= 10
        ? Promise.resolve(popularProfiles.value)
        : slateApi.boardRankings('POPULAR_PROFILE', 10)
      const [workRows, profileRows] = await Promise.all([
        slateApi.boardRankings(rankingTypeForPeriod(), 5, popularWorkType.value, popularGenreId.value),
        profilesPromise
      ])
      if (requestId !== postsRequestId) return
      rankings.value = workRows
      popularProfiles.value = profileRows
      popularProfilesLoadedLimit.value = 10
      return
    }
    rankings.value = []
    if (activeTab.value === 'ALL') {
      const [workRows, freeRows] = await Promise.all([
        slateApi.boardPosts('WORK', sort.value, 60, workSearchKeyword.value),
        slateApi.boardPosts('FREE', sort.value, 60, workSearchKeyword.value)
      ])
      if (requestId !== postsRequestId) return
      posts.value = sortBoardRows([...workRows, ...freeRows], sort.value).slice(0, 60)
      boardCurrentPage.value = 1
      selected.value = null
      return
    }
    if (activeTab.value !== 'WORK' && activeTab.value !== 'FREE') return
    const rows = await slateApi.boardPosts(activeTab.value, sort.value, 60, workSearchKeyword.value, {
      freeCategory: activeTab.value === 'FREE' ? freeCategory.value : '',
      workType: activeTab.value === 'WORK' ? workType.value : '',
      genreId: activeTab.value === 'WORK' ? genreId.value : ''
    })
    if (requestId !== postsRequestId) return
    posts.value = rows
    boardCurrentPage.value = 1
    const next = preferredPostId
      ? posts.value.find((post) => Number(post.postId) === Number(preferredPostId))
      : null
    if (next) await selectPost(next)
    else selected.value = null
  } catch (err) {
    if (requestId !== postsRequestId) return
    posts.value = []
    rankings.value = []
    popularProfiles.value = []
    popularProfilesLoadedLimit.value = 0
    error.value = err.message
  } finally {
    if (requestId === postsRequestId) loading.value = false
  }
}

async function loadHome() {
  const requestId = ++homeRequestId
  const [works, freePosts, popularWorkRows, profileRows] = await Promise.all([
    slateApi.boardPosts('WORK', 'latest', 4),
    slateApi.boardPosts('FREE', 'latest', 5),
    slateApi.boardRankings('POPULAR_WORK', 5),
    slateApi.boardRankings('POPULAR_PROFILE', 5)
  ])
  if (requestId !== homeRequestId) return
  homeWorks.value = works
  homeFreePosts.value = freePosts
  rankings.value = popularWorkRows
  popularProfiles.value = profileRows
  popularProfilesLoadedLimit.value = 5
  posts.value = []
}

async function loadGenres() {
  try {
    genres.value = await slateApi.genres()
  } catch {
    genres.value = []
  }
}

async function toggleProfileFollow(profile) {
  if (!props.currentUser) {
    emit('login')
    return
  }
  if (!profile || Number(profile.userId) === Number(props.currentUser.userId) || followSavingProfileId.value) return
  followSavingProfileId.value = profile.profileId
  error.value = ''
  try {
    if (profile.followingByCurrentUser) await slateApi.unfollowProfile(profile.profileId)
    else await slateApi.followProfile(profile.profileId)
    const profileLimit = activeTab.value === 'POPULAR' ? 10 : 5
    popularProfiles.value = await slateApi.boardRankings('POPULAR_PROFILE', profileLimit)
    popularProfilesLoadedLimit.value = profileLimit
  } catch (err) {
    error.value = err.message
  } finally {
    followSavingProfileId.value = null
  }
}

function openPublicProfile(profileId) {
  if (!profileId) return
  router.push({ name: 'public-profile', params: { profileId } })
}

async function selectPost(post) {
  error.value = ''
  try {
    pendingPostDelete.value = false
    pendingReviewDeleteId.value = null
    reportTarget.value = null
    selected.value = await slateApi.boardPost(post.postId)
    reviewDraft.value = ''
    editingReviewId.value = null
  } catch (err) {
    error.value = err.message
  }
}

async function loadRoutePost(postId, { edit = false } = {}) {
  if (!postId) return
  loading.value = true
  error.value = ''
  try {
    await selectPost({ postId })
    activeTab.value = selected.value?.category || 'WORK'
    activeBoardPanel.value = ''
    if (edit) populateSelectedPostForm()
  } finally {
    loading.value = false
  }
}

async function savePost() {
  if (!props.currentUser) {
    emit('login')
    return
  }
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    const payload = postPayload()
    if (!payload.title || !payload.content) throw new Error('제목과 내용을 입력해주세요.')
    const wasEditing = Boolean(editingPostId.value)
    if (!wasEditing && teamWorkNeedsApproval.value) {
      await slateApi.createTeamWorkRequest(payload)
      resetPostForm(payload.category)
      await loadMyTeamWorkRequests()
      await loadTeamWorkRequests()
      saved.value = '팀 작업물 승인 요청을 보냈습니다.'
      await router.push(boardListLocation('WORK'))
      return
    }
    const savedPost = editingPostId.value
      ? await slateApi.updateBoardPost(editingPostId.value, payload)
      : await slateApi.createBoardPost(payload)
    if (payload.category === 'WORK' && workImageFile.value) await slateApi.uploadEntityImage('work', savedPost.postId, workImageFile.value)
    else if (payload.category === 'WORK' && workImageDelete.value && savedPost.representativeImageUrl) await slateApi.deleteEntityImage('work', savedPost.postId)
    resetPostForm(payload.category)
    await router.push({ name: 'boards-detail', params: { postId: savedPost.postId } })
    saved.value = wasEditing ? '게시글이 수정되었습니다.' : '게시글이 작성되었습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function deleteSelectedPost() {
  if (!selected.value || !canManagePost.value) return
  const deletedYoutubeWork = selectedIsYoutubeWork.value
  const deletedCategory = selected.value.category
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.deleteBoardPost(selected.value.postId)
    selected.value = null
    resetPostForm(deletedCategory)
    pendingPostDelete.value = false
    saved.value = deletedYoutubeWork ? 'YouTube 작업물 게시글이 삭제되었습니다.' : '게시글이 삭제되었습니다.'
    await router.push(boardListLocation(deletedCategory))
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function toggleLike() {
  if (!props.currentUser) {
    emit('login')
    return
  }
  if (!selected.value) return
  try {
    const result = await slateApi.toggleBoardLike(selected.value.postId)
    syncPostLikeState(selected.value.postId, result)
  } catch (err) {
    error.value = err.message
  }
}

function scopeLabel(scope) {
  return boardSearchScopeOptions.find((item) => item.key === normalizedScope(scope))?.label || '전체'
}

async function saveReview() {
  if (!props.currentUser) {
    emit('login')
    return
  }
  if (!selected.value) return
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.createReview(selected.value.postId, { content: reviewDraft.value })
    selected.value = await slateApi.boardPost(selected.value.postId)
    reviewDraft.value = ''
    saved.value = '리뷰가 등록되었습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

function editReview(review) {
  editingReviewId.value = review.reviewId
  editingReviewContent.value = review.content
}

async function updateReview() {
  if (!editingReviewId.value) return
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.updateReview(editingReviewId.value, { content: editingReviewContent.value })
    selected.value = await slateApi.boardPost(selected.value.postId)
    editingReviewId.value = null
    editingReviewContent.value = ''
    saved.value = '리뷰가 수정되었습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

async function deleteReview(review) {
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    await slateApi.deleteReview(review.reviewId)
    selected.value = await slateApi.boardPost(selected.value.postId)
    pendingReviewDeleteId.value = null
    saved.value = '리뷰가 삭제되었습니다.'
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

function openReport(targetType, target) {
  if (!props.currentUser) {
    emit('login')
    return
  }
  clearPendingActions()
  reportTarget.value = {
    targetType,
    targetId: targetType === 'BOARD_POST' ? target.postId : target.reviewId,
    title: targetType === 'BOARD_POST' ? target.title : `리뷰 #${target.reviewId}`
  }
  reportForm.reasonCode = 'SPAM'
  reportForm.detail = ''
}

function closeReport() {
  reportTarget.value = null
  reportForm.reasonCode = 'SPAM'
  reportForm.detail = ''
}

async function submitReport() {
  if (!reportTarget.value) return
  saving.value = true
  error.value = ''
  saved.value = ''
  try {
    const payload = {
      reasonCode: reportForm.reasonCode,
      detail: reportForm.detail.trim()
    }
    if (reportTarget.value.targetType === 'BOARD_POST') {
      await slateApi.reportBoardPost(reportTarget.value.targetId, payload)
    } else {
      await slateApi.reportReview(reportTarget.value.targetId, payload)
    }
    saved.value = '신고가 접수되었습니다.'
    closeReport()
  } catch (err) {
    error.value = err.message
  } finally {
    saving.value = false
  }
}

function openRankedPost(postId) {
  if (!postId) return
  router.push({ name: 'boards-detail', params: { postId }, query: { from: activeTab.value } })
}

function goBoardPage(page) {
  const next = Math.min(Math.max(Number(page) || 1, 1), boardTotalPages.value)
  boardCurrentPage.value = next
}

function freeCategoryLabel(code) {
  return freeCategories.find((item) => item.key === (code || 'FREE'))?.label || '자유'
}

function workTypeLabel(code) {
  return workTypes.find((item) => item.key === (code || 'OTHER'))?.label || '기타'
}

function handleWorkImageError(event) {
  const image = event.currentTarget
  const candidates = JSON.parse(image.dataset.fallbacks || '[]')
  const nextIndex = Number(image.dataset.fallbackIndex || 0) + 1
  if (candidates[nextIndex]) {
    image.dataset.fallbackIndex = String(nextIndex)
    image.src = candidates[nextIndex]
    return
  }
  image.onerror = null
  image.src = defaultWorkImage
}

function workImage(item) {
  return workImageCandidates(item)[0]
}

function workImageCandidates(item) {
  return [...new Set([
    workField(item, 'representativeImageUrl'),
    youtubeMeta(item).thumbnailUrl,
    defaultWorkImage
  ].filter(Boolean))]
}

function workImageFallbacks(item) {
  return JSON.stringify(workImageCandidates(item))
}

function profileImageAvailable(profile) {
  return Boolean(profile?.profileImageUrl) && !failedProfileImages.value.has(Number(profile.profileId))
}

function profileImage(profile) {
  return profileImageAvailable(profile) ? profile.profileImageUrl : defaultProfileImage
}

function handleProfileImageError(profileId) {
  const next = new Set(failedProfileImages.value)
  next.add(Number(profileId))
  failedProfileImages.value = next
}

function teamWorkStatusLabel(status) {
  return {
    PENDING: '승인 대기',
    APPROVED: '승인 완료',
    REJECTED: '거절',
    CANCELED: '취소'
  }[status] || status
}

function workField(item, key) {
  return item?.work?.[key] ?? item?.[key]
}

function youtubeMeta(item) {
  return {
    url: workField(item, 'youtubeUrl') || '',
    videoId: workField(item, 'youtubeVideoId') || '',
    title: workField(item, 'youtubeTitle') || '',
    channelTitle: workField(item, 'youtubeChannelTitle') || '',
    thumbnailUrl: workField(item, 'youtubeThumbnailUrl') || '',
    durationSeconds: workField(item, 'youtubeDurationSeconds') || null
  }
}

function hasYoutubeWork(item) {
  const mediaType = workField(item, 'mediaType')
  return mediaType === 'YOUTUBE' || Boolean(workField(item, 'youtubeUrl'))
}

function workDurationLabel(item) {
  const youtube = youtubeMeta(item)
  if (hasYoutubeWork(item) && youtube.durationSeconds) return formatDuration(youtube.durationSeconds)
  const fileDuration = workField(item, 'fileDurationSeconds')
  return fileDuration ? formatDuration(fileDuration) : ''
}

function formatDuration(value) {
  const seconds = Number(value || 0)
  const minutes = String(Math.floor(seconds / 60)).padStart(2, '0')
  const remainder = String(seconds % 60).padStart(2, '0')
  return `${minutes}:${remainder}`
}

watch([activeTab, sort, freeCategory, workType, genreId, () => props.currentUser?.userId], () => {
  if (isStandaloneBoardRoute.value) return
  if (isBoardSearchRoute.value || activeTab.value === 'POPULAR') return
  const preferred = pendingPostId.value
  pendingPostId.value = null
  loadPosts(preferred)
})

watch(
  [
    () => route.name,
    () => route.params.postId,
    () => route.query.category,
    () => route.query.tab,
    () => route.query.scope,
    () => route.query.keyword,
    () => route.query.sort,
    () => route.query.freeCategory,
    () => route.query.workType,
    () => route.query.genreId,
    () => route.query.period
  ],
  ([name, postId, category, tab, scope, keyword, querySort, queryFreeCategory, queryWorkType, queryGenreId, period]) => {
    clearPendingActions()
    reportTarget.value = null
    if (name === 'boards-new') {
      const nextCategory = category === 'FREE' ? 'FREE' : 'WORK'
      activeTab.value = nextCategory
      activeBoardPanel.value = ''
      selected.value = null
      resetPostForm(nextCategory)
      return
    }
    if (name === 'boards-detail') {
      resetPostForm('WORK')
      loadRoutePost(postId)
      return
    }
    if (name === 'boards-edit') {
      resetPostForm('WORK')
      loadRoutePost(postId, { edit: true })
      return
    }
    if (name === 'boards') {
      selected.value = null
      activeBoardPanel.value = ''
      const nextTab = ['WORK', 'FREE', 'POPULAR'].includes(String(tab).toUpperCase())
        ? String(tab).toUpperCase()
        : 'HOME'
      if (nextTab === 'POPULAR') {
        popularPeriod.value = ['WEEKLY', 'MONTHLY', 'ALL'].includes(String(period).toUpperCase()) ? String(period).toUpperCase() : 'ALL'
        popularWorkType.value = String(queryWorkType || '')
        popularGenreId.value = String(queryGenreId || '')
      }
      if (activeTab.value === nextTab) loadPosts()
      else {
        activeTab.value = nextTab
        if (nextTab === 'POPULAR') loadPosts()
      }
      return
    }
    if (name === 'boards-search') {
      selected.value = null
      activeBoardPanel.value = ''
      activeTab.value = normalizedScope(scope)
      workSearchInput.value = String(keyword || '')
      workSearchKeyword.value = workSearchInput.value.trim()
      sort.value = ['latest', 'likes', 'views', 'reaction'].includes(String(querySort)) ? String(querySort) : 'latest'
      freeCategory.value = activeTab.value === 'FREE' ? String(queryFreeCategory || '') : ''
      workType.value = activeTab.value === 'WORK' ? String(queryWorkType || '') : ''
      genreId.value = activeTab.value === 'WORK' ? String(queryGenreId || '') : ''
      loadPosts()
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  if (workImagePreview.value) URL.revokeObjectURL(workImagePreview.value)
})

watch(() => props.currentUser?.userId, () => {
  loadBoardTeams()
  loadGenres()
}, { immediate: true })

watch(approvalTeamId, () => {
  loadTeamWorkRequests()
})

watch(() => postForm.teamId, () => {
  if (editingPostId.value) return
  postForm.fileId = null
  selectedWorkFile.value = null
  uploadedWorkFile.value = null
  workFileDuration.value = null
})
</script>

<template>
  <section class="board-page">
    <div v-if="isBoardListRoute || isBoardSearchRoute" class="board-dashboard-head">
      <nav class="board-dashboard-tabs" aria-label="게시판 탭">
        <button
          v-for="tab in dashboardTabs"
          :key="tab.key"
          :class="{ active: activeTab === tab.key }"
          type="button"
          @click="openDashboardTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </nav>
      <div class="board-dashboard-actions">
        <button v-if="activeTab === 'HOME' || activeTab === 'FREE'" class="board-outline-button" type="button" @click="openBoardCompose('FREE')">
          <span>✎</span>
          글쓰기
        </button>
        <button v-if="activeTab === 'HOME' || activeTab === 'WORK'" class="board-primary-button" type="button" @click="openBoardCompose('WORK')">
          <span>☁</span>
          작업물 올리기
        </button>
        <button v-if="props.currentUser && managedTeams.length && (activeTab === 'HOME' || activeTab === 'WORK')" class="board-outline-button" type="button" @click="openTeamApprovalPanel">
          <span>♙</span>
          팀 작업물 승인
        </button>
      </div>
    </div>

    <section v-if="isBoardListRoute && activeTab === 'HOME' && !activeBoardPanel" class="board-home-search board-section-card">
      <form class="board-search-form" @submit.prevent="submitHomeSearch">
        <div class="board-search-scope">
          <span>게시판 선택</span>
          <div class="board-scope-select">
            <button
              class="board-scope-trigger"
              type="button"
              :aria-expanded="homeSearchScopeOpen"
              @click="homeSearchScopeOpen = !homeSearchScopeOpen"
            >
              {{ scopeLabel(homeSearchScope) }}
              <span aria-hidden="true">⌄</span>
            </button>
            <div v-if="homeSearchScopeOpen" class="board-scope-options" role="listbox" aria-label="게시판 선택">
              <button
                v-for="scope in boardSearchScopeOptions"
                :key="scope.key"
                class="board-scope-option"
                type="button"
                role="option"
                :aria-selected="homeSearchScope === scope.key"
                :class="{ active: homeSearchScope === scope.key }"
                @click="setSearchScope(scope.key)"
              >
                {{ scope.label }}
              </button>
            </div>
          </div>
        </div>
        <label class="board-search-grow">
          통합 검색
          <input v-model="homeSearchInput" type="search" maxlength="100" placeholder="제목과 내용을 검색하세요">
        </label>
        <button class="board-primary-button" type="submit">검색</button>
      </form>
    </section>

    <section v-if="isBoardListRoute && activeTab === 'HOME' && !activeBoardPanel" class="board-dashboard-grid">
      <div class="board-main-column">
        <section class="board-section-card">
          <div class="board-section-head">
            <h2>최신 작업물</h2>
            <button type="button" @click="openDashboardTab('WORK')">더 보기 ›</button>
          </div>
          <div class="board-work-grid">
            <article
              v-for="work in dashboardWorks"
              :key="work.id"
              class="board-work-card"
              @click="openDashboardPost(work, 'HOME')"
            >
              <figure>
                <img :src="work.image" :alt="work.title" :data-fallbacks="workImageFallbacks(work.raw)" data-fallback-index="0" @error="handleWorkImageError">
                <figcaption>{{ work.duration }}</figcaption>
              </figure>
              <div class="board-work-body">
                <div>
                  <strong>{{ work.title }}</strong>
                </div>
                <small v-if="work.youtubeTitle" class="board-youtube-title">{{ work.youtubeTitle }}</small>
                <p>
                  <span>{{ work.tag }}</span>
                  {{ work.author }}
                </p>
                <footer>
                  <span class="board-stat">
                    <span class="board-stat-icon like" :class="{ 'is-liked': work.liked }" aria-hidden="true">{{ work.liked ? '♥' : '♡' }}</span>
                    {{ work.likes }}
                  </span>
                  <span class="board-stat">
                    <span class="board-stat-icon comment" aria-hidden="true"></span>
                    {{ work.comments }}
                  </span>
                </footer>
              </div>
            </article>
            <p v-if="dashboardWorks.length === 0 && !loading" class="muted board-grid-empty">등록된 최신 작업물이 없습니다.</p>
          </div>
        </section>

        <section class="board-section-card board-free-card">
          <div class="board-section-head">
            <h2>자유 게시판</h2>
            <button type="button" @click="openDashboardTab('FREE')">더 보기 ›</button>
          </div>
          <article
            v-for="post in dashboardFreePosts"
            :key="post.post.postId"
            class="board-free-row"
            @click="openDashboardPost(post, 'HOME')"
          >
            <span :class="['board-free-icon', post.tone]">{{ post.type }}</span>
            <div>
              <strong>{{ post.title }}</strong>
              <p>{{ post.content }}</p>
            </div>
            <aside>
              <b>{{ post.author }}</b>
              <small>{{ post.time }}</small>
            </aside>
            <footer>
              <span class="board-stat">
                <span class="board-stat-icon like" :class="{ 'is-liked': post.liked }" aria-hidden="true">{{ post.liked ? '♥' : '♡' }}</span>
                {{ post.likes }}
              </span>
              <span class="board-stat">
                <span class="board-stat-icon comment" aria-hidden="true"></span>
                {{ post.comments }}
              </span>
            </footer>
          </article>
          <p v-if="dashboardFreePosts.length === 0 && !loading" class="muted">등록된 자유게시글이 없습니다.</p>
        </section>
      </div>

      <aside class="board-sidebar-column">
        <section class="board-side-card">
          <div class="board-section-head">
            <h2>인기 작업물</h2>
            <button type="button" @click="openDashboardTab('POPULAR')">더 보기 ›</button>
          </div>
          <article v-for="(work, index) in periodPopularWorks.slice(0, 5)" :key="work.postId" class="board-popular-row" tabindex="0" @click="openRankedPost(work.postId)" @keydown.enter="openRankedPost(work.postId)">
            <b>{{ index + 1 }}</b>
            <img :src="work.image" :alt="work.title" :data-fallbacks="workImageFallbacks(work.raw)" data-fallback-index="0" @error="handleWorkImageError">
            <div>
              <strong>{{ work.title }}</strong>
              <small>{{ work.tag }}</small>
            </div>
            <span class="board-stat board-stat-right">
              <span class="board-stat-icon like" :class="{ 'is-liked': work.liked }" aria-hidden="true">{{ work.liked ? '♥' : '♡' }}</span>
              {{ work.likes }}
            </span>
          </article>
          <p v-if="periodPopularWorks.length === 0 && !loading" class="muted">인기 작업물이 없습니다.</p>
        </section>

        <section class="board-side-card">
          <div class="board-section-head">
            <h2>인기 프로필</h2>
          </div>
          <article v-for="(profile, index) in popularProfiles" :key="profile.profileId" class="board-profile-row">
            <b>{{ index + 1 }}</b>
            <img class="board-profile-avatar-image" :src="profileImage(profile)" :alt="`${profile.displayName} 프로필`" @error="handleProfileImageError(profile.profileId)">
            <div role="link" tabindex="0" @click="openPublicProfile(profile.profileId)" @keydown.enter="openPublicProfile(profile.profileId)">
              <strong>{{ profile.displayName }}</strong>
              <small>{{ profile.primaryRoleName || profile.shortIntro || '역할 정보 없음' }}</small>
            </div>
            <button
              v-if="Number(profile.userId) !== Number(props.currentUser?.userId)"
              class="board-profile-follow"
              :class="{ active: profile.followingByCurrentUser }"
              type="button"
              :aria-label="profile.followingByCurrentUser ? `${profile.displayName} 팔로우 취소` : `${profile.displayName} 팔로우`"
              :aria-pressed="Boolean(profile.followingByCurrentUser)"
              :disabled="followSavingProfileId === profile.profileId"
              @click="toggleProfileFollow(profile)"
            >♥ <span>{{ profile.followerCount ?? 0 }}</span></button>
          </article>
          <p v-if="popularProfiles.length === 0 && !loading" class="muted">인기 프로필이 없습니다.</p>
        </section>
      </aside>
    </section>

    <section v-if="activeBoardPanel || showBoardListPanel || isStandaloneBoardRoute" class="board-editor-shell">
      <div
        v-if="activeTab === 'WORK' || activeTab === 'FREE'"
        class="board-grid"
        :class="{ 'board-route-detail-grid': isBoardDetailRoute, 'board-route-form-grid': showPostForm, 'board-route-list-grid': showBoardListPanel }"
      >
      <div v-if="!isBoardDetailRoute" class="stack">
        <form v-if="showPostForm" class="form-panel" @submit.prevent="savePost">
          <div class="form-head">
            <div>
              <span class="eyebrow">{{ editingPostId ? '수정' : '작성' }}</span>
              <h2>{{ postForm.category === 'WORK' ? '작업물 게시글' : '자유 게시글' }}</h2>
            </div>
            <button class="ghost-button" type="button" @click="router.push(boardListLocation(postForm.category))">목록</button>
            <button v-if="isBoardEditRoute && selected" class="ghost-button" type="button" @click="router.push({ name: 'boards-detail', params: { postId: selected.postId } })">상세</button>
            <button class="primary-button" type="submit" :disabled="saving || youtubeRegistrationBlocked">
              {{ saving ? '저장 중' : teamWorkNeedsApproval ? '승인 요청' : '저장' }}
            </button>
          </div>

          <p v-if="error" class="error-text">{{ error }}</p>
          <p v-if="saved" class="notice-text">{{ saved }}</p>

          <div class="form-grid">
            <label class="field">
              <span>게시판</span>
              <input :value="postForm.category === 'WORK' ? '작업물' : '자유'" readonly>
            </label>
            <label class="field">
              <span>공개 범위</span>
              <select v-model="postForm.visibility">
                <option value="PUBLIC">전체 공개</option>
                <option value="COMPANY">회사 공개</option>
                <option value="PRIVATE">비공개</option>
              </select>
            </label>
            <label v-if="postForm.category === 'FREE'" class="field wide">
              <span>자유게시판 분류</span>
              <select v-model="postForm.freeCategory" required>
                <option
                  v-for="category in freeCategories.filter((item) => item.key && (item.key !== 'NOTICE' || props.currentUser?.accountType === 'ADMIN'))"
                  :key="category.key"
                  :value="category.key"
                >{{ category.label }}</option>
              </select>
            </label>
            <label v-if="postForm.category === 'WORK'" class="field wide">
              <span>작품 유형</span>
              <select v-model="postForm.workType" required>
                <option v-for="type in workTypes.filter((item) => item.key)" :key="type.key" :value="type.key">{{ type.label }}</option>
              </select>
            </label>
            <fieldset v-if="postForm.category === 'WORK'" class="field wide board-genre-field">
              <legend>장르</legend>
              <div class="board-genre-options">
                <label v-for="genre in genres" :key="genre.genreId">
                  <input v-model="postForm.genreIds" type="checkbox" :value="Number(genre.genreId)">
                  <span>{{ genre.name }}</span>
                </label>
              </div>
              <small v-if="genres.length === 0">등록된 장르가 없습니다.</small>
            </fieldset>
            <label v-if="postForm.category === 'WORK'" class="field wide">
              <span>작업 주체</span>
              <select v-model="postForm.teamId">
                <option value="">개인 작업물</option>
                <option v-for="team in workTeams" :key="team.teamId" :value="team.teamId">
                  {{ team.name }} · {{ team.myTeamRole === 'LEADER' ? '팀장' : team.myTeamRole === 'SUB_LEADER' ? '부팀장' : '팀원' }}
                </option>
              </select>
            </label>
            <p v-if="teamWorkNeedsApproval" class="notice-text wide">팀원 작업물은 팀장 또는 부팀장 승인 후 작업물 게시판에 공개됩니다.</p>
            <div v-if="postForm.category === 'WORK' && !teamWorkNeedsApproval" class="image-picker wide">
              <img :src="workImagePreview || (!workImageDelete && selected?.representativeImageUrl) || defaultWorkImage" alt="작품 대표 이미지 미리보기">
              <div>
                <strong>작품 대표 이미지</strong><small>미지정 시 YouTube 썸네일, 이후 기본 이미지가 사용됩니다.</small>
                <label class="ghost-button inline">이미지 선택<input type="file" accept="image/jpeg,image/png,image/webp" @change="selectWorkImage"></label>
                <button v-if="workImagePreview || selected?.representativeImageUrl" class="ghost-button danger" type="button" @click="removeWorkImage">이미지 삭제</button>
              </div>
            </div>
            <label class="field wide">
              <span>제목</span>
              <input v-model="postForm.title" maxlength="150" required>
            </label>
            <label class="field wide">
              <span>내용</span>
              <textarea v-model="postForm.content" rows="5" required></textarea>
            </label>
            <div v-if="postForm.category === 'WORK'" class="youtube-preview-field wide">
              <label class="field">
                <span>유튜브 URL</span>
                <input
                  v-model="postForm.youtubeUrl"
                  :disabled="Boolean(postForm.fileId) || Boolean(selectedWorkFile)"
                  placeholder="https://www.youtube.com/watch?v=..."
                  maxlength="500"
                  @input="handleYoutubeUrlInput"
                >
              </label>
              <button
                class="ghost-button"
                type="button"
                :disabled="!canPreviewYoutube"
                @click="previewYoutubeVideo"
              >
                {{ youtubePreviewLoading ? '확인 중' : '미리보기' }}
              </button>
            </div>
            <p v-if="postForm.category === 'WORK' && (selectedWorkFile || postForm.fileId)" class="muted wide">
              서버 업로드 파일을 사용하는 동안 유튜브 미리보기는 비활성화됩니다.
            </p>
            <p v-if="postForm.category === 'WORK' && youtubePreviewError" class="error-text wide">{{ youtubePreviewError }}</p>
            <article v-if="postForm.category === 'WORK' && youtubePreview" class="youtube-preview-card wide">
              <figure class="youtube-preview-thumb">
                <img v-if="youtubePreview.thumbnailUrl" :src="youtubePreview.thumbnailUrl" alt="">
                <span v-else>NO THUMBNAIL</span>
              </figure>
              <div class="youtube-preview-meta">
                <span class="youtube-preview-kicker">{{ youtubePreviewCardLabel }}</span>
                <strong>{{ youtubePreview.title }}</strong>
                <p>{{ youtubePreview.channelTitle }}</p>
                <div class="subline">
                  <span>{{ formatYoutubeDuration(youtubePreview.durationSeconds) }}</span>
                  <span>{{ youtubePreview.videoId }}</span>
                </div>
              </div>
            </article>
            <p v-if="postForm.category === 'WORK' && youtubePreviewMatchesCurrentUrl" class="notice-text wide">
              {{ youtubePreviewSuccessMessage }}
            </p>
            <p v-else-if="postForm.category === 'WORK' && youtubeSaveBlockedMessage && !youtubePreviewError" class="notice-text wide">
              {{ youtubeSaveBlockedMessage }}
            </p>
            <label v-if="postForm.category === 'WORK'" class="field wide">
              <span>서버 영상 업로드</span>
              <input accept=".mp4,.webm,.mov,video/mp4,video/webm,video/quicktime" type="file" @change="onWorkFileChange">
            </label>
            <div v-if="postForm.category === 'WORK'" class="upload-inline wide">
              <div>
                <strong>{{ uploadedWorkFile?.originalName || selectedWorkFile?.name || '선택된 파일 없음' }}</strong>
                <p v-if="workFileDuration">길이 {{ workFileDuration }}초</p>
                <p v-else-if="postForm.fileId">기존 서버 파일이 연결되어 있습니다. YouTube로 전환하려면 연결을 해제하세요.</p>
                <p v-else-if="selectedWorkFile">브라우저에서 길이를 확인하지 못했습니다. 서버 ffprobe가 필요합니다.</p>
              </div>
              <div class="upload-actions">
                <button class="ghost-button" type="button" :disabled="!selectedWorkFile || uploadingFile || Boolean(postForm.fileId)" @click="uploadSelectedWorkFile">
                  {{ uploadingFile ? '업로드 중' : postForm.fileId ? '업로드 완료' : '파일 업로드' }}
                </button>
                <button v-if="postForm.fileId" class="ghost-button" type="button" @click="clearLinkedWorkFile">연결 해제</button>
              </div>
            </div>
          </div>
        </form>

        <section v-if="props.currentUser && myTeamWorkRequests.length && (showPostForm || showTeamApprovalTools)" class="form-panel">
          <div class="form-head">
            <div>
              <span class="eyebrow">내 요청</span>
              <h2>팀 작업물 승인 상태</h2>
            </div>
          </div>
          <article v-for="request in myTeamWorkRequests" :key="request.requestId" class="list-panel">
            <div>
              <strong>{{ request.title }}</strong>
              <small v-if="request.youtubeTitle" class="board-youtube-title">{{ request.youtubeTitle }}</small>
              <p>{{ request.teamName }} · {{ teamWorkStatusLabel(request.status) }}</p>
              <div class="subline">
                <span>{{ request.mediaType }}</span>
                <span v-if="request.youtubeChannelTitle">YouTube · {{ request.youtubeChannelTitle }}</span>
                <span v-if="request.youtubeDurationSeconds">{{ formatDuration(request.youtubeDurationSeconds) }}</span>
                <span v-if="request.fileOriginalName">{{ request.fileOriginalName }}</span>
                <span v-if="request.decidedByNickname">처리 {{ request.decidedByNickname }}</span>
                <span v-if="request.rejectReason">{{ request.rejectReason }}</span>
              </div>
            </div>
            <button v-if="request.boardPostId" class="ghost-button" type="button" @click="openRankedPost(request.boardPostId)">게시글</button>
          </article>
        </section>

        <section v-if="showTeamApprovalTools && managedTeams.length" class="form-panel">
          <div class="form-head">
            <div>
              <span class="eyebrow">팀 승인</span>
              <h2>팀 작업물 요청</h2>
            </div>
            <label class="field compact-field">
              <span>팀</span>
              <select v-model="approvalTeamId">
                <option v-for="team in managedTeams" :key="team.teamId" :value="team.teamId">{{ team.name }}</option>
              </select>
            </label>
          </div>
          <article v-for="request in teamWorkRequests" :key="request.requestId" class="list-panel">
            <div>
              <strong>{{ request.title }}</strong>
              <small v-if="request.youtubeTitle" class="board-youtube-title">{{ request.youtubeTitle }}</small>
              <p>{{ request.requesterNickname }} · {{ teamWorkStatusLabel(request.status) }}</p>
              <div class="subline">
                <span>{{ request.mediaType }}</span>
                <span v-if="request.youtubeChannelTitle">YouTube · {{ request.youtubeChannelTitle }}</span>
                <span v-if="request.youtubeDurationSeconds">{{ formatDuration(request.youtubeDurationSeconds) }}</span>
                <span v-if="request.fileOriginalName">{{ request.fileOriginalName }}</span>
                <span v-if="request.boardPostId">게시글 #{{ request.boardPostId }}</span>
                <span v-if="request.rejectReason">{{ request.rejectReason }}</span>
              </div>
            </div>
            <div v-if="request.status === 'PENDING'" class="row-actions decision-actions">
              <input v-model="decisionReasons[request.requestId]" placeholder="거절 사유">
              <button class="primary-button" type="button" :disabled="requestSaving" @click="decideTeamWorkRequest(request, 'APPROVED')">승인</button>
              <button class="ghost-button" type="button" :disabled="requestSaving" @click="decideTeamWorkRequest(request, 'REJECTED')">거절</button>
            </div>
            <button v-else-if="request.boardPostId" class="ghost-button" type="button" @click="openRankedPost(request.boardPostId)">게시글</button>
          </article>
          <p v-if="teamWorkRequests.length === 0" class="muted">처리할 팀 작업물 요청이 없습니다.</p>
        </section>

        <div v-if="showBoardListPanel" class="tool-surface">
          <div class="filters">
            <div v-if="isBoardSearchRoute" class="board-search-scope board-search-route-scope">
              <span>게시판 선택</span>
              <div class="board-scope-select">
                <button
                  class="board-scope-trigger"
                  type="button"
                  :aria-expanded="routeSearchScopeOpen"
                  @click="routeSearchScopeOpen = !routeSearchScopeOpen"
                >
                  {{ scopeLabel(activeTab) }}
                  <span aria-hidden="true">⌄</span>
                </button>
                <div v-if="routeSearchScopeOpen" class="board-scope-options" role="listbox" aria-label="검색 게시판 선택">
                  <button
                    v-for="scope in boardSearchScopeOptions"
                    :key="scope.key"
                    class="board-scope-option"
                    type="button"
                    role="option"
                    :aria-selected="activeTab === scope.key"
                    :class="{ active: activeTab === scope.key }"
                    @click="setSearchScope(scope.key)"
                  >
                    {{ scope.label }}
                  </button>
                </div>
              </div>
            </div>
            <form class="board-search-form" @submit.prevent="submitWorkSearch">
              <label>
                검색
                <input
                  v-model="workSearchInput"
                  type="search"
                  :placeholder="activeTab === 'WORK' ? '제목, 내용, 채널명, 작성자를 검색하세요' : '제목과 내용을 검색하세요'"
                  maxlength="100"
                >
              </label>
              <button class="primary-button" type="submit" :disabled="loading">검색</button>
            </form>
            <label v-if="activeTab === 'FREE'">
              세부 분류
              <select v-model="freeCategory" @change="syncSearchRoute">
                <option v-for="category in freeCategories" :key="category.key" :value="category.key">{{ category.label }}</option>
              </select>
            </label>
            <label v-if="activeTab === 'WORK'">
              작품 유형
              <select v-model="workType" @change="syncSearchRoute">
                <option v-for="type in workTypes" :key="type.key" :value="type.key">{{ type.label }}</option>
              </select>
            </label>
            <label v-if="activeTab === 'WORK'">
              장르
              <select v-model="genreId" @change="syncSearchRoute">
                <option value="">전체 장르</option>
                <option v-for="genre in genres" :key="genre.genreId" :value="genre.genreId">{{ genre.name }}</option>
              </select>
            </label>
            <label>
              정렬
              <select v-model="sort" @change="syncSearchRoute">
                <option value="reaction">반응순</option>
                <option value="latest">최신순</option>
                <option value="likes">좋아요순</option>
                <option value="views">조회순</option>
              </select>
            </label>
          </div>

          <div class="result-list">
            <p v-if="loading" class="notice-text">게시글을 불러오는 중입니다.</p>
            <article
              v-for="post in paginatedPosts"
              :key="post.postId"
              class="list-panel selectable"
              :class="{ selected: selected?.postId === post.postId, 'board-free-list-row': postCategory(post) === 'FREE' }"
              :role="activeTab !== 'WORK' ? 'link' : undefined"
              :tabindex="activeTab !== 'WORK' ? 0 : undefined"
              @click="activeTab !== 'WORK' && router.push({ name: 'boards-detail', params: { postId: post.postId }, query: { from: activeTab } })"
              @keydown.enter="activeTab !== 'WORK' && router.push({ name: 'boards-detail', params: { postId: post.postId }, query: { from: activeTab } })"
            >
              <img v-if="postCategory(post) === 'WORK'" class="board-list-thumb" :src="workImage(post)" :alt="post.title" :data-fallbacks="workImageFallbacks(post)" data-fallback-index="0" @error="handleWorkImageError">
              <div>
                <strong>{{ post.title }}</strong>
                <small v-if="postCategory(post) === 'FREE'" class="board-free-category-label">{{ freeCategoryLabel(post.freeCategory) }}</small>
                <small v-else class="board-free-category-label">{{ workTypeLabel(post.workType) }}</small>
                <small v-if="youtubeMeta(post).title" class="board-youtube-title">{{ youtubeMeta(post).title }}</small>
                <p>{{ post.content }}</p>
                <div class="subline">
                  <span>{{ post.authorNickname }}</span>
                  <span v-if="postCategory(post) === 'FREE' && post.createdAt">{{ String(post.createdAt).slice(0, 10) }}</span>
                  <span v-if="youtubeMeta(post).channelTitle">YouTube · {{ youtubeMeta(post).channelTitle }}</span>
                  <span v-if="workDurationLabel(post)">{{ workDurationLabel(post) }}</span>
                  <span>좋아요 {{ post.likeCount }}</span>
                  <span>리뷰 {{ post.reviewCount }}</span>
                  <span>조회 {{ post.viewCount }}</span>
                </div>
              </div>
              <button v-if="activeTab === 'WORK'" class="ghost-button" type="button" @click="router.push({ name: 'boards-detail', params: { postId: post.postId }, query: { from: activeTab } })">보기</button>
            </article>
            <p v-if="!loading && posts.length === 0" class="muted">
              {{ hasActiveWorkSearch ? '검색 결과가 없습니다.' : '게시글이 없습니다.' }}
            </p>
            <nav v-if="!loading && posts.length > BOARD_PAGE_SIZE" class="board-list-pagination" aria-label="게시글 페이지">
              <button type="button" :disabled="boardCurrentPage === 1" @click="goBoardPage(boardCurrentPage - 1)">이전</button>
              <button
                v-for="page in boardPageNumbers"
                :key="`board-page-${page}`"
                type="button"
                :class="{ active: boardCurrentPage === page }"
                :aria-current="boardCurrentPage === page ? 'page' : undefined"
                @click="goBoardPage(page)"
              >
                {{ page }}
              </button>
              <button type="button" :disabled="boardCurrentPage === boardTotalPages" @click="goBoardPage(boardCurrentPage + 1)">다음</button>
            </nav>
          </div>
        </div>
      </div>

      <aside v-if="isBoardDetailRoute && selected" class="board-detail">
        <div class="detail-head">
          <div>
            <span class="eyebrow">{{ selected.category === 'FREE' ? freeCategoryLabel(selected.freeCategory) : workTypeLabel(selected.work?.workType || selected.workType) }}</span>
            <h2>{{ selected.title }}</h2>
          </div>
          <div class="top-actions">
            <button class="ghost-button" type="button" @click="router.push(boardListLocation(String(route.query.from || selected.category)))">목록</button>
            <button class="ghost-button" type="button" @click="toggleLike">
              {{ selected.likedByCurrentUser ? '좋아요 취소' : '좋아요' }}
            </button>
            <button v-if="canReportPost" class="ghost-button" type="button" @click="openReport('BOARD_POST', selected)">신고</button>
            <button v-if="canEditPost" class="ghost-button" type="button" @click="editSelectedPost">수정</button>
            <button v-if="canManagePost" class="ghost-button" type="button" @click="requestPostDelete">삭제</button>
          </div>
          <div v-if="pendingPostDelete" class="confirm-inline danger-confirm board-confirm">
            <span>{{ deleteConfirmMessage }}</span>
            <button class="primary-button" type="button" :disabled="saving" @click="deleteSelectedPost">삭제 확인</button>
            <button class="ghost-button" type="button" @click="pendingPostDelete = false">취소</button>
          </div>
        </div>

        <figure v-if="selected.category === 'WORK'" class="board-detail-cover">
          <img :src="workImage(selected)" :alt="selected.title" :data-fallbacks="workImageFallbacks(selected)" data-fallback-index="0" @error="handleWorkImageError">
        </figure>

        <div v-if="selected.work?.youtubeUrl" class="media-frame">
          <iframe
            :src="selected.work.youtubeUrl"
            title="작업물 영상"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowfullscreen
          ></iframe>
        </div>
        <article
          v-if="selected.work?.youtubeUrl"
          class="youtube-detail-meta"
          :class="{ 'without-thumb': !selected.work.youtubeThumbnailUrl }"
        >
          <img v-if="selected.work.youtubeThumbnailUrl" :src="selected.work.youtubeThumbnailUrl" alt="">
          <div>
            <strong>{{ selected.work.youtubeTitle || selected.title }}</strong>
            <p v-if="selected.work.youtubeChannelTitle">YouTube · {{ selected.work.youtubeChannelTitle }}</p>
            <div class="subline">
              <span v-if="selected.work.youtubeDurationSeconds">{{ formatDuration(selected.work.youtubeDurationSeconds) }}</span>
              <span v-if="selected.work.youtubeVideoId">{{ selected.work.youtubeVideoId }}</span>
            </div>
          </div>
        </article>
        <div v-else-if="selected.work?.fileId" class="media-frame">
          <video controls :src="workFileStreamUrl(selected.work.fileId)"></video>
        </div>

        <p class="detail-content">{{ selected.content }}</p>
        <div class="subline">
          <span>{{ selected.authorNickname }}</span>
          <span>좋아요 {{ selected.likeCount }}</span>
          <span>리뷰 {{ selected.reviewCount }}</span>
          <span>조회 {{ selected.viewCount }}</span>
          <span>점수 {{ selected.reactionScore }}</span>
        </div>

        <form v-if="reportTarget" class="report-compose" @submit.prevent="submitReport">
          <div class="row-head">
            <strong>{{ reportTarget.title }} 신고</strong>
            <button class="ghost-button" type="button" @click="closeReport">닫기</button>
          </div>
          <label class="field">
            <span>사유</span>
            <select v-model="reportForm.reasonCode">
              <option v-for="reason in reportReasons" :key="reason.key" :value="reason.key">{{ reason.label }}</option>
            </select>
          </label>
          <label class="field">
            <span>내용</span>
            <textarea v-model="reportForm.detail" rows="3" maxlength="1000"></textarea>
          </label>
          <button class="primary-button" type="submit" :disabled="saving">신고 접수</button>
        </form>

        <form class="review-compose" @submit.prevent="saveReview">
          <textarea v-model="reviewDraft" rows="3" maxlength="300" placeholder="리뷰를 입력하세요"></textarea>
          <button class="primary-button" type="submit" :disabled="saving">리뷰 등록</button>
        </form>

        <div class="review-list">
          <article v-for="review in selected.reviews" :key="review.reviewId" class="review-row">
            <div>
              <strong>{{ review.authorNickname }}</strong>
              <p v-if="editingReviewId !== review.reviewId">{{ review.content }}</p>
              <textarea v-else v-model="editingReviewContent" rows="3" maxlength="300"></textarea>
            </div>
            <div v-if="canManageReview(review) && review.status === 'PUBLISHED'" class="row-actions">
              <button v-if="editingReviewId !== review.reviewId" class="ghost-button" type="button" @click="editReview(review)">수정</button>
              <button v-else class="primary-button" type="button" @click="updateReview">저장</button>
              <button class="ghost-button" type="button" @click="requestReviewDelete(review)">삭제</button>
            </div>
            <div v-else-if="canReportReview(review)" class="row-actions">
              <button class="ghost-button" type="button" @click="openReport('BOARD_REVIEW', review)">신고</button>
            </div>
            <div v-if="pendingReviewDeleteId === review.reviewId" class="confirm-inline danger-confirm review-confirm">
              <span>이 리뷰를 삭제할까요?</span>
              <button class="primary-button" type="button" :disabled="saving" @click="deleteReview(review)">삭제 확인</button>
              <button class="ghost-button" type="button" @click="pendingReviewDeleteId = null">취소</button>
            </div>
          </article>
        </div>
      </aside>
      <section v-else-if="isBoardDetailRoute" class="board-detail">
        <p v-if="loading" class="muted">게시글을 불러오는 중입니다.</p>
        <p v-else-if="error" class="error-text">{{ error }}</p>
        <p v-else class="muted">게시글을 찾을 수 없습니다.</p>
        <button class="ghost-button" type="button" @click="router.push(boardListLocation(String(route.query.from || 'HOME')))">목록으로</button>
      </section>
    </div>
    </section>

    <section v-if="isBoardListRoute && activeTab === 'POPULAR'" class="board-popular-board">
      <div class="board-popular-columns">
        <section class="board-popular-works-panel">
          <h2>인기 작업물</h2>
          <div class="filters board-popular-filters">
            <label>
              작품 유형
              <select v-model="popularWorkType" @change="syncPopularRoute">
                <option v-for="type in workTypes" :key="type.key" :value="type.key">{{ type.label }}</option>
              </select>
            </label>
            <label>
              장르
              <select v-model="popularGenreId" @change="syncPopularRoute">
                <option value="">전체 장르</option>
                <option v-for="genre in genres" :key="genre.genreId" :value="genre.genreId">{{ genre.name }}</option>
              </select>
            </label>
            <div class="board-ranking-periods" role="tablist" aria-label="인기 작업물 집계 기간">
              <button v-for="period in [{ key: 'WEEKLY', label: '주간' }, { key: 'MONTHLY', label: '월간' }, { key: 'ALL', label: '전체' }]" :key="period.key" role="tab" type="button" :aria-selected="popularPeriod === period.key" :class="{ active: popularPeriod === period.key }" @click="setPopularPeriod(period.key)">{{ period.label }}</button>
            </div>
            <span>좋아요 수 기준이며 동률은 최신 등록순입니다.</span>
          </div>
          <article
            v-for="(item, index) in periodPopularWorks"
            :key="item.id || item.title"
            class="list-panel ranking-row board-popular-board-row"
          >
            <span class="rank-no">{{ index + 1 }}</span>
            <img :src="item.image" :alt="item.title" :data-fallbacks="workImageFallbacks(item.raw)" data-fallback-index="0" @error="handleWorkImageError">
            <div>
              <strong>{{ item.title }}</strong>
              <small v-if="item.youtubeTitle" class="board-youtube-title">{{ item.youtubeTitle }}</small>
              <p>{{ item.tag }}</p>
              <div class="subline">
                <span>{{ item.author }}</span>
                <span v-if="item.duration">{{ item.duration }}</span>
                <span class="board-stat">
                  <span class="board-stat-icon like" :class="{ 'is-liked': item.liked }" aria-hidden="true">{{ item.liked ? '♥' : '♡' }}</span>
                  {{ item.likes }}
                </span>
              </div>
            </div>
            <button v-if="item.postId" class="ghost-button" type="button" @click="openRankedPost(item.postId)">보기</button>
          </article>
          <p v-if="!loading && periodPopularWorks.length === 0" class="muted">조건에 맞는 인기 작업물이 없습니다.</p>
        </section>

        <section class="board-popular-profiles-panel">
          <h2>인기 프로필</h2>
          <article v-for="(profile, index) in popularProfiles" :key="profile.profileId" class="list-panel board-popular-profile-card">
            <span class="rank-no">{{ index + 1 }}</span>
            <button class="board-profile-link" type="button" @click="openPublicProfile(profile.profileId)">
              <img class="board-profile-avatar-image" :src="profileImage(profile)" :alt="`${profile.displayName} 프로필`" @error="handleProfileImageError(profile.profileId)">
              <span><strong>{{ profile.displayName }}</strong><small>{{ profile.primaryRoleName || profile.shortIntro || '역할 정보 없음' }}</small></span>
            </button>
            <span>팔로워 {{ profile.followerCount ?? 0 }}</span>
            <button
              v-if="Number(profile.userId) !== Number(props.currentUser?.userId)"
              class="board-profile-follow"
              :class="{ active: profile.followingByCurrentUser }"
              type="button"
              :aria-label="profile.followingByCurrentUser ? `${profile.displayName} 팔로우 취소` : `${profile.displayName} 팔로우`"
              :aria-pressed="Boolean(profile.followingByCurrentUser)"
              :disabled="followSavingProfileId === profile.profileId"
              @click="toggleProfileFollow(profile)"
            >♥</button>
          </article>
          <p v-if="!loading && popularProfiles.length === 0" class="muted">인기 프로필이 없습니다.</p>
        </section>
      </div>
    </section>
  </section>
</template>
