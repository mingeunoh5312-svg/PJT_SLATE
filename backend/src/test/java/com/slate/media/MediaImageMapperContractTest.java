package com.slate.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MediaImageMapperContractTest {
    @Test
    void allImageEntitiesHaveReadAndUpdateContracts() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/mappers/MediaImageMapper.xml")) {
            assertThat(input).isNotNull();
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(xml)
                    .contains("profile_image_path")
                    .contains("team SET representative_image_path")
                    .contains("work_item SET representative_image_path")
                    .contains("portfolio_item SET thumbnail_image_path")
                    .contains("entityType == 'CONTEST'")
                    .contains("entityType == 'CONTEST_REQUEST'")
                    .contains("COALESCE(requester_company_user_id, created_by) AS ownerUserId")
                    .contains("contest SET representative_image_path")
                    .contains("contest_open_request SET representative_image_path")
                    .doesNotContain("requester_company_user_id IS NOT NULL");
        }
    }
}
