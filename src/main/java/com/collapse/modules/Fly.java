package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;
import net.minecraft.util.math.Vec3d;

public class Fly extends Module {
    private final NumSetting speed = add(new NumSetting("Speed", 1.0, 0.1, 5.0, 0.1));

    public Fly() {
        super("Fly", "Полёт (на серверах с античитом кикнет)", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        double s = speed.get();
        Vec3d v = mc.player.getVelocity();
        double y = 0;
        if (mc.options.jumpKey.isPressed()) y = s;
        else if (mc.options.sneakKey.isPressed()) y = -s;
        mc.player.setVelocity(v.x, y, v.z);
        mc.player.fallDistance = 0;
    }
}
