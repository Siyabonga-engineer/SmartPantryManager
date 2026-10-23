package com.example.smartpantrymanager.logic;

import com.example.smartpantrymanager.model.PantryItem;
import com.example.smartpantrymanager.model.Recipe;
import com.example.smartpantrymanager.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * THE STRICT-MATCHING RULE (Section 2.3 of the brief).
 *
 * A recipe is "suggested" only if EVERY ingredient it needs is in the pantry
 * in AT LEAST the required quantity. Missing even one ingredient, or having
 * too little of one, means the recipe is NOT suggested.
 *
 * To cope with real-world messiness the matcher:
 *  1. Normalises names: "Tomatoes " -> "tomato", "Eggs" -> "egg", "Potatoes" -> "potato".
 *  2. Maps common alternative names to one name: "spaghetti" -> "pasta", "mayo" -> "mayonnaise".
 *  3. Converts units to a base unit before comparing: 1 kg = 1000 g, 1 l = 1000 ml,
 *     1 cup = 250 ml, 1 tbsp = 15 ml, 1 tsp = 5 ml.
 *  4. Adds up the same ingredient if it appears in the pantry more than once.
 *
 * This class has no Android code in it, so it is easy to read and to test.
 */
public final class RecipeMatcher {

    private RecipeMatcher() {
        // Utility class - not meant to be instantiated
    }

    // =====================================================================
    //  Result classes
    // =====================================================================

    /** One recipe and the list of ingredients the user is missing for it. */
    public static class RecipeMatch {
        public final Recipe recipe;
        public final List<String> missing;

        RecipeMatch(Recipe recipe, List<String> missing) {
            this.recipe = recipe;
            this.missing = missing;
        }

        public boolean isFullMatch() {
            return missing.isEmpty();
        }
    }

    /** The two lists shown on the Suggested Recipes screen. */
    public static class MatchResult {
        /** Recipes the user can make right now (strict matches only). */
        public final List<RecipeMatch> ready = new ArrayList<>();
        /** Bonus: recipes missing exactly one ingredient. Kept completely separate. */
        public final List<RecipeMatch> almost = new ArrayList<>();
    }

    // =====================================================================
    //  Main entry point
    // =====================================================================

    /** Checks every recipe against the pantry and sorts them into "ready" and "almost". */
    public static MatchResult match(List<Recipe> recipes, List<PantryItem> pantry) {
        Map<String, Stock> stock = buildStock(pantry);
        MatchResult result = new MatchResult();

        for (Recipe recipe : recipes) {
            List<String> missing = findMissing(recipe, stock);
            if (missing.isEmpty()) {
                result.ready.add(new RecipeMatch(recipe, missing));      // strict match
            } else if (missing.size() == 1) {
                result.almost.add(new RecipeMatch(recipe, missing));     // bonus list only
            }
        }
        return result;
    }

    /** True only if the pantry has every ingredient of the recipe in a large enough amount. */
    public static boolean canCook(Recipe recipe, List<PantryItem> pantry) {
        return findMissing(recipe, buildStock(pantry)).isEmpty();
    }

    /** True if this single ingredient is covered by the pantry (used by the detail screen). */
    public static boolean hasEnough(RecipeIngredient ingredient, List<PantryItem> pantry) {
        return hasEnough(ingredient, buildStock(pantry));
    }

    // =====================================================================
    //  Step 1: add up what is in the pantry
    // =====================================================================

    /** How much of one ingredient the user has, split into "weight/volume" and "pieces". */
    private static class Stock {
        double amount;     // grams or millilitres (treated as roughly equal for kitchen use)
        double pieces;     // countable items, e.g. 6 eggs
    }

    /** Groups pantry items by their normalised name and adds up the quantities. */
    private static Map<String, Stock> buildStock(List<PantryItem> pantry) {
        Map<String, Stock> stock = new HashMap<>();
        for (PantryItem item : pantry) {
            String key = normaliseName(item.getName());
            if (key.isEmpty()) continue;

            Stock s = stock.get(key);
            if (s == null) {
                s = new Stock();
                stock.put(key, s);
            }

            double base = toBaseAmount(item.getQuantity(), item.getUnit());
            if (isCountUnit(item.getUnit())) {
                s.pieces += base;
            } else {
                s.amount += base;
            }
        }
        return stock;
    }

    // =====================================================================
    //  Step 2: check each ingredient of a recipe
    // =====================================================================

