package com.Version_1;

import com.Version_1.Engine.GameEngineBase;
import com.Version_1.Entities.Player;
import com.Version_1.Entities.Enemies.Boss;
import com.Version_1.Phases.FightPhase;
import com.Version_1.Phases.Phase;
import com.Version_1.World.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.EnumMap;
import java.util.Map;

/** Owns shared assets and runs one phase at a time. */
public class Main extends GameEngineBase {
    public static final float VIEW_W = 640f;
    public static final float VIEW_H = 360f;
    private static final String PLAYER_SHEET = "sprites/combined_spritesheet.png";
    private static final String JUMP_SHEET = "sprites/girl_jump_spritesheet_4x48x80.png";
    private static final String BOSS_SHEET = "big_boss/boss_combined.png";
    private static final int FRAME_W = 64;
    private static final int FRAME_H = 64;
    private static final int PLAYER_ROW_Y = 277;
    private static final int[] PLAYER_FRAME_X = {108, 166, 214, 272, 326, 390};
    private static final float FRAME_TIME = 0.1f;
    private static final float BACKGROUND_FRAME_TIME = 0.65f;
    private static final float ATTACK_FRAME_TIME = 0.5f / 3f;
    private static final float DRAW_W = 64f;
    private static final float DRAW_H = 64f;
    private static final float GROUND_Y = 60f;
    // Boss body is 54 source pixels high; player body is about 55 source pixels high.
    private static final float BOSS_SCALE = 2.6f;
    private static final int BOSS_HEALTH = 240;
    private static final float FINISHER_HEALTH_FRACTION = 0.25f;

    private OrthographicCamera camera;
    private FitViewport viewport;
    private Stage stage;
    private Phase currentPhase;
    private int phaseIndex;
    private Texture playerSheet;
    private Texture jumpSheet;
    private Texture bossSheet;
    private Texture mobSheet;
    private Texture mob2Sheet;
    private Animation<TextureRegion> mobAnimation;
    private Animation<TextureRegion> mob2Animation;
    private BitmapFont font;
    private Map<Player.State, Animation<TextureRegion>> playerAnimations;
    private Map<Boss.State, Animation<TextureRegion>> bossAnimations;
    private Animation<TextureRegion> waveAnimation;
    private Animation<TextureRegion> backgroundAnimation;
    private TextureRegion crack;
    private TextureRegion pixel;

    @Override
    public void create() {
        super.create();
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        stage = new Stage(viewport);
        font = new BitmapFont();
        playerSheet = loadTexture(PLAYER_SHEET);
        jumpSheet = loadTexture(JUMP_SHEET);
        bossSheet = loadTexture(BOSS_SHEET);
        mobSheet = loadTexture("evileye_shadow/waechter-20 Charset.png");
        mob2Sheet = loadTexture("eye_shadow/waechter-19.png");
        buildAnimations();
        currentPhase = buildPhase(phaseIndex);
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private void buildAnimations() {
        TextureRegion[] frames = new TextureRegion[PLAYER_FRAME_X.length];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new TextureRegion(playerSheet, PLAYER_FRAME_X[i], PLAYER_ROW_Y, FRAME_W, FRAME_H);
        }
        playerAnimations = new EnumMap<>(Player.State.class);
        playerAnimations.put(Player.State.IDLE, buildAnimation(frames, 0, 1, FRAME_TIME, Animation.PlayMode.LOOP));
        playerAnimations.put(Player.State.WALK, buildAnimation(frames, 0, 3, FRAME_TIME, Animation.PlayMode.LOOP));
        playerAnimations.put(Player.State.ATTACK, buildAnimation(frames, 3, 3, ATTACK_FRAME_TIME, Animation.PlayMode.NORMAL));
        TextureRegion[] jumpFrames = TextureRegion.split(jumpSheet, 48, 80)[0];
        playerAnimations.put(Player.State.TAKEOFF, buildAnimation(jumpFrames, 0, 1, FRAME_TIME, Animation.PlayMode.NORMAL));
        playerAnimations.put(Player.State.RISE, buildAnimation(jumpFrames, 1, 1, FRAME_TIME, Animation.PlayMode.NORMAL));
        playerAnimations.put(Player.State.FALL, buildAnimation(jumpFrames, 2, 1, FRAME_TIME, Animation.PlayMode.NORMAL));
        playerAnimations.put(Player.State.LAND, buildAnimation(jumpFrames, 3, 1, FRAME_TIME, Animation.PlayMode.NORMAL));

        // Unified atlas rows are 93px high, each cell 140px wide.
        bossAnimations = new EnumMap<>(Boss.State.class);
        bossAnimations.put(Boss.State.IDLE, bossAnimation(0, 8, 0.12f, Animation.PlayMode.LOOP));
        bossAnimations.put(Boss.State.WALK, bossAnimation(1, 8, 0.1f, Animation.PlayMode.LOOP));
        bossAnimations.put(Boss.State.ATTACK, bossAnimation(2, 10, 0.08f, Animation.PlayMode.NORMAL));
        bossAnimations.put(Boss.State.CAST, bossAnimation(3, 9, 0.08f, Animation.PlayMode.NORMAL));
        bossAnimations.put(Boss.State.DEATH, bossAnimation(6, 10, 0.1f, Animation.PlayMode.NORMAL));
        TextureRegion[] spells = new TextureRegion[16];
        for (int i = 0; i < spells.length; i++) {
            spells[i] = new TextureRegion(bossSheet, i * 140 + 50, 4 * 93 + 32, 36, 61);
        }
        waveAnimation = buildAnimation(spells, 0, spells.length, 0.055f, Animation.PlayMode.LOOP);
        TextureRegion[] eyes = new TextureRegion[4];
        for (int i = 0; i < eyes.length; i++) eyes[i] = new TextureRegion(bossSheet, i * 256, 651, 256, 256);
        backgroundAnimation = buildAnimation(eyes, 0, eyes.length, BACKGROUND_FRAME_TIME, Animation.PlayMode.LOOP_PINGPONG);
        crack = new TextureRegion(bossSheet, 0, 907, 640, 360);
        pixel = new TextureRegion(bossSheet, 2239, 1279, 1, 1);
        // Charset cells: 96x96 and 48x48; row 2 faces right in both sheets.
        TextureRegion[] leeches = new TextureRegion[3];
        TextureRegion[] bombs = new TextureRegion[3];
        for (int i = 0; i < 3; i++) {
            leeches[i] = new TextureRegion(mobSheet, i * 96 + 8, 192 + 8, 80, 80);
            bombs[i] = new TextureRegion(mob2Sheet, i * 48, 96, 48, 48);
        }
        mobAnimation = buildAnimation(leeches, 0, 3, 0.16f, Animation.PlayMode.LOOP_PINGPONG);
        mob2Animation = buildAnimation(bombs, 0, 3, 0.13f, Animation.PlayMode.LOOP_PINGPONG);
    }

