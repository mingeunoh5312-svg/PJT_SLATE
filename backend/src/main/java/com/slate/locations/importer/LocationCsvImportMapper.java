package com.slate.locations.importer;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LocationCsvImportMapper {

    int upsertLocation(Map<String, Object> location);

    int upsertHistory(Map<String, Object> history);
}
