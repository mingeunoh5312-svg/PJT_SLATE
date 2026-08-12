// Static mock data for Slate design mockups. Not wired to any backend.

export type Role =
  | "감독"
  | "조감독"
  | "촬영감독"
  | "촬영"
  | "조명"
  | "동시녹음"
  | "사운드 디자이너"
  | "프로듀서"
  | "편집"
  | "색보정"
  | "VFX"
  | "미술"
  | "분장"
  | "의상"
  | "배우"
  | "스크립터"
  | "마케팅";

export const roles: Role[] = [
  "감독",
  "촬영감독",
  "조명",
  "동시녹음",
  "프로듀서",
  "편집",
  "색보정",
  "VFX",
  "미술",
  "배우",
];

export const genres = ["드라마", "스릴러", "다큐", "단편", "뮤직비디오", "광고", "공포", "코미디", "SF"];
export const regions = ["서울", "경기", "인천", "부산", "대구", "광주", "제주", "강원"];

export const profiles = [
  {
    id: "p-yoon",
    name: "윤하정",
    handle: "@hajeong",
    role: "촬영감독" as Role,
    region: "서울",
    years: 6,
    tags: ["ARRI", "아나모픽", "야간"],
    bio: "단편·뮤직비디오 위주. 자연광과 차가운 색온도를 좋아합니다.",
    works: 17,
    followers: 1284,
    score: 92,
    available: "2026.07 — 2026.10",
    reason: "장르(스릴러), 지역(서울), 야간 촬영 경험 일치",
  },
  {
    id: "p-kim",
    name: "김도윤",
    handle: "@doyoonk",
    role: "동시녹음" as Role,
    region: "경기",
    years: 4,
    tags: ["Sennheiser", "야외", "다큐"],
    bio: "야외 다큐 5년차. 험지 현장 환영.",
    works: 9,
    followers: 412,
    score: 87,
    available: "2026.08 부터",
    reason: "야외 다큐 경험, 일정 일치",
  },
  {
    id: "p-lee",
    name: "이서원",
    handle: "@seowonl",
    role: "편집" as Role,
    region: "서울",
    years: 8,
    tags: ["DaVinci", "Premiere", "뮤직비디오"],
    bio: "리듬감 있는 컷. 단편·뮤직비디오 30편+.",
    works: 31,
    followers: 2104,
    score: 90,
    available: "상시",
    reason: "포트폴리오 장르 적합도 상위",
  },
  {
    id: "p-park",
    name: "박지운",
    handle: "@jiwoonp",
    role: "프로듀서" as Role,
    region: "서울",
    years: 10,
    tags: ["장편", "지원사업", "공모"],
    bio: "독립 장편 프로듀싱. 지원사업·공모 경험 다수.",
    works: 12,
    followers: 980,
    score: 84,
    available: "프리프로덕션 한정",
    reason: "팀 단계(프리프로덕션) 일치",
  },
  {
    id: "p-han",
    name: "한미르",
    handle: "@mirh",
    role: "색보정" as Role,
    region: "서울",
    years: 5,
    tags: ["DaVinci", "필름룩"],
    bio: "필름룩·시네마틱 그레이딩.",
    works: 22,
    followers: 1530,
    score: 88,
    available: "2026.09 부터",
    reason: "톤 레퍼런스 일치",
  },
  {
    id: "p-cho",
    name: "조유나",
    handle: "@yunac",
    role: "배우" as Role,
    region: "부산",
    years: 3,
    tags: ["단편", "독립영화"],
    bio: "독립영화·단편 위주 활동. 사투리 가능.",
    works: 7,
    followers: 640,
    score: 81,
    available: "주말",
    reason: "캐스팅 노트와 외형 일치",
  },
];

export const teams = [
  {
    id: "t-blueroom",
    name: "블루룸 픽처스",
    title: "단편영화 〈푸른 방〉 제작팀",
    cover: "blueroom",
    stage: "프리프로덕션",
    genre: "드라마",
    region: "서울",
    period: "2026.07 — 2026.10",
    capacity: 12,
    members: 7,
    open: 3,
    leader: "박지운",
    summary:
      "9분 단편. 한 여성이 텅 빈 아파트에서 마주하는 기억에 대한 이야기. 16mm 필름룩 추구.",
    slots: [
      { role: "촬영감독", need: 1, deadline: "07/02", note: "야간 촬영 다수" },
      { role: "동시녹음", need: 1, deadline: "07/05", note: "주말 가능자" },
      { role: "미술", need: 1, deadline: "07/10", note: "1990s 인테리어" },
    ],
  },
  {
    id: "t-nightowl",
    name: "나이트아울",
    title: "뮤직비디오 〈Owl〉",
    cover: "nightowl",
    stage: "촬영 준비",
    genre: "뮤직비디오",
    region: "서울 · 경기",
    period: "2026.07.18 — 07.22",
    capacity: 9,
    members: 6,
    open: 2,
    leader: "윤하정",
    summary: "단일 곡 뮤직비디오. 도심 야경 + 옥상 시퀀스.",
    slots: [
      { role: "조명", need: 2, deadline: "07/08", note: "야간 5일" },
    ],
  },
  {
    id: "t-doc",
    name: "오프시즌 다큐",
    title: "다큐 〈오프시즌〉",
    cover: "doc",
    stage: "촬영 중",
    genre: "다큐",
    region: "강원 · 경기",
    period: "2026.05 — 2026.12",
    capacity: 6,
    members: 5,
    open: 1,
    leader: "김도윤",
    summary: "비시즌 스키장 노동자들의 삶을 따라가는 장편 다큐.",
    slots: [{ role: "편집", need: 1, deadline: "상시", note: "원격 협업 가능" }],
  },
  {
    id: "t-ad",
    name: "스튜디오 스파크",
    title: "브랜드 광고 〈Re:Light〉",
    cover: "ad",
    stage: "기획",
    genre: "광고",
    region: "서울",
    period: "2026.08.01 — 08.10",
    capacity: 10,
    members: 4,
    open: 4,
    leader: "이서원",
    summary: "조명 브랜드 30초 광고 3종. 스튜디오 + 외부 로케 1.",
    slots: [
      { role: "VFX", need: 1, deadline: "07/15", note: "Houdini 우대" },
      { role: "프로듀서", need: 1, deadline: "07/12", note: "" },
    ],
  },
];

