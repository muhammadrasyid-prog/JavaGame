package io.github.jekjek.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import io.github.jekjek.Entity.Player;
import io.github.jekjek.Main;

public class GameScreen implements Screen {
    Main game;
    SpriteBatch batch;
    BitmapFont font;

    Texture idleTex;
    Texture runTex;
    Texture bulletTexture;
    Texture enemyTex;
    Texture bgTex;

    Animation<TextureRegion> currentAnim;

    Animation<TextureRegion> idleRightAnim;
    Animation<TextureRegion> runRightAnim;
    Animation<TextureRegion> enemyAnim;

    Array<Bullet> bullets;

    float stateTime = 0f;

    com.badlogic.gdx.graphics.OrthographicCamera camera;
    StretchViewport viewport;
    Stage overlayStage;           // stage khusus untuk TransitionOverlay
    TransitionOverlay transition;
    BitmapFont fontLarge, fontSmall;

    // Dimensions will be dynamically calculated

    // worldX = seberapa jauh MC sudah berjalan di "dunia"
    // MC selalu digambar di posisi layar tetap (PLAYER_SCREEN_X)
    float worldX = 0f;
    float y = 100f;
    boolean facingRight = true;

    // Posisi MC di layar (tetap, tidak bergerak)
    private static final float PLAYER_SCREEN_X = 180f;
    // Jarak encounter diukur dalam koordinat dunia
    private static final float ENCOUNTER_WORLD_DIST = 600f;

    String difficulty;
    public int currentWave;
    int maxWaves;
    public float encounterX;       // koordinat dunia tempat musuh spawn
    com.badlogic.gdx.math.Rectangle playerRect;
    com.badlogic.gdx.math.Rectangle encounterRect;
    public boolean isTransitioning = false;

    public GameScreen(Main game, String difficulty, int currentWave, float startX) {
        this.game = game;
        this.font = new BitmapFont();
        this.difficulty = difficulty;
        this.currentWave = currentWave;
        this.worldX = startX;

        if (difficulty.equals("Easy")) maxWaves = 2;
        else if (difficulty.equals("Normal")) maxWaves = 3;
        else maxWaves = 4;

        this.encounterX = startX + ENCOUNTER_WORLD_DIST;
        this.playerRect  = new com.badlogic.gdx.math.Rectangle();
        this.encounterRect = new com.badlogic.gdx.math.Rectangle();
    }

    @Override
    public void show() {
        camera       = new com.badlogic.gdx.graphics.OrthographicCamera();
        viewport     = new StretchViewport(800, 480, camera);
        overlayStage = new Stage(viewport, game.batch);
        transition   = new TransitionOverlay();

        fontLarge = new BitmapFont(); fontLarge.getData().setScale(2.8f);
        fontSmall = new BitmapFont(); fontSmall.getData().setScale(1.1f);

        idleTex = new Texture("MC/idle.png");
        runTex = new Texture("MC/run.png");
        bgTex = new Texture("bg_idle.png");

        int frameWidth = 768;
        int frameHeight = 448;

        TextureRegion[][] idleTmp = TextureRegion.split(idleTex, frameWidth, frameHeight);
        idleRightAnim = new Animation<>(0.15f, 
            idleTmp[0][0], idleTmp[0][1], idleTmp[0][2], idleTmp[0][3]
        );
        idleRightAnim.setPlayMode(Animation.PlayMode.LOOP);

        TextureRegion[][] runTmp = TextureRegion.split(runTex, frameWidth, frameHeight);
        runRightAnim = new Animation<>(0.1f, 
            runTmp[0][0], runTmp[0][1], runTmp[0][2], runTmp[0][3]
        );
        runRightAnim.setPlayMode(Animation.PlayMode.LOOP);

        bulletTexture = new Texture("bullet.png");
        bullets = new Array<>();
        
        enemyTex = new Texture("default_enemy.png");
        TextureRegion[][] enemyTmp = TextureRegion.split(enemyTex, frameWidth, frameHeight);
        enemyAnim = new Animation<>(0.15f, 
            enemyTmp[0][0], enemyTmp[0][1], enemyTmp[0][2], enemyTmp[0][3]
        );
        enemyAnim.setPlayMode(Animation.PlayMode.LOOP);

        currentAnim = idleRightAnim;

        // Tampilkan animasi "MULAI EKSPLORASI" saat masuk GameScreen
        transition.show(overlayStage, fontLarge, fontSmall,
            "EKSPLORASI", "Wave " + currentWave + " / " + maxWaves + "  —  Temukan musuhmu!",
            new Color(0.4f, 0.85f, 1f, 1f), 1.0f, null);
    }

