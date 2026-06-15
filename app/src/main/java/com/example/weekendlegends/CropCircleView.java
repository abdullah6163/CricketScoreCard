package com.example.weekendlegends;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class CropCircleView extends AppCompatImageView {

    private Bitmap bitmap;
    private final Matrix matrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();

    private float startX, startY;
    private int mode = 0; // 0 none, 1 drag, 2 zoom

    private ScaleGestureDetector scaleDetector;

    public CropCircleView(Context c) { super(c); init(); }
    public CropCircleView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    public CropCircleView(Context c, @Nullable AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
    }

    public void setImageUri(Uri uri) {
        bitmap = ImageUtil.loadBitmapFromUri(getContext(), uri);
        if (bitmap != null) {
            setImageBitmap(bitmap);
            centerFit();
        }
    }

    private void centerFit() {
        if (bitmap == null) return;

        float vw = getWidth();
        float vh = getHeight();
        if (vw <= 0 || vh <= 0) return;

        matrix.reset();

        float bw = bitmap.getWidth();
        float bh = bitmap.getHeight();

        float scale = Math.max(vw / bw, vh / bh);
        float dx = (vw - bw * scale) / 2f;
        float dy = (vh - bh * scale) / 2f;

        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);

        setImageMatrix(matrix);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerFit();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                startX = event.getX();
                startY = event.getY();
                mode = 1;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == 1) {
                    matrix.set(savedMatrix);
                    float dx = event.getX() - startX;
                    float dy = event.getY() - startY;
                    matrix.postTranslate(dx, dy);
                    setImageMatrix(matrix);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mode = 0;
                break;
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = detector.getScaleFactor();
            matrix.postScale(scale, scale, detector.getFocusX(), detector.getFocusY());
            setImageMatrix(matrix);
            mode = 2;
            return true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // ✅ dark overlay outside circle
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) * 0.35f;

        Paint overlay = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlay.setColor(0xAA000000);

        Path path = new Path();
        path.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);

        Path circle = new Path();
        circle.addCircle(cx, cy, radius, Path.Direction.CW);

        path.op(circle, Path.Op.DIFFERENCE);
        canvas.drawPath(path, overlay);

        // ✅ circle border
        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(5f);
        border.setColor(0xFF00E5FF);
        canvas.drawCircle(cx, cy, radius, border);
    }

    public Bitmap exportCircleBitmap(int outSize) {
        if (bitmap == null) return null;

        Bitmap viewBmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(viewBmp);
        draw(c);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) * 0.35f;

        int left = (int) (cx - radius);
        int top = (int) (cy - radius);
        int size = (int) (radius * 2);

        Bitmap cropped = Bitmap.createBitmap(viewBmp, left, top, size, size);
        return ImageUtil.circleCropCenter(cropped, outSize);
    }
}
