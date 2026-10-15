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
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Меню Collapse (современный стиль): одно окно, слева навигация по категориям,
 * по центру сетка карточек модулей, справа панель настроек выбранного модуля.
 * Во вкладке «Косметика» справа показывается живое превью твоего персонажа.
 */
public class ClickMenuScreen extends Screen {
    // ---- палитра ----
    private static final int C_WIN = 0xF2090910;
    private static final int C_SIDE = 0xFF0C0C13;
    private static final int C_CARD = 0xFF13131B;
    private static final int C_CARD_H = 0xFF191924;
    private static final int C_TEXT = 0xFFF2F2F8;
    private static final int C_SUB = 0xFFA0A0B6;
    private static final int C_MUTED = 0xFF646479;
    private static final int C_FIELD = 0xFF1A1A24;
    private static final int C_OFF = 0xFF2A2A38;
    private static final int C_LINE = 0x14FFFFFF;

    // ---- состояние (static: переживает закрытие меню) ----
    private static int tab = 0;                 // индекс категории; = число категорий -> «Избранное»
    private static Module selected = null;      // модуль, чьи настройки открыты справа
    private static float winX = Float.NaN, winY = Float.NaN;
    private static final float[] TSCROLL = new float[16];
    private static float setScroll = 0;

    private enum Overlay { NONE, PROFILES, SETTINGS }
    private Overlay overlay = Overlay.NONE;

    private final Anim openAnim = new Anim(0f, 10f);
    private final Anim tabInd = new Anim(0f, 16f);
    private final Anim panelAnim = new Anim(0f, 13f);
    private final Anim scrollAnim = new Anim(0f, 20f);
    private final Map<Object, Anim> anims = new HashMap<>();

    private Module binding = null;
    private ModeSetting openDrop = null;
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
    private float overlayScroll = 0;
    private float overlayMax = 0;
    private float setMax = 0;
    private float gridMax = 0;

    private float sc;
    private float wx, wy, ww, wh;
    private float gx, gy, gw, gh;   // область сетки
    private float rx, ry, rw, rh;   // правая панель

    private record Hit(float x, float y, float w, float h, int type, Object a, int b, int btnMask) {}
    private static final int H_TAB = 1, H_MOD = 2, H_SET = 3, H_OPT = 4, H_SLIDER = 5, H_TOPBTN = 6, H_SEARCH = 7,
            H_CSET = 8, H_THEME = 9, H_CSTEP = 10, H_PSAVE = 11, H_PLOAD = 12, H_PDEL = 13, H_PNAME = 14,
            H_OVERLAY_CLOSE = 15, H_COLOR = 16, H_NOP = 17, H_DRAG = 18, H_CLOSEP = 19, H_FAV = 20, H_BIND = 21;
    private final List<Hit> hits = new ArrayList<>();
    private final Map<NumSetting, float[]> dragXFor = new HashMap<>();

    private static final String[] ICONS = {"combat", "movement", "visuals", "player", "misc", "cosmetics"};

    private ClickMenuScreen() { super(Text.literal("Collapse")); }
    public static ClickMenuScreen create() { return new ClickMenuScreen(); }

    private int favTab() { return Module.Category.values().length; }

