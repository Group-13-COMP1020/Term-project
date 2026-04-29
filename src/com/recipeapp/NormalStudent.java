package com.recipeapp;

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
    }
}
