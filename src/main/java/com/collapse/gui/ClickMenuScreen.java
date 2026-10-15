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
 * Меню Collapse: боковая навигация, сетка карточек модулей, правая панель настроек,
 * избранное, профили конфигов и вкладка настроек клиента.
 */
public class ClickMenuScreen extends Screen {
    // ---- палитра ----
    private static final int C_WIN = 0xF20B0B10;
    private static final int C_SIDE = 0xFF0F0F15;
    private static final int C_CARD = 0xFF14141C;
    private static final int C_CARD_H = 0xFF1A1A24;
    private static final int C_LINE = 0x14FFFFFF;
    private static final int C_TEXT = 0xFFF1F1F7;
    private static final int C_SUB = 0xFF9A9AAD;
    private static final int C_MUTED = 0xFF63637A;
    private static final int C_FIELD = 0xFF1B1B25;
    private static final int C_OFF = 0xFF2A2A37;

    // ---- вкладки ----
    private static final int TAB_FAV = -1, TAB_SETTINGS = -2, TAB_PROFILES = -3;
    private static int tab = Module.Category.COMBAT.ordinal();

    // ---- состояние ----
    private static float posX = Float.NaN, posY = Float.NaN;
    private Module binding = null;
    private Module opened = null;         // модуль, чьи настройки открыты справа
    private final Anim openAnim = new Anim(0f, 11f);
    private final Anim panelAnim = new Anim(0f, 13f);
    private final Anim tabAnim = new Anim(0f, 16f);
    private final Anim scrollAnim = new Anim(0f, 20f);
    private final Anim sideScrollAnim = new Anim(0f, 20f);
    private final Map<Object, Anim> anims = new HashMap<>();
    private ModeSetting openDrop = null;
    private NumSetting dragging = null;
    private float dragX, dragW;
    private boolean draggingWin = false;
    private float dragOffX, dragOffY;
    private float scroll = 0, maxScroll = 0, sideScroll = 0, sideMax = 0;
    private String search = "";
    private boolean searching = false;
    private String profileName = "";
    private boolean typingProfile = false;
    private String toast = "";
    private long toastAt = 0;

    private float wx, wy, ww, wh, sc;
    private float gridX, gridY, gridW, gridH, panX, panY, panW, panH;

    private record Hit(float x, float y, float w, float h, int type, Object a, int b) {}
    private static final int H_TAB = 1, H_MOD = 2, H_SET = 3, H_THEME = 4, H_SEARCH = 5, H_OPT = 6, H_FAV = 7,
            H_SLIDER = 8, H_CLOSEP = 9, H_BIND = 10, H_DRAG = 11, H_PSAVE = 12, H_PLOAD = 13, H_PDEL = 14,
            H_PNAME = 15, H_CSET = 16, H_RESET = 17, H_PREV = 18;
    private final List<Hit> hits = new ArrayList<>();

    private ClickMenuScreen() { super(Text.literal("Collapse")); }
    public static ClickMenuScreen create() { return new ClickMenuScreen(); }

    @Override
    protected void init() {
        openAnim.snap(0f);
        openAnim.set(1f);
        panelAnim.snap(opened != null ? 1f : 0f);
        tabAnim.snap(tabIndexY());
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

    private float tabIndexY() { return tab >= 0 ? tab : (tab == TAB_FAV ? 5 : tab == TAB_PROFILES ? 6 : 7); }

    private List<Module> list() {
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase();
        for (Module m : ModuleManager.MODULES) {
            if (m.isHidden()) continue;
            if (!q.isEmpty()) {
                if (m.getName().toLowerCase().contains(q) || m.getDescription().toLowerCase().contains(q)) out.add(m);
            } else if (tab == TAB_FAV) {
                if (m.isFavorite()) out.add(m);
            } else if (tab >= 0 && m.getCategory().ordinal() == tab) out.add(m);
        }
        return out;
    }

    // ================= отрисовка =================
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        try {
            float o = Anim.ease(openAnim.get());
            if (ClientSettings.dimBackground) {
                ctx.fillGradient(0, 0, width, height, (int) (150 * o) << 24, (int) (200 * o) << 24);
            }
            sc = Math.max(0.7f, Math.min(1.4f, ClientSettings.menuScale));
            float availW = width / sc, availH = height / sc;
            ww = Math.min(availW - 16, 560);
            wh = Math.min(availH - 16, 330);
            if (Float.isNaN(posX)) { posX = (availW - ww) / 2f; posY = (availH - wh) / 2f; }
            posX = Math.max(-ww + 80, Math.min(availW - 80, posX));
            posY = Math.max(0, Math.min(availH - 30, posY));
            wx = posX;
            wy = posY + (1 - o) * 14;
            hits.clear();

            var m = ctx.getMatrices();
            m.pushMatrix();
            m.scale(sc, sc);
            float lmx = mx / sc, lmy = my / sc;
            draw(ctx, lmx, lmy, o);
            m.popMatrix();
        } catch (Throwable t) {
            CollapseClient.LOGGER.error("Menu render error", t);
        }
    }

