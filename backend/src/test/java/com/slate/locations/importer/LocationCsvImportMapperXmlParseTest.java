package com.slate.locations.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class LocationCsvImportMapperXmlParseTest {

    @Test
    void importerMapperXmlParsesWithMyBatis() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getResourceAsStream("/mappers/LocationCsvImportMapper.xml")) {
            assertThat(input).isNotNull();
            XMLMapperBuilder builder = new XMLMapperBuilder(
                    input,
                    configuration,
                    "mappers/LocationCsvImportMapper.xml",
                    configuration.getSqlFragments()
            );
            builder.parse();
        }

        assertThat(configuration.hasStatement(
                "com.slate.locations.importer.LocationCsvImportMapper.upsertLocation"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.slate.locations.importer.LocationCsvImportMapper.upsertHistory"
        )).isTrue();
    }
}
