package io.github.jekjek.Screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayList;

/**
 * Cinematic announcement overlay.
 * Shows: dark backdrop (fade) + a belt that slides from left → holds → slides right.
 * Safe to reuse: call show() again at any time; it auto-dismisses the previous one.
 */
public class TransitionOverlay {

    private static final float SLIDE_IN  = 0.28f;
    private static final float SLIDE_OUT = 0.22f;

    private Table backdrop;
    private Table belt;
    private final ArrayList<Texture> managed = new ArrayList<>();
    private boolean showing = false;

    public boolean isShowing() { return showing; }

    // ─── Public API ────────────────────────────────────────────────────────────

    public void show(Stage stage,
                     BitmapFont titleFont, BitmapFont subFont,
                     String title, String subtitle,
                     Color accent, float holdSeconds,
                     Runnable onDone) {
        dismiss();
        showing = true;

        float sw = stage.getWidth();   // 800
        float sh = stage.getHeight();  // 480

        // ── Backdrop (full-screen dark overlay) ──
        backdrop = new Table();
        backdrop.setFillParent(true);
        backdrop.setBackground(new TextureRegionDrawable(
            new TextureRegion(solid(0f, 0f, 0f, 0.75f))));
        backdrop.getColor().a = 0f;
        stage.addActor(backdrop);

        // ── Belt textures ──
        Texture beltTex = solid(
            accent.r * 0.15f, accent.g * 0.15f, accent.b * 0.15f, 0.97f);
        Texture stripTex = solid(accent.r, accent.g, accent.b, 1f);
        Texture dimTex   = solid(0f, 0f, 0f, 0.35f); // side dim strips

        // ── Build belt content ──
        belt = new Table();
        belt.setBackground(new TextureRegionDrawable(new TextureRegion(beltTex)));

        // Top accent strip
        Image topStrip = new Image(new TextureRegionDrawable(new TextureRegion(stripTex)));
        belt.add(topStrip).expandX().fillX().height(4f).row();

        // Title label
        Label titleLbl = new Label(title, new Label.LabelStyle(titleFont, accent));
        titleLbl.setAlignment(Align.center);
        belt.add(titleLbl).expandX().center()
            .padTop(subtitle != null ? 12f : 18f)
            .padLeft(40f).padRight(40f)
            .padBottom(subtitle != null ? 4f : 18f).row();

        // Optional subtitle
        if (subtitle != null && subFont != null) {
            Label subLbl = new Label(subtitle, new Label.LabelStyle(subFont, Color.WHITE));
            subLbl.setAlignment(Align.center);
            belt.add(subLbl).expandX().center()
                .padBottom(12f).padLeft(40f).padRight(40f).row();
        }

        // Bottom accent strip
        Image botStrip = new Image(new TextureRegionDrawable(new TextureRegion(stripTex)));
        belt.add(botStrip).expandX().fillX().height(4f).row();

        // Belt height ≈ depends on content; pack to measure, then position
        belt.pack();
        float beltH = belt.getPrefHeight();
        float beltY = (sh - beltH) / 2f;

        // Start off-screen to the left, full width
        belt.setBounds(-sw, beltY, sw, beltH);
        stage.addActor(belt);

        // ── Animate ──
        // Backdrop: fade in
        backdrop.addAction(Actions.sequence(
            Actions.fadeIn(SLIDE_IN)
        ));

        // Belt: slide in → hold → slide out → done
        belt.addAction(Actions.sequence(
            Actions.moveTo(0f, beltY, SLIDE_IN, Interpolation.swingOut),
            Actions.delay(holdSeconds),
            Actions.moveTo(sw, beltY, SLIDE_OUT, Interpolation.swingIn),
            Actions.run(() -> {
                // Fade backdrop out simultaneously
                if (backdrop != null)
                    backdrop.addAction(Actions.sequence(
                        Actions.fadeOut(0.18f),
                        Actions.run(this::dismiss)
                    ));
                showing = false;
                if (onDone != null) onDone.run();
            })
        ));
    }

    /** Remove all overlay actors and free textures. */
    public void dismiss() {
        if (backdrop != null) { backdrop.remove(); backdrop = null; }
        if (belt     != null) { belt.remove();     belt     = null; }
        for (Texture t : managed) t.dispose();
        managed.clear();
        showing = false;
    }

    /** Free all resources (call on Screen.dispose). */
    public void dispose() { dismiss(); }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Texture solid(float r, float g, float b, float a) {
        Pixmap pm = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, a);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        managed.add(t);
        return t;
    }
}
