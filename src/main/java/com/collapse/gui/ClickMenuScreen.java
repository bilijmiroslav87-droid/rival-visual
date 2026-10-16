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
 * Меню в стиле Glass Menu: матовое стекло, слева сайдбар с вкладками, справа сетка карточек в 2 колонки.
 * Карточка: название, статус (On/Off или значение), тумблер. ЛКМ: вкл/выкл. ПКМ: окно настроек модуля.
 * Акцентный цвет используется только у включённых переключателей и ползунков.
 */
public class ClickMenuScreen extends Screen {
    // ---- геометрия (виртуальные пиксели) ----
    private static final int W = 640, H = 420;
    private static final int SIDE_W = 150;
    private static final int NAV_X = 10, NAV_Y = 62, NAV_H = 28, NAV_STEP = 32;
    private static final int GRID_X = SIDE_W + 20, GRID_Y = 62, GRID_BOTTOM = H - 16;
    private static final int GRID_W = W - GRID_X - 18;
    private static final int GAP = 10, CARD_W = (GRID_W - GAP) / 2, CARD_H = 54, STEP = CARD_H + GAP;
    private static final int POP_W = 250;

    // ---- цвета: матовое стекло ----
    private static final int BODY = 0xDC0C0C10;
    private static final int TEXT = 0xFFF2F2F5, DIM = 0xFF8A8F98;

    private static final String[] TABS = {"Visuals", "Cosmetics", "HUD", "Player", "Movement", "Combat", "Misc"};

    private static int tab = 0;
    private static final int[] TSCROLL = new int[TABS.length];

    private final Anim openAnim = new Anim(0f, 12f);
    private final Map<Object, Anim> anims = new HashMap<>();

    private Module popup = null;
    private int popScroll = 0;
    private Module binding = null;
    private NumSetting dragging = null;
    private float dragX, dragW;
    private String search = "";
    private boolean searching = false;
    private String profileName = "";
    private boolean typingProfile = false;
    private boolean profilesOpen = false;
    private String toast = "";
    private long toastAt = 0;
    private int profScroll = 0;

    private float ui = 1f;
    private final List<Hit> hits = new ArrayList<>();
    private final Map<NumSetting, float[]> dragFor = new HashMap<>();

    private record Hit(int x, int y, int w, int h, int type, Object a, int b, int mask) {}
    private static final int H_TAB = 1, H_CARD = 2, H_SLIDER = 3, H_MODE = 4, H_COLOR = 5, H_BOOL = 6, H_SEARCH = 7,
            H_PCLOSE = 8, H_PBODY = 9, H_PSAVE = 10, H_PLOAD = 11, H_PDEL = 12, H_PNAME = 13, H_PROF = 14,
            H_PROFCLOSE = 15, H_PROFBODY = 16, H_THEME = 17;

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

    private float computeUi() {
        float s = Math.min(width / (float) (W + 24), height / (float) (H + 24));
        return Math.max(0.5f, Math.min(1.6f, s)) * Math.max(0.7f, Math.min(1.3f, ClientSettings.menuScale));
    }

    private int vW() { return Math.round(width / ui); }
    private int vH() { return Math.round(height / ui); }
    private int left() { return (vW() - W) / 2; }
    private int top() { return (vH() - H) / 2; }

    /** Во вкладку попадают модули по категориям и имени. */
    private static int tabOf(Module m) {
        switch (m.getCategory()) {
            case COSMETICS: return 1;
            case PLAYER: return 3;
            case MOVEMENT: return 4;
            case COMBAT: return 5;
            case MISC: return 6;
            default: break;
        }
        return switch (m.getName()) {
            case "TargetHUD", "Watermark", "Notifications", "Keystrokes", "Potions", "Armor", "Crosshair", "Vignette" -> 2;
            default -> 0;
        };
    }

