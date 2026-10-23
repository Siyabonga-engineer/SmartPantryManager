package com.example.smartpantrymanager.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.R;
import com.example.smartpantrymanager.logic.RecipeMatcher;
import com.example.smartpantrymanager.model.Recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom Adapter for recipe cards (item_recipe.xml).
 * The same adapter is used twice on the Suggested Recipes screen:
 *  - "Ready to cook" list  (green icon, no missing line)
 *  - "Almost there" list   (orange icon, shows the one missing ingredient)
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    private final List<RecipeMatcher.RecipeMatch> matches = new ArrayList<>();
    private final boolean almostThereStyle;
    private final OnRecipeClickListener listener;

    public RecipeAdapter(boolean almostThereStyle, OnRecipeClickListener listener) {
        this.almostThereStyle = almostThereStyle;
        this.listener = listener;
    }

    public void setMatches(List<RecipeMatcher.RecipeMatch> newMatches) {
        matches.clear();
        matches.addAll(newMatches);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        RecipeMatcher.RecipeMatch match = matches.get(position);
        Recipe recipe = match.recipe;
        Context context = holder.itemView.getContext();

        holder.txtRecipeName.setText(recipe.getName());

        int ingredientCount = recipe.getIngredients().size();
        int stepCount = countSteps(recipe.getSteps());
        holder.txtIngredientCount.setText(ingredientCount + " ingredients · " + stepCount + " steps");

        // Colour the icon tile: green = ready, orange = almost there
        int tileColour = almostThereStyle ? R.color.orange_light : R.color.green_light;
        int iconColour = almostThereStyle ? R.color.orange_dark : R.color.green_primary;
        holder.iconTile.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(context, tileColour)));
        holder.imgRecipeIcon.setImageTintList(
                ColorStateList.valueOf(ContextCompat.getColor(context, iconColour)));

        if (almostThereStyle && !match.missing.isEmpty()) {
            holder.txtMissing.setText("Missing: " + TextUtils.join(", ", match.missing));
            holder.txtMissing.setVisibility(View.VISIBLE);
        } else {
            holder.txtMissing.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onRecipeClick(recipe));
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    /** Counts the non-empty lines in the steps text, e.g. 4 for "1. ...\n2. ...\n3. ...\n4. ...". */
    public static int countSteps(String steps) {
        if (steps == null || steps.trim().isEmpty()) return 0;
        int count = 0;
        for (String line : steps.split("\n")) {
            if (!line.trim().isEmpty()) count++;
        }
        return count;
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final View iconTile;
        final ImageView imgRecipeIcon;
        final TextView txtRecipeName;
        final TextView txtIngredientCount;
        final TextView txtMissing;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            iconTile = itemView.findViewById(R.id.iconTile);
            imgRecipeIcon = itemView.findViewById(R.id.imgRecipeIcon);
            txtRecipeName = itemView.findViewById(R.id.txtRecipeName);
            txtIngredientCount = itemView.findViewById(R.id.txtIngredientCount);
            txtMissing = itemView.findViewById(R.id.txtMissing);
        }
    }
}