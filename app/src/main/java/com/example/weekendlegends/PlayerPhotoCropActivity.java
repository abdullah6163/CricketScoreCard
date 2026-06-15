package com.example.weekendlegends;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class PlayerPhotoCropActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER = "player";
    public static final String EXTRA_URI = "uri";

    private CropCircleView cropView;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_player_photo_crop);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        String player = getIntent().getStringExtra(EXTRA_PLAYER);
        String uriStr = getIntent().getStringExtra(EXTRA_URI);

        TextView tv = findViewById(R.id.tvTitle);
        tv.setText(player == null ? "Crop Photo" : player);

        cropView = findViewById(R.id.cropView);

        if (uriStr != null) {
            cropView.setImageUri(Uri.parse(uriStr));
        }

        MaterialButton btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> {
            Bitmap out = cropView.exportCircleBitmap(512);
            if (out != null && player != null) {
                PlayerImageStore.saveBitmapBase64(this, player, out);
            }
            setResult(RESULT_OK);
            finish();
        });
    }
}
