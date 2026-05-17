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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import io.github.jekjek.Entity.Enemy.Enemy;
import io.github.jekjek.Entity.Player;
import io.github.jekjek.GameManager.EnemyManager;
import io.github.jekjek.GameManager.Range.Difficulty;
import io.github.jekjek.Main;
import io.github.jekjek.Skill.Skill;

import java.util.List;

public class BattleScreen implements Screen {

    private static final float W = 800f, H = 480f;

    private final Main game;
    private Stage stage;
    private FitViewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    private BitmapFont fontSmall, fontMedium, fontLarge;

    private Player player;
    private List<Enemy> enemies;
    private Array<EnemySprite> enemySprites = new Array<>();
    private int currentWave = 1;
    private int maxWaves = 3;
    private int currentLevel = 1;
    private String selectedDifficulty = "Normal";

    private boolean isPlayerTurn = true;
    private Array<String> logHistory = new Array<>();
    private Label logLabel;
    private Label playerHpLabel, playerArmorLabel, playerAttackLabel, playerScoreLabel, playerLevelLabel, waveLabel;
    private Table turnPanel;
    private Table enemyTable;
    private Array<DamagePopup> damagePopups = new Array<>();
    private Table skillOverlay;

    private Skill pendingSkill = null;
    private Table endGameOverlay;

    // Textures untuk entities (colored placeholder)
    private Texture playerTex;
    private Texture regularEnemyTex;
    private Texture medicEnemyTex;
    private Texture shieldEnemyTex;
    private Texture bulldozerEnemyTex;

    // Background textures
    private Texture backgroundWave1, backgroundWave2, backgroundWave3;
    private Texture currentBackground;

    // UI Textures
    private Texture uiPlayerPanel;
    private Texture uiEnemyPanel;
    private Texture uiStoneFrame;
    private Texture uiLogPanel;
    private Texture uiYourTurn;
    private Texture uiEnemyTurn;
    private Texture uiWavePanel;
    private Texture uiBtnAttack;
    private Texture uiBtnSkill;
    private Texture uiBtnFlee;
    private Texture iconShield;
    private Texture iconSword;

    private GameScreen parentScreen;

    public BattleScreen(Main game, String difficultyName, int wave, GameScreen parentScreen) {
        this.game = game;
        this.selectedDifficulty = difficultyName;
        this.currentWave = wave;
        this.parentScreen = parentScreen;

        if (difficultyName.equals("Easy")) maxWaves = 2;
        else if (difficultyName.equals("Normal")) maxWaves = 3;
        else maxWaves = 4;
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

        // Load UI Textures
        uiPlayerPanel = new Texture("GUI/player_panel.png");
        uiEnemyPanel  = new Texture("GUI/enemy_panel.png");
        uiStoneFrame  = new Texture("GUI/stone_frame.png");
        uiLogPanel    = new Texture("GUI/log_panel.png");
        uiYourTurn    = new Texture("GUI/your_turn.png");
        uiEnemyTurn   = new Texture("GUI/enemy_turn.png");
        uiWavePanel   = new Texture("GUI/wave_panel.png");
        uiBtnAttack   = new Texture("GUI/attack_button_green.png");
        uiBtnSkill    = new Texture("GUI/skill_button_blue.png");
        uiBtnFlee     = new Texture("GUI/flee_button_red.png");
        iconShield    = new Texture("icon_shield.png");
        iconSword     = new Texture("icon_sword.png");

        // Create colored textures for enemies
        regularEnemyTex  = createColoredTexture(0.2f, 0.4f, 0.8f, 1f);
        medicEnemyTex    = createColoredTexture(0.2f, 0.7f, 0.2f, 1f);
        shieldEnemyTex   = createColoredTexture(0.8f, 0.7f, 0.1f, 1f);
        bulldozerEnemyTex = createColoredTexture(0.8f, 0.2f, 0.2f, 1f);

        // Create player
        playerTex = createColoredTexture(0.2f, 0.8f, 0.4f, 1f);
        player = new Player(
            game.inventory.getProfileName(),
            game.inventory.getTotalHealth(),
            game.inventory.getTotalArmor(),
            game.inventory.getTotalAttack()
        );

        generateWave();
        buildUI();
        updateEnemyTable();

        isPlayerTurn = true;
        addLog("=== BATTLE START ===");
        addLog("Battle started at " + selectedDifficulty + " difficulty!");
        addLog("Your turn! Click on enemy to attack.");
    }

