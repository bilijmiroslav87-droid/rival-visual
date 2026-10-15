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
 * Меню Collapse в стиле читов: отдельные перетаскиваемые панели по категориям.
 * ЛКМ по модулю: вкл/выкл. ПКМ: раскрыть настройки. СКМ: назначить клавишу.
 * Заголовок панели: ЛКМ таскать, ПКМ свернуть. Справа сверху: поиск, профили, настройки клиента.
 */
public class ClickMenuScreen extends Screen {
    // ---- палитра ----
    private static final int C_HEAD = 0xFF0C0C12;
    private static final int C_BODY = 0xEE101018;
    private static final int C_ROW_H = 0x16FFFFFF;
    private static final int C_SET_BG = 0xCC0B0B11;
    private static final int C_TEXT = 0xFFF1F1F7;
    private static final int C_SUB = 0xFFA4A4B8;
    private static final int C_MUTED = 0xFF6A6A80;
    private static final int C_FIELD = 0xFF1B1B25;
    private static final int C_OFF = 0xFF2A2A37;

    private static final int PANEL_W = 118;
    private static final int HEAD_H = 20;
    private static final int ROW_H = 16;

    // ---- состояние панелей (static: переживает закрытие меню) ----
    private static final float[] PX = new float[Module.Category.values().length];
    private static final float[] PY = new float[Module.Category.values().length];
    private static final boolean[] COLLAPSED = new boolean[Module.Category.values().length];
    private static final float[] PSCROLL = new float[Module.Category.values().length];
    private static boolean placed = false;
    private static final Map<Module, Boolean> EXPANDED = new HashMap<>();

    private enum Overlay { NONE, PROFILES, SETTINGS }
    private Overlay overlay = Overlay.NONE;

    private final Anim openAnim = new Anim(0f, 10f);
    private final Map<Object, Anim> anims = new HashMap<>();
    private final Map<Module, Anim> expandAnims = new HashMap<>();

    private Module binding = null;
    private ModeSetting openDrop = null;
    private NumSetting dragging = null;
    private float dragX, dragW;
    private int draggingPanel = -1;
    private float dragOffX, dragOffY;
    private String search = "";
    private boolean searching = false;
    private String profileName = "";
    private boolean typingProfile = false;
    private String toast = "";
    private long toastAt = 0;
    private float overlayScroll = 0;
    private float overlayMax = 0;

    private float sc;
    private final long openedAt = System.currentTimeMillis();

    private record Hit(float x, float y, float w, float h, int type, Object a, int b, int btnMask) {}
    private static final int H_HEAD = 1, H_MOD = 2, H_SET = 3, H_OPT = 4, H_SLIDER = 5, H_TOPBTN = 6, H_SEARCH = 7,
            H_CSET = 8, H_THEME = 9, H_CSTEP = 10, H_PSAVE = 11, H_PLOAD = 12, H_PDEL = 13, H_PNAME = 14,
            H_OVERLAY_CLOSE = 15, H_COLOR = 16, H_NOP = 17;
    private final List<Hit> hits = new ArrayList<>();

    private ClickMenuScreen() { super(Text.literal("Collapse")); }
    public static ClickMenuScreen create() { return new ClickMenuScreen(); }

    private static float placedForW = -1;

    @Override
    protected void init() {
        openAnim.snap(0f);
        openAnim.set(1f);
        sc = Math.max(0.7f, Math.min(1.4f, ClientSettings.menuScale));
        float availW = width / sc;
        // раскладываем заново при первом запуске или если экран изменился (например, поворот на телефоне)
        if (!placed || Math.abs(placedForW - availW) > 1) {
            placed = true;
            placedForW = availW;
            autoLayout(availW);
        }
    }

    /** Ряд за рядом: панели переносятся на новую строку, если не влезают по ширине. */
    private static void autoLayout(float availW) {
        int n = PX.length;
        float gap = 8, startX = 10, x = startX, y = 34, rowH = 0;
        for (int i = 0; i < n; i++) {
            if (x + PANEL_W > availW - 6 && x > startX) {
                x = startX;
                y += 150;
            }
            PX[i] = x;
            PY[i] = y;
            x += PANEL_W + gap;
        }
    }

    @Override
    public void close() {
        Config.save();
        super.close();
    }

    private float spd() { return ClientSettings.animSpeed; }
    private Anim an(Object key, float init, float speed) { return anims.computeIfAbsent(key, k -> new Anim(init, speed)); }
    private float smooth(Object key, float target, float speed) {
        Anim a = an(key, target, speed * spd());
        a.set(target);
        return a.get();
    }

    private List<Module> modulesOf(int cat) {
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase();
        for (Module m : ModuleManager.MODULES) {
            if (m.isHidden() || m.getCategory().ordinal() != cat) continue;
            if (!q.isEmpty() && !(m.getName().toLowerCase().contains(q) || m.getDescription().toLowerCase().contains(q))) continue;
            out.add(m);
        }
        return out;
    }

    private float expandT(Module m) {
        Anim a = expandAnims.computeIfAbsent(m, k -> new Anim(EXPANDED.getOrDefault(m, false) ? 1f : 0f, 14f));
        a.set(EXPANDED.getOrDefault(m, false) ? 1f : 0f);
        return a.get();
    }

