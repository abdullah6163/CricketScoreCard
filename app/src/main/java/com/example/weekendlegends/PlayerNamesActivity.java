package com.example.weekendlegends;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

public class PlayerNamesActivity extends AppCompatActivity {

    private MaterialToolbar tb;
    private LinearLayout containerTeamA, containerTeamB;
    private MaterialButton btnToSelection;

    private String teamAName, teamBName, tossWinner, opted;
    private int oversLimit, playersPerTeam;

    private final ArrayList<MaterialAutoCompleteTextView> teamAFields = new ArrayList<>();
    private final ArrayList<MaterialAutoCompleteTextView> teamBFields = new ArrayList<>();

    private static final String PREFS = "weekend_legends_prefs";
    private static final String KEY_SUGGESTIONS = "saved_player_names";

    private ArrayList<String> savedNames = new ArrayList<>();
    private ArrayAdapter<String> suggestionAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_names);

        tb = findViewById(R.id.tbNames);
        containerTeamA = findViewById(R.id.containerTeamA);
        containerTeamB = findViewById(R.id.containerTeamB);
        btnToSelection = findViewById(R.id.btnToSelection);

        teamAName = getIntent().getStringExtra("teamAName");
        teamBName = getIntent().getStringExtra("teamBName");
        oversLimit = getIntent().getIntExtra("oversLimit", 10);
        playersPerTeam = getIntent().getIntExtra("playersPerTeam", 1);
        tossWinner = getIntent().getStringExtra("tossWinner");
        opted = getIntent().getStringExtra("opted");

        tb.setNavigationOnClickListener(v -> finish());

        savedNames = loadSavedNames();

        if (!savedNames.isEmpty()) {
            suggestionAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    savedNames
            );
        }

        buildInputs(containerTeamA, teamAFields,
                safeName(teamAName, "Team A") + " player ",
                playersPerTeam);

        buildInputs(containerTeamB, teamBFields,
                safeName(teamBName, "Team B") + " player ",
                playersPerTeam);

        btnToSelection.setOnClickListener(v -> goSelection());
    }

    private void buildInputs(
            LinearLayout container,
            ArrayList<MaterialAutoCompleteTextView> fields,
            String labelPrefix,
            int count
    ) {
        container.removeAllViews();
        fields.clear();

        LayoutInflater inf = LayoutInflater.from(this);

        for (int i = 0; i < count; i++) {
            View row = inf.inflate(R.layout.row_player_name, container, false);

            TextInputLayout til = row.findViewById(R.id.tilPlayer);
            MaterialAutoCompleteTextView act = row.findViewById(R.id.actName);

            til.setHint(labelPrefix + (i + 1));

            act.setSaveEnabled(false);
            act.setText("", false);

            if (suggestionAdapter != null) {
                act.setAdapter(suggestionAdapter);
                act.setThreshold(1);

                final boolean[] selecting = {false};

                act.setOnItemClickListener((parent, view, position, id) -> {
                    selecting[0] = true;
                    act.dismissDropDown();
                    act.post(() -> {
                        selecting[0] = false;
                        if (act.getText() != null)
                            act.setSelection(act.getText().length());
                    });
                });

                act.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (selecting[0]) return;
                        if (!act.hasFocus()) return;

                        if (s != null && s.length() >= 1) {
                            act.showDropDown();
                        } else {
                            act.dismissDropDown();
                        }
                    }

                    @Override public void afterTextChanged(Editable s) {}
                });

                act.setOnFocusChangeListener((v, hasFocus) -> {
                    if (!hasFocus) act.dismissDropDown();
                });

            } else {
                act.setAdapter(null);
            }

            container.addView(row);
            fields.add(act);
        }
    }

    private void goSelection() {
        ArrayList<String> teamAPlayers = collectPlayers(teamAFields, "Team A");
        if (teamAPlayers == null) return;

        ArrayList<String> teamBPlayers = collectPlayers(teamBFields, "Team B");
        if (teamBPlayers == null) return;

        // ✅ NEW: block same name across teams
        if (!validateCrossTeamDuplicates(teamAPlayers, teamBPlayers)) return;

        saveNames(teamAPlayers, teamBPlayers);

        Intent i = new Intent(this, PlayerSelectionActivity.class);
        i.putExtra("teamAName", teamAName);
        i.putExtra("teamBName", teamBName);
        i.putExtra("oversLimit", oversLimit);
        i.putExtra("tossWinner", tossWinner);
        i.putExtra("opted", opted);

        i.putStringArrayListExtra("teamAPlayers", teamAPlayers);
        i.putStringArrayListExtra("teamBPlayers", teamBPlayers);

        startActivity(i);
    }

    private ArrayList<String> collectPlayers(
            ArrayList<MaterialAutoCompleteTextView> fields,
            String teamLabel
    ) {
        ArrayList<String> list = new ArrayList<>();
        HashSet<String> set = new HashSet<>();

        for (int i = 0; i < fields.size(); i++) {
            MaterialAutoCompleteTextView act = fields.get(i);
            String name = act.getText() == null ? "" : act.getText().toString().trim();

            if (name.isEmpty()) {
                act.setError(teamLabel + ": enter player " + (i + 1));
                act.requestFocus();
                return null;
            }

            String key = name.toLowerCase(Locale.US);
            if (set.contains(key)) {
                act.setError(teamLabel + ": duplicate name");
                act.requestFocus();
                return null;
            }

            set.add(key);
            list.add(name);
        }

        return list;
    }

    // ✅ NEW: cross-team duplicate check
    private boolean validateCrossTeamDuplicates(
            ArrayList<String> teamA,
            ArrayList<String> teamB
    ) {
        HashSet<String> set = new HashSet<>();
        for (String s : teamA) {
            set.add(s.toLowerCase(Locale.US));
        }

        for (int i = 0; i < teamB.size(); i++) {
            String name = teamB.get(i);
            if (set.contains(name.toLowerCase(Locale.US))) {
                teamBFields.get(i).setError("Same name already in Team A");
                teamBFields.get(i).requestFocus();
                return false;
            }
        }
        return true;
    }

    private ArrayList<String> loadSavedNames() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String raw = sp.getString(KEY_SUGGESTIONS, "");
        ArrayList<String> out = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return out;

        HashSet<String> uniq = new HashSet<>();
        for (String p : raw.split("\\|")) {
            String t = safe(p);
            if (uniq.add(t.toLowerCase(Locale.US))) out.add(t);
        }
        return out;
    }

    private void saveNames(ArrayList<String> a, ArrayList<String> b) {
        HashSet<String> uniq = new HashSet<>();
        ArrayList<String> merged = new ArrayList<>();

        for (String s : loadSavedNames()) addIfNew(merged, uniq, s);
        for (String s : a) addIfNew(merged, uniq, s);
        for (String s : b) addIfNew(merged, uniq, s);

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.edit().putString(KEY_SUGGESTIONS, String.join("|", merged)).apply();
    }

    private void addIfNew(ArrayList<String> merged, HashSet<String> uniq, String name) {
        String k = name.trim().toLowerCase(Locale.US);
        if (uniq.add(k)) merged.add(name.trim());
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private String safeName(String s, String fallback) {
        return (s == null || s.trim().isEmpty()) ? fallback : s.trim();
    }
}
