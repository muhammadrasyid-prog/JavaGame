package io.github.jekjek.Screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.jekjek.Main;

public class MenuScreen implements Screen {

    private static final float W = 800f, H = 480f;
    private static final float BTN_W = 270f, BTN_H = 52f, BTN_GAP = 14f;

    private final Main game;
    private Stage stage;
    private FitViewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shape;

    private BitmapFont fontTitle, fontSub, fontBtn, fontHint, fontBody;

    private final float[] starX = new float[90];
    private final float[] starY = new float[90];
    private final float[] starS = new float[90];
    private final float[] starR = new float[90];
    private float time = 0f;

    private Table overlayTable;
    private Label overlayTitle, overlayBody;
    private String selectedDifficulty = "Normal";  // Default: Normal
    private TextButton btnEasy, btnNormal, btnHard;

    public MenuScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch    = new SpriteBatch();
        shape    = new ShapeRenderer();
        viewport = new FitViewport(W, H);
        stage    = new Stage(viewport, batch);
        Gdx.input.setInputProcessor(stage);
        initStars();
        buildFonts();
        buildUI();
    }

    private void initStars() {
        for (int i = 0; i < starX.length; i++) {
            starX[i] = MathUtils.random(W);
            starY[i] = MathUtils.random(H);
            starS[i] = MathUtils.random(4f, 18f);
            starR[i] = MathUtils.random(0.7f, 2.4f);
        }
    }

    private void buildFonts() {
        fontTitle = new BitmapFont(); fontTitle.getData().setScale(3.6f);
        fontTitle.setColor(1f, 0.88f, 0.25f, 1f);
        fontSub = new BitmapFont(); fontSub.getData().setScale(1.35f);
        fontSub.setColor(0.75f, 0.93f, 1f, 1f);
        fontBtn = new BitmapFont(); fontBtn.getData().setScale(1.65f);
        fontBtn.setColor(Color.WHITE);
        fontHint = new BitmapFont(); fontHint.getData().setScale(1.05f);
        fontHint.setColor(0.55f, 0.55f, 0.65f, 1f);
        fontBody = new BitmapFont(); fontBody.getData().setScale(1.2f);
        fontBody.setColor(0.9f, 0.95f, 1f, 1f);
    }

    private void buildUI() {
        stage.clear();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(20f);
        stage.addActor(root);

        Label title = new Label("JOGJA EXPERIENCE", new Label.LabelStyle(fontTitle, fontTitle.getColor()));
        title.setAlignment(Align.center);
        root.add(title).expandX().padBottom(2f).row();

        Label sub = new Label("Turn-Based Adventure", new Label.LabelStyle(fontSub, fontSub.getColor()));
        sub.setAlignment(Align.center);
        root.add(sub).expandX().padBottom(14f).row();

        // Info score & level dari inventory nyata
        String infoText = "Score: " + game.totalScore
            + "   |   Lv " + game.inventory.getLevelCount()
            + "   |   XP " + game.inventory.getXpCount()
            + " / " + game.inventory.xpThresholdForCurrentLevel();
        Label infoLabel = new Label(infoText, new Label.LabelStyle(fontHint, new Color(0.6f, 1f, 0.7f, 1f)));
        infoLabel.setAlignment(Align.center);
        root.add(infoLabel).expandX().padBottom(22f).row();

        // Difficulty selector section
        root.add(new Label("Select Difficulty:", new Label.LabelStyle(fontSub, Color.WHITE)))
            .padTop(10f).padBottom(5f).row();

        Table difficultyTable = new Table();

// Warna button sesuai difficulty
        btnEasy = createDifficultyButton("EASY");
        btnNormal = createDifficultyButton("NORMAL");
        btnHard = createDifficultyButton("HARD");

// Set default Normal sebagai terpilih
        btnNormal.getLabel().setColor(0.8f, 0.8f, 0.3f, 1f);

        difficultyTable.add(btnEasy).width(100f).height(40f).pad(5f);
        difficultyTable.add(btnNormal).width(100f).height(40f).pad(5f);
        difficultyTable.add(btnHard).width(100f).height(40f).pad(5f);

        root.add(difficultyTable).padBottom(15f).row();

// Tombol Start Battle (pindahkan/ubah yang sudah ada)
// Hapus atau comment tombol Start Battle yang lama, lalu tambahkan yang baru:
        root.add(makeBtn("START BATTLE", this::startBattleWithDifficulty))
            .size(BTN_W, BTN_H).padBottom(BTN_GAP).row();

        root.add(makeBtn("Inventory",    this::onInventory)).size(BTN_W, BTN_H).padBottom(BTN_GAP).row();
        root.add(makeBtn("Credits",      this::onCredits)).size(BTN_W, BTN_H).padBottom(BTN_GAP).row();
        root.add(makeBtn("Exit",         () -> Gdx.app.exit())).size(BTN_W, BTN_H).row();

        Label hint = new Label("v1.0  |  Team Jekjek  |  libGDX 1.14",
            new Label.LabelStyle(fontHint, fontHint.getColor()));
        hint.setAlignment(Align.center);
        root.add(hint).expandX().padTop(20f);

        buildOverlay();

        title.getColor().a = 0f;
        title.addAction(Actions.sequence(Actions.delay(0.1f), Actions.fadeIn(0.4f)));
        sub.getColor().a = 0f;
        sub.addAction(Actions.sequence(Actions.delay(0.25f), Actions.fadeIn(0.4f)));
    }

    private TextButton makeBtn(String label, Runnable action) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = fontBtn;
        s.fontColor     = Color.WHITE;
        s.overFontColor = new Color(1f, 0.95f, 0.4f, 1f);
        s.downFontColor = new Color(0.55f, 0.55f, 0.55f, 1f);
        TextButton btn = new TextButton(label, s);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { action.run(); }
        });
        btn.addListener(new InputListener() {
            @Override public void enter(InputEvent e, float x, float y, int ptr, Actor from) {
                btn.clearActions(); btn.addAction(Actions.scaleTo(1.04f, 1.04f, 0.08f));
            }
            @Override public void exit(InputEvent e, float x, float y, int ptr, Actor to) {
                btn.clearActions(); btn.addAction(Actions.scaleTo(1f, 1f, 0.08f));
            }
        });
        return btn;
    }

    private TextButton createDifficultyButton(String text) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = fontBtn;
        style.fontColor = Color.WHITE;

        TextButton button = new TextButton(text, style);

        // Special handling untuk difficulty buttons
        if (text.equals("EASY")) {
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectedDifficulty = "Easy";
                    resetDifficultyButtons();
                    button.getLabel().setColor(0.8f, 0.8f, 0.3f, 1f);
                    addLog("Difficulty set to EASY");
                }
            });
        } else if (text.equals("NORMAL")) {
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectedDifficulty = "Normal";
                    resetDifficultyButtons();
                    button.getLabel().setColor(0.8f, 0.8f, 0.3f, 1f);
                    addLog("Difficulty set to NORMAL");
                }
            });
        } else if (text.equals("HARD")) {
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectedDifficulty = "Hard";
                    resetDifficultyButtons();
                    button.getLabel().setColor(0.8f, 0.8f, 0.3f, 1f);
                    addLog("Difficulty set to HARD");
                }
            });
        }
        // Removing the START BATTLE condition from here as it's now a makeBtn
        return button;
    }

    private void resetDifficultyButtons() {
        btnEasy.getLabel().setColor(Color.WHITE);
        btnNormal.getLabel().setColor(Color.WHITE);
        btnHard.getLabel().setColor(Color.WHITE);
    }

    private void addLog(String message) {
        System.out.println("[MENU] " + message);
        // Optional: tampilkan di overlay
    }

    private void startBattleWithDifficulty() {
        stage.addAction(Actions.sequence(
            Actions.fadeOut(0.3f),
            Actions.run(() -> game.setScreen(new GameScreen(game, selectedDifficulty, 1, 100f)))
        ));
    }

    private void buildOverlay() {
        overlayTable = new Table();
        overlayTable.setFillParent(true);
        overlayTable.setVisible(false);
        stage.addActor(overlayTable);

        Table panel = new Table();
        panel.setBackground(new BaseDrawable() {
            @Override public void draw(Batch b, float x, float y, float w, float h) {
                b.end();
                Gdx.gl.glEnable(GL20.GL_BLEND);
                shape.setProjectionMatrix(b.getProjectionMatrix());
                shape.begin(ShapeRenderer.ShapeType.Filled);
                shape.setColor(0.04f, 0.07f, 0.18f, 0.97f);
                shape.rect(x, y, w, h);
                shape.end();
                shape.begin(ShapeRenderer.ShapeType.Line);
                shape.setColor(0.3f, 0.55f, 0.95f, 1f);
                shape.rect(x+1, y+1, w-2, h-2);
                shape.end();
                b.begin();
            }
        });
        panel.pad(28f, 36f, 24f, 36f);

        overlayTitle = new Label("", new Label.LabelStyle(fontSub, new Color(1f, 0.88f, 0.25f, 1f)));
        overlayTitle.setAlignment(Align.center);
        panel.add(overlayTitle).expandX().padBottom(12f).row();

        overlayBody = new Label("", new Label.LabelStyle(fontBody, Color.WHITE));
        overlayBody.setAlignment(Align.center);
        overlayBody.setWrap(true);
        panel.add(overlayBody).width(430f).expandX().padBottom(20f).row();

        TextButton.TextButtonStyle cs = new TextButton.TextButtonStyle();
        cs.font = fontBtn;
        cs.fontColor = new Color(1f, 0.4f, 0.4f, 1f);
        cs.overFontColor = Color.RED;
        TextButton closeBtn = new TextButton("[ Tutup ]", cs);
        closeBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { hideOverlay(); }
        });
        panel.add(closeBtn);
        overlayTable.add(panel).width(500f).minHeight(240f);
    }

    private void showOverlay(String title, String body) {
        overlayTitle.setText(title);
        overlayBody.setText(body);
        overlayTable.setVisible(true);
        overlayTable.getColor().a = 0f;
        overlayTable.addAction(Actions.fadeIn(0.18f));
    }

    private void hideOverlay() {
        overlayTable.addAction(Actions.sequence(
            Actions.fadeOut(0.14f),
            Actions.run(() -> overlayTable.setVisible(false))
        ));
    }

    // ── Aksi tombol ───────────────────────────────────────────────────────────
    private void onStart() {
        startBattleWithDifficulty();
    }
    


    private void onInventory() {
        String body = String.format(
            "Nama       : %s\n" +
                "Level      : %d / %d\n" +
                "XP         : %d / %d\n" +
                "Total XP   : %d\n" +
                "Score      : %d\n" +
                "Money      : %d\n\n" +
                "HP         : %.0f\n" +
                "Armor      : %.0f\n" +
                "Attack     : %.0f\n\n" +
                "Skill Point: %d",
            game.inventory.getProfileName(),
            game.inventory.getLevelCount(), game.inventory.MAX_LEVEL,
            game.inventory.getXpCount(), game.inventory.xpThresholdForCurrentLevel(),
            game.inventory.getTotalXp(),
            game.totalScore,
            game.inventory.getMoneyCount(),
            game.inventory.getTotalHealth(),
            game.inventory.getTotalArmor(),
            game.inventory.getTotalAttack(),
            game.inventory.getSkillPoint()
        );
        showOverlay("Inventory — " + game.inventory.getProfileName(), body);
    }

    private void onCredits() {
        showOverlay("Credits",
            "JOGJA EXPERIENCE\n\n" +
                "Dibuat oleh:\n" +
                "Haidar  •  Salwa  •  Rasya\n" +
                "Ariq  •  Rasyid\n\n" +
                "Dibuat dengan libGDX 1.14 | Java 21");
    }

    // =========================================================================
    //  RENDER
    // =========================================================================
    @Override
    public void render(float delta) {
        time += delta;
        ScreenUtils.clear(0.03f, 0.04f, 0.11f, 1f);
        viewport.apply();
        shape.setProjectionMatrix(stage.getCamera().combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < starX.length; i++) {
            float tw = 0.5f + 0.5f * MathUtils.sin(time * starS[i] * 0.5f + i);
            shape.setColor(1f, 1f, 1f, 0.2f + 0.7f * tw);
            shape.circle(starX[i], starY[i], starR[i]);
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.07f, 0.05f, 0.15f, 1f);
        shape.rect(0, 0, W, H * 0.20f);
        drawBuilding(30,0,28,72); drawBuilding(68,0,18,92); drawBuilding(96,0,38,56);
        drawBuilding(144,0,22,108); drawBuilding(176,0,48,62); drawBuilding(234,0,18,84);
        drawBuilding(590,0,42,98); drawBuilding(642,0,28,66);
        drawBuilding(680,0,52,88); drawBuilding(742,0,24,58);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.28f, 0.38f, 0.68f, 0.6f);
        shape.rect(0, H * 0.20f, W, 1.5f);
        shape.end();

        drawAllButtonBg();
        stage.act(delta);
        stage.draw();
    }

    private void drawBuilding(float x, float y, float w, float h) {
        shape.setColor(0.05f, 0.05f, 0.13f, 1f);
        shape.rect(x, y, w, h);
        shape.setColor(0.88f, 0.82f, 0.35f, 0.42f);
        for (float wy = y+8; wy < y+h-6; wy += 13)
            for (float wx = x+4; wx < x+w-4; wx += 8)
                if (MathUtils.randomBoolean(0.40f)) shape.rect(wx, wy, 3, 4);
    }

    private void drawAllButtonBg() {
        for (Actor a : stage.getActors()) drawBtnBgInActor(a);
    }

    private void drawBtnBgInActor(Actor actor) {
        if (!actor.isVisible()) return;
        if (actor instanceof Table t) { for (Actor c : t.getChildren()) drawBtnBgInActor(c); }
        if (!(actor instanceof TextButton btn)) return;
        com.badlogic.gdx.math.Vector2 pos = btn.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(0, 0));
        float bx = pos.x, by = pos.y, bw = btn.getWidth(), bh = btn.getHeight();
        float r, g, b;
        if      (btn.isPressed()) { r=0.07f; g=0.12f; b=0.30f; }
        else if (btn.isOver())    { r=0.20f; g=0.38f; b=0.76f; }
        else                      { r=0.11f; g=0.19f; b=0.42f; }
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(r, g, b, btn.isOver() ? 0.97f : 0.88f);
        drawRoundRect(bx, by, bw, bh, 10f);
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(btn.isOver()?0.55f:0.27f, btn.isOver()?0.75f:0.42f, 1f, btn.isOver()?1f:0.55f);
        drawRoundRectLine(bx, by, bw, bh, 10f);
        shape.end();
    }

    private void drawRoundRect(float x, float y, float w, float h, float r) {
        r = Math.min(r, Math.min(w,h)/2f);
        shape.rect(x+r,y,w-2*r,h); shape.rect(x,y+r,r,h-2*r); shape.rect(x+w-r,y+r,r,h-2*r);
        drawCF(x+r,y+r,r,180,8); drawCF(x+w-r,y+r,r,270,8);
        drawCF(x+w-r,y+h-r,r,0,8); drawCF(x+r,y+h-r,r,90,8);
    }
    private void drawCF(float cx,float cy,float r,float s,int n){
        for(int i=0;i<n;i++){float a1=s+i*90f/n,a2=s+(i+1)*90f/n;
            shape.triangle(cx,cy,cx+r*MathUtils.cosDeg(a1),cy+r*MathUtils.sinDeg(a1),
                cx+r*MathUtils.cosDeg(a2),cy+r*MathUtils.sinDeg(a2));}
    }
    private void drawRoundRectLine(float x,float y,float w,float h,float r){
        r=Math.min(r,Math.min(w,h)/2f);
        shape.line(x+r,y,x+w-r,y); shape.line(x+r,y+h,x+w-r,y+h);
        shape.line(x,y+r,x,y+h-r); shape.line(x+w,y+r,x+w,y+h-r);
        drawCL(x+r,y+r,r,180,8); drawCL(x+w-r,y+r,r,270,8);
        drawCL(x+w-r,y+h-r,r,0,8); drawCL(x+r,y+h-r,r,90,8);
    }
    private void drawCL(float cx,float cy,float r,float s,int n){
        for(int i=0;i<n;i++){float a1=s+i*90f/n,a2=s+(i+1)*90f/n;
            shape.line(cx+r*MathUtils.cosDeg(a1),cy+r*MathUtils.sinDeg(a1),
                cx+r*MathUtils.cosDeg(a2),cy+r*MathUtils.sinDeg(a2));}
    }

    @Override public void resize(int w, int h) { viewport.update(w, h, true); }
    @Override public void dispose() {
        stage.dispose(); batch.dispose(); shape.dispose();
        fontTitle.dispose(); fontSub.dispose(); fontBtn.dispose();
        fontHint.dispose(); fontBody.dispose();
    }
    @Override public void pause() {} @Override public void resume() {} @Override public void hide() {}
}
