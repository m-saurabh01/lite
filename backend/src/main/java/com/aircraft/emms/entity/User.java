package com.aircraft.emms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "service_id", unique = true, nullable = false, length = 50)
    private String serviceId;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    /** Primary role (kept for backward compat with security filter) */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Comma-separated roles for multi-role support */
    @Column(name = "roles")
    private String roles;

    @Column(name = "security_question", length = 255)
    private String securityQuestion;

    @Column(name = "security_answer")
    private String securityAnswer;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id")
    private AircraftDataSet dataset;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (roles == null || roles.isBlank()) {
            roles = role != null ? role.name() : "";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Get all roles as a Set */
    public Set<Role> getRoleSet() {
        if (roles == null || roles.isBlank()) {
            return role != null ? Set.of(role) : Set.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Role::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Set multiple roles */
    public void setRoleSet(Set<Role> roleSet) {
        this.roles = roleSet.stream().map(Enum::name).collect(Collectors.joining(","));
        // Primary role = first in set
        if (!roleSet.isEmpty()) {
            this.role = roleSet.iterator().next();
        }
    }

    /** Check if user has a specific role */
    public boolean hasRole(Role r) {
        return getRoleSet().contains(r);
    }

    /** Add a role without removing existing ones */
    public void addRole(Role r) {
        Set<Role> current = new LinkedHashSet<>(getRoleSet());
        current.add(r);
        setRoleSet(current);
    }

    /** Remove a role */
    public void removeRole(Role r) {
        Set<Role> current = new LinkedHashSet<>(getRoleSet());
        current.remove(r);
        setRoleSet(current);
    }
}
