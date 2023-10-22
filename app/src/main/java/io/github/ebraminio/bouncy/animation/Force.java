package io.github.ebraminio.bouncy.animation;

interface Force {
    // Acceleration based on position.
    float getAcceleration(float position, float velocity);

    boolean isAtEquilibrium(float value, float velocity);
}
