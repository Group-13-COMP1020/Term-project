package com.vinrecipe.controller;

import com.vinrecipe.dao.DatabaseConnection;
import com.vinrecipe.dao.UserDAO;
import com.vinrecipe.model.*;
import com.vinrecipe.service.RecipeService;
import com.vinrecipe.service.SearchService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * Controller for AdminPanelView.fxml.
 * Handles user management, account deletions, role shifts, and core metrics lookup.
 */
public class AdminPanelController implements ContextAware {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colUserId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colRoom;

    @FXML private Label selectedUserLabel;
    @FXML private ComboBox<String> roleCombo;
    @FXML private TextField roomIdField;
    @FXML private Label statusLabel;

    @FXML private VBox editorContainer;
    @FXML private VBox placeholderContainer;

    @FXML private Label kpiTotalUsers;
    @FXML private Label kpiTotalRooms;
    @FXML private Label kpiTotalRecipes;

    private User currentUser;
    private RecipeService recipeService;
    private SearchService searchService;
    private MainController mainController;

    private final UserDAO userDAO = new UserDAO();
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private User selectedUser = null;

    @FXML
    public void initialize() {
        // Set up table columns
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        colRoom.setCellValueFactory(cellData -> {
            User u = cellData.getValue();
            int rId = 0;
            if (u instanceof NormalStudent) rId = ((NormalStudent) u).getRoomId();
            else if (u instanceof RoomLeader) rId = ((RoomLeader) u).getRoomId();

            if (rId <= 0) return new SimpleStringProperty("None");
            try {
                String rName = userDAO.getRoomName(rId);
                return new SimpleStringProperty("ID " + rId + " (" + rName + ")");
            } catch (Exception e) {
                return new SimpleStringProperty("ID " + rId);
            }
        });

        userTable.setItems(userList);

        // Selection Listener
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateEditor(newVal);
            } else {
                clearEditor();
            }
        });

        // Populate Role Combo Box
        roleCombo.setItems(FXCollections.observableArrayList("ADMIN", "ROOM_LEADER", "NORMAL_STUDENT"));
    }

    @Override
    public void setContext(User currentUser, RecipeService recipeService,
                           SearchService searchService, MainController mainController) {
        this.currentUser    = currentUser;
        this.recipeService  = recipeService;
        this.searchService  = searchService;
        this.mainController = mainController;

        reloadAll();
    }

    private void reloadAll() {
        try {
            // Load Table
            List<User> allUsers = userDAO.findAll();
            userList.setAll(allUsers);

            // Load KPIs
            kpiTotalUsers.setText(String.valueOf(allUsers.size()));
            kpiTotalRooms.setText(String.valueOf(getCount("SELECT COUNT(*) FROM rooms")));
            kpiTotalRecipes.setText(String.valueOf(recipeService.getAllRecipes().size()));
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Failed to load ecosystem metrics: " + e.getMessage(), true);
        }
    }

    private void populateEditor(User user) {
        this.selectedUser = user;
        selectedUserLabel.setText(user.getUsername());
        roleCombo.setValue(user.getRole());

        int rId = 0;
        if (user instanceof NormalStudent) rId = ((NormalStudent) user).getRoomId();
        else if (user instanceof RoomLeader) rId = ((RoomLeader) user).getRoomId();
        roomIdField.setText(String.valueOf(rId));

        editorContainer.setDisable(false);
        editorContainer.setVisible(true);
        editorContainer.setManaged(true);
        placeholderContainer.setVisible(false);
        placeholderContainer.setManaged(false);
    }

    private void clearEditor() {
        this.selectedUser = null;
        selectedUserLabel.setText("No User Selected");
        roleCombo.setValue(null);
        roomIdField.clear();

        editorContainer.setDisable(true);
        editorContainer.setVisible(false);
        editorContainer.setManaged(false);
        placeholderContainer.setVisible(true);
        placeholderContainer.setManaged(true);
    }

    @FXML
    private void handleSaveUser() {
        if (selectedUser == null) return;

        try {
            String role = roleCombo.getValue();
            if (role == null) {
                showStatus("Security role selection is required.", true);
                return;
            }

            String rIdStr = roomIdField.getText();
            int roomId = rIdStr == null || rIdStr.trim().isEmpty() ? 0 : Integer.parseInt(rIdStr.trim());

            if (roomId < 0) {
                showStatus("Room ID must be a non-negative number.", true);
                return;
            }

            boolean success = userDAO.updateUserRole(selectedUser.getUserId(), role, roomId);
            if (success) {
                showStatus("Account settings for " + selectedUser.getUsername() + " updated successfully! ✅", false);
                userTable.getSelectionModel().clearSelection();
                reloadAll();
            } else {
                showStatus("Failed to update database record. Try again.", true);
            }
        } catch (NumberFormatException e) {
            showStatus("Room ID must be a valid whole number.", true);
        } catch (Exception e) {
            showStatus("Database error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDeleteUser() {
        if (selectedUser == null) return;

        if (selectedUser.getUserId() == currentUser.getUserId()) {
            showStatus("Operation denied: You cannot delete your own administrative session! ❌", true);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                 "Permanently delete user \"" + selectedUser.getUsername() + "\"? This cannot be undone.",
                 ButtonType.YES, ButtonType.CANCEL);
        confirm.setHeaderText("Confirm Administrative Account Deletion");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    boolean success = userDAO.deleteUser(selectedUser.getUserId());
                    if (success) {
                        showStatus("Account \"" + selectedUser.getUsername() + "\" has been deleted permanently. 🗑️", false);
                        userTable.getSelectionModel().clearSelection();
                        reloadAll();
                    } else {
                        showStatus("Failed to delete user from database.", true);
                    }
                } catch (Exception e) {
                    showStatus("Error: " + e.getMessage(), true);
                }
            }
        });
    }

    private int getCount(String sql) {
        try (Connection conn = DatabaseConnection.getInstance();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void showStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        if (isError) {
            statusLabel.setStyle("-fx-text-fill: #E76F51; -fx-font-weight: bold; -fx-font-size: 13px;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold; -fx-font-size: 13px;");
        }
    }
}
