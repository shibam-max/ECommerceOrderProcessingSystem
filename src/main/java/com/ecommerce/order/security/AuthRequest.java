package com.ecommerce.order.security;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AuthRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    public AuthRequest() {}

    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String username;
        private String password;

        public Builder username(String username) { this.username = username; return this; }
        public Builder password(String password) { this.password = password; return this; }

        public AuthRequest build() {
            return new AuthRequest(username, password);
        }
    }
}
