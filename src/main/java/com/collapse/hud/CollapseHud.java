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

    public static final int GLASS = 0xCC0E0E15;

    private static void watermark(DrawContext ctx, MinecraftClient mc) {
        int acc = Theme.accent(), acc2 = Theme.accent2();
        String name = mc.player != null ? mc.player.getName().getString() : "";
        int ping = 0;
        if (mc.getNetworkHandler() != null && mc.player != null) {
            var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        String brand = "Collapse";
        String[] parts = {name, mc.getCurrentFps() + " fps", ping + " ms"};
        float bs = 9.5f, ps = 8f;
        float logoW = 22;
        float brandW = Fonts.HEAD.width(brand, bs);
        float x = 6, y = 6, hgt = 24;
        float total = 5 + logoW + 6 + brandW + 8;
        for (String t : parts) if (!t.isEmpty()) total += 1 + 8 + Fonts.MEDIUM.width(t, ps) + 8;
        total += 2;

        Draw.shadow(ctx, x, y, total, hgt, 10, 12);
        Draw.glow(ctx, x, y, total, hgt, 7, Draw.alpha(Theme.withAlpha(acc, 30), 1f));
        Draw.roundRect(ctx, x, y, total, hgt, 10, GLASS);
        // верхний блик стекла
        Draw.roundRect(ctx, x + 1, y + 1, total - 2, hgt / 2f - 1, 9, 0x0CFFFFFF);
        Draw.ring(ctx, x, y, total, hgt, 10, 0x26FFFFFF);
        // логотип-плитка с градиентом
        Draw.glow(ctx, x + 4, y + 4, logoW - 4, hgt - 8, 4, Draw.alpha(Theme.withAlpha(acc, 90), 1f));
        Draw.roundGradientV(ctx, x + 4, y + 4, logoW - 4, hgt - 8, 6, acc, acc2);
        Fonts.HEAD.drawCentered(ctx, "C", x + 4 + (logoW - 4) / 2f, y + 7.5f, 9f, 0xFF0B0B10);
        float cx = x + 5 + logoW + 3;
        // бренд с анимированным градиентом (по буквам)
        float bx = cx;
        for (int i = 0; i < brand.length(); i++) {
            String ch = String.valueOf(brand.charAt(i));
            int c = Theme.mix(0xFFFFFFFF, acc, (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 600.0 - i * 0.55)) * 0.75f);
            Fonts.HEAD.draw(ctx, ch, bx, y + 8, bs, c);
            bx += Fonts.HEAD.width(ch, bs);
        }
        cx += brandW + 8;
        for (String t : parts) {
            if (t.isEmpty()) continue;
            ctx.fill((int) cx, (int) y + 7, (int) cx + 1, (int) (y + hgt - 7), 0x28FFFFFF);
            cx += 9;
            Fonts.MEDIUM.draw(ctx, t, cx, y + 8.5f, ps, 0xFFD2D2E0);
            cx += Fonts.MEDIUM.width(t, ps) + 8;
        }
        Draw.gradientH(ctx, x + 10, y + hgt - 1.6f, total - 20, 1.4f, Draw.alpha(acc, 0.95f), Draw.alpha(acc2, 0.95f));
    }

    private static void arrayList(DrawContext ctx, int w, int h) {
        List<Module> enabled = new ArrayList<>();
        for (Module m : ModuleManager.MODULES) {
            if (m.anim.get() > 0.02f && !m.isHidden() && !m.getName().equals("Watermark") && !m.getName().equals("Notifications")) enabled.add(m);
        }
        float size = 9f;
        enabled.sort(Comparator.comparingDouble((Module m) -> Fonts.MEDIUM.width(m.getName(), size)).reversed());
        int pos = Math.floorMod(ClientSettings.arrayListPos, 4);
        boolean right = pos == 0 || pos == 2;
        boolean bottom = pos == 2 || pos == 3;
        float rowH = 13.5f;
        float y = bottom ? h - 6 - rowH : (right ? 6 : 36);
        int i = 0;
        int acc = Theme.accent(), acc2 = Theme.accent2();
        for (Module m : enabled) {
            float t = com.collapse.render.Anim.ease(m.anim.get());
            String n = m.getName();
            float tw = Fonts.MEDIUM.width(n, size);
            float bw = tw + 13;
            float slide = (bw + 10) * (1 - t);
            float x = right ? w - bw - 5 + slide : 5 - slide;
            float k = (i % 12) / 12f;
            int col = Theme.mix(acc, acc2, k);
            if (ClientSettings.arrayListBg) {
                Draw.roundRect(ctx, x, y, bw, rowH - 1, 6, Draw.alpha(GLASS, t));
                // мягкая цветная подложка слева направо
                Draw.roundRect(ctx, x, y, bw, rowH - 1, 6, Draw.alpha(Theme.withAlpha(col, 26), t));
            }
            float barX = right ? x + bw - 2.8f : x + 0.8f;
            Draw.glow(ctx, barX - 1, y + 2, 3, rowH - 5, 3, Draw.alpha(Theme.withAlpha(col, 80), t));
            Draw.roundRect(ctx, barX, y + 2.5f, 2, rowH - 6, 1, Draw.alpha(col, t));
            Fonts.MEDIUM.draw(ctx, n, right ? x + 5.5f : x + 7.5f, y + 2.7f, size, Draw.alpha(Theme.mix(col, 0xFFFFFFFF, 0.25f), t));
            y += (bottom ? -rowH : rowH) * t;
            i++;
        }
    }
}
