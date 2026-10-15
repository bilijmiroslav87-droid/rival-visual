package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;
import net.minecraft.item.Items;

public class AutoGapple extends Module {
    private final NumSetting health = add(new NumSetting("Health", 10, 2, 19, 1));
    private int prev = -1;
    private boolean eating = false;

    public AutoGapple() {
        super("AutoGapple", "Ест яблоко при низком HP", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (eating) {
            if (!mc.player.isUsingItem() || mc.player.getHealth() > health.f() + 4) {
                mc.options.useKey.setPressed(false);
                if (prev >= 0) mc.player.getInventory().setSelectedSlot(prev);
                prev = -1; eating = false;
            }
            return;
        }
        if (mc.player.getHealth() > health.f()) return;
        for (int i = 0; i < 9; i++) {
            var st = mc.player.getInventory().getStack(i);
            if (st.isOf(Items.GOLDEN_APPLE) || st.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
                prev = mc.player.getInventory().getSelectedSlot();
                mc.player.getInventory().setSelectedSlot(i);
                mc.options.useKey.setPressed(true);
                eating = true;
                return;
            }
        }
    }

    @Override
    protected void onDisable() {
        if (mc.options != null) mc.options.useKey.setPressed(false);
        eating = false;
    }
}
