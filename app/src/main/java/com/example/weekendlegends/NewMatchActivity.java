package com.example.weekendlegends;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class NewMatchActivity extends AppCompatActivity {

    private static final String PREFS = "weekend_legends_prefs";
    private static final String KEY_LAST_TOSS_WINNER = "last_toss_winner"; // "A"/"B"
    private static final String KEY_LAST_OPTED = "last_opted";            // "BAT"/"BOWL"
    private static final String KEY_LAST_TEAM_A = "last_team_a";
    private static final String KEY_LAST_TEAM_B = "last_team_b";

    private TextInputEditText etTeamA, etTeamB, etOvers, etPlayers;

    private RadioGroup rgTossMode;
    private RadioButton rbTossNow, rbAlreadyTossed;

    private View boxTossNow;
    private MaterialButton btnVirtualToss;
    private TextView tvTossStatus;

    private View boxAlreadyTossed;
    private RadioGroup rgTossWinner, rgOpted;
    private RadioButton rbWinnerA, rbWinnerB, rbOptBat, rbOptBowl;

    private MaterialButton btnNext;

    private String pendingTeamA = "";
    private String pendingTeamB = "";
    private int pendingOvers = 0;
    private int pendingPlayersPerTeam = 0;

    private String tossWinner = ""; // "A"/"B"
    private String opted = "";      // "BAT"/"BOWL"

    private final ActivityResultLauncher<Intent> tossLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;

                String w = result.getData().getStringExtra(TossActivity.EXTRA_TOSS_WINNER);
                String o = result.getData().getStringExtra(TossActivity.EXTRA_OPTED);
                if (w == null) w = "";
                if (o == null) o = "";

                tossWinner = w;
                opted = o;

                String winnerName = tossWinner.equals("A") ? pendingTeamA : pendingTeamB;
                String optedText = opted.equalsIgnoreCase("BAT") ? "bat" : "bowl";
                tvTossStatus.setText("Toss: " + winnerName + " won and chose to " + optedText + ".");

                // ✅ SAVE toss info so it can be shown later in scorecard
                saveLastTossToPrefs();

                // ✅ IMPORTANT FIX:
                // After toss, go DIRECTLY to PlayerNamesActivity (do not stay here).
                goToPlayersAndFinish();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_match);

        MaterialToolbar tb = findViewById(R.id.tbNew);
        setSupportActionBar(tb);
        tb.setTitle("Match Settings");
        tb.setNavigationOnClickListener(v -> onBackPressed());

        etTeamA = findViewById(R.id.etTeamA);
        etTeamB = findViewById(R.id.etTeamB);
        etOvers = findViewById(R.id.etOvers);
        etPlayers = findViewById(R.id.etPlayers);

        rgTossMode = findViewById(R.id.rgTossMode);
        rbTossNow = findViewById(R.id.rbTossNow);
        rbAlreadyTossed = findViewById(R.id.rbAlreadyTossed);

        boxTossNow = findViewById(R.id.boxTossNow);
        btnVirtualToss = findViewById(R.id.btnVirtualToss);
        tvTossStatus = findViewById(R.id.tvTossStatus);

        boxAlreadyTossed = findViewById(R.id.boxAlreadyTossed);
        rgTossWinner = findViewById(R.id.rgTossWinner);
        rgOpted = findViewById(R.id.rgOpted);
        rbWinnerA = findViewById(R.id.rbWinnerA);
        rbWinnerB = findViewById(R.id.rbWinnerB);
        rbOptBat = findViewById(R.id.rbOptBat);
        rbOptBowl = findViewById(R.id.rbOptBowl);

        btnNext = findViewById(R.id.btnNext);

        // Next hidden initially
        btnNext.setVisibility(View.GONE);

        // Initial: no mode selected
        rgTossMode.clearCheck();
        applyModeNone();

        TextWatcher tw = new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) { syncTeamLabels(); }
        };
        etTeamA.addTextChangedListener(tw);
        etTeamB.addTextChangedListener(tw);
        syncTeamLabels();

        rgTossMode.setOnCheckedChangeListener((group, checkedId) -> {
            tossWinner = "";
            opted = "";
            btnNext.setVisibility(View.GONE);

            if (checkedId == R.id.rbTossNow) {
                applyModeTossNow();
            } else if (checkedId == R.id.rbAlreadyTossed) {
                applyModeAlreadyTossed();
            } else {
                applyModeNone();
            }
        });

        rgTossWinner.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.rbWinnerA) tossWinner = "A";
            else if (id == R.id.rbWinnerB) tossWinner = "B";
            updateNextForAlreadyTossed();
        });

        rgOpted.setOnCheckedChangeListener((g, id) -> {
            if (id == R.id.rbOptBat) opted = "BAT";
            else if (id == R.id.rbOptBowl) opted = "BOWL";
            updateNextForAlreadyTossed();
        });

        btnVirtualToss.setOnClickListener(v -> {
            if (!validateAndStorePending()) return;

            // Next stays hidden in Toss-now flow
            btnNext.setVisibility(View.GONE);

            Intent toss = new Intent(this, TossActivity.class);
            toss.putExtra(TossActivity.EXTRA_TEAM_A_NAME, pendingTeamA);
            toss.putExtra(TossActivity.EXTRA_TEAM_B_NAME, pendingTeamB);
            tossLauncher.launch(toss);
        });

        // Already-tossed flow uses Next button
        btnNext.setOnClickListener(v -> {
            if (!validateAndStorePending()) return;

            int mode = rgTossMode.getCheckedRadioButtonId();
            if (mode != R.id.rbAlreadyTossed) return;

            if (tossWinner.isEmpty() || opted.isEmpty()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Toss info missing")
                        .setMessage("Select who won the toss and what they opted to do.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            // ✅ SAVE toss info
            saveLastTossToPrefs();

            // ✅ go to players
            goToPlayersAndFinish();
        });
    }

    private void goToPlayersAndFinish() {
        Intent i = new Intent(this, PlayerNamesActivity.class);
        i.putExtra("teamAName", pendingTeamA);
        i.putExtra("teamBName", pendingTeamB);
        i.putExtra("oversLimit", pendingOvers);
        i.putExtra("playersPerTeam", pendingPlayersPerTeam);
        i.putExtra("tossWinner", tossWinner);
        i.putExtra("opted", opted);
        startActivity(i);
        finish(); // ✅ so you don't come back here after toss/players
    }

    private void saveLastTossToPrefs() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.edit()
                .putString(KEY_LAST_TEAM_A, pendingTeamA)
                .putString(KEY_LAST_TEAM_B, pendingTeamB)
                .putString(KEY_LAST_TOSS_WINNER, tossWinner)
                .putString(KEY_LAST_OPTED, opted)
                .apply();
    }

    private void applyModeNone() {
        boxTossNow.setVisibility(View.GONE);
        boxAlreadyTossed.setVisibility(View.GONE);
        tvTossStatus.setText("Not tossed yet");
        rgTossWinner.clearCheck();
        rgOpted.clearCheck();
        btnNext.setVisibility(View.GONE);
    }

    private void applyModeTossNow() {
        boxTossNow.setVisibility(View.VISIBLE);
        boxAlreadyTossed.setVisibility(View.GONE);
        tvTossStatus.setText("Not tossed yet");
        rgTossWinner.clearCheck();
        rgOpted.clearCheck();
        btnNext.setVisibility(View.GONE);
    }

    private void applyModeAlreadyTossed() {
        boxTossNow.setVisibility(View.GONE);
        boxAlreadyTossed.setVisibility(View.VISIBLE);
        tvTossStatus.setText("Not tossed yet");
        rgTossWinner.clearCheck();
        rgOpted.clearCheck();
        btnNext.setVisibility(View.GONE);
    }

    private void updateNextForAlreadyTossed() {
        if (rgTossMode.getCheckedRadioButtonId() != R.id.rbAlreadyTossed) return;
        boolean ready = !tossWinner.isEmpty() && !opted.isEmpty();
        btnNext.setVisibility(ready ? View.VISIBLE : View.GONE);
    }

    private void syncTeamLabels() {
        String a = raw(etTeamA);
        String b = raw(etTeamB);
        rbWinnerA.setText(a.isEmpty() ? "Team A" : a);
        rbWinnerB.setText(b.isEmpty() ? "Team B" : b);
    }

    private boolean validateAndStorePending() {
        String teamA = raw(etTeamA);
        String teamB = raw(etTeamB);
        String oversStr = raw(etOvers);
        String playersStr = raw(etPlayers);

        if (teamA.isEmpty() || teamB.isEmpty() || oversStr.isEmpty() || playersStr.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Missing information")
                    .setMessage("Please fill Team A, Team B, Overs, and Players per team.")
                    .setPositiveButton("OK", null)
                    .show();

            if (teamA.isEmpty()) etTeamA.setError("Required");
            if (teamB.isEmpty()) etTeamB.setError("Required");
            if (oversStr.isEmpty()) etOvers.setError("Required");
            if (playersStr.isEmpty()) etPlayers.setError("Required");
            return false;
        }

        if (teamA.equalsIgnoreCase(teamB)) {
            etTeamB.setError("Team names must be different");
            return false;
        }

        int overs = parseInt(etOvers, -1);
        int playersPerTeam = parseInt(etPlayers, -1);

        if (overs <= 0 || overs > 50) {
            etOvers.setError("Overs must be 1 to 50");
            return false;
        }

        if (playersPerTeam <= 0 || playersPerTeam > 25) {
            etPlayers.setError("Players must be 1 to 25");
            return false;
        }

        pendingTeamA = teamA;
        pendingTeamB = teamB;
        pendingOvers = overs;
        pendingPlayersPerTeam = playersPerTeam;

        syncTeamLabels();
        return true;
    }

    private String raw(TextInputEditText e) {
        if (e == null || e.getText() == null) return "";
        return e.getText().toString().trim();
    }

    private int parseInt(TextInputEditText e, int fallback) {
        try {
            if (e == null || e.getText() == null) return fallback;
            String s = e.getText().toString().trim();
            if (s.isEmpty()) return fallback;
            return Integer.parseInt(s);
        } catch (Exception ex) {
            return fallback;
        }
    }

    abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
