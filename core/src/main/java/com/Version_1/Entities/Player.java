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
 * The player. J = attack, W = single jump in fights.
 * setTopDown(true) switches from side-view fights to WASD exploration.
 * Hit detection: the player checks its own attack hitbox against Attackables in the World
 * and calls takeDamage on them. Each swing hits a given target at most once.
 */
public class Player extends Entity implements Attackable {

    public enum State { IDLE, WALK, ATTACK, TAKEOFF, RISE, FALL, LAND }

    // --- Tuning (adjust to taste / to match the art) ---
    private static final boolean ART_FACES_RIGHT = false; // idle/walk face left; attack poses face right
    private static final float ATTACK_WIDTH_SCALE = 1.4f;
    private static final float SPEED = 120f;            // world units per second
    private static final float JUMP_SPEED = 300f;
    private static final float GRAVITY = 900f;
    private static final float TAKEOFF_TIME = 0.08f;
    private static final float LAND_TIME = 0.08f;
    private static final float ATTACK_BUFFER_TIME = 0.2f;
    private static final float IDLE_TURN_TIME = 3f;
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
    private float idleTurnTimer = 0f;
    private boolean facingRight = true;                 // which way the player currently faces in the game
    private boolean attacking = false;
    private boolean topDown = false;                    // false = 1D side-view (fights), true = 2D (exploration)
    private float groundY;
    private float verticalVelocity;
    private float jumpTime;
    private float landingTimer;
    private float attackBufferTimer;

    public Player(float x, float y, float width, float height,
                  World world, Map<State, Animation<TextureRegion>> animations) {
        super(x, y, width, height);
        this.world = world;
        this.groundY = y;
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

        attackBufferTimer = Math.max(0f, attackBufferTimer - delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            attackBufferTimer = ATTACK_BUFFER_TIME;
        }

        anim.update(delta);
        updateJumpMotion(delta);

        if (attacking) {
            checkAttackHits();
            if (!anim.isAnimationFinished()) return; // no moving while attacking
            attacking = false;
            anim.setState(State.IDLE);
        }

        handleMovement(delta);

        if (!topDown && isGrounded() && landingTimer <= 0f
            && Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            verticalVelocity = JUMP_SPEED;
            jumpTime = 0f;
            idleTurnTimer = 0f;
        }
        updateJumpAnimation();

        // Exploration has no jumping, so attacks remain available there.
        if (attackBufferTimer > 0f && (topDown || isGrounded())) {
            startAttack();
        }
    }

    private void handleMovement(float delta) {
        float dx = 0f;
        float dy = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1f;
        if (topDown) { // vertical movement only outside fights
            if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1f;
        }

        if (dx != 0f || dy != 0f) {
            idleTurnTimer = 0f;
            float len = (float) Math.sqrt(dx * dx + dy * dy); // normalize so diagonals aren't faster
            position.x += (dx / len) * SPEED * delta;
            position.y += (dy / len) * SPEED * delta;
            if (dx != 0f) {
                facingRight = dx > 0f;
            }
            if (topDown || (isGrounded() && landingTimer <= 0f)) {
                anim.setState(State.WALK);
            }
        } else {
            if (!topDown && (!isGrounded() || landingTimer > 0f)) {
                return;
            }
            anim.setState(State.IDLE);
            idleTurnTimer += delta;
            while (idleTurnTimer >= IDLE_TURN_TIME) {
                facingRight = !facingRight;
                idleTurnTimer -= IDLE_TURN_TIME;
            }
        }
    }

    private void updateJumpMotion(float delta) {
        if (topDown) return;
        landingTimer = Math.max(0f, landingTimer - delta);
        if (isGrounded()) {
            position.y = groundY;
            return;
        }
        jumpTime += delta;
        position.y += verticalVelocity * delta - 0.5f * GRAVITY * delta * delta;
        verticalVelocity -= GRAVITY * delta;
        if (position.y <= groundY && verticalVelocity <= 0f) {
            position.y = groundY;
            verticalVelocity = 0f;
            landingTimer = LAND_TIME;
        }
    }

    private void updateJumpAnimation() {
        if (topDown || attacking) return;
        if (!isGrounded()) {
            anim.setState(jumpTime < TAKEOFF_TIME ? State.TAKEOFF
                : verticalVelocity > 0f ? State.RISE : State.FALL);
        } else if (landingTimer > 0f) {
            anim.setState(State.LAND);
        }
    }

    private void startAttack() {
        attackBufferTimer = 0f; // consume this press so it cannot fire again
        idleTurnTimer = 0f;
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
        boolean attackFrame = anim.getCurrentState() == State.ATTACK;
        boolean artFacesRight =
            attackFrame || ART_FACES_RIGHT;
        boolean flip = facingRight != artFacesRight; // flip only when facing opposite to how it's drawn
        float drawWidth = attackFrame ? width * ATTACK_WIDTH_SCALE : width;
        boolean jumpFrame = anim.getCurrentState() == State.TAKEOFF
            || anim.getCurrentState() == State.RISE || anim.getCurrentState() == State.FALL
            || anim.getCurrentState() == State.LAND;
        // Jump frames are 48x80; keep their proportions at the normal sprite's pixel scale.
        float drawHeight = height;
        float drawY = position.y;
        if (jumpFrame) {
            drawWidth = width * 48f / 64f;
            drawHeight = height * 80f / 64f;
            drawY -= height * 4f / 64f; // four transparent rows below the feet
        }
        float drawX = position.x + (width - drawWidth) / 2f;
        batch.draw(frame, flip ? drawX + drawWidth : drawX, drawY,
            flip ? -drawWidth : drawWidth, drawHeight);
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

    /** False for fights (A/D and W jump), true for exploration (WASD). */
    public void setTopDown(boolean topDown) {
        if (this.topDown == topDown) return;
        this.topDown = topDown;
        groundY = position.y;
        verticalVelocity = 0f;
        landingTimer = 0f;
        if (!attacking) anim.setState(State.IDLE);
    }

    public boolean isGrounded() {
        return !topDown && position.y <= groundY && verticalVelocity <= 0f;
    }

    /** Sets a fight's ground line and places the player on it. */
    public void setGroundY(float groundY) {
        this.groundY = groundY;
        position.y = groundY;
        verticalVelocity = 0f;
        landingTimer = 0f;
        if (!attacking) anim.setState(State.IDLE);
    }

    public int getHealth() { return health; }
    public int getMaxHealth() { return MAX_HEALTH; }
    public float getRage() { return rage; }
    public float getMaxRage() { return MAX_RAGE; }

    public void addRage(float amount) {
        rage = Math.max(0f, Math.min(MAX_RAGE, rage + amount));
    }
}
