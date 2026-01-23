package com.example.initialProject.common.util;

public class KeycloakRoleConverter {
    public static Collection<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
        Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
        if (realmAccess == null || realmAccess.get("roles") == null) {
            return Collections.emptyList(); // Sử dụng Collections (có s) để sửa lỗi image_f5cd33
        }

        Collection<String> roles = (Collection<String>) realmAccess.get("roles");
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }
}