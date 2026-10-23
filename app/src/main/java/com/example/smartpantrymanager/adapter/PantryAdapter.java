package com.example.smartpantrymanager.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.R;
import com.example.smartpantrymanager.model.PantryItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Custom Adapter that connects the list of PantryItems (from the database)
 * to the RecyclerView on the Pantry List screen.
 *
 * For every item it inflates one copy of item_pantry.xml and fills in the
 * initial letter, name, quantity, unit and an expiry "pill".
 */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.PantryViewHolder> {

    /** Items expiring within this many days are highlighted in red. */
    public static final int EXPIRING_SOON_DAYS = 3;

    /** Lets the Activity decide what happens when a row is tapped or deleted. */
    public interface OnItemActionListener {
        void onItemClick(PantryItem item);
        void onDeleteClick(PantryItem item);
    }

    private final List<PantryItem> items = new ArrayList<>();
    private final OnItemActionListener listener;
    private boolean highlightExpiring = true;

    public PantryAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    /** Replaces the current list with fresh data from the database and redraws it. */
    public void setItems(List<PantryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    /** Turned on or off from the Settings screen ("Expiring-soon alerts"). */
    public void setHighlightExpiring(boolean highlight) {
        this.highlightExpiring = highlight;
        notifyDataSetChanged();
    }

    /** Called when the RecyclerView needs a new row view: inflate item_pantry.xml. */
    @NonNull
    @Override
    public PantryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pantry, parent, false);
        return new PantryViewHolder(view);
    }

    /** Called to show the data for one position in an existing row view. */
    @Override
    public void onBindViewHolder(@NonNull PantryViewHolder holder, int position) {
        PantryItem item = items.get(position);
        Context context = holder.itemView.getContext();

        String name = item.getName() == null ? "" : item.getName();
        holder.txtInitial.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT));
        holder.txtName.setText(capitalise(name));
        holder.txtQuantity.setText(formatQuantity(item.getQuantity()) + " " + item.getUnit());

        bindExpiry(holder.txtExpiry, item.getExpiryDate(), context);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));
    }

    /** Shows the expiry pill: red if expired / expiring soon, orange otherwise, hidden if no date. */
    private void bindExpiry(TextView pill, String expiry, Context context) {
        if (expiry == null || expiry.isEmpty()) {
            pill.setVisibility(View.GONE);
            return;
        }

        long daysLeft = daysUntil(expiry);
        String label;
        int background;
        int textColour;

        if (highlightExpiring && daysLeft != Long.MAX_VALUE && daysLeft < 0) {
            label = "Expired";
            background = R.color.red_light;
            textColour = R.color.warning_red;
        } else if (highlightExpiring && daysLeft <= EXPIRING_SOON_DAYS) {
            label = daysLeft == 0 ? "Expires today"
                    : "Expires in " + daysLeft + (daysLeft == 1 ? " day" : " days");
            background = R.color.red_light;
            textColour = R.color.warning_red;
        } else {
            label = "Exp " + formatShortDate(expiry);
            background = R.color.orange_light;
            textColour = R.color.orange_dark;
        }

        pill.setText(label);
        pill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, background)));
        pill.setTextColor(ContextCompat.getColor(context, textColour));
        pill.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // =====================================================================
    //  Small helpers (also used by MainActivity)
    // =====================================================================

    /** Shows 4.0 as "4" but keeps 1.5 as "1.5". */
    public static String formatQuantity(double qty) {
        if (qty == Math.floor(qty)) {
            return String.valueOf((long) qty);
        }
        return String.format(Locale.US, "%.2f", qty).replaceAll("0+$", "");
    }

    /**
     * Number of whole days from today until the given "yyyy-MM-dd" date.
     * 0 = today, negative = already expired, Long.MAX_VALUE = date could not be read.
     */
    public static long daysUntil(String isoDate) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setLenient(false);
        try {
            Date expiry = format.parse(isoDate);
            if (expiry == null) return Long.MAX_VALUE;

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            long diffMillis = expiry.getTime() - today.getTimeInMillis();
            return Math.round(diffMillis / (24.0 * 60 * 60 * 1000));
        } catch (ParseException e) {
            return Long.MAX_VALUE;
        }
    }

    /** True if the item has a date that is expired or within EXPIRING_SOON_DAYS. */
    public static boolean isExpiringSoon(PantryItem item) {
        String expiry = item.getExpiryDate();
        if (expiry == null || expiry.isEmpty()) return false;
        long days = daysUntil(expiry);
        return days != Long.MAX_VALUE && days <= EXPIRING_SOON_DAYS;
    }

    /** "2026-10-25" becomes "25 Oct". */
    private static String formatShortDate(String isoDate) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate);
            return date == null ? isoDate : new SimpleDateFormat("d MMM", Locale.US).format(date);
        } catch (ParseException e) {
            return isoDate;
        }
    }

    private static String capitalise(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    /** Holds references to the views inside one row so they are only looked up once. */
    static class PantryViewHolder extends RecyclerView.ViewHolder {
        final TextView txtInitial;
        final TextView txtName;
        final TextView txtQuantity;
        final TextView txtExpiry;
        final ImageButton btnDelete;

        PantryViewHolder(@NonNull View itemView) {
            super(itemView);
            txtInitial = itemView.findViewById(R.id.txtInitial);
            txtName = itemView.findViewById(R.id.txtName);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
            txtExpiry = itemView.findViewById(R.id.txtExpiry);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}