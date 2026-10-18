package com.Version_1.UI;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

/** Plain paged dialogue. Input belongs to the phase; shared font/pixel belong to Main. */
public class DialogueBox {
    private static final float PANEL_HEIGHT = 126f;
    private final BitmapFont font;
    private final TextureRegion pixel;
    private final float screenWidth;
    private final Array<String> pages = new Array<>();
    private final GlyphLayout layout = new GlyphLayout();
    private String speaker;
    private int page;
    private boolean open;
    private Runnable onComplete;
    private Runnable onDecline;
    private boolean choice;

    public DialogueBox(BitmapFont font, TextureRegion pixel, float screenWidth) {
        this.font = font;
        this.pixel = pixel;
        this.screenWidth = screenWidth;
    }

    public void show(String speaker, String[] dialogue, Runnable onComplete) {
        if (open) return;
        if (dialogue == null || dialogue.length == 0) throw new IllegalArgumentException("Dialogue needs text");
        pages.clear();
        for (String text : dialogue) paginate(text == null ? "" : text);
        this.speaker = speaker;
        this.onComplete = onComplete;
        onDecline = null;
        choice = false;
        page = 0;
        open = true;
    }

    private void paginate(String text) {
        // Measure actual wrapped font height; long entries automatically become more pages.
        String remaining = text.trim();
        if (remaining.isEmpty()) { pages.add(""); return; }
        while (!remaining.isEmpty()) {
            int end = 1;
            while (end <= remaining.length()) {
                layout.setText(font, remaining.substring(0, end), Color.WHITE,
                    screenWidth - 80f, Align.left, true);
                if (layout.height > 58f && end > 1) break;
                end++;
            }
            int count = Math.min(remaining.length(), end - 1);
            if (count < remaining.length()) {
                int space = remaining.lastIndexOf(' ', count);
                if (space > 0) count = space;
            }
            pages.add(remaining.substring(0, count).trim());
            remaining = remaining.substring(count).trim();
        }
    }

    public boolean isOpen() { return open; }
    public String getCurrentText() { return open ? pages.get(page) : ""; }
    public int getPageCount() { return pages.size; }
    public boolean isChoosing() { return open && choice && page == pages.size - 1; }

    public void ask(String question, Runnable onYes, Runnable onNo) {
        if (open) return;
        show("Help her?", new String[] {question}, onYes);
        choice = true;
        onDecline = onNo;
    }

    public void choose(boolean yes) {
        if (!isChoosing()) return;
        open = false;
        Runnable completion = yes ? onComplete : onDecline;
        onComplete = null;
        onDecline = null;
        choice = false;
        if (completion != null) completion.run();
    }

    public void advance() {
        if (!open || isChoosing()) return;
        if (++page < pages.size) return;
        open = false;
        Runnable completion = onComplete;
        onComplete = null;
        if (completion != null) completion.run();
    }

    public void draw(SpriteBatch batch) {
        if (!open) return;
        Color old = batch.getColor();
        float r = old.r, g = old.g, b = old.b, a = old.a;
        Color oldFont = new Color(font.getColor());
        batch.setColor(0.48f, 0.4f, 0.55f, 1f);
        batch.draw(pixel, 18f, 14f, screenWidth - 36f, PANEL_HEIGHT);
        batch.setColor(0.055f, 0.045f, 0.075f, 0.98f);
        batch.draw(pixel, 20f, 16f, screenWidth - 40f, PANEL_HEIGHT - 4f);
        batch.setColor(Color.WHITE);
        font.setColor(0.95f, 0.8f, 0.6f, 1f);
        font.draw(batch, speaker, 36f, 122f);
        font.setColor(Color.WHITE);
        font.draw(batch, getCurrentText(), 36f, 98f, screenWidth - 80f, Align.left, true);
        font.setColor(0.65f, 0.63f, 0.7f, 1f);
        font.draw(batch, isChoosing() ? "Y - Yes    N - No" : "E / ENTER  " + (page + 1 < pages.size ? "next" : "close"),
            screenWidth - 190f, 34f);
        font.setColor(oldFont);
        batch.setColor(r, g, b, a);
    }
}
