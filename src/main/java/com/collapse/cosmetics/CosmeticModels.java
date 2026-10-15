package com.collapse.cosmetics;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

/**
 * Процедурные модели косметики из кубов. Текстура: атлас 64x64 (белая, красится тинтом).
 * Все координаты в "пикселях" модели, ось Y вниз (как в ванильных моделях).
 */
public final class CosmeticModels {
    private CosmeticModels() {}

    public static final int TEX = 64;

    private static ModelPartBuilder box(ModelPartBuilder b, float x, float y, float z, float w, float h, float d) {
        return b.cuboid(x, y, z, w, h, d, Dilation.NONE);
    }

    private static ModelPart build(ModelData data) {
        return TexturedModelData.of(data, TEX, TEX).createModel();
    }

    // ---------------- КРЫЛЬЯ ----------------
    /** Одно крыло (правое, зеркалится в слое). style: 0 ангел, 1 демон, 2 бабочка, 3 дракон. */
    public static ModelPart wing(int style) {
        ModelData md = new ModelData();
        ModelPartData root = md.getRoot();
        ModelPartData base = root.addChild("wing", ModelPartBuilder.create(), ModelTransform.origin(0, 0, 0));
        switch (style) {
            default -> { // ангел: веер из перьев, каждое длиннее и ниже предыдущего
                for (int i = 0; i < 6; i++) {
                    float len = 12 + i * 3.2f;
                    float rotZ = -0.25f - i * 0.2f;
                    base.addChild("f" + i,
                            box(ModelPartBuilder.create().uv(0, 0), 0, -1.2f, -0.5f, len, 2.4f, 1f),
                            ModelTransform.of(0, i * 1.1f - 2f, 0, 0, -0.08f * i, rotZ));
                }
            }
            case 1 -> { // демон: костяные рёбра + мембрана из тонких пластин
                ModelPartData arm = base.addChild("arm",
                        box(ModelPartBuilder.create().uv(0, 8), 0, -1f, -1f, 18, 2f, 2f),
                        ModelTransform.of(0, 0, 0, 0, 0, -0.55f));
                for (int i = 0; i < 4; i++) {
                    float len = 20 - i * 2.5f;
                    base.addChild("r" + i,
                            box(ModelPartBuilder.create().uv(0, 16), 0, -0.6f, -0.6f, len, 1.2f, 1.2f),
                            ModelTransform.of(8 + i * 1.4f, -6 + i * 0.6f, 0, 0, -0.05f * i, -0.2f - i * 0.38f));
                    base.addChild("m" + i,
                            box(ModelPartBuilder.create().uv(0, 24), 0, -0.2f, -0.2f, len * 0.9f, 7f, 0.4f),
                            ModelTransform.of(9 + i * 1.4f, -6.5f + i * 0.6f, 0, 0, -0.05f * i, -0.2f - i * 0.38f));
                }
            }
            case 2 -> { // бабочка: две пары крупных пластин
                base.addChild("up", box(ModelPartBuilder.create().uv(0, 32), 0, -14, -0.3f, 16, 14, 0.6f),
                        ModelTransform.of(0, 0, 0, 0, -0.35f, 0.1f));
                base.addChild("low", box(ModelPartBuilder.create().uv(0, 32), 0, 0, -0.3f, 11, 12, 0.6f),
                        ModelTransform.of(0, 1, 0, 0, -0.3f, -0.15f));
                base.addChild("tip", box(ModelPartBuilder.create().uv(0, 40), 10, -16, -0.3f, 6, 5, 0.6f),
                        ModelTransform.of(0, 0, 0, 0, -0.35f, 0.1f));
            }
            case 3 -> { // дракон: толстые кости и широкая перепонка
                base.addChild("bone", box(ModelPartBuilder.create().uv(0, 8), 0, -1.5f, -1.5f, 22, 3, 3),
                        ModelTransform.of(0, 0, 0, 0, 0, -0.6f));
                for (int i = 0; i < 3; i++) {
                    base.addChild("fin" + i, box(ModelPartBuilder.create().uv(0, 16), 0, -0.8f, -0.8f, 24 - i * 3, 1.6f, 1.6f),
                            ModelTransform.of(10, -9 + i * 1.5f, 0, 0, -0.06f * i, -0.15f - i * 0.45f));
                    base.addChild("sail" + i, box(ModelPartBuilder.create().uv(0, 24), 0, -0.2f, -0.2f, 22 - i * 3, 9, 0.4f),
                            ModelTransform.of(11, -9.5f + i * 1.5f, 0, 0, -0.06f * i, -0.15f - i * 0.45f));
                }
            }
        }
        return build(md).getChild("wing");
    }

