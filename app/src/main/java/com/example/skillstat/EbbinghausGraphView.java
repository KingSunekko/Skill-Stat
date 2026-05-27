package com.example.skillstat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class EbbinghausGraphView extends View {
    private Paint curvePaint, fillPaint, dotPaint, textPaint;
    private Path path, fillPath;
    
    private float currentMastery = 80f;
    private int daysLeft = 7;

    public EbbinghausGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setStrokeWidth(12f);
        curvePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(34f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        path = new Path();
        fillPath = new Path();
    }

    public void setSkillData(int mastery, int daysLeft) {
        this.currentMastery = (float) mastery;
        this.daysLeft = daysLeft;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) return;

        float paddingL = 100f;
        float paddingR = 100f;
        float paddingT = 120f;
        float paddingB = 80f;

        float startX = paddingL;
        // Start height based on current mastery (higher mastery = higher start)
        float startY = paddingT + (1f - (currentMastery / 100f)) * (h - paddingT - paddingB);
        
        float endX = w - paddingR;
        float endY = h - paddingB;

        // Control point adjusted by "health" of the skill
        // If daysLeft is high, the curve stays higher longer
        float healthFactor = Math.min(daysLeft / 7f, 1.0f);
        float ctrlX = paddingL + (w - paddingL - paddingR) * (0.2f + 0.5f * healthFactor);
        float ctrlY = startY + (endY - startY) * 0.1f;

        // 1. Draw Background Glow
        fillPath.reset();
        fillPath.moveTo(startX, startY);
        fillPath.quadTo(ctrlX, ctrlY, endX, endY);
        fillPath.lineTo(endX, h);
        fillPath.lineTo(startX, h);
        fillPath.close();
        
        int fillStartColor = Color.argb(50, 88, 204, 2); 
        fillPaint.setShader(new LinearGradient(0, startY, 0, h, 
                fillStartColor, Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);

        // 2. Draw Curve
        path.reset();
        path.moveTo(startX, startY);
        path.quadTo(ctrlX, ctrlY, endX, endY);
        
        curvePaint.setShader(new LinearGradient(startX, 0, endX, 0,
                new int[]{Color.parseColor("#58CC02"), Color.parseColor("#FF9600"), Color.parseColor("#FF4B4B")},
                null, Shader.TileMode.CLAMP));
        canvas.drawPath(path, curvePaint);

        // 3. Draw Key Points
        // "Now" point
        drawPoint(canvas, startX, startY, "#58CC02", "Now (" + (int)currentMastery + "%)");
        
        // "Critical" point (where it hits 50% or "At Risk")
        float midX = getBezierX(0.5f, startX, ctrlX, endX);
        float midY = getBezierY(0.5f, startY, ctrlY, endY);
        String midLabel = daysLeft <= 1 ? "Critical" : "Day " + (daysLeft / 2);
        drawPoint(canvas, midX, midY, "#FF9600", midLabel);

        // "Forget" point
        drawPoint(canvas, endX, endY, "#FF4B4B", "Day " + daysLeft);
    }

    private float getBezierX(float t, float p0, float p1, float p2) {
        return (1 - t) * (1 - t) * p0 + 2 * (1 - t) * t * p1 + t * t * p2;
    }

    private float getBezierY(float t, float p0, float p1, float p2) {
        return (1 - t) * (1 - t) * p0 + 2 * (1 - t) * t * p1 + t * t * p2;
    }

    private void drawPoint(Canvas canvas, float x, float y, String color, String label) {
        int c = Color.parseColor(color);
        dotPaint.setColor(c);
        dotPaint.setAlpha(80);
        canvas.drawCircle(x, y, 26f, dotPaint); // Glow
        
        dotPaint.setAlpha(255);
        canvas.drawCircle(x, y, 16f, dotPaint); // Main Dot
        
        dotPaint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, 7f, dotPaint); // Highlight
        
        textPaint.setColor(c);
        canvas.drawText(label, x, y - 50f, textPaint);
    }
}
