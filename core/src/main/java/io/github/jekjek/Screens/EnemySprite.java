package io.github.jekjek.Screens;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class EnemySprite {
    private Texture idleTexture;
    private Texture attackTexture;
    private Texture hurtTexture;
    private Texture deadTexture;

    private float x, y;
    private float stateTime = 0f;
    private float stateDuration = 0f; // berapa lama state ini bertahan
    private EnemyState currentState = EnemyState.IDLE;

    public enum EnemyState {
        IDLE, ATTACK, HURT, DEAD
    }

    // Durasi animasi tiap state (detik)
    private static final float ATTACK_DURATION = 0.4f;
    private static final float HURT_DURATION   = 0.3f;
    // DEAD dan IDLE tidak auto-kembali

    public EnemySprite(float x, float y, String idleFile, String attackFile, String hurtFile, String deadFile) {
        this.x = x;
        this.y = y;
        try {
            idleTexture   = new Texture(idleFile);
            attackTexture = new Texture(attackFile);
            hurtTexture   = new Texture(hurtFile);
            deadTexture   = new Texture(deadFile);
            System.out.println("[EnemySprite] Loaded: " + idleFile);
        } catch (Exception e) {
            System.out.println("[EnemySprite] Failed to load textures: " + e.getMessage());
            idleTexture   = createPlaceholderTexture(0.2f, 0.4f, 0.8f);
            attackTexture = createPlaceholderTexture(0.8f, 0.2f, 0.2f);
            hurtTexture   = createPlaceholderTexture(0.8f, 0.8f, 0.2f);
            deadTexture   = createPlaceholderTexture(0.2f, 0.2f, 0.2f);
        }
    }

    public Texture getIdleTexture() { return idleTexture; }
    public float getX() { return x; }
    public float getY() { return y; }

    private Texture createPlaceholderTexture(float r, float g, float b) {
        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, 1f);
        pixmap.fill();
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.drawRectangle(0, 0, 63, 63);
        pixmap.fillCircle(20, 40, 6);
        pixmap.fillCircle(44, 40, 6);
        pixmap.setColor(0f, 0f, 0f, 1f);
        pixmap.fillCircle(20, 40, 3);
        pixmap.fillCircle(44, 40, 3);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    public void setState(EnemyState state) {
        if (state == EnemyState.DEAD) {
            // DEAD tidak bisa di-override kecuali reset
            currentState = EnemyState.DEAD;
            stateTime = 0f;
            return;
        }
        if (currentState == EnemyState.DEAD) return; // sudah mati, abaikan

        currentState = state;
        stateTime = 0f;

        switch (state) {
            case ATTACK: stateDuration = ATTACK_DURATION; break;
            case HURT:   stateDuration = HURT_DURATION;   break;
            default:     stateDuration = 0f; break; // IDLE = permanen
        }
    }

    public void update(float delta) {
        stateTime += delta;

        // Auto-kembali ke IDLE setelah durasi habis
        if (currentState == EnemyState.ATTACK || currentState == EnemyState.HURT) {
            if (stateDuration > 0 && stateTime >= stateDuration) {
                currentState = EnemyState.IDLE;
                stateTime = 0f;
                stateDuration = 0f;
            }
        }
    }

    public void draw(SpriteBatch batch) {
        Texture currentTexture;
        switch (currentState) {
            case ATTACK: currentTexture = attackTexture; break;
            case HURT:   currentTexture = hurtTexture;   break;
            case DEAD:   currentTexture = deadTexture;   break;
            default:     currentTexture = idleTexture;   break;
        }
        batch.draw(currentTexture, x, y, 64, 64);
    }

    public EnemyState getState() { return currentState; }

    public void dispose() {
        if (idleTexture   != null) idleTexture.dispose();
        if (attackTexture != null) attackTexture.dispose();
        if (hurtTexture   != null) hurtTexture.dispose();
        if (deadTexture   != null) deadTexture.dispose();
    }
}