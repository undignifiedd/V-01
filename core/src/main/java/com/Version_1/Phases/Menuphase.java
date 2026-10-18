package com.Version_1.Phases;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Title menu. Shared artwork/font belong to Main; input uses the game's fitted
 * viewport.
 */
public class MenuPhase implements Phase {
    private enum Panel {
        MAIN, CREDITS, EXIT
    }

    private final TextureRegion background, exitQuestion, pixel;
    private final TextureRegion[] labels;
    private final BitmapFont font;
    private final Viewport viewport;
    private final Rectangle[] choices = new Rectangle[3];
    private final Rectangle back = new Rectangle(64f, 36f, 160f, 32f);
    private final Rectangle yes = new Rectangle(250f, 145f, 62f, 32f);
    private final Rectangle no = new Rectangle(336f, 145f, 54f, 32f);
    private final Vector2 mouse = new Vector2();
    private Panel panel = Panel.MAIN;
    private int selected;
    private boolean exitYes, finished;
    private boolean retryAvailable;
    private int lastMouseX = -1, lastMouseY = -1;
    private float time;

    public MenuPhase(TextureRegion background, TextureRegion[] labels, TextureRegion exitQuestion,
            TextureRegion pixel, BitmapFont font, Viewport viewport) {
        this.background = background;
        this.labels = labels;
        this.exitQuestion = exitQuestion;
        this.pixel = pixel;
        this.font = font;
        this.viewport = viewport;
        for (int i = 0; i < choices.length; i++)
            choices[i] = new Rectangle(60f, 209f - i * 48f, 300f, 38f);
    }

