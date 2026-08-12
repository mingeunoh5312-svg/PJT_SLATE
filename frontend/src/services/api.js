const API_BASE = import.meta.env.VITE_API_BASE_URL || ''
const TOKEN_KEY = 'slate.accessToken'
const DEMO_ACCESS_CODE_KEY = 'slate.demoAccessCode'
const DEMO_ACCESS_GATE = import.meta.env.VITE_DEMO_ACCESS_GATE === 'true'

function requestUrl(path) {
  if (/^https?:\/\//i.test(String(path || ''))) return path
  return `${API_BASE}${path}`
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

export function isDemoAccessGateEnabled() {
  return DEMO_ACCESS_GATE
}

export function getDemoAccessCode() {
  return sessionStorage.getItem(DEMO_ACCESS_CODE_KEY)
}

export function setDemoAccessCode(code) {
  if (code) sessionStorage.setItem(DEMO_ACCESS_CODE_KEY, code)
  else sessionStorage.removeItem(DEMO_ACCESS_CODE_KEY)
}

export function isProtectedApiResourceUrl(value) {
  if (!DEMO_ACCESS_GATE || !value) return false
  const source = String(value).trim()
  if (source.startsWith('/api/')) return true
  if (!/^https?:\/\//i.test(source)) return false

  const frontendOrigin = globalThis.location?.origin || 'http://localhost'
  const apiBaseUrl = new URL(API_BASE || '/', frontendOrigin)
  const sourceUrl = new URL(source)
  const basePath = apiBaseUrl.pathname.replace(/\/$/, '')
  const apiPath = `${basePath}/api/`.replace(/\/{2,}/g, '/')
  return sourceUrl.origin === apiBaseUrl.origin
    && (sourceUrl.pathname.startsWith('/api/') || sourceUrl.pathname.startsWith(apiPath))
}

function applyDemoAccessHeader(headers) {
  const code = getDemoAccessCode()
  if (DEMO_ACCESS_GATE && code && !headers.has('X-Slate-Demo-Code')) {
    headers.set('X-Slate-Demo-Code', code)
  }
}

function isDemoAccessFailure(response, payload, path) {
  if (!DEMO_ACCESS_GATE || response.status !== 403 || !getDemoAccessCode()) return false
  if (String(path || '').startsWith('/api/demo/access')) return false
  const message = String(payload?.message || '')
  return message.includes('접속 코드') || message.includes('접근 코드')
}

function handleDemoAccessFailure(response, payload, path) {
  if (!isDemoAccessFailure(response, payload, path)) return
  setDemoAccessCode(null)
  if (typeof globalThis.dispatchEvent !== 'function' || typeof globalThis.CustomEvent !== 'function') return
  globalThis.dispatchEvent(new CustomEvent('slate-demo-access-rejected', {
    detail: { path, message: payload?.message || 'Demo Access code rejected' }
  }))
}

export async function api(path, options = {}) {
  const headers = new Headers(options.headers || {})
  if (!headers.has('Content-Type') && options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  applyDemoAccessHeader(headers)
  const response = await fetch(requestUrl(path), { ...options, headers })
  const payload = await response.json().catch(() => ({ success: false, message: '응답을 읽지 못했습니다.' }))
  if (!response.ok || payload.success === false) {
    handleDemoAccessFailure(response, payload, path)
    throw new Error(payload.message || '요청 처리에 실패했습니다.')
  }
  return payload.data
}

export async function apiBlob(path, options = {}) {
  const headers = new Headers(options.headers || {})
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  applyDemoAccessHeader(headers)
  const response = await fetch(requestUrl(path), { ...options, headers })
  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    handleDemoAccessFailure(response, payload, path)
    throw new Error(payload?.message || '파일 요청 처리에 실패했습니다.')
  }
  return response.blob()
}

function matchingSearchParams(params = {}) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') return
    if (Array.isArray(value)) {
      value.filter((item) => item !== null && item !== undefined && item !== '').forEach((item) => query.append(key, String(item)))
      return
    }
    query.set(key, String(value))
  })
  return query
}

