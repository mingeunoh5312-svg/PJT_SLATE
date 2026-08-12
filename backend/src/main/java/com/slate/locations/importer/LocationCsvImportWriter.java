package com.slate.locations.importer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.slate.common.SlateException;
import com.slate.locations.importer.LocationCsvImportService.ImportHistory;
import com.slate.locations.importer.LocationCsvImportService.ImportLocation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationCsvImportWriter {

    private final LocationCsvImportMapper mapper;

    public LocationCsvImportWriter(LocationCsvImportMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public WriteCount writeChunk(List<ImportLocation> locations) {
        int historyCount = 0;
        for (ImportLocation location : locations) {
            Map<String, Object> locationValues = new LinkedHashMap<>(location.values());
            mapper.upsertLocation(locationValues);
            Long locationId = longValue(locationValues.get("locationId"));
            if (locationId == null) {
                throw new SlateException("로케이션 장소 upsert 결과 ID를 확인할 수 없습니다.");
            }
            for (ImportHistory history : location.histories()) {
                Map<String, Object> historyValues = new LinkedHashMap<>(history.values());
                historyValues.put("locationId", locationId);
                mapper.upsertHistory(historyValues);
                historyCount++;
            }
        }
        return new WriteCount(locations.size(), historyCount);
    }

    private Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    public record WriteCount(int locations, int histories) {
    }
}
