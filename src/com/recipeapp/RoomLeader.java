package com.recipeapp;

public class RoomLeader extends User {
    private int roomId;

    public RoomLeader() {
        super();
        setRole("ROOM_LEADER");
    }

    public RoomLeader(int userId, String username, String password, String email, int roomId) {
        super(userId, username, password, email, "ROOM_LEADER");
        this.roomId = roomId;
    }

    @Override
    public int getPermissionLevel() {
        return 2; // Mid-level permission
    }

    public void manageRoom() {
        System.out.println("RoomLeader " + getUsername() + " is managing room ID: " + roomId);
    }

    public void assignMembers() {
        System.out.println("RoomLeader " + getUsername() + " is assigning members to room ID: " + roomId);
    }

    // Getters and Setters
    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    @Override
    public String toString() {
        return "RoomLeader{userId=" + getUserId() + ", username='" + getUsername() + "', roomId=" + roomId + "}";
    }
}