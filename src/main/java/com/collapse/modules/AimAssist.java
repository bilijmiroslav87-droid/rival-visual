package com.collapse.modules;

import com.collapse.Module;
import com.collapse.combat.Targets;
import com.collapse.settings.NumSetting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public class AimAssist extends Module {
    private final NumSetting range = add(new NumSetting("Range", 4.5, 2.0, 7.0, 0.1));
    private final NumSetting smooth = add(new NumSetting("Smooth", 6, 1, 20, 1));
    private final NumSetting fov = add(new NumSetting("FOV", 60, 10, 180, 5));

    public AimAssist() {
        super("AimAssist", "Мягкое наведение камеры на цель", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        List<LivingEntity> l = Targets.find(range.get(), true, false, false, false, false, "FOV");
        if (l.isEmpty()) return;
        LivingEntity t = l.get(0);
        float[] w = com.collapse.combat.RotationManager.calc(mc.player.getEyePos(), t.getEntityPos().add(0, t.getHeight() * 0.62, 0));
        float dy = MathHelper.wrapDegrees(w[0] - mc.player.getYaw());
        float dp = w[1] - mc.player.getPitch();
        if (Math.hypot(dy, dp) > fov.get() / 2) return;
        mc.player.setYaw(mc.player.getYaw() + com.collapse.combat.RotationManager.snap(dy / smooth.f()));
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + com.collapse.combat.RotationManager.snap(dp / smooth.f()), -90, 90));
    }
}
