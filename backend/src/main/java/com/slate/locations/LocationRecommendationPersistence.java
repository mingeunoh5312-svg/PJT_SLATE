package com.slate.locations;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.slate.common.SlateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationRecommendationPersistence {

    private final LocationMapper locationMapper;

    public LocationRecommendationPersistence(LocationMapper locationMapper) {
        this.locationMapper = locationMapper;
    }

    @Transactional
    public Long createSession(Map<String, Object> session) {
        locationMapper.insertSearchSession(session);
        Object value = session.get("sessionId");
        if (!(value instanceof Number number)) {
            throw new SlateException("추천 세션을 생성하지 못했습니다.");
        }
        return number.longValue();
    }

    @Transactional
    public void completeSession(
            Long sessionId,
            int candidateCount,
            String status,
            String failureReason,
            List<Map<String, Object>> recommendations
    ) {
        for (Map<String, Object> recommendation : recommendations) {
            locationMapper.insertRecommendationResult(recommendation);
        }
        int updated = locationMapper.updateSearchSessionStatus(
                sessionId,
                status,
                candidateCount,
                recommendations.size(),
                failureReason
        );
        if (updated != 1) {
            throw new SlateException("추천 세션 결과를 저장하지 못했습니다.");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failSession(Long sessionId, int candidateCount, String failureReason) {
        locationMapper.updateSearchSessionStatus(
                sessionId,
                "FAILED",
                candidateCount,
                0,
                Objects.toString(failureReason, "추천 결과 저장에 실패했습니다.")
        );
    }
}
