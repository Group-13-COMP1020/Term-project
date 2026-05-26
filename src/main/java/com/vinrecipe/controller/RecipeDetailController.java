package com.vinrecipe.controller;

import com.vinrecipe.model.*;
import com.vinrecipe.service.RecipeService;
import com.vinrecipe.service.SearchService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 * Redesigned Controller for RecipeDetailView.fxml.
 * Displays a premium visual recipe detail layout, featuring high-fidelity hero images,
 * custom tag chips, structured ingredient cards, and visual step-by-step checklist cards.
 */
public class RecipeDetailController implements ContextAware {

    @FXML private Label titleLabel;
    @FXML private Label ratingLabel;
    
    // Detailed Meta Labels
    @FXML private Label prepTimeLabel;
    @FXML private Label cookTimeLabel;
    @FXML private Label totalTimeLabel;
    @FXML private Label servingsLabel;
    @FXML private Label authorLabel;
    
    @FXML private ImageView recipeImageView;
    @FXML private FlowPane tagsPane;
    @FXML private VBox stepsContainer;
    
    @FXML private ListView<String> ingredientList;
    
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Button saveBtn;

    private User currentUser;
    private RecipeService recipeService;
    private SearchService searchService;
    private MainController mainController;
    private Recipe recipe;

    @Override
    public void setContext(User currentUser, RecipeService recipeService,
                           SearchService searchService, MainController mainController) {
        this.currentUser    = currentUser;
        this.recipeService  = recipeService;
        this.searchService  = searchService;
        this.mainController = mainController;
    }

    /** Populate all fields with the given recipe. */
    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;

        titleLabel.setText(recipe.getTitle());
        ratingLabel.setText("★ " + String.format("%.1f", recipe.getRating()));
        
        // Detailed Meta values
        prepTimeLabel.setText("⏱ Prep: " + recipe.getPrepTime() + " min");
        cookTimeLabel.setText("🍳 Cook: " + recipe.getCookTime() + " min");
        totalTimeLabel.setText("⚡ Total: " + recipe.getTotalTime() + " min");
        servingsLabel.setText("👤 Servings: " + recipe.getServings());

        String authorName = recipe.getAuthor() != null ? recipe.getAuthor().getUsername() : "Unknown";
        authorLabel.setText("By: " + capitalize(authorName));

        // Load Recipe Image
        Image image = null;
        String imgUrl = recipe.getImageUrl();
        if (imgUrl != null && !imgUrl.isBlank()) {
            try {
                if (!imgUrl.startsWith("/")) {
                    imgUrl = "/" + imgUrl;
                }
                java.io.InputStream is = getClass().getResourceAsStream(imgUrl);
                if (is != null) {
                    image = new Image(is);
                }
            } catch (Exception e) {
                System.err.println("[RecipeDetailController] Error loading detail image: " + e.getMessage());
            }
        }
        
        if (image != null) {
            recipeImageView.setImage(image);
        } else {
            // Fallback
            try {
                java.io.InputStream is = getClass().getResourceAsStream("/images/recipe_placeholder.png");
                if (is != null) {
                    recipeImageView.setImage(new Image(is));
                }
            } catch (Exception e) {
                // no-op
            }
        }

        // Clip ImageView with rounded corners
        Rectangle clip = new Rectangle(280, 180);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        recipeImageView.setClip(clip);

        // Populate Tag Chips
        tagsPane.getChildren().clear();
        if (recipe.getTags().isEmpty()) {
            Label noTags = new Label("No tags");
            noTags.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic; -fx-font-size: 11px;");
            tagsPane.getChildren().add(noTags);
        } else {
            for (Tag tag : recipe.getTags()) {
                Label chip = new Label("#" + tag.getName());
                chip.setStyle(
                    "-fx-background-color: #E6F7ED; " +
                    "-fx-text-fill: #27AE60; " +
                    "-fx-background-radius: 12px; " +
                    "-fx-padding: 4 10; " +
                    "-fx-font-size: 11px; " +
                    "-fx-font-weight: bold;"
                );
                tagsPane.getChildren().add(chip);
            }
        }

