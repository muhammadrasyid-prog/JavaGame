package io.github.jekjek.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.StretchViewport;

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
    
    // Player idle animation
    private Texture playerIdleSheet;
    private Animation<TextureRegion> playerIdleAnim;
    private float playerAnimTime = 0f;
    private Stage stage;
    private StretchViewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    private BitmapFont fontSmall, fontMedium, fontLarge;

    private Player player;
    private List<Enemy> enemies;
    private Array<EnemySprite> enemySprites = new Array<>();
    private Array<Table> enemyCards = new Array<>();
    private Array<Actor> enemyBodyActors = new Array<>();
    private int currentWave = 1;
    private int maxWaves = 3;
    private int currentLevel = 1;
    private String selectedDifficulty = "Normal";

    // Predefined formation slot coordinates for different numbers of enemies
    private static final float[][] FORMATION_2_SLOTS = {{480f, 135f}, {640f, 155f}};
    private static final float[][] FORMATION_3_SLOTS = {{460f, 135f}, {560f, 160f}, {660f, 135f}};
    private static final float[][] FORMATION_4_SLOTS = {{440f, 135f}, {515f, 157f}, {590f, 135f}, {665f, 157f}};
    private static final float[][] FORMATION_5_SLOTS = {{420f, 135f}, {485f, 157f}, {550f, 135f}, {615f, 157f}, {680f, 135f}};

    public enum BattleState {
        PLAYER_TURN,
        PLAYER_ACTION,
        ENEMY_TURN,
        ENEMY_ACTION,
        WAVE_CLEAR,
        BATTLE_END
    }
    private BattleState currentState = BattleState.PLAYER_TURN;
    private float stateTimer = 0f;
    private int activeEnemyIndex = -1;
    private int activeActionPhase = 0; // 0 = Anticipation/Lunge, 1 = Hit/Popup, 2 = Reaction/Dead
    private int enemyTurnListIndex = 0;

    private boolean isPlayerTurn = true;
    private boolean firstTurn    = true;  // skip announcement on very first turn
    private TransitionOverlay turnOverlay;
    private Array<String> logHistory = new Array<>();
    private Label logLabel;
    private Label playerHpLabel, playerArmorLabel, playerAttackLabel, playerScoreLabel, playerLevelLabel, waveLabel;
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

    // Procedural UI elements
    private TextureRegionDrawable customLogBg;
    private Texture customLogBgTex;

    // UI Textures
    private Texture uiPlayerPanel;
    private Texture uiEnemyPanel;
    private Texture uiStoneFrame;
    private Texture uiLogPanel;
    private Texture uiWavePanel;
    private Texture iconShield;
    private Texture iconSword;
    private Texture guiIconSheet;
    private TextureRegion iconAttack;
    private TextureRegion iconSkill;
    private TextureRegion iconFlee;
    
    // Custom button textures from assets/GUI/
    private Texture btnAttackTex;
    private Texture btnSkillTex;
    private Texture btnFleeTex;

    // Skill Visual Effect states
    private boolean showSkillEffect = false;
    private Skill.SkillType skillEffectType = null;
    private float skillEffectTimer = 0f;

    private GameScreen parentScreen;
    private boolean initialized = false;

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
        if (initialized) {
            Gdx.input.setInputProcessor(stage);
            return;
        }
        initialized = true;

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        viewport = new StretchViewport(W, H);
        stage = new Stage(viewport, batch);
        Gdx.input.setInputProcessor(stage);

        // Fonts
        fontSmall = new BitmapFont();
        fontSmall.getData().setScale(0.8f);
        fontSmall.getData().setLineHeight(fontSmall.getData().lineHeight * 1.18f);
        fontSmall.getData().markupEnabled = true;
        fontMedium = new BitmapFont();
        fontMedium.getData().setScale(1.2f);
        fontLarge = new BitmapFont();
        fontLarge.getData().setScale(1.6f);

        turnOverlay = new TransitionOverlay();

        // Load backgrounds
        backgroundWave1 = new Texture("GUNUNGMERAPI.png");
        backgroundWave2 = new Texture("RINGROAD.png");
        backgroundWave3 = new Texture("MALIOBORO.png");
        updateBackground();

        // Load UI Textures
        uiPlayerPanel = new Texture("GUI/player_panel.png");
        uiPlayerPanel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        uiEnemyPanel  = new Texture("GUI/enemy_panel.png");
        uiEnemyPanel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        uiStoneFrame  = new Texture("GUI/stone_frame.png");
        uiStoneFrame.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        uiLogPanel    = new Texture("GUI/log_panel.png");
        uiLogPanel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        uiWavePanel   = new Texture("GUI/wave_panel.png");
        uiWavePanel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        iconShield    = new Texture("icon_shield.png");
        iconShield.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        iconSword     = new Texture("icon_sword.png");
        iconSword.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        guiIconSheet  = new Texture("GUI/icon.png");
        guiIconSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        iconAttack = new TextureRegion(guiIconSheet, 0 * 32, 0 * 32, 32, 32);
        iconSkill  = new TextureRegion(guiIconSheet, 4 * 32, 2 * 32, 32, 32);
        iconFlee   = new TextureRegion(guiIconSheet, 6 * 32, 5 * 32, 32, 32);

        btnAttackTex = new Texture("GUI/attack_button_green.png");
        btnAttackTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        btnSkillTex  = new Texture("GUI/skill_button_blue.png");
        btnSkillTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        btnFleeTex   = new Texture("GUI/flee_button_red.png");
        btnFleeTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Create premium procedural background for log panel
        customLogBg = createPremiumPanelDrawable(300, 143);

        // Create colored textures for enemies
        regularEnemyTex  = createColoredTexture(0.2f, 0.4f, 0.8f, 1f);
        medicEnemyTex    = createColoredTexture(0.2f, 0.7f, 0.2f, 1f);
        shieldEnemyTex   = createColoredTexture(0.8f, 0.7f, 0.1f, 1f);
        bulldozerEnemyTex = createColoredTexture(0.8f, 0.2f, 0.2f, 1f);

        // Create player
        playerTex = createColoredTexture(0.2f, 0.8f, 0.4f, 1f);
        try {
            playerIdleSheet = new Texture("MC/idle.png");
            playerIdleSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            int frameWidth = 768;
            int frameHeight = 448;
            TextureRegion[][] playerTmp = TextureRegion.split(playerIdleSheet, frameWidth, frameHeight);
            playerIdleAnim = new Animation<>(0.15f, 
                playerTmp[0][0], playerTmp[0][1], playerTmp[0][2], playerTmp[0][3]
            );
            playerIdleAnim.setPlayMode(Animation.PlayMode.LOOP);
            System.out.println("[BattleScreen] Loaded player idle animation successfully.");
        } catch (Exception e) {
            System.out.println("[BattleScreen] Failed to load player idle animation: " + e.getMessage());
        }
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
        int count = enemies.size();
        float[][] slots;
        if (count <= 2) {
            slots = FORMATION_2_SLOTS;
        } else if (count == 3) {
            slots = FORMATION_3_SLOTS;
        } else if (count == 4) {
            slots = FORMATION_4_SLOTS;
        } else {
            slots = FORMATION_5_SLOTS;
        }

        for (int i = 0; i < count; i++) {
            Enemy enemy = enemies.get(i);
            String idleFile   = "default_enemy.png";
            String attackFile = "enemy_attack.png";
            String hurtFile   = "enemy_hurt.png";
            String deadFile   = "enemy_dead.png";
            
            float enemyX = slots[i][0];
            float enemyY = slots[i][1];
            
            enemySprites.add(new EnemySprite(enemyX, enemyY,
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



        // ── 3. WAVE PANEL (kanan-atas) ──────────────────────────
        // enemyTable = container untuk menampilkan Wave Indicator
        enemyTable = new Table();
        enemyTable.center();

        Table enemyWrapper = new Table();
        enemyWrapper.setFillParent(true);
        // Disejajarkan padTop(8f) rata dengan panel player di kiri-atas
        enemyWrapper.top().right().padTop(8f).padRight(8f);
        enemyWrapper.add(enemyTable).width(120f).height(46f);
        stage.addActor(enemyWrapper);

        // ── 4. LOG PANEL  (kiri-bawah) ────────────────────────────
        // Menggunakan background board premium prosedural buatan sendiri (manual visual element)
        Table logContainer = new Table();
        logContainer.setBackground(customLogBg);
        // Padding disesuaikan agar teks log pas berada di dalam border panel dengan rapi
        logContainer.pad(14f, 20f, 14f, 20f);

        logLabel = new Label("", new Label.LabelStyle(fontSmall, Color.WHITE));
        logLabel.setWrap(true);

        Table scrollTable = new Table();
        scrollTable.add(logLabel).expandX().fillX().padLeft(6f).padRight(12f).padTop(6f).padBottom(6f);

        ScrollPane scrollPane = new ScrollPane(scrollTable);
        scrollPane.setFadeScrollBars(true);
        scrollPane.setScrollingDisabled(true, false);
        
        // Membiarkan scrollPane mengisi penuh area dalam logContainer yang sudah dipad
        logContainer.add(scrollPane).expand().fill();

        // ── 5. BUTTON BAR  (bawah-tengah) ─────────────────────────
        Table buttonBar = new Table();

        // Tombol interaktif menggunakan asset custom bergaya RPG: attack_button_green, skill_button_blue, flee_button_red
        TextButton btnAttack = makeBattleBtn("Serang", btnAttackTex, iconAttack);
        TextButton btnSkill  = makeBattleBtn("Skill", btnSkillTex, iconSkill);
        TextButton btnFlee   = makeBattleBtn("Kembali", btnFleeTex, iconFlee);

        btnAttack.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (currentState != BattleState.PLAYER_TURN) return;
                if (isPlayerTurn) addLog("Klik pada musuh untuk menyerang!");
                else addLog("Bukan giliranmu! Tunggu musuh.");
            }
        });

        btnSkill.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (currentState != BattleState.PLAYER_TURN) return;
                if (isPlayerTurn) showSkillMenu();
                else addLog("Tidak bisa pakai skill sekarang!");
            }
        });

        btnFlee.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (currentState != BattleState.PLAYER_TURN) return;
                game.setScreen(new MenuScreen(game));
            }
        });

        buttonBar.add(btnAttack).width(142f).height(38f).padRight(6f);
        buttonBar.add(btnSkill) .width(142f).height(38f).padRight(6f);
        buttonBar.add(btnFlee)  .width(135f).height(38f);

        // ── 6. BOTTOM WRAPPER ─────────────────────────────────────
        // Satu root table yang menempel di bawah layar.
        // Kolom kiri = log, kolom kanan (expand) = tombol terpusat
        Table bottomWrapper = new Table();
        bottomWrapper.setFillParent(true);
        bottomWrapper.bottom().left().padBottom(12f).padLeft(8f).padRight(8f);

        // Log di kiri bawah dengan ukuran presisi yang menjaga rasio aspek asli (300 x 143 px)
        bottomWrapper.add(logContainer).width(300f).height(143f).bottom().left();

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

        // Wave label di pojok kanan-atas menggunakan uiWavePanel background
        enemyTable.setBackground(new TextureRegionDrawable(new TextureRegion(uiWavePanel)));
        Label waveLabel = new Label("WAVE " + currentWave + "/" + maxWaves,
            new Label.LabelStyle(fontMedium, Color.WHITE));
        // Sejajarkan tulisan Wave secara vertikal dan horisontal di tengah cell panel (diturunkan sedikit biar pas visual)
        enemyTable.add(waveLabel).expand().center().padTop(4f);

        // Bersihkan kartu musuh yang melayang sebelumnya dari stage
        for (Table card : enemyCards) {
            card.remove();
        }
        enemyCards.clear();

        // Bersihkan body actor musuh sebelumnya
        for (Actor actor : enemyBodyActors) {
            actor.remove();
        }
        enemyBodyActors.clear();

        // Buat kartu musuh melayang secara dinamis untuk setiap musuh
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy   = enemies.get(i);
            final int idx = i;

            // Tipe musuh
            String enemyType;
            if      (enemy instanceof io.github.jekjek.Entity.Enemy.MedicEnemy)     enemyType = "Medic";
            else if (enemy instanceof io.github.jekjek.Entity.Enemy.ShieldEnemy)    enemyType = "Shield";
            else if (enemy instanceof io.github.jekjek.Entity.Enemy.BulldozerEnemy) enemyType = "Bulldozer";
            else                                                                      enemyType = "Regular";

            // ── Kartu musuh parchment kuning ──
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

            // Click listener agar panel musuh melayang interaktif saat diklik
            enemyCard.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    if (!enemy.isAlive()) { addLog("That enemy is already dead!"); return; }
                    if (!isPlayerTurn)    { addLog("Wait for your turn!");         return; }
                    if (pendingSkill != null) useSkillOnEnemy(idx);
                    else                      performAttack(idx);
                }
            });

            // Clickable body actor agar sprite visual musuh interaktif saat diklik langsung
            Actor bodyActor = new Actor();
            bodyActor.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    if (!enemy.isAlive()) { addLog("That enemy is already dead!"); return; }
                    if (!isPlayerTurn)    { addLog("Wait for your turn!");         return; }
                    if (pendingSkill != null) useSkillOnEnemy(idx);
                    else                      performAttack(idx);
                }
            });

            // Tambahkan langsung ke stage
            stage.addActor(bodyActor);
            enemyBodyActors.add(bodyActor);

            // Tambahkan langsung ke stage sebagai floating actor
            stage.addActor(enemyCard);
            enemyCards.add(enemyCard);
        }
        
        // Panggil update posisi kartu untuk inisialisasi awal
        updateFloatingCardPositions();
    }

    private void updateFloatingCardPositions() {
        for (int i = 0; i < enemies.size(); i++) {
            if (i < enemyCards.size && i < enemySprites.size) {
                Table card = enemyCards.get(i);
                Enemy enemy = enemies.get(i);
                
                Actor bodyActor = null;
                if (i < enemyBodyActors.size) {
                    bodyActor = enemyBodyActors.get(i);
                }
                
                if (!enemy.isAlive()) {
                    card.setVisible(false);
                    if (bodyActor != null) bodyActor.setVisible(false);
                    continue;
                }
                card.setVisible(true);
                if (bodyActor != null) bodyActor.setVisible(true);
                
                EnemySprite sprite = enemySprites.get(i);
                
                float panelWidth = 165f;
                float panelHeight = 100f;
                float panelX = sprite.getX() - panelWidth / 2f + sprite.lungeOffset;
                // Posisi Y disesuaikan agar melayang tepat di atas sprite musuh secara dinamis
                float panelY = sprite.getY() + 115f;
                
                card.setBounds(panelX, panelY, panelWidth, panelHeight);
                
                // Update bounds dari clickable body actor untuk menyesuaikan dengan tubuh visual musuh (tanpa area kosong transparan di samping)
                if (bodyActor != null) {
                    float bodyW = 80f;
                    float bodyH = 100f;
                    float bodyX = sprite.getX() - bodyW / 2f + sprite.lungeOffset;
                    float bodyY = sprite.getY() - 10f;
                    bodyActor.setBounds(bodyX, bodyY, bodyW, bodyH);
                }
            }
        }
    }

    // =========================================================
    //  HELPERS
    // =========================================================
    /**
     * Buat TextButton ala MenuScreen: tanpa background asset,
     * background dan border digambar oleh ShapeRenderer di drawBattleBtnBg().
     * Lengkap dengan scale-in/out saat hover.
     */
    private TextButton makeBattleBtn(String label, Texture backgroundTex, com.badlogic.gdx.graphics.g2d.TextureRegion iconRegion) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        if (backgroundTex != null) {
            s.up = new TextureRegionDrawable(new TextureRegion(backgroundTex));
            s.down = ((TextureRegionDrawable)s.up).tint(new Color(0.8f, 0.8f, 0.8f, 1f));
            s.over = ((TextureRegionDrawable)s.up).tint(new Color(1.15f, 1.15f, 1.15f, 1f));
        }
        s.font          = fontMedium;
        s.fontColor     = Color.WHITE;
        s.overFontColor = new Color(1f, 0.95f, 0.4f, 1f);
        s.downFontColor = new Color(0.55f, 0.55f, 0.55f, 1f);
        TextButton btn = new TextButton(label, s);
        btn.getLabel().setAlignment(com.badlogic.gdx.utils.Align.center);

        if (iconRegion != null) {
            btn.clearChildren();
            Image iconImg = new Image(iconRegion);
            btn.add(iconImg).size(18f, 18f).padRight(6f).align(com.badlogic.gdx.utils.Align.center);
            btn.add(btn.getLabel()).align(com.badlogic.gdx.utils.Align.center);
        }

        btn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void enter(InputEvent e, float x, float y, int ptr, Actor from) {
                btn.clearActions(); btn.addAction(Actions.scaleTo(1.06f, 1.06f, 0.08f));
            }
            @Override public void exit(InputEvent e, float x, float y, int ptr, Actor to) {
                btn.clearActions(); btn.addAction(Actions.scaleTo(1f, 1f, 0.08f));
            }
        });
        return btn;
    }

    // ── Battle Button ShapeRenderer Background (ala MenuScreen) ─────────────
    private void drawAllBattleBtnBg() {
        for (Actor a : stage.getActors()) drawBattleBtnBgInActor(a);
    }

    private void drawBattleBtnBgInActor(Actor actor) {
        if (!actor.isVisible()) return;
        if (actor instanceof Table t) {
            for (Actor c : t.getChildren()) drawBattleBtnBgInActor(c);
        }
        if (!(actor instanceof TextButton btn)) return;
        if (btn.getStyle().up != null) return; // Skip buttons with texture backgrounds!
        com.badlogic.gdx.math.Vector2 pos =
            btn.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(0, 0));
        float bx = pos.x, by = pos.y, bw = btn.getWidth(), bh = btn.getHeight();
        float r, g, b;
        if      (btn.isPressed()) { r=0.07f; g=0.12f; b=0.30f; }
        else if (btn.isOver())    { r=0.20f; g=0.38f; b=0.76f; }
        else                      { r=0.11f; g=0.19f; b=0.42f; }
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(r, g, b, btn.isOver() ? 0.97f : 0.88f);
        drawBtnRoundRect(bx, by, bw, bh, 8f);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(
            btn.isOver() ? 0.55f : 0.27f,
            btn.isOver() ? 0.75f : 0.42f,
            1f,
            btn.isOver() ? 1f : 0.55f);
        drawBtnRoundRectLine(bx, by, bw, bh, 8f);
        shapeRenderer.end();
    }

    private void drawBtnRoundRect(float x, float y, float w, float h, float r) {
        r = Math.min(r, Math.min(w, h) / 2f);
        shapeRenderer.rect(x+r, y, w-2*r, h);
        shapeRenderer.rect(x, y+r, r, h-2*r);
        shapeRenderer.rect(x+w-r, y+r, r, h-2*r);
        drawBtnCornerFilled(x+r,   y+r,   r, 180, 6);
        drawBtnCornerFilled(x+w-r, y+r,   r, 270, 6);
        drawBtnCornerFilled(x+w-r, y+h-r, r, 0,   6);
        drawBtnCornerFilled(x+r,   y+h-r, r, 90,  6);
    }
    private void drawBtnCornerFilled(float cx, float cy, float r, float s, int n) {
        for (int i = 0; i < n; i++) {
            float a1 = s + i * 90f / n, a2 = s + (i+1) * 90f / n;
            shapeRenderer.triangle(cx, cy,
                cx + r * com.badlogic.gdx.math.MathUtils.cosDeg(a1),
                cy + r * com.badlogic.gdx.math.MathUtils.sinDeg(a1),
                cx + r * com.badlogic.gdx.math.MathUtils.cosDeg(a2),
                cy + r * com.badlogic.gdx.math.MathUtils.sinDeg(a2));
        }
    }
    private void drawBtnRoundRectLine(float x, float y, float w, float h, float r) {
        r = Math.min(r, Math.min(w, h) / 2f);
        shapeRenderer.line(x+r, y, x+w-r, y);
        shapeRenderer.line(x+r, y+h, x+w-r, y+h);
        shapeRenderer.line(x, y+r, x, y+h-r);
        shapeRenderer.line(x+w, y+r, x+w, y+h-r);
        drawBtnCornerLine(x+r,   y+r,   r, 180, 6);
        drawBtnCornerLine(x+w-r, y+r,   r, 270, 6);
        drawBtnCornerLine(x+w-r, y+h-r, r, 0,   6);
        drawBtnCornerLine(x+r,   y+h-r, r, 90,  6);
    }
    private void drawBtnCornerLine(float cx, float cy, float r, float s, int n) {
        for (int i = 0; i < n; i++) {
            float a1 = s + i * 90f / n, a2 = s + (i+1) * 90f / n;
            shapeRenderer.line(
                cx + r * com.badlogic.gdx.math.MathUtils.cosDeg(a1),
                cy + r * com.badlogic.gdx.math.MathUtils.sinDeg(a1),
                cx + r * com.badlogic.gdx.math.MathUtils.cosDeg(a2),
                cy + r * com.badlogic.gdx.math.MathUtils.sinDeg(a2));
        }
    }

    private TextButton createButton(String text, Color bgColor) {
        return createButton(text, bgColor, null);
    }

    private TextButton createButton(String text, Color bgColor, TextureRegion iconRegion) {
        TextureRegionDrawable stoneDrawable =
            new TextureRegionDrawable(new TextureRegion(uiStoneFrame));
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.up   = stoneDrawable.tint(bgColor);
        style.down = stoneDrawable.tint(bgColor.cpy().mul(0.8f, 0.8f, 0.8f, 1f));
        style.over = stoneDrawable.tint(bgColor.cpy().mul(1.2f, 1.2f, 1.2f, 1f));
        style.font = fontMedium;
        style.fontColor = Color.WHITE;
        TextButton btn = new TextButton(text, style);
        if (iconRegion != null) {
            btn.clearChildren();
            Image iconImg = new Image(iconRegion);
            btn.add(iconImg).size(24f, 24f).padRight(10f).align(com.badlogic.gdx.utils.Align.center);
            btn.add(btn.getLabel()).align(com.badlogic.gdx.utils.Align.center);
        }
        return btn;
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

        updateFloatingCardPositions();
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

    private TextureRegionDrawable createPremiumPanelDrawable(int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        
        // 1. Solid dark slate/navy background with high opacity (0.80f)
        pixmap.setColor(0.06f, 0.09f, 0.16f, 0.80f);
        pixmap.fill();
        
        // 2. Thick outer border in deep gold
        pixmap.setColor(0.72f, 0.53f, 0.04f, 1f);
        pixmap.drawRectangle(0, 0, width, height);
        pixmap.drawRectangle(1, 1, width - 2, height - 2);
        
        // 3. Thin inner border in bright gold
        pixmap.setColor(1.00f, 0.84f, 0.00f, 1f);
        pixmap.drawRectangle(3, 3, width - 6, height - 6);
        
        // 4. Corner decorative squares (4x4 pixels)
        pixmap.setColor(1.00f, 0.84f, 0.00f, 1f);
        pixmap.fillRectangle(0, 0, 4, 4);
        pixmap.fillRectangle(width - 4, 0, 4, 4);
        pixmap.fillRectangle(0, height - 4, 4, 4);
        pixmap.fillRectangle(width - 4, height - 4, 4, 4);
        
        customLogBgTex = new Texture(pixmap);
        customLogBgTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(customLogBgTex));
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
        if (currentState != BattleState.PLAYER_TURN) return;
        Enemy target = enemies.get(enemyIndex);
        if (!target.isAlive()) { addLog("Target already dead!"); return; }

        currentState = BattleState.PLAYER_ACTION;
        activeEnemyIndex = enemyIndex;
        activeActionPhase = 0; // Anticipation
        stateTimer = 0f;
        pendingSkill = null; // Standard attack

    }

    private void executePlayerActionHit() {
        if (pendingSkill != null) {
            showSkillEffect = true;
            skillEffectType = pendingSkill.getType();
            skillEffectTimer = 0f;
        }
        if (activeEnemyIndex >= 0) {
            Enemy target = enemies.get(activeEnemyIndex);
            if (pendingSkill == null) {
                // Standard Attack calculation
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

                if (activeEnemyIndex < enemySprites.size) {
                    enemySprites.get(activeEnemyIndex).setState(
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
                if (activeEnemyIndex < enemySprites.size) {
                    popupX = enemySprites.get(activeEnemyIndex).getX() + 32f;
                    popupY = enemySprites.get(activeEnemyIndex).getY() + 64f;
                }
                addDamagePopup(popupX, popupY, (int)totalDamage, totalDamage > 30);
                game.addScore((int)totalDamage);

                if (!target.isAlive()) {
                    int killBonus = 50;
                    game.addScore(killBonus);
                    game.addMoney(3);  // +3 gold per kill
                    addLog(target.getNama() + " died! +" + killBonus + " score  +3 gold");
                }
            } else {
                // Skill Attack calculation
                if (game.inventory.consumeSkillCharge(pendingSkill)) {
                    double oldArmor = target.getArmor();
                    switch (pendingSkill.getType()) {
                        case DAMAGE: {
                            double skillDamage   = pendingSkill.getValue();
                            double damageToArmor = Math.min(target.getArmor(), skillDamage * 0.3);
                            double damageToHealth = skillDamage - damageToArmor;
                            if (damageToArmor  > 0) target.takeDamage(damageToArmor);
                            if (damageToHealth > 0) target.takeDamage(damageToHealth);
                            addLog(player.getNama() + " uses " + pendingSkill.getName() +
                                " on " + target.getNama() + "!");
                            addLog("  Deals " + (int)skillDamage + " damage! (Armor: -" +
                                (int)damageToArmor + ", HP: -" + (int)damageToHealth + ")");
                            game.addScore((int)skillDamage * 2);
                            float popupX = 600f, popupY = 350f;
                            if (activeEnemyIndex < enemySprites.size) {
                                popupX = enemySprites.get(activeEnemyIndex).getX() + 32f;
                                popupY = enemySprites.get(activeEnemyIndex).getY() + 64f;
                            }
                            addDamagePopup(popupX, popupY, (int)skillDamage, true);
                            break;
                        }
                        case DEBUFF_ARMOR: {
                            double armorReduce = pendingSkill.getValue();
                            target.debuffArmor(armorReduce);
                            addLog(player.getNama() + " uses " + pendingSkill.getName() +
                                " on " + target.getNama() + "!");
                            addLog("  Armor reduced by " + (int)armorReduce +
                                "! (" + (int)oldArmor + " -> " + (int)target.getArmor() + ")");
                            float debuffX = 600f, debuffY = 350f;
                            if (activeEnemyIndex < enemySprites.size) {
                                debuffX = enemySprites.get(activeEnemyIndex).getX() + 32f;
                                debuffY = enemySprites.get(activeEnemyIndex).getY() + 64f;
                            }
                            addDamagePopup(debuffX, debuffY, (int)armorReduce, false);
                            break;
                        }
                        default:
                            addLog("Used " + pendingSkill.getName() + " on " + target.getNama() + "!");
                            break;
                    }
                    
                    if (activeEnemyIndex < enemySprites.size) {
                        enemySprites.get(activeEnemyIndex).setState(
                            target.isAlive() ? EnemySprite.EnemyState.HURT : EnemySprite.EnemyState.DEAD);
                    }

                    if (!target.isAlive()) {
                        int killBonus = 50;
                        game.addScore(killBonus);
                        game.addMoney(3);  // +3 gold per kill
                        addLog(target.getNama() + " died! +" + killBonus + " score  +3 gold");
                    }
                } else {
                    addLog("No skill charge available for " + pendingSkill.getName() + "!");
                }
                pendingSkill = null;
            }
        } else {
            // Self Target Skill calculation
            if (pendingSkill != null) {
                if (game.inventory.consumeSkillCharge(pendingSkill)) {
                    switch (pendingSkill.getType()) {
                        case HEAL:
                            player.heal(pendingSkill.getValue());
                            addLog(player.getNama() + " heals for " + (int)pendingSkill.getValue() + " HP!");
                            addDamagePopup(130f, 360f, (int)pendingSkill.getValue(), false); // popup green numbers in future, text for now
                            break;
                        case BUFF_ARMOR:
                            player.buffArmor(pendingSkill.getValue());
                            addLog(player.getNama() + " gains +" + (int)pendingSkill.getValue() + " armor!");
                            addDamagePopup(130f, 360f, (int)pendingSkill.getValue(), false);
                            break;
                        default:
                            addLog("Used " + pendingSkill.getName() + " on self!");
                            break;
                    }
                } else {
                    addLog("No skill charge available!");
                }
                pendingSkill = null;
            }
        }

        updateEnemyTable();
        updateUI();

        if (game.inventory.isJustLeveledUp()) {
            addLog("LEVEL UP! Now level " + game.inventory.getLevelCount() + "!");
            addLog("HP +10 | Armor +3 | Attack +2");
            game.inventory.clearLevelUpFlag();
            player.heal(player.getMaxHealth() * 0.3);
            addLog("You recovered 30% HP from leveling up!");
        }
    }

    private void executeEnemyActionHit() {
        Enemy enemy = enemies.get(activeEnemyIndex);
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
            addLog("  Your armor: " + (int)oldArmor + " -> " + (int)player.getArmor());
        } else if (damageToArmor > 0) {
            logMsg += " (Armor -" + (int)damageToArmor + ")";
            addLog("  Your armor: " + (int)oldArmor + " -> " + (int)player.getArmor());
        } else {
            logMsg += " (HP -" + (int)damageToHealth + ")";
        }
        addLog(logMsg);

        addDamagePopup(130f, 360f, (int)totalDamage, false);
        updateUI();
    }

    private void updateBattleStateMachine(float delta) {
        // Blokir state machine saat overlay sedang ditampilkan
        if (turnOverlay != null && turnOverlay.isShowing()) return;
        if (currentState == BattleState.PLAYER_TURN) return;

        stateTimer += delta;

        switch (currentState) {
            case PLAYER_ACTION: {
                // Phase 0: Anticipation (0.2 sec)
                if (activeActionPhase == 0) {
                    if (stateTimer >= 0.2f) {
                        activeActionPhase = 1;
                        stateTimer = 0f;
                        executePlayerActionHit();
                    }
                }
                // Phase 1: Hit delay / damage reaction (0.2 sec)
                else if (activeActionPhase == 1) {
                    if (showSkillEffect) {
                        skillEffectTimer += delta;
                    }
                    if (stateTimer >= 0.2f) {
                        activeActionPhase = 2;
                        stateTimer = 0f;
                        showSkillEffect = false;
                    }
                }
                // Phase 2: Follow-up / Check transitions (0.4 sec)
                else if (activeActionPhase == 2) {
                    if (stateTimer >= 0.4f) {
                        boolean allDead = enemies.stream().noneMatch(Enemy::isAlive);
                        if (allDead) {
                            currentState = BattleState.WAVE_CLEAR;
                            stateTimer = 0f;
                            onWaveComplete();
                        } else {
                            // Announce ENEMY TURN before enemies act
                            currentState = BattleState.ENEMY_TURN;
                            stateTimer = 0f;
                            enemyTurnListIndex = 0;
                            addLog("=== ENEMY TURN ===");
                            BitmapFont big = new BitmapFont(); big.getData().setScale(2.4f);
                            BitmapFont sm  = new BitmapFont(); sm.getData().setScale(1.1f);
                            turnOverlay.show(stage, big, sm,
                                "ENEMY TURN", "Musuh-musuh menyerang!",
                                new Color(1f, 0.25f, 0.2f, 1f), 0.85f,
                                () -> { big.dispose(); sm.dispose(); });
                        }
                    }
                }
                break;
            }
            case ENEMY_TURN: {
                // Reset lunges from previous actions
                for (EnemySprite sprite : enemySprites) {
                    sprite.lungeOffset = 0f;
                }
                
                // Sequential processing of enemy attacks
                if (enemyTurnListIndex < enemies.size()) {
                    Enemy enemy = enemies.get(enemyTurnListIndex);
                    if (enemy.isAlive() && player.isAlive()) {
                        activeEnemyIndex = enemyTurnListIndex;
                        activeActionPhase = 0;
                        stateTimer = 0f;
                        currentState = BattleState.ENEMY_ACTION;
                        if (activeEnemyIndex < enemySprites.size) {
                            enemySprites.get(activeEnemyIndex).setState(EnemySprite.EnemyState.ATTACK);
                            // Visual lunge horizontally
                            enemySprites.get(activeEnemyIndex).lungeOffset = -35f;
                        }
                    } else {
                        // Skip dead enemy
                        enemyTurnListIndex++;
                    }
                } else {
                    // All enemy turns processed. Transition back to Player Turn!
                currentState = BattleState.PLAYER_TURN;
                isPlayerTurn = true;
                updateUI();
                addLog("=== YOUR TURN ===");
                addLog("Click on an enemy to attack, or use skill.");
                // Announce
                BitmapFont big = new BitmapFont(); big.getData().setScale(2.4f);
                BitmapFont sm  = new BitmapFont(); sm.getData().setScale(1.1f);
                turnOverlay.show(stage, big, sm,
                    "YOUR TURN", "Pilih aksi: Serang, Skill, atau Kembali",
                    new Color(0.95f, 0.85f, 0.2f, 1f), 0.85f,
                    () -> { big.dispose(); sm.dispose(); });
                }
                break;
            }
            case ENEMY_ACTION: {
                // Phase 0: Anticipation (0.2 sec)
                if (activeActionPhase == 0) {
                    if (stateTimer >= 0.2f) {
                        activeActionPhase = 1;
                        stateTimer = 0f;
                        executeEnemyActionHit();
                    }
                }
                // Phase 1: Hit delay / damage reaction (0.2 sec)
                else if (activeActionPhase == 1) {
                    if (stateTimer >= 0.2f) {
                        activeActionPhase = 2;
                        stateTimer = 0f;
                        if (activeEnemyIndex < enemySprites.size) {
                            enemySprites.get(activeEnemyIndex).setState(EnemySprite.EnemyState.IDLE);
                            enemySprites.get(activeEnemyIndex).lungeOffset = 0f;
                        }
                    }
                }
                // Phase 2: Check transition / death (0.4f sec)
                else if (activeActionPhase == 2) {
                    if (stateTimer >= 0.4f) {
                        if (!player.isAlive()) {
                            currentState = BattleState.BATTLE_END;
                            stateTimer = 0f;
                            addLog("=== GAME OVER ===");
                            addLog("You were defeated at Wave " + currentWave + "!");
                            addLog("Final Score: " + game.totalScore);
                            game.addMoney(5);  // +5 gold consolation
                            addLog("+5 money (consolation)");
                            showEndGameOverlay(false);
                        } else {
                            // Move to next enemy's turn
                            enemyTurnListIndex++;
                            currentState = BattleState.ENEMY_TURN;
                            stateTimer = 0f;
                        }
                    }
                }
                break;
            }
            case WAVE_CLEAR:
                // Existing onWaveComplete will handle continuing exploring or victory screen.
                break;
            case BATTLE_END:
                break;
        }
    }

    private void onWaveComplete() {
        int waveBonus = 100 * currentWave;
        int moneyWave = 15 * currentWave;   // +15 gold × wave number
        game.addScore(waveBonus);
        game.addMoney(moneyWave);
        addLog("=== WAVE " + currentWave + " COMPLETE! +" + waveBonus + " score  +" + moneyWave + " gold ===");
        player.heal(player.getMaxHealth() * 0.5);
        addLog("You recovered " + (int)(player.getMaxHealth() * 0.5) + " HP after wave!");

        if (currentWave >= maxWaves) {
            int levelBonus = 500;
            int moneyVictory = 30 * maxWaves;  // +30 gold × maxWaves bonus kemenangan
            game.addScore(levelBonus);
            game.addMoney(moneyVictory);
            addLog("=== VICTORY! ===");
            addLog("Level " + currentLevel + " completed! +" + levelBonus + " score  +" + moneyVictory + " gold bonus!");
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
        inner.add(title).padBottom(8f).row();

        // Money/reward summary
        Label moneyLabel = new Label(
            "Gold: " + game.inventory.getMoneyCount() + "  |  Score: " + game.totalScore,
            new Label.LabelStyle(fontSmall, new Color(1f, 0.85f, 0.3f, 1f)));
        inner.add(moneyLabel).padBottom(20f).row();

        // Buttons row
        Table btnRow = new Table();

        TextButton shopBtn = createButton("SHOP", new Color(0.15f, 0.45f, 0.75f, 1f));
        shopBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                endGameOverlay.setVisible(false);
                game.setScreen(new ShopScreen(game, () -> {
                    endGameOverlay.setVisible(true);
                    game.setScreen(BattleScreen.this);
                }));
            }
        });
        btnRow.add(shopBtn).width(160f).height(50f).padRight(12f);

        TextButton continueBtn = createButton("LANJUT ▶", new Color(0.2f, 0.6f, 0.2f, 1f));
        continueBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (parentScreen != null) {
                    parentScreen.currentWave++;
                    parentScreen.encounterX   = parentScreen.worldX + 600f;
                    parentScreen.isTransitioning = false;
                    game.setScreen(parentScreen);
                } else {
                    game.setScreen(new MenuScreen(game));
                }
            }
        });
        btnRow.add(continueBtn).width(200f).height(50f);

        inner.add(btnRow);
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

    private TextureRegion getSkillIcon(Skill.SkillType type) {
        if (guiIconSheet == null) return null;
        return switch (type) {
            case DAMAGE      -> iconAttack;
            case HEAL        -> new TextureRegion(guiIconSheet, 0 * 32, 4 * 32, 32, 32);
            case BUFF_ARMOR  -> new TextureRegion(guiIconSheet, 1 * 32, 2 * 32, 32, 32);
            case DEBUFF_ARMOR-> new TextureRegion(guiIconSheet, 3 * 32, 5 * 32, 32, 32);
        };
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
                    new Color(0.2f, 0.3f, 0.6f, 1f),
                    getSkillIcon(skill.getType()));
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
        if (currentState != BattleState.PLAYER_TURN) return;
        if (pendingSkill == null) return;

        currentState = BattleState.PLAYER_ACTION;
        activeEnemyIndex = -1; // -1 indicates self target
        activeActionPhase = 0;
        stateTimer = 0f;
    }

    private void useSkillOnEnemy(int enemyIndex) {
        if (currentState != BattleState.PLAYER_TURN) return;
        if (pendingSkill == null) return;
        Enemy target = enemies.get(enemyIndex);
        if (!target.isAlive()) { addLog("Target is already dead!"); pendingSkill = null; return; }

        currentState = BattleState.PLAYER_ACTION;
        activeEnemyIndex = enemyIndex;
        activeActionPhase = 0; // Anticipation
        stateTimer = 0f;
    }

    private void addLog(String message) {
        String formatted = message;
        
        // 1. Format headers / special sections
        if (formatted.contains("===")) {
            if (formatted.contains("BATTLE START") || formatted.contains("YOUR TURN")) {
                formatted = "[#fbbf24]" + formatted + "[]"; // Bright Gold
            } else if (formatted.contains("ENEMY TURN")) {
                formatted = "[#f87171]" + formatted + "[]"; // Light Red
            } else if (formatted.contains("COMPLETE") || formatted.contains("VICTORY")) {
                formatted = "[#34d399]" + formatted + "[]"; // Emerald Green
            } else if (formatted.contains("GAME OVER")) {
                formatted = "[#ef4444]" + formatted + "[]"; // Deep Red
            } else {
                formatted = "[#eab308]" + formatted + "[]"; // Yellow
            }
        } else {
            // 2. Format regular messages based on keywords
            String lower = formatted.toLowerCase();
            
            if (lower.contains("level up")) {
                formatted = "[#ec4899]" + formatted + "[]"; // Pink / level up
            } else if (lower.contains("died") || lower.contains("defeat") || lower.contains("dead")) {
                formatted = "[#f43f5e]" + formatted + "[]"; // Crimson Red
            } else if (lower.contains("heal") || lower.contains("recover")) {
                formatted = "[#10b981]" + formatted + "[]"; // Emerald Green
            } else if (lower.contains("gain") || lower.contains("armor")) {
                formatted = "[#06b6d4]" + formatted + "[]"; // Sky Blue / Armor
            } else if (lower.contains("attack") || lower.contains("deal") || lower.contains("damage")) {
                // If it is enemy attack
                if (lower.contains("attacks player") || lower.contains("you for")) {
                    formatted = "[#f87171]" + formatted + "[]"; // Light Red for enemy attacks
                } else {
                    formatted = "[#f8a5c2]" + formatted + "[]"; // Soft Pinkish/White for player actions
                }
            } else if (lower.contains("your turn")) {
                formatted = "[#eab308]" + formatted + "[]"; // Warm Yellow
            } else if (lower.contains("cancelled") || lower.contains("no skill") || lower.contains("not enough")) {
                formatted = "[#fbbf24]" + formatted + "[]"; // Amber Warning
            }
        }
        
        // Keep difficulty formatting as a fallback / override
        if (formatted.contains("Easy")) {
            formatted = formatted.replace("Easy", "[#22c55e]Easy[]");
        } else if (formatted.contains("Normal")) {
            formatted = formatted.replace("Normal", "[#22c55e]Normal[]");
        } else if (formatted.contains("Hard")) {
            formatted = formatted.replace("Hard", "[#22c55e]Hard[]");
        }
        
        if (formatted.contains("EASY")) {
            formatted = formatted.replace("EASY", "[#22c55e]EASY[]");
        } else if (formatted.contains("NORMAL")) {
            formatted = formatted.replace("NORMAL", "[#22c55e]NORMAL[]");
        } else if (formatted.contains("HARD")) {
            formatted = formatted.replace("HARD", "[#22c55e]HARD[]");
        }

        logHistory.add(formatted);
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

        // Update battle pacing state machine
        updateBattleStateMachine(delta);

        for (DamagePopup popup : damagePopups) popup.update(delta);
        for (int i = damagePopups.size - 1; i >= 0; i--)
            if (!damagePopups.get(i).isAlive()) damagePopups.removeIndex(i);

        for (EnemySprite sprite : enemySprites) sprite.update(delta);

        batch.begin();
        if (currentBackground != null) batch.draw(currentBackground, 0, 0, W, H);

        float playerX = 65f;
        // Lunge forward horizontally during anticipation phase of standard or skill attack
        if (currentState == BattleState.PLAYER_ACTION && activeActionPhase == 0) {
            playerX += 35f;
        }

        playerAnimTime += delta;
        // Draw player idle animation
        if (playerIdleAnim != null && player.isAlive()) {
            TextureRegion currentFrame = playerIdleAnim.getKeyFrame(playerAnimTime);
            // Perbesar ukuran sprite player sebesar 25% (240x140 px) agar lebih gagah dan proporsional dengan musuh
            float drawWidth = (currentFrame.getRegionWidth() / 4f) * 1.25f;
            float drawHeight = (currentFrame.getRegionHeight() / 4f) * 1.25f;
            // Di-ground-kan pada y = 130f agar selaras dengan ground line
            batch.draw(currentFrame, playerX, 130f, drawWidth, drawHeight);
        } else if (playerTex != null && player.isAlive()) {
            batch.draw(playerTex, playerX + 35f, 130f, 80f, 80f);
        }

        // Enemy sprites
        for (EnemySprite sprite : enemySprites) sprite.draw(batch);

        // Damage popups
        for (DamagePopup popup : damagePopups) popup.draw(batch, fontMedium);

        // ── SKILL VISUAL EFFECTS OVERLAY ──
        if (showSkillEffect && skillEffectType != null) {
            if (skillEffectType == Skill.SkillType.DAMAGE || skillEffectType == Skill.SkillType.DEBUFF_ARMOR) {
                if (activeEnemyIndex >= 0 && activeEnemyIndex < enemySprites.size) {
                    EnemySprite sprite = enemySprites.get(activeEnemyIndex);
                    float ex = sprite.getX() + sprite.lungeOffset;
                    float ey = sprite.getY() + 40f;
                    TextureRegion fxRegion = null;
                    Color tintColor = Color.WHITE;
                    if (skillEffectType == Skill.SkillType.DAMAGE) {
                        fxRegion = new TextureRegion(guiIconSheet, 0 * 32, 0 * 32, 32, 32); // sword / slash
                        tintColor = new Color(1f, 0.9f, 0.2f, 0.85f); // glowing gold
                    } else {
                        fxRegion = new TextureRegion(guiIconSheet, 3 * 32, 5 * 32, 32, 32); // skull curse
                        tintColor = new Color(0.8f, 0.2f, 0.9f, 0.85f); // glowing purple
                    }
                    if (fxRegion != null) {
                        float scale = 2.0f + 0.8f * com.badlogic.gdx.math.MathUtils.sin(skillEffectTimer * 25f);
                        float w = 32f * scale;
                        float h = 32f * scale;
                        batch.setColor(tintColor);
                        batch.draw(fxRegion, ex - w / 2f + 32f, ey - h / 2f + 16f, w, h);
                        batch.setColor(Color.WHITE);
                    }
                }
            } else if (skillEffectType == Skill.SkillType.HEAL || skillEffectType == Skill.SkillType.BUFF_ARMOR) {
                float px = playerX + 60f;
                float py = 130f + 60f;
                TextureRegion fxRegion = null;
                Color tintColor = Color.WHITE;
                if (skillEffectType == Skill.SkillType.HEAL) {
                    fxRegion = new TextureRegion(guiIconSheet, 0 * 32, 4 * 32, 32, 32); // heart
                    tintColor = new Color(0.1f, 0.9f, 0.3f, 0.85f); // glowing green
                } else {
                    fxRegion = new TextureRegion(guiIconSheet, 1 * 32, 2 * 32, 32, 32); // shield
                    tintColor = new Color(0.2f, 0.7f, 1f, 0.85f); // glowing cyan
                }
                if (fxRegion != null) {
                    float scale = 2.0f + 0.8f * com.badlogic.gdx.math.MathUtils.sin(skillEffectTimer * 25f);
                    float w = 32f * scale;
                    float h = 32f * scale;
                    batch.setColor(tintColor);
                    batch.draw(fxRegion, px - w / 2f, py - h / 2f, w, h);
                    batch.setColor(Color.WHITE);
                }
            }
        }

        batch.end();

        if (shapeRenderer != null) {
            shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);

            // ── 1. Enemy status pointers ──────────────────────────────────────
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(new Color(0.93f, 0.84f, 0.68f, 1f));
            for (int i = 0; i < enemies.size(); i++) {
                Enemy enemy = enemies.get(i);
                if (!enemy.isAlive()) continue;
                if (i < enemySprites.size) {
                    EnemySprite sprite = enemySprites.get(i);
                    float x = sprite.getX() + sprite.lungeOffset;
                    float y = sprite.getY() + 115f;
                    shapeRenderer.triangle(
                        x - 10f, y + 1f,
                        x + 10f, y + 1f,
                        x, y - 10f);
                }
            }
            shapeRenderer.end();

            // ── 2. Interactive button backgrounds (ala MenuScreen) ────────────
            drawAllBattleBtnBg();
        }

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
        if (uiWavePanel       != null) uiWavePanel.dispose();
        if (iconShield        != null) iconShield.dispose();
        if (iconSword         != null) iconSword.dispose();
        if (guiIconSheet      != null) guiIconSheet.dispose();
        if (btnAttackTex      != null) btnAttackTex.dispose();
        if (btnSkillTex       != null) btnSkillTex.dispose();
        if (btnFleeTex        != null) btnFleeTex.dispose();
        if (playerIdleSheet   != null) playerIdleSheet.dispose();
        if (turnOverlay       != null) turnOverlay.dispose();
        if (customLogBgTex    != null) customLogBgTex.dispose();
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
}