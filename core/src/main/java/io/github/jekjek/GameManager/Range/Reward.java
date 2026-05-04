package io.github.jekjek.GameManager.Range;

import java.util.Random;

public class Reward {
    private final int minXp;
    private final int maxXp;
    private final int minMoney;
    private final int maxMoney;

    public Reward(int minXp, int maxXp, int minMoney, int maxMoney) {
        this.minXp = minXp;
        this.maxXp = maxXp;
        this.minMoney = minMoney;
        this.maxMoney = maxMoney;
    }

    public int getMinXp() {
        return minXp;
    }

    public int getMaxXp() {
        return maxXp;
    }

    public int getMinMoney() {
        return minMoney;
    }

    public int getMaxMoney() {
        return maxMoney;
    }

    public int rollXp(Random random) {
        return rollInRange(random, minXp, maxXp);
    }

    public int rollMoney(Random random) {
        return rollInRange(random, minMoney, maxMoney);
    }

    private int rollInRange(Random random, int min, int max) {
        if (max <= min) {
            return min;
        }

        return random.nextInt(max - min + 1) + min;
    }
}
