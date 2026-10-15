package com.collapse.settings;

/** Настройки самого клиента (меню и HUD), сохраняются в конфиге. */
public final class ClientSettings {
    private ClientSettings() {}

    public static float menuScale = 1.0f;       // масштаб окна меню
    public static float animSpeed = 1.0f;       // скорость анимаций
    public static boolean dimBackground = true; // затемнение фона
    public static boolean blurLike = true;      // лёгкая виньетка под меню
    public static boolean showHints = true;     // подсказки внизу меню
    public static boolean arrayList = true;     // список модулей справа
    public static boolean arrayListBg = true;   // подложка у списка
    public static int arrayListPos = 0;         // 0 справа сверху, 1 слева сверху, 2 справа снизу
    public static boolean animatedTheme = true; // переливающийся акцент
    public static boolean closeOnEsc = true;
}
