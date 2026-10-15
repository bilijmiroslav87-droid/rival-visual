package com.collapse.modules;

import com.collapse.Module;
import net.minecraft.client.MinecraftClient;

public class Fullbright extends Module {
    private Double previousGamma = null;

    public Fullbright() {
        super("Fullbright", "Максимальная яркость", Category.VISUALS);
    }

    @Override
    protected void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null) {
            previousGamma = client.options.getGamma().getValue();
            client.options.getGamma().setValue(16.0);
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null && previousGamma != null) {
            client.options.getGamma().setValue(previousGamma);
            previousGamma = null;
        }
    }

    @Override
    public void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null && previousGamma != null && client.options.getGamma().getValue() < 16.0) {
            client.options.getGamma().setValue(16.0);
        }
    }
}
