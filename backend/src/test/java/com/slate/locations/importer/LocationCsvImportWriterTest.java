package com.slate.locations.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.locations.importer.LocationCsvImportService.ImportHistory;
import com.slate.locations.importer.LocationCsvImportService.ImportLocation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LocationCsvImportWriterTest {

    @Test
    void repeatedImportUsesSameSourceVariantAndEventKeys() {
        LocationCsvImportMapper mapper = mock(LocationCsvImportMapper.class);
        when(mapper.upsertLocation(any())).thenAnswer(invocation -> {
            invocation.<Map<String, Object>>getArgument(0).put("locationId", 88L);
            return 1;
        });
        LocationCsvImportWriter writer = new LocationCsvImportWriter(mapper);
        ImportLocation location = location();

        writer.writeChunk(List.of(location));
        writer.writeChunk(List.of(location));

        ArgumentCaptor<Map<String, Object>> locationCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> historyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mapper, times(2)).upsertLocation(locationCaptor.capture());
        verify(mapper, times(2)).upsertHistory(historyCaptor.capture());
        assertThat(locationCaptor.getAllValues())
                .extracting(value -> value.get("sourceLocationId"), value -> value.get("sourceVariantNo"))
                .containsOnly(org.assertj.core.groups.Tuple.tuple("LOC-1", 1));
        assertThat(historyCaptor.getAllValues())
                .extracting(value -> value.get("sourceEventId"), value -> value.get("locationId"))
                .containsOnly(org.assertj.core.groups.Tuple.tuple("EV-1", 88L));
    }

    private ImportLocation location() {
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("sourceLocationId", "LOC-1");
        location.put("sourceVariantNo", 1);
        Map<String, Object> history = new LinkedHashMap<>();
        history.put("sourceEventId", "EV-1");
        return new ImportLocation(location, List.of(new ImportHistory(history)));
    }
}
