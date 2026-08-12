package com.slate.accounts;

import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "계정을 생성했습니다.");
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request), "로그인했습니다.");
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(authService.me(currentUser.userId()));
    }

    @PatchMapping("/me")
    public ApiResponse<Map<String, Object>> updateMe(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody AccountUpdateRequest request
    ) {
        return ApiResponse.ok(authService.updateMe(currentUser.userId(), request), "계정 정보를 저장했습니다.");
    }

    @DeleteMapping("/me")
    public ApiResponse<Map<String, Object>> withdrawMe(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody AccountWithdrawalRequest request
    ) {
        return ApiResponse.ok(authService.withdrawMe(currentUser.userId(), request), "회원 탈퇴가 완료되었습니다.");
    }

    public record LoginRequest(
            @NotBlank String loginId,
            @NotBlank String password
    ) {
    }

    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._-]{4,50}$") String loginId,
            @NotBlank String email,
            @NotBlank @Size(min = 8, max = 80) String password,
            @NotBlank @Size(max = 50) String nickname,
            @NotBlank String accountType,
            CompanyApplicationRequest company
    ) {
    }

    public record CompanyApplicationRequest(
            @NotBlank @Size(max = 120) String companyName,
            @NotBlank @Size(max = 30) String businessRegistrationNo,
            @NotBlank @Size(max = 50) String managerName,
            @NotBlank @Size(max = 30) String managerPhone,
            @NotBlank @Size(max = 1000) String companyIntro,
            @Size(max = 120) String publicDataCompanyName
    ) {
    }

    public record AccountUpdateRequest(
            @NotBlank @Size(max = 50) String nickname,
            @NotBlank @Email @Size(max = 255) String email,
            @Size(max = 80) String currentPassword,
            @Size(min = 8, max = 80) String newPassword
    ) {
    }

    public record AccountWithdrawalRequest(
            @NotBlank String currentPassword
    ) {
    }
}
