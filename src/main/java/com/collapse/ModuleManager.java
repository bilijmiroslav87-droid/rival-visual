package com.collapse;

import com.collapse.modules.AutoSprint;
import com.collapse.modules.Fullbright;
import com.collapse.modules.GlowEsp;
import com.collapse.modules.Zoom;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() {}

    public static void init() {
        register(new GlowEsp());
        register(new Fullbright());
        register(new Zoom());
        register(new AutoSprint());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (Module m : MODULES) {
                if (m.isEnabled()) m.onTick();
            }
        });
    }

    public static void register(Module module) {
        MODULES.add(module);
    }

    public static Module byName(String name) {
        for (Module m : MODULES) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }
}
