package com.collapse.cosmetics;

import com.collapse.ModuleManager;
import com.collapse.modules.Cosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * 3D-слой косметики на модели игрока. Рисуется только для локального игрока
 * (и только если включён модуль Cosmetics).
 */
public class CosmeticLayer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static final Identifier TEX = Identifier.of("collapse-client", "textures/cosmetics/atlas.png");

    public CosmeticLayer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> ctx) {
        super(ctx);
    }

    private static boolean loggedCall = false, loggedDraw = false;

    private static boolean isLocal(PlayerEntityRenderState s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        try {
            if (s.id == mc.player.getId()) return true;
        } catch (Throwable ignored) {}
        try {
            // запасной вариант: по имени
            if (s.playerName != null && mc.player.getName() != null
                    && s.playerName.getString().equals(mc.player.getName().getString())) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    @Override
    public void render(MatrixStack m, OrderedRenderCommandQueue q, int light, PlayerEntityRenderState s, float yaw, float pitch) {
        try {
            if (!loggedCall) {
                loggedCall = true;
                com.collapse.CollapseClient.LOGGER.info("[Cosmetics] layer render() called (id={}, name={})", s.id, s.playerName);
            }
            Cosmetics c = Cosmetics.INSTANCE;
            if (c == null || !c.isEnabled() || s.invisible) return;
            if (!isLocal(s)) return;
            if (!loggedDraw) {
                loggedDraw = true;
                com.collapse.CollapseClient.LOGGER.info("[Cosmetics] drawing on local player");
            }
            c.draw(this, m, q, light, s);
        } catch (Throwable t) {
            com.collapse.CollapseClient.LOGGER.error("Cosmetics render error", t);
        }
    }

    public RenderLayer layer(boolean glow) {
        return glow ? RenderLayers.entityTranslucentEmissive(TEX) : RenderLayers.entityCutoutNoCull(TEX);
    }

    public void part(OrderedRenderCommandQueue q, ModelPart p, MatrixStack m, int light, int rgb, boolean glow) {
        // цвет задаём через тинт слоя: сабмит ModelPart принимает ARGB-цвет в последнем int перегрузки
        q.submitModelPart(p, m, layer(glow), glow ? 0xF000F0 : light, OverlayTexture.DEFAULT_UV, null, false, false, 0xFF000000 | rgb, null, 0);
    }

    public static RotationAxis X() { return RotationAxis.POSITIVE_X; }
    public static RotationAxis Y() { return RotationAxis.POSITIVE_Y; }
    public static RotationAxis Z() { return RotationAxis.POSITIVE_Z; }
}
