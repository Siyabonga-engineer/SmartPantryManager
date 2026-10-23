package com.example.smartpantrymanager;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartpantrymanager.adapter.PantryAdapter;
import com.example.smartpantrymanager.data.DatabaseHelper;
import com.example.smartpantrymanager.logic.RecipeMatcher;
import com.example.smartpantrymanager.model.PantryItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

/**
 * Add/Edit Ingredient screen.
 *
 * - Opened with no extra: ADD mode (CREATE in CRUD).
 * - Opened with MainActivity.EXTRA_ITEM_ID: EDIT mode (UPDATE in CRUD) - the form is
 *   filled with the existing item and saving updates that row.
 *
 * Every field is validated before anything is written to the database.
 */
public class AddEditIngredientActivity extends AppCompatActivity {

    private DatabaseHelper db;

    private TextInputLayout tilName, tilQuantity, tilUnit, tilExpiry;
    private TextInputEditText etName, etQuantity, etExpiry;
    private MaterialAutoCompleteTextView actvUnit;

    /** The item being edited, or null when adding a new one. */
    private PantryItem editingItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit_ingredient);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime()); // also keep the form above the keyboard
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        db = DatabaseHelper.getInstance(this);
        findViews();
        setUpUnitDropdown();
        setUpExpiryDatePicker();
        clearErrorsWhileTyping();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveIngredient());

        // Read the id passed with the Intent. -1 means "no id", i.e. ADD mode.
        long itemId = getIntent().getLongExtra(MainActivity.EXTRA_ITEM_ID, -1);
        if (itemId != -1) {
            loadItemForEditing(itemId);
        }
    }

    private void findViews() {
        tilName = findViewById(R.id.tilName);
        tilQuantity = findViewById(R.id.tilQuantity);
        tilUnit = findViewById(R.id.tilUnit);
        tilExpiry = findViewById(R.id.tilExpiry);
        etName = findViewById(R.id.etName);
        etQuantity = findViewById(R.id.etQuantity);
        etExpiry = findViewById(R.id.etExpiry);
        actvUnit = findViewById(R.id.actvUnit);
    }

    // =====================================================================
    //  Form set-up
    // =====================================================================

    /** Fills the unit dropdown and pre-selects the default unit chosen in Settings. */
    private void setUpUnitDropdown() {
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, RecipeMatcher.UNITS);
        actvUnit.setAdapter(unitAdapter);

        String defaultUnit = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
                .getString(SettingActivity.KEY_DEFAULT_UNIT, "g");
        actvUnit.setText(defaultUnit, false); // false = don't open the list
    }

    /** The expiry field is not typed into: tapping it opens a calendar. */
    private void setUpExpiryDatePicker() {
        etExpiry.setOnClickListener(v -> showDatePicker());

        // A bin icon at the end of the field removes the date (it is optional)
        tilExpiry.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        tilExpiry.setEndIconDrawable(R.drawable.ic_delete);
        tilExpiry.setEndIconContentDescription("Clear expiry date");
        tilExpiry.setEndIconOnClickListener(v -> {
            etExpiry.setText("");
            tilExpiry.setEndIconVisible(false);
        });
        tilExpiry.setEndIconVisible(false);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(this, (picker, year, month, day) -> {
            // Store dates as yyyy-MM-dd so they sort and compare correctly
            String date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            etExpiry.setText(date);
            tilExpiry.setError(null);
            tilExpiry.setEndIconVisible(true);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        dialog.show();
    }

    /** Removes a red error message as soon as the user starts fixing that field. */
    private void clearErrorsWhileTyping() {
        etName.addTextChangedListener(clearErrorOn(tilName));
        etQuantity.addTextChangedListener(clearErrorOn(tilQuantity));
        actvUnit.addTextChangedListener(clearErrorOn(tilUnit));
    }

    private TextWatcher clearErrorOn(TextInputLayout layout) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        };
    }

    // =====================================================================
    //  EDIT mode: load the existing item into the form
    // =====================================================================

    private void loadItemForEditing(long itemId) {
        editingItem = db.getPantryItem(itemId);
        if (editingItem == null) {
            Toast.makeText(this, "That ingredient no longer exists", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ((TextView) findViewById(R.id.txtTitle)).setText("Edit ingredient");
        ((MaterialButton) findViewById(R.id.btnSave)).setText("Save changes");

        etName.setText(capitalise(editingItem.getName()));
        etQuantity.setText(PantryAdapter.formatQuantity(editingItem.getQuantity()));
        actvUnit.setText(editingItem.getUnit(), false);

        if (editingItem.getExpiryDate() != null && !editingItem.getExpiryDate().isEmpty()) {
            etExpiry.setText(editingItem.getExpiryDate());
            tilExpiry.setEndIconVisible(true);
        }
    }

    // =====================================================================
    //  Validation + saving
    // =====================================================================

    /** Validates every field; only if all are valid is the item saved (CREATE or UPDATE). */
    private void saveIngredient() {
        String name = textOf(etName).trim().replaceAll("\\s+", " ");
        String quantityText = textOf(etQuantity).trim();
        String unit = actvUnit.getText() == null ? "" : actvUnit.getText().toString().trim();
        String expiry = textOf(etExpiry).trim();

        boolean valid = true;

        // --- Name: required, 2-40 characters, letters/spaces/hyphens/apostrophes only
        if (name.isEmpty()) {
            tilName.setError("Enter an ingredient name");
            valid = false;
        } else if (name.length() < 2 || name.length() > 40) {
            tilName.setError("Use between 2 and 40 characters");
            valid = false;
        } else if (!name.matches("[\\p{L} '\\-]+")) {
            tilName.setError("Use letters only (no numbers or symbols)");
            valid = false;
        } else if (isDuplicate(name)) {
            tilName.setError("You already have this in your pantry. Edit that item instead.");
            valid = false;
        }

        // --- Quantity: required number, greater than 0, not absurdly large
        double quantity = 0;
        if (quantityText.isEmpty()) {
            tilQuantity.setError("Enter a quantity");
            valid = false;
        } else {
            try {
                quantity = Double.parseDouble(quantityText);
                if (quantity <= 0) {
                    tilQuantity.setError("Must be more than 0");
                    valid = false;
                } else if (quantity > 100000) {
                    tilQuantity.setError("That amount is too large");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                tilQuantity.setError("Enter a valid number");
                valid = false;
            }
        }

        // --- Unit: must be one from the list
        if (!Arrays.asList(RecipeMatcher.UNITS).contains(unit)) {
            tilUnit.setError("Choose a unit");
            valid = false;
        }

        // --- Expiry: optional, but must be a real yyyy-MM-dd date if given
        if (!expiry.isEmpty() && PantryAdapter.daysUntil(expiry) == Long.MAX_VALUE) {
            tilExpiry.setError("Pick a valid date");
            valid = false;
        }

        if (!valid) {
            return; // stay on the form so the user can fix the red errors
        }

        String storedName = name.toLowerCase(Locale.ROOT);
        String storedExpiry = expiry.isEmpty() ? null : expiry;

        if (editingItem == null) {
            // CREATE
            long newId = db.addPantryItem(new PantryItem(storedName, quantity, unit, storedExpiry));
            if (newId == -1) {
                Toast.makeText(this, "Could not save. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, capitalise(storedName) + " added to your pantry", Toast.LENGTH_SHORT).show();
        } else {
            // UPDATE
            editingItem.setName(storedName);
            editingItem.setQuantity(quantity);
            editingItem.setUnit(unit);
            editingItem.setExpiryDate(storedExpiry);
            db.updatePantryItem(editingItem);
            Toast.makeText(this, capitalise(storedName) + " updated", Toast.LENGTH_SHORT).show();
        }

        finish(); // back to the Pantry List, which reloads itself in onResume()
    }

    /**
     * True if another pantry item has the same ingredient name after normalising,
     * so "Tomatoes" is a duplicate of "tomato". The item being edited is ignored.
     */
    private boolean isDuplicate(String name) {
        String key = RecipeMatcher.normaliseName(name);
        for (PantryItem item : db.getAllPantryItems()) {
            boolean isSameRow = editingItem != null && item.getId() == editingItem.getId();
            if (!isSameRow && RecipeMatcher.normaliseName(item.getName()).equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static String textOf(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }

    private static String capitalise(String text) {
        if (TextUtils.isEmpty(text)) return "";
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }
}