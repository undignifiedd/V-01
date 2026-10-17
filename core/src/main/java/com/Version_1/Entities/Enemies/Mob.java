package com.Version_1.Entities.Enemies;

import com.Version_1.Entities.Player;
import com.Version_1.World.World;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/** Small flying leech. FightPhase decides contact, attachment and damage. */
public class Mob extends Enemy {
    private static final float SPEED = 82f;
    private static final float HIT_STAGGER = 0.18f;
    private static final float DEFEAT_TIME = 0.25f;
    private static final float EMERGE_TIME = 0.4f;
    private final Animation<TextureRegion> animation;
    private final float groundY;
    protected float time;
    private boolean attached;
    private float latchSide;
    private float drainTimer;
    private float detachCooldown;
    private float hitTimer;
    private float recoilVelocity;
    private float defeatTimer;
    private float emergeRemaining;
    private float emergeDirection;
    private final TextureRegion emergingFrame = new TextureRegion();

    public Mob(float x, float y, Animation<TextureRegion> animation, Player player, World world) {
        super(x, y, 30f, 32f, 20, player, world);
        this.animation = animation;
        groundY = y;
    }

    @Override
    public void update(float delta) {
        if (updateReaction(delta)) return;
        if (player.isDead() || attached) return;
        facePlayer();
        Rectangle target = player.getBounds();
        float dx = target.x + target.width / 2f - (position.x + width / 2f);
        float dy = target.y + 20f - (position.y + height / 2f);
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance > 0f) {
            float step = Math.min(SPEED * delta, distance);
            position.add(dx / distance * step, dy / distance * step);
        }
        position.y = Math.max(groundY, position.y);
    }

    /** Shared feedback also runs while Mob2 is holding its fuse or fading after a punch. */
    protected boolean updateReaction(float delta) {
        if (isRemoved()) return true;
        time += delta;
        drainTimer = Math.max(0f, drainTimer - delta);
        detachCooldown = Math.max(0f, detachCooldown - delta);
        hitTimer = Math.max(0f, hitTimer - delta);
        position.x += recoilVelocity * delta;
        recoilVelocity *= (float) Math.exp(-10f * delta);
        if (isDead()) {
            defeatTimer += delta;
            if (defeatTimer >= DEFEAT_TIME) remove();
            return true;
        }
        if (isEmerging()) {
            position.x += emergeDirection * 40f * Math.min(delta, emergeRemaining);
            emergeRemaining = Math.max(0f, emergeRemaining - delta);
            return true;
        }
        return isStaggered();
    }

    public void emergeFromShadow(float direction) {
        emergeRemaining = EMERGE_TIME;
        emergeDirection = Math.signum(direction);
    }
    public boolean isEmerging() { return emergeRemaining > 0f; }
    public boolean isStaggered() { return hitTimer > 0f; }
    public float getHitFlash() { return hitTimer / HIT_STAGGER; }

    @Override
    public void takeDamage(int amount) {
        if (amount <= 0 || isDead() || isRemoved()) return;
        float away = -directionToPlayer();
        if (away == 0f) away = player.getDashDirectionX() < 0f ? -1f : 1f;
        attached = false;
        detachCooldown = Math.max(detachCooldown, HIT_STAGGER);
        hitTimer = HIT_STAGGER;
        recoilVelocity = away * 160f;
        super.takeDamage(amount);
    }

    @Override
    protected void onDeath() {
        attached = false;
        defeatTimer = 0f; // Keep the body briefly for visible defeat feedback.
    }

    public boolean isAttached() { return attached; }
    public boolean canLatch() {
        return !attached && !isDead() && !isEmerging() && !isStaggered() && detachCooldown <= 0f;
    }

    public void attach() {
        attached = true;
        latchSide = directionToPlayer() >= 0f ? -1f : 1f;
        drainTimer = 0f;
        followPlayer();
    }

    public void followPlayer() {
        Rectangle target = player.getBounds();
        position.set(target.x + target.width / 2f + latchSide * 13f - width / 2f,
            target.y + 17f);
    }

    public void detach() {
        attached = false;
        detachCooldown = 1f;
        position.x -= player.getDashDirectionX() * 35f;
        position.y = groundY;
    }

    /** Consumed by the phase so every leech keeps its own drain cadence. */
    public boolean consumeDrainTick() {
        if (!attached || drainTimer > 0f) return false;
        drainTimer = 0.8f;
        return true;
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (isRemoved()) return;
        TextureRegion frame = animation.getKeyFrame(time);
        Color color = batch.getColor();
        float r = color.r, g = color.g, b = color.b, a = color.a;
        float flash = getHitFlash();
        float fade = isDead() ? Math.max(0f, 1f - defeatTimer / DEFEAT_TIME) : 1f;
        if (flash > 0f) batch.setColor(1f, 0.35f + 0.65f * (1f - flash), 0.4f, a * fade);
        else if (isDead()) batch.setColor(0.55f, 0.3f, 0.65f, a * fade);
        else if (attached) batch.setColor(1f, 0.55f, 0.6f, a);
        float drawWidth = width * (1f + flash * 0.25f);
        float drawHeight = height * (1f - flash * 0.2f) * fade;
        float drawX = position.x + (width - drawWidth) / 2f;
        if (isEmerging()) {
            float progress = 1f - emergeRemaining / EMERGE_TIME;
            int visibleRows = Math.max(1, Math.round(frame.getRegionHeight() * progress));
            emergingFrame.setRegion(frame);
            emergingFrame.setRegionHeight(visibleRows);
            frame = emergingFrame;
            drawHeight *= progress;
        }
        batch.draw(frame, facingRight ? drawX : drawX + drawWidth, position.y,
            facingRight ? drawWidth : -drawWidth, drawHeight);
        batch.setColor(r, g, b, a);
    }
}
