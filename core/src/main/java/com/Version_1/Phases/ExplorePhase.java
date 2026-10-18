package com.Version_1.Phases;

import com.Version_1.Entities.Entity;
import com.Version_1.Entities.Interactable;
import com.Version_1.Entities.NPC;
import com.Version_1.Entities.Player;
import com.Version_1.UI.DialogueBox;
import com.Version_1.World.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;

/** Reusable top-down room and nearest-Interactable dialogue handling. */
public class ExplorePhase implements Phase {
    protected final World world = new World();
    protected final Player player;
    protected final DialogueBox dialogue;
    protected final BitmapFont font;
    protected final TextureRegion pixel;
    protected final float width, height;
    private final Array<Rectangle> obstacles = new Array<>();
    private final Rectangle feet = new Rectangle();
    private NPC completionTarget;
    private Rectangle exit;
    private Rectangle returnExit;
    private boolean returning;
    private float roomWidth, roomHeight;
    private float floorY = 140f;
    private final OrthographicCamera roomCamera = new OrthographicCamera();
    private final Matrix4 screenProjection = new Matrix4();
    private Interactable nearby;
    private boolean finished;
    private TextureRegion floorTile, shadowTree;
    private Animation<TextureRegion> roomEye;
    private float roomTime;

    public ExplorePhase(Player player, DialogueBox dialogue, BitmapFont font, TextureRegion pixel,
                        float width, float height) {
        this.player = player;
        this.dialogue = dialogue;
        this.font = font;
        this.pixel = pixel;
        this.width = width;
        this.height = height;
        roomWidth = width;
        roomHeight = height;
        roomCamera.setToOrtho(false, width, height);
        player.stopMovement();
        player.setTopDown(true);
        player.setBurning(false);
        player.getPosition().set(80f, 160f);
        world.spawn(player);
        world.update(0f);
    }

    public void addNPC(NPC npc) { world.spawn(npc); }
    public void addObstacle(Rectangle obstacle) { obstacles.add(new Rectangle(obstacle)); }
    public void finishWhenRead(NPC npc) { completionTarget = npc; }
    public void setExit(Rectangle exit) { this.exit = new Rectangle(exit); }
    public void setReturnExit(Rectangle exit) { returnExit = new Rectangle(exit); }
    public void setRoomScenery(TextureRegion floorTile, TextureRegion shadowTree,
                               Animation<TextureRegion> roomEye) {
        this.floorTile = floorTile;
        this.shadowTree = shadowTree;
        this.roomEye = roomEye;
    }
    public int getExitDirection() { return returning ? -1 : 1; }
    public float getRoomWidth() { return roomWidth; }
    public float getRoomHeight() { return roomHeight; }

    public void setRoomSize(float roomWidth, float roomHeight) {
        if (roomWidth < width || roomHeight < height) throw new IllegalArgumentException("Room smaller than view");
        this.roomWidth = roomWidth;
        this.roomHeight = roomHeight;
        floorY = 24f;
    }

    /** Re-enter an existing room without recreating its NPCs or dialogue progress. */
    public void enterAt(float x, float y) {
        finished = false;
        returning = false;
        nearby = null;
        player.stopMovement();
        player.getPosition().set(x, y);
    }