    private void draw(DrawContext ctx, float mx, float my, float o) {
        int acc = Theme.accent();
        int acc2 = Theme.accent2();

        // окно
        Draw.shadow(ctx, wx, wy, ww, wh, 14, 18);
        Draw.glow(ctx, wx, wy, ww, wh, 10, Draw.alpha(Theme.withAlpha(acc, 34), o));
        Draw.roundRect(ctx, wx, wy, ww, wh, 14, Draw.alpha(C_WIN, o));
        Draw.ring(ctx, wx, wy, ww, wh, 14, Draw.alpha(0x1FFFFFFF, o));

        float sideW = 112;
        drawSidebar(ctx, mx, my, o, acc, acc2, sideW);

        // верхняя панель контента
        float cx = wx + sideW + 12, cy = wy + 12, cw = ww - sideW - 24;
        drawTopBar(ctx, mx, my, o, acc, cx, cy, cw);

        // область контента
        float areaY = cy + 36;
        float areaH = wy + wh - areaY - (ClientSettings.showHints ? 26 : 12);
        panelAnim.set(opened != null && tab >= 0 || (opened != null && tab == TAB_FAV) ? 1f : 0f);
        float pa = Anim.ease(panelAnim.get());
        panW = Math.min(188, cw * 0.46f);
        float gap = 10;
        gridX = cx; gridY = areaY;
        gridW = cw - (panW + gap) * pa;
        gridH = areaH;
        panX = cx + cw - panW * pa + (1 - pa) * 0; panY = areaY; panH = areaH;

        if (tab == TAB_SETTINGS) {
            drawClientSettings(ctx, mx, my, o, acc, cx, areaY, cw, areaH);
        } else if (tab == TAB_PROFILES) {
            drawProfiles(ctx, mx, my, o, acc, cx, areaY, cw, areaH);
        } else {
            drawGrid(ctx, mx, my, o, acc, acc2);
            if (pa > 0.01f && opened != null) drawPanel(ctx, mx, my, o * pa, acc, acc2);
        }

        // подсказки
        if (ClientSettings.showHints) {
            String hint = binding != null
                    ? "Нажмите клавишу для «" + binding.getName() + "»   ·   ESC: сбросить"
                    : "ЛКМ: вкл/выкл   ·   ПКМ: настройки   ·   СКМ: бинд   ·   RShift: закрыть";
            Fonts.MEDIUM.draw(ctx, hint, cx, wy + wh - 19, 8f, Draw.alpha(binding != null ? acc : C_MUTED, o));
        }
        // toast
        if (System.currentTimeMillis() - toastAt < 1800 && !toast.isEmpty()) {
            float tw = Fonts.BOLD.width(toast, 9f) + 22;
            float tx = wx + ww / 2 - tw / 2, ty = wy + wh - 40;
            Draw.shadow(ctx, tx, ty, tw, 20, 8, 8);
            Draw.roundRect(ctx, tx, ty, tw, 20, 10, Draw.alpha(0xFF1D1D29, o));
            Draw.ring(ctx, tx, ty, tw, 20, 10, Draw.alpha(Theme.withAlpha(acc, 120), o));
            Fonts.BOLD.drawCentered(ctx, toast, tx + tw / 2, ty + 6, 9f, Draw.alpha(C_TEXT, o));
        }
    }

    // ---------- боковая панель ----------
    private void drawSidebar(DrawContext ctx, float mx, float my, float o, int acc, int acc2, float sideW) {
        // фон панели (скруглена слева)
        Draw.roundRect(ctx, wx, wy, sideW, wh, 14, Draw.alpha(C_SIDE, o));
        ctx.fill((int) (wx + sideW - 14), (int) wy + 1, (int) (wx + sideW), (int) (wy + wh) - 1, Draw.alpha(C_SIDE, o));
        ctx.fill((int) (wx + sideW), (int) wy + 14, (int) (wx + sideW + 1), (int) (wy + wh - 14), Draw.alpha(C_LINE, o));
        // область перетаскивания окна — вся левая верхняя часть
        hits.add(new Hit(wx, wy, sideW, 48, H_DRAG, null, 0));

        // логотип
        float lx = wx + 14, ly = wy + 14;
        Draw.glow(ctx, lx, ly, 22, 22, 6, Draw.alpha(Theme.withAlpha(acc, 70), o));
        Draw.roundRect(ctx, lx, ly, 22, 22, 7, Draw.alpha(acc, o));
        Draw.roundRect(ctx, lx, ly + 11, 22, 11, 7, Draw.alpha(acc2, o * 0.55f));
        Fonts.HEAD.drawCentered(ctx, "C", lx + 11, ly + 5, 11f, Draw.alpha(0xFF0B0B10, o));
        Fonts.HEAD.draw(ctx, "Collapse", lx + 29, ly + 2, 9.5f, Draw.alpha(C_TEXT, o));
        Fonts.MEDIUM.draw(ctx, "client", lx + 29, ly + 13, 7.5f, Draw.alpha(C_MUTED, o));

        // пункты
        String[] names = {"Combat", "Movement", "Visuals", "Player", "Misc"};
        float y = wy + 58, itemH = 24, x = wx + 8, w = sideW - 16;
        // индикатор выбранного
        tabAnim.set(visualIndex());
        float indY = wy + 58 + tabAnim.get() * itemH + (tabAnim.get() > 4.5f ? 14 : 0) + (tabAnim.get() > 5.5f ? 0 : 0);

        section(ctx, "МОДУЛИ", x + 6, y - 11, o);
        for (int i = 0; i < names.length; i++) {
            navItem(ctx, mx, my, o, acc, x, y + i * itemH, w, itemH - 2, names[i], i, count(i), tab == i && search.isEmpty());
        }
        float y2 = y + names.length * itemH + 14;
        section(ctx, "ЛИЧНОЕ", x + 6, y2 - 11, o);
        navItem(ctx, mx, my, o, acc, x, y2, w, itemH - 2, "Избранное", TAB_FAV, favCount(), tab == TAB_FAV && search.isEmpty());
        navItem(ctx, mx, my, o, acc, x, y2 + itemH, w, itemH - 2, "Профили", TAB_PROFILES, -1, tab == TAB_PROFILES);
        navItem(ctx, mx, my, o, acc, x, y2 + itemH * 2, w, itemH - 2, "Настройки", TAB_SETTINGS, -1, tab == TAB_SETTINGS);

        // подвал: тема
        float fy = wy + wh - 34;
        boolean hv = in(mx, my, x, fy, w, 24);
        float hvv = smooth("themebtn", hv ? 1f : 0f, 18f);
        Draw.roundRect(ctx, x, fy, w, 24, 8, Draw.alpha(Theme.mix(C_FIELD, 0xFF232330, hvv), o));
        Draw.circle(ctx, x + 8, fy + 7, 10, Draw.alpha(acc, o));
        Draw.circle(ctx, x + 13, fy + 7, 10, Draw.alpha(acc2, o * 0.9f));
        Fonts.MEDIUM.draw(ctx, Theme.name(), x + 29, fy + 8, 8.5f, Draw.alpha(C_TEXT, o));
        Fonts.MEDIUM.drawRight(ctx, "›", x + w - 8, fy + 7, 10f, Draw.alpha(C_MUTED, o));
        hits.add(new Hit(x, fy, w, 24, H_THEME, null, 0));
    }

    private float visualIndex() {
        if (tab >= 0) return tab;
        return tab == TAB_FAV ? 5.0f : tab == TAB_PROFILES ? 6.0f : 7.0f;
    }

    private void section(DrawContext ctx, String t, float x, float y, float o) {
        Fonts.BOLD.draw(ctx, t, x, y, 6.5f, Draw.alpha(C_MUTED, o));
    }

    private int count(int cat) {
        int n = 0, on = 0;
        for (Module m : ModuleManager.MODULES) if (m.getCategory().ordinal() == cat) { n++; if (m.isEnabled()) on++; }
        return on;
    }
    private int favCount() { int n = 0; for (Module m : ModuleManager.MODULES) if (m.isFavorite()) n++; return n; }

