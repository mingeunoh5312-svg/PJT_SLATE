const fs = require('fs')
const https = require('https')
const path = require('path')
const { TextDecoder } = require('util')

const ROOT = path.resolve(__dirname, '..')
const SHAPE_DIR = path.join(ROOT, 'assets', 'vworld', 'LT_C_ADSIGG_INFO', '국가기본도_시군구구역경계')
const SHP_PATH = path.join(SHAPE_DIR, 'TN_SIGNGU_BNDRY.shp')
const DBF_PATH = path.join(SHAPE_DIR, 'TN_SIGNGU_BNDRY.dbf')
const OUTPUT_PATH = path.join(ROOT, 'sql', '27_seed_korea_regions.sql')

const FALLBACK_COORDS = {
  '2812500000': { lat: 37.4652463, lng: 126.6064148, source: 'OSM proposed boundary: 제물포구' },
  '2815500000': { lat: 37.5043470, lng: 126.5389436, source: 'OSM administrative district: 영종동 representative for 영종구' },
  '2827500000': { lat: 37.5450000, lng: 126.6760000, source: 'OSM administrative boundary: 인천 서구 representative for 서해구' },
  '2829000000': { lat: 37.5972286, lng: 126.6601317, source: 'OSM proposed boundary: 검단구' },
  '4159100000': { lat: 37.1542798, lng: 126.7328625, source: 'OSM administrative district: 만세구' },
  '4159300000': { lat: 37.2078296, lng: 126.9237243, source: 'OSM administrative district: 효행구' },
  '4159500000': { lat: 37.2135809, lng: 127.0184489, source: 'OSM administrative district: 병점구' },
  '4159700000': { lat: 37.1972554, lng: 127.0971925, source: 'OSM government office: 동탄구청 representative for 동탄구' }
}

function get(url) {
  return new Promise((resolve, reject) => {
    const request = https.request(url, { headers: { 'User-Agent': 'Slate region seed builder' } }, (response) => {
      let data = ''
      response.setEncoding('utf8')
      response.on('data', (chunk) => {
        data += chunk
      })
      response.on('end', () => {
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error(`HTTP ${response.statusCode} for ${url}`))
          return
        }
        resolve(data)
      })
    })
    request.on('error', reject)
    request.setTimeout(30000, () => request.destroy(new Error(`Timeout for ${url}`)))
    request.end()
  })
}

function sql(value) {
  return `'${String(value).replaceAll("'", "''")}'`
}

function num(value) {
  return Number(value).toFixed(7)
}

function extractCodeRows(html) {
  const rows = []
  const regex = /<td class="table_left">\s*(\d{10})\s*<\/td>\s*<td class="table_center01">\s*([^<]+?)\s*<\/td>/g
  let match
  while ((match = regex.exec(html))) {
    rows.push({
      code: match[1],
      name: match[2].replace(/&nbsp;/g, ' ').trim()
    })
  }
  return rows
}

function currentLeafSigungu(rows) {
  const sigungu = rows.filter((row) => (
    row.code.slice(0, 2) !== '12'
    && row.code.slice(2, 5) !== '000'
    && row.code.slice(5) === '00000'
  ))
  return sigungu.filter((row) => !sigungu.some((candidate) => (
    candidate !== row && candidate.name.startsWith(`${row.name} `)
  )))
}

function readDbf(filePath) {
  const buffer = fs.readFileSync(filePath)
  const decoder = new TextDecoder('euc-kr')
  const recordCount = buffer.readUInt32LE(4)
  const headerLength = buffer.readUInt16LE(8)
  const recordLength = buffer.readUInt16LE(10)
  const fields = []
  let recordOffset = 1

  for (let offset = 32; offset < headerLength - 1; offset += 32) {
    if (buffer[offset] === 0x0d) break
    const rawName = buffer.slice(offset, offset + 11)
    const zeroIndex = rawName.indexOf(0)
    const name = rawName.slice(0, zeroIndex >= 0 ? zeroIndex : 11).toString('ascii')
    const length = buffer[offset + 16]
    fields.push({ name, length, offset: recordOffset })
    recordOffset += length
  }

  const rows = []
  for (let index = 0; index < recordCount; index++) {
    const base = headerLength + index * recordLength
    if (buffer[base] === 0x2a) {
      rows.push(null)
      continue
    }
    const row = {}
    for (const field of fields) {
      const raw = buffer.slice(base + field.offset, base + field.offset + field.length)
      row[field.name] = decoder.decode(raw).trim()
    }
    rows.push(row)
  }
  return rows
}

