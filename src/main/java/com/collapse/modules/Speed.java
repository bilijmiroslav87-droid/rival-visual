package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.ModeSetting;
import com.collapse.settings.NumSetting;
import net.minecraft.util.math.Vec3d;

public class Speed extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Strafe", "Strafe", "Ground", "BHop"));
    private final NumSetting mult = add(new NumSetting("Speed", 1.25, 1.0, 3.0, 0.05));

    public Speed() {
        super("Speed", "Ускорение движения", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        if (!(com.collapse.util.Keys.forward() || com.collapse.util.Keys.back() || com.collapse.util.Keys.left() || com.collapse.util.Keys.right())) return;
        Vec3d v = mc.player.getVelocity();

        if (mode.is("BHop") && mc.player.isOnGround()) {
            mc.player.jump();
            v = mc.player.getVelocity();
        }
        if (mode.is("Ground") && !mc.player.isOnGround()) return;

        double base = 0.2873 * mult.get();
        double fwd = (com.collapse.util.Keys.forward() ? 1 : 0) - (com.collapse.util.Keys.back() ? 1 : 0);
        double side = (com.collapse.util.Keys.left() ? 1 : 0) - (com.collapse.util.Keys.right() ? 1 : 0);
        double len = Math.sqrt(fwd * fwd + side * side);
        if (len == 0) return;
        fwd /= len; side /= len;
        double yaw = Math.toRadians(mc.player.getYaw());
        mc.player.setVelocity(
                (-Math.sin(yaw) * fwd + Math.cos(yaw) * side) * base, v.y,
                (Math.cos(yaw) * fwd + Math.sin(yaw) * side) * base);
    }
}
