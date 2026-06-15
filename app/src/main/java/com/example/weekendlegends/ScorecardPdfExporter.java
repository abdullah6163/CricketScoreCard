package com.example.weekendlegends;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

public class ScorecardPdfExporter {

    // A4 @ 72dpi
    private static final int PAGE_W = 595;
    private static final int PAGE_H = 842;

    private static final int M = 36; // margin
    private static final int LINE_GAP = 6;
    private static final int BOTTOM = PAGE_H - 36; // usable bottom

    private static final String SUBDIR = "WeekendLegends";

    /**
     * ✅ Google Play safe:
     * - Android 10+ : save to Downloads using MediaStore (no permission)
     * - Android 9-  : save to public Downloads directory (needs WRITE_EXTERNAL_STORAGE maxSdk=28)
     *
     * After saving, it opens the PDF with chooser ("Open with").
     */
    public static void exportToDownloadsAndOpen(Context c, String title, String matchJson) {
        try {
            JSONObject match = new JSONObject(matchJson == null ? "{}" : matchJson);

            String safeName = (title == null ? "Match" : title)
                    .replaceAll("[^a-zA-Z0-9_\\-]", "_");

            String fileName = "Scorecard_" + safeName + "_" + System.currentTimeMillis() + ".pdf";

            Uri savedUri = savePdfToDownloads(c, fileName, match);
            if (savedUri == null) return;

            openPdf(c, savedUri);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Keeps your old behavior (share chooser), but still writes a real PDF first.
     * Note: sharing is not "saving". Use exportToDownloadsAndOpen() for saving.
     */
    public static void exportAndShare(Context c, String title, String matchJson) {
        try {
            JSONObject match = new JSONObject(matchJson == null ? "{}" : matchJson);

            // Write to app external files dir (no permission) then share via FileProvider
            File dir = new File(c.getExternalFilesDir(null), "exports");
            if (!dir.exists()) dir.mkdirs();

            String safeName = (title == null ? "Match" : title)
                    .replaceAll("[^a-zA-Z0-9_\\-]", "_");

            File file = new File(dir, "Scorecard_" + safeName + "_" + System.currentTimeMillis() + ".pdf");
            FileOutputStream fos = new FileOutputStream(file);
            writePdfToStream(match, fos);
            fos.close();

            Uri uri = FileProvider.getUriForFile(c, c.getPackageName() + ".fileprovider", file);

            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/pdf");
            send.putExtra(Intent.EXTRA_SUBJECT, safeName);
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            c.startActivity(Intent.createChooser(send, "Share scorecard PDF"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- SAVE to Downloads (API split) ----------------

    private static Uri savePdfToDownloads(Context c, String fileName, JSONObject match) {
        OutputStream os = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // ✅ Android 10+ : MediaStore Downloads (no permission)
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                cv.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + SUBDIR);
                cv.put(MediaStore.Downloads.IS_PENDING, 1);

                ContentResolver cr = c.getContentResolver();
                Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri == null) return null;

                os = cr.openOutputStream(uri);
                if (os == null) return null;

                writePdfToStream(match, os);
                os.flush();
                os.close();
                os = null;

                cv.clear();
                cv.put(MediaStore.Downloads.IS_PENDING, 0);
                cr.update(uri, cv, null, null);

                return uri;

            } else {
                // ✅ Android 9- : public Downloads (needs WRITE_EXTERNAL_STORAGE maxSdk=28 in manifest)
                File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File dir = new File(downloads, SUBDIR);
                if (!dir.exists()) dir.mkdirs();

                File outFile = new File(dir, fileName);
                os = new FileOutputStream(outFile);
                writePdfToStream(match, os);
                os.flush();
                os.close();
                os = null;

                // Use FileProvider for N+ (and generally safe)
                return FileProvider.getUriForFile(c, c.getPackageName() + ".fileprovider", outFile);
            }

        } catch (Exception e) {
            e.printStackTrace();
            try { if (os != null) os.close(); } catch (Exception ignored) {}
            return null;
        }
    }

    private static void openPdf(Context c, Uri uri) {
        try {
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, "application/pdf");
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(Intent.createChooser(view, "Open scorecard PDF"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- PDF writer (multi-page, writes to any OutputStream) ----------------

    private static class Writer {
        PdfDocument doc;
        PdfDocument.Page page;
        Canvas cv;
        Paint p;
        int pageNo = 0;
        int y = M;

        Writer() {
            doc = new PdfDocument();
            p = new Paint(Paint.ANTI_ALIAS_FLAG);
            newPage();
        }

        void newPage() {
            if (page != null) doc.finishPage(page);
            pageNo++;
            PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create();
            page = doc.startPage(pi);
            cv = page.getCanvas();
            y = M;
        }

        void ensureSpace(int neededPx) {
            if (y + neededPx >= BOTTOM) newPage();
        }

        void closeToStream(OutputStream os) throws Exception {
            if (page != null) doc.finishPage(page);
            doc.writeTo(os);
            doc.close();
        }
    }

    private static void writePdfToStream(JSONObject match, OutputStream os) throws Exception {
        Writer w = new Writer();

        String teamA = match.optString("teamA", "Team A");
        String teamB = match.optString("teamB", "Team B");
        String title = teamA + " vs " + teamB;

        // ---- Title ----
        w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        w.p.setTextSize(18);
        w.ensureSpace(28);
        w.y = drawText(w.cv, w.p, title, M, w.y);

        // ---- Result + MoM (only completed) ----
        String result = buildResultText(match);
        if (!result.isEmpty()) {
            w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            w.p.setTextSize(13);
            w.ensureSpace(22);
            w.y = drawText(w.cv, w.p, result, M, w.y + 6);
        }

        Mom mom = computeManOfTheMatch(match);
        if (match.optBoolean("isCompleted", false) && mom != null && !mom.name.isEmpty()) {
            w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            w.p.setTextSize(12);
            w.ensureSpace(20);
            w.y = drawText(w.cv, w.p,
                    "Man of the Match: " + mom.name + (mom.team.isEmpty() ? "" : (" (" + mom.team + ")")),
                    M, w.y + 6);

            w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            w.p.setTextSize(11);
            String detail = "";
            if (mom.runs > 0 || mom.balls > 0) detail += String.format(Locale.US, "Batting: %d (%d balls)  ", mom.runs, mom.balls);
            if (mom.wickets > 0 || mom.bowlRuns > 0) detail += String.format(Locale.US, "Bowling: %d W, %d R", mom.wickets, mom.bowlRuns);
            if (!detail.trim().isEmpty()) {
                w.ensureSpace(18);
                w.y = drawText(w.cv, w.p, detail.trim(), M, w.y + 2);
            }
        }

        // ---- Date + Toss ----
        w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        w.p.setTextSize(11);

        long dateUtc = match.optLong("matchDateUtc", 0L);
        String dateText = (dateUtc > 0)
                ? android.text.format.DateFormat.format("dd MMM yyyy, hh:mm a", dateUtc).toString()
                : "";

        String tossWinner = match.optString("tossWinner", "");
        String tossDecision = match.optString("tossDecision", "");
        String tossLine = buildTossLine(teamA, teamB, tossWinner, tossDecision);

        if (!dateText.isEmpty()) { w.ensureSpace(18); w.y = drawText(w.cv, w.p, "Date: " + dateText, M, w.y + 8); }
        if (!tossLine.isEmpty()) { w.ensureSpace(18); w.y = drawText(w.cv, w.p, tossLine, M, w.y + 2); }

        w.y += 10;
        w.ensureSpace(20);
        drawLine(w.cv, w.y);
        w.y += 14;

        JSONArray ballEvents = match.optJSONArray("ballEvents");

        // ---- Innings 1 ----
        InningsData inn1 = computeInningsFromEvents(match, ballEvents, 1);
        w = drawInningsBlock(w, inn1);

        // Divider between innings
        w.y += 12;
        w.ensureSpace(20);
        drawLine(w.cv, w.y);
        w.y += 14;

        // ---- Innings 2 (only if exists/started/completed) ----
        boolean has2 = hasSecondInnings(match, ballEvents);
        if (has2) {
            InningsData inn2 = computeInningsFromEvents(match, ballEvents, 2);
            w = drawInningsBlock(w, inn2);
        } else {
            w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
            w.p.setTextSize(11);
            w.ensureSpace(18);
            w.y = drawText(w.cv, w.p, "2nd Innings not started.", M, w.y);
        }

        w.closeToStream(os);
    }

    private static boolean hasSecondInnings(JSONObject match, JSONArray ballEvents) {
        int innings = match.optInt("innings", 1);
        if (innings >= 2) return true;
        if (ballEvents == null) return false;
        for (int i = 0; i < ballEvents.length(); i++) {
            JSONObject e = ballEvents.optJSONObject(i);
            if (e != null && e.optInt("innings", 1) == 2) return true;
        }
        return false;
    }

    private static String buildResultText(JSONObject match) {
        if (!match.optBoolean("isCompleted", false)) return "";

        String teamA = match.optString("teamA", "Team A");
        String teamB = match.optString("teamB", "Team B");

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
                return battingTeam + " won by " + wkLeft + " wickets";
            } else {
                int margin = (target - 1) - runs;
                if (margin == 0) return "Match Tied";
                return bowlingTeam + " won by " + margin + " runs";
            }
        }
        return "Match Completed";
    }

    private static String buildTossLine(String teamA, String teamB, String tw, String td) {
        if (tw == null) tw = "";
        if (td == null) td = "";
        if (tw.trim().isEmpty() || td.trim().isEmpty()) return "";

        String winnerName = tw.equalsIgnoreCase("A") ? teamA : teamB;
        String decision = td.equalsIgnoreCase("BAT") ? "bat" : "bowl";
        return "Toss: " + winnerName + " won and chose to " + decision;
    }

    // ---------------- innings computation (FULL, from ballEvents + team lists) ----------------

    private static class BatRow {
        String name = "";
        int r = 0, b = 0, f4 = 0, f6 = 0;
        String status = "not out";
    }

    private static class BowlRow {
        String name = "";
        int balls = 0;
        int runs = 0;
        int wk = 0;
        int maidens = 0;
    }

    private static class InningsData {
        int innNo;
        String battingTeam = "";
        String bowlingTeam = "";
        int runs = 0, wkts = 0, ballsUsed = 0;
        ArrayList<BatRow> bats = new ArrayList<>();
        ArrayList<BowlRow> bowls = new ArrayList<>();
        ArrayList<String> fow = new ArrayList<>();
        ArrayList<String> yetToBat = new ArrayList<>();
        int extrasTotal = 0, extrasWd = 0, extrasNb = 0, extrasBye = 0;
    }

    /**
     * ✅ FIXED:
     * - Correctly detects batting team for each innings by checking striker/nonStriker against squads
     * - Yet-to-bat rule: remove if name appeared as striker OR nonStriker (even if 0 balls faced)
     */
    private static InningsData computeInningsFromEvents(JSONObject match, JSONArray ballEvents, int innNo) {
        InningsData out = new InningsData();
        out.innNo = innNo;

        String teamA = match.optString("teamA", "Team A");
        String teamB = match.optString("teamB", "Team B");

        ArrayList<String> teamAPlayers = jsonArrayToList(match.optJSONArray("teamAPlayers"));
        ArrayList<String> teamBPlayers = jsonArrayToList(match.optJSONArray("teamBPlayers"));

        // ✅ FIX: infer who batted in this innings from the first event of that innings
        boolean innBatA = inferInningsBatA(ballEvents, innNo, teamAPlayers, teamBPlayers);

        out.battingTeam = innBatA ? teamA : teamB;
        out.bowlingTeam = innBatA ? teamB : teamA;

        ArrayList<String> battingSquad = innBatA ? teamAPlayers : teamBPlayers;

        Map<String, BatRow> batMap = new HashMap<>();
        Map<String, BowlRow> bowlMap = new HashMap<>();
        Map<String, int[]> overMap = new HashMap<>();

        // track who appeared (striker OR nonStriker) for Yet-to-bat rule
        HashSet<String> appearedBatting = new HashSet<>();

        if (ballEvents != null) {
            int wkCount = 0;
            for (int i = 0; i < ballEvents.length(); i++) {
                JSONObject e = ballEvents.optJSONObject(i);
                if (e == null) continue;
                if (e.optInt("innings", 1) != innNo) continue;

                boolean legalBall = e.optBoolean("legalBall", true);
                boolean wide = e.optBoolean("wide", false);
                boolean noBall = e.optBoolean("noBall", false);
                boolean byes = e.optBoolean("byes", false);
                boolean wicket = e.optBoolean("wicket", false);

                String label = e.optString("label", "");
                int teamRuns = e.optInt("teamRuns", 0);

                String striker = e.optString("striker", "").trim();
                String nonStriker = e.optString("nonStriker", "").trim();
                String bowler = e.optString("bowler", "").trim();

                String wicketType = e.optString("wicketType", "");
                String wicketBy = e.optString("wicketBy", "");
                String fielder = e.optString("fielder", "");

                int overNo = e.optInt("overNo", 0);
                int ballInOver = e.optInt("ballInOver", 0);

                // ✅ appeared (even if only wide happened)
                if (!striker.isEmpty()) appearedBatting.add(striker.toLowerCase(Locale.US));
                if (!nonStriker.isEmpty()) appearedBatting.add(nonStriker.toLowerCase(Locale.US));

                out.runs += teamRuns;
                if (wicket) { out.wkts += 1; wkCount++; }
                if (legalBall) out.ballsUsed += 1;

                // extras
                if (wide) out.extrasWd += teamRuns;
                else if (noBall) out.extrasNb += 1;
                else if (byes) out.extrasBye += teamRuns;

                // batsman row
                if (!striker.isEmpty()) {
                    BatRow br = batMap.get(striker);
                    if (br == null) {
                        br = new BatRow();
                        br.name = striker;
                        br.status = "not out";
                        batMap.put(striker, br);
                    }

                    // faced ball: anything that is NOT wide counts as faced (even no-ball/byes)
                    if (!wide) br.b += 1;

                    int batRuns = 0;
                    if (!wide && !byes) batRuns = parseRunsFromLabel(label);
                    br.r += batRuns;

                    if (batRuns == 4) br.f4 += 1;
                    if (batRuns == 6) br.f6 += 1;

                    if (wicket) {
                        String credited = wicketBy.isEmpty() ? bowler : wicketBy;
                        br.status = formatDismissal(wicketType, credited, fielder);

                        // ✅ FOW (clean)
                        String ov = String.format(Locale.US, "%d.%d", overNo, ballInOver);
                        out.fow.add(String.format(Locale.US,
                                "%d) %s  %d/%d  (%s ov)",
                                wkCount, striker, out.runs, out.wkts, ov
                        ));
                    }
                }

                // bowler row
                if (!bowler.isEmpty()) {
                    BowlRow bw = bowlMap.get(bowler);
                    if (bw == null) {
                        bw = new BowlRow();
                        bw.name = bowler;
                        bowlMap.put(bowler, bw);
                    }

                    if (!byes) bw.runs += teamRuns;
                    if (legalBall) bw.balls += 1;
                    if (wicket && countsToBowlerWicket(wicketType)) bw.wk += 1;

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

        // maidens
        Map<String, Integer> maidenByBowler = new HashMap<>();
        for (Map.Entry<String, int[]> en : overMap.entrySet()) {
            int[] st = en.getValue();
            if (st[0] == 6 && st[1] == 0) {
                String bowler = en.getKey().split("#")[0];
                maidenByBowler.put(bowler, maidenByBowler.getOrDefault(bowler, 0) + 1);
            }
        }
        for (BowlRow bw : bowlMap.values()) bw.maidens = maidenByBowler.getOrDefault(bw.name, 0);

        out.bats = new ArrayList<>(batMap.values());
        out.bowls = new ArrayList<>(bowlMap.values());

        // sort bats by runs desc then balls desc
        Collections.sort(out.bats, (a, b) -> {
            if (b.r != a.r) return b.r - a.r;
            return b.b - a.b;
        });

        // sort bowls by wickets desc then runs asc
        Collections.sort(out.bowls, (a, b) -> {
            if (b.wk != a.wk) return b.wk - a.wk;
            return a.runs - b.runs;
        });

        // ✅ Yet to bat: only those who NEVER appeared as striker or nonStriker
        out.yetToBat.clear();
        for (String p : battingSquad) {
            if (p == null) continue;
            String name = p.trim();
            if (name.isEmpty()) continue;

            if (!appearedBatting.contains(name.toLowerCase(Locale.US))) {
                out.yetToBat.add(name);
            }
        }

        // fallback if squad missing
        if (battingSquad.isEmpty()) {
            for (BatRow br : out.bats) out.yetToBat.remove(br.name);
        }

        return out;
    }

    private static boolean inferInningsBatA(JSONArray ballEvents, int innNo,
                                            ArrayList<String> teamAPlayers,
                                            ArrayList<String> teamBPlayers) {
        if (ballEvents == null) return true;

        for (int i = 0; i < ballEvents.length(); i++) {
            JSONObject e = ballEvents.optJSONObject(i);
            if (e == null) continue;
            if (e.optInt("innings", 1) != innNo) continue;

            String s = e.optString("striker", "").trim();
            String ns = e.optString("nonStriker", "").trim();

            if (!s.isEmpty()) {
                if (belongsTo(teamAPlayers, s)) return true;
                if (belongsTo(teamBPlayers, s)) return false;
            }
            if (!ns.isEmpty()) {
                if (belongsTo(teamAPlayers, ns)) return true;
                if (belongsTo(teamBPlayers, ns)) return false;
            }
        }
        return true;
    }

    private static ArrayList<String> jsonArrayToList(JSONArray arr) {
        ArrayList<String> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            String s = arr.optString(i, "").trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    // ---------------- drawing blocks ----------------

    private static Writer drawInningsBlock(Writer w, InningsData in) {
        // Header
        w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        w.p.setTextSize(14);
        w.ensureSpace(24);
        w.y = drawText(w.cv, w.p, (in.innNo == 1 ? "1st Innings" : "2nd Innings"), M, w.y);

        // Score line
        String oversStr = formatOvers(in.ballsUsed);
        float rr = in.ballsUsed > 0 ? (in.runs / (in.ballsUsed / 6f)) : 0f;

        w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        w.p.setTextSize(13);
        w.ensureSpace(24);
        w.y = drawText(w.cv, w.p,
                String.format(Locale.US, "Score: %d/%d  (%s overs)   RR %.2f", in.runs, in.wkts, oversStr, rr),
                M, w.y + 8);

        // Extras line
        w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        w.p.setTextSize(11);
        w.ensureSpace(18);
        w.y = drawText(w.cv, w.p,
                String.format(Locale.US, "Extras: %d (Wd %d, Nb %d, Bye %d)",
                        in.extrasTotal, in.extrasWd, in.extrasNb, in.extrasBye),
                M, w.y + 4);

        w.y += 10;

        // Batting header WITH TEAM NAME (as requested)
        w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        w.p.setTextSize(12);
        w.ensureSpace(18);
        w.y = drawText(w.cv, w.p, "Batting: " + in.battingTeam, M, w.y);

        w.y += 6;
        w = drawBatsmanTable(w, in);

        // Yet to bat
        if (in.yetToBat != null && !in.yetToBat.isEmpty()) {
            w.y += 10;
            w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            w.p.setTextSize(12);
            w.ensureSpace(18);
            w.y = drawText(w.cv, w.p, "Yet to bat", M, w.y);

            w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            w.p.setTextSize(11);
            String joined = joinWithComma(in.yetToBat);
            w.ensureSpace(40);
            w.y = drawWrappedText(w.cv, w.p, joined, M, w.y + 6, PAGE_W - 2 * M);
        }

        // Bowling header WITH TEAM NAME (as requested)
        w.y += 12;
        w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        w.p.setTextSize(12);
        w.ensureSpace(18);
        w.y = drawText(w.cv, w.p, "Bowling: " + in.bowlingTeam, M, w.y);

        w.y += 6;
        w = drawBowlerTable(w, in);

        // Fall of wickets (clean bullets, no "@")
        if (in.fow != null && !in.fow.isEmpty()) {
            w.y += 12;
            w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            w.p.setTextSize(12);
            w.ensureSpace(18);
            w.y = drawText(w.cv, w.p, "Fall of wickets", M, w.y);

            w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            w.p.setTextSize(11);

            for (String line : in.fow) {
                w.ensureSpace(18);
                w.y = drawText(w.cv, w.p, "• " + line, M, w.y + 6);
            }
        }

        return w;
    }

    private static Writer drawBatsmanTable(Writer w, InningsData in) {
        // columns: Name | R | B | 4s | 6s | SR | Status
        int width = PAGE_W - 2 * M;

        int cName = (int) (width * 0.34f);
        int cSmall = (int) (width * 0.08f);
        int cSR = (int) (width * 0.12f);
        int cStatus = width - (cName + cSmall * 4 + cSR);

        w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        w.p.setTextSize(10);
        w.ensureSpace(18);
        w.y = tableRow(w, new String[]{"Batsman", "R", "B", "4s", "6s", "SR", "Status"},
                new int[]{cName, cSmall, cSmall, cSmall, cSmall, cSR, cStatus});

        w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        w.p.setTextSize(10);

        if (in.bats == null || in.bats.isEmpty()) {
            w.ensureSpace(18);
            w.y = tableRow(w, new String[]{"—", "0", "0", "0", "0", "0.00", ""},
                    new int[]{cName, cSmall, cSmall, cSmall, cSmall, cSR, cStatus});
            return w;
        }

        for (BatRow br : in.bats) {
            float sr = br.b > 0 ? (br.r * 100f / br.b) : 0f;
            w.ensureSpace(18);
            w.y = tableRow(w,
                    new String[]{
                            br.name,
                            String.valueOf(br.r),
                            String.valueOf(br.b),
                            String.valueOf(br.f4),
                            String.valueOf(br.f6),
                            String.format(Locale.US, "%.2f", sr),
                            br.status
                    },
                    new int[]{cName, cSmall, cSmall, cSmall, cSmall, cSR, cStatus});
        }
        return w;
    }

    private static Writer drawBowlerTable(Writer w, InningsData in) {
        // columns: Name | O | M | R | W | Econ
        int width = PAGE_W - 2 * M;

        int cName = (int) (width * 0.40f);
        int cO = (int) (width * 0.12f);
        int cM = (int) (width * 0.10f);
        int cR = (int) (width * 0.12f);
        int cW = (int) (width * 0.10f);
        int cE = width - (cName + cO + cM + cR + cW);

        w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        w.p.setTextSize(10);
        w.ensureSpace(18);
        w.y = tableRow(w, new String[]{"Bowler", "O", "M", "R", "W", "Econ"},
                new int[]{cName, cO, cM, cR, cW, cE});

        w.p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        w.p.setTextSize(10);

        if (in.bowls == null || in.bowls.isEmpty()) {
            w.ensureSpace(18);
            w.y = tableRow(w, new String[]{"—", "0.0", "0", "0", "0", "0.00"},
                    new int[]{cName, cO, cM, cR, cW, cE});
            return w;
        }

        for (BowlRow br : in.bowls) {
            String ov = formatOvers(br.balls);
            float econ = br.balls > 0 ? (br.runs / (br.balls / 6f)) : 0f;

            w.ensureSpace(18);
            w.y = tableRow(w,
                    new String[]{
                            br.name,
                            ov,
                            String.valueOf(br.maidens),
                            String.valueOf(br.runs),
                            String.valueOf(br.wk),
                            String.format(Locale.US, "%.2f", econ)
                    },
                    new int[]{cName, cO, cM, cR, cW, cE});
        }

        return w;
    }

    private static int tableRow(Writer w, String[] cells, int[] widths) {
        int rowH = 18;
        int x = M;

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStrokeWidth(1);
        line.setAlpha(50);

        for (int i = 0; i < cells.length; i++) {
            w.cv.drawText(trimToFit(w.p, cells[i], widths[i] - 6), x + 3, w.y + 13, w.p);
            x += widths[i];
        }

        w.cv.drawLine(M, w.y + rowH, PAGE_W - M, w.y + rowH, line);
        return w.y + rowH;
    }

    private static void drawLine(Canvas cv, int y) {
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStrokeWidth(1.2f);
        line.setAlpha(70);
        cv.drawLine(M, y, PAGE_W - M, y, line);
    }

    private static int drawText(Canvas cv, Paint p, String s, int x, int y) {
        cv.drawText(s, x, y + p.getTextSize(), p);
        return y + (int) p.getTextSize();
    }

    private static int drawWrappedText(Canvas cv, Paint p, String text, int x, int y, int maxW) {
        if (text == null) text = "";
        String[] words = text.split("\\s+");
        String line = "";
        for (String w : words) {
            String test = line.isEmpty() ? w : (line + " " + w);
            if (p.measureText(test) > maxW) {
                cv.drawText(line, x, y + p.getTextSize(), p);
                y += (int) p.getTextSize() + LINE_GAP;
                line = w;
            } else {
                line = test;
            }
        }
        if (!line.isEmpty()) {
            cv.drawText(line, x, y + p.getTextSize(), p);
            y += (int) p.getTextSize() + LINE_GAP;
        }
        return y;
    }

    private static String trimToFit(Paint p, String s, int maxW) {
        if (s == null) return "";
        if (p.measureText(s) <= maxW) return s;
        String ell = "…";
        int n = s.length();
        while (n > 0 && p.measureText(s.substring(0, n) + ell) > maxW) n--;
        return (n <= 0) ? ell : (s.substring(0, n) + ell);
    }

    private static String formatOvers(int balls) {
        int ov = balls / 6;
        int bl = balls % 6;
        return String.format(Locale.US, "%d.%d", ov, bl);
    }

    private static int parseRunsFromLabel(String label) {
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

    private static boolean countsToBowlerWicket(String wicketType) {
        if (wicketType == null) return true;
        String t = wicketType.toLowerCase(Locale.US);
        return !(t.contains("run out")
                || t.contains("retired")
                || t.contains("obstruct")
                || t.contains("timed out"));
    }

    private static String formatDismissal(String wicketType, String wicketBy, String fielder) {
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

    private static String safeName(String s) {
        if (s == null) return "?";
        s = s.trim();
        return s.isEmpty() ? "?" : s;
    }

    private static String joinWithComma(ArrayList<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    // ---------------- Man of the Match (simple, safe) ----------------
    // Logic used:
    // score = runs*1000 + wickets*600 + strikeRateComponent
    // strikeRateComponent = (runs*100/balls) only if balls>0 else 0
    // This naturally prefers: big runs OR big wickets; tie-breaker favors SR.
    private static class Mom {
        String name = "";
        String team = "";
        int runs = 0;
        int balls = 0;
        int wickets = 0;
        int bowlRuns = 0;
    }

    private static Mom computeManOfTheMatch(JSONObject match) {
        if (!match.optBoolean("isCompleted", false)) return null;

        ArrayList<String> teamAPlayers = jsonArrayToList(match.optJSONArray("teamAPlayers"));
        ArrayList<String> teamBPlayers = jsonArrayToList(match.optJSONArray("teamBPlayers"));

        JSONArray ballEvents = match.optJSONArray("ballEvents");
        if (ballEvents == null) return null;

        class Agg { int r=0,b=0,w=0,br=0; } // batting runs/balls, wickets, bowling runs
        Map<String, Agg> map = new HashMap<>();

        for (int i = 0; i < ballEvents.length(); i++) {
            JSONObject e = ballEvents.optJSONObject(i);
            if (e == null) continue;

            boolean wide = e.optBoolean("wide", false);
            boolean byes = e.optBoolean("byes", false);
            boolean wicket = e.optBoolean("wicket", false);

            String striker = e.optString("striker", "").trim();
            String bowler = e.optString("bowler", "").trim();

            int teamRuns = e.optInt("teamRuns", 0);
            String label = e.optString("label", "");

            // batting
            if (!striker.isEmpty()) {
                Agg a = map.get(striker);
                if (a == null) { a = new Agg(); map.put(striker, a); }
                if (!wide) a.b += 1;
                int batRuns = (!wide && !byes) ? parseRunsFromLabel(label) : 0;
                a.r += batRuns;
            }

            // bowling
            if (!bowler.isEmpty()) {
                Agg a = map.get(bowler);
                if (a == null) { a = new Agg(); map.put(bowler, a); }
                if (!byes) a.br += teamRuns;
                String wt = e.optString("wicketType", "");
                if (wicket && countsToBowlerWicket(wt)) a.w += 1;
            }
        }

        String best = "";
        int bestScore = -1;

        for (Map.Entry<String, Agg> en : map.entrySet()) {
            Agg a = en.getValue();
            int srPart = (a.b > 0) ? (a.r * 100 / a.b) : 0;
            int score = (a.r * 1000) + (a.w * 600) + srPart;
            if (score > bestScore) {
                bestScore = score;
                best = en.getKey();
            }
        }

        if (best.isEmpty()) return null;

        Mom m = new Mom();
        m.name = best;

        Agg a = map.get(best);
        if (a != null) {
            m.runs = a.r;
            m.balls = a.b;
            m.wickets = a.w;
            m.bowlRuns = a.br;
        }

        String teamA = match.optString("teamA", "Team A");
        String teamB = match.optString("teamB", "Team B");
        m.team = belongsTo(teamAPlayers, best) ? teamA : (belongsTo(teamBPlayers, best) ? teamB : "");

        return m;
    }

    private static boolean belongsTo(ArrayList<String> list, String name) {
        if (list == null || name == null) return false;
        for (String s : list) {
            if (s != null && s.equalsIgnoreCase(name)) return true;
        }
        return false;
    }
}
