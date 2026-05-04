package io.github.jekjek.GameManager.Range;

public class Difficulty {
    private final String nama;
    private final String desc;
    private final int level;

    private final double healthMultiplier;
    private final double armorMultiplier;
    private final double attackMultiplier;
    private final double rewardMultiplier;
    private final int enemyCount;

    public Difficulty(String nama, String desc, int level, double healthMultiplier, double armorMultiplier, double attackMultiplier, double rewardMultiplier, int enemyCount) {
        this.nama = nama;
        this.desc = desc;
        this.level = level;
        this.healthMultiplier = healthMultiplier;
        this.armorMultiplier = armorMultiplier;
        this.attackMultiplier = attackMultiplier;
        this.rewardMultiplier = rewardMultiplier;
        this.enemyCount = enemyCount;
    }

    public String getNama() {
        return nama;
    }

    public String getDesc() {
        return desc;
    }

    public int getLevel() {
        return level;
    }

    public double getHealthMultiplier() {
        return healthMultiplier;
    }

    public double getArmorMultiplier() {
        return armorMultiplier;
    }

    public double getAttackMultiplier() {
        return attackMultiplier;
    }

    public double getRewardMultiplier() {
        return rewardMultiplier;
    }

    public int getEnemyCount() {
        return enemyCount;
    }
}