    @Override
    public void update(float delta) {
        time += Math.min(delta, 0.05f);
        int mouseX = Gdx.input.getX(), mouseY = Gdx.input.getY();
        boolean moved = mouseX != lastMouseX || mouseY != lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        viewport.unproject(mouse.set(mouseX, mouseY));
        boolean click = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
        boolean enter = Gdx.input.isKeyJustPressed(Input.Keys.ENTER);
        boolean escape = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);
        if (panel == Panel.MAIN) {
            if (key(Input.Keys.UP, Input.Keys.W))
                selected = (selected + choices.length - 1) % choices.length;
            if (key(Input.Keys.DOWN, Input.Keys.S))
                selected = (selected + 1) % choices.length;
            for (int i = 0; i < choices.length; i++) {
                if (choices[i].contains(mouse) && (moved || click))
                    selected = i;
                if (click && choices[i].contains(mouse)) {
                    activate();
                    return;
                }
            }
            if (escape) {
                panel = Panel.EXIT;
                exitYes = false;
            } else if (enter)
                activate();
        } else if (panel == Panel.EXIT) {
            if (escape || Gdx.input.isKeyJustPressed(Input.Keys.N)) {
                panel = Panel.MAIN;
                return;
            }
            if (key(Input.Keys.LEFT, Input.Keys.A))
                exitYes = true;
            if (key(Input.Keys.RIGHT, Input.Keys.D))
                exitYes = false;
            if (moved) {
                if (yes.contains(mouse))
                    exitYes = true;
                else if (no.contains(mouse))
                    exitYes = false;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.Y) || (click && yes.contains(mouse)) || (enter && exitYes))
                Gdx.app.exit();
            else if ((click && no.contains(mouse)) || (enter && !exitYes))
                panel = Panel.MAIN;
        } else {
            if (escape || (click && back.contains(mouse))) {
                panel = Panel.MAIN;
                return;
            }
        }
    }

    private boolean key(int first, int second) {
        return Gdx.input.isKeyJustPressed(first) || Gdx.input.isKeyJustPressed(second);
    }

    public void setRetryAvailable(boolean available) {
        retryAvailable = available;
    }

    private void activate() {
        if (selected == 0)
            finished = true;
        else if (selected == 1)
            panel = Panel.CREDITS;
        else {
            panel = Panel.EXIT;
            exitYes = false;
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        Color old = batch.getColor();
        float r = old.r, g = old.g, b = old.b, a = old.a;
        batch.setColor(Color.WHITE);
        batch.draw(background, 0f, 0f, 640f, 360f);
        shade(batch, 0f, 0f, 640f, 360f, 0.32f);
        font.setColor(Color.WHITE);
        font.getData().setScale(2f);
        font.draw(batch, "THE OTHER HALF", 64f, 322f);
        font.getData().setScale(1f);
        font.setColor(0.75f, 0.72f, 0.64f, 1f);
        font.draw(batch, "DEMO", 66f, 284f);
        if (panel == Panel.MAIN || panel == Panel.EXIT) {
            for (int i = 0; i < choices.length; i++) {
                Rectangle box = choices[i];
                boolean active = i == selected;
                if (active) {
                    batch.setColor(0.85f, 0.75f, 0.3f, 0.10f + 0.04f * (float) Math.sin(time * 3f));
                    batch.draw(pixel, box.x - 8f, box.y, box.width, box.height);
                }
                batch.setColor(active ? 1f : 0.55f, active ? 1f : 0.55f, active ? 0.8f : 0.5f, 1f);
                if (i == 0 && retryAvailable) {
                    font.setColor(active ? 1f : 0.55f, active ? 1f : 0.55f, active ? 0.8f : 0.5f, 1f);
                    font.getData().setScale(1.5f);
                    font.draw(batch, "TRY AGAIN", 92f, box.y + 30f);
                    font.getData().setScale(1f);
                } else {
                    TextureRegion label = labels[i];
                    batch.draw(label, 92f, box.y + 4f, label.getRegionWidth() * 0.62f, 30f);
                }
                if (active) {
                    font.setColor(1f, 0.92f, 0.5f, 1f);
                    font.draw(batch, ">>", 64f, box.y + 26f);
                }
            }
            font.setColor(0.75f, 0.72f, 0.64f, 1f);
            font.draw(batch, "ARROWS / W-S   ENTER select   Mouse", 64f, 30f);
        } else {
            font.setColor(Color.WHITE);
            font.draw(batch, "CREDITS", 64f, 247f);
            font.draw(batch, "Special thanks to my teammates", 64f, 213f);
            font.draw(batch, "Muntaha, Yusra and Adeen", 64f, 189f);
            font.draw(batch, "Especially Adeen. (Head of creative Direction) ", 64f, 165f);
            font.draw(batch, "Combat sound assets (CC0)", 64f, 125f);
            font.draw(batch, "Kenney, Julien Matthey, EZduzziteh", 64f, 103f);
            font.draw(batch, "Thank you for playing.", 64f, 79f);
            drawBack(batch);
        }
        if (panel == Panel.EXIT) {
            shade(batch, 0f, 0f, 640f, 360f, 0.8f);
            batch.setColor(Color.WHITE);
            batch.draw(exitQuestion, 216f, 140f, 208f, 80f);
            Rectangle choice = exitYes ? yes : no;
            batch.setColor(1f, 0.9f, 0.3f, 1f);
            batch.draw(pixel, choice.x, choice.y - 2f, choice.width, 2f);
            font.setColor(Color.WHITE);
            font.draw(batch, "LEFT / RIGHT   ENTER   Y / N   ESC cancel", 145f, 112f);
        }
        batch.setColor(r, g, b, a);
        font.setColor(Color.WHITE);
    }

    private void drawBack(SpriteBatch batch) {
        font.setColor(1f, 0.92f, 0.5f, 1f);
        font.draw(batch, "< Back / ESC", back.x, back.y + 22f);
    }

    private void shade(SpriteBatch batch, float x, float y, float w, float h, float alpha) {
        batch.setColor(0f, 0f, 0f, alpha);
        batch.draw(pixel, x, y, w, h);
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void dispose() {
        /* Shared assets belong to Main. */ }
}
