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

    private static void watermark(DrawContext ctx, MinecraftClient mc) {
        int acc = Theme.accent(), acc2 = Theme.accent2();
        String a = "Collapse";
        String b = mc.getCurrentFps() + " fps";
        float fa = Fonts.HEAD.width(a, 9f);
        float fb = Fonts.MEDIUM.width(b, 8f);
        float bw = 8 + fa + 10 + fb + 8;
        float x = 6, y = 6, hgt = 18;
        Draw.shadow(ctx, x, y, bw, hgt, 8, 8);
        Draw.roundRect(ctx, x, y, bw, hgt, 8, 0xDD0E0E14);
        Draw.ring(ctx, x, y, bw, hgt, 8, 0x1FFFFFFF);
        Draw.roundRect(ctx, x + 5, y + 5, 3, 8, 1.5f, acc);
        Fonts.HEAD.draw(ctx, a, x + 12, y + 5, 9f, acc);
        ctx.fill((int) (x + 12 + fa + 5), (int) y + 5, (int) (x + 12 + fa + 6), (int) y + 13, 0x33FFFFFF);
        Fonts.MEDIUM.draw(ctx, b, x + 12 + fa + 10, y + 5, 8f, 0xFFB8B8C8);
        // тонкий градиент снизу
        Draw.gradientH(ctx, x + 8, y + hgt - 1, bw - 16, 1, Draw.alpha(acc, 0.9f), Draw.alpha(acc2, 0.9f));
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
        float rowH = 13;
        float y = bottom ? h - 6 - rowH : 6;
        int i = 0;
        for (Module m : enabled) {
            float t = com.collapse.render.Anim.ease(m.anim.get());
            String n = m.getName();
            float tw = Fonts.MEDIUM.width(n, size);
            float bw = tw + 12;
            float slide = (bw + 8) * (1 - t);
            float x = right ? w - bw - 5 + slide : 5 - slide;
            int col = Theme.mix(Theme.accent(), Theme.accent2(), (i % 10) / 10f);
            if (ClientSettings.arrayListBg) {
                Draw.roundRect(ctx, x, y, bw, rowH - 1, 5, Draw.alpha(0xDD0E0E14, t));
            }
            float barX = right ? x + bw - 3 : x + 1;
            Draw.roundRect(ctx, barX, y + 2.5f, 2, rowH - 6, 1, Draw.alpha(col, t));
            Fonts.MEDIUM.draw(ctx, n, right ? x + 5 : x + 7, y + 2.5f, size, Draw.alpha(col, t));
            y += (bottom ? -rowH : rowH) * t;
            i++;
        }
    }
}
