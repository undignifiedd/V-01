package com.Version_1.Entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Player extends Entity implements Attackable{
    protected Player(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    @Override
    public void takeDamage() {

    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void draw(SpriteBatch batch) {

    }
}
