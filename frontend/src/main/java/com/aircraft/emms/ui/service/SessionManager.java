package com.aircraft.emms.ui.service;

import com.aircraft.emms.ui.model.LoginResponse;
import com.aircraft.emms.ui.model.Role;

/**
 * Holds the current user session state. Singleton pattern for desktop app.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private String token;
    private String serviceId;
    private String name;
    private Role role;
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
    }

    public void logout() {
        this.token = null;
        this.serviceId = null;
        this.name = null;
        this.role = null;
        this.userId = null;
    }

    public boolean isLoggedIn() {
        return token != null;
    }

    public String getToken() { return token; }
    public String getServiceId() { return serviceId; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isCaptain() { return role == Role.CAPTAIN; }
    public boolean isPilot() { return role == Role.PILOT; }
}
