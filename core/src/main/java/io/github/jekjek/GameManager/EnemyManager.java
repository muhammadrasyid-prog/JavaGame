package io.github.jekjek.GameManager;

import io.github.jekjek.Entity.Enemy.*;
import io.github.jekjek.GameManager.Range.Difficulty;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EnemyManager {

    public List<Enemy> generateWave(Difficulty difficulty, int levelNumber, int waveNumber) {
        List<Enemy> enemies = new ArrayList<>();
        // Scale enemy count
        int baseCount = difficulty.getEnemyCount();
        int additionalByLevel = Math.min(2, (levelNumber - 1) / 2);
        int additionalByWave = waveNumber > 2 ? 1 : 0;
        int enemyCount = Math.max(1, baseCount + additionalByLevel + additionalByWave);

        // Cap enemies to 5 max
        enemyCount = Math.min(5, enemyCount);

        for (int i = 0; i < enemyCount; i++) {
            EnemyType typeToSpawn = determineEnemyType(waveNumber, levelNumber, i);

            // Scale stats by level and wave
            double levelScale = 1.0 + ((levelNumber - 1) * 0.15) + ((waveNumber - 1) * 0.05);
            
            double baseHealth = 30;
            double baseArmor = 5;
            double baseAttack = 6;
            String name = "Enemy";

            switch (typeToSpawn) {
                case REGULAR:
                    baseHealth = 30; baseArmor = 5; baseAttack = 6; name = "Bandit";
                    break;
                case SHIELD:
                    baseHealth = 35; baseArmor = 15; baseAttack = 5; name = "Guardian";
                    break;
                case MEDIC:
                    baseHealth = 25; baseArmor = 2; baseAttack = 4; name = "Healer";
                    break;
                case BULLDOZER:
                    baseHealth = 50; baseArmor = 8; baseAttack = 12; name = "Brute";
                    break;
            }

            double health = Math.round(baseHealth * difficulty.getHealthMultiplier() * levelScale);
            double armor = Math.round(baseArmor * difficulty.getArmorMultiplier() * levelScale);
            double attack = Math.round(baseAttack * difficulty.getAttackMultiplier() * levelScale);

            switch (typeToSpawn) {
                case SHIELD:
                    enemies.add(new ShieldEnemy(name, health, armor, attack));
                    break;
                case MEDIC:
                    enemies.add(new MedicEnemy(name, health, armor, attack));
                    break;
                case BULLDOZER:
                    enemies.add(new BulldozerEnemy(name, health, armor, attack));
                    break;
                case REGULAR:
                default:
                    enemies.add(new RegularEnemy(name, health, armor, attack));
                    break;
            }
        }
        return enemies;
    }

    private enum EnemyType {
        REGULAR, SHIELD, MEDIC, BULLDOZER
    }

    private EnemyType determineEnemyType(int wave, int level, int slotIndex) {
        if (wave == 1) {
            // Wave 1 mostly regular. If level is high, maybe a shield at the end
            if (level >= 2 && slotIndex == 2) return EnemyType.SHIELD;
            return EnemyType.REGULAR;
        } else if (wave == 2) {
            // Wave 2 introduces Medics and Shields
            if (slotIndex == 0) return EnemyType.SHIELD;
            if (slotIndex == 1 && level >= 2) return EnemyType.MEDIC;
            return EnemyType.REGULAR;
        } else {
            // Wave 3+ uses Bulldozer and mixes
            if (slotIndex == 0) return EnemyType.BULLDOZER;
            if (slotIndex == 1) return EnemyType.MEDIC;
            if (slotIndex == 2) return EnemyType.SHIELD;
            return EnemyType.REGULAR;
        }
    }

}