    private Animation<TextureRegion> bossAnimation(int row, int count, float frameTime, Animation.PlayMode mode) {
        TextureRegion[] frames = new TextureRegion[count];
        for (int i = 0; i < count; i++) frames[i] = new TextureRegion(bossSheet, i * 140, row * 93, 140, 93);
        return buildAnimation(frames, 0, count, frameTime, mode);
    }

    private Animation<TextureRegion> buildAnimation(TextureRegion[] frames, int first, int count,
                                                    float frameTime, Animation.PlayMode mode) {
        Array<TextureRegion> frameList = new Array<>();
        for (int i = first; i < first + count; i++) frameList.add(frames[i]);
        return new Animation<>(frameTime, frameList, mode);
    }

    /** Story order lives here. A zero finisher fraction allows a normal boss death. */
    private Phase buildPhase(int index) {
        switch (index) {
            case 0:
                World world = new World();
                Player player = new Player(100f, GROUND_Y, DRAW_W, DRAW_H, playerAnimations);
                player.setGroundY(GROUND_Y);
                // Leave player-sized exits on both sides so a dash can emerge past the boss.
                float bossMargin = DRAW_W + 24f;
                Boss boss = new Boss(VIEW_W - 200f, GROUND_Y, BOSS_SCALE, BOSS_HEALTH,
                    bossMargin, VIEW_W - bossMargin, FINISHER_HEALTH_FRACTION,
                    bossAnimations, waveAnimation, player, world);
                return new FightPhase(world, player, boss, camera, font, backgroundAnimation,
                    crack, pixel, VIEW_W, VIEW_H, mobAnimation, mob2Animation);
            default:
                return null; // Further story phases are not implemented yet.
        }
    }

    private void nextPhase() {
        currentPhase.dispose();
        phaseIndex++;
        resetCamera();
        currentPhase = buildPhase(phaseIndex);
    }

    private void resetCamera() {
        camera.position.set(VIEW_W / 2f, VIEW_H / 2f, 0f);
        camera.update();
    }

    @Override
    protected void update(float delta) {
        if (currentPhase != null) {
            currentPhase.update(delta);
            if (currentPhase.isFinished()) nextPhase();
        }
        boolean retry = currentPhase == null || (currentPhase instanceof FightPhase
            && ((FightPhase) currentPhase).isPlayerDead());
        if (retry && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            if (currentPhase != null) currentPhase.dispose();
            phaseIndex = 0;
            resetCamera();
            currentPhase = buildPhase(phaseIndex);
        }
        stage.act(delta);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
    }

    @Override
    protected void draw() {
        if (currentPhase != null) currentPhase.draw(batch);
        else {
            batch.setColor(Color.WHITE);
            font.draw(batch, "The story continues...", 210f, 195f);
            font.draw(batch, "Press R to replay the opening fight.", 155f, 165f);
        }
    }

    @Override
    protected void drawUI() { stage.draw(); }

    @Override
    public void resize(int width, int height) { viewport.update(width, height, true); }

    @Override
    public void dispose() {
        if (currentPhase != null) currentPhase.dispose();
        stage.dispose();
        font.dispose();
        playerSheet.dispose();
        jumpSheet.dispose();
        bossSheet.dispose();
        mobSheet.dispose();
        mob2Sheet.dispose();
        super.dispose();
    }

    public OrthographicCamera getCamera() { return camera; }
    public Viewport getViewport() { return viewport; }
    public Stage getStage() { return stage; }
}
