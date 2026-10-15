package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.ModeSetting;
import net.minecraft.util.math.Vec3d;

public class Criticals extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Jump", "Jump", "Hop"));

    public Criticals() {
        super("Criticals", "Помогает наносить криты при атаке", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (!mc.options.attackKey.isPressed()) return;
        if (mc.player.isOnGround() && !mc.player.isTouchingWater() && !mc.player.isClimbing()) {
            if (mode.is("Jump")) mc.player.jump();
            else {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.2, v.z);
            }
        }
    }
}
