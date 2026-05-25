package io.github.jekjek.Screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class EnemyInfo {
    private String name;
    private float x, y;
    private int hp, maxHp;
    private int armor;
    private boolean isAlive;
    
    public EnemyInfo(String name, float x, float y, int hp, int maxHp, int armor) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.hp = hp;
        this.maxHp = maxHp;
        this.armor = armor;
        this.isAlive = true;
    }
    
    public void update(int newHp, int newArmor, boolean alive) {
        this.hp = newHp;
        this.armor = newArmor;
        this.isAlive = alive;
    }
    
    public void draw(SpriteBatch batch, BitmapFont font) {
        if (!isAlive) return;
        
        float nameY = y + 80;
        float hpY = y + 65;
        float armorY = y + 50;
        
        font.setColor(Color.YELLOW);
        font.draw(batch, name, x + 10, nameY);
        
        font.setColor(Color.RED);
        font.draw(batch, "HP: " + hp + "/" + maxHp, x + 10, hpY);
        
        font.setColor(Color.CYAN);
        font.draw(batch, "DEF: " + armor, x + 10, armorY);
        
        font.setColor(Color.WHITE); // reset
    }
    
    public boolean isAlive() { return isAlive; }
    public float getX() { return x; }
    public float getY() { return y; }
}