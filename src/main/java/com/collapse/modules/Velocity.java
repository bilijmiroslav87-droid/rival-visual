package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;
import net.minecraft.util.math.Vec3d;

public class Velocity extends Module {
    private final NumSetting horizontal = add(new NumSetting("Horizontal %", 0, 0, 100, 5));
    private final NumSetting vertical = add(new NumSetting("Vertical %", 0, 0, 100, 5));

    public Velocity() {
        super("Velocity", "Уменьшает отбрасывание", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.player.hurtTime <= 0) return;
        Vec3d v = mc.player.getVelocity();
        mc.player.setVelocity(v.x * horizontal.get() / 100.0, v.y * vertical.get() / 100.0, v.z * horizontal.get() / 100.0);
    }
}
