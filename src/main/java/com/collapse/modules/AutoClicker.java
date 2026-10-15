package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

public class AutoClicker extends Module {
    private final NumSetting cps = add(new NumSetting("CPS", 11, 4, 20, 1));
    private long next = 0;

    public AutoClicker() {
        super("AutoClicker", "Авто-клик по цели", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult ehr)) return;
        if (!mc.options.attackKey.isPressed()) return;
        long now = System.currentTimeMillis();
        if (now < next) return;
        mc.interactionManager.attackEntity(mc.player, ehr.getEntity());
        mc.player.swingHand(Hand.MAIN_HAND);
        next = now + (long) (1000.0 / cps.get() * (0.85 + Math.random() * 0.3));
    }
}
