package com.collapse.config;

import com.collapse.CollapseClient;
import com.collapse.Module;
import com.collapse.ModuleManager;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.ModeSetting;
import com.collapse.settings.NumSetting;
import com.collapse.settings.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Простой конфиг .properties: включённые модули, значения настроек, бинды. */
public final class Config {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("collapse-client.properties");

    private Config() {}

    public static void save() {
        try {
            Properties p = new Properties();
            for (Module m : ModuleManager.MODULES) {
                String k = m.getName() + ".";
                p.setProperty(k + "enabled", String.valueOf(m.isEnabled()));
                p.setProperty(k + "bind", String.valueOf(m.getBind()));
                for (Setting<?> s : m.getSettings()) {
                    p.setProperty(k + s.getName(), String.valueOf(s.get()));
                }
            }
            p.setProperty("theme", String.valueOf(com.collapse.util.Theme.index()));
            try (var w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                p.store(w, "Collapse Client");
            }
        } catch (IOException e) {
            CollapseClient.LOGGER.error("Config save failed", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void load() {
        if (!Files.exists(FILE)) return;
        try (var r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            Properties p = new Properties();
            p.load(r);
            for (Module m : ModuleManager.MODULES) {
                String k = m.getName() + ".";
                String b = p.getProperty(k + "bind");
                if (b != null) m.setBind(Integer.parseInt(b));
                for (Setting<?> s : m.getSettings()) {
                    String v = p.getProperty(k + s.getName());
                    if (v == null) continue;
                    try {
                        if (s instanceof BoolSetting bs) bs.set(Boolean.parseBoolean(v));
                        else if (s instanceof NumSetting ns) ns.set(Double.parseDouble(v));
                        else if (s instanceof ModeSetting ms) ms.set(v);
                    } catch (Exception ignored) {}
                }
                if (Boolean.parseBoolean(p.getProperty(k + "enabled", "false")) && m.isSaveEnabled()) m.setEnabled(true);
            }
            String t = p.getProperty("theme");
            if (t != null) com.collapse.util.Theme.setIndex(Integer.parseInt(t));
        } catch (Exception e) {
            CollapseClient.LOGGER.error("Config load failed", e);
        }
    }
}
