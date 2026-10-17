package com.Version_1.Entities;


import com.Version_1.Anim.AnimationStateMachine;
import com.Version_1.World.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import java.util.Map;

/**
 * The player. WASD = move (4-directional, top-down), J = attack.
 * Hit detection: the player checks its own attack hitbox against Attackables in the World
 * and calls takeDamage on them. Each swing hits a given target at most once.
 */
public class Player extends Entity implements Attackable {

    public enum State { IDLE, WALK, ATTACK }

    // --- Tuning (adjust to taste / to match the art) ---
    private static final float SPEED = 120f;            // world units per second
    private static final int MAX_HEALTH = 100;
    private static final float MAX_RAGE = 100f;
    private static final float INVULN_TIME = 0.6f;      // seconds of immunity after a hit
    private static final int ATTACK_DAMAGE = 10;
    private static final float ATTACK_RANGE = 28f;      // hitbox length in front of the player
    private static final float HIT_START = 0.10f;       // seconds into the attack anim when damage starts
    private static final float HIT_END = 0.25f;         // ...and when it stops

    private final World world;
    private final AnimationStateMachine<State> anim;
    private final Rectangle attackBox = new Rectangle();
    private final Array<Attackable> hitThisSwing = new Array<>();

    private int health = MAX_HEALTH;
    private float rage = 0f;                            // rules for filling/using it: TBD
    private float invulnTimer = 0f;
    private boolean facingRight = true;                 // art is drawn facing right
    private boolean attacking = false;

    public Player(float x, float y, float width, float height,
                  World world, Map<State, Animation<TextureRegion>> animations) {
        super(x, y, width, height);
        this.world = world;
        this.anim = new AnimationStateMachine<>(animations, State.IDLE);
    }

    @Override
    public void update(float delta) {
        if (invulnTimer > 0) {
            invulnTimer -= delta;
        }
        if (isDead()) {
            return;
        }

        anim.update(delta);

        if (attacking) {
            checkAttackHits();
            if (anim.isAnimationFinished()) {
                attacking = false;
                anim.setState(State.IDLE);
            }
            return; // no moving while attacking
        }

        handleMovement(delta);

        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            startAttack();
        }
    }

    private void handleMovement(float delta) {
        float dx = 0f;
        float dy = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1f;

        if (dx != 0f || dy != 0f) {
            float len = (float) Math.sqrt(dx * dx + dy * dy); // normalize so diagonals aren't faster
            position.x += (dx / len) * SPEED * delta;
            position.y += (dy / len) * SPEED * delta;
            if (dx != 0f) {
                facingRight = dx > 0f;
            }
            anim.setState(State.WALK);
        } else {
            anim.setState(State.IDLE);
        }
    }

    private void startAttack() {
        attacking = true;
        hitThisSwing.clear();
        anim.setState(State.ATTACK);
        anim.restart();
    }

    private void checkAttackHits() {
        float t = anim.getStateTime();
        if (t < HIT_START || t > HIT_END) {
            return;
        }

        float boxX = facingRight ? position.x + width : position.x - ATTACK_RANGE;
        attackBox.set(boxX, position.y, ATTACK_RANGE, height);

        Array<Entity> entities = world.getEntities();
        for (int i = 0; i < entities.size; i++) {
            Entity e = entities.get(i);
            if (e == this || !(e instanceof Attackable)) {
                continue;
            }
            Attackable target = (Attackable) e;
            if (target.isDead() || hitThisSwing.contains(target, true)) {
                continue;
            }
            if (attackBox.overlaps(target.getBounds())) {
                hitThisSwing.add(target);
                target.takeDamage(ATTACK_DAMAGE);
            }
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        // Blink while invulnerable.
        if (invulnTimer > 0 && ((int) (invulnTimer * 20) % 2 == 0)) {
            return;
        }
        TextureRegion frame = anim.getCurrentFrame();
        if (facingRight) {
            batch.draw(frame, position.x, position.y, width, height);
        } else {
            batch.draw(frame, position.x + width, position.y, -width, height); // flip
        }
    }

    @Override
    public void takeDamage(int amount) {
        if (isDead() || invulnTimer > 0) {
            return;
        }
        health = Math.max(0, health - amount);
        invulnTimer = INVULN_TIME;
    }


    @Override
    public boolean isDead() {
        return health <= 0;
    }

    public int getHealth() { return health; }
    public int getMaxHealth() { return MAX_HEALTH; }
    public float getRage() { return rage; }
    public float getMaxRage() { return MAX_RAGE; }

    public void addRage(float amount) {
        rage = Math.max(0f, Math.min(MAX_RAGE, rage + amount));
    }
}
