package com.example.smartpantrymanager;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartpantrymanager.adapter.PantryAdapter;
import com.example.smartpantrymanager.adapter.RecipeAdapter;
import com.example.smartpantrymanager.data.DatabaseHelper;
import com.example.smartpantrymanager.logic.RecipeMatcher;
import com.example.smartpantrymanager.model.PantryItem;
import com.example.smartpantrymanager.model.Recipe;
import com.example.smartpantrymanager.model.RecipeIngredient;

import java.util.List;

/**
 * Recipe Detail screen.
 * Receives a recipe id through the Intent, loads that recipe from SQLite and shows
 * its full ingredient list (ticked if you have enough, crossed if not) and its method.
 */
public class RecipeDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recipe_detail2);

        // The green header runs up behind the status bar, so use white status-bar icons
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        // Push only the header's content down below the status bar
        View hero = findViewById(R.id.heroHeader);
        int heroTop = hero.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            hero.setPadding(hero.getPaddingLeft(), heroTop + bars.top,
                    hero.getPaddingRight(), hero.getPaddingBottom());
            return insets;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Read the recipe id that SuggestedRecipesActivity put in the Intent
        long recipeId = getIntent().getLongExtra(SuggestedRecipeActivity.EXTRA_RECIPE_ID, -1);
        Recipe recipe = recipeId == -1 ? null : DatabaseHelper.getInstance(this).getRecipe(recipeId);

        if (recipe == null) {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showRecipe(recipe);
    }

    private void showRecipe(Recipe recipe) {
        ((TextView) findViewById(R.id.txtRecipeName)).setText(recipe.getName());

        int ingredientCount = recipe.getIngredients().size();
        int stepCount = RecipeAdapter.countSteps(recipe.getSteps());
        ((TextView) findViewById(R.id.txtIngredientChip)).setText(ingredientCount + " ingredients");
        ((TextView) findViewById(R.id.txtStepChip)).setText(stepCount + " steps");

        ((TextView) findViewById(R.id.txtIngredients)).setText(buildIngredientList(recipe));
        ((TextView) findViewById(R.id.txtSteps)).setText(recipe.getSteps());
    }

    /**
     * One line per ingredient, e.g. "✓  250 g pasta".
     * A green tick means the pantry has enough; a red cross means it is missing or short.
     */
    private CharSequence buildIngredientList(Recipe recipe) {
        List<PantryItem> pantry = DatabaseHelper.getInstance(this).getAllPantryItems();
        int green = ContextCompat.getColor(this, R.color.green_primary);
        int red = ContextCompat.getColor(this, R.color.warning_red);

        SpannableStringBuilder text = new SpannableStringBuilder();
        List<RecipeIngredient> ingredients = recipe.getIngredients();

        for (int i = 0; i < ingredients.size(); i++) {
            RecipeIngredient ingredient = ingredients.get(i);
            boolean have = RecipeMatcher.hasEnough(ingredient, pantry);

            // Coloured, bold tick or cross
            int start = text.length();
            text.append(have ? "✓" : "✗");
            text.setSpan(new ForegroundColorSpan(have ? green : red),
                    start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new StyleSpan(Typeface.BOLD),
                    start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            text.append("   ")
                    .append(PantryAdapter.formatQuantity(ingredient.getQuantity()))
                    .append(" ")
                    .append(ingredient.getUnit())
                    .append(" ")
                    .append(ingredient.getName());

            if (!have) {
                int noteStart = text.length();
                text.append("  (need more)");
                text.setSpan(new ForegroundColorSpan(red),
                        noteStart, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (i < ingredients.size() - 1) {
                text.append("\n");
            }
        }
        return text;
    }
}