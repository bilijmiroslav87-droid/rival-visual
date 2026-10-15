package com.collapse.mixin;

import com.collapse.settings.WorldFx;
import net.minecraft.client.render.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Настоящий туман мира: подменяем цвет и дистанции в момент записи фога в буфер.
 * Аргументы приватного applyFog(ByteBuffer, int, Vector4f, envStart, envEnd, renderStart, renderEnd, skyEnd, cloudEnd).
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @ModifyArgs(method = "applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V", at = @At("HEAD"), require = 0)
    private void collapse$fog(Args args) {
        if (!WorldFx.fogOn) return;
        int rgb = WorldFx.fogRgb();
        Vector4f c = new Vector4f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, 1f);
        args.set(2, c);
        args.set(3, WorldFx.fogStart);   // environmental start
        args.set(4, WorldFx.fogEnd);     // environmental end
        args.set(5, WorldFx.fogStart);   // render distance start
        args.set(6, WorldFx.fogEnd);     // render distance end
    }
}
