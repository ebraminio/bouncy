package io.github.ebraminio.bouncy;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new Bouncy(this));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus) return;

        final var window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setStatusBarColor(Color.TRANSPARENT);

            var color = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES ? Color.BLACK : Color.WHITE;
            window.setBackgroundDrawable(new ColorDrawable(color));

            var controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }
}

class Bouncy extends View {
    private final FloatValueHolder x = new FloatValueHolder();
    private final FlingAnimation horizontalFling = new FlingAnimation(x);
    private final FloatValueHolder y = new FloatValueHolder();
    private final FlingAnimation verticalFling = new FlingAnimation(y);
    private final GestureDetector flingDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
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
        setFocusable(true);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP ->
                    verticalFling.setStartVelocity(-4500).start();
            case KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN ->
                    verticalFling.setStartVelocity(4500).start();
            case KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT ->
                    horizontalFling.setStartVelocity(-4500).start();
            case KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT ->
                    horizontalFling.setStartVelocity(4500).start();
        }
        return true;
    }

    private boolean initialized = false;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (initialized) return;
        x.setValue(w / 2f);
        y.setValue(h / 2f);
        path.moveTo(x.getValue(), y.getValue());
        initialized = true;
        r = Math.min(w, h) / 20f;
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
        if (isWallHit) {
            playSound();
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        flingDetector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                horizontalFling.cancel();
                verticalFling.cancel();
                previousX = event.getX();
                previousY = event.getY();
            }

            case MotionEvent.ACTION_MOVE -> {
                x.setValue(x.getValue() + event.getX() - previousX);
                y.setValue(y.getValue() + event.getY() - previousY);
                previousX = event.getX();
                previousY = event.getY();
                invalidate();
            }
        }
        return true;
    }

    private String jingleBells = "EEEEEEEGCDEFFFFFEEEEEDDEDGEEEEEEEGCDEFFFFFEEEEGGFDC";
    private int[] majorScale = {0, 2, 4, 5, 7};
    private int[] minorScale = {0, 2, 3, 5, 7};
    private AtomicInteger counter = new AtomicInteger(0);

    private void playSound() {
        new Thread(() -> {
            int index = counter.getAndAdd(1) % jingleBells.length();
            int note = majorScale[jingleBells.charAt(index) - 'C'];
            var sampleRate = 44100;
            var buffer =
                    guitarString(sampleRate, getStandardFrequency(MIDDLE_A_SEMITONE + note), 4);
            var audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, buffer.length, AudioTrack.MODE_STATIC
            );
            audioTrack.write(buffer, 0, buffer.length);
            try {
                audioTrack.play();
            } catch (Exception e) {
                Log.e("Bouncy", e.getMessage(), e);
            }
        }).start();
    }

    private final double MIDDLE_A_SEMITONE = 69;

    private double getStandardFrequency(double note) {
        double MIDDLE_A_FREQUENCY = 440;
        return MIDDLE_A_FREQUENCY * Math.pow(2.0, (note - MIDDLE_A_SEMITONE) / 12);
    }

    // Based on https://habr.com/ru/post/514844/ and https://timiskhakov.github.io/posts/programming-guitar-music
    private short[] guitarString(final int sampleRate, final double frequency,
                                 final double duration/*1.0*/) {
        final var p = .9;
        final var beta = .1;
        final var s = .1;
        final var c = .1;
        final var l = .1;
        final var n = (int) (sampleRate / frequency);

        // Pick-direction lowpass filter
        final var rand = new Random();
        final double[] random = new double[n];
        double lastOut = (1 - p) * rand.nextDouble() * 2 - 1;
        random[0] = lastOut;
        for (var i = 1; i < n; ++i) {
            lastOut = (1 - p) * (rand.nextDouble() * 2 - 1) + p * lastOut;
            random[i] = lastOut;
        }

        // Pick-position comb filter
        var pick = (int) (beta * n + .5);
        if (pick == 0) pick = n;
        var noise = new double[random.length];
        for (var i = 0; i < noise.length; ++i) {
            noise[i] = random[i] - (i < pick ? .0 : random[i - pick]);
        }

        var samples = new double[(int) (sampleRate * duration)];
        System.arraycopy(noise, 0, samples, 0, n);

        // First-order string-tuning allpass filter
        for (var i = n; i < samples.length; ++i) {
            // delay line
            var delayLine = samples[i - n];
            var delayLineM1 = (i - 1 - n) > 0 ? samples[i - 1 - n] : 0;
            var delayLineM2 = (i - 2 - n) > 0 ? samples[i - 2 - n] : 0;
            // String-dampling filter.
            var stringDamplingFilter = .996 * ((1 - s) * delayLine + s * delayLineM1);
            var stringDamplingFilterM1 = .996 * ((1 - s) * delayLineM1 + s * delayLineM2);

            samples[i] = c * (stringDamplingFilter - samples[i - 1]) + stringDamplingFilterM1;
        }

        // Dynamic-level lowpass filter. L ∈ (0, 1/3)
        var wTilde = Math.PI * frequency / sampleRate;
        var buffer = new double[samples.length];
        buffer[0] = wTilde / (1 + wTilde) * samples[0];
        for (int i = 1; i < samples.length; ++i) {
            buffer[i] = wTilde / (1 + wTilde) * (samples[i] + samples[i - 1]) + (1 - wTilde) / (1 + wTilde) * buffer[i - 1];
        }
        for (int i = 0; i < samples.length; ++i) {
            samples[i] = ((Math.pow(l, 4 / 3.0)) * samples[i]) + (1 - l) * buffer[i];
        }

        var max = .0;
        for (double sample : samples) max = Math.max(max, Math.abs(sample));

        var result = new short[samples.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = (short) (samples[i] / max * Short.MAX_VALUE);
        }
        return result;
    }
}
