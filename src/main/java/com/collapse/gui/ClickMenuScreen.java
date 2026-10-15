package com.collapse.gui;

import com.collapse.CollapseClient;
import com.collapse.Module;
import com.collapse.ModuleManager;
import com.collapse.settings.BoolSetting;
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
import java.util.List;

/**
 * Меню в стиле Nursultan: тёмное окно, сайдбар с вкладками слева,
 * справа карточки модулей. ПКМ по карточке раскрывает настройки.
 * Рисуется только через DrawContext.fill, без blur и шейдеров.
 */
public class ClickMenuScreen extends Screen {
    private static final int BG = 0xF0141418;
    private static final int SIDEBAR = 0xFF101014;
    private static final int CARD = 0xFF1C1C22;
    private static final int CARD_HOVER = 0xFF24242C;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8A8A96;

    private Module.Category selected = Module.Category.COMBAT;
    private Module settingsOf = null;
    private int scroll = 0;
    private NumSetting dragging = null;
    private int dragX, dragW;

    private int wx, wy, ww, wh;
    private final List<Runnable> clicks = new ArrayList<>();

    private ClickMenuScreen() {
        super(Text.literal("Collapse Client"));
    }

    public static ClickMenuScreen create() {
        return new ClickMenuScreen();
    }

    @Override
    protected void init() {
        ww = Math.min(this.width - 20, 420);
        wh = Math.min(this.height - 16, 260);
        wx = (this.width - ww) / 2;
        wy = (this.height - wh) / 2;
    }

    private List<Module> modulesOf(Module.Category c) {
        List<Module> list = new ArrayList<>();
        for (Module m : ModuleManager.MODULES) if (m.getCategory() == c) list.add(m);
        return list;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        try {
            ctx.fill(0, 0, this.width, this.height, 0x78000000);
            draw(ctx, mx, my);
        } catch (Throwable t) {
            CollapseClient.LOGGER.error("Menu render error", t);
        }
    }

    private void draw(DrawContext ctx, int mx, int my) {
        clicks.clear();
        tabHits.clear();
        cardHits.clear();
        settingHits.clear();
        int accent = Theme.accent();
        int sideW = 86;

        // окно
        ctx.fill(wx - 1, wy - 1, wx + ww + 1, wy + wh + 1, Theme.withAlpha(accent, 140));
        ctx.fill(wx, wy, wx + ww, wy + wh, BG);
        ctx.fill(wx, wy, wx + sideW, wy + wh, SIDEBAR);
        ctx.fill(wx + sideW, wy, wx + sideW + 1, wy + wh, 0xFF26262E);

        // логотип
        ctx.drawTextWithShadow(textRenderer, "Collapse", wx + 10, wy + 10, accent);
        ctx.drawTextWithShadow(textRenderer, "Client", wx + 10, wy + 21, TEXT);

        // вкладки
        int ty = wy + 44;
        for (Module.Category c : Module.Category.values()) {
            boolean sel = c == selected;
            boolean hov = inside(mx, my, wx + 6, ty, sideW - 12, 20);
            if (sel) {
                ctx.fill(wx + 6, ty, wx + sideW - 6, ty + 20, Theme.withAlpha(accent, 70));
                ctx.fill(wx + 6, ty, wx + 8, ty + 20, accent);
            } else if (hov) {
                ctx.fill(wx + 6, ty, wx + sideW - 6, ty + 20, 0x30FFFFFF);
            }
            int count = modulesOf(c).size();
            ctx.drawTextWithShadow(textRenderer, c.label, wx + 14, ty + 6, sel ? TEXT : MUTED);
            String cnt = String.valueOf(count);
            ctx.drawTextWithShadow(textRenderer, cnt, wx + sideW - 12 - textRenderer.getWidth(cnt), ty + 6, MUTED);
            final Module.Category cc = c;
            clicks.add(() -> {});
            tabHits.add(new int[]{wx + 6, ty, sideW - 12, 20, c.ordinal()});
            ty += 23;
        }

        // тема
        int themeY = wy + wh - 26;
        boolean thov = inside(mx, my, wx + 6, themeY, sideW - 12, 18);
        ctx.fill(wx + 6, themeY, wx + sideW - 6, themeY + 18, thov ? CARD_HOVER : CARD);
        ctx.fill(wx + 9, themeY + 5, wx + 17, themeY + 13, accent);
        ctx.drawTextWithShadow(textRenderer, Theme.name(), wx + 22, themeY + 5, TEXT);
        themeHit = new int[]{wx + 6, themeY, sideW - 12, 18};

        // область контента
        int cx = wx + sideW + 10;
        int cy = wy + 10;
        int cw = ww - sideW - 20;
        int ch = wh - 20;

        ctx.drawTextWithShadow(textRenderer, selected.label, cx, cy, TEXT);
        String hint = settingsOf == null ? "ЛКМ вкл/выкл  ПКМ настройки" : "ПКМ назад";
        ctx.drawTextWithShadow(textRenderer, hint, cx + cw - textRenderer.getWidth(hint), cy, MUTED);

        int top = cy + 16;
        int viewH = ch - 16;
        ctx.enableScissor(cx, top, cx + cw, top + viewH);

        if (settingsOf == null) drawModules(ctx, mx, my, cx, top, cw);
        else drawSettings(ctx, mx, my, cx, top, cw);

        ctx.disableScissor();
        scrollView = new int[]{cx, top, cw, viewH};
    }

