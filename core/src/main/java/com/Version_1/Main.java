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
/**
 * Orchestrator. Owns the shared things (batch via base, camera, viewport, stage,
 * textures) and runs ONE Phase at a time, swapping when it reports finished.
 */
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

    // ---- TEMP: sprite sheet config. EDIT THESE to match your sheet. ----
    private static final String PLAYER_SHEET = "sprites/pixil-frame-0 (1).png"; // file in assets/
    private static final int FRAME_W = 93;       // pixel width of ONE frame in the sheet
    private static final int FRAME_H = 118;       // pixel height of ONE frame
    private static final int IDLE_ROW = 2,   IDLE_FRAMES = 3;    // row index (0 = top row) + frame count
    private static final int WALK_ROW = 1,   WALK_FRAMES = 6;
    private static final int ATTACK_ROW = 2, ATTACK_FRAMES = 5;
    private static final float FRAME_TIME = 0.1f; // seconds per frame
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

        TextureRegion[][] grid = TextureRegion.split(playerSheet, FRAME_W, FRAME_H);
        Map<Player.State, Animation<TextureRegion>> anims = new EnumMap<>(Player.State.class);
        anims.put(Player.State.IDLE, buildAnimation(grid, IDLE_ROW, IDLE_FRAMES, Animation.PlayMode.LOOP));
        anims.put(Player.State.WALK, buildAnimation(grid, WALK_ROW, WALK_FRAMES, Animation.PlayMode.LOOP));
        anims.put(Player.State.ATTACK, buildAnimation(grid, ATTACK_ROW, ATTACK_FRAMES, Animation.PlayMode.NORMAL));

        Player player = new Player(100, GROUND_Y, DRAW_W, DRAW_H, world, anims);
        world.spawn(player);
        // ---- end TEMP ----
    }

    /** TEMP helper: builds one animation from the first `frames` cells of a sheet row. */
    private Animation<TextureRegion> buildAnimation(TextureRegion[][] grid, int row, int frames,
                                                    Animation.PlayMode mode) {
        Array<TextureRegion> frameList = new Array<>();
        for (int i = 0; i < frames; i++) {
            frameList.add(grid[row][i]);
        }
        return new Animation<>(FRAME_TIME, frameList, mode);
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
        super.dispose(); // disposes the batch
    }

    // Phases take only what they need from these, passed through their constructors.
    public OrthographicCamera getCamera() { return camera; }
    public Viewport getViewport() { return viewport; }
    public Stage getStage() { return stage; }
}

