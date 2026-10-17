package com.Version_1.Entities.Enemies;

import com.Version_1.Entities.Player;
import com.Version_1.World.World;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/** Flying suicide mob: stops and flashes for a dodgeable fuse, then bursts once. */
public class Mob2 extends Mob {
    private static final float FUSE_TIME = 0.7f;
    private final Rectangle blastBounds = new Rectangle();
    private float fuse = -1f;
    private float blastTime;
    private boolean exploded;
    private boolean blastResolved;

    public Mob2(float x, float y, Animation<TextureRegion> animation, Player player, World world) {
        super(x, y, animation, player, world);
    }

    @Override
    public void update(float delta) {
        if (isRemoved()) return;
        if (isDead()) {
            updateReaction(delta);
            return;
        }
        if (exploded) {
            blastTime += delta;
            if (blastTime >= 0.25f) remove();
        } else if (fuse >= 0f) {
            if (updateReaction(delta)) return;
            fuse += delta;
            if (fuse >= FUSE_TIME) exploded = true;
        } else super.update(delta);
    }

    public void arm() {
        if (fuse < 0f && !isEmerging() && !isStaggered()) fuse = 0f;
    }
    public boolean isArmed() { return fuse >= 0f; }
    public boolean hasExploded() { return exploded; }
    public float getBlastTime() { return blastTime; }
    public Rectangle getBlastBounds() {
        return blastBounds.set(position.x + 15f - 46f, position.y + 16f - 46f, 92f, 92f);
    }
    public boolean consumeBlast() {
        if (!exploded || blastResolved) return false;
        blastResolved = true;
        return true;
    }

    @Override
    public void takeDamage(int amount) {
        if (!exploded) super.takeDamage(amount);
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (exploded) return; // FightPhase draws the blast using its shared pixel.
        Color color = batch.getColor();
        float r = color.r, g = color.g, b = color.b, a = color.a;
        if (isArmed()) batch.setColor(1f, 0.25f + 0.5f * Math.abs((float) Math.sin(fuse * 28f)), 0.25f, a);
        super.draw(batch);
        batch.setColor(r, g, b, a);
    }
}
