package com.vinrecipe.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingList {
    private int listId;
    private User owner;
    private List<Recipe> selectedRecipes;
    private Map<String, Double> aggregatedItems; // ingredient name -> total quantity

    public ShoppingList() {
        this.selectedRecipes = new ArrayList<>();
        this.aggregatedItems = new HashMap<>();
    }

    public ShoppingList(int listId, User owner) {
        this.listId = listId;
        this.owner = owner;
        this.selectedRecipes = new ArrayList<>();
        this.aggregatedItems = new HashMap<>();
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
    public int getListId() { return listId; }
    public void setListId(int listId) { this.listId = listId; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public List<Recipe> getSelectedRecipes() { return selectedRecipes; }
    public void setSelectedRecipes(List<Recipe> selectedRecipes) { this.selectedRecipes = selectedRecipes; }

    public Map<String, Double> getAggregatedItems() { return aggregatedItems; }
    public void setAggregatedItems(Map<String, Double> aggregatedItems) { this.aggregatedItems = aggregatedItems; }

    @Override
    public String toString() {
        return "ShoppingList{listId=" + listId + ", owner=" + (owner != null ? owner.getUsername() : "none") + ", recipes=" + selectedRecipes.size() + "}";
    }
}
