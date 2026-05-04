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

    Texture playerSheet;
    Texture bulletTexture;

    Animation<TextureRegion> currentAnim;

    Animation<TextureRegion> idleRightAnim, idleLeftAnim;
    Animation<TextureRegion> runRightAnim, runLeftAnim;
    Animation<TextureRegion> shootIdleRightAnim, shootIdleLeftAnim;
    Animation<TextureRegion> shootRunRightAnim, shootRunLeftAnim;
    Animation<TextureRegion> jumpRightAnim, jumpLeftAnim;
    Animation<TextureRegion> crouchRightAnim, crouchLeftAnim;

    Array<Bullet> bullets;

    float stateTime = 0f;

    static final int FRAME_SIZE = 48;

    float x = 100, y = 100;
    boolean facingRight = true;

    public GameScreen(Main game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
    }

    @Override
    public void show() {
        playerSheet = new Texture("player.png");
        TextureRegion[][] tmp = TextureRegion.split(playerSheet, FRAME_SIZE, FRAME_SIZE);

        // ===== IDLE =====
        idleRightAnim = new Animation<>(0.15f, tmp[0][0], tmp[0][1], tmp[0][2], tmp[0][3], tmp[0][4]);
        idleLeftAnim  = new Animation<>(0.15f, tmp[1][0], tmp[1][1], tmp[1][2], tmp[1][3], tmp[1][4]);

        // ===== RUN =====
        runRightAnim = new Animation<>(0.1f,
            tmp[3][0], tmp[3][1], tmp[3][2], tmp[3][3],
            tmp[3][4], tmp[3][5], tmp[3][6], tmp[3][7]);

        runLeftAnim = new Animation<>(0.1f,
            tmp[2][0], tmp[2][1], tmp[2][2], tmp[2][3],
            tmp[2][4], tmp[2][5], tmp[2][6], tmp[2][7]);

        // ===== SHOOT =====
        shootRunRightAnim = new Animation<>(0.07f,
            tmp[5][0], tmp[5][1], tmp[5][2], tmp[5][3],
            tmp[5][4], tmp[5][5], tmp[5][6], tmp[5][7]);

        shootRunLeftAnim = new Animation<>(0.07f,
            tmp[4][0], tmp[4][1], tmp[4][2], tmp[4][3],
            tmp[4][4], tmp[4][5], tmp[4][6], tmp[4][7]);

        shootIdleRightAnim = new Animation<>(0.1f,
            tmp[7][0], tmp[7][1], tmp[7][2], tmp[7][3], tmp[7][4]);

        shootIdleLeftAnim = new Animation<>(0.1f,
            tmp[6][0], tmp[6][1], tmp[6][2], tmp[6][3], tmp[6][4]);

        // ===== CROUCH & JUMP (kalau cuma 1 frame) =====
        crouchRightAnim = new Animation<>(0.1f, tmp[7][4]);
        crouchLeftAnim  = new Animation<>(0.1f, tmp[6][4]);

        jumpRightAnim = new Animation<>(0.1f, tmp[7][2]);
        jumpLeftAnim  = new Animation<>(0.1f, tmp[6][2]);

        // LOOP
        idleRightAnim.setPlayMode(Animation.PlayMode.LOOP);
        idleLeftAnim.setPlayMode(Animation.PlayMode.LOOP);
        runRightAnim.setPlayMode(Animation.PlayMode.LOOP);
        runLeftAnim.setPlayMode(Animation.PlayMode.LOOP);
        shootRunRightAnim.setPlayMode(Animation.PlayMode.LOOP);
        shootRunLeftAnim.setPlayMode(Animation.PlayMode.LOOP);
        shootIdleRightAnim.setPlayMode(Animation.PlayMode.LOOP);
        shootIdleLeftAnim.setPlayMode(Animation.PlayMode.LOOP);

        bulletTexture = new Texture("bullet.png");
        bullets = new Array<>();

        currentAnim = idleRightAnim;
    }

    @Override
    public void render(float delta) {
        // ===== INPUT =====
        boolean left = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D);
        boolean jump = Gdx.input.isKeyPressed(Input.Keys.W);
        boolean crouch = Gdx.input.isKeyPressed(Input.Keys.S);
        boolean shoot = Gdx.input.isKeyPressed(Input.Keys.SPACE);
        boolean moving = left || right;

        float speed = 200 * delta;

        // ===== MOVEMENT =====
        if (left) {
            x -= speed;
            facingRight = false;
        } else if (right) {
            x += speed;
            facingRight = true;
        }

        // ===== STATE → ANIM =====
        Animation<TextureRegion> nextAnim;

        if (shoot && moving) {
            nextAnim = facingRight ? shootRunRightAnim : shootRunLeftAnim;
        } else if (shoot) {
            nextAnim = facingRight ? shootIdleRightAnim : shootIdleLeftAnim;
        } else if (moving) {
            nextAnim = facingRight ? runRightAnim : runLeftAnim;
        } else {
            nextAnim = facingRight ? idleRightAnim : idleLeftAnim;
        }

        // PRIORITY OVERRIDE
        if (crouch) {
            nextAnim = facingRight ? crouchRightAnim : crouchLeftAnim;
        } else if (jump) {
            nextAnim = facingRight ? jumpRightAnim : jumpLeftAnim;
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

        for (Bullet bullet : bullets) {
            bullet.update(delta);
        }

        // ===== RENDER =====
        stateTime += delta;
        TextureRegion frame = currentAnim.getKeyFrame(stateTime);

        ScreenUtils.clear(0.2f, 0.5f, 0.8f, 1);

        game.batch.begin();
        game.batch.draw(frame, x, y, FRAME_SIZE * 2, FRAME_SIZE * 2);

        for (Bullet bullet : bullets) {
            bullet.draw(game.batch);
        }

        // DEBUG TEXT
        font.draw(game.batch, "X: " + x, 20, 50);
        font.draw(game.batch, facingRight ? "RIGHT" : "LEFT", 20, 30);

        game.batch.end();
    }

    @Override public void dispose() {
        playerSheet.dispose();
        bulletTexture.dispose();
        font.dispose();
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
