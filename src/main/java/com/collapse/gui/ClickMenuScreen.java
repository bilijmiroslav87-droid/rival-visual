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

/**
 * Меню по референсу: широкое тёмное окно, сверху панель вкладок-пилюль,
 * ниже колонки, где у каждого модуля сразу видны его настройки.
 */
public class ClickMenuScreen extends Screen {
    private static final int C_BG = 0xFA0C0C10;
    private static final int C_BAR = 0xFF121218;
    private static final int C_LINE = 0xFF1E1E26;
    private static final int C_TEXT = 0xFFEDEDF3;
    private static final int C_MUTED = 0xFF7C7C8A;
    private static final int C_FIELD = 0xFF1A1A22;
    private static final int C_OFF = 0xFF2C2C36;

    private static Module.Category selected = Module.Category.COMBAT;
    private Module binding = null;
    private final Anim openAnim = new Anim(0f, 12f);
    private final Anim pill = new Anim(0f, 16f);
    private final Anim scrollAnim = new Anim(0f, 18f);
    private final Map<Setting<?>, Anim> anims = new HashMap<>();
    private final Map<Module, Anim> modAnim = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private ModeSetting openDrop = null;
    private NumSetting dragging = null;
    private float dragX, dragW;
    private float scroll = 0, maxScroll = 0;
    private String search = "";
    private boolean searching = false;

    private float wx, wy, ww, wh;

    private record Hit(float x, float y, float w, float h, int type, Object a, int b) {}
    private static final int H_TAB = 1, H_MOD = 2, H_SET = 3, H_THEME = 4, H_SEARCH = 5, H_OPT = 6, H_BIND = 7, H_SLIDER = 8;

    private ClickMenuScreen() { super(Text.literal("Collapse Client")); }
    public static ClickMenuScreen create() { return new ClickMenuScreen(); }

    @Override
    protected void init() {
        openAnim.snap(0f);
        openAnim.set(1f);
        pill.snap(selected.ordinal());
    }

    @Override
    public void close() {
        Config.save();
        super.close();
    }

    private Anim an(Setting<?> s, float init) { return anims.computeIfAbsent(s, k -> new Anim(init, 20f)); }
    private Anim ma(Module m) { return modAnim.computeIfAbsent(m, k -> new Anim(m.isEnabled() ? 1f : 0f, 18f)); }

    private List<Module> list() {
        List<Module> out = new ArrayList<>();
        for (Module m : ModuleManager.MODULES) {
            if (!search.isEmpty()) {
                if (m.getName().toLowerCase().contains(search)) out.add(m);
            } else if (m.getCategory() == selected) out.add(m);
        }
        return out;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        try {
            float o = Anim.ease(openAnim.get());
            ctx.fill(0, 0, width, height, (int) (140 * o) << 24);
            ww = Math.min(width - 12, 520);
            wh = Math.min(height - 12, 300);
            wx = (width - ww) / 2f;
            wy = (height - wh) / 2f + (1 - o) * 10;
            hits.clear();
            draw(ctx, mx, my, o);
        } catch (Throwable t) {
            CollapseClient.LOGGER.error("Menu render error", t);
        }
    }

