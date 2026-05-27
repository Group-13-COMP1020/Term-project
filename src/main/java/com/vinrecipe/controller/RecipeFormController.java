package com.vinrecipe.controller;

import com.vinrecipe.model.*;
import com.vinrecipe.service.RecipeService;
import com.vinrecipe.service.SearchService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for RecipeFormView.fxml.
 * Handles both Create and Edit modes for recipes.
 */
public class RecipeFormController implements ContextAware {

    @FXML private Label formTitleLabel;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private TextArea instructionsArea;
    @FXML private TextField prepTimeField;
    @FXML private TextField cookTimeField;
    @FXML private TextField imageUrlField;

    // Ingredient input row
    @FXML private TextField ingNameField;
    @FXML private TextField ingQtyField;
    @FXML private TextField ingUnitField;
    @FXML private TextField ingPriceField;

    @FXML private TableView<Ingredient> ingredientTable;
    @FXML private TableColumn<Ingredient, String> ingNameCol;
    @FXML private TableColumn<Ingredient, Double> ingQtyCol;
    @FXML private TableColumn<Ingredient, String> ingUnitCol;
    @FXML private TableColumn<Ingredient, Double> ingPriceCol;

    @FXML private ComboBox<Tag> tagCombo;
    @FXML private ListView<Tag> selectedTagsList;
    @FXML private Label statusLabel;

    private User currentUser;
    private RecipeService recipeService;
    private SearchService searchService;
    private MainController mainController;

    private Recipe editingRecipe = null; // null = create mode
    private final ObservableList<Ingredient> ingredients = FXCollections.observableArrayList();
    private final ObservableList<Tag> selectedTags      = FXCollections.observableArrayList();

    @Override
    public void setContext(User currentUser, RecipeService recipeService,
                           SearchService searchService, MainController mainController) {
        this.currentUser    = currentUser;
        this.recipeService  = recipeService;
        this.searchService  = searchService;
        this.mainController = mainController;

        setupTable();
        loadTags();
        formTitleLabel.setText("Add New Recipe");
    }

    /** Call this to switch to Edit mode for an existing recipe. */
    public void setEditMode(Recipe recipe) {
        this.editingRecipe = recipe;
        formTitleLabel.setText("Edit Recipe");
        titleField.setText(recipe.getTitle());
        descriptionArea.setText(recipe.getDescription() != null ? recipe.getDescription() : "");
        instructionsArea.setText(recipe.getInstructions() != null ? recipe.getInstructions() : "");
        prepTimeField.setText(String.valueOf(recipe.getPrepTime()));
        cookTimeField.setText(String.valueOf(recipe.getCookTime()));
        imageUrlField.setText(recipe.getImageUrl() != null ? recipe.getImageUrl() : "");
        ingredients.setAll(recipe.getIngredients());
        selectedTags.setAll(recipe.getTags());
    }

    private void setupTable() {
        ingNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        ingQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        ingUnitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        ingPriceCol.setCellValueFactory(new PropertyValueFactory<>("priceEstimate"));
        ingredientTable.setItems(ingredients);
        selectedTagsList.setItems(selectedTags);
    }

    private void loadTags() {
        List<Tag> tags = recipeService.getAllTags();
        tagCombo.setItems(FXCollections.observableArrayList(tags));
    }

