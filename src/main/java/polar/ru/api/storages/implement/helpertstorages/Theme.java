package polar.ru.api.storages.implement.helpertstorages;

import polar.ru.api.QClient;
import polar.ru.api.utils.color.ColorUtils;

public class Theme
implements QClient {
    private String name;
    public int[] color;

    public Theme(String name, int ... color) {
        this.name = name;
        this.color = color;
    }

    public int getColor(int index) {
        if (this.name.equals("Rainbow")) {
            return ColorUtils.rainbow(10, index, 0.6f, 1.0f, 1.0f);
        }
        return ColorUtils.gradient(5, index, this.color);
    }
    public String getName() {
        return this.name;
    }
    public int[] getColor() {
        return this.color;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setColor(int[] color) {
        this.color = color;
    }
}

