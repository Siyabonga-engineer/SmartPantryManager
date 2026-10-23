# Smart Pantry Manager

An Android app (Java, Android Studio) that helps reduce food waste. It tracks the ingredients you actually have at home and suggests recipes you can cook **using only what is already in your pantry** — no shopping trip needed.

## Features

- **Pantry management (full CRUD)** – add, view, edit and delete ingredients with a name, quantity, unit and optional expiry date.
- **Pantry list** – a `RecyclerView` with a custom `PantryAdapter`, showing every ingredient from the database. Items expiring within 3 days are highlighted in red.
- **17 pre-loaded recipes** – seeded into the database the first time the app runs.
- **Suggested Recipes (strict matching)** – lists only recipes for which *every* ingredient is in the pantry in at least the required quantity.
- **"Almost there" (bonus)** – a separate list of recipes missing exactly one ingredient, clearly kept apart from the real suggestions.
- **Recipe detail** – the full ingredient list (ticked or crossed against your pantry) and the method.
- **Settings** – expiring-soon alerts on/off and a default unit, saved with `SharedPreferences`.
- **Input validation** – required fields, positive numbers only, letters-only names, valid dates, and duplicate detection ("Tomatoes" is a duplicate of "tomato").
- **Empty states** – "Your pantry is empty" and "No recipes match your pantry yet" instead of blank screens.
- **Bottom navigation** between Pantry, Recipes and Settings, with Intents (and `putExtra`) passing data between screens.

The app does **not** use Google Maps, any mapping SDK or device location.

## The strict-matching rule

Implemented in `logic/RecipeMatcher.java`. A recipe is suggested only if every ingredient it needs is present in at least the required quantity. To handle real-world messiness it:

1. **Normalises names** – lower case, trimmed, plurals made singular (`Tomatoes` → `tomato`, `Potatoes` → `potato`, `Cherries` → `cherry`).
2. **Maps common alternative names** – e.g. `spaghetti` → `pasta`, `mayo` → `mayonnaise`, `mielie meal` → `maize meal`.
3. **Converts units** before comparing – `1 kg = 1000 g`, `1 l = 1000 ml`, `1 cup = 250 ml`, `1 tbsp = 15 ml`, `1 tsp = 5 ml`.
4. **Adds up duplicates** – two pantry rows of tomatoes are counted together.

## Database choice: SQLite

The app uses **SQLite** through `SQLiteOpenHelper` (`data/DatabaseHelper.java`).

**Why SQLite:**
- The pantry belongs to one person on one phone, so a local, on-device database is the natural fit — no accounts, server or internet connection needed.
- It works offline and data persists between app sessions.
- It is built into Android, so there is no extra setup, API key or hosting cost.
- It matches the persistent-data approach covered in the module.

**Tables**

| Table | Columns |
|---|---|
| `pantry_items` | `id` (PK), `name`, `quantity`, `unit`, `expiry_date` |
| `recipes` | `id` (PK), `name`, `steps` |
| `recipe_ingredients` | `id` (PK), `recipe_id` (FK → recipes.id), `name`, `quantity`, `unit` |

One recipe has many recipe ingredients. Pantry items are matched to recipe ingredients by normalised name.

## Project structure

```
app/src/main/java/com/example/smartpantrymanager/
├── MainActivity.java               Pantry List screen
├── AddEditIngredientActivity.java  Add / Edit form with validation
├── SuggestedRecipeActivity.java    Ready-to-cook and Almost-there lists
├── RecipeDetailActivity.java       Ingredients and method for one recipe
├── SettingActivity.java            Alerts and default unit
├── adapter/  PantryAdapter, RecipeAdapter
├── data/     DatabaseHelper (SQLite, CRUD, seed recipes)
├── logic/    RecipeMatcher (strict-matching rule)
└── model/    PantryItem, Recipe, RecipeIngredient
```

## Setup and run

**Requirements:** Android Studio (recent version), JDK 17+, an emulator or Android phone running **Android 7.0 (API 24) or newer**.

1. Clone the repository:
```bash
   git clone https://github.com/Siyabonga-engineer/SmartPantryManager.git
```
2. In Android Studio choose **File → Open** and select the `SmartPantryManager` folder.
3. Wait for the Gradle sync to finish.
4. Choose an emulator, or connect a phone with **USB debugging** turned on.
5. Press **Run ▶**.

The 17 recipes are loaded automatically on first launch. Add a few ingredients with **Add item**, then open the **Recipes** tab.

**Quick test:** add `Bananas 3 pcs`, `Milk 1 l` and `Honey 100 g` – *Banana Smoothie* appears under *Ready to cook*. Delete the honey and it moves to *Almost there* with "Missing: honey".

## Author

Mobile App Development 700 – Practical Assignment
