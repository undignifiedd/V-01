package com.Version_1.Phases;

import com.Version_1.Entities.NPC;
import com.Version_1.Entities.Player;
import com.Version_1.UI.DialogueBox;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Quiet destination after the shield launch. Flowers reuse the same readable-NPC contract. */
public class FlowerRoomPhase extends ExplorePhase {
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
                addNPC(new NPC(x, y, 12f, 20f, flower, "Flowers", text, dialogue));
            }
        }
        world.update(0f);
    }
}
