package com.recipeapp;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private int roomId;
    private String roomName;
    private RoomLeader leader;
    private List<NormalStudent> members;

    public Room() {
        this.members = new ArrayList<>();
    }

    public Room(int roomId, String roomName, RoomLeader leader) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.leader = leader;
        this.members = new ArrayList<>();
    }

    public void addMember(NormalStudent student) {
        if (student != null) {
            members.add(student);
            student.setRoomId(this.roomId);
            System.out.println("Student '" + student.getUsername() + "' added to room '" + roomName + "'.");
        }
    }

    public void removeMember(NormalStudent student) {
        if (members.remove(student)) {
            System.out.println("Student '" + student.getUsername() + "' removed from room '" + roomName + "'.");
        } else {
            System.out.println("Student not found in room '" + roomName + "'.");
        }
    }

    // Getters and Setters
    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public RoomLeader getLeader() { return leader; }
    public void setLeader(RoomLeader leader) { this.leader = leader; }

    public List<NormalStudent> getMembers() { return members; }
    public void setMembers(List<NormalStudent> members) { this.members = members; }

    @Override
    public String toString() {
        return "Room{roomId=" + roomId + ", roomName='" + roomName + "', leader=" + (leader != null ? leader.getUsername() : "none") + ", members=" + members.size() + "}";
    }
}