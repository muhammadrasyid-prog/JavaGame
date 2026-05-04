package io.github.jekjek.Entity;

import io.github.jekjek.Skill.SkillEffect;

import java.util.ArrayList;
import java.util.List;

public abstract class Entity {
    protected String nama;
    protected double maxHealth;
    protected double health;
    protected double armor;
    protected double attack;
    protected List<SkillEffect> activeEffects = new ArrayList<>();
    private double damageReductionPercent = 0;

    protected Entity(String nama, double health, double armor, double attack) {
        this.nama = nama;
        this.maxHealth = health;
        this.health = health;
        this.armor = armor;
        this.attack = attack;
        normalizeHealth();
    }

    public String getNama() {
        return this.nama;
    }

    public double getHealth() {
        return normalizedHealth();
    }

    public double getArmor() {
        return this.armor;
    }

    public double getAttack() {
        return this.attack;
    }

    public boolean isAlive() {
        return normalizedHealth() > 0;
    }

    public double getDamageReductionPercent() {
        return damageReductionPercent;
    }

    public void setDamageReductionPercent(double damageReductionPercent) {
        this.damageReductionPercent = Math.max(0, damageReductionPercent);
    }

    public void clearDamageReductionPercent() {
        this.damageReductionPercent = 0;
    }

    public void takeDamage(double damage) {
        if (damage <= 0) {
            return;
        }

        double reducedDamage = damage * (1.0 - (damageReductionPercent / 100.0));

        if (this.armor > 0) {
            double damageToArmor = Math.min(this.armor, reducedDamage);
            this.armor -= damageToArmor;
            reducedDamage -= damageToArmor;
        }

        if (reducedDamage > 0) {
            this.health = Math.max(0, this.health - reducedDamage);
            normalizeHealth();
        }
    }

    public void takeTrueDamage(double damage) {
        if (damage <= 0) {
            return;
        }

        this.health = Math.max(0, this.health - damage);
        normalizeHealth();
    }

    public void addEffect(SkillEffect effect) {
        activeEffects.add(effect);
    }

    public List<SkillEffect> getActiveEffects() {
        return activeEffects;
    }

    public void removeExpiredEffects() {
        activeEffects.removeIf(SkillEffect::isExpired);
    }

    public void applyEffect(SkillEffect effect) {
        switch (effect.getType()) {
            case DAMAGE:
                takeTrueDamage(effect.getValue());
                break;
            case HEAL:
                heal(effect.getValue());
                break;
            case BUFF_ARMOR:
                buffArmor(effect.getValue());
                break;
            case DEBUFF_ARMOR:
                debuffArmor(effect.getValue());
                break;
        }
        effect.setApplied(true);
    }

    public void heal(double amount) {
        if (amount <= 0) {
            return;
        }
        this.health = Math.min(maxHealth, this.health + amount);
        normalizeHealth();
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public void buffArmor(double amount) {
        if (amount > 0) {
            this.armor += amount;
        }
    }

    public void debuffArmor(double amount) {
        if (amount > 0) {
            this.armor = Math.max(0, this.armor - amount);
        }
    }

    protected void setMaxHealth(double maxHealth) {
        this.maxHealth = Math.max(0, maxHealth);
        normalizeHealth();
    }

    private double normalizedHealth() {
        return Math.min(Math.max(0, this.health), this.maxHealth);
    }

    private void normalizeHealth() {
        this.health = normalizedHealth();
    }

    public void printActiveEffects() {
        if (activeEffects.isEmpty()) {
            return;
        }
        System.out.println("    Active effects on " + nama + ": " + activeEffects);
    }
}
