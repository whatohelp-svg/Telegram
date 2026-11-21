package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;

/**
 * Lightweight original RiseGram bokeh effect for the profile header.
 * Visual-only, transparent and independent of authentication/session state.
 */
public class RiseGramProfileGlowView extends View {
    private static final int[] COLORS = {
        0x8046C8FA, 0x8066F4BC, 0x8065B8FF, 0x808E7CFF,
        0x80FFD18D, 0x80FF95CF, 0x80A8F9EA, 0x8096ACFF
    };
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RadialGradient[] gradients = new RadialGradient[COLORS.length];
    private final float[] xx = {0.12f,0.72f,0.48f,0.83f,0.24f,0.91f,0.40f,0.64f};
    private final float[] yy = {0.19f,0.18f,0.55f,0.65f,0.78f,0.38f,0.31f,0.86f};
    private boolean running;
    private boolean reduceMotion;

    public RiseGramProfileGlowView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
        for (int i = 0; i < gradients.length; i++) {
            gradients[i] = new RadialGradient(0, 0, 1f,
                new int[]{COLORS[i], COLORS[i] & 0x00FFFFFF, 0x00000000},
                new float[]{0f,0.68f,1f}, Shader.TileMode.CLAMP);
        }
        try {
            reduceMotion = Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (Exception ignored) {
            reduceMotion = false;
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        invalidate();
    }

    @Override protected void onDetachedFromWindow() {
        running = false;
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float seconds = SystemClock.uptimeMillis() / 1000f;
        for (int i = 0; i < COLORS.length; i++) {
            float t = reduceMotion ? 0f : seconds * (0.32f + i * 0.043f) + i * 1.8f;
            float x = w * xx[i] + (float) Math.sin(t) * w * 0.065f;
            float y = h * yy[i] + (float) Math.cos(t * 0.83f) * h * 0.07f;
            float radius = Math.min(w, h) * (0.13f + (i % 3) * 0.036f);
            canvas.save();
            canvas.translate(x, y);
            canvas.scale(radius, radius);
            paint.setShader(gradients[i]);
            canvas.drawCircle(0, 0, 1, paint);
            canvas.restore();
        }
        paint.setShader(null);
        if (running && !reduceMotion && getWindowVisibility() == VISIBLE && isShown()) {
            postInvalidateDelayed(34);
        }
    }
}