    private List<Module> cards() {
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

    private int contentH(int n) { int rows = (n + 1) / 2; return rows == 0 ? 0 : (rows - 1) * STEP + CARD_H; }
    private int maxScroll(int n) { return Math.max(0, contentH(n) - (GRID_BOTTOM - GRID_Y)); }

    private static int a(int rgb, float alpha) {
        return (Math.max(0, Math.min(255, Math.round(alpha))) << 24) | (rgb & 0xFFFFFF);
    }

    // ================= отрисовка =================
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        try {
            ui = computeUi();
            float p = Anim.ease(openAnim.get());
            int vw = vW(), vh = vH();
            int x = left(), y = top() + Math.round((1f - p) * 12f);
            int ta = Math.round(255 * p);
            int mx = Math.round(mouseX / ui), my = Math.round(mouseY / ui);
            hits.clear();
            boolean modal = popup != null || profilesOpen;
            int hmx = modal ? -9999 : mx, hmy = modal ? -9999 : my;

            var mat = ctx.getMatrices();
            mat.pushMatrix();
            mat.scale(ui, ui);

            if (ClientSettings.dimBackground) ctx.fill(0, 0, vw, vh, a(0x000000, 0x66 * p));
            Draw.shadow(ctx, x, y, W, H, 12, 18);
            Draw.roundRect(ctx, x, y, W, H, 12, a(0xFFFFFF, 0x1A * p));
            Draw.roundRect(ctx, x + 1, y + 1, W - 2, H - 2, 11, a(0x0C0C10, 0xDC * p));

            int acc = Theme.accent();
            drawSidebar(ctx, x, y, hmx, hmy, p, ta, acc);
            drawContent(ctx, x, y, hmx, hmy, p, ta, acc);

            if (ClientSettings.showHints) {
                String hint = binding != null ? "Нажмите клавишу для «" + binding.getName() + "» · ESC: сбросить"
                        : "ЛКМ вкл/выкл  ·  ПКМ настройки  ·  СКМ бинд  ·  Ctrl+F поиск  ·  RShift закрыть";
                Fonts.MEDIUM.draw(ctx, hint, x + 4, y + H + 8, 8f, a(binding != null ? acc : 0xFFFFFF, 0x80 * p));
            }
            String diag = "font " + (Fonts.MEDIUM.isOk() ? "ok (Poppins)" : "FALLBACK (vanilla)")
                    + "   aura " + com.collapse.modules.ParticleCosmetics.class.getSimpleName();
            Fonts.MEDIUM.drawRight(ctx, "font " + (Fonts.MEDIUM.isOk() ? "ok" : "FALLBACK") + "  v" + CollapseClient.VERSION, x + W - 4, y + H + 8, 7.5f, a(0xFFFFFF, 0x55 * p));
            if (System.currentTimeMillis() - toastAt < 1800 && !toast.isEmpty()) {
                int tw = Math.round(Fonts.BOLD.width(toast, 9f)) + 28;
                int tx = x + W / 2 - tw / 2, ty = y + H - 44;
                Draw.roundRect(ctx, tx, ty, tw, 22, 8, a(0xFFFFFF, 0x30));
                Draw.roundRect(ctx, tx + 1, ty + 1, tw - 2, 20, 7, a(0x14161C, 0xF4));
                Fonts.BOLD.drawCentered(ctx, toast, tx + tw / 2f, ty + 7, 9f, a(0xF2F2F5, ta));
            }
            if (popup != null) drawPopup(ctx, x, y, mx, my, p, ta, acc);
            if (profilesOpen) drawProfiles(ctx, x, y, mx, my, p, ta, acc);

            mat.popMatrix();
        } catch (Throwable t) {
            CollapseClient.LOGGER.error("Menu render error", t);
        }
    }

    private void drawSidebar(DrawContext ctx, int x, int y, int mx, int my, float p, int ta, int acc) {
        Fonts.HEAD.draw(ctx, "COLLAPSE", x + NAV_X + 2, y + 22, 15f, a(0xF2F2F5, ta));
        Fonts.MEDIUM.draw(ctx, "client  " + CollapseClient.VERSION, x + NAV_X + 3, y + 42, 8f, a(0x8A8F98, ta));
        ctx.fill(x + SIDE_W, y + 16, x + SIDE_W + 1, y + H - 16, a(0xFFFFFF, 0x14 * p));

        for (int i = 0; i < TABS.length; i++) {
            int ny = y + NAV_Y + i * NAV_STEP;
            boolean sel = tab == i && search.isEmpty();
            boolean hv = inside(mx, my, x + NAV_X, ny, SIDE_W - NAV_X * 2, NAV_H);
            float t = smooth("nav" + i, sel ? 1f : (hv ? 0.9f : 0.45f), 12f);
            float s = smooth("navs" + i, sel ? 1f : 0f, 14f);
            if (s > 0.01f) {
                Draw.roundRect(ctx, x + NAV_X, ny, SIDE_W - NAV_X * 2, NAV_H, 8, a(0xFFFFFF, 12 * p * s));
                Draw.roundRect(ctx, x + NAV_X, ny + 7, 2, NAV_H - 14, 1, a(0xFFFFFF, 0xE0 * p * s));
            }
            Fonts.MEDIUM.draw(ctx, TABS[i], x + NAV_X + 14, ny + 9, 9.5f, a(0xF2F2F5, 255 * t * p));
            hits.add(new Hit(x + NAV_X, ny, SIDE_W - NAV_X * 2, NAV_H, H_TAB, i, 0, 1));
        }

        // нижние кнопки сайдбара: профили и тема
        int by = y + H - 84;
        sideBtn(ctx, x + NAV_X, by, "Profiles", mx, my, p, ta, 1);
        sideBtn(ctx, x + NAV_X, by + 36, "Theme: " + Theme.name(), mx, my, p, ta, 2);
    }

