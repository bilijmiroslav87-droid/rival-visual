package com.collapse;

public abstract class Module {
    public enum Category { VISUALS, MOVEMENT, MISC }

    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled = false;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        try {
            if (enabled) onEnable(); else onDisable();
        } catch (Throwable t) {
            CollapseClient.LOGGER.error("Module '{}' error", name, t);
        }
    }

    public void toggle() { setEnabled(!enabled); }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
}
