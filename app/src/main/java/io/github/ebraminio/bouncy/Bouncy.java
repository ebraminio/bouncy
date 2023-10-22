package io.github.ebraminio.bouncy;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.RippleDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Random;

import io.github.ebraminio.bouncy.animation.FlingAnimation;
import io.github.ebraminio.bouncy.animation.FloatValueHolder;

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
    // private final Paint linesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float r = 0;
    private float previousX = 0;
    private float previousY = 0;
    private float storedVelocityX = 0;
    private float storedVelocityY = 0;
    // private final Path path = new Path();
    private final RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(Color.WHITE), null, null);

    Bouncy(final Context context) {
        super(context);
        setBackground(rippleDrawable);
        horizontalFling.addUpdateListener((a, v, velocity) -> {
            storedVelocityX = velocity;
            invalidate();
        });
        verticalFling.addUpdateListener((a, v, velocity) -> {
            storedVelocityY = velocity;
            invalidate();
        });
        paint.setColor(Color.GRAY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            paint.setColor(context.getColor(android.R.color.system_accent1_500));
        }
        // linesPaint.setColor(Color.GRAY);
        // linesPaint.setStyle(Paint.Style.STROKE);
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
        // path.moveTo(x.getValue(), y.getValue());
        initialized = true;
        r = Math.min(w, h) / 20f;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        // path.lineTo(x.getValue(), y.getValue());
        // canvas.drawPath(path, linesPaint);
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
        if (isWallHit) onWallHit();
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

    private int[] diatonicScale = {0, 2, 4, 5, 7, 9, 11, 12, 11, 9, 7, 5, 4, 2};
    private int counter = 0;
    private Random random = new Random();

    private void onWallHit() {
        rippleDrawable.setColor(ColorStateList.valueOf(Color.argb(0x10, random.nextInt(256), random.nextInt(256), random.nextInt(256))));
        setPressed(false);
        rippleDrawable.setHotspot(x.getValue(), y.getValue());
        setPressed(true);
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        final var index = ++counter % diatonicScale.length;
        new Thread(() -> {
            final var note = diatonicScale[index];
            final var sampleRate = 44100;
            final var buffer =
                    guitarString(sampleRate, getStandardFrequency(MIDDLE_A_SEMITONE + note), 4);
            final var audioTrack = new AudioTrack(
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
        final var MIDDLE_A_FREQUENCY = 440;
        return MIDDLE_A_FREQUENCY * Math.pow(2.0, (note - MIDDLE_A_SEMITONE) / 12);
    }

    // Based on https://habr.com/ru/post/514844/ and https://timiskhakov.github.io/posts/programming-guitar-music
    private short[] guitarString(int sampleRate, double frequency, double duration/*1.0*/) {
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
            final var delayLine = samples[i - n];
            final var delayLineM1 = (i - 1 - n) > 0 ? samples[i - 1 - n] : 0;
            final var delayLineM2 = (i - 2 - n) > 0 ? samples[i - 2 - n] : 0;
            // String-dampling filter.
            final var stringDamplingFilter = .996 * ((1 - s) * delayLine + s * delayLineM1);
            final var stringDamplingFilterM1 = .996 * ((1 - s) * delayLineM1 + s * delayLineM2);

            samples[i] = c * (stringDamplingFilter - samples[i - 1]) + stringDamplingFilterM1;
        }

        // Dynamic-level lowpass filter. L ∈ (0, 1/3)
        final var wTilde = Math.PI * frequency / sampleRate;
        final var buffer = new double[samples.length];
        buffer[0] = wTilde / (1 + wTilde) * samples[0];
        for (int i = 1; i < samples.length; ++i) {
            buffer[i] = wTilde / (1 + wTilde) * (samples[i] + samples[i - 1]) + (1 - wTilde) / (1 + wTilde) * buffer[i - 1];
        }
        for (int i = 0; i < samples.length; ++i) {
            samples[i] = ((Math.pow(l, 4 / 3.0)) * samples[i]) + (1 - l) * buffer[i];
        }

        var max = .0;
        for (final double sample : samples) max = Math.max(max, Math.abs(sample));

        final var result = new short[samples.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = (short) (samples[i] / max * Short.MAX_VALUE);
        }
        return result;
    }
}