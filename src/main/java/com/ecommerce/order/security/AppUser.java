package com.ecommerce.order.security;

import javax.persistence.*;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false)
    private boolean enabled;

    public AppUser() {}

    public AppUser(String username, String password, String role, boolean enabled) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String username;
        private String password;
        private String role;
        private boolean enabled;

        public Builder username(String username) { this.username = username; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }

        public AppUser build() {
            return new AppUser(username, password, role, enabled);
        }
    }
}
