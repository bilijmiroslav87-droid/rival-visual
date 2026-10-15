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
            case 4 -> { // феникс: длинные изогнутые перья с «огненными» кончиками
                for (int i = 0; i < 8; i++) {
                    float len = 10 + i * 3.4f;
                    base.addChild("p" + i, box(ModelPartBuilder.create().uv(0, 0), 0, -1.4f, -0.5f, len, 2.8f, 1f),
                            ModelTransform.of(0, i * 1.2f - 4f, 0, 0, -0.07f * i, -0.15f - i * 0.17f));
                    base.addChild("pt" + i, box(ModelPartBuilder.create().uv(8, 56), len - 0.5f, -1.0f, -0.4f, 3.2f, 2.0f, 0.8f),
                            ModelTransform.of(0, i * 1.2f - 4f, 0, 0, -0.07f * i, -0.15f - i * 0.17f));
                }
            }
            case 5 -> { // кристальные: острые грани-«осколки»
                for (int i = 0; i < 7; i++) {
                    float len = 8 + (i % 4) * 4.5f + i * 0.8f;
                    float w = 2.4f + (i % 3) * 0.8f;
                    base.addChild("c" + i, box(ModelPartBuilder.create().uv(0, 16), 0, -w / 2, -0.7f, len, w, 1.4f),
                            ModelTransform.of(0, -5 + i * 1.7f, 0, 0.05f * (i % 3), -0.06f * i, -0.3f - i * 0.22f));
                    base.addChild("ct" + i, box(ModelPartBuilder.create().uv(8, 56), len, -w / 4, -0.4f, 3f, w / 2, 0.8f),
                            ModelTransform.of(0, -5 + i * 1.7f, 0, 0.05f * (i % 3), -0.06f * i, -0.3f - i * 0.22f));
                }
            }
            case 6 -> { // призрачные: рваные, вытянутые вниз лоскуты
                for (int i = 0; i < 6; i++) {
                    float len = 14 + (i % 2) * 6 + i * 1.5f;
                    base.addChild("g" + i, box(ModelPartBuilder.create().uv(0, 24), 0, -0.3f, -0.3f, len * 0.6f, len, 0.5f),
                            ModelTransform.of(1 + i * 1.8f, -6, 0, 0, -0.05f * i, -0.1f - i * 0.1f));
                }
                base.addChild("gb", box(ModelPartBuilder.create().uv(0, 8), 0, -1f, -1f, 14, 2f, 2f),
                        ModelTransform.of(0, -5, 0, 0, 0, -0.35f));
            }
            case 7 -> { // кибер: прямые панели-«лезвия» с неоновыми кромками
                for (int i = 0; i < 5; i++) {
                    float len = 12 + i * 3.6f;
                    base.addChild("k" + i, box(ModelPartBuilder.create().uv(0, 16), 0, -1.6f, -0.6f, len, 3.2f, 1.2f),
                            ModelTransform.of(0, -6 + i * 3.2f, 0, 0, -0.04f * i, -0.35f - i * 0.1f));
                    base.addChild("kl" + i, box(ModelPartBuilder.create().uv(8, 56), 0, -0.4f, -0.8f, len, 0.8f, 1.6f),
                            ModelTransform.of(0, -6 + i * 3.2f, 0, 0, -0.04f * i, -0.35f - i * 0.1f));
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

    /** Кошачьи уши. */
    public static ModelPart catEars() {
        ModelData md = new ModelData();
        ModelPartData r = md.getRoot().addChild("ears", ModelPartBuilder.create(), ModelTransform.origin(0, 0, 0));
        for (int side = -1; side <= 1; side += 2) {
            r.addChild("e" + side, box(ModelPartBuilder.create().uv(0, 52), -1.8f, -4.2f, -0.8f, 3.6f, 4.2f, 1.6f), ModelTransform.of(side * 3f, -0.5f, -1.2f, -0.1f, 0, side * 0.28f));
            r.addChild("t" + side, box(ModelPartBuilder.create().uv(8, 56), -0.9f, -5.6f, -0.5f, 1.8f, 1.8f, 1.0f), ModelTransform.of(side * 3f, -0.5f, -1.2f, -0.1f, 0, side * 0.28f));
        }
        return build(md).getChild("ears");
    }

    /** Антенны с шариками. */
    public static ModelPart antennae() {
        ModelData md = new ModelData();
        ModelPartData r = md.getRoot().addChild("ant", ModelPartBuilder.create(), ModelTransform.origin(0, 0, 0));
        for (int side = -1; side <= 1; side += 2) {
            r.addChild("a" + side, box(ModelPartBuilder.create().uv(0, 52), -0.4f, -7f, -0.4f, 0.8f, 7f, 0.8f), ModelTransform.of(side * 2.2f, -0.5f, -1f, 0, 0, side * 0.32f));
            r.addChild("b" + side, box(ModelPartBuilder.create().uv(8, 56), -1.2f, -9f, -1.2f, 2.4f, 2.4f, 2.4f), ModelTransform.of(side * 2.2f, -0.5f, -1f, 0, 0, side * 0.32f));
        }
        return build(md).getChild("ant");
    }

    /** Венок: кольцо из листьев. */
    public static ModelPart wreath() {
        ModelData md = new ModelData();
        ModelPartData r = md.getRoot().addChild("wreath", ModelPartBuilder.create(), ModelTransform.origin(0, 0, 0));
        int n = 20;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            float rad = 5.0f + (i % 2) * 0.35f;
            float x = (float) Math.cos(a) * rad, z = (float) Math.sin(a) * rad;
            r.addChild("w" + i, box(ModelPartBuilder.create().uv(0, 56), -1.3f, -1.1f - (i % 3) * 0.4f, -0.6f, 2.6f, 1.6f + (i % 3) * 0.4f, 1.2f),
                    ModelTransform.of(x, 0.4f, z, 0, (float) (-a + Math.PI / 2), (i % 2 == 0 ? 0.25f : -0.25f)));
        }
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2 / 5;
            r.addChild("f" + i, box(ModelPartBuilder.create().uv(8, 56), -0.8f, -2.2f, -0.8f, 1.6f, 1.6f, 1.6f),
                    ModelTransform.origin((float) Math.cos(a) * 5.2f, 0, (float) Math.sin(a) * 5.2f));
        }
        return build(md).getChild("wreath");
    }

    /** Самурайский шлем: купол, козырёк, гребень-месяц. */
    public static ModelPart samurai() {
        ModelData md = new ModelData();
        ModelPartData r = md.getRoot().addChild("samurai", ModelPartBuilder.create(), ModelTransform.origin(0, 0, 0));
        r.addChild("dome", box(ModelPartBuilder.create().uv(0, 48), -4.8f, -5f, -4.8f, 9.6f, 5f, 9.6f), ModelTransform.origin(0, 0.3f, 0));
        r.addChild("brim", box(ModelPartBuilder.create().uv(0, 52), -5.6f, -0.8f, -5.6f, 11.2f, 1.2f, 11.2f), ModelTransform.origin(0, 0.3f, 0));
        r.addChild("band", box(ModelPartBuilder.create().uv(8, 56), -5f, -2f, -5f, 10f, 1f, 10f), ModelTransform.origin(0, 0.3f, 0));
        for (int side = -1; side <= 1; side += 2) {
            r.addChild("h" + side, box(ModelPartBuilder.create().uv(8, 56), -0.6f, -6f, -0.6f, 1.2f, 6f, 1.2f), ModelTransform.of(side * 1.6f, -4.8f, -4.6f, -0.2f, 0, side * 0.7f));
        }
        r.addChild("gem", box(ModelPartBuilder.create().uv(8, 56), -1.2f, -3.4f, -5.4f, 2.4f, 2.4f, 1.0f), ModelTransform.origin(0, 0.3f, 0));
        return build(md).getChild("samurai");
    }

    /** Хвост: цепочка сегментов, крепится на пояснице. */
    public static ModelPart tail() {
        ModelData md = new ModelData();
        ModelPartData r = md.getRoot().addChild("tail", ModelPartBuilder.create(), ModelTransform.origin(0, 0, 0));
        ModelPartData cur = r;
        for (int i = 0; i < 6; i++) {
            float w = 2.8f - i * 0.28f;
            cur = cur.addChild("t" + i, box(ModelPartBuilder.create().uv(0, 52), -w / 2, -w / 2, 0, w, w, 3.2f), ModelTransform.of(0, 0, i == 0 ? 0 : 3f, 0.14f, 0, 0));
        }
        return build(md).getChild("tail");
    }

    /** Ореол-орбиты: три шарика-спутника (позиция задаётся в Cosmetics.draw). */
    public static ModelPart orb() {
        ModelData md = new ModelData();
        ModelPartData r = md.getRoot().addChild("orb", ModelPartBuilder.create(), ModelTransform.origin(0, 0, 0));
        r.addChild("o", box(ModelPartBuilder.create().uv(8, 56), -1.5f, -1.5f, -1.5f, 3f, 3f, 3f), ModelTransform.origin(0, 0, 0));
        r.addChild("oc", box(ModelPartBuilder.create().uv(0, 56), -0.8f, -2.4f, -0.8f, 1.6f, 4.8f, 1.6f), ModelTransform.origin(0, 0, 0));
        return build(md).getChild("orb");
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
