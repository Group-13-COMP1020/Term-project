package com.recipeapp;

public abstract class User {
    private int userId;
    private String username;
    private String password;
    private String email;
    private String role;

    public User() {}

    public User(int userId, String username, String password, String email, String role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    // Abstract method - each subclass must implement
    public abstract int getPermissionLevel();

    public boolean login() {
        // Basic login logic placeholder
        return username != null && password != null;
    }

    public void logout() {
        System.out.println(username + " has logged out.");
    }

    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public String toString() {
        return "User{userId=" + userId + ", username='" + username + "', role='" + role + "'}";
    }
}