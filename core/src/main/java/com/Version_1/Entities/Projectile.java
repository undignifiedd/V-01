package com.Version_1.Entities;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Moves and expires; the phase handles its collision and damage. */
public class Projectile extends Entity {
    private final Attackable owner;
    private final Animation<TextureRegion> animation;
    private final float velocityX;
    private final int damage;
    private float lifetime;
    private float stateTime;

    public Projectile(float x, float y, float width, float height, float velocityX,
                      int damage, float lifetime, Animation<TextureRegion> animation, Attackable owner) {
        super(x, y, width, height);
        this.velocityX = velocityX;
        this.damage = damage;
        this.lifetime = lifetime;
        this.animation = animation;
        this.owner = owner;
    }

    @Override
    public void update(float delta) {
        if (isRemoved()) return;
        position.x += velocityX * delta;
        stateTime += delta;
        lifetime -= delta;
        if (lifetime <= 0f) remove();
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (isRemoved()) return;
        // Rotate the original vertical spell burst into a horizontal travelling wave.
        batch.draw(animation.getKeyFrame(stateTime), position.x + (width - height) / 2f,
            position.y + (height - width) / 2f, height / 2f, width / 2f,
            height, width, 1f, 1f, velocityX >= 0f ? -90f : 90f);
    }

    public int getDamage() { return damage; }
    public Attackable getOwner() { return owner; }
}
