package com.Version_1.Phases;

import com.Version_1.Main;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * "Haunted terminal" main menu for The Better Half.
 * Keys: W/S or UP/DOWN to move, ENTER/SPACE to choose. Mouse hover and click
 * also work.
 * PLAY ends this phase (Main then moves to the next one). QUIT closes the game.
 * No image files needed: everything is drawn with a font and a generated 1x1
 * pixel.
 */
public class Menuphase implements Phase {

    private static final String TITLE = "THE BETTER HALF";
    private static final String[] OPTIONS = { "[ PLAY ]", "[ QUIT ]" };
    private static final String[] BOOT_LINES = { "> BOOT THE_BETTER_HALF.EXE", "> HOST NOT FOUND" };

    // Text sizes (the default font is small, so we scale it up).
    private static final float BOOT_SCALE = 1.2f;
    private static final float TITLE_SCALE = 3.2f;
    private static final float OPTION_SCALE = 2f;

    // Layout in world units (screen is 640 x 360). Y is the TOP of the text.
    private static final float TITLE_TOP_Y = 250f;
    private static final float OPTION_TOP_Y = 150f;
    private static final float OPTION_GAP = 50f;
    private static final float CHARS_PER_SEC = 30f; // boot text typing speed

    // Palette: near-black green, bright green, dim green, blood red.
    private static final Color BG = new Color(0.03f, 0.05f, 0.03f, 1f);
    private static final Color GREEN = new Color(0.29f, 0.88f, 0.42f, 1f);
    private static final Color DIM = new Color(0.15f, 0.44f, 0.23f, 1f);
    private static final Color RED = new Color(0.63f, 0.06f, 0.09f, 1f);

    private final Viewport viewport;
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();
    private final Texture pixel; // 1x1 white square, tinted when drawn
    private final Vector2 mouse = new Vector2();

    private final float[] optionX = new float[OPTIONS.length];
    private final float[] optionW = new float[OPTIONS.length];
    private final float[] optionH = new float[OPTIONS.length];
    private final Rectangle[] hitBox = new Rectangle[OPTIONS.length]; // clickable areas

    private int selected = 0;
    private boolean finished = false;
    private float time = 0f;
    private float glitchTimer = 3f; // countdown to the next glitch
    private float glitchLeft = 0f; // how long the current glitch lasts
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    public Menuphase(Viewport viewport) {
        this.viewport = viewport;

        // Make a 1x1 white texture in code so we can draw rectangles with the batch.
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        // Nearest keeps the scaled-up font crunchy instead of blurry.
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Measure each option once so we know where to centre it and where to click.
        font.getData().setScale(OPTION_SCALE);
        for (int i = 0; i < OPTIONS.length; i++) {
            layout.setText(font, OPTIONS[i]);
            optionW[i] = layout.width;
            optionH[i] = layout.height;
            optionX[i] = (Main.VIEW_W - layout.width) / 2f;
            hitBox[i] = new Rectangle(optionX[i] - 12f, optionTopY(i) - layout.height - 8f,
                    layout.width + 24f, layout.height + 16f);
        }
    }

    private float optionTopY(int i) {
        return OPTION_TOP_Y - i * OPTION_GAP;
    }

    @Override
    public void update(float delta) {
        time += delta;

        // Random short glitch every few seconds.
        glitchTimer -= delta;
        if (glitchTimer <= 0f) {
            glitchLeft = 0.15f;
            glitchTimer = MathUtils.random(2f, 5f);
        }
        if (glitchLeft > 0f) {
            glitchLeft -= delta;
        }

        // Keyboard.
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selected = (selected + OPTIONS.length - 1) % OPTIONS.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selected = (selected + 1) % OPTIONS.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            activate(selected);
        }

        // Mouse. Selection only follows the mouse when it actually moves,
        // so a resting mouse doesn't fight the keyboard.
        int rawX = Gdx.input.getX();
        int rawY = Gdx.input.getY();
        boolean moved = rawX != lastMouseX || rawY != lastMouseY;
        lastMouseX = rawX;
        lastMouseY = rawY;

        mouse.set(rawX, rawY);
        viewport.unproject(mouse); // screen pixels -> world units

        for (int i = 0; i < OPTIONS.length; i++) {
            if (hitBox[i].contains(mouse)) {
                if (moved) {
                    selected = i;
                }
                if (Gdx.input.justTouched()) {
                    selected = i;
                    activate(i);
                }
            }
        }
    }

    private void activate(int index) {
        if (index == 0) {
            finished = true; // PLAY
        } else {
            Gdx.app.exit(); // QUIT
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        // Background.
        batch.setColor(BG);
        batch.draw(pixel, 0, 0, Main.VIEW_W, Main.VIEW_H);
        batch.setColor(Color.WHITE);

        drawBootText(batch);
        drawTitle(batch);
        drawOptions(batch);

        // Scanlines: a thin dark line every 4 units.
        batch.setColor(0f, 0f, 0f, 0.35f);
        for (float y = 0f; y < Main.VIEW_H; y += 4f) {
            batch.draw(pixel, 0, y, Main.VIEW_W, 1f);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawBootText(SpriteBatch batch) {
        font.getData().setScale(BOOT_SCALE);
        font.setColor(DIM);
        float start = 0.3f; // when this line starts typing
        float y = Main.VIEW_H - 20f;
        for (String line : BOOT_LINES) {
            int count = MathUtils.clamp((int) ((time - start) * CHARS_PER_SEC), 0, line.length());
            if (count > 0) {
                font.draw(batch, line.substring(0, count), 24f, y);
            }
            start += line.length() / CHARS_PER_SEC + 0.3f;
            y -= 20f;
        }
    }

    private void drawTitle(SpriteBatch batch) {
        font.getData().setScale(TITLE_SCALE);
        layout.setText(font, TITLE);
        float x = (Main.VIEW_W - layout.width) / 2f;
        boolean glitching = glitchLeft > 0f;

        // Red copy sits behind and slightly off to the side (and jumps during a
        // glitch).
        font.setColor(RED);
        font.draw(batch, TITLE, x + 3f + (glitching ? MathUtils.random(-8f, 8f) : 0f), TITLE_TOP_Y - 2f);

        font.setColor(GREEN);
        font.draw(batch, TITLE, x + (glitching ? MathUtils.random(-2f, 2f) : 0f), TITLE_TOP_Y);
    }

    private void drawOptions(SpriteBatch batch) {
        font.getData().setScale(OPTION_SCALE);
        for (int i = 0; i < OPTIONS.length; i++) {
            boolean isSelected = (i == selected);
            font.setColor(isSelected ? GREEN : DIM);
            font.draw(batch, OPTIONS[i], optionX[i], optionTopY(i));

            // Blinking block cursor after the selected option.
            if (isSelected && ((int) (time * 2f) % 2 == 0)) {
                batch.setColor(GREEN);
                batch.draw(pixel, optionX[i] + optionW[i] + 12f, optionTopY(i) - optionH[i], 12f, optionH[i]);
                batch.setColor(Color.WHITE);
            }
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void dispose() {
        font.dispose();
        pixel.dispose();
    }
}