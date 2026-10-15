package com.collapse.modules;

import com.collapse.Module;
import com.collapse.util.Keys;

public class AutoSprint extends Module {
    public AutoSprint() {
        super("AutoSprint", "Автоматический спринт", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (Keys.forward() && !mc.player.isSneaking() && !mc.player.horizontalCollision
                && mc.player.getHungerManager().getFoodLevel() > 6) {
            mc.player.setSprinting(true);
        }
    }
}
