package io.github.jekjek.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import io.github.jekjek.Main;
import io.github.jekjek.Skill.Skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ShopScreen — toko untuk isi ulang skill charge + upgrade stats.
 * Bisa diakses dari MenuScreen atau dari wave clear overlay di BattleScreen.
 * @param onBack callback yang dipanggil saat player klik "Kembali"
 */
public class ShopScreen implements Screen {

    private static final float W = 800f, H = 480f;
    private static final int   SKILL_CHARGE_COST = 10;   // gold per charge
    private static final int   HP_COST    = 20;
    private static final int   ARMOR_COST = 20;
    private static final int   ATK_COST   = 25;

    // ── colours ──────────────────────────────────────────────────────────────
    private static final Color COL_GOLD      = new Color(1f, 0.85f, 0.25f, 1f);
    private static final Color COL_SKILL_HAS = new Color(0.15f, 0.75f, 0.35f, 1f);  // hijau (ada charge)
    private static final Color COL_SKILL_EMPTY= new Color(0.6f, 0.18f, 0.18f, 1f);  // merah (charge 0)
    private static final Color COL_STAT      = new Color(0.12f, 0.42f, 0.72f, 1f);  // biru
    private static final Color COL_DISABLED  = new Color(0.3f, 0.3f, 0.3f, 1f);

    // ── core ─────────────────────────────────────────────────────────────────
    private final Main game;
    private final Runnable onBack;
    private SpriteBatch  batch;
    private ShapeRenderer shape;
    private Stage        stage;
    private StretchViewport  viewport;
    private OrthographicCamera camera;

    // ── fonts ─────────────────────────────────────────────────────────────────
    private BitmapFont fontTitle, fontSub, fontBtn, fontSmall;

    // ── UI refs for live update ───────────────────────────────────────────────
    private Label moneyLabel;
    private final Array<Runnable> cardRefreshers = new Array<>();

    // ── toast ─────────────────────────────────────────────────────────────────
    private Label toastLabel;

    // ── background stars (ala MenuScreen) ────────────────────────────────────
    private static final int STAR_COUNT = 80;
    private float[] starX, starY, starSize, starSpeed, starAlpha;

    // ── managed textures ─────────────────────────────────────────────────────
    private Texture guiIconSheet;
    private final Array<Texture> textures = new Array<>();

    // ─────────────────────────────────────────────────────────────────────────

