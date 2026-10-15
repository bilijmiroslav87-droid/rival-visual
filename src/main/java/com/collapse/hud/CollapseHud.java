package com.collapse.hud;

import com.collapse.Module;
import com.collapse.ModuleManager;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.util.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

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

            // водянка
            if (on("Watermark")) {
                String a = "Collapse", b = " Client";
                float fa = Fonts.BOLD.width(a, 11f), fb = Fonts.MEDIUM.width(b, 11f);
                Draw.roundRect(ctx, 5, 5, fa + fb + 16, 18, 6, 0xC8121218);
                Draw.roundRect(ctx, 5, 8, 3, 12, 1, Theme.accent());
                Fonts.BOLD.draw(ctx, a, 13, 9, 11f, Theme.accent());
                Fonts.MEDIUM.draw(ctx, b, 13 + fa, 9, 11f, 0xFFFFFFFF);
            }

            // ArrayList
            List<Module> enabled = new ArrayList<>();
            for (Module m : ModuleManager.MODULES) if (m.anim.get() > 0.02f && m.getCategory() != null && !m.getName().equals("Watermark")) enabled.add(m);
            enabled.sort(Comparator.comparingDouble((Module m) -> Fonts.MEDIUM.width(m.getName(), 10f)).reversed());
            float y = 5;
            int i = 0;
            for (Module m : enabled) {
                float t = m.anim.get();
                float tw = Fonts.MEDIUM.width(m.getName(), 10f);
                float x = w - (tw + 14) * t - 4;
                int col = Theme.mix(Theme.accent(), Theme.accent2(), (i % 8) / 8f);
                Draw.roundRect(ctx, x, y, tw + 10, 14, 4, Draw.alpha(0xC8121218, t));
                Draw.roundRect(ctx, x + tw + 8, y + 2, 2, 10, 1, Draw.alpha(col, t));
                Fonts.MEDIUM.draw(ctx, m.getName(), x + 5, y + 2, 10f, Draw.alpha(col, t));
                y += 16 * t;
                i++;
            }

            if (on("Notifications")) Notify.render(ctx, w, h);
        } catch (Throwable t) {
            if (frames % 600 == 1) com.collapse.CollapseClient.LOGGER.error("HUD render error", t);
        }
    }
}
