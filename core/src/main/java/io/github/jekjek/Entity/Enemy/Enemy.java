package io.github.jekjek.Entity.Enemy;

import io.github.jekjek.Entity.Entity;

public abstract class Enemy extends Entity {
    protected Enemy(String name, double health, double armor, double attack) {
        super(name, health, armor, attack);
    }
}
