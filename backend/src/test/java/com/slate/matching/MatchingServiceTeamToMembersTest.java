package com.slate.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import com.slate.common.SlateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MatchingServiceTeamToMembersTest {

    private MatchingService service;
    private Map<String, Object> slot;
    private Map<String, Object> team;
    private String teamRole;

    @BeforeEach
    void setUp() {
        slot = openSlot();
        team = activeTeam();
        teamRole = "LEADER";
        MatchingMapper mapper = (MatchingMapper) Proxy.newProxyInstance(
                MatchingMapper.class.getClassLoader(),
                new Class<?>[]{MatchingMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectSlotById" -> slot;
                    case "selectTeamById" -> team;
                    case "selectActiveTeamRole" -> teamRole;
                    case "selectTeamGenres", "selectCandidateProfiles", "selectProfilePortfolioItems", "selectScorePolicyItems" -> List.of();
                    case "selectActiveScorePolicy" -> null;
                    default -> defaultValue(method.getReturnType());
                }
        );
        service = new MatchingService(mapper, null, null);
    }

    @Test
    void requiresLeaderOrSubLeaderPermission() {
        teamRole = "MEMBER";

        assertThatThrownBy(() -> service.teamToMembers(7L, Map.of("teamId", 10L, "slotId", 100L)))
                .isInstanceOf(SlateException.class)
                .hasMessage("팀장 또는 부팀장만 팀원을 찾을 수 있습니다.");
    }

    @Test
    void rejectsClosedOrFilledRecruitmentSlot() {
        slot = new java.util.LinkedHashMap<>(openSlot());
        slot.put("remainingCount", 0L);

        assertThatThrownBy(() -> service.teamToMembers(7L, Map.of("teamId", 10L, "slotId", 100L)))
                .isInstanceOf(SlateException.class)
                .hasMessage("현재 모집 중인 역할만 사용할 수 있습니다.");
    }

    @Test
    void rejectsSlotFromAnotherTeam() {
        assertThatThrownBy(() -> service.teamToMembers(7L, Map.of("teamId", 99L, "slotId", 100L)))
                .isInstanceOf(SlateException.class)
                .hasMessage("선택한 팀의 모집 역할을 찾을 수 없습니다.");
    }

    @Test
    void regionFilterUsesExactProfileRegionId() throws Exception {
        var method = MatchingService.class.getDeclaredMethod(
                "matchesTeamToMemberFilters", Map.class, Map.class, Map.class);
        method.setAccessible(true);
        Map<String, Object> profile = Map.of(
                "regionId", 101L,
                "roles", List.of(Map.of("roleId", 5L)),
                "genres", List.of(),
                "collaborationConditions", List.of()
        );

        assertThat(method.invoke(service, profile, openSlot(), Map.of("regionIds", List.of("101")))).isEqualTo(true);
        assertThat(method.invoke(service, profile, openSlot(), Map.of("regionIds", List.of("202")))).isEqualTo(false);
    }

    @Test
    void teamToMemberExperienceFilterAcceptsMultipleSelectedLevels() throws Exception {
        var method = MatchingService.class.getDeclaredMethod(
                "matchesTeamToMemberFilters", Map.class, Map.class, Map.class);
        method.setAccessible(true);
        Map<String, Object> profile = Map.of(
                "experienceLevel", "MID",
                "roles", List.of(Map.of("roleId", 5L)),
                "genres", List.of(),
                "collaborationConditions", List.of()
        );

        assertThat(method.invoke(service, profile, openSlot(), Map.of("experienceLevel", List.of("JUNIOR", "MID")))).isEqualTo(true);
        assertThat(method.invoke(service, profile, openSlot(), Map.of("experienceLevel", List.of("SENIOR")))).isEqualTo(false);
    }

    @Test
    void memberToTeamExperienceFilterAcceptsMultipleSelectedLevels() throws Exception {
        var method = MatchingService.class.getDeclaredMethod(
                "matchesMemberToTeamFilters", Map.class, Map.class, Map.class);
        method.setAccessible(true);
        Map<String, Object> profile = Map.of(
                "roles", List.of(Map.of("roleId", 5L)),
                "genres", List.of(),
                "collaborationConditions", List.of()
        );
        Map<String, Object> slot = Map.of(
                "roleId", 5L,
                "requiredExperienceLevel", "SENIOR",
                "teamGenres", List.of(),
                "collaborationCondition", "ANY"
        );

        assertThat(method.invoke(service, profile, slot, Map.of("experienceLevel", List.of("MID", "SENIOR")))).isEqualTo(true);
        assertThat(method.invoke(service, profile, slot, Map.of("experienceLevel", List.of("JUNIOR")))).isEqualTo(false);
    }

    private Map<String, Object> openSlot() {
        return Map.of(
                "slotId", 100L,
                "teamId", 10L,
                "recruitmentStatus", "OPEN",
                "slotStatus", "OPEN",
                "remainingCount", 1L,
                "roleId", 5L
        );
    }

    private Map<String, Object> activeTeam() {
        return Map.of(
                "teamId", 10L,
                "name", "남산 새벽팀",
                "status", "RECRUITING"
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        return null;
    }
}
