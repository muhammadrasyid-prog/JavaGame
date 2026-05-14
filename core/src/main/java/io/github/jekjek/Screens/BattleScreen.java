package io.github.jekjek.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import io.github.jekjek.Entity.Enemy.Enemy;
import io.github.jekjek.Entity.Player;
import io.github.jekjek.GameManager.EnemyManager;
import io.github.jekjek.GameManager.Range.Difficulty;
import io.github.jekjek.Main;
import io.github.jekjek.Skill.Skill;
import io.github.jekjek.Screens.DamagePopup;
import io.github.jekjek.Screens.EnemySprite;

import java.util.List;

/**
 * BATTLE SCREEN - Fixed Version
 */
public class BattleScreen implements Screen {

    private static final float W = 800f, H = 480f;

    private final Main game;
    private Stage stage;
    private FitViewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    private BitmapFont fontSmall, fontMedium, fontLarge;

    private Player player;
    private Array<EnemySprite> enemySprites = new Array<>();
    private List<Enemy> enemies;
    private int currentWave = 1;
    private int maxWaves = 3;
    private int currentLevel = 1;
    // Di bagian deklarasi variable, tambahkan ini:
    private String selectedDifficulty = "Normal";

    private boolean isPlayerTurn = true;
    private Array<String> logHistory = new Array<>();
    private Label logLabel;
    private Label playerHpLabel, playerArmorLabel, playerAttackLabel;
    private Label turnLabel;
    private Table enemyTable;
    // Di bagian deklarasi variable (sekitar baris 50-60), tambahkan:
    private Array<DamagePopup> damagePopups = new Array<>();
    private Table skillOverlay;

    private Skill pendingSkill = null;
    // Enemy sprites (temporary colored rectangles)
    private Texture regularEnemyTex;
    private Texture medicEnemyTex;
    private Texture shieldEnemyTex;
    private Texture bulldozerEnemyTex;
    // Di bagian deklarasi variable
    private Texture backgroundWave1, backgroundWave2, backgroundWave3;
    private Texture currentBackground;

    public BattleScreen(Main game, String difficultyName) {
        this.game = game;
        this.selectedDifficulty = difficultyName;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        viewport = new FitViewport(W, H);
        stage = new Stage(viewport, batch);
        Gdx.input.setInputProcessor(stage);

        // Fonts
        fontSmall = new BitmapFont();
        fontSmall.getData().setScale(0.8f);
        fontMedium = new BitmapFont();
        fontMedium.getData().setScale(1.2f);
        fontLarge = new BitmapFont();
        fontLarge.getData().setScale(1.6f);

        // Load backgrounds
        backgroundWave1 = new Texture("GUNUNGMERAPI.png");
        backgroundWave2 = new Texture("RINGROAD.png");
        backgroundWave3 = new Texture("MALIOBORO.png");
        updateBackground();

        // Create colored textures for enemies (temporary)
        regularEnemyTex = createColoredTexture(0.2f, 0.4f, 0.8f, 1f);  // Biru
        medicEnemyTex = createColoredTexture(0.2f, 0.7f, 0.2f, 1f);     // Hijau
        shieldEnemyTex = createColoredTexture(0.8f, 0.7f, 0.1f, 1f);    // Kuning
        bulldozerEnemyTex = createColoredTexture(0.8f, 0.2f, 0.2f, 1f); // Merah

        // Create player
        player = new Player(
            game.inventory.getProfileName(),
            game.inventory.getTotalHealth(),
            game.inventory.getTotalArmor(),
            game.inventory.getTotalAttack()
        );

        generateWave();
        buildUI();

        isPlayerTurn = true;
        addLog("=== BATTLE START ===");
        addLog("Battle started at " + selectedDifficulty + " difficulty!");
        addLog("Your turn! Click on enemy to attack.");
    }

    private void updateBackground() {
        switch (currentWave) {
            case 1:
                currentBackground = backgroundWave1;
                break;
            case 2:
                currentBackground = backgroundWave2;
                break;
            case 3:
                currentBackground = backgroundWave3;
                break;
            default:
                currentBackground = backgroundWave1;
                break;
        }
    }