    private void navItem(DrawContext ctx, float mx, float my, float o, int acc, float x, float y, float w, float h,
                         String label, int id, int badge, boolean sel) {
        boolean hv = in(mx, my, x, y, w, h);
        float s = smooth("nav" + id, sel ? 1f : 0f, 16f);
        float hvv = smooth("navh" + id, hv ? 1f : 0f, 18f);
        if (s > 0.01f) {
            Draw.roundRect(ctx, x, y, w, h, 8, Draw.alpha(Theme.withAlpha(acc, (int) (38 * s)), o));
            Draw.roundRect(ctx, x, y + h / 2 - 6 * s, 3, 12 * s, 1.5f, Draw.alpha(acc, o));
        } else if (hvv > 0.01f) {
            Draw.roundRect(ctx, x, y, w, h, 8, Draw.alpha(Theme.withAlpha(0xFFFFFF, (int) (14 * hvv)), o));
        }
        // иконка: цветная точка-значок
        int ic = Theme.mix(C_MUTED, acc, s);
        icon(ctx, id, x + 11, y + h / 2f, Draw.alpha(ic, o));
        int tc = Theme.mix(Theme.mix(C_SUB, C_TEXT, hvv), C_TEXT, s);
        (s > 0.5f ? Fonts.BOLD : Fonts.MEDIUM).draw(ctx, label, x + 24, y + h / 2f - 4.5f, 9f, Draw.alpha(tc, o));
        if (badge > 0) {
            String b = String.valueOf(badge);
            float bw = Fonts.BOLD.width(b, 7f) + 8;
            Draw.roundRect(ctx, x + w - bw - 6, y + h / 2f - 6, bw, 12, 6, Draw.alpha(Theme.withAlpha(acc, 60), o));
            Fonts.BOLD.drawCentered(ctx, b, x + w - bw / 2 - 6, y + h / 2f - 3.5f, 7f, Draw.alpha(acc, o));
        }
        hits.add(new Hit(x, y, w, h, H_TAB, id, 0));
    }

    /** Простые векторные иконки из примитивов (без пикселей и эмодзи). */
    private void icon(DrawContext ctx, int id, float cx, float cy, int c) {
        switch (id) {
            case 0 -> { // Combat: ромб/мишень
                Draw.circle(ctx, cx - 4.5f, cy - 4.5f, 9, c);
                Draw.circle(ctx, cx - 2.5f, cy - 2.5f, 5, Theme.mix(C_SIDE, 0xFF000000, 0.0f));
                Draw.circle(ctx, cx - 1.2f, cy - 1.2f, 2.4f, c);
            }
            case 1 -> { // Movement: стрелка
                Draw.roundRect(ctx, cx - 4.5f, cy - 1, 9, 2, 1, c);
                Draw.roundRect(ctx, cx + 0.5f, cy - 4.5f, 2, 6, 1, c);
                Draw.roundRect(ctx, cx + 0.5f, cy - 0.5f, 2, 5.5f, 1, c);
            }
            case 2 -> { // Visuals: глаз
                Draw.roundRect(ctx, cx - 5, cy - 3, 10, 6, 3, c);
                Draw.circle(ctx, cx - 2, cy - 2, 4, C_SIDE);
                Draw.circle(ctx, cx - 1, cy - 1, 2, c);
            }
            case 3 -> { // Player: человек
                Draw.circle(ctx, cx - 2.5f, cy - 5, 5, c);
                Draw.roundRect(ctx, cx - 4.5f, cy + 0.5f, 9, 4.5f, 2.5f, c);
            }
            case 4 -> { // Misc: сетка 2x2
                Draw.roundRect(ctx, cx - 4.5f, cy - 4.5f, 4, 4, 1.5f, c);
                Draw.roundRect(ctx, cx + 0.5f, cy - 4.5f, 4, 4, 1.5f, c);
                Draw.roundRect(ctx, cx - 4.5f, cy + 0.5f, 4, 4, 1.5f, c);
                Draw.roundRect(ctx, cx + 0.5f, cy + 0.5f, 4, 4, 1.5f, c);
            }
            case TAB_FAV -> star(ctx, cx, cy, 5.5f, c);
            case TAB_PROFILES -> { // слои
                Draw.roundRect(ctx, cx - 5, cy - 5, 10, 3.2f, 1.6f, c);
                Draw.roundRect(ctx, cx - 5, cy - 1.5f, 10, 3.2f, 1.6f, Draw.alpha(c, 0.7f));
                Draw.roundRect(ctx, cx - 5, cy + 2, 10, 3.2f, 1.6f, Draw.alpha(c, 0.45f));
            }
            case TAB_SETTINGS -> { // ползунки
                Draw.roundRect(ctx, cx - 5, cy - 4, 10, 1.6f, 0.8f, Draw.alpha(c, 0.6f));
                Draw.circle(ctx, cx - 3, cy - 5.2f, 4, c);
                Draw.roundRect(ctx, cx - 5, cy + 2.4f, 10, 1.6f, 0.8f, Draw.alpha(c, 0.6f));
                Draw.circle(ctx, cx + 1, cy + 1.2f, 4, c);
            }
            default -> Draw.circle(ctx, cx - 2, cy - 2, 4, c);
        }
    }

    private void star(DrawContext ctx, float cx, float cy, float r, int c) {
        // звезда из трёх повёрнутых по-разному "ромбов" через маленькие круги и бруски
        Draw.roundRect(ctx, cx - 1.1f, cy - r, 2.2f, r * 2, 1.1f, c);
        Draw.roundRect(ctx, cx - r, cy - 1.1f, r * 2, 2.2f, 1.1f, c);
        float d = r * 0.62f;
        Draw.circle(ctx, cx - d - 1, cy - d - 1, 2.2f, c);
        Draw.circle(ctx, cx + d - 1.2f, cy - d - 1, 2.2f, c);
        Draw.circle(ctx, cx - d - 1, cy + d - 1.2f, 2.2f, c);
        Draw.circle(ctx, cx + d - 1.2f, cy + d - 1.2f, 2.2f, c);
        Draw.circle(ctx, cx - r * 0.5f, cy - r * 0.5f, r, c);
    }

