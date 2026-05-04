package io.github.jekjek.Entity.Enemy;

public class ShieldEnemy extends SpecialEnemy {
    private static final int COOLDOWN_DURATION = 2;
    private int skillCooldown = 0;

    public ShieldEnemy(String name, double health, double armor, double attack) {
        super(name, health, armor, attack);
    }

    @Override
    public boolean shouldUseSkill(java.util.List<Enemy> allyTeam) {
        if (skillCooldown > 0) {
            return false;
        }

        return getHealth() < getMaxHealth() * 0.6 || hasWeakAlly(allyTeam);
    }

    private boolean hasWeakAlly(java.util.List<Enemy> allyTeam) {
        for (Enemy ally : allyTeam) {
            if (ally != this && ally.isAlive() && ally.getHealth() < ally.getMaxHealth() * 0.4) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void executeSkill(java.util.List<Enemy> allyTeam) {
        if (getHealth() < getMaxHealth() * 0.6) {
            setDamageReductionPercent(5);
            System.out.println(getNama() + " activates personal shield! Damage reduction: 5%");
        } else {
            for (Enemy ally : allyTeam) {
                if (ally != this && ally.isAlive()) {
                    ally.setDamageReductionPercent(2.5);
                }
            }
            System.out.println(getNama() + " activates team shield! Team damage reduction: 2.5%");
        }
        skillCooldown = COOLDOWN_DURATION;
    }

    @Override
    public void decrementSkillCooldown() {
        if (skillCooldown > 0) {
            skillCooldown--;
        }
        clearDamageReductionPercent();
    }

    @Override
    public int getSkillCooldown() {
        return skillCooldown;
    }

    @Override
    public String getSkillName() {
        return "Shield";
    }
}