    // ================= отрисовка =================
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        try {
            float o = Anim.ease(openAnim.get());
            if (ClientSettings.dimBackground) {
                ctx.fillGradient(0, 0, width, height, (int) (120 * o) << 24, (int) (190 * o) << 24);
            }
            sc = Math.max(0.7f, Math.min(1.4f, ClientSettings.menuScale));
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
        int acc = Theme.accent();
        int acc2 = Theme.accent2();
        float availW = width / sc, availH = height / sc;

        drawTopBar(ctx, mx, my, o, acc, acc2, availW);

        // панели
        boolean blocked = overlay != Overlay.NONE;
        int n = Module.Category.values().length;
        // рисуем в порядке; перетаскиваемая рисуется последней (поверх)
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < n; i++) {
                boolean top = i == draggingPanel;
                if ((pass == 0) == top) continue;
                drawPanel(ctx, mx, my, o, acc, acc2, i, availW, availH, blocked);
            }
        }

        if (overlay == Overlay.SETTINGS) drawSettingsOverlay(ctx, mx, my, o, acc, acc2, availW, availH);
        else if (overlay == Overlay.PROFILES) drawProfilesOverlay(ctx, mx, my, o, acc, acc2, availW, availH);

        // подсказки
        if (ClientSettings.showHints) {
            String hint = binding != null
                    ? "Нажмите клавишу для «" + binding.getName() + "»   ·   ESC: сбросить"
                    : "ЛКМ: вкл/выкл   ·   ПКМ: настройки   ·   СКМ: бинд   ·   Заголовок: ЛКМ тащить, ПКМ свернуть   ·   Ctrl+F: поиск";
            float hw = Fonts.MEDIUM.width(hint, 8f) + 20;
            float hx = availW / 2 - hw / 2, hy = availH - 22;
            Draw.roundRect(ctx, hx, hy, hw, 16, 8, Draw.alpha(0xCC0B0B11, o));
            Draw.ring(ctx, hx, hy, hw, 16, 8, Draw.alpha(0x18FFFFFF, o));
            Fonts.MEDIUM.drawCentered(ctx, hint, availW / 2, hy + 4.5f, 8f, Draw.alpha(binding != null ? acc : C_SUB, o));
        }
        // toast
        if (System.currentTimeMillis() - toastAt < 1800 && !toast.isEmpty()) {
            float tw = Fonts.BOLD.width(toast, 9f) + 24;
            float tx = availW / 2 - tw / 2, ty = availH - 50;
            Draw.shadow(ctx, tx, ty, tw, 20, 8, 8);
            Draw.roundRect(ctx, tx, ty, tw, 20, 10, Draw.alpha(0xFF1D1D29, o));
            Draw.ring(ctx, tx, ty, tw, 20, 10, Draw.alpha(Theme.withAlpha(acc, 120), o));
            Fonts.BOLD.drawCentered(ctx, toast, tx + tw / 2, ty + 6, 9f, Draw.alpha(C_TEXT, o));
        }
    }

    // ---------- верхняя полоса ----------
    private void drawTopBar(DrawContext ctx, float mx, float my, float o, int acc, int acc2, float availW) {
        float h = 22, y = 6;
        // логотип слева
        float lx = 14;
        float lw = 98;
        Draw.shadow(ctx, lx, y, lw, h, 9, 8);
        Draw.roundRect(ctx, lx, y, lw, h, 9, Draw.alpha(C_BODY, o));
        Draw.ring(ctx, lx, y, lw, h, 9, Draw.alpha(0x20FFFFFF, o));
        Draw.roundRect(ctx, lx + 4, y + 4, 14, 14, 5, Draw.alpha(acc, o));
        Draw.roundRect(ctx, lx + 4, y + 11, 14, 7, 5, Draw.alpha(acc2, o * 0.55f));
        Fonts.HEAD.drawCentered(ctx, "C", lx + 11, y + 6, 8f, Draw.alpha(0xFF0B0B10, o));
        Fonts.HEAD.draw(ctx, "Collapse", lx + 23, y + 7, 8.5f, Draw.alpha(C_TEXT, o));
        Draw.gradientH(ctx, lx + 8, y + h - 1.5f, lw - 16, 1.2f, Draw.alpha(acc, o * 0.9f), Draw.alpha(acc2, o * 0.9f));

        // поиск по центру
        float sw = Math.min(200, availW * 0.28f), sx = availW / 2 - sw / 2;
        float sf = smooth("search", searching ? 1f : 0f, 16f);
        Draw.shadow(ctx, sx, y, sw, h, 9, 8);
        Draw.roundRect(ctx, sx, y, sw, h, 9, Draw.alpha(Theme.mix(0xEE101018, 0xEE1A1A26, sf), o));
        Draw.ring(ctx, sx, y, sw, h, 9, Draw.alpha(Theme.mix(0x20FFFFFF, Theme.withAlpha(acc, 200), sf), o));
        Draw.ring(ctx, sx + 9, y + 6.5f, 8, 8, 4, Draw.alpha(C_MUTED, o));
        Draw.roundRect(ctx, sx + 16, y + 13.5f, 3.5f, 1.5f, 0.7f, Draw.alpha(C_MUTED, o));
        boolean blink = searching && (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = search.isEmpty() ? (searching ? (blink ? "|" : "") : "Поиск модулей  (Ctrl+F)")
                : Fonts.MEDIUM.fit(search, sw - 46, 9f) + (blink ? "|" : "");
        Fonts.MEDIUM.draw(ctx, shown, sx + 24, y + 7, 9f, Draw.alpha(search.isEmpty() ? C_MUTED : C_TEXT, o));
        if (!search.isEmpty()) Fonts.BOLD.draw(ctx, "×", sx + sw - 13, y + 5, 11f, Draw.alpha(C_SUB, o));
        hits.add(new Hit(sx, y, sw, h, H_SEARCH, null, 0, 1));

        // кнопки справа
        float bx = availW - 14;
        bx = topButton(ctx, mx, my, o, acc, bx, y, h, "Тема: " + Theme.name(), 0, true);
        bx = topButton(ctx, mx, my, o, acc, bx - 6, y, h, "Настройки", 1, false);
        topButton(ctx, mx, my, o, acc, bx - 6, y, h, "Профили", 2, false);
    }

    /** Рисует кнопку справа налево. Возвращает левый край. */
    private float topButton(DrawContext ctx, float mx, float my, float o, int acc, float right, float y, float h, String label, int id, boolean swatch) {
        float tw = Fonts.BOLD.width(label, 8.5f);
        float w = tw + 20 + (swatch ? 16 : 0);
        float x = right - w;
        boolean active = (id == 1 && overlay == Overlay.SETTINGS) || (id == 2 && overlay == Overlay.PROFILES);
        boolean hv = in(mx, my, x, y, w, h);
        float hvv = smooth("tb" + id, hv ? 1f : 0f, 18f);
        float act = smooth("tba" + id, active ? 1f : 0f, 16f);
        Draw.shadow(ctx, x, y, w, h, 9, 8);
        Draw.roundRect(ctx, x, y, w, h, 9, Draw.alpha(Theme.mix(Theme.mix(C_BODY, 0xEE1C1C28, hvv), Theme.withAlpha(acc, 0x66), act), o));
        Draw.ring(ctx, x, y, w, h, 9, Draw.alpha(Theme.mix(0x20FFFFFF, Theme.withAlpha(acc, 190), act), o));
        float tx = x + 10;
        if (swatch) {
            Draw.circle(ctx, x + 8, y + 6, 10, Draw.alpha(acc, o));
            Draw.circle(ctx, x + 13, y + 6, 10, Draw.alpha(Theme.accent2(), o * 0.9f));
            tx = x + 29;
        }
        Fonts.BOLD.draw(ctx, label, tx, y + 7, 8.5f, Draw.alpha(Theme.mix(C_SUB, C_TEXT, Math.max(hvv, act)), o));
        hits.add(new Hit(x, y, w, h, H_TOPBTN, id, 0, 3));
        return x;
    }

    // ---------- панель категории ----------
    private static final String[] ICONS = {"combat", "movement", "visuals", "player", "misc", "cosmetics"};

    private void drawPanel(DrawContext ctx, float mx, float my, float o, int acc, int acc2, int cat, float availW, float availH, boolean blocked) {
        List<Module> mods = modulesOf(cat);
        if (!search.isEmpty() && mods.isEmpty()) return;

        float x = PX[cat], y = PY[cat];
        float w = PANEL_W;
        boolean collapsed = COLLAPSED[cat] && search.isEmpty();
        float colT = smooth("col" + cat, collapsed ? 0f : 1f, 14f);

        // высота содержимого
        float contentH = 0;
        for (Module m : mods) contentH += ROW_H + settingsBlockH(m);
        float maxBody = Math.max(60, availH - y - HEAD_H - 34);
        float bodyH = Math.min(contentH, maxBody);
        float maxScroll = Math.max(0, contentH - bodyH);
        if (PSCROLL[cat] > maxScroll) PSCROLL[cat] = maxScroll;
        float shownBody = bodyH * colT;
        float totalH = HEAD_H + shownBody;

        // тень/свечение панели
        Draw.shadow(ctx, x, y, w, totalH, 10, 14);
        Draw.glow(ctx, x, y, w, totalH, 8, Draw.alpha(Theme.withAlpha(acc, 22), o));

        // тело
        if (shownBody > 1) {
            Draw.roundRect(ctx, x, y + HEAD_H - 8, w, shownBody + 8, 8, Draw.alpha(C_BODY, o));
        }
        // шапка
        Draw.roundRect(ctx, x, y, w, HEAD_H, 8, Draw.alpha(C_HEAD, o));
        if (shownBody > 1) ctx.fill(Math.round(x), Math.round(y + HEAD_H - 8), Math.round(x + w), Math.round(y + HEAD_H), Draw.alpha(C_HEAD, o));
        // акцентная линия под шапкой
        Draw.gradientH(ctx, x, y + HEAD_H - 1.5f, w, 1.5f, Draw.alpha(acc, o), Draw.alpha(acc2, o));
        Draw.ring(ctx, x, y, w, totalH, 8, Draw.alpha(0x1CFFFFFF, o));

        // иконка + заголовок
        Draw.icon(ctx, ICONS[Math.min(ICONS.length - 1, cat)], x + 13, y + HEAD_H / 2f - 0.5f, 12, Draw.alpha(acc, o));
        Fonts.HEAD.draw(ctx, Module.Category.values()[cat].label, x + 24, y + 6.5f, 8.5f, Draw.alpha(C_TEXT, o));
        int onN = 0;
        for (Module m : mods) if (m.isEnabled()) onN++;
        String cnt = onN + "/" + mods.size();
        Fonts.BOLD.drawRight(ctx, cnt, x + w - 24, y + 7, 7f, Draw.alpha(onN > 0 ? acc : C_MUTED, o));
        // стрелка сворачивания
        Fonts.BOLD.drawCentered(ctx, collapsed ? "+" : "−", x + w - 11, y + 5, 10f, Draw.alpha(C_SUB, o));
        hits.add(new Hit(x, y, w, HEAD_H, H_HEAD, cat, 0, 3));

        if (shownBody < 2) return;

        // содержимое с обрезкой
        float top = y + HEAD_H;
        ctx.enableScissor(Math.round(x * sc), Math.round(top * sc), Math.round((x + w) * sc), Math.round((top + shownBody) * sc));
        float cy = top - PSCROLL[cat];
        boolean viewport = !blocked;
        for (Module m : mods) {
            float rowTop = cy;
            drawModuleRow(ctx, mx, my, o, acc, acc2, m, x, cy, w, top, shownBody, viewport);
            cy += ROW_H;
            float sh = settingsBlockH(m);
            if (sh > 0.5f) {
                drawSettingsBlock(ctx, mx, my, o, acc, m, x, cy, w, sh, top, shownBody, viewport);
                cy += sh;
            }
        }
        ctx.disableScissor();

        // скроллбар
        if (maxScroll > 1) {
            float th = Math.max(14, shownBody * bodyH / contentH);
            float ty = top + (shownBody - th) * (PSCROLL[cat] / maxScroll);
            Draw.roundRect(ctx, x + w - 3, ty, 1.5f, th, 0.7f, Draw.alpha(0x55FFFFFF, o));
        }
    }

    private float settingsBlockH(Module m) {
        float t = expandT(m);
        if (t < 0.01f) return 0;
        float full = 4;
        for (Setting<?> s : m.getSettings()) full += settingHeight(s);
        if (m.getSettings().isEmpty()) full = 18;
        return full * Anim.ease(t);
    }

    private void drawModuleRow(DrawContext ctx, float mx, float my, float o, int acc, int acc2, Module m,
                               float x, float y, float w, float viewY, float viewH, boolean interactive) {
        if (y + ROW_H < viewY - 1 || y > viewY + viewH + 1) {
            // всё равно регистрируем анимации, чтобы они не сбрасывались
            smooth(m, m.isEnabled() ? 1f : 0f, 14f);
            return;
        }
        boolean inView = my >= viewY && my <= viewY + viewH;
        boolean hv = interactive && inView && in(mx, my, x, y, w, ROW_H);
        float on = smooth(m, m.isEnabled() ? 1f : 0f, 14f);
        float hvv = smooth("rh" + m.getName(), hv ? 1f : 0f, 18f);
        float ex = expandT(m);

        if (on > 0.01f) {
            // градиентная подсветка включённого модуля
            Draw.gradientH(ctx, x + 2, y, w - 4, ROW_H, Draw.alpha(Theme.withAlpha(acc, 70), on * o), Draw.alpha(Theme.withAlpha(acc2, 18), on * o));
            Draw.roundRect(ctx, x + 1, y + 2.5f, 2, ROW_H - 5, 1, Draw.alpha(acc, on * o));
        }
        if (hvv > 0.01f) ctx.fill(Math.round(x + 2), Math.round(y), Math.round(x + w - 2), Math.round(y + ROW_H), Draw.alpha(C_ROW_H, hvv * o));

        boolean isBind = binding == m;
        int tc = Theme.mix(Theme.mix(C_SUB, C_TEXT, hvv), 0xFFFFFFFF, on);
        String label = isBind ? "бинд..." : m.getName();
        float nameMax = w - 40 - (m.getBind() > 0 ? 24 : 0);
        Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(label, nameMax, 8.8f), x + 9, y + 4.5f, 8.8f, Draw.alpha(isBind ? acc : tc, o));

        // чип бинда
        if (m.getBind() > 0 && !isBind) {
            String bt = keyName(m.getBind());
            float bw = Fonts.BOLD.width(bt, 6.2f) + 7;
            float bx = x + w - 18 - bw;
            Draw.roundRect(ctx, bx, y + 3.5f, bw, 9, 4.5f, Draw.alpha(0x30FFFFFF, o));
            Fonts.BOLD.drawCentered(ctx, bt, bx + bw / 2, y + 5.3f, 6.2f, Draw.alpha(Theme.mix(C_SUB, acc, on), o));
        }
        // индикатор: стрелка настроек (если есть настройки)
        if (!m.getSettings().isEmpty()) {
            float ax = x + w - 10, ay = y + ROW_H / 2f;
            Fonts.BOLD.drawCentered(ctx, ex > 0.5f ? "˅" : "˃", ax, ay - 4.5f, 8f, Draw.alpha(Theme.mix(C_MUTED, acc, Math.max(on * 0.6f, ex)), o));
        } else {
            Draw.circle(ctx, x + w - 12, y + ROW_H / 2f - 2, 4, Draw.alpha(Theme.mix(C_OFF, acc, on), o));
        }
        hits.add(new Hit(x, y, w, ROW_H, H_MOD, m, 0, 7));
    }

    private float settingHeight(Setting<?> s) {
        float base = s instanceof NumSetting ? 26 : 18;
        if (s instanceof ModeSetting ms && openDrop == ms) base += ms.getModes().length * 15 + 3;
        return base;
    }

    private void drawSettingsBlock(DrawContext ctx, float mx, float my, float o, int acc, Module m,
                                   float x, float y, float w, float h, float viewY, float viewH, boolean interactive) {
        ctx.fill(Math.round(x + 2), Math.round(y), Math.round(x + w - 2), Math.round(y + h), Draw.alpha(C_SET_BG, o));
        Draw.roundRect(ctx, x + 2, y, 1.5f, h, 0.7f, Draw.alpha(Theme.withAlpha(acc, 140), o));
        if (m.getSettings().isEmpty()) {
            if (h > 10) Fonts.MEDIUM.draw(ctx, "Нет настроек", x + 12, y + 5, 7.5f, Draw.alpha(C_MUTED, o));
            return;
        }
        if (h < 14) return;
        float cy = y + 2;
        for (Setting<?> s : m.getSettings()) {
            float sh = settingHeight(s);
            if (cy + sh >= viewY - 1 && cy <= viewY + viewH + 1) {
                drawSetting(ctx, mx, my, s, x + 10, cy, w - 18, viewY, viewH, o, acc, interactive);
            }
            cy += sh;
        }
    }

    private void drawSetting(DrawContext ctx, float mx, float my, Setting<?> s, float x, float y, float w,
                             float viewY, float viewH, float o, int acc, boolean interactive) {
        boolean vis = interactive && my >= viewY && my <= viewY + viewH;
        if (s instanceof BoolSetting b) {
            float t = smooth(b, b.get() ? 1f : 0f, 16f);
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w - 30, 8f), x, y + 5, 8f, Draw.alpha(Theme.mix(C_SUB, C_TEXT, t), o));
            float tw = 18, th = 9, tx = x + w - tw, ty = y + 4.5f;
            Draw.roundRect(ctx, tx, ty, tw, th, 4.5f, Draw.alpha(Theme.mix(C_OFF, acc, t), o));
            Draw.circle(ctx, tx + 1.2f + (tw - th) * t, ty + 1.2f, th - 2.4f, Draw.alpha(0xFFFFFFFF, o));
            hits.add(new Hit(x - 4, y, w + 8, 18, H_SET, s, 0, 1));
        } else if (s instanceof ColorSetting cs) {
            Fonts.MEDIUM.draw(ctx, s.getName(), x, y + 5, 8f, Draw.alpha(C_SUB, o));
            Draw.roundRect(ctx, x + w - 22, y + 3.5f, 22, 11, 5.5f, Draw.alpha(0xFF2A2A37, o));
            Draw.roundRect(ctx, x + w - 21, y + 4.5f, 20, 9, 4.5f, Draw.alpha(cs.get(), o));
            hits.add(new Hit(x - 4, y, w + 8, 18, H_COLOR, s, 0, 3));
        } else if (s instanceof ModeSetting ms) {
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w * 0.42f, 8f), x, y + 5, 8f, Draw.alpha(C_SUB, o));
            float dw = Math.min(w * 0.56f, 70), dx = x + w - dw;
            boolean open = openDrop == ms;
            boolean hov = vis && in(mx, my, dx, y + 1.5f, dw, 15);
            Draw.roundRect(ctx, dx, y + 1.5f, dw, 15, 6, Draw.alpha(hov || open ? 0xFF262634 : C_FIELD, o));
            if (open) Draw.ring(ctx, dx, y + 1.5f, dw, 15, 6, Draw.alpha(Theme.withAlpha(acc, 160), o));
            Fonts.BOLD.draw(ctx, Fonts.BOLD.fit(ms.get(), dw - 18, 7.5f), dx + 6, y + 5.5f, 7.5f, Draw.alpha(C_TEXT, o));
            Fonts.BOLD.drawRight(ctx, open ? "˄" : "˅", dx + dw - 5, y + 4.5f, 7.5f, Draw.alpha(C_MUTED, o));
            hits.add(new Hit(dx, y + 1.5f, dw, 15, H_SET, ms, 1, 3));
            if (open) {
                float oy = y + 19;
                Draw.roundRect(ctx, dx, oy - 1.5f, dw, ms.getModes().length * 15 + 3, 6, Draw.alpha(0xFF1F1F2C, o));
                Draw.ring(ctx, dx, oy - 1.5f, dw, ms.getModes().length * 15 + 3, 6, Draw.alpha(0x20FFFFFF, o));
                int idx = 0;
                for (String opt : ms.getModes()) {
                    boolean sel = opt.equals(ms.get());
                    boolean ho = vis && in(mx, my, dx, oy, dw, 14);
                    if (ho) Draw.roundRect(ctx, dx + 2, oy, dw - 4, 14, 5, Draw.alpha(0xFF2C2C3B, o));
                    Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(opt, dw - 20, 7.5f), dx + 7, oy + 4, 7.5f, Draw.alpha(sel ? acc : C_TEXT, o));
                    if (sel) Draw.circle(ctx, dx + dw - 10, oy + 5, 4, Draw.alpha(acc, o));
                    hits.add(new Hit(dx, oy, dw, 14, H_OPT, ms, idx++, 1));
                    oy += 15;
                }
            }
        } else if (s instanceof NumSetting ns) {
            float p = smooth(ns, (float) ns.getPercent(), 22f);
            String v = (ns.get() % 1 == 0) ? String.valueOf(ns.get().intValue()) : String.format(java.util.Locale.US, "%.2f", ns.get());
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w - 34, 8f), x, y + 3, 8f, Draw.alpha(C_SUB, o));
            Fonts.BOLD.drawRight(ctx, v, x + w, y + 3, 8f, Draw.alpha(C_TEXT, o));
            float bw = w, by = y + 17;
            Draw.roundRect(ctx, x, by, bw, 3.5f, 1.7f, Draw.alpha(C_OFF, o));
            float fw = Math.max(3.5f, bw * p);
            Draw.gradientH(ctx, x, by, fw, 3.5f, Draw.alpha(acc, o), Draw.alpha(Theme.accent2(), o));
            boolean dr = dragging == ns;
            Draw.glow(ctx, x + bw * p - 4, by - 2.5f, 8, 8, 4, Draw.alpha(Theme.withAlpha(acc, dr ? 100 : 40), o));
            Draw.circle(ctx, x + bw * p - 4, by - 2.2f, dr ? 8.5f : 7.5f, Draw.alpha(0xFFFFFFFF, o));
            hits.add(new Hit(x - 4, y + 10, bw + 8, 14, H_SLIDER, ns, 0, 1));
            dragXFor.put(ns, new float[]{x, bw});
        }
    }

    private final Map<NumSetting, float[]> dragXFor = new HashMap<>();

    // ---------- оверлеи ----------
    private float[] overlayRect(float availW, float availH, float w, float h) {
        float x = availW / 2 - w / 2, y = availH / 2 - h / 2;
        return new float[]{x, y, w, h};
    }

    private void overlayFrame(DrawContext ctx, float mx, float my, float o, int acc, int acc2, float x, float y, float w, float h, String title) {
        // затемнение за оверлеем + клик вне закрывает
        ctx.fill(0, 0, width, height, Draw.alpha(0x88000000, o));
        hits.add(new Hit(0, 0, width / sc, height / sc, H_OVERLAY_CLOSE, null, 0, 1));
        Draw.shadow(ctx, x, y, w, h, 14, 18);
        Draw.glow(ctx, x, y, w, h, 10, Draw.alpha(Theme.withAlpha(acc, 34), o));
        Draw.roundRect(ctx, x, y, w, h, 12, Draw.alpha(0xF50C0C12, o));
        Draw.ring(ctx, x, y, w, h, 12, Draw.alpha(0x22FFFFFF, o));
        Draw.gradientH(ctx, x + 12, y + 28, w - 24, 1.2f, Draw.alpha(acc, o * 0.9f), Draw.alpha(acc2, o * 0.2f));
        Fonts.HEAD.draw(ctx, title, x + 14, y + 11, 10.5f, Draw.alpha(C_TEXT, o));
        boolean ch = in(mx, my, x + w - 26, y + 7, 16, 16);
        Draw.circle(ctx, x + w - 26, y + 7, 16, Draw.alpha(ch ? 0xFF2A2A38 : 0xFF1E1E29, o));
        Fonts.BOLD.drawCentered(ctx, "×", x + w - 18, y + 10, 10f, Draw.alpha(C_SUB, o));
        hits.add(new Hit(x + w - 26, y + 7, 16, 16, H_OVERLAY_CLOSE, null, 0, 1));
        // клики внутри окна не закрывают
        hits.add(new Hit(x, y, w, h, H_NOP, null, 0, 7));
    }

    private void drawSettingsOverlay(DrawContext ctx, float mx, float my, float o, int acc, int acc2, float availW, float availH) {
        float w = Math.min(300, availW - 20), h = Math.min(290, availH - 30);
        float[] r = overlayRect(availW, availH, w, h);
        float x = r[0], y = r[1];
        overlayFrame(ctx, mx, my, o, acc, acc2, x, y, w, h, "Настройки клиента");

        float ax = x + 14, aw = w - 28, ay = y + 36, ah = h - 46;
        ctx.enableScissor(Math.round(ax * sc), Math.round(ay * sc), Math.round((ax + aw) * sc), Math.round((ay + ah) * sc));
        float cy = ay - overlayScroll;
        cy = group(ctx, "Интерфейс", ax, cy, o);
        cy = csToggle(ctx, mx, my, o, acc, "Затемнять фон", "dim", ClientSettings.dimBackground, ax, cy, aw);
        cy = csToggle(ctx, mx, my, o, acc, "Подсказки внизу", "hints", ClientSettings.showHints, ax, cy, aw);
        cy = csToggle(ctx, mx, my, o, acc, "Переливающийся акцент", "animtheme", ClientSettings.animatedTheme, ax, cy, aw);
        cy = csStepper(ctx, mx, my, o, acc, "Масштаб меню", "scale", String.format(java.util.Locale.US, "%.0f%%", ClientSettings.menuScale * 100), ax, cy, aw);
        cy = csStepper(ctx, mx, my, o, acc, "Скорость анимаций", "anim", String.format(java.util.Locale.US, "x%.1f", ClientSettings.animSpeed), ax, cy, aw);
        cy += 4;
        cy = group(ctx, "Список модулей (HUD)", ax, cy, o);
        cy = csToggle(ctx, mx, my, o, acc, "Показывать список", "al", ClientSettings.arrayList, ax, cy, aw);
        cy = csToggle(ctx, mx, my, o, acc, "Подложка", "albg", ClientSettings.arrayListBg, ax, cy, aw);
        String[] posN = {"Справа сверху", "Слева сверху", "Справа снизу", "Слева снизу"};
        cy = csStepper(ctx, mx, my, o, acc, "Позиция", "alpos", posN[Math.floorMod(ClientSettings.arrayListPos, 4)], ax, cy, aw);
        cy += 4;
        cy = group(ctx, "Панели", ax, cy, o);
        {
            float rh = 24;
            boolean hv = in(mx, my, ax, cy, aw, rh);
            float hvv = smooth("csreset", hv ? 1f : 0f, 18f);
            Draw.roundRect(ctx, ax, cy, aw, rh, 8, Draw.alpha(Theme.mix(0xFF14141C, Theme.withAlpha(acc, 70), hvv), o));
            Draw.ring(ctx, ax, cy, aw, rh, 8, Draw.alpha(Theme.withAlpha(acc, 90), o));
            Fonts.BOLD.drawCentered(ctx, "Сбросить раскладку панелей", ax + aw / 2, cy + 8, 8.5f, Draw.alpha(Theme.mix(C_SUB, acc, hvv), o));
            hits.add(new Hit(ax, cy, aw, rh, H_CSET, "resetlayout", 0, 1));
            cy += rh + 7;
        }
        cy = group(ctx, "Тема", ax, cy, o);
        int n = Theme.count();
        int perRow = 4;
        float sw = (aw - (perRow - 1) * 6) / perRow;
        for (int i = 0; i < n; i++) {
            int rr = i / perRow, cc = i % perRow;
            float sx = ax + cc * (sw + 6), sy = cy + rr * 38;
            boolean sel = Theme.index() == i;
            boolean hv = in(mx, my, sx, sy, sw, 32);
            float s2 = smooth("th" + i, sel ? 1f : (hv ? 0.4f : 0f), 16f);
            Draw.roundRect(ctx, sx, sy, sw, 32, 9, Draw.alpha(0xFF14141C, o));
            Draw.roundRect(ctx, sx + 4, sy + 4, sw - 8, 14, 6, Draw.alpha(Theme.accentOf(i), o));
            Draw.roundRect(ctx, sx + 4 + (sw - 8) / 2, sy + 4, (sw - 8) / 2, 14, 6, Draw.alpha(Theme.secondOf(i), o));
            Fonts.MEDIUM.drawCentered(ctx, Theme.NAMES[i], sx + sw / 2, sy + 21, 7f, Draw.alpha(Theme.mix(C_MUTED, C_TEXT, s2), o));
            if (s2 > 0.3f) Draw.ring(ctx, sx, sy, sw, 32, 9, Draw.alpha(Theme.withAlpha(Theme.accentOf(i), (int) (230 * s2)), o));
            hits.add(new Hit(sx, sy, sw, 32, H_THEME, null, i, 1));
        }
        cy += ((n + perRow - 1) / perRow) * 38 + 4;
        ctx.disableScissor();
        overlayMax = Math.max(0, cy + overlayScroll - (ay + ah));
        if (overlayScroll > overlayMax) overlayScroll = overlayMax;
    }

    private float group(DrawContext ctx, String t, float x, float y, float o) {
        Fonts.BOLD.draw(ctx, t.toUpperCase(), x + 2, y + 2, 7f, Draw.alpha(C_MUTED, o));
        return y + 14;
    }

    private float csToggle(DrawContext ctx, float mx, float my, float o, int acc, String label, String key, boolean v, float x, float y, float w) {
        float rh = 24;
        boolean hv = in(mx, my, x, y, w, rh);
        float hvv = smooth("cs" + key, hv ? 1f : 0f, 18f);
        float t = smooth("cst" + key, v ? 1f : 0f, 16f);
        Draw.roundRect(ctx, x, y, w, rh, 8, Draw.alpha(Theme.mix(0xFF14141C, 0xFF1A1A24, hvv), o));
        Fonts.MEDIUM.draw(ctx, label, x + 10, y + 8, 8.8f, Draw.alpha(C_TEXT, o));
        float tw = 24, th = 12, tx = x + w - tw - 9, ty = y + 6;
        Draw.roundRect(ctx, tx, ty, tw, th, 6, Draw.alpha(Theme.mix(C_OFF, acc, t), o));
        Draw.circle(ctx, tx + 1.5f + (tw - th) * t, ty + 1.5f, th - 3, Draw.alpha(0xFFFFFFFF, o));
        hits.add(new Hit(x, y, w, rh, H_CSET, key, 0, 1));
        return y + rh + 3;
    }

    private float csStepper(DrawContext ctx, float mx, float my, float o, int acc, String label, String key, String value, float x, float y, float w) {
        float rh = 24;
        Draw.roundRect(ctx, x, y, w, rh, 8, Draw.alpha(0xFF14141C, o));
        Fonts.MEDIUM.draw(ctx, label, x + 10, y + 8, 8.8f, Draw.alpha(C_TEXT, o));
        float bw = 17, vx = x + w - 9;
        float vw = Math.max(58, Fonts.BOLD.width(value, 8.5f) + 8);
        float rx = vx - bw, vxx = rx - vw, lx = vxx - bw;
        boolean hl = in(mx, my, lx, y + 3, bw, 18), hr = in(mx, my, rx, y + 3, bw, 18);
        Draw.roundRect(ctx, lx, y + 3, bw, 18, 7, Draw.alpha(hl ? 0xFF2A2A38 : C_FIELD, o));
        Draw.roundRect(ctx, rx, y + 3, bw, 18, 7, Draw.alpha(hr ? 0xFF2A2A38 : C_FIELD, o));
        Fonts.BOLD.drawCentered(ctx, "‹", lx + bw / 2, y + 7, 10f, Draw.alpha(C_SUB, o));
        Fonts.BOLD.drawCentered(ctx, "›", rx + bw / 2, y + 7, 10f, Draw.alpha(C_SUB, o));
        Fonts.BOLD.drawCentered(ctx, value, vxx + vw / 2, y + 8, 8.5f, Draw.alpha(acc, o));
        hits.add(new Hit(lx, y + 3, bw, 18, H_CSTEP, key, -1, 1));
        hits.add(new Hit(rx, y + 3, bw, 18, H_CSTEP, key, 1, 1));
        return y + rh + 3;
    }

    private void drawProfilesOverlay(DrawContext ctx, float mx, float my, float o, int acc, int acc2, float availW, float availH) {
        float w = Math.min(300, availW - 20), h = Math.min(270, availH - 30);
        float[] r = overlayRect(availW, availH, w, h);
        float x = r[0], y = r[1];
        overlayFrame(ctx, mx, my, o, acc, acc2, x, y, w, h, "Профили");

        float ax = x + 14, aw = w - 28, ay = y + 36;
        float fh = 26, bw = 80, fw = aw - bw - 8;
        float fa = smooth("pname", typingProfile ? 1f : 0f, 16f);
        Draw.roundRect(ctx, ax, ay, fw, fh, 9, Draw.alpha(Theme.mix(C_FIELD, 0xFF22222E, fa), o));
        Draw.ring(ctx, ax, ay, fw, fh, 9, Draw.alpha(Theme.mix(0x14FFFFFF, Theme.withAlpha(acc, 190), fa), o));
        boolean blink = typingProfile && (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = profileName.isEmpty() ? (typingProfile ? (blink ? "|" : "") : "Имя нового профиля") : profileName + (blink ? "|" : "");
        Fonts.MEDIUM.draw(ctx, shown, ax + 11, ay + 9, 9f, Draw.alpha(profileName.isEmpty() ? C_MUTED : C_TEXT, o));
        hits.add(new Hit(ax, ay, fw, fh, H_PNAME, null, 0, 1));
        boolean hs = in(mx, my, ax + fw + 8, ay, bw, fh);
        Draw.roundRect(ctx, ax + fw + 8, ay, bw, fh, 9, Draw.alpha(hs ? Theme.mix(acc, 0xFFFFFFFF, 0.15f) : acc, o));
        Fonts.BOLD.drawCentered(ctx, "Сохранить", ax + fw + 8 + bw / 2, ay + 9, 8.5f, Draw.alpha(0xFF0B0B10, o));
        hits.add(new Hit(ax + fw + 8, ay, bw, fh, H_PSAVE, null, 0, 1));

        List<String> ps = Config.profiles();
        float ly = ay + fh + 12;
        Fonts.BOLD.draw(ctx, "СОХРАНЁННЫЕ  ·  активный: " + Config.currentProfile, ax + 2, ly - 2, 7f, Draw.alpha(C_MUTED, o));
        ly += 11;
        float viewH = y + h - ly - 10;
        ctx.enableScissor(Math.round(ax * sc), Math.round(ly * sc), Math.round((ax + aw) * sc), Math.round((ly + viewH) * sc));
        float cy = ly - overlayScroll;
        if (ps.isEmpty()) {
            Fonts.MEDIUM.drawCentered(ctx, "Профилей пока нет. Введите имя и нажмите «Сохранить».", ax + aw / 2, ly + 30, 8.5f, Draw.alpha(C_MUTED, o));
        }
        for (String p : ps) {
            float rh = 28;
            boolean act = p.equals(Config.currentProfile);
            boolean hv = in(mx, my, ax, cy, aw, rh) && my >= ly && my <= ly + viewH;
            float hvv = smooth("pr" + p, hv ? 1f : 0f, 18f);
            Draw.roundRect(ctx, ax, cy, aw, rh, 9, Draw.alpha(Theme.mix(0xFF14141C, 0xFF1A1A24, hvv), o));
            if (act) Draw.ring(ctx, ax, cy, aw, rh, 9, Draw.alpha(Theme.withAlpha(acc, 140), o));
            Draw.circle(ctx, ax + 10, cy + 10, 8, Draw.alpha(act ? acc : C_OFF, o));
            Fonts.BOLD.draw(ctx, Fonts.BOLD.fit(p, aw - 160, 9.2f), ax + 26, cy + 9.5f, 9.2f, Draw.alpha(C_TEXT, o));
            float bx = ax + aw - 8 - 52;
            boolean hd = in(mx, my, bx, cy + 5, 52, 18);
            Draw.roundRect(ctx, bx, cy + 5, 52, 18, 7, Draw.alpha(hd ? 0xFF4A2530 : 0xFF2A1D24, o));
            Fonts.BOLD.drawCentered(ctx, "Удалить", bx + 26, cy + 10, 7.5f, Draw.alpha(0xFFFF6B7D, o));
            hits.add(new Hit(bx, cy + 5, 52, 18, H_PDEL, p, 0, 1));
            bx -= 58;
            boolean hl = in(mx, my, bx, cy + 5, 52, 18);
            Draw.roundRect(ctx, bx, cy + 5, 52, 18, 7, Draw.alpha(hl ? Theme.withAlpha(acc, 110) : Theme.withAlpha(acc, 60), o));
            Fonts.BOLD.drawCentered(ctx, "Загрузить", bx + 26, cy + 10, 7.5f, Draw.alpha(acc, o));
            hits.add(new Hit(bx, cy + 5, 52, 18, H_PLOAD, p, 0, 1));
            cy += rh + 5;
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
        boolean wasOpenDrop = openDrop != null;
        searching = false; typingProfile = false;

        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!in(mx, my, h.x, h.y, h.w, h.h)) continue;
            // при открытом оверлее панели недоступны (оверлей-хиты добавлены позже, поэтому проверяются первыми)
            if ((h.btnMask & bit) == 0) {
                if (h.type == H_NOP || h.type == H_OVERLAY_CLOSE) return true;
                continue;
            }
            switch (h.type) {
                case H_NOP -> { return true; }
                case H_OVERLAY_CLOSE -> { overlay = Overlay.NONE; overlayScroll = 0; typingProfile = false; return true; }
                case H_HEAD -> {
                    int cat = (Integer) h.a;
                    if (btn == 1) { COLLAPSED[cat] = !COLLAPSED[cat]; }
                    else { draggingPanel = cat; dragOffX = mx - PX[cat]; dragOffY = my - PY[cat]; }
                    return true;
                }
                case H_MOD -> {
                    Module m = (Module) h.a;
                    openDrop = null;
                    if (btn == 2) binding = m;
                    else if (btn == 1) { EXPANDED.put(m, !EXPANDED.getOrDefault(m, false)); }
                    else m.toggle();
                    return true;
                }
                case H_SEARCH -> {
                    if (!search.isEmpty() && mx > h.x + h.w - 20) search = "";
                    else searching = true;
                    return true;
                }
                case H_TOPBTN -> {
                    int id = (Integer) h.a;
                    if (id == 0) { if (btn == 1) Theme.prev(); else Theme.next(); }
                    else if (id == 1) { overlay = overlay == Overlay.SETTINGS ? Overlay.NONE : Overlay.SETTINGS; overlayScroll = 0; }
                    else { overlay = overlay == Overlay.PROFILES ? Overlay.NONE : Overlay.PROFILES; overlayScroll = 0; }
                    return true;
                }
                case H_OPT -> {
                    ModeSetting ms = (ModeSetting) h.a;
                    ms.set(ms.getModes()[h.b]);
                    openDrop = null;
                    return true;
                }
                case H_SET -> {
                    Setting<?> s = (Setting<?>) h.a;
                    if (s instanceof BoolSetting b) { b.toggle(); openDrop = null; }
                    else if (s instanceof ModeSetting ms) {
                        if (btn == 1) { ms.next(); openDrop = null; }
                        else openDrop = (openDrop == ms) ? null : ms;
                    }
                    return true;
                }
                case H_COLOR -> {
                    ColorSetting cs = (ColorSetting) h.a;
                    if (btn == 1) cs.prev(); else cs.next();
                    return true;
                }
                case H_CSET -> { clientToggle((String) h.a); return true; }
                case H_THEME -> { Theme.setIndex(h.b); return true; }
                case H_CSTEP -> { clientStep((String) h.a, h.b); return true; }
                case H_SLIDER -> {
                    dragging = (NumSetting) h.a;
                    float[] d = dragXFor.get(dragging);
                    dragX = d != null ? d[0] : h.x + 4;
                    dragW = d != null ? d[1] : h.w - 8;
                    applyDrag(mx);
                    return true;
                }
                case H_PNAME -> { typingProfile = true; return true; }
                case H_PSAVE -> {
                    String n = profileName.trim();
                    if (n.isEmpty()) { toast("Введите имя профиля"); typingProfile = true; return true; }
                    if (Config.saveProfile(n)) { toast("Профиль «" + n + "» сохранён"); profileName = ""; }
                    else toast("Не удалось сохранить");
                    return true;
                }
                case H_PLOAD -> { String n = (String) h.a; toast(Config.loadProfile(n) ? "Загружен «" + n + "»" : "Ошибка загрузки"); return true; }
                case H_PDEL -> { String n = (String) h.a; Config.deleteProfile(n); toast("Удалён «" + n + "»"); return true; }
                default -> { return true; }
            }
        }
        if (wasOpenDrop) openDrop = null;
        return true;
    }

    private void clientToggle(String k) {
        switch (k) {
            case "dim" -> ClientSettings.dimBackground = !ClientSettings.dimBackground;
            case "hints" -> ClientSettings.showHints = !ClientSettings.showHints;
            case "animtheme" -> { ClientSettings.animatedTheme = !ClientSettings.animatedTheme; Theme.animated = ClientSettings.animatedTheme; }
            case "al" -> ClientSettings.arrayList = !ClientSettings.arrayList;
            case "albg" -> ClientSettings.arrayListBg = !ClientSettings.arrayListBg;
            case "resetlayout" -> {
                java.util.Arrays.fill(COLLAPSED, false);
                java.util.Arrays.fill(PSCROLL, 0f);
                autoLayout(width / sc);
                toast("Раскладка сброшена");
            }
            default -> {}
        }
    }

    private void clientStep(String k, int d) {
        switch (k) {
            case "scale" -> {
                ClientSettings.menuScale = Math.max(0.7f, Math.min(1.4f, Math.round((ClientSettings.menuScale + d * 0.1f) * 10) / 10f));
                sc = ClientSettings.menuScale;
                autoLayout(width / sc);
            }
            case "anim" -> ClientSettings.animSpeed = Math.max(0.5f, Math.min(2.5f, Math.round((ClientSettings.animSpeed + d * 0.25f) * 100) / 100f));
            case "alpos" -> ClientSettings.arrayListPos = Math.floorMod(ClientSettings.arrayListPos + d, 4);
            default -> {}
        }
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
        if (draggingPanel >= 0) {
            float availW = width / sc, availH = height / sc;
            PX[draggingPanel] = Math.max(-PANEL_W + 30, Math.min(availW - 30, mx - dragOffX));
            PY[draggingPanel] = Math.max(0, Math.min(availH - HEAD_H - 4, my - dragOffY));
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(Click c) { dragging = null; draggingPanel = -1; return true; }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        float mx = (float) x / sc, my = (float) y / sc;
        if (overlay != Overlay.NONE) {
            overlayScroll = (float) Math.max(0, Math.min(overlayMax, overlayScroll - v * 24));
            return true;
        }
        // панель под курсором
        int n = Module.Category.values().length;
        for (int i = n - 1; i >= 0; i--) {
            List<Module> mods = modulesOf(i);
            if (mods.isEmpty() && !search.isEmpty()) continue;
            if (COLLAPSED[i] && search.isEmpty()) continue;
            float contentH = 0;
            for (Module m : mods) contentH += ROW_H + settingsBlockH(m);
            float availH = height / sc;
            float bodyH = Math.min(contentH, Math.max(60, availH - PY[i] - HEAD_H - 34));
            if (in(mx, my, PX[i], PY[i] + HEAD_H, PANEL_W, bodyH)) {
                float maxScroll = Math.max(0, contentH - bodyH);
                PSCROLL[i] = (float) Math.max(0, Math.min(maxScroll, PSCROLL[i] - v * 22));
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        int cp = input.codepoint();
        if (cp < 32 || cp == 127) return false;
        String ch = new String(Character.toChars(cp));
        if (searching) { if (search.length() < 24) search += ch; return true; }
        if (typingProfile) { if (profileName.length() < 20) profileName += ch; return true; }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int k = input.key();
        if (binding != null) { binding.setBind(k == 256 ? -1 : k); binding = null; return true; }
        if (searching) {
            if (k == 259 && !search.isEmpty()) search = search.substring(0, search.length() - 1);
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
        if (k == 256 && openDrop != null) { openDrop = null; return true; }
        if (k == 256 && overlay != Overlay.NONE) { overlay = Overlay.NONE; overlayScroll = 0; return true; }
        if (k == 344 || k == 256) { this.close(); return true; }
        if (k == 70 && (input.modifiers() & 2) != 0) { searching = true; return true; } // Ctrl+F
        return super.keyPressed(input);
    }

    @Override public boolean shouldPause() { return false; }
}
