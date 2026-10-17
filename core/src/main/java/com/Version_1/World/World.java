package com.Version_1.World;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.Version_1.Entities.Entity;

/**
 * Owns all live entities. Spawns are queued and added AFTER the update pass,
 * so a boss can summon mobs / fire projectiles mid-update without
 * mutating the list being iterated.
 */
public class World {
    private final Array<Entity> entities = new Array<>();
    private final Array<Entity> pending = new Array<>();

    /** Safe to call from inside any entity's update(). Entity appears next frame. */
    public void spawn(Entity e) {
        pending.add(e);
    }

    public void update(float delta) {
        // Index loops on purpose: libGDX Array's for-each iterator is not nestable.
        for (int i = 0; i < entities.size; i++) {
            entities.get(i).update(delta);
        }

        for (int i = entities.size - 1; i >= 0; i--) {
            if (entities.get(i).isRemoved()) {
                entities.removeIndex(i);
            }
        }

        for (int i = 0; i < pending.size; i++) {
            entities.add(pending.get(i));
        }
        pending.clear();
    }

    public void draw(SpriteBatch batch) {
        for (int i = 0; i < entities.size; i++) {
            entities.get(i).draw(batch);
        }
    }

    /** Read-only use please. Iterate with an index loop, and never add/remove directly. */
    public Array<Entity> getEntities() {
        return entities;
    }
}