    @Override
    protected void init() {
        openAnim.snap(0f);
        openAnim.set(1f);
        panelAnim.snap(selected != null ? 1f : 0f);
        tabInd.snap(tab);
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

    private List<Module> list() {
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase();
        for (Module m : ModuleManager.MODULES) {
            if (m.isHidden()) continue;
            if (!q.isEmpty()) {
                if (m.getName().toLowerCase().contains(q) || m.getDescription().toLowerCase().contains(q)) out.add(m);
            } else if (tab == favTab()) {
                if (m.isFavorite()) out.add(m);
            } else if (m.getCategory().ordinal() == tab) out.add(m);
        }
        return out;
    }

    private int countOn(int cat) {
        int on = 0;
        for (Module m : ModuleManager.MODULES) if (!m.isHidden() && m.getCategory().ordinal() == cat && m.isEnabled()) on++;
        return on;
    }

    // ================= отрисовка =================
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        try {
            float o = Anim.ease(openAnim.get());
            if (ClientSettings.dimBackground) {
                ctx.fillGradient(0, 0, width, height, (int) (130 * o) << 24, (int) (200 * o) << 24);
            }
            sc = Math.max(0.7f, Math.min(1.4f, ClientSettings.menuScale));
            float availW = width / sc, availH = height / sc;
            ww = Math.min(availW - 12, 600);
            wh = Math.min(availH - 12, 380);
            if (Float.isNaN(winX)) { winX = (availW - ww) / 2f; winY = (availH - wh) / 2f; }
            winX = Math.max(-ww + 80, Math.min(availW - 80, winX));
            winY = Math.max(0, Math.min(availH - 30, winY));
            wx = winX;
            wy = winY + (1 - o) * 14;
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
        boolean blocked = overlay != Overlay.NONE;
        float mxx = blocked ? -9999 : mx, myy = blocked ? -9999 : my;

        // окно
        Draw.shadow(ctx, wx, wy, ww, wh, 14, 20);
        Draw.glow(ctx, wx, wy, ww, wh, 12, Draw.alpha(Theme.withAlpha(acc, 36), o));
        Draw.roundRect(ctx, wx, wy, ww, wh, 14, Draw.alpha(C_WIN, o));
        Draw.ring(ctx, wx, wy, ww, wh, 14, Draw.alpha(0x22FFFFFF, o));

        float sideW = 116;
        drawSidebar(ctx, mxx, myy, o, acc, acc2, sideW);

        float cx = wx + sideW + 12, cy = wy + 12, cw = ww - sideW - 24;
        drawTopBar(ctx, mxx, myy, o, acc, acc2, cx, cy, cw);

        float areaY = cy + 38;
        float areaH = wy + wh - areaY - (ClientSettings.showHints ? 24 : 12);
        boolean cosTab = tab == Module.Category.COSMETICS.ordinal() && search.isEmpty();
        boolean wantPanel = selected != null || cosTab;
        panelAnim.set(wantPanel ? 1f : 0f);
        float pa = Anim.ease(panelAnim.get());
        float panW = Math.min(196, cw * 0.44f);
        float gap = 10;
        gx = cx; gy = areaY; gw = cw - (panW + gap) * pa; gh = areaH;
        rx = cx + cw - panW * pa; ry = areaY; rw = panW; rh = areaH;

        drawGrid(ctx, mxx, myy, o, acc, acc2);
        if (pa > 0.02f) drawRight(ctx, mxx, myy, o * pa, acc, acc2, cosTab);

        // подсказки
        if (ClientSettings.showHints) {
            String hint = binding != null
                    ? "Нажмите клавишу для «" + binding.getName() + "»   ·   ESC: сбросить"
                    : "ЛКМ: вкл/выкл   ·   ПКМ: настройки   ·   СКМ: бинд   ·   Ctrl+F: поиск   ·   RShift: закрыть";
            Fonts.MEDIUM.draw(ctx, hint, cx, wy + wh - 18, 8f, Draw.alpha(binding != null ? acc : C_MUTED, o));
        }
        if (System.currentTimeMillis() - toastAt < 1800 && !toast.isEmpty()) {
            float tw = Fonts.BOLD.width(toast, 9f) + 24;
            float tx = wx + ww / 2 - tw / 2, ty = wy + wh - 44;
            Draw.shadow(ctx, tx, ty, tw, 20, 8, 8);
            Draw.roundRect(ctx, tx, ty, tw, 20, 10, Draw.alpha(0xFF1D1D29, o));
            Draw.ring(ctx, tx, ty, tw, 20, 10, Draw.alpha(Theme.withAlpha(acc, 120), o));
            Fonts.BOLD.drawCentered(ctx, toast, tx + tw / 2, ty + 6, 9f, Draw.alpha(C_TEXT, o));
        }

        if (overlay == Overlay.SETTINGS) drawSettingsOverlay(ctx, mx, my, o, acc, acc2);
        else if (overlay == Overlay.PROFILES) drawProfilesOverlay(ctx, mx, my, o, acc, acc2);
    }

    // ---------- боковая навигация ----------
    private void drawSidebar(DrawContext ctx, float mx, float my, float o, int acc, int acc2, float sideW) {
        Draw.roundRect(ctx, wx, wy, sideW, wh, 14, Draw.alpha(C_SIDE, o));
        ctx.fill(Math.round(wx + sideW - 14), Math.round(wy) + 1, Math.round(wx + sideW), Math.round(wy + wh) - 1, Draw.alpha(C_SIDE, o));
        ctx.fill(Math.round(wx + sideW), Math.round(wy) + 14, Math.round(wx + sideW) + 1, Math.round(wy + wh) - 14, Draw.alpha(C_LINE, o));
        hits.add(new Hit(wx, wy, sideW, 46, H_DRAG, null, 0, 1));

        float lx = wx + 14, ly = wy + 13;
        Draw.glow(ctx, lx, ly, 22, 22, 6, Draw.alpha(Theme.withAlpha(acc, 70), o));
        Draw.roundGradientV(ctx, lx, ly, 22, 22, 7, Draw.alpha(acc, o), Draw.alpha(acc2, o));
        Fonts.HEAD.drawCentered(ctx, "C", lx + 11, ly + 5, 11f, Draw.alpha(0xFF0B0B10, o));
        Fonts.HEAD.draw(ctx, "Collapse", lx + 29, ly + 2, 9.5f, Draw.alpha(C_TEXT, o));
        Fonts.MEDIUM.draw(ctx, "v0.9.2", lx + 29, ly + 13, 7.5f, Draw.alpha(C_MUTED, o));

        Module.Category[] cats = Module.Category.values();
        float itemH = 23, x = wx + 8, w = sideW - 16, y = wy + 54;
        tabInd.set(tab);
        Fonts.BOLD.draw(ctx, "МОДУЛИ", x + 6, y - 9, 6.5f, Draw.alpha(C_MUTED, o));
        for (int i = 0; i < cats.length; i++) {
            nav(ctx, mx, my, o, acc, x, y + i * itemH, w, itemH - 2, cats[i].label, i, countOn(i), tab == i && search.isEmpty());
        }
        float y2 = y + cats.length * itemH + 12;
        Fonts.BOLD.draw(ctx, "ЛИЧНОЕ", x + 6, y2 - 9, 6.5f, Draw.alpha(C_MUTED, o));
        nav(ctx, mx, my, o, acc, x, y2, w, itemH - 2, "Избранное", favTab(), favCount(), tab == favTab() && search.isEmpty());
    }

    private int favCount() { int n = 0; for (Module m : ModuleManager.MODULES) if (!m.isHidden() && m.isFavorite()) n++; return n; }

    private void nav(DrawContext ctx, float mx, float my, float o, int acc, float x, float y, float w, float h,
                     String label, int id, int badge, boolean sel) {
        boolean hv = in(mx, my, x, y, w, h);
        float s = smooth("nav" + id, sel ? 1f : 0f, 16f);
        float hvv = smooth("navh" + id, hv ? 1f : 0f, 18f);
        if (s > 0.01f) {
            Draw.gradientH(ctx, x + 1, y, w - 2, h, Draw.alpha(Theme.withAlpha(acc, (int) (60 * s)), o), Draw.alpha(Theme.withAlpha(acc, (int) (6 * s)), o));
            Draw.roundRect(ctx, x, y + h / 2 - 6 * s, 3, 12 * s, 1.5f, Draw.alpha(acc, o));
        } else if (hvv > 0.01f) {
            Draw.roundRect(ctx, x, y, w, h, 8, Draw.alpha(Theme.withAlpha(0xFFFFFF, (int) (14 * hvv)), o));
        }
        String ic = id < ICONS.length ? ICONS[id] : "fav";
        Draw.icon(ctx, ic, x + 12, y + h / 2f, 13, Draw.alpha(Theme.mix(C_MUTED, acc, s), o));
        int tc = Theme.mix(Theme.mix(C_SUB, C_TEXT, hvv), C_TEXT, s);
        (s > 0.5f ? Fonts.BOLD : Fonts.MEDIUM).draw(ctx, label, x + 26, y + h / 2f - 4.5f, 9f, Draw.alpha(tc, o));
        if (badge > 0) {
            String b = String.valueOf(badge);
            float bw = Fonts.BOLD.width(b, 7f) + 8;
            Draw.roundRect(ctx, x + w - bw - 4, y + h / 2f - 6, bw, 12, 6, Draw.alpha(Theme.withAlpha(acc, 60), o));
            Fonts.BOLD.drawCentered(ctx, b, x + w - bw / 2 - 4, y + h / 2f - 3.5f, 7f, Draw.alpha(acc, o));
        }
        hits.add(new Hit(x, y, w, h, H_TAB, id, 0, 1));
    }

    // ---------- верхняя панель ----------
    private void drawTopBar(DrawContext ctx, float mx, float my, float o, int acc, int acc2, float cx, float cy, float cw) {
        hits.add(new Hit(cx, wy, cw, 10, H_DRAG, null, 0, 1));
        String title = !search.isEmpty() ? "Поиск"
                : tab == favTab() ? "Избранное" : Module.Category.values()[tab].label;
        Fonts.HEAD.draw(ctx, title, cx, cy + 3, 12f, Draw.alpha(C_TEXT, o));
        List<Module> ls = list();
        int onN = 0;
        for (Module m : ls) if (m.isEnabled()) onN++;
        float tx = cx + Fonts.HEAD.width(title, 12f) + 9;
        Fonts.MEDIUM.draw(ctx, ls.size() + " модулей", tx, cy + 6, 8f, Draw.alpha(C_MUTED, o));
        float px = tx + Fonts.MEDIUM.width(ls.size() + " модулей", 8f) + 9;
        Draw.roundRect(ctx, px, cy + 9, 44, 4, 2, Draw.alpha(0xFF22222E, o));
        if (!ls.isEmpty() && onN > 0) Draw.gradientH(ctx, px, cy + 9, 44f * onN / ls.size(), 4, Draw.alpha(acc, o), Draw.alpha(acc2, o));

        // кнопки справа: Тема, Профили, Настройки, поиск
        float by = cy, bh = 24;
        float right = cx + cw;
        right = topBtn(ctx, mx, my, o, acc, acc2, right, by, bh, "Настройки", 1);
        right = topBtn(ctx, mx, my, o, acc, acc2, right - 5, by, bh, "Профили", 2);
        right = topBtn(ctx, mx, my, o, acc, acc2, right - 5, by, bh, Theme.name(), 0);

        float sw = Math.max(80, Math.min(130, right - 8 - (px + 56) - 6));
        float sx = right - 6 - sw;
        if (sx > px + 56) {
            float sf = smooth("search", searching ? 1f : 0f, 16f);
            Draw.roundRect(ctx, sx, by, sw, bh, 9, Draw.alpha(Theme.mix(C_FIELD, 0xFF22222E, sf), o));
            Draw.ring(ctx, sx, by, sw, bh, 9, Draw.alpha(Theme.mix(0x14FFFFFF, Theme.withAlpha(acc, 190), sf), o));
            Draw.ring(ctx, sx + 9, by + 7.5f, 8, 8, 4, Draw.alpha(C_MUTED, o));
            Draw.roundRect(ctx, sx + 16, by + 15, 3.5f, 1.5f, 0.7f, Draw.alpha(C_MUTED, o));
            boolean blink = searching && (System.currentTimeMillis() / 500) % 2 == 0;
            String shown = search.isEmpty() ? (searching ? (blink ? "|" : "") : "Поиск")
                    : Fonts.MEDIUM.fit(search, sw - 40, 9f) + (blink ? "|" : "");
            Fonts.MEDIUM.draw(ctx, shown, sx + 24, by + 8, 9f, Draw.alpha(search.isEmpty() ? C_MUTED : C_TEXT, o));
            if (!search.isEmpty()) Fonts.BOLD.draw(ctx, "×", sx + sw - 12, by + 6, 11f, Draw.alpha(C_SUB, o));
            hits.add(new Hit(sx, by, sw, bh, H_SEARCH, null, 0, 1));
        }
    }

    /** Кнопка справа налево. Возвращает левый край. */
    private float topBtn(DrawContext ctx, float mx, float my, float o, int acc, int acc2, float right, float y, float h, String label, int id) {
        float tw = Fonts.BOLD.width(label, 8.3f);
        float w = tw + 18 + (id == 0 ? 17 : 0);
        float x = right - w;
        boolean active = (id == 1 && overlay == Overlay.SETTINGS) || (id == 2 && overlay == Overlay.PROFILES);
        boolean hv = in(mx, my, x, y, w, h);
        float hvv = smooth("tb" + id, hv ? 1f : 0f, 18f);
        float act = smooth("tba" + id, active ? 1f : 0f, 16f);
        Draw.roundRect(ctx, x, y, w, h, 9, Draw.alpha(Theme.mix(Theme.mix(C_FIELD, 0xFF232330, hvv), Theme.withAlpha(acc, 0x66), act), o));
        Draw.ring(ctx, x, y, w, h, 9, Draw.alpha(Theme.mix(0x16FFFFFF, Theme.withAlpha(acc, 190), act), o));
        float tx = x + 9;
        if (id == 0) {
            Draw.circle(ctx, x + 7, y + 7, 10, Draw.alpha(acc, o));
            Draw.circle(ctx, x + 12, y + 7, 10, Draw.alpha(acc2, o * 0.9f));
            tx = x + 27;
        }
        Fonts.BOLD.draw(ctx, label, tx, y + 8, 8.3f, Draw.alpha(Theme.mix(C_SUB, C_TEXT, Math.max(hvv, act)), o));
        hits.add(new Hit(x, y, w, h, H_TOPBTN, id, 0, 3));
        return x;
    }

    // ---------- сетка модулей ----------
    private void drawGrid(DrawContext ctx, float mx, float my, float o, int acc, int acc2) {
        List<Module> mods = list();
        int cols = gw >= 330 ? 3 : (gw >= 190 ? 2 : 1);
        float gap = 8, cardH = 58;
        float cwid = (gw - gap * (cols - 1)) / cols;
        int rows = (int) Math.ceil(mods.size() / (double) cols);
        float contentH = rows * (cardH + gap);
        gridMax = Math.max(0, contentH - gh);
        float sc0 = Math.min(TSCROLL[tab], gridMax);
        TSCROLL[tab] = sc0;
        scrollAnim.set(sc0);
        float sy = scrollAnim.get();

        if (mods.isEmpty()) {
            String msg = tab == favTab() && search.isEmpty() ? "Пока пусто. Нажмите ★ на карточке модуля." : "Ничего не найдено";
            float ex = gx + gw / 2, ey = gy + gh / 2;
            Draw.roundRect(ctx, ex - 22, ey - 40, 44, 44, 14, Draw.alpha(0xFF1A1A24, o));
            Draw.icon(ctx, "fav", ex, ey - 18, 22, Draw.alpha(acc, o));
            Fonts.BOLD.drawCentered(ctx, msg, ex, ey + 12, 9.5f, Draw.alpha(C_SUB, o));
            return;
        }
        ctx.enableScissor(Math.round((gx - 4) * sc), Math.round(gy * sc), Math.round((gx + gw + 4) * sc), Math.round((gy + gh) * sc));
        for (int i = 0; i < mods.size(); i++) {
            int r = i / cols, c = i % cols;
            float x = gx + c * (cwid + gap), y = gy + r * (cardH + gap) - sy;
            if (y + cardH < gy - 2 || y > gy + gh + 2) continue;
            drawCard(ctx, mx, my, mods.get(i), x, y, cwid, cardH, o, acc, acc2);
        }
        ctx.disableScissor();
        if (gridMax > 1) {
            float th = Math.max(20, gh * gh / contentH);
            float ty = gy + (gh - th) * (sy / gridMax);
            Draw.roundRect(ctx, gx + gw + 1, ty, 2, th, 1, Draw.alpha(0x40FFFFFF, o));
        }
    }

    private void drawCard(DrawContext ctx, float mx, float my, Module m, float x, float y, float w, float h, float o, int acc, int acc2) {
        boolean visible = my >= gy && my <= gy + gh;
        boolean hv = visible && in(mx, my, x, y, w, h);
        float on = smooth(m, m.isEnabled() ? 1f : 0f, 14f);
        float hvv = smooth("ch" + m.getName(), hv ? 1f : 0f, 18f);
        boolean sel = selected == m;

        if (on > 0.02f) Draw.glow(ctx, x, y, w, h, 5, Draw.alpha(Theme.withAlpha(acc, (int) (28 * on)), o));
        int bg = Theme.mix(C_CARD, C_CARD_H, hvv);
        Draw.roundRect(ctx, x, y, w, h, 10, Draw.alpha(bg, o));
        if (on > 0.02f) {
            // градиентная заливка сверху у включённых
            Draw.roundRect(ctx, x, y, w, h, 10, Draw.alpha(Theme.withAlpha(acc, (int) (22 * on)), o));
        }
        int border = sel ? Theme.withAlpha(acc, 210) : Theme.mix(0x14FFFFFF, Theme.withAlpha(acc, 110), on);
        Draw.ring(ctx, x, y, w, h, 10, Draw.alpha(border, o));
        if (on > 0.02f) Draw.roundRect(ctx, x + 12, y + h - 3.5f, (w - 24) * on, 2, 1, Draw.alpha(acc, o * 0.9f));

        // иконка
        float ti = 24;
        Draw.roundRect(ctx, x + 9, y + 9, ti, ti, 8, Draw.alpha(Theme.mix(0xFF1D1D29, Theme.withAlpha(acc, 64), on), o));
        Draw.icon(ctx, ICONS[Math.min(ICONS.length - 1, m.getCategory().ordinal())], x + 9 + ti / 2f, y + 9 + ti / 2f, 13,
                Draw.alpha(Theme.mix(C_SUB, acc, on), o));

        float tx0 = x + 9 + ti + 7;
        Fonts.BOLD.draw(ctx, Fonts.BOLD.fit(m.getName(), w - (tx0 - x) - 36, 9.5f), tx0, y + 10, 9.5f, Draw.alpha(Theme.mix(C_TEXT, 0xFFFFFFFF, on), o));
        Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(m.getDescription(), w - (tx0 - x) - 8, 7.2f), tx0, y + 23, 7.2f, Draw.alpha(C_MUTED, o));

        // переключатель
        float tw = 20, th = 11, tgx = x + w - tw - 8, tgy = y + 8;
        Draw.roundRect(ctx, tgx, tgy, tw, th, 5.5f, Draw.alpha(Theme.mix(C_OFF, acc, on), o));
        Draw.circle(ctx, tgx + 1.4f + (tw - th) * on, tgy + 1.4f, th - 2.8f, Draw.alpha(0xFFFFFFFF, o));

        // низ: бинд, настройки, звезда
        float by = y + h - 17;
        String bt = binding == m ? "..." : (m.getBind() > 0 ? keyName(m.getBind()) : "—");
        float bw = Fonts.BOLD.width(bt, 6.8f) + 10;
        Draw.roundRect(ctx, x + 9, by, bw, 11, 5.5f, Draw.alpha(binding == m ? Theme.withAlpha(acc, 70) : 0xFF21212D, o));
        Fonts.BOLD.drawCentered(ctx, bt, x + 9 + bw / 2, by + 2.6f, 6.8f, Draw.alpha(binding == m || m.getBind() > 0 ? acc : C_MUTED, o));
        hits.add(new Hit(x + 9, by, bw, 11, H_BIND, m, 0, 1));
        int ns = m.getSettings().size();
        if (ns > 0) Fonts.MEDIUM.draw(ctx, ns + " настр.", x + 9 + bw + 6, by + 2.6f, 6.8f, Draw.alpha(C_MUTED, o));

        float fx = x + w - 19, fy = y + h - 19;
        boolean fav = m.isFavorite();
        boolean fh = visible && in(mx, my, fx - 2, fy - 2, 16, 16);
        Draw.icon(ctx, "star", fx + 7, fy + 7, 11, Draw.alpha(fav ? 0xFFFFC857 : (fh ? C_SUB : 0xFF3A3A4A), o));
        hits.add(new Hit(fx - 2, fy - 2, 16, 16, H_FAV, m, 0, 1));

        hits.add(new Hit(x, y, w, h, H_MOD, m, 0, 7));
    }

