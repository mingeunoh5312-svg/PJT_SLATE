package com.slate.locations;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class LocationMapperXmlParseTest {

    @Test
    void locationMapperXmlParsesWithMyBatis() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getResourceAsStream("/mappers/LocationMapper.xml")) {
            assertThat(input).isNotNull();
            XMLMapperBuilder builder = new XMLMapperBuilder(
                    input,
                    configuration,
                    "mappers/LocationMapper.xml",
                    configuration.getSqlFragments()
            );
            builder.parse();
        }

        assertThat(configuration.hasStatement("com.slate.locations.LocationMapper.selectCandidateLocations")).isTrue();
        assertThat(configuration.hasStatement("com.slate.locations.LocationMapper.insertSavedCandidate")).isTrue();
        assertThat(configuration.hasStatement("com.slate.locations.LocationMapper.selectTeamCandidates")).isTrue();
    }
}
