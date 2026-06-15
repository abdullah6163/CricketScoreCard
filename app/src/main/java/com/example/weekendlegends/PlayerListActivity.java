package com.example.weekendlegends;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

public class PlayerListActivity extends AppCompatActivity {

    private RecyclerView rv;
    private TextView tvEmpty;
    private TextView tvLoading;
    private TextInputEditText etSearch;

    private final ArrayList<String> allPlayers = new ArrayList<>();
    private final ArrayList<String> filtered = new ArrayList<>();

    private PlayerAdapter adapter;

    private String pendingPickPlayer = "";

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_player_list);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        rv = findViewById(R.id.rvPlayers);
        tvEmpty = findViewById(R.id.tvEmpty);
        etSearch = findViewById(R.id.etSearch);
        tvLoading = findViewById(R.id.tvLoading);

        // ✅ responsive columns: 2 on small, 3 on bigger
        int span = calculateSpanCount();
        rv.setLayoutManager(new GridLayoutManager(this, span));

        cropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                res -> {
                    if (adapter != null) adapter.notifyDataSetChanged();
                    pendingPickPlayer = "";
                }
        );

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    if (pendingPickPlayer == null || pendingPickPlayer.trim().isEmpty()) return;

                    Intent i = new Intent(this, PlayerPhotoCropActivity.class);
                    i.putExtra(PlayerPhotoCropActivity.EXTRA_PLAYER, pendingPickPlayer);
                    i.putExtra(PlayerPhotoCropActivity.EXTRA_URI, uri.toString());
                    cropLauncher.launch(i);
                }
        );

        // ✅ create adapter ONCE
        adapter = new PlayerAdapter(filtered, new PlayerAdapter.Listener() {
            @Override
            public void onEditImage(String playerName) {
                pendingPickPlayer = playerName;
                pickImageLauncher.launch("image/*");
            }

            @Override
            public void onViewStats(String playerName) {
                Intent i = new Intent(PlayerListActivity.this, PlayerCareerActivity.class);
                i.putExtra("player", playerName);
                startActivity(i);
            }

            @Override
            public void onDeletePlayer(String playerName) {
                showSoftDeleteDialog(playerName);
            }
        });

        rv.setAdapter(adapter);

        // ✅ Search filter
        if (etSearch != null) {
            etSearch.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFilter(s == null ? "" : s.toString());
                }
            });
        }

        // ✅ IMPORTANT: load players in background (fix lag)
        loadPlayersAsync();
    }

    // =========================================================
    // ✅ Background loading (fix lag)
    // =========================================================
    private void loadPlayersAsync() {
        // show loading
        if (tvLoading != null) tvLoading.setVisibility(View.VISIBLE);
        rv.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        new Thread(() -> {
            ArrayList<String> loaded = loadPlayersHeavy();

            runOnUiThread(() -> {
                allPlayers.clear();
                allPlayers.addAll(loaded);

                // apply current search query if user typed something
                String currentQuery = "";
                if (etSearch != null && etSearch.getText() != null) {
                    currentQuery = etSearch.getText().toString();
                }
                applyFilter(currentQuery);

                if (tvLoading != null) tvLoading.setVisibility(View.GONE);
            });
        }).start();
    }

    private ArrayList<String> loadPlayersHeavy() {
        HashSet<String> set = new HashSet<>();

        for (JSONObject row : MatchStore.load(this)) {
            String id = row.optString("id", "");
            String json = MatchStore.getMatchJsonById(this, id);
            if (json == null || json.trim().isEmpty()) continue;

            try {
                JSONObject match = new JSONObject(json);
                JSONArray balls = match.optJSONArray("ballEvents");
                if (balls == null) continue;

                for (int i = 0; i < balls.length(); i++) {
                    JSONObject e = balls.optJSONObject(i);
                    if (e == null) continue;

                    add(set, e.optString("striker"));
                    add(set, e.optString("nonStriker"));
                    add(set, e.optString("bowler"));
                    add(set, e.optString("outBatsman"));
                }
            } catch (Exception ignored) {}
        }

        ArrayList<String> out = new ArrayList<>();
        for (String p : set) {
            if (!PlayerRemoveStore.isRemoved(this, p)) out.add(p);
        }

        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    // =========================================================
    // ✅ Soft delete
    // =========================================================
    private void showSoftDeleteDialog(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Remove player?")
                .setMessage("This will hide \"" + playerName + "\" from your players list.\nMatch history will remain unchanged.")
                .setPositiveButton("Remove", (d, w) -> {
                    PlayerRemoveStore.softRemove(this, playerName);
                    PlayerImageStore.clearImage(this, playerName);

                    // ✅ reload in background again (no lag)
                    loadPlayersAsync();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================================================
    // ✅ Filter + UI
    // =========================================================
    private void applyFilter(String q) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.US);

        filtered.clear();
        if (query.isEmpty()) {
            filtered.addAll(allPlayers);
        } else {
            for (String name : allPlayers) {
                if (name != null && name.toLowerCase(Locale.US).contains(query)) {
                    filtered.add(name);
                }
            }
        }

        if (adapter != null) adapter.notifyDataSetChanged();
        refreshEmpty();
    }

    private void refreshEmpty() {
        boolean empty = filtered.isEmpty();

        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private int calculateSpanCount() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float dpWidth = dm.widthPixels / dm.density;
        if (dpWidth >= 420) return 3;
        return 2;
    }

    private void add(HashSet<String> set, String name) {
        if (name != null) {
            name = name.trim();
            if (!name.isEmpty()) set.add(name);
        }
    }
}