    // ---------- верхняя панель ----------
    private void drawTopBar(DrawContext ctx, float mx, float my, float o, int acc, float cx, float cy, float cw) {
        // перетаскивание за верхнюю полосу
        hits.add(new Hit(cx, wy, cw, 12, H_DRAG, null, 0));

        // заголовок
        String title = !search.isEmpty() ? "Поиск" : tab >= 0 ? Module.Category.values()[tab].label
                : tab == TAB_FAV ? "Избранное" : tab == TAB_PROFILES ? "Профили" : "Настройки клиента";
        Fonts.HEAD.draw(ctx, title, cx, cy + 3, 12f, Draw.alpha(C_TEXT, o));
        String sub = !search.isEmpty() ? "найдено: " + list().size()
                : tab >= 0 || tab == TAB_FAV ? list().size() + " модулей" : "";
        if (!sub.isEmpty()) Fonts.MEDIUM.draw(ctx, sub, cx + Fonts.HEAD.width(title, 12f) + 8, cy + 6, 8f, Draw.alpha(C_MUTED, o));

        // поиск
        float sw = Math.min(150, cw * 0.38f), sx = cx + cw - sw, sh = 24;
        float sf = smooth("search", searching ? 1f : 0f, 16f);
        Draw.roundRect(ctx, sx, cy, sw, sh, 9, Draw.alpha(Theme.mix(C_FIELD, 0xFF22222E, sf), o));
        Draw.ring(ctx, sx, cy, sw, sh, 9, Draw.alpha(Theme.mix(0x14FFFFFF, Theme.withAlpha(acc, 190), sf), o));
        // лупа
        Draw.ring(ctx, sx + 9, cy + 7.5f, 8, 8, 4, Draw.alpha(C_MUTED, o));
        Draw.roundRect(ctx, sx + 16, cy + 15, 3.5f, 1.5f, 0.7f, Draw.alpha(C_MUTED, o));
        boolean blink = searching && (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = search.isEmpty() ? (searching ? (blink ? "|" : "") : "Поиск модулей") : Fonts.MEDIUM.fit(search, sw - 44, 9f) + (blink ? "|" : "");
        Fonts.MEDIUM.draw(ctx, shown, sx + 24, cy + 8, 9f, Draw.alpha(search.isEmpty() ? C_MUTED : C_TEXT, o));
        if (!search.isEmpty()) {
            Fonts.BOLD.draw(ctx, "×", sx + sw - 13, cy + 6, 11f, Draw.alpha(C_SUB, o));
        }
        hits.add(new Hit(sx, cy, sw, sh, H_SEARCH, null, 0));
    }

    // ---------- сетка модулей ----------
    private void drawGrid(DrawContext ctx, float mx, float my, float o, int acc, int acc2) {
        List<Module> mods = list();
        int cols = gridW >= 330 ? 3 : (gridW >= 210 ? 2 : 1);
        float gap = 8, cardH = 52;
        float cwid = (gridW - gap * (cols - 1)) / cols;
        int rows = (int) Math.ceil(mods.size() / (double) cols);
        float contentH = rows * (cardH + gap);
        maxScroll = Math.max(0, contentH - gridH);
        if (scroll > maxScroll) scroll = maxScroll;
        scrollAnim.set(scroll);
        float sy = scrollAnim.get();

        if (mods.isEmpty()) {
            String msg = tab == TAB_FAV && search.isEmpty() ? "Пока пусто. Нажмите ☆ на карточке модуля." : "Ничего не найдено";
            Fonts.MEDIUM.drawCentered(ctx, msg, gridX + gridW / 2, gridY + gridH / 2 - 6, 10f, Draw.alpha(C_MUTED, o));
            return;
        }
        ctx.enableScissor(Math.round((gridX - 4) * sc), Math.round(gridY * sc), Math.round((gridX + gridW + 4) * sc), Math.round((gridY + gridH) * sc));
        for (int i = 0; i < mods.size(); i++) {
            int r = i / cols, c = i % cols;
            float x = gridX + c * (cwid + gap), y = gridY + r * (cardH + gap) - sy;
            if (y + cardH < gridY - 2 || y > gridY + gridH + 2) continue;
            drawCard(ctx, mx, my, mods.get(i), x, y, cwid, cardH, o, acc, acc2);
        }
        ctx.disableScissor();

        // полоса прокрутки
        if (maxScroll > 1) {
            float th = Math.max(20, gridH * gridH / (contentH));
            float ty = gridY + (gridH - th) * (sy / maxScroll);
            Draw.roundRect(ctx, gridX + gridW + 1, ty, 2, th, 1, Draw.alpha(0x40FFFFFF, o));
        }
    }

    private void drawCard(DrawContext ctx, float mx, float my, Module m, float x, float y, float w, float h, float o, int acc, int acc2) {
        boolean visible = my >= gridY && my <= gridY + gridH;
        boolean hv = visible && in(mx, my, x, y, w, h);
        float on = smooth(m, m.isEnabled() ? 1f : 0f, 14f);
        float hvv = smooth("ch" + m.getName(), hv ? 1f : 0f, 18f);
        boolean sel = opened == m;

        if (on > 0.02f) Draw.glow(ctx, x, y, w, h, 5, Draw.alpha(Theme.withAlpha(acc, (int) (26 * on)), o));
        int bg = Theme.mix(C_CARD, C_CARD_H, hvv);
        bg = Theme.mix(bg, Theme.mix(0xFF1B1D2B, 0xFF1A2038, 0.5f), on * 0.6f);
        Draw.roundRect(ctx, x, y, w, h, 10, Draw.alpha(bg, o));
        int border = sel ? Theme.withAlpha(acc, 200) : Theme.mix(0x12FFFFFF, Theme.withAlpha(acc, 90), on);
        Draw.ring(ctx, x, y, w, h, 10, Draw.alpha(border, o));

        // акцентная полоса снизу при включении
        if (on > 0.02f) {
            Draw.roundRect(ctx, x + 12, y + h - 3, (w - 24) * on, 2, 1, Draw.alpha(acc, o * 0.9f));
        }

        // название
        float pad = 10;
        float nameMax = w - pad * 2 - 16;
        Fonts.BOLD.draw(ctx, Fonts.BOLD.fit(m.getName(), nameMax, 10f), x + pad, y + 9, 10f, Draw.alpha(Theme.mix(C_TEXT, 0xFFFFFFFF, on), o));
        Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(m.getDescription(), w - pad * 2, 7.5f), x + pad, y + 23, 7.5f, Draw.alpha(C_MUTED, o));

        // нижняя строка: бинд-чип + кол-во настроек
        float by = y + h - 17;
        String bind = m.getBind() > 0 ? keyName(m.getBind()) : "—";
        boolean binding = this.binding == m;
        String bt = binding ? "..." : bind;
        float bw = Fonts.BOLD.width(bt, 7f) + 10;
        Draw.roundRect(ctx, x + pad, by, bw, 11, 5.5f, Draw.alpha(binding ? Theme.withAlpha(acc, 70) : 0xFF21212D, o));
        Fonts.BOLD.drawCentered(ctx, bt, x + pad + bw / 2, by + 2.5f, 7f, Draw.alpha(binding || m.getBind() > 0 ? acc : C_MUTED, o));
        hits.add(new Hit(x + pad, by, bw, 11, H_BIND, m, 0));

        int ns = m.getSettings().size();
        if (ns > 0) Fonts.MEDIUM.draw(ctx, ns + " настр.", x + pad + bw + 6, by + 2.5f, 7f, Draw.alpha(C_MUTED, o));

        // переключатель справа сверху
        float tw = 22, th = 12, tx = x + w - tw - 8, ty = y + 8;
        Draw.roundRect(ctx, tx, ty, tw, th, 6, Draw.alpha(Theme.mix(C_OFF, acc, on), o));
        Draw.circle(ctx, tx + 1.5f + (tw - th) * on, ty + 1.5f, th - 3, Draw.alpha(0xFFFFFFFF, o));

        // звезда избранного
        float fx = x + w - 20, fy = y + h - 18;
        boolean fav = m.isFavorite();
        boolean fh = visible && in(mx, my, fx - 2, fy - 2, 16, 16);
        int fc = fav ? 0xFFFFC857 : (fh ? C_SUB : 0xFF3A3A4A);
        star(ctx, fx + 7, fy + 7, 4.6f, Draw.alpha(fc, o));
        hits.add(new Hit(fx - 2, fy - 2, 16, 16, H_FAV, m, 0));

        hits.add(new Hit(x, y, w, h, H_MOD, m, 0));
    }

