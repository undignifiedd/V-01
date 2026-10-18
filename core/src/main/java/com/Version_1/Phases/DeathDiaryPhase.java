package com.Version_1.Phases;

import com.Version_1.Story.DialogueData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

/** A silent shadow rescue, followed by the diary page that returns to the title menu. */
public class DeathDiaryPhase implements Phase {
    private static final float SHADOW_DURATION = 5.8f;
    private static final float READ_TIME = 4f;
    private static final float PAGE_TURN_TIME = 0.7f;
    private final TextureRegion playerFrame, pixel;
    private final BitmapFont font;
    private final Viewport viewport;
    private final Rectangle book = new Rectangle(90f, 72f, 460f, 228f);
    private final Vector2 mouse = new Vector2();
    private float time, turnTime = -1f;

    public DeathDiaryPhase(TextureRegion playerFrame, TextureRegion pixel, BitmapFont font, Viewport viewport) {
        this.playerFrame = playerFrame;
        this.pixel = pixel;
        this.font = font;
        this.viewport = viewport;
    }

    @Override
    public void update(float delta) {
        delta = Math.min(delta, 0.05f);
        time += delta;
        if (turnTime >= 0f) { turnTime += delta; return; }
        if (time < SHADOW_DURATION + READ_TIME) return;
        boolean turn = Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER);
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            viewport.unproject(mouse.set(Gdx.input.getX(), Gdx.input.getY()));
            turn |= book.contains(mouse);
        }
        if (turn) turnTime = 0f;
    }

    @Override
    public void draw(SpriteBatch batch) {
        Color color = batch.getColor();
        float r = color.r, g = color.g, b = color.b, a = color.a;
        fill(batch, 0f, 0f, 640f, 360f, 0.008f, 0.005f, 0.015f, 1f);
        if (time < SHADOW_DURATION) drawShadow(batch);
        else drawDiary(batch);
        batch.setColor(r, g, b, a);
        font.setColor(Color.WHITE);
    }

    private void drawShadow(SpriteBatch batch) {
        fill(batch, 0f, 0f, 640f, 108f, 0.045f, 0.03f, 0.06f, 1f);
        fill(batch, 0f, 107f, 640f, 1f, 0.18f, 0.11f, 0.24f, 1f);
        float lift = MathUtils.clamp((time - 2f) / 2f, 0f, 1f);
        lift = lift * lift * (3f - 2f * lift);
        float bend = MathUtils.clamp((time - 1f) / 1f, 0f, 1f) * (1f - lift);
        float sink = MathUtils.clamp((time - 4.1f) / 0.8f, 0f, 1f);
        float bodyX = MathUtils.lerp(240f, 272f, lift);
        float shadowHeight = 96f * (1f - sink);
        for (int row = -3; row <= 3; row++) {
            float half = (float) Math.sqrt(Math.max(0f, 1f - row * row / 16f)) * 68f;
            fill(batch, 340f - half, 107f + row * 2f, half * 2f, 2f, 0.08f, 0.045f, 0.13f, 1f);
        }
        batch.setColor(0.18f, 0.10f, 0.27f, 1f - sink);
        batch.draw(playerFrame, 311f, 107f, 49f, 1f, 98f, shadowHeight + 2f, 1f, 1f, bend * 30f);
        batch.setColor(0.015f, 0.008f, 0.025f, 1f - sink);
        batch.draw(playerFrame, 312f, 108f, 48f, 0f, 96f, shadowHeight, 1f, 1f, bend * 30f);
        if (time >= 1.4f && time < 4.1f) {
            float shoulderX = 358f - bend * 20f, shoulderY = 170f - bend * 12f;
            float handX = bodyX + 62f, handY = MathUtils.lerp(126f, 167f, lift);
            float dx = handX - shoulderX, dy = handY - shoulderY;
            batch.setColor(0.12f, 0.07f, 0.19f, 1f - lift * 0.8f);
            batch.draw(pixel, shoulderX, shoulderY, 0f, 3f,
                (float) Math.sqrt(dx * dx + dy * dy), 6f, 1f, 1f, MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees);
        }
        batch.setColor(0.8f, 0.75f, 0.85f, 1f);
        batch.draw(playerFrame, bodyX, 108f, 48f, 0f, 96f, 96f, 1f, 1f, -90f * (1f - lift));
        float darkness = Math.max(1f - MathUtils.clamp(time / 0.65f, 0f, 1f),
            MathUtils.clamp((time - 4.9f) / 0.9f, 0f, 1f));
        fill(batch, 0f, 0f, 640f, 360f, 0f, 0f, 0f, darkness);
    }

    private void drawDiary(SpriteBatch batch) {
        float age = time - SHADOW_DURATION;
        float alpha = MathUtils.clamp(age / 0.8f, 0f, 1f);
        fill(batch, 82f, 64f, 476f, 240f, 0.16f, 0.075f, 0.055f, alpha);
        fill(batch, 90f, 72f, 224f, 228f, 0.79f, 0.72f, 0.57f, alpha);
        fill(batch, 326f, 72f, 224f, 228f, 0.85f, 0.78f, 0.63f, alpha);
        fill(batch, 314f, 72f, 12f, 228f, 0.30f, 0.21f, 0.15f, alpha);
        for (int y = 100; y < 280; y += 22) {
            fill(batch, 110f, y, 186f, 1f, 0.5f, 0.4f, 0.3f, alpha * 0.18f);
            fill(batch, 344f, y, 186f, 1f, 0.5f, 0.4f, 0.3f, alpha * 0.18f);
        }
        batch.setColor(Color.WHITE);
        font.setColor(0.17f, 0.10f, 0.075f, MathUtils.clamp((age - 0.8f) / 0.7f, 0f, 1f));
        font.draw(batch, DialogueData.DEATH_DIARY[0], 126f, 226f);
        font.setColor(0.17f, 0.10f, 0.075f, MathUtils.clamp((age - 2.1f) / 0.7f, 0f, 1f));
        font.draw(batch, DialogueData.DEATH_DIARY[1], 346f, 204f);
        if (age >= READ_TIME && turnTime < 0f) {
            font.setColor(0.8f, 0.73f, 0.6f, 1f);
            font.draw(batch, "E / ENTER / click the diary - turn the page", 158f, 44f);
            font.draw(batch, "Return to the main menu", 231f, 23f);
        }
        if (turnTime >= 0f) {
            float progress = MathUtils.clamp(turnTime / PAGE_TURN_TIME, 0f, 1f);
            float pageWidth = Math.abs(MathUtils.cos(progress * MathUtils.PI)) * 224f;
            float x = progress < 0.5f ? 326f : 314f - pageWidth;
            fill(batch, x, 72f, pageWidth, 228f, 0.87f, 0.81f, 0.68f, 1f);
            fill(batch, 0f, 0f, 640f, 360f, 0f, 0f, 0f, MathUtils.clamp((progress - 0.5f) * 2f, 0f, 1f));
        }
    }

    private void fill(SpriteBatch batch, float x, float y, float w, float h, float r, float g, float b, float a) {
        batch.setColor(r, g, b, a);
        batch.draw(pixel, x, y, w, h);
    }

    @Override public boolean isFinished() { return turnTime >= PAGE_TURN_TIME; }
    @Override public void dispose() { /* Main owns all resources. */ }
}
