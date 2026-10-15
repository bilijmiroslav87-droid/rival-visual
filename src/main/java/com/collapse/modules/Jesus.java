package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.util.math.Vec3d;

public class Jesus extends Module {
    public Jesus() {
        super("Jesus", "Ходьба по воде", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if ((mc.player.isTouchingWater() || mc.player.isInLava()) && !mc.player.isSneaking()) {
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, 0.11, v.z);
        }
    }
}
