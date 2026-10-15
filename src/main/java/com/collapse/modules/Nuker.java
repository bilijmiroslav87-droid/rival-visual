package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Hand;

public class Nuker extends Module {
    private final NumSetting radius = add(new NumSetting("Radius", 2, 1, 5, 1));

    public Nuker() {
        super("Nuker", "Ломает блоки вокруг", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        BlockPos p = mc.player.getBlockPos();
        int r = radius.i();
        int broken = 0;
        for (int x = -r; x <= r && broken < 3; x++)
            for (int y = 0; y <= r && broken < 3; y++)
                for (int z = -r; z <= r && broken < 3; z++) {
                    BlockPos bp = p.add(x, y, z);
                    if (!mc.world.getBlockState(bp).isAir()
                            && mc.world.getBlockState(bp).getHardness(mc.world, bp) >= 0) {
                        mc.interactionManager.breakBlock(bp);
                        broken++;
                    }
                }
        if (broken > 0) mc.player.swingHand(Hand.MAIN_HAND);
    }
}
