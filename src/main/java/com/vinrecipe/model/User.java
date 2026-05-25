package com.vinrecipe.model;

<<<<<<< HEAD
/**
 * Abstract base class representing a system user.
 * Demonstrates OOP concepts: Abstraction, Encapsulation.
 * All subclasses (Admin, NormalStudent, RoomLeader) inherit from this class.
 */
public abstract class User {

    // All fields private — Encapsulation
=======
public abstract class User {
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    private int userId;
    private String username;
    private String password;
    private String email;
<<<<<<< HEAD
    private String role; // "ADMIN", "NORMAL_STUDENT", "ROOM_LEADER"

    // ---- Constructors ----
=======
    private String role;

>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    public User() {}

    public User(int userId, String username, String password, String email, String role) {
        this.userId = userId;
<<<<<<< HEAD
        setUsername(username);
        setPassword(password);
        setEmail(email);
        this.role = role;
    }

    // ---- Abstract method — Abstraction ----
    /**
     * Returns the permission level of this user type.
     * Must be overridden by each subclass — Polymorphism.
     */
    public abstract int getPermissionLevel();

    /**
     * Returns the room ID this user belongs to.
     * Overridden by subclasses (NormalStudent, RoomLeader) that participate in rooms.
     */
    public int getRoomId() {
        return 0; // Default: not in a room.
    }

    /**
     * Simulates login logic: validate credentials.
     * Subclasses may override for additional checks — Polymorphism.
     */
    public boolean login(String inputUsername, String inputPassword) {
        return this.username != null
                && this.username.equals(inputUsername)
                && this.password != null
                && this.password.equals(inputPassword);
    }

    /** Clears session data on logout. */
=======
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

>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    public void logout() {
        System.out.println(username + " has logged out.");
    }

<<<<<<< HEAD
    // ---- Getters / Setters with validation — Encapsulation ----
    public int getUserId() { return userId; }
    public void setUserId(int userId) {
        if (userId < 0) throw new IllegalArgumentException("userId must be non-negative");
        this.userId = userId;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username must not be blank");
        this.username = username.trim();
    }

    public String getPassword() { return password; }
    public void setPassword(String password) {
        if (password == null || password.length() < 4)
            throw new IllegalArgumentException("Password must be at least 4 characters");
        this.password = password;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        if (email == null || !email.contains("@"))
            throw new IllegalArgumentException("Email is invalid");
        this.email = email.trim();
    }
=======
    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public String toString() {
<<<<<<< HEAD
        return "User{id=" + userId + ", username='" + username + "', role='" + role + "'}";
=======
        return "User{userId=" + userId + ", username='" + username + "', role='" + role + "'}";
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    }
}
