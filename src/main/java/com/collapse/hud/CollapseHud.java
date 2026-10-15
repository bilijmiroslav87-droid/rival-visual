package com.collapse.hud;

import com.collapse.Module;
import com.collapse.ModuleManager;
import com.collapse.util.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CollapseHud {
    private CollapseHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(CollapseHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.textRenderer == null || mc.options == null || mc.options.hudHidden) return;
            int w = ctx.getScaledWindowWidth();
            int h = ctx.getScaledWindowHeight();

            // модули рисуют свои оверлеи (туман, прицел, виньетка, инфо)
            for (Module m : ModuleManager.MODULES) {
                if (m.isEnabled()) m.onHud(ctx, w, h);
            }

            // водянка
            String name = "Collapse";
            String rest = " Client";
            int nw = mc.textRenderer.getWidth(name);
            int rw = mc.textRenderer.getWidth(rest);
            ctx.fill(3, 3, 3 + nw + rw + 8, 15, 0xA0101014);
            ctx.fill(3, 3, 5, 15, Theme.accent());
            ctx.drawTextWithShadow(mc.textRenderer, name, 8, 5, Theme.accent());
            ctx.drawTextWithShadow(mc.textRenderer, rest, 8 + nw, 5, 0xFFFFFFFF);

            // ArrayList справа, по длине, с цветом темы
            List<Module> on = new ArrayList<>();
            for (Module m : ModuleManager.MODULES) if (m.isEnabled()) on.add(m);
            on.sort(Comparator.comparingInt((Module m) -> mc.textRenderer.getWidth(m.getName())).reversed());

            int y = 3;
            for (int i = 0; i < on.size(); i++) {
                String n = on.get(i).getName();
                int tw = mc.textRenderer.getWidth(n);
                int x = w - tw - 8;
                ctx.fill(x - 2, y, w - 2, y + 11, 0x90101014);
                ctx.fill(w - 4, y, w - 2, y + 11, Theme.accent());
                ctx.drawTextWithShadow(mc.textRenderer, n, x, y + 2, Theme.accent());
                y += 11;
            }
        } catch (Throwable ignored) {
            // HUD никогда не должен валить игру
        }
    }
}
