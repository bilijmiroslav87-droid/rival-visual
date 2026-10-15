package com.collapse.gui;

import com.collapse.CollapseClient;
import com.collapse.Module;
import com.collapse.ModuleManager;
import com.collapse.config.Config;
import com.collapse.render.Anim;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.ClientSettings;
import com.collapse.settings.ColorSetting;
import com.collapse.settings.ModeSetting;
import com.collapse.settings.NumSetting;
import com.collapse.settings.Setting;
import com.collapse.util.Theme;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Меню в стиле Nursultan Hub (Lua): компактное окно, слева вкладки, справа список карточек в один столбец.
 * Карточка: название слева, тумблер справа. ЛКМ: вкл/выкл. ПКМ: раскрыть настройки карточки прямо под ней.
 * Слайдер, список (клик по кругу), цвет (клик по палитре) показываются внутри раскрытой карточки.
 */
public class ClickMenuScreen extends Screen {
    // ---- палитра Nursultan Hub ----
    private static final int C_BG = 0xFF0C090B;
    private static final int C_CARD = 0xFF120D10;
    private static final int C_PILL = 0xFF100C0E;
    private static final int C_SIDE_ACT = 0xFF1B1013;
    private static final int C_BORDER = 0xFF24181C;
    private static final int C_BORDER_ACT = 0xFF5A242A;
    private static final int C_TEXT = 0xFFEBEBEB;
    private static final int C_MUTED = 0xFF786E73;
    private static final int C_DIM = 0xFF484044;
    private static final int C_TOGGLE_OFF = 0xFF241C20;
    private static final int C_KNOB_OFF = 0xFF4E4449;

    private static final int WIN_W = 420, WIN_H = 280;

    /** Вкладки: имя, иконка, какие категории модулей показывать. */
    private static final String[] TAB_NAMES = {"World", "Player", "Effects", "Screen", "Misc", "Combat"};
    private static final String[] TAB_ICONS = {"visuals", "player", "cosmetics", "settings", "misc", "combat"};

    // ---- состояние (static: переживает закрытие меню) ----
    private static int tab = 0;
    private static float winX = Float.NaN, winY = Float.NaN;
    private static final float[] TSCROLL = new float[16];
    private static final Map<Module, Boolean> OPEN = new HashMap<>();

    private enum Overlay { NONE, PROFILES }
    private Overlay overlay = Overlay.NONE;

    private final Anim openAnim = new Anim(0f, 10f);
    private final Anim scrollAnim = new Anim(0f, 22f);
    private final Map<Object, Anim> anims = new HashMap<>();

    private Module binding = null;
    private NumSetting dragging = null;
    private float dragX, dragW;
    private boolean draggingWin = false;
    private float dragOffX, dragOffY;
    private String search = "";
    private boolean searching = false;
    private String profileName = "";
    private boolean typingProfile = false;
    private String toast = "";
    private long toastAt = 0;
    private float overlayScroll = 0, overlayMax = 0;
    private float scrollMax = 0;

    private float sc;
    private float wx, wy, ww, wh;
    private float cx0, cy0, cw0, ch0; // область контента

    private record Hit(float x, float y, float w, float h, int type, Object a, int b, int mask) {}
    private static final int H_TAB = 1, H_CARD = 2, H_SLIDER = 3, H_MODE = 4, H_COLOR = 5, H_BOOL = 6, H_SEARCH = 7,
            H_CLOSE = 8, H_DRAG = 9, H_PROF = 10, H_PSAVE = 11, H_PLOAD = 12, H_PDEL = 13, H_PNAME = 14,
            H_OVER_CLOSE = 15, H_NOP = 16, H_THEME = 17, H_BIND = 18, H_FAV = 19;
    private final List<Hit> hits = new ArrayList<>();
    private final Map<NumSetting, float[]> dragXFor = new HashMap<>();

    private ClickMenuScreen() { super(Text.literal("Collapse")); }
    public static ClickMenuScreen create() { return new ClickMenuScreen(); }

    @Override
    protected void init() {
        openAnim.snap(0f);
        openAnim.set(1f);
    }

    @Override
    public void close() {
        Config.save();
        super.close();
    }

    private float spd() { return ClientSettings.animSpeed; }
    private float smooth(Object key, float target, float speed) {
        Anim a = anims.computeIfAbsent(key, k -> new Anim(target, speed * spd()));
        a.set(target);
        return a.get();
    }

    /** В какую вкладку попадает модуль. */
    private static int tabOf(Module m) {
        switch (m.getCategory()) {
            case COMBAT: return 5;
            case COSMETICS: return 2;
            case PLAYER: return 1;
            case MISC: return 4;
            case MOVEMENT: return 1;
            default: break;
        }
        String n = m.getName();
        switch (n) {
            case "Fullbright": case "CustomTime": case "CustomFog": case "NoRain": case "Chams":
            case "Esp": case "Tracers": case "ChestEsp": case "ItemEsp":
                return 0;
            case "Zoom": case "FovChanger": case "Cosmetics":
                return 2;
            default:
                return 3; // HUD / Crosshair / Vignette / TargetHUD / Armor / Keystrokes / Watermark ...
        }
    }