function ringCentroid(points, start, end) {
  let twiceArea = 0
  let cx = 0
  let cy = 0
  for (let index = start; index < end; index++) {
    const current = points[index]
    const next = points[index + 1 < end ? index + 1 : start]
    const cross = current.x * next.y - next.x * current.y
    twiceArea += cross
    cx += (current.x + next.x) * cross
    cy += (current.y + next.y) * cross
  }
  if (Math.abs(twiceArea) < 1e-9) return null
  return {
    area: Math.abs(twiceArea / 2),
    x: cx / (3 * twiceArea),
    y: cy / (3 * twiceArea)
  }
}

function readLargestPartsByCode(shpPath, dbfRows) {
  const buffer = fs.readFileSync(shpPath)
  const byCode = new Map()
  let offset = 100
  let index = 0

  while (offset < buffer.length && index < dbfRows.length) {
    const contentLength = buffer.readInt32BE(offset + 4) * 2
    const contentOffset = offset + 8
    const shapeType = buffer.readInt32LE(contentOffset)
    const row = dbfRows[index]

    if (shapeType === 5 && row?.LEGLCD_SE) {
      const numParts = buffer.readInt32LE(contentOffset + 36)
      const numPoints = buffer.readInt32LE(contentOffset + 40)
      const parts = []
      for (let partIndex = 0; partIndex < numParts; partIndex++) {
        parts.push(buffer.readInt32LE(contentOffset + 44 + partIndex * 4))
      }
      const pointOffset = contentOffset + 44 + numParts * 4
      const points = []
      for (let pointIndex = 0; pointIndex < numPoints; pointIndex++) {
        points.push({
          x: buffer.readDoubleLE(pointOffset + pointIndex * 16),
          y: buffer.readDoubleLE(pointOffset + pointIndex * 16 + 8)
        })
      }
      for (let partIndex = 0; partIndex < parts.length; partIndex++) {
        const start = parts[partIndex]
        const end = partIndex + 1 < parts.length ? parts[partIndex + 1] : points.length
        const centroid = ringCentroid(points, start, end)
        if (!centroid) continue
        const previous = byCode.get(row.LEGLCD_SE)
        if (!previous || centroid.area > previous.area) {
          byCode.set(row.LEGLCD_SE, {
            ...centroid,
            name: row.ADZONE_NM,
            updatedAt: row.OBCHG_DT
          })
        }
      }
    }

    offset += 8 + contentLength
    index += 1
  }

  return byCode
}

function meridionalArc(latitude, a, e2) {
  const e4 = e2 * e2
  const e6 = e4 * e2
  return a * (
    (1 - e2 / 4 - 3 * e4 / 64 - 5 * e6 / 256) * latitude
    - (3 * e2 / 8 + 3 * e4 / 32 + 45 * e6 / 1024) * Math.sin(2 * latitude)
    + (15 * e4 / 256 + 45 * e6 / 1024) * Math.sin(4 * latitude)
    - (35 * e6 / 3072) * Math.sin(6 * latitude)
  )
}

