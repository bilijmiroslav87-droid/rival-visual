package com.collapse.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Targets {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private Targets() {}

    public static boolean isFriend(PlayerEntity p) {
        return Friends.isFriend(p.getName().getString());
    }

    public static List<LivingEntity> find(double range, boolean players, boolean mobs, boolean animals,
                                          boolean invisible, boolean walls, String sort) {
        List<LivingEntity> out = new ArrayList<>();
        if (mc.world == null || mc.player == null) return out;
        double r2 = range * range;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity l) || l == mc.player || !l.isAlive()) continue;
            if (e instanceof ArmorStandEntity) continue;
            if (l.squaredDistanceTo(mc.player) > r2) continue;
            if (l.isInvisible() && !invisible) continue;
            if (l instanceof PlayerEntity p) {
                if (!players || isFriend(p)) continue;
                if (p.isSpectator()) continue;
            } else if (l instanceof Monster) {
                if (!mobs) continue;
            } else if (l instanceof AnimalEntity) {
                if (!animals) continue;
            } else continue;
            if (!walls && !canSee(l)) continue;
            out.add(l);
        }
        Comparator<LivingEntity> cmp = switch (sort) {
            case "Health" -> Comparator.comparingDouble(LivingEntity::getHealth);
            case "FOV" -> Comparator.comparingDouble(RotationManager::error);
            default -> Comparator.comparingDouble(l -> l.squaredDistanceTo(mc.player));
        };
        out.sort(cmp);
        return out;
    }

    public static boolean canSee(Entity e) {
        if (mc.world == null || mc.player == null) return false;
        Vec3d from = mc.player.getEyePos();
        Vec3d to = new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.6, e.getZ());
        var res = mc.world.raycast(new RaycastContext(from, to,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return res.getType() == HitResult.Type.MISS;
    }
}
