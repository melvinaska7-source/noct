package polar.ru.api.storages.implement;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Arrays;
import polar.ru.api.storages.implement.helpertstorages.Theme;
import polar.ru.api.utils.color.ColorUtils;

public class ThemeStorage {
    private ObjectArrayList<Themes> themeList = new ObjectArrayList();
    private Themes themes;

    public ThemeStorage() {
        this.onInitialize();
    }

    private void onInitialize() {
        this.themeList.addAll(Arrays.asList(Themes.Custom, Themes.Purple, Themes.Red, Themes.Blue, Themes.Green, Themes.Pink, Themes.Orange, Themes.Blues, Themes.Yellows, Themes.Cyan, Themes.Lime, Themes.Magenta, Themes.Coral, Themes.Teal, Themes.Violet, Themes.Amber, Themes.Mint, Themes.Crimson, Themes.Aqua, Themes.Peach));
        this.themes = (Themes)((Object)this.themeList.get(1));
    }
    public ObjectArrayList<Themes> getThemeList() {
        return this.themeList;
    }
    public Themes getThemes() {
        return this.themes;
    }
    public void setThemeList(ObjectArrayList<Themes> themeList) {
        this.themeList = themeList;
    }
    public void setThemes(Themes themes) {
        this.themes = themes;
    }

    public static enum Themes {
        Custom(new Theme("Rainbow", ColorUtils.rgba(255, 255, 255, 0))),
        Purple(new Theme("Lavender", ColorUtils.rgba(190, 143, 255, 255), ColorUtils.darken(ColorUtils.rgba(190, 143, 255, 255), 0.35f))),
        Red(new Theme("Blood", ColorUtils.rgba(230, 50, 57, 255), ColorUtils.darken(ColorUtils.rgba(230, 50, 57, 255), 0.35f))),
        Blue(new Theme("Ocean", ColorUtils.rgba(95, 113, 191, 255), ColorUtils.darken(ColorUtils.rgba(95, 113, 191, 255), 0.35f))),
        Green(new Theme("Emerald", ColorUtils.rgba(60, 220, 140, 255), ColorUtils.darken(ColorUtils.rgba(60, 220, 140, 255), 0.35f))),
        Pink(new Theme("Rose", ColorUtils.rgba(255, 120, 190, 255), ColorUtils.darken(ColorUtils.rgba(255, 120, 190, 255), 0.35f))),
        Orange(new Theme("Gold", ColorUtils.rgba(252, 192, 88, 255), ColorUtils.darken(ColorUtils.rgba(252, 192, 88, 255), 0.35f))),
        Blues(new Theme("Diamond", ColorUtils.rgba(125, 217, 250, 255), ColorUtils.darken(ColorUtils.rgba(125, 217, 250, 255), 0.35f))),
        Yellows(new Theme("Sun", ColorUtils.rgba(252, 231, 88, 255), ColorUtils.darken(ColorUtils.rgba(252, 231, 88, 255), 0.35f))),
        Cyan(new Theme("Glacier", ColorUtils.rgba(0, 230, 230, 255), ColorUtils.darken(ColorUtils.rgba(0, 230, 230, 255), 0.35f))),
        Lime(new Theme("Lime", ColorUtils.rgba(180, 255, 70, 255), ColorUtils.darken(ColorUtils.rgba(180, 255, 70, 255), 0.35f))),
        Magenta(new Theme("Magenta", ColorUtils.rgba(235, 52, 192, 255), ColorUtils.darken(ColorUtils.rgba(235, 52, 192, 255), 0.35f))),
        Coral(new Theme("Coral", ColorUtils.rgba(255, 127, 80, 255), ColorUtils.darken(ColorUtils.rgba(255, 127, 80, 255), 0.35f))),
        Teal(new Theme("Teal", ColorUtils.rgba(0, 180, 150, 255), ColorUtils.darken(ColorUtils.rgba(0, 180, 150, 255), 0.35f))),
        Violet(new Theme("Violet", ColorUtils.rgba(148, 0, 211, 255), ColorUtils.darken(ColorUtils.rgba(148, 0, 211, 255), 0.35f))),
        Amber(new Theme("Amber", ColorUtils.rgba(255, 191, 0, 255), ColorUtils.darken(ColorUtils.rgba(255, 191, 0, 255), 0.35f))),
        Mint(new Theme("Mint", ColorUtils.rgba(152, 255, 152, 255), ColorUtils.darken(ColorUtils.rgba(152, 255, 152, 255), 0.35f))),
        Crimson(new Theme("Crimson", ColorUtils.rgba(220, 20, 60, 255), ColorUtils.darken(ColorUtils.rgba(220, 20, 60, 255), 0.35f))),
        Aqua(new Theme("Aqua", ColorUtils.rgba(64, 224, 208, 255), ColorUtils.darken(ColorUtils.rgba(64, 224, 208, 255), 0.35f))),
        Peach(new Theme("Peach", ColorUtils.rgba(255, 218, 185, 255), ColorUtils.darken(ColorUtils.rgba(255, 218, 185, 255), 0.35f)));

        final Theme theme;
        private Themes(Theme theme) {
            this.theme = theme;
        }
        public Theme getTheme() {
            return this.theme;
        }
    }
}

