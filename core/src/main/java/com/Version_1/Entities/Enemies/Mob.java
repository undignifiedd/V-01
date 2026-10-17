package com.Version_1.Entities.Enemies;

import com.Version_1.Entities.Attackable;
import com.Version_1.Entities.Entity;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Mob extends Entity implements Attackable {
    protected Mob(float x, float y, float width, float height) {
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
