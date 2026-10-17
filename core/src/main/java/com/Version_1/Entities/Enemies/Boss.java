package com.Version_1.Entities.Enemies;

import com.Version_1.Anim.AnimationStateMachine;
import com.Version_1.Entities.Player;
import com.Version_1.Entities.Projectile;
import com.Version_1.World.World;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import java.util.Map;

/** Grounded boss: a readable slash, a low wave, and the story-ending attack. */
public class Boss extends Enemy {
    public enum State { IDLE, WALK, ATTACK, CAST, DEATH }
    private enum Action { CHASE, SLASH_WINDUP, SLASH, CAST_WINDUP, CAST, RECOVER,
        SINK, HIDDEN, RISE, FINISH_WINDUP, FINISH, DONE, DEAD }

    private static final float BODY_WIDTH = 40f;
    private static final float BODY_HEIGHT = 54f;
    private static final float HURTBOX_HEIGHT_SCALE = 0.75f;
    private static final float CONTACT_HEIGHT_SCALE = 0.5f;
    private static final float CONTACT_WIDTH = 18f;
    private static final float SHADOW_TRANSITION_TIME = 0.9f;
    private static final float FRAME_WIDTH = 140f;
    private static final float FRAME_HEIGHT = 93f;
    private static final float BODY_PIVOT_X = 106f;
    private static final float SPEED = 55f;
    private static final float STOP_GAP = 20f;
    private static final float SLASH_REACH = 150f;
    private static final float SLASH_WINDUP = 0.55f;
    private static final float SLASH_HIT_START = 0.32f;
    private static final float SLASH_HIT_END = 0.56f;
    private static final float CAST_WINDUP = 0.65f;
    private static final float CAST_FIRE_TIME = 0.32f;
    private static final float RECOVERY_TIME = 0.7f;
    private static final float RANGED_COOLDOWN = 3.2f;
    private static final float FINISH_WINDUP = 1.4f;
    private static final float FINISH_IMPACT_TIME = 0.4f;
    private static final int SLASH_DAMAGE = 18;
    private static final int WAVE_DAMAGE = 14;
    private static final float WAVE_SPEED = 180f;
    private static final float WAVE_WIDTH = 56f;
    private static final float WAVE_HEIGHT = 30f;

    private final AnimationStateMachine<State> anim;
    private final Animation<TextureRegion> waveAnimation;
    private final float scale;
    private final float arenaLeft;
    private final float arenaRight;
    private final int finisherHealth;
    private final Rectangle attackBox = new Rectangle();
    private final Rectangle contactBox = new Rectangle();
    private Action action = Action.CHASE;
    private float actionTime;
    private float rangedCooldown = 1f;
    private float damageFlash;
    private int attackId;
    private boolean waveFired;
    private boolean finisherStarted;
    private boolean finisherImpacted;
    private boolean shadowUsed;
    private final TextureRegion sinkingFrame = new TextureRegion();

    public Boss(float x, float y, float scale, int maxHealth, float arenaLeft, float arenaRight,
                float finisherHealthFraction, Map<State, Animation<TextureRegion>> animations,
                Animation<TextureRegion> waveAnimation, Player player, World world) {
        super(x, y, BODY_WIDTH * scale, BODY_HEIGHT * scale, maxHealth, player, world);
        if (scale <= 0f || arenaRight - arenaLeft < width) {
            throw new IllegalArgumentException("Invalid boss scale or arena width");
        }
        if (finisherHealthFraction < 0f || finisherHealthFraction >= 1f) {
            throw new IllegalArgumentException("Finisher fraction must be in [0, 1)");
        }
        this.scale = scale;
        this.arenaLeft = arenaLeft;
        this.arenaRight = arenaRight;
        this.finisherHealth = finisherHealthFraction == 0f ? 0
            : Math.max(1, (int) Math.ceil(maxHealth * finisherHealthFraction));
        this.anim = new AnimationStateMachine<>(animations, State.IDLE);
        this.waveAnimation = waveAnimation;
        facePlayer();
    }

