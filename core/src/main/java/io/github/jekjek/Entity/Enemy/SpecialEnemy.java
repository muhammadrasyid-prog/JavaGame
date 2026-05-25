package io.github.jekjek.Entity.Enemy;

import java.util.List;

public abstract class SpecialEnemy extends Enemy implements SkillCaster {
    public SpecialEnemy(String name, double health, double armor, double attack) {
        super(name, health, armor, attack);
    }

    public abstract boolean shouldUseSkill(List<Enemy> allyTeam);

    @Override
    public abstract void executeSkill(List<Enemy> allyTeam);

    public abstract void decrementSkillCooldown();

    public abstract int getSkillCooldown();

    public abstract String getSkillName();
}
