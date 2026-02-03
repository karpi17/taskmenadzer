package com.example.taskmenadzer.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StarfieldView extends View {

    private static final int STAR_COUNT = 150;
    private final List<Star> stars = new ArrayList<>();
    private final Paint paint = new Paint();
    private final Random random = new Random();
    private float speed = 10f;

    public StarfieldView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        stars.clear();
        for (int i = 0; i < STAR_COUNT; i++) {
            stars.add(new Star(w, h));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xFF0B0E27); // Deep Midnight Blue Background

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        for (Star star : stars) {
            star.update(speed, width, height);
            
            float x = (star.x - centerX) * (width / star.z) + centerX;
            float y = (star.y - centerY) * (width / star.z) + centerY;
            float size = (1 - star.z / width) * 5;

            paint.setAlpha((int) ((1 - star.z / width) * 255));
            canvas.drawCircle(x, y, size, paint);
        }

        invalidate(); // Trigger next frame
    }

    private class Star {
        float x, y, z;

        Star(int width, int height) {
            reset(width, height);
        }

        void reset(int width, int height) {
            x = random.nextInt(width * 2) - width; // Spread wider
            y = random.nextInt(height * 2) - height;
            z = random.nextFloat() * width; // Depth
        }

        void update(float speed, int width, int height) {
            z -= speed;
            if (z <= 0) {
                reset(width, height);
                z = width;
            }
        }
    }
}