    private List<Module> list() {
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase();
        for (Module m : ModuleManager.MODULES) {
            if (m.isHidden()) continue;
            if (!q.isEmpty()) {
                if (m.getName().toLowerCase().contains(q) || m.getDescription().toLowerCase().contains(q)) out.add(m);
            } else if (tabOf(m) == tab) out.add(m);
        }
        return out;
    }

    private static String sectionOf(Module m) {
        String n = m.getName();
        return switch (n) {
            case "CustomFog", "Fullbright", "CustomTime", "NoRain" -> "Мир";
            case "Esp", "Tracers", "ChestEsp", "ItemEsp", "Chams" -> "Цели";
            case "Zoom", "FovChanger" -> "Камера";
            case "Cosmetics" -> "Косметика";
            case "Potions", "Watermark", "Notifications", "TargetHUD", "Keystrokes", "ArmorHUD" -> "HUD";
            case "Crosshair", "Vignette" -> "Экран";
            default -> m.getCategory().label;
        };
    }

    // ================= отрисовка =================
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        try {
            float o = Anim.ease(openAnim.get());
            if (ClientSettings.dimBackground) ctx.fillGradient(0, 0, width, height, (int) (110 * o) << 24, (int) (180 * o) << 24);
            sc = Math.max(0.6f, Math.min(1.8f, ClientSettings.menuScale));
            // окно подстраивается под экран: на узких экранах масштаб уменьшается автоматически
            float fit = Math.min(1f, Math.min((width - 8f) / (WIN_W * sc), (height - 8f) / (WIN_H * sc)));
            float s = sc * fit;
            sc = s;
            float availW = width / sc, availH = height / sc;
            ww = WIN_W; wh = WIN_H;
            if (Float.isNaN(winX) || fit < 1f) { winX = (availW - ww) / 2f; winY = (availH - wh) / 2f; }
            winX = Math.max(-ww + 60, Math.min(availW - 60, winX));
            winY = Math.max(0, Math.min(availH - 30, winY));
            wx = winX; wy = winY + (1 - o) * 12;
            hits.clear();
            var m = ctx.getMatrices();
            m.pushMatrix();
            m.scale(sc, sc);
            draw(ctx, mx / sc, my / sc, o);
            m.popMatrix();
        } catch (Throwable t) {
            CollapseClient.LOGGER.error("Menu render error", t);
        }
    }

    private void draw(DrawContext ctx, float mx, float my, float o) {
        int acc = Theme.accent(), acc2 = Theme.accent2();
        boolean blocked = overlay != Overlay.NONE;
        float mxx = blocked ? -9999 : mx, myy = blocked ? -9999 : my;

        // окно с мягким свечением
        Draw.glow(ctx, wx - 6, wy - 6, ww + 12, wh + 12, 18, Draw.alpha(Theme.withAlpha(acc, 44), o));
        Draw.shadow(ctx, wx, wy, ww, wh, 10, 16);
        Draw.roundRect(ctx, wx, wy, ww, wh, 10, Draw.alpha(C_BG, o));
        Draw.ring(ctx, wx, wy, ww, wh, 10, Draw.alpha(C_BORDER, o));

        // ---- верхняя полоса ----
        hits.add(new Hit(wx, wy, ww, 36, H_DRAG, null, 0, 1));
        drawLogo(ctx, wx + 8, wy + 3, o, acc, acc2);
        drawSearch(ctx, mxx, myy, wx + 52, wy + 6, 190, 24, o, acc);
        // кнопки Профили / Тема / закрыть
        float bx = wx + ww - 32;
        closeBtn(ctx, mxx, myy, bx, wy + 6, o, acc);
        bx -= 6;
        bx = pillBtn(ctx, mxx, myy, bx, wy + 6, 24, "Тема: " + Theme.name(), 1, o, acc);
        bx -= 6;
        pillBtn(ctx, mxx, myy, bx, wy + 6, 24, "Профили", 2, o, acc);

        // ---- сайдбар ----
        float sy = wy + 40;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            drawTab(ctx, mxx, myy, wx + 8, sy + i * 29, 96, 26, i, o, acc);
        }

        // ---- контент ----
        cx0 = wx + 112; cy0 = wy + 40; cw0 = ww - 120; ch0 = wh - 48;
        drawContent(ctx, mxx, myy, o, acc, acc2);

        if (ClientSettings.showHints) {
            String hint = binding != null ? "Нажмите клавишу для «" + binding.getName() + "»  ·  ESC: сбросить"
                    : "ЛКМ: вкл/выкл · ПКМ: настройки · СКМ: бинд · Ctrl+F: поиск";
            Fonts.MEDIUM.draw(ctx, hint, wx + 10, wy + wh + 6, 7.5f, Draw.alpha(binding != null ? acc : 0xAAFFFFFF, o * 0.7f));
        }
        {
            String diag = "font " + (Fonts.MEDIUM != null && Fonts.MEDIUM.isOk() ? "ok/" + Fonts.MEDIUM.glyphCount() : "FALLBACK")
                    + "  ·  cos " + com.collapse.cosmetics.CosmeticLayer.calls + "/" + com.collapse.cosmetics.CosmeticLayer.draws
                    + "/" + com.collapse.cosmetics.CosmeticLayer.errors
                    + "  ·  v" + CollapseClient.VERSION;
            Fonts.MEDIUM.drawRight(ctx, diag, wx + ww - 4, wy + wh + 6, 7f, Draw.alpha(0xAAFFFFFF, o * 0.5f));
        }
        if (System.currentTimeMillis() - toastAt < 1800 && !toast.isEmpty()) {
            float tw = Fonts.BOLD.width(toast, 9f) + 24;
            float tx = wx + ww / 2 - tw / 2, ty = wy + wh - 38;
            Draw.roundRect(ctx, tx, ty, tw, 20, 6, Draw.alpha(C_CARD, o));
            Draw.ring(ctx, tx, ty, tw, 20, 6, Draw.alpha(C_BORDER_ACT, o));
            Fonts.BOLD.drawCentered(ctx, toast, tx + tw / 2, ty + 6, 9f, Draw.alpha(C_TEXT, o));
        }
        if (overlay == Overlay.PROFILES) drawProfiles(ctx, mx, my, o, acc, acc2);
    }

    private void drawLogo(DrawContext ctx, float x, float y, float o, int acc, int acc2) {
        Draw.glow(ctx, x - 2, y - 2, 30, 30, 10, Draw.alpha(Theme.withAlpha(acc, 90), o));
        Draw.roundGradientV(ctx, x + 2, y + 2, 26, 26, 8, Draw.alpha(acc2, o), Draw.alpha(acc, o));
        Fonts.HEAD.drawCentered(ctx, "C", x + 15, y + 7, 13f, Draw.alpha(0xFF1A0709, o));
    }

    private void drawSearch(DrawContext ctx, float mx, float my, float x, float y, float w, float h, float o, int acc) {
        float sf = smooth("search", searching ? 1f : 0f, 16f);
        Draw.roundRect(ctx, x, y, w, h, 6, Draw.alpha(C_CARD, o));
        Draw.ring(ctx, x, y, w, h, 6, Draw.alpha(Theme.mix(C_BORDER, Theme.withAlpha(acc, 200), sf), o));
        Draw.ring(ctx, x + 8, y + 7, 8, 8, 4, Draw.alpha(C_MUTED, o));
        Draw.roundRect(ctx, x + 15, y + 14, 3.5f, 1.5f, 0.7f, Draw.alpha(C_MUTED, o));
        boolean blink = searching && (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = search.isEmpty() ? (searching ? (blink ? "|" : "") : "Search functions...")
                : Fonts.MEDIUM.fit(search, w - 44, 9f) + (blink ? "|" : "");
        Fonts.MEDIUM.draw(ctx, shown, x + 24, y + 8, 9f, Draw.alpha(search.isEmpty() ? C_MUTED : C_TEXT, o));
        if (!search.isEmpty()) Fonts.BOLD.draw(ctx, "×", x + w - 13, y + 6, 11f, Draw.alpha(C_MUTED, o));
        hits.add(new Hit(x, y, w, h, H_SEARCH, null, 0, 1));
    }

    private void closeBtn(DrawContext ctx, float mx, float my, float x, float y, float o, int acc) {
        boolean hv = in(mx, my, x, y, 24, 24);
        float t = smooth("close", hv ? 1f : 0f, 18f);
        Draw.roundRect(ctx, x, y, 24, 24, 6, Draw.alpha(C_CARD, o));
        Draw.ring(ctx, x, y, 24, 24, 6, Draw.alpha(C_BORDER, o));
        Fonts.BOLD.drawCentered(ctx, "X", x + 12, y + 7.5f, 10f, Draw.alpha(Theme.mix(C_MUTED, acc, t), o));
        hits.add(new Hit(x, y, 24, 24, H_CLOSE, null, 0, 1));
    }

    /** Кнопка-пилюля справа налево, возвращает левый край. */
    private float pillBtn(DrawContext ctx, float mx, float my, float right, float y, float h, String label, int id, float o, int acc) {
        float tw = Fonts.BOLD.width(label, 8f);
        float w = tw + 18;
        float x = right - w;
        boolean hv = in(mx, my, x, y, w, h);
        float t = smooth("pill" + id, hv ? 1f : 0f, 18f);
        boolean act = id == 2 && overlay == Overlay.PROFILES;
        Draw.roundRect(ctx, x, y, w, h, 6, Draw.alpha(act ? C_SIDE_ACT : C_CARD, o));
        Draw.ring(ctx, x, y, w, h, 6, Draw.alpha(Theme.mix(C_BORDER, C_BORDER_ACT, Math.max(t, act ? 1f : 0f)), o));
        Fonts.BOLD.drawCentered(ctx, label, x + w / 2, y + 8, 8f, Draw.alpha(Theme.mix(C_MUTED, C_TEXT, t), o));
        hits.add(new Hit(x, y, w, h, H_PROF, id, 0, 3));
        return x;
    }

    private void drawTab(DrawContext ctx, float mx, float my, float x, float y, float w, float h, int i, float o, int acc) {
        boolean sel = tab == i && search.isEmpty();
        boolean hv = in(mx, my, x, y, w, h);
        float s = smooth("tab" + i, sel ? 1f : 0f, 16f);
        float hvv = smooth("tabh" + i, hv ? 1f : 0f, 18f);
        if (s > 0.01f || hvv > 0.01f) {
            Draw.roundRect(ctx, x, y, w, h, 6, Draw.alpha(Theme.withAlpha(C_SIDE_ACT, (int) (255 * Math.max(s, hvv * 0.5f))), o));
        }
        if (s > 0.01f) Draw.ring(ctx, x, y, w, h, 6, Draw.alpha(Theme.withAlpha(C_BORDER_ACT, (int) (255 * s)), o));
        Draw.icon(ctx, TAB_ICONS[i], x + 15, y + h / 2f, 13, Draw.alpha(Theme.mix(C_MUTED, acc, s), o));
        Fonts.MEDIUM.draw(ctx, TAB_NAMES[i], x + 28, y + h / 2f - 4.5f, 9.5f, Draw.alpha(Theme.mix(C_MUTED, C_TEXT, Math.max(s, hvv * 0.6f)), o));
        hits.add(new Hit(x, y, w, h, H_TAB, i, 0, 1));
    }

    // ---------- контент ----------
    private float settingH(Setting<?> s) {
        return s instanceof NumSetting ? 30 : 22;
    }

    private float cardH(Module m) {
        float t = smooth("open" + m.getName(), OPEN.getOrDefault(m, false) ? 1f : 0f, 14f);
        float full = 6;
        for (Setting<?> s : m.getSettings()) full += settingH(s);
        if (m.getSettings().isEmpty()) full = 0;
        return 30 + full * t;
    }

    private void drawContent(DrawContext ctx, float mx, float my, float o, int acc, int acc2) {
        List<Module> mods = list();
        // общая высота
        float total = 0;
        String lastSec = null;
        for (Module m : mods) {
            String sec = search.isEmpty() ? sectionOf(m) : null;
            if (sec != null && !sec.equals(lastSec)) { total += 18; lastSec = sec; }
            total += cardH(m) + 5;
        }
        scrollMax = Math.max(0, total - ch0);
        TSCROLL[tab] = Math.min(TSCROLL[tab], scrollMax);
        scrollAnim.set(TSCROLL[tab]);
        float sy = scrollAnim.get();

        if (mods.isEmpty()) {
            Fonts.BOLD.drawCentered(ctx, "Ничего не найдено", cx0 + cw0 / 2, cy0 + ch0 / 2 - 4, 9.5f, Draw.alpha(C_MUTED, o));
            return;
        }
        ctx.enableScissor(Math.round((cx0 - 2) * sc), Math.round(cy0 * sc), Math.round((cx0 + cw0 + 2) * sc), Math.round((cy0 + ch0) * sc));
        float y = cy0 - sy;
        lastSec = null;
        for (Module m : mods) {
            String sec = search.isEmpty() ? sectionOf(m) : null;
            if (sec != null && !sec.equals(lastSec)) {
                lastSec = sec;
                if (y + 16 > cy0 - 2 && y < cy0 + ch0) Fonts.BOLD.draw(ctx, sec.toUpperCase(), cx0 + 2, y + 3, 8f, Draw.alpha(acc, o));
                y += 18;
            }
            float h = cardH(m);
            if (y + h >= cy0 - 2 && y <= cy0 + ch0 + 2) drawCard(ctx, mx, my, m, cx0, y, cw0 - 6, h, o, acc, acc2);
            y += h + 5;
        }
        ctx.disableScissor();
        if (scrollMax > 1) {
            float th = Math.max(16, ch0 * ch0 / total);
            float ty = cy0 + (ch0 - th) * (sy / scrollMax);
            Draw.roundRect(ctx, cx0 + cw0 - 2, ty, 2, th, 1, Draw.alpha(C_BORDER_ACT, o));
        }
    }

    private void drawCard(DrawContext ctx, float mx, float my, Module m, float x, float y, float w, float h, float o, int acc, int acc2) {
        boolean inView = my >= cy0 && my <= cy0 + ch0;
        boolean hv = inView && in(mx, my, x, y, w, 30);
        float on = smooth(m, m.isEnabled() ? 1f : 0f, 14f);
        float hvv = smooth("ch" + m.getName(), hv ? 1f : 0f, 18f);
        boolean open = OPEN.getOrDefault(m, false);

        Draw.roundRect(ctx, x, y, w, h, 6, Draw.alpha(Theme.mix(C_PILL, 0xFF171014, hvv), o));
        Draw.ring(ctx, x, y, w, h, 6, Draw.alpha(Theme.mix(C_BORDER, C_BORDER_ACT, Math.max(on * 0.8f, hvv * 0.6f)), o));

        // название и описание
        Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(m.getName(), w - 110, 10.5f), x + 10, y + 5.5f, 10.5f, Draw.alpha(Theme.mix(C_TEXT, 0xFFFFFFFF, on), o));
        Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(m.getDescription(), w - 110, 6.8f), x + 10, y + 18.5f, 6.8f, Draw.alpha(C_DIM, o));

        // тумблер (32x16 как в Lua)
        float tw = 32, th = 16, tx = x + w - tw - 8, ty = y + 7;
        Draw.roundRect(ctx, tx, ty, tw, th, 8, Draw.alpha(Theme.mix(C_TOGGLE_OFF, acc, on), o));
        Draw.circle(ctx, tx + 2 + (tw - 16) * on, ty + 2, 12, Draw.alpha(Theme.mix(C_KNOB_OFF, 0xFFFFFFFF, on), o));

        // бинд-чип и стрелка настроек
        float rx = tx - 6;
        if (!m.getSettings().isEmpty()) {
            Fonts.BOLD.drawRight(ctx, open ? "˅" : "˃", rx, y + 8, 9f, Draw.alpha(Theme.mix(C_MUTED, acc, open ? 1f : 0f), o));
            rx -= 12;
        }
        String bt = binding == m ? "..." : (m.getBind() > 0 ? keyName(m.getBind()) : null);
        if (bt != null) {
            float bw = Fonts.BOLD.width(bt, 6.5f) + 9;
            Draw.roundRect(ctx, rx - bw, y + 9, bw, 12, 4, Draw.alpha(0xFF1E1519, o));
            Fonts.BOLD.drawCentered(ctx, bt, rx - bw / 2, y + 11.5f, 6.5f, Draw.alpha(acc, o));
        }
        hits.add(new Hit(x, y, w, 30, H_CARD, m, 0, 7));

        // раскрытые настройки
        float t = smooth("open" + m.getName(), open ? 1f : 0f, 14f);
        if (t > 0.05f && h > 34) {
            ctx.fill(Math.round(x + 10), Math.round(y + 30), Math.round(x + w - 10), Math.round(y + 31), Draw.alpha(C_BORDER, o));
            float sy = y + 34;
            for (Setting<?> s : m.getSettings()) {
                if (sy + settingH(s) <= y + h + 1) drawSetting(ctx, mx, my, s, x + 10, sy, w - 20, o * t, acc, acc2, inView);
                sy += settingH(s);
            }
        }
    }

    private void drawSetting(DrawContext ctx, float mx, float my, Setting<?> s, float x, float y, float w, float o, int acc, int acc2, boolean inView) {
        if (s instanceof BoolSetting b) {
            float t = smooth(b, b.get() ? 1f : 0f, 16f);
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w - 40, 9f), x, y + 5, 9f, Draw.alpha(Theme.mix(C_MUTED, C_TEXT, t), o));
            float tw = 26, th = 13, tx = x + w - tw, ty = y + 3;
            Draw.roundRect(ctx, tx, ty, tw, th, 6.5f, Draw.alpha(Theme.mix(C_TOGGLE_OFF, acc, t), o));
            Draw.circle(ctx, tx + 1.5f + (tw - 10 - 3) * t + 0.5f, ty + 1.5f, 10, Draw.alpha(Theme.mix(C_KNOB_OFF, 0xFFFFFFFF, t), o));
            hits.add(new Hit(x - 4, y, w + 8, 20, H_BOOL, s, 0, 1));
        } else if (s instanceof ColorSetting cs) {
            Fonts.MEDIUM.draw(ctx, s.getName(), x, y + 5, 9f, Draw.alpha(C_MUTED, o));
            Draw.roundRect(ctx, x + w - 32, y + 2, 32, 16, 5, Draw.alpha(C_BORDER_ACT, o));
            Draw.roundRect(ctx, x + w - 31, y + 3, 30, 14, 4, Draw.alpha(cs.get(), o));
            hits.add(new Hit(x - 4, y, w + 8, 20, H_COLOR, s, 0, 3));
        } else if (s instanceof ModeSetting ms) {
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w * 0.45f, 9f), x, y + 5, 9f, Draw.alpha(C_MUTED, o));
            float dw = Math.min(w * 0.55f, 100), dx = x + w - dw;
            boolean hov = inView && in(mx, my, dx, y + 1, dw, 18);
            Draw.roundRect(ctx, dx, y + 1, dw, 18, 5, Draw.alpha(hov ? 0xFF1B1317 : C_CARD, o));
            Draw.ring(ctx, dx, y + 1, dw, 18, 5, Draw.alpha(hov ? C_BORDER_ACT : C_BORDER, o));
            Fonts.BOLD.drawCentered(ctx, Fonts.BOLD.fit(ms.get(), dw - 22, 8f), dx + dw / 2, y + 6, 8f, Draw.alpha(acc, o));
            Fonts.BOLD.draw(ctx, "‹", dx + 5, y + 4.5f, 9f, Draw.alpha(C_MUTED, o));
            Fonts.BOLD.drawRight(ctx, "›", dx + dw - 5, y + 4.5f, 9f, Draw.alpha(C_MUTED, o));
            hits.add(new Hit(dx, y + 1, dw / 2, 18, H_MODE, ms, -1, 3));
            hits.add(new Hit(dx + dw / 2, y + 1, dw / 2, 18, H_MODE, ms, 1, 3));
        } else if (s instanceof NumSetting ns) {
            float p = smooth(ns, (float) ns.getPercent(), 22f);
            String v = (ns.get() % 1 == 0) ? String.valueOf(ns.get().intValue()) : String.format(java.util.Locale.US, "%.2f", ns.get());
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w - 40, 9f), x, y + 3, 9f, Draw.alpha(C_MUTED, o));
            Fonts.BOLD.drawRight(ctx, v, x + w, y + 3, 9f, Draw.alpha(C_TEXT, o));
            float by = y + 20;
            Draw.roundRect(ctx, x, by, w, 4, 2, Draw.alpha(C_TOGGLE_OFF, o));
            Draw.roundRect(ctx, x, by, Math.max(4, w * p), 4, 2, Draw.alpha(acc, o));
            Draw.circle(ctx, x + w * p - 4, by - 2, 8, Draw.alpha(0xFFFFFFFF, o));
            hits.add(new Hit(x - 5, y + 12, w + 10, 18, H_SLIDER, ns, 0, 1));
            dragXFor.put(ns, new float[]{x, w});
        }
    }

    // ---------- профили ----------
    private void drawProfiles(DrawContext ctx, float mx, float my, float o, int acc, int acc2) {
        ctx.fill(0, 0, width, height, Draw.alpha(0x99000000, o));
        hits.add(new Hit(0, 0, width / sc, height / sc, H_OVER_CLOSE, null, 0, 1));
        float w = 280, h = 240;
        float x = wx + ww / 2 - w / 2, y = wy + wh / 2 - h / 2;
        Draw.glow(ctx, x - 4, y - 4, w + 8, h + 8, 14, Draw.alpha(Theme.withAlpha(acc, 40), o));
        Draw.roundRect(ctx, x, y, w, h, 10, Draw.alpha(C_BG, o));
        Draw.ring(ctx, x, y, w, h, 10, Draw.alpha(C_BORDER, o));
        hits.add(new Hit(x, y, w, h, H_NOP, null, 0, 7));
        Fonts.HEAD.draw(ctx, "Профили", x + 14, y + 11, 11f, Draw.alpha(C_TEXT, o));
        boolean ch = in(mx, my, x + w - 30, y + 8, 20, 20);
        Fonts.BOLD.drawCentered(ctx, "X", x + w - 20, y + 12, 10f, Draw.alpha(ch ? acc : C_MUTED, o));
        hits.add(new Hit(x + w - 30, y + 8, 20, 20, H_OVER_CLOSE, null, 0, 1));

        float ax = x + 14, aw = w - 28, ay = y + 38, fh = 26, bw = 74, fw = aw - bw - 6;
        float fa = smooth("pname", typingProfile ? 1f : 0f, 16f);
        Draw.roundRect(ctx, ax, ay, fw, fh, 6, Draw.alpha(C_CARD, o));
        Draw.ring(ctx, ax, ay, fw, fh, 6, Draw.alpha(Theme.mix(C_BORDER, Theme.withAlpha(acc, 200), fa), o));
        boolean blink = typingProfile && (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = profileName.isEmpty() ? (typingProfile ? (blink ? "|" : "") : "Имя профиля") : profileName + (blink ? "|" : "");
        Fonts.MEDIUM.draw(ctx, shown, ax + 10, ay + 9, 9f, Draw.alpha(profileName.isEmpty() ? C_MUTED : C_TEXT, o));
        hits.add(new Hit(ax, ay, fw, fh, H_PNAME, null, 0, 1));
        Draw.roundRect(ctx, ax + fw + 6, ay, bw, fh, 6, Draw.alpha(acc, o));
        Fonts.BOLD.drawCentered(ctx, "Сохранить", ax + fw + 6 + bw / 2, ay + 9, 8.5f, Draw.alpha(0xFF1A0709, o));
        hits.add(new Hit(ax + fw + 6, ay, bw, fh, H_PSAVE, null, 0, 1));

        List<String> ps = Config.profiles();
        float ly = ay + fh + 10, viewH = y + h - ly - 10;
        ctx.enableScissor(Math.round(ax * sc), Math.round(ly * sc), Math.round((ax + aw) * sc), Math.round((ly + viewH) * sc));
        float cy = ly - overlayScroll;
        if (ps.isEmpty()) Fonts.MEDIUM.drawCentered(ctx, "Профилей пока нет", ax + aw / 2, ly + 30, 9f, Draw.alpha(C_MUTED, o));
        for (String p : ps) {
            boolean act = p.equals(Config.currentProfile);
            Draw.roundRect(ctx, ax, cy, aw, 28, 6, Draw.alpha(C_PILL, o));
            Draw.ring(ctx, ax, cy, aw, 28, 6, Draw.alpha(act ? C_BORDER_ACT : C_BORDER, o));
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(p, aw - 130, 10f), ax + 10, cy + 9, 10f, Draw.alpha(act ? acc : C_TEXT, o));
            float bx = ax + aw - 8 - 48;
            Draw.roundRect(ctx, bx, cy + 5, 48, 18, 5, Draw.alpha(0xFF2A1519, o));
            Fonts.BOLD.drawCentered(ctx, "Удалить", bx + 24, cy + 10, 7.5f, Draw.alpha(0xFFFF6B7D, o));
            hits.add(new Hit(bx, cy + 5, 48, 18, H_PDEL, p, 0, 1));
            bx -= 52;
            Draw.roundRect(ctx, bx, cy + 5, 48, 18, 5, Draw.alpha(Theme.withAlpha(acc, 50), o));
            Fonts.BOLD.drawCentered(ctx, "Загрузить", bx + 24, cy + 10, 7.5f, Draw.alpha(acc, o));
            hits.add(new Hit(bx, cy + 5, 48, 18, H_PLOAD, p, 0, 1));
            cy += 33;
        }
        ctx.disableScissor();
        overlayMax = Math.max(0, cy + overlayScroll - (ly + viewH));
        if (overlayScroll > overlayMax) overlayScroll = overlayMax;
    }

    // ================= ввод =================
    private static boolean in(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static String keyName(int key) {
        String n = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
        if (n != null) return n.toUpperCase();
        return switch (key) {
            case 344 -> "RSHIFT"; case 340 -> "LSHIFT"; case 345 -> "RCTRL"; case 341 -> "LCTRL";
            case 346 -> "RALT"; case 342 -> "LALT"; case 32 -> "SPACE"; case 258 -> "TAB";
            case 290 -> "F1"; case 291 -> "F2"; case 292 -> "F3"; case 293 -> "F4"; case 294 -> "F5";
            case 295 -> "F6"; case 296 -> "F7"; case 297 -> "F8"; case 298 -> "F9"; case 299 -> "F10";
            default -> "K" + key;
        };
    }

    private void toast(String s) { toast = s; toastAt = System.currentTimeMillis(); }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        float mx = (float) click.x() / sc, my = (float) click.y() / sc;
        int btn = click.button();
        int bit = btn == 0 ? 1 : btn == 1 ? 2 : 4;
        searching = false; typingProfile = false;
        boolean inContent = in(mx, my, cx0, cy0, cw0, ch0);

        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!in(mx, my, h.x, h.y, h.w, h.h)) continue;
            if ((h.mask & bit) == 0) {
                if (h.type == H_NOP || h.type == H_OVER_CLOSE) return true;
                continue;
            }
            boolean contentHit = h.type == H_CARD || h.type == H_SLIDER || h.type == H_MODE || h.type == H_COLOR || h.type == H_BOOL;
            if (contentHit && (!inContent || overlay != Overlay.NONE)) continue;
            switch (h.type) {
                case H_NOP -> { return true; }
                case H_OVER_CLOSE -> { overlay = Overlay.NONE; overlayScroll = 0; typingProfile = false; return true; }
                case H_DRAG -> { draggingWin = true; dragOffX = mx - wx; dragOffY = my - winY; return true; }
                case H_CLOSE -> { this.close(); return true; }
                case H_TAB -> { tab = (Integer) h.a; search = ""; scrollAnim.snap(TSCROLL[tab]); return true; }
                case H_SEARCH -> {
                    if (!search.isEmpty() && mx > h.x + h.w - 20) search = ""; else searching = true;
                    return true;
                }
                case H_PROF -> {
                    int id = (Integer) h.a;
                    if (id == 1) { if (btn == 1) Theme.prev(); else Theme.next(); }
                    else { overlay = Overlay.PROFILES; overlayScroll = 0; }
                    return true;
                }
                case H_CARD -> {
                    Module m = (Module) h.a;
                    if (btn == 2) binding = m;
                    else if (btn == 1) { if (!m.getSettings().isEmpty()) OPEN.put(m, !OPEN.getOrDefault(m, false)); else binding = m; }
                    else m.toggle();
                    return true;
                }
                case H_BOOL -> { ((BoolSetting) h.a).toggle(); return true; }
                case H_COLOR -> { ColorSetting cs = (ColorSetting) h.a; if (btn == 1) cs.prev(); else cs.next(); return true; }
                case H_MODE -> {
                    ModeSetting ms = (ModeSetting) h.a;
                    String[] md = ms.getModes();
                    int idx = 0;
                    for (int k = 0; k < md.length; k++) if (md[k].equals(ms.get())) idx = k;
                    ms.set(md[Math.floorMod(idx + h.b, md.length)]);
                    return true;
                }
                case H_SLIDER -> {
                    dragging = (NumSetting) h.a;
                    float[] d = dragXFor.get(dragging);
                    dragX = d != null ? d[0] : h.x + 5;
                    dragW = d != null ? d[1] : h.w - 10;
                    applyDrag(mx);
                    return true;
                }
                case H_PNAME -> { typingProfile = true; return true; }
                case H_PSAVE -> {
                    String n = profileName.trim();
                    if (n.isEmpty()) { toast("Введите имя профиля"); typingProfile = true; return true; }
                    if (Config.saveProfile(n)) { toast("Профиль «" + n + "» сохранён"); profileName = ""; } else toast("Не удалось сохранить");
                    return true;
                }
                case H_PLOAD -> { String n = (String) h.a; toast(Config.loadProfile(n) ? "Загружен «" + n + "»" : "Ошибка загрузки"); return true; }
                case H_PDEL -> { String n = (String) h.a; Config.deleteProfile(n); toast("Удалён «" + n + "»"); return true; }
                default -> { return true; }
            }
        }
        return true;
    }

    private void applyDrag(double mx) {
        if (dragging == null) return;
        double p = Math.max(0, Math.min(1, (mx - dragX) / dragW));
        dragging.set(dragging.getMin() + (dragging.getMax() - dragging.getMin()) * p);
    }

    @Override
    public boolean mouseDragged(Click c, double dx, double dy) {
        float mx = (float) c.x() / sc, my = (float) c.y() / sc;
        if (dragging != null) { applyDrag(mx); return true; }
        if (draggingWin) { winX = mx - dragOffX; winY = my - dragOffY; return true; }
        return true;
    }

    @Override
    public boolean mouseReleased(Click c) { dragging = null; draggingWin = false; return true; }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        if (overlay != Overlay.NONE) { overlayScroll = (float) Math.max(0, Math.min(overlayMax, overlayScroll - v * 24)); return true; }
        TSCROLL[tab] = (float) Math.max(0, Math.min(scrollMax, TSCROLL[tab] - v * 28));
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        int cp = input.codepoint();
        if (cp < 32 || cp == 127) return false;
        String ch = new String(Character.toChars(cp));
        if (searching) { if (search.length() < 24) { search += ch; TSCROLL[tab] = 0; } return true; }
        if (typingProfile) { if (profileName.length() < 20) profileName += ch; return true; }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int k = input.key();
        if (binding != null) { binding.setBind(k == 256 ? -1 : k); binding = null; return true; }
        if (searching) {
            if (k == 259 && !search.isEmpty()) { search = search.substring(0, search.length() - 1); TSCROLL[tab] = 0; }
            else if (k == 256 || k == 257) searching = false;
            return true;
        }
        if (typingProfile) {
            if (k == 259 && !profileName.isEmpty()) profileName = profileName.substring(0, profileName.length() - 1);
            else if (k == 256) typingProfile = false;
            else if (k == 257) {
                String n = profileName.trim();
                if (!n.isEmpty() && Config.saveProfile(n)) { toast("Профиль «" + n + "» сохранён"); profileName = ""; }
                typingProfile = false;
            }
            return true;
        }
        if (k == 256 && overlay != Overlay.NONE) { overlay = Overlay.NONE; overlayScroll = 0; return true; }
        if (k == 344 || k == 256) { this.close(); return true; }
        if (k == 70 && (input.modifiers() & 2) != 0) { searching = true; return true; }
        return super.keyPressed(input);
    }

    @Override public boolean shouldPause() { return false; }
}
