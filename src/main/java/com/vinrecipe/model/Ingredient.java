package com.vinrecipe.model;

<<<<<<< HEAD
/**
 * Represents a single ingredient in a recipe.
 * POJO with encapsulated fields and getters/setters.
 */
public class Ingredient {

=======
public class Ingredient {
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    private int ingredientId;
    private String name;
    private double quantity;
    private String unit;
<<<<<<< HEAD
    private double priceEstimate; // in VND or USD
=======
    private double priceEstimate;
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3

    public Ingredient() {}

    public Ingredient(int ingredientId, String name, double quantity, String unit, double priceEstimate) {
        this.ingredientId = ingredientId;
<<<<<<< HEAD
        setName(name);
        setQuantity(quantity);
        this.unit = unit;
        setPriceEstimate(priceEstimate);
    }

    // ---- Getters / Setters with validation ----
=======
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.priceEstimate = priceEstimate;
    }

    // Getters and Setters
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public String getName() { return name; }
<<<<<<< HEAD
    public void setName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Ingredient name must not be blank");
        this.name = name.trim().toLowerCase();
    }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Quantity must be non-negative");
        this.quantity = quantity;
    }
=======
    public void setName(String name) { this.name = name; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getPriceEstimate() { return priceEstimate; }
<<<<<<< HEAD
    public void setPriceEstimate(double priceEstimate) {
        if (priceEstimate < 0) throw new IllegalArgumentException("Price estimate must be non-negative");
        this.priceEstimate = priceEstimate;
    }

    @Override
    public String toString() {
        return quantity + " " + unit + " " + name;
=======
    public void setPriceEstimate(double priceEstimate) { this.priceEstimate = priceEstimate; }

    @Override
    public String toString() {
        return "Ingredient{ingredientId=" + ingredientId + ", name='" + name + "', quantity=" + quantity + " " + unit + ", price=" + priceEstimate + "}";
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    }
}
