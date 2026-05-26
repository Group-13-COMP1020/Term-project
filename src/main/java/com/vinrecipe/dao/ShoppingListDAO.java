package com.vinrecipe.dao;

import com.vinrecipe.model.Recipe;
import com.vinrecipe.service.RecipeService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for persistent Shopping Lists.
 * Manages saving and loading selected recipes to/from the database.
 * Supports both room-wide lists (room_id) and individual lists (user_id).
 */
public class ShoppingListDAO {

    /**
     * Gets the unique list_id for a room or user, creating a new row if none exists.
     *
     * @param roomId room ID of the student (0 if unassigned)
     * @param userId user ID of the student
     * @return list_id of the persistent shopping list
     */
    public int getOrCreateListId(int roomId, int userId) throws SQLException {
        Connection conn = DatabaseConnection.getInstance();
        if (roomId > 0) {
            // Check room-wide list
            String selectSql = "SELECT list_id FROM shopping_lists WHERE room_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setInt(1, roomId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("list_id");
                }
            }
            // Create new room-wide list
            String insertSql = "INSERT INTO shopping_lists (room_id) VALUES (?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, roomId);
                stmt.executeUpdate();
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } else {
            // Check personal list
            String selectSql = "SELECT list_id FROM shopping_lists WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("list_id");
                }
            }
            // Create new personal list
            String insertSql = "INSERT INTO shopping_lists (user_id) VALUES (?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Retrieves the selected recipes for a given shopping list.
     */
    public List<Recipe> getSelectedRecipes(int listId, RecipeService recipeService) throws SQLException {
        List<Recipe> selected = new ArrayList<>();
        String sql = "SELECT recipe_id FROM shopping_list_recipes WHERE list_id = ?";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, listId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int recipeId = rs.getInt("recipe_id");
                    Recipe r = recipeService.getRecipeById(recipeId);
                    if (r != null) {
                        selected.add(r);
                    }
                }
            }
        }
        return selected;
    }

    /**
     * Persists the selected recipes list into the database by overwriting old selections.
     */
    public void saveSelectedRecipes(int listId, List<Integer> recipeIds) throws SQLException {
        Connection conn = DatabaseConnection.getInstance();
        conn.setAutoCommit(false);
        try {
            // Delete existing associations
            String deleteSql = "DELETE FROM shopping_list_recipes WHERE list_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setInt(1, listId);
                stmt.executeUpdate();
            }

            // Insert new associations
            if (recipeIds != null && !recipeIds.isEmpty()) {
                String insertSql = "INSERT INTO shopping_list_recipes (list_id, recipe_id) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    for (int recipeId : recipeIds) {
                        stmt.setInt(1, listId);
                        stmt.setInt(2, recipeId);
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Clears all recipe associations for a shopping list.
     */
    public void clearSelectedRecipes(int listId) throws SQLException {
        String sql = "DELETE FROM shopping_list_recipes WHERE list_id = ?";
        Connection conn = DatabaseConnection.getInstance();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, listId);
            stmt.executeUpdate();
        }
    }
}
