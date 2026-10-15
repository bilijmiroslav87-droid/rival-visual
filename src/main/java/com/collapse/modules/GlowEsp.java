package com.collapse.modules;

import com.collapse.CollapseClient;
import com.collapse.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;

public class GlowEsp extends Module {
    private final Set<LivingEntity> glowed = new HashSet<>();
    private boolean players = true;
    private boolean mobs = true;
    private double range = 64.0;

    public GlowEsp() {
        super("GlowESP", "Подсветка сущностей сквозь стены", Category.VISUALS);
    }

    public void setPlayers(boolean players) { this.players = players; }
    public void setMobs(boolean mobs) { this.mobs = mobs; }
    public void setRange(double range) { this.range = range; }

    @Override
    public void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            clearGlow();
            return;
        }

        glowed.removeIf(e -> {
            boolean invalid = e.isRemoved() || !e.isAlive()
                    || e.squaredDistanceTo(client.player) > range * range;
            if (invalid && e.isGlowing()) e.setGlowing(false);
            return invalid;
        });

        for (Entity e : client.world.getEntities()) {
            if (!(e instanceof LivingEntity living)) continue;
            if (living == client.player) continue;
            if (living.isAlive() && isTarget(living)
                    && living.squaredDistanceTo(client.player) <= range * range) {
                if (!living.isGlowing()) {
                    living.setGlowing(true);
                    glowed.add(living);
                }
            }
        }
    }

    private boolean isTarget(LivingEntity e) {
        boolean isPlayer = e instanceof PlayerEntity;
        return (players && isPlayer) || (mobs && !isPlayer);
    }

    private void clearGlow() {
        for (LivingEntity e : glowed) {
            if (e.isGlowing() && !e.isRemoved()) e.setGlowing(false);
        }
        glowed.clear();
    }

    @Override
    protected void onDisable() { clearGlow(); }

    @Override
    protected void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§7[§bCollapse§7] §f" + getName() + " §aвключён"), false);
        }
    }
}
