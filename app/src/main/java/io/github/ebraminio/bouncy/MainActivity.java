package io.github.ebraminio.bouncy;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final var window = getWindow();
        window.setDecorFitsSystemWindows(false);
        window.setNavigationBarContrastEnforced(false);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER |
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setContentView(new Bouncy(this));
    }
}

class Bouncy extends View {
    private final FloatValueHolder x = new FloatValueHolder();
    private final FlingAnimation horizontalFling = new FlingAnimation(x);
    private final FloatValueHolder y = new FloatValueHolder();
    private final FlingAnimation verticalFling = new FlingAnimation(y);
    private final GestureDetector flingDetector = new GestureDetector(
            getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            horizontalFling.setStartVelocity(velocityX).start();
            verticalFling.setStartVelocity(velocityY).start();
            return true;
        }
    });
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float r = 0;
    private float previousX = 0;
    private float previousY = 0;
    private float storedVelocityX = 0;
    private float storedVelocityY = 0;
    private final Path path = new Path();

    Bouncy(Context context) {
        super(context);
        horizontalFling.addUpdateListener((a, v, velocity) -> {
            storedVelocityX = velocity;
            invalidate();
        });
        verticalFling.addUpdateListener((a, v, velocity) -> {
            storedVelocityY = velocity;
            invalidate();
        });
        paint.setColor(Color.GRAY);
        linesPaint.setColor(Color.GRAY);
        linesPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        x.setValue(w / 2f);
        y.setValue(h / 2f);
        r = w / 20f;
        path.rewind();
        path.moveTo(x.getValue(), y.getValue());
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        path.lineTo(x.getValue(), y.getValue());
        canvas.drawPath(path, linesPaint);
        canvas.drawCircle(x.getValue(), y.getValue(), r, paint);
        var isWallHit = false;
        if (x.getValue() < r) {
            x.setValue(r);
            horizontalFling.cancel();
            horizontalFling.setStartVelocity(-storedVelocityX).start();
            isWallHit = true;
        }
        if (x.getValue() > getWidth() - r) {
            x.setValue(getWidth() - r);
            horizontalFling.cancel();
            horizontalFling.setStartVelocity(-storedVelocityX).start();
            isWallHit = true;
        }
        if (y.getValue() < r) {
            y.setValue(r);
            verticalFling.cancel();
            verticalFling.setStartVelocity(-storedVelocityY).start();
            isWallHit = true;
        }
        if (y.getValue() > getHeight() - r) {
            y.setValue(getHeight() - r);
            verticalFling.cancel();
            verticalFling.setStartVelocity(-storedVelocityY).start();
            isWallHit = true;
        }
        if (isWallHit) Log.d("", "Wall Hit");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        flingDetector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                horizontalFling.cancel();
                verticalFling.cancel();
                previousX = event.getX();
                previousY = event.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                x.setValue(x.getValue() + event.getX() - previousX);
                y.setValue(y.getValue() + event.getY() - previousY);
                previousX = event.getX();
                previousY = event.getY();
                invalidate();
                break;
        }
        return true;
    }
}