export const slateApi = {
  verifyDemoAccess(code) {
    return api('/api/demo/access', {
      method: 'POST',
      headers: { 'X-Slate-Demo-Code': code },
      body: JSON.stringify({ code })
    })
  },
  login(loginId, password) {
    return api('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ loginId, password })
    })
  },
  register(payload) {
    return api('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  me() {
    return api('/api/auth/me')
  },
  updateMe(payload) {
    return api('/api/auth/me', {
      method: 'PATCH',
      body: JSON.stringify(payload)
    })
  },
  withdrawMe(payload) {
    return api('/api/auth/me', {
      method: 'DELETE',
      body: JSON.stringify(payload)
    })
  },
  roles() {
    return api('/api/references/roles')
  },
  genres() {
    return api('/api/references/genres')
  },
  regions(keyword = '', limit = 50) {
    const query = new URLSearchParams()
    if (keyword) query.set('keyword', keyword)
    query.set('limit', String(limit))
    return api(`/api/references/regions?${query.toString()}`)
  },
  codes(groups = []) {
    const query = groups.length ? `?${groups.map((group) => `groups=${encodeURIComponent(group)}`).join('&')}` : ''
    return api(`/api/references/codes${query}`)
  },
  myProfile() {
    return api('/api/profiles/me')
  },
  profile(profileId) {
    return api(`/api/profiles/${profileId}`)
  },
  publicProfile(profileId) {
    return api(`/api/profiles/public/${profileId}`)
  },
  createProfile(payload) {
    return api('/api/profiles', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  updateProfile(profileId, payload) {
    return api(`/api/profiles/${profileId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  deleteMyProfile() {
    return api('/api/profiles/me', {
      method: 'DELETE'
    })
  },
  uploadEntityImage(entityType, entityId, file) {
    const body = new FormData()
    body.append('file', file)
    return api(`/api/media/images/${String(entityType).toLowerCase()}/${entityId}`, { method: 'POST', body })
  },
  deleteEntityImage(entityType, entityId) {
    return api(`/api/media/images/${String(entityType).toLowerCase()}/${entityId}`, { method: 'DELETE' })
  },
  entityImageBlob(entityType, entityId) {
    return apiBlob(`/api/media/images/${String(entityType).toLowerCase()}/${entityId}`)
  },
  workFileBlob(fileId) {
    return apiBlob(`/api/boards/work-files/${fileId}/stream`)
  },
  followProfile(profileId) {
    return api(`/api/profiles/${profileId}/follow`, { method: 'POST' })
  },
  unfollowProfile(profileId) {
    return api(`/api/profiles/${profileId}/follow`, { method: 'DELETE' })
  },
  followStatus(profileId) {
    return api(`/api/profiles/${profileId}/follow-status`)
  },
  profileFollowers(profileId, { limit = 20, offset = 0 } = {}) {
    const query = new URLSearchParams({ limit: String(limit), offset: String(offset) })
    return api(`/api/profiles/${profileId}/followers?${query.toString()}`)
  },
  profileFollowing(profileId, { limit = 20, offset = 0 } = {}) {
    const query = new URLSearchParams({ limit: String(limit), offset: String(offset) })
    return api(`/api/profiles/${profileId}/following?${query.toString()}`)
  },
  myPortfolioItems() {
    return api('/api/profiles/me/portfolio-items')
  },
  createPortfolioItem(payload) {
    return api('/api/profiles/me/portfolio-items', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  updatePortfolioItem(portfolioItemId, payload) {
    return api(`/api/profiles/me/portfolio-items/${portfolioItemId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  deletePortfolioItem(portfolioItemId) {
    return api(`/api/profiles/me/portfolio-items/${portfolioItemId}`, {
      method: 'DELETE'
    })
  },
  kobisMovieSearch(keyword = '', limit = 10) {
    const query = new URLSearchParams()
    if (keyword) query.set('keyword', keyword)
    query.set('limit', String(limit))
    return api(`/api/profiles/public-data/kobis/movies?${query.toString()}`)
  },
  publicDataSearch(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/profiles/public-data/search?${query.toString()}`)
  },
  createPortfolioFromPublicData(payload) {
    return api('/api/profiles/me/portfolio-items/from-public-data', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  myTeams() {
    return api('/api/teams/mine')
  },
  recommendLocations(payload) {
    return api('/api/locations/ai/recommendations', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  locationRecommendationSession(sessionId) {
    return api(`/api/locations/sessions/${sessionId}`)
  },
  saveLocationCandidate(payload) {
    return api('/api/locations/candidates', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  personalLocationCandidates() {
    return api('/api/locations/candidates')
  },
  teamLocationCandidates(teamId) {
    return api(`/api/teams/${teamId}/locations`)
  },
  createTeam(payload) {
    return api('/api/teams', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  updateTeam(teamId, payload) {
    return api(`/api/teams/${teamId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  deleteTeam(teamId) {
    return api(`/api/teams/${teamId}`, {
      method: 'DELETE'
    })
  },
  recruitments(teamId) {
    return api(`/api/teams/${teamId}/recruitments`)
  },
  createRecruitment(teamId, payload) {
    return api(`/api/teams/${teamId}/recruitments`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  updateRecruitment(recruitmentId, payload) {
    return api(`/api/teams/recruitments/${recruitmentId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  deleteRecruitment(recruitmentId) {
    return api(`/api/teams/recruitments/${recruitmentId}`, {
      method: 'DELETE'
    })
  },
  createSlot(recruitmentId, payload) {
    return api(`/api/teams/recruitments/${recruitmentId}/slots`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  updateSlot(slotId, payload) {
    return api(`/api/teams/recruitment-slots/${slotId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  deleteSlot(slotId) {
    return api(`/api/teams/recruitment-slots/${slotId}`, {
      method: 'DELETE'
    })
  },
  teamApplications(teamId) {
    return api(`/api/teams/${teamId}/applications`)
  },
  decideTeamApplication(applicationId, decision, reason = '') {
    return api(`/api/teams/applications/${applicationId}/decision`, {
      method: 'POST',
      body: JSON.stringify({ decision, reason })
    })
  },
  teamInvitations(teamId) {
    return api(`/api/teams/${teamId}/invitations`)
  },
  myTeamInvitations() {
    return api('/api/teams/invitations/mine')
  },
  decideTeamInvitation(invitationId, decision) {
    return api(`/api/teams/invitations/${invitationId}/decision`, {
      method: 'POST',
      body: JSON.stringify({ decision })
    })
  },
  updateTeamMember(teamId, memberUserId, payload) {
    return api(`/api/teams/${teamId}/members/${memberUserId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  leaveTeam(teamId) {
    return api(`/api/teams/${teamId}/leave`, { method: 'POST' })
  },
  transferTeamLeader(teamId, payload) {
    return api(`/api/teams/${teamId}/transfer-leader`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  closeTeam(teamId, payload) {
    return api(`/api/teams/${teamId}/close`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  teamClosureSnapshots(teamId) {
    return api(`/api/teams/${teamId}/closure-snapshots`)
  },
  reopenTeam(teamId, payload) {
    return api(`/api/teams/${teamId}/reopen`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  teamPlans(teamId) {
    return api(`/api/teams/${teamId}/plans`)
  },
  createTeamPlan(teamId, payload) {
    return api(`/api/teams/${teamId}/plans`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  updateTeamPlan(planItemId, payload) {
    return api(`/api/teams/plans/${planItemId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  updateTeamPlanStatus(planItemId, status) {
    return api(`/api/teams/plans/${planItemId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    })
  },
  teamToMembers(params) {
    return api(`/api/matching/team-to-members?${matchingSearchParams(params).toString()}`)
  },
  memberToTeams(params) {
    return api(`/api/matching/member-to-teams?${matchingSearchParams(params).toString()}`)
  },
  aiMatchingRecommendations(payload) {
    return api('/api/matching/ai/recommendations', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  policy() {
    return api('/api/matching/policies/active')
  },
  bookmark(targetType, targetId) {
    return api('/api/matching/bookmarks', {
      method: 'POST',
      body: JSON.stringify({ targetType, targetId })
    })
  },
  matchingBookmarks(targetType = 'TEAM') {
    return api(`/api/matching/bookmarks?targetType=${encodeURIComponent(targetType)}`)
  },
  deleteMatchingBookmark(targetType, targetId) {
    return api(`/api/matching/bookmarks/${encodeURIComponent(targetType)}/${targetId}`, {
      method: 'DELETE'
    })
  },
  invite(payload) {
    return api('/api/matching/invitations', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  sentInvitations() {
    return api('/api/matching/invitations')
  },
  cancelInvitation(invitationId) {
    return api(`/api/matching/invitations/${invitationId}`, {
      method: 'DELETE'
    })
  },
  apply(payload) {
    return api('/api/matching/applications', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  sentApplications() {
    return api('/api/matching/applications')
  },
  cancelApplication(applicationId) {
    return api(`/api/matching/applications/${applicationId}`, {
      method: 'DELETE'
    })
  },
  companyApplications() {
    return api('/api/admin/company-applications')
  },
  companyDecision(applicationId, decision, reason) {
    return api(`/api/admin/company-applications/${applicationId}/decision`, {
      method: 'POST',
      body: JSON.stringify({ decision, reason })
    })
  },
  uploadCompanyApplicationDocument(applicationId, { businessRegistrationNo, documentType = 'BUSINESS_REGISTRATION', file }) {
    const body = new FormData()
    body.append('businessRegistrationNo', businessRegistrationNo)
    body.append('documentType', documentType)
    body.append('file', file)
    return api(`/api/auth/company-applications/${applicationId}/documents`, {
      method: 'POST',
      body
    })
  },
  myCompanyDocuments() {
    return api('/api/company/application/documents')
  },
  uploadMyCompanyDocument(file, documentType = 'BUSINESS_REGISTRATION') {
    const body = new FormData()
    body.append('documentType', documentType)
    body.append('file', file)
    return api('/api/company/application/documents', {
      method: 'POST',
      body
    })
  },
  deleteMyCompanyDocument(documentId) {
    return api(`/api/company/application/documents/${documentId}`, {
      method: 'DELETE'
    })
  },
  companyApplicationDocuments(applicationId) {
    return api(`/api/admin/company-applications/${applicationId}/documents`)
  },
  downloadCompanyDocument(documentId) {
    return apiBlob(`/api/admin/company-applications/documents/${documentId}/download`)
  },
  boardPosts(category = 'WORK', sort = 'reaction', limit = 20, keyword = '', filters = {}) {
    const query = new URLSearchParams({ category, sort, limit: String(limit) })
    if (keyword.trim()) query.set('keyword', keyword.trim())
    if (filters.freeCategory) query.set('freeCategory', filters.freeCategory)
    if (filters.workType) query.set('workType', filters.workType)
    if (filters.genreId) query.set('genreId', String(filters.genreId))
    return api(`/api/boards/posts?${query.toString()}`)
  },
  myBoardWorks(limit = 100) {
    const query = new URLSearchParams({ limit: String(limit) })
    return api(`/api/boards/posts/my-works?${query.toString()}`)
  },
  boardPost(postId) {
    return api(`/api/boards/posts/${postId}`)
  },
  createBoardPost(payload) {
    return api('/api/boards/posts', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  updateBoardPost(postId, payload) {
    return api(`/api/boards/posts/${postId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  deleteBoardPost(postId) {
    return api(`/api/boards/posts/${postId}`, {
      method: 'DELETE'
    })
  },
  previewYoutubeVideo(youtubeUrl) {
    return api('/api/boards/youtube/preview', {
      method: 'POST',
      body: JSON.stringify({ youtubeUrl })
    })
  },
  createReview(postId, payload) {
    return api(`/api/boards/posts/${postId}/reviews`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  updateReview(reviewId, payload) {
    return api(`/api/boards/reviews/${reviewId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  deleteReview(reviewId) {
    return api(`/api/boards/reviews/${reviewId}`, {
      method: 'DELETE'
    })
  },
  toggleBoardLike(postId) {
    return api(`/api/boards/posts/${postId}/likes/toggle`, {
      method: 'POST'
    })
  },
  uploadWorkFile(file, { teamId = '', clientDurationSeconds = '' } = {}) {
    const body = new FormData()
    body.append('file', file)
    if (teamId) body.append('teamId', String(teamId))
    if (clientDurationSeconds) body.append('clientDurationSeconds', String(clientDurationSeconds))
    return api('/api/boards/work-files', {
      method: 'POST',
      body
    })
  },
  myWorkFiles(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/boards/work-files/mine?${query.toString()}`)
  },
  deleteWorkFile(fileId) {
    return api(`/api/boards/work-files/${fileId}`, {
      method: 'DELETE'
    })
  },
  restoreWorkFile(fileId) {
    return api(`/api/boards/work-files/${fileId}/restore`, {
      method: 'POST'
    })
  },
  myTeamWorkRequests() {
    return api('/api/boards/team-work-requests/mine')
  },
  createTeamWorkRequest(payload) {
    return api('/api/boards/team-work-requests', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  teamWorkRequests(teamId) {
    return api(`/api/boards/teams/${teamId}/work-requests`)
  },
  decideTeamWorkRequest(requestId, payload) {
    return api(`/api/boards/team-work-requests/${requestId}/decision`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  reportBoardPost(postId, payload) {
    return api(`/api/boards/posts/${postId}/reports`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  reportReview(reviewId, payload) {
    return api(`/api/boards/reviews/${reviewId}/reports`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  boardRankings(type = 'POPULAR_WORK', limit = 10, workType = '', genreId = '') {
    const query = new URLSearchParams({ type, limit: String(limit) })
    if (workType) query.set('workType', workType)
    if (genreId) query.set('genreId', String(genreId))
    return api(`/api/boards/rankings?${query.toString()}`)
  },
  contests(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (Array.isArray(value)) {
        value.filter(Boolean).forEach((item) => query.append(key, String(item)))
      } else if (value !== undefined && value !== null && value !== '') {
        query.set(key, String(value))
      }
    })
    return api(`/api/contests?${query.toString()}`)
  },
  urgentContests(limit = 4) {
    return api(`/api/contests/urgent?limit=${encodeURIComponent(limit)}`)
  },
  contest(contestId, params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    const suffix = query.toString() ? `?${query.toString()}` : ''
    return api(`/api/contests/${contestId}${suffix}`)
  },
  contestBases() {
    return api('/api/contests/bases')
  },
  toggleContestSave(contestId) {
    return api(`/api/contests/${contestId}/save/toggle`, {
      method: 'POST'
    })
  },
  calculateContestFit(contestId, payload) {
    return api(`/api/contests/${contestId}/fit`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  saveContestPreparation(contestId, payload) {
    return api(`/api/contests/${contestId}/prepare`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  myContestOpenRequests() {
    return api('/api/contests/open-requests/mine')
  },
  createContestOpenRequest(payload) {
    return api('/api/contests/open-requests', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  myManagedContests() {
    return api('/api/contests/manage/mine')
  },
  updateManagedContest(contestId, payload) {
    return api(`/api/contests/manage/${contestId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  updateManagedContestStatus(contestId, payload) {
    return api(`/api/contests/manage/${contestId}/status`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  notifications(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/notifications?${query.toString()}`)
  },
  unreadNotifications() {
    return api('/api/notifications/unread-count')
  },
  markNotificationRead(notificationId) {
    return api(`/api/notifications/${notificationId}/read`, { method: 'PATCH' })
  },
  markAllNotificationsRead() {
    return api('/api/notifications/read-all', { method: 'PATCH' })
  },
  hideNotification(notificationId) {
    return api(`/api/notifications/${notificationId}/hide`, { method: 'PATCH' })
  },
  adminSendNotification(payload) {
    return api('/api/notifications/admin/send', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminNotificationTemplates() {
    return api('/api/notifications/admin/templates')
  },
  adminNotificationRecipientPreview(payload) {
    return api('/api/notifications/admin/recipients/preview', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminNotificationBatches(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/notifications/admin/batches?${query.toString()}`)
  },
  adminPermissionCatalog() {
    return api('/api/admin/permissions/catalog')
  },
  myAdminPermissions() {
    return api('/api/admin/permissions/me')
  },
  adminPermissionUsers() {
    return api('/api/admin/permissions/users')
  },
  updateAdminPermissions(userId, permissions) {
    return api(`/api/admin/permissions/users/${userId}`, {
      method: 'PUT',
      body: JSON.stringify({ permissions })
    })
  },
  adminDemoAccessCodes() {
    return api('/api/admin/demo-access/codes')
  },
  adminRegions(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/admin/regions?${query.toString()}`)
  },
  adminRegionSummary() {
    return api('/api/admin/regions/summary')
  },
  adminUpdateRegion(regionId, payload) {
    return api(`/api/admin/regions/${regionId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  adminCreateDemoAccessCode(payload) {
    return api('/api/admin/demo-access/codes', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminUpdateDemoAccessCode(codeId, payload) {
    return api(`/api/admin/demo-access/codes/${codeId}`, {
      method: 'PATCH',
      body: JSON.stringify(payload)
    })
  },
  adminRevokeDemoAccessCode(codeId, payload) {
    return api(`/api/admin/demo-access/codes/${codeId}/revoke`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  auditLogs(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/admin/logs/audit?${query.toString()}`)
  },
  operationLogs(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/admin/logs/operations?${query.toString()}`)
  },
  adminActiveScorePolicy() {
    return api('/api/admin/matching/policies/active')
  },
  publishScorePolicy(payload) {
    return api('/api/admin/matching/policies/active', {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  previewScorePolicy(payload) {
    return api('/api/admin/matching/policies/preview', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  rollbackScorePolicy(policyId, payload) {
    return api(`/api/admin/matching/policies/${policyId}/rollback`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  scorePolicyHistory(limit = 20) {
    return api(`/api/admin/matching/policies/history?${new URLSearchParams({ limit: String(limit) }).toString()}`)
  },
  adminCreateContest(payload) {
    return api('/api/admin/contests', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminContests(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/admin/contests?${query.toString()}`)
  },
  adminUpdateContest(contestId, payload) {
    return api(`/api/admin/contests/${contestId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  adminUpdateContestStatus(contestId, payload) {
    return api(`/api/admin/contests/${contestId}/status`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminDeleteContests(payload) {
    return api('/api/admin/contests', {
      method: 'DELETE',
      body: JSON.stringify(payload)
    })
  },
  adminContestRequests(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/admin/contests/requests?${query.toString()}`)
  },
  decideContestRequest(requestId, payload) {
    return api(`/api/admin/contests/requests/${requestId}/decision`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminRunContestKoreaCrawler(payload = {}) {
    return api('/api/admin/contests/crawl-sources/contest-korea/run', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminBoardPosts(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/admin/boards/posts?${query.toString()}`)
  },
  adminBoardPost(postId) {
    return api(`/api/admin/boards/posts/${postId}`)
  },
  adminUpdateBoardPost(postId, payload) {
    return api(`/api/admin/boards/posts/${postId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  adminHideBoardPost(postId, payload) {
    return api(`/api/admin/boards/posts/${postId}/hide`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminRestoreBoardPost(postId, payload) {
    return api(`/api/admin/boards/posts/${postId}/restore`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminDeleteBoardPost(postId, payload) {
    return api(`/api/admin/boards/posts/${postId}`, {
      method: 'DELETE',
      body: JSON.stringify(payload)
    })
  },
  adminTeams(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/admin/teams?${query.toString()}`)
  },
  adminTeam(teamId) {
    return api(`/api/admin/teams/${teamId}`)
  },
  adminUpdateTeam(teamId, payload) {
    return api(`/api/admin/teams/${teamId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  adminHideTeam(teamId, payload) {
    return api(`/api/admin/teams/${teamId}/hide`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminCloseTeam(teamId, payload) {
    return api(`/api/admin/teams/${teamId}/close`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminRestoreTeam(teamId, payload) {
    return api(`/api/admin/teams/${teamId}/restore`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminDeleteTeam(teamId, payload) {
    return api(`/api/admin/teams/${teamId}`, {
      method: 'DELETE',
      body: JSON.stringify(payload)
    })
  },
  adminUsers(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/admin/users?${query.toString()}`)
  },
  adminUser(userId) {
    return api(`/api/admin/users/${userId}`)
  },
  adminUpdateUser(userId, payload) {
    return api(`/api/admin/users/${userId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    })
  },
  adminDeactivateUser(userId, payload) {
    return api(`/api/admin/users/${userId}/deactivate`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminRestoreUser(userId, payload) {
    return api(`/api/admin/users/${userId}/restore`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  contentReports(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/admin/moderation/reports?${query.toString()}`)
  },
  decideContentReport(reportId, payload) {
    return api(`/api/admin/moderation/reports/${reportId}/decision`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminWorkFiles(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/admin/work-files?${query.toString()}`)
  },
  adminWorkFileStorageSummary() {
    return api('/api/admin/work-files/storage-summary')
  },
  adminHoldWorkFile(fileId, payload) {
    return api(`/api/admin/work-files/${fileId}/hold`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminRestoreWorkFile(fileId, payload) {
    return api(`/api/admin/work-files/${fileId}/restore`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  adminDeleteWorkFile(fileId, payload) {
    return api(`/api/admin/work-files/${fileId}`, {
      method: 'DELETE',
      body: JSON.stringify(payload)
    })
  },
  moderationUsers(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/admin/moderation/users?${query.toString()}`)
  },
  userSanctions(params = {}) {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
    })
    return api(`/api/admin/moderation/sanctions?${query.toString()}`)
  },
  createUserSanction(userId, payload) {
    return api(`/api/admin/moderation/users/${userId}/sanctions`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  revokeUserSanction(sanctionId, reason) {
    return api(`/api/admin/moderation/sanctions/${sanctionId}/revoke`, {
      method: 'POST',
      body: JSON.stringify({ reason })
    })
  }
}
