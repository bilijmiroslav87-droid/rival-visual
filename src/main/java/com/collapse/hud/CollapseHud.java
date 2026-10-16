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

    // матовое стекло, как в Glass Menu
    public static final int GLASS = 0xB40F0F14;
    private static final int BORDER = 0x14FFFFFF;
    private static final int TXT = 0xFFF2F2F5, DIM = 0xFF8A8F98;
    private static final int GREEN = 0xFF7CFFB2, YELLOW = 0xFFFFD36E, RED = 0xFFFF6F6F;
    private static float smoothFps = -1f;

    private static void panel(DrawContext ctx, float x, float y, float w, float h) {
        Draw.roundRect(ctx, x, y, w, h, 6, BORDER);
        Draw.roundRect(ctx, x + 1, y + 1, w - 2, h - 2, 5, GLASS);
    }

    private static void watermark(DrawContext ctx, MinecraftClient mc) {
        int fps = mc.getCurrentFps();
        smoothFps = smoothFps < 0 ? fps : smoothFps + (fps - smoothFps) * 0.08f;
        int shown = Math.round(smoothFps);
        int fc = shown >= 100 ? GREEN : shown >= 50 ? YELLOW : RED;
        int ping = -1;
        if (mc.getNetworkHandler() != null && mc.player != null) {
            var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        int pc = ping < 0 ? 0xFFAAAAAA : ping < 80 ? GREEN : ping < 150 ? YELLOW : RED;
        String name = "COLLAPSE";
        String f = shown + " FPS";
        String pg = ping < 0 ? "-- MS" : ping + " MS";
        float size = 9f, pad = 9, dot = 6, sep = 12, h = 20;
        float nw = Fonts.HEAD.width(name, 11f);
        float fw = Fonts.MEDIUM.width(f, size), pw = Fonts.MEDIUM.width(pg, size);
        float w = pad + nw + sep + dot + 6 + fw + sep + pw + pad;
        float x = 6, y = 6;
        panel(ctx, x, y, w, h);
        float tx = x + pad;
        Fonts.HEAD.draw(ctx, name, tx, y + 4.5f, 11f, TXT);
        tx += nw + sep / 2f;
        ctx.fill(Math.round(tx), Math.round(y + 5), Math.round(tx) + 1, Math.round(y + h - 5), 0x30FFFFFF);
        tx += sep / 2f;
        Draw.roundRect(ctx, tx, y + (h - dot) / 2f, dot, dot, 3, fc);
        tx += dot + 6;
        Fonts.MEDIUM.draw(ctx, f, tx, y + 5.5f, size, TXT);
        tx += fw + sep / 2f;
        ctx.fill(Math.round(tx), Math.round(y + 5), Math.round(tx) + 1, Math.round(y + h - 5), 0x30FFFFFF);
        tx += sep / 2f;
        Fonts.MEDIUM.draw(ctx, pg, tx, y + 5.5f, size, pc);

        // вторая плашка: координаты и скорость
        if (mc.player != null) {
            double bps = Math.hypot(mc.player.getX() - mc.player.lastX, mc.player.getZ() - mc.player.lastZ) * 20;
            String pos = (int) Math.floor(mc.player.getX()) + "  " + (int) Math.floor(mc.player.getY()) + "  " + (int) Math.floor(mc.player.getZ());
            String sp = String.format(java.util.Locale.US, "%.1f b/s", bps);
            float w2 = pad + Fonts.MEDIUM.width(pos, size) + sep + Fonts.MEDIUM.width(sp, size) + pad;
            float y2 = y + h + 4;
            panel(ctx, x, y2, w2, h);
            Fonts.MEDIUM.draw(ctx, pos, x + pad, y2 + 5.5f, size, TXT);
            float sx = x + pad + Fonts.MEDIUM.width(pos, size) + sep / 2f;
            ctx.fill(Math.round(sx), Math.round(y2 + 5), Math.round(sx) + 1, Math.round(y2 + h - 5), 0x30FFFFFF);
            Fonts.MEDIUM.draw(ctx, sp, sx + sep / 2f, y2 + 5.5f, size, DIM);
        }
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
        float rowH = 16;
        float y = bottom ? h - 6 - rowH : 6;
        for (Module m : enabled) {
            float t = com.collapse.render.Anim.ease(m.anim.get());
            String n = m.getName();
            float tw = Fonts.MEDIUM.width(n, size);
            float bw = tw + 18;
            float slide = (bw + 10) * (1 - t);
            float x = right ? w - bw - 6 + slide : 6 - slide;
            if (ClientSettings.arrayListBg) {
                Draw.roundRect(ctx, x, y, bw, rowH - 2, 5, Draw.alpha(BORDER, t));
                Draw.roundRect(ctx, x + 1, y + 1, bw - 2, rowH - 4, 4, Draw.alpha(GLASS, t));
            }
            Draw.roundRect(ctx, right ? x + bw - 6 : x + 3, y + 4, 3, rowH - 10, 1.5f, Draw.alpha(Theme.accent(), t));
            Fonts.MEDIUM.draw(ctx, n, right ? x + 7 : x + 10, y + 3.5f, size, Draw.alpha(TXT, t));
            y += (bottom ? -rowH : rowH) * t;
        }
    }
}
