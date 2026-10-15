package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.NumSetting;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class ChestStealer extends Module {
    private final NumSetting delay = add(new NumSetting("Delay ms", 80, 0, 500, 10));
    private final BoolSetting autoClose = add(new BoolSetting("Auto close", true));
    private long last = 0;

    public ChestStealer() {
        super("ChestStealer", "Забирает всё из сундука", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler h)) return;
        if (System.currentTimeMillis() - last < delay.get()) return;
        int size = h.getRows() * 9;
        for (int i = 0; i < size; i++) {
            if (h.getSlot(i).hasStack()) {
                mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                last = System.currentTimeMillis();
                return;
            }
        }
        if (autoClose.get()) mc.player.closeHandledScreen();
    }
}
