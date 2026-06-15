package com.example.weekendlegends;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;
import java.util.Random;

public class TossActivity extends AppCompatActivity {

    public static final String EXTRA_TEAM_A_NAME = "teamAName";
    public static final String EXTRA_TEAM_B_NAME = "teamBName";

    // result extras (keep your existing format)
    public static final String EXTRA_TOSS_WINNER = "tossWinner"; // "A" or "B"
    public static final String EXTRA_OPTED = "opted";            // "BAT" or "BOWL"

    private MaterialToolbar tb;
    private ImageView ivCoin;
    private TextView tvStatus;

    private MaterialButton btnToss;
    private RadioButton rbBat, rbBowl;
    private MaterialButton btnContinue;

    private String teamA = "Team A";
    private String teamB = "Team B";

    private String tossWinner = ""; // "A"/"B"
    private boolean isTossDone = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toss);

        tb = findViewById(R.id.tbToss);
        ivCoin = findViewById(R.id.ivCoin);
        ivCoin.setCameraDistance(getResources().getDisplayMetrics().density * 8000f);
        tvStatus = findViewById(R.id.tvTossStatus);

        btnToss = findViewById(R.id.btnDoToss);
        rbBat = findViewById(R.id.rbBat);
        rbBowl = findViewById(R.id.rbBowl);
        btnContinue = findViewById(R.id.btnContinue);

        Intent in = getIntent();
        teamA = safe(in.getStringExtra(EXTRA_TEAM_A_NAME), "Team A");
        teamB = safe(in.getStringExtra(EXTRA_TEAM_B_NAME), "Team B");

        tb.setTitle("Virtual Toss");
        tb.setSubtitle(teamA + " vs " + teamB);
        tb.setNavigationOnClickListener(v -> onBackPressed());

        // before toss: disable decision + continue
        setDecisionEnabled(false);
        btnContinue.setEnabled(false);

        // defaults
        rbBat.setChecked(true);

        btnToss.setOnClickListener(v -> doToss());
        btnContinue.setOnClickListener(v -> finishWithResult());
    }

    private void doToss() {
        if (isTossDone) {
            // allow re-toss if user wants
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Toss again?")
                    .setMessage("This will change the toss winner.")
                    .setPositiveButton("Toss again", (d, w) -> startCoinSpin())
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }
        startCoinSpin();
    }

    private void vibrateForce() {
        try {
            // 1) Haptic feedback fallback (works even when vibrator API is “ignored”)
            View root = getWindow().getDecorView();
            if (root != null) {
                root.performHapticFeedback(
                        android.view.HapticFeedbackConstants.CONFIRM,
                        android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                );
            }

            // 2) Real vibration
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                android.os.VibratorManager vm =
                        (android.os.VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                if (vm == null) return;

                android.os.Vibrator v = vm.getDefaultVibrator();
                if (v == null || !v.hasVibrator()) return;

                // DEFAULT_AMPLITUDE is often more reliable on OnePlus than 255
                v.vibrate(android.os.VibrationEffect.createOneShot(
                        180,
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE
                ));

            } else {
                android.os.Vibrator v =
                        (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (v == null || !v.hasVibrator()) return;

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(android.os.VibrationEffect.createOneShot(
                            180,
                            android.os.VibrationEffect.DEFAULT_AMPLITUDE
                    ));
                } else {
                    v.vibrate(180);
                }
            }
        } catch (Exception ignored) {}
    }



    private void startCoinSpin() {
        vibrateForce();

        isTossDone = false;
        tossWinner = "";
        btnContinue.setEnabled(false);
        setDecisionEnabled(false);
        tvStatus.setText("Flipping...");




        // 1️⃣ Jump up
        ObjectAnimator jumpUp = ObjectAnimator.ofFloat(ivCoin, View.TRANSLATION_Y, 0f, -100f);
        jumpUp.setDuration(200);

        // 2️⃣ Flip vertically (up–down)
        ObjectAnimator flip = ObjectAnimator.ofFloat(ivCoin, View.ROTATION_X, 0f, 2160f); // 6 flips
        flip.setDuration(1500);
        flip.setInterpolator(new LinearInterpolator());

        // 3️⃣ Fall back down
        ObjectAnimator fallDown = ObjectAnimator.ofFloat(ivCoin, View.TRANSLATION_Y, -100f, 0f);
        fallDown.setDuration(450);

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(jumpUp, flip, fallDown);

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                boolean aWon = new Random().nextBoolean();
                tossWinner = aWon ? "A" : "B";
                String name = aWon ? teamA : teamB;

                tvStatus.setText(name + " won the toss");
                isTossDone = true;

                setDecisionEnabled(true);
                btnContinue.setEnabled(true);
            }
        });

        set.start();
    }


    // helper: dp -> px
    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void setDecisionEnabled(boolean enabled) {
        rbBat.setEnabled(enabled);
        rbBowl.setEnabled(enabled);
        rbBat.setAlpha(enabled ? 1f : 0.5f);
        rbBowl.setAlpha(enabled ? 1f : 0.5f);
    }

    private void finishWithResult() {
        if (!isTossDone || tossWinner.isEmpty()) return;

        String opted = rbBat.isChecked() ? "BAT" : "BOWL";

        Intent out = new Intent();
        out.putExtra(EXTRA_TOSS_WINNER, tossWinner);
        out.putExtra(EXTRA_OPTED, opted);
        setResult(RESULT_OK, out);
        finish();
    }

    private String safe(String s, String fallback) {
        if (s == null) return fallback;
        s = s.trim();
        return s.isEmpty() ? fallback : s;
    }
}
