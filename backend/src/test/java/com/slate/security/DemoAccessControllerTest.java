package com.slate.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.slate.common.ApiResponse;
import com.slate.common.SlateException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class DemoAccessControllerTest {

    @Test
    void disabledOrUnconfiguredGateAcceptsWithoutCode() {
        DemoAccessGateService gateService = mock(DemoAccessGateService.class);
        when(gateService.verify(null, null)).thenReturn(java.util.Map.of("enabled", false, "accepted", true));

        ApiResponse<?> disabledResponse = new DemoAccessController(gateService).verify(null, null);
        assertThat(disabledResponse.success()).isTrue();
        assertThat(disabledResponse.data()).isEqualTo(java.util.Map.of("enabled", false, "accepted", true));
    }

    @Test
    void delegatesHeaderAndBodyCodeToGateService() {
        DemoAccessGateService gateService = mock(DemoAccessGateService.class);
        DemoAccessController controller = new DemoAccessController(gateService);
        when(gateService.verify("configured-code", null)).thenReturn(java.util.Map.of("enabled", true, "accepted", true));
        when(gateService.verify(null, "configured-code")).thenReturn(java.util.Map.of("enabled", true, "accepted", true));

        ApiResponse<?> headerResponse = controller.verify("configured-code", null);
        ApiResponse<?> bodyResponse = controller.verify(
                null,
                new DemoAccessController.DemoAccessRequest("configured-code")
        );

        assertThat(headerResponse.data()).isEqualTo(java.util.Map.of("enabled", true, "accepted", true));
        assertThat(bodyResponse.data()).isEqualTo(java.util.Map.of("enabled", true, "accepted", true));
        verify(gateService).verify("configured-code", null);
        verify(gateService).verify(null, "configured-code");
    }

    @Test
    void propagatesGateServiceForbiddenResult() {
        DemoAccessGateService gateService = mock(DemoAccessGateService.class);
        when(gateService.verify("wrong-code", null)).thenThrow(new SlateException(HttpStatus.FORBIDDEN, "접속 코드가 올바르지 않습니다."));
        DemoAccessController controller = new DemoAccessController(gateService);

        assertForbidden(() -> controller.verify("wrong-code", null));
    }

    @Test
    void nonBlankHeaderAndBodyAreBothPassedToServiceForPriorityHandling() {
        DemoAccessGateService gateService = mock(DemoAccessGateService.class);
        when(gateService.verify("wrong-code", "configured-code")).thenThrow(new SlateException(HttpStatus.FORBIDDEN, "접속 코드가 올바르지 않습니다."));
        DemoAccessController controller = new DemoAccessController(gateService);

        assertForbidden(() -> controller.verify(
                "wrong-code",
                new DemoAccessController.DemoAccessRequest("configured-code")
        ));
        verify(gateService).verify("wrong-code", "configured-code");
    }

    private void assertForbidden(Runnable request) {
        assertThatThrownBy(request::run)
                .isInstanceOfSatisfying(SlateException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception).hasMessage("접속 코드가 올바르지 않습니다.");
                });
    }
}
