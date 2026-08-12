package com.slate.security;

import java.util.List;

public record CurrentUser(Long userId, String email, String nickname, String accountType, List<String> authorities) {

    public boolean isAdmin() {
        return "ADMIN".equals(accountType);
    }
}