    private Texture createColoredTexture(float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();

        // Add border
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.drawRectangle(0, 0, 63, 63);

        // Add simple face (two eyes)
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fillCircle(20, 40, 6);
        pixmap.fillCircle(44, 40, 6);
        pixmap.setColor(0f, 0f, 0f, 1f);
        pixmap.fillCircle(20, 40, 3);
        pixmap.fillCircle(44, 40, 3);

        // Mouth (depends on enemy type, optional)
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.drawLine(25, 25, 39, 25);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void generateWave() {
        EnemyManager enemyManager = new EnemyManager();

        // Buat Difficulty object berdasarkan pilihan player
        Difficulty diff;
        switch (selectedDifficulty) {
            case "Easy":
                diff = new Difficulty("Easy", "", 1, 0.7, 0.7, 0.7, 1.2, 2);
                break;
            case "Hard":
                diff = new Difficulty("Hard", "", 3, 1.4, 1.3, 1.3, 0.8, 4);
                break;
            case "Normal":
            default:
                diff = new Difficulty("Normal", "", 2, 1.0, 1.0, 1.0, 1.0, 3);
                break;
        }

        // GENERATE ENEMIES DULU
        enemies = enemyManager.generateWave(diff, currentLevel, currentWave);
        
        // BUAT SPRITE BERDASARKAN ENEMIES YANG SUDAH DIGENERATE
        enemySprites.clear();
        float startX = 500f;
        float startY = 300f;
        for (int i = 0; i < enemies.size(); i++) {
            EnemySprite sprite = new EnemySprite(startX + (i * 80f), startY);
            enemySprites.add(sprite);
        }
        
        addLog("Difficulty: " + selectedDifficulty);
        addLog("Wave " + currentWave + "/" + maxWaves + " - " + enemies.size() + " enemies appear!");
    }

    private void buildUI() {
        // Background panel untuk log (bawah)
        Table bottomPanel = new Table();
        bottomPanel.setFillParent(true);
        bottomPanel.bottom().pad(10f);

        // Label log
        logLabel = new Label("", new Label.LabelStyle(fontSmall, Color.WHITE));
        logLabel.setWrap(true);

        ScrollPane scrollPane = new ScrollPane(logLabel);
        scrollPane.setSize(760f, 80f);
        scrollPane.setScrollingDisabled(false, true);

        bottomPanel.add(scrollPane).width(760f).height(80f).padBottom(10f).row();

        // Tombol aksi
        Table buttonPanel = new Table();

        TextButton btnAttack = createButton("⚔ ATTACK", new Color(0.2f, 0.6f, 0.2f, 1f));
        TextButton btnSkill = createButton("✨ SKILL", new Color(0.2f, 0.3f, 0.6f, 1f));
        TextButton btnBack = createButton("🏠 BACK", new Color(0.6f, 0.2f, 0.2f, 1f));

        btnAttack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isPlayerTurn) {
                    addLog("Click on an enemy to attack!");
                } else {
                    addLog("Not your turn! Wait for enemy.");
                }
            }
        });

        btnSkill.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isPlayerTurn) {
                    showSkillMenu();
                } else {
                    addLog("Cannot use skill now. Wait your turn!");
                }
            }
        });

        btnBack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });

        buttonPanel.add(btnAttack).width(140f).height(50f).pad(5f);
        buttonPanel.add(btnSkill).width(140f).height(50f).pad(5f);
        buttonPanel.add(btnBack).width(140f).height(50f).pad(5f);

        bottomPanel.add(buttonPanel).row();
        stage.addActor(bottomPanel);

        // Enemy table (kanan atas)
        enemyTable = new Table();
        enemyTable.setFillParent(true);
        enemyTable.top().right().pad(20f);
        stage.addActor(enemyTable);

        // Panel info player (kiri atas)
        Table playerPanel = new Table();
        playerPanel.setFillParent(true);
        playerPanel.top().left().pad(20f);

        playerHpLabel = new Label("", new Label.LabelStyle(fontMedium, Color.RED));
        playerArmorLabel = new Label("", new Label.LabelStyle(fontSmall, Color.CYAN));
        playerAttackLabel = new Label("", new Label.LabelStyle(fontSmall, Color.ORANGE));

        playerPanel.add(new Label("⚔ PLAYER ⚔", new Label.LabelStyle(fontMedium, Color.YELLOW))).padBottom(5f).row();
        playerPanel.add(playerHpLabel).padBottom(3f).row();
        playerPanel.add(playerArmorLabel).padBottom(3f).row();
        playerPanel.add(playerAttackLabel).row();

        stage.addActor(playerPanel);

        // Turn indicator (tengah atas)
        Table turnPanel = new Table();
        turnPanel.setFillParent(true);
        turnPanel.top().padTop(10f);
        turnLabel = new Label("", new Label.LabelStyle(fontLarge, Color.GREEN));
        turnPanel.add(turnLabel).center();
        stage.addActor(turnPanel);

        updateUI();
    }

    private TextButton createButton(String text, Color bgColor) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(bgColor);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.up = new TextureRegionDrawable(new TextureRegion(texture));
        style.down = new TextureRegionDrawable(new TextureRegion(texture));
        style.over = new TextureRegionDrawable(new TextureRegion(texture));
        style.font = fontMedium;
        style.fontColor = Color.WHITE;

        return new TextButton(text, style);
    }

    private void updateUI() {
        // Update player stats
        playerHpLabel.setText("❤ HP: " + (int)player.getHealth() + "/" + (int)player.getMaxHealth());
        playerArmorLabel.setText("🛡 Armor: " + (int)player.getArmor());
        playerAttackLabel.setText("⚔ Attack: " + (int)player.getAttack());

        // Update turn indicator
        if (isPlayerTurn) {
            turnLabel.setText("▶ YOUR TURN ◀");
            turnLabel.setColor(Color.GREEN);
        } else {
            turnLabel.setText("⚔ ENEMY TURN ⚔");
            turnLabel.setColor(Color.RED);
        }

        // Update enemy table
        updateEnemyTable();
    }

    private void updateEnemyTable() {
        enemyTable.clear();

        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            final int enemyIndex = i;

            Table enemyCard = new Table();
            enemyCard.setBackground(createBorderDrawable(0.2f, 0.2f, 0.3f, 0.9f));
            enemyCard.pad(10f);

            // === PILIH SPRITE BERDASARKAN TIPE ENEMY ===
            Texture enemySprite;
            if (enemy instanceof io.github.jekjek.Entity.Enemy.MedicEnemy) {
                enemySprite = medicEnemyTex;
            } else if (enemy instanceof io.github.jekjek.Entity.Enemy.ShieldEnemy) {
                enemySprite = shieldEnemyTex;
            } else if (enemy instanceof io.github.jekjek.Entity.Enemy.BulldozerEnemy) {
                enemySprite = bulldozerEnemyTex;
            } else {
                enemySprite = regularEnemyTex;
            }

            // Gambar sprite enemy
            Image enemyImage = new Image(new TextureRegionDrawable(new TextureRegion(enemySprite)));
            enemyCard.add(enemyImage).size(50f, 50f).padBottom(8f).row();

            // Nama enemy
            Label nameLabel = new Label(enemy.getNama(), new Label.LabelStyle(fontMedium, Color.YELLOW));
            enemyCard.add(nameLabel).padBottom(5f).row();

            // HP Bar
            float hpPercent = (float)(enemy.getHealth() / enemy.getMaxHealth());
            Image hpBar = createHpBarImage(hpPercent);
            enemyCard.add(hpBar).width(100f).height(10f).padBottom(5f).row();

            // HP text
            Label hpText = new Label((int)enemy.getHealth() + "/" + (int)enemy.getMaxHealth(),
                new Label.LabelStyle(fontSmall, Color.WHITE));
            enemyCard.add(hpText).padBottom(3f).row();

            // Armor text
            String armorText = enemy.getArmor() > 0 ? "🛡 " + (int)enemy.getArmor() : "No armor";
            Color armorColor = enemy.getArmor() > 0 ? Color.CYAN : Color.GRAY;
            Label armorLabel = new Label(armorText, new Label.LabelStyle(fontSmall, armorColor));
            enemyCard.add(armorLabel).row();

            // Click handler
            enemyCard.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!enemy.isAlive()) {
                        addLog("That enemy is already dead!");
                        return;
                    }
                    if (!isPlayerTurn) {
                        addLog("Wait for your turn!");
                        return;
                    }
                    if (pendingSkill != null) {
                        useSkillOnEnemy(enemyIndex);
                    } else {
                        performAttack(enemyIndex);
                    }
                }
            });

            enemyTable.add(enemyCard).width(150f).pad(8f);

            if ((i + 1) % 2 == 0 && i != enemies.size() - 1) {
                enemyTable.row();
            }
        }
    }

    private TextureRegionDrawable createBorderDrawable(float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private Image createHpBarImage(float percent) {
        int width = 100;
        int height = 12;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        // Background merah
        pixmap.setColor(0.5f, 0f, 0f, 1f);
        pixmap.fill();

        // Foreground hijau
        int fillWidth = (int)(width * Math.max(0, Math.min(1, percent)));
        pixmap.setColor(0f, 0.8f, 0f, 1f);
        pixmap.fillRectangle(0, 0, fillWidth, height);

        // Border putih
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.drawRectangle(0, 0, width, height);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        return new Image(new TextureRegionDrawable(new TextureRegion(texture)));
    }

    private void addDamagePopup(float x, float y, int damage, boolean isCritical) {
        damagePopups.add(new DamagePopup(x, y, damage, isCritical));
    }

    private void performAttack(int enemyIndex) {
        if (!isPlayerTurn) return;

        Enemy target = enemies.get(enemyIndex);
        if (!target.isAlive()) {
            addLog("Target already dead!");
            return;
        }

        // Damage calculation
        double rawDamage = player.getAttack() * 1.5;
        double oldArmor = target.getArmor();
        double damageToArmor = 0;
        double damageToHealth = rawDamage;
        
        // Hitung damage ke armor dulu
        if (target.getArmor() > 0) {
            damageToArmor = Math.min(target.getArmor(), rawDamage * 0.4);
            target.takeDamage(damageToArmor); 
            damageToHealth = rawDamage - damageToArmor;
        }
        
        // Sisa damage ke HP
        if (damageToHealth > 0) {
            target.takeDamage(damageToHealth);
        }
        
        double totalDamage = damageToArmor + damageToHealth;
        
        // Log dengan info detail
        String logMessage = player.getNama() + " attacks " + target.getNama() + " for " + (int)totalDamage + " total damage!";
        if (damageToArmor > 0) {
            logMessage += " (Armor: -" + (int)damageToArmor + ", HP: -" + (int)damageToHealth + ")";
            addLog("  " + target.getNama() + " armor: " + (int)oldArmor + " → " + (int)target.getArmor());
        } else {
            logMessage += " (HP: -" + (int)damageToHealth + ")";
        }
        addLog(logMessage);
        
        // Damage popup
        addDamagePopup(600f, 350f, (int)totalDamage, totalDamage > 30);
        
        game.addScore((int)totalDamage);

        // Check death
        if (!target.isAlive()) {
            int killBonus = 50;
            game.addScore(killBonus);
            addLog("💀 " + target.getNama() + " died! +" + killBonus + " score!");
        }

        updateEnemyTable();

        // Animasi hurt (kena damage)
        final int hurtIndex = enemyIndex;
        enemySprites.get(hurtIndex).setState(EnemySprite.EnemyState.HURT);
        Gdx.app.postRunnable(() -> {
            try { Thread.sleep(200); } catch(Exception e) {}
            enemySprites.get(hurtIndex).setState(EnemySprite.EnemyState.IDLE);
        });

        // Check win
        boolean allDead = true;
        for (Enemy e : enemies) {
            if (e.isAlive()) {
                allDead = false;
                break;
            }
        }

        if (allDead) {
            onWaveComplete();
        } else {
            isPlayerTurn = false;
            updateUI();
            addLog("⚔️ === ENEMY TURN === ⚔️");
            Gdx.app.postRunnable(this::enemyTurn);
        }

        // Level up check
        if (game.inventory.isJustLeveledUp()) {
            addLog("🎉 LEVEL UP! Now level " + game.inventory.getLevelCount() + "! 🎉");
            addLog("❤️ HP +10 | 🛡 Armor +3 | ⚔ Attack +2");
            game.inventory.clearLevelUpFlag();
            player.heal(player.getMaxHealth() * 0.3);
            addLog("✨ You recovered 30% HP from leveling up!");
        }
    }

    private void enemyTurn() {
        if (isPlayerTurn) return;

        addLog("⚔️ ENEMIES ATTACK! ⚔️");

        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (!enemy.isAlive()) continue;

            // Buat final copy untuk lambda
            final int enemyIndex = i;
            
            // Animasi attack
            enemySprites.get(enemyIndex).setState(EnemySprite.EnemyState.ATTACK);
            Gdx.app.postRunnable(() -> {
                try { Thread.sleep(200); } catch(Exception e) {}
                enemySprites.get(enemyIndex).setState(EnemySprite.EnemyState.IDLE);
            });

            if (!player.isAlive()) break;

            // Simpan armor player sebelum damage
            double oldArmor = player.getArmor();
            double rawDamage = enemy.getAttack();
            
            // Hitung damage ke armor (maksimal 40% dari raw damage atau sisa armor)
            double damageToArmor = 0;
            double damageToHealth = rawDamage;
            
            if (player.getArmor() > 0) {
                damageToArmor = Math.min(player.getArmor(), rawDamage * 0.4);
                // Kurangi armor dulu
                player.takeDamage(damageToArmor);
                damageToHealth = rawDamage - damageToArmor;
            }
            
            // Sisa damage ke HP
            if (damageToHealth > 0) {
                player.takeDamage(damageToHealth);
            }
            
            double totalDamage = damageToArmor + damageToHealth;
            
            // Log yang rapi
            String logMsg = enemy.getNama() + " attacks " + player.getNama() + " for " + (int)totalDamage + " damage!";
            if (damageToArmor > 0 && damageToHealth > 0) {
                logMsg += " (Armor -" + (int)damageToArmor + ", HP -" + (int)damageToHealth + ")";
                addLog("  🛡 Your armor: " + (int)oldArmor + " → " + (int)player.getArmor());
            } else if (damageToArmor > 0) {
                logMsg += " (Armor -" + (int)damageToArmor + ")";
                addLog("  🛡 Your armor: " + (int)oldArmor + " → " + (int)player.getArmor());
            } else {
                logMsg += " (HP -" + (int)damageToHealth + ")";
            }
            addLog(logMsg);
            
            // Damage popup
            addDamagePopup(150f, 350f, (int)totalDamage, false);
            
            // Update UI setelah setiap attack
            updateUI();

            if (!player.isAlive()) {
                addLog("💀 === GAME OVER === 💀");
                addLog("You were defeated at Wave " + currentWave + "!");
                addLog("Final Score: " + game.totalScore);
                isPlayerTurn = false;
                return;
            }
            
            // Delay sedikit biar keliatan satu per satu
            try { Thread.sleep(100); } catch(Exception e) {}
        }

            // Giliran player
        isPlayerTurn = true;
        updateUI();
        addLog("✨ === YOUR TURN === ✨");
        addLog("Click on an enemy to attack, or use skill.");
    }

    private void onWaveComplete() {
        int waveBonus = 100 * currentWave;
        game.addScore(waveBonus);
        addLog("=== WAVE " + currentWave + " COMPLETE! +" + waveBonus + " score ===");

        // === TAMBAHKAN INI: HEAL PLAYER SETELAH WAVE ===
        player.heal(player.getMaxHealth() * 0.5);  // Heal 50% HP
        addLog("You recovered " + (int)(player.getMaxHealth() * 0.5) + " HP after wave!");

        if (currentWave >= maxWaves) {
            int levelBonus = 500;
            game.addScore(levelBonus);
            addLog("=== VICTORY! ===");
            addLog("Level " + currentLevel + " completed! +" + levelBonus + " score!");
            addLog("Press BACK to return to menu.");
            isPlayerTurn = false;
            updateUI();
        } else {
            currentWave++;
            updateBackground();
            generateWave();
            updateEnemyTable();
            isPlayerTurn = true;
            updateUI();
            addLog("Wave " + currentWave + "/" + maxWaves + " started!");
            addLog("Your turn!");
        }
    }

    private void showSkillMenu() {
        if (skillOverlay != null && skillOverlay.hasParent()) {
            skillOverlay.remove();
        }

        skillOverlay = new Table();
        skillOverlay.setFillParent(true);
        skillOverlay.setBackground(createBorderDrawable(0f, 0f, 0f, 0.85f));

        Table inner = new Table();
        inner.setBackground(createBorderDrawable(0.1f, 0.1f, 0.2f, 0.95f));
        inner.pad(20f);

        Label title = new Label("SELECT SKILL", new Label.LabelStyle(fontLarge, Color.YELLOW));
        inner.add(title).padBottom(20f).row();

        boolean hasSkill = false;

        for (Skill skill : game.inventory.getUnlockedSkills().keySet()) {
            if (game.inventory.hasSkillCharge(skill)) {
                hasSkill = true;
                TextButton skillBtn = createButton(
                    skill.getName() + " [" + game.inventory.getSkillCharge(skill) + "x]\n" + skill.getDescription(),
                    new Color(0.2f, 0.3f, 0.6f, 1f)
                );

                skillBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        pendingSkill = skill;
                        skillOverlay.remove();
                        addLog("Selected skill: " + skill.getName());

                        if (skill.getTargetType() == Skill.TargetType.SELF) {
                            useSkillOnSelf();
                        } else {
                            addLog("Click on an enemy to use " + skill.getName());
                        }
                    }
                });

                inner.add(skillBtn).width(260f).height(60f).pad(5f).row();
            }
        }

        if (!hasSkill) {
            Label noSkill = new Label("No skill charges available!\nEarn more by playing.",
                new Label.LabelStyle(fontMedium, Color.RED));
            inner.add(noSkill).padBottom(10f).row();
        }

        TextButton cancelBtn = createButton("CANCEL", new Color(0.6f, 0.2f, 0.2f, 1f));
        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                pendingSkill = null;
                skillOverlay.remove();
                addLog("Skill cancelled.");
            }
        });
        inner.add(cancelBtn).width(140f).height(40f).padTop(15f);

        skillOverlay.add(inner).center();
        stage.addActor(skillOverlay);
    }

    private void useSkillOnSelf() {
        if (pendingSkill == null) return;

        if (game.inventory.consumeSkillCharge(pendingSkill)) {
            switch (pendingSkill.getType()) {
                case HEAL:
                    player.heal(pendingSkill.getValue());
                    addLog(player.getNama() + " heals for " + (int)pendingSkill.getValue() + " HP!");
                    break;
                case BUFF_ARMOR:
                    player.buffArmor(pendingSkill.getValue());
                    addLog(player.getNama() + " gains +" + (int)pendingSkill.getValue() + " armor!");
                    break;
                default:
                    addLog("Used " + pendingSkill.getName() + " on self!");
                    break;
            }
            pendingSkill = null;
            updateUI();

            // Check win condition after skill
            boolean allDead = true;
            for (Enemy e : enemies) {
                if (e.isAlive()) {
                    allDead = false;
                    break;
                }
            }

            if (allDead) {
                onWaveComplete();
            } else {
                isPlayerTurn = false;
                updateUI();
                addLog("=== ENEMY TURN ===");
                Gdx.app.postRunnable(this::enemyTurn);
            }
        } else {
            addLog("No skill charge available!");
            pendingSkill = null;
        }
    }

    private void useSkillOnEnemy(int enemyIndex) {
        if (pendingSkill == null) return;

        Enemy target = enemies.get(enemyIndex);
        if (!target.isAlive()) {
            addLog("Target is already dead!");
            pendingSkill = null;
            return;
        }

        if (game.inventory.consumeSkillCharge(pendingSkill)) {
            
            // Catat armor awal untuk info log
            double oldArmor = target.getArmor();
            double damageToArmor = 0;
            double damageToHealth = 0;
            
            switch (pendingSkill.getType()) {
                case DAMAGE:
                    // Damage skill (true damage - ignore armor sebagian)
                    double skillDamage = pendingSkill.getValue();
                    damageToArmor = Math.min(target.getArmor(), skillDamage * 0.3);
                    damageToHealth = skillDamage - damageToArmor;
                    
                    if (damageToArmor > 0) {
                        target.takeDamage(damageToArmor);
                        addLog("  🛡 " + target.getNama() + " armor: " + (int)oldArmor + " → " + (int)target.getArmor());
                    }
                    if (damageToHealth > 0) {
                        target.takeDamage(damageToHealth);
                    }
                    
                    addLog("💥 " + player.getNama() + " uses " + pendingSkill.getName() + " on " + target.getNama() + "!");
                    addLog("  💀 Deals " + (int)skillDamage + " damage! (Armor: -" + (int)damageToArmor + ", HP: -" + (int)damageToHealth + ")");
                    game.addScore((int)skillDamage * 2);
                    addDamagePopup(600f, 350f, (int)skillDamage, true);
                    break;
                    
                case DEBUFF_ARMOR:
                    // Kurangi armor enemy
                    double armorReduce = pendingSkill.getValue();
                    target.debuffArmor(armorReduce);
                    addLog("🔻 " + player.getNama() + " uses " + pendingSkill.getName() + " on " + target.getNama() + "!");
                    addLog("  🛡 Armor reduced by " + (int)armorReduce + "! (" + (int)oldArmor + " → " + (int)target.getArmor() + ")");
                    addDamagePopup(600f, 350f, (int)armorReduce, false);
                    break;
                    
                default:
                    addLog("✨ Used " + pendingSkill.getName() + " on " + target.getNama() + "!");
                    break;
            }

            // Cek apakah enemy mati setelah kena skill
            if (!target.isAlive()) {
                int killBonus = 50;
                game.addScore(killBonus);
                addLog("💀 " + target.getNama() + " died! +" + killBonus + " score!");
            }

            // Update UI
            updateEnemyTable();
            pendingSkill = null;

            // Cek apakah semua enemy mati
            boolean allDead = true;
            for (Enemy e : enemies) {
                if (e.isAlive()) {
                    allDead = false;
                    break;
                }
            }

            if (allDead) {
                onWaveComplete();
            } else {
                // Ganti giliran ke enemy
                isPlayerTurn = false;
                updateUI();
                addLog("⚔️ === ENEMY TURN === ⚔️");
                Gdx.app.postRunnable(this::enemyTurn);
            }
            
        } else {
            addLog("❌ No skill charge available for " + pendingSkill.getName() + "!");
            pendingSkill = null;
        }
    }

    private void addLog(String message) {
        logHistory.add(message);
        if (logHistory.size > 15) {
            logHistory.removeIndex(0);
        }

        StringBuilder sb = new StringBuilder();
        for (String log : logHistory) {
            sb.append(log).append("\n");
        }

        if (logLabel != null) {
            logLabel.setText(sb.toString());
        }

        // Print ke console untuk debugging
        System.out.println("[BATTLE] " + message);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1f);

        viewport.apply();

        // Update damage popups
        for (DamagePopup popup : damagePopups) {
            popup.update(delta);
        }

        for (int i = damagePopups.size - 1; i >= 0; i--) {
            if (!damagePopups.get(i).isAlive()) {
                damagePopups.removeIndex(i);
            }
        }

        // Draw background pattern (optional)
        batch.begin();

        if (currentBackground != null) {
            batch.draw(currentBackground, 0, 0, W, H);
        }

        // Score and wave info at top
        fontSmall.setColor(Color.WHITE);
        fontSmall.draw(batch, "Score: " + game.totalScore, 10, H - 10);
        fontSmall.draw(batch, "Wave: " + currentWave + "/" + maxWaves, W - 100, H - 10);
        fontSmall.draw(batch, "Level: " + game.inventory.getLevelCount(), W - 100, H - 25);
        fontSmall.draw(batch, "Lv: " + game.inventory.getLevelCount(), 10, H - 30);

        // Draw damage popups
        for (DamagePopup popup : damagePopups) {
            popup.draw(batch, fontMedium);
        }

        // Gambar enemy sprites
        for (EnemySprite sprite : enemySprites) {
            sprite.update(delta);
            sprite.draw(batch);
        }

        batch.end();

        stage.act(delta);
        stage.draw();

        // Update player stats in real-time
        if (playerHpLabel != null) {
            playerHpLabel.setText("❤ HP: " + (int)player.getHealth() + "/" + (int)player.getMaxHealth());
            playerArmorLabel.setText("🛡 Armor: " + (int)player.getArmor());
            playerAttackLabel.setText("⚔ Attack: " + (int)player.getAttack());
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        fontSmall.dispose();
        fontMedium.dispose();
        fontLarge.dispose();

        if (backgroundWave1 != null) backgroundWave1.dispose();
        if (backgroundWave2 != null) backgroundWave2.dispose();
        if (backgroundWave3 != null) backgroundWave3.dispose();

        // Dispose enemy textures
        if (regularEnemyTex != null) regularEnemyTex.dispose();
        if (medicEnemyTex != null) medicEnemyTex.dispose();
        if (shieldEnemyTex != null) shieldEnemyTex.dispose();
        if (bulldozerEnemyTex != null) bulldozerEnemyTex.dispose();

        for (EnemySprite sprite : enemySprites) {
            sprite.dispose();
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
