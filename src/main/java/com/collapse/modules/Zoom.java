package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.client.MinecraftClient;

public class Zoom extends Module {
    private Integer previousFov = null;

    public Zoom() {
        super("Zoom", "Приближение, зажми C", Category.MISC);
    }

    @Override
    protected void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null) {
            previousFov = client.options.getFov().getValue();
            client.options.getFov().setValue(15);
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null && previousFov != null) {
            client.options.getFov().setValue(previousFov);
            previousFov = null;
        }
    }
}
