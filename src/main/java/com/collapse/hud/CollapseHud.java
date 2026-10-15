package com.collapse.hud;

import com.collapse.Module;
import com.collapse.ModuleManager;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.settings.ClientSettings;
import com.collapse.util.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CollapseHud {
    private CollapseHud() {}

    private static long frames = 0;

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("collapse-client", "hud"), CollapseHud::render);
    }

    private static boolean on(String n) {
        Module m = ModuleManager.byName(n);
        return m != null && m.isEnabled();
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        if (frames++ == 0) com.collapse.CollapseClient.LOGGER.info("Collapse HUD: first frame rendered");
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options == null || mc.options.hudHidden || mc.player == null) return;
            if (Fonts.MEDIUM == null) return;
            int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();

            for (Module m : ModuleManager.MODULES) {
                if (m.isEnabled()) {
                    try { m.onHud(ctx, w, h); } catch (Throwable ignored) {}
                }
            }

            if (on("Watermark")) watermark(ctx, mc);
            if (ClientSettings.arrayList) arrayList(ctx, w, h);
            if (on("Notifications")) Notify.render(ctx, w, h);
        } catch (Throwable t) {
            if (frames % 600 == 1) com.collapse.CollapseClient.LOGGER.error("HUD render error", t);
        }
    }

    public static final int GLASS = 0xE6100C0E;
    private static final int BORDER = 0xFF24181C;
    private static final int DIVIDER = 0xFF30242A;

    /** Пилюля: фон, обводка. Возвращает ширину. */
    private static void pillBg(DrawContext ctx, float x, float y, float w, float h) {
        Draw.roundRect(ctx, x, y, w, h, 5, GLASS);
        Draw.ring(ctx, x, y, w, h, 5, BORDER);
    }

    private static float seg(DrawContext ctx, String icon, String text, float x, float y, float h, int acc, boolean first) {
        float size = 8.5f;
        float cx = x;
        if (!first) { ctx.fill((int) cx, (int) (y + 6), (int) cx + 1, (int) (y + h - 6), DIVIDER); cx += 6; }
        if (icon != null) { Draw.icon(ctx, icon, cx + 5.5f, y + h / 2f, 11, acc); cx += 15; }
        Fonts.MEDIUM.draw(ctx, text, cx, y + h / 2f - 4.2f, size, 0xFFEBEBEB);
        return cx + Fonts.MEDIUM.width(text, size) + 6;
    }

    private static float segWidth(String icon, String text, boolean first) {
        return (first ? 0 : 6) + (icon != null ? 15 : 0) + Fonts.MEDIUM.width(text, 8.5f) + 6;
    }

    private static void watermark(DrawContext ctx, MinecraftClient mc) {
        int acc = Theme.accent(), acc2 = Theme.accent2();
        float h = 22, x0 = 8, y0 = 8, gap = 5;
        String name = mc.player != null ? mc.player.getName().getString().toLowerCase() : "";
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        int ping = 0;
        if (mc.getNetworkHandler() != null && mc.player != null) {
            var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }

        // ---- верхний ряд: бренд | имя fps время ----
        String brand = "collapse";
        float bw = 8 + 20 + 4 + 1 + 6 + Fonts.BOLD.width(brand, 9f) + 8;
        Draw.glow(ctx, x0, y0, bw, h, 8, Draw.alpha(Theme.withAlpha(acc, 32), 1f));
        pillBg(ctx, x0, y0, bw, h);
        Draw.glow(ctx, x0 + 4, y0 + 3, 16, 16, 7, Draw.alpha(Theme.withAlpha(acc, 100), 1f));
        Draw.roundGradientV(ctx, x0 + 5, y0 + 4, 14, 14, 5, acc2, acc);
        Fonts.HEAD.drawCentered(ctx, "C", x0 + 12, y0 + 6.2f, 8.5f, 0xFF1A0709);
        float bx = x0 + 28;
        ctx.fill((int) bx, (int) (y0 + 6), (int) bx + 1, (int) (y0 + h - 6), DIVIDER);
        bx += 7;
        for (int i = 0; i < brand.length(); i++) {
            String ch = String.valueOf(brand.charAt(i));
            float k = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 700.0 - i * 0.5));
            Fonts.BOLD.draw(ctx, ch, bx, y0 + h / 2f - 4.4f, 9f, Theme.mix(acc2, acc, k));
            bx += Fonts.BOLD.width(ch, 9f);
        }

        String fps = mc.getCurrentFps() + " fps";
        float w2 = 6 + segWidth("player", name, true) + segWidth("visuals", fps, false) + segWidth("misc", time, false);
        float x2 = x0 + bw + gap;
        pillBg(ctx, x2, y0, w2, h);
        float cx = x2 + 6;
        cx = seg(ctx, "player", name, cx, y0, h, acc, true);
        cx = seg(ctx, "visuals", fps, cx, y0, h, acc, false);
        seg(ctx, "misc", time, cx, y0, h, acc, false);

        // ---- нижний ряд: координаты | пинг | скорость ----
        if (mc.player != null) {
            double bps = Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ) * 20;
            String pos = (int) Math.floor(mc.player.getX()) + ", " + (int) Math.floor(mc.player.getY()) + ", " + (int) Math.floor(mc.player.getZ());
            String[] ic = {"movement", "visuals", "combat"};
            String[] tx = {pos, ping + " ping", String.format(java.util.Locale.US, "%.1f bps", bps)};
            float y1 = y0 + h + gap, x1 = x0;
            for (int i = 0; i < 3; i++) {
                float w = 6 + segWidth(ic[i], tx[i], true);
                pillBg(ctx, x1, y1, w, h);
                seg(ctx, ic[i], tx[i], x1 + 6, y1, h, acc, true);
                x1 += w + gap;
            }
        }
    }

    private static void arrayList(DrawContext ctx, int w, int h) {
        List<Module> enabled = new ArrayList<>();
        for (Module m : ModuleManager.MODULES) {
            if (m.anim.get() > 0.02f && !m.isHidden() && !m.getName().equals("Watermark") && !m.getName().equals("Notifications")) enabled.add(m);
        }
        float size = 8.8f;
        enabled.sort(Comparator.comparingDouble((Module m) -> Fonts.BOLD.width(m.getName().toUpperCase(), size)).reversed());
        int pos = Math.floorMod(ClientSettings.arrayListPos, 4);
        boolean right = pos == 0 || pos == 2;
        boolean bottom = pos == 2 || pos == 3;
        float rowH = 15;
        float y = bottom ? h - 8 - rowH : 8;
        int i = 0;
        int acc = Theme.accent(), acc2 = Theme.accent2();
        for (Module m : enabled) {
            float t = com.collapse.render.Anim.ease(m.anim.get());
            String n = m.getName().toUpperCase();
            float tw = Fonts.BOLD.width(n, size);
            float bw = tw + 17;
            float slide = (bw + 14) * (1 - t);
            float x = right ? w - bw - 8 + slide : 8 - slide;
            int col = Theme.mix(acc2, acc, Math.min(1f, i / 7f) * -1f + 1f);
            if (ClientSettings.arrayListBg) {
                Draw.roundRect(ctx, x, y, bw, rowH - 2, 4, Draw.alpha(GLASS, t));
                Draw.ring(ctx, x, y, bw, rowH - 2, 4, Draw.alpha(Theme.withAlpha(0xFF5A242A, 140), t));
            }
            float barX = right ? x + bw - 4f : x + 2f;
            Draw.roundRect(ctx, barX, y + 3, 2, rowH - 8, 1, Draw.alpha(col, t));
            Fonts.BOLD.draw(ctx, n, right ? x + 6 : x + 9, y + rowH / 2f - 5.2f, size, Draw.alpha(col, t));
            y += (bottom ? -rowH : rowH) * t;
            i++;
        }
    }
}
