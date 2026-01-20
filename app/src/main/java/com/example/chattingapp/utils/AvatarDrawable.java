package com.example.chattingapp.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AvatarDrawable extends Drawable {

    private final Paint paintCircle;
    private final Paint paintText;
    private final String text;
    private final Rect textBounds;

    public AvatarDrawable(String text) {
        this.text = text != null && !text.isEmpty() ? text.substring(0, 1).toUpperCase() : "?";
        this.textBounds = new Rect();

        paintCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCircle.setColor(Color.parseColor("#6200EE")); // Default Purple
        paintCircle.setStyle(Paint.Style.FILL);

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(Color.WHITE);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        float radius = Math.min(bounds.width(), bounds.height()) / 2f;
        float centerX = bounds.centerX();
        float centerY = bounds.centerY();

        // Draw Circle
        canvas.drawCircle(centerX, centerY, radius, paintCircle);

        // Draw Text
        paintText.setTextSize(radius); // Text size relative to radius
        paintText.getTextBounds(text, 0, text.length(), textBounds);
        
        // Vertically center text
        float textY = centerY - textBounds.exactCenterY();
        canvas.drawText(text, centerX, textY, paintText);
    }

    @Override
    public void setAlpha(int alpha) {
        paintCircle.setAlpha(alpha);
        paintText.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paintCircle.setColorFilter(colorFilter);
        paintText.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