    private void updateBackground() {
        switch (currentWave) {
            case 1:  currentBackground = backgroundWave1; break;
            case 2:  currentBackground = backgroundWave2; break;
            case 3:  currentBackground = backgroundWave3; break;
            default: currentBackground = backgroundWave1; break;
        }
    }

    private Texture createColoredTexture(float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.drawRectangle(0, 0, 63, 63);
        pixmap.fillCircle(20, 40, 6);
        pixmap.fillCircle(44, 40, 6);
        pixmap.setColor(0f, 0f, 0f, 1f);
        pixmap.fillCircle(20, 40, 3);
        pixmap.fillCircle(44, 40, 3);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.drawLine(25, 25, 39, 25);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void generateWave() {
        EnemyManager enemyManager = new EnemyManager();
        Difficulty diff;
        switch (selectedDifficulty) {
            case "Easy": diff = new Difficulty("Easy", "", 1, 0.7, 0.7, 0.7, 1.2, 2); break;
            case "Hard": diff = new Difficulty("Hard", "", 3, 1.4, 1.3, 1.3, 0.8, 4); break;
            default:     diff = new Difficulty("Normal", "", 2, 1.0, 1.0, 1.0, 1.0, 3); break;
        }
        enemies = enemyManager.generateWave(diff, currentLevel, currentWave);

        enemySprites.clear();
        float startX = 500f;
        float startY = 300f;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            String idleFile   = "default_enemy.png";
            String attackFile = "enemy_attack.png";
            String hurtFile   = "enemy_hurt.png";
            String deadFile   = "enemy_dead.png";
            enemySprites.add(new EnemySprite(startX + (i * 80f), startY,
                idleFile, attackFile, hurtFile, deadFile));
        }

        addLog("Difficulty: " + selectedDifficulty);
        addLog("Wave " + currentWave + "/" + maxWaves + " - " + enemies.size() + " enemies appear!");
    }

