package com.collapse.gui;

import com.collapse.CollapseClient;
import com.collapse.Module;
import com.collapse.ModuleManager;
import com.collapse.config.Config;
import com.collapse.render.Anim;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.ColorSetting;
import com.collapse.settings.ModeSetting;
import com.collapse.settings.NumSetting;
import com.collapse.settings.Setting;
import com.collapse.util.Theme;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Меню в стиле Nursultan/Celestial: окно со скруглениями, сайдбар, карточки, слайдеры, анимации. */
public class ClickMenuScreen extends Screen {
    private static final int C_BG = 0xF2121218;
    private static final int C_SIDE = 0xFF0E0E13;
    private static final int C_CARD = 0xFF1A1A22;
    private static final int C_CARD_H = 0xFF21212B;
    private static final int C_LINE = 0xFF24242E;
    private static final int C_TEXT = 0xFFF2F2F7;
    private static final int C_MUTED = 0xFF8A8A9A;

    private Module.Category selected = Module.Category.COMBAT;
    private Module open = null;          // модуль, чьи настройки показаны
    private Module binding = null;       // модуль, которому назначается клавиша
    private final Anim openAnim = new Anim(0f, 11f);
    private final Anim scrollAnim = new Anim(0f, 16f);
    private final Anim tabBar = new Anim(0f, 14f);
    private final Anim settingsAnim = new Anim(0f, 14f);
    private final Map<Module, Anim> hover = new HashMap<>();
    private final Map<Setting<?>, Anim> sliderAnim = new HashMap<>();
    private final Map<Setting<?>, Anim> boolAnim = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private NumSetting dragging = null;
    private float dragX, dragW;
    private float scroll = 0, maxScroll = 0;
    private String search = "";
    private boolean searching = false;
    private boolean closing = false;

    private float wx, wy, ww, wh;

    private record Hit(float x, float y, float w, float h, int type, Object a, int b) {}
    private static final int H_TAB = 1, H_MODULE = 2, H_SETTING = 3, H_THEME = 4, H_SEARCH = 5, H_BACK = 6;

    private ClickMenuScreen() { super(Text.literal("Collapse Client")); }
    public static ClickMenuScreen create() { return new ClickMenuScreen(); }

    @Override
    protected void init() {
        openAnim.snap(0f);
        openAnim.set(1f);
        tabBar.snap(selected.ordinal());
    }

    @Override
    public void close() {
        Config.save();
        super.close();
    }

    private Anim hov(Module m) { return hover.computeIfAbsent(m, k -> new Anim(0f, 16f)); }
    private Anim bool(Setting<?> s) { return boolAnim.computeIfAbsent(s, k -> new Anim(((BoolSetting) s).get() ? 1f : 0f, 18f)); }
    private Anim sl(NumSetting s) { return sliderAnim.computeIfAbsent(s, k -> new Anim((float) s.getPercent(), 20f)); }

    private List<Module> list() {
        List<Module> out = new ArrayList<>();
        for (Module m : ModuleManager.MODULES) {
            if (!search.isEmpty()) {
                if (m.getName().toLowerCase().contains(search.toLowerCase())) out.add(m);
            } else if (m.getCategory() == selected) out.add(m);
        }
        return out;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        try {
            float o = Anim.ease(openAnim.get());
            ctx.fill(0, 0, width, height, (int) (150 * o) << 24);

            ww = Math.min(width - 16, 440);
            wh = Math.min(height - 12, 270);
            wx = (width - ww) / 2f;
            wy = (height - wh) / 2f + (1 - o) * 14;
            hits.clear();
            drawWindow(ctx, mx, my, o);
        } catch (Throwable t) {
            CollapseClient.LOGGER.error("Menu render error", t);
        }
    }

