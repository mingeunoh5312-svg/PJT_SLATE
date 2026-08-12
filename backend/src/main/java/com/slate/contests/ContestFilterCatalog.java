package com.slate.contests;

import java.util.Set;

public final class ContestFilterCatalog {
    public static final Set<String> TARGETS = Set.of(
            "ANYONE", "PRESCHOOL", "ELEMENTARY", "MIDDLE", "HIGH", "UNIVERSITY",
            "GRADUATE", "ADULT", "FOREIGNER", "ELIGIBLE_ONLY"
    );
    public static final Set<String> REGIONS = Set.of(
            "ALL", "ONLINE", "CAPITAL", "NATIONWIDE", "SEOUL", "INCHEON", "DAEJEON", "GWANGJU",
            "DAEGU", "BUSAN", "ULSAN", "SEJONG", "GYEONGGI", "GANGWON", "CHUNGNAM", "CHUNGBUK",
            "JEONNAM", "JEONBUK", "GYEONGNAM", "GYEONGBUK", "JEJU", "OVERSEAS", "OTHER"
    );
    public static final Set<String> LIST_REGION_FILTERS = Set.of(
            "ONLINE", "NATIONWIDE", "CAPITAL", "REGIONAL"
    );
    public static final Set<String> ORGANIZER_TYPES = Set.of(
            "GOVERNMENT_PUBLIC", "MEDIA_PUBLISHER", "SCHOOL_ASSOCIATION", "CULTURE_VENUE",
            "COMPANY", "ORGANIZATION", "CLUB", "OVERSEAS", "OTHER"
    );
    private ContestFilterCatalog() { }
}