    @Override
    public void update(float delta) {
        anim.update(delta);
        actionTime += delta;
        damageFlash = Math.max(0f, damageFlash - delta);
        rangedCooldown = Math.max(0f, rangedCooldown - delta);
        if (action == Action.DEAD) {
            if (anim.isAnimationFinished()) remove();
            return;
        }
        if (action == Action.DONE) return;
        if (player.isDead() && !finisherStarted) {
            action = Action.CHASE;
            anim.setState(State.IDLE);
            return;
        }

        switch (action) {
            case SINK:
                if (actionTime >= SHADOW_TRANSITION_TIME) begin(Action.HIDDEN, State.IDLE);
                break;
            case HIDDEN:
                break;
            case RISE:
                if (actionTime >= SHADOW_TRANSITION_TIME) begin(Action.RECOVER, State.IDLE);
                break;
            case CHASE:
                facePlayer();
                float gap = distanceToPlayer() - width / 2f - player.getBounds().width / 2f;
                if (gap > SLASH_REACH && rangedCooldown <= 0f) {
                    begin(Action.CAST_WINDUP, State.IDLE);
                } else if (gap <= SLASH_REACH * 0.7f && player.isGrounded()) {
                    begin(Action.SLASH_WINDUP, State.IDLE);
                } else if (gap > STOP_GAP) {
                    float step = Math.min(SPEED * delta, gap - STOP_GAP);
                    position.x = MathUtils.clamp(position.x + directionToPlayer() * step,
                        arenaLeft, arenaRight - width);
                    anim.setState(State.WALK);
                } else {
                    anim.setState(State.IDLE);
                }
                break;
            case SLASH_WINDUP:
                if (actionTime >= SLASH_WINDUP) {
                    attackId++;
                    begin(Action.SLASH, State.ATTACK);
                }
                break;
            case SLASH:
                if (anim.isAnimationFinished()) begin(Action.RECOVER, State.IDLE);
                break;
            case CAST_WINDUP:
                if (actionTime >= CAST_WINDUP) {
                    waveFired = false;
                    begin(Action.CAST, State.CAST);
                }
                break;
            case CAST:
                if (!waveFired && actionTime >= CAST_FIRE_TIME) {
                    waveFired = true;
                    float x = facingRight ? position.x + width : position.x - WAVE_WIDTH;
                    world.spawn(new Projectile(x, position.y + 8f, WAVE_WIDTH, WAVE_HEIGHT,
                        facingRight ? WAVE_SPEED : -WAVE_SPEED, WAVE_DAMAGE, 4f, waveAnimation, this));
                    rangedCooldown = RANGED_COOLDOWN;
                }
                if (anim.isAnimationFinished()) begin(Action.RECOVER, State.IDLE);
                break;
            case RECOVER:
                if (actionTime >= RECOVERY_TIME) begin(Action.CHASE, State.IDLE);
                break;
            case FINISH_WINDUP:
                if (actionTime >= FINISH_WINDUP) begin(Action.FINISH, State.ATTACK);
                break;
            case FINISH:
                if (actionTime >= FINISH_IMPACT_TIME) finisherImpacted = true;
                if (anim.isAnimationFinished()) begin(Action.DONE, State.IDLE);
                break;
            default:
                break;
        }
    }

    private void begin(Action next, State visual) {
        action = next;
        actionTime = 0f;
        anim.setState(visual);
        anim.restart();
    }

    @Override
    public void takeDamage(int amount) {
        if (amount <= 0 || isDead() || finisherStarted || isInShadowPhase()) return;
        // Clamp the crossing hit so even a large hit cannot skip the mob interlude.
        int shadowHealth = maxHealth / 2;
        if (finisherHealth > 0 && !shadowUsed && health - amount <= shadowHealth) {
            health = shadowHealth;
            shadowUsed = true;
            begin(Action.SINK, State.IDLE);
            return;
        }
        // Preserve the scripted ending even if a hit would otherwise kill the boss.
        int applied = finisherHealth > 0 ? Math.min(amount, health - finisherHealth) : amount;
        super.takeDamage(applied);
        damageFlash = 0.12f;
        if (finisherHealth > 0 && health <= finisherHealth) {
            finisherStarted = true;
            facePlayer();
            begin(Action.FINISH_WINDUP, State.IDLE);
        }
    }

