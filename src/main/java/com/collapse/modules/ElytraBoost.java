package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;
import net.minecraft.util.math.Vec3d;

public class ElytraBoost extends Module {
    private final NumSetting boost = add(new NumSetting("Boost", 0.04, 0.0, 0.2, 0.01));

    public ElytraBoost() {
        super("ElytraBoost", "Ускорение на элитрах (зажми вперёд)", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || !mc.player.isGliding()) return;
        if (!mc.player.input.playerInput.forward()) return;
        Vec3d look = mc.player.getRotationVector();
        Vec3d v = mc.player.getVelocity();
        mc.player.setVelocity(v.add(look.multiply(boost.get())));
    }
}
