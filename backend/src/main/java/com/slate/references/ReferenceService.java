package com.slate.references;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class ReferenceService {

    private static final List<String> DEFAULT_GROUPS = List.of(
            "PROFILE_VISIBILITY",
            "ACTIVITY_STATUS",
            "EXPERIENCE_LEVEL",
            "JOIN_AVAILABILITY",
            "COLLABORATION_STATUS",
            "COLLABORATION_CONDITION",
            "TRAVEL_RANGE",
            "DURATION",
            "TEAM_STATUS",
            "REQUEST_STATUS"
    );

    private final ReferenceMapper referenceMapper;

    public ReferenceService(ReferenceMapper referenceMapper) {
        this.referenceMapper = referenceMapper;
    }

    public Map<String, List<Map<String, Object>>> codes(List<String> groups) {
        List<String> requested = groups == null || groups.isEmpty() ? DEFAULT_GROUPS : groups;
        return referenceMapper.selectCodes(requested).stream()
                .collect(Collectors.groupingBy(
                        row -> String.valueOf(row.get("codeGroup")),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public List<Map<String, Object>> regions(String keyword, Integer limit) {
        int safeLimit = Math.max(1, Math.min(limit == null ? 20 : limit, 1000));
        return referenceMapper.selectRegions(keyword, safeLimit);
    }

    public List<Map<String, Object>> roles() {
        Map<Long, Map<String, Object>> categories = new LinkedHashMap<>();
        for (Map<String, Object> row : referenceMapper.selectRoles()) {
            Long categoryId = ((Number) row.get("roleCategoryId")).longValue();
            Map<String, Object> category = categories.computeIfAbsent(categoryId, ignored -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("roleCategoryId", categoryId);
                value.put("name", row.get("roleCategoryName"));
                value.put("sortOrder", row.get("categorySortOrder"));
                value.put("roles", new ArrayList<Map<String, Object>>());
                return value;
            });
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> roles = (List<Map<String, Object>>) category.get("roles");
            roles.add(Map.of(
                    "roleId", row.get("roleId"),
                    "name", row.get("roleName"),
                    "sortOrder", row.get("roleSortOrder")
            ));
        }
        return new ArrayList<>(categories.values());
    }

    public List<Map<String, Object>> genres() {
        return referenceMapper.selectGenres();
    }
}
