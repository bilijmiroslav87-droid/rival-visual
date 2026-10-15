package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.NumSetting;

public class AutoLeave extends Module {
    private final NumSetting health = add(new NumSetting("Health", 5, 1, 19, 1));

    public AutoLeave() {
        super("AutoLeave", "Выходит в /hub при низком HP", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (mc.player.getHealth() <= health.f() && mc.player.getHealth() > 0) {
            mc.getNetworkHandler().sendChatCommand("hub");
            setEnabled(false);
        }
    }
}
