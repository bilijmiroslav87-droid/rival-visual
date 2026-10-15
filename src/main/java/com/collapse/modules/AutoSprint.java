package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class AutoSprint extends Module {
    public AutoSprint() {
        super("AutoSprint", "Автоматический спринт", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        if (!player.isSprinting() && !player.isSneaking() && player.input.hasForwardMovement()) {
            player.setSprinting(true);
        }
    }
}
