package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall extends Module {
    public NoFall() {
        super("NoFall", "Сбрасывает урон от падения", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (mc.player.fallDistance > 2.5f && !mc.player.isOnGround()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
            mc.player.fallDistance = 0;
        }
    }
}
