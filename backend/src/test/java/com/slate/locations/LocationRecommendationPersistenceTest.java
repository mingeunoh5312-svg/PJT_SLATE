package com.slate.locations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.common.SlateException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class LocationRecommendationPersistenceTest {

    @Test
    void completeSessionStoresResultsBeforeFinalStatus() {
        LocationMapper mapper = mock(LocationMapper.class);
        LocationRecommendationPersistence persistence = new LocationRecommendationPersistence(mapper);
        Map<String, Object> recommendation = new LinkedHashMap<>();
        recommendation.put("locationId", 88L);
        when(mapper.updateSearchSessionStatus(101L, "COMPLETED", 1, 1, null)).thenReturn(1);

        persistence.completeSession(101L, 1, "COMPLETED", null, List.of(recommendation));

        InOrder inOrder = inOrder(mapper);
        inOrder.verify(mapper).insertRecommendationResult(recommendation);
        inOrder.verify(mapper).updateSearchSessionStatus(101L, "COMPLETED", 1, 1, null);
    }

    @Test
    void createSessionRequiresGeneratedSessionId() {
        LocationMapper mapper = mock(LocationMapper.class);
        LocationRecommendationPersistence persistence = new LocationRecommendationPersistence(mapper);

        assertThatThrownBy(() -> persistence.createSession(new LinkedHashMap<>()))
                .isInstanceOf(SlateException.class)
                .hasMessage("추천 세션을 생성하지 못했습니다.");
    }

    @Test
    void failSessionWritesFailedStatus() {
        LocationMapper mapper = mock(LocationMapper.class);
        LocationRecommendationPersistence persistence = new LocationRecommendationPersistence(mapper);

        persistence.failSession(101L, 12, "result write failed");

        verify(mapper).updateSearchSessionStatus(101L, "FAILED", 12, 0, "result write failed");
    }
}
