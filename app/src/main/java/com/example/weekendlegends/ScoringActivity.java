package com.example.weekendlegends;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import android.text.InputType;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ScoringActivity extends AppCompatActivity {

    public static final String EXTRA_GO_HOME_ON_BACK = "go_home_on_back";


    // ✅ Home should pass only matchId
    public static final String EXTRA_MATCH_ID = "matchId";

    // (keep for backward compatibility if you ever used it)
    public static final String EXTRA_MATCH_JSON = "match_json";

    // ✅ must match PlayerSelectionActivity constants
    public static final String EXTRA_TEAM_A_NAME = PlayerSelectionActivity.EXTRA_TEAM_A_NAME;
    public static final String EXTRA_TEAM_B_NAME = PlayerSelectionActivity.EXTRA_TEAM_B_NAME;
    public static final String EXTRA_OVERS_LIMIT = PlayerSelectionActivity.EXTRA_OVERS_LIMIT;
    public static final String EXTRA_PLAYERS_PER_TEAM = PlayerSelectionActivity.EXTRA_PLAYERS_PER_TEAM;

    public static final String EXTRA_TEAM_A_PLAYERS = PlayerSelectionActivity.EXTRA_TEAM_A_PLAYERS;
    public static final String EXTRA_TEAM_B_PLAYERS = PlayerSelectionActivity.EXTRA_TEAM_B_PLAYERS;

    public static final String EXTRA_TEAM_A_BATTING = PlayerSelectionActivity.EXTRA_TEAM_A_BATTING;

    public static final String EXTRA_STRIKER = PlayerSelectionActivity.EXTRA_STRIKER;
    public static final String EXTRA_NON_STRIKER = PlayerSelectionActivity.EXTRA_NON_STRIKER;
    public static final String EXTRA_BOWLER = PlayerSelectionActivity.EXTRA_BOWLER;

    public static final String EXTRA_TOSS_WINNER = PlayerSelectionActivity.EXTRA_TOSS_WINNER;
    public static final String EXTRA_TOSS_DECISION = PlayerSelectionActivity.EXTRA_OPTED;

    // --- bowling change correctness ---
    private final HashSet<String> currentOverBowlers = new HashSet<>(); // bowlers used in THIS over
    private final HashSet<String> lastOverBowlers = new HashSet<>();    // bowlers used in PREVIOUS over


    // ---------- WICKET TYPES ----------
    private static final String[] WICKET_TYPES = new String[]{
            "Bowled", "Caught", "LBW", "Run Out", "Stumped",
            "Hit Wicket", "Retired Hurt", "Timed Out", "Obstructing the field"
    };

    // ✅ next legal ball is Free Hit (after a No Ball)
    private boolean pendingFreeHit = false;
    private int effectToken = 0;


    // ---------- Full ball-by-ball history ----------
    private final JSONArray ballEvents = new JSONArray();

    // ---------- UI ----------
    private View vConfettiDim;
    private View vCelebrationDim;
    private TextView tvCelebration;
    private nl.dionsegijn.konfetti.KonfettiView konfettiView;

    private static final long CELEBRATION_MS = 5200;







    private MaterialToolbar tb;
    private TextView tvInnings, tvScoreLine, tvCRR, tvOverProgressLarge;

    private TextView tvBatAName, tvBatAR, tvBatAB, tvBatA4, tvBatA6, tvBatASR;
    private TextView tvBatBName, tvBatBR, tvBatBB, tvBatB4, tvBatB6, tvBatBSR;

    private TextView tvBowlerName, tvBowlerO, tvBowlerR, tvBowlerW, tvBowlerEcon;

    private TextView tvThisOverTotal;


    private RecyclerView rvThisOver;
    private BallAdapter thisOverAdapter;
    private final ArrayList<BallItem> thisOverBalls = new ArrayList<>();

    private CheckBox cbWide, cbNoBall, cbByes;
    private MaterialButton btnUndo, btnSwap, btn0, btn1, btn2, btn3, btn4, btn5, btn6, btnW;

    private TextView tvChaseLine, tvRRR;

    // ---------- Match meta ----------
    private String teamA = "", teamB = "";
    private int oversLimit = 5;
    private int playersPerTeam = 7;

    private ArrayList<String> teamAPlayers = new ArrayList<>();
    private ArrayList<String> teamBPlayers = new ArrayList<>();

    private boolean teamABatting = true;

    private String striker = "", nonStriker = "", bowler = "";

    // toss
    private String tossWinner = "";
    private String tossDecision = "";

    // match identity
    private String matchId = "";
    private long matchDateUtc = 0L;
    private boolean isCompleted = false;

    // ---------- State ----------
    private int totalRuns = 0;
    private int wickets = 0;

    private final JSONObject batterEntries = new JSONObject(); // name -> count (1..2)


    private int overNumber = 0;   // completed overs
    private int ballInOver = 0;   // 0..5 legal balls in current over

    private final JSONObject batsmanStats = new JSONObject(); // name -> {r,b,4s,6s}
    private final JSONObject bowlerStats = new JSONObject();  // name -> {balls,r,w}

    private String lastOverBowler = null;

    // ✅ undo history (must be persisted, but snapshots must NOT include undoStack)
    private final ArrayDeque<JSONObject> undoStack = new ArrayDeque<>();

    private static final String PREFS = "weekend_legends_prefs";
    private static final String KEY_INPROGRESS = "match_in_progress_json";

    // ---------- 2 INNINGS ----------
    private int innings = 1; // 1 or 2
    private int firstInningsRuns = 0;
    private int target = 0;

    private boolean dialogOpen = false;

    // pending wicket delivery
    private int pendingRuns = 0;
    private boolean pendingWide = false, pendingNoBall = false, pendingByes = false;
    private String pendingWicketType = "";
    private String pendingFielder = "";




    // ✅ NEW: who got out (for Run Out)
    private boolean pendingOutIsStriker = true;
    private String pendingOutBatsman = "";


    private JSONObject innings1Snapshot = new JSONObject();
    private JSONObject innings2Snapshot = new JSONObject();

    private View viewDim;
    private ImageView ivEffectGif;


    // hat-trick tracking

    private String htBowler = "";
    private int htCount = 0;


    // batsman milestone (avoid repeating 50/100)
    private final HashSet<String> shownHalfCentury = new HashSet<>();
    private final HashSet<String> shownCentury = new HashSet<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoring);

        bindViews();
        setupThisOverRecycler();

        String mid = getIntent().getStringExtra(ScoringActivity.EXTRA_MATCH_ID);

        String raw = null;
        if (mid != null && !mid.trim().isEmpty()) {
            raw = MatchStore.getMatchJsonById(this, mid);
        }

// fallback (only if you still support old flow)
        if (raw == null || raw.trim().isEmpty()) {
            raw = getIntent().getStringExtra(ScoringActivity.EXTRA_MATCH_JSON);
        }

        if (raw == null || raw.trim().isEmpty()) {
            finish();
            return;
        }

        JSONObject match;
        try {
            match = new JSONObject(raw);
        } catch (Exception e) {
            finish();
            return;
        }

