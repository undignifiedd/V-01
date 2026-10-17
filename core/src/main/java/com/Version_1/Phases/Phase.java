package com.Version_1.Phases;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** One chunk of the game (fight, explore, real world...). MrMonsterGame runs one at a time. */
public interface Phase {
    void update(float delta);

    /** Called between batch.begin()/end() by the game. */
    void draw(SpriteBatch batch);

    /** When true, the game swaps to the next phase. */
    boolean isFinished();

    void dispose();
}
