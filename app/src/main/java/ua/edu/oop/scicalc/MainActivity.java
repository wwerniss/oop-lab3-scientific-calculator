package ua.edu.oop.scicalc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowInsets;
import android.widget.GridLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ua.edu.oop.scicalc.core.AngleMode;
import ua.edu.oop.scicalc.core.CalculationException;
import ua.edu.oop.scicalc.core.CalculatorSession;
import ua.edu.oop.scicalc.core.HistoryEntry;

/** Android view layer: one handler for every calculator key and localized UI. */
public final class MainActivity extends Activity {
    private static final String PREFS = "scientific_calculator";
    private static final String KEY_ANSWER = "last_answer";
    private static final String KEY_LANGUAGE = "ui_language";
    private static final String KEY_MODE = "angle_mode";
    private static final String KEY_HISTORY = "history_json";
    private static final String STATE_EXPRESSION = "expression";
    private static final String STATE_RESULT = "result";
    private static final String STATE_EVALUATED = "evaluated";
    private static final String[][] KEY_ROWS = {
            {"DEG", "AC", "DEL", "(", ")"},
            {"sin", "cos", "tan", "ln", "log"},
            {"√", "x²", "xʸ", "1/x", "!"},
            {"7", "8", "9", "÷", "%"},
            {"4", "5", "6", "×", "π"},
            {"1", "2", "3", "−", "e"},
            {"0", ".", "Ans", "+", "="}
    };

    private final CalculatorSession session = new CalculatorSession();
    private TextView expressionView;
    private TextView resultView;
    private TextView modeLabel;
    private TextView modeKey;

    @Override
    protected void attachBaseContext(Context base) {
        String language = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, "system");
        if (language == null || language.equals("system")) {
            super.attachBaseContext(base);
        } else {
            Configuration configuration = new Configuration(base.getResources().getConfiguration());
            configuration.setLocale(Locale.forLanguageTag(language));
            super.attachBaseContext(base.createConfigurationContext(configuration));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppLogger.info("Calculator activity created");

        ScrollView root = findViewById(R.id.rootScroll);
        // Insets are needed for Android 15+ edge-to-edge layouts.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(),
                    insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(),
                    insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();

        expressionView = findViewById(R.id.expressionView);
        resultView = findViewById(R.id.resultView);
        modeLabel = findViewById(R.id.modeLabel);

        restoreState(savedInstanceState);
        buildKeypad(findViewById(R.id.keypad));
        findViewById(R.id.historyButton).setOnClickListener(view -> showHistory());
        findViewById(R.id.languageButton).setOnClickListener(view -> showLanguageChoice());
        resultView.setOnLongClickListener(view -> {
            copyResult();
            return true;
        });
        refresh();
    }