        // Populate Ingredients List
        ingredientList.getItems().clear();
        for (Ingredient ing : recipe.getIngredients()) {
            ingredientList.getItems().add(
                    String.format("• %.1f %s  %s  (~%.0f VND)",
                            ing.getQuantity(), ing.getUnit(), capitalize(ing.getName()), ing.getPriceEstimate()));
        }

        // Populate Step-by-Step Cooking Checklist Cards
        stepsContainer.getChildren().clear();
        String inst = recipe.getInstructions();
        if (inst != null && !inst.isBlank()) {
            String[] lines = inst.split("\n");
            int stepNum = 1;
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                
                HBox stepCard = new HBox(12);
                stepCard.setAlignment(Pos.TOP_LEFT);
                stepCard.setPadding(new Insets(10, 14, 10, 14));
                stepCard.setStyle(
                    "-fx-background-color: #FFFDF9; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-border-color: #FFE0CC; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 10px;"
                );
                
                // Step Number Label
                Label numLbl = new Label(String.valueOf(stepNum));
                numLbl.setStyle(
                    "-fx-background-color: #E76F51; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-size: 12px; " +
                    "-fx-min-width: 24px; -fx-max-width: 24px; " +
                    "-fx-min-height: 24px; -fx-max-height: 24px; " +
                    "-fx-background-radius: 50%; " +
                    "-fx-alignment: center;"
                );
                
                // Step Text Label
                String cleanedLine = line.trim();
                if (cleanedLine.matches("^\\d+\\.\\s*.*")) {
                    cleanedLine = cleanedLine.replaceFirst("^\\d+\\.\\s*", "");
                }
                
                Label textLbl = new Label(cleanedLine);
                textLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #2D3748; -fx-wrap-text: true;");
                HBox.setHgrow(textLbl, Priority.ALWAYS);
                
                stepCard.getChildren().addAll(numLbl, textLbl);
                stepsContainer.getChildren().add(stepCard);
                stepNum++;
            }
        } else {
            Label noSteps = new Label("No instructions provided.");
            noSteps.setStyle("-fx-font-style: italic; -fx-text-fill: #718096;");
            stepsContainer.getChildren().add(noSteps);
        }

        // Show edit/delete only if user is author or admin
        boolean canModify = currentUser instanceof Admin
                || (recipe.getAuthor() != null
                    && recipe.getAuthor().getUserId() == currentUser.getUserId());
        editBtn.setVisible(canModify);
        deleteBtn.setVisible(canModify);

        // Update Save button state
        boolean isSaved = recipeService.isRecipeSaved(currentUser.getUserId(), recipe.getRecipeId());
        updateSaveBtnState(isSaved);
    }

    private void updateSaveBtnState(boolean isSaved) {
        if (isSaved) {
            saveBtn.setText("❤️ Saved");
            saveBtn.setStyle("-fx-background-color: #FFF2E6; -fx-text-fill: #E76F51; -fx-border-color: #FFE0CC; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            saveBtn.setText("🤍 Save");
            saveBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #718096; -fx-border-color: #E2E8F0; -fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 8 16; -fx-cursor: hand;");
        }
    }

    @FXML
    private void handleEdit() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/views/RecipeFormView.fxml"));
            javafx.scene.Node view = loader.load();
            RecipeFormController ctrl = loader.getController();
            ctrl.setContext(currentUser, recipeService, searchService, mainController);
            ctrl.setEditMode(recipe);
            mainController.loadViewNode(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSaveToggle() {
        boolean isSaved = recipeService.isRecipeSaved(currentUser.getUserId(), recipe.getRecipeId());
        if (isSaved) {
            recipeService.unsaveRecipe(currentUser.getUserId(), recipe.getRecipeId());
            updateSaveBtnState(false);
        } else {
            recipeService.saveRecipe(currentUser.getUserId(), recipe.getRecipeId());
            updateSaveBtnState(true);
        }
    }

    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                 "Delete \"" + recipe.getTitle() + "\"? This cannot be undone.",
                 ButtonType.YES, ButtonType.CANCEL);
        confirm.setHeaderText("Confirm Deletion");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                recipeService.deleteRecipe(recipe.getRecipeId());
                searchService.buildIndex(recipeService.getAllRecipes());
                mainController.loadView("/fxml/views/RecipesView.fxml");
            }
        });
    }

    @FXML
    private void handleBack() {
        mainController.loadView("/fxml/views/RecipesView.fxml");
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
