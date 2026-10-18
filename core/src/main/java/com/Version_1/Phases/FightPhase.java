package com.Version_1.Phases;

import com.Version_1.Entities.Attackable;
import com.Version_1.Entities.Entity;
import com.Version_1.Entities.Player;
import com.Version_1.Entities.Projectile;
import com.Version_1.Entities.Enemies.Boss;
import com.Version_1.Entities.Enemies.Enemy;
import com.Version_1.Entities.Enemies.Mob;
import com.Version_1.Entities.Enemies.Mob2;
import com.Version_1.World.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
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
    private static final float SHIELD_ANTICIPATION = 2.4f;
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
    private float deathTime = -1f;
    private boolean finished;
    private float burnRemaining;
    private float burnTickTimer;
    private final Animation<TextureRegion> mobAnimation;
    private final Animation<TextureRegion> mob2Animation;
    private final Array<Mob> shadowMobs = new Array<>();
    private static final int SHADOW_MOB_COUNT = 4;
    private int summonedMobs;
    private float summonTimer;
    private float shadowSpawnX;
    private boolean shadowPuddleActive;
    private boolean supportAndShieldExit;
    private float supportTimer = 2f;
    private float shieldTime = -1f;
    private float launchX, launchY, launchDirection;
    private Animation<TextureRegion> rageWave;
    private TextureRegion rageFrame;
    private Sound punchHit1, punchHit2, heavyHit, waveLaunch, explosion, shieldImpact;
    private int punchSoundAttackId = -1, punchVariant, shieldsHeard;

    public FightPhase(World world, Player player, Boss boss, OrthographicCamera camera,
                      BitmapFont font, Animation<TextureRegion> background,
                      TextureRegion crack, TextureRegion pixel, float arenaWidth, float arenaHeight,
                      Animation<TextureRegion> mobAnimation, Animation<TextureRegion> mob2Animation) {
        this.world = world;
        this.player = player;
        this.boss = boss;
        this.camera = camera;
        this.font = font;
        this.background = background;
        this.crack = crack;
        this.pixel = pixel;
        this.mobAnimation = mobAnimation;
        this.mob2Animation = mob2Animation;
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
        if (player.isDead()) {
            if (deathTime < 0f) {
                deathTime = 0f;
                player.startCinematic();
            }
            deathTime += delta;
            return;
        }
        if (shieldTime >= 0f) {
            updateShieldExit(delta);
            return;
        }
        int healthBefore = player.getHealth();
        fireRageWave();
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
        playPlayerHit(healthBefore);
        healthBefore = player.getHealth();
        if (supportAndShieldExit && boss.isFinishing()) {
            beginShieldExit();
            return;
        }
        if (boss.isInShadowPhase() && !shadowPuddleActive) {
            shadowSpawnX = boss.getBounds().x + boss.getBounds().width / 2f;
            shadowPuddleActive = true;
        }
        updateShadowWave(delta);
        resolveMobs();
        playPlayerHit(healthBefore);

        if (boss.hasFinisherImpacted()) {
            if (endingTime < 0f) {
                endingTime = 0f;
                play(explosion, 0.8f, 0.75f);
            }
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
        if (!player.didDashThisFrame() || boss.isDead() || boss.isInShadowPhase()
            || !player.getBounds().overlaps(boss.getContactBounds())) return;
        Rectangle body = boss.getContactBounds();
        float exitX = player.getDashDirectionX() > 0f ? body.x + body.width + 4f
            : body.x - player.getBounds().width - 4f;
        exitX = MathUtils.clamp(exitX, 12f, arenaWidth - 12f - player.getBounds().width);
        player.extendDashToX(exitX);
    }

    public void setRageArt(TextureRegion wave, TextureRegion meterFrame) {
        rageWave = new Animation<>(1f, wave);
        rageFrame = meterFrame;
    }

    public void setCombatSounds(Sound punchHit1, Sound punchHit2, Sound heavyHit,
                                Sound waveLaunch, Sound explosion, Sound shieldImpact) {
        this.punchHit1 = punchHit1;
        this.punchHit2 = punchHit2;
        this.heavyHit = heavyHit;
        this.waveLaunch = waveLaunch;
        this.explosion = explosion;
        this.shieldImpact = shieldImpact;
    }

    private void play(Sound sound, float volume, float pitch) {
        if (sound != null) sound.play(volume, pitch, 0f);
    }

    private void playPlayerHit(int healthBefore) {
        if (player.getHealth() < healthBefore) play(heavyHit, 0.55f, 0.95f);
    }

    private void fireRageWave() {
        if (rageWave == null || boss.isDead() || boss.isFinishing()
            || !Gdx.input.isKeyJustPressed(Input.Keys.K) || !player.spendFullRage()) return;
        float direction = player.isFacingRight() ? 1f : -1f;
        Rectangle body = player.getContactBounds();
        float x = MathUtils.clamp(body.x + body.width / 2f - 48f, 12f, arenaWidth - 108f);
        world.spawn(new Projectile(x, player.getPosition().y, 96f, 120f,
            direction * 280f, 40, 3f, rageWave, player, true));
    }

    private void resolveContactFire(float delta) {
        if (player.isDead() || boss.isDead() || boss.isFinishing() || boss.isInShadowPhase() || boss.isRageCarried()) {
            burnRemaining = 0f;
            burnTickTimer = 0f;
            player.setBurning(false);
            return;
        }
        boolean touching = !player.isDashing() && !player.didDashThisFrame()
            && player.getContactBounds().overlaps(boss.getContactBounds());
        burnRemaining = touching ? BURN_DURATION : Math.max(0f, burnRemaining - delta);
        burnTickTimer = Math.max(0f, burnTickTimer - delta);
        player.setBurning(burnRemaining > 0f);
        if (burnRemaining <= 0f) {
            burnTickTimer = 0f;
            return;
        }
        if (burnTickTimer <= 0f) {
            int before = player.getHealth();
            player.takeDamage(Math.round(CONTACT_FIRE_DAMAGE * boss.getDifficulty()));
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
                int before = target instanceof Enemy ? ((Enemy) target).getHealth() : 0;
                target.takeDamage(player.getAttackDamage());
                if (target instanceof Enemy) {
                    int dealt = Math.max(0, before - ((Enemy) target).getHealth());
                    player.addRage(dealt);
                    if (dealt > 0 && punchSoundAttackId != playerAttackId) {
                        punchSoundAttackId = playerAttackId;
                        play(punchVariant++ % 2 == 0 ? punchHit1 : punchHit2, 0.65f, MathUtils.random(0.96f, 1.04f));
                    }
                }
            }
        }
    }

    private void resolveBossAttack() {
        if (!boss.isRageCarried() && boss.isAttackActive() && bossHitId != boss.getAttackId()
            && boss.getAttackBounds().overlaps(player.getBounds())) {
            bossHitId = boss.getAttackId();
            player.takeDamage(boss.getAttackDamage());
        }
    }

    private void updateShadowWave(float delta) {
        if (supportAndShieldExit) {
            updateSupportMobs(delta);
            return;
        }
        if (!boss.isSubmerged() || player.isDead()) return;
        summonTimer -= delta;
        if (summonedMobs < SHADOW_MOB_COUNT && summonTimer <= 0f) {
            float x = MathUtils.clamp(shadowSpawnX - 15f, 12f, arenaWidth - 42f);
            Mob mob = summonedMobs % 2 == 0
                ? new Mob(x, groundY, mobAnimation, player, world)
                : new Mob2(x, groundY, mob2Animation, player, world);
            mob.emergeFromShadow(summonedMobs % 2 == 0 ? -1f : 1f);
            shadowMobs.add(mob);
            world.spawn(mob);
            summonedMobs++;
            summonTimer = 0.8f;
        }
        if (summonedMobs < SHADOW_MOB_COUNT) return;
        for (int i = 0; i < shadowMobs.size; i++) {
            Mob mob = shadowMobs.get(i);
            if (!mob.isRemoved()) return;
        }
        boss.returnFromShadow();
    }

    public void enableSupportAndShieldExit() { supportAndShieldExit = true; }
    public boolean isShieldExitActive() { return shieldTime >= 0f; }
    public float getShieldTime() { return shieldTime; }

    private void updateSupportMobs(float delta) {
        if (player.isDead() || boss.isFinishing() || boss.isRageCarried()) return;
        supportTimer -= delta;
        if (supportTimer > 0f) return;
        int active = 0;
        for (int i = 0; i < shadowMobs.size; i++) {
            if (!shadowMobs.get(i).isDead() && !shadowMobs.get(i).isRemoved()) active++;
        }
        if (active >= 2) return;
        Rectangle body = boss.getBounds();
        float direction = player.getPosition().x + 32f < body.x + body.width / 2f ? -1f : 1f;
        shadowSpawnX = MathUtils.clamp(direction < 0f ? body.x - 32f : body.x + body.width + 32f,
            40f, arenaWidth - 40f);
        shadowPuddleActive = true;
        for (int i = active; i < 2; i++) {
            Mob mob = summonedMobs % 2 == 0
                ? new Mob(shadowSpawnX - 15f, groundY, mobAnimation, player, world)
                : new Mob2(shadowSpawnX - 15f, groundY, mob2Animation, player, world);
            mob.emergeFromShadow(direction);
            shadowMobs.add(mob);
            world.spawn(mob);
            summonedMobs++;
        }
        supportTimer = 4f;
    }

    private void beginShieldExit() {
        shieldTime = 0f;
        player.startCinematic();
        player.setBurning(false);
        burnRemaining = 0f;
        launchX = player.getPosition().x;
        launchY = groundY;
        launchDirection = player.getPosition().x + 32f < boss.getBounds().x + boss.getBounds().width / 2f ? -1f : 1f;
        Array<Entity> entities = world.getEntities();
        for (int i = 0; i < entities.size; i++) {
            Entity entity = entities.get(i);
            if (entity instanceof Mob || entity instanceof Projectile) entity.remove();
        }
        shadowPuddleActive = false;
    }

    private void updateShieldExit(float delta) {
        shieldTime += delta;
        float actionTime = shieldTime - SHIELD_ANTICIPATION;
        if (actionTime <= 0f) {
            camera.position.set(cameraX, cameraY, 0f);
            return;
        }
        while (shieldsHeard < 3 && actionTime >= 0.55f + shieldsHeard * 0.18f) {
            play(shieldImpact, 0.75f, 0.85f + shieldsHeard * 0.1f);
            shieldsHeard++;
        }
        float flight = Math.max(0f, actionTime - 1.15f);
        if (flight > 0f) {
            player.getPosition().set(launchX + launchDirection * flight * 360f,
                launchY + MathUtils.sin(Math.min(1f, flight / 1.6f) * MathUtils.PI) * 160f + flight * 20f);
        }
        float shake = actionTime < 1.15f ? 4f : 1f;
        camera.position.x = cameraX + launchDirection * Math.max(0f, flight - 0.15f) * 280f
            + MathUtils.sin(actionTime * 73f) * shake;
        camera.position.y = cameraY + MathUtils.cos(actionTime * 61f) * shake;
        finished = actionTime >= 3.2f;
    }

    private void resolveMobs() {
        if (player.isDead() || boss.isFinishing()) return;
        for (int i = 0; i < shadowMobs.size; i++) {
            Mob mob = shadowMobs.get(i);
            if (mob.isDead() || mob.isRemoved() || mob.isEmerging() || mob.isRageCarried()) continue;
            if (mob instanceof Mob2) {
                Mob2 bomb = (Mob2) mob;
                // Proximity starts a fixed fuse; moving away can evade the eventual blast.
                Rectangle body = player.getContactBounds();
                if (!bomb.isArmed() && !bomb.isStaggered() && !player.isDashing() && !player.didDashThisFrame()
                    && bomb.getBlastBounds().overlaps(body)) bomb.arm();
                if (bomb.consumeBlast()) {
                    play(explosion, 0.45f, 1.2f);
                    if (bomb.getBlastBounds().overlaps(body) && !player.didDashThisFrame()) player.takeDamage(16);
                }
            } else if (player.isDashing() || player.didDashThisFrame()) {
                if (mob.isAttached()) {
                    mob.detach();
                    mob.getPosition().x = MathUtils.clamp(mob.getPosition().x,
                        12f, arenaWidth - 12f - mob.getBounds().width);
                }
            } else {
                if (mob.canLatch() && mob.getBounds().overlaps(player.getContactBounds())) mob.attach();
                if (mob.isAttached()) {
                    mob.followPlayer();
                    if (mob.consumeDrainTick()) player.takeDamage(4);
                }
            }
        }
    }

    private void resolveProjectiles() {
        Array<Entity> entities = world.getEntities();
        for (int i = 0; i < entities.size; i++) {
            Entity entity = entities.get(i);
            if (!(entity instanceof Projectile) || entity.isRemoved()) continue;
            Projectile projectile = (Projectile) entity;
            if ((boss.isFinishing() && !projectile.isExploding()) || (boss.isInShadowPhase() && projectile.getOwner() != player)
                || player.isDead() || projectile.getPosition().x < -100f
                || projectile.getPosition().x > arenaWidth + 100f) {
                projectile.remove();
                continue;
            }
            if (projectile.consumeLaunchSound() && !boss.isFinishing()) {
                play(waveLaunch, projectile.getOwner() == player ? 0.6f : 0.35f,
                    projectile.getOwner() == player ? 1f : 0.8f);
            }
            if (projectile.getOwner() == boss && projectile.getBounds().overlaps(player.getBounds())) {
                player.takeDamage(projectile.getDamage());
                projectile.remove();
            } else if (projectile.getOwner() == player) {
                resolveRageWave(projectile, entities);
            }
        }
    }

    private boolean canRageHit(Enemy enemy) {
        return !enemy.isDead() && !enemy.isRemoved()
            && !(enemy == boss && (boss.isInShadowPhase() || boss.isFinishing()))
            && !(enemy instanceof Mob && ((Mob) enemy).isEmerging())
            && !(enemy instanceof Mob2 && ((Mob2) enemy).hasExploded());
    }

    private void resolveRageWave(Projectile wave, Array<Entity> entities) {
        if (!wave.isExploding()) {
            Rectangle waveBox = wave.getBounds();
            wave.getPosition().x = MathUtils.clamp(waveBox.x, 12f, arenaWidth - 12f - waveBox.width);
            for (int j = 0; j < entities.size; j++) {
                Entity candidate = entities.get(j);
                if (!(candidate instanceof Enemy)) continue;
                Enemy enemy = (Enemy) candidate;
                if (canRageHit(enemy) && !wave.hasHit(enemy)
                    && wave.getBounds().overlaps(enemy.getBounds()) && enemy.holdByRage(wave)) wave.markHit(enemy);
            }
            Array<Attackable> caught = wave.getHitTargets();
            for (int j = 0; j < caught.size; j++) {
                Enemy enemy = (Enemy) caught.get(j);
                if (!canRageHit(enemy)) { enemy.releaseFromRage(wave); continue; }
                Rectangle body = enemy.getBounds();
                float offsetX = body.x - enemy.getPosition().x;
                float center = wave.getPosition().x + waveBox.width / 2f + wave.getDirection() * waveBox.width * 0.25f;
                float bodyX = MathUtils.clamp(center - body.width / 2f, 12f, arenaWidth - 12f - body.width);
                enemy.getPosition().x = bodyX - offsetX;
            }
            if ((wave.getDirection() > 0f && wave.getPosition().x + waveBox.width >= arenaWidth - 12f)
                || (wave.getDirection() < 0f && wave.getPosition().x <= 12f)) wave.explode();
        }
        if (!wave.consumeExplosion()) return;
        play(explosion, 0.7f, 1f);
        for (int j = 0; j < entities.size; j++) {
            Entity candidate = entities.get(j);
            if (!(candidate instanceof Enemy)) continue;
            Enemy enemy = (Enemy) candidate;
            boolean hit = canRageHit(enemy) && (wave.hasHit(enemy) || wave.getBlastBounds().overlaps(enemy.getBounds()));
            enemy.releaseFromRage(wave);
            if (hit) enemy.takeDamage(wave.getDamage());
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
        if (shadowPuddleActive) drawShadowPuddle(batch);
        batch.setColor(Color.WHITE);
        if (shieldTime < 0f) world.draw(batch);
        else {
            Array<Entity> entities = world.getEntities();
            for (int i = 0; i < entities.size; i++) {
                Entity entity = entities.get(i);
                if (entity != player && !entity.isRemoved()) entity.draw(batch);
            }
            if (shieldTime >= SHIELD_ANTICIPATION + 1.15f) {
                player.drawSpinning(batch, launchDirection * (shieldTime - SHIELD_ANTICIPATION - 1.15f) * 900f);
            }
            else player.draw(batch);
            drawShields(batch);
        }
        drawMobEffects(batch);
        drawRageEffects(batch);
        if (player.isBurning() && !player.isDead()) drawFire(batch);

        if (endingTime >= 0f) {
            float flash = Math.max(0f, 1f - endingTime / 0.2f);
            fill(batch, screenX, screenY, arenaWidth, arenaHeight, 1f, 0.9f, 1f, flash);
            batch.setColor(1f, 1f, 1f, Math.min(1f, endingTime * 12f));
            batch.draw(crack, screenX, screenY, arenaWidth, arenaHeight);
        }
        if (shieldTime < 0f) drawHud(batch);
        else if (shieldTime > SHIELD_ANTICIPATION + 2.5f) {
            fill(batch, screenX, screenY, arenaWidth, arenaHeight, 0f, 0f, 0f,
                MathUtils.clamp((shieldTime - SHIELD_ANTICIPATION - 2.5f) / 0.7f, 0f, 1f));
        }
        if (deathTime >= 0f) {
            fill(batch, screenX, screenY, arenaWidth, arenaHeight, 0f, 0f, 0f,
                MathUtils.clamp(deathTime / 0.8f, 0f, 1f));
        }
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

    private void drawRageEffects(SpriteBatch batch) {
        Array<Entity> entities = world.getEntities();
        for (int i = 0; i < entities.size; i++) {
            Entity entity = entities.get(i);
            if (!(entity instanceof Projectile) || entity.isRemoved()) continue;
            Projectile wave = (Projectile) entity;
            if (wave.getOwner() != player) continue;
            Rectangle box = wave.getBounds();
            float centerX = box.x + box.width / 2f, centerY = box.y + box.height / 2f;
            if (wave.isExploding()) {
                float progress = wave.getExplosionTime() / 0.55f;
                float radius = 25f + 100f * progress;
                for (int spark = 0; spark < 36; spark++) {
                    float angle = spark * MathUtils.PI2 / 36f;
                    float x = centerX + MathUtils.cos(angle) * radius;
                    float y = centerY + MathUtils.sin(angle) * radius * 0.8f;
                    fill(batch, x - 5f, y - 5f, 10f, 10f, 1f, 0.25f + spark % 3 * 0.2f, 0.02f, 1f - progress);
                }
                fill(batch, box.x - 15f * progress, box.y, box.width + 30f * progress,
                    box.height, 1f, 0.65f, 0.15f, (1f - progress) * 0.65f);
            } else {
                for (int flame = 0; flame < 18; flame++) {
                    float flicker = 0.5f + 0.5f * MathUtils.sin(time * 25f + flame * 2.3f);
                    float x = box.x + (flame % 6) * box.width / 6f;
                    float y = box.y + (flame / 6) * box.height / 3f;
                    // Rim flames leave the temporary central artwork visible.
                    if (flame / 6 == 1 && flame % 6 > 0 && flame % 6 < 5) continue;
                    fill(batch, x, y, 10f, 14f + flicker * 20f, 1f, 0.22f + flicker * 0.3f, 0.01f, 0.85f);
                    fill(batch, x + 3f, y, 4f, 8f + flicker * 12f, 1f, 0.85f, 0.3f, 0.95f);
                }
                for (int ember = 0; ember < 16; ember++) {
                    float age = (time * 1.8f + ember * 0.137f) % 1f;
                    float x = centerX - wave.getDirection() * (45f + age * 65f);
                    float y = box.y + ember % 7 * 16f + age * 20f;
                    fill(batch, x, y, 3f, 5f, 1f, 0.5f, 0.05f, 1f - age);
                }
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawShields(SpriteBatch batch) {
        float origin = boss.getBounds().x + boss.getBounds().width / 2f;
        for (int i = 0; i < 3; i++) {
            float age = shieldTime - SHIELD_ANTICIPATION - i * 0.18f;
            if (age < 0f) continue;
            float x = MathUtils.clamp(origin + launchDirection * (100f + i * 75f) - 32f, 20f, arenaWidth - 84f);
            float drop = MathUtils.clamp(age / 0.55f, 0f, 1f);
            float y = MathUtils.lerp(arenaHeight + 40f, groundY, drop * drop);
            fill(batch, x, y, 64f, 238f, 0.12f, 0.1f, 0.19f, 1f);
            fill(batch, x + 4f, y + 5f, 56f, 228f, 0.30f, 0.26f, 0.38f, 1f);
            fill(batch, x + 10f, y + 10f, 44f, 216f, 0.09f, 0.07f, 0.14f, 1f);
            for (int rib = 0; rib < 5; rib++) {
                fill(batch, x + 8f, y + 30f + rib * 38f, 48f, 4f, 0.39f, 0.31f, 0.45f, 1f);
            }
            // A carved eye and pointed crown make the barrier read as a massive shield.
            for (int row = 0; row < 5; row++) {
                fill(batch, x + 22f - row * 2f, y + 125f + row * 3f, 20f + row * 4f, 3f,
                    0.56f, 0.25f, 0.40f, 1f);
            }
            fill(batch, x + 29f, y + 126f, 6f, 16f, 0.04f, 0.015f, 0.04f, 1f);
            for (int point = 0; point < 4; point++) {
                fill(batch, x + 5f + point * 15f, y + 238f, 9f, 10f + point % 2 * 6f, 0.3f, 0.26f, 0.38f, 1f);
            }
            if (drop >= 1f && age < 0.95f) {
                fill(batch, x - 16f, groundY, 96f, 10f * (1f - (age - 0.55f) / 0.4f),
                    0.42f, 0.33f, 0.48f, 0.6f);
            }
        }
    }

    private void drawShadowPuddle(SpriteBatch batch) {
        float radius = 75f;
        float pulse = 0.5f + 0.5f * MathUtils.sin(time * 5f);
        // The rim keeps the black pool readable against the arena's black background.
        for (int y = -10; y <= 10; y += 2) {
            float halfWidth = (radius + 3f) * (float) Math.sqrt(Math.max(0f, 1f - y * y / 121f));
            fill(batch, shadowSpawnX - halfWidth, groundY + y, halfWidth * 2f, 2f,
                0.16f + pulse * 0.05f, 0.07f, 0.24f + pulse * 0.08f, 1f);
            if (Math.abs(y) <= 8) {
                halfWidth = radius * (float) Math.sqrt(Math.max(0f, 1f - y * y / 81f));
                fill(batch, shadowSpawnX - halfWidth, groundY + y, halfWidth * 2f, 2f,
                    0.005f, 0.002f, 0.012f, 1f);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawMobEffects(SpriteBatch batch) {
        for (int i = 0; i < shadowMobs.size; i++) {
            Mob mob = shadowMobs.get(i);
            if (mob.isRemoved()) continue;
            float flash = mob.getHitFlash();
            if (flash > 0f) {
                Rectangle body = mob.getBounds();
                float distance = 8f + (1f - flash) * 15f;
                for (int spark = 0; spark < 6; spark++) {
                    float angle = spark * MathUtils.PI2 / 6f;
                    float x = body.x + body.width / 2f + MathUtils.cos(angle) * distance;
                    float y = body.y + body.height / 2f + MathUtils.sin(angle) * distance;
                    fill(batch, x, y, 3f, 2f, 1f, 0.85f, 0.65f, flash);
                }
            }
            if (mob.isDead() || !(mob instanceof Mob2)) continue;
            Mob2 bomb = (Mob2) mob;
            Rectangle blast = bomb.getBlastBounds();
            if (bomb.hasExploded()) {
                float alpha = 1f - bomb.getBlastTime() / 0.25f;
                fill(batch, blast.x, blast.y, blast.width, blast.height, 0.65f, 0.25f, 0.85f, alpha * 0.5f);
            } else if (bomb.isArmed()) {
                fill(batch, blast.x, groundY, blast.width, 2f, 0.9f, 0.3f, 0.65f, 0.8f);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawHud(SpriteBatch batch) {
        if (rageFrame != null) {
            float fillHeight = 46f * player.getRage() / player.getMaxRage();
            if (player.isRageReady()) {
                float glow = 0.12f + 0.06f * MathUtils.sin(time * 8f);
                fill(batch, 29f, 129f, 23f, 58f, 1f, 0.25f, 0.02f, glow);
            }
            fill(batch, 35f, 133f, 11f, 46f, 0.12f, 0.06f, 0.14f, 1f);
            for (int row = 0; row < fillHeight; row++) {
                float heat = 1f - row / Math.max(1f, fillHeight);
                fill(batch, 35f, 133f + row, 11f, 1f, 1f, 0.25f + 0.65f * heat, 0.03f + 0.25f * heat, 1f);
            }
            if (fillHeight > 0f) {
                for (int flame = 0; flame < 4; flame++) {
                    float flicker = 0.5f + 0.5f * MathUtils.sin(time * 20f + flame * 2f);
                    fill(batch, 35f + flame * 3f, 133f + fillHeight, 2f,
                        2f + 6f * flicker, 1f, 0.5f + flicker * 0.4f, 0.08f, 0.9f);
                }
            }
            batch.setColor(1f, 0.6f, 0.2f, 1f);
            batch.draw(rageFrame, 8f, 124f, 48f, 128f);
            if (player.isRageReady()) {
                for (int ember = 0; ember < 7; ember++) {
                    float age = (time * 1.2f + ember * 0.147f) % 1f;
                    fill(batch, 30f + MathUtils.sin(ember * 2f + age * 5f) * 15f,
                        183f + age * 65f, 2f, 3f, 1f, 0.6f, 0.1f, 1f - age);
                }
            }
            batch.setColor(Color.WHITE);
            font.setColor(1f, 0.75f, 0.3f, 1f);
            font.draw(batch, player.isRageReady() ? "K READY" : "RAGE " + (int) player.getRage(), 12f, 116f);
            font.setColor(Color.WHITE);
        }
        fill(batch, 18f, arenaHeight - 39f, 180f, 7f, 0.15f, 0.12f, 0.18f, 1f);
        fill(batch, 18f, arenaHeight - 39f, 180f * player.getHealth() / player.getMaxHealth(), 7f,
            0.3f, 0.85f, 0.65f, 1f);
        fill(batch, arenaWidth - 218f, arenaHeight - 39f, 200f, 7f, 0.15f, 0.12f, 0.18f, 1f);
        fill(batch, arenaWidth - 218f, arenaHeight - 39f, 200f * boss.getHealth() / boss.getMaxHealth(), 7f,
            0.8f, 0.25f, 0.45f, 1f);
        batch.setColor(Color.WHITE);
        font.draw(batch, "YOU  " + player.getHealth() + "/" + player.getMaxHealth(), 18f, arenaHeight - 12f);
        font.draw(batch, "MR. MONSTER", arenaWidth - 218f, arenaHeight - 12f);
        font.draw(batch, "A/D move   W jump x2   J attack   SPACE dash   K rage", 18f, 24f);
        if (player.isBurning()) font.draw(batch, "BURNING", 18f, arenaHeight - 80f);
        String message = player.isDead() ? ""
            : endingTime >= 0f ? "The world fractures." : boss.getTelegraph();
        if (!message.isEmpty()) font.draw(batch, message, 18f, arenaHeight - 58f);
    }

    private void fill(SpriteBatch batch, float x, float y, float width, float height,
                      float r, float g, float b, float a) {
        batch.setColor(r, g, b, a);
        batch.draw(pixel, x, y, width, height);
    }

    public boolean isPlayerDead() { return player.isDead(); }
    public boolean isDeathReady() { return deathTime >= 0.8f; }

    @Override
    public boolean isFinished() { return finished; }

    @Override
    public void dispose() {
        Sound[] sounds = {punchHit1, punchHit2, heavyHit, waveLaunch, explosion, shieldImpact};
        for (int i = 0; i < sounds.length; i++) if (sounds[i] != null) sounds[i].stop();
        camera.position.set(cameraX, cameraY, 0f);
        // Main owns every texture and the font; entities hold no disposable resources.
    }
}