    private void drawWindow(DrawContext ctx, int mx, int my, float o) {
        int acc = Draw.alpha(Theme.accent(), o);
        Draw.shadow(ctx, wx, wy, ww, wh, 10, 6);
        Draw.roundRect(ctx, wx - 1, wy - 1, ww + 2, wh + 2, 11, Draw.alpha(Theme.withAlpha(Theme.accent(), 90), o));
        Draw.roundRect(ctx, wx, wy, ww, wh, 10, Draw.alpha(C_BG, o));

        float sideW = 92;
        Draw.roundRect(ctx, wx, wy, sideW, wh, 10, Draw.alpha(C_SIDE, o));
        ctx.fill((int) (wx + sideW - 10), (int) wy, (int) (wx + sideW), (int) (wy + wh), Draw.alpha(C_SIDE, o));
        ctx.fill((int) (wx + sideW), (int) (wy + 8), (int) (wx + sideW + 1), (int) (wy + wh - 8), Draw.alpha(C_LINE, o));

        // логотип
        Draw.roundRect(ctx, wx + 12, wy + 12, 22, 22, 7, acc);
        Fonts.BOLD.drawCentered(ctx, "C", wx + 23, wy + 16, 14f, Draw.alpha(0xFFFFFFFF, o));
        Fonts.BOLD.draw(ctx, "Collapse", wx + 40, wy + 13, 10.5f, Draw.alpha(C_TEXT, o));
        Fonts.MEDIUM.draw(ctx, "SpookyTime", wx + 40, wy + 24, 8f, Draw.alpha(C_MUTED, o));

        // вкладки
        float ty = wy + 50;
        tabBar.set(selected.ordinal());
        float barY = ty + tabBar.get() * 26;
        if (search.isEmpty()) {
            Draw.roundRect(ctx, wx + 8, barY, sideW - 16, 22, 7, Draw.alpha(Theme.withAlpha(Theme.accent(), 60), o));
            Draw.roundRect(ctx, wx + 8, barY + 5, 3, 12, 1, acc);
        }
        for (Module.Category c : Module.Category.values()) {
            boolean sel = c == selected && search.isEmpty();
            boolean hv = in(mx, my, wx + 8, ty, sideW - 16, 22);
            if (hv && !sel) Draw.roundRect(ctx, wx + 8, ty, sideW - 16, 22, 7, Draw.alpha(0x22FFFFFF, o));
            Fonts.MEDIUM.draw(ctx, c.label, wx + 20, ty + 6, 10f, Draw.alpha(sel ? C_TEXT : C_MUTED, o));
            hits.add(new Hit(wx + 8, ty, sideW - 16, 22, H_TAB, c, 0));
            ty += 26;
        }

        // тема
        float thY = wy + wh - 30;
        boolean thh = in(mx, my, wx + 8, thY, sideW - 16, 22);
        Draw.roundRect(ctx, wx + 8, thY, sideW - 16, 22, 7, Draw.alpha(thh ? C_CARD_H : C_CARD, o));
        Draw.roundRect(ctx, wx + 15, thY + 6, 10, 10, 5, acc);
        Fonts.MEDIUM.draw(ctx, Theme.name(), wx + 30, thY + 6, 9.5f, Draw.alpha(C_TEXT, o));
        hits.add(new Hit(wx + 8, thY, sideW - 16, 22, H_THEME, null, 0));

        // шапка контента
        float cx = wx + sideW + 14, cw = ww - sideW - 28;
        float topY = wy + 12;
        String title = open != null ? open.getName() : (search.isEmpty() ? selected.label : "Search");
        Fonts.BOLD.draw(ctx, title, cx, topY, 13f, Draw.alpha(C_TEXT, o));

        // поиск / назад
        if (open != null) {
            float bw = 54;
            boolean bh = in(mx, my, cx + cw - bw, topY - 2, bw, 18);
            Draw.roundRect(ctx, cx + cw - bw, topY - 2, bw, 18, 6, Draw.alpha(bh ? C_CARD_H : C_CARD, o));
            Fonts.MEDIUM.drawCentered(ctx, "< Back", cx + cw - bw / 2, topY + 2, 9.5f, Draw.alpha(C_TEXT, o));
            hits.add(new Hit(cx + cw - bw, topY - 2, bw, 18, H_BACK, null, 0));
        } else {
            float sw = 96;
            Draw.roundRect(ctx, cx + cw - sw, topY - 2, sw, 18, 6, Draw.alpha(searching ? C_CARD_H : C_CARD, o));
            String shown = search.isEmpty() ? (searching ? "|" : "Search...") : search + (searching && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
            Fonts.MEDIUM.draw(ctx, shown, cx + cw - sw + 7, topY + 2, 9.5f, Draw.alpha(search.isEmpty() ? C_MUTED : C_TEXT, o));
            hits.add(new Hit(cx + cw - sw, topY - 2, sw, 18, H_SEARCH, null, 0));
        }

        float viewY = topY + 24, viewH = wh - 24 - 28;
        ctx.enableScissor((int) cx - 2, (int) viewY, (int) (cx + cw + 2), (int) (viewY + viewH));
        scrollAnim.set(scroll);
        float sc = scrollAnim.get();
        if (open == null) drawModules(ctx, mx, my, cx, viewY - sc, cw, viewY, viewH, o);
        else drawSettings(ctx, mx, my, cx, viewY - sc, cw, viewY, viewH, o);
        ctx.disableScissor();

        // подсказка
        String hint = open != null ? "ПКМ или Back: назад" : "ЛКМ вкл/выкл   ПКМ настройки   СКМ бинд";
        if (binding != null) hint = "Нажми клавишу для " + binding.getName() + " (ESC сброс)";
        Fonts.MEDIUM.draw(ctx, hint, cx, wy + wh - 18, 8.5f, Draw.alpha(C_MUTED, o));
        String cnt = ModuleManager.MODULES.size() + " modules";
        Fonts.MEDIUM.drawRight(ctx, cnt, cx + cw, wy + wh - 18, 8.5f, Draw.alpha(C_MUTED, o));
    }

    private void drawModules(DrawContext ctx, int mx, int my, float cx, float top, float cw, float viewY, float viewH, float o) {
        List<Module> mods = list();
        int cols = cw >= 280 ? 2 : 1;
        float gap = 8, cardW = (cw - gap * (cols - 1)) / cols, cardH = 42;
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            float x = cx + (i % cols) * (cardW + gap);
            float y = top + (i / cols) * (cardH + gap);
            if (y + cardH < viewY - 4 || y > viewY + viewH + 4) {
                hits.add(new Hit(x, y, cardW, cardH, H_MODULE, m, 0));
                continue;
            }
            boolean vis = my >= viewY && my <= viewY + viewH;
            boolean hv = vis && in(mx, my, x, y, cardW, cardH);
            Anim ha = hov(m);
            ha.set(hv ? 1f : 0f);
            float h = ha.get();
            float on = m.anim.get();

            int base = Theme.mix(C_CARD, C_CARD_H, h);
            Draw.roundRect(ctx, x, y, cardW, cardH, 8, Draw.alpha(base, o));
            if (on > 0.01f) {
                Draw.roundRect(ctx, x, y, cardW, cardH, 8, Draw.alpha(Theme.withAlpha(Theme.accent(), (int) (40 * on)), o));
                Draw.roundRect(ctx, x + 1, y + cardH - 3, (cardW - 2) * on, 2, 1, Draw.alpha(Theme.accent(), o * on));
            }
            Fonts.BOLD.draw(ctx, m.getName(), x + 11, y + 8, 10.5f, Draw.alpha(on > 0.5f ? C_TEXT : 0xFFD6D6E0, o));
            String d = trim(m.getDescription(), cardW - 70, 8f);
            Fonts.MEDIUM.draw(ctx, d, x + 11, y + 23, 8f, Draw.alpha(C_MUTED, o));

            // переключатель
            float sx = x + cardW - 36, sy = y + 8;
            Draw.roundRect(ctx, sx, sy, 26, 14, 7, Draw.alpha(Theme.mix(0xFF33333F, Theme.accent(), on), o));
            Draw.roundRect(ctx, sx + 2 + 12 * on, sy + 2, 10, 10, 5, Draw.alpha(0xFFFFFFFF, o));

            // бинд / шестерёнка
            if (m.getBind() > 0) {
                String kn = keyName(m.getBind());
                float kw = Fonts.MEDIUM.width(kn, 7.5f) + 8;
                Draw.roundRect(ctx, x + cardW - kw - 10, y + 27, kw, 11, 4, Draw.alpha(0xFF2A2A36, o));
                Fonts.MEDIUM.drawCentered(ctx, kn, x + cardW - kw / 2 - 10, y + 29, 7.5f, Draw.alpha(C_MUTED, o));
            } else if (!m.getSettings().isEmpty()) {
                Fonts.MEDIUM.drawRight(ctx, "...", x + cardW - 11, y + 26, 10f, Draw.alpha(C_MUTED, o));
            }
            hits.add(new Hit(x, y, cardW, cardH, H_MODULE, m, 0));
        }
        int rows = (mods.size() + cols - 1) / cols;
        maxScroll = Math.max(0, rows * (cardH + gap) - viewH);
        if (scroll > maxScroll) scroll = maxScroll;
    }

