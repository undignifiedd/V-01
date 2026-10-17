package com.Version_1;

import com.Version_1.Engine.GameEngineBase;
import com.Version_1.World.World;
import com.Version_1.Phases.Phase;
import com.Version_1.Entities.Player;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.EnumMap;
import java.util.Map;

/**
 * Orchestrator. Owns the shared things (batch via base, camera, viewport, stage,
 * textures) and runs ONE Phase at a time, swapping when it reports finished.
 *
 * Everything marked TEMP is a sprite-sheet test harness: delete it once FightPhase exists.
 */
public class Main extends GameEngineBase {
    // Virtual resolution in world units (pixel-art friendly). Change to taste.
    public static final float VIEW_W = 640;
    public static final float VIEW_H = 360;

    // Bottom-row poses are unevenly spaced. Equal crops keep their feet aligned.
    private static final String PLAYER_SHEET = "sprites/combined_spritesheet.png";
    private static final String JUMP_SHEET = "sprites/girl_jump_spritesheet_4x48x80.png";
    private static final int FRAME_W = 64;
    private static final int FRAME_H = 64;
    private static final int PLAYER_ROW_Y = 277;
    private static final int[] PLAYER_FRAME_X = {108, 166, 214, 272, 326, 390};
    private static final float FRAME_TIME = 0.1f; // seconds per frame
    private static final float ATTACK_FRAME_TIME = 0.5f/3f;
    private static final float DRAW_W = 64;       // size on screen (world units)
    private static final float DRAW_H = 64;
    private static final float GROUND_Y = 60;     // y of the player's feet line
    // --------------------------------------------------------------------

    private OrthographicCamera camera;
    private Viewport viewport;
    private Stage stage;

    private Phase currentPhase;
    private int phaseIndex = 0;

    // TEMP test harness
    private World world;
    private Texture playerSheet;
    private Texture jumpSheet;

    @Override
    public void create() {
        super.create(); // creates the SpriteBatch

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);
        resetCamera();

        stage = new Stage(viewport); // shares the viewport; Stage uses its own batch

        // TODO: load all textures ONCE here and keep them as fields.
        // Pass them into phases/entities via constructors; never load inside those.

        currentPhase = buildPhase(phaseIndex);

        // ---- TEMP: spawn a player so we can see the sprite sheet ----
        world = new World();
        playerSheet = new Texture(PLAYER_SHEET);
        playerSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest); // crisp pixels

        TextureRegion[] frames = new TextureRegion[PLAYER_FRAME_X.length];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new TextureRegion(playerSheet, PLAYER_FRAME_X[i], PLAYER_ROW_Y, FRAME_W, FRAME_H);
        }
        Map<Player.State, Animation<TextureRegion>> anims = new EnumMap<>(Player.State.class);
        anims.put(Player.State.IDLE, buildAnimation(frames, 0, 1, FRAME_TIME, Animation.PlayMode.LOOP));
        anims.put(Player.State.WALK, buildAnimation(frames, 0, 3, FRAME_TIME, Animation.PlayMode.LOOP));
        anims.put(Player.State.ATTACK, buildAnimation(frames, 3, 3, ATTACK_FRAME_TIME, Animation.PlayMode.NORMAL));
        jumpSheet = new Texture(JUMP_SHEET);
        jumpSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        TextureRegion[] jumpFrames = TextureRegion.split(jumpSheet, 48, 80)[0];
        anims.put(Player.State.TAKEOFF, buildAnimation(jumpFrames, 0, 1, FRAME_TIME, Animation.PlayMode.NORMAL));
        anims.put(Player.State.RISE, buildAnimation(jumpFrames, 1, 1, FRAME_TIME, Animation.PlayMode.NORMAL));
        anims.put(Player.State.FALL, buildAnimation(jumpFrames, 2, 1, FRAME_TIME, Animation.PlayMode.NORMAL));
        anims.put(Player.State.LAND, buildAnimation(jumpFrames, 3, 1, FRAME_TIME, Animation.PlayMode.NORMAL));

        Player player = new Player(100, GROUND_Y, DRAW_W, DRAW_H, world, anims);
        world.spawn(player);
        // ---- end TEMP ----
    }

    /** Builds an animation from a consecutive range of the cropped player poses. */
    private Animation<TextureRegion> buildAnimation(TextureRegion[] frames, int first, int count, float frameTime,
                                                    Animation.PlayMode mode) {
        Array<TextureRegion> frameList = new Array<>();
        for (int i = first; i < first + count; i++) {
            frameList.add(frames[i]);
        }
        return new Animation<>(frameTime, frameList, mode);
    }

    /** The story order lives here. Add a case as each phase gets written. */
    private Phase buildPhase(int index) {
        switch (index) {
            // case 0: return new FightPhase(...);      // fight 1 -> screen crack
            // case 1: return new RealWorldPhase(...);
            // case 2: return new FightPhase(...);      // fight 2 (different params)
            // case 3: return new ExplorePhase(...);
            // case 4: return new RealWorldPhase(...);  // cliffhanger ending
            default:
                return null; // story over / not written yet
        }
    }

    private void nextPhase() {
        currentPhase.dispose();
        phaseIndex++;
        resetCamera();
        currentPhase = buildPhase(phaseIndex);
    }

    private void resetCamera() {
        camera.position.set(VIEW_W / 2f, VIEW_H / 2f, 0);
        camera.update();
    }

    @Override
    protected void update(float delta) {
        if (currentPhase != null) {
            currentPhase.update(delta);
            if (currentPhase.isFinished()) {
                nextPhase();
            }
        }

        world.update(delta); // TEMP: remove when phases own their World

        stage.act(delta);

        // Keep last: the batch must use the camera before draw() starts.
        camera.update();
        batch.setProjectionMatrix(camera.combined);
    }

    @Override
    protected void draw() {
        if (currentPhase != null) {
            currentPhase.draw(batch);
        }
        world.draw(batch); // TEMP: remove when phases own their World
    }

    @Override
    protected void drawUI() {
        stage.draw(); // outside the batch block, as required
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (currentPhase != null) {
            currentPhase.dispose();
        }
        stage.dispose();
        playerSheet.dispose(); // TEMP
        jumpSheet.dispose();
        super.dispose(); // disposes the batch
    }

    // Phases take only what they need from these, passed through their constructors.
    public OrthographicCamera getCamera() { return camera; }
    public Viewport getViewport() { return viewport; }
    public Stage getStage() { return stage; }
}

