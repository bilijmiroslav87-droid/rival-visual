package com.collapse.modules;

import com.collapse.Module;
import com.collapse.cosmetics.CosmeticLayer;
import com.collapse.cosmetics.CosmeticModels;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.ModeSetting;
import com.collapse.settings.NumSetting;
import com.collapse.util.Theme;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

/** Косметика: крылья, головные уборы, нимб, плащи. Видна на твоём персонаже (F5 / инвентарь). */
public class Cosmetics extends Module {
    public static Cosmetics INSTANCE;

    public final ModeSetting wings = add(new ModeSetting("Крылья", "Нет", "Нет", "Ангел", "Демон", "Бабочка", "Дракон"));
    public final ModeSetting head = add(new ModeSetting("Головной убор", "Нет", "Нет", "Корона", "Нимб", "Рога", "Шляпа мага"));
    public final BoolSetting cape = add(new BoolSetting("Плащ", false));
    public final ModeSetting color = add(new ModeSetting("Цвет", "Тема", "Тема", "Белый", "Золото", "Красный", "Синий", "Фиолетовый", "Розовый", "Зелёный", "Радуга"));
    public final BoolSetting glow = add(new BoolSetting("Свечение", false));
    public final NumSetting flap = add(new NumSetting("Взмах крыльев", 1.0, 0.0, 3.0, 0.1));
    public final NumSetting size = add(new NumSetting("Размер", 1.0, 0.6, 1.6, 0.05));

    private ModelPart[] wingModels;
    private ModelPart crown, halo, horns, hat, capePart;

    public Cosmetics() {
        super("Cosmetics", "Крылья, короны, нимб и плащи на твоём персонаже", Category.COSMETICS);
        INSTANCE = this;
    }

    private void ensureModels() {
        if (wingModels != null) return;
        wingModels = new ModelPart[]{
                CosmeticModels.wing(0), CosmeticModels.wing(1), CosmeticModels.wing(2), CosmeticModels.wing(3)};
        crown = CosmeticModels.crown();
        halo = CosmeticModels.halo();
        horns = CosmeticModels.horns();
        hat = CosmeticModels.wizardHat();
        capePart = CosmeticModels.cape();
    }

    private int rgb() {
        return switch (color.get()) {
            case "Белый" -> 0xFFFFFF;
            case "Золото" -> 0xFFC93D;
            case "Красный" -> 0xFF3B4D;
            case "Синий" -> 0x4D8CFF;
            case "Фиолетовый" -> 0xA066FF;
            case "Розовый" -> 0xFF6BC1;
            case "Зелёный" -> 0x3DDC84;
            case "Радуга" -> Theme.rainbow(0) & 0xFFFFFF;
            default -> Theme.accent() & 0xFFFFFF;
        };
    }

    /** Вызывается из CosmeticLayer. Матрица уже в системе координат тела игрока (Y вниз, начало у плеч). */
    public void draw(CosmeticLayer l, MatrixStack m, OrderedRenderCommandQueue q, int light, PlayerEntityRenderState s) {
        ensureModels();
        int col = rgb();
        boolean g = glow.get();
        float sc = size.get().floatValue();
        float t = (System.currentTimeMillis() % 100000L) / 1000f;
        float sneak = s.isInSneakingPose ? 1f : 0f;
        float gliding = s.isGliding ? 1f : 0f;

        // ---- крылья ----
        int wi = indexOf(wings.get(), "Нет", "Ангел", "Демон", "Бабочка", "Дракон") - 1;
        if (wi >= 0) {
            float flapAmt = flap.get().floatValue();
            float swing = (float) Math.sin(t * 2.6f) * 0.28f * flapAmt;
            float spread = gliding > 0 ? 0.15f : (0.35f + swing);
            for (int side = -1; side <= 1; side += 2) {
                m.push();
                m.translate(0f, 0.05f + sneak * 0.12f, 0.12f + sneak * 0.05f);
                m.scale(sc, sc, sc);
                m.scale(0.0625f, 0.0625f, 0.0625f); // пиксели модели -> блоки
                m.scale(side, 1f, 1f);               // зеркалим левое крыло
                m.multiply(CosmeticLayer.Y().rotation(-spread * side * side));
                m.multiply(CosmeticLayer.Z().rotation(0.15f + swing * 0.4f));
                l.part(q, wingModels[wi], m, light, col, g);
                m.pop();
            }
        }

        // ---- головные уборы ----
        int hi = indexOf(head.get(), "Нет", "Корона", "Нимб", "Рога", "Шляпа мага");
        if (hi > 0) {
            m.push();
            // Голова: от плеч вверх на 8 пикселей (=0.5 блока), стоим на макушке
            m.translate(0f, -0.5f - sneak * 0.0f, 0f);
            m.scale(sc, sc, sc);
            m.scale(0.0625f, 0.0625f, 0.0625f);
            m.multiply(CosmeticLayer.Y().rotationDegrees(s.relativeHeadYaw));
            m.multiply(CosmeticLayer.X().rotationDegrees(s.pitch));
            ModelPart p = switch (hi) {
                case 1 -> crown;
                case 2 -> {
                    m.translate(0f, -4f + (float) Math.sin(t * 2f) * 0.6f, 0f); // нимб парит над головой
                    yield halo;
                }
                case 3 -> horns;
                default -> hat;
            };
            l.part(q, p, m, light, hi == 2 ? 0xFFE066 & 0xFFFFFF : col, hi == 2 || g);
            m.pop();
        }

        // ---- плащ ----
        if (cape.get() && s.equippedChestStack.isEmpty()) {
            m.push();
            m.translate(0f, 0f, 0.125f);
            m.scale(sc, sc, sc);
            m.scale(0.0625f, 0.0625f, 0.0625f);
            float lift = Math.min(1f, s.limbSwingAmplitude) * 0.35f + sneak * 0.2f;
            m.multiply(CosmeticLayer.X().rotation(0.08f + lift));
            l.part(q, capePart, m, light, col, g);
            m.pop();
        }
    }

    private static int indexOf(String v, String... all) {
        for (int i = 0; i < all.length; i++) if (all[i].equals(v)) return i;
        return 0;
    }
}
