package com.vinrecipe.model;

import java.util.ArrayList;
import java.util.List;

<<<<<<< HEAD
/**
 * Represents a dorm room grouping students together.
 * Demonstrates Composition: Room HAS-A RoomLeader and List of NormalStudents.
 */
public class Room {

    private int roomId;
    private String roomName;
    private RoomLeader leader;
    private List<NormalStudent> members; // Composition
=======
public class Room {
    private int roomId;
    private String roomName;
    private RoomLeader leader;
    private List<NormalStudent> members;
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3

    public Room() {
        this.members = new ArrayList<>();
    }

    public Room(int roomId, String roomName, RoomLeader leader) {
<<<<<<< HEAD
        if (roomName == null || roomName.isBlank())
            throw new IllegalArgumentException("Room name must not be blank");
        this.roomId = roomId;
        this.roomName = roomName.trim();
=======
        this.roomId = roomId;
        this.roomName = roomName;
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
        this.leader = leader;
        this.members = new ArrayList<>();
    }

<<<<<<< HEAD
    /** Add a student to this room. */
    public void addMember(NormalStudent student) {
        if (student == null) throw new IllegalArgumentException("Student must not be null");
        if (!members.contains(student)) {
            members.add(student);
            student.setRoomId(this.roomId);
        }
    }

    /** Remove a student from this room. */
    public void removeMember(NormalStudent student) {
        members.remove(student);
    }

    // ---- Getters / Setters ----
=======
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
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
<<<<<<< HEAD
    public void setRoomName(String roomName) {
        if (roomName == null || roomName.isBlank())
            throw new IllegalArgumentException("Room name must not be blank");
        this.roomName = roomName.trim();
    }
=======
    public void setRoomName(String roomName) { this.roomName = roomName; }
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3

    public RoomLeader getLeader() { return leader; }
    public void setLeader(RoomLeader leader) { this.leader = leader; }

    public List<NormalStudent> getMembers() { return members; }
    public void setMembers(List<NormalStudent> members) { this.members = members; }

    @Override
    public String toString() {
<<<<<<< HEAD
        return "Room{id=" + roomId + ", name='" + roomName + "', members=" + members.size() + "}";
=======
        return "Room{roomId=" + roomId + ", roomName='" + roomName + "', leader=" + (leader != null ? leader.getUsername() : "none") + ", members=" + members.size() + "}";
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    }
}