    @FXML
    private void handleAddIngredient() {
        try {
            String name  = getSafeText(ingNameField);
            String qtyStr = getSafeText(ingQtyField);
            String unit  = getSafeText(ingUnitField);
            String priceStr = getSafeText(ingPriceField);
            
            if (name.isEmpty()) { showStatus("Ingredient name required", true); return; }
            if (qtyStr.isEmpty()) { showStatus("Ingredient quantity required", true); return; }
            
            double qty   = Double.parseDouble(qtyStr);
            double price = priceStr.isEmpty() ? 0 : Double.parseDouble(priceStr);
            
            Ingredient ing = new Ingredient(0, name, qty, unit, price);
            ingredients.add(ing);
            ingNameField.clear(); ingQtyField.clear(); ingUnitField.clear(); ingPriceField.clear();
        } catch (NumberFormatException e) {
            showStatus("Quantity and price must be valid numbers.", true);
        } catch (IllegalArgumentException e) {
            showStatus(e.getMessage(), true);
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleRemoveIngredient() {
        Ingredient selected = ingredientTable.getSelectionModel().getSelectedItem();
        if (selected != null) ingredients.remove(selected);
    }

    @FXML
    private void handleAddTag() {
        Tag selected = tagCombo.getValue();
        if (selected != null && !selectedTags.contains(selected)) {
            selectedTags.add(selected);
        }
    }

    @FXML
    private void handleRemoveTag() {
        Tag selected = selectedTagsList.getSelectionModel().getSelectedItem();
        if (selected != null) selectedTags.remove(selected);
    }

    @FXML
    private void handleSave() {
        try {
            String title = getSafeText(titleField);
            if (title.isEmpty()) { showStatus("Title is required", true); return; }
            
            String prepStr = getSafeText(prepTimeField);
            int prepTime = prepStr.isEmpty() ? 0 : Integer.parseInt(prepStr);
            
            String cookStr = getSafeText(cookTimeField);
            int cookTime = cookStr.isEmpty() ? 0 : Integer.parseInt(cookStr);
            
            int servings = 1;
            double rating = (editingRecipe != null) ? editingRecipe.getRating() : 5.0;

            Recipe recipe = editingRecipe != null ? editingRecipe : new Recipe();
            recipe.setTitle(title);
            recipe.setDescription(getSafeText(descriptionArea));
            recipe.setInstructions(getSafeText(instructionsArea));
            recipe.setPrepTime(prepTime);
            recipe.setCookTime(cookTime);
            recipe.setServings(servings);
            String imgUrl = getSafeText(imageUrlField);
            recipe.setImageUrl(imgUrl.isEmpty() ? null : imgUrl);
            recipe.setRating(rating);
            recipe.setAuthor(currentUser);
            recipe.setIngredients(new ArrayList<>(ingredients));
            recipe.setTags(new ArrayList<>(selectedTags));

            boolean success;
            if (editingRecipe != null) {
                success = recipeService.updateRecipe(recipe);
            } else {
                success = recipeService.createRecipe(recipe) != -1;
            }

            if (success) {
                searchService.buildIndex(filterRecipesByRoom(recipeService.getAllRecipes()));
                mainController.showRecipes();
            } else {
                showStatus("Failed to save recipe. Check input and try again.", true);
            }
        } catch (NumberFormatException e) {
            showStatus("Prep time and cook time must be whole numbers.", true);
        } catch (IllegalArgumentException e) {
            showStatus(e.getMessage(), true);
        } catch (Exception e) {
            e.printStackTrace();
            showStatus("An unexpected error occurred: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleCancel() {
        mainController.showRecipes();
    }

    private String getSafeText(TextField field) {
        if (field == null) return "";
        String val = field.getText();
        return val == null ? "" : val.trim();
    }

    private String getSafeText(TextArea area) {
        if (area == null) return "";
        String val = area.getText();
        return val == null ? "" : val.trim();
    }

    private void showStatus(String msg, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(msg);
            statusLabel.setStyle("-fx-text-fill: " + (isError ? "#E53E3E" : "#38A169") + ";");
        }
    }

    private int getUserRoomId(User user) {
        if (user instanceof NormalStudent) {
            return ((NormalStudent) user).getRoomId();
        } else if (user instanceof RoomLeader) {
            return ((RoomLeader) user).getRoomId();
        }
        return 0;
    }

    private List<Recipe> filterRecipesByRoom(List<Recipe> recipes) {
        if (recipes == null) return new ArrayList<>();
        if (currentUser instanceof Admin) {
            return new ArrayList<>(recipes);
        }
        
        int userRoomId = getUserRoomId(currentUser);
        List<Recipe> filtered = new ArrayList<>();
        for (Recipe r : recipes) {
            boolean isDefault = r.getRecipeId() <= 30 
                    || r.getAuthor() == null 
                    || "ADMIN".equalsIgnoreCase(r.getAuthor().getRole());
            
            if (isDefault) {
                filtered.add(r);
                continue;
            }
            
            int authorRoomId = getUserRoomId(r.getAuthor());
            if (userRoomId != 0 && userRoomId == authorRoomId) {
                filtered.add(r);
            }
        }
        return filtered;
    }
}