function inverseKoreaUnified(x, y) {
  const a = 6378137.0
  const f = 1 / 298.257222101
  const e2 = 2 * f - f * f
  const ep2 = e2 / (1 - e2)
  const k0 = 0.9996
  const x0 = 1000000.0
  const y0 = 2000000.0
  const lat0 = 38 * Math.PI / 180
  const lon0 = 127.5 * Math.PI / 180
  const m0 = meridionalArc(lat0, a, e2)
  const m = m0 + (y - y0) / k0
  const mu = m / (a * (1 - e2 / 4 - 3 * e2 * e2 / 64 - 5 * e2 * e2 * e2 / 256))
  const e1 = (1 - Math.sqrt(1 - e2)) / (1 + Math.sqrt(1 - e2))
  const phi1 = mu
    + (3 * e1 / 2 - 27 * e1 ** 3 / 32) * Math.sin(2 * mu)
    + (21 * e1 ** 2 / 16 - 55 * e1 ** 4 / 32) * Math.sin(4 * mu)
    + (151 * e1 ** 3 / 96) * Math.sin(6 * mu)
    + (1097 * e1 ** 4 / 512) * Math.sin(8 * mu)
  const sinPhi1 = Math.sin(phi1)
  const cosPhi1 = Math.cos(phi1)
  const tanPhi1 = Math.tan(phi1)
  const c1 = ep2 * cosPhi1 ** 2
  const t1 = tanPhi1 ** 2
  const n1 = a / Math.sqrt(1 - e2 * sinPhi1 ** 2)
  const r1 = n1 * (1 - e2) / (1 - e2 * sinPhi1 ** 2)
  const d = (x - x0) / (n1 * k0)
  const latitude = phi1 - (n1 * tanPhi1 / r1) * (
    d ** 2 / 2
    - (5 + 3 * t1 + 10 * c1 - 4 * c1 ** 2 - 9 * ep2) * d ** 4 / 24
    + (61 + 90 * t1 + 298 * c1 + 45 * t1 ** 2 - 252 * ep2 - 3 * c1 ** 2) * d ** 6 / 720
  )
  const longitude = lon0 + (
    d
    - (1 + 2 * t1 + c1) * d ** 3 / 6
    + (5 - 2 * c1 + 28 * t1 - 3 * c1 ** 2 + 8 * ep2 + 24 * t1 ** 2) * d ** 5 / 120
  ) / cosPhi1
  return {
    lat: latitude * 180 / Math.PI,
    lng: longitude * 180 / Math.PI
  }
}

function splitRegionName(name) {
  const parts = name.split(/\s+/)
  const sidoName = parts[0]
  const sigunguName = parts.slice(1).join(' ') || sidoName
  return { sidoName, sigunguName }
}

