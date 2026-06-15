package com.example.weekendlegends;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;

import java.io.InputStream;

public class ImageUtil {

    public static Bitmap loadBitmapFromUri(Context c, Uri uri) {
        try {
            ContentResolver cr = c.getContentResolver();
            InputStream in = cr.openInputStream(uri);
            if (in == null) return null;
            Bitmap bmp = android.graphics.BitmapFactory.decodeStream(in);
            in.close();
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    // ✅ circle crop with centerCrop, outputSize px
    public static Bitmap circleCropCenter(Bitmap src, int outputSize) {
        if (src == null) return null;

        Bitmap out = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BitmapShader shader = new BitmapShader(src, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);

        float scale;
        float dx = 0, dy = 0;

        int sw = src.getWidth();
        int sh = src.getHeight();

        if (sw * outputSize > outputSize * sh) {
            scale = (float) outputSize / (float) sh;
            dx = (outputSize - sw * scale) * 0.5f;
        } else {
            scale = (float) outputSize / (float) sw;
            dy = (outputSize - sh * scale) * 0.5f;
        }

        Matrix m = new Matrix();
        m.setScale(scale, scale);
        m.postTranslate(dx, dy);
        shader.setLocalMatrix(m);

        paint.setShader(shader);

        float r = outputSize / 2f;
        canvas.drawCircle(r, r, r, paint);

        return out;
    }
}