    private void sideBtn(DrawContext ctx, int bx, int by, String label, int mx, int my, float p, int ta, int id) {
        int w = SIDE_W - NAV_X * 2, h = 28;
        boolean hv = inside(mx, my, bx, by, w, h);
        float t = smooth("sb" + id, hv ? 1f : 0f, 16f);
        Draw.roundRect(ctx, bx, by, w, h, 8, a(0xFFFFFF, (0x14 + 0x16 * t) * p));
        Fonts.MEDIUM.drawCentered(ctx, Fonts.MEDIUM.fit(label, w - 12, 9f), bx + w / 2f, by + 9, 9f, a(0xF2F2F5, ta * (0.7f + 0.3f * t)));
        hits.add(new Hit(bx, by, w, h, H_PROF, id, 0, 3));
    }

    private void drawContent(DrawContext ctx, int x, int y, int mx, int my, float p, int ta, int acc) {
        String title = search.isEmpty() ? TABS[tab] : "Search";
        List<Module> list = cards();
        Fonts.HEAD.draw(ctx, title.toUpperCase(), x + GRID_X, y + 20, 15f, a(0xF2F2F5, ta));
        Fonts.MEDIUM.draw(ctx, list.size() + " options", x + GRID_X, y + 40, 8f, a(0x8A8F98, ta));

        // поле поиска справа в шапке
        int sw = 150, sx = x + W - 18 - sw, sy = y + 16;
        float sf = smooth("search", searching ? 1f : 0f, 16f);
        Draw.roundRect(ctx, sx, sy, sw, 24, 8, a(0xFFFFFF, (0x1C + 0x18 * sf) * p));
        Draw.roundRect(ctx, sx + 1, sy + 1, sw - 2, 22, 7, a(0x0C0C10, 0xE0 * p));
        boolean blink = searching && (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = search.isEmpty() ? (searching ? (blink ? "|" : "") : "Search...") : Fonts.MEDIUM.fit(search, sw - 30, 9f) + (blink ? "|" : "");
        Fonts.MEDIUM.draw(ctx, shown, sx + 10, sy + 7, 9f, a(search.isEmpty() ? 0x8A8F98 : 0xF2F2F5, ta));
        if (!search.isEmpty()) Fonts.BOLD.draw(ctx, "x", sx + sw - 14, sy + 6, 10f, a(0x8A8F98, ta));
        hits.add(new Hit(sx, sy, sw, 24, H_SEARCH, null, 0, 1));

        int maxS = maxScroll(list.size());
        TSCROLL[tab] = Math.min(TSCROLL[tab], maxS);
        int scroll = TSCROLL[tab];

        if (list.isEmpty()) {
            Fonts.MEDIUM.drawCentered(ctx, "Ничего не найдено", x + GRID_X + GRID_W / 2f, y + 200, 10f, a(0x8A8F98, ta));
            return;
        }
        ctx.enableScissor(Math.round((x + GRID_X - 4) * ui), Math.round((y + GRID_Y - 2) * ui),
                Math.round((x + W - 8) * ui), Math.round((y + GRID_BOTTOM + 2) * ui));
        for (int i = 0; i < list.size(); i++) {
            int cx = x + GRID_X + (i % 2) * (CARD_W + GAP);
            int cy = y + GRID_Y + (i / 2) * STEP - scroll;
            if (cy + CARD_H < y + GRID_Y - 2 || cy > y + GRID_BOTTOM + 2) continue;
            boolean vis = my >= y + GRID_Y && my <= y + GRID_BOTTOM;
            drawCard(ctx, list.get(i), cx, cy, vis && inside(mx, my, cx, cy, CARD_W, CARD_H), p, ta, acc);
            hits.add(new Hit(cx, cy, CARD_W, CARD_H, H_CARD, list.get(i), 0, 7));
        }
        ctx.disableScissor();
        if (maxS > 0) {
            int track = GRID_BOTTOM - GRID_Y;
            int thumb = Math.max(18, track * track / contentH(list.size()));
            int ty = y + GRID_Y + (track - thumb) * scroll / maxS;
            Draw.roundRect(ctx, x + W - 9, ty, 3, thumb, 1, a(0xFFFFFF, 0x30 * p));
        }
    }

    private void drawCard(DrawContext ctx, Module m, int cx, int cy, boolean hot, float p, int ta, int acc) {
        float on = smooth(m, m.isEnabled() ? 1f : 0f, 14f);
        float hv = smooth("ch" + m.getName(), hot ? 1f : 0f, 16f);
        Draw.roundRect(ctx, cx, cy, CARD_W, CARD_H, 8, a(0xFFFFFF, (0x18 + 0x0E * hv) * p));
        Draw.roundRect(ctx, cx + 1, cy + 1, CARD_W - 2, CARD_H - 2, 7, a(0xFFFFFF, 6 * p));

        Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(m.getName(), CARD_W - 64, 10f), cx + 12, cy + 11, 10f, a(0xF2F2F5, ta));
        String sub = m.getBind() > 0 ? (m.isEnabled() ? "On" : "Off") + "  ·  " + keyName(m.getBind()) : (m.isEnabled() ? "On" : "Off");
        if (binding == m) sub = "press a key...";
        Fonts.MEDIUM.draw(ctx, sub, cx + 12, cy + 27, 8f, a(binding == m ? acc : 0x8A8F98, ta));

        drawSwitch(ctx, cx + CARD_W - 42, cy + 19, on, p);
        if (!m.getSettings().isEmpty()) {
            Fonts.MEDIUM.draw(ctx, m.getSettings().size() + " opt", cx + 12, cy + CARD_H - 13, 7f, a(0x8A8F98, ta * 0.7f));
        }
    }

