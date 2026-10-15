package com.collapse.config;

import com.collapse.CollapseClient;
import com.collapse.Module;
import com.collapse.ModuleManager;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.ClientSettings;
import com.collapse.settings.ColorSetting;
import com.collapse.settings.ModeSetting;
import com.collapse.settings.NumSetting;
import com.collapse.settings.Setting;
import com.collapse.util.Theme;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Конфиги .properties. Основной: collapse-client.properties.
 * Профили: config/collapse-client/profiles/&lt;имя&gt;.properties.
 */
public final class Config {
    private static final Path DIR = FabricLoader.getInstance().getConfigDir();
    private static final Path FILE = DIR.resolve("collapse-client.properties");
    private static final Path PROFILES = DIR.resolve("collapse-client").resolve("profiles");

    public static String currentProfile = "default";

    private Config() {}

    public static void save() { write(FILE); }

    public static void load() { read(FILE); }

    // ----------- профили -----------
    public static List<String> profiles() {
        List<String> out = new ArrayList<>();
        try {
            Files.createDirectories(PROFILES);
            try (Stream<Path> s = Files.list(PROFILES)) {
                s.forEach(p -> {
                    String n = p.getFileName().toString();
                    if (n.endsWith(".properties")) out.add(n.substring(0, n.length() - 11));
                });
            }
        } catch (IOException ignored) {}
        Collections.sort(out);
        return out;
    }

    private static String safe(String name) {
        String n = name.replaceAll("[^A-Za-z0-9_\\-А-Яа-яЁё]", "_");
        return n.isEmpty() ? "profile" : n;
    }

    public static boolean saveProfile(String name) {
        try {
            Files.createDirectories(PROFILES);
            write(PROFILES.resolve(safe(name) + ".properties"));
            currentProfile = safe(name);
            return true;
        } catch (IOException e) { return false; }
    }

    public static boolean loadProfile(String name) {
        Path p = PROFILES.resolve(safe(name) + ".properties");
        if (!Files.exists(p)) return false;
        read(p);
        currentProfile = safe(name);
        save();
        return true;
    }

    public static boolean deleteProfile(String name) {
        try { return Files.deleteIfExists(PROFILES.resolve(safe(name) + ".properties")); }
        catch (IOException e) { return false; }
    }

    // ----------- запись / чтение -----------
    private static void write(Path file) {
        try {
            Properties p = new Properties();
            for (Module m : ModuleManager.MODULES) {
                String k = m.getName() + ".";
                p.setProperty(k + "enabled", String.valueOf(m.isEnabled()));
                p.setProperty(k + "bind", String.valueOf(m.getBind()));
                p.setProperty(k + "fav", String.valueOf(m.isFavorite()));
                for (Setting<?> s : m.getSettings()) {
                    p.setProperty(k + s.getName(), String.valueOf(s.get()));
                }
            }
            p.setProperty("theme", String.valueOf(Theme.index()));
            p.setProperty("ui.scale", String.valueOf(ClientSettings.menuScale));
            p.setProperty("ui.anim", String.valueOf(ClientSettings.animSpeed));
            p.setProperty("ui.dim", String.valueOf(ClientSettings.dimBackground));
            p.setProperty("ui.hints", String.valueOf(ClientSettings.showHints));
            p.setProperty("ui.arraylist", String.valueOf(ClientSettings.arrayList));
            p.setProperty("ui.arraylistBg", String.valueOf(ClientSettings.arrayListBg));
            p.setProperty("ui.arraylistPos", String.valueOf(ClientSettings.arrayListPos));
            p.setProperty("ui.animTheme", String.valueOf(ClientSettings.animatedTheme));
            p.setProperty("ui.profile", currentProfile);
            Files.createDirectories(file.getParent());
            try (var w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                p.store(w, "Collapse Client");
            }
        } catch (IOException e) {
            CollapseClient.LOGGER.error("Config save failed", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void read(Path file) {
        if (!Files.exists(file)) return;
        try (var r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Properties p = new Properties();
            p.load(r);
            for (Module m : ModuleManager.MODULES) {
                String k = m.getName() + ".";
                String b = p.getProperty(k + "bind");
                if (b != null) m.setBind(Integer.parseInt(b));
                m.setFavorite(Boolean.parseBoolean(p.getProperty(k + "fav", "false")));
                for (Setting<?> s : m.getSettings()) {
                    String v = p.getProperty(k + s.getName());
                    if (v == null) continue;
                    try {
                        if (s instanceof BoolSetting bs) bs.set(Boolean.parseBoolean(v));
                        else if (s instanceof NumSetting ns) ns.set(Double.parseDouble(v));
                        else if (s instanceof ModeSetting ms) ms.set(v);
                        else if (s instanceof ColorSetting cs) cs.set(Integer.parseInt(v));
                    } catch (Exception ignored) {}
                }
                boolean want = Boolean.parseBoolean(p.getProperty(k + "enabled", "false")) && m.isSaveEnabled();
                if (m.isEnabled() != want) m.setEnabled(want);
            }
            String t = p.getProperty("theme");
            if (t != null) Theme.setIndex(Integer.parseInt(t));
            ClientSettings.menuScale = f(p, "ui.scale", ClientSettings.menuScale);
            ClientSettings.animSpeed = f(p, "ui.anim", ClientSettings.animSpeed);
            ClientSettings.dimBackground = Boolean.parseBoolean(p.getProperty("ui.dim", String.valueOf(ClientSettings.dimBackground)));
            ClientSettings.showHints = Boolean.parseBoolean(p.getProperty("ui.hints", String.valueOf(ClientSettings.showHints)));
            ClientSettings.arrayList = Boolean.parseBoolean(p.getProperty("ui.arraylist", String.valueOf(ClientSettings.arrayList)));
            ClientSettings.arrayListBg = Boolean.parseBoolean(p.getProperty("ui.arraylistBg", String.valueOf(ClientSettings.arrayListBg)));
            ClientSettings.arrayListPos = (int) f(p, "ui.arraylistPos", ClientSettings.arrayListPos);
            ClientSettings.animatedTheme = Boolean.parseBoolean(p.getProperty("ui.animTheme", String.valueOf(ClientSettings.animatedTheme)));
            Theme.animated = ClientSettings.animatedTheme;
            currentProfile = p.getProperty("ui.profile", currentProfile);
        } catch (Exception e) {
            CollapseClient.LOGGER.error("Config load failed", e);
        }
    }

    private static float f(Properties p, String k, float d) {
        try { return Float.parseFloat(p.getProperty(k, String.valueOf(d))); } catch (Exception e) { return d; }
    }
}
