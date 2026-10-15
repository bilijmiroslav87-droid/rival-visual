package com.collapse.modules;

import com.collapse.Module;

/** Без замедления при еде/использовании: компенсируем замедление скоростью. */
public class NoSlow extends Module {
    public NoSlow() {
        super("NoSlow", "Нет замедления при использовании предметов", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || !mc.player.isUsingItem() || mc.player.hasVehicle()) return;
        var v = mc.player.getVelocity();
        mc.player.setVelocity(v.x * 1.7, v.y, v.z * 1.7);
    }
}
