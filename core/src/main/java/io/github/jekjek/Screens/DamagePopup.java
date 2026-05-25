package io.github.jekjek.Screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

public class DamagePopup {
    private float x, y;
    private String text;
    private float lifeTime = 1.0f;  // 1 detik
    private float elapsedTime = 0f;
    private float startY;
    private Color color;
    
    public DamagePopup(float x, float y, int damage, boolean isCritical) {
        this.x = x;
        this.y = y;
        this.startY = y;
        this.text = String.valueOf(damage);
        
        if (isCritical) {
            this.text = "CRIT! " + damage + " !!!";
            this.color = Color.ORANGE;
        } else {
            this.color = Color.RED;
        }
    }
    
    public void update(float delta) {
        elapsedTime += delta;
        // Floating up effect
        y = startY + (elapsedTime / lifeTime) * 40f;
    }
    
    public void draw(SpriteBatch batch, BitmapFont font) {
        if (!isAlive()) return;
        
        float alpha = 1.0f - (elapsedTime / lifeTime);
        font.setColor(color.r, color.g, color.b, alpha);
        font.draw(batch, text, x, y);
        font.setColor(Color.WHITE); // reset
    }
    
    public boolean isAlive() {
        return elapsedTime < lifeTime;
    }
}