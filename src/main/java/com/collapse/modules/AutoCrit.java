package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.util.math.Vec3d;

public class AutoCrit extends Module {
    public AutoCrit() {
        super("AutoCrit", "Подпрыгивает для критов", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.options.attackKey.isPressed() && mc.player.isOnGround() && !mc.player.isTouchingWater()) {
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, 0.2, v.z);
        }
    }
}
