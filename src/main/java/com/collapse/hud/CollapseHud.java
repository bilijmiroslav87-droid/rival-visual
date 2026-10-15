package com.collapse.hud;

import com.collapse.CollapseClient;
import com.collapse.Module;
import com.collapse.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class CollapseHud {
    private CollapseHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(CollapseHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.textRenderer == null) return;

        // Водянка
        context.drawTextWithShadow(client.textRenderer,
                Text.literal("§bCollapse§f Client"), 4, 4, 0xFFFFFF);

        // ArrayList: включённые модули в правом верхнем углу
        List<Module> enabled = new ArrayList<>();
        for (Module m : ModuleManager.MODULES) {
            if (m.isEnabled()) enabled.add(m);
        }
        int y = 4;
        for (int i = enabled.size() - 1; i >= 0; i--) {
            String line = enabled.get(i).getName();
            int width = client.textRenderer.getWidth(line);
            context.drawTextWithShadow(client.textRenderer, Text.literal("§b" + line),
                    context.getScaledWindowWidth() - width - 4, y, 0xFFFFFF);
            y += client.textRenderer.fontHeight + 1;
        }
    }
}
