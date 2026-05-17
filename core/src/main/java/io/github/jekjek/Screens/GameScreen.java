package io.github.jekjek.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
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
    com.badlogic.gdx.utils.viewport.Viewport viewport;

    // Dimensions will be dynamically calculated

    float x = 100, y = 100;
    boolean facingRight = true;

    String difficulty;
    int currentWave;
    int maxWaves;
    float encounterX;
    com.badlogic.gdx.math.Rectangle playerRect;
    com.badlogic.gdx.math.Rectangle encounterRect;
    boolean isTransitioning = false;

    public GameScreen(Main game, String difficulty, int currentWave, float startX) {
        this.game = game;
        this.font = new BitmapFont();
        this.difficulty = difficulty;
        this.currentWave = currentWave;
        this.x = startX;
        
        if (difficulty.equals("Easy")) maxWaves = 2;
        else if (difficulty.equals("Normal")) maxWaves = 3;
        else maxWaves = 4;
        
        this.encounterX = startX + 600f;
        this.playerRect = new com.badlogic.gdx.math.Rectangle();
        this.encounterRect = new com.badlogic.gdx.math.Rectangle();
    }

    @Override
    public void show() {
        camera = new com.badlogic.gdx.graphics.OrthographicCamera();
        viewport = new com.badlogic.gdx.utils.viewport.FitViewport(800, 480, camera);

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
    }

    @Override
    public void render(float delta) {
        if (isTransitioning) return;

        // ===== INPUT =====
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D);
        boolean moving = right;
        facingRight = true; // Force right facing as per sidescroller logic

        float speed = 200 * delta;

        // ===== MOVEMENT =====
        if (right) {
            x += speed;
        }

        // Calculate dynamic draw size based on current frame to keep aspect ratio
        TextureRegion currentFrame = currentAnim.getKeyFrame(stateTime);
        float drawWidth = currentFrame.getRegionWidth() / 4f;
        float drawHeight = currentFrame.getRegionHeight() / 4f;

        TextureRegion enemyFrameTop = enemyAnim.getKeyFrame(stateTime);
        float enemyDrawW = enemyFrameTop.getRegionWidth() / 4f;
        float enemyDrawH = enemyFrameTop.getRegionHeight() / 4f;

        float bgOffset = (x - 100) * 0.5f; // calculate parallax offset
        float visualEnemyX = encounterX - bgOffset;

        playerRect.set(x, y, drawWidth, drawHeight);
        encounterRect.set(visualEnemyX, 100, enemyDrawW, enemyDrawH);
        
        if (playerRect.overlaps(encounterRect)) {
            isTransitioning = true;
            game.setScreen(new BattleScreen(game, difficulty, currentWave, this));
            return;
        }

        // ===== STATE → ANIM =====
        Animation<TextureRegion> nextAnim;

        if (moving) {
            nextAnim = runRightAnim;
        } else {
            nextAnim = idleRightAnim;
        }

        // APPLY
        if (currentAnim != nextAnim) {
            stateTime = 0;
            currentAnim = nextAnim;
        }

        // ===== SHOOT =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            bullets.add(new Bullet(bulletTexture, x, y, facingRight));
        }

        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.update(delta);
            if (!bullet.isActive()) {
                bullets.removeIndex(i);
            }
        }

        // ===== RENDER =====
        stateTime += delta;
        TextureRegion frame = currentAnim.getKeyFrame(stateTime);
        TextureRegion enemyFrame = enemyAnim.getKeyFrame(stateTime);

        ScreenUtils.clear(0.2f, 0.5f, 0.8f, 1);

        drawWidth = frame.getRegionWidth() / 4f;
        drawHeight = frame.getRegionHeight() / 4f;

        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        
        // Scrolling background logic (parallax effect)
        float bgWidth = 800;
        float bgHeight = 480;
        float shift = bgOffset % bgWidth;
        
        game.batch.draw(bgTex, -shift, 0, bgWidth, bgHeight);
        game.batch.draw(bgTex, bgWidth - shift, 0, bgWidth, bgHeight);
        
        float enemyDrawWBottom = enemyFrame.getRegionWidth() / 4f;
        float enemyDrawHBottom = enemyFrame.getRegionHeight() / 4f;
        
        game.batch.draw(frame, x, y, drawWidth, drawHeight);
        game.batch.draw(enemyFrame, visualEnemyX, 100, enemyDrawWBottom, enemyDrawHBottom);

        for (Bullet bullet : bullets) {
            bullet.draw(game.batch);
        }

        // DEBUG TEXT
        font.draw(game.batch, "Find the enemy to start Wave " + currentWave + " / " + maxWaves + "!", 20, 70);
        font.draw(game.batch, "X: " + x, 20, 50);
        font.draw(game.batch, facingRight ? "RIGHT" : "LEFT", 20, 30);

        game.batch.end();
    }

    @Override public void dispose() {
        idleTex.dispose();
        runTex.dispose();
        bgTex.dispose();
        bulletTexture.dispose();
        if (enemyTex != null) enemyTex.dispose();
        font.dispose();
    }

    @Override public void resize(int width, int height) {
        if (viewport != null) viewport.update(width, height, true);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
