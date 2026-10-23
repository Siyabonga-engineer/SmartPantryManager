package com.example.smartpantrymanager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.adapter.PantryAdapter;
import com.example.smartpantrymanager.data.DatabaseHelper;
import com.example.smartpantrymanager.model.PantryItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

/**
 * Pantry List screen (the first screen of the app).
 * Shows every pantry item from the database in a RecyclerView and lets the user
 * add ("Add item" button), edit (tap a card) and delete (bin icon) items.
 */
public class MainActivity extends AppCompatActivity {

    /** Key used to pass the id of the item being edited to AddEditIngredientActivity. */
    public static final String EXTRA_ITEM_ID = "item_id";

    /** SharedPreferences file and keys, shared with SettingsActivity. */
    public static final String PREFS_NAME = "smart_pantry_prefs";
    public static final String KEY_EXPIRY_ALERTS = "expiry_alerts";

    private DatabaseHelper db;
    private PantryAdapter adapter;
    private RecyclerView recyclerPantry;
    private View emptyState;
    private TextView txtSummary;
    private TextView txtItemCount;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Keep content clear of the status bar and navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = DatabaseHelper.getInstance(this);

        recyclerPantry = findViewById(R.id.recyclerPantry);
        emptyState = findViewById(R.id.emptyState);
        txtSummary = findViewById(R.id.txtSummary);
        txtItemCount = findViewById(R.id.txtItemCount);

        // Connect the adapter and tell it what to do on tap / delete
        adapter = new PantryAdapter(new PantryAdapter.OnItemActionListener() {
            @Override
            public void onItemClick(PantryItem item) {
                // EDIT: open the form and pass the item's id with the Intent
                Intent intent = new Intent(MainActivity.this, AddEditIngredientActivity.class);
                intent.putExtra(EXTRA_ITEM_ID, item.getId());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(PantryItem item) {
                confirmDelete(item);
            }
        });
        recyclerPantry.setAdapter(adapter);

        // ADD: the "Add item" button opens an empty form
        ExtendedFloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddEditIngredientActivity.class)));

        // Shrink the button to just "+" while scrolling down, expand it again when scrolling up
        NestedScrollView scrollPantry = findViewById(R.id.scrollPantry);
        scrollPantry.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, x, y, oldX, oldY) -> {
                    if (y > oldY + 10) {
                        fabAdd.shrink();
                    } else if (y < oldY - 10) {
                        fabAdd.extend();
                    }
                });

        // "What can I cook?" card is a shortcut to the Suggested Recipes screen
        View.OnClickListener openRecipes = v ->
                startActivity(new Intent(MainActivity.this, SuggestedRecipeActivity.class));
        findViewById(R.id.cardHero).setOnClickListener(openRecipes);
        findViewById(R.id.btnSeeRecipes).setOnClickListener(openRecipes);

        setUpBottomNavigation();
    }

    /**
     * onResume runs every time this screen comes back into view (for example after
     * saving on the Add/Edit screen), so reloading here keeps the list up to date.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Read the "Expiring-soon alerts" setting (on by default)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        adapter.setHighlightExpiring(prefs.getBoolean(KEY_EXPIRY_ALERTS, true));

        loadPantry();

        // Highlight the Pantry tab without triggering the click listener
        bottomNav.getMenu().findItem(R.id.nav_pantry).setChecked(true);
    }

    /** READ: fetch all items from SQLite and show either the list or the empty message. */
    private void loadPantry() {
        List<PantryItem> items = db.getAllPantryItems();
        adapter.setItems(items);

        boolean isEmpty = items.isEmpty();
        recyclerPantry.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        updateSummary(items);
    }

    /** Header line, e.g. "6 ingredients · 2 expiring soon", and the "6 items" pill. */
    private void updateSummary(List<PantryItem> items) {
        int count = items.size();
        txtItemCount.setText(count + (count == 1 ? " item" : " items"));

        if (count == 0) {
            txtSummary.setText("Track what you have. Waste less.");
            return;
        }

        int expiringSoon = 0;
        for (PantryItem item : items) {
            if (PantryAdapter.isExpiringSoon(item)) expiringSoon++;
        }

        String summary = count + (count == 1 ? " ingredient" : " ingredients");
        if (expiringSoon > 0) {
            summary += " · " + expiringSoon + " expiring soon";
        }
        txtSummary.setText(summary);
    }

    /** DELETE: ask the user to confirm first, then remove the row from the database. */
    private void confirmDelete(PantryItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete ingredient")
                .setMessage("Remove \"" + item.getName() + "\" from your pantry?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.deletePantryItem(item.getId());
                    loadPantry();
                    Toast.makeText(this, item.getName() + " deleted", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /** Bottom navigation: each tab opens its screen with an Intent. */
    private void setUpBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.nav_recipes) {
                startActivity(new Intent(this, SuggestedRecipeActivity.class));
                return false; // keep "Pantry" highlighted on this screen
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingActivity.class));
                return false;
            }
            return true; // already on Pantry
        });
    }
}