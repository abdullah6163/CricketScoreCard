package com.example.weekendlegends;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

public class ScoreboardFragment extends Fragment {

    private static final String ARG_JSON = "arg_json";

    public static ScoreboardFragment newInstance(String matchJson) {
        ScoreboardFragment f = new ScoreboardFragment();
        Bundle b = new Bundle();
        b.putString(ARG_JSON, matchJson);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_scoreboard, container, false);

        String json = getArguments() != null ? getArguments().getString(ARG_JSON, "{}") : "{}";
        JSONObject match;
        try { match = new JSONObject(json); }
        catch (Exception e) { match = new JSONObject(); }

        TextView tvToss = v.findViewById(R.id.tvTossInfo);

        String teamA = match.optString("teamA", "Team A");
        String teamB = match.optString("teamB", "Team B");

        String tossWinner = match.optString("tossWinner", "");
        String tossDecision = match.optString("tossDecision", "");

        if (!tossWinner.isEmpty() && !tossDecision.isEmpty()) {
            String winnerName = tossWinner.equalsIgnoreCase("A") ? teamA : teamB;

            String decisionText;
            if (tossDecision.equalsIgnoreCase("BAT")) decisionText = "bat";
            else if (tossDecision.equalsIgnoreCase("BOWL")) decisionText = "bowl";
            else decisionText = tossDecision.toLowerCase(Locale.US);

            tvToss.setVisibility(View.VISIBLE);
            tvToss.setText("  Toss: " + winnerName + " won and chose to " + decisionText);
        } else {
            tvToss.setVisibility(View.GONE);
        }

        TextView tvResult = v.findViewById(R.id.tvResultInfo);

        // ✅ MoM views
        View cardMom = v.findViewById(R.id.cardMom);
        TextView tvMomName = v.findViewById(R.id.tvMomName);
        TextView tvMomDetails = v.findViewById(R.id.tvMomDetails);

        boolean completed = match.optBoolean("isCompleted", false);
        if (completed) {
            String resultText = buildMatchResult(match, teamA, teamB);
            if (!resultText.isEmpty()) {
                tvResult.setVisibility(View.VISIBLE);
                tvResult.setText(resultText);
            } else tvResult.setVisibility(View.GONE);

            // ✅ compute + show MoM
            JSONArray ballEventsMom = match.optJSONArray("ballEvents");
            Mom mom = computeManOfTheMatch(match, ballEventsMom, teamA, teamB);

            if (mom != null && mom.name != null && !mom.name.trim().isEmpty()) {
                if (cardMom != null) cardMom.setVisibility(View.VISIBLE);

                if (tvMomName != null) {
                    String teamTag = (mom.team == null || mom.team.trim().isEmpty()) ? "" : (" (" + mom.team + ")");
                    tvMomName.setText(mom.name + teamTag);
                }

                if (tvMomDetails != null) {
                    String details = "";

                    if (mom.balls > 0 || mom.runs > 0) {
                        details += "Bat: " + mom.runs + " (" + mom.balls + ")";
                    }

                    if (mom.wickets > 0 || mom.bowlRuns > 0) {
                        if (!details.isEmpty()) details += "   |   ";
                        details += "Bowl: " + mom.wickets + "W, " + mom.bowlRuns + "R";
                    }

                    tvMomDetails.setText(details.isEmpty() ? "—" : details);
                }

            } else {
                if (cardMom != null) cardMom.setVisibility(View.GONE);
            }

        } else {
            tvResult.setVisibility(View.GONE);
            if (cardMom != null) cardMom.setVisibility(View.GONE);
        }

        int runningInnings = match.optInt("innings", 1);
        JSONArray ballEvents = match.optJSONArray("ballEvents");

        InningsData inns1 = buildInnings(match, ballEvents, 1, runningInnings);
        InningsData inns2 = buildInnings(match, ballEvents, 2, runningInnings);

