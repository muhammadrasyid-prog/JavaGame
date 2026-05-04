package io.github.jekjek.Entity.Enemy;

public class MedicEnemy extends SpecialEnemy {
    private int skillCooldown = 0;
    private static final int COOLDOWN_DURATION = 2;

    public MedicEnemy(String name, double health, double armor, double attack) {
        super(name, health, armor, attack);
    }

    @Override
    public boolean shouldUseSkill(java.util.List<Enemy> allyTeam) {
        if (skillCooldown > 0) {
            return false;
        }

        for (Enemy ally : allyTeam) {
            if (ally != this && ally.isAlive() && ally.getHealth() < ally.getMaxHealth() * 0.5) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void executeSkill(java.util.List<Enemy> allyTeam) {
        Enemy lowestHealthAlly = null;
        double lowestHealth = Double.MAX_VALUE;

        for (Enemy ally : allyTeam) {
            if (ally != this && ally.isAlive() && ally.getHealth() < lowestHealth) {
                lowestHealth = ally.getHealth();
                lowestHealthAlly = ally;
            }
        }

        if (lowestHealthAlly != null) {
            double healAmount = 20;
            lowestHealthAlly.heal(healAmount);
            System.out.println(getNama() + " heals " + lowestHealthAlly.getNama() + " for " + (int)healAmount + " HP!");
            skillCooldown = COOLDOWN_DURATION;
        }
    }

    @Override
    public void decrementSkillCooldown() {
        if (skillCooldown > 0) {
            skillCooldown--;
        }
    }

    @Override
    public int getSkillCooldown() {
        return skillCooldown;
    }

    @Override
    public String getSkillName() {
        return "Heal";
    }
}
