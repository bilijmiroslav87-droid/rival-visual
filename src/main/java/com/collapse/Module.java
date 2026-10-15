package com.collapse;

import com.collapse.settings.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public enum Category {
        COMBAT("Combat"), MOVEMENT("Movement"), VISUALS("Visuals"),
        PLAYER("Player"), MISC("Misc"), COSMETICS("Косметика");

        public final String label;
        Category(String label) { this.label = label; }
    }

    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled = false;
    private int bind = -1;
    private boolean saveEnabled = true;
    private boolean favorite = false;
    private boolean hidden = false;
    public final com.collapse.render.Anim anim = new com.collapse.render.Anim(0f, 12f);

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <S extends Setting<?>> S add(S s) {
        settings.add(s);
        return s;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public List<Setting<?>> getSettings() { return settings; }
    public boolean isEnabled() { return enabled; }
    public int getBind() { return bind; }
    public void setBind(int k) { this.bind = k; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean f) { this.favorite = f; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean h) { this.hidden = h; }
    public boolean isSaveEnabled() { return saveEnabled; }
    protected void noSave() { this.saveEnabled = false; }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        anim.set(value ? 1f : 0f);
        try { ModuleManager.notifyToggle(this); } catch (Throwable ignored) {}
        try {
            if (enabled) onEnable(); else onDisable();
        } catch (Throwable t) {
            CollapseClient.LOGGER.error("Module '{}' toggle error", name, t);
        }
    }

    public void toggle() { setEnabled(!enabled); }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
    public void onHud(net.minecraft.client.gui.DrawContext ctx, int w, int h) {}
}
