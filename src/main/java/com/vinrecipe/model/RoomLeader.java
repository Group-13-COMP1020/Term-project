package com.vinrecipe.model;

<<<<<<< HEAD
import java.util.ArrayList;
import java.util.List;

/**
 * Room leader — manages a dorm room and its members.
 * Demonstrates Inheritance from User.
 */
public class RoomLeader extends User {

=======
public class RoomLeader extends User {
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    private int roomId;

    public RoomLeader() {
        super();
        setRole("ROOM_LEADER");
    }

    public RoomLeader(int userId, String username, String password, String email, int roomId) {
        super(userId, username, password, email, "ROOM_LEADER");
        this.roomId = roomId;
    }

<<<<<<< HEAD
    /** RoomLeaders have permission level 2. — Polymorphism */
    @Override
    public int getPermissionLevel() {
        return 2;
    }

    /** RoomLeader-specific: manage room settings. */
    public void manageRoom() {
        System.out.println("[RoomLeader] " + getUsername() + " is managing room " + roomId);
    }

    /** RoomLeader-specific: assign members to the room. */
    public void assignMembers(List<NormalStudent> members) {
        System.out.println("[RoomLeader] " + getUsername()
                + " assigned " + members.size() + " members to room " + roomId);
    }

    // ---- Getters / Setters ----
    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) {
        if (roomId < 0) throw new IllegalArgumentException("roomId must be non-negative");
        this.roomId = roomId;
=======
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
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    }
}
