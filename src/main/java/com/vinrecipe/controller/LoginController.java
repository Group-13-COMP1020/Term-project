package com.vinrecipe.controller;

import com.vinrecipe.model.User;
import com.vinrecipe.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for LoginView.fxml.
 * Handles login and three registration modes:
 *   - Simple sign-up (no room)
 *   - Room Leader (creates a room, receives access code)
 *   - Normal Student (joins a room via name + access code)
 */
public class LoginController {

    // ---- Shared fields (Login + Register) ----
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginBtn;
    @FXML private Button registerBtn;
    @FXML private Label toggleLabel;
    @FXML private Label welcomeTitle;
    @FXML private Label welcomeSubtitle;

    // ---- Register-only groups ----
    @FXML private VBox loginFieldsBox;
    @FXML private VBox registerFieldsBox;

    // ---- Register fields ----
    @FXML private TextField emailField; // kept for FXML compatibility but hidden

    // ---- Role toggles ----
    @FXML private ToggleButton roleLeaderBtn;
    @FXML private ToggleButton roleStudentBtn;

    // ---- Dynamic extra fields ----
    @FXML private VBox roomNameBox;
    @FXML private TextField roomNameField;
    @FXML private VBox accessCodeBox;
    @FXML private TextField accessCodeField;

    private final UserService userService = new UserService();
    private boolean isRegisterMode = false;

    /** "Leader", "Student", or null (neither selected yet) */
    private String selectedRole = null;

    @FXML
    public void initialize() {
        setRegisterMode(false);
    }