    // ---------- панель настроек модуля ----------
    private void drawPanel(DrawContext ctx, float mx, float my, float o, int acc, int acc2) {
        Module m = opened;
        float x = panX, y = panY, w = panW, h = panH;
        Draw.roundRect(ctx, x, y, w, h, 10, Draw.alpha(C_CARD, o));
        Draw.ring(ctx, x, y, w, h, 10, Draw.alpha(Theme.withAlpha(acc, 80), o));
        // шапка
        Fonts.HEAD.draw(ctx, Fonts.HEAD.fit(m.getName(), w - 44, 10f), x + 11, y + 10, 10f, Draw.alpha(C_TEXT, o));
        Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(m.getDescription(), w - 22, 7.5f), x + 11, y + 25, 7.5f, Draw.alpha(C_MUTED, o));
        // закрыть
        boolean ch = in(mx, my, x + w - 24, y + 6, 18, 18);
        Draw.circle(ctx, x + w - 23, y + 7, 16, Draw.alpha(ch ? 0xFF2A2A38 : 0xFF1E1E29, o));
        Fonts.BOLD.drawCentered(ctx, "×", x + w - 15, y + 10, 10f, Draw.alpha(C_SUB, o));
        hits.add(new Hit(x + w - 24, y + 6, 18, 18, H_CLOSEP, null, 0));
        ctx.fill((int) x + 10, (int) (y + 38), (int) (x + w - 10), (int) (y + 39), Draw.alpha(C_LINE, o));