    /** Returns a readable line for every ingredient that is missing or not enough. */
    private static List<String> findMissing(Recipe recipe, Map<String, Stock> stock) {
        List<String> missing = new ArrayList<>();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            if (!hasEnough(ingredient, stock)) {
                missing.add(ingredient.getName());
            }
        }
        return missing;
    }

    private static boolean hasEnough(RecipeIngredient ingredient, Map<String, Stock> stock) {
        Stock s = stock.get(normaliseName(ingredient.getName()));
        if (s == null) {
            return false; // not in the pantry at all
        }

        double needed = toBaseAmount(ingredient.getQuantity(), ingredient.getUnit());
        double have = isCountUnit(ingredient.getUnit()) ? s.pieces : s.amount;

        // Small tolerance so that 0.1 + 0.2 style rounding never causes a false "missing"
        return have + 1e-6 >= needed;
    }

    // =====================================================================
    //  Name normalisation: "Tomatoes" -> "tomato"
    // =====================================================================

    /** Different names that mean the same ingredient. Keys and values are already singular. */
    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        ALIASES.put("spaghetti", "pasta");
        ALIASES.put("macaroni", "pasta");
        ALIASES.put("penne", "pasta");
        ALIASES.put("noodle", "pasta");
        ALIASES.put("mayo", "mayonnaise");
        ALIASES.put("spring onion", "green onion");
        ALIASES.put("scallion", "green onion");
        ALIASES.put("bell pepper", "green pepper");
        ALIASES.put("green bell pepper", "green pepper");
        ALIASES.put("capsicum", "green pepper");
        ALIASES.put("mince", "beef mince");
        ALIASES.put("minced beef", "beef mince");
        ALIASES.put("ground beef", "beef mince");
        ALIASES.put("mielie meal", "maize meal");
        ALIASES.put("mealie meal", "maize meal");
        ALIASES.put("maize", "maize meal");
        ALIASES.put("chicken breast", "chicken");
        ALIASES.put("chicken thigh", "chicken");
        ALIASES.put("chicken piece", "chicken");
        ALIASES.put("tinned tuna", "tuna");
        ALIASES.put("canned tuna", "tuna");
        ALIASES.put("stock", "stock cube");
        ALIASES.put("bouillon cube", "stock cube");
        ALIASES.put("cooking oil", "oil");
        ALIASES.put("sunflower oil", "oil");
        ALIASES.put("vegetable oil", "oil");
        ALIASES.put("cheddar", "cheese");
        ALIASES.put("cheddar cheese", "cheese");
        ALIASES.put("white sugar", "sugar");
        ALIASES.put("brown sugar", "sugar");
        ALIASES.put("cake flour", "flour");
        ALIASES.put("plain flour", "flour");
        ALIASES.put("all purpose flour", "flour");
        ALIASES.put("full cream milk", "milk");
        ALIASES.put("bread slice", "bread");
        ALIASES.put("slice of bread", "bread");
        ALIASES.put("garlic clove", "garlic");
        ALIASES.put("clove of garlic", "garlic");
    }

    /**
     * Turns a typed ingredient name into one standard form so that small differences
     * do not stop a match: lower case, extra spaces and punctuation removed, each word
     * made singular, then common alternative names replaced.
     */
    public static String normaliseName(String raw) {
        if (raw == null) return "";

        String cleaned = raw.toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replaceAll("[^a-z ]", "")   // drop punctuation and numbers
                .trim()
                .replaceAll("\\s+", " ");    // collapse repeated spaces
        if (cleaned.isEmpty()) return "";

        StringBuilder singular = new StringBuilder();
        for (String word : cleaned.split(" ")) {
            if (singular.length() > 0) singular.append(' ');
            singular.append(singularise(word));
        }

        String name = singular.toString();
        String alias = ALIASES.get(name);
        return alias != null ? alias : name;
    }

    /** Simple English plural rules - enough for ingredient names. */
    static String singularise(String word) {
        if (word.length() <= 3) return word;                                   // "egg", "oil"
        if (word.endsWith("ies")) return word.substring(0, word.length() - 3) + "y";   // berries -> berry
        if (word.endsWith("oes")) return word.substring(0, word.length() - 2);          // tomatoes -> tomato
        if (word.endsWith("ches") || word.endsWith("shes")
                || word.endsWith("xes") || word.endsWith("sses")) {
            return word.substring(0, word.length() - 2);                        // peaches -> peach
        }
        if (word.endsWith("ss") || word.endsWith("us") || word.endsWith("is")) {
            return word;                                                        // "swiss", "hummus"
        }
        if (word.endsWith("s")) return word.substring(0, word.length() - 1);   // eggs -> egg
        return word;
    }

    // =====================================================================
    //  Unit conversion: everything becomes grams/millilitres or pieces
    // =====================================================================

    /** Units the app understands. Shown in the Add/Edit dropdown. */
    public static final String[] UNITS = {"g", "kg", "ml", "l", "tsp", "tbsp", "cup", "pcs"};

    /** True for units that count whole items rather than weigh or measure them. */
    public static boolean isCountUnit(String unit) {
        return "pcs".equals(normaliseUnit(unit));
    }

    /**
     * Converts a quantity to grams / millilitres (or pieces).
     * Grams and millilitres are treated as about equal, which is close enough
     * for kitchen ingredients and lets "500 g salt" cover "1 tsp salt".
     */
    public static double toBaseAmount(double quantity, String unit) {
        switch (normaliseUnit(unit)) {
            case "kg":   return quantity * 1000;
            case "l":    return quantity * 1000;
            case "cup":  return quantity * 250;
            case "tbsp": return quantity * 15;
            case "tsp":  return quantity * 5;
            default:     return quantity;        // g, ml and pcs are already base units
        }
    }

    /** Accepts common spellings of units, e.g. "grams", "Kg", "litres", "pieces". */
    public static String normaliseUnit(String unit) {
        if (unit == null) return "pcs";
        String u = unit.trim().toLowerCase(Locale.ROOT).replace(".", "");
        switch (u) {
            case "g": case "gram": case "grams": case "gr":
                return "g";
            case "kg": case "kilogram": case "kilograms": case "kgs":
                return "kg";
            case "ml": case "millilitre": case "millilitres": case "milliliter": case "milliliters":
                return "ml";
            case "l": case "litre": case "litres": case "liter": case "liters":
                return "l";
            case "tsp": case "teaspoon": case "teaspoons":
                return "tsp";
            case "tbsp": case "tablespoon": case "tablespoons":
                return "tbsp";
            case "cup": case "cups":
                return "cup";
            default:
                return "pcs";   // "pcs", "piece", "pieces", "whole", "" ...
        }
    }
}