    private void draw(DrawContext ctx, int mx, int my, float o) {
        int acc = Theme.accent();
        Draw.shadow(ctx, wx, wy, ww, wh, 10, 6);
        Draw.roundRect(ctx, wx, wy, ww, wh, 10, Draw.alpha(C_BG, o));

        // ---- верхняя панель ----
        float barH = 34;
        Draw.roundRect(ctx, wx, wy, ww, barH + 8, 10, Draw.alpha(C_BAR, o));
        ctx.fill((int) wx, (int) (wy + barH), (int) (wx + ww), (int) (wy + barH + 1), Draw.alpha(C_LINE, o));

        // логотип
        Draw.roundRect(ctx, wx + 8, wy + 7, 20, 20, 6, Draw.alpha(acc, o));
        Fonts.BOLD.drawCentered(ctx, "C", wx + 18, wy + 11, 12f, Draw.alpha(0xFF0C0C10, o));
        Fonts.BOLD.draw(ctx, "Collapse", wx + 34, wy + 12, 11f, Draw.alpha(C_TEXT, o));

        // вкладки-пилюли
        Module.Category[] cats = Module.Category.values();
        float tabW = 62, tx0 = wx + 108;
        pill.set(selected.ordinal());
        if (search.isEmpty()) Draw.roundRect(ctx, tx0 + pill.get() * (tabW + 4), wy + 6, tabW, 22, 8, Draw.alpha(acc, o));
        for (int i = 0; i < cats.length; i++) {
            float x = tx0 + i * (tabW + 4);
            boolean sel = cats[i] == selected && search.isEmpty();
            boolean hv = in(mx, my, x, wy + 6, tabW, 22);
            if (hv && !sel) Draw.roundRect(ctx, x, wy + 6, tabW, 22, 8, Draw.alpha(0x20FFFFFF, o));
            Fonts.MEDIUM.drawCentered(ctx, cats[i].label, x + tabW / 2, wy + 12, 10f,
                    Draw.alpha(sel ? 0xFF0C0C10 : C_MUTED, o));
            hits.add(new Hit(x, wy + 6, tabW, 22, H_TAB, cats[i], 0));
        }

        // поиск справа
        float sw = 80;
        float sx = wx + ww - sw - 8;
        Draw.roundRect(ctx, sx, wy + 8, sw, 18, 6, Draw.alpha(searching ? 0xFF22222C : C_FIELD, o));
        String shown = search.isEmpty() ? (searching ? "|" : "Поиск") : search + (searching && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
        Fonts.MEDIUM.draw(ctx, shown, sx + 7, wy + 12, 9.5f, Draw.alpha(search.isEmpty() ? C_MUTED : C_TEXT, o));
        hits.add(new Hit(sx, wy + 8, sw, 18, H_SEARCH, null, 0));

        // тема
        float thx = sx - 78;
        boolean thh = in(mx, my, thx, wy + 8, 72, 18);
        Draw.roundRect(ctx, thx, wy + 8, 72, 18, 6, Draw.alpha(thh ? 0xFF22222C : C_FIELD, o));
        Draw.roundRect(ctx, thx + 6, wy + 13, 8, 8, 4, Draw.alpha(acc, o));
        Fonts.MEDIUM.draw(ctx, Theme.name(), thx + 19, wy + 12, 9.5f, Draw.alpha(C_TEXT, o));
        hits.add(new Hit(thx, wy + 8, 72, 18, H_THEME, null, 0));

        // ---- контент ----
        float pad = 12;
        float cx = wx + pad, cy = wy + barH + 10;
        float cw = ww - pad * 2, ch = wh - barH - 18;
        String title = search.isEmpty() ? selected.label : "Поиск: " + search;
        Fonts.BOLD.draw(ctx, title, cx, cy, 12f, Draw.alpha(C_TEXT, o));
        String hint = binding != null ? "Нажми клавишу для " + binding.getName() + "  (ESC сброс)" : "СКМ или иконка: бинд   RShift: закрыть";
        Fonts.MEDIUM.drawRight(ctx, hint, cx + cw, cy + 2, 8.5f, Draw.alpha(C_MUTED, o));

        float viewY = cy + 20, viewH = ch - 20;
        ctx.enableScissor((int) cx - 2, (int) viewY, (int) (cx + cw + 2), (int) (viewY + viewH));
        scrollAnim.set(scroll);
        drawColumns(ctx, mx, my, cx, viewY - scrollAnim.get(), cw, viewY, viewH, o);
        ctx.disableScissor();
    }

    private void drawColumns(DrawContext ctx, int mx, int my, float cx, float top, float cw, float viewY, float viewH, float o) {
        List<Module> mods = list();
        int cols = cw >= 440 ? 3 : (cw >= 300 ? 2 : 1);
        float gap = 12, colW = (cw - gap * (cols - 1)) / cols;
        float[] ys = new float[cols];
        java.util.Arrays.fill(ys, top);

        for (Module m : mods) {
            // выбираем самую короткую колонку
            int c = 0;
            for (int i = 1; i < cols; i++) if (ys[i] < ys[c]) c = i;
            float x = cx + c * (colW + gap);
            float y = ys[c];
            float used = drawModule(ctx, mx, my, m, x, y, colW, viewY, viewH, o);
            ys[c] += used + 14;
        }
        float tallest = 0;
        for (float v : ys) tallest = Math.max(tallest, v - top);
        maxScroll = Math.max(0, tallest - viewH + 6);
        if (scroll > maxScroll) scroll = maxScroll;
    }

    /** Рисует модуль с развёрнутыми настройками. Возвращает занятую высоту. */
    private float drawModule(DrawContext ctx, int mx, int my, Module m, float x, float y, float w, float viewY, float viewH, float o) {
        int acc = Theme.accent();
        float on = ma(m).get();
        ma(m).set(m.isEnabled() ? 1f : 0f);
        boolean vis = my >= viewY && my <= viewY + viewH;

        // шапка модуля
        float hh = 34;
        boolean hv = vis && in(mx, my, x, y, w, hh);
        Draw.roundRect(ctx, x, y, w, hh, 8, Draw.alpha(hv ? 0xFF1B1B24 : 0xFF15151C, o));
        // индикатор
        Draw.roundRect(ctx, x + 9, y + 12, 10, 10, 5, Draw.alpha(Theme.mix(0xFF3A3A46, acc, on), o));
        Fonts.BOLD.draw(ctx, m.getName(), x + 26, y + 6, 11f, Draw.alpha(C_TEXT, o));
        String d = trim(m.getDescription(), w - 80, 8.5f);
        Fonts.MEDIUM.draw(ctx, d, x + 26, y + 20, 8.5f, Draw.alpha(C_MUTED, o));
        // переключатель
        float sx = x + w - 34, sy = y + 10;
        Draw.roundRect(ctx, sx, sy, 26, 14, 7, Draw.alpha(Theme.mix(C_OFF, acc, on), o));
        Draw.roundRect(ctx, sx + 2 + 12 * on, sy + 2, 10, 10, 5, Draw.alpha(0xFFFFFFFF, o));
        hits.add(new Hit(x, y, w, hh, H_MOD, m, 0));
        // бинд
        if (m.getBind() > 0) {
            String kn = keyName(m.getBind());
            Fonts.MEDIUM.drawRight(ctx, kn, x + w - 40, y + 12, 8.5f, Draw.alpha(acc, o));
        }

        float cy = y + hh + 4;
        for (Setting<?> s : m.getSettings()) {
            cy += drawSetting(ctx, mx, my, s, x, cy, w, viewY, viewH, o);
        }
        return cy - y;
    }

    private float drawSetting(DrawContext ctx, int mx, int my, Setting<?> s, float x, float y, float w, float viewY, float viewH, float o) {
        int acc = Theme.accent();
        boolean vis = my >= viewY && my <= viewY + viewH;
        float rowH = 22;
        Fonts.MEDIUM.draw(ctx, s.getName(), x + 8, y + 6, 9.5f, Draw.alpha(0xFFC4C4D0, o));

        if (s instanceof BoolSetting b) {
            Anim a = an(b, b.get() ? 1f : 0f); a.set(b.get() ? 1f : 0f);
            float t = a.get();
            float bx = x + w - 22, by = y + 4;
            Draw.roundRect(ctx, bx, by, 14, 14, 4, Draw.alpha(Theme.mix(C_OFF, acc, t), o));
            if (t > 0.5f) {
                Fonts.BOLD.drawCentered(ctx, "+", bx + 7, by + 2, 10f, Draw.alpha(0xFF0C0C10, o * t));
            }
            hits.add(new Hit(x, y, w, rowH, H_SET, s, 0));
        } else if (s instanceof ColorSetting cs) {
            Draw.roundRect(ctx, x + w - 30, y + 4, 22, 14, 4, Draw.alpha(0xFFFFFFFF, o));
            Draw.roundRect(ctx, x + w - 29, y + 5, 20, 12, 3, Draw.alpha(cs.get(), o));
            hits.add(new Hit(x, y, w, rowH, H_SET, s, 0));
        } else if (s instanceof ModeSetting ms) {
            float dw = Math.min(w * 0.5f, 92);
            float dx = x + w - dw - 4;
            boolean open = openDrop == ms;
            Draw.roundRect(ctx, dx, y + 2, dw, 18, 5, Draw.alpha(C_FIELD, o));
            Fonts.MEDIUM.draw(ctx, trim(ms.get(), dw - 22, 9.5f), dx + 7, y + 6, 9.5f, Draw.alpha(C_TEXT, o));
            Fonts.MEDIUM.drawRight(ctx, open ? "^" : "v", dx + dw - 6, y + 6, 9f, Draw.alpha(C_MUTED, o));
            hits.add(new Hit(dx, y + 2, dw, 18, H_SET, ms, 1));
            float extra = 0;
            if (open) {
                float oy = y + 22;
                for (String opt : ms.getModes()) {
                    boolean sel = opt.equals(ms.get());
                    boolean ho = vis && in(mx, my, dx, oy, dw, 17);
                    Draw.roundRect(ctx, dx, oy, dw, 17, 4, Draw.alpha(ho ? 0xFF2A2A36 : 0xFF1F1F29, o));
                    Fonts.MEDIUM.draw(ctx, trim(opt, dw - 14, 9f), dx + 7, oy + 4, 9f, Draw.alpha(sel ? acc : C_TEXT, o));
                    hits.add(new Hit(dx, oy, dw, 17, H_OPT, ms, java.util.Arrays.asList(ms.getModes()).indexOf(opt)));
                    oy += 18;
                    extra += 18;
                }
            }
            return rowH + extra;
        } else if (s instanceof NumSetting ns) {
            Anim a = an(ns, (float) ns.getPercent()); a.set((float) ns.getPercent());
            float p = a.get();
            String v = (ns.get() % 1 == 0) ? String.valueOf(ns.get().intValue()) : String.format("%.2f", ns.get());
            float vw = Fonts.MEDIUM.width(v, 9f);
            float bx = x + w * 0.42f, bw = w - (w * 0.42f) - vw - 20;
            Fonts.MEDIUM.drawRight(ctx, v, x + w - 4, y + 6, 9f, Draw.alpha(C_MUTED, o));
            Draw.roundRect(ctx, bx, y + 9, bw, 3, 1.5f, Draw.alpha(C_OFF, o));
            Draw.roundRect(ctx, bx, y + 9, Math.max(3, bw * p), 3, 1.5f, Draw.alpha(acc, o));
            Draw.roundRect(ctx, bx + bw * p - 5, y + 5, 10, 10, 5, Draw.alpha(0xFFFFFFFF, o));
            hits.add(new Hit(bx - 6, y, bw + 12, rowH, H_SLIDER, ns, 0));
        }
        return rowH;
    }

    private String trim(String s, float maxW, float size) {
        if (Fonts.MEDIUM.width(s, size) <= maxW) return s;
        while (s.length() > 2 && Fonts.MEDIUM.width(s + "..", size) > maxW) s = s.substring(0, s.length() - 1);
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
        float mx = (float) click.x(), my = (float) click.y();
        int btn = click.button();
        searching = false;
        boolean inView = my > wy + 56;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!in(mx, my, h.x, h.y, h.w, h.h)) continue;
            if ((h.type == H_MOD || h.type == H_SET || h.type == H_SLIDER || h.type == H_OPT) && !inView) continue;
            switch (h.type) {
                case H_TAB -> { selected = (Module.Category) h.a; search = ""; scroll = 0; scrollAnim.snap(0); openDrop = null; return true; }
                case H_THEME -> { Theme.next(); return true; }
                case H_SEARCH -> { searching = true; return true; }
                case H_MOD -> {
                    Module m = (Module) h.a;
                    if (btn == 2 || btn == 1) binding = m; else m.toggle();
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
                    if (s instanceof BoolSetting b) b.toggle();
                    else if (s instanceof ColorSetting cs) cs.next();
                    else if (s instanceof ModeSetting ms) openDrop = (openDrop == ms) ? null : ms;
                    return true;
                }
                case H_SLIDER -> {
                    dragging = (NumSetting) h.a; dragX = h.x + 6; dragW = h.w - 12;
                    applyDrag(mx);
                    return true;
                }
            }
        }
        openDrop = null;
        return true;
    }

    private void applyDrag(double mx) {
        if (dragging == null) return;
        double p = Math.max(0, Math.min(1, (mx - dragX) / dragW));
        dragging.set(dragging.getMin() + (dragging.getMax() - dragging.getMin()) * p);
    }

    @Override public boolean mouseDragged(Click c, double dx, double dy) {
        if (dragging != null) { applyDrag(c.x()); return true; }
        // перетаскивание контента пальцем
        if (c.y() > wy + 56) scroll = (float) Math.max(0, Math.min(maxScroll, scroll - dy));
        return true;
    }
    @Override public boolean mouseReleased(Click c) { dragging = null; return true; }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        scroll = (float) Math.max(0, Math.min(maxScroll, scroll - v * 30));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int k = input.key();
        if (binding != null) { binding.setBind(k == 256 ? -1 : k); binding = null; return true; }
        if (searching) {
            if (k == 259 && !search.isEmpty()) search = search.substring(0, search.length() - 1);
            else if (k == 256 || k == 257) searching = false;
            else if (k >= 32 && k <= 126) search += Character.toLowerCase((char) k);
            return true;
        }
        if (k == 344 || k == 256) { this.close(); return true; }
        return super.keyPressed(input);
    }

    @Override public boolean shouldPause() { return false; }
}
