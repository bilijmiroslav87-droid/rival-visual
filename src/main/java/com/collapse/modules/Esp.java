package com.collapse.modules;

import com.collapse.Module;
import com.collapse.render.Draw;
import com.collapse.render.Fonts;
import com.collapse.render.Projector;
import com.collapse.settings.BoolSetting;
import com.collapse.settings.ModeSetting;
import com.collapse.settings.NumSetting;
import com.collapse.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;

public class Esp extends Module {
    private final ModeSetting style = add(new ModeSetting("Style", "Corners", "Box", "Corners"));
    private final NumSetting range = add(new NumSetting("Range", 80, 10, 200, 5));
    private final BoolSetting players = add(new BoolSetting("Players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", false));
    private final BoolSetting health = add(new BoolSetting("Health bar", true));
    private final BoolSetting names = add(new BoolSetting("Names", true));
    private final BoolSetting self = add(new BoolSetting("Self (3rd person)", false));

    public Esp() {
        super("ESP", "Рамки вокруг игроков и мобов", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext ctx, int w, int h) {
        if (mc.world == null || mc.player == null) return;
        float td = mc.getRenderTickCounter().getTickProgress(false);
        double r2 = range.get() * range.get();
        int col = Theme.accent();

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity l) || e instanceof ArmorStandEntity || !l.isAlive()) continue;
            if (l == mc.player && !(self.get() && !mc.options.getPerspective().isFirstPerson())) continue;
            if (l.squaredDistanceTo(mc.player) > r2) continue;
            boolean isPlayer = l instanceof PlayerEntity;
            if (isPlayer ? !players.get() : !mobs.get()) continue;

            float[] b = Projector.entityBox(l, td);
            if (b == null) continue;
            float x1 = b[0], y1 = b[1], x2 = b[2], y2 = b[3];
            if (x2 < -50 || x1 > w + 50 || y2 < -50 || y1 > h + 50) continue;
            if (y2 - y1 < 3) continue;

            int c = com.collapse.combat.Friends.isFriend(l.getName().getString()) ? 0xFF3DDC84 : col;
            if (style.is("Box")) {
                outline(ctx, x1, y1, x2, y2, 0xFF000000, 2);
                outline(ctx, x1, y1, x2, y2, c, 1);
            } else {
                corners(ctx, x1, y1, x2, y2, c);
            }

            if (health.get()) {
                float hp = Math.max(0, Math.min(1, l.getHealth() / l.getMaxHealth()));
                float bh = (y2 - y1) * hp;
                int hc = Theme.mix(0xFFFF3B3B, 0xFF3DDC84, hp);
                ctx.fill((int) x1 - 5, (int) y1 - 1, (int) x1 - 2, (int) y2 + 1, 0xA0000000);
                ctx.fill((int) x1 - 4, (int) (y2 - bh), (int) x1 - 3, (int) y2, hc);
            }
            if (names.get()) {
                String n = l.getName().getString();
                float size = 9f;
                float tw = Fonts.MEDIUM.width(n, size);
                float cx = (x1 + x2) / 2f;
                Draw.roundRect(ctx, cx - tw / 2 - 3, y1 - 13, tw + 6, 11, 3, 0xB0101014);
                Fonts.MEDIUM.draw(ctx, n, cx - tw / 2, y1 - 12, size, 0xFFFFFFFF);
            }
        }
    }

    private static void outline(DrawContext c, float x1, float y1, float x2, float y2, int col, int t) {
        int a = (int) x1, b = (int) y1, d = (int) x2, e = (int) y2;
        c.fill(a, b, d, b + t, col);
        c.fill(a, e - t, d, e, col);
        c.fill(a, b, a + t, e, col);
        c.fill(d - t, b, d, e, col);
    }

    private static void corners(DrawContext c, float x1, float y1, float x2, float y2, int col) {
        int a = (int) x1, b = (int) y1, d = (int) x2, e = (int) y2;
        int lx = Math.max(3, (d - a) / 4), ly = Math.max(3, (e - b) / 4);
        int[] k = {0xFF000000, col};
        for (int p = 0; p < 2; p++) {
            int o = p == 0 ? 1 : 0, t = p == 0 ? 3 : 1;
            int cc = k[p];
            c.fill(a - o, b - o, a + lx, b - o + t, cc);
            c.fill(a - o, b - o, a - o + t, b + ly, cc);
            c.fill(d - lx, b - o, d + o, b - o + t, cc);
            c.fill(d + o - t, b - o, d + o, b + ly, cc);
            c.fill(a - o, e + o - t, a + lx, e + o, cc);
            c.fill(a - o, e - ly, a - o + t, e + o, cc);
            c.fill(d - lx, e + o - t, d + o, e + o, cc);
            c.fill(d + o - t, e - ly, d + o, e + o, cc);
        }
    }
}