    /** Handle Login button. */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please enter username and password.", true);
            return;
        }

        User user = userService.login(username, password);
        if (user == null) {
            showStatus("Invalid username or password.", true);
            return;
        }

        navigateToMain(user);
    }

    /** Handle Create Account button. */
    @FXML
    private void handleRegister() {
        if (!isRegisterMode) {
            setRegisterMode(true);
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Username and password are required.", true);
            return;
        }

        if (selectedRole == null) {
            showStatus("Please choose: 🏠 Room Leader or 🎓 Join a Room.", true);
            return;
        }

        if ("Leader".equals(selectedRole)) {
            handleRegisterLeader(username, password);
        } else {
            handleRegisterStudent(username, password);
        }
    }

    private void handleRegisterLeader(String username, String password) {
        String roomName = roomNameField.getText().trim();
        if (roomName.isEmpty()) {
            showStatus("Please enter a name for your room.", true);
            return;
        }

        String accessCode = userService.registerLeader(username, password, roomName);
        if (accessCode == null) {
            String errorMsg = userService.getLastError();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "username may already be taken.";
            }
            showStatus("Registration failed — " + errorMsg, true);
            return;
        }

        // Show access code in a prominent styled dialog
        showAccessCodeDialog(username, roomName, accessCode);
        setRegisterMode(false);
        showStatus("Room created! Log in with your new account.", false);
    }

    private void handleRegisterStudent(String username, String password) {
        String roomName   = roomNameField.getText().trim();
        String accessCode = accessCodeField.getText().trim().toUpperCase();

        if (roomName.isEmpty() || accessCode.isEmpty()) {
            showStatus("Please enter room name and access code.", true);
            return;
        }

        int result = userService.registerStudent(username, password, roomName, accessCode);
        if (result == -1) {
            String errorMsg = userService.getLastError();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "username may already be taken.";
            }
            showStatus("Registration failed — " + errorMsg, true);
        } else if (result == -2) {
            String errorMsg = userService.getLastError();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "Room not found or invalid access code. Double-check with your leader.";
            }
            showStatus("❌ " + errorMsg, true);
        } else {
            setRegisterMode(false);
            showStatus("✅ Joined room \"" + roomName + "\"! You can now sign in.", false);
        }
    }

    /** Called when the role toggle buttons are clicked. */
    @FXML
    private void handleRoleToggle() {
        if (roleLeaderBtn.isSelected() && !roleStudentBtn.isSelected()) {
            selectedRole = "Leader";
            styleRoleBtn(roleLeaderBtn, true);
            styleRoleBtn(roleStudentBtn, false);
            showRoomFields(true, false);  // show room name, hide code
        } else if (roleStudentBtn.isSelected() && !roleLeaderBtn.isSelected()) {
            selectedRole = "Student";
            styleRoleBtn(roleStudentBtn, true);
            styleRoleBtn(roleLeaderBtn, false);
            showRoomFields(true, true);   // show both room name + code
        } else {
            // Toggled off — deselect both
            roleLeaderBtn.setSelected(false);
            roleStudentBtn.setSelected(false);
            selectedRole = null;
            styleRoleBtn(roleLeaderBtn, false);
            styleRoleBtn(roleStudentBtn, false);
            showRoomFields(false, false);
        }
    }

    /** Toggle between login and register mode. */
    @FXML
    private void toggleMode() {
        setRegisterMode(!isRegisterMode);
        statusLabel.setText("");
    }

    // ---- Helpers ----

    private void setRegisterMode(boolean register) {
        isRegisterMode = register;

        // Header labels
        welcomeTitle.setText(register ? "Create an Account ✨" : "Welcome Back! 👋");
        welcomeSubtitle.setText(register
            ? "Fill in your details below to get started."
            : "Please enter your details to sign in.");

        // Show/hide register-only group
        registerFieldsBox.setVisible(register);
        registerFieldsBox.setManaged(register);

        // Reset role selection on mode switch
        selectedRole = null;
        if (roleLeaderBtn != null) {
            roleLeaderBtn.setSelected(false);
            styleRoleBtn(roleLeaderBtn, false);
        }
        if (roleStudentBtn != null) {
            roleStudentBtn.setSelected(false);
            styleRoleBtn(roleStudentBtn, false);
        }
        showRoomFields(false, false);

        // Swap buttons
        loginBtn.setVisible(!register);
        loginBtn.setManaged(!register);
        registerBtn.setVisible(register);
        registerBtn.setManaged(register);

        toggleLabel.setText(register
            ? "Already have an account? Sign In"
            : "Don't have an account? Sign up");
    }

    private void showRoomFields(boolean showRoomName, boolean showCode) {
        if (roomNameBox == null) return;
        roomNameBox.setVisible(showRoomName);
        roomNameBox.setManaged(showRoomName);
        if (!showRoomName) roomNameField.clear();

        accessCodeBox.setVisible(showCode);
        accessCodeBox.setManaged(showCode);
        if (!showCode) accessCodeField.clear();
    }

    private void styleRoleBtn(ToggleButton btn, boolean active) {
        if (active) {
            btn.setStyle("-fx-background-color: #E76F51; -fx-border-color: #E76F51; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 16; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 16; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4A5568; -fx-cursor: hand;");
        }
    }

    private void showAccessCodeDialog(String username, String roomName, String accessCode) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("🏠 Room Created Successfully!");
        alert.setHeaderText("Welcome, " + username + "! Your room is ready.");

        String content = "Room Name:   " + roomName + "\n\n"
            + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
            + "    ACCESS CODE:  " + accessCode + "\n"
            + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
            + "Share this code with your roommates so they can join.\n"
            + "Log in now to manage your room!";
        alert.setContentText(content);

        // Style the dialog
        alert.getDialogPane().setStyle(
            "-fx-background-color: #FFFDF9; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12px; " +
            "-fx-background-radius: 12px;"
        );
        javafx.scene.control.Button okButton = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle(
                "-fx-background-color: #E76F51; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 6 20;"
            );
        }
        alert.showAndWait();
    }

    private void navigateToMain(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/layout/MainLayout.fxml"));
            Parent root = loader.load();
            MainController mainController = loader.getController();
            mainController.setCurrentUser(user);

            Stage stage = (Stage) loginBtn.getScene().getWindow();
            Scene scene = new Scene(root, 1100, 700);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/components.css").toExternalForm());
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.setTitle("VinRECIPE — " + user.getUsername());
        } catch (IOException e) {
            showStatus("Failed to load main window: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add(isError ? "status-error" : "status-success");
    }
}