    public ShopScreen(Main game, Runnable onBack) {
        this.game   = game;
        this.onBack = onBack;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Screen lifecycle
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void show() {
        camera   = new OrthographicCamera();
        viewport = new StretchViewport(W, H, camera);
        batch    = new SpriteBatch();
        shape    = new ShapeRenderer();
        stage    = new Stage(viewport, batch);
        Gdx.input.setInputProcessor(stage);

        guiIconSheet = new Texture("GUI/icon.png");
        guiIconSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        textures.add(guiIconSheet);

        buildFonts();
        initStars();
        buildUI();
    }

    @Override
    public void render(float delta) {
        updateStars(delta);

        ScreenUtils.clear(0.07f, 0.07f, 0.14f, 1f);
        viewport.apply();

        // ── draw starfield ──
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < STAR_COUNT; i++) {
            shape.setColor(1f, 1f, 1f, starAlpha[i]);
            shape.circle(starX[i], starY[i], starSize[i]);
        }
        shape.end();

        // ── draw button backgrounds ──
        stage.getBatch().setProjectionMatrix(camera.combined);
        drawAllBtnBg();

        // ── draw stage ──
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { viewport.update(w, h, true); }
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
    @Override public void dispose() {
        stage.dispose(); batch.dispose(); shape.dispose();
        fontTitle.dispose(); fontSub.dispose(); fontBtn.dispose(); fontSmall.dispose();
        for (Texture t : textures) t.dispose();
        textures.clear();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI Construction
    // ═════════════════════════════════════════════════════════════════════════

    private void buildFonts() {
        fontTitle = new BitmapFont();  fontTitle.getData().setScale(2.0f);
        fontSub   = new BitmapFont();  fontSub.getData().setScale(1.15f);
        fontBtn   = new BitmapFont();  fontBtn.getData().setScale(1.0f);
        fontSmall = new BitmapFont();  fontSmall.getData().setScale(0.85f);
    }

    private com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable createPanelBackground() {
        return new com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable() {
            @Override public void draw(com.badlogic.gdx.graphics.g2d.Batch b, float x, float y, float w, float h) {
                b.end();
                Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
                shape.setProjectionMatrix(b.getProjectionMatrix());
                shape.begin(ShapeRenderer.ShapeType.Filled);
                // Glassmorphism dark indigo background
                shape.setColor(0.04f, 0.06f, 0.16f, 0.85f);
                drawRoundRect(x, y, w, h, 12f);
                shape.end();
                shape.begin(ShapeRenderer.ShapeType.Line);
                // Subtle blue border
                shape.setColor(0.2f, 0.4f, 0.78f, 0.6f);
                drawRoundRectLine(x, y, w, h, 12f);
                shape.end();
                b.begin();
            }
        };
    }

    private TextureRegion getSkillIconRegion(Skill.SkillType type) {
        if (guiIconSheet == null) return null;
        return switch (type) {
            case DAMAGE      -> new TextureRegion(guiIconSheet, 0 * 32, 0 * 32, 32, 32);
            case HEAL        -> new TextureRegion(guiIconSheet, 0 * 32, 4 * 32, 32, 32);
            case BUFF_ARMOR  -> new TextureRegion(guiIconSheet, 1 * 32, 2 * 32, 32, 32);
            case DEBUFF_ARMOR-> new TextureRegion(guiIconSheet, 3 * 32, 5 * 32, 32, 32);
        };
    }

    private void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(15f);

        // ── Header ──────────────────────────────────────────────────────────
        Table header = new Table();
        Label shopTitle = new Label("TOKO PETUALANG", new Label.LabelStyle(fontTitle, COL_GOLD));
        moneyLabel = new Label("DOMPET: " + moneyStr(), new Label.LabelStyle(fontSub, COL_GOLD));
        header.add(shopTitle).expandX().left();
        header.add(moneyLabel).right();
        root.add(header).expandX().fillX().padBottom(10f).row();

        // Divider
        root.add(dividerImage()).expandX().fillX().height(1.5f).padBottom(12f).row();

        // ── Two-column Grid Body ─────────────────────────────────────────────
        Table body = new Table();

        // LEFT PANEL: Skill Shop
        Table skillCol = new Table();
        skillCol.setBackground(createPanelBackground());
        skillCol.pad(16f);
        skillCol.top();

        Label skillTitle = new Label("SKILL CHARGE SHOP", new Label.LabelStyle(fontSub, COL_GOLD));
        skillCol.add(skillTitle).left().padBottom(12f).row();

        Table skillGrid = new Table();
        Map<Skill, Integer> skills = game.inventory.getUnlockedSkills();
        List<Skill> skillList = new ArrayList<>(skills.keySet());

        if (skillList.isEmpty()) {
            skillGrid.add(new Label("Tidak ada skill yang terbuka.", new Label.LabelStyle(fontSmall, Color.GRAY))).left();
        } else {
            for (Skill skill : skillList) {
                Table nameWithIcon = new Table();
                TextureRegion iconReg = getSkillIconRegion(skill.getType());
                if (iconReg != null) {
                    Image iconImg = new Image(iconReg);
                    nameWithIcon.add(iconImg).size(18f, 18f).padRight(6f);
                }
                Label nameLbl = new Label(skill.getName(), new Label.LabelStyle(fontBtn, Color.WHITE));
                nameWithIcon.add(nameLbl);

                Label chargeLbl = new Label("", new Label.LabelStyle(fontBtn, Color.WHITE));

                TextButton buyBtn = makeBtn("BELI (10g)", COL_SKILL_HAS, () -> {
                    if (game.inventory.getMoneyCount() >= SKILL_CHARGE_COST) {
                        game.inventory.buySkillCharge(skill, SKILL_CHARGE_COST, 1);
                        refreshAll();
                        showToast("Berhasil membeli charge " + skill.getName() + "!", true);
                    } else {
                        showToast("Uang tidak cukup! (Butuh " + SKILL_CHARGE_COST + " gold)", false);
                    }
                });

                cardRefreshers.add(() -> {
                    int c = game.inventory.getSkillCharge(skill);
                    chargeLbl.setText("Charge: " + c);
                    chargeLbl.setColor(c > 0 ? new Color(0.4f, 1f, 0.4f, 1f) : new Color(1f, 0.3f, 0.3f, 1f));
                    boolean canAfford = game.inventory.getMoneyCount() >= SKILL_CHARGE_COST;
                    buyBtn.setColor(canAfford ? Color.WHITE : new Color(0.5f, 0.5f, 0.5f, 0.8f));
                });

                Table rowTable = new Table();
                rowTable.add(nameWithIcon).width(160f).left();
                rowTable.add(chargeLbl).width(120f).left();
                rowTable.add(buyBtn).width(110f).height(32f).right();
                
                skillGrid.add(rowTable).expandX().fillX().padBottom(10f).row();
            }
        }
        skillCol.add(skillGrid).expand().fill();

        // RIGHT PANEL: Stat Shop
        Table statCol = new Table();
        statCol.setBackground(createPanelBackground());
        statCol.pad(16f);
        statCol.top();

        Label statTitle = new Label("STAT UPGRADE SHOP", new Label.LabelStyle(fontSub, COL_GOLD));
        statCol.add(statTitle).left().padBottom(12f).row();

        Table statGrid = new Table();
        
        // HP Row
        Table hpNameWithIcon = new Table();
        if (guiIconSheet != null) {
            Image hpIcon = new Image(new TextureRegion(guiIconSheet, 0 * 32, 4 * 32, 32, 32)); // heart
            hpNameWithIcon.add(hpIcon).size(18f, 18f).padRight(6f);
        }
        Label hpName = new Label("HP (+10)", new Label.LabelStyle(fontBtn, Color.WHITE));
        hpNameWithIcon.add(hpName);

        Label hpVal  = new Label("", new Label.LabelStyle(fontBtn, COL_GOLD));
        TextButton hpBtn = makeBtn("BELI (20g)", COL_STAT, () -> {
            boolean ok = game.inventory.upgradeHealth(HP_COST, 10);
            if (ok) {
                refreshAll();
                showToast("Upgrade HP Berhasil! (+10 HP)", true);
            } else {
                showToast("Uang tidak cukup! (Butuh " + HP_COST + " gold)", false);
            }
        });
        cardRefreshers.add(() -> {
            hpVal.setText("Stat: " + (int) game.inventory.getTotalHealth());
            boolean canAfford = game.inventory.getMoneyCount() >= HP_COST;
            hpBtn.setColor(canAfford ? Color.WHITE : new Color(0.5f, 0.5f, 0.5f, 0.8f));
        });
        Table hpRow = new Table();
        hpRow.add(hpNameWithIcon).width(110f).left();
        hpRow.add(hpVal).width(100f).left();
        hpRow.add(hpBtn).width(110f).height(32f).right();
        statGrid.add(hpRow).expandX().fillX().padBottom(12f).row();

        // Armor Row
        Table armorNameWithIcon = new Table();
        if (guiIconSheet != null) {
            Image armorIcon = new Image(new TextureRegion(guiIconSheet, 1 * 32, 2 * 32, 32, 32)); // shield
            armorNameWithIcon.add(armorIcon).size(18f, 18f).padRight(6f);
        }
        Label armorName = new Label("Armor (+5)", new Label.LabelStyle(fontBtn, Color.WHITE));
        armorNameWithIcon.add(armorName);

        Label armorVal  = new Label("", new Label.LabelStyle(fontBtn, COL_GOLD));
        TextButton armorBtn = makeBtn("BELI (20g)", COL_STAT, () -> {
            boolean ok = game.inventory.upgradeArmor(ARMOR_COST, 5);
            if (ok) {
                refreshAll();
                showToast("Upgrade Armor Berhasil! (+5 Armor)", true);
            } else {
                showToast("Uang tidak cukup! (Butuh " + ARMOR_COST + " gold)", false);
            }
        });
        cardRefreshers.add(() -> {
            armorVal.setText("Stat: " + (int) game.inventory.getTotalArmor());
            boolean canAfford = game.inventory.getMoneyCount() >= ARMOR_COST;
            armorBtn.setColor(canAfford ? Color.WHITE : new Color(0.5f, 0.5f, 0.5f, 0.8f));
        });
        Table armorRow = new Table();
        armorRow.add(armorNameWithIcon).width(110f).left();
        armorRow.add(armorVal).width(100f).left();
        armorRow.add(armorBtn).width(110f).height(32f).right();
        statGrid.add(armorRow).expandX().fillX().padBottom(12f).row();

        // Attack Row
        Table atkNameWithIcon = new Table();
        if (guiIconSheet != null) {
            Image atkIcon = new Image(new TextureRegion(guiIconSheet, 0 * 32, 0 * 32, 32, 32)); // sword
            atkNameWithIcon.add(atkIcon).size(18f, 18f).padRight(6f);
        }
        Label atkName = new Label("Attack (+2)", new Label.LabelStyle(fontBtn, Color.WHITE));
        atkNameWithIcon.add(atkName);

        Label atkVal  = new Label("", new Label.LabelStyle(fontBtn, COL_GOLD));
        TextButton atkBtn = makeBtn("BELI (25g)", COL_STAT, () -> {
            boolean ok = game.inventory.upgradeAttack(ATK_COST, 2);
            if (ok) {
                refreshAll();
                showToast("Upgrade Attack Berhasil! (+2 Attack)", true);
            } else {
                showToast("Uang tidak cukup! (Butuh " + ATK_COST + " gold)", false);
            }
        });
        cardRefreshers.add(() -> {
            atkVal.setText("Stat: " + (int) game.inventory.getTotalAttack());
            boolean canAfford = game.inventory.getMoneyCount() >= ATK_COST;
            atkBtn.setColor(canAfford ? Color.WHITE : new Color(0.5f, 0.5f, 0.5f, 0.8f));
        });
        Table atkRow = new Table();
        atkRow.add(atkNameWithIcon).width(110f).left();
        atkRow.add(atkVal).width(100f).left();
        atkRow.add(atkBtn).width(110f).height(32f).right();
        statGrid.add(atkRow).expandX().fillX().row();

        statCol.add(statGrid).expand().fill();

        // Add both columns to body
        body.add(skillCol).expand().fill().padRight(12f);
        body.add(statCol).expand().fill();

        root.add(body).expand().fill().row();

        // ── Status Message Area (Middle-Bottom) ──────────────────────────────
        toastLabel = new Label("Pilih barang yang ingin Anda beli / upgrade.", new Label.LabelStyle(fontSmall, Color.WHITE));
        toastLabel.setAlignment(Align.center);
        root.add(toastLabel).expandX().fillX().padTop(10f).padBottom(10f).row();

        // ── Footer: Prominent Back button ────────────────────────────────────
        Table footer = new Table();
        TextButton backBtn = makeBtn("← KEMBALI KE GAME", COL_DISABLED, () -> {
            stage.addAction(Actions.sequence(
                Actions.fadeOut(0.2f),
                Actions.run(onBack)
            ));
        });
        footer.add(backBtn).center().width(240f).height(40f);
        root.add(footer).expandX().fillX();

        stage.addActor(root);
        
        // Refresh values initially
        refreshAll();

        // Fade in
        stage.getRoot().getColor().a = 0f;
        stage.getRoot().addAction(Actions.fadeIn(0.25f));
    }
    // ═════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════════════

    private void refreshAll() {
        moneyLabel.setText(moneyStr());
        for (Runnable r : cardRefreshers) r.run();
    }

    private String moneyStr() {
        return "Gold: " + game.inventory.getMoneyCount();
    }

    private void showToast(String msg, boolean success) {
        toastLabel.setText(msg);
        toastLabel.setColor(success ? new Color(0.3f, 1f, 0.4f, 1f) : new Color(1f, 0.3f, 0.3f, 1f));
        toastLabel.clearActions();
        toastLabel.getColor().a = 1f;
        toastLabel.addAction(Actions.sequence(
            Actions.delay(3.0f),
            Actions.fadeOut(0.5f),
            Actions.run(() -> {
                toastLabel.setText("Pilih barang yang ingin Anda beli / upgrade.");
                toastLabel.setColor(Color.WHITE);
                toastLabel.getColor().a = 1f;
            })
        ));
    }

    // ── Button factory (ShapeRenderer-backed, ala MenuScreen) ────────────────

    private TextButton makeBtn(String label, Color bgColor, Runnable onClick) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font          = fontBtn;
        s.fontColor     = Color.WHITE;
        s.overFontColor = COL_GOLD;
        s.downFontColor = new Color(0.6f, 0.6f, 0.6f, 1f);
        TextButton btn = new TextButton(label, s);
        btn.getLabel().setAlignment(Align.center);

        // Hover scale animation
        btn.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override public void enter(InputEvent e, float x, float y, int ptr, Actor from) {
                btn.clearActions(); btn.addAction(Actions.scaleTo(1.06f, 1.06f, 0.08f));
            }
            @Override public void exit(InputEvent e, float x, float y, int ptr, Actor to) {
                btn.clearActions(); btn.addAction(Actions.scaleTo(1f, 1f, 0.08f));
            }
            @Override public void clicked(InputEvent event, float x, float y) {
                onClick.run();
            }
        });

        // Store bg color as user object for ShapeRenderer pass
        btn.setUserObject(bgColor);
        return btn;
    }

    /** Walk all TextButton actors and draw their ShapeRenderer backgrounds. */
    private void drawAllBtnBg() {
        for (Actor a : stage.getActors()) drawBtnBgInActor(a);
    }

    private void drawBtnBgInActor(Actor actor) {
        if (!actor.isVisible()) return;
        if (actor instanceof Table t) {
            for (Actor child : t.getChildren()) drawBtnBgInActor(child);
        } else if (actor instanceof TextButton btn && btn.getUserObject() instanceof Color bgColor) {
            drawBtnRoundRect(btn, bgColor);
        }
    }

    private void drawBtnRoundRect(TextButton btn, Color base) {
        float x = btn.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(0, 0)).x;
        float y = btn.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(0, 0)).y;
        float w = btn.getWidth()  * btn.getScaleX();
        float h = btn.getHeight() * btn.getScaleY();
        float r = 7f;

        boolean hover   = btn.isOver();
        boolean pressed = btn.isPressed();
        boolean canAfford = !(btn.getColor().a < 0.9f); // grey = disabled

        Color fill = pressed
            ? new Color(base.r * 0.55f, base.g * 0.55f, base.b * 0.55f, 0.95f)
            : hover
                ? new Color(Math.min(1, base.r * 1.35f), Math.min(1, base.g * 1.35f), Math.min(1, base.b * 1.35f), 0.95f)
                : new Color(base.r, base.g, base.b, 0.85f);

        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(fill);
        drawRoundRect(x, y, w, h, r);
        shape.end();

        // Border
        Color border = hover ? COL_GOLD : new Color(1f, 1f, 1f, 0.25f);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(border);
        drawRoundRectLine(x, y, w, h, r);
        shape.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    // ── Divider texture ──────────────────────────────────────────────────────

    private Image dividerImage() {
        Pixmap pm = new Pixmap(4, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1f, 0.85f, 0.25f, 0.4f); pm.fill();
        Texture t = new Texture(pm); pm.dispose(); textures.add(t);
        return new Image(new TextureRegionDrawable(new TextureRegion(t)));
    }

    // ── Stars ────────────────────────────────────────────────────────────────

    private void initStars() {
        starX = new float[STAR_COUNT]; starY = new float[STAR_COUNT];
        starSize = new float[STAR_COUNT]; starSpeed = new float[STAR_COUNT];
        starAlpha = new float[STAR_COUNT];
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i]     = MathUtils.random(W);
            starY[i]     = MathUtils.random(H);
            starSize[i]  = MathUtils.random(0.5f, 2.2f);
            starSpeed[i] = MathUtils.random(0.3f, 1.2f);
            starAlpha[i] = MathUtils.random(0.2f, 1f);
        }
    }

    private void updateStars(float delta) {
        for (int i = 0; i < STAR_COUNT; i++) {
            starAlpha[i] += starSpeed[i] * delta * 0.6f;
            if (starAlpha[i] > 1f) { starAlpha[i] = 0f; starX[i] = MathUtils.random(W); starY[i] = MathUtils.random(H); }
        }
    }

    // ── Rounded-rect drawing primitives ─────────────────────────────────────

    private void drawRoundRect(float x, float y, float w, float h, float r) {
        shape.rect(x + r, y, w - 2 * r, h);
        shape.rect(x, y + r, r, h - 2 * r);
        shape.rect(x + w - r, y + r, r, h - 2 * r);
        shape.arc(x + r,       y + r,       r, 180, 90);
        shape.arc(x + w - r,   y + r,       r, 270, 90);
        shape.arc(x + r,       y + h - r,   r,  90, 90);
        shape.arc(x + w - r,   y + h - r,   r,   0, 90);
    }

    private void drawRoundRectLine(float x, float y, float w, float h, float r) {
        shape.line(x + r, y,          x + w - r, y);
        shape.line(x + r, y + h,      x + w - r, y + h);
        shape.line(x,     y + r,      x,          y + h - r);
        shape.line(x + w, y + r,      x + w,      y + h - r);
        drawArcLine(x + r,     y + r,     r, 180, 90);
        drawArcLine(x + w - r, y + r,     r, 270, 90);
        drawArcLine(x + r,     y + h - r, r,  90, 90);
        drawArcLine(x + w - r, y + h - r, r,   0, 90);
    }

    private void drawArcLine(float cx, float cy, float r, float startDeg, float degreesSpan) {
        int segs = 8;
        for (int i = 0; i < segs; i++) {
            float a1 = (float) Math.toRadians(startDeg + (degreesSpan * i)       / segs);
            float a2 = (float) Math.toRadians(startDeg + (degreesSpan * (i + 1)) / segs);
            shape.line(cx + r * MathUtils.cos(a1), cy + r * MathUtils.sin(a1),
                       cx + r * MathUtils.cos(a2), cy + r * MathUtils.sin(a2));
        }
    }
}
