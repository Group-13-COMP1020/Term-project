package com.vinrecipe.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

<<<<<<< HEAD
/**
 * Shopping list that aggregates ingredients from selected recipes.
 * Uses HashMap to combine duplicate ingredients — key data structure per requirements.
 */
public class ShoppingList {

    private int listId;
    private User owner;
    private List<Recipe> selectedRecipes;             // selected recipes
    private Map<String, Double> aggregatedItems;      // ingredient name → total quantity (HashMap)
    private Map<String, String> itemUnits;            // ingredient name → unit
=======
public class ShoppingList {
    private int listId;
    private User owner;
    private List<Recipe> selectedRecipes;
    private Map<String, Double> aggregatedItems; // ingredient name -> total quantity
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3

    public ShoppingList() {
        this.selectedRecipes = new ArrayList<>();
        this.aggregatedItems = new HashMap<>();
<<<<<<< HEAD
        this.itemUnits = new HashMap<>();
=======
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    }

    public ShoppingList(int listId, User owner) {
        this.listId = listId;
        this.owner = owner;
        this.selectedRecipes = new ArrayList<>();
        this.aggregatedItems = new HashMap<>();
<<<<<<< HEAD
        this.itemUnits = new HashMap<>();
    }

    /** Add a recipe to the selection. */
    public void addRecipe(Recipe recipe) {
        if (recipe != null && !selectedRecipes.contains(recipe)) {
            selectedRecipes.add(recipe);
        }
    }

    /** Remove a recipe from the selection. */
    public void removeRecipe(Recipe recipe) {
        selectedRecipes.remove(recipe);
    }

    /**
     * Generate the aggregated shopping list from all selected recipes.
     * Uses HashMap.merge to sum quantities of same-named ingredients.
     *
     * @return Map of ingredient name → total quantity
     */
    public Map<String, Double> generateList() {
        aggregatedItems.clear();
        itemUnits.clear();
        for (Recipe recipe : selectedRecipes) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                String key = ingredient.getName(); // already lowercase from setter
                aggregatedItems.merge(key, ingredient.getQuantity(), Double::sum);
                // store unit (last one wins if mixed — acceptable for this scope)
                itemUnits.put(key, ingredient.getUnit());
            }
        }
        return aggregatedItems;
    }

    /** Clear all selections and aggregated data. */
    public void clearList() {
        selectedRecipes.clear();
        aggregatedItems.clear();
        itemUnits.clear();
    }

    // ---- Getters / Setters ----
=======
    }

    public void addRecipe(Recipe recipe) {
        if (recipe != null) {
            selectedRecipes.add(recipe);
            System.out.println("Recipe '" + recipe.getTitle() + "' added to shopping list.");
        }
    }

    public Map<String, Double> generateList() {
        aggregatedItems.clear();
        for (Recipe recipe : selectedRecipes) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                String name = ingredient.getName();
                double qty = ingredient.getQuantity();
                // Aggregate quantities for the same ingredient across recipes
                aggregatedItems.put(name, aggregatedItems.getOrDefault(name, 0.0) + qty);
            }
        }
        System.out.println("Shopping list generated with " + aggregatedItems.size() + " items.");
        return aggregatedItems;
    }

    public void clearList() {
        selectedRecipes.clear();
        aggregatedItems.clear();
        System.out.println("Shopping list cleared.");
    }

    // Getters and Setters
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    public int getListId() { return listId; }
    public void setListId(int listId) { this.listId = listId; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public List<Recipe> getSelectedRecipes() { return selectedRecipes; }
<<<<<<< HEAD
    public void setSelectedRecipes(List<Recipe> selectedRecipes) {
        this.selectedRecipes = selectedRecipes != null ? selectedRecipes : new ArrayList<>();
    }

    public Map<String, Double> getAggregatedItems() { return aggregatedItems; }

    public Map<String, String> getItemUnits() { return itemUnits; }
=======
    public void setSelectedRecipes(List<Recipe> selectedRecipes) { this.selectedRecipes = selectedRecipes; }

    public Map<String, Double> getAggregatedItems() { return aggregatedItems; }
    public void setAggregatedItems(Map<String, Double> aggregatedItems) { this.aggregatedItems = aggregatedItems; }

    @Override
    public String toString() {
        return "ShoppingList{listId=" + listId + ", owner=" + (owner != null ? owner.getUsername() : "none") + ", recipes=" + selectedRecipes.size() + "}";
    }
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
}
