package com.slate.accounts;

import java.util.List;
import java.util.Map;

import com.slate.common.ApiResponse;
import com.slate.security.CurrentUser;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class CompanyDocumentController {

    private final CompanyDocumentService companyDocumentService;

    public CompanyDocumentController(CompanyDocumentService companyDocumentService) {
        this.companyDocumentService = companyDocumentService;
    }

    @PostMapping("/api/auth/company-applications/{applicationId}/documents")
    public ApiResponse<Map<String, Object>> publicUploadDocument(
            @PathVariable Long applicationId,
            @RequestParam String businessRegistrationNo,
            @RequestParam(defaultValue = "BUSINESS_REGISTRATION") String documentType,
            @RequestParam MultipartFile file
    ) {
        return ApiResponse.ok(
                companyDocumentService.publicUpload(applicationId, businessRegistrationNo, documentType, file),
                "회사 승인 서류를 업로드했습니다."
        );
    }

    @GetMapping("/api/company/application/documents")
    @PreAuthorize("hasRole('COMPANY')")
    public ApiResponse<List<Map<String, Object>>> myDocuments(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(companyDocumentService.myDocuments(currentUser));
    }

    @PostMapping("/api/company/application/documents")
    @PreAuthorize("hasRole('COMPANY')")
    public ApiResponse<Map<String, Object>> uploadMyDocument(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "BUSINESS_REGISTRATION") String documentType,
            @RequestParam MultipartFile file
    ) {
        return ApiResponse.ok(
                companyDocumentService.uploadMyDocument(currentUser, documentType, file),
                "회사 서류를 업로드했습니다."
        );
    }

    @DeleteMapping("/api/company/application/documents/{documentId}")
    @PreAuthorize("hasRole('COMPANY')")
    public ApiResponse<Map<String, Object>> deleteMyDocument(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long documentId
    ) {
        return ApiResponse.ok(companyDocumentService.deleteMyDocument(currentUser, documentId), "회사 서류를 삭제했습니다.");
    }

    @GetMapping("/api/admin/company-applications/{applicationId}/documents")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Map<String, Object>>> adminDocuments(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long applicationId
    ) {
        return ApiResponse.ok(companyDocumentService.adminDocuments(currentUser.userId(), applicationId));
    }

    @GetMapping("/api/admin/company-applications/documents/{documentId}/download")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> adminDownload(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Long documentId
    ) {
        return companyDocumentService.adminDownload(currentUser.userId(), documentId);
    }
}
