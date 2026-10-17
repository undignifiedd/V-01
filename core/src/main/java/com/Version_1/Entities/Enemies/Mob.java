package com.Version_1.Entities.Enemies;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

import com.Version_1.Anim.AnimationStateMachine;
import com.Version_1.Entities.Player;
import com.Version_1.World.World;

/** Chases the player along x, then melees. Parameterized so one class covers every mob type. */
public class Mob extends Enemy {

    public enum State { IDLE, WALK, ATTACK }

    private final AnimationStateMachine<State> anim;
    private final float speed;
    private final int damage;
    private final float attackRange;     // horizontal reach, also the distance at which it stops chasing
    private final float attackCooldown;  // seconds between attacks
    private final float hitTime;         // seconds into the attack anim when the hit lands

    private float cooldownTimer = 0f;
    private boolean hitApplied = false;

    public Mob(float x, float y, float width, float height,
               int maxHealth, float speed, int damage,
               float attackRange, float attackCooldown, float hitTime,
               AnimationStateMachine<State> anim,
               Player player, World world) {
        super(x, y, width, height, maxHealth, player, world);
        this.speed = speed;
        this.damage = damage;
        this.attackRange = attackRange;
        this.attackCooldown = attackCooldown;
        this.hitTime = hitTime;
        this.anim = anim;
    }

    @Override
    public void update(float delta) {
        if (isDead()) return;

        anim.update(delta);
        cooldownTimer = Math.max(0f, cooldownTimer - delta);

        // Mid-attack: wait for the hit frame, then for the animation to finish.
        if (anim.getCurrentState() == State.ATTACK) {
            if (!hitApplied && anim.getStateTime() >= hitTime) {
                hitApplied = true;
                tryHit();
            }
            if (anim.isAnimationFinished()) {
                anim.setState(State.IDLE);
                cooldownTimer = attackCooldown;
            }
            return;
        }

        if (player.isDead()) {
            anim.setState(State.IDLE);
            return;
        }

        facePlayer();

        if (distanceToPlayer() > attackRange) {
            position.x += directionToPlayer() * speed * delta;
            anim.setState(State.WALK);
        } else if (cooldownTimer <= 0f) {
            hitApplied = false;
            anim.setState(State.ATTACK);
            anim.restart();
        } else {
            anim.setState(State.IDLE);
        }
    }

    /** Hitbox sits in front of the mob, attackRange wide. */
    private void tryHit() {
        Rectangle b = getBounds();
        float hx = facingRight ? b.x + b.width : b.x - attackRange;
        Rectangle hitbox = new Rectangle(hx, b.y, attackRange, b.height);
        if (hitbox.overlaps(player.getBounds())) {
            player.takeDamage(damage);
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        TextureRegion frame = anim.getCurrentFrame();
        // Art is assumed to face RIGHT; flip when facing left.
        if (facingRight) {
            batch.draw(frame, position.x, position.y, width, height);
        } else {
            batch.draw(frame, position.x + width, position.y, -width, height);
        }
    }
}
