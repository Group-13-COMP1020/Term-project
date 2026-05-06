package com.vinrecipe.model;

public class Ingredient {
    private int ingredientId;
    private String name;
    private double quantity;
    private String unit;
    private double priceEstimate;

    public Ingredient() {}

    public Ingredient(int ingredientId, String name, double quantity, String unit, double priceEstimate) {
        this.ingredientId = ingredientId;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.priceEstimate = priceEstimate;
    }

    // Getters and Setters
    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getPriceEstimate() { return priceEstimate; }
    public void setPriceEstimate(double priceEstimate) { this.priceEstimate = priceEstimate; }

    @Override
    public String toString() {
        return "Ingredient{ingredientId=" + ingredientId + ", name='" + name + "', quantity=" + quantity + " " + unit + ", price=" + priceEstimate + "}";
    }
}
