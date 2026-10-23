package com.example.smartpantrymanager.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.smartpantrymanager.model.PantryItem;
import com.example.smartpantrymanager.model.Recipe;
import com.example.smartpantrymanager.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the local SQLite database for Smart Pantry Manager.
 *
 * Tables:
 *   pantry_items       - the user's ingredients (full CRUD)
 *   recipes            - pre-loaded recipes (seeded on first run)
 *   recipe_ingredients - the ingredients each recipe needs (linked by recipe_id)
 *
 * A single shared instance is used across the app (singleton) so that every
 * screen talks to the same database connection.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "smart_pantry.db";
    private static final int DB_VERSION = 1;

    // ---- pantry_items table ----
    public static final String TABLE_PANTRY = "pantry_items";
    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_QUANTITY = "quantity";
    public static final String COL_UNIT = "unit";
    public static final String COL_EXPIRY = "expiry_date";

    // ---- recipes table ----
    public static final String TABLE_RECIPES = "recipes";
    public static final String COL_STEPS = "steps";

    // ---- recipe_ingredients table ----
    public static final String TABLE_RECIPE_INGREDIENTS = "recipe_ingredients";
    public static final String COL_RECIPE_ID = "recipe_id";

    private static DatabaseHelper instance;

    /** Returns the single shared DatabaseHelper for the whole app. */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            // Application context avoids leaking an Activity
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Needed so that deleting a recipe also deletes its ingredient rows
        db.setForeignKeyConstraintsEnabled(true);
    }

    /** Runs only once, the very first time the database is created on the device. */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PANTRY + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NAME + " TEXT NOT NULL, "
                + COL_QUANTITY + " REAL NOT NULL, "
                + COL_UNIT + " TEXT NOT NULL, "
                + COL_EXPIRY + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NAME + " TEXT NOT NULL, "
                + COL_STEPS + " TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPE_INGREDIENTS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_RECIPE_ID + " INTEGER NOT NULL, "
                + COL_NAME + " TEXT NOT NULL, "
                + COL_QUANTITY + " REAL NOT NULL, "
                + COL_UNIT + " TEXT NOT NULL, "
                + "FOREIGN KEY(" + COL_RECIPE_ID + ") REFERENCES "
                + TABLE_RECIPES + "(" + COL_ID + ") ON DELETE CASCADE)");

        seedRecipes(db);
    }

    /** Called when DB_VERSION is increased. For this project we simply rebuild. */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPE_INGREDIENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PANTRY);
        onCreate(db);
    }

    // =====================================================================
    //  PANTRY CRUD
    // =====================================================================

    /** CREATE: inserts a new pantry item and returns its new id (-1 on failure). */
    public long addPantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        return db.insert(TABLE_PANTRY, null, toContentValues(item));
    }

    /** READ: returns every pantry item, sorted alphabetically by name. */
    public List<PantryItem> getAllPantryItems() {
        List<PantryItem> items = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_PANTRY, null, null, null, null, null,
                COL_NAME + " COLLATE NOCASE ASC")) {
            while (c.moveToNext()) {
                items.add(cursorToPantryItem(c));
            }
        }
        return items;
    }

    /** READ: returns a single pantry item by id, or null if it does not exist. */
    public PantryItem getPantryItem(long id) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_PANTRY, null, COL_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (c.moveToFirst()) {
                return cursorToPantryItem(c);
            }
        }
        return null;
    }

    /** UPDATE: saves changes to an existing item. Returns number of rows changed. */
    public int updatePantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        return db.update(TABLE_PANTRY, toContentValues(item), COL_ID + " = ?",
                new String[]{String.valueOf(item.getId())});
    }

    /** DELETE: removes an item by id. Returns number of rows deleted. */
    public int deletePantryItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_PANTRY, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    private ContentValues toContentValues(PantryItem item) {
        ContentValues values = new ContentValues();
        values.put(COL_NAME, item.getName().trim());
        values.put(COL_QUANTITY, item.getQuantity());
        values.put(COL_UNIT, item.getUnit());
        values.put(COL_EXPIRY, item.getExpiryDate()); // null is allowed
        return values;
    }

    private PantryItem cursorToPantryItem(Cursor c) {
        int expiryIndex = c.getColumnIndexOrThrow(COL_EXPIRY);
        return new PantryItem(
                c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                c.getString(c.getColumnIndexOrThrow(COL_NAME)),
                c.getDouble(c.getColumnIndexOrThrow(COL_QUANTITY)),
                c.getString(c.getColumnIndexOrThrow(COL_UNIT)),
                c.isNull(expiryIndex) ? null : c.getString(expiryIndex));
    }

    // =====================================================================
    //  RECIPES (read-only for the user)
    // =====================================================================

    /** Returns every recipe with its ingredient list filled in. */
    public List<Recipe> getAllRecipes() {
        SQLiteDatabase db = getReadableDatabase();
        Map<Long, Recipe> recipesById = new LinkedHashMap<>();

        // 1. Load all recipes
        try (Cursor c = db.query(TABLE_RECIPES, null, null, null, null, null,
                COL_NAME + " COLLATE NOCASE ASC")) {
            while (c.moveToNext()) {
                long id = c.getLong(c.getColumnIndexOrThrow(COL_ID));
                recipesById.put(id, new Recipe(id,
                        c.getString(c.getColumnIndexOrThrow(COL_NAME)),
                        c.getString(c.getColumnIndexOrThrow(COL_STEPS))));
            }
        }

        // 2. Load all ingredients in one query and attach each to its recipe
        try (Cursor c = db.query(TABLE_RECIPE_INGREDIENTS, null, null, null, null, null, null)) {
            while (c.moveToNext()) {
                RecipeIngredient ing = cursorToIngredient(c);
                Recipe recipe = recipesById.get(ing.getRecipeId());
                if (recipe != null) {
                    recipe.addIngredient(ing);
                }
            }
        }
        return new ArrayList<>(recipesById.values());
    }

    /** Returns one recipe (with ingredients) by id, or null if not found. */
    public Recipe getRecipe(long recipeId) {
        SQLiteDatabase db = getReadableDatabase();
        Recipe recipe = null;
        try (Cursor c = db.query(TABLE_RECIPES, null, COL_ID + " = ?",
                new String[]{String.valueOf(recipeId)}, null, null, null)) {
            if (c.moveToFirst()) {
                recipe = new Recipe(recipeId,
                        c.getString(c.getColumnIndexOrThrow(COL_NAME)),
                        c.getString(c.getColumnIndexOrThrow(COL_STEPS)));
            }
        }
        if (recipe == null) return null;

        try (Cursor c = db.query(TABLE_RECIPE_INGREDIENTS, null, COL_RECIPE_ID + " = ?",
                new String[]{String.valueOf(recipeId)}, null, null, null)) {
            while (c.moveToNext()) {
                recipe.addIngredient(cursorToIngredient(c));
            }
        }
        return recipe;
    }

    private RecipeIngredient cursorToIngredient(Cursor c) {
        return new RecipeIngredient(
                c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                c.getLong(c.getColumnIndexOrThrow(COL_RECIPE_ID)),
                c.getString(c.getColumnIndexOrThrow(COL_NAME)),
                c.getDouble(c.getColumnIndexOrThrow(COL_QUANTITY)),
                c.getString(c.getColumnIndexOrThrow(COL_UNIT)));
    }

    // =====================================================================
    //  SEED DATA (runs once, inside onCreate)
    // =====================================================================

    /** Short helper so the seed list below stays readable. */
    private static RecipeIngredient ing(String name, double qty, String unit) {
        return new RecipeIngredient(name, qty, unit);
    }

    /** Inserts one recipe and all of its ingredients. */
    private void seedRecipe(SQLiteDatabase db, String name, String steps,
                            RecipeIngredient... ingredients) {
        ContentValues recipeValues = new ContentValues();
        recipeValues.put(COL_NAME, name);
        recipeValues.put(COL_STEPS, steps);
        long recipeId = db.insert(TABLE_RECIPES, null, recipeValues);

        for (RecipeIngredient i : ingredients) {
            ContentValues v = new ContentValues();
            v.put(COL_RECIPE_ID, recipeId);
            v.put(COL_NAME, i.getName());
            v.put(COL_QUANTITY, i.getQuantity());
            v.put(COL_UNIT, i.getUnit());
            db.insert(TABLE_RECIPE_INGREDIENTS, null, v);
        }
    }

    /**
     * Pre-loads 17 recipes. Ingredient names are stored in singular, lowercase form
     * and units use a small fixed set: g, kg, ml, l, tsp, tbsp, cup, pcs.
     */
    private void seedRecipes(SQLiteDatabase db) {
        seedRecipe(db, "Scrambled Eggs",
                "1. Whisk the eggs, milk and salt together.\n"
                        + "2. Melt the butter in a pan over low heat.\n"
                        + "3. Add the egg mixture and stir gently until just set.",
                ing("egg", 3, "pcs"), ing("milk", 50, "ml"),
                ing("butter", 10, "g"), ing("salt", 1, "tsp"));

        seedRecipe(db, "Cheese Omelette",
                "1. Whisk the eggs and milk.\n"
                        + "2. Melt the butter in a pan and pour in the eggs.\n"
                        + "3. When almost set, sprinkle cheese over one half.\n"
                        + "4. Fold and cook for another minute.",
                ing("egg", 3, "pcs"), ing("milk", 30, "ml"),
                ing("butter", 10, "g"), ing("cheese", 50, "g"));

        seedRecipe(db, "French Toast",
                "1. Whisk the eggs, milk and sugar in a shallow dish.\n"
                        + "2. Dip each slice of bread into the mixture.\n"
                        + "3. Fry in butter until golden on both sides.",
                ing("bread", 4, "pcs"), ing("egg", 2, "pcs"), ing("milk", 100, "ml"),
                ing("sugar", 1, "tbsp"), ing("butter", 20, "g"));

        seedRecipe(db, "Pancakes",
                "1. Mix the flour and sugar in a bowl.\n"
                        + "2. Whisk in the eggs and milk until smooth.\n"
                        + "3. Melt a little butter in a pan and cook ladlefuls of batter.\n"
                        + "4. Flip when bubbles appear and cook until golden.",
                ing("flour", 200, "g"), ing("milk", 300, "ml"), ing("egg", 2, "pcs"),
                ing("sugar", 2, "tbsp"), ing("butter", 20, "g"));

        seedRecipe(db, "Tomato Pasta",
                "1. Boil the pasta in salted water until cooked.\n"
                        + "2. Fry the chopped onion and garlic in olive oil.\n"
                        + "3. Add the chopped tomatoes and simmer for 10 minutes.\n"
                        + "4. Toss the sauce with the drained pasta.",
                ing("pasta", 250, "g"), ing("tomato", 4, "pcs"), ing("onion", 1, "pcs"),
                ing("garlic", 2, "pcs"), ing("olive oil", 2, "tbsp"));

        seedRecipe(db, "Garlic Butter Pasta",
                "1. Boil the pasta until cooked.\n"
                        + "2. Melt the butter and gently fry the crushed garlic.\n"
                        + "3. Toss the drained pasta in the garlic butter.\n"
                        + "4. Top with grated cheese.",
                ing("pasta", 250, "g"), ing("garlic", 4, "pcs"),
                ing("butter", 50, "g"), ing("cheese", 30, "g"));

        seedRecipe(db, "Egg Fried Rice",
                "1. Cook the rice and let it cool.\n"
                        + "2. Fry the diced onion and carrot in oil.\n"
                        + "3. Push aside, scramble the eggs in the pan.\n"
                        + "4. Add the rice and soy sauce and stir-fry for 3 minutes.",
                ing("rice", 300, "g"), ing("egg", 2, "pcs"), ing("onion", 1, "pcs"),
                ing("carrot", 1, "pcs"), ing("soy sauce", 2, "tbsp"), ing("oil", 2, "tbsp"));

        seedRecipe(db, "Vegetable Soup",
                "1. Dice the potatoes, carrots, onion and tomatoes.\n"
                        + "2. Fry the onion until soft, then add the other vegetables.\n"
                        + "3. Cover with water, crumble in the stock cube.\n"
                        + "4. Simmer for 25 minutes until the vegetables are tender.",
                ing("potato", 2, "pcs"), ing("carrot", 2, "pcs"), ing("onion", 1, "pcs"),
                ing("tomato", 2, "pcs"), ing("stock cube", 1, "pcs"));

        seedRecipe(db, "Mashed Potatoes",
                "1. Peel and boil the potatoes until soft.\n"
                        + "2. Drain and mash with butter and milk.\n"
                        + "3. Season with salt.",
                ing("potato", 4, "pcs"), ing("butter", 30, "g"),
                ing("milk", 100, "ml"), ing("salt", 1, "tsp"));

        seedRecipe(db, "Chicken Stir Fry",
                "1. Cook the rice.\n"
                        + "2. Slice the chicken, green pepper and onion.\n"
                        + "3. Stir-fry the chicken in oil until cooked through.\n"
                        + "4. Add the vegetables and soy sauce and cook for 4 minutes.\n"
                        + "5. Serve over the rice.",
                ing("chicken", 500, "g"), ing("green pepper", 1, "pcs"), ing("onion", 1, "pcs"),
                ing("soy sauce", 3, "tbsp"), ing("oil", 2, "tbsp"), ing("rice", 300, "g"));

        seedRecipe(db, "Grilled Cheese Sandwich",
                "1. Butter one side of each slice of bread.\n"
                        + "2. Place cheese between the unbuttered sides.\n"
                        + "3. Fry in a pan until golden and the cheese has melted.",
                ing("bread", 2, "pcs"), ing("cheese", 50, "g"), ing("butter", 15, "g"));

        seedRecipe(db, "Tuna Pasta Salad",
                "1. Boil the pasta, drain and let it cool.\n"
                        + "2. Mix in the drained tuna, chopped onion and mayonnaise.\n"
                        + "3. Chill before serving.",
                ing("pasta", 200, "g"), ing("tuna", 1, "pcs"),
                ing("mayonnaise", 3, "tbsp"), ing("onion", 1, "pcs"));

        seedRecipe(db, "Banana Smoothie",
                "1. Peel and slice the bananas.\n"
                        + "2. Blend with the milk and honey until smooth.",
                ing("banana", 2, "pcs"), ing("milk", 250, "ml"), ing("honey", 1, "tbsp"));

        seedRecipe(db, "Chicken Curry",
                "1. Fry the onion and garlic in oil until soft.\n"
                        + "2. Add the curry powder and cook for 1 minute.\n"
                        + "3. Add the chicken pieces and brown them.\n"
                        + "4. Add the chopped tomatoes and simmer for 25 minutes.",
                ing("chicken", 500, "g"), ing("onion", 1, "pcs"), ing("tomato", 2, "pcs"),
                ing("garlic", 2, "pcs"), ing("curry powder", 2, "tbsp"), ing("oil", 2, "tbsp"));

        seedRecipe(db, "Spaghetti Bolognese",
                "1. Boil the pasta until cooked.\n"
                        + "2. Fry the onion and garlic, then brown the mince.\n"
                        + "3. Add the chopped tomatoes and simmer for 20 minutes.\n"
                        + "4. Serve the sauce over the pasta.",
                ing("pasta", 250, "g"), ing("beef mince", 500, "g"), ing("tomato", 3, "pcs"),
                ing("onion", 1, "pcs"), ing("garlic", 2, "pcs"));

        seedRecipe(db, "Pap and Tomato Gravy",
                "1. Bring water to the boil and slowly stir in the maize meal.\n"
                        + "2. Cover and cook on low heat for 20 minutes, stirring now and then.\n"
                        + "3. Fry the onion in oil, add the chopped tomatoes and simmer.\n"
                        + "4. Serve the gravy over the pap.",
                ing("maize meal", 250, "g"), ing("tomato", 3, "pcs"),
                ing("onion", 1, "pcs"), ing("oil", 2, "tbsp"));

        seedRecipe(db, "Banana Bread",
                "1. Preheat the oven to 180 degrees C.\n"
                        + "2. Mash the bananas and mix with melted butter and sugar.\n"
                        + "3. Beat in the eggs, then fold in the flour.\n"
                        + "4. Bake in a greased loaf tin for 50 minutes.",
                ing("flour", 250, "g"), ing("banana", 3, "pcs"), ing("sugar", 100, "g"),
                ing("egg", 2, "pcs"), ing("butter", 100, "g"));
    }
}