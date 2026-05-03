package com.example.skillstat.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class EmojiDrawableUtils {

    public static Drawable emojiToDrawable(Context context, String emoji, int sizeDp) {
        float scale = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (sizeDp * scale + 0.5f);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(sizePx * 0.8f);
        paint.setTextAlign(Paint.Align.CENTER);

        Rect bounds = new Rect();
        paint.getTextBounds(emoji, 0, emoji.length(), bounds);
        
        float x = sizePx / 2f;
        float y = (sizePx / 2f) - bounds.centerY();

        canvas.drawText(emoji, x, y, paint);

        return new BitmapDrawable(context.getResources(), bitmap);
    }
}