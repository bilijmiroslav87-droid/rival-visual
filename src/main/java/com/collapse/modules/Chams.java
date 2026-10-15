package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;

/** Встроенное свечение игроков сквозь стены (glow), без шейдеров. */
public class Chams extends Module {
    private final NumSetting range = add(new NumSetting("Range", 70, 8, 150, 2));
    private final Set<LivingEntity> glowed = new HashSet<>();

    public Chams() {
        super("Glow", "Свечение игроков сквозь стены", Category.VISUALS);
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) { clear(); return; }
        double r2 = range.get() * range.get();
        glowed.removeIf(e -> {
            boolean bad = e.isRemoved() || !e.isAlive() || e.squaredDistanceTo(mc.player) > r2;
            if (bad && e.isGlowing()) e.setGlowing(false);
            return bad;
        });
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof PlayerEntity p) || p == mc.player || e instanceof ArmorStandEntity) continue;
            if (p.isAlive() && p.squaredDistanceTo(mc.player) <= r2 && !p.isGlowing()) {
                p.setGlowing(true);
                glowed.add(p);
            }
        }
    }

    private void clear() {
        for (LivingEntity e : glowed) if (!e.isRemoved() && e.isGlowing()) e.setGlowing(false);
        glowed.clear();
    }

    @Override protected void onDisable() { clear(); }
}
