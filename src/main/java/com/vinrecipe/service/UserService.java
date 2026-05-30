package com.vinrecipe.service;

import com.vinrecipe.dao.UserDAO;
import com.vinrecipe.model.NormalStudent;
import com.vinrecipe.model.RoomLeader;
import com.vinrecipe.model.User;

import java.sql.SQLException;
import java.util.Random;

/**
 * Service layer for user authentication and registration.
 * Delegates to UserDAO for all DB operations.
 */
public class UserService {

    private final UserDAO userDAO = new UserDAO();
    private String lastError = null;

    public String getLastError() {
        return lastError;
    }

    /**
     * Authenticate a user with username + password.
     * @return User object on success, null on failure.
     */
    public User login(String username, String password) {
        try {
            return userDAO.authenticate(username, password);
        } catch (SQLException e) {
            System.err.println("[UserService] Login error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Register a new NormalStudent account (no room join).
     * @return generated userId on success, -1 on failure or if username taken.
     */
    public int register(String username, String password) {
        try {
            lastError = null;
            if (userDAO.findByUsername(username) != null) {
                lastError = "Username '" + username + "' already taken";
                System.err.println("[UserService] " + lastError);
                return -1;
            }
            NormalStudent newUser = new NormalStudent(0, username, password, 0);
            return userDAO.insert(newUser);
        } catch (SQLException | IllegalArgumentException e) {
            lastError = e.getMessage();
            System.err.println("[UserService] Register error: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Register a new ROOM_LEADER account.
     * Creates the user, generates an access code, creates the room, and links them.
     * @return the generated access code on success, null on failure.
     */
    public String registerLeader(String username, String password, String roomName) {
        try {
            lastError = null;
            if (userDAO.findByUsername(username) != null) {
                lastError = "Username '" + username + "' already taken";
                System.err.println("[UserService] " + lastError);
                return null;
            }
            String accessCode = generateAccessCode();
            RoomLeader leader = new RoomLeader(0, username, password, 0);
            int userId = userDAO.insert(leader);
            if (userId == -1) {
                lastError = "Failed to create user in database";
                return null;
            }

            // Create room and link to leader
            int roomId = userDAO.createRoomForLeader(roomName, userId, accessCode);
            if (roomId == -1) {
                lastError = "Failed to create room in database";
                return null;
            }

            System.out.println("[UserService] Room Leader '" + username + "' created room '" + roomName + "' with code: " + accessCode);
            return accessCode;
        } catch (SQLException | IllegalArgumentException e) {
            lastError = e.getMessage();
            System.err.println("[UserService] RegisterLeader error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Register a new NORMAL_STUDENT and join an existing room by name + code.
     * @return userId on success, -1 if username taken, -2 if room not found/wrong code.
     */
    public int registerStudent(String username, String password, String roomName, String accessCode) {
        try {
            lastError = null;
            if (userDAO.findByUsername(username) != null) {
                lastError = "Username '" + username + "' already taken";
                System.err.println("[UserService] " + lastError);
                return -1;
            }
            int roomId = userDAO.findRoomByNameAndCode(roomName, accessCode);
            if (roomId == -1) {
                lastError = "Room not found or invalid access code. Double-check with your leader.";
                System.err.println("[UserService] " + lastError);
                return -2;
            }
            NormalStudent student = new NormalStudent(0, username, password, roomId);
            int userId = userDAO.insert(student);
            if (userId == -1) {
                lastError = "Failed to create user in database";
                return -1;
            }

            // Link student's room_id (already set on insert via role, but ensure via update)
            userDAO.updateUserRoom(userId, roomId);
            System.out.println("[UserService] Student '" + username + "' joined room '" + roomName + "'.");
            return userId;
        } catch (SQLException | IllegalArgumentException e) {
            lastError = e.getMessage();
            System.err.println("[UserService] RegisterStudent error: " + e.getMessage());
            return -1;
        }
    }

    /** Find user by username. */
    public User findByUsername(String username) {
        try {
            return userDAO.findByUsername(username);
        } catch (SQLException e) {
            System.err.println("[UserService] FindByUsername error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generate a random 6-character uppercase alphanumeric access code.
     * e.g. "A3F9KL"
     */
    private String generateAccessCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // omit confusing chars I/O/1/0
        Random rng = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /** Admin power: Get all users. */
    public java.util.List<User> getAllUsers() {
        try {
            return userDAO.findAll();
        } catch (SQLException e) {
            System.err.println("[UserService] getAllUsers error: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /** Admin power: Delete a user by ID. */
    public boolean deleteUser(int userId) {
        try {
            return userDAO.deleteUser(userId);
        } catch (SQLException e) {
            System.err.println("[UserService] deleteUser error: " + e.getMessage());
            return false;
        }
    }

    /** Admin power: Update user role and room. */
    public boolean updateUserRole(int userId, String role, int roomId) {
        try {
            return userDAO.updateUserRole(userId, role, roomId);
        } catch (SQLException e) {
            System.err.println("[UserService] updateUserRole error: " + e.getMessage());
            return false;
        }
    }
}
