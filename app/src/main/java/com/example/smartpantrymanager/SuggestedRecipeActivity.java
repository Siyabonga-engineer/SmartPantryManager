package com.example.smartpantrymanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.adapter.RecipeAdapter;
import com.example.smartpantrymanager.data.DatabaseHelper;
import com.example.smartpantrymanager.logic.RecipeMatcher;
import com.example.smartpantrymanager.model.PantryItem;
import com.example.smartpantrymanager.model.Recipe;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

/**
 * Suggested Recipes screen.
 * Runs the strict-matching rule (RecipeMatcher) against the current pantry and shows:
 *  - "Ready to cook": recipes the user has EVERY ingredient for (the real suggestions)
 *  - "Almost there":  recipes missing exactly one ingredient (bonus, kept separate)
 */
public class SuggestedRecipeActivity extends AppCompatActivity {

    /** Key used to pass the chosen recipe's id to RecipeDetailActivity. */
    public static final String EXTRA_RECIPE_ID = "recipe_id";

    private DatabaseHelper db;
    private RecipeAdapter readyAdapter;
    private RecipeAdapter almostAdapter;

    private RecyclerView recyclerSuggested;
    private RecyclerView recyclerAlmost;
    private View emptySuggested;
    private TextView txtRecipeSummary, txtReadyCount, txtAlmostCount, txtAlmostEmpty;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_suggested_recipe);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = DatabaseHelper.getInstance(this);

        recyclerSuggested = findViewById(R.id.recyclerSuggested);
        recyclerAlmost = findViewById(R.id.recyclerAlmost);
        emptySuggested = findViewById(R.id.emptySuggested);
        txtRecipeSummary = findViewById(R.id.txtRecipeSummary);
        txtReadyCount = findViewById(R.id.txtReadyCount);
        txtAlmostCount = findViewById(R.id.txtAlmostCount);
        txtAlmostEmpty = findViewById(R.id.txtAlmostEmpty);

        // Tapping any recipe card opens its detail screen, passing the recipe id
        RecipeAdapter.OnRecipeClickListener openDetail = recipe -> {
            Intent intent = new Intent(this, RecipeDetailActivity.class);
            intent.putExtra(EXTRA_RECIPE_ID, recipe.getId());
            startActivity(intent);
        };

        readyAdapter = new RecipeAdapter(false, openDetail);
        almostAdapter = new RecipeAdapter(true, openDetail);
        recyclerSuggested.setAdapter(readyAdapter);
        recyclerAlmost.setAdapter(almostAdapter);

        // "Add ingredients" button on the empty message goes straight to the form
        findViewById(R.id.btnGoToPantry).setOnClickListener(v ->
                startActivity(new Intent(this, AddEditIngredientActivity.class)));

        setUpBottomNavigation();
    }

    /** Re-run the matching every time the screen is shown, so it always reflects the latest pantry. */
    @Override
    protected void onResume() {
        super.onResume();
        loadSuggestions();
        bottomNav.getMenu().findItem(R.id.nav_recipes).setChecked(true);
    }

    private void loadSuggestions() {
        List<Recipe> recipes = db.getAllRecipes();
        List<PantryItem> pantry = db.getAllPantryItems();

        RecipeMatcher.MatchResult result = RecipeMatcher.match(recipes, pantry);

        // ----- Section 1: strict matches -----
        readyAdapter.setMatches(result.ready);
        int readyCount = result.ready.size();
        txtReadyCount.setText(String.valueOf(readyCount));

        boolean noMatches = readyCount == 0;
        recyclerSuggested.setVisibility(noMatches ? View.GONE : View.VISIBLE);
        emptySuggested.setVisibility(noMatches ? View.VISIBLE : View.GONE);

        if (noMatches) {
            txtRecipeSummary.setText("No recipes match your pantry yet");
        } else {
            txtRecipeSummary.setText("You can cook " + readyCount
                    + (readyCount == 1 ? " recipe" : " recipes") + " right now");
        }

        // ----- Section 2: almost there (bonus) -----
        almostAdapter.setMatches(result.almost);
        int almostCount = result.almost.size();
        txtAlmostCount.setText(String.valueOf(almostCount));
        recyclerAlmost.setVisibility(almostCount == 0 ? View.GONE : View.VISIBLE);
        txtAlmostEmpty.setVisibility(almostCount == 0 ? View.VISIBLE : View.GONE);
    }

    /** Bottom navigation: move to the other main screens with Intents. */
    private void setUpBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.nav_pantry) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return false;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingActivity.class));
                finish();
                return false;
            }
            return true; // already on Recipes
        });
    }
}