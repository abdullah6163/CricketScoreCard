package com.example.weekendlegends;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PlayerCareerActivity extends AppCompatActivity {

    private ImageView imgAvatar;
    private TextView tvPlayerName, tvQuickLine;

    private TextView tvMatchesVal, tvRunsVal, tvWktsVal, tvSrVal, tvEconVal, tvBestVal;

    private View cardBatting, cardBowling;
    private RecyclerView rvRecent;
    private TextView tvNoRecent;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_player_career);

        String player = getIntent().getStringExtra("player");
        if (player == null) player = "";

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());
        tb.setTitle("Player Career");

        imgAvatar = findViewById(R.id.imgAvatar);
        tvPlayerName = findViewById(R.id.tvPlayerName);
        tvQuickLine = findViewById(R.id.tvQuickLine);

        tvMatchesVal = findViewById(R.id.tvMatchesVal);
        tvRunsVal = findViewById(R.id.tvRunsVal);
        tvWktsVal = findViewById(R.id.tvWktsVal);
        tvSrVal = findViewById(R.id.tvSrVal);
        tvEconVal = findViewById(R.id.tvEconVal);
        tvBestVal = findViewById(R.id.tvBestVal);

        cardBatting = findViewById(R.id.cardBatting);
        cardBowling = findViewById(R.id.cardBowling);

        rvRecent = findViewById(R.id.rvRecent);
        tvNoRecent = findViewById(R.id.tvNoRecent);

        tvPlayerName.setText(player);

        // avatar from PlayerImageStore
        setAvatar(player);

        // compute career
        Career c = computeCareer(player);

        tvQuickLine.setText("Matches " + c.matches + "  •  Runs " + c.runs + "  •  Wickets " + c.wickets);

        tvMatchesVal.setText(String.valueOf(c.matches));
        tvRunsVal.setText(String.valueOf(c.runs));
        tvWktsVal.setText(String.valueOf(c.wickets));
        tvSrVal.setText(String.format(Locale.US, "%.1f", c.strikeRate()));
        tvEconVal.setText(String.format(Locale.US, "%.2f", c.economy()));
        tvBestVal.setText(c.bestText());

        // ✅ Batting card (now 5 rows including 50s/100s)
        bindCard(cardBatting,
                "Batting",
                new String[]{"Runs", "Balls", "4s / 6s", "50s / 100s", "Strike Rate"},
                new String[]{
                        String.valueOf(c.runs),
                        String.valueOf(c.balls),
                        c.fours + " / " + c.sixes,
                        c.fifties + " / " + c.hundreds,
                        String.format(Locale.US, "%.1f", c.strikeRate())
                }
        );

        // Bowling card (still 4 rows - you can also make it 5 if you want)
        bindCard(cardBowling,
                "Bowling",
                new String[]{
                        "Wickets",
                        "Runs Conceded",
                        "Balls",
                        "Economy",
                        "Overs"
                },
                new String[]{
                        String.valueOf(c.wickets),
                        String.valueOf(c.bowlRuns),
                        String.valueOf(c.bowlBalls),
                        String.format(Locale.US, "%.2f", c.economy()),
                        String.format(Locale.US, "%.1f", c.bowlBalls / 6f)
                }
        );


        // Recent
        rvRecent.setLayoutManager(new LinearLayoutManager(this));
        rvRecent.setAdapter(new RecentMatchAdapter(c.recent));

        boolean no = c.recent.isEmpty();
        tvNoRecent.setVisibility(no ? View.VISIBLE : View.GONE);
        rvRecent.setVisibility(no ? View.GONE : View.VISIBLE);
    }

    private void setAvatar(String player) {
        String b64 = PlayerImageStore.getImageBase64(this, player);
        Bitmap bmp = decode(b64);
        if (bmp != null) imgAvatar.setImageBitmap(bmp);
        else imgAvatar.setImageResource(R.drawable.ic_person);
    }

    private Bitmap decode(String b64) {
        try {
            if (b64 == null || b64.trim().isEmpty()) return null;
            byte[] data = Base64.decode(b64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception e) {
            return null;
        }
    }

    // =========================
    // Career Models
    // =========================
    static class Career {
        int matches = 0;

        int runs = 0;
        int balls = 0;
        int fours = 0;
        int sixes = 0;

        // ✅ NEW
        int fifties = 0;
        int hundreds = 0;
        int batInnings = 0;

        int wickets = 0;
        int bowlRuns = 0;
        int bowlBalls = 0;

        int bestRuns = 0;
        int bestWkts = 0;

        ArrayList<RecentRow> recent = new ArrayList<>();

        float strikeRate() { return balls > 0 ? (runs * 100f / balls) : 0f; }
        float economy() {
            float overs = bowlBalls / 6f;
            return overs > 0 ? (bowlRuns / overs) : 0f;
        }

        String bestText() {
            if (bestRuns == 0 && bestWkts == 0) return "—";
            if (bestRuns > 0 && bestWkts > 0) return bestRuns + " & " + bestWkts + "W";
            if (bestRuns > 0) return String.valueOf(bestRuns);
            return bestWkts + "W";
        }
    }

    public static class RecentRow {
        public String title;
        public String dateText;
        public String roleLine;

        public RecentRow(String t, String d, String r) {
            title = t; dateText = d; roleLine = r;
        }
    }

    // =========================
    // Career calculation
    // =========================
    private Career computeCareer(String playerName) {
        Career out = new Career();
        if (playerName == null) return out;
        String target = playerName.trim();

        ArrayList<JSONObject> rows = MatchStore.load(this);
        SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        for (int idx = rows.size() - 1; idx >= 0; idx--) {
            JSONObject row = rows.get(idx);

            String id = row.optString("id", "");
            String title = row.optString("title", "Match");
            long dateUtc = row.optLong("dateUtc", 0L);
            String dateText = dateUtc > 0 ? df.format(new Date(dateUtc)) : "";

            String json = MatchStore.getMatchJsonById(this, id);
            if (json == null || json.trim().isEmpty()) continue;

            boolean appeared = false;

            int matchRuns = 0;
            int matchBalls = 0;
            int matchWkts = 0;
            int matchBowlRuns = 0;

            try {
                JSONObject match = new JSONObject(json);
                JSONArray balls = match.optJSONArray("ballEvents");
                if (balls == null) continue;

                for (int i = 0; i < balls.length(); i++) {
                    JSONObject e = balls.optJSONObject(i);
                    if (e == null) continue;

                    String striker = safe(e.optString("striker", ""));
                    String nonStriker = safe(e.optString("nonStriker", ""));
                    String bowler = safe(e.optString("bowler", ""));
                    String outBatsman = safe(e.optString("outBatsman", ""));

                    boolean wide = e.optBoolean("wide", false);
                    boolean byes = e.optBoolean("byes", false);
                    boolean legalBall = e.optBoolean("legalBall", true);

                    int teamRuns = e.optInt("teamRuns", 0);
                    String label = e.optString("label", "");

                    boolean wicket = e.optBoolean("wicket", false);
                    String wicketType = e.optString("wicketType", "");

                    // appeared?
                    if (equalsName(striker, target) || equalsName(nonStriker, target)
                            || equalsName(bowler, target) || equalsName(outBatsman, target)) {
                        appeared = true;
                    }

                    // Batting
                    if (equalsName(striker, target)) {
                        if (!wide) {
                            out.balls++;
                            matchBalls++;
                        }

                        int batRuns = 0;
                        if (!wide && !byes) batRuns = parseRunsFromLabel(label);

                        out.runs += batRuns;
                        matchRuns += batRuns;

                        if (batRuns == 4) out.fours++;
                        if (batRuns == 6) out.sixes++;
                    }

                    // Bowling
                    if (equalsName(bowler, target)) {
                        if (!byes) {
                            out.bowlRuns += teamRuns;
                            matchBowlRuns += teamRuns;
                        }
                        if (legalBall) out.bowlBalls++;

                        if (wicket && countsToBowlerWicket(wicketType)) {
                            out.wickets++;
                            matchWkts++;
                        }
                    }
                }

            } catch (Exception ignored) {}

            if (appeared) {
                out.matches++;

                // ✅ 50s/100s counting only if he actually batted
                if (matchBalls > 0) {
                    out.batInnings++;

                    if (matchRuns >= 100) out.hundreds++;
                    else if (matchRuns >= 50) out.fifties++;
                }

                if (matchRuns > out.bestRuns) out.bestRuns = matchRuns;
                if (matchWkts > out.bestWkts) out.bestWkts = matchWkts;

                if (out.recent.size() < 5) {
                    String line = "Bat " + matchRuns + " (" + matchBalls + ")";
                    if (matchWkts > 0 || matchBowlRuns > 0) {
                        line += "  •  Bowl " + matchWkts + "W, " + matchBowlRuns + "R";
                    }
                    out.recent.add(new RecentRow(title, dateText, line));
                }
            }
        }

        return out;
    }

    private boolean countsToBowlerWicket(String wicketType) {
        if (wicketType == null) return true;
        String t = wicketType.toLowerCase(Locale.US);
        return !(t.contains("run out")
                || t.contains("retired")
                || t.contains("obstruct")
                || t.contains("timed out"));
    }

    private int parseRunsFromLabel(String label) {
        if (label == null) return 0;
        label = label.trim();

        if (label.equalsIgnoreCase("W")) return 0;
        if (label.equalsIgnoreCase("Wd")) return 0;
        if (label.equalsIgnoreCase("Nb")) return 0;
        if (label.equalsIgnoreCase("B")) return 0;

        int n = 0;
        boolean found = false;
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c >= '0' && c <= '9') {
                n = n * 10 + (c - '0');
                found = true;
            } else break;
        }
        return found ? n : 0;
    }

    private boolean equalsName(String a, String b) {
        return safe(a).equalsIgnoreCase(safe(b));
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    // =========================
    // Bind the reused batting/bowling include
    // =========================
    private void bindCard(View card, String title, String[] keys, String[] vals) {
        if (card == null) return;

        TextView tvTitle = card.findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText(title);

        ViewGroup wrapper = (ViewGroup) card;
        if (wrapper.getChildCount() == 0) return;

        View child0 = wrapper.getChildAt(0);
        if (!(child0 instanceof ViewGroup)) return;

        ViewGroup inner = (ViewGroup) child0;
        if (inner.getChildCount() < 2) return;

        View rows = inner.getChildAt(1);
        if (!(rows instanceof ViewGroup)) return;

        ViewGroup vg = (ViewGroup) rows;

        for (int i = 0; i < vg.getChildCount() && i < keys.length && i < vals.length; i++) {
            View row = vg.getChildAt(i);
            TextView k = row.findViewById(R.id.tvKey);
            TextView v = row.findViewById(R.id.tvVal);
            if (k != null) k.setText(keys[i]);
            if (v != null) v.setText(vals[i]);
        }
    }
}
