package com.Version_1.Phases;

import com.Version_1.Entities.Attackable;
import com.Version_1.Entities.Entity;
import com.Version_1.Entities.Player;
import com.Version_1.Entities.Projectile;
import com.Version_1.Entities.Enemies.Boss;
import com.Version_1.World.World;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/** Owns one fight's entities and collision rules. Textures and font belong to Main. */
public class FightPhase implements Phase {
    private static final float END_HOLD_TIME = 2.4f;
    private static final int CONTACT_FIRE_DAMAGE = 5;
    private static final float BURN_DURATION = 1.5f;
    private static final float BURN_TICK_INTERVAL = 0.75f;
    private static final float CONTACT_KNOCKBACK_SPEED = 220f;
    private final World world;
    private final Player player;
    private final Boss boss;
    private final OrthographicCamera camera;
    private final BitmapFont font;
    private final Animation<TextureRegion> background;
    private final TextureRegion crack;
    private final TextureRegion pixel;
    private final float arenaWidth;
    private final float arenaHeight;
    private final float groundY;
    private final float cameraX;
    private final float cameraY;
    private final Array<Attackable> playerHitTargets = new Array<>();
    private int playerAttackId = -1;
    private int bossHitId = -1;
    private float time;
    private float endingTime = -1f;
    private boolean finished;
    private float burnRemaining;
    private float burnTickTimer;

    public FightPhase(World world, Player player, Boss boss, OrthographicCamera camera,
                      BitmapFont font, Animation<TextureRegion> background,
                      TextureRegion crack, TextureRegion pixel, float arenaWidth, float arenaHeight) {
        this.world = world;
        this.player = player;
        this.boss = boss;
        this.camera = camera;
        this.font = font;
        this.background = background;
        this.crack = crack;
        this.pixel = pixel;
        this.arenaWidth = arenaWidth;
        this.arenaHeight = arenaHeight;
        this.groundY = player.getPosition().y;
        this.cameraX = camera.position.x;
        this.cameraY = camera.position.y;
        world.spawn(boss);
        world.spawn(player);
        world.update(0f); // flush the spawn queue before the first playable frame
    }

    @Override
    public void update(float delta) {
        if (finished) return;
        // Keep a hitch from skipping an entire attack window or crossing a target in one step.
        delta = Math.min(delta, 0.05f);
        time += delta;
        world.update(delta);
        player.getPosition().x = MathUtils.clamp(player.getPosition().x, 12f,
            arenaWidth - 12f - player.getBounds().width);
        resolveDashPassThrough();

        if (!player.isDead()) {
            resolvePlayerAttack();
            if (!boss.isFinishing()) resolveBossAttack();
        }
        resolveProjectiles();
        resolveContactFire(delta);

        if (boss.hasFinisherImpacted()) {
            if (endingTime < 0f) endingTime = 0f;
            endingTime += delta;
            float shake = 5f * Math.max(0f, 1f - endingTime / 0.65f);
            camera.position.x = cameraX + MathUtils.sin(endingTime * 83f) * shake;
            camera.position.y = cameraY + MathUtils.cos(endingTime * 71f) * shake;
            finished = endingTime >= END_HOLD_TIME;
        } else if (boss.isRemoved()) {
            finished = true;
        }
    }

    private void resolveDashPassThrough() {
        if (!player.didDashThisFrame() || boss.isDead()
            || !player.getBounds().overlaps(boss.getContactBounds())) return;
        Rectangle body = boss.getContactBounds();
        float exitX = player.getDashDirectionX() > 0f ? body.x + body.width + 4f
            : body.x - player.getBounds().width - 4f;
        exitX = MathUtils.clamp(exitX, 12f, arenaWidth - 12f - player.getBounds().width);
        player.extendDashToX(exitX);
    }

