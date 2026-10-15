package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class Nuker extends Module {
    private final NumSetting radius = add(new NumSetting("Radius", 2, 1, 5, 1));
    private final NumSetting perTick = add(new NumSetting("Blocks/tick", 2, 1, 6, 1));

    public Nuker() {
        super("Nuker", "Ломает блоки вокруг", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        BlockPos p = mc.player.getBlockPos();
        int r = radius.i(), max = perTick.i(), broken = 0;
        for (int x = -r; x <= r && broken < max; x++)
            for (int y = 0; y <= r && broken < max; y++)
                for (int z = -r; z <= r && broken < max; z++) {
                    BlockPos bp = p.add(x, y, z);
                    var st = mc.world.getBlockState(bp);
                    if (!st.isAir() && st.getHardness(mc.world, bp) >= 0) {
                        mc.interactionManager.breakBlock(bp);
                        broken++;
                    }
                }
        if (broken > 0) mc.player.swingHand(Hand.MAIN_HAND);
    }
}
