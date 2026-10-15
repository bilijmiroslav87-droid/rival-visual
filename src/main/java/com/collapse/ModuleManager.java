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
        register(new AutoCrit());
        register(new Velocity());
        // Movement
        register(new AutoSprint());
        register(new Fly());
        register(new Speed());
        register(new Step());
        register(new Jesus());
        register(new NoFall());
        register(new HighJump());
        register(new Spider());
        register(new Glide());
        // Visuals
        register(new GlowEsp());
        register(new Fullbright());
        register(new Zoom());
        register(new FovChanger());
        register(new CustomTime());
        register(new NoRain());
        register(new CustomFog());
        register(new Crosshair());
        register(new Vignette());
        register(new Coordinates());
        register(new FpsInfo());
        // Player
        register(new AutoTotem());
        register(new AutoRespawn());
        register(new AntiAfk());
        register(new Nuker());
        register(new FastPlace());
        // Misc
        register(new Timer());
        register(new Panic());

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

    public static Module byName(String name) {
        for (Module m : MODULES) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }
}