    private void buildKeypad(GridLayout grid) {
        for (int row = 0; row < KEY_ROWS.length; row++) {
            for (int col = 0; col < KEY_ROWS[row].length; col++) {
                String key = KEY_ROWS[row][col];
                TextView button = new TextView(this);
                button.setText(key);
                button.setTextColor(getColor(key.equals("=") ? R.color.button_dark_text : R.color.primary_text));
                button.setTypeface(Typeface.DEFAULT, key.equals("=") ? Typeface.BOLD : Typeface.NORMAL);
                button.setTextSize(key.equals("Ans") || key.equals("DEL") || key.equals("DEG") ? 14 : 18);
                button.setGravity(Gravity.CENTER);
                button.setSingleLine(true);
                button.setClickable(true);
                button.setFocusable(true);
                button.setContentDescription(descriptionFor(key));
                button.setBackground(makeBackground(key));

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col, 1, 1f);
                params.width = 0;
                params.height = dp(52);
                int margin = dp(3);
                params.setMargins(margin, margin, margin, margin);
                grid.addView(button, params);

                if (key.equals("DEG")) modeKey = button;
                button.setOnClickListener(view -> {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    String action = view == modeKey ? "DEG" : key;
                    session.press(action);
                    if (key.equals("=")) {
                        if (session.getError() != null) {
                            AppLogger.warn("Rejected invalid mathematical expression: " + session.getError());
                        } else {
                            AppLogger.info("Calculation completed successfully");
                            persistData();
                        }
                    }
                    refresh();
                });
            }
        }
    }

    private CharSequence descriptionFor(String key) {
        switch (key) {
            case "AC": return getString(R.string.clear);
            case "DEL": return getString(R.string.delete);
            case "=": return getString(R.string.equals);
            case "DEG": return getString(R.string.mode_button_description);
            default: return key;
        }
    }

    private RippleDrawable makeBackground(String key) {
        int color = R.color.button_number;
        if (key.equals("=")) color = R.color.button_equals;
        else if (key.equals("AC") || key.equals("DEL")) color = R.color.button_clear;
        else if (key.equals("+") || key.equals("−") || key.equals("×") || key.equals("÷"))
            color = R.color.button_operator;
        else if (!key.matches("[0-9.]")) color = R.color.button_function;
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(getColor(color));
        shape.setCornerRadius(dp(14));
        ColorStateList rippleColor = ColorStateList.valueOf(Color.argb(72, 255, 255, 255));
        return new RippleDrawable(rippleColor, shape, null);
    }

    private void refresh() {
        expressionView.setText(session.getExpression().isEmpty() ? "0" : session.getExpression());
        CalculationException.Type error = session.getError();
        if (error != null) {
            resultView.setText(errorString(error));
            resultView.setTextSize(20);
        } else {
            resultView.setText(session.getResult());
            resultView.setTextSize(37);
        }
        boolean degrees = session.getAngleMode() == AngleMode.DEGREES;
        modeLabel.setText(degrees ? R.string.degree_mode : R.string.radian_mode);
        if (modeKey != null) modeKey.setText(degrees ? "DEG" : "RAD");
    }

    private String errorString(CalculationException.Type error) {
        switch (error) {
            case SYNTAX: return getString(R.string.error_syntax);
            case DIVISION_BY_ZERO: return getString(R.string.error_division);
            case DOMAIN: return getString(R.string.error_domain);
            case OVERFLOW: return getString(R.string.error_overflow);
            case COMPLEXITY: return getString(R.string.error_complexity);
            default: return getString(R.string.error_generic);
        }
    }

    private void showLanguageChoice() {
        String[] languages = {getString(R.string.language_system), getString(R.string.language_uk),
                getString(R.string.language_en), getString(R.string.language_pl)};
        String[] codes = {"system", "uk", "en", "pl"};
        String current = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_LANGUAGE, "system");
        int selected = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(current)) selected = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.language_title)
                .setSingleChoiceItems(languages, selected, (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString(KEY_LANGUAGE, codes[which]).apply();
                    AppLogger.info("Interface language changed");
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(R.string.close, null)
                .show();
    }

    private void showHistory() {
        List<HistoryEntry> history = session.getHistory();
        if (history.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.history_title)
                    .setMessage(R.string.history_empty)
                    .setPositiveButton(R.string.close, null)
                    .show();
            return;
        }
        String[] entries = new String[history.size()];
        for (int i = 0; i < history.size(); i++) entries[i] = history.get(i).asLine();
        new AlertDialog.Builder(this)
                .setTitle(R.string.history_title)
                .setItems(entries, (dialog, which) -> {
                    // Load history result as a fresh expression, not executable code.
                    String previousResult = history.get(which).getResult();
                    session.restore(previousResult, previousResult, session.getLastAnswer(),
                            session.getAngleMode(), true);
                    refresh();
                })
                .setNeutralButton(R.string.history_clear, (dialog, which) -> {
                    session.clearHistory();
                    persistData();
                    AppLogger.info("Calculation history cleared");
                })
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void copyResult() {
        if (session.getError() != null || session.getResult().equals("…")) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.result_description), session.getResult()));
            Toast.makeText(this, R.string.copy_success, Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void restoreState(Bundle saved) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        double answer = Double.longBitsToDouble(prefs.getLong(KEY_ANSWER, Double.doubleToLongBits(0)));
        AngleMode mode = "RADIANS".equals(prefs.getString(KEY_MODE, "DEGREES"))
                ? AngleMode.RADIANS : AngleMode.DEGREES;
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_HISTORY, "[]"));
            List<HistoryEntry> entries = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                entries.add(new HistoryEntry(item.getString("expression"), item.getString("result")));
            }
            session.restoreHistory(entries);
        } catch (JSONException ex) {
            AppLogger.warn("Invalid stored history, starting empty");
            session.clearHistory();
        }
        if (saved == null) {
            session.restore("", "0", answer, mode, false);
        } else {
            session.restore(saved.getString(STATE_EXPRESSION, ""),
                    saved.getString(STATE_RESULT, "0"), answer, mode,
                    saved.getBoolean(STATE_EVALUATED, false));
        }
    }

    private void persistData() {
        JSONArray array = new JSONArray();
        for (HistoryEntry entry : session.getHistory()) {
            JSONObject item = new JSONObject();
            try {
                item.put("expression", entry.getExpression());
                item.put("result", entry.getResult());
                array.put(item);
            } catch (JSONException ex) {
                AppLogger.error("Could not serialize history entry", ex);
            }
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putLong(KEY_ANSWER, Double.doubleToLongBits(session.getLastAnswer()))
                .putString(KEY_MODE, session.getAngleMode().name())
                .putString(KEY_HISTORY, array.toString())
                .apply();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_EXPRESSION, session.getExpression());
        outState.putString(STATE_RESULT, session.getResult());
        outState.putBoolean(STATE_EVALUATED, session.isJustEvaluated());
        persistData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        persistData();
    }
}
