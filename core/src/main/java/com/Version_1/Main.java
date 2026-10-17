package com.Version_1;

import com.Version_1.Engine.GameEngineBase;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.Version_1.Phases.Phase;

/**
 * Orchestrator. Owns the shared things (batch via base, camera, viewport, stage,
 * textures) and runs ONE Phase at a time, swapping when it reports finished.
 */
public class Main extends GameEngineBase {
    // Virtual resolution in world units (pixel-art friendly). Change to taste.
    public static final float VIEW_W = 640;
    public static final float VIEW_H = 360;

    private OrthographicCamera camera;
    private Viewport viewport;
    private Stage stage;

    private Phase currentPhase;
    private int phaseIndex = 0;

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
    }

    /** The story order lives here. Add a case as each phase gets written. */
    private Phase buildPhase(int index) {
        switch (index) {
            // case 0: return new FightPhase(...);      // fight 1 -> screen crack
            // case 1: return new RealWorldPhase(...);
            // case 3: return new ExplorePhase(...);
            // case 2: return new FightPhase(...);      // fight 2 (different params)
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
        // TODO: dispose the textures you load in create()
        super.dispose(); // disposes the batch
    }

    // Phases take only what they need from these, passed through their constructors.
    public OrthographicCamera getCamera() { return camera; }
    public Viewport getViewport() { return viewport; }
    public Stage getStage() { return stage; }
}
