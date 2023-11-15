package io.github.ebraminio.bouncy.animation;
/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.os.Build;
import android.os.SystemClock;
import android.view.Choreographer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This custom, static handler handles the timing pulse that is shared by all active
 * ValueAnimators. This approach ensures that the setting of animation values will happen on the
 * same thread that animations start on, and that all animations will share the same times for
 * calculating their values, which makes synchronizing animations possible.
 * <p>
 * The handler uses the Choreographer by default for doing periodic callbacks. A custom
 * AnimationFrameCallbackProvider can be set on the handler to provide timing pulse that
 * may be independent of UI frame update. This could be useful in testing.
 */
class AnimationHandler {
    /**
     * Callbacks that receives notifications for animation timing
     */
    interface AnimationFrameCallback {
        /**
         * Run animation based on the frame time.
         *
         * @param frameTime The frame start time
         */
        boolean doAnimationFrame(long frameTime);
    }

    /**
     * This class is responsible for interacting with the available frame provider by either
     * registering frame callback or posting runnable, and receiving a callback for when a
     * new frame has arrived. This dispatcher class then notifies all the on-going animations of
     * the new frame, so that they can update animation values as needed.
     */
    class AnimationCallbackDispatcher {
        void dispatchAnimationFrame() {
            mCurrentFrameTime = SystemClock.uptimeMillis();
            doAnimationFrame(mCurrentFrameTime);
            if (mAnimationCallbacks.size() > 0) {
                getProvider().postFrameCallback();
            }
        }
    }

    public static final ThreadLocal<AnimationHandler> sAnimatorHandler = new ThreadLocal<>();

    /**
     * Internal per-thread collections used to avoid set collisions as animations start and end
     * while being processed.
     */
    private final HashMap<AnimationFrameCallback, Long> mDelayedCallbackStartTime =
            new HashMap<>();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ArrayList<AnimationFrameCallback> mAnimationCallbacks = new ArrayList<>();
    private final AnimationCallbackDispatcher mCallbackDispatcher =
            new AnimationCallbackDispatcher();

    private AnimationFrameCallbackProvider mProvider;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
            long mCurrentFrameTime = 0;
    private boolean mListDirty = false;

    public static AnimationHandler getInstance() {
        if (sAnimatorHandler.get() == null) {
            sAnimatorHandler.set(new AnimationHandler());
        }
        return sAnimatorHandler.get();
    }

    public static long getFrameTime() {
        if (sAnimatorHandler.get() == null) {
            return 0;
        }
        return sAnimatorHandler.get().mCurrentFrameTime;
    }

    /**
     * By default, the Choreographer is used to provide timing for frame callbacks. A custom
     * provider can be used here to provide different timing pulse.
     */
    public void setProvider(AnimationFrameCallbackProvider provider) {
        mProvider = provider;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    AnimationFrameCallbackProvider getProvider() {
        if (mProvider == null) {
            mProvider = new AnimationFrameCallbackProvider(mCallbackDispatcher);
        }
        return mProvider;
    }

    /**
     * Register to get a callback on the next frame after the delay.
     */
    public void addAnimationFrameCallback(final AnimationFrameCallback callback, long delay) {
        if (mAnimationCallbacks.size() == 0) {
            getProvider().postFrameCallback();
        }
        if (!mAnimationCallbacks.contains(callback)) {
            mAnimationCallbacks.add(callback);
        }

        if (delay > 0) {
            mDelayedCallbackStartTime.put(callback, (SystemClock.uptimeMillis() + delay));
        }
    }

    /**
     * Removes the given callback from the list, so it will no longer be called for frame related
     * timing.
     */
    public void removeCallback(AnimationFrameCallback callback) {
        mDelayedCallbackStartTime.remove(callback);
        int id = mAnimationCallbacks.indexOf(callback);
        if (id >= 0) {
            mAnimationCallbacks.set(id, null);
            mListDirty = true;
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void doAnimationFrame(long frameTime) {
        long currentTime = SystemClock.uptimeMillis();
        for (int i = 0; i < mAnimationCallbacks.size(); i++) {
            final AnimationFrameCallback callback = mAnimationCallbacks.get(i);
            if (callback == null) {
                continue;
            }
            if (isCallbackDue(callback, currentTime)) {
                callback.doAnimationFrame(frameTime);
            }
        }
        cleanUpList();
    }

    /**
     * Remove the callbacks from mDelayedCallbackStartTime once they have passed the initial delay
     * so that they can start getting frame callbacks.
     *
     * @return true if they have passed the initial delay or have no delay, false otherwise.
     */
    private boolean isCallbackDue(AnimationFrameCallback callback, long currentTime) {
        Long startTime = mDelayedCallbackStartTime.get(callback);
        if (startTime == null) {
            return true;
        }
        if (startTime < currentTime) {
            mDelayedCallbackStartTime.remove(callback);
            return true;
        }
        return false;
    }

    private void cleanUpList() {
        if (mListDirty) {
            for (int i = mAnimationCallbacks.size() - 1; i >= 0; i--) {
                if (mAnimationCallbacks.get(i) == null) {
                    mAnimationCallbacks.remove(i);
                }
            }
            mListDirty = false;
        }
    }

    /**
     * Default provider of timing pulse that uses Choreographer for frame callbacks.
     * <p>
     * The intention for having this interface is to increase the testability of ValueAnimator.
     * Specifically, we can have a custom implementation of the interface below and provide
     * timing pulse without using Choreographer. That way we could use any arbitrary interval for
     * our timing pulse in the tests.
     */
    private static class AnimationFrameCallbackProvider {

        private final Choreographer mChoreographer =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ? Choreographer.getInstance() : null;
        private final Choreographer.FrameCallback mChoreographerCallback;

        AnimationFrameCallbackProvider(AnimationCallbackDispatcher dispatcher) {
            mChoreographerCallback = frameTimeNanos -> dispatcher.dispatchAnimationFrame();
        }

        void postFrameCallback() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mChoreographer.postFrameCallback(mChoreographerCallback);
            }
        }
    }
}
