package io.github.jekjek.GameManager;

import io.github.jekjek.Skill.Skill;

import java.util.HashMap;
import java.util.Map;

public class Inventory {
    private String profileName;
    private int totalXp;
    private boolean justLeveledUp = false;
    private int levelUpCount = 0;
    private int xpCount;
    private int levelCount;
    private int moneyCount;
    private int skillPoint;
    private double healthBonus;
    private double armorBonus;
    private double attackBonus;
    private final Map<Skill, Integer> skillCharges = new HashMap<>();

    public static final int MAX_LEVEL = 10;

    private static final int[] XP_THRESHOLDS = {
        30, 60, 100, 150, 210, 280, 360, 450, 999, 999
    };

    public Inventory(String profileName, int xpCount, int levelCount, int moneyCount, int skillPoint) {
        this.profileName = profileName;
        this.xpCount = xpCount;
        this.levelCount = levelCount;
        this.moneyCount = moneyCount;
        this.skillPoint = skillPoint;
        this.healthBonus = 0;
        this.armorBonus = 0;
        this.attackBonus = 0;
    }

    public String getProfileName() {
        return profileName;
    }

    public int getXpCount() {
        return xpCount;
    }

    public boolean addXpAndCheckLevelUp(int xp) {
        boolean leveledUp = false;
        justLeveledUp = false;

        xpCount += xp;
        totalXp += xp;

        while (!isMaxLevel() && xpCount >= xpThresholdForCurrentLevel()) {
            xpCount -= xpThresholdForCurrentLevel();
            levelCount++;
            skillPoint++;
            applyLevelUpBonus();

            leveledUp = true;
            justLeveledUp = true;
            levelUpCount++;
        }

        return leveledUp;
    }

    private void applyLevelUpBonus() {
        healthBonus += 10;
        armorBonus += 3;
        attackBonus += 2;
    }

    public int getLevelCount() {
        return levelCount;
    }

    public void addLevelCount() {
        this.levelCount += 1;
    }

    public int getMoneyCount() {
        return moneyCount;
    }

    public void addMoneyCount(int moneyCount) {
        this.moneyCount = Math.max(0, this.moneyCount + moneyCount);
    }

    public void subtractMoneyCount(int moneyCount) {
        this.moneyCount = Math.max(0, this.moneyCount - moneyCount);
    }

    public int getSkillPoint() {
        return skillPoint;
    }

    public void addSkillPoint() {
        this.skillPoint += 1;
    }

    public void subtractSkillPoint(int skillPoint) {
        this.skillPoint = Math.max(0, this.skillPoint - skillPoint);
    }

    public void applyReward(int xpReward, int moneyReward) {
        addXpAndCheckLevelUp(xpReward);
        addMoneyCount(moneyReward);
    }

    public int xpThresholdForCurrentLevel() {
        int index = Math.min(levelCount - 1, XP_THRESHOLDS.length - 1);
        return XP_THRESHOLDS[index];
    }

    public float xpProgress() {
        return (float) xpCount / xpThresholdForCurrentLevel();
    }

    public boolean isMaxLevel() {
        return levelCount >= MAX_LEVEL;
    }

    public boolean isJustLeveledUp() {
        return justLeveledUp;
    }

    public void clearLevelUpFlag() {
        justLeveledUp = false;
    }

    public int getLevelUpCount() {
        return levelUpCount;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public double getHealthBonus() {
        return healthBonus;
    }

    public double getArmorBonus() {
        return armorBonus;
    }

    public double getAttackBonus() {
        return attackBonus;
    }

    public double getTotalHealth() {
        return 100 + healthBonus;
    }

    public double getTotalArmor() {
        return 25 + armorBonus;
    }

    public double getTotalAttack() {
        return 15 + attackBonus;
    }

    public boolean upgradeHealth(int moneyCost, double value) {
        if (moneyCount < moneyCost || value <= 0) {
            return false;
        }

        subtractMoneyCount(moneyCost);
        this.healthBonus += value;
        return true;
    }

    public boolean upgradeArmor(int moneyCost, double value) {
        if (moneyCount < moneyCost || value <= 0) {
            return false;
        }

        subtractMoneyCount(moneyCost);
        this.armorBonus += value;
        return true;
    }

    public boolean upgradeAttack(int moneyCost, double value) {
        if (moneyCount < moneyCost || value <= 0) {
            return false;
        }

        subtractMoneyCount(moneyCost);
        this.attackBonus += value;
        return true;
    }

    public void addSkill(Skill skill, int initialCharges) {
        if (!skillCharges.containsKey(skill)) {
            skillCharges.put(skill, initialCharges);
        }
    }

    public Map<Skill, Integer> getUnlockedSkills() {
        return skillCharges;
    }

    public int getSkillCharge(Skill skill) {
        return skillCharges.getOrDefault(skill, 0);
    }

    public boolean hasSkillCharge(Skill skill) {
        return getSkillCharge(skill) > 0;
    }

    public boolean consumeSkillCharge(Skill skill) {
        if (!hasSkillCharge(skill)) {
            return false;
        }
        skillCharges.put(skill, getSkillCharge(skill) - 1);
        return true;
    }

    public String getSkillsState() {
        if (skillCharges.isEmpty()) {
            return "No skills unlocked";
        }
        StringBuilder sb = new StringBuilder();
        for (Skill skill : skillCharges.keySet()) {
            sb.append("\n  - ").append(skill.getName())
                .append(" (").append(skillCharges.get(skill)).append(" charge)")
                .append(" - ").append(skill.getDescription());
        }
        return sb.toString();
    }

    public boolean buySkillCharge(Skill skill, int moneyCost, int chargeAmount) {
        if (!skillCharges.containsKey(skill) || moneyCount < moneyCost || chargeAmount <= 0) {
            return false;
        }

        subtractMoneyCount(moneyCost);
        skillCharges.put(skill, getSkillCharge(skill) + chargeAmount);
        return true;
    }

    @Override
    public String toString() {
        return "Profile: " + profileName +
            " | XP: " + xpCount +
            " | Level: " + levelCount +
            " | Money: " + moneyCount +
            " | Skill Point: " + skillPoint +
            " | HP Bonus: " + healthBonus +
            " | Armor Bonus: " + armorBonus +
            " | ATK Bonus: " + attackBonus +
            " | Skills: " + getSkillsState();
    }
}
