package zov.alphadlc.ui.component;

import lombok.Getter;
import lombok.Setter;
import zov.alphadlc.util.render.math.Animation;
import zov.alphadlc.util.render.math.Easing;

@Getter
@Setter
public abstract class Component implements IComponent {
    public float x, y, width, height;

    private final Animation alphaAnim = new Animation(Easing.BACK_OUT, 550);
    private final Animation alphaAnimSetting = new Animation(Easing.CUBIC_OUT, 280);
    private final Animation alphaAnimBack = new Animation(Easing.CUBIC_OUT, 280);

    // === НОВОЕ: Анимация сдвига по Y для эффекта "выпадания" настроек ===
    // Каждый компонент плавно появляется сверху вниз (slideY 0→1)
    private final Animation slideYAnim = new Animation(Easing.QUINTIC_OUT, 280);

    public boolean isVisible() {
        return true;
    }
}
