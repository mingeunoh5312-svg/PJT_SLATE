package com.slate.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AdminPermissionCatalog {

    public static final String COMPANY_APPROVAL = "COMPANY_APPROVAL";
    public static final String USER_SANCTION = "USER_SANCTION";
    public static final String CONTENT_MODERATION = "CONTENT_MODERATION";
    public static final String SCORE_POLICY = "SCORE_POLICY";
    public static final String CONTEST_MANAGE = "CONTEST_MANAGE";
    public static final String DEMO_ACCESS_MANAGE = "DEMO_ACCESS_MANAGE";
    public static final String NOTIFICATION_SEND = "NOTIFICATION_SEND";
    public static final String LOG_VIEW = "LOG_VIEW";
    public static final String ADMIN_PERMISSION_MANAGE = "ADMIN_PERMISSION_MANAGE";
    public static final String REGION_MANAGE = "REGION_MANAGE";

    public static final List<Map<String, String>> ITEMS = List.of(
            item(COMPANY_APPROVAL, "회사 승인", "회사 계정 신청을 조회하고 승인 또는 거절합니다."),
            item(USER_SANCTION, "사용자 제재", "사용자 계정 상태와 제재 정책을 관리합니다."),
            item(CONTENT_MODERATION, "콘텐츠 관리", "게시글, 리뷰, 업로드 파일을 운영 정책에 따라 처리합니다."),
            item(SCORE_POLICY, "점수 정책", "매칭 점수 정책을 조회하고 수정합니다."),
            item(CONTEST_MANAGE, "공모전 관리", "공모전을 직접 등록하고 회사 개설 요청을 심사합니다."),
            item(DEMO_ACCESS_MANAGE, "접근 코드 관리", "배포 전/점검 안내 접근 코드를 생성하고 폐기합니다."),
            item(NOTIFICATION_SEND, "알림 발송", "관리자 공지 알림을 사용자에게 발송합니다."),
            item(LOG_VIEW, "로그 조회", "감사 로그와 운영 로그를 조회합니다."),
            item(ADMIN_PERMISSION_MANAGE, "관리자 권한", "관리자별 세부 권한을 부여하거나 회수합니다."),
            item(REGION_MANAGE, "지역 DB 관리", "서비스 지역명과 좌표를 조회하고 수정합니다.")
    );

    public static final Set<String> CODES = Set.of(
            COMPANY_APPROVAL,
            USER_SANCTION,
            CONTENT_MODERATION,
            SCORE_POLICY,
            CONTEST_MANAGE,
            DEMO_ACCESS_MANAGE,
            NOTIFICATION_SEND,
            LOG_VIEW,
            ADMIN_PERMISSION_MANAGE,
            REGION_MANAGE
    );

    private AdminPermissionCatalog() {
    }

    private static Map<String, String> item(String code, String label, String description) {
        return Map.of("code", code, "label", label, "description", description);
    }
}
