package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class TriggerBot extends Module {
    public TriggerBot() {
        super("TriggerBot", "Бьёт, когда прицел на цели", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.player.getAttackCooldownProgress(0f) < 0.95f) return;
        HitResult hit = mc.crosshairTarget;
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity l && l.isAlive()) {
            mc.interactionManager.attackEntity(mc.player, l);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
