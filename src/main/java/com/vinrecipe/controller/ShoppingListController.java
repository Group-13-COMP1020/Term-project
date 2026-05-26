package com.vinrecipe.controller;

import com.vinrecipe.model.*;
import com.vinrecipe.dao.ShoppingListDAO;
import com.vinrecipe.service.RecipeService;
import com.vinrecipe.service.SearchService;
import com.vinrecipe.service.ShoppingListService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redesigned Controller for ShoppingListView.fxml.
 * Beautifully aggregates ingredients, categorizes with grocery emojis,
 * offers gamified completion tracking for lower attention span,
 * and maintains full room-wide persistent database sync.
 */
public class ShoppingListController implements ContextAware {

    @FXML private ListView<Recipe> recipeListView;
    @FXML private ListView<ShoppingListItem> shoppingListView;
    @FXML private Label totalPriceLabel;
    @FXML private Label instructionLabel;

    @FXML private VBox suggestionBox;
    @FXML private HBox suggestionPane;

    // Gamification progress trackers
    @FXML private ProgressBar completionProgress;
    @FXML private Label motivationLabel;

    private User currentUser;
    private RecipeService recipeService;
    private SearchService searchService;
    private MainController mainController;

    private final ShoppingListService shoppingListService = new ShoppingListService();
    private final ShoppingListDAO shoppingListDAO = new ShoppingListDAO();
    private int listId = -1;

    // Custom robust selection set to bypass JavaFX ListView multiple-selection event conflicts
    private final Set<Integer> selectedRecipeIds = new HashSet<>();

    public static class ShoppingListItem {
        private final String name;
        private final double quantity;
        private final String unit;
        private final double priceEstimate;
        private boolean checked;

        public ShoppingListItem(String name, double quantity, String unit, double priceEstimate) {
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
            this.priceEstimate = priceEstimate;
            this.checked = false;
        }

        public String getName() { return name; }
        public double getQuantity() { return quantity; }
        public String getUnit() { return unit; }
        public double getPriceEstimate() { return priceEstimate; }
        public boolean isChecked() { return checked; }
        public void setChecked(boolean checked) { this.checked = checked; }
    }