        bindInningsCard(match, v, R.id.cardInnings1, "1st Innings", inns1, runningInnings);
        bindInningsCard(match, v, R.id.cardInnings2, "2nd Innings", inns2, runningInnings);

        return v;
    }

    @Nullable
    private String getWinnerTeamName(JSONObject match, String teamA, String teamB) {
        // Only reliable after match completed
        boolean completed = match.optBoolean("isCompleted", false);
        if (!completed) return null;

        int innings = match.optInt("innings", 1);
        int target = match.optInt("target", 0);
        int runs = match.optInt("totalRuns", 0);

        boolean teamABatting = match.optBoolean("teamABatting", true);
        String battingTeam = teamABatting ? teamA : teamB;
        String bowlingTeam = teamABatting ? teamB : teamA;

        // Winner is only meaningful after 2nd innings chase OR your completion logic
        if (innings == 2 && target > 0) {
            if (runs >= target) return battingTeam;            // chasing team won
            int margin = (target - 1) - runs;
            if (margin == 0) return null;                      // tie
            return bowlingTeam;                                // defending team won
        }

        return null; // if not decidable, don't pick MoM
    }


    // =========================
    // ✅ Man of the Match
    // =========================

    static class Mom {
        String name = "";
        String team = "";
        int runs = 0;
        int balls = 0;
        int wickets = 0;
        int bowlRuns = 0;
        long score = Long.MIN_VALUE;
    }

    private Mom computeManOfTheMatch(JSONObject match, JSONArray ballEvents, String teamA, String teamB) {

        ArrayList<String> teamAPlayers = jsonArrayToList(match.optJSONArray("teamAPlayers"));
        ArrayList<String> teamBPlayers = jsonArrayToList(match.optJSONArray("teamBPlayers"));

        // ✅ winner team filter
        String winnerTeam = getWinnerTeamName(match, teamA, teamB);
        if (winnerTeam == null || winnerTeam.trim().isEmpty()) {
            // Tie/no-result => no MoM (so card hides)
            return null;
        }

        Map<String, int[]> bat = new HashMap<>();   // name -> [runs, balls]
        Map<String, int[]> bowl = new HashMap<>();  // name -> [wkts, runsConceded]

        if (ballEvents != null) {
            for (int i = 0; i < ballEvents.length(); i++) {
                JSONObject e = ballEvents.optJSONObject(i);
                if (e == null) continue;

                String striker = e.optString("striker", "");
                String bowler = e.optString("bowler", "");

                boolean legalBall = e.optBoolean("legalBall", true);
                boolean wide = e.optBoolean("wide", false);
                boolean byes = e.optBoolean("byes", false);

                boolean wicket = e.optBoolean("wicket", false);
                String wicketType = e.optString("wicketType", "");

                String label = e.optString("label", "");
                int teamRuns = e.optInt("teamRuns", 0);

                // Batting (balls: every ball except wide)
                if (!striker.isEmpty()) {
                    int[] st = bat.get(striker);
                    if (st == null) st = new int[]{0, 0}; // [runs, balls]

                    // ✅ balls faced = ONLY legal balls
                    if (legalBall) st[1]++;

                    // ✅ runs from bat (even on no-ball)
                    int batRuns = 0;
                    if (!wide && !byes) batRuns = parseRunsFromLabel(label);
                    st[0] += batRuns;

                    bat.put(striker, st);
                }

                // Bowling (runs conceded: ignore byes)
                if (!bowler.isEmpty()) {
                    int[] st = bowl.get(bowler);
                    if (st == null) st = new int[]{0, 0}; // [wkts, runsConceded]

                    if (!byes) st[1] += teamRuns;

                    if (wicket && countsToBowlerWicket(wicketType)) st[0] += 1;

                    bowl.put(bowler, st);
                }
            }
        }

        HashSet<String> all = new HashSet<>();
        all.addAll(bat.keySet());
        all.addAll(bowl.keySet());

        Mom best = null;

        for (String name : all) {

            // ✅ determine team
            String team = resolveTeam(name, teamA, teamB, teamAPlayers, teamBPlayers);

            // ✅ only winner team players
            if (!team.equalsIgnoreCase(winnerTeam)) continue;

            int runs = bat.containsKey(name) ? bat.get(name)[0] : 0;
            int balls = bat.containsKey(name) ? bat.get(name)[1] : 0;

            int wkts = bowl.containsKey(name) ? bowl.get(name)[0] : 0;
            int bowlRuns = bowl.containsKey(name) ? bowl.get(name)[1] : 0;

            long sr = (balls > 0) ? (runs * 100L / balls) : 0L;

            long score = (runs * 1000L) + (wkts * 600L) + sr;

            Mom m = new Mom();
            m.name = name;
            m.team = team;
            m.runs = runs;
            m.balls = balls;
            m.wickets = wkts;
            m.bowlRuns = bowlRuns;
            m.score = score;

            if (best == null || m.score > best.score) best = m;
        }

        return best;
    }


    private String resolveTeam(String player, String teamA, String teamB,
                               ArrayList<String> a, ArrayList<String> b) {
        String p = player == null ? "" : player.trim().toLowerCase(Locale.US);
        for (String s : a) {
            if (s != null && s.trim().toLowerCase(Locale.US).equals(p)) return teamA;
        }
        for (String s : b) {
            if (s != null && s.trim().toLowerCase(Locale.US).equals(p)) return teamB;
        }
        return "";
    }

    private ArrayList<String> jsonArrayToList(JSONArray arr) {
        ArrayList<String> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            String s = arr.optString(i, "");
            if (s != null) {
                s = s.trim();
                if (!s.isEmpty()) out.add(s);
            }
        }
        return out;
    }

    // ---------------- UI bind per innings card ----------------

    private void bindInningsCard(JSONObject match,
                                 View root,
                                 int includeId,
                                 String title,
                                 InningsData data,
                                 int runningInnings) {

        View card = root.findViewById(includeId);
        if (card == null) return;

        TextView tvInningsTitle = card.findViewById(R.id.tvInningsTitle);
        TextView tvScore = card.findViewById(R.id.tvInningsScore);
        TextView tvOvers = card.findViewById(R.id.tvInningsOvers);
        TextView tvRate = card.findViewById(R.id.tvInningsCRR);
        TextView tvExtras = card.findViewById(R.id.tvExtras);

        TextView tvBattingLabel = card.findViewById(R.id.tvBattingLabel);
        TextView tvBowlingLabel = card.findViewById(R.id.tvBowlingLabel);

        TextView tvYetToBat = card.findViewById(R.id.tvYetToBat);

        TextView tvBatBowl = card.findViewById(R.id.tvBatBowl);
        if (tvBatBowl != null) tvBatBowl.setVisibility(View.GONE);

        ImageView ivDrop = card.findViewById(R.id.ivDrop);
        LinearLayout body = card.findViewById(R.id.bodyContainer);

        RecyclerView rvBat = card.findViewById(R.id.rvBatsmen);
        RecyclerView rvBowl = card.findViewById(R.id.rvBowlers);

        tvInningsTitle.setText(title);
        tvScore.setText(String.format(Locale.US, "%d/%d", data.runs, data.wkts));
        tvOvers.setText("Overs: " + formatOvers(data.legalBalls));

        boolean inningsCompleted = (data.inningsNo < runningInnings);
        String rateLabel = inningsCompleted ? "RR" : "CRR";
        tvRate.setText(String.format(Locale.US, "%s %.2f", rateLabel, data.getRate()));

        tvExtras.setText(String.format(Locale.US,
                "Extras: %d (Wd %d, Nb %d, Bye %d)",
                data.extrasTotal, data.extrasWd, data.extrasNb, data.extrasBye
        ));

        if (tvBattingLabel != null) {
            String batName = (data.battingTeam == null || data.battingTeam.trim().isEmpty())
                    ? "Team" : data.battingTeam.trim();
            tvBattingLabel.setText("Batting: " + batName);
        }

        if (tvBowlingLabel != null) {
            String bowlName = (data.bowlingTeam == null || data.bowlingTeam.trim().isEmpty())
                    ? "Team" : data.bowlingTeam.trim();
            tvBowlingLabel.setText("Bowling: " + bowlName);
        }

        if (tvYetToBat != null) {
            ArrayList<String> yet = computeYetToBat(match, data);
            if (yet.isEmpty()) {
                tvYetToBat.setVisibility(View.GONE);
            } else {
                tvYetToBat.setVisibility(View.VISIBLE);
                tvYetToBat.setText("Yet to bat: " + joinComma(yet));
            }
        }

        boolean defaultExpanded = (data.inningsNo == runningInnings);
        body.setVisibility(defaultExpanded ? View.VISIBLE : View.GONE);
        ivDrop.setRotation(defaultExpanded ? 180f : 0f);

        View header = card.findViewById(R.id.headerRow);
        View.OnClickListener toggler = vv -> toggleDrop(body, ivDrop, rvBat, rvBowl);

        header.setOnClickListener(toggler);
        ivDrop.setOnClickListener(toggler);

        LinearLayoutManager batLM = new LinearLayoutManager(requireContext()) {
            @Override public boolean canScrollVertically() { return false; }
        };
        rvBat.setLayoutManager(batLM);
        rvBat.setNestedScrollingEnabled(false);
        rvBat.setHasFixedSize(false);
        rvBat.setAdapter(new BatsmanAdapter(data.batsmen));
        rvBat.post(rvBat::requestLayout);

        LinearLayoutManager bowlLM = new LinearLayoutManager(requireContext()) {
            @Override public boolean canScrollVertically() { return false; }
        };
        rvBowl.setLayoutManager(bowlLM);
        rvBowl.setNestedScrollingEnabled(false);
        rvBowl.setHasFixedSize(false);
        rvBowl.setAdapter(new BowlerAdapter(data.bowlers));
        rvBowl.post(rvBowl::requestLayout);
    }

    private ArrayList<String> computeYetToBat(JSONObject match, InningsData data) {
        ArrayList<String> out = new ArrayList<>();

        JSONArray arrA = match.optJSONArray("teamAPlayers");
        JSONArray arrB = match.optJSONArray("teamBPlayers");

        ArrayList<String> pool = new ArrayList<>();
        String teamA = match.optString("teamA", "Team A");
        String teamB = match.optString("teamB", "Team B");

        boolean battingIsA = data.battingTeam != null && data.battingTeam.equalsIgnoreCase(teamA);
        boolean battingIsB = data.battingTeam != null && data.battingTeam.equalsIgnoreCase(teamB);

        if (battingIsA && arrA != null) {
            for (int i = 0; i < arrA.length(); i++) pool.add(arrA.optString(i));
        } else if (battingIsB && arrB != null) {
            for (int i = 0; i < arrB.length(); i++) pool.add(arrB.optString(i));
        } else {
            return out;
        }

        HashSet<String> remove = new HashSet<>();
        for (BatsmanRow br : data.batsmen) {
            if (br == null || br.name == null) continue;
            String key = br.name.trim().toLowerCase(Locale.US);

            // REMOVE if player ever appeared in batsmen list (even 0 balls)
            remove.add(key);

        }

        for (String p : pool) {
            if (p == null) continue;
            String name = p.trim();
            if (name.isEmpty()) continue;
            if (!remove.contains(name.toLowerCase(Locale.US))) out.add(name);
        }

        return out;
    }

    private String joinComma(ArrayList<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private void toggleDrop(View body, ImageView ivDrop, RecyclerView rvBat, RecyclerView rvBowl) {
        if (body == null || ivDrop == null) return;

        boolean show = body.getVisibility() != View.VISIBLE;
        body.setVisibility(show ? View.VISIBLE : View.GONE);
        ivDrop.animate().rotation(show ? 180f : 0f).setDuration(150).start();

        body.post(() -> {
            body.requestLayout();
            if (rvBat != null) rvBat.requestLayout();
            if (rvBowl != null) rvBowl.requestLayout();
        });
    }


    private String safe(String s) { return s == null ? "" : s.trim(); }

    private boolean countsAsTeamWicket(String wicketType) {
        return !safe(wicketType).equalsIgnoreCase("Retired Hurt");
    }

    private String getDismissedBatsman(JSONObject e) {
        // ✅ your ScoringActivity stores this correctly for run out
        String out = safe(e.optString("outBatsman", ""));
        if (!out.isEmpty()) return out;

        // fallback for older saved matches (if any)
        return safe(e.optString("striker", ""));
    }


    // ---------------- Build innings from ballEvents ----------------

    private InningsData buildInnings(@NonNull JSONObject match,
                                     @Nullable JSONArray ballEvents,
                                     int inningsNo,
                                     int runningInnings) {

        InningsData out = new InningsData();
        out.inningsNo = inningsNo;

        JSONObject snap = match.optJSONObject(inningsNo == 1 ? "innings1Snapshot" : "innings2Snapshot");
        if (snap != null) {
            out.battingTeam = snap.optString("battingTeam", "");
            out.bowlingTeam = snap.optString("bowlingTeam", "");
        }

        if (out.battingTeam.isEmpty() || out.bowlingTeam.isEmpty()) {
            String teamA = match.optString("teamA", "Team A");
            String teamB = match.optString("teamB", "Team B");

            boolean teamABattingNow = match.optBoolean("teamABatting", true);

            boolean innsBatA;
            if (inningsNo == runningInnings) innsBatA = teamABattingNow;
            else if (inningsNo == 1 && runningInnings == 2) innsBatA = !teamABattingNow;
            else innsBatA = teamABattingNow;

            out.battingTeam = innsBatA ? teamA : teamB;
            out.bowlingTeam = innsBatA ? teamB : teamA;
        }

        Map<String, BatsmanRow> batMap = new HashMap<>();
        Map<String, BowlerRow> bowlMap = new HashMap<>();

        String strikerToMark = "";
        if (inningsNo == runningInnings) strikerToMark = match.optString("striker", "");

        // ✅ pre-add current innings active players so they show even if 0 balls
        if (inningsNo == runningInnings) {
            String s = match.optString("striker", "");
            String ns = match.optString("nonStriker", "");
            String bw = match.optString("bowler", "");

            if (!safe(s).isEmpty()) {
                BatsmanRow br = new BatsmanRow();
                br.name = s;
                br.status = "not out";
                batMap.put(s, br);
            }
            if (!safe(ns).isEmpty() && !ns.equalsIgnoreCase(s)) {
                BatsmanRow br = new BatsmanRow();
                br.name = ns;
                br.status = "not out";
                batMap.put(ns, br);
            }
            if (!safe(bw).isEmpty()) {
                BowlerRow r = new BowlerRow();
                r.name = bw;
                bowlMap.put(bw, r);
            }
        }

        Map<String, int[]> overMap = new HashMap<>();

        if (ballEvents != null) {
            for (int i = 0; i < ballEvents.length(); i++) {
                JSONObject e = ballEvents.optJSONObject(i);
                if (e == null) continue;
                if (e.optInt("innings", 1) != inningsNo) continue;

                boolean legalBall = e.optBoolean("legalBall", true);
                boolean wide = e.optBoolean("wide", false);
                boolean noBall = e.optBoolean("noBall", false);
                boolean byes = e.optBoolean("byes", false);
                boolean wicket = e.optBoolean("wicket", false);

                String label = e.optString("label", "");
                int teamRuns = e.optInt("teamRuns", 0);

                String striker = safe(e.optString("striker", ""));
                String nonStriker = safe(e.optString("nonStriker", ""));
                String bowler = safe(e.optString("bowler", ""));

                String wicketType = e.optString("wicketType", "");
                String wicketBy = e.optString("wicketBy", "");
                String fielder = e.optString("fielder", "");

                int overNo = e.optInt("overNo", -1);

                // ✅ totals
                out.runs += teamRuns;
                if (wicket && countsAsTeamWicket(wicketType)) out.wkts += 1;
                if (legalBall) out.legalBalls += 1;

                // ✅ extras (keep your existing logic)
                if (wide) out.extrasWd += teamRuns;
                else if (noBall) out.extrasNb += 1;     // (your scoring adds only +1 extra for NB)
                else if (byes) out.extrasBye += teamRuns;

                // ✅ Ensure striker/nonStriker exist in batMap (important for run-out non-striker)
                if (!striker.isEmpty() && !batMap.containsKey(striker)) {
                    BatsmanRow br = new BatsmanRow();
                    br.name = striker;
                    br.status = "not out";
                    batMap.put(striker, br);
                }
                if (!nonStriker.isEmpty() && !batMap.containsKey(nonStriker)) {
                    BatsmanRow br = new BatsmanRow();
                    br.name = nonStriker;
                    br.status = "not out";
                    batMap.put(nonStriker, br);
                }

                // ✅ Update STRIKER batting stats (he faced the ball, even if non-striker is run out)
                if (!striker.isEmpty()) {
                    BatsmanRow br = batMap.get(striker);
                    if (br != null) {
                        if (legalBall) br.b += 1;

                        int batRuns = 0;
                        if (!wide && !byes) batRuns = parseRunsFromLabel(label);

                        br.r += batRuns;
                        if (batRuns == 4) br.f4 += 1;
                        if (batRuns == 6) br.f6 += 1;

                        br.sr = br.b > 0 ? (br.r * 100f / br.b) : 0f;
                    }
                }

                // ✅ Apply wicket status to the ACTUAL dismissed batsman (run-out can be non-striker)
                if (wicket) {
                    String dismissed = getDismissedBatsman(e);

                    if (!dismissed.isEmpty()) {
                        // ensure row exists for dismissed batsman
                        BatsmanRow outRow = batMap.get(dismissed);
                        if (outRow == null) {
                            outRow = new BatsmanRow();
                            outRow.name = dismissed;
                            outRow.status = "not out";
                            batMap.put(dismissed, outRow);
                        }

                        String credited = wicketBy.isEmpty() ? bowler : wicketBy;
                        outRow.status = formatDismissal(wicketType, credited, fielder);
                    }
                }

                // ✅ bowling
                if (!bowler.isEmpty()) {
                    BowlerRow bw = bowlMap.get(bowler);
                    if (bw == null) {
                        bw = new BowlerRow();
                        bw.name = bowler;
                        bowlMap.put(bowler, bw);
                    }

                    if (!byes) bw.r += teamRuns;
                    if (legalBall) bw.balls += 1;

                    if (wicket && countsToBowlerWicket(wicketType)) bw.w += 1;

                    // maiden calc
                    if (overNo >= 0) {
                        String key = bowler + "#" + overNo;
                        int[] st = overMap.get(key);
                        if (st == null) st = new int[]{0, 0};

                        if (legalBall) st[0]++;
                        if (!byes) st[1] += teamRuns;

                        overMap.put(key, st);
                    }
                }
            }
        }

        out.extrasTotal = out.extrasWd + out.extrasNb + out.extrasBye;

        for (BowlerRow bw : bowlMap.values()) {
            bw.o = formatOvers(bw.balls);
            bw.econ = (bw.balls > 0) ? (bw.r / (bw.balls / 6f)) : 0f;
        }

        // maidens
        Map<String, Integer> maidenByBowler = new HashMap<>();
        for (Map.Entry<String, int[]> en : overMap.entrySet()) {
            int[] st = en.getValue();
            if (st[0] == 6 && st[1] == 0) {
                String b = en.getKey().split("#")[0];
                maidenByBowler.put(b, maidenByBowler.getOrDefault(b, 0) + 1);
            }
        }
        for (BowlerRow bw : bowlMap.values()) {
            bw.m = maidenByBowler.getOrDefault(bw.name, 0);
        }

        // mark striker on running innings
        if (!strikerToMark.isEmpty()) {
            BatsmanRow br = batMap.get(strikerToMark);
            if (br != null) br.isStriker = true;
        }

        out.batsmen = new ArrayList<>(batMap.values());
        out.bowlers = new ArrayList<>(bowlMap.values());
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

    private String formatDismissal(String wicketType, String wicketBy, String fielder) {
        String how = wicketType == null ? "" : wicketType.trim();
        String by = wicketBy == null ? "" : wicketBy.trim();
        String f = fielder == null ? "" : fielder.trim();

        if (how.isEmpty()) return "out";

        if (how.equalsIgnoreCase("Bowled")) return "b " + safeName(by);
        if (how.equalsIgnoreCase("LBW")) return "lbw b " + safeName(by);
        if (how.equalsIgnoreCase("Hit Wicket")) return "hit wicket b " + safeName(by);

        if (how.equalsIgnoreCase("Caught")) {
            return "c " + (f.isEmpty() ? "?" : f) + " b " + safeName(by);
        }
        if (how.equalsIgnoreCase("Stumped")) {
            return "st " + (f.isEmpty() ? "?" : f) + " b " + safeName(by);
        }
        if (how.equalsIgnoreCase("Run Out")) {
            return "run out" + (f.isEmpty() ? "" : (" (" + f + ")"));
        }
        if (how.equalsIgnoreCase("Retired Hurt")) return "retired hurt";

        return how + (by.isEmpty() ? "" : (" b " + by));
    }

    private String safeName(String s) {
        if (s == null) return "?";
        s = s.trim();
        return s.isEmpty() ? "?" : s;
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

    private String formatOvers(int balls) {
        int ov = balls / 6;
        int bl = balls % 6;
        return String.format(Locale.US, "%d.%d", ov, bl);
    }

    static class InningsData {
        int inningsNo = 1;

        String battingTeam = "";
        String bowlingTeam = "";

        int runs = 0;
        int wkts = 0;
        int legalBalls = 0;

        int extrasWd = 0;
        int extrasNb = 0;
        int extrasBye = 0;
        int extrasTotal = 0;

        ArrayList<BatsmanRow> batsmen = new ArrayList<>();
        ArrayList<BowlerRow> bowlers = new ArrayList<>();

        float getRate() {
            float overs = legalBalls / 6f;
            return overs > 0 ? (runs / overs) : 0f;
        }
    }

    private String buildMatchResult(JSONObject match, String teamA, String teamB) {
        int innings = match.optInt("innings", 1);
        int target = match.optInt("target", 0);
        int runs = match.optInt("totalRuns", 0);
        int wickets = match.optInt("wickets", 0);
        int players = match.optInt("playersPerTeam", 11);

        boolean teamABatting = match.optBoolean("teamABatting", true);
        String battingTeam = teamABatting ? teamA : teamB;
        String bowlingTeam = teamABatting ? teamB : teamA;

        if (innings == 2 && target > 0) {
            if (runs >= target) {
                int wkLeft = Math.max(0, (players - 1) - wickets);
                return "  Match Result : " + battingTeam + " won by " + wkLeft + " wickets\n";
            } else {
                int margin = (target - 1) - runs;
                if (margin == 0) return "  Match Tied";
                return "  Match Result : " + bowlingTeam + " won by " + margin + " runs\n";
            }
        }

        return "Match Completed";
    }
}
