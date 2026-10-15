package com.collapse.modules;

import com.collapse.Module;

public class FastPlace extends Module {
    public FastPlace() {
        super("FastPlace", "Быстрая установка блоков", Category.PLAYER);
    }

    @Override
    public void onTick() {
        // Задержка использования предмета сбрасывается в миксине; тут оставляем заглушку-флаг
    }
}
