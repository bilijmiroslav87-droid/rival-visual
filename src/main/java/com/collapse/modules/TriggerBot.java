package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.BoolSetting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class TriggerBot extends Module {
    private final BoolSetting onlyPlayers = add(new BoolSetting("Only players", true));
    private final BoolSetting crits = add(new BoolSetting("Wait crit", false));

    public TriggerBot() {
        super("TriggerBot", "Бьёт, когда прицел на цели", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.93f) return;
        HitResult hit = mc.crosshairTarget;
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity l && l.isAlive()) {
            if (onlyPlayers.get() && !(l instanceof PlayerEntity)) return;
            if (com.collapse.combat.Friends.isFriend(l.getName().getString())) return;
            if (crits.get() && (mc.player.isOnGround() || mc.player.fallDistance <= 0.05f)) return;
            mc.interactionManager.attackEntity(mc.player, l);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