    @Override
    public void setContext(User currentUser, RecipeService recipeService,
                           SearchService searchService, MainController mainController) {
        this.currentUser    = currentUser;
        this.recipeService  = recipeService;
        this.searchService  = searchService;
        this.mainController = mainController;

        // Hide suggestions panel initially
        suggestionBox.setVisible(false);

        // Bind custom cell factories
        shoppingListView.setCellFactory(lv -> new ShoppingListCell());

        // Load room-wide or personal list ID from DB
        int roomId = getUserRoomId(currentUser);
        try {
            this.listId = shoppingListDAO.getOrCreateListId(roomId, currentUser.getUserId());
            List<Recipe> savedRecipes = shoppingListDAO.getSelectedRecipes(listId, recipeService);
            
            setupRecipeList(savedRecipes);

            if (!savedRecipes.isEmpty()) {
                handleGenerate();
            } else {
                instructionLabel.setText("Select recipes on the left to start");
                totalPriceLabel.setText("Estimated total: 0 VND");
                if (completionProgress != null) completionProgress.setProgress(0.0);
                if (motivationLabel != null) motivationLabel.setText("Select recipes to start! 🍳");
            }
        } catch (SQLException e) {
            instructionLabel.setText("⚠ Database error loading shopping list: " + e.getMessage());
            e.printStackTrace();
            setupRecipeList(new ArrayList<>());
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

    private void setupRecipeList(List<Recipe> savedRecipes) {
        List<Recipe> allRecipes = recipeService.getAllRecipes();
        recipeListView.getItems().setAll(allRecipes);
        recipeListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        recipeListView.setCellFactory(lv -> new RecipeCardCell());

        // Initialize our custom selected set!
        selectedRecipeIds.clear();
        for (Recipe r : savedRecipes) {
            selectedRecipeIds.add(r.getRecipeId());
        }
    }

    @FXML
    private void handleGenerate() {
        List<Recipe> selected = new ArrayList<>();
        for (Recipe r : recipeListView.getItems()) {
            if (selectedRecipeIds.contains(r.getRecipeId())) {
                selected.add(r);
            }
        }

        if (selected.isEmpty()) {
            instructionLabel.setText("Select recipes on the left to start");
            shoppingListView.getItems().clear();
            totalPriceLabel.setText("Estimated total: 0 VND");
            suggestionBox.setVisible(false);
            if (completionProgress != null) completionProgress.setProgress(0.0);
            if (motivationLabel != null) motivationLabel.setText("Select recipes to start! 🍳");
            try {
                if (listId != -1) {
                    shoppingListDAO.clearSelectedRecipes(listId);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }

        // Save selected recipes in DB
        if (listId != -1) {
            List<Integer> selectedIds = selected.stream().map(Recipe::getRecipeId).toList();
            try {
                shoppingListDAO.saveSelectedRecipes(listId, selectedIds);
            } catch (SQLException e) {
                System.err.println("[ShoppingListController] Error saving list selections: " + e.getMessage());
            }
        }

        ShoppingList list = shoppingListService.generate(selected);
        
        // Convert to ShoppingListItem objects
        List<ShoppingListItem> displayItems = new ArrayList<>();
        Map<String, Double> aggregated = list.getAggregatedItems();
        Map<String, String> units      = list.getItemUnits();
        
        for (Map.Entry<String, Double> entry : aggregated.entrySet()) {
            String name = entry.getKey();
            double qty  = entry.getValue();
            String unit = units.getOrDefault(name, "");
            
            // Find estimated price from ingredients
            double priceEst = 0;
            for (Recipe r : selected) {
                for (Ingredient ing : r.getIngredients()) {
                    if (ing.getName().equalsIgnoreCase(name)) {
                        priceEst = ing.getPriceEstimate();
                        break;
                    }
                }
            }
            displayItems.add(new ShoppingListItem(name, qty, unit, priceEst));
        }

        shoppingListView.getItems().setAll(displayItems);
        sortShoppingList();
        updateProgress();

        // Calculate total estimated price
        double total = selected.stream().mapToDouble(Recipe::getTotalPrice).sum();
        totalPriceLabel.setText(String.format("Estimated total: %.0f VND", total));
        instructionLabel.setText("Shopping list generated for " + selected.size() + " recipe(s)");

        // Update smart recipe recommendations
        updateSmartSuggestions(list, selected);
    }

    private void sortShoppingList() {
        List<ShoppingListItem> items = new ArrayList<>(shoppingListView.getItems());
        items.sort((a, b) -> {
            if (a.isChecked() != b.isChecked()) {
                return Boolean.compare(a.isChecked(), b.isChecked());
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });
        shoppingListView.getItems().setAll(items);
    }

    private void updateProgress() {
        List<ShoppingListItem> items = shoppingListView.getItems();
        if (items.isEmpty()) {
            if (completionProgress != null) completionProgress.setProgress(0.0);
            if (motivationLabel != null) motivationLabel.setText("Select recipes to start! 🍳");
            return;
        }
        long checkedCount = items.stream().filter(ShoppingListItem::isChecked).count();
        double percent = (double) checkedCount / items.size();
        if (completionProgress != null) completionProgress.setProgress(percent);
        
        if (motivationLabel != null) {
            if (checkedCount == 0) {
                motivationLabel.setText("Let's get shopping! 🛒");
            } else if (percent < 0.5) {
                motivationLabel.setText("Off to a great start! 🚀 (" + checkedCount + "/" + items.size() + ")");
            } else if (percent < 1.0) {
                motivationLabel.setText("Over halfway there! 🍳 (" + checkedCount + "/" + items.size() + ")");
            } else {
                motivationLabel.setText("All done! Ready to cook! 🎉");
            }
        }
    }

    private String getEmojiForIngredient(String name) {
        name = name.toLowerCase().trim();
        if (name.contains("apple") || name.contains("fruit")) return "🍎";
        if (name.contains("avocado")) return "🥑";
        if (name.contains("egg")) return "🥚";
        if (name.contains("bread") || name.contains("toast")) return "🍞";
        if (name.contains("chicken")) return "🍗";
        if (name.contains("beef") || name.contains("steak")) return "🥩";
        if (name.contains("pork") || name.contains("ribs")) return "🥓";
        if (name.contains("potato") || name.contains("fry") || name.contains("fries")) return "🍟";
        if (name.contains("tofu")) return "🍲";
        if (name.contains("tomato")) return "🍅";
        if (name.contains("pumpkin") || name.contains("soup")) return "🎃";
        if (name.contains("rice")) return "🍚";
        if (name.contains("pickle")) return "🥒";
        if (name.contains("gimbap") || name.contains("seaweed") || name.contains("roll")) return "🍣";
        if (name.contains("salt") || name.contains("pepper") || name.contains("sugar") || name.contains("spice")) return "🧂";
        if (name.contains("onion") || name.contains("garlic")) return "🧄";
        if (name.contains("scallion") || name.contains("spinach") || name.contains("carrot") || name.contains("vegetable")) return "🥕";
        if (name.contains("milk") || name.contains("cream") || name.contains("cheese") || name.contains("butter")) return "🥛";
        if (name.contains("pasta") || name.contains("noodle")) return "🍝";
        if (name.contains("sauce") || name.contains("vinegar") || name.contains("oil")) return "🍶";
        return "🛒";
    }

    @FXML
    private void handleAddItem() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Custom Item");
        dialog.setHeaderText("Add an ingredient manually to your list");
        dialog.setContentText("Ingredient name:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                ShoppingListItem customItem = new ShoppingListItem(name.trim(), 1.0, "pcs", 0.0);
                shoppingListView.getItems().add(customItem);
                sortShoppingList();
                updateProgress();
            }
        });
    }

    /**
     * Finds other recipes sharing ingredients currently in the shopping list, 
     * showing them as clickable suggestions.
     */
    private void updateSmartSuggestions(ShoppingList list, List<Recipe> selected) {
        Set<String> listIngredients = list.getAggregatedItems().keySet().stream()
                .map(String::toLowerCase)
                .map(String::trim)
                .collect(Collectors.toSet());

        Set<Integer> selectedIds = selected.stream().map(Recipe::getRecipeId).collect(Collectors.toSet());

        List<Recipe> allRecipes = recipeService.getAllRecipes();
        List<Recipe> candidates = allRecipes.stream()
                .filter(r -> !selectedIds.contains(r.getRecipeId()))
                .toList();

        class ScoredRecipe {
            final Recipe recipe;
            final int score;
            ScoredRecipe(Recipe recipe, int score) { this.recipe = recipe; this.score = score; }
        }

        List<ScoredRecipe> scored = new ArrayList<>();
        for (Recipe r : candidates) {
            int score = 0;
            for (Ingredient ing : r.getIngredients()) {
                if (listIngredients.contains(ing.getName().toLowerCase().trim())) {
                    score++;
                }
            }
            if (score > 0) {
                scored.add(new ScoredRecipe(r, score));
            }
        }

        scored.sort((a, b) -> Integer.compare(b.score, a.score));

        suggestionPane.getChildren().clear();

        if (scored.isEmpty()) {
            suggestionBox.setVisible(false);
        } else {
            suggestionBox.setVisible(true);
            int count = Math.min(scored.size(), 3);
            for (int i = 0; i < count; i++) {
                Recipe r = scored.get(i).recipe;
                int matches = scored.get(i).score;

                Button suggestBtn = new Button("➕ " + r.getTitle() + " (" + matches + " matches)");
                suggestBtn.getStyleClass().add("btn-outline");
                suggestBtn.setStyle("-fx-font-size: 11px; -fx-text-fill: #E76F51; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-color: #FFF2E6; -fx-border-color: #FFE0CC; -fx-background-radius: 12px; -fx-border-radius: 12px;");
                suggestBtn.setOnAction(e -> {
                    selectedRecipeIds.add(r.getRecipeId());
                    recipeListView.refresh();
                    handleGenerate();
                });
                suggestionPane.getChildren().add(suggestBtn);
            }
        }
    }

    @FXML
    private void handleClear() {
        selectedRecipeIds.clear();
        recipeListView.refresh();
        shoppingListView.getItems().clear();
        totalPriceLabel.setText("Estimated total: 0 VND");
        suggestionBox.setVisible(false);
        instructionLabel.setText("Select recipes on the left to start");
        if (completionProgress != null) completionProgress.setProgress(0.0);
        if (motivationLabel != null) motivationLabel.setText("Select recipes to start! 🍳");

        try {
            if (listId != -1) {
                shoppingListDAO.clearSelectedRecipes(listId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Inner RecipeCardCell mimicking active "Weekly Groceries" mockup card styling. */
    private class RecipeCardCell extends ListCell<Recipe> {
        private final HBox container = new HBox(12);
        private final Label selectCircle = new Label();
        private final VBox textContainer = new VBox(4);
        private final Label recipeTitle = new Label();
        private final Label recipeMeta = new Label();

        RecipeCardCell() {
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(12, 16, 12, 16));
            
            selectCircle.setStyle(
                "-fx-min-width: 20px; -fx-max-width: 20px; " +
                "-fx-min-height: 20px; -fx-max-height: 20px; " +
                "-fx-background-radius: 50%; " +
                "-fx-border-color: #A0AEC0; -fx-border-width: 2px; " +
                "-fx-background-color: transparent;"
            );
            
            recipeTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2D3748;");
            recipeMeta.setStyle("-fx-font-size: 11px; -fx-text-fill: #718096;");
            
            textContainer.getChildren().addAll(recipeTitle, recipeMeta);
            container.getChildren().addAll(selectCircle, textContainer);

            // Toggle selection in our custom Set on click
            setOnMouseClicked(e -> {
                if (!isEmpty()) {
                    Recipe recipe = getItem();
                    if (selectedRecipeIds.contains(recipe.getRecipeId())) {
                        selectedRecipeIds.remove(recipe.getRecipeId());
                    } else {
                        selectedRecipeIds.add(recipe.getRecipeId());
                    }
                    
                    // Refresh the recipe list view to redraw all cells!
                    recipeListView.refresh();
                    
                    // Generate shopping list
                    handleGenerate();
                }
            });
        }

        @Override
        protected void updateItem(Recipe recipe, boolean empty) {
            super.updateItem(recipe, empty);
            if (empty || recipe == null) {
                setGraphic(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            } else {
                recipeTitle.setText(recipe.getTitle());
                recipeMeta.setText("⏱ " + recipe.getTotalTime() + " min  |  👤 " + recipe.getServings() + " serving");
                
                boolean isSelected = selectedRecipeIds.contains(recipe.getRecipeId());
                if (isSelected) {
                    selectCircle.setStyle(
                        "-fx-min-width: 20px; -fx-max-width: 20px; " +
                        "-fx-min-height: 20px; -fx-max-height: 20px; " +
                        "-fx-background-radius: 50%; " +
                        "-fx-background-color: #E76F51; " +
                        "-fx-alignment: center; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;"
                    );
                    selectCircle.setText("✓");
                    
                    setStyle(
                        "-fx-background-color: #FFF2E6; " +
                        "-fx-border-color: #E76F51; " +
                        "-fx-border-width: 1.5px; " +
                        "-fx-border-radius: 12px; " +
                        "-fx-background-radius: 12px; " +
                        "-fx-padding: 2; " +
                        "-fx-margin: 4 0; " +
                        "-fx-effect: dropshadow(gaussian, rgba(231,111,81,0.1), 4, 0, 0, 1);"
                    );
                    recipeTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #E76F51;");
                } else {
                    selectCircle.setStyle(
                        "-fx-min-width: 20px; -fx-max-width: 20px; " +
                        "-fx-min-height: 20px; -fx-max-height: 20px; " +
                        "-fx-background-radius: 50%; " +
                        "-fx-border-color: #A0AEC0; -fx-border-width: 2px; " +
                        "-fx-background-color: transparent;"
                    );
                    selectCircle.setText("");
                    
                    setStyle(
                        "-fx-background-color: #FFFFFF; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 12px; " +
                        "-fx-background-radius: 12px; " +
                        "-fx-padding: 2; " +
                        "-fx-margin: 4 0;"
                    );
                    recipeTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2D3748;");
                }
                setGraphic(container);
            }
        }
    }

    /** Inner ShoppingListCell for the aggregated grocery checklist. */
    private class ShoppingListCell extends ListCell<ShoppingListItem> {
        private final HBox container = new HBox(12);
        private final StackPane checkCircle = new StackPane();
        private final Label checkMark = new Label();
        private final VBox textContainer = new VBox(2);
        private final HBox nameRow = new HBox(6);
        private final Label itemEmoji = new Label();
        private final Label itemName = new Label();
        private final Label itemQty = new Label();
        private final Region spacer = new Region();
        private final Button deleteBtn = new Button("✕");

        ShoppingListCell() {
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(8, 12, 8, 12));
            
            checkCircle.setPrefSize(24, 24);
            checkCircle.setMinSize(24, 24);
            checkCircle.setMaxSize(24, 24);
            
            checkMark.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
            checkCircle.getChildren().add(checkMark);
            
            nameRow.setAlignment(Pos.CENTER_LEFT);
            itemEmoji.setStyle("-fx-font-size: 16px;");
            itemName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2D3748;");
            nameRow.getChildren().addAll(itemEmoji, itemName);
            
            itemQty.setStyle("-fx-font-size: 11px; -fx-text-fill: #718096;");
            textContainer.getChildren().addAll(nameRow, itemQty);
            
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #A0AEC0; -fx-font-size: 12px; -fx-cursor: hand;");
            deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E53E3E; -fx-font-size: 12px; -fx-cursor: hand;"));
            deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #A0AEC0; -fx-font-size: 12px; -fx-cursor: hand;"));
            
            HBox.setHgrow(spacer, Priority.ALWAYS);
            container.getChildren().addAll(checkCircle, textContainer, spacer, deleteBtn);

            checkCircle.setOnMouseClicked(e -> {
                if (!isEmpty()) {
                    ShoppingListItem item = getItem();
                    item.setChecked(!item.isChecked());
                    sortShoppingList();
                    updateProgress();
                    e.consume();
                }
            });
            
            setOnMouseClicked(e -> {
                if (!isEmpty() && e.getClickCount() == 1) {
                    ShoppingListItem item = getItem();
                    item.setChecked(!item.isChecked());
                    sortShoppingList();
                    updateProgress();
                }
            });

            deleteBtn.setOnAction(e -> {
                if (!isEmpty()) {
                    shoppingListView.getItems().remove(getItem());
                    updateProgress();
                }
            });
        }

        @Override
        protected void updateItem(ShoppingListItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            } else {
                itemName.setText(item.getName());
                itemQty.setText(String.format("%.1f %s", item.getQuantity(), item.getUnit()));
                itemEmoji.setText(getEmojiForIngredient(item.getName()));
                
                if (item.isChecked()) {
                    checkCircle.setStyle(
                        "-fx-background-color: #E76F51; " +
                        "-fx-background-radius: 50%;"
                    );
                    checkMark.setText("✓");
                    
                    setStyle(
                        "-fx-background-color: #F8FAFC; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 12px; " +
                        "-fx-background-radius: 12px; " +
                        "-fx-padding: 2; " +
                        "-fx-margin: 4 0; " +
                        "-fx-opacity: 0.75;"
                    );
                    itemName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #A0AEC0; -fx-strikethrough: true;");
                } else {
                    checkCircle.setStyle(
                        "-fx-background-color: transparent; " +
                        "-fx-border-color: #A0AEC0; " +
                        "-fx-border-width: 2px; " +
                        "-fx-background-radius: 50%;"
                    );
                    checkMark.setText("");
                    
                    setStyle(
                        "-fx-background-color: #FFFFFF; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 12px; " +
                        "-fx-background-radius: 12px; " +
                        "-fx-padding: 2; " +
                        "-fx-margin: 4 0; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.02), 4, 0, 0, 1);"
                    );
                    itemName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2D3748; -fx-strikethrough: false;");
                }
                setGraphic(container);
            }
        }
    }
}
