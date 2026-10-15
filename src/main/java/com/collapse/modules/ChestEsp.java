package com.collapse.modules;

import com.collapse.Module;
import com.collapse.render.Projector;
import com.collapse.settings.NumSetting;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

public class ChestEsp extends Module {
    private final NumSetting range = add(new NumSetting("Range", 48, 8, 128, 4));

    public ChestEsp() {
        super("ChestESP", "Подсвечивает сундуки и шалкеры", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        if (mc.world == null || mc.player == null) return;
        float td = mc.getRenderTickCounter().getTickProgress(false);
        int cx = mc.player.getChunkPos().x, cz = mc.player.getChunkPos().z;
        int rc = (int) (range.get() / 16) + 1;
        double r2 = range.get() * range.get();

        for (int x = cx - rc; x <= cx + rc; x++) for (int z = cz - rc; z <= cz + rc; z++) {
            WorldChunk ch = mc.world.getChunkManager().getWorldChunk(x, z);
            if (ch == null) continue;
            for (BlockEntity be : ch.getBlockEntities().values()) {
                int col;
                if (be instanceof ChestBlockEntity) col = 0xFFFFB84A;
                else if (be instanceof EnderChestBlockEntity) col = 0xFFB04AFF;
                else if (be instanceof ShulkerBoxBlockEntity) col = 0xFFFF6BC1;
                else if (be instanceof BarrelBlockEntity) col = 0xFFC48A4A;
                else continue;
                BlockPos p = be.getPos();
                if (p.getSquaredDistance(mc.player.getBlockPos()) > r2) continue;
                float[] a = Projector.project(new Vec3d(p.getX(), p.getY(), p.getZ()), td);
                float[] b = Projector.project(new Vec3d(p.getX() + 1, p.getY() + 1, p.getZ() + 1), td);
                float[] c2 = Projector.project(new Vec3d(p.getX() + 1, p.getY(), p.getZ()), td);
                float[] d = Projector.project(new Vec3d(p.getX(), p.getY() + 1, p.getZ() + 1), td);
                if (a == null || b == null || c2 == null || d == null) continue;
                float x1 = Math.min(Math.min(a[0], b[0]), Math.min(c2[0], d[0]));
                float x2 = Math.max(Math.max(a[0], b[0]), Math.max(c2[0], d[0]));
                float y1 = Math.min(Math.min(a[1], b[1]), Math.min(c2[1], d[1]));
                float y2 = Math.max(Math.max(a[1], b[1]), Math.max(c2[1], d[1]));
                ctx.fill((int) x1, (int) y1, (int) x2, (int) y1 + 1, col);
                ctx.fill((int) x1, (int) y2 - 1, (int) x2, (int) y2, col);
                ctx.fill((int) x1, (int) y1, (int) x1 + 1, (int) y2, col);
                ctx.fill((int) x2 - 1, (int) y1, (int) x2, (int) y2, col);
                ctx.fill((int) x1 + 1, (int) y1 + 1, (int) x2 - 1, (int) y2 - 1, (col & 0xFFFFFF) | 0x28000000);
            }
        }
    }
}