// now use match json normally...




        // ✅ ONE clean loading flow only (no duplicates)
        loadMatchStateOrFreshStart();

        teamAPlayers = cleanPlayers(teamAPlayers);
        teamBPlayers = cleanPlayers(teamBPlayers);

        ensureBatsman(striker);
        ensureBatsman(nonStriker);
        ensureBowler(bowler);

        setupToolbar();
        setupButtons();
        setupBackPress();

        renderAll();

        snapshotCurrentInnings();
        persistMatchState(false);
    }

    private void loadMatchStateOrFreshStart() {
        // 1) Continue from Home (matchId)
        String mid = getIntent().getStringExtra(EXTRA_MATCH_ID);
        if (mid != null && !mid.trim().isEmpty()) {
            String raw = MatchStore.getMatchJsonById(this, mid); // ✅ must return String
            if (raw != null && !raw.trim().isEmpty()) {
                try {
                    importMatchJson(new JSONObject(raw));
                    return;
                } catch (Exception ignored) {}
            }
        }

        // 2) Backward compatibility: old flow (EXTRA_MATCH_JSON)
        String raw = getIntent().getStringExtra(EXTRA_MATCH_JSON);
        if (raw != null && !raw.trim().isEmpty()) {
            try {
                importMatchJson(new JSONObject(raw));
                return;
            } catch (Exception ignored) {}
        }

        // 3) Fresh start
        readIntentFresh();

        // ✅ baseline undo state
        undoStack.clear();
    }

    private void bindViews() {
        tb = findViewById(R.id.tbScore);

        tvInnings = findViewById(R.id.tvInnings);
        tvScoreLine = findViewById(R.id.tvScoreLine);
        tvCRR = findViewById(R.id.tvCRR);
        tvOverProgressLarge = findViewById(R.id.tvOverProgressLarge);

        tvBatAName = findViewById(R.id.tvBatAName);
        tvBatAR = findViewById(R.id.tvBatAR);
        tvBatAB = findViewById(R.id.tvBatAB);
        tvBatA4 = findViewById(R.id.tvBatA4);
        tvBatA6 = findViewById(R.id.tvBatA6);
        tvBatASR = findViewById(R.id.tvBatASR);

        tvBatBName = findViewById(R.id.tvBatBName);
        tvBatBR = findViewById(R.id.tvBatBR);
        tvBatBB = findViewById(R.id.tvBatBB);
        tvBatB4 = findViewById(R.id.tvBatB4);
        tvBatB6 = findViewById(R.id.tvBatB6);
        tvBatBSR = findViewById(R.id.tvBatBSR);

        tvBowlerName = findViewById(R.id.tvBowlerName);
        tvBowlerO = findViewById(R.id.tvBowlerO);
        tvBowlerR = findViewById(R.id.tvBowlerR);
        tvBowlerW = findViewById(R.id.tvBowlerW);
        tvBowlerEcon = findViewById(R.id.tvBowlerEcon);

        tvThisOverTotal = findViewById(R.id.tvThisOverTotal);

        rvThisOver = findViewById(R.id.rvThisOver);

        cbWide = findViewById(R.id.cbWide);
        cbNoBall = findViewById(R.id.cbNoBall);
        cbByes = findViewById(R.id.cbByes);

        btnUndo = findViewById(R.id.btnUndo);
        btnSwap = findViewById(R.id.btnSwap);

        btn0 = findViewById(R.id.btn0);
        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);
        btn4 = findViewById(R.id.btn4);
        btn5 = findViewById(R.id.btn5);
        btn6 = findViewById(R.id.btn6);
        btnW = findViewById(R.id.btnW);

        tvChaseLine = findViewById(R.id.tvChaseLine);
        tvRRR = findViewById(R.id.tvRRR);
        setupNameClickEditors();

        konfettiView = findViewById(R.id.konfettiView);
        vCelebrationDim = findViewById(R.id.vCelebrationDim);
        tvCelebration = findViewById(R.id.tvCelebration);
        viewDim = findViewById(R.id.viewDim);
        ivEffectGif = findViewById(R.id.ivEffectGif);






    }

    private boolean containsIgnoreCase(java.util.Set<String> set, String name) {
        if (set == null) return false;
        String n = safe(name).toLowerCase(java.util.Locale.US);
        for (String s : set) {
            if (safe(s).toLowerCase(java.util.Locale.US).equals(n)) return true;
        }
        return false;
    }

    private java.util.HashSet<String> getBowlersUsedInOver(int inn, int overNo) {
        java.util.HashSet<String> used = new java.util.HashSet<>();
        for (int i = 0; i < ballEvents.length(); i++) {
            org.json.JSONObject e = ballEvents.optJSONObject(i);
            if (e == null) continue;

            if (e.optInt("innings", 1) != inn) continue;
            if (e.optInt("overNo", -1) != overNo) continue;

            String b = e.optString("bowler", "");
            if (!safe(b).isEmpty()) used.add(b);
        }
        return used;
    }

    private void showRunOutWhoOutDialog() {
        if (dialogOpen) return;
        dialogOpen = true;

        String[] who = new String[]{
                "Striker: " + striker,
                "Non-striker: " + nonStriker
        };

        final int[] idx = {0};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Run Out - Who is out?")
                .setSingleChoiceItems(who, 0, (d, which) -> idx[0] = which)
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    dialogOpen = false;

                    pendingOutIsStriker = (idx[0] == 0);
                    pendingOutBatsman = pendingOutIsStriker ? striker : nonStriker;

                    applyDelivery(
                            pendingRuns, pendingWide, pendingNoBall, pendingByes,
                            true, pendingWicketType, pendingFielder,
                            pendingOutBatsman
                    );
                })
                .show();
    }



    private boolean isBowlerCreditedWicket(JSONObject e) {
        if (e == null) return false;
        if (!e.optBoolean("wicket", false)) return false;

        String type = safe(e.optString("wicketType", "")).toLowerCase(Locale.US);

        // NOT credited to bowler
        if (type.contains("run out")) return false;
        if (type.contains("retired")) return false;         // retired hurt etc
        if (type.contains("timed out")) return false;
        if (type.contains("obstruct")) return false;        // obstructing the field

        // credited wickets
        return true; // bowled/caught/lbw/stumped/hit wicket
    }

    private void recomputeHatTrickState() {
        htBowler = "";
        htCount = 0;

        // Go backwards through SAME innings until streak breaks.
        for (int i = ballEvents.length() - 1; i >= 0; i--) {
            JSONObject e = ballEvents.optJSONObject(i);
            if (e == null) continue;

            if (e.optInt("innings", 1) != innings) break;

            // If this delivery is NOT a bowler-credited wicket => streak broken
            if (!isBowlerCreditedWicket(e)) break;

            String b = safe(e.optString("bowler", ""));
            if (b.isEmpty()) break;

            if (htCount == 0) {
                htBowler = b;
                htCount = 1;
            } else {
                // Must be same bowler for hat-trick
                if (!b.equalsIgnoreCase(htBowler)) break;
                htCount++;
            }

            if (htCount >= 3) break;
        }
    }



    private void playGifEffect(int gifRes, int durationMs, @Nullable Runnable onDone) {
        final int token = ++effectToken;

        // show overlay + dim
        ivEffectGif.setVisibility(View.VISIBLE);
        ivEffectGif.setAlpha(1f);

        viewDim.setVisibility(View.VISIBLE);
        viewDim.setAlpha(0f);
        viewDim.animate().alpha(0.55f).setDuration(120).start();

        // IMPORTANT: clear previous load so replay restarts
        Glide.with(this).clear(ivEffectGif);

        // load GIF properly
        Glide.with(this)
                .asGif()
                .load(gifRes)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(new CustomTarget<GifDrawable>() {
                    @Override
                    public void onResourceReady(GifDrawable resource, @Nullable Transition<? super GifDrawable> transition) {
                        if (token != effectToken) return;

                        // restart from beginning
                        resource.stop();
                        resource.start();
                        ivEffectGif.setImageDrawable(resource);

                        // hide after duration
                        ivEffectGif.removeCallbacks(null);
                        ivEffectGif.postDelayed(() -> {
                            if (token != effectToken) return;

                            ivEffectGif.animate()
                                    .alpha(0f)
                                    .setDuration(200)
                                    .withEndAction(() -> {
                                        if (token != effectToken) return;

                                        Glide.with(ScoringActivity.this).clear(ivEffectGif);
                                        ivEffectGif.setImageDrawable(null);
                                        ivEffectGif.setVisibility(View.GONE);

                                        viewDim.setVisibility(View.GONE);

                                        if (onDone != null) onDone.run();
                                    })
                                    .start();
                        }, durationMs);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // no-op
                    }
                });
    }







    private void showWinnerFireworks(String winnerTeamName) {
        if (konfettiView == null || tvCelebration == null || vCelebrationDim == null) return;

        // show overlay
        vCelebrationDim.setVisibility(View.VISIBLE);
        konfettiView.setVisibility(View.VISIBLE);
        tvCelebration.setVisibility(View.VISIBLE);

        tvCelebration.setText(winnerTeamName + " WON! 🏆");

        // animate text (pop + bounce)
        tvCelebration.setScaleX(0.7f);
        tvCelebration.setScaleY(0.7f);
        tvCelebration.setAlpha(0f);
        tvCelebration.animate()
                .alpha(1f)
                .scaleX(1f).scaleY(1f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();

        playFireworks();

        // auto hide after some time
        konfettiView.postDelayed(() -> {
            tvCelebration.animate().alpha(0f).setDuration(250).start();
            konfettiView.setVisibility(View.GONE);
            tvCelebration.setVisibility(View.GONE);
            vCelebrationDim.setVisibility(View.GONE);
        }, 5200);
    }

    private void playFireworks() {
        int[] colors = new int[] {
                android.graphics.Color.parseColor("#FFD54F"),
                android.graphics.Color.parseColor("#00E5FF"),
                android.graphics.Color.parseColor("#8BFF63"),
                android.graphics.Color.parseColor("#FF3B30"),
                android.graphics.Color.parseColor("#B388FF")
        };

        int w = konfettiView.getWidth();
        int h = konfettiView.getHeight();
        if (w == 0 || h == 0) {
            // layout not ready yet
            konfettiView.post(this::playFireworks);
            return;
        }

        // Fireworks = repeated upward bursts from random x positions at bottom
        java.util.Random rnd = new java.util.Random();

        for (int i = 0; i < 9; i++) {
            int delay = i * 350;

            konfettiView.postDelayed(() -> {
                float x = (0.15f + rnd.nextFloat() * 0.70f) * w; // spread across width
                float y = h * 1.02f; // slightly below screen

                konfettiView.build()
                        .addColors(colors)
                        .setDirection(250.0, 290.0)     // mostly upward
                        .setSpeed(12f, 28f)             // strong burst
                        .setFadeOutEnabled(true)
                        .setTimeToLive(3000L)           // longer
                        .addShapes(
                                nl.dionsegijn.konfetti.models.Shape.Circle.INSTANCE,
                                nl.dionsegijn.konfetti.models.Shape.Square.INSTANCE
                        )
                        .addSizes(
                                new nl.dionsegijn.konfetti.models.Size(14, 6f),
                                new nl.dionsegijn.konfetti.models.Size(10, 4f)
                        )
                        .setPosition(x, x, y, y)
                        .burst(180);
            }, delay);
        }

        // Final big center boom
        konfettiView.postDelayed(() -> {
            konfettiView.build()
                    .addColors(colors)
                    .setDirection(0.0, 359.0)          // full spread explosion
                    .setSpeed(10f, 24f)
                    .setFadeOutEnabled(true)
                    .setTimeToLive(3800L)
                    .addShapes(
                            nl.dionsegijn.konfetti.models.Shape.Circle.INSTANCE,
                            nl.dionsegijn.konfetti.models.Shape.Square.INSTANCE
                    )
                    .addSizes(new nl.dionsegijn.konfetti.models.Size(16, 6f))
                    .setPosition(w * 0.2f, w * 0.8f, h * 0.25f, h * 0.55f)
                    .burst(420);
        }, 3400);
    }

    private void setupNameClickEditors() {
        tvBatAName.setOnClickListener(v -> showRenamePlayerDialog(striker));
        tvBatBName.setOnClickListener(v -> showRenamePlayerDialog(nonStriker));
        tvBowlerName.setOnClickListener(v -> showRenamePlayerDialog(bowler));
    }


    private void showRenamePlayerDialog(String oldName) {
        oldName = safe(oldName);
        if (oldName.isEmpty()) return;
        if (dialogOpen) return;
        dialogOpen = true;

        final String oldFinal = oldName; // ✅ IMPORTANT for lambda

        final com.google.android.material.textfield.TextInputEditText et =
                new com.google.android.material.textfield.TextInputEditText(this);
        et.setText(oldFinal);
        et.setSelection(oldFinal.length());

        final com.google.android.material.textfield.TextInputLayout til =
                new com.google.android.material.textfield.TextInputLayout(this);
        til.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        til.setHint("Correct name");
        til.addView(et);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit name")
                .setView(til)
                .setCancelable(true)
                .setPositiveButton("Save", (d, w) -> {
                    dialogOpen = false;

                    String newName = safe(et.getText() == null ? "" : et.getText().toString());
                    if (newName.isEmpty()) return;

                    // same name (case-insensitive) => nothing
                    if (newName.equalsIgnoreCase(oldFinal)) return;

                    // prevent duplicates
                    if (nameExistsInMatch(newName, oldFinal)) {
                        toast("Name already exists");
                        return;
                    }

                    // ✅ Undo snapshot first

                    renameEverywhere(oldFinal, newName);

                    renderAll();
                    snapshotCurrentInnings();
                    persistMatchState(false);
                })
                .setNegativeButton("Cancel", (d, w) -> dialogOpen = false)
                .setOnDismissListener(d -> dialogOpen = false)
                .show();
    }


    /**
     * Returns true if newName already exists in either team,
     * excluding the player we are renaming (oldName).
     */

    private boolean nameExistsInMatch(String newName, String oldName) {
        String n = safe(newName).toLowerCase(Locale.US);
        String old = safe(oldName).toLowerCase(Locale.US);

        for (String p : teamAPlayers) {
            String x = safe(p).toLowerCase(Locale.US);
            if (x.equals(n) && !x.equals(old)) return true;
        }
        for (String p : teamBPlayers) {
            String x = safe(p).toLowerCase(Locale.US);
            if (x.equals(n) && !x.equals(old)) return true;
        }
        return false;
    }

    private void renameEverywhere(String oldNameRaw, String newNameRaw) {
        String oldName = safe(oldNameRaw);
        String newName = safe(newNameRaw);
        if (oldName.isEmpty() || newName.isEmpty()) return;

        // 1) current active names (case-insensitive)
        if (oldName.equalsIgnoreCase(striker)) striker = newName;
        if (oldName.equalsIgnoreCase(nonStriker)) nonStriker = newName;
        if (oldName.equalsIgnoreCase(bowler)) bowler = newName;
        if (lastOverBowler != null && oldName.equalsIgnoreCase(lastOverBowler)) lastOverBowler = newName;

        // 2) team lists
        replaceNameInList(teamAPlayers, oldName, newName);
        replaceNameInList(teamBPlayers, oldName, newName);

        // 3) batsmanStats key rename
        renameStatsKey(batsmanStats, oldName, newName, true);

        // 4) bowlerStats key rename
        renameStatsKey(bowlerStats, oldName, newName, false);

        // 5) ballEvents rename (striker/nonStriker/bowler + wicketBy + fielder)
        for (int i = 0; i < ballEvents.length(); i++) {
            JSONObject e = ballEvents.optJSONObject(i);
            if (e == null) continue;

            if (oldName.equalsIgnoreCase(e.optString("striker"))) safePutString(e, "striker", newName);
            if (oldName.equalsIgnoreCase(e.optString("nonStriker"))) safePutString(e, "nonStriker", newName);
            if (oldName.equalsIgnoreCase(e.optString("bowler"))) safePutString(e, "bowler", newName);
            if (oldName.equalsIgnoreCase(e.optString("wicketBy"))) safePutString(e, "wicketBy", newName);
            if (oldName.equalsIgnoreCase(e.optString("fielder"))) safePutString(e, "fielder", newName);
        }

        // 6) snapshots (so scorecard shows renamed player)
        renameInsideSnapshot(innings1Snapshot, oldName, newName);
        renameInsideSnapshot(innings2Snapshot, oldName, newName);

        // 7) ensure new keys exist
        ensureBatsman(striker);
        ensureBatsman(nonStriker);
        ensureBowler(bowler);
    }


    private void replaceNameInList(ArrayList<String> list, String oldName, String newName) {
        if (list == null) return;
        for (int i = 0; i < list.size(); i++) {
            if (safe(list.get(i)).equalsIgnoreCase(oldName)) {
                list.set(i, newName);
            }
        }
    }

    private void renameStatsKey(JSONObject stats, String oldName, String newName, boolean isBatsman) {
        if (stats == null) return;

        String oldKey = findExistingKeyCaseInsensitive(stats, oldName);
        String newKey = findExistingKeyCaseInsensitive(stats, newName);

        if (oldKey == null) return;

        // if new doesn't exist: simple rename
        if (newKey == null) {
            JSONObject val = stats.optJSONObject(oldKey);
            stats.remove(oldKey);
            try { stats.put(newName, val == null ? new JSONObject() : val); } catch (Exception ignored) {}
            return;
        }

        // both exist => merge then remove old
        JSONObject a = stats.optJSONObject(oldKey);
        JSONObject b = stats.optJSONObject(newKey);
        if (a != null && b != null) {
            if (isBatsman) {
                safePutInt(b, "r", b.optInt("r") + a.optInt("r"));
                safePutInt(b, "b", b.optInt("b") + a.optInt("b"));
                safePutInt(b, "4s", b.optInt("4s") + a.optInt("4s"));
                safePutInt(b, "6s", b.optInt("6s") + a.optInt("6s"));
            } else {
                safePutInt(b, "balls", b.optInt("balls") + a.optInt("balls"));
                safePutInt(b, "r", b.optInt("r") + a.optInt("r"));
                safePutInt(b, "w", b.optInt("w") + a.optInt("w"));
            }
        }
        stats.remove(oldKey);

        // ensure key uses the exact newName spelling in map
        if (!newKey.equals(newName)) {
            JSONObject val = stats.optJSONObject(newKey);
            stats.remove(newKey);
            try { stats.put(newName, val == null ? new JSONObject() : val); } catch (Exception ignored) {}
        }
    }

    private String findExistingKeyCaseInsensitive(JSONObject obj, String name) {
        if (obj == null) return null;
        String target = safe(name).toLowerCase(Locale.US);

        JSONArray names = obj.names();
        if (names == null) return null;

        for (int i = 0; i < names.length(); i++) {
            String k = names.optString(i);
            if (safe(k).toLowerCase(Locale.US).equals(target)) return k;
        }
        return null;
    }
    private void renameInsideSnapshot(JSONObject snap, String oldName, String newName) {
        if (snap == null) return;

        JSONObject bs = snap.optJSONObject("batsmanStats");
        if (bs != null) renameKeyCaseInsensitive(bs, oldName, newName);

        JSONObject bw = snap.optJSONObject("bowlerStats");
        if (bw != null) renameKeyCaseInsensitive(bw, oldName, newName);
    }
    private void renameKeyCaseInsensitive(JSONObject obj, String oldName, String newName) {
        if (obj == null) return;

        String oldKey = findExistingKeyCaseInsensitive(obj, oldName);
        if (oldKey == null) return;

        String newKey = findExistingKeyCaseInsensitive(obj, newName);

        try {
            if (newKey == null) {
                obj.put(newName, obj.get(oldKey));
            } else {
                // If both exist, keep newKey and just remove oldKey
                // (optional: merge here too if you want)
            }
            obj.remove(oldKey);

            // normalize spelling to newName if newKey existed but different casing
            if (newKey != null && !newKey.equals(newName) && obj.has(newKey)) {
                Object val = obj.get(newKey);
                obj.remove(newKey);
                obj.put(newName, val);
            }
        } catch (Exception ignored) {}
    }



    private void safePutString(JSONObject o, String k, String v) {
        if (o == null) return;
        try { o.put(k, v); } catch (Exception ignored) {}
    }

    private void toast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }





    private void setupThisOverRecycler() {
        rvThisOver.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvThisOver.setNestedScrollingEnabled(false);
        rvThisOver.setHasFixedSize(false);

        thisOverAdapter = new BallAdapter(thisOverBalls);
        rvThisOver.setAdapter(thisOverAdapter);
    }

    private void markBatsmanEntered(String name) {
        if (name == null || name.trim().isEmpty()) return;
        ensureBatsman(name);
        int c = batterEntries.optInt(name, 0);
        try { batterEntries.put(name, c + 1); } catch (Exception ignored) {}
    }

    private void showRetireDialog() {
        if (dialogOpen) return;
        dialogOpen = true;

        String[] who = new String[]{
                "Retire Striker: " + striker,
                "Retire Non-striker: " + nonStriker
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Retire batsman")
                .setItems(who, (d, which) -> {
                    dialogOpen = false;
                    if (which == 0) retireAndReplace(true);
                    else retireAndReplace(false);
                })
                .setOnCancelListener(d -> dialogOpen = false)
                .show();
    }
    private void retireAndReplace(boolean retireStriker) {
        // undo snapshot (so undo restores names too)

        String retiring = retireStriker ? striker : nonStriker;

        ArrayList<String> candidates = new ArrayList<>();
        for (String p : cleanPlayers(getBattingList())) {
            if (p.equalsIgnoreCase(striker) || p.equalsIgnoreCase(nonStriker)) continue;

            // allow max 2 entries
            int entries = batterEntries.optInt(p, 0);
            if (entries >= 2) continue;

            candidates.add(p);
        }

        if (candidates.isEmpty()) {
            // nothing to replace with
            renderAll();
            snapshotCurrentInnings();
            persistMatchState(false);
            return;
        }

        final int[] idx = {0};
        String[] items = candidates.toArray(new String[0]);

        dialogOpen = true;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Select replacement")
                .setSingleChoiceItems(items, 0, (d, which) -> idx[0] = which)
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    String selected = items[idx[0]];
                    markBatsmanEntered(selected); // entry count++

                    if (retireStriker) striker = selected;
                    else nonStriker = selected;

                    dialogOpen = false;
                    renderAll();
                    snapshotCurrentInnings();
                    persistMatchState(false);
                })
                .show();
    }




    private void readIntentFresh() {
        Intent i = getIntent();

        teamA = safe(i.getStringExtra(EXTRA_TEAM_A_NAME));
        teamB = safe(i.getStringExtra(EXTRA_TEAM_B_NAME));

        oversLimit = i.getIntExtra(EXTRA_OVERS_LIMIT, 20);
        playersPerTeam = i.getIntExtra(EXTRA_PLAYERS_PER_TEAM, 11);

        teamABatting = i.getBooleanExtra(EXTRA_TEAM_A_BATTING, true);

        ArrayList<String> a = i.getStringArrayListExtra(EXTRA_TEAM_A_PLAYERS);
        ArrayList<String> b = i.getStringArrayListExtra(EXTRA_TEAM_B_PLAYERS);
        if (a != null) teamAPlayers = a;
        if (b != null) teamBPlayers = b;

        striker = safe(i.getStringExtra(EXTRA_STRIKER));
        nonStriker = safe(i.getStringExtra(EXTRA_NON_STRIKER));
        bowler = safe(i.getStringExtra(EXTRA_BOWLER));

        tossWinner = safe(i.getStringExtra(EXTRA_TOSS_WINNER));
        tossDecision = safe(i.getStringExtra(EXTRA_TOSS_DECISION));

        if (striker.isEmpty() && getBattingList().size() > 0) striker = getBattingList().get(0);
        if (nonStriker.isEmpty() && getBattingList().size() > 1) nonStriker = getBattingList().get(1);
        markBatsmanEntered(striker);
        markBatsmanEntered(nonStriker);
        if (bowler.isEmpty() && getBowlingList().size() > 0) bowler = getBowlingList().get(0);

        // ✅ IMPORTANT: Use matchId/matchDateUtc if sent (PlayerSelection/Home flow)
        String incomingId = i.getStringExtra(PlayerSelectionActivity.EXTRA_MATCH_ID);
        long incomingDate = i.getLongExtra(PlayerSelectionActivity.EXTRA_MATCH_DATE_UTC, 0L);

        if (incomingId != null && !incomingId.trim().isEmpty()) {
            matchId = incomingId.trim();
        }

        if (incomingDate > 0) {
            matchDateUtc = incomingDate;
        }

        // ✅ If still missing, generate new
        if (matchDateUtc <= 0) matchDateUtc = System.currentTimeMillis();
        if (matchId == null || matchId.trim().isEmpty()) {
            matchId = "m_" + matchDateUtc;
        }

        isCompleted = false;
    }


    private void setupToolbar() {
        String title = teamA + " vs " + teamB;
        tb.setTitle(title);

        if (title.length() > 20) {
            tb.setTitleTextAppearance(this, R.style.ToolbarTitleSmall);
        }
        String subtitle = getBattingTeamName() + (innings == 2 ? " chasing" : " batting");
        tb.setSubtitle(subtitle);
        if (subtitle.length() > 20) {
            tb.setSubtitleTextAppearance(this, R.style.ToolbarSubtitleSmall);
        }
        tb.setNavigationOnClickListener(v -> confirmLeaveMatch());

        tb.getMenu().clear();
        tb.inflateMenu(R.menu.menu_scoring);
        tb.setOnMenuItemClickListener(this::onToolbarItem);
    }

    private boolean onToolbarItem(MenuItem item) {

        if (item.getItemId() == R.id.action_details) {

            snapshotCurrentInnings();
            persistMatchState(false);

            Intent i = new Intent(this, ScorecardActivity.class);
            i.putExtra(EXTRA_MATCH_ID, matchId);   // ✅ ONLY ID
            i.putExtra(EXTRA_GO_HOME_ON_BACK, false);
            startActivity(i);

            return true;
        }


        if (item.getItemId() == R.id.action_settings) {
            showSettingsDialog();
            return true;
        }


        return false;
    }

    private void showSettingsDialog() {
        if (dialogOpen) return;
        dialogOpen = true;

        // Settings ONLY in 1st innings
        if (innings != 1) {
            dialogOpen = false;
            toast("Settings can be changed only in 1st innings.");
            return;
        }

        String[] items = new String[] {
                "Change Total Overs",
                "Change Total Wickets",
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Match Settings")
                .setItems(items, (d, which) -> {
                    dialogOpen = false;
                    if (which == 0) showChangeOversDialog();
                    else if (which == 1) showChangeWicketsIncreaseOnlyDialog();
                })
                .setOnDismissListener(dd -> dialogOpen = false)
                .show();
    }

    private void showChangeWicketsIncreaseOnlyDialog() {
        if (innings != 1) { toast("Only in 1st innings"); return; }

        int currentMaxWkts = getMaxWicketsForCurrentInnings();


        com.google.android.material.textfield.TextInputEditText et =
                new com.google.android.material.textfield.TextInputEditText(this);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(currentMaxWkts));
        et.setSelection(et.getText() != null ? et.getText().length() : 0);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Change Total Wickets ")
                .setMessage("Current max wickets: " + currentMaxWkts +
                        "\nWickets fallen: " + wickets)
                .setView(et)
                .setPositiveButton("Next", (d, w) -> {
                    String txt = et.getText() == null ? "" : et.getText().toString().trim();
                    if (txt.isEmpty()) return;

                    int newMax;
                    try { newMax = Integer.parseInt(txt); }
                    catch (Exception e) { toast("Invalid number"); return; }

                    if (newMax < wickets) {
                        toast("Cannot set below wickets fallen (" + wickets + ")");
                        return;
                    }
                    if (newMax <= currentMaxWkts) {
                        toast("Only increasing is allowed.");
                        return;
                    }

                    int batCount = cleanPlayers(getBattingList()).size();
                    int newBatCount = newMax + 1; // since max wickets = players - 1
                    int addCount = newBatCount - batCount;


                    showAddPlayersForBothTeamsDialog(addCount, () -> {
                        // ✅ undo snapshot BEFORE apply

                        teamAPlayers = cleanPlayers(teamAPlayers);
                        teamBPlayers = cleanPlayers(teamBPlayers);

// keep playersPerTeam updated only for old UI/older code (optional)
                        playersPerTeam = Math.max(teamAPlayers.size(), teamBPlayers.size());


                        renderAll();
                        snapshotCurrentInnings();
                        persistMatchState(false);

                        toast("Max wickets updated to " + newMax);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddPlayersForBothTeamsDialog(int addCount, Runnable onDone) {
        if (addCount <= 0) { if (onDone != null) onDone.run(); return; }

        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);

        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        sv.addView(root);

        android.widget.TextView aTitle = new android.widget.TextView(this);
        aTitle.setText("Add " + addCount + " player to " + (teamA.isEmpty() ? "Team A" : teamA));
        aTitle.setTextColor(0xFFEAF2FF);
        root.addView(aTitle);

        ArrayList<com.google.android.material.textfield.TextInputEditText> aInputs = new ArrayList<>();
        for (int i = 0; i < addCount; i++) {
            com.google.android.material.textfield.TextInputLayout til =
                    new com.google.android.material.textfield.TextInputLayout(this);
            til.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
            til.setHint("Team A player " + (i + 1));

            com.google.android.material.textfield.TextInputEditText et =
                    new com.google.android.material.textfield.TextInputEditText(this);
            til.addView(et);
            root.addView(til);
            aInputs.add(et);
        }

        android.widget.TextView bTitle = new android.widget.TextView(this);
        bTitle.setText("\nAdd " + addCount + " player(s) to " + (teamB.isEmpty() ? "Team B" : teamB));
        bTitle.setTextColor(0xFFEAF2FF);
        root.addView(bTitle);

        ArrayList<com.google.android.material.textfield.TextInputEditText> bInputs = new ArrayList<>();
        for (int i = 0; i < addCount; i++) {
            com.google.android.material.textfield.TextInputLayout til =
                    new com.google.android.material.textfield.TextInputLayout(this);
            til.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
            til.setHint("Team B player " + (i + 1));

            com.google.android.material.textfield.TextInputEditText et =
                    new com.google.android.material.textfield.TextInputEditText(this);
            til.addView(et);
            root.addView(til);
            bInputs.add(et);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add New Players")
                .setView(sv)
                .setCancelable(false)
                .setPositiveButton("Add", (d, w) -> {

                    ArrayList<String> addA = new ArrayList<>();
                    ArrayList<String> addB = new ArrayList<>();

                    for (com.google.android.material.textfield.TextInputEditText et : aInputs) {
                        String name = safe(et.getText() == null ? "" : et.getText().toString());
                        if (name.isEmpty()) { toast("Fill all Team A names"); return; }
                        if (nameExistsInMatchExcept(name, "")) { toast("Name already exists: " + name); return; }
                        addA.add(name);
                    }
                    for (com.google.android.material.textfield.TextInputEditText et : bInputs) {
                        String name = safe(et.getText() == null ? "" : et.getText().toString());
                        if (name.isEmpty()) { toast("Fill all Team B names"); return; }
                        if (nameExistsInMatchExcept(name, "")) { toast("Name already exists: " + name); return; }
                        addB.add(name);
                    }

                    // apply add
                    teamAPlayers.addAll(addA);
                    teamBPlayers.addAll(addB);

                    if (onDone != null) onDone.run();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private boolean nameExistsInMatchExcept(String name, String exceptOld) {
        String n = safe(name).toLowerCase(java.util.Locale.US);
        String ex = safe(exceptOld).toLowerCase(java.util.Locale.US);

        for (String p : teamAPlayers) {
            String x = safe(p).toLowerCase(java.util.Locale.US);
            if (x.equals(n) && !x.equals(ex)) return true;
        }
        for (String p : teamBPlayers) {
            String x = safe(p).toLowerCase(java.util.Locale.US);
            if (x.equals(n) && !x.equals(ex)) return true;
        }
        return false;
    }






    private void showChangeOversDialog() {
        if (innings != 1) {
            toast("You can change overs only in 1st innings.");
            return;
        }

        int minOvers = getMinOversAllowed();
        int currentOversLimit = oversLimit;

        TextInputEditText et = new TextInputEditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(currentOversLimit));
        et.setSelection(et.getText() != null ? et.getText().length() : 0);

        TextInputLayout til = new TextInputLayout(this);
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        til.addView(et);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Change Overs")
                .setMessage("Overs played: " + overNumber + "." + ballInOver +
                        "\nMinimum allowed: " + minOvers)
                .setView(til)
                .setPositiveButton("Apply", (d, w) -> {
                    String txt = et.getText() == null ? "" : et.getText().toString().trim();
                    if (txt.isEmpty()) return;

                    int newOvers;
                    try { newOvers = Integer.parseInt(txt); }
                    catch (Exception e) { toast("Invalid overs"); return; }

                    if (newOvers < minOvers) {
                        toast("Overs cannot be less than " + minOvers);
                        return;
                    }


                    oversLimit = newOvers;
                    renderAll();
                    snapshotCurrentInnings();
                    persistMatchState(false);

                    toast("Overs updated to " + newOvers);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int getMinOversAllowed() {
        if (ballInOver == 0) return Math.max(1, overNumber);
        return Math.max(1, overNumber + 1);
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { confirmLeaveMatch(); }
        });
    }

    private void confirmLeaveMatch() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Leave match?")
                .setMessage("Match will stay In Progress on Home.")
                .setPositiveButton("Leave", (d, w) -> {
                    snapshotCurrentInnings();
                    persistMatchState(false);

                    Intent i = new Intent(this, HomeActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton("Stay", null)
                .show();
    }

    private void setupButtons() {
        View.OnClickListener runClick = v -> {
            int runs = Integer.parseInt(((MaterialButton) v).getText().toString());
            onDelivery(runs, false);
        };

        btn0.setOnClickListener(runClick);
        btn1.setOnClickListener(runClick);
        btn2.setOnClickListener(runClick);
        btn3.setOnClickListener(runClick);
        btn4.setOnClickListener(runClick);
        btn5.setOnClickListener(runClick);
        btn6.setOnClickListener(runClick);

        btnW.setOnClickListener(v -> onDelivery(0, true));
        btnSwap.setText("Change");
        btnSwap.setOnClickListener(v -> showChangeDialog());

        btnUndo.setOnClickListener(v -> undo());
    }

    private void showChangeDialog() {
        if (dialogOpen) return;
        dialogOpen = true;

        String[] items = new String[]{
                "Change batsman",
                "Change bowler",
                "Swap striker"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Change")
                .setItems(items, (d, which) -> {
                    dialogOpen = false;
                    if (which == 0) showChangeBatsmanDialog();
                    else if (which == 1) promptNewBowler(false); // ✅ manual change mid-over allowed
                    else if (which == 2) swapStrike();
                })
                .setOnCancelListener(d -> dialogOpen = false)
                .show();
    }

    private void recomputeBatterEntriesFromBallEvents() {
        // reset
        clearJson(batterEntries);

        // init all batting players to 0 (for current match state)
        for (String p : cleanPlayers(teamAPlayers)) {
            try { batterEntries.put(p, 0); } catch (Exception ignored) {}
        }
        for (String p : cleanPlayers(teamBPlayers)) {
            try { batterEntries.put(p, 0); } catch (Exception ignored) {}
        }

        // conservative rebuild: if a batter appears in any ball as striker/nonStriker => entries = 1
        // (this avoids false "2 entries" after undo; retired-hurt re-entry = advanced logic, optional)
        for (int i = 0; i < ballEvents.length(); i++) {
            JSONObject e = ballEvents.optJSONObject(i);
            if (e == null) continue;

            String s = safe(e.optString("striker", ""));
            String ns = safe(e.optString("nonStriker", ""));

            if (!s.isEmpty() && batterEntries.optInt(s, 0) == 0) {
                try { batterEntries.put(s, 1); } catch (Exception ignored) {}
            }
            if (!ns.isEmpty() && batterEntries.optInt(ns, 0) == 0) {
                try { batterEntries.put(ns, 1); } catch (Exception ignored) {}
            }
        }

        // ensure current live pair at least 1
        if (!safe(striker).isEmpty() && batterEntries.optInt(striker, 0) == 0) {
            try { batterEntries.put(striker, 1); } catch (Exception ignored) {}
        }
        if (!safe(nonStriker).isEmpty() && batterEntries.optInt(nonStriker, 0) == 0) {
            try { batterEntries.put(nonStriker, 1); } catch (Exception ignored) {}
        }
    }

    private HashSet<String> getOutBattersForCurrentInnings() {
        HashSet<String> out = new HashSet<>();

        for (int i = 0; i < ballEvents.length(); i++) {
            JSONObject e = ballEvents.optJSONObject(i);
            if (e == null) continue;

            if (e.optInt("innings", 1) != innings) continue;
            if (!e.optBoolean("wicket", false)) continue;

            String wt = safe(e.optString("wicketType", ""));
            // Retired Hurt is NOT out (for change batsman / availability)
            if (wt.equalsIgnoreCase("Retired Hurt")) continue;

            // ✅ prefer explicit outBatsman (run out/non-striker)
            String dismissed = safe(e.optString("outBatsman", ""));

            // fallback only if missing
            if (dismissed.isEmpty()) dismissed = safe(e.optString("striker", ""));

            // ✅ THIS was missing
            if (!dismissed.isEmpty()) out.add(dismissed);
        }

        return out;
    }

    private void showChangeBatsmanDialog() {
        if (dialogOpen) return;
        dialogOpen = true;

        String[] who = new String[]{
                "Replace Striker: " + striker,
                "Replace Non-striker: " + nonStriker
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Change batsman")
                .setItems(who, (d, which) -> {
                    dialogOpen = false;
                    boolean replaceStriker = (which == 0);
                    showSelectReplacementBatsman(replaceStriker);
                })
                .setOnCancelListener(d -> dialogOpen = false)
                .show();
    }

    private void showSelectReplacementBatsman(boolean replaceStriker) {
        if (dialogOpen) return;
        dialogOpen = true;

        HashSet<String> outBatters = getOutBattersForCurrentInnings();

        ArrayList<String> candidates = new ArrayList<>();
        for (String p : cleanPlayers(getBattingList())) {
            if (p.equalsIgnoreCase(striker) || p.equalsIgnoreCase(nonStriker)) continue;
            if (containsIgnoreCase(outBatters, p)) continue;
           // ✅ no out batsmen
            candidates.add(p);
        }

        if (candidates.isEmpty()) {
            dialogOpen = false;
            toast("No replacement available");
            return;
        }

        String[] items = candidates.toArray(new String[0]);
        final int[] idx = {0};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select replacement")
                .setSingleChoiceItems(items, 0, (d, which) -> idx[0] = which)
                .setCancelable(true)
                .setPositiveButton("OK", (d, w) -> {
                    dialogOpen = false;


                    String selected = items[idx[0]];
                    ensureBatsman(selected); // ✅ new guy starts fresh if never batted

                    if (replaceStriker) striker = selected;
                    else nonStriker = selected;

                    renderAll();
                    snapshotCurrentInnings();
                    persistMatchState(false);
                })
                .setNegativeButton("Cancel", (d, w) -> dialogOpen = false)
                .show();
    }







    // ========================= DELIVERY =========================

    private void onDelivery(int runs, boolean wicketPressed) {
        if (dialogOpen) return;

        boolean wide = cbWide.isChecked();
        boolean noBall = cbNoBall.isChecked();
        boolean byes = cbByes.isChecked();
        resetExtras();

        if (isInningsOver()) {
            finishInningsNow();
            return;
        }

        if (innings == 2 && totalRuns >= target) {
            snapshotCurrentInnings();
            showMatchResultDialog();
            return;
        }

        if ((wide || noBall) && byes) return;

        if (wicketPressed) {
            pendingRuns = runs;
            pendingWide = wide;
            pendingNoBall = noBall;
            pendingByes = byes;
            pendingWicketType = "";
            pendingFielder = "";
            showWicketTypeDialog();
            return;
        }

        applyDelivery(runs, wide, noBall, byes, false, "", "", "");

    }

    private void showWicketTypeDialog() {
        if (dialogOpen) return;
        dialogOpen = true;

        final int[] selected = {0};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Wicket Type")
                .setSingleChoiceItems(WICKET_TYPES, 0, (d, which) -> selected[0] = which)
                .setCancelable(false)
                .setPositiveButton("Next", (d, w) -> {
                    dialogOpen = false;
                    pendingWicketType = WICKET_TYPES[selected[0]];
                    pendingFielder = "";

                    if (needsFielder(pendingWicketType)) {
                        showFielderDialog();
                    } else {
                        // ✅ default dismissed = striker for non-runout wickets
                        pendingOutIsStriker = true;
                        pendingOutBatsman = striker;

                        if (pendingWicketType.equalsIgnoreCase("Run Out")) {
                            showRunOutWhoOutDialog();
                        } else {
                            applyDelivery(pendingRuns, pendingWide, pendingNoBall, pendingByes,
                                    true, pendingWicketType, pendingFielder,
                                    pendingOutBatsman);
                        }
                    }
                })
                .show();
    }

    private boolean needsFielder(String wicketType) {
        if (wicketType == null) return false;
        String t = wicketType.trim().toLowerCase(Locale.US);
        return t.contains("caught") || t.contains("run out") || t.contains("stumped");
    }

    private void showFielderDialog() {
        if (dialogOpen) return;
        dialogOpen = true;

        ArrayList<String> fielders = cleanPlayers(getBowlingList());
        if (fielders.isEmpty()) {
            dialogOpen = false;
            pendingFielder = "";
            // ✅ for Run Out, ask who got out
            pendingOutIsStriker = true;
            pendingOutBatsman = striker;

            if (pendingWicketType.equalsIgnoreCase("Run Out")) {
                showRunOutWhoOutDialog();
            } else {
                applyDelivery(pendingRuns, pendingWide, pendingNoBall, pendingByes,
                        true, pendingWicketType, pendingFielder,
                        pendingOutBatsman);
            }

            return;
        }

        final int[] idx = {0};
        String[] items = fielders.toArray(new String[0]);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Fielder")
                .setSingleChoiceItems(items, 0, (d, which) -> idx[0] = which)
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    dialogOpen = false;
                    pendingFielder = items[idx[0]];
                    // ✅ for Run Out, ask who got out
                    pendingOutIsStriker = true;
                    pendingOutBatsman = striker;

                    if (pendingWicketType.equalsIgnoreCase("Run Out")) {
                        showRunOutWhoOutDialog();
                    } else {
                        applyDelivery(pendingRuns, pendingWide, pendingNoBall, pendingByes,
                                true, pendingWicketType, pendingFielder,
                                pendingOutBatsman);
                    }

                })
                .show();
    }

    private boolean isBowlerCreditedWicketType(String wicketType) {
        String t = safe(wicketType).toLowerCase(Locale.US);
        if (t.contains("run out")) return false;
        if (t.contains("retired")) return false;
        if (t.contains("timed out")) return false;
        if (t.contains("obstruct")) return false;
        return true;
    }


    private void applyDelivery(int runs,
                               boolean wide,
                               boolean noBall,
                               boolean byes,
                               boolean wicket,
                               String wicketType,
                               String fielder,
                               String outBatsman) {

        if (isInningsOver()) {
            finishInningsNow();
            return;
        }

        // ✅ snapshot for undo (must be before any mutations)

        // ✅ FREEZE WHO FACED THIS BALL (MUST be before ANY swap)
        final String strikerAtBall = striker;
        final String nonStrikerAtBall = nonStriker;
        final String bowlerAtBall = bowler;

        boolean legalBall = !(wide || noBall);

        // ✅ track bowler used in THIS over (even if wide/no-ball)
        if (!safe(bowlerAtBall).isEmpty()) {
            currentOverBowlers.add(bowlerAtBall);
        }

        boolean showNoBallFreeHitGif = noBall;

        // free hit state
        if (legalBall) pendingFreeHit = false;
        if (noBall) pendingFreeHit = true;

        int eventOverNo = overNumber;
        int eventBallInOver = ballInOver;

        int teamRuns, batRuns, bowlerRuns;

        if (wide) {
            teamRuns = 1 + runs;
            bowlerRuns = 1 + runs;
            batRuns = 0;
        } else if (noBall) {
            teamRuns = 1 + runs;
            bowlerRuns = 1 + runs;
            batRuns = runs;
        } else if (byes) {
            teamRuns = runs;
            batRuns = 0;
            bowlerRuns = 0;
        } else {
            teamRuns = runs;
            bowlerRuns = runs;
            batRuns = runs;
        }

        totalRuns += teamRuns;

        // ---------------- BEFORE stats (milestones) ----------------
        ensureBatsman(strikerAtBall);
        JSONObject sBefore = batsmanStats.optJSONObject(strikerAtBall);
        int strikerRunsBefore = (sBefore != null) ? sBefore.optInt("r") : 0;

        // ---------------- Update striker stats ----------------
        JSONObject s = batsmanStats.optJSONObject(strikerAtBall);
        if (s != null) {
            if (batRuns > 0) {
                safePutInt(s, "r", s.optInt("r") + batRuns);
                if (batRuns == 4) safePutInt(s, "4s", s.optInt("4s") + 1);
                if (batRuns == 6) safePutInt(s, "6s", s.optInt("6s") + 1);
            }
            if (legalBall) safePutInt(s, "b", s.optInt("b") + 1);
        }

        int strikerRunsAfter = (s != null) ? s.optInt("r") : strikerRunsBefore;

        // ---------------- Update bowler stats ----------------
        ensureBowler(bowlerAtBall);
        JSONObject bw = bowlerStats.optJSONObject(bowlerAtBall);
        if (bw != null) {
            safePutInt(bw, "r", bw.optInt("r") + bowlerRuns);
            if (legalBall) safePutInt(bw, "balls", bw.optInt("balls") + 1);

            boolean creditedToBowler = wicket && isBowlerCreditedWicketType(wicketType);
            if (creditedToBowler) safePutInt(bw, "w", bw.optInt("w") + 1);
        }

        // ✅ Team wicket should NOT increase for Retired Hurt
        boolean countsAsTeamWicket = wicket && !safe(wicketType).equalsIgnoreCase("Retired Hurt");
        if (countsAsTeamWicket) wickets++;

        // ---------------- Over progression (DEFER swaps if wicket) ----------------
        boolean overFinishedNow = false;
        boolean pendingSwapEndOver = false;
        boolean pendingSwapOddRuns = false;

        if (legalBall) {
            ballInOver++;
            if (ballInOver >= 6) {
                overFinishedNow = true;
                overNumber++;
                ballInOver = 0;

                // ✅ DO NOT swap now if wicket happened
                if (!wicket) {
                    swapStrikeInternal();
                } else {
                    pendingSwapEndOver = true;
                }

                lastOverBowlers.clear();
                lastOverBowlers.addAll(currentOverBowlers);
                currentOverBowlers.clear();
            }
        }

        // strike change for odd runs (NOT wides) — also DEFER if wicket
        if (!wide && (runs % 2 == 1)) {
            if (!wicket) swapStrikeInternal();
            else pendingSwapOddRuns = true;
        }

        String label = buildEventLabel(runs, wide, noBall, byes, wicket);

        // ✅ Hat-trick state before adding ball
        recomputeHatTrickState();
        String prevHtBowler = htBowler;
        int prevHtCount = htCount;

        String outName = wicket ? safe(outBatsman) : "";

        // ✅ Save ball event USING frozen names (pre-swap)
        pushBallEvent(eventOverNo, eventBallInOver, label, teamRuns, legalBall,
                wide, noBall, byes, wicket, wicketType, fielder,
                strikerAtBall, nonStrikerAtBall, bowlerAtBall,
                outName);

        // ✅ Hat-trick state after adding ball
        recomputeHatTrickState();

        renderAll();
        snapshotCurrentInnings();
        persistMatchState(false);

        final boolean overFinishedFinal = overFinishedNow;
        final boolean wicketFinal = wicket;

        // ✅ IMPORTANT: use STRIKER/NONSTRIKER AT BALL, not current (may change later)
        final boolean outIsStrikerFinal =
                wicketFinal && safe(outBatsman).equalsIgnoreCase(strikerAtBall);

        // capture deferred swaps
        final boolean deferOdd = pendingSwapOddRuns;
        final boolean deferEnd = pendingSwapEndOver;

        Runnable applyDeferredSwaps = () -> {
            // Apply in correct order:
            if (deferOdd) swapStrikeInternal();
            if (deferEnd) swapStrikeInternal();
        };

        Runnable nextFlow = () -> {

            if (isInningsOver()) {
                finishInningsNow();
                return;
            }

            if (innings == 2 && totalRuns >= target) {
                showMatchResultDialog();
                return;
            }

            if (wicketFinal && overFinishedFinal) {
                promptNewBatsman(outIsStrikerFinal, () -> {
                    applyDeferredSwaps.run();
                    promptNewBowler(false);
                });
                return;
            }

            if (wicketFinal) {
                promptNewBatsman(outIsStrikerFinal, () -> {
                    applyDeferredSwaps.run();
                    // ✅ state settled after batsman is chosen + swaps applied
                    commitPostStateToLastEvent();
                    persistMatchState(false);
                });
                return;
            }

            if (overFinishedFinal) {
                promptNewBowler(true);
                return;
            }

            // ✅ no dialogs, state is final now
            commitPostStateToLastEvent();
            persistMatchState(false);
        };

        // ---------------- PICK ONE EFFECT (ONLY ONE) ----------------
        int effectRes = 0;
        int effectMs = 2400;

        boolean hatTrickJustHappened =
                (htCount >= 3) &&
                        (prevHtCount < 3 || !safe(prevHtBowler).equalsIgnoreCase(htBowler));

        // ✅ Determine OUT batsman's runs for duck animation
        int outRunsAfter = 0;
        if (wicketFinal) {
            String outKey = findExistingKeyCaseInsensitive(batsmanStats, outBatsman);
            JSONObject outObj = (outKey == null) ? null : batsmanStats.optJSONObject(outKey);
            outRunsAfter = (outObj != null) ? outObj.optInt("r", 0) : 0;
        }

        if (hatTrickJustHappened) {
            effectRes = R.drawable.hat_trick;
            effectMs = 3500;
        }
        else if (strikerRunsBefore < 100 && strikerRunsAfter >= 100) {
            effectRes = R.drawable.century;
        }
        else if (strikerRunsBefore < 50 && strikerRunsAfter >= 50) {
            effectRes = R.drawable.half_century;
        }
        else if (wicketFinal) {
            // ✅ duck depends on OUT batsman, not striker
            effectRes = (outRunsAfter == 0) ? R.drawable.duck_out : R.drawable.wicket;
        }
        else if (showNoBallFreeHitGif) {
            effectRes = R.drawable.free_hit;
            effectMs = 2200;
        }
        else if (batRuns == 6) {
            effectRes = R.drawable.six;
            effectMs = 1800;
        }
        else if (batRuns == 4) {
            effectRes = R.drawable.four;
            effectMs = 1800;
        }

        if (effectRes != 0) playGifEffect(effectRes, effectMs, nextFlow);
        else nextFlow.run();
    }









    private void pushBallEvent(int eventOverNo,
                               int eventBallInOver,
                               String label,
                               int teamRuns,
                               boolean legalBall,
                               boolean wide,
                               boolean noBall,
                               boolean byes,
                               boolean wicket,
                               String wicketType,
                               String fielder,
                               String strikerAtBall,
                               String nonStrikerAtBall,
                               String bowlerAtBall,
                               String outBatsman) {

        JSONObject e = new JSONObject();
        try {
            e.put("innings", innings);
            e.put("overNo", eventOverNo);
            e.put("ballInOver", eventBallInOver);

            e.put("label", label);
            e.put("teamRuns", teamRuns);

            // ✅ who got out (important for run out / correct scorecard)
            e.put("outBatsman", wicket ? safe(outBatsman) : "");

            e.put("legalBall", legalBall);
            e.put("wide", wide);
            e.put("noBall", noBall);
            e.put("byes", byes);

            e.put("wicket", wicket);

            String wt = wicket ? safe(wicketType) : "";
            e.put("wicketType", wt);

            // ✅ wicketBy ONLY when bowler gets credit
            boolean credited = wicket && isBowlerCreditedWicketType(wt);
            e.put("wicketBy", credited ? safe(bowlerAtBall) : "");

            e.put("fielder", wicket ? safe(fielder) : "");

            // ✅ store who faced THIS ball (before swaps)
            e.put("striker", safe(strikerAtBall));
            e.put("nonStriker", safe(nonStrikerAtBall));
            e.put("bowler", safe(bowlerAtBall));

            ballEvents.put(e);
        } catch (Exception ignored) {}
    }




    // ========================= DIALOGS =========================

    private void promptNewBatsman(boolean replaceStriker, @Nullable Runnable afterPick) {
        if (dialogOpen) return;
        dialogOpen = true;

        if (isAllOut()) {
            dialogOpen = false;
            finishInningsNow();
            return;
        }

        ArrayList<String> batting = cleanPlayers(getBattingList());

        // ✅ who is OUT from ballEvents (Retired Hurt excluded in your helper)
        HashSet<String> outBatters = getOutBattersForCurrentInnings();

        ArrayList<String> candidates = new ArrayList<>();
        for (String p : batting) {
            String name = safe(p);
            if (name.isEmpty()) continue;

            // cannot pick current batters
            if (name.equalsIgnoreCase(striker) || name.equalsIgnoreCase(nonStriker)) continue;

            // cannot pick already OUT
            if (containsIgnoreCase(outBatters, name)) continue;

            // ✅ enforce max 2 entries (like your retire flow)
            int entries = batterEntries.optInt(name, 0);
            if (entries >= 2) continue;

            candidates.add(name);
        }

        if (candidates.isEmpty()) {
            dialogOpen = false;
            new MaterialAlertDialogBuilder(this)
                    .setTitle("No batsman available")
                    .setMessage("No eligible batsman left (out or entry limit reached).")
                    .setCancelable(false)
                    .setPositiveButton("OK", (d, w) -> finishInningsNow())
                    .show();
            return;
        }

        String[] items = candidates.toArray(new String[0]);
        final int[] selected = {0};

        new MaterialAlertDialogBuilder(this)
                .setTitle("New batsman")
                .setSingleChoiceItems(items, 0, (d, which) -> selected[0] = which)
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    String chosen = items[selected[0]];

                    // assign chosen batsman
                    if (replaceStriker) striker = chosen;
                    else nonStriker = chosen;

                    // ✅ ensure + entry count for chosen
                    ensureBatsman(chosen);
                    markBatsmanEntered(chosen);

                    // keep current pair safe
                    ensureBatsman(striker);
                    ensureBatsman(nonStriker);

                    // ✅ NOW commit post-state (correct time)
                    commitPostStateToLastEvent();

                    dialogOpen = false;

                    renderAll();
                    snapshotCurrentInnings();
                    persistMatchState(false);

                    if (afterPick != null) afterPick.run();
                })
                .setNegativeButton("Cancel", (d, w) -> dialogOpen = false)
                .setOnDismissListener(d -> dialogOpen = false)
                .show();
    }



    private String oversFromBalls(int balls) {
        int ov = balls / 6;
        int bl = balls % 6;
        return ov + "." + bl;
    }

    private int getBowlerBallsFromStats(String bowlerName) {
        if (bowlerName == null) return 0;

        // bowlerStats in your app already counts legal balls only ✅
        String key = findExistingKeyCaseInsensitive(bowlerStats, bowlerName);
        if (key == null) return 0;

        JSONObject o = bowlerStats.optJSONObject(key);
        return (o != null) ? o.optInt("balls", 0) : 0;
    }






    private void promptNewBowler(boolean betweenOvers) {
        if (dialogOpen) return;
        dialogOpen = true;

        ArrayList<String> bowling = cleanPlayers(getBowlingList());
        if (bowling.isEmpty()) {
            dialogOpen = false;
            new MaterialAlertDialogBuilder(this)
                    .setTitle("No bowlers found")
                    .setMessage("Bowling list empty. Check player selection.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // ✅ exclusions: current over / previous over (depending on betweenOvers)
        HashSet<String> exclude = new HashSet<>();

        if (betweenOvers) {
            int lastCompletedOver = overNumber - 1;
            if (lastCompletedOver >= 0) {
                exclude.addAll(getBowlersUsedInOver(innings, lastCompletedOver));
            }
        } else {
            exclude.addAll(getBowlersUsedInOver(innings, overNumber));
            exclude.add(bowler); // manual mid-over change: don't show current
        }

        ArrayList<String> candidates = new ArrayList<>();
        for (String p : bowling) {
            if (containsIgnoreCase(exclude, p)) continue;
            candidates.add(p);
        }

        // fallback: if all excluded, allow anyone except current
        if (candidates.isEmpty()) {
            for (String p : bowling) {
                if (safe(p).equalsIgnoreCase(bowler)) continue;
                candidates.add(p);
            }
        }

        // final fallback: allow all
        if (candidates.isEmpty()) candidates.addAll(bowling);

        // ✅ Build display list: "Name (Overs: X.Y)"
        DecimalFormat df = new DecimalFormat("0.#");
        ArrayList<String> display = new ArrayList<>();

        for (String name : candidates) {
            int balls = getBowlerBallsFromStats(name);
            String oversStr = oversFromBalls(balls);
            display.add(name + " ( " + df.format(Float.parseFloat(oversStr)) + " Over)\n");
        }

        final int[] selected = {0};

        android.widget.ArrayAdapter<String> adapter =
                new android.widget.ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_single_choice,
                        display
                );

        new MaterialAlertDialogBuilder(this)
                .setTitle("New bowler")
                .setSingleChoiceItems(adapter, 0, (d, which) -> selected[0] = which)
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    bowler = candidates.get(selected[0]);
                    ensureBowler(bowler);

                    // ✅ NOW commit post-state (correct time)
                    commitPostStateToLastEvent();

                    dialogOpen = false;

                    renderAll();
                    snapshotCurrentInnings();
                    persistMatchState(false);
                })
                .setNegativeButton("Cancel", (d, w) -> dialogOpen = false)
                .setOnDismissListener(d -> dialogOpen = false)
                .show();
    }


    private void recomputeStateFromBallEvents() {

        // reset totals
        totalRuns = 0;
        wickets = 0;
        overNumber = 0;
        ballInOver = 0;

        pendingFreeHit = false;

        clearJson(batsmanStats);
        clearJson(bowlerStats);

        // find last innings present
        int lastInnings = 1;
        for (int i = 0; i < ballEvents.length(); i++) {
            JSONObject e = ballEvents.optJSONObject(i);
            if (e == null) continue;
            lastInnings = Math.max(lastInnings, e.optInt("innings", 1));
        }
        innings = lastInnings;

        // ✅ teamABatting should match the innings logic
        // (your 2nd innings flips)
        // so keep a "teamABattingStart" saved once at match creation if possible.
        // fallback: if innings==2, invert current saved field.
        if (innings == 1) {
            // keep as is (already loaded from match json)
        } else {
            // force opposite of start
            // if you add teamABattingStart, use that instead
            teamABatting = !teamABatting;
        }

        // replay events for *both* innings to build totals/stats correctly:
        for (int i = 0; i < ballEvents.length(); i++) {
            JSONObject e = ballEvents.optJSONObject(i);
            if (e == null) continue;

            int inn = e.optInt("innings", 1);

            // update innings when crossing boundary
            innings = inn;

            String strikerAtBall = e.optString("striker", "");
            String bowlerAtBall = e.optString("bowler", "");

            boolean wide = e.optBoolean("wide", false);
            boolean noBall = e.optBoolean("noBall", false);
            boolean byes = e.optBoolean("byes", false);
            boolean legalBall = e.optBoolean("legalBall", !(wide || noBall));

            boolean wicket = e.optBoolean("wicket", false);
            String wicketType = e.optString("wicketType", "");

            int teamRuns = e.optInt("teamRuns", 0);
            totalRuns += teamRuns;

            // runs from label (for batsman runs)
            int runs = extractRunsFromLabel(e.optString("label", ""));

            int batRuns, bowlRuns;
            if (wide) {
                batRuns = 0;
                bowlRuns = teamRuns;
            } else if (noBall) {
                batRuns = runs;
                bowlRuns = teamRuns;
            } else if (byes) {
                batRuns = 0;
                bowlRuns = 0;
            } else {
                batRuns = runs;
                bowlRuns = runs;
            }

            ensureBatsman(strikerAtBall);
            JSONObject bs = batsmanStats.optJSONObject(strikerAtBall);
            if (bs != null) {
                if (batRuns > 0) {
                    safePutInt(bs, "r", bs.optInt("r") + batRuns);
                    if (batRuns == 4) safePutInt(bs, "4s", bs.optInt("4s") + 1);
                    if (batRuns == 6) safePutInt(bs, "6s", bs.optInt("6s") + 1);
                }
                if (legalBall) safePutInt(bs, "b", bs.optInt("b") + 1);
            }

            ensureBowler(bowlerAtBall);
            JSONObject bw = bowlerStats.optJSONObject(bowlerAtBall);
            if (bw != null) {
                safePutInt(bw, "r", bw.optInt("r") + bowlRuns);
                if (legalBall) safePutInt(bw, "balls", bw.optInt("balls") + 1);

                boolean credited = wicket && isBowlerCreditedWicketType(wicketType);
                if (credited) safePutInt(bw, "w", bw.optInt("w") + 1);
            }

            boolean countsAsTeamWicket = wicket && !safe(wicketType).equalsIgnoreCase("Retired Hurt");
            if (countsAsTeamWicket) wickets++;

            // progress overs (your model)
            if (legalBall) {
                ballInOver++;
                if (ballInOver >= 6) {
                    overNumber++;
                    ballInOver = 0;
                }
            }

            // free hit
            if (legalBall) pendingFreeHit = false;
            if (noBall) pendingFreeHit = true;

            // ✅ APPLY FINAL STATE FROM EVENT (post fields)
            striker = e.optString("postStriker", strikerAtBall);
            nonStriker = e.optString("postNonStriker", e.optString("nonStriker", ""));
            bowler = e.optString("postBowler", bowlerAtBall);

            overNumber = e.optInt("postOverNumber", overNumber);
            ballInOver = e.optInt("postBallInOver", ballInOver);

            totalRuns = e.optInt("postTotalRuns", totalRuns);
            wickets = e.optInt("postWickets", wickets);

            pendingFreeHit = e.optBoolean("postPendingFreeHit", pendingFreeHit);

            innings = e.optInt("postInnings", innings);
            teamABatting = e.optBoolean("postTeamABatting", teamABatting);
        }

        recomputeHatTrickState();
        recomputeBatterEntriesFromBallEvents();
    }

    private int extractRunsFromLabel(String label) {
        if (label == null) return 0;
        label = label.trim();

        if (label.equalsIgnoreCase("W")) return 0;
        if (label.equalsIgnoreCase("Wd") || label.equalsIgnoreCase("Nb") || label.equalsIgnoreCase("B")) return 0;

        String digits = label.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;

        try { return Integer.parseInt(digits); }
        catch (Exception e) { return 0; }
    }








    private void showFirstInningsOverDialog() {
        snapshotCurrentInnings();
        firstInningsRuns = totalRuns;
        target = firstInningsRuns + 1;

        dialogOpen = true;

        new MaterialAlertDialogBuilder(this)
                .setTitle("1st Innings Over")
                .setMessage(getBattingTeamName() + " scored " + firstInningsRuns + "\nTarget: " + target)
                .setCancelable(false)
                .setNegativeButton("Undo last ball", (d, w) -> {
                    dialogOpen = false;
                    undo();
                })
                .setPositiveButton("Start 2nd Innings", (d, w) -> {
                    dialogOpen = false;
                    startSecondInnings();
                })
                .show();
    }

    private void startSecondInnings() {
        // ✅ IMPORTANT: push a state BEFORE switching innings (so undo can go back)

        innings = 2;
        teamABatting = !teamABatting;
        tb.setSubtitle(getBattingTeamName() + " chasing");

        totalRuns = 0;
        wickets = 0;
        overNumber = 0;
        ballInOver = 0;
        lastOverBowler = null;

        clearJson(batsmanStats);
        clearJson(bowlerStats);

        showOpeningSelectionDialogs();
    }

    private void showOpeningSelectionDialogs() {
        if (dialogOpen) return;
        dialogOpen = true;

        final ArrayList<String> bat = cleanPlayers(getBattingList());
        final ArrayList<String> bowl = cleanPlayers(getBowlingList());

        if (bat.size() < 1 || bowl.size() < 1) {
            dialogOpen = false;
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Players missing")
                    .setMessage("Batting or bowling list is empty.")
                    .setCancelable(false)
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        final int[] sIdx = {0};
        String[] batItems = bat.toArray(new String[0]);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Striker (2nd Innings)")
                .setSingleChoiceItems(batItems, 0, (d, which) -> sIdx[0] = which)
                .setCancelable(false)
                .setPositiveButton("Next", (d, w) -> {
                    striker = bat.get(sIdx[0]);

                    ArrayList<String> nsCand = new ArrayList<>();
                    for (String p : bat) if (!p.equalsIgnoreCase(striker)) nsCand.add(p);
                    if (nsCand.isEmpty()) nsCand.add(striker);

                    final int[] nIdx = {0};
                    String[] nsItems = nsCand.toArray(new String[0]);

                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Select Non-striker")
                            .setSingleChoiceItems(nsItems, 0, (d2, which) -> nIdx[0] = which)
                            .setCancelable(false)
                            .setPositiveButton("Next", (d2, w2) -> {
                                nonStriker = nsCand.get(nIdx[0]);

                                ArrayList<String> tempBowl = new ArrayList<>();
                                for (String p : bowl) {
                                    if (!p.equalsIgnoreCase(striker) && !p.equalsIgnoreCase(nonStriker)) {
                                        tempBowl.add(p);
                                    }
                                }
                                if (tempBowl.isEmpty()) tempBowl.addAll(bowl);

// ✅ FINAL list for lambda
                                final ArrayList<String> bCand = tempBowl;


                                final int[] bIdx = {0};
                                String[] bowlItems = bCand.toArray(new String[0]);

                                new MaterialAlertDialogBuilder(this)
                                        .setTitle("Select Opening Bowler")
                                        .setSingleChoiceItems(bowlItems, 0, (d3, which) -> bIdx[0] = which)
                                        .setCancelable(false)
                                        .setPositiveButton("Start", (d3, w3) -> {
                                            bowler = bCand.get(bIdx[0]); // ✅ NOW VALID
                                            ensureBowler(bowler);

                                            dialogOpen = false;
                                            renderAll();
                                            snapshotCurrentInnings();
                                            persistMatchState(false);
                                        })
                                        .show();

                            })
                            .show();
                })
                .show();
    }

    private void showMatchResultDialogNow(String result) {

        dialogOpen = true;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Match Result")
                .setMessage(result)
                .setCancelable(false)
                .setNegativeButton("Undo last ball", (d, w) -> {
                    dialogOpen = false;
                    undo();
                })
                .setPositiveButton("View Scorecard", (d, w) -> {
                    dialogOpen = false;

                    // ✅ mark completed + save
                    isCompleted = true;
                    persistMatchState(true);

                    // ✅ IMPORTANT: do NOT send big JSON in Intent
                    Intent i = new Intent(this, ScorecardActivity.class);
                    i.putExtra(EXTRA_MATCH_ID, matchId);              // ✅ ONLY matchId
                    i.putExtra(EXTRA_GO_HOME_ON_BACK, true);
                    startActivity(i);

                    finish(); // scoring removed permanently
                })
                .setOnDismissListener(d -> dialogOpen = false)
                .show();
    }



    private void showMatchResultDialog() {
        snapshotCurrentInnings();

        String batTeam = getBattingTeamName();
        String bowlTeam = getBowlingTeamName();

        String result;
        String winnerTeamName = "";

        if (innings == 2) {
            if (totalRuns >= target) {
                int wkLeft = Math.max(0, (playersPerTeam - 1) - wickets);
                result = batTeam + " won by " + wkLeft + " wickets";
                winnerTeamName = batTeam;
            } else {
                int defendMargin = (target - 1) - totalRuns;
                result = (defendMargin == 0)
                        ? "Match Tied"
                        : (bowlTeam + " won by " + defendMargin + " runs");

                if (defendMargin != 0) winnerTeamName = bowlTeam;
            }
        } else {
            result = "Innings finished.";
        }

        // ✅ Play celebration first, then show dialog
        if (!winnerTeamName.isEmpty()) {
            showWinnerFireworks(winnerTeamName);

            konfettiView.postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    showMatchResultDialogNow(result);
                }
            }, CELEBRATION_MS);

        } else {
            // tie or no winner => show dialog immediately
            showMatchResultDialogNow(result);
        }
    }



    private void finishInningsNow() {
        dialogOpen = false;
        snapshotCurrentInnings();

        if (innings == 1) showFirstInningsOverDialog();
        else showMatchResultDialog();
    }

    // ========================= RENDER =========================

    private void renderAll() {
        tb.setSubtitle(getBattingTeamName() + (innings == 2 ? " chasing" : " batting"));

        String batTeam = getBattingTeamName();
        tvInnings.setText((innings == 1 ? "1st Innings - " : "2nd Innings - ") + batTeam);

        String overStr = String.format(Locale.US, "%d.%d", overNumber, ballInOver);
        tvScoreLine.setText(totalRuns + " - " + wickets + " (" + overStr + ")");
        tvOverProgressLarge.setText(String.format(Locale.US, "%s/%d", overStr, oversLimit));

        float oversFloat = overNumber + (ballInOver / 6f);
        float crr = oversFloat > 0 ? (totalRuns / oversFloat) : 0f;
        tvCRR.setText(String.format(Locale.US, "CRR %.2f", crr));

        if (innings == 2) {
            tvChaseLine.setVisibility(View.VISIBLE);

            int ballsLeft = getBallsLeft();
            int runsNeed = Math.max(0, target - totalRuns);
            float reqOvers = ballsLeft / 6f;
            float rrr = reqOvers > 0 ? (runsNeed / reqOvers) : 0f;

            SpannableStringBuilder sb = new SpannableStringBuilder();

            int start, end;

            start = sb.length();
            sb.append("Target: ").append(String.valueOf(target));
            end = sb.length();
            sb.setSpan(new ForegroundColorSpan(0xFFFFD54F), start, end, 0);

            sb.append(" | ");

            start = sb.length();
            sb.append("Need: ").append(String.valueOf(runsNeed));
            end = sb.length();
            sb.setSpan(new ForegroundColorSpan(0xFFEAF2FF), start, end, 0);

            sb.append(" ");

            start = sb.length();
            sb.append("Balls: ").append(String.valueOf(ballsLeft));
            end = sb.length();
            sb.setSpan(new ForegroundColorSpan(0xFFEAF2FF), start, end, 0);

            sb.append(" | ");

            start = sb.length();
            sb.append("RRR: ").append(String.format(Locale.US, "%.2f", rrr));
            end = sb.length();
            sb.setSpan(new ForegroundColorSpan(0xFFFFD54F), start, end, 0);

            tvChaseLine.setText(sb);
            tvRRR.setVisibility(View.GONE);
        } else {
            tvChaseLine.setVisibility(View.GONE);
            tvRRR.setVisibility(View.GONE);
        }

        renderBatsmen();
        renderBowler();

        // ✅ ALWAYS rebuild this over correctly
        updateThisOverFromBallEvents();
    }

    private int getBallsLeft() {
        int totalBalls = oversLimit * 6;
        int used = overNumber * 6 + ballInOver;
        return Math.max(0, totalBalls - used);
    }
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void renderBatsmen() {
        ensureBatsman(striker);
        ensureBatsman(nonStriker);

        JSONObject a = batsmanStats.optJSONObject(striker);
        JSONObject b = batsmanStats.optJSONObject(nonStriker);

        tvBatAName.setText(striker);
        tvBatAName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bat, 0, 0, 0);
        tvBatAName.setCompoundDrawablePadding(dp(4));
        android.graphics.drawable.Drawable d = tvBatAName.getCompoundDrawables()[0]; if (d != null)
        { int s = dp(10);
            d.setBounds(0, 0, s, s);
            tvBatAName.setCompoundDrawables(d, null, null, null); }
        tvBatBName.setText("  " + nonStriker);

        int ar = a != null ? a.optInt("r") : 0;
        int ab = a != null ? a.optInt("b") : 0;
        int a4 = a != null ? a.optInt("4s") : 0;
        int a6 = a != null ? a.optInt("6s") : 0;
        float asr = ab > 0 ? (ar * 100f / ab) : 0f;

        tvBatAR.setText(String.valueOf(ar));
        tvBatAB.setText(String.valueOf(ab));
        tvBatA4.setText(String.valueOf(a4));
        tvBatA6.setText(String.valueOf(a6));
        tvBatASR.setText(String.format(Locale.US, "%.2f", asr));

        int br = b != null ? b.optInt("r") : 0;
        int bb = b != null ? b.optInt("b") : 0;
        int b4 = b != null ? b.optInt("4s") : 0;
        int b6 = b != null ? b.optInt("6s") : 0;
        float bsr = bb > 0 ? (br * 100f / bb) : 0f;

        tvBatBR.setText(String.valueOf(br));
        tvBatBB.setText(String.valueOf(bb));
        tvBatB4.setText(String.valueOf(b4));
        tvBatB6.setText(String.valueOf(b6));
        tvBatBSR.setText(String.format(Locale.US, "%.2f", bsr));
    }

    private void renderBowler() {
        ensureBowler(bowler);
        JSONObject o = bowlerStats.optJSONObject(bowler);

        int balls = o != null ? o.optInt("balls") : 0;
        int r = o != null ? o.optInt("r") : 0;
        int w = o != null ? o.optInt("w") : 0;

        int ov = balls / 6;
        int bl = balls % 6;
        float overs = balls > 0 ? (balls / 6f) : 0f;
        float econ = overs > 0f ? (r / overs) : 0f;

        tvBowlerName.setText(bowler);
        tvBowlerName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ball, 0, 0, 0);
        tvBowlerName.setCompoundDrawablePadding(dp(4));
        android.graphics.drawable.Drawable d = tvBowlerName.getCompoundDrawables()[0]; if (d != null)
        { int s = dp(8);
            d.setBounds(0, 0, s, s);
            tvBowlerName.setCompoundDrawables(d, null, null, null);
        }
        tvBowlerO.setText(String.format(Locale.US, "%d.%d", ov, bl));
        tvBowlerR.setText(String.valueOf(r));
        tvBowlerW.setText(String.valueOf(w));
        tvBowlerEcon.setText(String.format(Locale.US, "%.2f", econ));
    }

    // ✅ "This Over" is ALWAYS based on current innings + current overNumber
    private void updateThisOverFromBallEvents() {
        thisOverBalls.clear();

        int shownOverNo = overNumber; // current running over
        int overTotal = 0;

        for (int i = 0; i < ballEvents.length(); i++) {
            JSONObject e = ballEvents.optJSONObject(i);
            if (e == null) continue;

            if (e.optInt("innings", 1) != innings) continue;
            if (e.optInt("overNo", -1) != shownOverNo) continue;

            String label = e.optString("label", "");
            boolean wicket = e.optBoolean("wicket", false);
            int teamRuns = e.optInt("teamRuns", 0);

            overTotal += teamRuns;
            thisOverBalls.add(new BallItem(label, wicket));
        }

        tvThisOverTotal.setText(String.format(Locale.US, "Over %d Total: %d", (shownOverNo + 1), overTotal));
        thisOverAdapter.notifyDataSetChanged();

        rvThisOver.post(() -> {
            if (!thisOverBalls.isEmpty()) rvThisOver.scrollToPosition(thisOverBalls.size() - 1);
        });
    }

    // ========================= PERSIST =========================

    private void persistMatchState(boolean completedNow) {
        if (completedNow) isCompleted = true;

        JSONObject matchJson = exportMatchJson(true);

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (isCompleted) sp.edit().remove(KEY_INPROGRESS).apply();
        else sp.edit().putString(KEY_INPROGRESS, matchJson.toString()).apply();

        String title = (teamA.isEmpty() ? "Team A" : teamA) + " vs " + (teamB.isEmpty() ? "Team B" : teamB);
        String status = isCompleted ? MatchStore.STATUS_COMPLETED : MatchStore.STATUS_IN_PROGRESS;

        MatchStore.upsert(this, matchId, title, matchDateUtc, status, matchJson.toString());
    }

    // ========================= UNDO =========================
    private void stopGifEffect() {
        if (ivEffectGif != null) {
            ivEffectGif.clearAnimation();
            ivEffectGif.setImageDrawable(null);
            ivEffectGif.setVisibility(View.GONE);
        }

        if (viewDim != null) {
            viewDim.setVisibility(View.GONE);
        }
    }

    private void undo() {
        if (ballEvents.length() == 0) return;

        stopGifEffect();

        // remove last delivery
        ballEvents.remove(ballEvents.length() - 1);

        // rebuild match state
        recomputeStateFromBallEvents();

        // ✅ rebuild entry counts too (VERY IMPORTANT)
        recomputeBatterEntriesFromBallEvents();

        recomputeBowlerOverSetsFromBallEvents();

        renderAll();
        snapshotCurrentInnings();
        persistMatchState(false);
    }

    private void commitPostStateToLastEvent() {
        if (ballEvents.length() == 0) return;

        JSONObject e = ballEvents.optJSONObject(ballEvents.length() - 1);
        if (e == null) return;

        try {
            e.put("postStriker", safe(striker));
            e.put("postNonStriker", safe(nonStriker));
            e.put("postBowler", safe(bowler));

            e.put("postOverNumber", overNumber);
            e.put("postBallInOver", ballInOver);

            e.put("postTotalRuns", totalRuns);
            e.put("postWickets", wickets);

            e.put("postPendingFreeHit", pendingFreeHit);

            e.put("postInnings", innings);
            e.put("postTeamABatting", teamABatting);

        } catch (Exception ignored) {}
    }


    private void recomputeBowlerOverSetsFromBallEvents() {
        currentOverBowlers.clear();
        lastOverBowlers.clear();

        // current over bowlers = bowlers in (innings, overNumber)
        // last over bowlers = bowlers in (innings, overNumber-1)

        int curOver = overNumber;
        int prevOver = overNumber - 1;

        for (int i = 0; i < ballEvents.length(); i++) {
            JSONObject e = ballEvents.optJSONObject(i);
            if (e == null) continue;

            if (e.optInt("innings", 1) != innings) continue;

            int ov = e.optInt("overNo", -99);
            String b = safe(e.optString("bowler"));

            if (b.isEmpty()) continue;

            if (ov == curOver) currentOverBowlers.add(b);
            if (ov == prevOver) lastOverBowlers.add(b);
        }
    }



    private void swapStrike() {
        // ✅ undo snapshot without undoStack
        swapStrikeInternal();

        renderAll();
        snapshotCurrentInnings();
        persistMatchState(false);
    }

    private void swapStrikeInternal() {
        String t = striker;
        striker = nonStriker;
        nonStriker = t;
    }

    // ========================= SAVE/LOAD JSON =========================

    // ✅ includeUndoStack=true ONLY for main saved file
    // ✅ undo snapshots MUST use includeUndoStack=false
    private JSONObject exportMatchJson(boolean includeUndoStack) {
        JSONObject j = new JSONObject();
        try {
            j.put("matchId", matchId);
            j.put("matchDateUtc", matchDateUtc);
            j.put("isCompleted", isCompleted);
            j.put("pendingFreeHit", pendingFreeHit);


            j.put("teamA", teamA);
            j.put("teamB", teamB);
            j.put("oversLimit", oversLimit);
            j.put("playersPerTeam", playersPerTeam);

            j.put("innings", innings);
            j.put("firstInningsRuns", firstInningsRuns);
            j.put("target", target);

            j.put("teamABatting", teamABatting);
            j.put("striker", striker);
            j.put("nonStriker", nonStriker);
            j.put("bowler", bowler);

            j.put("tossWinner", tossWinner);
            j.put("tossDecision", tossDecision);

            j.put("totalRuns", totalRuns);
            j.put("wickets", wickets);
            j.put("overNumber", overNumber);
            j.put("ballInOver", ballInOver);
            j.put("lastOverBowler", lastOverBowler);

            j.put("batsmanStats", batsmanStats);
            j.put("bowlerStats", bowlerStats);

            j.put("teamAPlayers", new JSONArray(teamAPlayers));
            j.put("teamBPlayers", new JSONArray(teamBPlayers));

            j.put("ballEvents", ballEvents);

            j.put("innings1Snapshot", innings1Snapshot);
            j.put("innings2Snapshot", innings2Snapshot);

            if (includeUndoStack) {
                j.put("undoStack", exportUndoStack());
            }

        } catch (JSONException ignored) {}
        return j;
    }

    // ✅ undoStack states are already snapshots without undoStack
    private JSONArray exportUndoStack() {
        JSONArray arr = new JSONArray();
        for (JSONObject state : undoStack) {
            arr.put(safeDeepCopy(state));
        }
        return arr;
    }

    private void importMatchJson(@NonNull JSONObject j) {
        matchId = j.optString("matchId", matchId);
        matchDateUtc = j.optLong("matchDateUtc", matchDateUtc);
        isCompleted = j.optBoolean("isCompleted", false);

        if (matchId == null || matchId.trim().isEmpty()) {
            matchDateUtc = (matchDateUtc > 0 ? matchDateUtc : System.currentTimeMillis());
            matchId = "m_" + matchDateUtc;
        }
        if (matchDateUtc <= 0) matchDateUtc = System.currentTimeMillis();

        pendingFreeHit = j.optBoolean("pendingFreeHit", false);


        teamA = j.optString("teamA", teamA);
        teamB = j.optString("teamB", teamB);
        oversLimit = j.optInt("oversLimit", oversLimit);
        playersPerTeam = j.optInt("playersPerTeam", playersPerTeam);

        innings = j.optInt("innings", innings);
        firstInningsRuns = j.optInt("firstInningsRuns", firstInningsRuns);
        target = j.optInt("target", target);

        teamABatting = j.optBoolean("teamABatting", teamABatting);
        striker = j.optString("striker", striker);
        nonStriker = j.optString("nonStriker", nonStriker);
        bowler = j.optString("bowler", bowler);

        tossWinner = j.optString("tossWinner", tossWinner);
        tossDecision = j.optString("tossDecision", tossDecision);

        totalRuns = j.optInt("totalRuns", totalRuns);
        wickets = j.optInt("wickets", wickets);
        overNumber = j.optInt("overNumber", overNumber);
        ballInOver = j.optInt("ballInOver", ballInOver);
        lastOverBowler = j.optString("lastOverBowler", lastOverBowler);

        teamAPlayers = new ArrayList<>();
        teamBPlayers = new ArrayList<>();

        JSONArray ap = j.optJSONArray("teamAPlayers");
        JSONArray bp = j.optJSONArray("teamBPlayers");
        if (ap != null) for (int i = 0; i < ap.length(); i++) teamAPlayers.add(ap.optString(i));
        if (bp != null) for (int i = 0; i < bp.length(); i++) teamBPlayers.add(bp.optString(i));

        innings1Snapshot = j.optJSONObject("innings1Snapshot") != null ? j.optJSONObject("innings1Snapshot") : new JSONObject();
        innings2Snapshot = j.optJSONObject("innings2Snapshot") != null ? j.optJSONObject("innings2Snapshot") : new JSONObject();

        overwriteJson(j.optJSONObject("batsmanStats"), batsmanStats);
        overwriteJson(j.optJSONObject("bowlerStats"), bowlerStats);

        clearBallEvents();
        JSONArray be = j.optJSONArray("ballEvents");
        if (be != null) {
            for (int i = 0; i < be.length(); i++) {
                JSONObject o = be.optJSONObject(i);
                if (o != null) ballEvents.put(o);
            }
        }
        recomputeHatTrickState();

        // ✅ restore undoStack
        undoStack.clear();
        JSONArray us = j.optJSONArray("undoStack");
        if (us != null) {
            // stored top->bottom, so push reverse to keep same top
            for (int i = us.length() - 1; i >= 0; i--) {
                JSONObject state = us.optJSONObject(i);
                if (state != null) undoStack.push(safeDeepCopy(state));
            }
        }

        // ✅ if old saved match had no undoStack
        if (undoStack.isEmpty()) {

        }

        dialogOpen = false;
        resetExtras();
    }

    private void clearBallEvents() {
        while (ballEvents.length() > 0) {
            ballEvents.remove(ballEvents.length() - 1);
        }
    }

    private void overwriteJson(JSONObject from, JSONObject to) {
        if (from == null) return;

        JSONArray keys = to.names();
        if (keys != null) for (int i = 0; i < keys.length(); i++) to.remove(keys.optString(i));

        JSONArray names = from.names();
        if (names == null) return;

        for (int i = 0; i < names.length(); i++) {
            String k = names.optString(i);
            try { to.put(k, from.get(k)); } catch (JSONException ignored) {}
        }
    }

    // ========================= SNAPSHOT HELPERS =========================

    private static String formatOversFromBalls(int balls) {
        int ov = balls / 6;
        int bl = balls % 6;
        return ov + "." + bl;
    }

    private void snapshotCurrentInnings() {
        JSONObject snap = new JSONObject();
        try {
            snap.put("battingTeam", getBattingTeamName());
            snap.put("bowlingTeam", getBowlingTeamName());

            int ballsUsed = overNumber * 6 + ballInOver;
            snap.put("ballsUsed", ballsUsed);
            snap.put("oversStr", formatOversFromBalls(ballsUsed));

            snap.put("runs", totalRuns);
            snap.put("wkts", wickets);

            snap.put("batsmanStats", new JSONObject(batsmanStats.toString()));
            snap.put("bowlerStats", new JSONObject(bowlerStats.toString()));

        } catch (Exception ignored) {}

        if (innings == 1) innings1Snapshot = snap;
        else innings2Snapshot = snap;
    }

    private int getMaxWicketsForCurrentInnings() {
        int batCount = cleanPlayers(getBattingList()).size();
        return Math.max(0, batCount - 1);
    }

    private int getMaxWicketsForTeam(boolean teamA) {
        int c = cleanPlayers(teamA ? teamAPlayers : teamBPlayers).size();
        return Math.max(0, c - 1);
    }


    // ========================= UTILS =========================

    private void resetExtras() {
        cbWide.setChecked(false);
        cbNoBall.setChecked(false);
        cbByes.setChecked(false);
    }

    private void ensureBatsman(String name) {
        if (name == null || name.trim().isEmpty()) return;

        if (!batsmanStats.has(name)) {
            JSONObject o = new JSONObject();
            safePutInt(o, "r", 0);
            safePutInt(o, "b", 0);
            safePutInt(o, "4s", 0);
            safePutInt(o, "6s", 0);
            try { batsmanStats.put(name, o); } catch (JSONException ignored) {}
        }

        // entry counter init
        if (!batterEntries.has(name)) {
            try { batterEntries.put(name, 0); } catch (Exception ignored) {}
        }
    }


    private void ensureBowler(String name) {
        if (name == null || name.trim().isEmpty()) return;
        if (!bowlerStats.has(name)) {
            JSONObject o = new JSONObject();
            safePutInt(o, "balls", 0);
            safePutInt(o, "r", 0);
            safePutInt(o, "w", 0);
            try { bowlerStats.put(name, o); } catch (JSONException ignored) {}
        }
    }

    private void safePutInt(JSONObject o, String k, int v) {
        if (o == null) return;
        try { o.put(k, v); } catch (JSONException ignored) {}
    }

    private boolean isAllOut() {
        return wickets >= getMaxWicketsForCurrentInnings();
    }

    private boolean isOversFinished() { return overNumber >= oversLimit; }
    private boolean isInningsOver() { return isAllOut() || isOversFinished(); }

    private ArrayList<String> getBattingList() { return teamABatting ? teamAPlayers : teamBPlayers; }
    private ArrayList<String> getBowlingList() { return teamABatting ? teamBPlayers : teamAPlayers; }

    private HashSet<String> getUsedBatters() {
        HashSet<String> used = new HashSet<>();
        if (!striker.isEmpty()) used.add(striker);
        if (!nonStriker.isEmpty()) used.add(nonStriker);

        JSONArray names = batsmanStats.names();
        if (names != null) for (int i = 0; i < names.length(); i++) used.add(names.optString(i));
        return used;
    }

    private String buildEventLabel(int runs, boolean wide, boolean noBall, boolean byes, boolean wicket) {
        if (wicket) return "W";
        if (wide) return runs == 0 ? "Wd" : (runs + "Wd");
        if (noBall) return runs == 0 ? "Nb" : (runs + "Nb");
        if (byes) return runs == 0 ? "B" : (runs + "B");
        return String.valueOf(runs);
    }

    private void clearJson(JSONObject obj) {
        JSONArray keys = obj.names();
        if (keys == null) return;
        for (int i = 0; i < keys.length(); i++) obj.remove(keys.optString(i));
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private JSONObject safeDeepCopy(JSONObject src) {
        try { return new JSONObject(src.toString()); }
        catch (Exception e) { return src; }
    }

    private ArrayList<String> cleanPlayers(ArrayList<String> list) {
        ArrayList<String> out = new ArrayList<>();
        if (list == null) return out;
        for (String s : list) {
            if (s == null) continue;
            s = s.trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private String getBattingTeamName() { return teamABatting ? teamA : teamB; }
    private String getBowlingTeamName() { return teamABatting ? teamB : teamA; }
}