    private void resolveContactFire(float delta) {
        if (player.isDead() || boss.isDead() || boss.isFinishing()) {
            burnRemaining = 0f;
            burnTickTimer = 0f;
            player.setBurning(false);
            return;
        }
        boolean touching = !player.isDashing() && player.getBounds().overlaps(boss.getContactBounds());
        burnRemaining = touching ? BURN_DURATION : Math.max(0f, burnRemaining - delta);
        burnTickTimer = Math.max(0f, burnTickTimer - delta);
        player.setBurning(burnRemaining > 0f);
        if (burnRemaining <= 0f) {
            burnTickTimer = 0f;
            return;
        }
        if (burnTickTimer <= 0f) {
            int before = player.getHealth();
            player.takeDamage(CONTACT_FIRE_DAMAGE);
            burnTickTimer = BURN_TICK_INTERVAL;
            if (touching && player.getHealth() < before && !player.isDead()) pushPlayerClear();
        }
    }

    private void pushPlayerClear() {
        Rectangle body = boss.getContactBounds();
        Rectangle target = player.getBounds();
        float leftX = body.x - target.width - 2f;
        float rightX = body.x + body.width + 2f;
        float minX = 12f;
        float maxX = arenaWidth - 12f - target.width;
        boolean pushRight = target.x + target.width / 2f >= body.x + body.width / 2f;
        // At a wall, push toward the arena instead of trapping the player inside the boss.
        if (leftX < minX) pushRight = true;
        else if (rightX > maxX) pushRight = false;
        player.getPosition().x = MathUtils.clamp(pushRight ? rightX : leftX, minX, maxX);
        player.knockBack(pushRight ? CONTACT_KNOCKBACK_SPEED : -CONTACT_KNOCKBACK_SPEED);
    }

    private void resolvePlayerAttack() {
        if (playerAttackId != player.getAttackId()) {
            playerAttackId = player.getAttackId();
            playerHitTargets.clear();
        }
        if (!player.isAttackActive()) return;
        Array<Entity> entities = world.getEntities();
        for (int i = 0; i < entities.size; i++) {
            Entity entity = entities.get(i);
            if (entity == player || entity.isRemoved() || !(entity instanceof Attackable)) continue;
            Attackable target = (Attackable) entity;
            if (target.isDead() || playerHitTargets.contains(target, true)) continue;
            if (player.getAttackBounds().overlaps(target.getBounds())) {
                playerHitTargets.add(target);
                target.takeDamage(player.getAttackDamage());
            }
        }
    }

    private void resolveBossAttack() {
        if (boss.isAttackActive() && bossHitId != boss.getAttackId()
            && boss.getAttackBounds().overlaps(player.getBounds())) {
            bossHitId = boss.getAttackId();
            player.takeDamage(boss.getAttackDamage());
        }
    }