    // =========================================================
    //  BUILD UI  –  layout sesuai referensi gambar
    // =========================================================
    private void buildUI() {

        // ── 1. PLAYER PANEL  (kiri-atas) ──────────────────────────
        // Dikecilkan: 185 × 128 px
        Table playerPanel = new Table();
        playerPanel.setBackground(new TextureRegionDrawable(new TextureRegion(uiPlayerPanel)));
        playerPanel.pad(16f, 20f, 16f, 20f);

        playerScoreLabel = new Label("Score: 0",
            new Label.LabelStyle(fontSmall, Color.LIGHT_GRAY));
        playerLevelLabel = new Label("Lv: 1  PLAYER",
            new Label.LabelStyle(fontSmall, Color.GOLD));
        playerHpLabel    = new Label("HP: 100/100",
            new Label.LabelStyle(fontSmall, new Color(1f, 0.3f, 0.3f, 1f)));

        playerPanel.add(playerScoreLabel).left().row();
        playerPanel.add(playerLevelLabel).left().padBottom(4f).row();

        // HP label + bar
        playerPanel.add(playerHpLabel).left().padBottom(2f).row();
        Image hpBar = createHpBarImage(1f);
        playerPanel.add(hpBar).width(130f).height(10f).left().padBottom(8f).row();

        // Armor + Attack
        Table statsRow = new Table();
        Image shieldImg = new Image(new TextureRegionDrawable(new TextureRegion(iconShield)));
        playerArmorLabel = new Label("Armor: 0",
            new Label.LabelStyle(fontSmall, Color.CYAN));
        statsRow.add(shieldImg).width(12f).height(12f).padRight(3f);
        statsRow.add(playerArmorLabel).left().padRight(12f);

        Image swordImg = new Image(new TextureRegionDrawable(new TextureRegion(iconSword)));
        playerAttackLabel = new Label("Attack: 0",
            new Label.LabelStyle(fontSmall, Color.ORANGE));
        statsRow.add(swordImg).width(12f).height(12f).padRight(3f);
        statsRow.add(playerAttackLabel).left();

        playerPanel.add(statsRow).left();

        // Wrapper: top-left
        Table playerWrapper = new Table();
        playerWrapper.setFillParent(true);
        playerWrapper.top().left().pad(8f);
        playerWrapper.add(playerPanel).width(185f).height(128f);
        stage.addActor(playerWrapper);

        // ── 2. TURN INDICATOR  (tengah-atas) ──────────────────────
        // asset your_turn.png – lebarnya cukup dominan
        // Di-render 275 × 80 px, di-tengahkan
        turnPanel = new Table();
        turnPanel.setBackground(new TextureRegionDrawable(new TextureRegion(uiYourTurn)));

        Table turnWrapper = new Table();
        turnWrapper.setFillParent(true);
        turnWrapper.top().padTop(8f);
        turnWrapper.add(turnPanel).width(275f).height(80f);
        stage.addActor(turnWrapper);

        // ── 3. ENEMY TABLE  (kanan-atas) ──────────────────────────
        // enemyTable = container kosong yang diisi oleh updateEnemyTable()
        enemyTable = new Table();
        enemyTable.top().right();

        // "Wave: X/Y" label  — pojok kanan-atas di LUAR enemy cards
        // Kita render ia di atas enemyTable sebagai row pertama
        // (updateEnemyTable() akan mengisinya)

        Table enemyWrapper = new Table();
        enemyWrapper.setFillParent(true);
        // padTop = 8 (margin) + 80 (turn indicator height) + 4 (gap) = 92
        enemyWrapper.top().right().padTop(92f).padRight(8f);
        enemyWrapper.add(enemyTable);
        stage.addActor(enemyWrapper);

        // ── 4. LOG PANEL  (kiri-bawah) ────────────────────────────
        // Tanpa background image sesuai permintaan;
        // cukup semi-transparent overlay gelap supaya teks terbaca
        Table logContainer = new Table();
        logContainer.setBackground(createBorderDrawable(0f, 0f, 0f, 0.65f));
        logContainer.pad(10f, 12f, 10f, 12f);

        logLabel = new Label("", new Label.LabelStyle(fontSmall, Color.WHITE));
        logLabel.setWrap(true);

        ScrollPane scrollPane = new ScrollPane(logLabel);
        scrollPane.setFadeScrollBars(true);
        // Lebar log-panel di gambar referensi ≈ 290px, tinggi ≈ 120px
        logContainer.add(scrollPane).width(280f).height(110f);

        // ── 5. BUTTON BAR  (bawah-tengah) ─────────────────────────
        // Setiap tombol = Stack berisi ImageButton + Label teks di tengah
        Table buttonBar = new Table();
        buttonBar.add(makeLabeledButton(uiBtnAttack, "Serang", 120f, 46f,
            new Color(0.15f, 0.7f, 0.15f, 1f))).padRight(6f);
        buttonBar.add(makeLabeledButton(uiBtnSkill,  "Skill",  110f, 46f,
            new Color(0.15f, 0.35f, 0.8f, 1f))).padRight(6f);
        buttonBar.add(makeLabeledButton(uiBtnFlee,   "Kembali",118f, 46f,
            new Color(0.75f, 0.15f, 0.15f, 1f)));

        // Tambahkan listener ke tombol melalui Stack child table
        // Listener diletakkan di Stack-level, bukan di ImageButton,
        // supaya area teks pun bisa diklik.
        // Kita simpan referensi via field agar listener bisa diakses.
        // Cara mudah: gunakan Table biasa dengan background gambar + Label overlay.

        // Hapus buttonBar yang baru dibuat, ganti pakai versi dengan listener:
        buttonBar.clear();

        Stack btnAttackStack = makeButtonStack(uiBtnAttack, "⚔  Serang", 120f, 46f);
        btnAttackStack.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (isPlayerTurn) addLog("Klik pada musuh untuk menyerang!");
                else addLog("Bukan giliranmu! Tunggu musuh.");
            }
        });

        Stack btnSkillStack = makeButtonStack(uiBtnSkill, "\uD83C\uDF00  Skill", 110f, 46f);
        btnSkillStack.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (isPlayerTurn) showSkillMenu();
                else addLog("Tidak bisa pakai skill sekarang!");
            }
        });

        Stack btnFleeStack = makeButtonStack(uiBtnFlee, "\uD83C\uDFC3  Kembali", 118f, 46f);
        btnFleeStack.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });

        buttonBar.add(btnAttackStack).width(120f).height(46f).padRight(6f);
        buttonBar.add(btnSkillStack) .width(110f).height(46f).padRight(6f);
        buttonBar.add(btnFleeStack)  .width(118f).height(46f);

        // ── 6. BOTTOM WRAPPER ─────────────────────────────────────
        // Satu root table yang menempel di bawah layar.
        // Kolom kiri = log, kolom kanan (expand) = tombol terpusat
        Table bottomWrapper = new Table();
        bottomWrapper.setFillParent(true);
        bottomWrapper.bottom().padBottom(12f).padLeft(8f).padRight(8f);

        // Log di kiri bawah
        bottomWrapper.add(logContainer).bottom().left();

        // Tombol di tengah-bawah: gunakan expandX agar mendorong ke tengah
        // Sedikit offset ke kiri agar visual center antara log dan tepi kanan
        bottomWrapper.add(buttonBar).expandX().center().padBottom(0f);

        stage.addActor(bottomWrapper);

        updateUI();
    }

    // =========================================================
    //  UPDATE ENEMY TABLE
    // =========================================================
    private void updateEnemyTable() {
        if (enemyTable == null) return;
        enemyTable.clear();

        // Baris pertama: Wave label rata kanan, spanning semua kolom
        Label waveLabel = new Label("Wave: " + currentWave + "/" + maxWaves,
            new Label.LabelStyle(fontSmall, Color.WHITE));
        enemyTable.add(waveLabel).right().colspan(2).padBottom(6f).row();

        // Baris berikutnya: kartu musuh, 2 per baris
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy   = enemies.get(i);
            final int idx = i;

            // Tipe musuh
            String enemyType;
            if      (enemy instanceof io.github.jekjek.Entity.Enemy.MedicEnemy)     enemyType = "Medic";
            else if (enemy instanceof io.github.jekjek.Entity.Enemy.ShieldEnemy)    enemyType = "Shield";
            else if (enemy instanceof io.github.jekjek.Entity.Enemy.BulldozerEnemy) enemyType = "Bulldozer";
            else                                                                      enemyType = "Regular";

            // ── Kartu musuh ──
            // Dikecilkan: 165 × 100 px
            Table enemyCard = new Table();
            enemyCard.setBackground(new TextureRegionDrawable(new TextureRegion(uiEnemyPanel)));
            enemyCard.pad(10f, 18f, 10f, 18f);

            // Nama + tipe
            Label nameLabel = new Label(enemy.getNama() + " [" + enemyType + "]",
                new Label.LabelStyle(fontSmall, Color.YELLOW));
            enemyCard.add(nameLabel).padBottom(4f).row();

            // HP Bar
            float hpPct = (float)(enemy.getHealth() / enemy.getMaxHealth());
            Image hpBar  = createHpBarImage(hpPct);
            enemyCard.add(hpBar).width(90f).height(8f).padBottom(3f).row();

            // HP teks
            Label hpText = new Label(
                "[ " + (int)enemy.getHealth() + "/" + (int)enemy.getMaxHealth() + " ]",
                new Label.LabelStyle(fontSmall, Color.WHITE));
            enemyCard.add(hpText).padBottom(4f).row();

            // Armor
            Table armorRow = new Table();
            if (enemy.getArmor() > 0) {
                Image shieldImg = new Image(new TextureRegionDrawable(new TextureRegion(iconShield)));
                armorRow.add(shieldImg).width(14f).height(14f).padRight(4f);
                armorRow.add(new Label(String.valueOf((int)enemy.getArmor()),
                    new Label.LabelStyle(fontSmall, Color.CYAN)));
            } else {
                armorRow.add(new Label("No armor",
                    new Label.LabelStyle(fontSmall, Color.GRAY)));
            }
            enemyCard.add(armorRow);

            // Click listener
            enemyCard.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    if (!enemy.isAlive()) { addLog("That enemy is already dead!"); return; }
                    if (!isPlayerTurn)    { addLog("Wait for your turn!");         return; }
                    if (pendingSkill != null) useSkillOnEnemy(idx);
                    else                      performAttack(idx);
                }
            });

            enemyTable.add(enemyCard).width(165f).height(100f).pad(5f);

            // Baris baru setiap 2 kartu (kecuali kartu terakhir)
            if ((i + 1) % 2 == 0 && i != enemies.size() - 1) {
                enemyTable.row();
            }
        }
    }

    // =========================================================
    //  HELPERS
    // =========================================================
    /** Buat Stack berisi image background + label teks putih di tengahnya. */
    private Stack makeButtonStack(Texture bgTexture, String text, float w, float h) {
        Stack stack = new Stack();

        // Layer 1: image background (stretched ke ukuran yang kita mau)
        Image bg = new Image(new TextureRegionDrawable(new TextureRegion(bgTexture)));
        stack.add(bg);

        // Layer 2: label teks di tengah
        Table labelTable = new Table();
        Label lbl = new Label(text, new Label.LabelStyle(fontMedium, Color.WHITE));
        lbl.setFontScale(0.85f);
        // Tambahkan padBottom agar posisinya terangkat sedikit (pas dengan bevel 3D tombol)
        labelTable.add(lbl).center().padBottom(8f);
        stack.add(labelTable);

        // Paksa ukuran melalui Cell di parent (caller harus set via .width/.height di Cell)
        // Ukuran kita set di Cell level, bukan di Stack. Kembalikan saja Stack-nya.
        return stack;
    }

    private Table makeLabeledButton(Texture bgTexture, String text, float w, float h, Color unused) {
        // Tidak dipakai, tapi tetap ada agar kompilasi aman.
        return new Table();
    }

    private TextButton createButton(String text, Color bgColor) {
        TextureRegionDrawable stoneDrawable =
            new TextureRegionDrawable(new TextureRegion(uiStoneFrame));
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.up   = stoneDrawable.tint(bgColor);
        style.down = stoneDrawable.tint(bgColor.cpy().mul(0.8f, 0.8f, 0.8f, 1f));
        style.over = stoneDrawable.tint(bgColor.cpy().mul(1.2f, 1.2f, 1.2f, 1f));
        style.font = fontMedium;
        style.fontColor = Color.WHITE;
        return new TextButton(text, style);
    }

    private void updateUI() {
        if (playerScoreLabel != null) {
            playerScoreLabel.setText("Score: " + game.totalScore);
            playerLevelLabel.setText(
                "Lv: " + game.inventory.getLevelCount() + "  " +
                player.getNama().toUpperCase());
        }
        if (playerHpLabel != null) {
            playerHpLabel.setText(
                "HP: " + (int)player.getHealth() + "/" + (int)player.getMaxHealth());
            playerArmorLabel.setText("Armor: " + (int)player.getArmor());
            playerAttackLabel.setText("Attack: " + (int)player.getAttack());
        }
        if (turnPanel != null) {
            turnPanel.setBackground(new TextureRegionDrawable(new TextureRegion(
                isPlayerTurn ? uiYourTurn : uiEnemyTurn)));
        }
    }

    private TextureRegionDrawable createBorderDrawable(float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(10, 10, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();
        pixmap.setColor(
            Math.min(1f, r + 0.2f),
            Math.min(1f, g + 0.2f),
            Math.min(1f, b + 0.2f), 1f);
        pixmap.drawRectangle(0, 0, 10, 10);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private Image createHpBarImage(float percent) {
        int width = 100, height = 12;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.5f, 0f, 0f, 1f);
        pixmap.fill();
        int fillWidth = (int)(width * Math.max(0, Math.min(1, percent)));
        pixmap.setColor(0f, 0.8f, 0f, 1f);
        pixmap.fillRectangle(0, 0, fillWidth, height);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.drawRectangle(0, 0, width, height);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new Image(new TextureRegionDrawable(new TextureRegion(texture)));
    }

    private void addDamagePopup(float x, float y, int damage, boolean isCritical) {
        damagePopups.add(new DamagePopup(x, y, damage, isCritical));
    }

    // =========================================================
    //  BATTLE LOGIC  (tidak berubah dari versi asli)
    // =========================================================
    private void performAttack(int enemyIndex) {
        if (!isPlayerTurn) return;
        Enemy target = enemies.get(enemyIndex);
        if (!target.isAlive()) { addLog("Target already dead!"); return; }

        double rawDamage    = player.getAttack() * 1.5;
        double oldArmor     = target.getArmor();
        double damageToArmor = 0, damageToHealth = rawDamage;

        if (target.getArmor() > 0) {
            damageToArmor = Math.min(target.getArmor(), rawDamage * 0.4);
            target.takeDamage(damageToArmor);
            damageToHealth = rawDamage - damageToArmor;
        }
        if (damageToHealth > 0) target.takeDamage(damageToHealth);

        double totalDamage = damageToArmor + damageToHealth;

        if (enemyIndex < enemySprites.size) {
            enemySprites.get(enemyIndex).setState(
                target.isAlive() ? EnemySprite.EnemyState.HURT : EnemySprite.EnemyState.DEAD);
        }

        String logMessage = player.getNama() + " attacks " + target.getNama() +
            " for " + (int)totalDamage + " total damage!";
        if (damageToArmor > 0) {
            logMessage += " (Armor: -" + (int)damageToArmor +
                ", HP: -" + (int)damageToHealth + ")";
            addLog("  " + target.getNama() + " armor: " +
                (int)oldArmor + " → " + (int)target.getArmor());
        } else {
            logMessage += " (HP: -" + (int)damageToHealth + ")";
        }
        addLog(logMessage);

        float popupX = 600f, popupY = 350f;
        if (enemyIndex < enemySprites.size) {
            popupX = enemySprites.get(enemyIndex).getX() + 32f;
            popupY = enemySprites.get(enemyIndex).getY() + 64f;
        }
        addDamagePopup(popupX, popupY, (int)totalDamage, totalDamage > 30);
        game.addScore((int)totalDamage);

        if (!target.isAlive()) {
            int killBonus = 50;
            game.addScore(killBonus);
            addLog("💀 " + target.getNama() + " died! +" + killBonus + " score!");
        }

        updateEnemyTable();

        boolean allDead = enemies.stream().noneMatch(Enemy::isAlive);
        if (allDead) {
            onWaveComplete();
        } else {
            isPlayerTurn = false;
            updateUI();
            addLog("⚔️ === ENEMY TURN === ⚔️");
            Gdx.app.postRunnable(this::enemyTurn);
        }

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
            if (!player.isAlive()) break;

            if (i < enemySprites.size)
                enemySprites.get(i).setState(EnemySprite.EnemyState.ATTACK);

            double oldArmor      = player.getArmor();
            double rawDamage     = enemy.getAttack();
            double damageToArmor = 0, damageToHealth = rawDamage;

            if (player.getArmor() > 0) {
                damageToArmor = Math.min(player.getArmor(), rawDamage * 0.4);
                player.takeDamage(damageToArmor);
                damageToHealth = rawDamage - damageToArmor;
            }
            if (damageToHealth > 0) player.takeDamage(damageToHealth);

            double totalDamage = damageToArmor + damageToHealth;

            String logMsg = enemy.getNama() + " attacks " + player.getNama() +
                " for " + (int)totalDamage + " damage!";
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

            addDamagePopup(130f, 360f, (int)totalDamage, false);
            updateUI();

            if (!player.isAlive()) {
                addLog("=== GAME OVER ===");
                addLog("You were defeated at Wave " + currentWave + "!");
                addLog("Final Score: " + game.totalScore);
                isPlayerTurn = false;
                showEndGameOverlay(false);
                return;
            }

            try { Thread.sleep(100); } catch (Exception e) { /* ignored */ }
        }

        isPlayerTurn = true;
        updateUI();
        addLog("✨ === YOUR TURN === ✨");
        addLog("Click on an enemy to attack, or use skill.");
    }

    private void onWaveComplete() {
        int waveBonus = 100 * currentWave;
        game.addScore(waveBonus);
        addLog("=== WAVE " + currentWave + " COMPLETE! +" + waveBonus + " score ===");
        player.heal(player.getMaxHealth() * 0.5);
        addLog("You recovered " + (int)(player.getMaxHealth() * 0.5) + " HP after wave!");

        if (currentWave >= maxWaves) {
            int levelBonus = 500;
            game.addScore(levelBonus);
            addLog("=== VICTORY! ===");
            addLog("Level " + currentLevel + " completed! +" + levelBonus + " score!");
            isPlayerTurn = false;
            updateUI();
            showEndGameOverlay(true);
        } else {
            isPlayerTurn = false;
            updateUI();
            showWaveClearedOverlay();
        }
    }

    private void showWaveClearedOverlay() {
        if (endGameOverlay != null && endGameOverlay.hasParent()) endGameOverlay.remove();
        endGameOverlay = new Table();
        endGameOverlay.setFillParent(true);
        endGameOverlay.setBackground(createBorderDrawable(0f, 0f, 0f, 0.85f));

        Table inner = new Table();
        inner.setBackground(createBorderDrawable(0.1f, 0.1f, 0.2f, 0.95f));
        inner.pad(30f);

        Label title = new Label("WAVE " + currentWave + " CLEARED!",
            new Label.LabelStyle(fontLarge, Color.CYAN));
        inner.add(title).padBottom(20f).row();

        TextButton continueBtn = createButton("CONTINUE EXPLORING",
            new Color(0.2f, 0.6f, 0.2f, 1f));
        continueBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (parentScreen != null) {
                    parentScreen.currentWave++;
                    parentScreen.encounterX      += 600f;
                    parentScreen.isTransitioning  = false;
                    game.setScreen(parentScreen);
                } else {
                    game.setScreen(new MenuScreen(game));
                }
            }
        });
        inner.add(continueBtn).width(300f).height(50f);

        endGameOverlay.add(inner).center();
        stage.addActor(endGameOverlay);
    }

    private void showEndGameOverlay(boolean isWin) {
        if (endGameOverlay != null && endGameOverlay.hasParent()) endGameOverlay.remove();
        endGameOverlay = new Table();
        endGameOverlay.setFillParent(true);
        endGameOverlay.setBackground(createBorderDrawable(0f, 0f, 0f, 0.85f));

        Table inner = new Table();
        inner.setBackground(createBorderDrawable(0.1f, 0.1f, 0.2f, 0.95f));
        inner.pad(30f);

        String titleText  = isWin ? "VICTORY!" : "GAME OVER";
        Color  titleColor = isWin ? Color.YELLOW : Color.RED;
        inner.add(new Label(titleText, new Label.LabelStyle(fontLarge, titleColor)))
            .padBottom(20f).row();

        String message = isWin
            ? "Level " + currentLevel + " completed!"
            : "You were defeated at Wave " + currentWave + ".";
        inner.add(new Label(message, new Label.LabelStyle(fontMedium, Color.WHITE)))
            .padBottom(10f).row();
        inner.add(new Label("Final Score: " + game.totalScore,
            new Label.LabelStyle(fontMedium, Color.CYAN))).padBottom(20f).row();

        TextButton returnBtn = createButton("RETURN TO MENU",
            new Color(0.2f, 0.4f, 0.8f, 1f));
        returnBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });
        inner.add(returnBtn).width(200f).height(50f);

        endGameOverlay.add(inner).center();
        stage.addActor(endGameOverlay);
    }

    private void showSkillMenu() {
        if (skillOverlay != null && skillOverlay.hasParent()) skillOverlay.remove();
        skillOverlay = new Table();
        skillOverlay.setFillParent(true);
        skillOverlay.setBackground(createBorderDrawable(0f, 0f, 0f, 0.85f));

        Table inner = new Table();
        inner.setBackground(createBorderDrawable(0.1f, 0.1f, 0.2f, 0.95f));
        inner.pad(20f);

        inner.add(new Label("SELECT SKILL",
            new Label.LabelStyle(fontLarge, Color.YELLOW))).padBottom(20f).row();

        boolean hasSkill = false;
        for (Skill skill : game.inventory.getUnlockedSkills().keySet()) {
            if (game.inventory.hasSkillCharge(skill)) {
                hasSkill = true;
                TextButton skillBtn = createButton(
                    skill.getName() + " [" + game.inventory.getSkillCharge(skill) + "x]\n" +
                    skill.getDescription(),
                    new Color(0.2f, 0.3f, 0.6f, 1f));
                skillBtn.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent event, float x, float y) {
                        pendingSkill = skill;
                        skillOverlay.remove();
                        addLog("Selected skill: " + skill.getName());
                        if (skill.getTargetType() == Skill.TargetType.SELF)
                            useSkillOnSelf();
                        else
                            addLog("Click on an enemy to use " + skill.getName());
                    }
                });
                inner.add(skillBtn).width(260f).height(60f).pad(5f).row();
            }
        }
        if (!hasSkill) {
            inner.add(new Label("No skill charges available!\nEarn more by playing.",
                new Label.LabelStyle(fontMedium, Color.RED))).padBottom(10f).row();
        }

        TextButton cancelBtn = createButton("CANCEL", new Color(0.6f, 0.2f, 0.2f, 1f));
        cancelBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
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
            boolean allDead = enemies.stream().noneMatch(Enemy::isAlive);
            if (allDead) onWaveComplete();
            else {
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
        if (!target.isAlive()) { addLog("Target is already dead!"); pendingSkill = null; return; }

        if (game.inventory.consumeSkillCharge(pendingSkill)) {
            double oldArmor = target.getArmor();
            switch (pendingSkill.getType()) {
                case DAMAGE: {
                    double skillDamage   = pendingSkill.getValue();
                    double damageToArmor = Math.min(target.getArmor(), skillDamage * 0.3);
                    double damageToHealth = skillDamage - damageToArmor;
                    if (damageToArmor  > 0) target.takeDamage(damageToArmor);
                    if (damageToHealth > 0) target.takeDamage(damageToHealth);
                    addLog("💥 " + player.getNama() + " uses " + pendingSkill.getName() +
                        " on " + target.getNama() + "!");
                    addLog("  💀 Deals " + (int)skillDamage + " damage! (Armor: -" +
                        (int)damageToArmor + ", HP: -" + (int)damageToHealth + ")");
                    game.addScore((int)skillDamage * 2);
                    float popupX = 600f, popupY = 350f;
                    if (enemyIndex < enemySprites.size) {
                        popupX = enemySprites.get(enemyIndex).getX() + 32f;
                        popupY = enemySprites.get(enemyIndex).getY() + 64f;
                    }
                    addDamagePopup(popupX, popupY, (int)skillDamage, true);
                    break;
                }
                case DEBUFF_ARMOR: {
                    double armorReduce = pendingSkill.getValue();
                    target.debuffArmor(armorReduce);
                    addLog("🔻 " + player.getNama() + " uses " + pendingSkill.getName() +
                        " on " + target.getNama() + "!");
                    addLog("  🛡 Armor reduced by " + (int)armorReduce +
                        "! (" + (int)oldArmor + " → " + (int)target.getArmor() + ")");
                    float debuffX = 600f, debuffY = 350f;
                    if (enemyIndex < enemySprites.size) {
                        debuffX = enemySprites.get(enemyIndex).getX() + 32f;
                        debuffY = enemySprites.get(enemyIndex).getY() + 64f;
                    }
                    addDamagePopup(debuffX, debuffY, (int)armorReduce, false);
                    break;
                }
                default:
                    addLog("✨ Used " + pendingSkill.getName() + " on " + target.getNama() + "!");
                    break;
            }
            updateEnemyTable();
            if (!target.isAlive()) {
                int killBonus = 50;
                game.addScore(killBonus);
                addLog("💀 " + target.getNama() + " died! +" + killBonus + " score!");
            }
            pendingSkill = null;
            boolean allDead = enemies.stream().noneMatch(Enemy::isAlive);
            if (allDead) onWaveComplete();
            else {
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
        if (logHistory.size > 15) logHistory.removeIndex(0);
        StringBuilder sb = new StringBuilder();
        for (String log : logHistory) sb.append(log).append("\n");
        if (logLabel != null) logLabel.setText(sb.toString());
        System.out.println("[BATTLE] " + message);
    }

    // =========================================================
    //  RENDER
    // =========================================================
    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1f);
        viewport.apply();

        for (DamagePopup popup : damagePopups) popup.update(delta);
        for (int i = damagePopups.size - 1; i >= 0; i--)
            if (!damagePopups.get(i).isAlive()) damagePopups.removeIndex(i);

        for (EnemySprite sprite : enemySprites) sprite.update(delta);

        batch.begin();
        if (currentBackground != null) batch.draw(currentBackground, 0, 0, W, H);

        // Player placeholder sprite
        if (playerTex != null && player.isAlive())
            batch.draw(playerTex, 100f, 300f, 64f, 64f);

        // Enemy sprites
        for (EnemySprite sprite : enemySprites) sprite.draw(batch);

        // Damage popups
        for (DamagePopup popup : damagePopups) popup.draw(batch, fontMedium);

        batch.end();

        stage.act(delta);
        stage.draw();

        updateUI();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        fontSmall.dispose();
        fontMedium.dispose();
        fontLarge.dispose();
        if (backgroundWave1   != null) backgroundWave1.dispose();
        if (backgroundWave2   != null) backgroundWave2.dispose();
        if (backgroundWave3   != null) backgroundWave3.dispose();
        if (playerTex         != null) playerTex.dispose();
        if (regularEnemyTex   != null) regularEnemyTex.dispose();
        if (medicEnemyTex     != null) medicEnemyTex.dispose();
        if (shieldEnemyTex    != null) shieldEnemyTex.dispose();
        if (bulldozerEnemyTex != null) bulldozerEnemyTex.dispose();
        if (uiPlayerPanel     != null) uiPlayerPanel.dispose();
        if (uiEnemyPanel      != null) uiEnemyPanel.dispose();
        if (uiStoneFrame      != null) uiStoneFrame.dispose();
        if (uiLogPanel        != null) uiLogPanel.dispose();
        if (uiYourTurn        != null) uiYourTurn.dispose();
        if (uiEnemyTurn       != null) uiEnemyTurn.dispose();
        if (uiWavePanel       != null) uiWavePanel.dispose();
        if (uiBtnAttack       != null) uiBtnAttack.dispose();
        if (uiBtnSkill        != null) uiBtnSkill.dispose();
        if (uiBtnFlee         != null) uiBtnFlee.dispose();
        if (iconShield        != null) iconShield.dispose();
        if (iconSword         != null) iconSword.dispose();
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
}