async function main() {
  const html = await get('https://www.code.go.kr/stdcode/regCodeL.do?cPage=1&pageSize=30000')
  const currentRegions = currentLeafSigungu(extractCodeRows(html))
  const dbfRows = readDbf(DBF_PATH)
  const shapesByCode = readLargestPartsByCode(SHP_PATH, dbfRows)

  const missing = []
  const rows = currentRegions.map((region) => {
    const shape = shapesByCode.get(region.code)
    let coordinates
    let coordSource
    if (shape) {
      coordinates = inverseKoreaUnified(shape.x, shape.y)
      coordSource = `VWorld TN_SIGNGU_BNDRY ${shape.updatedAt || ''}`.trim()
    } else {
      const fallback = FALLBACK_COORDS[region.code]
      if (!fallback) {
        missing.push(region)
        return null
      }
      coordinates = fallback
      coordSource = fallback.source
    }
    const { sidoName, sigunguName } = splitRegionName(region.name)
    return {
      code: region.code,
      sidoName,
      sigunguName,
      dongName: '',
      lat: coordinates.lat,
      lng: coordinates.lng,
      publicDisplayName: region.name,
      coordSource
    }
  }).filter(Boolean)

  if (missing.length > 0) {
    throw new Error(`Missing coordinates: ${JSON.stringify(missing)}`)
  }

  const generatedAt = new Date().toISOString()
  const fallbackRows = rows.filter((row) => !shapesByCode.has(row.code))
  const values = rows.map((row) => (
    `(${sql(row.code)}, ${sql(row.sidoName)}, ${sql(row.sigunguName)}, ${sql(row.dongName)}, ${num(row.lat)}, ${num(row.lng)}, ${sql(row.publicDisplayName)})`
  )).join(',\n')

  const content = `-- Korea nationwide region seed for Slate.\n`
    + `-- Generated at: ${generatedAt}\n`
    + `-- Region code source: 행정표준코드관리시스템 법정동코드 목록(https://www.code.go.kr/stdcode/regCodeL.do), current leaf 시군구 ${rows.length}건.\n`
    + `-- Coordinate source: VWorld 국가기본도_시군구구역경계 SHP(TN_SIGNGU_BNDRY) largest-polygon centroid transformed from Korea 2000 Unified Coordinate System to WGS84.\n`
    + `-- Fallback coordinates for SHP-missing current codes: ${fallbackRows.map((row) => `${row.code} ${row.publicDisplayName} (${row.coordSource})`).join('; ') || 'none'}.\n`
    + `-- Apply once after 01_schema.sql and 02_seed_reference.sql. Existing sample/dummy rows with the same public_display_name are remapped to the new region rows and deleted.\n\n`
    + `INSERT INTO common_code (code_group, code, display_name, description, sort_order)\n`
    + `VALUES ('ADMIN_PERMISSION', 'REGION_MANAGE', '지역 DB 관리', NULL, 10)\n`
    + `ON DUPLICATE KEY UPDATE\n`
    + `  display_name = VALUES(display_name),\n`
    + `  description = VALUES(description),\n`
    + `  sort_order = VALUES(sort_order),\n`
    + `  active_yn = 'Y';\n\n`
    + `INSERT INTO admin_permission (user_id, permission_code, active_yn, granted_by)\n`
    + `SELECT user_id, 'REGION_MANAGE', 'Y', user_id\n`
    + `FROM user_account\n`
    + `WHERE account_type = 'ADMIN'\n`
    + `ON DUPLICATE KEY UPDATE\n`
    + `  active_yn = VALUES(active_yn),\n`
    + `  granted_by = VALUES(granted_by);\n\n`
    + `DROP TEMPORARY TABLE IF EXISTS tmp_korea_region_seed;\n`
    + `CREATE TEMPORARY TABLE tmp_korea_region_seed (\n`
    + `  region_code varchar(20) NOT NULL,\n`
    + `  sido_name varchar(50) NOT NULL,\n`
    + `  sigungu_name varchar(80) NOT NULL,\n`
    + `  dong_name varchar(80) NOT NULL,\n`
    + `  center_lat decimal(10,7) NOT NULL,\n`
    + `  center_lng decimal(10,7) NOT NULL,\n`
    + `  public_display_name varchar(150) NOT NULL,\n`
    + `  PRIMARY KEY (region_code),\n`
    + `  UNIQUE KEY uk_tmp_korea_region_display (public_display_name)\n`
    + `) ENGINE=Memory;\n\n`
    + `INSERT INTO tmp_korea_region_seed\n`
    + `  (region_code, sido_name, sigungu_name, dong_name, center_lat, center_lng, public_display_name)\n`
    + `VALUES\n${values};\n\n`
    + `INSERT INTO region (region_code, sido_name, sigungu_name, dong_name, center_lat, center_lng, public_display_name, active_yn)\n`
    + `SELECT s.region_code, s.sido_name, s.sigungu_name, s.dong_name, s.center_lat, s.center_lng, s.public_display_name, 'Y'\n`
    + `FROM tmp_korea_region_seed s\n`
    + `ON DUPLICATE KEY UPDATE\n`
    + `  sido_name = VALUES(sido_name),\n`
    + `  sigungu_name = VALUES(sigungu_name),\n`
    + `  dong_name = VALUES(dong_name),\n`
    + `  center_lat = VALUES(center_lat),\n`
    + `  center_lng = VALUES(center_lng),\n`
    + `  public_display_name = VALUES(public_display_name),\n`
    + `  active_yn = 'Y';\n\n`
    + `UPDATE member_profile mp\n`
    + `JOIN region old_region ON old_region.region_id = mp.region_id\n`
    + `JOIN tmp_korea_region_seed s ON s.public_display_name = old_region.public_display_name\n`
    + `JOIN region new_region ON new_region.region_code = s.region_code\n`
    + `SET mp.region_id = new_region.region_id\n`
    + `WHERE old_region.region_code <> s.region_code;\n\n`
    + `UPDATE team t\n`
    + `JOIN region old_region ON old_region.region_id = t.region_id\n`
    + `JOIN tmp_korea_region_seed s ON s.public_display_name = old_region.public_display_name\n`
    + `JOIN region new_region ON new_region.region_code = s.region_code\n`
    + `SET t.region_id = new_region.region_id\n`
    + `WHERE old_region.region_code <> s.region_code;\n\n`
    + `DELETE old_region\n`
    + `FROM region old_region\n`
    + `JOIN tmp_korea_region_seed s ON s.public_display_name = old_region.public_display_name\n`
    + `JOIN region new_region ON new_region.region_code = s.region_code\n`
    + `WHERE old_region.region_id <> new_region.region_id\n`
    + `  AND old_region.region_code <> s.region_code;\n\n`
    + `SELECT COUNT(*) AS active_region_count\n`
    + `FROM region\n`
    + `WHERE active_yn = 'Y';\n`

  fs.writeFileSync(OUTPUT_PATH, content, 'utf8')
  console.log(`Wrote ${path.relative(ROOT, OUTPUT_PATH)} with ${rows.length} regions.`)
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