    @Override
    protected void onDeath() {
        begin(Action.DEAD, State.DEATH);
    }

    public boolean isAttackActive() {
        return action == Action.SLASH && actionTime >= SLASH_HIT_START && actionTime <= SLASH_HIT_END;
    }

    @Override
    public Rectangle getBounds() {
        Rectangle bounds = super.getBounds();
        bounds.height *= HURTBOX_HEIGHT_SCALE;
        return bounds;
    }

    /** Low contact box lets a single jump clear the boss while air attacks can hit its torso. */
    public Rectangle getContactBounds() {
        float contactWidth = CONTACT_WIDTH * scale;
        return contactBox.set(position.x + (width - contactWidth) / 2f, position.y,
            contactWidth, height * CONTACT_HEIGHT_SCALE);
    }

    public boolean isInShadowPhase() {
        return action == Action.SINK || action == Action.HIDDEN || action == Action.RISE;
    }
    public boolean isSubmerged() { return action == Action.HIDDEN; }
    public void returnFromShadow() {
        if (isSubmerged()) begin(Action.RISE, State.IDLE);
    }
    public float getShadowDepth() {
        if (action == Action.HIDDEN) return 1f;
        if (action == Action.SINK) return MathUtils.clamp(actionTime / SHADOW_TRANSITION_TIME, 0f, 1f);
        if (action == Action.RISE) return 1f - MathUtils.clamp(actionTime / SHADOW_TRANSITION_TIME, 0f, 1f);
        return 0f;
    }

    public Rectangle getAttackBounds() {
        return attackBox.set(facingRight ? position.x + width : position.x - SLASH_REACH,
            position.y + 5f, SLASH_REACH, 48f);
    }

    public String getTelegraph() {
        if (action == Action.SINK) return "THE MONSTER SINKS INTO SHADOW";
        if (action == Action.HIDDEN) return "CLEAR THE SHADOWS - DASH TO SHAKE OFF LEECHES";
        if (action == Action.RISE) return "THE MONSTER RETURNS";
        if (action == Action.SLASH_WINDUP) return "SLASH - JUMP OR BACK AWAY";
        if (action == Action.CAST_WINDUP) return "SHADOW WAVE - JUMP";
        if (action == Action.FINISH_WINDUP) return "SOMETHING IS BREAKING...";
        return "";
    }

    public boolean isFinishing() { return finisherStarted; }
    public boolean hasFinisherImpacted() { return finisherImpacted; }
    public int getAttackId() { return attackId; }
    public int getAttackDamage() { return SLASH_DAMAGE; }

    @Override
    public void draw(SpriteBatch batch) {
        if (isSubmerged()) return;
        float visualScale = scale * (action == Action.FINISH ? 1.2f : 1f);
        float drawWidth = FRAME_WIDTH * visualScale;
        float drawHeight = FRAME_HEIGHT * visualScale;
        float pivot = facingRight ? FRAME_WIDTH - BODY_PIVOT_X : BODY_PIVOT_X;
        float x = position.x + width / 2f - pivot * visualScale;
        Color old = batch.getColor();
        float r = old.r, g = old.g, b = old.b, a = old.a;
        if (damageFlash > 0f) batch.setColor(1f, 0.55f, 0.55f, 1f);
        else if (!getTelegraph().isEmpty()) {
            float pulse = 0.65f + 0.35f * MathUtils.sin(actionTime * 18f);
            batch.setColor(1f, pulse, pulse, 1f);
        }
        TextureRegion frame = anim.getCurrentFrame();
        if (isInShadowPhase()) {
            int visibleHeight = Math.round(frame.getRegionHeight() * (1f - getShadowDepth()));
            if (visibleHeight > 0) {
                sinkingFrame.setRegion(frame);
                sinkingFrame.setRegionHeight(visibleHeight);
                batch.draw(sinkingFrame, facingRight ? x + drawWidth : x, position.y,
                    facingRight ? -drawWidth : drawWidth, visibleHeight * visualScale);
            }
        } else batch.draw(frame, facingRight ? x + drawWidth : x, position.y,
            facingRight ? -drawWidth : drawWidth, drawHeight);
        batch.setColor(r, g, b, a);
    }
}
