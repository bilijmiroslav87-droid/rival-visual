package com.collapse.gui;

import com.collapse.CollapseClient;
import com.collapse.Module;
import com.collapse.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class ClickMenuScreen extends Screen {
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 22;
    private static final int PADDING = 8;
    private static final int TOP_OFFSET = 45;

    private ClickMenuScreen() {
        super(Text.literal("Collapse Client"));
    }

    public static ClickMenuScreen create() {
        return new ClickMenuScreen();
    }

    @Override
    protected void init() {
        try {
            List<Module> modules = ModuleManager.MODULES;
            int usableHeight = this.height - TOP_OFFSET - PADDING;
            int perColumn = Math.max(1, usableHeight / (BUTTON_HEIGHT + PADDING));
            int columns = Math.max(1, (this.width - PADDING * 2) / (BUTTON_WIDTH + PADDING));

            for (int i = 0; i < modules.size(); i++) {
                Module module = modules.get(i);
                int col = i / perColumn;
                int row = i % perColumn;
                if (col >= columns) break;
                int x = PADDING + col * (BUTTON_WIDTH + PADDING);
                int y = TOP_OFFSET + row * (BUTTON_HEIGHT + PADDING);
                addDrawableChild(ButtonWidget.builder(buttonText(module), b -> {
                    module.toggle();
                    b.setMessage(buttonText(module));
                }).dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
            }
        } catch (Throwable t) {
            CollapseClient.LOGGER.error("Menu init error", t);
        }
    }

    private static Text buttonText(Module module) {
        String state = module.isEnabled() ? "§aON" : "§cOFF";
        return Text.literal(module.getName() + " " + state);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Без renderBackground: его blur-эффект вылетает на мобильных рендерах FCL.
        // Обычная полупрозрачная заливка работает везде.
        try {
            context.fill(0, 0, this.width, this.height, 0x90101014);
            context.fill(0, 0, this.width, 34, 0xFF1A1A2E);
            context.fill(0, 34, this.width, 35, 0xFF4A9EFF);
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("§bCollapse§f Client"), PADDING, 13, 0xFFFFFF);
            String hint = ModuleManager.MODULES.size() + " модулей · ESC закрыть";
            context.drawTextWithShadow(this.textRenderer, Text.literal("§7" + hint),
                    this.width - this.textRenderer.getWidth(hint) - PADDING, 13, 0xFFFFFF);
        } catch (Throwable t) {
            CollapseClient.LOGGER.error("Menu render error", t);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
