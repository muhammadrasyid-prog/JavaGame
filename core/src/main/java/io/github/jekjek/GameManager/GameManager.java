package io.github.jekjek.GameManager;

import io.github.jekjek.Entity.Player;
import io.github.jekjek.GameManager.Range.Difficulty;
import io.github.jekjek.GameManager.Range.Reward;
import io.github.jekjek.Skill.Skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
//import java.util.logging.Level;

public class GameManager {
    private final Scanner scanner = new Scanner(System.in);
    private final Inventory inventory = new Inventory("Adventurer", 0, 0, 0, 0);

    public void startGame() {
        initializeSkills();
        Difficulty difficulty = chooseDifficulty();
        int maxLevels = 5;

        for (int currentLevel = 1; currentLevel <= maxLevels; currentLevel++) {
            Reward reward = chooseReward(difficulty, currentLevel);
            int waves = chooseWaveCount(difficulty, currentLevel);
            Level level = new Level(
                "Jogja Expedition",
                currentLevel,
                "Clear all waves and survive the battle.",
                difficulty,
                reward,
                waves
            );
            Player player = createPlayerFromInventory();

            Level.BattleOutcome battleOutcome = level.startBattle(player, inventory, scanner);
            if (battleOutcome == Level.BattleOutcome.BACK_TO_MENU) {
                System.out.println("Returning to main menu.");
                return;
            }

            if (battleOutcome == Level.BattleOutcome.LOST) {
                System.out.println("Campaign stopped at Level " + currentLevel + ".");
                return;
            }

            inventory.addSkillPoint();
            System.out.println("Level " + currentLevel + " cleared.");
        }

        System.out.println("Campaign complete. All 5 levels cleared.");
    }

    void startCredits() {
        System.out.println("""
                JOGJA EXPERIENCE

                Our Team:
                Haidar
                Salwa
                Rasya
                Ariq
                Rasyid
                """);
    }

    public int getInput() {
        while (true) {
            try {
                String rawInput = scanner.nextLine().trim();
                return Integer.parseInt(rawInput);
            } catch (Exception e) {
                System.out.println("Your input is not valid, please try again!");
            }
        }

    }

    public void startMenu() {
        while (true) {
            System.out.println("""
                    JOGJA EXPERIENCE

                    Menu:
                    1. Start Mission
                    2. Inventory
                    3. Credits
                    4. Exit
                    """);

            switch (getInput()) {
                case 1:
                    startGame();
                    break;
                case 2:
                    startInventoryMenu();
                    break;
                case 3:
                    startCredits();
                    break;
                case 4:
                    System.out.println("Thank you for playing our game!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Unknown menu choice.");
                    break;
            }
        }
    }

    private Difficulty chooseDifficulty() {
        while (true) {
            System.out.println("""
                    Choose difficulty:
                    1. Easy
                    2. Normal
                    3. Hard
                    """);

            switch (getInput()) {
                case 1:
                    return new Difficulty("Easy", "Lower enemy power and a lighter reward pool.", 1, 0.85, 0.85, 0.85, 0.85, 2);
                case 2:
                    return new Difficulty("Normal", "Balanced enemy power and rewards.", 2, 1.0, 1.0, 1.0, 1.0, 3);
                case 3:
                    return new Difficulty("Hard", "Stronger enemies with better rewards.", 3, 1.25, 1.2, 1.2, 1.35, 4);
                default:
                    System.out.println("Unknown difficulty choice.");
                    break;
            }
        }
    }

    private Reward chooseReward(Difficulty difficulty, int currentLevel) {
        int baseXp = 10 + ((currentLevel - 1) * 6);
        int baseMoney = 8 + ((currentLevel - 1) * 5);
        int xpSpread = 10 + (difficulty.getLevel() * 3);
        int moneySpread = 8 + (difficulty.getLevel() * 2);

        return switch (difficulty.getLevel()) {
            case 1 -> new Reward(baseXp, baseXp + xpSpread, baseMoney, baseMoney + moneySpread);
            case 2 -> new Reward(baseXp + 8, baseXp + 8 + xpSpread, baseMoney + 5, baseMoney + 5 + moneySpread);
            case 3 -> new Reward(baseXp + 15, baseXp + 15 + xpSpread, baseMoney + 10, baseMoney + 10 + moneySpread);
            default -> new Reward(baseXp, baseXp + xpSpread, baseMoney, baseMoney + moneySpread);
        };
    }

