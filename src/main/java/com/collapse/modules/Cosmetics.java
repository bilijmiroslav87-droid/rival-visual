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

/** Косметика: крылья, головные уборы, хвост, орбиты, плащи. Видна на твоём персонаже (F5 / инвентарь). */
public class Cosmetics extends Module {
    public static Cosmetics INSTANCE;

    private static final String[] WINGS = {"Нет", "Ангел", "Демон", "Бабочка", "Дракон", "Феникс", "Кристалл", "Призрак", "Кибер"};
    private static final String[] HEADS = {"Нет", "Корона", "Нимб", "Рога", "Шляпа мага", "Кошачьи уши", "Антенны", "Венок", "Самурай"};

    public final ModeSetting wings = add(new ModeSetting("Крылья", "Нет", WINGS));
    public final ModeSetting head = add(new ModeSetting("Головной убор", "Нет", HEADS));
    public final BoolSetting cape = add(new BoolSetting("Плащ", false));
    public final BoolSetting tail = add(new BoolSetting("Хвост", false));
    public final BoolSetting orbs = add(new BoolSetting("Орбиты", false));
    public final ModeSetting color = add(new ModeSetting("Цвет", "Тема", "Тема", "Градиент", "Белый", "Золото", "Красный", "Синий", "Фиолетовый", "Розовый", "Зелёный", "Радуга"));
    public final BoolSetting glow = add(new BoolSetting("Свечение", false));
    public final BoolSetting float_ = add(new BoolSetting("Парение", true));
    public final NumSetting flap = add(new NumSetting("Взмах крыльев", 1.0, 0.0, 3.0, 0.1));
    public final NumSetting size = add(new NumSetting("Размер", 1.0, 0.6, 1.6, 0.05));

    private ModelPart[] wingModels;
    private ModelPart crown, halo, horns, hat, capePart, ears, antennae, wreath, samurai, tailPart, orb;

    public Cosmetics() {
        super("Cosmetics", "Крылья, короны, хвост, орбиты и плащи на твоём персонаже", Category.COSMETICS);
        INSTANCE = this;
    }

    private void ensureModels() {
        if (wingModels != null) return;
        wingModels = new ModelPart[8];
        for (int i = 0; i < 8; i++) wingModels[i] = CosmeticModels.wing(i);
        crown = CosmeticModels.crown();
        halo = CosmeticModels.halo();
        horns = CosmeticModels.horns();
        hat = CosmeticModels.wizardHat();
        capePart = CosmeticModels.cape();
        ears = CosmeticModels.catEars();
        antennae = CosmeticModels.antennae();
        wreath = CosmeticModels.wreath();
        samurai = CosmeticModels.samurai();
        tailPart = CosmeticModels.tail();
        orb = CosmeticModels.orb();
    }