        // настройки
        float top = y + 44, vh = h - 50;
        List<Setting<?>> ss = m.getSettings();
        float total = 0;
        for (Setting<?> s : ss) total += settingHeight(s);
        sideMax = Math.max(0, total - vh);
        if (sideScroll > sideMax) sideScroll = sideMax;
        sideScrollAnim.set(sideScroll);
        if (ss.isEmpty()) {
            Fonts.MEDIUM.drawCentered(ctx, "Нет настроек", x + w / 2, y + h / 2 - 4, 9f, Draw.alpha(C_MUTED, o));
            return;
        }
        ctx.enableScissor(Math.round(x * sc), Math.round(top * sc), Math.round((x + w) * sc), Math.round((top + vh) * sc));
        float cy = top - sideScrollAnim.get();
        for (Setting<?> s : ss) {
            cy += drawSetting(ctx, mx, my, s, x + 8, cy, w - 16, top, vh, o, acc);
        }
        ctx.disableScissor();
    }

    private float settingHeight(Setting<?> s) {
        float base = s instanceof NumSetting ? 30 : 24;
        if (s instanceof ModeSetting ms && openDrop == ms) base += ms.getModes().length * 18 + 2;
        return base;
    }

    private float drawSetting(DrawContext ctx, float mx, float my, Setting<?> s, float x, float y, float w, float viewY, float viewH, float o, int acc) {
        boolean vis = my >= viewY && my <= viewY + viewH;
        float hgt = settingHeight(s);
        if (y + hgt < viewY - 2 || y > viewY + viewH + 2) return hgt;

        if (s instanceof BoolSetting b) {
            float t = smooth(b, b.get() ? 1f : 0f, 16f);
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w - 36, 8.5f), x + 2, y + 8, 8.5f, Draw.alpha(C_SUB, o));
            float tw = 22, th = 12, tx = x + w - tw - 2, ty = y + 5;
            Draw.roundRect(ctx, tx, ty, tw, th, 6, Draw.alpha(Theme.mix(C_OFF, acc, t), o));
            Draw.circle(ctx, tx + 1.5f + (tw - th) * t, ty + 1.5f, th - 3, Draw.alpha(0xFFFFFFFF, o));
            hits.add(new Hit(x, y, w, 22, H_SET, s, 0));
        } else if (s instanceof ColorSetting cs) {
            Fonts.MEDIUM.draw(ctx, s.getName(), x + 2, y + 8, 8.5f, Draw.alpha(C_SUB, o));
            Draw.roundRect(ctx, x + w - 26, y + 4, 24, 14, 7, Draw.alpha(0xFF2A2A37, o));
            Draw.roundRect(ctx, x + w - 25, y + 5, 22, 12, 6, Draw.alpha(cs.get(), o));
            hits.add(new Hit(x, y, w, 22, H_CSET, s, 0));
        } else if (s instanceof ModeSetting ms) {
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w * 0.45f, 8.5f), x + 2, y + 8, 8.5f, Draw.alpha(C_SUB, o));
            float dw = Math.min(w * 0.52f, 84), dx = x + w - dw;
            boolean open = openDrop == ms;
            boolean hov = vis && in(mx, my, dx, y + 2, dw, 18);
            Draw.roundRect(ctx, dx, y + 2, dw, 18, 7, Draw.alpha(hov || open ? 0xFF242431 : C_FIELD, o));
            if (open) Draw.ring(ctx, dx, y + 2, dw, 18, 7, Draw.alpha(Theme.withAlpha(acc, 150), o));
            Fonts.BOLD.draw(ctx, Fonts.BOLD.fit(ms.get(), dw - 22, 8f), dx + 8, y + 7, 8f, Draw.alpha(C_TEXT, o));
            Fonts.BOLD.drawRight(ctx, open ? "˄" : "˅", dx + dw - 7, y + 6, 8f, Draw.alpha(C_MUTED, o));
            hits.add(new Hit(dx, y + 2, dw, 18, H_SET, ms, 1));
            if (open) {
                float oy = y + 24;
                Draw.roundRect(ctx, dx, oy - 2, dw, ms.getModes().length * 18 + 3, 7, Draw.alpha(0xFF1D1D29, o));
                int idx = 0;
                for (String opt : ms.getModes()) {
                    boolean sel = opt.equals(ms.get());
                    boolean ho = vis && in(mx, my, dx, oy, dw, 17);
                    if (ho) Draw.roundRect(ctx, dx + 2, oy, dw - 4, 17, 6, Draw.alpha(0xFF2A2A38, o));
                    Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(opt, dw - 24, 8f), dx + 9, oy + 5, 8f, Draw.alpha(sel ? acc : C_TEXT, o));
                    if (sel) Draw.circle(ctx, dx + dw - 12, oy + 6, 5, Draw.alpha(acc, o));
                    hits.add(new Hit(dx, oy, dw, 17, H_OPT, ms, idx++));
                    oy += 18;
                }
            }
        } else if (s instanceof NumSetting ns) {
            float p = smooth(ns, (float) ns.getPercent(), 22f);
            String v = (ns.get() % 1 == 0) ? String.valueOf(ns.get().intValue()) : String.format(java.util.Locale.US, "%.2f", ns.get());
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w - 40, 8.5f), x + 2, y + 5, 8.5f, Draw.alpha(C_SUB, o));
            Fonts.BOLD.drawRight(ctx, v, x + w - 2, y + 5, 8.5f, Draw.alpha(C_TEXT, o));
            float bx = x + 2, bw = w - 4, by = y + 20;
            Draw.roundRect(ctx, bx, by, bw, 4, 2, Draw.alpha(C_OFF, o));
            float fw = Math.max(4, bw * p);
            Draw.roundRect(ctx, bx, by, fw, 4, 2, Draw.alpha(acc, o));
            boolean dr = dragging == ns;
            Draw.glow(ctx, bx + bw * p - 5, by - 3, 10, 10, 4, Draw.alpha(Theme.withAlpha(acc, dr ? 90 : 40), o));
            Draw.circle(ctx, bx + bw * p - 4.5f, by - 2.5f, dr ? 11 : 9, Draw.alpha(0xFFFFFFFF, o));
            hits.add(new Hit(bx - 5, y + 12, bw + 10, 20, H_SLIDER, ns, 0));
        }
        return hgt;
    }

    // ---------- настройки клиента ----------
    private void drawClientSettings(DrawContext ctx, float mx, float my, float o, int acc, float x, float y, float w, float h) {
        ctx.enableScissor(Math.round(x * sc), Math.round(y * sc), Math.round((x + w) * sc), Math.round((y + h) * sc));
        scrollAnim.set(scroll);
        float cy = y - scrollAnim.get();
        float rowW = w;

        cy = group(ctx, "Интерфейс", x, cy, rowW, o);
        cy = csToggle(ctx, mx, my, o, acc, "Затемнять фон", "dim", ClientSettings.dimBackground, x, cy, rowW);
        cy = csToggle(ctx, mx, my, o, acc, "Подсказки внизу", "hints", ClientSettings.showHints, x, cy, rowW);
        cy = csToggle(ctx, mx, my, o, acc, "Переливающийся акцент", "animtheme", ClientSettings.animatedTheme, x, cy, rowW);
        cy = csStepper(ctx, mx, my, o, acc, "Масштаб меню", "scale", String.format(java.util.Locale.US, "%.0f%%", ClientSettings.menuScale * 100), x, cy, rowW);
        cy = csStepper(ctx, mx, my, o, acc, "Скорость анимаций", "anim", String.format(java.util.Locale.US, "x%.1f", ClientSettings.animSpeed), x, cy, rowW);
        cy += 6;
        cy = group(ctx, "Список модулей (HUD)", x, cy, rowW, o);
        cy = csToggle(ctx, mx, my, o, acc, "Показывать список", "al", ClientSettings.arrayList, x, cy, rowW);
        cy = csToggle(ctx, mx, my, o, acc, "Подложка", "albg", ClientSettings.arrayListBg, x, cy, rowW);
        String[] posN = {"Справа сверху", "Слева сверху", "Справа снизу", "Слева снизу"};
        cy = csStepper(ctx, mx, my, o, acc, "Позиция", "alpos", posN[Math.floorMod(ClientSettings.arrayListPos, 4)], x, cy, rowW);
        cy += 6;
        cy = group(ctx, "Тема", x, cy, rowW, o);
        // палитра тем
        int n = Theme.count();
        float sw = (rowW - (n - 1) * 6) / n;
        for (int i = 0; i < n; i++) {
            float sx = x + i * (sw + 6);
            boolean sel = Theme.index() == i;
            boolean hv = in(mx, my, sx, cy, sw, 30);
            float s2 = smooth("th" + i, sel ? 1f : (hv ? 0.4f : 0f), 16f);
            Draw.roundRect(ctx, sx, cy, sw, 30, 9, Draw.alpha(C_CARD, o));
            Draw.roundRect(ctx, sx + 4, cy + 4, sw - 8, 14, 6, Draw.alpha(Theme.accentOf(i), o));
            Draw.roundRect(ctx, sx + 4 + (sw - 8) / 2, cy + 4, (sw - 8) / 2, 14, 6, Draw.alpha(Theme.secondOf(i), o));
            Fonts.MEDIUM.drawCentered(ctx, Theme.NAMES[i], sx + sw / 2, cy + 20, 6.5f, Draw.alpha(Theme.mix(C_MUTED, C_TEXT, s2), o));
            if (s2 > 0.3f) Draw.ring(ctx, sx, cy, sw, 30, 9, Draw.alpha(Theme.withAlpha(Theme.accentOf(i), (int) (230 * s2)), o));
            hits.add(new Hit(sx, cy, sw, 30, H_CSET + 100, null, i));
        }
        cy += 38;
        ctx.disableScissor();
        maxScroll = Math.max(0, cy + scrollAnim.get() - (y + h));
        if (scroll > maxScroll) scroll = maxScroll;
    }

    private float group(DrawContext ctx, String t, float x, float y, float w, float o) {
        Fonts.BOLD.draw(ctx, t.toUpperCase(), x + 2, y + 2, 7f, Draw.alpha(C_MUTED, o));
        return y + 16;
    }

    private float csToggle(DrawContext ctx, float mx, float my, float o, int acc, String label, String key, boolean v, float x, float y, float w) {
        float rh = 26;
        boolean hv = in(mx, my, x, y, w, rh);
        float hvv = smooth("cs" + key, hv ? 1f : 0f, 18f);
        float t = smooth("cst" + key, v ? 1f : 0f, 16f);
        Draw.roundRect(ctx, x, y, w, rh, 9, Draw.alpha(Theme.mix(C_CARD, C_CARD_H, hvv), o));
        Fonts.MEDIUM.draw(ctx, label, x + 11, y + 9, 9f, Draw.alpha(C_TEXT, o));
        float tw = 24, th = 13, tx = x + w - tw - 10, ty = y + 6.5f;
        Draw.roundRect(ctx, tx, ty, tw, th, 6.5f, Draw.alpha(Theme.mix(C_OFF, acc, t), o));
        Draw.circle(ctx, tx + 1.5f + (tw - th) * t, ty + 1.5f, th - 3, Draw.alpha(0xFFFFFFFF, o));
        hits.add(new Hit(x, y, w, rh, H_CSET, key, 0));
        return y + rh + 4;
    }

    private float csStepper(DrawContext ctx, float mx, float my, float o, int acc, String label, String key, String value, float x, float y, float w) {
        float rh = 26;
        Draw.roundRect(ctx, x, y, w, rh, 9, Draw.alpha(C_CARD, o));
        Fonts.MEDIUM.draw(ctx, label, x + 11, y + 9, 9f, Draw.alpha(C_TEXT, o));
        float bw = 18, vx = x + w - 10;
        float vw = Math.max(54, Fonts.BOLD.width(value, 8.5f) + 8);
        float rx = vx - bw, vxx = rx - vw, lx = vxx - bw;
        boolean hl = in(mx, my, lx, y + 4, bw, 18), hr = in(mx, my, rx, y + 4, bw, 18);
        Draw.roundRect(ctx, lx, y + 4, bw, 18, 7, Draw.alpha(hl ? 0xFF2A2A38 : C_FIELD, o));
        Draw.roundRect(ctx, rx, y + 4, bw, 18, 7, Draw.alpha(hr ? 0xFF2A2A38 : C_FIELD, o));
        Fonts.BOLD.drawCentered(ctx, "‹", lx + bw / 2, y + 8, 10f, Draw.alpha(C_SUB, o));
        Fonts.BOLD.drawCentered(ctx, "›", rx + bw / 2, y + 8, 10f, Draw.alpha(C_SUB, o));
        Fonts.BOLD.drawCentered(ctx, value, vxx + vw / 2, y + 9, 8.5f, Draw.alpha(acc, o));
        hits.add(new Hit(lx, y + 4, bw, 18, H_CSET + 200, key, -1));
        hits.add(new Hit(rx, y + 4, bw, 18, H_CSET + 200, key, 1));
        return y + rh + 4;
    }

    // ---------- профили ----------
    private void drawProfiles(DrawContext ctx, float mx, float my, float o, int acc, float x, float y, float w, float h) {
        // ввод имени + сохранить
        float fh = 28;
        float bw = 84;
        float fw = w - bw - 8;
        float fa = smooth("pname", typingProfile ? 1f : 0f, 16f);
        Draw.roundRect(ctx, x, y, fw, fh, 10, Draw.alpha(Theme.mix(C_FIELD, 0xFF22222E, fa), o));
        Draw.ring(ctx, x, y, fw, fh, 10, Draw.alpha(Theme.mix(0x14FFFFFF, Theme.withAlpha(acc, 190), fa), o));
        boolean blink = typingProfile && (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = profileName.isEmpty() ? (typingProfile ? (blink ? "|" : "") : "Имя нового профиля") : profileName + (blink ? "|" : "");
        Fonts.MEDIUM.draw(ctx, shown, x + 12, y + 10, 9f, Draw.alpha(profileName.isEmpty() ? C_MUTED : C_TEXT, o));
        hits.add(new Hit(x, y, fw, fh, H_PNAME, null, 0));
        boolean hs = in(mx, my, x + fw + 8, y, bw, fh);
        Draw.roundRect(ctx, x + fw + 8, y, bw, fh, 10, Draw.alpha(hs ? Theme.mix(acc, 0xFFFFFFFF, 0.15f) : acc, o));
        Fonts.BOLD.drawCentered(ctx, "Сохранить", x + fw + 8 + bw / 2, y + 10, 8.5f, Draw.alpha(0xFF0B0B10, o));
        hits.add(new Hit(x + fw + 8, y, bw, fh, H_PSAVE, null, 0));

        // список
        List<String> ps = Config.profiles();
        float ly = y + fh + 12;
        Fonts.BOLD.draw(ctx, "СОХРАНЁННЫЕ  ·  активный: " + Config.currentProfile, x + 2, ly - 2, 7f, Draw.alpha(C_MUTED, o));
        ly += 12;
        float viewH = y + h - ly;
        ctx.enableScissor(Math.round(x * sc), Math.round(ly * sc), Math.round((x + w) * sc), Math.round((ly + viewH) * sc));
        scrollAnim.set(scroll);
        float cy = ly - scrollAnim.get();
        if (ps.isEmpty()) {
            Fonts.MEDIUM.drawCentered(ctx, "Профилей пока нет. Введите имя и нажмите «Сохранить».", x + w / 2, ly + 30, 9f, Draw.alpha(C_MUTED, o));
        }
        for (String p : ps) {
            float rh = 30;
            boolean act = p.equals(Config.currentProfile);
            boolean hv = in(mx, my, x, cy, w, rh) && my >= ly && my <= ly + viewH;
            float hvv = smooth("pr" + p, hv ? 1f : 0f, 18f);
            Draw.roundRect(ctx, x, cy, w, rh, 10, Draw.alpha(Theme.mix(C_CARD, C_CARD_H, hvv), o));
            if (act) Draw.ring(ctx, x, cy, w, rh, 10, Draw.alpha(Theme.withAlpha(acc, 140), o));
            Draw.circle(ctx, x + 11, cy + 11, 8, Draw.alpha(act ? acc : C_OFF, o));
            Fonts.BOLD.draw(ctx, Fonts.BOLD.fit(p, w - 170, 9.5f), x + 28, cy + 10, 9.5f, Draw.alpha(C_TEXT, o));
            float bx = x + w - 10;
            bx -= 54; boolean hd = in(mx, my, bx, cy + 6, 54, 18);
            Draw.roundRect(ctx, bx, cy + 6, 54, 18, 7, Draw.alpha(hd ? 0xFF4A2530 : 0xFF2A1D24, o));
            Fonts.BOLD.drawCentered(ctx, "Удалить", bx + 27, cy + 11, 7.5f, Draw.alpha(0xFFFF6B7D, o));
            hits.add(new Hit(bx, cy + 6, 54, 18, H_PDEL, p, 0));
            bx -= 62; boolean hl = in(mx, my, bx, cy + 6, 58, 18);
            Draw.roundRect(ctx, bx, cy + 6, 58, 18, 7, Draw.alpha(hl ? Theme.mix(acc, 0xFFFFFFFF, 0.15f) : Theme.withAlpha(acc, 60), o));
            Fonts.BOLD.drawCentered(ctx, "Загрузить", bx + 29, cy + 11, 7.5f, Draw.alpha(hl ? 0xFF0B0B10 : acc, o));
            hits.add(new Hit(bx, cy + 6, 58, 18, H_PLOAD, p, 0));
            cy += rh + 6;
        }
        ctx.disableScissor();
        maxScroll = Math.max(0, cy + scrollAnim.get() - (ly + viewH));
        if (scroll > maxScroll) scroll = maxScroll;
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
        boolean wasSearching = searching, wasTyping = typingProfile;
        searching = false; typingProfile = false;
        boolean inGrid = my >= gridY && my <= gridY + gridH;
        boolean overPanel = opened != null && panelAnim.get() > 0.5f && in(mx, my, panX, panY, panW, panH);

        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!in(mx, my, h.x, h.y, h.w, h.h)) continue;
            // карточки под панелью не кликаем
            boolean gridHit = h.type == H_MOD || h.type == H_FAV || h.type == H_BIND;
            if (gridHit && (!inGrid || mx > gridX + gridW + 2 && opened != null)) continue;
            if ((h.type == H_SET || h.type == H_SLIDER || h.type == H_OPT || h.type == H_CSET) && h.type != H_CSET && !overPanel) continue;
            switch (h.type) {
                case H_DRAG -> { draggingWin = true; dragOffX = mx - wx; dragOffY = my - posY; return true; }
                case H_TAB -> {
                    tab = (Integer) h.a; search = ""; scroll = 0; scrollAnim.snap(0); openDrop = null;
                    if (tab < 0) opened = null;
                    return true;
                }
                case H_THEME -> { if (btn == 1) Theme.prev(); else Theme.next(); return true; }
                case H_SEARCH -> {
                    if (!search.isEmpty() && mx > h.x + h.w - 20) { search = ""; scroll = 0; }
                    else searching = true;
                    return true;
                }
                case H_FAV -> { Module m = (Module) h.a; m.setFavorite(!m.isFavorite()); toast(m.isFavorite() ? "Добавлено в избранное" : "Убрано из избранного"); return true; }
                case H_BIND -> { binding = (Module) h.a; return true; }
                case H_MOD -> {
                    Module m = (Module) h.a;
                    if (btn == 2) binding = m;
                    else if (btn == 1) { opened = (opened == m) ? null : m; sideScroll = 0; sideScrollAnim.snap(0); openDrop = null; }
                    else m.toggle();
                    return true;
                }
                case H_CLOSEP -> { opened = null; openDrop = null; return true; }
                case H_OPT -> {
                    ModeSetting ms = (ModeSetting) h.a;
                    ms.set(ms.getModes()[h.b]);
                    openDrop = null;
                    return true;
                }
                case H_SET -> {
                    Setting<?> s = (Setting<?>) h.a;
                    if (s instanceof BoolSetting b) b.toggle();
                    else if (s instanceof ModeSetting ms) openDrop = (openDrop == ms) ? null : ms;
                    return true;
                }
                case H_CSET -> {
                    if (h.a instanceof ColorSetting cs) { if (btn == 1) cs.prev(); else cs.next(); return true; }
                    clientToggle((String) h.a);
                    return true;
                }
                case H_CSET + 100 -> { Theme.setIndex(h.b); return true; }
                case H_CSET + 200 -> { clientStep((String) h.a, h.b); return true; }
                case H_SLIDER -> {
                    dragging = (NumSetting) h.a; dragX = h.x + 5; dragW = h.w - 10;
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
            }
        }
        openDrop = null;
        if (wasSearching || wasTyping) return true;
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
        float mx = (float) c.x() / sc;
        if (dragging != null) { applyDrag(mx); return true; }
        if (draggingWin) { posX = mx - dragOffX; posY = (float) c.y() / sc - dragOffY; return true; }
        return true;
    }

    @Override
    public boolean mouseReleased(Click c) { dragging = null; draggingWin = false; return true; }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        float mx = (float) x / sc, my = (float) y / sc;
        if (opened != null && tab >= 0 && in(mx, my, panX, panY, panW, panH) && panelAnim.get() > 0.5f) {
            sideScroll = (float) Math.max(0, Math.min(sideMax, sideScroll - v * 24));
        } else {
            scroll = (float) Math.max(0, Math.min(maxScroll, scroll - v * 30));
        }
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        int cp = input.codepoint();
        if (cp < 32 || cp == 127) return false;
        String ch = new String(Character.toChars(cp));
        if (searching) { if (search.length() < 24) { search += ch; scroll = 0; } return true; }
        if (typingProfile) { if (profileName.length() < 20) profileName += ch; return true; }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int k = input.key();
        if (binding != null) { binding.setBind(k == 256 ? -1 : k); binding = null; return true; }
        if (searching) {
            if (k == 259 && !search.isEmpty()) { search = search.substring(0, search.length() - 1); scroll = 0; }
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
        if (k == 256 && opened != null) { opened = null; return true; }
        if (k == 344 || k == 256) { this.close(); return true; }
        if (k == 70 && (input.modifiers() & 2) != 0) { searching = true; return true; } // Ctrl+F
        return super.keyPressed(input);
    }

    @Override public boolean shouldPause() { return false; }
}
