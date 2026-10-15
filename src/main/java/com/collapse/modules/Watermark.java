package com.collapse.modules;

import com.collapse.Module;

/** Только флаг для HUD: показывать ли водянку. */
public class Watermark extends Module {
    public Watermark() {
        super("Watermark", "Водяной знак клиента", Category.VISUALS);
    }
}
