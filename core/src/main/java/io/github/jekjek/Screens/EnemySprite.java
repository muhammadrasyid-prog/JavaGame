package io.github.jekjek.Screens;

import com.badlogic.gdx.graphics.Color;
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
    private EnemyState currentState = EnemyState.IDLE;
    
    public enum EnemyState {
        IDLE, ATTACK, HURT, DEAD
    }
    
    public EnemySprite(float x, float y) {
        this.x = x;
        this.y = y;
        
        // Load textures langsung dari folder assets (tanpa subfolder)
        try {
            idleTexture = new Texture("defualt_enemy.png");
            attackTexture = new Texture("enemy_attack.png");
            hurtTexture = new Texture("enemy_hurt.png");
            deadTexture = new Texture("enemy_dead.png");
            System.out.println("[EnemySprite] Loaded enemy textures from assets/");
        } catch (Exception e) {
            System.out.println("[EnemySprite] Failed to load textures: " + e.getMessage());
            // Fallback ke placeholder warna jika file tidak ada
            idleTexture = createPlaceholderTexture(0.2f, 0.4f, 0.8f);
            attackTexture = createPlaceholderTexture(0.8f, 0.2f, 0.2f);
            hurtTexture = createPlaceholderTexture(0.8f, 0.8f, 0.2f);
            deadTexture = createPlaceholderTexture(0.2f, 0.2f, 0.2f);
        }
    }
    
    private Texture createPlaceholderTexture(float r, float g, float b) {
        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, 1f);
        pixmap.fill();
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.drawRectangle(0, 0, 63, 63);
        // Gambar mata sederhana
        pixmap.setColor(1f, 1f, 1f, 1f);
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
        if (currentState != state) {
            currentState = state;
            stateTime = 0f;
        }
    }
    
    public void update(float delta) {
        stateTime += delta;
    }
    
    public void draw(SpriteBatch batch) {
        Texture currentTexture;
        switch (currentState) {
            case ATTACK:
                currentTexture = attackTexture;
                break;
            case HURT:
                currentTexture = hurtTexture;
                break;
            case DEAD:
                currentTexture = deadTexture;
                break;
            case IDLE:
            default:
                currentTexture = idleTexture;
                break;
        }
        batch.draw(currentTexture, x, y, 64, 64);
    }
    
    public void dispose() {
        if (idleTexture != null) idleTexture.dispose();
        if (attackTexture != null) attackTexture.dispose();
        if (hurtTexture != null) hurtTexture.dispose();
        if (deadTexture != null) deadTexture.dispose();
    }
}