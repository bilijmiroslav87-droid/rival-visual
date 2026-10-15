package com.collapse.modules;

import com.collapse.Module;
import com.collapse.ModuleManager;

public class Panic extends Module {
    public Panic() {
        super("Panic", "Выключает все модули разом", Category.MISC);
    }

    @Override
    protected void onEnable() {
        for (Module m : ModuleManager.MODULES) {
            if (m != this) m.setEnabled(false);
        }
        setEnabled(false);
    }
}
