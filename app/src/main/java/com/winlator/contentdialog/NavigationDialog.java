package com.winlator.contentdialog;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.navigation.NavigationView;
import app.gamenative.R;

public class NavigationDialog extends ContentDialog {

    public static final int ACTION_KEYBOARD = 1;
    public static final int ACTION_INPUT_CONTROLS = 2;
    public static final int ACTION_EXIT_GAME = 3;
    public static final int ACTION_EDIT_CONTROLS = 4;
    public static final int ACTION_MANAGE_PROFILES = 5;

    public interface NavigationListener {
        void onNavigationItemSelected(int itemId);
    }

    public NavigationDialog(@NonNull Context context, NavigationListener listener) {
        this(context, listener, true);
    }

    public NavigationDialog(@NonNull Context context, NavigationListener listener, boolean controlsEnabled) {
        super(context, R.layout.navigation_dialog);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(R.drawable.navigation_dialog_background);
        }
        // Hide the title bar and bottom bar for a clean menu-only dialog
        findViewById(R.id.LLTitleBar).setVisibility(View.GONE);
        findViewById(R.id.LLBottomBar).setVisibility(View.GONE);

        GridLayout grid = findViewById(R.id.main_menu_grid);
        int orientation = context.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            grid.setColumnCount(5);
        } else {
            grid.setColumnCount(2);
        }

        addMenuItem(context, grid, R.drawable.icon_keyboard, R.string.keyboard, ACTION_KEYBOARD, listener);
        // Use different icon and opacity for toggle based on controls state
        int controlsIcon = R.drawable.icon_input_controls;
        addMenuItem(context, grid, controlsIcon, R.string.input_controls, ACTION_INPUT_CONTROLS, listener, controlsEnabled);
        addMenuItem(context, grid, R.drawable.icon_popup_menu_edit, R.string.edit_controls, ACTION_EDIT_CONTROLS, listener);
        addMenuItem(context, grid, R.drawable.icon_gamepad, R.string.manage_profiles, ACTION_MANAGE_PROFILES, listener);
        addMenuItem(context, grid, R.drawable.icon_exit, R.string.exit_game, ACTION_EXIT_GAME, listener);
    }

    private void addMenuItem(Context context, GridLayout grid, int iconRes, int titleRes, int itemId, NavigationListener listener) {
        addMenuItem(context, grid, iconRes, titleRes, itemId, listener, true);
    }

    private void addMenuItem(Context context, GridLayout grid, int iconRes, int titleRes, int itemId, NavigationListener listener, boolean enabled) {
        int padding = dpToPx(5, context);
        LinearLayout layout = new LinearLayout(context);
        layout.setPadding(padding, padding, padding, padding);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setOnClickListener(view -> {
            listener.onNavigationItemSelected(itemId);
            dismiss();
        });

        int size = dpToPx(40, context);
        View icon = new View(context);
        icon.setBackground(AppCompatResources.getDrawable(context, iconRes));
        if (icon.getBackground() != null) {
            int color = context.getColor(R.color.navigation_dialog_item_color);
            // If disabled, apply alpha to make it look crossed out / faded
            if (!enabled) {
                icon.setAlpha(0.4f);
            }
            icon.getBackground().setTint(color);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        icon.setLayoutParams(lp);
        layout.addView(icon);

        int width = dpToPx(96, context);
        TextView text = new TextView(context);
        text.setLayoutParams(new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
        text.setText(context.getString(titleRes));
        text.setGravity(Gravity.CENTER);
        text.setLines(2);
        text.setTextColor(context.getColor(R.color.navigation_dialog_item_color));
        if (!enabled) {
            text.setAlpha(0.4f);
        }
        Typeface tf = ResourcesCompat.getFont(context, R.font.bricolage_grotesque_regular);
        if (tf != null) {
            text.setTypeface(tf);
        }
        layout.addView(text);

        grid.addView(layout);
    }


    public int dpToPx(float dp, Context context){
        return (int) (dp * context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
