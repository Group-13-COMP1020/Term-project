package com.vinrecipe.controller;

import com.vinrecipe.model.*;
import com.vinrecipe.service.RecipeService;
import com.vinrecipe.service.SearchService;
import javafx.collections.FXCollections;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Combined Controller for RecipesView.fxml.
 * Handles Dashboard grid, multi-criteria Searching (inverted index + title), 
 * Filtering (tags), Sorting, Saved Recipes filtering, and the visual Inverted Index "Clear my Fridge" inventory search.
 */
public class RecipesController implements ContextAware {

    private String globalSearchQuery = "";
    @FXML private ComboBox<String> tagFilterCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ToggleButton savedToggleBtn;
    @FXML private Label userLabel;
    @FXML private Label statusLabel;
    @FXML private Label resultCountLabel;
    @FXML private FlowPane recipeGrid;

    // Quick Filter chips matching premium mockup
    @FXML private ToggleButton chipBreakfast;
    @FXML private ToggleButton chipLunch;
    @FXML private ToggleButton chipDinner;
    @FXML private ToggleButton chipSnacks;
    @FXML private ToggleButton chipDesserts;
    @FXML private ToggleButton chipVegan;
    @FXML private ToggleButton chipHighEnergy;

    // Clear my Fridge toggle chips
    @FXML private ToggleButton fEgg;
    @FXML private ToggleButton fAvocado;
    @FXML private ToggleButton fBread;
    @FXML private ToggleButton fChicken;
    @FXML private ToggleButton fTomato;
    @FXML private ToggleButton fSpinach;
    @FXML private ToggleButton fRice;
    @FXML private ToggleButton fPotato;
    @FXML private ToggleButton fTofu;
    @FXML private ToggleButton fRibs;
    @FXML private ToggleButton fBeef;

    // Track active fridge ingredients (Inverted Index keys)
    private final Set<String> fridgeIngredients = new HashSet<>();

    private User currentUser;
    private RecipeService recipeService;
    private SearchService searchService;
    private MainController mainController;

    private List<Recipe> allRecipes;
    private Set<Integer> savedRecipeIds = new HashSet<>();

    @Override
    public void setContext(User currentUser, RecipeService recipeService,
                           SearchService searchService, MainController mainController) {
        this.currentUser    = currentUser;
        this.recipeService  = recipeService;
        this.searchService  = searchService;
        this.mainController = mainController;

        userLabel.setText("Welcome, " + currentUser.getUsername() + "!");

        // Load Unique Tags
        List<String> tagOptions = new ArrayList<>();
        tagOptions.add("All Tags");
        tagOptions.addAll(searchService.getUniqueTagNames().stream().sorted().toList());
        tagFilterCombo.setItems(FXCollections.observableArrayList(tagOptions));
        tagFilterCombo.getSelectionModel().selectFirst();

        // Load Sort Options
        sortCombo.setItems(FXCollections.observableArrayList("Newest First", "Quickest", "Cheapest", "A-Z", "Z-A"));
        sortCombo.getSelectionModel().selectFirst();

        // Load recipes and apply search pipeline
        loadAndApply();
    }

