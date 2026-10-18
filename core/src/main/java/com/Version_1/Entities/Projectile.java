package com.Version_1.Entities;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Rectangle;
import com.Version_1.Entities.Enemies.Enemy;

/** Moves and expires; the phase handles its collision and damage. */
public class Projectile extends Entity {
    private final Attackable owner;
    private final Animation<TextureRegion> animation;
    private final float velocityX;
    private final int damage;
    private float lifetime;
    private float stateTime;
    private final boolean piercing;
    private final Array<Attackable> hitTargets = new Array<>();
    private float explosionTime = -1f;
    private boolean explosionResolved;
    private boolean launchSoundPending = true;
    private final Rectangle blastBounds = new Rectangle();

    public Projectile(float x, float y, float width, float height, float velocityX,
                      int damage, float lifetime, Animation<TextureRegion> animation, Attackable owner) {
        this(x, y, width, height, velocityX, damage, lifetime, animation, owner, false);
    }

    public Projectile(float x, float y, float width, float height, float velocityX,
                      int damage, float lifetime, Animation<TextureRegion> animation, Attackable owner,
                      boolean piercing) {
        super(x, y, width, height);
        this.velocityX = velocityX;
        this.damage = damage;
        this.lifetime = lifetime;
        this.animation = animation;
        this.owner = owner;
        this.piercing = piercing;
    }

    @Override
    public void update(float delta) {
        if (isRemoved()) return;
        if (isExploding()) {
            explosionTime += delta;
            if (explosionTime >= 0.55f) remove();
            return;
        }
        position.x += velocityX * delta;
        stateTime += delta;
        lifetime -= delta;
        if (lifetime <= 0f) remove();
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (isRemoved() || isExploding()) return;
        if (piercing) {
            float pulse = 1f + 0.04f * (float) Math.sin(stateTime * 18f);
            float drawW = width * pulse, drawH = height * pulse;
            float x = position.x + (width - drawW) / 2f;
            batch.draw(animation.getKeyFrame(stateTime), velocityX >= 0f ? x : x + drawW,
                position.y + (height - drawH) / 2f, velocityX >= 0f ? drawW : -drawW, drawH);
            return;
        }
        // Rotate the original vertical spell burst into a horizontal travelling wave.
        batch.draw(animation.getKeyFrame(stateTime), position.x + (width - height) / 2f,
            position.y + (height - width) / 2f, height / 2f, width / 2f,
            height, width, 1f, 1f, velocityX >= 0f ? -90f : 90f);
    }

    public int getDamage() { return damage; }
    public boolean consumeLaunchSound() {
        if (!launchSoundPending) return false;
        launchSoundPending = false;
        return true;
    }
    public Attackable getOwner() { return owner; }
    public boolean hasHit(Attackable target) { return hitTargets.contains(target, true); }
    public void markHit(Attackable target) { hitTargets.add(target); }
    public Array<Attackable> getHitTargets() { return hitTargets; }
    public float getDirection() { return Math.signum(velocityX); }
    public boolean isExploding() { return explosionTime >= 0f; }
    public float getExplosionTime() { return explosionTime; }
    public void explode() { if (!isExploding()) explosionTime = 0f; }
    public boolean consumeExplosion() {
        if (!isExploding() || explosionResolved) return false;
        explosionResolved = true;
        return true;
    }
    public Rectangle getBlastBounds() {
        return blastBounds.set(position.x + width / 2f - 100f, position.y - 20f, 200f, 180f);
    }

    @Override
    public void remove() {
        for (int i = 0; i < hitTargets.size; i++) {
            Attackable target = hitTargets.get(i);
            if (target instanceof Enemy) ((Enemy) target).releaseFromRage(this);
        }
        super.remove();
    }
}