    /** k: 0..1, позиция вдоль элемента (для градиента). */
    private int rgb(float k) {
        return switch (color.get()) {
            case "Белый" -> 0xFFFFFF;
            case "Золото" -> 0xFFC93D;
            case "Красный" -> 0xFF3B4D;
            case "Синий" -> 0x4D8CFF;
            case "Фиолетовый" -> 0xA066FF;
            case "Розовый" -> 0xFF6BC1;
            case "Зелёный" -> 0x3DDC84;
            case "Радуга" -> Theme.rainbow(k * 0.35) & 0xFFFFFF;
            case "Градиент" -> Theme.mix(Theme.accent(), Theme.accent2(), (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 700.0 + k * 3))) & 0xFFFFFF;
            default -> Theme.accent() & 0xFFFFFF;
        };
    }

    /** Вызывается из CosmeticLayer. Матрица уже в системе координат тела игрока (Y вниз, начало у плеч). */
    public void draw(CosmeticLayer l, MatrixStack m, OrderedRenderCommandQueue q, int light, PlayerEntityRenderState s) {
        ensureModels();
        boolean g = glow.get();
        float sc = size.get().floatValue();
        float t = (System.currentTimeMillis() % 100000L) / 1000f;
        float sneak = s.isInSneakingPose ? 1f : 0f;
        float gliding = s.isGliding ? 1f : 0f;
        float bob = float_.get() ? (float) Math.sin(t * 1.6f) * 0.012f : 0f;

        // ---- крылья ----
        int wi = indexOf(wings.get(), WINGS) - 1;
        if (wi >= 0) {
            float flapAmt = flap.get().floatValue();
            float swing = (float) Math.sin(t * 2.6f) * 0.28f * flapAmt;
            float spread = gliding > 0 ? 0.15f : (0.35f + swing);
            // лёгкое «дыхание» крыльев в покое и более широкий взмах при ходьбе
            float walk = Math.min(1f, s.limbSwingAmplitude);
            spread += walk * (float) Math.sin(t * 6f) * 0.06f * flapAmt;
            for (int side = -1; side <= 1; side += 2) {
                m.push();
                m.translate(0f, 0.05f + sneak * 0.12f + bob, 0.12f + sneak * 0.05f);
                m.scale(sc, sc, sc);
                m.scale(0.0625f, 0.0625f, 0.0625f); // пиксели модели -> блоки
                m.scale(side, 1f, 1f);               // зеркалим левое крыло
                m.multiply(CosmeticLayer.Y().rotation(-spread * side * side));
                m.multiply(CosmeticLayer.Z().rotation(0.15f + swing * 0.4f));
                // у призрачных и кристальных крыльев свечение включено всегда
                boolean wg = g || wi == 5 || wi == 6 || wi == 7;
                l.part(q, wingModels[wi], m, light, rgb(side > 0 ? 0f : 0.4f), wg);
                m.pop();
            }
        }

        // ---- головные уборы ----
        int hi = indexOf(head.get(), HEADS);
        if (hi > 0) {
            m.push();
            m.translate(0f, -0.5f, 0f);
            m.scale(sc, sc, sc);
            m.scale(0.0625f, 0.0625f, 0.0625f);
            m.multiply(CosmeticLayer.Y().rotationDegrees(s.relativeHeadYaw));
            m.multiply(CosmeticLayer.X().rotationDegrees(s.pitch));
            ModelPart p;
            boolean hg = g;
            switch (hi) {
                case 1 -> p = crown;
                case 2 -> {
                    m.translate(0f, -4f + (float) Math.sin(t * 2f) * 0.6f, 0f); // нимб парит над головой
                    m.multiply(CosmeticLayer.Y().rotation(t * 0.8f));
                    p = halo;
                    hg = true;
                }
                case 3 -> p = horns;
                case 4 -> p = hat;
                case 5 -> p = ears;
                case 6 -> {
                    m.multiply(CosmeticLayer.Z().rotation((float) Math.sin(t * 2.4f) * 0.05f)); // антенны покачиваются
                    p = antennae;
                    hg = true;
                }
                case 7 -> p = wreath;
                default -> p = samurai;
            }
            l.part(q, p, m, light, hi == 2 ? 0xFFE066 : rgb(0.2f), hg);
            m.pop();
        }

        // ---- хвост ----
        if (tail.get()) {
            m.push();
            m.translate(0f, 0.72f + sneak * 0.05f, 0.1f);
            m.scale(sc, sc, sc);
            m.scale(0.0625f, 0.0625f, 0.0625f);
            float wag = (float) Math.sin(t * 3.2f) * 0.35f + Math.min(1f, s.limbSwingAmplitude) * (float) Math.sin(t * 8f) * 0.15f;
            m.multiply(CosmeticLayer.Y().rotation(wag));
            m.multiply(CosmeticLayer.X().rotation(-0.55f - sneak * 0.2f));
            l.part(q, tailPart, m, light, rgb(0.8f), g);
            m.pop();
        }

        // ---- орбиты ----
        if (orbs.get()) {
            for (int i = 0; i < 3; i++) {
                double a = t * 1.5 + i * Math.PI * 2 / 3;
                m.push();
                m.translate((float) Math.cos(a) * 0.55f * sc, 0.35f + (float) Math.sin(t * 2 + i) * 0.08f, (float) Math.sin(a) * 0.55f * sc);
                m.scale(sc, sc, sc);
                m.scale(0.0625f, 0.0625f, 0.0625f);
                m.multiply(CosmeticLayer.Y().rotation(t * 2f + i));
                m.multiply(CosmeticLayer.Z().rotation(t * 1.3f + i));
                l.part(q, orb, m, light, rgb(i / 3f), true);
                m.pop();
            }
        }

        // ---- плащ ----
        if (cape.get() && s.equippedChestStack.isEmpty()) {
            m.push();
            m.translate(0f, 0f, 0.125f);
            m.scale(sc, sc, sc);
            m.scale(0.0625f, 0.0625f, 0.0625f);
            float lift = Math.min(1f, s.limbSwingAmplitude) * 0.35f + sneak * 0.2f + (float) Math.sin(t * 1.8f) * 0.03f;
            m.multiply(CosmeticLayer.X().rotation(0.08f + lift));
            l.part(q, capePart, m, light, rgb(0.5f), g);
            m.pop();
        }
    }

    private static int indexOf(String v, String... all) {
        for (int i = 0; i < all.length; i++) if (all[i].equals(v)) return i;
        return 0;
    }
}
