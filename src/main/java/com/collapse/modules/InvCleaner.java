package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class InvCleaner extends Module {
    private long last = 0;

    public InvCleaner() {
        super("InvCleaner", "Выбрасывает мусор из инвентаря", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (!(mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen)) return;
        if (System.currentTimeMillis() - last < 120) return;
        for (int i = 9; i < 36; i++) {
            var st = mc.player.getInventory().getStack(i);
            if (st.isOf(Items.ROTTEN_FLESH) || st.isOf(Items.SPIDER_EYE) || st.isOf(Items.GRAVEL)
                    || st.isOf(Items.DIRT) || st.isOf(Items.COBBLESTONE) || st.isOf(Items.POISONOUS_POTATO)) {
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 1, SlotActionType.THROW, mc.player);
                last = System.currentTimeMillis();
                return;
            }
        }
    }
}
