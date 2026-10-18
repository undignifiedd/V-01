package com.Version_1.Phases;

import com.Version_1.Entities.NPC;
import com.Version_1.Entities.Player;
import com.Version_1.Story.DialogueData;
import com.Version_1.UI.DialogueBox;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/** The three-NPC flower request and its timed, noninteractive monster reveal. */
public class FlowerQuestPhase extends ExplorePhase {
    public enum QuestState { NOT_ACCEPTED, CARRYING, REVEAL, COMPLETE }
    private static final float REPOSITION_TIME = 3.85f;
    private static final float SWAY_TIME = 4.4f;
    private static final float SMILE_TIME = 6f;
    private static final float BURN_TIME = 10.7f;
    private static final float FROWN_TIME = 12.7f;
    public static final float FROWN_GROW_TIME = 2.8f;
    private static final float BLINK_TIME = FROWN_TIME + FROWN_GROW_TIME + 1.4f;
    private static final float END_TIME = BLINK_TIME + 3.2f;
    private final NPC bob, jack, lady;
    private final Animation<TextureRegion> smileAnimation;
    private final Animation<TextureRegion> bobFrownAnimation;
    private final OrthographicCamera shotCamera = new OrthographicCamera();
    private final Matrix4 previousProjection = new Matrix4();
    private QuestState quest = QuestState.NOT_ACCEPTED;
    private float revealTime;
    private float time;
    private boolean repositioned;
    private Animation<TextureRegion> arenaEye;

    public FlowerQuestPhase(Player player, DialogueBox dialogue, BitmapFont font, TextureRegion pixel,
                            Array<Animation<TextureRegion>> npcAnimations,
                            Animation<TextureRegion> smileAnimation,
                            Animation<TextureRegion> bobFrownAnimation, float width, float height) {
        super(player, dialogue, font, pixel, width, height);
        this.smileAnimation = smileAnimation;
        this.bobFrownAnimation = bobFrownAnimation;
        setRoomSize(width, height);
        setReturnExit(new Rectangle(36f, height / 2f - 70f, 40f, 140f));
        bob = new NPC(180f, 130f, 64f, 64f, npcAnimations.get(0),
            DialogueData.NPC_NAMES[0], DialogueData.NPC_DIALOGUE[0], dialogue);
        jack = new NPC(350f, 250f, 64f, 64f, npcAnimations.get(1),
            DialogueData.NPC_NAMES[1], DialogueData.NPC_DIALOGUE[1], dialogue);
        lady = new NPC(500f, 155f, 64f, 64f, npcAnimations.get(2),
            DialogueData.NPC_NAMES[2], DialogueData.NPC_DIALOGUE[2], dialogue);
        lady.setOnRead(() -> {
            if (quest == QuestState.NOT_ACCEPTED) {
                dialogue.ask(DialogueData.FLOWER_QUESTION, () -> {
                    quest = QuestState.CARRYING;
                    jack.setDialogue(DialogueData.JACK_FLOWER);
                    bob.setOnInteract(this::beginReveal);
                }, null);
            }
        });
        addNPC(bob);
        addNPC(jack);
        addNPC(lady);
        world.update(0f);
        shotCamera.setToOrtho(false, width, height);
    }

    public QuestState getQuestState() { return quest; }
    public float getRevealTime() { return revealTime; }

    @Override
    public void update(float delta) {
        delta = Math.min(delta, 0.05f);
        time += delta;
        if (quest == QuestState.COMPLETE) return;
        if (quest == QuestState.REVEAL) {
            revealTime += delta;
            if (revealTime >= 0.2f) {
                jack.setVisible(false);
                lady.setVisible(false);
            }
            if (!repositioned && revealTime >= REPOSITION_TIME) {
                repositioned = true;
                player.getPosition().set(252f, 120f);
                bob.getPosition().set(342f, 120f);
            }
            if (revealTime >= END_TIME) quest = QuestState.COMPLETE;
            return;
        }
        super.update(delta);
    }

    private void beginReveal() {
        quest = QuestState.REVEAL;
        player.startCinematic();
        bob.resetPose();
    }

    @Override
    public void setRoomScenery(TextureRegion floor, TextureRegion tree, Animation<TextureRegion> eye) {
        super.setRoomScenery(floor, tree, eye);
        arenaEye = eye;
    }

    @Override
    protected boolean shouldDrawPrompts() { return quest != QuestState.REVEAL; }

    @Override
    public boolean isFinished() { return quest == QuestState.COMPLETE || super.isFinished(); }

