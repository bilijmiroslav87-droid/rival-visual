package com.collapse.modules;

import com.collapse.Module;
import com.collapse.mixin.MinecraftClientMixin;

public class FastPlace extends Module {
    public FastPlace() {
        super("FastPlace", "Быстрая установка блоков", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc == null) return;
        ((MinecraftClientMixin) mc).collapse$setItemUseCooldown(0);
    }
}
