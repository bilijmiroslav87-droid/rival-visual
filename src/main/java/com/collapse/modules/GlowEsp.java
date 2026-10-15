package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.NumSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;

public class GlowEsp extends Module {
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", true));
    private final NumSetting range = add(new NumSetting("Range", 64, 8, 128, 1));
    private final Set<LivingEntity> glowed = new HashSet<>();

    public GlowEsp() {
        super("GlowESP", "Подсветка сущностей сквозь стены", Category.VISUALS);
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
            if (!(e instanceof LivingEntity l) || l == mc.player || e instanceof ArmorStandEntity) continue;
            boolean isPlayer = l instanceof PlayerEntity;
            if (isPlayer ? !players.get() : !mobs.get()) continue;
            if (l.isAlive() && l.squaredDistanceTo(mc.player) <= r2 && !l.isGlowing()) {
                l.setGlowing(true);
                glowed.add(l);
            }
        }
    }

    private void clear() {
        for (LivingEntity e : glowed) if (!e.isRemoved() && e.isGlowing()) e.setGlowing(false);
        glowed.clear();
    }

    @Override protected void onDisable() { clear(); }
}
