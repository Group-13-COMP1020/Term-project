package com.recipeapp;

import java.util.ArrayList;
import java.util.List;

public class Recipe {
    private int recipeId;
    private String title;
    private int prepTime;   // in minutes
    private int cookTime;   // in minutes
    private double rating;
    private int servings;
    private User author;
    private List<Ingredient> ingredients;
    private List<Tag> tags;

    public Recipe() {
        this.ingredients = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    public Recipe(int recipeId, String title, int prepTime, int cookTime, double rating, int servings, User author) {
        this.recipeId = recipeId;
        this.title = title;
        this.prepTime = prepTime;
        this.cookTime = cookTime;
        this.rating = rating;
        this.servings = servings;
        this.author = author;
        this.ingredients = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    public int getTotalTime() {
        return prepTime + cookTime;
    }

    public double getTotalPrice() {
        double total = 0;
        for (Ingredient ingredient : ingredients) {
            total += ingredient.getPriceEstimate();
        }
        return total;
    }

    public void addIngredient(Ingredient ingredient) {
        if (ingredient != null) {
            ingredients.add(ingredient);
        }
    }

    public void addTag(Tag tag) {
        if (tag != null) {
            tags.add(tag);
        }
    }

    // Getters and Setters
    public int getRecipeId() { return recipeId; }
    public void setRecipeId(int recipeId) { this.recipeId = recipeId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getPrepTime() { return prepTime; }
    public void setPrepTime(int prepTime) { this.prepTime = prepTime; }

    public int getCookTime() { return cookTime; }
    public void setCookTime(int cookTime) { this.cookTime = cookTime; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getServings() { return servings; }
    public void setServings(int servings) { this.servings = servings; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public List<Ingredient> getIngredients() { return ingredients; }
    public void setIngredients(List<Ingredient> ingredients) { this.ingredients = ingredients; }

    public List<Tag> getTags() { return tags; }
    public void setTags(List<Tag> tags) { this.tags = tags; }

    @Override
    public String toString() {
        return "Recipe{recipeId=" + recipeId + ", title='" + title + "', totalTime=" + getTotalTime() + " mins, rating=" + rating + "}";
    }
}
