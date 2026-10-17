package com.Version_1.Engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Reusable scaffolding: owns the SpriteBatch and runs update -> draw -> drawUI each frame.
 * Subclasses MUST call super.create() first in their own create().
 */
public abstract class GameEngineBase extends ApplicationAdapter {
    protected SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        update(delta);

        ScreenUtils.clear(0, 0, 0, 1);

        batch.begin();
        draw();
        batch.end();

        // Scene2D Stage.draw() goes here: it must run OUTSIDE the batch block.
        drawUI();
    }

    @Override
    public void dispose() {
        batch.dispose();
    }

    /** Game logic. Set the batch projection from the camera at the end of this. */
    protected abstract void update(float delta);

    /** Called between batch.begin()/end(). */
    protected abstract void draw();

    /** Called after batch.end(). Override for Stage.draw(). */
    protected void drawUI() {
    }
}

