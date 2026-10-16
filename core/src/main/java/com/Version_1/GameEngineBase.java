package com.Version_1;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public abstract class GameEngineBase extends ApplicationAdapter {
    protected SpriteBatch batch;

    private Texture background;

    @Override
    public void create() {
        background = new Texture("Background.jpg");
        batch = new SpriteBatch();
        Gdx.app.log("Lifecycle", "Create() called");
    }

    @Override
    public void render() {

        ScreenUtils.clear(0,0,0,1);

        float delta = Gdx.graphics.getDeltaTime();
        update(delta);

        batch.begin();
        batch.draw(background,0,0);
        batch.end();

        draw();
    }

    @Override
    public void dispose() {
        batch.dispose();
    }

    protected abstract void draw();

    protected abstract void update(float delta);

    public void resize(int width, int height) {
        if (batch==null){
            return;
        }
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }
}


