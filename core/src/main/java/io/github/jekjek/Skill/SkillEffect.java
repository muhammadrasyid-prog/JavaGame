package io.github.jekjek.Skill;

public class SkillEffect {
    public enum EffectTiming {
        IMMEDIATE,           // Apply langsung saat skill dipakai
        END_OF_TURN,         // Apply di akhir turn pemain
        END_OF_ROUND,        // Apply di akhir 1 round (player turn + semua enemy turn)
        NEXT_TURN_START      // Apply di awal turn berikutnya
    }

    private final String effectName;
    private final String skillName;
    private final Skill.SkillType type;
    private final double value;
    private int remainingDuration;  // Durasi dalam turn (jumlah turn effect aktif)
    private final EffectTiming timing;
    private boolean applied;

    public SkillEffect(String effectName, String skillName, Skill.SkillType type, double value, int duration, EffectTiming timing) {
        this.effectName = effectName;
        this.skillName = skillName;
        this.type = type;
        this.value = value;
        this.remainingDuration = duration;
        this.timing = timing;
        this.applied = false;
    }

    public String getEffectName() {
        return effectName;
    }

    public String getSkillName() {
        return skillName;
    }

    public Skill.SkillType getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public int getRemainingDuration() {
        return remainingDuration;
    }

    public EffectTiming getTiming() {
        return timing;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public void decrementDuration() {
        remainingDuration--;
    }

    public boolean isExpired() {
        return remainingDuration <= 0;
    }

    @Override
    public String toString() {
        return effectName + " (" + remainingDuration + " turns left)";
    }
}
