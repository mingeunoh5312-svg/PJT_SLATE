export const contestTargetOptions = [
  ['ANYONE', '누구나'], ['PRESCHOOL', '유치원'], ['ELEMENTARY', '초등학생'], ['MIDDLE', '중학생'],
  ['HIGH', '고등학생'], ['UNIVERSITY', '대학생'], ['GRADUATE', '대학원생'], ['ADULT', '일반인'],
  ['FOREIGNER', '외국인'], ['ELIGIBLE_ONLY', '해당자']
].map(([value, label]) => ({ value, label }))

export const contestRegionOptions = [
  ['ALL', '전체'], ['ONLINE', '온라인'], ['CAPITAL', '수도권'], ['NATIONWIDE', '전국'],
  ['SEOUL', '서울'], ['INCHEON', '인천'], ['DAEJEON', '대전'], ['GWANGJU', '광주'],
  ['DAEGU', '대구'], ['BUSAN', '부산'], ['ULSAN', '울산'], ['SEJONG', '세종'],
  ['GYEONGGI', '경기'], ['GANGWON', '강원'], ['CHUNGNAM', '충남'], ['CHUNGBUK', '충북'],
  ['JEONNAM', '전남'], ['JEONBUK', '전북'], ['GYEONGNAM', '경남'], ['GYEONGBUK', '경북'],
  ['JEJU', '제주'], ['OVERSEAS', '해외'], ['OTHER', '기타']
].map(([value, label]) => ({ value, label }))

export const contestListRegionOptions = [
  ['', '전체'], ['ONLINE', '온라인'], ['NATIONWIDE', '전국'], ['CAPITAL', '수도권'], ['REGIONAL', '지역별']
].map(([value, label]) => ({ value, label }))

export const contestDeadlineOptions = [
  ['', '전체'], ['7', '7일 이내'], ['14', '14일 이내'], ['30', '30일 이내']
].map(([value, label]) => ({ value, label }))

export const contestTypeOptions = [
  ['', '전체'], ['INTERNAL', '자체 공모전'], ['EXTERNAL', '외부 공모전']
].map(([value, label]) => ({ value, label }))

export const contestOrganizerOptions = [
  ['GOVERNMENT_PUBLIC', '정부·지자체·공공기관'], ['MEDIA_PUBLISHER', '신문·방송·언론·출판'],
  ['SCHOOL_ASSOCIATION', '학교·학회·협회·재단'], ['CULTURE_VENUE', '미술관·전시·박물관·공연장'],
  ['COMPANY', '대기업·중소기업·벤처기업'], ['ORGANIZATION', '단체·센터·위원회·연구회'],
  ['CLUB', '동아리·모임'], ['OVERSEAS', '해외'], ['OTHER', '기타']
].map(([value, label]) => ({ value, label }))

export const totalPrizeBands = [
  { value: '', label: '전체', min: null, max: null },
  { value: '5000_PLUS', label: '5천만원 이상', min: 50000000, max: null },
  { value: '3000_5000', label: '3천만원~5천만원', min: 30000000, max: 49999999 },
  { value: '1000_3000', label: '1천만원~3천만원', min: 10000000, max: 29999999 },
  { value: '500_1000', label: '5백만원~1천만원', min: 5000000, max: 9999999 },
  { value: '100_500', label: '1백만원~5백만원', min: 1000000, max: 4999999 },
  { value: '100_UNDER', label: '1백만원 이하', min: 0, max: 999999 }
]

export const firstPrizeBands = [
  { value: '', label: '전체', min: null, max: null },
  { value: '500_PLUS', label: '500만원 이상', min: 5000000, max: null },
  { value: '300_500', label: '300만원~500만원', min: 3000000, max: 4999999 },
  { value: '100_300', label: '100만원~300만원', min: 1000000, max: 2999999 },
  { value: '50_100', label: '50만원~100만원', min: 500000, max: 999999 },
  { value: '20_50', label: '20만원~50만원', min: 200000, max: 499999 },
  { value: '20_UNDER', label: '20만원 이하', min: 0, max: 199999 }
]
