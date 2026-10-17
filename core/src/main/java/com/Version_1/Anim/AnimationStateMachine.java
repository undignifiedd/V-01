package com.Version_1.Anim;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.Map;

/**
 * Enum-keyed animation state machine.
 * Looping vs one-shot is decided by each Animation's PlayMode when you build the map:
 * use PlayMode.LOOP for idle/walk, PlayMode.NORMAL for attacks (isAnimationFinished()
 * only ever returns true for non-looping animations).
 */
public class AnimationStateMachine<T> {
    private final Map<T, Animation<TextureRegion>> animations;
    private T currentState;
    private float stateTime;

    public AnimationStateMachine(Map<T, Animation<TextureRegion>> animations, T initialState) {
        if (!animations.containsKey(initialState)) {
            throw new IllegalArgumentException("No animation registered for initial state: " + initialState);
        }
        this.animations = animations;
        this.currentState = initialState;
        this.stateTime = 0;
    }

    /** Switches state; does nothing if already in that state (so loops aren't restarted every frame). */
    public void setState(T newState) {
        if (!animations.containsKey(newState)) {
            throw new IllegalArgumentException("No animation registered for state: " + newState);
        }
        if (!currentState.equals(newState)) {
            currentState = newState;
            stateTime = 0;
        }
    }

    /** Replays the current animation from frame 0 (e.g. attacking twice in a row). */
    public void restart() {
        stateTime = 0;
    }

    public void update(float delta) {
        stateTime += delta;
    }

    public TextureRegion getCurrentFrame() {
        return animations.get(currentState).getKeyFrame(stateTime);
    }

    public float getStateTime() {
        return stateTime;
    }

    public boolean isAnimationFinished() {
        return animations.get(currentState).isAnimationFinished(stateTime);
    }

    public T getCurrentState() {
        return currentState;
    }
}