    private void loadAndApply() {
        allRecipes = filterRecipesByRoom(recipeService.getAllRecipes());
        loadSavedRecipeIds();
        updateQuickFilterCounts();
        handleSearchAndFilter();
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

    private void loadSavedRecipeIds() {
        if (currentUser != null && recipeService != null) {
            try {
                savedRecipeIds = recipeService.getSavedRecipes(currentUser.getUserId()).stream()
                        .map(Recipe::getRecipeId)
                        .collect(Collectors.toCollection(HashSet::new));
            } catch (Exception e) {
                System.err.println("Error loading saved recipe IDs: " + e.getMessage());
                savedRecipeIds = new HashSet<>();
            }
        } else {
            savedRecipeIds = new HashSet<>();
        }
    }

    private void updateQuickFilterCounts() {
        if (allRecipes == null) return;
        
        long countBreakfast = 0;
        long countLunch = 0;
        long countDinner = 0;
        long countSnacks = 0;
        long countDesserts = 0;
        long countVegan = 0;
        long countHighEnergy = 0;
        
        for (Recipe r : allRecipes) {
            for (Tag t : r.getTags()) {
                String name = t.getName().trim().toLowerCase();
                if ("breakfast".equals(name)) countBreakfast++;
                else if ("lunch".equals(name)) countLunch++;
                else if ("dinner".equals(name)) countDinner++;
                else if ("snacks".equals(name)) countSnacks++;
                else if ("desserts".equals(name)) countDesserts++;
                else if ("vegan".equals(name)) countVegan++;
                else if ("high energy".equals(name)) countHighEnergy++;
            }
        }
        
        if (chipBreakfast != null) chipBreakfast.setText("Breakfast (" + countBreakfast + ")");
        if (chipLunch != null) chipLunch.setText("Lunch (" + countLunch + ")");
        if (chipDinner != null) chipDinner.setText("Dinner (" + countDinner + ")");
        if (chipSnacks != null) chipSnacks.setText("Snacks (" + countSnacks + ")");
        if (chipDesserts != null) chipDesserts.setText("Desserts (" + countDesserts + ")");
        if (chipVegan != null) chipVegan.setText("Vegan (" + countVegan + ")");
        if (chipHighEnergy != null) chipHighEnergy.setText("High Energy (" + countHighEnergy + ")");
    }

    /**
     * Core Search and Filter Pipeline.
     * Evaluates all UI search parameters and refreshes the recipe grid.
     */
    @FXML
    public void handleSearchAndFilter() {
        if (allRecipes == null) return;

        List<Recipe> filteredList = new ArrayList<>();

        // 1. Saved Recipes Filter using O(1) in-memory cached lookup
        if (savedToggleBtn.isSelected()) {
            filteredList = allRecipes.stream()
                    .filter(r -> savedRecipeIds.contains(r.getRecipeId()))
                    .collect(Collectors.toList());
        } else {
            filteredList = new ArrayList<>(allRecipes);
        }

        // 2. Unified Smart Search (Title, Ingredients, and Tags)
        String query = globalSearchQuery.trim().toLowerCase();
        if (!query.isEmpty()) {
            List<String> keywords = Arrays.stream(query.split("[\\s,]+"))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .toList();

            if (!keywords.isEmpty()) {
                Map<Recipe, Integer> recipeScores = new HashMap<>();

                for (Recipe recipe : filteredList) {
                    int score = 0;
                    String title = recipe.getTitle().toLowerCase();

                    // Check title matches (highest weight)
                    for (String kw : keywords) {
                        if (title.contains(kw)) {
                            score += 10;
                        }
                    }

                    // Check tag matches (medium weight)
                    for (Tag tag : recipe.getTags()) {
                        String tagName = tag.getName().toLowerCase();
                        for (String kw : keywords) {
                            if (tagName.contains(kw)) {
                                score += 5;
                            }
                        }
                    }

                    // Check ingredient matches (lower weight)
                    for (Ingredient ing : recipe.getIngredients()) {
                        String ingName = ing.getName().toLowerCase();
                        for (String kw : keywords) {
                            if (ingName.contains(kw)) {
                                score += 2;
                            }
                        }
                    }

                    if (score > 0) {
                        recipeScores.put(recipe, score);
                    }
                }

                // Filter list to only matched recipes, sorted by score descending
                filteredList = filteredList.stream()
                        .filter(recipeScores::containsKey)
                        .sorted((a, b) -> Integer.compare(recipeScores.get(b), recipeScores.get(a)))
                        .collect(Collectors.toList());
            }
        }

        // 3. Fridge Ingredients filter (Inverted Index narrow-down)
        if (!fridgeIngredients.isEmpty()) {
            List<Recipe> matches = searchService.searchByIngredients(new ArrayList<>(fridgeIngredients));
            Set<Integer> matchIds = matches.stream().map(Recipe::getRecipeId).collect(Collectors.toSet());
            
            filteredList = filteredList.stream()
                    .filter(r -> matchIds.contains(r.getRecipeId()))
                    .collect(Collectors.toList());
        }

        // 4. Tag Filter (supports both combo box and quick filter chips)
        String selectedTag = tagFilterCombo.getValue();
        
        List<String> chipTags = new ArrayList<>();
        if (chipBreakfast != null && chipBreakfast.isSelected()) chipTags.add("Breakfast");
        if (chipLunch != null && chipLunch.isSelected()) chipTags.add("Lunch");
        if (chipDinner != null && chipDinner.isSelected()) chipTags.add("Dinner");
        if (chipSnacks != null && chipSnacks.isSelected()) chipTags.add("Snacks");
        if (chipDesserts != null && chipDesserts.isSelected()) chipTags.add("Desserts");
        if (chipVegan != null && chipVegan.isSelected()) chipTags.add("Vegan");
        if (chipHighEnergy != null && chipHighEnergy.isSelected()) chipTags.add("High Energy");

        if (!chipTags.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(r -> r.getTags().stream().anyMatch(t -> {
                        for (String ct : chipTags) {
                            if (t.getName().equalsIgnoreCase(ct)) return true;
                        }
                        return false;
                    }))
                    .collect(Collectors.toList());
        } else if (selectedTag != null && !"All Tags".equals(selectedTag)) {
            filteredList = filteredList.stream()
                    .filter(r -> r.getTags().stream().anyMatch(t -> t.getName().equalsIgnoreCase(selectedTag)))
                    .collect(Collectors.toList());
        }

        // 5. Sorting Pipeline
        String selectedSort = sortCombo.getValue();
        
        // If there are active fridge ingredients, sort primarily by match percentage (descending)
        if (!fridgeIngredients.isEmpty()) {
            filteredList.sort((a, b) -> {
                double matchA = getMatchPercentage(a);
                double matchB = getMatchPercentage(b);
                if (Math.abs(matchA - matchB) > 0.0001) {
                    return Double.compare(matchB, matchA); // Descending by match percentage
                }
                // Tie-breaker: choose the one with the quickest total time (per proposal)
                return Integer.compare(a.getTotalTime(), b.getTotalTime());
            });
        } else {
            // Standard sorting when no fridge ingredients are selected
            filteredList = sortRecipesByCriteria(filteredList, selectedSort);
        }

        // Render Cards
        renderCards(filteredList);

        // Update Labels
        resultCountLabel.setText(filteredList.size() + " recipe(s) found");
        if (filteredList.isEmpty()) {
            statusLabel.setText("No recipes match your criteria.");
        } else {
            statusLabel.setText("");
        }
    }

    private double getMatchPercentage(Recipe recipe) {
        int totalIngs = recipe.getIngredients().size();
        if (totalIngs == 0) return 0.0;
        
        List<String> activeIngredients = new ArrayList<>(fridgeIngredients);
        if (globalSearchQuery != null && !globalSearchQuery.trim().isEmpty()) {
            activeIngredients.addAll(
                Arrays.stream(globalSearchQuery.trim().split("[\\s,]+"))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .toList()
            );
        }
        
        if (activeIngredients.isEmpty()) return 0.0;
        
        int matchCount = 0;
        for (Ingredient ing : recipe.getIngredients()) {
            for (String activeIng : activeIngredients) {
                if (ing.getName().toLowerCase().contains(activeIng.toLowerCase()) || 
                    activeIng.toLowerCase().contains(ing.getName().toLowerCase())) {
                    matchCount++;
                    break;
                }
            }
        }
        return (double) matchCount / totalIngs;
    }

    private int compareRecipesByCriteria(Recipe a, Recipe b, String criteria) {
        if ("Quickest".equals(criteria)) {
            return Integer.compare(a.getTotalTime(), b.getTotalTime());
        } else if ("Cheapest".equals(criteria)) {
            return Double.compare(a.getTotalPrice(), b.getTotalPrice());
        } else if ("A-Z".equals(criteria)) {
            return a.getTitle().compareToIgnoreCase(b.getTitle());
        } else if ("Z-A".equals(criteria)) {
            return b.getTitle().compareToIgnoreCase(a.getTitle());
        } else {
            return Integer.compare(b.getRecipeId(), a.getRecipeId());
        }
    }

    private List<Recipe> sortRecipesByCriteria(List<Recipe> list, String criteria) {
        List<Recipe> sorted = new ArrayList<>(list);
        if ("Quickest".equals(criteria)) {
            return searchService.sortByPrepTime(sorted);
        } else if ("Cheapest".equals(criteria)) {
            return searchService.sortByPrice(sorted);
        } else if ("A-Z".equals(criteria)) {
            return searchService.sortAlphabeticalAZ(sorted);
        } else if ("Z-A".equals(criteria)) {
            return searchService.sortAlphabeticalZA(sorted);
        } else {
            sorted.sort((a, b) -> Integer.compare(b.getRecipeId(), a.getRecipeId()));
            return sorted;
        }
    }

    @FXML
    private void handleChipAction() {
        updateChipStyle(chipBreakfast);
        updateChipStyle(chipLunch);
        updateChipStyle(chipDinner);
        updateChipStyle(chipSnacks);
        updateChipStyle(chipDesserts);
        updateChipStyle(chipVegan);
        updateChipStyle(chipHighEnergy);
        handleSearchAndFilter();
    }

    private void updateChipStyle(ToggleButton chip) {
        if (chip == null) return;
        if (chip.isSelected()) {
            chip.setStyle(
                "-fx-background-color: #E76F51; " +
                "-fx-border-color: #E76F51; " +
                "-fx-border-radius: 15px; " +
                "-fx-background-radius: 15px; " +
                "-fx-padding: 6 14; " +
                "-fx-font-size: 12px; " +
                "-fx-text-fill: #FFFFFF; " +
                "-fx-font-weight: bold; " +
                "-fx-cursor: hand;"
            );
        } else {
            chip.setStyle(
                "-fx-background-color: #FFFDF9; " +
                "-fx-border-color: #E2E8F0; " +
                "-fx-border-radius: 15px; " +
                "-fx-background-radius: 15px; " +
                "-fx-padding: 6 14; " +
                "-fx-font-size: 12px; " +
                "-fx-text-fill: #4A5568; " +
                "-fx-cursor: hand;"
            );
        }
    }

    @FXML
    private void handleFridgeToggle() {
        fridgeIngredients.clear();
        
        updateFridgeChip(fEgg, "egg");
        updateFridgeChip(fAvocado, "avocado");
        updateFridgeChip(fBread, "bread slice");
        updateFridgeChip(fChicken, "chicken breast");
        updateFridgeChip(fTomato, "tomatoes");
        updateFridgeChip(fSpinach, "spinach");
        updateFridgeChip(fRice, "cooked rice");
        updateFridgeChip(fPotato, "potato");
        updateFridgeChip(fTofu, "tofu block");
        updateFridgeChip(fRibs, "pork ribs");
        updateFridgeChip(fBeef, "beef slices");
        
        handleSearchAndFilter();
    }

    private void updateFridgeChip(ToggleButton chip, String ingName) {
        if (chip == null) return;
        if (chip.isSelected()) {
            fridgeIngredients.add(ingName);
            chip.setStyle(
                "-fx-background-color: #FFF2E6; " +
                "-fx-border-color: #E76F51; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; " +
                "-fx-padding: 6 12; " +
                "-fx-font-size: 11px; " +
                "-fx-text-fill: #E76F51; " +
                "-fx-font-weight: bold; " +
                "-fx-cursor: hand;"
            );
        } else {
            chip.setStyle(
                "-fx-background-color: #F8FAFC; " +
                "-fx-border-color: #E2E8F0; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; " +
                "-fx-padding: 6 12; " +
                "-fx-font-size: 11px; " +
                "-fx-text-fill: #4A5568; " +
                "-fx-cursor: hand;"
            );
        }
    }

    private void renderCards(List<Recipe> list) {
        recipeGrid.getChildren().clear();
        for (Recipe recipe : list) {
            recipeGrid.getChildren().add(buildCard(recipe));
        }
    }

    private String getDifficulty(Recipe recipe) {
        String title = recipe.getTitle().toLowerCase();
        if (title.contains("curry") || title.contains("pickles") || title.contains("ribs")) {
            return "Medium";
        }
        return "Easy";
    }

    private int getUserRoomId(User user) {
        if (user instanceof com.vinrecipe.model.NormalStudent) {
            return ((com.vinrecipe.model.NormalStudent) user).getRoomId();
        } else if (user instanceof com.vinrecipe.model.RoomLeader) {
            return ((com.vinrecipe.model.RoomLeader) user).getRoomId();
        }
        return 0;
    }

    private void addToShoppingList(Recipe recipe) {
        try {
            com.vinrecipe.dao.ShoppingListDAO shoppingListDAO = new com.vinrecipe.dao.ShoppingListDAO();
            int roomId = getUserRoomId(currentUser);
            int listId = shoppingListDAO.getOrCreateListId(roomId, currentUser.getUserId());
            
            // Query the current active plan date for this shopping list, fallback to today if null
            String activeDate = shoppingListDAO.getPlanDate(listId);
            if (activeDate == null || activeDate.trim().isEmpty()) {
                activeDate = java.time.LocalDate.now().toString();
                shoppingListDAO.updatePlanDate(listId, activeDate);
            }
            
            List<Recipe> existing = shoppingListDAO.getSelectedRecipesByDate(listId, activeDate, recipeService);
            boolean alreadyIn = existing.stream().anyMatch(r -> r.getRecipeId() == recipe.getRecipeId());
            if (!alreadyIn) {
                List<Integer> ids = new ArrayList<>();
                for (Recipe r : existing) {
                    ids.add(r.getRecipeId());
                }
                ids.add(recipe.getRecipeId());
                shoppingListDAO.saveSelectedRecipesByDate(listId, activeDate, ids);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION, 
                    recipe.getTitle() + " has been added to your shopping list!", ButtonType.OK);
                alert.setHeaderText("Shopping List Updated");
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, 
                    recipe.getTitle() + " is already in your shopping list.", ButtonType.OK);
                alert.setHeaderText("Already in Shopping List");
                alert.showAndWait();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to add recipe to shopping list: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    private void editRecipe(Recipe recipe) {
        boolean canModify = currentUser instanceof Admin
                || (recipe.getAuthor() != null
                    && recipe.getAuthor().getUserId() == currentUser.getUserId());
        if (!canModify) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "You can only edit recipes you created.", ButtonType.OK);
            alert.setHeaderText("Permission Denied");
            alert.showAndWait();
            return;
        }
        
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

    private void styleCircularButton(Button btn) {
        btn.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 50%; " +
            "-fx-min-width: 30px; " +
            "-fx-max-width: 30px; " +
            "-fx-min-height: 30px; " +
            "-fx-max-height: 30px; " +
            "-fx-text-fill: #333333; " +
            "-fx-font-size: 13px; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 0; " +
            "-fx-alignment: center; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 0, 1);"
        );
        
        btn.setOnMouseEntered(e -> {
            btn.setStyle(
                "-fx-background-color: #F8FAFC; " +
                "-fx-background-radius: 50%; " +
                "-fx-min-width: 30px; " +
                "-fx-max-width: 30px; " +
                "-fx-min-height: 30px; " +
                "-fx-max-height: 30px; " +
                "-fx-text-fill: #E76F51; " +
                "-fx-font-size: 13px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0; " +
                "-fx-alignment: center; " +
                "-fx-scale-x: 1.1; " +
                "-fx-scale-y: 1.1; " +
                "-fx-effect: dropshadow(gaussian, rgba(231,111,81,0.25), 6, 0, 0, 2);"
            );
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-background-radius: 50%; " +
                "-fx-min-width: 30px; " +
                "-fx-max-width: 30px; " +
                "-fx-min-height: 30px; " +
                "-fx-max-height: 30px; " +
                "-fx-text-fill: #333333; " +
                "-fx-font-size: 13px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0; " +
                "-fx-alignment: center; " +
                "-fx-scale-x: 1.0; " +
                "-fx-scale-y: 1.0; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 0, 1);"
            );
        });
    }

    /**
     * Programmatically constructs a Recipe Card VBox with hover overlays, images, and visual fridge matching percentages.
     */
    private VBox buildCard(Recipe recipe) {
        VBox card = new VBox(8);
        card.getStyleClass().add("recipe-card");
        card.setPrefWidth(215);
        card.setPadding(new Insets(12));
        card.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 16px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);"
        );

        // --- Image Container with Hover Action overlay ---
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(191, 140);
        imageContainer.setMaxSize(191, 140);
        
        ImageView imageView = new ImageView();
        imageView.setFitWidth(191);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(false);

        // Rounded corners clip
        Rectangle clip = new Rectangle(191, 140);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        imageView.setClip(clip);

        // Load image from resources
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
                System.err.println("[RecipesController] Could not load image from " + imgUrl + ": " + e.getMessage());
            }
        }

        if (image != null) {
            imageView.setImage(image);
        } else {
            // Try fallback
            try {
                java.io.InputStream is = getClass().getResourceAsStream("/images/recipe_placeholder.png");
                if (is != null) {
                    imageView.setImage(new Image(is));
                }
            } catch (Exception e) {
                // no-op
            }
        }

        // Overlay actions vertical stack
        VBox overlay = new VBox(6);
        overlay.setAlignment(Pos.TOP_RIGHT);
        overlay.setPadding(new Insets(8));
        overlay.setVisible(false); // hidden by default

        // Heart Button (Save/Bookmark) using O(1) in-memory cached lookup
        Button heartBtn = new Button();
        boolean isSaved = savedRecipeIds.contains(recipe.getRecipeId());
        heartBtn.setText(isSaved ? "★" : "☆");
        styleCircularButton(heartBtn);
        heartBtn.setOnAction(e -> {
            e.consume();
            boolean nowSaved = savedRecipeIds.contains(recipe.getRecipeId());
            if (nowSaved) {
                recipeService.unsaveRecipe(currentUser.getUserId(), recipe.getRecipeId());
                savedRecipeIds.remove(recipe.getRecipeId());
                heartBtn.setText("☆");
            } else {
                recipeService.saveRecipe(currentUser.getUserId(), recipe.getRecipeId());
                savedRecipeIds.add(recipe.getRecipeId());
                heartBtn.setText("★");
            }
            if (savedToggleBtn.isSelected()) {
                handleSearchAndFilter();
            }
        });

        // Plus Button (Add to Shopping List)
        Button plusBtn = new Button("+");
        styleCircularButton(plusBtn);
        plusBtn.setOnAction(e -> {
            e.consume();
            addToShoppingList(recipe);
        });

        // Edit Button (Edit Recipe details)
        Button editBtn = new Button("✎");
        styleCircularButton(editBtn);
        editBtn.setOnAction(e -> {
            e.consume();
            editRecipe(recipe);
        });

        overlay.getChildren().addAll(heartBtn, plusBtn, editBtn);
        imageContainer.getChildren().addAll(imageView, overlay);

        // --- Calculate and Draw dynamic Fridge Matching Percentage ---
        int matchCount = 0;
        int totalIngs = recipe.getIngredients().size();
        List<String> activeIngredients = new ArrayList<>(fridgeIngredients);
        if (!globalSearchQuery.isEmpty()) {
            activeIngredients.addAll(
                Arrays.stream(globalSearchQuery.split("[\\s,]+"))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .toList()
            );
        }

        if (!activeIngredients.isEmpty()) {
            for (Ingredient ing : recipe.getIngredients()) {
                for (String activeIng : activeIngredients) {
                    if (ing.getName().toLowerCase().contains(activeIng.toLowerCase()) || 
                        activeIng.toLowerCase().contains(ing.getName().toLowerCase())) {
                        matchCount++;
                        break;
                    }
                }
            }
        }

        if (matchCount > 0) {
            double percent = (double) matchCount / totalIngs;
            Label matchBadge = new Label(String.format("%.0f%% Match", percent * 100));
            matchBadge.setStyle(
                "-fx-background-color: " + (percent >= 0.99 ? "#27AE60" : "#E76F51") + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 10px; " +
                "-fx-background-radius: 8px; " +
                "-fx-padding: 4 8;"
            );
            StackPane.setAlignment(matchBadge, Pos.TOP_LEFT);
            StackPane.setMargin(matchBadge, new Insets(8));
            imageContainer.getChildren().add(matchBadge);
        }

        // --- Recipe Title ---
        Label title = new Label(recipe.getTitle());
        title.getStyleClass().add("card-title");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A1A1A;");
        title.setWrapText(true);
        title.setMinHeight(36); // standard height for 2 lines

        // --- Metadata Row ---
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        
        Label timeLabel = new Label("⏱ " + recipe.getTotalTime() + " min");
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D; -fx-font-weight: 500;");
        
        metaRow.getChildren().addAll(timeLabel);

        // --- Tag Chips Panel ---
        FlowPane tagsPane = new FlowPane(4, 4);
        tagsPane.setPrefWrapLength(190);
        for (Tag tag : recipe.getTags()) {
            Label chip = new Label(tag.getName());
            chip.setStyle(
                "-fx-background-color: #E6F7ED; " +
                "-fx-text-fill: #27AE60; " +
                "-fx-background-radius: 12px; " +
                "-fx-padding: 3 8; " +
                "-fx-font-size: 10px; " +
                "-fx-font-weight: bold;"
            );
            tagsPane.getChildren().add(chip);
        }

        card.getChildren().addAll(imageContainer, title, metaRow, tagsPane);

        // Card Hover effects
        card.setOnMouseEntered(e -> {
            overlay.setVisible(true);
            card.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-background-radius: 16px; " +
                "-fx-translate-y: -4; " +
                "-fx-effect: dropshadow(gaussian, rgba(46,204,113,0.18), 12, 0, 0, 4);"
            );
        });
        card.setOnMouseExited(e -> {
            overlay.setVisible(false);
            card.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-background-radius: 16px; " +
                "-fx-translate-y: 0; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);"
            );
        });

        // Click handler: navigate to detail view
        card.setOnMouseClicked(event -> showDetail(recipe));
        
        // Setup Hover preview tooltip for extra detail on desktop
        setupHoverTooltip(card, recipe);
        
        return card;
    }

    /**
     * Builds and installs a custom Tooltip providing a rich, responsive preview on hover.
     */
    private void setupHoverTooltip(Node node, Recipe recipe) {
        Tooltip tooltip = new Tooltip();
        VBox preview = new VBox(6);
        preview.setStyle("-fx-background-color: #2C3E50; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #34495E; -fx-border-radius: 8;");
        preview.setPrefWidth(240);

        Label tLabel = new Label(recipe.getTitle());
        tLabel.setStyle("-fx-text-fill: #2ECC71; -fx-font-weight: bold; -fx-font-size: 14px; -fx-wrap-text: true;");

        Label rLabel = new Label("⏱ " + recipe.getTotalTime() + " min");
        rLabel.setStyle("-fx-text-fill: #E76F51; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label dLabel = new Label(recipe.getDescription() != null && !recipe.getDescription().isBlank() ? recipe.getDescription() : "No description.");
        dLabel.setStyle("-fx-text-fill: #ECF0F1; -fx-font-size: 11px; -fx-wrap-text: true;");

        Label ingHeader = new Label("Ingredients Preview:");
        ingHeader.setStyle("-fx-text-fill: #BDC3C7; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 0 0 0;");

        VBox ingList = new VBox(2);
        int limit = Math.min(recipe.getIngredients().size(), 4);
        for (int i = 0; i < limit; i++) {
            Ingredient ing = recipe.getIngredients().get(i);
            Label ingL = new Label("• " + ing.getQuantity() + " " + ing.getUnit() + " " + ing.getName());
            ingL.setStyle("-fx-text-fill: #ECF0F1; -fx-font-size: 11px;");
            ingList.getChildren().add(ingL);
        }
        if (recipe.getIngredients().size() > 4) {
            Label moreL = new Label("and " + (recipe.getIngredients().size() - 4) + " more...");
            moreL.setStyle("-fx-text-fill: #95A5A6; -fx-font-style: italic; -fx-font-size: 10px;");
            ingList.getChildren().add(moreL);
        }

        preview.getChildren().addAll(tLabel, rLabel, dLabel, ingHeader, ingList);
        tooltip.setGraphic(preview);
        tooltip.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        tooltip.setShowDelay(Duration.millis(200));
        Tooltip.install(node, tooltip);
    }

    private void showDetail(Recipe recipe) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/RecipeDetailView.fxml"));
            Node view = loader.load();
            RecipeDetailController ctrl = loader.getController();
            ctrl.setContext(currentUser, recipeService, searchService, mainController);
            ctrl.setRecipe(recipe);
            mainController.loadViewNode(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddRecipe() {
        mainController.loadView("/fxml/views/RecipeFormView.fxml");
    }

    @FXML
    private void handleRefresh() {
        searchService.buildIndex(filterRecipesByRoom(recipeService.getAllRecipes()));
        loadAndApply();
    }

    @FXML
    private void handleClear() {
        globalSearchQuery = "";
        tagFilterCombo.getSelectionModel().selectFirst();
        sortCombo.getSelectionModel().selectFirst();
        savedToggleBtn.setSelected(false);
        
        // Reset quick filter chips
        if (chipBreakfast != null) chipBreakfast.setSelected(false);
        if (chipLunch != null) chipLunch.setSelected(false);
        if (chipDinner != null) chipDinner.setSelected(false);
        if (chipSnacks != null) chipSnacks.setSelected(false);
        if (chipDesserts != null) chipDesserts.setSelected(false);
        if (chipVegan != null) chipVegan.setSelected(false);
        if (chipHighEnergy != null) chipHighEnergy.setSelected(false);
        
        // Reset fridge chips
        if (fEgg != null) fEgg.setSelected(false);
        if (fAvocado != null) fAvocado.setSelected(false);
        if (fBread != null) fBread.setSelected(false);
        if (fChicken != null) fChicken.setSelected(false);
        if (fTomato != null) fTomato.setSelected(false);
        if (fSpinach != null) fSpinach.setSelected(false);
        if (fRice != null) fRice.setSelected(false);
        if (fPotato != null) fPotato.setSelected(false);
        if (fTofu != null) fTofu.setSelected(false);
        if (fRibs != null) fRibs.setSelected(false);
        if (fBeef != null) fBeef.setSelected(false);
        
        fridgeIngredients.clear();
        handleFridgeToggle(); // resets styles and triggers search
    }

    /** Expose public method to set search query from top global search field. */
    public void setSearchQuery(String text) {
        this.globalSearchQuery = (text == null) ? "" : text;
        handleSearchAndFilter();
    }

    /** Focus the ingredients search field for "Clear my Fridge" sidebar navigation. */
    public void focusIngredientsSearch() {
        // No-op (merged into unified search)
    }
}