    // ---------- правая панель ----------
    private void drawRight(DrawContext ctx, float mx, float my, float o, int acc, int acc2, boolean cosTab) {
        float x = rx, y = ry, w = rw, h = rh;
        Draw.roundRect(ctx, x, y, w, h, 10, Draw.alpha(C_CARD, o));
        Draw.ring(ctx, x, y, w, h, 10, Draw.alpha(Theme.withAlpha(acc, 80), o));

        float top = y;
        // превью персонажа в Косметике
        if (cosTab) {
            float ph = Math.min(150, h * 0.46f);
            drawPreview(ctx, mx, my, x, y, w, ph, o, acc, acc2);
            top = y + ph;
            ctx.fill(Math.round(x + 10), Math.round(top), Math.round(x + w - 10), Math.round(top) + 1, Draw.alpha(C_LINE, o));
        }

        Module m = selected != null && (selected.getCategory().ordinal() == tab || !search.isEmpty() || tab == favTab()) ? selected
                : (cosTab ? firstCosmetic() : selected);
        if (m == null) {
            Fonts.MEDIUM.drawCentered(ctx, "ПКМ по модулю: настройки", x + w / 2, y + h / 2 - 4, 8.5f, Draw.alpha(C_MUTED, o));
            return;
        }
        Fonts.HEAD.draw(ctx, Fonts.HEAD.fit(m.getName(), w - 44, 10f), x + 11, top + 9, 10f, Draw.alpha(C_TEXT, o));
        Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(m.getDescription(), w - 22, 7.2f), x + 11, top + 23, 7.2f, Draw.alpha(C_MUTED, o));
        if (!cosTab) {
            boolean ch = in(mx, my, x + w - 24, top + 6, 18, 18);
            Draw.circle(ctx, x + w - 23, top + 7, 16, Draw.alpha(ch ? 0xFF2A2A38 : 0xFF1E1E29, o));
            Fonts.BOLD.drawCentered(ctx, "×", x + w - 15, top + 10, 10f, Draw.alpha(C_SUB, o));
            hits.add(new Hit(x + w - 24, top + 6, 18, 18, H_CLOSEP, null, 0, 1));
        }
        ctx.fill(Math.round(x + 10), Math.round(top + 35), Math.round(x + w - 10), Math.round(top + 36), Draw.alpha(C_LINE, o));

