package com.Version_1.Entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** Base for everything that lives in the World. */

public abstract class Entity {
    protected final Vector2 position = new Vector2();
    protected float width;
    protected float height;

    private final Rectangle bounds = new Rectangle();
    private boolean removed = false;

    protected Entity(float x, float y, float width, float height) {
        this.position.set(x, y);
        this.width = width;
        this.height = height;
    }

    public abstract void update(float delta);

    public abstract void draw(SpriteBatch batch);

    /** Bounds are rebuilt from position/size on every call, so they never go stale. */
    public Rectangle getBounds() {
        return bounds.set(position.x, position.y, width, height);
    }

    public Vector2 getPosition() {
        return position;
    }

    /** Mark for removal; World deletes it after the update pass. */
    public void remove() {
        removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }
}
