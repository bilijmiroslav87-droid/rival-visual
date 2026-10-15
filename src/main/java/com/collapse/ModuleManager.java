package com.collapse;

import com.collapse.modules.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {}

    public static void init() {
        // Combat
        register(new KillAura());
        register(new TriggerBot());
        register(new AimAssist());
        register(new AutoClicker());
        register(new Criticals());
        register(new Velocity());
        register(new AutoGapple());
        register(new AntiBot());
        // Movement
        register(new AutoSprint());
        register(new Fly());
        register(new Speed());
        register(new Step());
        register(new Jesus());
        register(new NoFall());
        register(new Spider());
        register(new Glide());
        register(new NoSlow());
        register(new ElytraBoost());
        register(new AirJump());
        // Visuals
        register(new Esp());
        register(new Tracers());
        register(new ChestEsp());
        register(new ItemEsp());
        register(new Chams());
        register(new Fullbright());
        register(new Zoom());
        register(new FovChanger());
        register(new CustomTime());
        register(new NoRain());
        register(new CustomFog());
        register(new Crosshair());
        register(new Vignette());
        register(new TargetHud());
        register(new Armor());
        register(new FullStatus());
        register(new Keystrokes());
        register(new Coordinates());
        register(new FpsInfo());
        register(new Watermark());
        register(new Notifications());
        // Player
        register(new AutoTotem());
        register(new AutoRespawn());
        register(new AntiAfk());
        register(new Nuker());
        register(new FastPlace());
        register(new ChestStealer());
        register(new InvCleaner());
        register(new ClickPearl());
        register(new AutoSwap());
        // Misc
        register(new Timer());
        register(new AutoLeave());
        register(new Panic());
        register(new AutoGG());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (Module m : MODULES) {
                if (!m.isEnabled()) continue;
                try {
                    m.onTick();
                } catch (Throwable t) {
                    CollapseClient.LOGGER.error("Module '{}' tick error", m.getName(), t);
                    m.setEnabled(false);
                }
            }
        });
    }

    public static void register(Module module) { MODULES.add(module); }

    public static void notifyToggle(Module m) {
        if (m.getName().equals("Zoom") || m.getName().equals("Panic")) return;
        Module n = byName("Notifications");
        if (n != null && n.isEnabled()) com.collapse.hud.Notify.push(m.getName(), m.isEnabled());
    }

    public static Module byName(String name) {
        for (Module m : MODULES) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }
}
