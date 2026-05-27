package com.vinrecipe.dao;

import com.vinrecipe.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User entities.
 * All DB operations use PreparedStatement to prevent SQL injection.
 */
public class UserDAO {

    /** Insert a new user and return the generated user_id. */
    public int insert(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        }
        return -1;
    }

    /** Find a user by username. Returns null if not found. */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    /** Authenticate: returns User if credentials match, null otherwise. */
    public User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    /** Return all users. */
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) users.add(mapRow(rs));
        }
        return users;
    }

    /** Find user by ID. */
    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    /**
     * Map a ResultSet row to the appropriate User subclass.
     * Demonstrates Polymorphism — returns different subclasses based on role.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        int userId     = rs.getInt("user_id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String email    = rs.getString("email");
        String role     = rs.getString("role");
        int roomId      = rs.getInt("room_id");

        switch (role) {
            case "ADMIN":
                return new Admin(userId, username, password, email);
            case "ROOM_LEADER":
                return new RoomLeader(userId, username, password, email, roomId);
            default:
                return new NormalStudent(userId, username, password, email, roomId);
        }
    }

    /** Find all users belonging to the same room. */
    public List<User> findByRoomId(int roomId) throws SQLException {
        List<User> users = new ArrayList<>();
        if (roomId <= 0) return users;
        String sql = "SELECT * FROM users WHERE room_id = ?";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    /** Get Room Name by ID. */
    public String getRoomName(int roomId) throws SQLException {
        if (roomId <= 0) return "No Assigned Room";
        String sql = "SELECT room_name FROM rooms WHERE room_id = ?";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("room_name");
        }
        return "Unknown Room";
    }

    /**
     * Look up a room by its name AND access code.
     * @return room_id if found, -1 otherwise.
     */
    public int findRoomByNameAndCode(String roomName, String accessCode) throws SQLException {
        String sql = "SELECT room_id FROM rooms WHERE LOWER(room_name) = LOWER(?) AND access_code = ?";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomName.trim());
            stmt.setString(2, accessCode.trim().toUpperCase());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("room_id");
        }
        return -1;
    }

    /**
     * Create a new room for a just-registered Room Leader.
     * Inserts the room row, then updates the leader's room_id in users table.
     * @return the new room_id, or -1 on failure.
     */
    public int createRoomForLeader(String roomName, int leaderId, String accessCode) throws SQLException {
        Connection conn = DatabaseConnection.getInstance();
        // Insert room
        String insertRoom = "INSERT INTO rooms (room_name, leader_id, access_code) VALUES (?, ?, ?)";
        int roomId = -1;
        try (PreparedStatement stmt = conn.prepareStatement(insertRoom, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, roomName.trim());
            stmt.setInt(2, leaderId);
            stmt.setString(3, accessCode.trim().toUpperCase());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) roomId = keys.getInt(1);
        }
        // Link leader's room_id
        if (roomId != -1) {
            updateUserRoom(leaderId, roomId);
        }
        return roomId;
    }

    /**
     * Update the room_id of an existing user (used when a student joins a room).
     */
    public void updateUserRoom(int userId, int roomId) throws SQLException {
        String sql = "UPDATE users SET room_id = ? WHERE user_id = ?";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    /** Insert an announcement into the database. */
    public void insertAnnouncement(int roomId, int userId, String message) throws SQLException {
        String sql = "INSERT INTO announcements (room_id, user_id, message) VALUES (?, ?, ?)";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomId);
            stmt.setInt(2, userId);
            stmt.setString(3, message);
            stmt.executeUpdate();
        }
    }

    public static class Announcement {
        private final String message;
        private final String createdAt;

        public Announcement(String message, String createdAt) {
            this.message = message;
            this.createdAt = createdAt;
        }

        public String getMessage() { return message; }
        public String getCreatedAt() { return createdAt; }
    }

    /** Get all announcements for a room, ordered by created_at. */
    public List<Announcement> getAnnouncements(int roomId) throws SQLException {
        List<Announcement> list = new ArrayList<>();
        if (roomId <= 0) return list;
        String sql = "SELECT message, created_at FROM announcements WHERE room_id = ? ORDER BY created_at ASC";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Announcement(rs.getString("message"), rs.getString("created_at")));
            }
        }
        return list;
    }

    /** Delete user by ID. Admin power. */
    public boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    /** Update user role and room. Admin power. */
    public boolean updateUserRole(int userId, String role, int roomId) throws SQLException {
        String sql = "UPDATE users SET role = ?, room_id = ? WHERE user_id = ?";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role);
            if (roomId <= 0) {
                stmt.setNull(2, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(2, roomId);
            }
            stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        }
    }
}
