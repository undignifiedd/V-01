package com.Version_1.Entities;

import com.Version_1.Anim.AnimationStateMachine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.Map;

/**
 * The player. J = ground/air attack, W = jump / double jump, Space = dash.
 * setTopDown(true) switches from side-view fights to WASD exploration.
 * Exposes the active attack hitbox; the fight phase resolves collisions and damage.
 */
public class Player extends Entity implements Attackable {

    public enum State { IDLE, WALK, ATTACK, TAKEOFF, RISE, FALL, LAND }

    // --- Tuning (adjust to taste / to match the art) ---
    private static final boolean ART_FACES_RIGHT = false; // idle/walk face left; attack poses face right
    private static final float ATTACK_WIDTH_SCALE = 1.4f;
    private static final float SPEED = 120f;            // world units per second
    private static final float ATTACK_MOVE_SCALE = 0.5f;
    private static final float DASH_SPEED = 475f;
    private static final float DASH_TIME = 0.18f;
    private static final float DASH_COOLDOWN = 0.65f;
    private static final float KNOCKBACK_DRAG = 12f;
    private static final float JUMP_SPEED = 360f;
    private static final float GRAVITY = 800f;
    private static final int MAX_JUMPS = 2;
    private static final float JUMP_BUFFER_TIME = 0.15f;
    private static final float COYOTE_TIME = 0.1f;
    private static final float TAKEOFF_TIME = 0.08f;
    private static final float LAND_TIME = 0.08f;
    private static final float ATTACK_BUFFER_TIME = 0.2f;
    private static final float IDLE_TURN_TIME = 3f;
    private static final int MAX_HEALTH = 100;
    private static final float MAX_RAGE = 100f;
    private static final float INVULN_TIME = 0.6f;      // seconds of immunity after a hit
    private static final int ATTACK_DAMAGE = 10;
    private static final float ATTACK_RANGE = 45f;      // hitbox length in front of the player
    private static final float HIT_START = 0.10f;       // seconds into the attack anim when damage starts
    private static final float HIT_END = 0.25f;         // ...and when it stops

    private final AnimationStateMachine<State> anim;
    private final Rectangle attackBox = new Rectangle();
    private int attackId;
    private final Vector2 moveDirection = new Vector2();
    private final Vector2 dashDirection = new Vector2();

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
    private float jumpBufferTimer;
    private float coyoteTimer = COYOTE_TIME;
    private int jumpsUsed;
    private float dashTimer;
    private float dashCooldownTimer;
    private float knockbackVelocity;
    private float effectTime;
    private boolean burning;
    private boolean dashMovedThisFrame;

    public Player(float x, float y, float width, float height,
                  Map<State, Animation<TextureRegion>> animations) {
        super(x, y, width, height);
        this.groundY = y;
        this.anim = new AnimationStateMachine<>(animations, State.IDLE);
    }

    @Override
    public void update(float delta) {
        dashMovedThisFrame = false;
        if (invulnTimer > 0) {
            invulnTimer -= delta;
        }
        if (isDead()) {
            return;
        }
        effectTime += delta;
        dashCooldownTimer = Math.max(0f, dashCooldownTimer - delta);

        attackBufferTimer = Math.max(0f, attackBufferTimer - delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            attackBufferTimer = ATTACK_BUFFER_TIME;
        }
        jumpBufferTimer = Math.max(0f, jumpBufferTimer - delta);
        if (!topDown && Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            jumpBufferTimer = JUMP_BUFFER_TIME;
        }

        anim.update(delta);
        updateJumpMotion(delta);
        readMovementInput();

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && dashCooldownTimer <= 0f) {
            startDash();
        }
        if (isDashing()) {
            dashMovedThisFrame = true;
            float dashDelta = Math.min(delta, dashTimer);
            position.mulAdd(dashDirection, DASH_SPEED * dashDelta);
            dashTimer = Math.max(0f, dashTimer - delta);
            updateJumpAnimation();
            return;
        }
        position.x += knockbackVelocity * delta;
        knockbackVelocity *= (float) Math.exp(-KNOCKBACK_DRAG * delta);
        if (Math.abs(knockbackVelocity) < 1f) knockbackVelocity = 0f;