    private int chooseWaveCount(Difficulty difficulty, int currentLevel) {
        return Math.min(5, 1 + (currentLevel / 2) + (difficulty.getLevel() - 1));
    }

    private Player createPlayerFromInventory() {
        return new Player(
            inventory.getProfileName(),
            inventory.getTotalHealth(),
            inventory.getTotalArmor(),
            inventory.getTotalAttack()
        );
    }

    private void startInventoryMenu() {
        while (true) {
            System.out.println("""
                    Inventory Menu:
                    1. View Status
                    2. Stat Shop
                    3. Skill Shop
                    4. Back
                    """);

            switch (getInput()) {
                case 1:
                    System.out.println(inventory);
                    break;
                case 2:
                    startStatShop();
                    break;
                case 3:
                    startSkillShop();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Unknown inventory choice.");
                    break;
            }
        }
    }

    private void startStatShop() {
        while (true) {
            System.out.println("""
                    Stat Shop:
                    1. Upgrade Health (+10) - 20 Money
                    2. Upgrade Armor (+5) - 20 Money
                    3. Upgrade Attack (+2) - 25 Money
                    4. Back
                    """);

            switch (getInput()) {
                case 1:
                    if (inventory.getMoneyCount() >= 20) {
                        if (inventory.upgradeHealth(20, 10)) {
                            System.out.println("Health upgraded.");
                        }
                    } else {
                        System.out.println("Not enough money.");
                    }
                    break;
                case 2:
                    if (inventory.getMoneyCount() >= 20) {
                        if (inventory.upgradeArmor(20, 5)) {
                            System.out.println("Armor upgraded.");
                        }
                    } else {
                        System.out.println("Not enough money.");
                    }
                    break;
                case 3:
                    if (inventory.getMoneyCount() >= 25) {
                        if (inventory.upgradeAttack(25, 2)) {
                            System.out.println("Attack upgraded.");
                        }
                    } else {
                        System.out.println("Not enough money.");
                    }
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Unknown stat shop choice.");
                    break;
            }
        }
    }

    private void startSkillShop() {
        List<Skill> availableSkills = getAvailableSkills();

        while (true) {
            System.out.println("""
                    Skill Shop:
                    """);

            System.out.println("Available Skills:");
            for (int i = 0; i < availableSkills.size(); i++) {
                Skill skill = availableSkills.get(i);
                int currentCharge = inventory.getSkillCharge(skill);
                System.out.println((i + 1) + ". " + skill.getName() + " (" + currentCharge + " charge) - " + skill.getDescription());
            }

            System.out.println((availableSkills.size() + 1) + ". Back");
            System.out.print("Choose option: ");

            int choice = getInput();
            if (choice == availableSkills.size() + 1) {
                return;
            }

            if (choice < 1 || choice > availableSkills.size()) {
                System.out.println("Invalid choice.");
                continue;
            }

            Skill selectedSkill = availableSkills.get(choice - 1);
            System.out.println("\n" + selectedSkill.getName() + ": " + selectedSkill.getDescription());
            System.out.println("Buy 1 Skill Charge for 10 Money? (1 = Yes, 2 = No)");

            if (getInput() == 1) {
                if (inventory.buySkillCharge(selectedSkill, 10, 1)) {
                    System.out.println("Skill charge bought!");
                } else {
                    System.out.println("Not enough money.");
                }
            }
        }
    }

    private List<Skill> getAvailableSkills() {
        return new ArrayList<>(inventory.getUnlockedSkills().keySet());
    }

    private void initializeSkills() {
        if (inventory.getUnlockedSkills().isEmpty()) {
            Skill slashSkill = new Skill("Slash", Skill.SkillType.DAMAGE, 30, Skill.TargetType.ENEMY, "Deal 30 true damage to enemy");
            Skill ironSkinSkill = new Skill("Iron Skin", Skill.SkillType.BUFF_ARMOR, 15, Skill.TargetType.SELF, "Buff own armor by 15");
            Skill weakenSkill = new Skill("Weaken", Skill.SkillType.DEBUFF_ARMOR, 20, Skill.TargetType.ENEMY, "Reduce enemy armor by 20");
            Skill healSkill = new Skill("Heal", Skill.SkillType.HEAL, 25, Skill.TargetType.SELF, "Heal self by 25 HP");

            inventory.addSkill(slashSkill, 3);
            inventory.addSkill(ironSkinSkill, 2);
            inventory.addSkill(weakenSkill, 2);
            inventory.addSkill(healSkill, 1);
        }
    }
}
