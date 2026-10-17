package com.Version_1.Entities;

import com.badlogic.gdx.math.Rectangle;

public interface Attackable {

    void takeDamage(int amount);

    boolean isDead();

    Rectangle getBounds();
}