        if (attacking) {
            if (!anim.isAnimationFinished()) {
                handleMovement(delta);
                return; // jumping and the next swing wait for recovery
            }
            attacking = false;
            anim.setState(State.IDLE);
        }

        handleMovement(delta);

        if (!topDown && jumpBufferTimer > 0f && jumpsUsed < MAX_JUMPS) {
            // A grace-window jump counts as the ground jump, preserving one air jump.
            if (isGrounded() || coyoteTimer > 0f) jumpsUsed = 0;
            jumpsUsed++;
            verticalVelocity = JUMP_SPEED;
            jumpTime = 0f;
            landingTimer = 0f;
            idleTurnTimer = 0f;
            jumpBufferTimer = 0f;
            coyoteTimer = 0f;
            anim.setState(State.TAKEOFF);
            anim.restart();
        }
        updateJumpAnimation();

        if (attackBufferTimer > 0f) {
            startAttack();
        }
    }

    private void readMovementInput() {
        float dx = 0f;
        float dy = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1f;
        if (topDown) { // vertical movement only outside fights
            if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1f;
        }
        moveDirection.set(dx, dy).nor();
    }

    private void handleMovement(float delta) {
        float dx = moveDirection.x;
        float dy = moveDirection.y;
        if (dx != 0f || dy != 0f) {
            idleTurnTimer = 0f;
            float moveSpeed = SPEED * (attacking ? ATTACK_MOVE_SCALE : 1f);
            position.mulAdd(moveDirection, moveSpeed * delta);
            if (attacking) return; // keep the swing animation and facing direction
            if (dx != 0f) {
                facingRight = dx > 0f;
            }
            if (topDown || (isGrounded() && landingTimer <= 0f)) {
                anim.setState(State.WALK);
            }
        } else {
            if (attacking || (!topDown && (!isGrounded() || landingTimer > 0f))) {
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

    private void startDash() {
        dashDirection.set(moveDirection);
        if (dashDirection.isZero()) dashDirection.set(facingRight ? 1f : -1f, 0f);
        if (dashDirection.x != 0f) facingRight = dashDirection.x > 0f;
        attacking = false;
        attackBufferTimer = 0f;
        idleTurnTimer = 0f;
        knockbackVelocity = 0f;
        dashTimer = DASH_TIME;
        dashCooldownTimer = DASH_COOLDOWN;
        anim.setState(State.WALK);
        anim.restart();
    }

    public boolean isDashing() { return !isDead() && dashTimer > 0f; }
    public boolean didDashThisFrame() { return dashMovedThisFrame; }
    public float getDashDirectionX() { return dashDirection.x; }

    /** The phase can extend a dash just far enough to clear an overlapping enemy. */
    public void extendDashToX(float exitX) {
        if (!dashMovedThisFrame || isDead() || dashDirection.x == 0f) return;
        float remaining = (exitX - position.x) / (dashDirection.x * DASH_SPEED);
        if (remaining > 0f) dashTimer = Math.max(dashTimer, remaining + 0.01f);
    }
    public boolean isBurning() { return burning; }
    public void setBurning(boolean burning) { this.burning = burning; }
    public void knockBack(float velocity) { knockbackVelocity = velocity; }

    private void updateJumpMotion(float delta) {
        if (topDown) return;
        landingTimer = Math.max(0f, landingTimer - delta);
        if (isGrounded()) {
            position.y = groundY;
            jumpsUsed = 0;
            coyoteTimer = COYOTE_TIME;
            return;
        }
        coyoteTimer = Math.max(0f, coyoteTimer - delta);
        // Walking off a ledge spends the ground jump once the grace window ends.
        if (coyoteTimer <= 0f && jumpsUsed == 0) jumpsUsed = 1;
        jumpTime += delta;
        position.y += verticalVelocity * delta - 0.5f * GRAVITY * delta * delta;
        verticalVelocity -= GRAVITY * delta;
        if (position.y <= groundY && verticalVelocity <= 0f) {
            position.y = groundY;
            verticalVelocity = 0f;
            landingTimer = LAND_TIME;
            jumpsUsed = 0;
            coyoteTimer = COYOTE_TIME;
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
        attackId++;
        anim.setState(State.ATTACK);
        anim.restart();
    }

    public boolean isAttackActive() {
        float t = anim.getStateTime();
        return !isDead() && attacking && t >= HIT_START && t <= HIT_END;
    }

    public Rectangle getAttackBounds() {
        float boxX = facingRight ? position.x + width : position.x - ATTACK_RANGE;
        return attackBox.set(boxX, position.y, ATTACK_RANGE, height);
    }

    public int getAttackId() { return attackId; }
    public int getAttackDamage() { return ATTACK_DAMAGE; }

    @Override
    public void draw(SpriteBatch batch) {
        // Blink while invulnerable.
        if (!burning && !isDashing() && invulnTimer > 0 && ((int) (invulnTimer * 20) % 2 == 0)) {
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
        Color old = batch.getColor();
        float r = old.r, g = old.g, b = old.b, a = old.a;
        if (isDashing()) {
            for (int i = 2; i >= 1; i--) {
                batch.setColor(0.3f, 0.8f, 1f, 0.15f * (3 - i));
                drawFrame(batch, frame, drawX - dashDirection.x * 14f * i,
                    drawY - dashDirection.y * 14f * i, drawWidth, drawHeight, flip);
            }
        }
        if (burning) {
            float flicker = 0.5f + 0.5f * (float) Math.sin(effectTime * 24f);
            float glow = 0.4f + 0.2f * flicker;
            batch.setColor(1f, 0.45f + 0.2f * flicker, 0.02f, glow);
            drawFrame(batch, frame, drawX - drawWidth * 0.06f, drawY - drawHeight * 0.04f,
                drawWidth * 1.12f, drawHeight * 1.08f, flip);
            batch.setColor(1f, 0.55f + 0.15f * flicker, 0.06f, a);
        } else if (isDashing()) {
            batch.setColor(0.65f, 0.9f, 1f, a);
        } else {
            batch.setColor(r, g, b, a);
        }
        drawFrame(batch, frame, drawX, drawY, drawWidth, drawHeight, flip);
        batch.setColor(r, g, b, a);
    }

    private void drawFrame(SpriteBatch batch, TextureRegion frame, float drawX, float drawY,
                           float drawWidth, float drawHeight, boolean flip) {
        batch.draw(frame, flip ? drawX + drawWidth : drawX, drawY,
            flip ? -drawWidth : drawWidth, drawHeight);
    }

    @Override
    public void takeDamage(int amount) {
        if (amount <= 0 || isDead() || invulnTimer > 0 || isDashing()) {
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
        resetMovement();
    }

    public boolean isGrounded() {
        return !topDown && position.y <= groundY && verticalVelocity <= 0f;
    }

    /** Sets a fight's ground line and places the player on it. */
    public void setGroundY(float groundY) {
        this.groundY = groundY;
        position.y = groundY;
        resetMovement();
    }

    private void resetMovement() {
        verticalVelocity = 0f;
        jumpTime = 0f;
        landingTimer = 0f;
        jumpsUsed = 0;
        jumpBufferTimer = 0f;
        coyoteTimer = COYOTE_TIME;
        dashTimer = 0f;
        dashMovedThisFrame = false;
        dashCooldownTimer = 0f;
        knockbackVelocity = 0f;
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
