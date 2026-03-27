package com.aircraft.emms.ui.model;

import java.util.List;

public class LoginResponse {
    private String token;
    private String serviceId;
    private String name;
    private Role role;
    private List<String> roles;

    public LoginResponse() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}
