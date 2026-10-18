package com.Version_1.Phases;

import com.Version_1.Entities.NPC;
import com.Version_1.Entities.Player;
import com.Version_1.UI.DialogueBox;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

/** First waking room. Room furniture is drawn from a shared pixel; diary is an ordinary NPC. */
public class RealWorldPhase extends ExplorePhase {
    public RealWorldPhase(Player player, DialogueBox dialogue, BitmapFont font, TextureRegion pixel,
                          TextureRegion diarySprite, String[] diaryText, float width, float height) {
        super(player, dialogue, font, pixel, width, height);
        player.getPosition().set(288f, 160f);
        NPC diary = new NPC(310f, 252f, 24f, 20f, diarySprite, "Diary", diaryText, dialogue);
        addNPC(diary);
        addObstacle(new Rectangle(260f, 240f, 130f, 30f));
        addObstacle(new Rectangle(60f, 190f, 105f, 85f));
        finishWhenRead(diary);
        world.update(0f);
    }

    @Override
    protected void drawRoom(SpriteBatch batch) {
        fill(batch, 0f, 0f, width, height, 0.10f, 0.09f, 0.10f);
        fill(batch, 24f, 140f, width - 48f, height - 184f, 0.48f, 0.39f, 0.30f);
        for (int y = 140; y < 300; y += 20) {
            fill(batch, 24f, y, width - 48f, 1f, 0.36f, 0.28f, 0.22f);
        }
        fill(batch, 24f, 294f, width - 48f, 42f, 0.65f, 0.60f, 0.52f);
        fill(batch, 460f, 300f, 72f, 28f, 0.20f, 0.19f, 0.22f);
        fill(batch, 464f, 304f, 64f, 20f, 0.55f, 0.69f, 0.73f);
        fill(batch, 494f, 304f, 3f, 20f, 0.20f, 0.19f, 0.22f);
        // Bed, pillow, desk and legs: no external room textures required.
        fill(batch, 60f, 190f, 105f, 85f, 0.22f, 0.17f, 0.16f);
        fill(batch, 65f, 197f, 95f, 70f, 0.28f, 0.42f, 0.43f);
        fill(batch, 70f, 247f, 85f, 17f, 0.85f, 0.82f, 0.71f);
        fill(batch, 266f, 230f, 8f, 14f, 0.22f, 0.15f, 0.12f);
        fill(batch, 376f, 230f, 8f, 14f, 0.22f, 0.15f, 0.12f);
        fill(batch, 260f, 240f, 130f, 30f, 0.34f, 0.22f, 0.15f);
        fill(batch, 260f, 266f, 130f, 4f, 0.54f, 0.38f, 0.25f);
    }
}