    private void drawSwitch(DrawContext ctx, int x, int y, float anim, float p) {
        int w = 30, h = 16;
        int acc = Theme.accent();
        int track = Draw.lerpArgb(a(0xFFFFFF, 0x30), a(acc, 0xFF), anim);
        Draw.roundRect(ctx, x, y, w, h, 8, withA(track, p));
        int kx = Math.round(x + 2 + (w - h) * anim);
        Draw.roundRect(ctx, kx, y + 2, h - 4, h - 4, 6, a(0xFFFFFF, 255 * p));
    }

    private static int withA(int argb, float p) {
        int al = Math.round(((argb >>> 24) & 0xFF) * p);
        return (al << 24) | (argb & 0xFFFFFF);
    }

    // ---------- окно настроек модуля ----------
    private int popH(Module m) {
        int h = 56;
        for (Setting<?> s : m.getSettings()) h += s instanceof NumSetting ? 34 : 26;
        return Math.min(h + 8, 360);
    }

    private void drawPopup(DrawContext ctx, int x, int y, int mx, int my, float p, int ta, int acc) {
        Module m = popup;
        int ph = popH(m);
        int px = x + (W - POP_W) / 2, py = y + (H - ph) / 2;
        ctx.fill(x, y, x + W, y + H, a(0x000000, 0x66));
        hits.add(new Hit(x, y, W, H, H_PCLOSE, null, 0, 7));
        Draw.shadow(ctx, px, py, POP_W, ph, 12, 18);
        Draw.roundRect(ctx, px, py, POP_W, ph, 12, a(0xFFFFFF, 0x1A));
        Draw.roundRect(ctx, px + 1, py + 1, POP_W - 2, ph - 2, 11, a(0x14161C, 0xF8));
        hits.add(new Hit(px, py, POP_W, ph, H_PBODY, null, 0, 7));

        Fonts.MEDIUM.draw(ctx, m.getName(), px + 16, py + 14, 11f, a(0xF2F2F5, ta));
        boolean cx = inside(mx, my, px + POP_W - 28, py + 8, 20, 20);
        Fonts.BOLD.draw(ctx, "x", px + POP_W - 20, py + 13, 11f, a(cx ? 0xF2F2F5 : 0x8A8F98, ta));
        hits.add(new Hit(px + POP_W - 28, py + 8, 20, 20, H_PCLOSE, null, 0, 1));
        ctx.fill(px + 16, py + 34, px + POP_W - 16, py + 35, a(0xFFFFFF, 0x18));
        Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(m.getDescription(), POP_W - 32, 8f), px + 16, py + 40, 8f, a(0x8A8F98, ta));

