package io.github.jekjek.Screens;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class Bullet {
    private float x, y;
    private float speed;
    private boolean active;
    private Texture texture;
    private boolean goRight;

    public Bullet(Texture texture, float x, float y, boolean goRight) {
        this.texture = texture;
        this.active = true;
        this.goRight = goRight;

        // Sesuaikan speed: di LibGDX, speed 8 itu pelan banget.
        // 500f-800f biasanya baru kerasa pas.
        this.speed = 600f;

        // --- RAHASIA BIAR PAS ---
        // Kita tiru logika "pas" kamu dengan offset yang spesifik
        // Asumsi: tangan MC ada di sekitar pixel ke-35 (tinggi)
        // dan pixel ke-40 (lebar)
        this.y = y + 35;

        if (goRight) {
            this.x = x + 45; // Muncul di depan MC (kanan)
        } else {
            this.x = x - 10; // Muncul di depan MC (kiri)
        }
    }

    public void update(float delta) {
        if (goRight) {
            x += speed * delta;
        } else {
            x -= speed * delta;
        }

        // Deaktivasi jika keluar layar (misal lebar layar 800)
        if (x > 800 || x < -50) {
            active = false;
        }
    }

    public void draw(SpriteBatch batch) {
        // Kita gambar kecil saja biar proporsional (20x10 pixel)
        batch.draw(texture, x, y, 20, 10);
    }

    public boolean isActive() {
        return active;
    }
}