    // ---------------- ГОЛОВНЫЕ УБОРЫ ----------------
    /** Корона: основание и зубцы. */
    public static ModelPart crown() {
        ModelData md = new ModelData();
        ModelPartData r = md.getRoot().addChild("crown", ModelPartBuilder.create(), ModelTransform.origin(0, 0, 0));
        r.addChild("band", box(ModelPartBuilder.create().uv(0, 48), -4.5f, -2.5f, -4.5f, 9, 2.5f, 9), ModelTransform.origin(0, 0, 0));
        float[][] spikes = {{-4, -4}, {0, -4}, {4, -4}, {-4, 0}, {4, 0}, {-4, 4}, {0, 4}, {4, 4}};
        int i = 0;
        for (float[] s : spikes) {
            float h = (i % 2 == 0) ? 4f : 2.6f;
            r.addChild("s" + i, box(ModelPartBuilder.create().uv(0, 52), s[0] - 0.7f, -2.5f - h, s[1] - 0.7f, 1.4f, h, 1.4f), ModelTransform.origin(0, 0, 0));
            r.addChild("g" + i, box(ModelPartBuilder.create().uv(8, 56), s[0] - 1f, -2.5f - h - 1.2f, s[1] - 1f, 2f, 1.2f, 2f), ModelTransform.origin(0, 0, 0));
            i++;
        }
        return build(md).getChild("crown");
    }

    /** Нимб: плоское кольцо из 16 брусков, лежащее над головой. */
    public static ModelPart halo() {
        ModelData md = new ModelData();
        ModelPartData r = md.getRoot().addChild("halo", ModelPartBuilder.create(), ModelTransform.origin(0, 0, 0));
        int n = 16;
        float rad = 5.2f;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            float x = (float) Math.cos(a) * rad, z = (float) Math.sin(a) * rad;
            r.addChild("h" + i, box(ModelPartBuilder.create().uv(0, 56), -1.3f, -0.4f, -0.5f, 2.6f, 0.8f, 1f),
                    ModelTransform.of(x, 0, z, 0, (float) (-a + Math.PI / 2), 0));
        }
        return build(md).getChild("halo");
    }

    /** Рога. */
    public static ModelPart horns() {
        ModelData md = new ModelData();
        ModelPartData r = md.getRoot().addChild("horns", ModelPartBuilder.create(), ModelTransform.origin(0, 0, 0));
        for (int side = -1; side <= 1; side += 2) {
            r.addChild("a" + side, box(ModelPartBuilder.create().uv(0, 52), -1, -4, -1, 2, 4, 2), ModelTransform.of(side * 3.2f, -1, 0, 0, 0, side * 0.35f));
            r.addChild("b" + side, box(ModelPartBuilder.create().uv(0, 52), -0.8f, -4, -0.8f, 1.6f, 4, 1.6f), ModelTransform.of(side * 4.5f, -4.5f, 0, 0, 0, side * 0.95f));
            r.addChild("c" + side, box(ModelPartBuilder.create().uv(0, 52), -0.6f, -3, -0.6f, 1.2f, 3, 1.2f), ModelTransform.of(side * 6.3f, -7.2f, 0, 0, 0, side * 1.45f));
        }
        return build(md).getChild("horns");
    }

    /** Шляпа волшебника (конус из ступеней). */
    public static ModelPart wizardHat() {
        ModelData md = new ModelData();
        ModelPartData r = md.getRoot().addChild("hat", ModelPartBuilder.create(), ModelTransform.origin(0, 0, 0));
        r.addChild("brim", box(ModelPartBuilder.create().uv(0, 48), -7, -1, -7, 14, 1, 14), ModelTransform.origin(0, 0, 0));
        float[] w = {8.5f, 7f, 5.5f, 4f, 2.6f, 1.4f};
        float y = -1;
        for (int i = 0; i < w.length; i++) {
            r.addChild("t" + i, box(ModelPartBuilder.create().uv(0, 52), -w[i] / 2, -2.6f, -w[i] / 2, w[i], 2.6f, w[i]),
                    ModelTransform.of(i * 0.35f, y, 0, 0, 0, i * 0.08f));
            y -= 2.5f;
        }
        return build(md).getChild("hat");
    }

    // ---------------- ПЛАЩ ----------------
    /** Плащ: большая плоскость, поворачивается по скорости. */
    public static ModelPart cape() {
        ModelData md = new ModelData();
        ModelPartData r = md.getRoot().addChild("cape", ModelPartBuilder.create(), ModelTransform.origin(0, 0, 0));
        r.addChild("body", box(ModelPartBuilder.create().uv(0, 0), -5, 0, 0, 10, 16, 1), ModelTransform.origin(0, 0, 0));
        r.addChild("trim", box(ModelPartBuilder.create().uv(0, 48), -5, 15, -0.1f, 10, 1.4f, 1.2f), ModelTransform.origin(0, 0, 0));
        r.addChild("collar", box(ModelPartBuilder.create().uv(0, 48), -5.5f, -1, -0.5f, 11, 2, 2), ModelTransform.origin(0, 0, 0));
        return build(md).getChild("cape");
    }
}
