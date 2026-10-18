package com.Version_1.Entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.Version_1.UI.DialogueBox;

/** A stationary speaker or readable prop. A diary is simply an NPC with book art. */
public class NPC extends Entity implements Interactable {
    private final Animation<TextureRegion> animation;
    private final DialogueBox dialogueBox;
    private final String name;
    private String[] dialogue;
    private boolean read;
    private boolean visible = true;
    private float animationTime;
    private Runnable onRead;
    private Runnable onInteract;

    public NPC(float x, float y, float width, float height, TextureRegion sprite,
               String name, String[] dialogue, DialogueBox dialogueBox) {
        this(x, y, width, height, new Animation<>(0.22f, sprite), name, dialogue, dialogueBox);
    }

    public NPC(float x, float y, float width, float height, Animation<TextureRegion> animation,
               String name, String[] dialogue, DialogueBox dialogueBox) {
        super(x, y, width, height);
        if (dialogue == null || dialogue.length == 0) throw new IllegalArgumentException("NPC needs dialogue");
        this.animation = animation;
        this.name = name;
        this.dialogue = dialogue.clone();
        this.dialogueBox = dialogueBox;
    }

    @Override
    public void update(float delta) {
        animationTime += delta;
    }

    @Override
    public void draw(SpriteBatch batch) {
        if (visible) batch.draw(animation.getKeyFrame(animationTime), position.x, position.y, width, height);
    }

    @Override
    public void interact() {
        if (!isRemoved() && visible && onInteract != null) { onInteract.run(); return; }
        if (!isRemoved() && visible) dialogueBox.show(name, dialogue, () -> {
            read = true;
            if (onRead != null) onRead.run();
        });
    }

    public String getName() { return name; }
    public boolean hasBeenRead() { return read; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public void setOnRead(Runnable onRead) { this.onRead = onRead; }
    public void setOnInteract(Runnable onInteract) { this.onInteract = onInteract; }
    public void resetPose() { animationTime = 0f; }
    public void setDialogue(String[] dialogue) {
        if (dialogue == null || dialogue.length == 0) throw new IllegalArgumentException("NPC needs dialogue");
        this.dialogue = dialogue.clone();
    }
}
