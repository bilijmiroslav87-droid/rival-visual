package com.collapse.modules;

import com.collapse.Module;
import com.collapse.combat.RotationManager;
import com.collapse.combat.Targets;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.ModeSetting;
import com.collapse.settings.NumSetting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

import java.util.List;
import java.util.Random;

public class KillAura extends Module {
    public final NumSetting range = add(new NumSetting("Range", 3.0, 2.5, 6.0, 0.1));
    public final NumSetting rotSpeed = add(new NumSetting("Rot speed", 38, 8, 120, 1));
    private final NumSetting jitter = add(new NumSetting("Jitter", 3, 0, 10, 1));
    private final NumSetting fovLimit = add(new NumSetting("Attack FOV err", 14, 2, 90, 1));
    private final ModeSetting sort = add(new ModeSetting("Sort", "Distance", "Distance", "Health", "FOV"));
    private final ModeSetting rotMode = add(new ModeSetting("Rotation", "Smooth", "Smooth", "Snap", "None"));
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", false));
    private final BoolSetting animals = add(new BoolSetting("Animals", false));
    private final BoolSetting invis = add(new BoolSetting("Invisible", true));
    private final BoolSetting walls = add(new BoolSetting("Through walls", false));
    private final BoolSetting onlyCrit = add(new BoolSetting("Only crits", false));
    private final BoolSetting rayCheck = add(new BoolSetting("Raycast check", true));
    private final BoolSetting sprintReset = add(new BoolSetting("Sprint reset", true));
    private final BoolSetting noHitUsing = add(new BoolSetting("Pause when eating", true));

    private static LivingEntity current;
    private final Random rng = new Random();
    private int delay = 0;

    public KillAura() {
        super("KillAura", "Атака целей с серверными ротациями", Category.COMBAT);
    }

    public static LivingEntity getTarget() { return current; }

    @Override
    protected void onDisable() {
        current = null;
        RotationManager.reset();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        RotationManager.tick();
        if (delay > 0) delay--;
        if (noHitUsing.get() && mc.player.isUsingItem()) { current = null; return; }

        List<LivingEntity> list = Targets.find(range.get() + 0.6, players.get(), mobs.get(), animals.get(),
                invis.get(), walls.get(), sort.get());
        if (list.isEmpty()) { current = null; RotationManager.reset(); return; }
        current = list.get(0);

        if (!rotMode.is("None")) {
            float speed = rotMode.is("Snap") ? 180f : rotSpeed.f();
            RotationManager.rotateTo(current, speed, jitter.f());
        }

        if (mc.player.squaredDistanceTo(current) > range.get() * range.get()) return;
        if (delay > 0) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.93f) return;
        if (!rotMode.is("None") && RotationManager.error(current) > fovLimit.get()) return;
        if (rayCheck.get() && !Targets.canSee(current) && !walls.get()) return;
        if (onlyCrit.get() && !(mc.player.fallDistance > 0.05f && !mc.player.isOnGround())) return;

        boolean wasSprint = mc.player.isSprinting();
        if (sprintReset.get() && wasSprint) mc.player.setSprinting(false);
        mc.interactionManager.attackEntity(mc.player, current);
        mc.player.swingHand(Hand.MAIN_HAND);
        if (sprintReset.get() && wasSprint) mc.player.setSprinting(true);
        delay = rng.nextInt(2);
    }
}
