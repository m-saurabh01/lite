package com.aircraft.emms.ui.service;

import com.aircraft.emms.ui.model.LoginResponse;
import com.aircraft.emms.ui.model.Role;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private String token;
    private String serviceId;
    private String name;
    private Role role;
    private List<String> roles;
    private Long userId;

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void login(LoginResponse response) {
        this.token = response.getToken();
        this.serviceId = response.getServiceId();
        this.name = response.getName();
        this.role = response.getRole();
        this.roles = response.getRoles() != null ? response.getRoles() : List.of(response.getRole().name());
    }

    public void logout() {
        this.token = null;
        this.serviceId = null;
        this.name = null;
        this.role = null;
        this.roles = null;
        this.userId = null;
    }

    public boolean isLoggedIn() {
        return token != null;
    }

    public String getToken() { return token; }
    public String getServiceId() { return serviceId; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public List<String> getRoles() { return roles != null ? roles : Collections.emptyList(); }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public boolean hasRole(String roleName) {
        return roles != null && roles.contains(roleName);
    }

    public boolean isAdmin() { return hasRole("ADMIN"); }
    public boolean isCaptain() { return hasRole("CAPTAIN"); }
    public boolean isPilot() { return hasRole("PILOT"); }
    public boolean isTechnician() { return hasRole("TECHNICIAN"); }
    public boolean isMechanic() { return hasRole("MECHANIC"); }

    public String getRolesDisplay() {
        return roles != null ? String.join(", ", roles) : "";
    }
}
