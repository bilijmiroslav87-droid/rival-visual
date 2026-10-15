package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.item.Items;

public class AutoSwap extends Module {
    public AutoSwap() {
        super("AutoTool", "Подбирает лучший инструмент", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (!mc.options.attackKey.isPressed()) return;
        if (!(mc.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult bhr)) return;
        var state = mc.world.getBlockState(bhr.getBlockPos());
        float best = 1f; int slot = -1;
        for (int i = 0; i < 9; i++) {
            float s = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(state);
            if (s > best) { best = s; slot = i; }
        }
        if (slot >= 0 && slot != mc.player.getInventory().getSelectedSlot()) mc.player.getInventory().setSelectedSlot(slot);
    }
}
