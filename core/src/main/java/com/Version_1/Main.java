package com.Version_1;

import com.Version_1.Engine.GameEngineBase;
import com.Version_1.Entities.Player;
import com.Version_1.Entities.Enemies.Boss;
import com.Version_1.Phases.FightPhase;
import com.Version_1.Phases.Phase;
import com.Version_1.Phases.MenuPhase;
import com.Version_1.Phases.DeathDiaryPhase;
import com.Version_1.Phases.ExplorePhase;
import com.Version_1.Phases.RealWorldPhase;
import com.Version_1.Phases.FlowerQuestPhase;
import com.Version_1.Phases.FlowerRoomPhase;
import com.Version_1.Story.DialogueData;
import com.Version_1.UI.DialogueBox;
import com.Version_1.World.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
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
    // Full story starts at the opening boss fight; 1 skips to the diary for testing.
    private static final int START_PHASE = 3;
    private static final float INNER_ROOM_W = VIEW_W;
    private static final float INNER_ROOM_H = VIEW_H;

    private OrthographicCamera camera;
    private FitViewport viewport;
    private Stage stage;
    private Phase currentPhase;
    private int phaseIndex = START_PHASE;
    private int retryPhase = -1;
    private Texture playerSheet;
    private Texture jumpSheet;
    private Texture bossSheet;
    private Texture rageWaveTexture;
    private Texture rageMeterTexture;
    private Texture menuSheet, exitMenuSheet;
    private Music diaryMusic, npcMusic, activeMusic;
    private Sound punchHit1, punchHit2, heavyHit, rageLaunch, rageExplosion, shieldImpact;
    private Texture mobSheet;
    private Texture mob2Sheet;
    private Texture diaryTexture;
    private Texture flowerTexture;
    private TextureRegion flowerSprite;
    private Texture forestSheet;
    private Texture smileSheet;
    private Animation<TextureRegion> smileAnimation;
    private Texture bobFrownSheet;
    private Animation<TextureRegion> bobFrownAnimation;
    private TextureRegion floorTile, shadowTree;
    private TextureRegion diarySprite;
    private final Array<Animation<TextureRegion>> npcAnimations = new Array<>();
    private DialogueBox dialogue;
    private Player storyPlayer;
    private ExplorePhase passagePhase;
    private FlowerQuestPhase npcPhase;
    private Animation<TextureRegion> mobAnimation;
    private Animation<TextureRegion> mob2Animation;
    private BitmapFont font;
    private Map<Player.State, Animation<TextureRegion>> playerAnimations;
    private Map<Boss.State, Animation<TextureRegion>> bossAnimations;
    private Animation<TextureRegion> waveAnimation;
    private Animation<TextureRegion> backgroundAnimation;
    private Animation<TextureRegion> closedEyeAnimation, openingEyeAnimation, openEyeAnimation;
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
        diaryMusic = Gdx.audio.newMusic(Gdx.files.internal("sfx/2. Moonless Night.wav"));
        npcMusic = Gdx.audio.newMusic(Gdx.files.internal("sfx/1. Behind the Curtains.wav"));
        diaryMusic.setLooping(true);
        npcMusic.setLooping(true);
        diaryMusic.setVolume(0.5f);
        npcMusic.setVolume(0.5f);
        punchHit1 = Gdx.audio.newSound(Gdx.files.internal("sfx/combat/punch_hit_01.ogg"));
        punchHit2 = Gdx.audio.newSound(Gdx.files.internal("sfx/combat/punch_hit_02.ogg"));
        heavyHit = Gdx.audio.newSound(Gdx.files.internal("sfx/combat/heavy_hit.ogg"));
        rageLaunch = Gdx.audio.newSound(Gdx.files.internal("sfx/combat/rage_launch.wav"));
        rageExplosion = Gdx.audio.newSound(Gdx.files.internal("sfx/combat/rage_explosion.ogg"));
        shieldImpact = Gdx.audio.newSound(Gdx.files.internal("sfx/combat/shield_impact.ogg"));
        playerSheet = loadTexture(PLAYER_SHEET);
        jumpSheet = loadTexture(JUMP_SHEET);
        bossSheet = loadTexture(BOSS_SHEET);
        rageWaveTexture = loadTexture("netanyahu/bro.jpg");
        rageMeterTexture = loadTexture("health and rage/ragemeter.png");
        menuSheet = loadTexture("ui elements/Skin06-Start-Screen-Menu.png");
        exitMenuSheet = loadTexture("ui elements/Skin06-Question_Exit-Game.png");
        mobSheet = loadTexture("evileye_shadow/waechter-20 Charset.png");
        mob2Sheet = loadTexture("eye_shadow/waechter-19.png");
        forestSheet = loadTexture("forest_tilesheet/free.png");
        smileSheet = loadTexture("sprites/player_smile_spritesheet.png");
        bobFrownSheet = loadTexture("sprites/bob_frown_spritesheet.png");
        floorTile = new TextureRegion(forestSheet, 40, 24, 32, 32);
        shadowTree = new TextureRegion(forestSheet, 112, 64, 48, 88);
        buildAnimations();
        buildStoryArt();
        dialogue = new DialogueBox(font, pixel, VIEW_W);
        currentPhase = buildMenu();
        updateSceneMusic();
    }

    private MenuPhase buildMenu() {
        TextureRegion[] menuLabels = {
            new TextureRegion(menuSheet, 174, 214, 230, 50),
            new TextureRegion(menuSheet, 174, 461, 140, 50),
            new TextureRegion(menuSheet, 174, 544, 210, 56)
        };
        MenuPhase menu = new MenuPhase(new TextureRegion(menuSheet, 480, 0, 480, 720), menuLabels,
            new TextureRegion(exitMenuSheet, 324, 300, 312, 120), pixel, font, viewport);
        menu.setRetryAvailable(retryPhase >= 0);
        return menu;
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private void buildStoryArt() {
        // Bob, Jack, then the lady. Keep all three frames; exclude the baby.
        int[][] frameX = {{108, 166, 214}, {277, 347, 395}, {286, 338, 390}};
        int[] rowY = {44, 208, 44};
        npcAnimations.clear();
        for (int npc = 0; npc < frameX.length; npc++) {
            TextureRegion[] frames = new TextureRegion[3];
            for (int frame = 0; frame < 3; frame++) {
                frames[frame] = new TextureRegion(playerSheet, frameX[npc][frame], rowY[npc], 64, 64);
            }
            npcAnimations.add(buildAnimation(frames, 0, 3, 0.22f, Animation.PlayMode.LOOP_PINGPONG));
        }
        Pixmap diary = new Pixmap(24, 20, Pixmap.Format.RGBA8888);
        diary.setColor(0.12f, 0.06f, 0.05f, 1f);
        diary.fillRectangle(1, 2, 22, 17);
        diary.setColor(0.85f, 0.78f, 0.60f, 1f);
        diary.fillRectangle(4, 3, 18, 13);
        diary.setColor(0.36f, 0.12f, 0.10f, 1f);
        diary.fillRectangle(1, 5, 20, 14);
        diary.setColor(0.65f, 0.37f, 0.20f, 1f);
        diary.fillRectangle(4, 5, 2, 14);
        diary.setColor(0.90f, 0.73f, 0.37f, 1f);
        diary.fillRectangle(10, 10, 7, 2);
        diaryTexture = new Texture(diary);
        diary.dispose();
        diaryTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        diarySprite = new TextureRegion(diaryTexture);
        Pixmap flower = new Pixmap(12, 20, Pixmap.Format.RGBA8888);
        flower.setColor(0.25f, 0.52f, 0.26f, 1f);
        flower.fillRectangle(5, 6, 1, 14);
        flower.fillRectangle(6, 12, 4, 2);
        flower.setColor(0.85f, 0.34f, 0.49f, 1f);
        flower.fillRectangle(2, 3, 8, 4);
        flower.setColor(0.94f, 0.48f, 0.62f, 1f);
        flower.fillRectangle(4, 1, 4, 8);
        flower.setColor(1f, 0.86f, 0.4f, 1f);
        flower.fillRectangle(5, 4, 2, 2);
        flowerTexture = new Texture(flower);
        flower.dispose();
        flowerTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        flowerSprite = new TextureRegion(flowerTexture);
    }

    private void buildAnimations() {
        TextureRegion[] smileFrames = TextureRegion.split(smileSheet, 256, 256)[0];
        smileAnimation = buildAnimation(smileFrames, 0, 6, 3.2f / 5f, Animation.PlayMode.NORMAL);
        TextureRegion[] frownFrames = TextureRegion.split(bobFrownSheet, 256, 256)[0];
        bobFrownAnimation = buildAnimation(frownFrames, 0, 6,
            FlowerQuestPhase.FROWN_GROW_TIME / 5f, Animation.PlayMode.NORMAL);
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
        closedEyeAnimation = new Animation<>(1f, eyes[3]);
        openingEyeAnimation = new Animation<>(0.35f, eyes[3], eyes[2], eyes[1], eyes[0]);
        openingEyeAnimation.setPlayMode(Animation.PlayMode.NORMAL);
        openEyeAnimation = new Animation<>(1f, eyes[0]);
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
        if (storyPlayer == null || index == 0) {
            storyPlayer = new Player(100f, GROUND_Y, DRAW_W, DRAW_H, playerAnimations);
        }
        switch (index) {
            case 0:
                World world = new World();
                Player player = storyPlayer;
                player.setGroundY(GROUND_Y);
                // Leave player-sized exits on both sides so a dash can emerge past the boss.
                float bossMargin = DRAW_W + 24f;
                Boss boss = new Boss(VIEW_W - 200f, GROUND_Y, BOSS_SCALE, BOSS_HEALTH,
                    bossMargin, VIEW_W - bossMargin, FINISHER_HEALTH_FRACTION,
                    bossAnimations, waveAnimation, player, world);
                FightPhase openingFight = new FightPhase(world, player, boss, camera, font, backgroundAnimation,
                    crack, pixel, VIEW_W, VIEW_H, mobAnimation, mob2Animation);
                openingFight.setRageArt(new TextureRegion(rageWaveTexture), new TextureRegion(rageMeterTexture, 0, 0, 48, 128));
                openingFight.setCombatSounds(punchHit1, punchHit2, heavyHit, rageLaunch, rageExplosion, shieldImpact);
                return openingFight;
            case 1:
                return new RealWorldPhase(storyPlayer, dialogue, font, pixel, diarySprite,
                    DialogueData.DIARY, VIEW_W, VIEW_H);
            case 2:
                if (passagePhase == null) {
                    passagePhase = new ExplorePhase(storyPlayer, dialogue, font, pixel,
                        VIEW_W, VIEW_H);
                    passagePhase.setRoomSize(INNER_ROOM_W, INNER_ROOM_H);
                    passagePhase.setRoomScenery(floorTile, shadowTree, closedEyeAnimation);
                    passagePhase.setExit(new Rectangle(INNER_ROOM_W - 88f, INNER_ROOM_H / 2f - 70f, 40f, 140f));
                }
                return passagePhase;
            case 3:
                if (npcPhase == null) {
                    npcPhase = new FlowerQuestPhase(storyPlayer, dialogue, font, pixel,
                        npcAnimations, smileAnimation,
                        bobFrownAnimation, VIEW_W, VIEW_H);
                    npcPhase.setRoomScenery(floorTile, shadowTree, closedEyeAnimation);
                }
                return npcPhase;
            case 4:
                return new RealWorldPhase(storyPlayer, dialogue, font, pixel, diarySprite,
                    DialogueData.AFTER_REVEAL, VIEW_W, VIEW_H);
            case 5:
                storyPlayer.stopMovement();
                storyPlayer.setTopDown(false);
                storyPlayer.setGroundY(GROUND_Y);
                storyPlayer.getPosition().x = 150f;
                World secondWorld = new World();
                float secondMargin = DRAW_W + 24f;
                Boss secondBoss = new Boss(VIEW_W - 200f, GROUND_Y, BOSS_SCALE, BOSS_HEALTH,
                    secondMargin, VIEW_W - secondMargin, 0.75f,
                    bossAnimations, waveAnimation, storyPlayer, secondWorld);
                FightPhase secondFight = new FightPhase(secondWorld, storyPlayer, secondBoss, camera, font, openingEyeAnimation,
                    crack, pixel, VIEW_W, VIEW_H, mobAnimation, mob2Animation);
                secondFight.enableSupportAndShieldExit();
                secondFight.setRageArt(new TextureRegion(rageWaveTexture), new TextureRegion(rageMeterTexture, 0, 0, 48, 128));
                secondFight.setCombatSounds(punchHit1, punchHit2, heavyHit, rageLaunch, rageExplosion, shieldImpact);
                return secondFight;
            case 6:
                FlowerRoomPhase flowers = new FlowerRoomPhase(storyPlayer, dialogue, font, pixel,
                    flowerSprite, DialogueData.FLOWERS, VIEW_W, VIEW_H);
                flowers.setRoomScenery(floorTile, shadowTree, openEyeAnimation);
                return flowers;
            default:
                return null; // Further story phases are not implemented yet.
        }
    }

    private void nextPhase() {
        int previousIndex = phaseIndex;
        int direction = currentPhase instanceof ExplorePhase
            ? ((ExplorePhase) currentPhase).getExitDirection() : 1;
        currentPhase.dispose();
        phaseIndex += direction;
        resetCamera();
        currentPhase = buildPhase(phaseIndex);
        if (phaseIndex == 2) {
            passagePhase.enterAt(previousIndex == 3 ? INNER_ROOM_W - 150f : 80f, INNER_ROOM_H / 2f);
        } else if (phaseIndex == 3) {
            npcPhase.enterAt(90f, INNER_ROOM_H / 2f);
        }
    }

    private void resetCamera() {
        camera.position.set(VIEW_W / 2f, VIEW_H / 2f, 0f);
        camera.update();
    }

    private void updateSceneMusic() {
        Music wanted = null;
        if (currentPhase instanceof RealWorldPhase) wanted = diaryMusic;
        else if (currentPhase instanceof FlowerQuestPhase) {
            FlowerQuestPhase.QuestState state = ((FlowerQuestPhase) currentPhase).getQuestState();
            if (state == FlowerQuestPhase.QuestState.NOT_ACCEPTED
                || state == FlowerQuestPhase.QuestState.CARRYING) wanted = npcMusic;
        }
        if (wanted == activeMusic) return;
        if (activeMusic != null) activeMusic.stop();
        activeMusic = wanted;
        if (activeMusic != null) activeMusic.play();
    }

    @Override
    protected void update(float delta) {
        if (currentPhase != null) {
            currentPhase.update(delta);
            if (currentPhase instanceof FightPhase && ((FightPhase) currentPhase).isDeathReady()) {
                retryPhase = phaseIndex;
                currentPhase.dispose();
                resetCamera();
                currentPhase = new DeathDiaryPhase(playerAnimations.get(Player.State.IDLE).getKeyFrame(0f),
                    pixel, font, viewport);
            } else if (currentPhase.isFinished()) {
                if (currentPhase instanceof MenuPhase) {
                    currentPhase.dispose();
                    phaseIndex = retryPhase >= 0 ? retryPhase : START_PHASE;
                    retryPhase = -1;
                    storyPlayer = null;
                    passagePhase = null;
                    npcPhase = null;
                    dialogue = new DialogueBox(font, pixel, VIEW_W);
                    currentPhase = buildPhase(phaseIndex);
                } else if (currentPhase instanceof DeathDiaryPhase) {
                    currentPhase.dispose();
                    resetCamera();
                    currentPhase = buildMenu();
                } else nextPhase();
            }
        }
        if (currentPhase == null && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            retryPhase = -1;
            resetCamera();
            currentPhase = buildMenu();
        }
        updateSceneMusic();
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
            font.draw(batch, "Press ENTER to return to the menu.", 155f, 165f);
        }
    }

    @Override
    protected void drawUI() { stage.draw(); }

    @Override
    public void resize(int width, int height) { viewport.update(width, height, true); }

    @Override
    public void dispose() {
        if (activeMusic != null) activeMusic.stop();
        diaryMusic.dispose();
        npcMusic.dispose();
        if (currentPhase != null) currentPhase.dispose();
        punchHit1.dispose();
        punchHit2.dispose();
        heavyHit.dispose();
        rageLaunch.dispose();
        rageExplosion.dispose();
        shieldImpact.dispose();
        stage.dispose();
        font.dispose();
        playerSheet.dispose();
        jumpSheet.dispose();
        bossSheet.dispose();
        mobSheet.dispose();
        mob2Sheet.dispose();
        diaryTexture.dispose();
        flowerTexture.dispose();
        forestSheet.dispose();
        smileSheet.dispose();
        bobFrownSheet.dispose();
        rageWaveTexture.dispose();
        rageMeterTexture.dispose();
        menuSheet.dispose();
        exitMenuSheet.dispose();
        super.dispose();
    }

    public OrthographicCamera getCamera() { return camera; }
    public Viewport getViewport() { return viewport; }
    public Stage getStage() { return stage; }
}