    private void resolveProjectiles() {
        Array<Entity> entities = world.getEntities();
        for (int i = 0; i < entities.size; i++) {
            Entity entity = entities.get(i);
            if (!(entity instanceof Projectile) || entity.isRemoved()) continue;
            Projectile projectile = (Projectile) entity;
            if (boss.isFinishing() || player.isDead() || projectile.getPosition().x < -100f
                || projectile.getPosition().x > arenaWidth + 100f) {
                projectile.remove();
            } else if (projectile.getOwner() == boss && projectile.getBounds().overlaps(player.getBounds())) {
                player.takeDamage(projectile.getDamage());
                projectile.remove();
            }
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        Color old = batch.getColor();
        float r = old.r, g = old.g, b = old.b, a = old.a;
        float screenX = camera.position.x - cameraX;
        float screenY = camera.position.y - cameraY;
        fill(batch, screenX, screenY, arenaWidth, arenaHeight, 0f, 0f, 0f, 1f);
        batch.setColor(0.55f, 0.3f, 0.6f, 0.6f);
        batch.draw(background.getKeyFrame(time), arenaWidth / 2f - 80f, 170f, 160f, 160f);
        fill(batch, 0f, 0f, arenaWidth, groundY, 0.075f, 0.055f, 0.1f, 1f);
        fill(batch, 0f, groundY - 1f, arenaWidth, 1f, 0.35f, 0.23f, 0.38f, 1f);
        batch.setColor(Color.WHITE);
        world.draw(batch);
        if (player.isBurning() && !player.isDead()) drawFire(batch);

        if (endingTime >= 0f) {
            float flash = Math.max(0f, 1f - endingTime / 0.2f);
            fill(batch, screenX, screenY, arenaWidth, arenaHeight, 1f, 0.9f, 1f, flash);
            batch.setColor(1f, 1f, 1f, Math.min(1f, endingTime * 12f));
            batch.draw(crack, screenX, screenY, arenaWidth, arenaHeight);
        }
        drawHud(batch);
        batch.setColor(r, g, b, a);
    }

    private void drawFire(SpriteBatch batch) {
        float centerX = player.getPosition().x + player.getBounds().width / 2f;
        float feetY = player.getPosition().y;
        for (int i = 0; i < 5; i++) {
            float height = 4f + 6f * (0.5f + 0.5f * MathUtils.sin(time * 23f + i * 1.7f));
            float x = centerX - 8f + i * 4f;
            fill(batch, x, feetY + 2f, 3f, height, 1f, 0.3f, 0.015f, 0.8f);
            fill(batch, x + 1f, feetY + 2f, 1f, height * 0.6f, 1f, 0.85f, 0.25f, 0.95f);
        }
        for (int i = 0; i < 9; i++) {
            float age = (time * (1.2f + i * 0.07f) + i * 0.173f) % 1f;
            float x = centerX + MathUtils.sin(i * 2.1f + age * 5f) * (6f + i % 3 * 2f);
            float y = feetY + 8f + age * 55f;
            fill(batch, x, y, 1f, 2f, 1f, 0.45f + age * 0.4f, 0.05f, 1f - age);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawHud(SpriteBatch batch) {
        fill(batch, 18f, arenaHeight - 39f, 180f, 7f, 0.15f, 0.12f, 0.18f, 1f);
        fill(batch, 18f, arenaHeight - 39f, 180f * player.getHealth() / player.getMaxHealth(), 7f,
            0.3f, 0.85f, 0.65f, 1f);
        fill(batch, arenaWidth - 218f, arenaHeight - 39f, 200f, 7f, 0.15f, 0.12f, 0.18f, 1f);
        fill(batch, arenaWidth - 218f, arenaHeight - 39f, 200f * boss.getHealth() / boss.getMaxHealth(), 7f,
            0.8f, 0.25f, 0.45f, 1f);
        batch.setColor(Color.WHITE);
        font.draw(batch, "YOU  " + player.getHealth() + "/" + player.getMaxHealth(), 18f, arenaHeight - 12f);
        font.draw(batch, "MR. MONSTER", arenaWidth - 218f, arenaHeight - 12f);
        font.draw(batch, "A/D move   W jump x2   J attack   SPACE dash", 18f, 24f);
        if (player.isBurning()) font.draw(batch, "BURNING", 18f, arenaHeight - 80f);
        String message = player.isDead() ? "YOU FELL. Press R to retry."
            : endingTime >= 0f ? "The world fractures." : boss.getTelegraph();
        if (!message.isEmpty()) font.draw(batch, message, 18f, arenaHeight - 58f);
    }

    private void fill(SpriteBatch batch, float x, float y, float width, float height,
                      float r, float g, float b, float a) {
        batch.setColor(r, g, b, a);
        batch.draw(pixel, x, y, width, height);
    }

    public boolean isPlayerDead() { return player.isDead(); }

    @Override
    public boolean isFinished() { return finished; }

    @Override
    public void dispose() {
        camera.position.set(cameraX, cameraY, 0f);
        // Main owns every texture and the font; entities hold no disposable resources.
    }
}