    private void drawSettings(DrawContext ctx, int mx, int my, float cx, float top, float cw, float viewY, float viewH, float o) {
        Module m = open;
        float y = top;
        Fonts.MEDIUM.draw(ctx, m.getDescription(), cx, y, 9f, Draw.alpha(C_MUTED, o));
        y += 16;

        // карточка включения
        boolean ho = in(mx, my, cx, y, cw, 30) && my >= viewY && my <= viewY + viewH;
        float on = m.anim.get();
        Draw.roundRect(ctx, cx, y, cw, 30, 8, Draw.alpha(ho ? C_CARD_H : C_CARD, o));
        Fonts.BOLD.draw(ctx, "Enabled", cx + 11, y + 9, 10.5f, Draw.alpha(C_TEXT, o));
        float sx = cx + cw - 38, sy = y + 8;
        Draw.roundRect(ctx, sx, sy, 28, 14, 7, Draw.alpha(Theme.mix(0xFF33333F, Theme.accent(), on), o));
        Draw.roundRect(ctx, sx + 2 + 14 * on, sy + 2, 10, 10, 5, Draw.alpha(0xFFFFFFFF, o));
        hits.add(new Hit(cx, y, cw, 30, H_MODULE, m, 99));
        y += 36;

        for (Setting<?> s : m.getSettings()) {
            float h = (s instanceof NumSetting) ? 40 : 30;
            boolean hv = my >= viewY && my <= viewY + viewH && in(mx, my, cx, y, cw, h);
            Draw.roundRect(ctx, cx, y, cw, h, 8, Draw.alpha(hv ? C_CARD_H : C_CARD, o));
            Fonts.MEDIUM.draw(ctx, s.getName(), cx + 11, y + (s instanceof NumSetting ? 7 : 10), 9.5f, Draw.alpha(C_TEXT, o));

            if (s instanceof BoolSetting b) {
                Anim a = bool(b); a.set(b.get() ? 1f : 0f);
                float t = a.get();
                float bx = cx + cw - 38, by = y + 8;
                Draw.roundRect(ctx, bx, by, 28, 14, 7, Draw.alpha(Theme.mix(0xFF33333F, Theme.accent(), t), o));
                Draw.roundRect(ctx, bx + 2 + 14 * t, by + 2, 10, 10, 5, Draw.alpha(0xFFFFFFFF, o));
                hits.add(new Hit(cx, y, cw, h, H_SETTING, s, 0));
            } else if (s instanceof ModeSetting ms) {
                String v = ms.get();
                float vw = Fonts.MEDIUM.width(v, 9.5f) + 16;
                Draw.roundRect(ctx, cx + cw - vw - 10, y + 6, vw, 18, 6, Draw.alpha(0xFF2A2A36, o));
                Fonts.MEDIUM.drawCentered(ctx, v, cx + cw - vw / 2 - 10, y + 10, 9.5f, Draw.alpha(Theme.accent(), o));
                hits.add(new Hit(cx, y, cw, h, H_SETTING, s, 0));
            } else if (s instanceof ColorSetting cs) {
                Draw.roundRect(ctx, cx + cw - 36, y + 7, 26, 16, 5, Draw.alpha(0xFFFFFFFF, o));
                Draw.roundRect(ctx, cx + cw - 35, y + 8, 24, 14, 4, Draw.alpha(cs.get(), o));
                hits.add(new Hit(cx, y, cw, h, H_SETTING, s, 0));
            } else if (s instanceof NumSetting ns) {
                Anim a = sl(ns); a.set((float) ns.getPercent());
                float p = a.get();
                String v = (ns.get() % 1 == 0) ? String.valueOf(ns.get().intValue()) : String.format("%.2f", ns.get());
                Fonts.MEDIUM.drawRight(ctx, v, cx + cw - 11, y + 7, 9.5f, Draw.alpha(Theme.accent(), o));
                float bx = cx + 11, bw = cw - 22, by = y + 28;
                Draw.roundRect(ctx, bx, by, bw, 4, 2, Draw.alpha(0xFF33333F, o));
                Draw.roundRect(ctx, bx, by, Math.max(4, bw * p), 4, 2, Draw.alpha(Theme.accent(), o));
                Draw.roundRect(ctx, bx + bw * p - 4, by - 3, 9, 10, 4, Draw.alpha(0xFFFFFFFF, o));
                hits.add(new Hit(bx - 6, y + 20, bw + 12, 20, H_SETTING, s, 1));
            }
            y += h + 6;
        }
        maxScroll = Math.max(0, (y - top) - viewH + 4);
        if (scroll > maxScroll) scroll = maxScroll;
    }

