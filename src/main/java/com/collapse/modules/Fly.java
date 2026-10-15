package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.ModeSetting;
import com.collapse.settings.NumSetting;
import net.minecraft.util.math.Vec3d;

public class Fly extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Motion", "Motion", "Glide"));
    private final NumSetting speed = add(new NumSetting("Speed", 0.8, 0.1, 5.0, 0.1));
    private final NumSetting vspeed = add(new NumSetting("Vertical", 0.6, 0.1, 3.0, 0.1));

    public Fly() {
        super("Fly", "Полёт", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        Vec3d v = mc.player.getVelocity();
        double y = mode.is("Glide") ? -0.03 : 0;
        if (mc.options.jumpKey.isPressed()) y = vspeed.get();
        else if (mc.options.sneakKey.isPressed()) y = -vspeed.get();

        double fwd = 0, side = 0;
        
        if (com.collapse.util.Keys.forward()) fwd += 1;
        if (com.collapse.util.Keys.back()) fwd -= 1;
        if (com.collapse.util.Keys.left()) side += 1;
        if (com.collapse.util.Keys.right()) side -= 1;
        double yaw = Math.toRadians(mc.player.getYaw());
        double mx = 0, mz = 0;
        if (fwd != 0 || side != 0) {
            double len = Math.sqrt(fwd * fwd + side * side);
            fwd /= len; side /= len;
            mx = (-Math.sin(yaw) * fwd + Math.cos(yaw) * side) * speed.get();
            mz = (Math.cos(yaw) * fwd + Math.sin(yaw) * side) * speed.get();
        }
        mc.player.setVelocity(mx, y, mz);
        mc.player.fallDistance = 0;
    }
}
