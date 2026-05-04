package io.github.jekjek.Skill;

public class Skill {
    public enum SkillType {
        DAMAGE,
        HEAL,
        BUFF_ARMOR,
        DEBUFF_ARMOR
    }

    public enum TargetType {
        SELF,
        ENEMY
    }

    private final String name;
    private final SkillType type;
    private final double value;
    private final TargetType targetType;
    private final String description;

    public Skill(String name, SkillType type, double value, TargetType targetType, String description) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.targetType = targetType;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public SkillType getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }
}
