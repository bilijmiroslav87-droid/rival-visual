package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.NumSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

public class KillAura extends Module {
    private final NumSetting range = add(new NumSetting("Range", 3.5, 2.0, 6.0, 0.1));
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", true));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", true));

    public KillAura() {
        super("KillAura", "Авто-атака ближайших целей", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.player.getAttackCooldownProgress(0f) < 0.95f) return;

        LivingEntity best = null;
        double bestD = range.get() * range.get();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity l) || l == mc.player || !l.isAlive() || e instanceof ArmorStandEntity) continue;
            boolean isPlayer = l instanceof PlayerEntity;
            if (isPlayer ? !players.get() : !mobs.get()) continue;
            double d = l.squaredDistanceTo(mc.player);
            if (d <= bestD) { bestD = d; best = l; }
        }
        if (best == null) return;

        if (rotate.get()) {
            double dx = best.getX() - mc.player.getX();
            double dz = best.getZ() - mc.player.getZ();
            double dy = (best.getY() + best.getHeight() / 2) - mc.player.getEyeY();
            double dist = Math.sqrt(dx * dx + dz * dz);
            mc.player.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90));
            mc.player.setPitch((float) -Math.toDegrees(Math.atan2(dy, dist)));
        }
        mc.interactionManager.attackEntity(mc.player, best);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
