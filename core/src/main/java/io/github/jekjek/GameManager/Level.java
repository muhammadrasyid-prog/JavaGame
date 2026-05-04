package io.github.jekjek.GameManager;

import io.github.jekjek.Entity.Enemy.Enemy;
import io.github.jekjek.Entity.Enemy.SpecialEnemy;
import io.github.jekjek.Entity.Entity;
import io.github.jekjek.Entity.Player;
import io.github.jekjek.GameManager.Range.Difficulty;
import io.github.jekjek.GameManager.Range.Reward;
import io.github.jekjek.Skill.Skill;
import io.github.jekjek.Skill.SkillEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public record Level(String nama, int level, String desc, Difficulty diff, Reward rewardPool, int waves) {

    public BattleOutcome startBattle(Player player, Inventory inventory, Scanner scanner) {
        EnemyManager enemyManager = new EnemyManager();
        Random random = new Random();
        int clearedWaves = 0;

        System.out.println("\n=== Level " + level + " | " + nama + " ===");
        System.out.println(desc);
        System.out.println("Difficulty: " + diff.getNama());

        for (int currentWave = 1; currentWave <= waves; currentWave++) {
            List<Enemy> enemies = enemyManager.generateWave(this.diff, this.level, currentWave);
            System.out.println("\n--- Wave " + currentWave + " / " + waves + " ---");

            BattleOutcome outcome = fightWave(player, enemies, inventory, scanner);
            if (outcome == BattleOutcome.BACK_TO_MENU) {
                return BattleOutcome.BACK_TO_MENU;
            }

            if (outcome == BattleOutcome.LOST) {
                int consolationXp = Math.max(1, Math.round(rewardPool.getMinXp() * (0.15f + (clearedWaves * 0.05f))));
                int consolationMoney = Math.max(0, Math.round(rewardPool.getMinMoney() * (0.10f + (clearedWaves * 0.04f))));
                inventory.applyReward(consolationXp, consolationMoney);

                System.out.println("\nMission failed.");
                System.out.println("Consolation reward: " + consolationXp + " XP, " + consolationMoney + " Money.");
                System.out.println(inventory);
                return BattleOutcome.LOST;
            }

            clearedWaves++;
            System.out.println("Wave " + currentWave + " cleared!");
        }

        int xpReward = Math.max(1, Math.round((rewardPool.rollXp(random) + (level * 2)) * (float) diff.getRewardMultiplier()));
        int moneyReward = Math.max(1, Math.round((rewardPool.rollMoney(random) + level) * (float) diff.getRewardMultiplier()));
        inventory.applyReward(xpReward, moneyReward);
        inventory.addLevelCount();

        System.out.println("\nLevel cleared.");
        System.out.println("Reward obtained: " + xpReward + " XP, " + moneyReward + " Money.");
        System.out.println(inventory);
        return BattleOutcome.WON;
    }

    private BattleOutcome fightWave(Player player, List<Enemy> enemies, Inventory inventory, Scanner scanner) {
        while (player.isAlive() && hasAliveEnemy(enemies)) {
            printBattleStatus(player, enemies);
            int action = promptAction(scanner, inventory);

            if (action == 3) {
                return BattleOutcome.BACK_TO_MENU;
            }

            if (action == 2) {
                executeSkill(player, enemies, inventory, scanner);
            } else {
                int targetIndex = promptTarget(scanner, enemies);
                Enemy target = enemies.get(targetIndex);

                double playerDamage = Math.max(1, Math.round(player.getAttack() * diff.getAttackMultiplier()));
                target.takeDamage(playerDamage);

                System.out.println(player.getNama() + " attacks " + target.getNama() + " for " + formatNumber(playerDamage) + " damage.");
                printEntityStatus(target);
            }

            if (!hasAliveEnemy(enemies) || !player.isAlive()) {
                break;
            }

            System.out.println("\nEnemy turn:");
            for (Enemy enemy : enemies) {
                if (!enemy.isAlive()) {
                    continue;
                }

                boolean skillUsed = false;

                if (enemy instanceof SpecialEnemy specialEnemy && specialEnemy.shouldUseSkill(enemies)) {
                    specialEnemy.executeSkill(enemies);
                    skillUsed = true;
                }

                if (!skillUsed) {
                    double enemyDamage = Math.max(1, enemy.getAttack());
                    player.takeDamage(enemyDamage);
                    System.out.println(enemy.getNama() + " attacks " + player.getNama() + " for " + formatNumber(enemyDamage) + " damage.");
                    printEntityStatus(player);
                }

                if (!player.isAlive()) {
                    break;
                }
            }

            applyEndOfRoundEffects(player, enemies);
        }

        return player.isAlive() ? BattleOutcome.WON : BattleOutcome.LOST;
    }

    private boolean hasAliveEnemy(List<Enemy> enemies) {
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                return true;
            }
        }

        return false;
    }

    private void printBattleStatus(Player player, List<Enemy> enemies) {
        System.out.println("\n--- Battle Status ---");
        printEntityStatus(player);

        for (int index = 0; index < enemies.size(); index++) {
            Enemy enemy = enemies.get(index);
            if (enemy.isAlive()) {
                System.out.println((index + 1) + ". " + enemy.getNama() + " | HP: " + formatNumber(enemy.getHealth()) + " | Armor: " + formatNumber(enemy.getArmor()) + " | ATK: " + formatNumber(enemy.getAttack()));
            } else {
                System.out.println((index + 1) + ". " + enemy.getNama() + " | DEAD");
            }
        }
    }

    private void printEntityStatus(Entity entity) {
        System.out.println(entity.getNama() + " | HP: " + formatNumber(entity.getHealth()) + " | Armor: " + formatNumber(entity.getArmor()) + " | ATK: " + formatNumber(entity.getAttack()));
    }

    private int promptTarget(Scanner scanner, List<Enemy> enemies) {
        while (true) {
            System.out.print("Choose target enemy: ");
            String input = scanner.nextLine().trim();

            try {
                int selectedTarget = Integer.parseInt(input) - 1;
                if (selectedTarget < 0 || selectedTarget >= enemies.size()) {
                    System.out.println("Target is out of range.");
                    continue;
                }

                if (!enemies.get(selectedTarget).isAlive()) {
                    System.out.println("That enemy is already dead.");
                    continue;
                }

                return selectedTarget;
            } catch (NumberFormatException exception) {
                System.out.println("Your input is not valid, please try again.");
            }
        }
    }

    private int promptAction(Scanner scanner, Inventory inventory) {
        while (true) {
            System.out.println("Choose action:");
            System.out.println("1. Attack");

            boolean hasAnySkillCharge = inventory.getUnlockedSkills().values().stream().anyMatch(charge -> charge > 0);
            if (hasAnySkillCharge) {
                System.out.println("2. Use Skill");
            }

            System.out.println("3. Back to Menu");

            String input = scanner.nextLine().trim();

            try {
                int action = Integer.parseInt(input);
                if (action == 1) {
                    return 1;
                }

                if (action == 2 && hasAnySkillCharge) {
                    return 2;
                }

                if (action == 3) {
                    return 3;
                }

                System.out.println("Action is not available.");
            } catch (NumberFormatException exception) {
                System.out.println("Your input is not valid, please try again.");
            }
        }
    }

    private String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }

        return String.valueOf(value);
    }

    private void executeSkill(Player player, List<Enemy> enemies, Inventory inventory, Scanner scanner) {
        Map<Skill, Integer> skills = inventory.getUnlockedSkills();
        List<Skill> availableSkills = new ArrayList<>();

        for (Skill skill : skills.keySet()) {
            if (inventory.hasSkillCharge(skill)) {
                availableSkills.add(skill);
            }
        }

        if (availableSkills.isEmpty()) {
            System.out.println("No skill charges available.");
            return;
        }

        System.out.println("\nChoose skill:");
        for (int i = 0; i < availableSkills.size(); i++) {
            Skill skill = availableSkills.get(i);
            System.out.println((i + 1) + ". " + skill.getName() + " (" + skill.getDescription() + ")");
        }

        int skillChoice = getInputRange(scanner, 1, availableSkills.size()) - 1;
        Skill chosenSkill = availableSkills.get(skillChoice);

        Entity skillTarget = chosenSkill.getTargetType() == Skill.TargetType.SELF ? player : selectEnemyTarget(scanner, enemies);
        SkillEffect effect = new SkillEffect(
            chosenSkill.getName() + " effect",
            chosenSkill.getName(),
            chosenSkill.getType(),
            chosenSkill.getValue(),
            1,
            SkillEffect.EffectTiming.IMMEDIATE
        );

        skillTarget.addEffect(effect);
        inventory.consumeSkillCharge(chosenSkill);

        System.out.println(player.getNama() + " uses " + chosenSkill.getName() + " on " + skillTarget.getNama() + "!");
        skillTarget.applyEffect(effect);
        printEntityStatus(skillTarget);
    }

    private Entity selectEnemyTarget(Scanner scanner, List<Enemy> enemies) {
        int targetIndex = promptTarget(scanner, enemies);
        return enemies.get(targetIndex);
    }

    private int getInputRange(Scanner scanner, int min, int max) {
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }

                System.out.println("Please choose between " + min + " and " + max);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
            }
        }
    }

    private void applyEndOfRoundEffects(Player player, List<Enemy> enemies) {
        List<Entity> allEntities = new ArrayList<>();
        allEntities.add(player);
        allEntities.addAll(enemies);

        for (Entity entity : allEntities) {
            for (SkillEffect effect : entity.getActiveEffects()) {
                if (effect.getTiming() == SkillEffect.EffectTiming.END_OF_ROUND && !effect.isApplied()) {
                    entity.applyEffect(effect);
                    System.out.println("    " + entity.getNama() + " is affected by " + effect.getEffectName());
                }
            }
        }

        for (Entity entity : allEntities) {
            for (SkillEffect effect : entity.getActiveEffects()) {
                effect.decrementDuration();
            }
            entity.removeExpiredEffects();
            entity.clearDamageReductionPercent();
            entity.printActiveEffects();
        }

        for (Enemy enemy : enemies) {
            if (enemy instanceof SpecialEnemy specialEnemy) {
                specialEnemy.decrementSkillCooldown();
            }
        }
    }

    public enum BattleOutcome {
        WON,
        LOST,
        BACK_TO_MENU
    }
}
