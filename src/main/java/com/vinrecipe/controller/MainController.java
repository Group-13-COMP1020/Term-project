package com.vinrecipe.controller;

import com.vinrecipe.model.User;
import com.vinrecipe.service.RecipeService;
import com.vinrecipe.service.SearchService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

/**
 * Controller for MainLayout.fxml.
 * Manages the BorderPane shell, side navigation active styling, 
 * top unified global search bar, and dynamic profile initials circle.
 */
public class MainController {

    @FXML private BorderPane mainPane;
    @FXML private StackPane contentArea;

    // Top Header components
    @FXML private TextField topSearchField;
    @FXML private Label profileCircleLabel;
    @FXML private StackPane profileCirclePane;

    // Sidebar navigation buttons (for active styling)
    @FXML private Button dashboardBtn;
    @FXML private Button roomBtn;
    @FXML private Button shoppingBtn;
    @FXML private Button adminBtn;

    private User currentUser;
    private final RecipeService recipeService = new RecipeService();
    private final SearchService searchService = new SearchService();
    private RecipesController activeRecipesController = null;

    @FXML
    public void initialize() {
        // Dynamic search forwarding to active RecipesController
        topSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (activeRecipesController == null) {
                // Switch to Recipes/Dashboard view if we aren't already there
                showRecipes();
            }
            if (activeRecipesController != null) {
                activeRecipesController.setSearchQuery(newVal);
            }
        });
    }

    /** Called by LoginController after successful login. */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        
        // Load all recipes into search index
        searchService.buildIndex(recipeService.getAllRecipes());
        
        // Set dynamic initials in the profile circle
        if (user != null && user.getUsername() != null) {
            String name = user.getUsername();
            String initials = name.substring(0, Math.min(name.length(), 2)).toUpperCase();
            profileCircleLabel.setText(initials);
        }
        
        // Enable Admin button conditionally
        if (user instanceof com.vinrecipe.model.Admin) {
            adminBtn.setVisible(true);
            adminBtn.setManaged(true);
        }
        
        // Show recipes/dashboard by default
        showRecipes();
    }

    /** Navigation: go to Recipes/Dashboard. */
    @FXML
    public void showRecipes() {
        loadView("/fxml/views/RecipesView.fxml");
        setActiveButton(dashboardBtn); // Highlight Dashboard as active
    }

    /** Navigation: go to My Room. */
    @FXML
    private void showRoom() {
        loadView("/fxml/views/RoomView.fxml");
        setActiveButton(roomBtn);
    }

    /** Navigation: go to Shopping List. */
    @FXML
    private void showShoppingList() {
        loadView("/fxml/views/ShoppingListView.fxml");
        setActiveButton(shoppingBtn);
    }

    /** Navigation: go to Admin Control Panel. */
    @FXML
    private void showAdminPanel() {
        loadView("/fxml/views/AdminPanelView.fxml");
        setActiveButton(adminBtn);
    }



    /**
     * Load an FXML view into the center StackPane.
     */
    public void loadView(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.err.println("[MainController] FXML not found: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Node view = loader.load();
            
            // Inject context into child controller
            Object controller = loader.getController();
            if (controller instanceof ContextAware) {
                ((ContextAware) controller).setContext(currentUser, recipeService, searchService, this);
            }
            
            // Reference active RecipesController to bind global search field
            if (controller instanceof RecipesController) {
                this.activeRecipesController = (RecipesController) controller;
            } else {
                this.activeRecipesController = null;
            }
            
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("[MainController] Failed to load view " + fxmlPath + ": " + e.getMessage());
        }
    }

    /** Load a pre-built Node directly into the content area. */
    public void loadViewNode(Node view) {
        this.activeRecipesController = null;
        contentArea.getChildren().setAll(view);
    }

    private void setActiveButton(Button activeBtn) {
        Button[] btns = {dashboardBtn, roomBtn, shoppingBtn, adminBtn};
        for (Button btn : btns) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-btn-active");
                if (btn == activeBtn) {
                    btn.getStyleClass().add("nav-btn-active");
                }
            }
        }
    }

    public User getCurrentUser() { return currentUser; }
    public RecipeService getRecipeService() { return recipeService; }
    public SearchService getSearchService() { return searchService; }

    @FXML
    private void handleProfileClick(javafx.scene.input.MouseEvent event) {
        if (profileCirclePane == null) return;
        
        javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();
        
        // Custom styling for elegant drop down
        contextMenu.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 4);"
        );
        
        javafx.scene.control.MenuItem profileItem = new javafx.scene.control.MenuItem("Signed in as: " + (currentUser != null ? currentUser.getUsername() : "Guest"));
        profileItem.setDisable(true);
        profileItem.setStyle("-fx-font-weight: bold; -fx-text-fill: #2D3748; -fx-padding: 6 12;");
        
        javafx.scene.control.MenuItem logoutItem = new javafx.scene.control.MenuItem("Logout 🚪");
        logoutItem.setStyle("-fx-text-fill: #E76F51; -fx-font-weight: bold; -fx-padding: 6 12;");
        logoutItem.setOnAction(e -> handleLogout());
        
        contextMenu.getItems().addAll(profileItem, new javafx.scene.control.SeparatorMenuItem(), logoutItem);
        
        // Show below the profile circle pane
        contextMenu.show(profileCirclePane, javafx.geometry.Side.BOTTOM, 0, 4);
    }
    
    private void handleLogout() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/views/LoginView.fxml"));
            javafx.scene.Parent root = loader.load();
            
            javafx.stage.Stage stage = (javafx.stage.Stage) profileCirclePane.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 950, 580);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setTitle("VinRECIPE — Smart Recipe Planner");
            System.out.println("[Session] User logged out successfully.");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