    private String trim(String s, float maxW, float size) {
        if (Fonts.MEDIUM.width(s, size) <= maxW) return s;
        while (s.length() > 3 && Fonts.MEDIUM.width(s + "..", size) > maxW) s = s.substring(0, s.length() - 1);
        return s + "..";
    }

    private static boolean in(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static String keyName(int key) {
        String n = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
        if (n != null) return n.toUpperCase();
        return switch (key) {
            case 344 -> "RSHIFT"; case 340 -> "LSHIFT"; case 345 -> "RCTRL"; case 341 -> "LCTRL";
            case 346 -> "RALT"; case 342 -> "LALT"; case 32 -> "SPACE"; case 258 -> "TAB";
            default -> "K" + key;
        };
    }

    // ---------------- ввод ----------------
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int btn = click.button();
        searching = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!in((float) mx, (float) my, h.x, h.y, h.w, h.h)) continue;
            switch (h.type) {
                case H_TAB -> {
                    if (btn == 0) { selected = (Module.Category) h.a; open = null; search = ""; scroll = 0; scrollAnim.snap(0); }
                    return true;
                }
                case H_THEME -> { Theme.next(); return true; }
                case H_SEARCH -> { searching = true; open = null; return true; }
                case H_BACK -> { open = null; scroll = 0; return true; }
                case H_MODULE -> {
                    Module m = (Module) h.a;
                    if (h.b == 99) { if (btn == 0) m.toggle(); else if (btn == 1) { open = null; scroll = 0; } return true; }
                    if (btn == 0) m.toggle();
                    else if (btn == 1) { open = m; scroll = 0; scrollAnim.snap(0); }
                    else if (btn == 2) binding = m;
                    return true;
                }
                case H_SETTING -> {
                    Setting<?> s = (Setting<?>) h.a;
                    if (btn == 1) { open = null; scroll = 0; return true; }
                    if (s instanceof BoolSetting b) b.toggle();
                    else if (s instanceof ModeSetting ms) ms.next();
                    else if (s instanceof ColorSetting cs) cs.next();
                    else if (s instanceof NumSetting ns) {
                        dragging = ns; dragX = h.x + 6; dragW = h.w - 12;
                        applyDrag(mx);
                    }
                    return true;
                }
            }
        }
        if (btn == 1 && open != null) { open = null; scroll = 0; }
        return true;
    }

    private void applyDrag(double mx) {
        if (dragging == null) return;
        double p = Math.max(0, Math.min(1, (mx - dragX) / dragW));
        dragging.set(dragging.getMin() + (dragging.getMax() - dragging.getMin()) * p);
    }

    @Override public boolean mouseDragged(Click c, double dx, double dy) { if (dragging != null) applyDrag(c.x()); return true; }
    @Override public boolean mouseReleased(Click c) { dragging = null; return true; }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        scroll = (float) Math.max(0, Math.min(maxScroll, scroll - v * 28));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int k = input.key();
        if (binding != null) {
            binding.setBind(k == 256 ? -1 : k);
            binding = null;
            return true;
        }
        if (searching) {
            if (k == 259 && !search.isEmpty()) search = search.substring(0, search.length() - 1);
            else if (k == 256 || k == 257) searching = false;
            else if (k >= 32 && k <= 126) {
                char c = (char) k;
                search += Character.toLowerCase(c);
            }
            return true;
        }
        if (k == 344 || k == 256) {
            if (open != null && k == 256) { open = null; return true; }
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override public boolean shouldPause() { return false; }
}
