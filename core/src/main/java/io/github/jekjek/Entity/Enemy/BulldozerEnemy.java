package io.github.jekjek.Entity.Enemy;

public class BulldozerEnemy extends SpecialEnemy {
    private int skillCooldown = 0;
    private static final int COOLDOWN_DURATION = 2;

    public BulldozerEnemy(String name, double health, double armor, double attack) {
        super(name, health, armor, attack);
    }

    @Override
    public boolean shouldUseSkill(java.util.List<Enemy> allyTeam) {
        if (skillCooldown > 0) {
            return false;
        }

        double lowestArmor = Double.MAX_VALUE;
        for (Enemy ally : allyTeam) {
            if (ally.isAlive() && ally.getArmor() < lowestArmor) {
                lowestArmor = ally.getArmor();
            }
        }

        return lowestArmor < 15;
    }

    @Override
    public void executeSkill(java.util.List<Enemy> allyTeam) {
        double armorBoost = 5;
        for (Enemy ally : allyTeam) {
            if (ally.isAlive()) {
                ally.buffArmor(armorBoost);
            }
        }
        System.out.println(getNama() + " uses Team Fortify! All allies gain " + (int)armorBoost + " armor!");
        skillCooldown = COOLDOWN_DURATION;
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
        return "Team Fortify";
    }
}
