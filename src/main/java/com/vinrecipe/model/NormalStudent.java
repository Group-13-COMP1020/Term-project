package com.vinrecipe.model;

<<<<<<< HEAD
/**
 * Normal student user — can view shared list in their room.
 * Demonstrates Inheritance from User.
 */
public class NormalStudent extends User {

    private int roomId; // 0 means not assigned to any room

    public NormalStudent() {
        super();
        setRole("NORMAL_STUDENT");
    }

    public NormalStudent(int userId, String username, String password, String email, int roomId) {
        super(userId, username, password, email, "NORMAL_STUDENT");
        this.roomId = roomId;
    }

    /** NormalStudents have permission level 1. — Polymorphism */
    @Override
    public int getPermissionLevel() {
        return 1;
    }

    /** NormalStudent-specific: view the shared shopping list in their room. */
    public ShoppingList viewSharedList() {
        System.out.println("[NormalStudent] " + getUsername() + " viewing shared list for room " + roomId);
        return new ShoppingList();
    }

    // ---- Getters / Setters ----
    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) {
        if (roomId < 0) throw new IllegalArgumentException("roomId must be non-negative");
        this.roomId = roomId;
=======
public class NormalStudent extends User {
    private int roomId;

    public NormalStudent() {
        super();
        setRole("STUDENT");
    }

    public NormalStudent(int userId, String username, String password, String email, int roomId) {
        super(userId, username, password, email, "STUDENT");
        this.roomId = roomId;
    }

    @Override
    public int getPermissionLevel() {
        return 1; // Basic permission level
    }

    public ShoppingList viewSharedList() {
        System.out.println("NormalStudent " + getUsername() + " is viewing the shared list for room ID: " + roomId);
        // Returns the shared shopping list for the student's room (stub)
        return new ShoppingList();
    }

    // Getters and Setters
    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    @Override
    public String toString() {
        return "NormalStudent{userId=" + getUserId() + ", username='" + getUsername() + "', roomId=" + roomId + "}";
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    }
}