    @Override
    public void draw(SpriteBatch batch) {
        if (quest != QuestState.REVEAL && quest != QuestState.COMPLETE) {
            super.draw(batch);
            if (quest == QuestState.CARRYING) {
                drawFlower(batch, player.getPosition().x + 47f, player.getPosition().y + 27f, -1f);
            }
            return;
        }
        Color old = batch.getColor();
        float r = old.r, g = old.g, b = old.b, a = old.a;
        if (revealTime < 1.9f) {
            super.draw(batch);
            drawBlinks(batch);
            batch.setColor(r, g, b, a);
            return;
        }
        previousProjection.set(batch.getProjectionMatrix());
        if (revealTime >= BLINK_TIME) drawArenaPreview(batch);
        else {
            setShotCamera();
            batch.setProjectionMatrix(shotCamera.combined);
            drawRoom(batch);
            shade(batch, 0f, 0f, width, height, 0f, 0f, 0f, 0.94f);
            drawCandle(batch);
            if (revealTime >= REPOSITION_TIME) {
                batch.setColor(0.9f, 0.75f, 0.56f, 1f);
                drawPlayerExpression(batch);
                drawBobExpression(batch);
                batch.setColor(Color.WHITE);
                if (revealTime < FROWN_TIME) {
                    drawFlower(batch, 326f, 164f + MathUtils.sin(time * 2f) * 2f,
                        revealTime < BURN_TIME ? -1f : revealTime - BURN_TIME);
                }
            }
        }
        batch.setProjectionMatrix(previousProjection);
        drawBlinks(batch);
        batch.setColor(r, g, b, a);
    }

    private void setShotCamera() {
        float x = width / 2f, y = 180f, zoom = 0.72f;
        if (revealTime >= SMILE_TIME && revealTime < BURN_TIME) {
            float approach = MathUtils.clamp((revealTime - SMILE_TIME) / 1.2f, 0f, 1f);
            x = MathUtils.lerp(x, 284f, approach);
            y = MathUtils.lerp(y, 171f, approach);
            zoom = MathUtils.lerp(zoom, 0.32f, approach);
        } else if (revealTime >= BURN_TIME && revealTime < BURN_TIME + 0.4f) {
            float retreat = (revealTime - BURN_TIME) / 0.4f;
            x = MathUtils.lerp(284f, x, retreat);
            y = MathUtils.lerp(171f, y, retreat);
            zoom = MathUtils.lerp(0.32f, zoom, retreat);
        } else if (revealTime >= FROWN_TIME && revealTime < BLINK_TIME) {
            float approach = MathUtils.clamp((revealTime - FROWN_TIME) / 0.9f, 0f, 1f);
            x = MathUtils.lerp(x, 374f, approach);
            y = MathUtils.lerp(y, 171f, approach);
            zoom = MathUtils.lerp(zoom, 0.32f, approach);
        } else if (revealTime >= BLINK_TIME) zoom = 1f;
        shotCamera.position.set(x, y, 0f);
        shotCamera.zoom = zoom;
        shotCamera.update();
    }

    private void drawCandle(SpriteBatch batch) {
        float swingAge = Math.max(0f, revealTime - SWAY_TIME);
        float swing = MathUtils.sin(swingAge * 1.4f) * Math.min(4f, swingAge * 2f);
        float x = width / 2f + swing;
        for (int y = 280; y < height; y += 5) {
            float chainX = width / 2f + swing * (height - y) / (height - 280f);
            fill(batch, chainX, y, 1f, 3f, 0.28f, 0.24f, 0.20f);
        }
        float flicker = 0.8f + 0.2f * MathUtils.sin(time * 19f);
        for (int y = 104; y < 260; y += 6) {
            float halfWidth = 12f + (260f - y) * 0.65f;
            shade(batch, x - halfWidth, y, halfWidth * 2f, 6f, 1f, 0.65f, 0.2f, 0.045f * flicker);
        }
        fill(batch, x - 11f, 256f, 22f, 3f, 0.32f, 0.26f, 0.19f);
        fill(batch, x - 3f, 259f, 6f, 15f, 0.85f, 0.77f, 0.57f);
        fill(batch, x - 1f, 274f, 2f, 3f, 0.18f, 0.10f, 0.04f);
        shade(batch, x - 3f, 276f, 6f, 9f * flicker, 1f, 0.36f, 0.03f, 0.85f);
        fill(batch, x - 1f, 277f, 2f, 5f * flicker, 1f, 0.9f, 0.5f);
    }

