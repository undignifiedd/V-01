package com.Version_1.Entities.Enemies;

import com.badlogic.gdx.math.Rectangle;

import com.Version_1.Entities.Attackable;
import com.Version_1.Entities.Entity;
import com.Version_1.Entities.Player;
import com.Version_1.World.World;

/**
 * Base class for everything hostile (Boss, Mob).
 * Enemies only exist in 1D side-view fights, so the player helpers are x-only.
 * update(float) and draw(SpriteBatch) stay abstract (inherited from Entity).
 */
public abstract class Enemy extends Entity implements Attackable {

    protected final Player player;
    protected final World world;

    protected final int maxHealth;
    protected int health;
    protected boolean facingRight = false;

    public Enemy(float x, float y, float width, float height,
                 int maxHealth, Player player, World world) {
        super(x, y, width, height);
        if (maxHealth <= 0) throw new IllegalArgumentException("maxHealth must be positive");
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.player = player;
        this.world = world;
    }

    // ---- Attackable ----

    @Override
    public void takeDamage(int amount) {
        if (isDead() || amount <= 0) return;
        health -= amount;
        if (health <= 0) {
            health = 0;
            onDeath();
        }
    }

    @Override
    public boolean isDead() {
        return health <= 0;
    }

    @Override
    public Rectangle getBounds() {
        return super.getBounds();      // rebuilt from position/size by Entity
    }

    // ---- Hooks ----

    /** Called once when health reaches 0. Default: remove from the world. Override for death anims. */
    protected void onDeath() {
        remove();
    }

    // ---- Helpers (x-only) ----

    /** -1 if the player is to the left, +1 if to the right, 0 if aligned. */
    protected float directionToPlayer() {
        float dx = centerX(player.getBounds()) - centerX(getBounds());
        return Math.signum(dx);
    }

    /** Horizontal distance between centers, always >= 0. */
    protected float distanceToPlayer() {
        return Math.abs(centerX(player.getBounds()) - centerX(getBounds()));
    }

    /** Point the sprite toward the player. Call from update() when the enemy should turn. */
    protected void facePlayer() {
        float dir = directionToPlayer();
        if (dir != 0) facingRight = dir > 0;
    }

    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }

    private static float centerX(Rectangle r) {
        return r.x + r.width / 2f;
    }
}
