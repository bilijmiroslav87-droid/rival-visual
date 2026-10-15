package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.util.Hand;

public class AntiAfk extends Module {
    private int ticks = 0;

    public AntiAfk() {
        super("AntiAFK", "Не даёт кикнуть за AFK", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        ticks++;
        if (ticks % 400 == 0) mc.player.swingHand(Hand.MAIN_HAND);
        if (ticks % 800 == 0) mc.player.setYaw(mc.player.getYaw() + 20f);
    }
}
