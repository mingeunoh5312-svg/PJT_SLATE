package com.slate.profiles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KobisRoleMatcher {

    private static final String VERIFIED = "VERIFIED";
    private static final String NOT_VERIFIED = "NOT_VERIFIED";
    private static final String AMBIGUOUS = "AMBIGUOUS";

    private final Map<String, String> roleGroups = new LinkedHashMap<>();

    public KobisRoleMatcher() {
        register("감독/연출", "감독", "조감독", "연출팀", "스크립터", "스토리보드", "각색", "시나리오", "각본");
        register("배우/출연", "배우", "출연", "조연", "단역");
        register("촬영", "촬영", "촬영팀", "카메라오퍼레이터", "스테디캠", "그립", "지미집", "촬영장비");
        register("조명", "조명", "조명팀", "발전차", "조명장비");
        register("미술/소품", "미술", "프로덕션 디자인", "미술/프로덕션 디자인", "아트디렉터", "미술팀", "세트", "세트팀", "세트미술팀", "소품", "소품팀");
        register("음향", "동시녹음", "붐오퍼레이터", "케이블맨", "사운드", "사운드믹싱", "사운드효과", "폴리", "사운드팀", "대사");
        register("의상/분장", "의상", "의상팀", "의상디자이너", "분장", "분장팀", "헤어", "특수분장");
        register("편집/후반", "편집", "편집팀", "현장편집", "DI팀", "색보정", "현상팀", "아날로그색보정");
        register("VFX", "VFX", "VFX 아티스트", "VFX 슈퍼바이저", "VFX 매니지먼트", "시각효과", "특수효과", "특수효과팀");
        register("음악", "음악", "음악진행", "작사", "작곡", "편곡", "음악 믹싱", "음악 레코딩");
        register("제작", "제작", "프로듀서", "라인프로듀서", "제작팀", "제작관리", "기획", "투자");
        register("홍보/마케팅", "홍보/마케팅 진행", "온라인마케팅", "광고디자인", "광고대행", "포스터사진", "스틸", "메이킹필름", "예고편");
    }

    public KobisVerificationMatch match(KobisMovieDetail detail, String creditName, String requestedRoleName) {
        String cleanCreditName = clean(creditName);
        String cleanRequestedRole = clean(requestedRoleName);
        if (detail == null || cleanCreditName == null || cleanRequestedRole == null) {
            return new KobisVerificationMatch(NOT_VERIFIED, null, null, null, null, null);
        }

        KobisVerificationMatch ambiguous = null;
        for (KobisCredit credit : allCredits(detail)) {
            if (!cleanCreditName.equals(clean(credit.peopleNm()))) {
                continue;
            }
            KobisVerificationMatch candidate = matchCredit(credit, cleanRequestedRole);
            if (VERIFIED.equals(candidate.verificationStatus())) {
                return candidate;
            }
            ambiguous = candidate;
        }
        return ambiguous == null
                ? new KobisVerificationMatch(NOT_VERIFIED, null, null, null, null, null)
                : ambiguous;
    }

    private KobisVerificationMatch matchCredit(KobisCredit credit, String requestedRoleName) {
        String providerRoleName = clean(credit.roleName());
        String requestedGroup = roleGroup(requestedRoleName);
        String providerGroup = roleGroup(providerRoleName);
        boolean exactRole = providerRoleName != null && normalize(providerRoleName).equals(normalize(requestedRoleName));
        boolean sameGroup = requestedGroup != null && requestedGroup.equals(providerGroup);
        String status = exactRole || sameGroup ? VERIFIED : AMBIGUOUS;
        return new KobisVerificationMatch(
                status,
                credit.source(),
                clean(credit.peopleNm()),
                clean(credit.peopleNmEn()),
                providerRoleName,
                sameGroup ? requestedGroup : providerGroup
        );
    }

    private List<KobisCredit> allCredits(KobisMovieDetail detail) {
        return List.of(detail.directors(), detail.actors(), detail.staffs()).stream()
                .flatMap(List::stream)
                .toList();
    }

    private void register(String groupName, String... names) {
        for (String name : names) {
            roleGroups.put(normalize(name), groupName);
        }
    }

    private String roleGroup(String roleName) {
        String normalized = normalize(roleName);
        if (normalized == null) {
            return null;
        }
        if (roleGroups.containsKey(normalized)) {
            return roleGroups.get(normalized);
        }
        for (Map.Entry<String, String> entry : roleGroups.entrySet()) {
            Set<String> parts = Set.of(entry.getKey().split("[/ ]"));
            for (String part : parts) {
                if (StringUtils.hasText(part) && normalized.contains(part)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalize(String value) {
        String cleanValue = clean(value);
        return cleanValue == null ? null : cleanValue.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
