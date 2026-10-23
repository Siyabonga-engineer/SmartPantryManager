package com.example.smartpantrymanager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * Settings screen.
 * Saves two small preferences with SharedPreferences (not the database):
 *  - whether expiring-soon items are highlighted on the Pantry List
 *  - which unit is pre-selected when adding a new ingredient
 */
public class SettingActivity extends AppCompatActivity {

    /** Key for the default unit, read by AddEditIngredientActivity. */
    public static final String KEY_DEFAULT_UNIT = "default_unit";

    private SharedPreferences prefs;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);

        setUpExpiryAlertsSwitch();
        setUpUnitChoice();
        setUpBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Highlight the Settings tab without triggering the click listener
        bottomNav.getMenu().findItem(R.id.nav_settings).setChecked(true);
    }

    /** Switch: load the saved value, and save it again whenever it is flipped. */
    private void setUpExpiryAlertsSwitch() {
        MaterialSwitch switchExpiry = findViewById(R.id.switchExpiryAlerts);
        switchExpiry.setChecked(prefs.getBoolean(MainActivity.KEY_EXPIRY_ALERTS, true));

        switchExpiry.setOnCheckedChangeListener((button, isChecked) -> {
            prefs.edit().putBoolean(MainActivity.KEY_EXPIRY_ALERTS, isChecked).apply();
            Toast.makeText(this,
                    isChecked ? "Expiring-soon alerts on" : "Expiring-soon alerts off",
                    Toast.LENGTH_SHORT).show();
        });
    }

    /** Radio buttons: tick the saved choice, and save the new one when it changes. */
    private void setUpUnitChoice() {
        RadioGroup radioUnits = findViewById(R.id.radioUnits);

        String savedUnit = prefs.getString(KEY_DEFAULT_UNIT, "g");
        if ("pcs".equals(savedUnit)) {
            radioUnits.check(R.id.radioPieces);
        } else if ("cup".equals(savedUnit)) {
            radioUnits.check(R.id.radioKitchen);
        } else {
            radioUnits.check(R.id.radioMetric);
        }

        radioUnits.setOnCheckedChangeListener((group, checkedId) -> {
            String unit;
            if (checkedId == R.id.radioPieces) {
                unit = "pcs";
            } else if (checkedId == R.id.radioKitchen) {
                unit = "cup";
            } else {
                unit = "g";
            }
            prefs.edit().putString(KEY_DEFAULT_UNIT, unit).apply();
        });
    }

    /** Bottom navigation: move to the other main screens with Intents. */
    private void setUpBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.nav_pantry) {
                // Go back to the existing Pantry screen instead of opening a new copy
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return false;
            } else if (id == R.id.nav_recipes) {
                startActivity(new Intent(this, SuggestedRecipeActivity.class));
                finish();
                return false;
            }
            return true; // already on Settings
        });
    }
}