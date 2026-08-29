package zov.alphadlc.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import zov.alphadlc.AlphaDLC;
import zov.alphadlc.event.list.EventKeyInput;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.list.combat.KillAura;
import zov.alphadlc.module.settings.BindSetting;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.base.Instance;
import zov.alphadlc.util.math.BestPoint;
import zov.alphadlc.util.text.ValueUnit;

@ModuleInformation(moduleName = "ElytraTarget", moduleCategory = ModuleCategory.MOVEMENT)
public class ElytraTarget extends Module {

    public final BooleanSetting predictate = new BooleanSetting("Перегон", true);
    public final BindSetting predictateKey = new BindSetting("Бинд Перегона", -1).setVisible(() -> true);
    public final SliderSetting predictValue = new SliderSetting("Значение", 3, 1, 6, 0.1f).setVisible(() -> predictate.getValue());

    public final BooleanSetting elytraSlowdown = new BooleanSetting("Замедлять", true);
    public final BindSetting elytraSlowdownKey = new BindSetting("Бинд Замедления", -1).setVisible(() -> true);
    public final SliderSetting slowdownRadius = new SliderSetting("Радиус", ValueUnit.countable("блок", "блока", "блоков"), 1.9f, 1.0f, 6.0f, 0.1f).setVisible(() -> elytraSlowdown.getValue());
    public final SliderSetting minSpeed = new SliderSetting("Скорость", 0.2f, 0.1f, 1.0f, 0.05f).setVisible(() -> elytraSlowdown.getValue());

    public final BooleanSetting hitAfterOvertake = new BooleanSetting("Sloth bypa$", false);
    public final BindSetting hitAfterOvertakeKey = new BindSetting("Бинд Sloth bypa$", -1).setVisible(() -> true);

    @Subscribe
    private void onKey(EventKeyInput e) {
        if (mc.currentScreen != null) {
            return;
        }
        if (e.getAction() != 1) {
            return;
        }
        
        if (e.getKey() == predictateKey.getValue().intValue()) {
            predictate.toggle();
        }
        if (e.getKey() == elytraSlowdownKey.getValue().intValue()) {
            elytraSlowdown.toggle();
        }
        if (e.getKey() == hitAfterOvertakeKey.getValue().intValue()) {
            hitAfterOvertake.toggle();
        }
    }

    public boolean canAttack(LivingEntity target) {
        if (target == null || mc.player == null) {
            return false;
        }
        
        KillAura attackAura = Instance.get(KillAura.class);
        float attackDist = attackAura != null ? attackAura.distance.getFloatValue() : 4.0f;
        double distNearest = mc.player.getEyePos().distanceTo(BestPoint.getNearestPoint((Entity)target));
        return distNearest <= (double)attackDist;
    }

    private static ElytraTarget getElytraTarget() {
        ElytraTarget target = AlphaDLC.getInstance().getModuleStorage().get(ElytraTarget.class);
        return (target != null && target.isEnabled()) ? target : null;
    }

    public static class PredictateWrapper {
        public boolean getValue() {
            ElytraTarget target = getElytraTarget();
            return target != null && target.predictate.getValue();
        }
    }

    public static class PredictValueWrapper {
        public double getValue() {
            ElytraTarget target = getElytraTarget();
            return target != null ? target.predictValue.getValue() : 3.0;
        }

        public float getFloatValue() {
            return (float) getValue();
        }
    }

    public static class ElytraSlowdownWrapper {
        public boolean getValue() {
            ElytraTarget target = getElytraTarget();
            return target != null && target.elytraSlowdown.getValue();
        }
    }

    public static class SlowdownRadiusWrapper {
        public double getValue() {
            ElytraTarget target = getElytraTarget();
            return target != null ? target.slowdownRadius.getValue() : 3.0;
        }
    }

    public static class MinSpeedWrapper {
        public float getFloatValue() {
            ElytraTarget target = getElytraTarget();
            return target != null ? target.minSpeed.getFloatValue() : 0.3f;
        }
    }

    public static class HitAfterOvertakeWrapper {
        public boolean getValue() {
            ElytraTarget target = getElytraTarget();
            return target != null && target.hitAfterOvertake.getValue();
        }
    }

    public static class ShowPredictPointWrapper {
        public boolean getValue() {
            ElytraTarget target = getElytraTarget();
            return target != null;
        }
    }
}
