package com.Version_1.Phases;

import com.Version_1.Entities.NPC;
import com.Version_1.Entities.Player;
import com.Version_1.UI.DialogueBox;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Quiet destination after the shield launch. Flowers reuse the same readable-NPC contract. */
public class FlowerRoomPhase extends ExplorePhase {
    private boolean finalDialogueRead, finished;
    private float endingTime = -1f;
    private final GlyphLayout endingLayout = new GlyphLayout();
    public FlowerRoomPhase(Player player, DialogueBox dialogue, BitmapFont font, TextureRegion pixel,
                           TextureRegion flower, String[] text, float width, float height) {
        super(player, dialogue, font, pixel, width, height);
        setRoomSize(width, height);
        player.getPosition().set(width / 2f - 32f, height / 2f - 30f);
        for (int row = -2; row <= 2; row++) {
            for (int column = -4; column <= 4; column++) {
                if (column * column / 20f + row * row / 6f > 1f) continue;
                float x = width / 2f - 6f + column * 17f + row % 2 * 3f;
                float y = height / 2f - 16f + row * 15f + (column + 4) % 3 * 2f;
                NPC bloom = new NPC(x, y, 12f, 20f, flower, "Flowers", text, dialogue);
                bloom.setOnRead(() -> finalDialogueRead = true);
                addNPC(bloom);
            }
        }
        world.update(0f);
    }

    @Override
    public void update(float delta) {
        if (endingTime >= 0f) {
            endingTime += Math.min(delta, 0.05f);
            if (endingTime >= 3.5f && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                    || Gdx.input.isKeyJustPressed(Input.Keys.E))) finished = true;
            return;
        }
        super.update(delta);
        if (finalDialogueRead && !dialogue.isOpen()) {
            player.stopMovement();
            endingTime = 0f;
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        super.draw(batch);
        if (endingTime < 0f) return;
        Color previous = new Color(batch.getColor());
        Color previousFont = new Color(font.getColor());
        batch.setColor(0f, 0f, 0f, Math.min(1f, endingTime / 1.2f));
        batch.draw(pixel, 0f, 0f, width, height);
        float alpha = Math.max(0f, Math.min(1f, (endingTime - 1.2f) / 0.8f));
        batch.setColor(Color.WHITE);
        font.setColor(0.95f, 0.8f, 0.6f, alpha);
        endingLayout.setText(font, "To be continued...");
        font.draw(batch, endingLayout, (width - endingLayout.width) / 2f, height / 2f + 12f);
        if (endingTime >= 3.5f) {
            font.setColor(0.65f, 0.63f, 0.7f, 1f);
            endingLayout.setText(font, "E / ENTER - Main menu");
            font.draw(batch, endingLayout, (width - endingLayout.width) / 2f, height / 2f - 32f);
        }
        font.setColor(previousFont);
        batch.setColor(previous);
    }

    @Override
    protected boolean shouldDrawPrompts() { return endingTime < 0f; }

    @Override
    public boolean isFinished() { return finished; }
}
