package io.github.jekjek;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.Game;
import io.github.jekjek.GameManager.Inventory;
import io.github.jekjek.Screens.MenuScreen;
import io.github.jekjek.Skill.Skill;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    public SpriteBatch batch;
    public Inventory inventory;
    public int totalScore = 0;

    @Override
    public void create() {
        batch = new SpriteBatch();

        // 🔹 INIT INVENTORY
        inventory = new Inventory("Adventurer", 0, 1, 0, 0);

        // 🔹 TAMBAH SKILL AWAL (FIX: pakai charges)
        inventory.addSkill(new Skill(
            "Slash",
            Skill.SkillType.DAMAGE,
            30,
            Skill.TargetType.ENEMY,
            "Deal 30 true damage to enemy"
        ), 3);

        inventory.addSkill(new Skill(
            "Iron Skin",
            Skill.SkillType.BUFF_ARMOR,
            15,
            Skill.TargetType.SELF,
            "Buff own armor by 15"
        ), 2);

        inventory.addSkill(new Skill(
            "Weaken",
            Skill.SkillType.DEBUFF_ARMOR,
            20,
            Skill.TargetType.ENEMY,
            "Reduce enemy armor by 20"
        ), 2);

        inventory.addSkill(new Skill(
            "Heal",
            Skill.SkillType.HEAL,
            25,
            Skill.TargetType.SELF,
            "Heal self by 25 HP"
        ), 1);

        // 🔹 START SCREEN (tetap)
        setScreen(new MenuScreen(this));
    }

    // 🔹 SISTEM SCORE + XP TERINTEGRASI
    public void addScore(int points) {
        totalScore += points;
        inventory.addXpAndCheckLevelUp(points);
    }

    @Override
    public void dispose() {
        batch.dispose(); // jangan lupa dispose
    }
}
