package com.example.smartpantrymanager.model;

/**
 * Represents one ingredient the user currently has at home.
 * Maps to a single row in the "pantry_items" table.
 */
public class PantryItem {

    private long id;
    private String name;
    private double quantity;
    private String unit;
    private String expiryDate; // stored as "yyyy-MM-dd", may be null (optional field)

    /** Empty constructor, used when building an item step by step. */
    public PantryItem() {
    }

    /** Constructor for a new item that has not been saved yet (no id). */
    public PantryItem(String name, double quantity, String unit, String expiryDate) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
    }

    /** Constructor for an item loaded from the database (has an id). */
    public PantryItem(long id, String name, double quantity, String unit, String expiryDate) {
        this(name, quantity, unit, expiryDate);
        this.id = id;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
}