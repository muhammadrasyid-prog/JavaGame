package io.github.jekjek.GameManager;

import io.github.jekjek.Entity.Enemy.*;
import io.github.jekjek.GameManager.Range.Difficulty;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EnemyManager {

    public List<Enemy> generateWave(Difficulty difficulty, int levelNumber, int waveNumber) {
        List<Enemy> enemies = new ArrayList<>();
        int enemyCount = Math.max(1, difficulty.getEnemyCount() + Math.min(2, Math.max(0, levelNumber - 1) / 2));
        int specialBudget = Math.max(1, Math.min(3, levelNumber / 2 + 1));
        Random random = new Random();

        for (int slot = 0; slot < enemyCount; slot++) {
            boolean isSpecialEnemy = specialBudget > 0 && random.nextDouble() < specialChance(levelNumber, waveNumber);

            double baseHealth = isSpecialEnemy ? 65 : 45;
            double baseArmor = isSpecialEnemy ? 22 : 14;
            double baseAttack = isSpecialEnemy ? 16 : 10;

            double levelScale = 1.0 + ((levelNumber - 1) * 0.08) + ((waveNumber - 1) * 0.05);
            double health = Math.round(baseHealth * difficulty.getHealthMultiplier() * levelScale);
            double armor = Math.round(baseArmor * difficulty.getArmorMultiplier() * levelScale);
            double attack = Math.round(baseAttack * difficulty.getAttackMultiplier() * levelScale);

            if (isSpecialEnemy) {
                specialBudget--;
                int specialType = random.nextInt(3);
                switch (specialType) {
                    case 0:
                        if (isAllowedSpecial(difficulty, 0)) {
                            enemies.add(new MedicEnemy("Medic", health, armor, attack));
                        } else {
                            enemies.add(new ShieldEnemy("Shield", health, armor, attack));
                        }
                        break;
                    case 1:
                        if (isAllowedSpecial(difficulty, 1)) {
                            enemies.add(new BulldozerEnemy("Bulldozer", health, armor, attack));
                        } else {
                            enemies.add(new ShieldEnemy("Shield", health, armor, attack));
                        }
                        break;
                    case 2:
                    default:
                        enemies.add(new ShieldEnemy("Shield", health, armor, attack));
                        break;
                }
            } else {
                enemies.add(new RegularEnemy("Enemy", health, armor, attack));
            }
        }

        return enemies;
    }

    private boolean isAllowedSpecial(Difficulty difficulty, int specialType) {
        if (difficulty.getLevel() <= 1) {
            return specialType == 2;
        }

        if (difficulty.getLevel() == 2) {
            return specialType == 0 || specialType == 2;
        }

        return true;
    }

    private double specialChance(int levelNumber, int waveNumber) {
        double chance = 0.20 + ((levelNumber - 1) * 0.03) + ((waveNumber - 1) * 0.02);
        return Math.min(0.55, chance);
    }

}
