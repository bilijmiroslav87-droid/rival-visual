package com.collapse.modules;

import com.collapse.Module;
import com.collapse.settings.ModeSetting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Fullbright extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Gamma", "Gamma", "Potion"));
    private Double prev = null;

    public Fullbright() {
        super("Fullbright", "Максимальная яркость", Category.VISUALS);
    }

    @Override
    protected void onEnable() {
        if (mc.options != null) prev = mc.options.getGamma().getValue();
    }

    @Override
    protected void onDisable() {
        if (mc.options != null && prev != null) mc.options.getGamma().setValue(prev);
        prev = null;
        if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }

    @Override
    public void onTick() {
        if (mc.options == null) return;
        if (mode.is("Gamma")) {
            if (mc.options.getGamma().getValue() < 16.0) mc.options.getGamma().setValue(16.0);
        } else if (mc.player != null) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 600, 0, false, false));
        }
    }
}
