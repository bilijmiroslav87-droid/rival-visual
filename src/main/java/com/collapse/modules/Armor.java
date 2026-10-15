package com.collapse.modules;

import com.collapse.Module;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class Armor extends Module {
    public Armor() {
        super("ArmorHUD", "Прочность брони на экране", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        if (mc.player == null) return;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int x = w / 2 + 96, y = h - 20;
        for (EquipmentSlot s : slots) {
            ItemStack st = mc.player.getEquippedStack(s);
            if (st.isEmpty()) { x += 20; continue; }
            ctx.drawItem(st, x, y - 16);
            if (st.isDamageable()) {
                float f = 1f - st.getDamage() / (float) st.getMaxDamage();
                Draw.roundRect(ctx, x, y + 1, 16, 3, 1.5f, 0xFF2A2A34);
                Draw.roundRect(ctx, x, y + 1, Math.max(2, 16 * f), 3, 1, com.collapse.util.Theme.mix(0xFFFF3B3B, 0xFF3DDC84, f));
            }
            x += 20;
        }
    }
}
