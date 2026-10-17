package com.Version_1.Entities.Enemies;

import com.Version_1.Entities.Attackable;
import com.Version_1.Entities.Entity;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Boss extends Entity implements Attackable {
    protected Boss(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void draw(SpriteBatch batch) {

    }

    @Override
    public void takeDamage(int amount) {

    }

    @Override
    public boolean isDead() {
        return false;
    }
}