    @Override
    public void render(float delta) {

        // ═══ GAME LOGIC (diblokir saat transisi sedang berjalan) ═══════════
        if (!isTransitioning) {
            boolean moving = Gdx.input.isKeyPressed(Input.Keys.D);

            // Movement — worldX naik, MC tetap di layar
            if (moving) worldX += 200f * delta;

            // State → Anim
            Animation<TextureRegion> nextAnim = moving ? runRightAnim : idleRightAnim;
            if (currentAnim != nextAnim) { stateTime = 0; currentAnim = nextAnim; }

            // Shoot
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
                bullets.add(new Bullet(bulletTexture, PLAYER_SCREEN_X, y, true));
            for (int i = bullets.size - 1; i >= 0; i--) {
                Bullet b = bullets.get(i); b.update(delta);
                if (!b.isActive()) bullets.removeIndex(i);
            }

            // Collision check — trigger encounter animation then switch screen
            float eSX = PLAYER_SCREEN_X + (encounterX - worldX);
            TextureRegion fr  = currentAnim.getKeyFrame(stateTime);
            float dw = fr.getRegionWidth() / 4f, dh = fr.getRegionHeight() / 4f;
            TextureRegion ef  = enemyAnim.getKeyFrame(stateTime);
            float ew = ef.getRegionWidth() / 4f, eh = ef.getRegionHeight() / 4f;
            playerRect.set(PLAYER_SCREEN_X, y, dw, dh);
            encounterRect.set(eSX, 100, ew, eh);

            if (playerRect.overlaps(encounterRect)) {
                isTransitioning = true;
                transition.show(overlayStage, fontLarge, fontSmall,
                    "ENCOUNTER!", "Wave " + currentWave + " — Bersiap tempur!",
                    new Color(1f, 0.35f, 0.25f, 1f), 0.9f,
                    () -> game.setScreen(new BattleScreen(game, difficulty, currentWave, this)));
            }
        }

        // Animasi karakter selalu berjalan (biar freeze di frame yg bagus)
        stateTime += delta;

        // ═══ RENDER (selalu, termasuk saat transisi) ════════════════════════
        ScreenUtils.clear(0.12f, 0.12f, 0.18f, 1f);
        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Background parallax
        float BG_W = 800f, BG_H = 480f;
        float bgShift = (worldX * 0.5f) % BG_W;
        game.batch.draw(bgTex, -bgShift,        0, BG_W, BG_H);
        game.batch.draw(bgTex,  BG_W - bgShift, 0, BG_W, BG_H);

        // Musuh
        TextureRegion enemyFrame = enemyAnim.getKeyFrame(stateTime);
        float eScreenX = PLAYER_SCREEN_X + (encounterX - worldX);
        game.batch.draw(enemyFrame, eScreenX, 100,
            enemyFrame.getRegionWidth() / 4f, enemyFrame.getRegionHeight() / 4f);

        // MC — selalu di PLAYER_SCREEN_X
        TextureRegion playerFrame = currentAnim.getKeyFrame(stateTime);
        game.batch.draw(playerFrame, PLAYER_SCREEN_X, y,
            playerFrame.getRegionWidth() / 4f, playerFrame.getRegionHeight() / 4f);

        // Bullets
        for (Bullet b : bullets) b.draw(game.batch);

        // HUD
        font.draw(game.batch,
            "Wave " + currentWave + "/" + maxWaves +
            "  |  Jarak: " + Math.max(0, (int)(encounterX - worldX)) + "m",
            16, 470);

        game.batch.end();

        // ═══ OVERLAY — selalu di paling atas, selalu di-update ══════════════
        overlayStage.act(delta);
        overlayStage.draw();
    }

    @Override public void dispose() {
        idleTex.dispose();
        runTex.dispose();
        bgTex.dispose();
        bulletTexture.dispose();
        if (enemyTex != null) enemyTex.dispose();
        font.dispose();
        if (fontLarge != null) fontLarge.dispose();
        if (fontSmall != null) fontSmall.dispose();
        if (transition != null) transition.dispose();
        if (overlayStage != null) overlayStage.dispose();
    }

    @Override public void resize(int width, int height) {
        if (viewport != null) viewport.update(width, height, true);
        if (overlayStage != null) overlayStage.getViewport().update(width, height, true);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