    // ---- хиты ----
    private final List<int[]> tabHits = new ArrayList<>();
    private int[] themeHit = null;
    private int[] scrollView = null;
    private final List<Object[]> cardHits = new ArrayList<>();

    private void drawModules(DrawContext ctx, int mx, int my, int cx, int top, int cw) {
        int accent = Theme.accent();
        List<Module> list = modulesOf(selected);
        int cols = cw >= 250 ? 2 : 1;
        int gap = 6;
        int cardW = (cw - gap * (cols - 1)) / cols;
        int cardH = 34;

        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int col = i % cols, row = i / cols;
            int x = cx + col * (cardW + gap);
            int y = top + row * (cardH + gap) - scroll;
            boolean hov = inside(mx, my, x, y, cardW, cardH)
                    && my >= top && my <= scrollView_bottom(top);
            ctx.fill(x, y, x + cardW, y + cardH, hov ? CARD_HOVER : CARD);
            if (m.isEnabled()) {
                ctx.fill(x, y, x + 2, y + cardH, accent);
                ctx.fill(x + 2, y, x + cardW, y + cardH, Theme.withAlpha(accent, 28));
            }
            ctx.drawTextWithShadow(textRenderer, m.getName(), x + 8, y + 6, m.isEnabled() ? TEXT : 0xFFD0D0D8);
            String d = m.getDescription();
            while (textRenderer.getWidth(d) > cardW - 40 && d.length() > 4) d = d.substring(0, d.length() - 2);
            if (!d.equals(m.getDescription())) d += "..";
            ctx.drawTextWithShadow(textRenderer, d, x + 8, y + 19, MUTED);

            // переключатель
            int sx = x + cardW - 26, sy = y + 11;
            ctx.fill(sx, sy, sx + 18, sy + 10, m.isEnabled() ? accent : 0xFF3A3A44);
            int knob = m.isEnabled() ? sx + 10 : sx + 2;
            ctx.fill(knob, sy + 2, knob + 6, sy + 8, 0xFFFFFFFF);

            if (!m.getSettings().isEmpty()) {
                ctx.drawTextWithShadow(textRenderer, "...", x + cardW - 44, y + 6, MUTED);
            }
            cardHits.add(new Object[]{x, y, cardW, cardH, m});
        }
        int rows = (list.size() + cols - 1) / cols;
        maxScroll = Math.max(0, rows * (cardH + gap) - (wh - 46));
    }

    private int scrollView_bottom(int top) { return top + (wh - 46); }

    private int maxScroll = 0;
    private final List<Object[]> settingHits = new ArrayList<>();

    private void drawSettings(DrawContext ctx, int mx, int my, int cx, int top, int cw) {
        int accent = Theme.accent();
        Module m = settingsOf;
        ctx.drawTextWithShadow(textRenderer, m.getName() + " / настройки", cx, top - scroll, accent);
        int y = top + 16 - scroll;

        if (m.getSettings().isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "У этого модуля нет настроек", cx, y, MUTED);
        }

        for (Setting<?> s : m.getSettings()) {
            int h = 26;
            ctx.fill(cx, y, cx + cw, y + h, CARD);
            ctx.drawTextWithShadow(textRenderer, s.getName(), cx + 8, y + 9, TEXT);

            if (s instanceof BoolSetting b) {
                int sx = cx + cw - 28, sy = y + 8;
                ctx.fill(sx, sy, sx + 18, sy + 10, b.get() ? accent : 0xFF3A3A44);
                int knob = b.get() ? sx + 10 : sx + 2;
                ctx.fill(knob, sy + 2, knob + 6, sy + 8, 0xFFFFFFFF);
                settingHits.add(new Object[]{cx, y, cw, h, s});
            } else if (s instanceof ModeSetting ms) {
                String v = ms.get();
                ctx.drawTextWithShadow(textRenderer, "< " + v + " >", cx + cw - 10 - textRenderer.getWidth("< " + v + " >"), y + 9, accent);
                settingHits.add(new Object[]{cx, y, cw, h, s});
            } else if (s instanceof NumSetting ns) {
                String v = (ns.get() % 1 == 0) ? String.valueOf(ns.get().intValue()) : String.format("%.2f", ns.get());
                ctx.drawTextWithShadow(textRenderer, v, cx + cw - 10 - textRenderer.getWidth(v), y + 4, MUTED);
                int bx = cx + 8, bw = cw - 16, by = y + 18;
                ctx.fill(bx, by, bx + bw, by + 3, 0xFF3A3A44);
                int fill = (int) (bw * ns.getPercent());
                ctx.fill(bx, by, bx + fill, by + 3, accent);
                ctx.fill(bx + fill - 2, by - 2, bx + fill + 2, by + 5, 0xFFFFFFFF);
                settingHits.add(new Object[]{bx, by - 4, bw, 12, s});
            }
            y += h + 4;
        }
        int total = 16 + m.getSettings().size() * 30;
        maxScroll = Math.max(0, total - (wh - 46));
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // ---------------- ввод ----------------
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int btn = click.button();

        // вкладки
        for (int[] t : tabHits) {
            if (inside((int) mx, (int) my, t[0], t[1], t[2], t[3]) && btn == 0) {
                selected = Module.Category.values()[t[4]];
                settingsOf = null;
                scroll = 0;
                return true;
            }
        }
        if (themeHit != null && inside((int) mx, (int) my, themeHit[0], themeHit[1], themeHit[2], themeHit[3]) && btn == 0) {
            Theme.next();
            return true;
        }
        if (scrollView == null || my < scrollView[1] || my > scrollView[1] + scrollView[3]) return true;

        if (settingsOf == null) {
            for (Object[] h : cardHits) {
                if (inside((int) mx, (int) my, (int) h[0], (int) h[1], (int) h[2], (int) h[3])) {
                    Module m = (Module) h[4];
                    if (btn == 0) m.toggle();
                    else if (btn == 1) { settingsOf = m; scroll = 0; }
                    return true;
                }
            }
        } else {
            if (btn == 1) { settingsOf = null; scroll = 0; return true; }
            for (Object[] h : settingHits) {
                if (inside((int) mx, (int) my, (int) h[0], (int) h[1], (int) h[2], (int) h[3])) {
                    Setting<?> s = (Setting<?>) h[4];
                    if (s instanceof BoolSetting b) b.toggle();
                    else if (s instanceof ModeSetting ms) ms.next();
                    else if (s instanceof NumSetting ns) {
                        dragging = ns; dragX = (int) h[0]; dragW = (int) h[2];
                        applyDrag(mx);
                    }
                    return true;
                }
            }
        }
        return true;
    }

    private void applyDrag(double mx) {
        if (dragging == null) return;
        double p = Math.max(0, Math.min(1, (mx - dragX) / (double) dragW));
        dragging.set(dragging.getMin() + (dragging.getMax() - dragging.getMin()) * p);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragging != null) { applyDrag(click.x()); return true; }
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        scroll = (int) Math.max(0, Math.min(maxScroll, scroll - v * 14));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 344 /* RSHIFT */ || input.key() == 256 /* ESC */) {
            if (settingsOf != null && input.key() == 256) { settingsOf = null; return true; }
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() { return false; }

}