        int sy = py + 58;
        ctx.enableScissor(Math.round(px * ui), Math.round((py + 54) * ui), Math.round((px + POP_W) * ui), Math.round((py + ph - 6) * ui));
        for (Setting<?> s : m.getSettings()) {
            int sh = s instanceof NumSetting ? 34 : 26;
            drawSetting(ctx, s, px + 16, sy - popScroll, POP_W - 32, mx, my, p, ta, acc);
            sy += sh;
        }
        ctx.disableScissor();
    }

    private void drawSetting(DrawContext ctx, Setting<?> s, int x, int y, int w, int mx, int my, float p, int ta, int acc) {
        if (s instanceof BoolSetting b) {
            float t = smooth(b, b.get() ? 1f : 0f, 16f);
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w - 44, 9f), x, y + 4, 9f, a(0xF2F2F5, ta));
            drawSwitch(ctx, x + w - 30, y + 1, t, 1f);
            hits.add(new Hit(x - 4, y - 2, w + 8, 22, H_BOOL, s, 0, 1));
        } else if (s instanceof ColorSetting cs) {
            Fonts.MEDIUM.draw(ctx, s.getName(), x, y + 4, 9f, a(0xF2F2F5, ta));
            Draw.roundRect(ctx, x + w - 34, y, 34, 18, 6, a(0xFFFFFF, 0x30));
            Draw.roundRect(ctx, x + w - 33, y + 1, 32, 16, 5, 0xFF000000 | (cs.get() & 0xFFFFFF));
            hits.add(new Hit(x - 4, y - 2, w + 8, 22, H_COLOR, s, 0, 3));
        } else if (s instanceof ModeSetting ms) {
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w * 0.45f, 9f), x, y + 4, 9f, a(0xF2F2F5, ta));
            int dw = Math.min(Math.round(w * 0.55f), 120), dx = x + w - dw;
            boolean hov = inside(mx, my, dx, y - 1, dw, 20);
            Draw.roundRect(ctx, dx, y - 1, dw, 20, 7, a(0xFFFFFF, hov ? 0x26 : 0x18));
            Fonts.MEDIUM.drawCentered(ctx, Fonts.MEDIUM.fit(ms.get(), dw - 26, 8.5f), dx + dw / 2f, y + 4, 8.5f, a(0xF2F2F5, ta));
            Fonts.BOLD.draw(ctx, "<", dx + 6, y + 3, 9f, a(0x8A8F98, ta));
            Fonts.BOLD.drawRight(ctx, ">", dx + dw - 6, y + 3, 9f, a(0x8A8F98, ta));
            hits.add(new Hit(dx, y - 1, dw / 2, 20, H_MODE, ms, -1, 3));
            hits.add(new Hit(dx + dw / 2, y - 1, dw / 2, 20, H_MODE, ms, 1, 3));
        } else if (s instanceof NumSetting ns) {
            float pr = smooth(ns, (float) ns.getPercent(), 22f);
            String v = (ns.get() % 1 == 0) ? String.valueOf(ns.get().intValue()) : String.format(java.util.Locale.US, "%.2f", ns.get());
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(s.getName(), w - 50, 9f), x, y + 1, 9f, a(0xF2F2F5, ta));
            Fonts.MEDIUM.drawRight(ctx, v, x + w, y + 1, 9f, a(0x8A8F98, ta));
            int by = y + 20;
            Draw.roundRect(ctx, x, by, w, 4, 2, a(0xFFFFFF, 0x22));
            int fw = Math.max(4, Math.round(w * pr));
            Draw.roundRect(ctx, x, by, fw, 4, 2, a(acc, 0xE0));
            Draw.circle(ctx, x + fw - 4, by - 2, 8, a(0xFFFFFF, 0xFF));
            hits.add(new Hit(x - 5, y + 12, w + 10, 20, H_SLIDER, ns, 0, 1));
            dragFor.put(ns, new float[]{x, w});
        }
    }

    // ---------- профили ----------
    private void drawProfiles(DrawContext ctx, int x, int y, int mx, int my, float p, int ta, int acc) {
        int pw = 300, ph = 280;
        int px = x + (W - pw) / 2, py = y + (H - ph) / 2;
        ctx.fill(x, y, x + W, y + H, a(0x000000, 0x66));
        hits.add(new Hit(x, y, W, H, H_PROFCLOSE, null, 0, 7));
        Draw.shadow(ctx, px, py, pw, ph, 12, 18);
        Draw.roundRect(ctx, px, py, pw, ph, 12, a(0xFFFFFF, 0x1A));
        Draw.roundRect(ctx, px + 1, py + 1, pw - 2, ph - 2, 11, a(0x14161C, 0xF8));
        hits.add(new Hit(px, py, pw, ph, H_PROFBODY, null, 0, 7));
        Fonts.MEDIUM.draw(ctx, "Profiles", px + 16, py + 14, 11f, a(0xF2F2F5, ta));
        boolean cx = inside(mx, my, px + pw - 28, py + 8, 20, 20);
        Fonts.BOLD.draw(ctx, "x", px + pw - 20, py + 13, 11f, a(cx ? 0xF2F2F5 : 0x8A8F98, ta));
        hits.add(new Hit(px + pw - 28, py + 8, 20, 20, H_PROFCLOSE, null, 0, 1));
        ctx.fill(px + 16, py + 34, px + pw - 16, py + 35, a(0xFFFFFF, 0x18));

        int ax = px + 16, aw = pw - 32, ay = py + 46, fh = 26, bw = 84, fw = aw - bw - 8;
        float fa = smooth("pname", typingProfile ? 1f : 0f, 16f);
        Draw.roundRect(ctx, ax, ay, fw, fh, 8, a(0xFFFFFF, 0x18 + 0x18 * fa));
        Draw.roundRect(ctx, ax + 1, ay + 1, fw - 2, fh - 2, 7, a(0x0C0C10, 0xE0));
        boolean blink = typingProfile && (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = profileName.isEmpty() ? (typingProfile ? (blink ? "|" : "") : "Profile name") : profileName + (blink ? "|" : "");
        Fonts.MEDIUM.draw(ctx, shown, ax + 10, ay + 8, 9f, a(profileName.isEmpty() ? 0x8A8F98 : 0xF2F2F5, ta));
        hits.add(new Hit(ax, ay, fw, fh, H_PNAME, null, 0, 1));
        Draw.roundRect(ctx, ax + fw + 8, ay, bw, fh, 8, a(0xFFFFFF, 0x24));
        Fonts.MEDIUM.drawCentered(ctx, "Save", ax + fw + 8 + bw / 2f, ay + 8, 9f, a(0xF2F2F5, ta));
        hits.add(new Hit(ax + fw + 8, ay, bw, fh, H_PSAVE, null, 0, 1));

        List<String> ps = Config.profiles();
        int ly = ay + fh + 14, viewH = py + ph - ly - 12;
        Fonts.MEDIUM.draw(ctx, "Active: " + Config.currentProfile, ax + 2, ly - 2, 8f, a(0x8A8F98, ta));
        ly += 14; viewH -= 14;
        ctx.enableScissor(Math.round(ax * ui), Math.round(ly * ui), Math.round((ax + aw) * ui), Math.round((ly + viewH) * ui));
        int cy = ly - profScroll;
        if (ps.isEmpty()) Fonts.MEDIUM.drawCentered(ctx, "No profiles yet", ax + aw / 2f, ly + 28, 9f, a(0x8A8F98, ta));
        for (String pn : ps) {
            boolean act = pn.equals(Config.currentProfile);
            Draw.roundRect(ctx, ax, cy, aw, 30, 8, a(0xFFFFFF, act ? 0x22 : 0x14));
            Fonts.MEDIUM.draw(ctx, Fonts.MEDIUM.fit(pn, aw - 150, 9.5f), ax + 12, cy + 10, 9.5f, a(0xF2F2F5, ta));
            int bx = ax + aw - 8 - 52;
            Draw.roundRect(ctx, bx, cy + 5, 52, 20, 6, a(0xFFFFFF, 0x18));
            Fonts.MEDIUM.drawCentered(ctx, "Delete", bx + 26, cy + 10, 8f, a(0xFF8A8A, ta));
            hits.add(new Hit(bx, cy + 5, 52, 20, H_PDEL, pn, 0, 1));
            bx -= 58;
            Draw.roundRect(ctx, bx, cy + 5, 52, 20, 6, a(0xFFFFFF, 0x24));
            Fonts.MEDIUM.drawCentered(ctx, "Load", bx + 26, cy + 10, 8f, a(0xF2F2F5, ta));
            hits.add(new Hit(bx, cy + 5, 52, 20, H_PLOAD, pn, 0, 1));
            cy += 36;
        }
        ctx.disableScissor();
    }

    // ================= ввод =================
    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
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
        int mx = Math.round((float) click.x() / ui), my = Math.round((float) click.y() / ui);
        int btn = click.button();
        int bit = btn == 0 ? 1 : btn == 1 ? 2 : 4;
        searching = false; typingProfile = false;

        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!inside(mx, my, h.x, h.y, h.w, h.h)) continue;
            if ((h.mask & bit) == 0) {
                if (h.type == H_PBODY || h.type == H_PROFBODY || h.type == H_PCLOSE || h.type == H_PROFCLOSE) return true;
                continue;
            }
            switch (h.type) {
                case H_PBODY, H_PROFBODY -> { return true; }
                case H_PCLOSE -> { popup = null; popScroll = 0; return true; }
                case H_PROFCLOSE -> { profilesOpen = false; typingProfile = false; return true; }
                case H_TAB -> { tab = (Integer) h.a; search = ""; return true; }
                case H_SEARCH -> {
                    if (!search.isEmpty() && mx > h.x + h.w - 22) search = ""; else searching = true;
                    return true;
                }
                case H_PROF -> {
                    int id = (Integer) h.a;
                    if (id == 1) profilesOpen = true;
                    else { if (btn == 1) Theme.prev(); else Theme.next(); }
                    return true;
                }
                case H_CARD -> {
                    Module m = (Module) h.a;
                    if (btn == 2) binding = m;
                    else if (btn == 1) { if (!m.getSettings().isEmpty()) { popup = m; popScroll = 0; } else binding = m; }
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
                    float[] d = dragFor.get(dragging);
                    dragX = d != null ? d[0] : h.x + 5;
                    dragW = d != null ? d[1] : h.w - 10;
                    applyDrag(mx);
                    return true;
                }
                case H_PNAME -> { typingProfile = true; return true; }
                case H_PSAVE -> {
                    String n = profileName.trim();
                    if (n.isEmpty()) { toast("Enter a profile name"); typingProfile = true; return true; }
                    if (Config.saveProfile(n)) { toast("Saved \"" + n + "\""); profileName = ""; } else toast("Save failed");
                    return true;
                }
                case H_PLOAD -> { String n = (String) h.a; toast(Config.loadProfile(n) ? "Loaded \"" + n + "\"" : "Load failed"); return true; }
                case H_PDEL -> { String n = (String) h.a; Config.deleteProfile(n); toast("Deleted \"" + n + "\""); return true; }
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
        if (dragging != null) { applyDrag(c.x() / ui); return true; }
        return true;
    }

    @Override
    public boolean mouseReleased(Click c) { dragging = null; return true; }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        if (popup != null) {
            popScroll = (int) Math.max(0, popScroll - v * 16);
            int total = 0;
            for (Setting<?> s : popup.getSettings()) total += s instanceof NumSetting ? 34 : 26;
            popScroll = Math.min(popScroll, Math.max(0, total + 62 - popH(popup)));
            return true;
        }
        if (profilesOpen) { profScroll = (int) Math.max(0, profScroll - v * 18); return true; }
        TSCROLL[tab] = (int) Math.max(0, Math.min(maxScroll(cards().size()), TSCROLL[tab] - v * 14));
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
                if (!n.isEmpty() && Config.saveProfile(n)) { toast("Saved \"" + n + "\""); profileName = ""; }
                typingProfile = false;
            }
            return true;
        }
        if (k == 256 && popup != null) { popup = null; popScroll = 0; return true; }
        if (k == 256 && profilesOpen) { profilesOpen = false; return true; }
        if (k == 344 || k == 256) { this.close(); return true; }
        if (k == 70 && (input.modifiers() & 2) != 0) { searching = true; return true; }
        return super.keyPressed(input);
    }

    @Override public boolean shouldPause() { return false; }
}
