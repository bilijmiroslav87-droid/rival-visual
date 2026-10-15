package com.collapse.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientMixin {
    @Accessor("itemUseCooldown")
    void collapse$setItemUseCooldown(int value);

    @Accessor("itemUseCooldown")
    int collapse$getItemUseCooldown();
}
