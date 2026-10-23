package com.example.smartpantrymanager.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A recipe with its name, preparation steps and the list of ingredients it needs.
 * Maps to a row in the "recipes" table, plus its rows in "recipe_ingredients".
 */
public class Recipe {

    private long id;
    private String name;
    private String steps; // numbered steps separated by new lines
    private final List<RecipeIngredient> ingredients = new ArrayList<>();

    public Recipe(long id, String name, String steps) {
        this.id = id;
        this.name = name;
        this.steps = steps;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getSteps() { return steps; }
    public List<RecipeIngredient> getIngredients() { return ingredients; }

    public void addIngredient(RecipeIngredient ingredient) {
        ingredients.add(ingredient);
    }
}