export const works = [
  {
    id: "w-1",
    title: "산책",
    by: "이서원",
    role: "감독·편집",
    kind: "단편",
    minutes: 8,
    year: 2025,
    likes: 412,
    reviews: 38,
    badge: "이주의 작업물",
  },
  {
    id: "w-2",
    title: "Owl (Official MV)",
    by: "윤하정",
    role: "촬영감독",
    kind: "뮤직비디오",
    minutes: 4,
    year: 2025,
    likes: 1820,
    reviews: 96,
    badge: null,
  },
  {
    id: "w-3",
    title: "오프시즌 — 티저",
    by: "김도윤",
    role: "동시녹음",
    kind: "다큐 티저",
    minutes: 2,
    year: 2026,
    likes: 230,
    reviews: 14,
    badge: null,
  },
  {
    id: "w-4",
    title: "푸른 방 (스틸 컷)",
    by: "박지운",
    role: "프로듀서",
    kind: "프로덕션 스틸",
    minutes: 0,
    year: 2026,
    likes: 178,
    reviews: 9,
    badge: "신규",
  },
];

export const contests = [
  {
    id: "c-1",
    title: "서울독립영화제 단편 부문",
    host: "서울독립영화제",
    prize: "총 4,000만원",
    deadline: "2026.08.20",
    dleft: 24,
    region: "전국",
    tags: ["단편", "독립"],
    saved: true,
    fit: 86,
    status: "접수 중",
  },
  {
    id: "c-2",
    title: "MISE-EN-SCÈNE 단편영화제",
    host: "미장센",
    prize: "총 3,500만원",
    deadline: "2026.09.15",
    dleft: 50,
    region: "전국",
    tags: ["단편", "장르"],
    saved: false,
    fit: 74,
    status: "접수 중",
  },
  {
    id: "c-3",
    title: "부산국제광고제 영상 부문",
    host: "AD STARS",
    prize: "Grand Prix",
    deadline: "2026.07.05",
    dleft: 9,
    region: "전국",
    tags: ["광고", "영상"],
    saved: true,
    fit: 62,
    status: "마감 임박",
  },
  {
    id: "c-4",
    title: "강원도 로케이션 다큐 공모",
    host: "강원영상위원회",
    prize: "제작비 2,000만원",
    deadline: "2026.06.30",
    dleft: -2,
    region: "강원",
    tags: ["다큐", "지역"],
    saved: false,
    fit: 91,
    status: "종료",
  },
];

export const locations = [
  {
    id: "loc-1",
    name: "을지로 4가 인쇄골목",
    region: "서울 중구",
    reason: "낡은 간판과 좁은 골목, 야간 네온이 풍부합니다.",
    caution: "야간 소음 민원 가능, 사전 협의 필요",
    saved: "팀",
  },
  {
    id: "loc-2",
    name: "양양 죽도 해변",
    region: "강원 양양",
    reason: "오프시즌 비어 있는 해변 풍경, 다큐 정서 일치",
    caution: "11월~3월 강풍 주의",
    saved: "팀",
  },
  {
    id: "loc-3",
    name: "수원 화서문 일대",
    region: "경기 수원",
    reason: "성벽 야경, 매직아워 광량이 좋습니다.",
    caution: "문화재 구역 — 촬영 허가 필요",
    saved: "개인",
  },
];

export const notifications = [
  { id: 1, kind: "지원", text: "〈푸른 방〉 팀이 지원을 수락했습니다.", time: "방금" },
  { id: 2, kind: "초대", text: "나이트아울 팀에서 조명 역할로 초대했습니다.", time: "1시간 전" },
  { id: 3, kind: "공모전", text: "저장한 공모전 ‘서울독립영화제’ 마감 24일 전.", time: "오늘" },
  { id: 4, kind: "리뷰", text: "‘산책’에 새 리뷰 3개가 달렸습니다.", time: "어제" },
];
