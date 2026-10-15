package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class ClickPearl extends Module {
    public ClickPearl() {
        super("ClickPearl", "Бросок жемчуга по включению", Category.PLAYER);
        noSave();
    }

    @Override
    protected void onEnable() {
        if (mc.player == null || mc.interactionManager == null) { setEnabled(false); return; }
        int prev = mc.player.getInventory().getSelectedSlot();
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.ENDER_PEARL)) {
                mc.player.getInventory().setSelectedSlot(i);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.getInventory().setSelectedSlot(prev);
                break;
            }
        }
        setEnabled(false);
    }
}
