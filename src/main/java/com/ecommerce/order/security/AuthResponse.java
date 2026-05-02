package com.ecommerce.order.security;

public class AuthResponse {

    private String token;
    private String tokenType;
    private String username;
    private String role;

    public AuthResponse() {}

    public AuthResponse(String token, String tokenType, String username, String role) {
        this.token = token;
        this.tokenType = tokenType;
        this.username = username;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String token;
        private String tokenType;
        private String username;
        private String role;

        public Builder token(String token) { this.token = token; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder role(String role) { this.role = role; return this; }

        public AuthResponse build() {
            return new AuthResponse(token, tokenType, username, role);
        }
    }
}