        float sTop = top + 41, vh = y + h - sTop - 4;
        List<Setting<?>> ss = m.getSettings();
        float total = 0;
        for (Setting<?> s : ss) total += settingHeight(s);
        setMax = Math.max(0, total - vh);
        if (setScroll > setMax) setScroll = setMax;
        if (ss.isEmpty()) {
            Fonts.MEDIUM.drawCentered(ctx, "Нет настроек", x + w / 2, sTop + 20, 8.5f, Draw.alpha(C_MUTED, o));
            return;
        }
        ctx.enableScissor(Math.round(x * sc), Math.round(sTop * sc), Math.round((x + w) * sc), Math.round((sTop + vh) * sc));
        float cy = sTop - setScroll;
        for (Setting<?> s : ss) {
            float sh = settingHeight(s);
            if (cy + sh >= sTop - 2 && cy <= sTop + vh + 2) drawSetting(ctx, mx, my, s, x + 10, cy, w - 20, sTop, vh, o, acc);
            cy += sh;
        }
        ctx.disableScissor();
    }

    private Module firstCosmetic() {
        for (Module m : ModuleManager.MODULES) if (m.getCategory() == Module.Category.COSMETICS) return m;
        return null;
    }

    /** Живое превью игрока с косметикой (рисуется ванильным drawEntity). */
    private void drawPreview(DrawContext ctx, float mx, float my, float x, float y, float w, float h, float o, int acc, int acc2) {
        Draw.glow(ctx, x + w / 2 - 28, y + 8, 56, h - 16, 18, Draw.alpha(Theme.withAlpha(acc, 34), o));
        Fonts.BOLD.draw(ctx, "ПРЕВЬЮ", x + 11, y + 8, 6.5f, Draw.alpha(C_MUTED, o));
        if (client == null || client.player == null) {
            Fonts.MEDIUM.drawCentered(ctx, "Зайди в мир", x + w / 2, y + h / 2 - 4, 8.5f, Draw.alpha(C_MUTED, o));
            return;
        }
        try {
            // координаты drawEntity в экранных пикселях, а мы рисуем в масштабированной матрице
            int x1 = Math.round((x + 4) * sc), y1 = Math.round((y + 16) * sc);
            int x2 = Math.round((x + w - 4) * sc), y2 = Math.round((y + h - 2) * sc);
            int size = Math.round(h * 0.42f * sc);
            InventoryScreen.drawEntity(ctx, x1, y1, x2, y2, size, 0.0625f,
                    (float) (mx * sc), (float) ((y + h * 0.4f) * sc), client.player);
        } catch (Throwable t) {
            Fonts.MEDIUM.drawCentered(ctx, "Превью недоступно", x + w / 2, y + h / 2 - 4, 8.5f, Draw.alpha(C_MUTED, o));
        }
    }

    private float settingHeight(Setting<?> s) {
        float base = s instanceof NumSetting ? 30 : 24;
        if (s instanceof ModeSetting ms && openDrop == ms) base += ms.getModes().length * 17 + 3;
        return base;
    }

    private void drawSetting(DrawContext ctx, float mx, float my, Setting<?> s, float x, float y, float w,
                             float viewY, float viewH, float o, int acc) {
        boolean vis = my >= viewY && my <= viewY + viewH;
        if (s instanceof BoolSetting b) {
            float t = smooth(b, b.get() ? 1f : 0f, 16f);
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w - 34, 8.3f), x, y + 8, 8.3f, Draw.alpha(Theme.mix(C_SUB, C_TEXT, t), o));
            float tw = 20, th = 11, tx = x + w - tw, ty = y + 6;
            Draw.roundRect(ctx, tx, ty, tw, th, 5.5f, Draw.alpha(Theme.mix(C_OFF, acc, t), o));
            Draw.circle(ctx, tx + 1.4f + (tw - th) * t, ty + 1.4f, th - 2.8f, Draw.alpha(0xFFFFFFFF, o));
            hits.add(new Hit(x - 4, y, w + 8, 22, H_SET, s, 0, 1));
        } else if (s instanceof ColorSetting cs) {
            Fonts.MEDIUM.draw(ctx, s.getName(), x, y + 8, 8.3f, Draw.alpha(C_SUB, o));
            Draw.roundRect(ctx, x + w - 26, y + 4, 26, 14, 7, Draw.alpha(0xFF2A2A37, o));
            Draw.roundRect(ctx, x + w - 25, y + 5, 24, 12, 6, Draw.alpha(cs.get(), o));
            hits.add(new Hit(x - 4, y, w + 8, 22, H_COLOR, s, 0, 3));
        } else if (s instanceof ModeSetting ms) {
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w * 0.42f, 8.3f), x, y + 8, 8.3f, Draw.alpha(C_SUB, o));
            float dw = Math.min(w * 0.56f, 84), dx = x + w - dw;
            boolean open = openDrop == ms;
            boolean hov = vis && in(mx, my, dx, y + 2, dw, 18);
            Draw.roundRect(ctx, dx, y + 2, dw, 18, 7, Draw.alpha(hov || open ? 0xFF262634 : C_FIELD, o));
            if (open) Draw.ring(ctx, dx, y + 2, dw, 18, 7, Draw.alpha(Theme.withAlpha(acc, 160), o));
            Fonts.BOLD.draw(ctx, Fonts.BOLD.fit(ms.get(), dw - 22, 7.8f), dx + 8, y + 7, 7.8f, Draw.alpha(C_TEXT, o));
            Fonts.BOLD.drawRight(ctx, open ? "˄" : "˅", dx + dw - 7, y + 6, 7.8f, Draw.alpha(C_MUTED, o));
            hits.add(new Hit(dx, y + 2, dw, 18, H_SET, ms, 1, 3));
            if (open) {
                float oy = y + 24;
                Draw.roundRect(ctx, dx, oy - 2, dw, ms.getModes().length * 17 + 3, 7, Draw.alpha(0xFF1F1F2C, o));
                Draw.ring(ctx, dx, oy - 2, dw, ms.getModes().length * 17 + 3, 7, Draw.alpha(0x20FFFFFF, o));
                int idx = 0;
                for (String opt : ms.getModes()) {
                    boolean sel = opt.equals(ms.get());
                    boolean ho = vis && in(mx, my, dx, oy, dw, 16);
                    if (ho) Draw.roundRect(ctx, dx + 2, oy, dw - 4, 16, 6, Draw.alpha(0xFF2C2C3B, o));
                    Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(opt, dw - 22, 7.8f), dx + 8, oy + 4.5f, 7.8f, Draw.alpha(sel ? acc : C_TEXT, o));
                    if (sel) Draw.circle(ctx, dx + dw - 11, oy + 5.5f, 5, Draw.alpha(acc, o));
                    hits.add(new Hit(dx, oy, dw, 16, H_OPT, ms, idx++, 1));
                    oy += 17;
                }
            }
        } else if (s instanceof NumSetting ns) {
            float p = smooth(ns, (float) ns.getPercent(), 22f);
            String v = (ns.get() % 1 == 0) ? String.valueOf(ns.get().intValue()) : String.format(java.util.Locale.US, "%.2f", ns.get());
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w - 40, 8.3f), x, y + 4, 8.3f, Draw.alpha(C_SUB, o));
            Fonts.BOLD.drawRight(ctx, v, x + w, y + 4, 8.3f, Draw.alpha(C_TEXT, o));
            float by = y + 20;
            Draw.roundRect(ctx, x, by, w, 4, 2, Draw.alpha(C_OFF, o));
            float fw = Math.max(4, w * p);
            Draw.gradientH(ctx, x, by, fw, 4, Draw.alpha(acc, o), Draw.alpha(Theme.accent2(), o));
            boolean dr = dragging == ns;
            Draw.glow(ctx, x + w * p - 5, by - 3, 10, 10, 4, Draw.alpha(Theme.withAlpha(acc, dr ? 100 : 40), o));
            Draw.circle(ctx, x + w * p - 4.5f, by - 2.5f, dr ? 11 : 9, Draw.alpha(0xFFFFFFFF, o));
            hits.add(new Hit(x - 5, y + 12, w + 10, 20, H_SLIDER, ns, 0, 1));
            dragXFor.put(ns, new float[]{x, w});
        }
    }

    // ---------- оверлеи ----------
    private void overlayFrame(DrawContext ctx, float mx, float my, float o, int acc, int acc2, float x, float y, float w, float h, String title) {
        ctx.fill(0, 0, width, height, Draw.alpha(0x99000000, o));
        hits.add(new Hit(0, 0, width / sc, height / sc, H_OVERLAY_CLOSE, null, 0, 1));
        Draw.shadow(ctx, x, y, w, h, 14, 18);
        Draw.glow(ctx, x, y, w, h, 10, Draw.alpha(Theme.withAlpha(acc, 34), o));
        Draw.roundRect(ctx, x, y, w, h, 12, Draw.alpha(0xF70C0C12, o));
        Draw.ring(ctx, x, y, w, h, 12, Draw.alpha(0x22FFFFFF, o));
        Draw.gradientH(ctx, x + 12, y + 28, w - 24, 1.2f, Draw.alpha(acc, o * 0.9f), Draw.alpha(acc2, o * 0.2f));
        Fonts.HEAD.draw(ctx, title, x + 14, y + 11, 10.5f, Draw.alpha(C_TEXT, o));
        boolean ch = in(mx, my, x + w - 26, y + 7, 16, 16);
        Draw.circle(ctx, x + w - 26, y + 7, 16, Draw.alpha(ch ? 0xFF2A2A38 : 0xFF1E1E29, o));
        Fonts.BOLD.drawCentered(ctx, "×", x + w - 18, y + 10, 10f, Draw.alpha(C_SUB, o));
        hits.add(new Hit(x + w - 26, y + 7, 16, 16, H_OVERLAY_CLOSE, null, 0, 1));
        hits.add(new Hit(x, y, w, h, H_NOP, null, 0, 7));
    }

    private void drawSettingsOverlay(DrawContext ctx, float mx, float my, float o, int acc, int acc2) {
        float availW = width / sc, availH = height / sc;
        float w = Math.min(300, availW - 20), h = Math.min(290, availH - 20);
        float x = availW / 2 - w / 2, y = availH / 2 - h / 2;
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
        float rxx = vx - bw, vxx = rxx - vw, lx = vxx - bw;
        boolean hl = in(mx, my, lx, y + 3, bw, 18), hr = in(mx, my, rxx, y + 3, bw, 18);
        Draw.roundRect(ctx, lx, y + 3, bw, 18, 7, Draw.alpha(hl ? 0xFF2A2A38 : C_FIELD, o));
        Draw.roundRect(ctx, rxx, y + 3, bw, 18, 7, Draw.alpha(hr ? 0xFF2A2A38 : C_FIELD, o));
        Fonts.BOLD.drawCentered(ctx, "‹", lx + bw / 2, y + 7, 10f, Draw.alpha(C_SUB, o));
        Fonts.BOLD.drawCentered(ctx, "›", rxx + bw / 2, y + 7, 10f, Draw.alpha(C_SUB, o));
        Fonts.BOLD.drawCentered(ctx, value, vxx + vw / 2, y + 8, 8.5f, Draw.alpha(acc, o));
        hits.add(new Hit(lx, y + 3, bw, 18, H_CSTEP, key, -1, 1));
        hits.add(new Hit(rxx, y + 3, bw, 18, H_CSTEP, key, 1, 1));
        return y + rh + 3;
    }

    private void drawProfilesOverlay(DrawContext ctx, float mx, float my, float o, int acc, int acc2) {
        float availW = width / sc, availH = height / sc;
        float w = Math.min(300, availW - 20), h = Math.min(270, availH - 20);
        float x = availW / 2 - w / 2, y = availH / 2 - h / 2;
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
            float rh2 = 28;
            boolean act = p.equals(Config.currentProfile);
            boolean hv = in(mx, my, ax, cy, aw, rh2) && my >= ly && my <= ly + viewH;
            float hvv = smooth("pr" + p, hv ? 1f : 0f, 18f);
            Draw.roundRect(ctx, ax, cy, aw, rh2, 9, Draw.alpha(Theme.mix(0xFF14141C, 0xFF1A1A24, hvv), o));
            if (act) Draw.ring(ctx, ax, cy, aw, rh2, 9, Draw.alpha(Theme.withAlpha(acc, 140), o));
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
            cy += rh2 + 5;
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
        boolean inGrid = my >= gy && my <= gy + gh;
        boolean inRight = panelAnim.get() > 0.5f && in(mx, my, rx, ry, rw, rh);

        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!in(mx, my, h.x, h.y, h.w, h.h)) continue;
            if ((h.btnMask & bit) == 0) {
                if (h.type == H_NOP || h.type == H_OVERLAY_CLOSE) return true;
                continue;
            }
            if (overlay == Overlay.NONE) {
                boolean gridHit = h.type == H_MOD || h.type == H_FAV || h.type == H_BIND;
                if (gridHit && (!inGrid || mx > gx + gw + 2)) continue;
                boolean rightHit = h.type == H_SET || h.type == H_SLIDER || h.type == H_OPT || h.type == H_COLOR || h.type == H_CLOSEP;
                if (rightHit && !inRight) continue;
            }
            switch (h.type) {
                case H_NOP -> { return true; }
                case H_OVERLAY_CLOSE -> { overlay = Overlay.NONE; overlayScroll = 0; typingProfile = false; return true; }
                case H_DRAG -> { draggingWin = true; dragOffX = mx - wx; dragOffY = my - winY; return true; }
                case H_TAB -> {
                    tab = (Integer) h.a; search = ""; scrollAnim.snap(0); openDrop = null; setScroll = 0;
                    if (tab != Module.Category.COSMETICS.ordinal()) selected = tab == tab ? selected : null;
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
                case H_FAV -> { Module m = (Module) h.a; m.setFavorite(!m.isFavorite()); toast(m.isFavorite() ? "Добавлено в избранное" : "Убрано из избранного"); return true; }
                case H_BIND -> { binding = (Module) h.a; return true; }
                case H_MOD -> {
                    Module m = (Module) h.a;
                    openDrop = null;
                    if (btn == 2) binding = m;
                    else if (btn == 1) { selected = (selected == m && tab != Module.Category.COSMETICS.ordinal()) ? null : m; setScroll = 0; }
                    else m.toggle();
                    return true;
                }
                case H_CLOSEP -> { selected = null; openDrop = null; return true; }
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
                    dragX = d != null ? d[0] : h.x + 5;
                    dragW = d != null ? d[1] : h.w - 10;
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
            default -> {}
        }
    }

    private void clientStep(String k, int d) {
        switch (k) {
            case "scale" -> ClientSettings.menuScale = Math.max(0.7f, Math.min(1.4f, Math.round((ClientSettings.menuScale + d * 0.1f) * 10) / 10f));
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
        if (draggingWin) { winX = mx - dragOffX; winY = my - dragOffY; return true; }
        return true;
    }

    @Override
    public boolean mouseReleased(Click c) { dragging = null; draggingWin = false; return true; }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        float mx = (float) x / sc, my = (float) y / sc;
        if (overlay != Overlay.NONE) {
            overlayScroll = (float) Math.max(0, Math.min(overlayMax, overlayScroll - v * 24));
            return true;
        }
        if (panelAnim.get() > 0.5f && in(mx, my, rx, ry, rw, rh)) {
            setScroll = (float) Math.max(0, Math.min(setMax, setScroll - v * 24));
        } else {
            TSCROLL[tab] = (float) Math.max(0, Math.min(gridMax, TSCROLL[tab] - v * 30));
        }
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
        if (k == 256 && openDrop != null) { openDrop = null; return true; }
        if (k == 256 && overlay != Overlay.NONE) { overlay = Overlay.NONE; overlayScroll = 0; return true; }
        if (k == 256 && selected != null) { selected = null; return true; }
        if (k == 344 || k == 256) { this.close(); return true; }
        if (k == 70 && (input.modifiers() & 2) != 0) { searching = true; return true; } // Ctrl+F
        return super.keyPressed(input);
    }

    @Override public boolean shouldPause() { return false; }
}