    private void drawPlayerExpression(SpriteBatch batch) {
        TextureRegion frame = smileAnimation.getKeyFrame(Math.max(0f, revealTime - SMILE_TIME));
        // Complete raster frames include the redrawn face; no separate mouth/eye overlay.
        // Source faces left. Face Bob on the right while keeping the original 64-unit body size.
        batch.draw(frame, player.getPosition().x + 64f, player.getPosition().y, -64f, 64f);
    }

    private void drawBobExpression(SpriteBatch batch) {
        TextureRegion frame = bobFrownAnimation.getKeyFrame(Math.max(0f, revealTime - FROWN_TIME));
        // Bob's complete sprite frames already contain the facial changes; source faces the player.
        batch.draw(frame, bob.getPosition().x, bob.getPosition().y, 64f, 64f);
    }

    private void drawArenaPreview(SpriteBatch batch) {
        fill(batch, 0f, 0f, width, height, 0f, 0f, 0f);
        batch.setColor(0.55f, 0.3f, 0.6f, 0.6f);
        batch.draw(arenaEye.getKeyFrame(time), width / 2f - 80f, 170f, 160f, 160f);
        fill(batch, 0f, 0f, width, 60f, 0.075f, 0.055f, 0.1f);
        fill(batch, 0f, 59f, width, 1f, 0.35f, 0.23f, 0.38f);
        batch.setColor(Color.WHITE);
        batch.draw(player.getCurrentFrame(), width / 2f - 32f, 60f, 64f, 64f);
    }

    private void drawBlinks(SpriteBatch batch) {
        blink(batch, 0f, 0.65f, 0.5f);
        blink(batch, 1.65f, 0.65f, 0.5f);
        blink(batch, 3.6f, 0.8f, 0.5f);
        if (revealTime >= BLINK_TIME) {
            // Two whole-frame glimpses separated by black, with no moving eyelid masks.
            float age = revealTime - BLINK_TIME;
            boolean firstGlimpse = age >= 1.2f && age < 1.32f;
            boolean secondGlimpse = age >= 1.77f && age < 2.02f;
            if (!firstGlimpse && !secondGlimpse) {
                shade(batch, 0f, 0f, width, height, 0f, 0f, 0f, 1f);
            }
        }
    }

    private void blink(SpriteBatch batch, float start, float duration, float coverage) {
        float age = revealTime - start;
        if (age < 0f || age > duration) return;
        float closure = Math.min(MathUtils.clamp(age / 0.16f, 0f, 1f),
            MathUtils.clamp((duration - age) / 0.2f, 0f, 1f));
        float lid = height * coverage * closure;
        shade(batch, 0f, 0f, width, lid, 0f, 0f, 0f, 1f);
        shade(batch, 0f, height - lid, width, lid, 0f, 0f, 0f, 1f);
    }

    private void drawFlower(SpriteBatch batch, float x, float y, float burn) {
        if (burn < 1.15f) {
            fill(batch, x, y - 13f, 1f, 14f, 0.3f, 0.6f, 0.25f);
            fill(batch, x + 1f, y - 8f, 3f, 2f, 0.3f, 0.6f, 0.25f);
            fill(batch, x - 3f, y - 1f, 7f, 3f, 0.9f, 0.35f, 0.52f);
            fill(batch, x - 1f, y - 3f, 3f, 7f, 0.95f, 0.48f, 0.63f);
            fill(batch, x, y, 1f, 1f, 1f, 0.85f, 0.35f);
        }
        if (burn >= 0f) {
            for (int i = 0; i < 7; i++) {
                float flame = 8f + 6f * MathUtils.sin(time * 20f + i);
                shade(batch, x - 5f + i * 1.5f, y - 10f, 2f, flame + 8f,
                    1f, 0.2f + i * 0.06f, 0.02f, Math.max(0f, 0.9f - burn * 0.25f));
            }
            for (int i = 0; i < 12; i++) {
                float age = (burn + i * 0.11f) % 1f;
                shade(batch, x + MathUtils.sin(i * 2f + burn * 3f) * (3f + age * 12f),
                    y + age * 30f, 1f, 2f, 1f, 0.55f, 0.1f, 1f - age);
            }
        }
    }

    private void shade(SpriteBatch batch, float x, float y, float w, float h,
                       float r, float g, float b, float a) {
        batch.setColor(r, g, b, MathUtils.clamp(a, 0f, 1f));
        batch.draw(pixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }
}