    @Override
    public void update(float delta) {
        roomTime += Math.min(delta, 0.05f);
        if (finished) return;
        boolean interactPressed = Gdx.input.isKeyJustPressed(Input.Keys.E);
        if (dialogue.isOpen()) {
            if (dialogue.isChoosing()) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) dialogue.choose(true);
                else if (Gdx.input.isKeyJustPressed(Input.Keys.N)) dialogue.choose(false);
            } else if (interactPressed || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) dialogue.advance();
            if (!dialogue.isOpen() && completionTarget != null && completionTarget.hasBeenRead()) finished = true;
            return; // Closing input cannot also reopen a conversation or move the player.
        }
        nearby = findNearby();
        if (interactPressed && nearby != null) {
            player.stopMovement();
            nearby.interact();
            return;
        }
        if (interactPressed && exit != null && exit.overlaps(player.getContactBounds())) {
            player.stopMovement();
            finished = true;
            return;
        }
        if (interactPressed && returnExit != null && returnExit.overlaps(player.getContactBounds())) {
            player.stopMovement();
            returning = true;
            finished = true;
            return;
        }
        float oldX = player.getPosition().x, oldY = player.getPosition().y;
        world.update(Math.min(delta, 0.05f));
        float newY = MathUtils.clamp(player.getPosition().y, floorY + 2f, roomHeight - 88f);
        player.getPosition().x = MathUtils.clamp(player.getPosition().x, 24f, roomWidth - 88f);
        player.getPosition().y = oldY;
        if (blocked()) player.getPosition().x = oldX;
        player.getPosition().y = newY;
        if (blocked()) player.getPosition().y = oldY;
        nearby = findNearby();
    }

    private boolean blocked() {
        feet.set(player.getPosition().x + 20f, player.getPosition().y, 24f, 10f);
        for (int i = 0; i < obstacles.size; i++) if (feet.overlaps(obstacles.get(i))) return true;
        return false;
    }

    private Interactable findNearby() {
        // Top-down proximity is measured from the feet, not the tall sprite's head.
        Rectangle body = feet.set(player.getPosition().x + 20f, player.getPosition().y, 24f, 10f);
        float best = 42f * 42f;
        Interactable closest = null;
        Array<Entity> entities = world.getEntities();
        for (int i = 0; i < entities.size; i++) {
            Entity entity = entities.get(i);
            if (entity.isRemoved() || !(entity instanceof Interactable)) continue;
            if (entity instanceof NPC && !((NPC) entity).isVisible()) continue;
            Rectangle target = ((Interactable) entity).getBounds();
            float dx = Math.max(0f, Math.max(target.x - body.x - body.width, body.x - target.x - target.width));
            float dy = Math.max(0f, Math.max(target.y - body.y - body.height, body.y - target.y - target.height));
            float distance = dx * dx + dy * dy;
            if (distance <= best) { best = distance; closest = (Interactable) entity; }
        }
        return closest;
    }

    @Override
    public void draw(SpriteBatch batch) {
        Color old = batch.getColor();
        float r = old.r, g = old.g, b = old.b, a = old.a;
        screenProjection.set(batch.getProjectionMatrix());
        roomCamera.position.set(
            MathUtils.clamp(player.getPosition().x + 32f, width / 2f, roomWidth - width / 2f),
            MathUtils.clamp(player.getPosition().y + 72f, height / 2f, roomHeight - height / 2f), 0f);
        roomCamera.update();
        batch.setProjectionMatrix(roomCamera.combined);
        drawRoom(batch);
        batch.setColor(Color.WHITE);
        // Sort only for drawing: feet lower on the screen appear in front.
        Array<Entity> entities = world.getEntities();
        for (int i = 0; i < entities.size; i++) {
            Entity entity = entities.get(i);
            if (entity != player && entity.getPosition().y >= player.getPosition().y) entity.draw(batch);
        }
        player.draw(batch);
        for (int i = 0; i < entities.size; i++) {
            Entity entity = entities.get(i);
            if (entity != player && entity.getPosition().y < player.getPosition().y) entity.draw(batch);
        }
        // Dialogue and prompts stay fixed on screen while the room camera scrolls.
        batch.setProjectionMatrix(screenProjection);
        batch.setColor(Color.WHITE);
        if (!dialogue.isOpen() && shouldDrawPrompts()) {
            font.draw(batch, "WASD move   E interact", 24f, 16f);
            if (nearby != null) {
                String name = nearby instanceof NPC ? ((NPC) nearby).getName() : "interact";
                font.draw(batch, "E - " + name, 24f, 42f);
            } else if (exit != null && exit.overlaps(player.getContactBounds())) {
                font.draw(batch, "E - continue", 24f, 42f);
            } else if (returnExit != null && returnExit.overlaps(player.getContactBounds())) {
                font.draw(batch, "E - return to the passage", 24f, 42f);
            }
        }
        dialogue.draw(batch);
        batch.setColor(r, g, b, a);
    }

    protected void drawRoom(SpriteBatch batch) {
        if (floorTile != null) {
            fill(batch, 0f, 0f, roomWidth, roomHeight, 0.008f, 0.005f, 0.015f);
            batch.setColor(0.45f, 0.30f, 0.52f, 0.75f);
            for (int y = 24; y < roomHeight - 24; y += 32) {
                for (int x = 24; x < roomWidth - 24; x += 32) batch.draw(floorTile, x, y, 32f, 32f);
            }
            batch.setColor(0.65f, 0.4f, 0.75f, 0.28f);
            batch.draw(roomEye.getKeyFrame(roomTime), roomWidth / 2f - 65f, roomHeight - 160f, 130f, 130f);
            batch.setColor(0.55f, 0.4f, 0.65f, 0.85f);
            batch.draw(shadowTree, 8f, roomHeight - 194f, 86f, 158f);
            batch.draw(shadowTree, roomWidth - 8f, roomHeight - 194f, -86f, 158f);
            // Fade the stone into darkness instead of framing it with a bright grid.
            for (int edge = 0; edge < 32; edge += 4) {
                batch.setColor(0f, 0f, 0f, (1f - edge / 32f) * 0.75f);
                batch.draw(pixel, edge, 0f, 4f, roomHeight);
                batch.draw(pixel, roomWidth - edge - 4f, 0f, 4f, roomHeight);
                batch.draw(pixel, 0f, edge, roomWidth, 4f);
                batch.draw(pixel, 0f, roomHeight - edge - 4f, roomWidth, 4f);
            }
            batch.setColor(Color.WHITE);
            return;
        }
        fill(batch, 0f, 0f, roomWidth, roomHeight, 0.025f, 0.02f, 0.04f);
        fill(batch, 24f, floorY, roomWidth - 48f, roomHeight - floorY - 24f, 0.10f, 0.075f, 0.14f);
        for (int y = (int) floorY; y < roomHeight - 24; y += 32) {
            fill(batch, 24f, y, roomWidth - 48f, 1f, 0.14f, 0.10f, 0.18f);
        }
        for (int x = 24; x < roomWidth - 24; x += 64) {
            fill(batch, x, floorY, 1f, roomHeight - floorY - 24f, 0.12f, 0.085f, 0.16f);
        }
    }

    protected boolean shouldDrawPrompts() { return true; }

    protected void fill(SpriteBatch batch, float x, float y, float w, float h, float r, float g, float b) {
        batch.setColor(r, g, b, 1f);
        batch.draw(pixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    @Override public boolean isFinished() { return finished; }
    @Override public void dispose() { player.stopMovement(); }
}
