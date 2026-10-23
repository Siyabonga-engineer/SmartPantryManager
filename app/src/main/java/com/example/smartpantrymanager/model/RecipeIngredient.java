package com.example.smartpantrymanager.model;

/**
 * One ingredient line inside a recipe, e.g. "3 pcs egg" or "250 g pasta".
 * Maps to a single row in the "recipe_ingredients" table.
 */
public class RecipeIngredient {

    private long id;
    private long recipeId;
    private String name;
    private double quantity;
    private String unit;

    /** Constructor used when seeding recipes (ids are assigned by the database). */
    public RecipeIngredient(String name, double quantity, String unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }

    /** Constructor used when loading an ingredient from the database. */
    public RecipeIngredient(long id, long recipeId, String name, double quantity, String unit) {
        this(name, quantity, unit);
        this.id = id;
        this.recipeId = recipeId;
    }

    public long getId() { return id; }
    public long getRecipeId() { return recipeId; }
    public String getName() { return name; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
}