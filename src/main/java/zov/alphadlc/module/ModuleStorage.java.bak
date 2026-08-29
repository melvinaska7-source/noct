package zov.alphadlc.module;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import zov.alphadlc.AlphaDLC;
import zov.alphadlc.event.EventGameUpdate;
import zov.alphadlc.event.list.EventHUD;
import zov.alphadlc.event.list.EventKeyInput;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.list.combat.*;
import zov.alphadlc.module.list.misc.*;
import zov.alphadlc.module.list.movement.*;
import zov.alphadlc.module.list.player.*;
import zov.alphadlc.module.list.render.*;
import zov.alphadlc.module.list.render.hud.Interface;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ThemeSetting;
import zov.alphadlc.util.IMinecraft;
import zov.alphadlc.util.base.Instance;
import zov.alphadlc.util.party.connection.PartyApiClient;
import zov.alphadlc.util.player.other.SlownessManager;
import zov.alphadlc.util.rotation.Rotation;
import zov.alphadlc.util.rotation.RotationComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ModuleStorage implements IMinecraft {

    private final List<Module> modules = new ArrayList<>();

    public void injectRegisterModules() {
        modules.addAll(List.of(
                new FullBright(),
                new ClickGui(),
                new Sprint(),

                new NoRender(),
                new NameProtect(),
                new TargetESP(),
                new ElytraHelper(),
                new AutoTotem(),
                new ClickPearl(),
                new ClientSounds(),
                new NoFriendDamage(),

                new SwingAnimations(),
                new Predictions(),
                new AutoTpaccept(),
                new FireFly(),
                new CrystalOptimizer(),
                new GuiMove(),
                new ViewModel(),
                new GlassHands(),
                new ItemReplacer(),
                new KillEffect(),

                new AutoArmor(),
                new AutoTool(),
                new Ambience(),
                new BlockOverlay(),
                new FreeLook(),
                new Trails(),
                new FastExp(),
                new AutoSwap(),
                new AutoEat(),
                new AutoLeave(),

                new Wings(),
                new AutoPotion(),
                new Cape(),
                new ShulkerView(),
                new UseIndicator(),
                new Particles(),
                new Arrows(),
                new LineGlyphs(),
                new HitIndicator(),
                new LockSlot(),
                new HitBubbles(),

                new AntiAFK(),
                new ItemScroller(),
                new Interface()
        ));

        AlphaDLC.getInstance().getEventBus().register(this);
    }

    public <T extends Module> T get(final String name) {
        return this.modules.stream()
                .filter(module -> module.getName().equalsIgnoreCase(name))
                .map(module -> (T) module)
                .findFirst()
                .orElse(null);
    }

    public <T extends Module> T get(final Class<T> clazz) {
        return this.modules.stream()
                .filter(module -> clazz.isAssignableFrom(module.getClass()))
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }

    public List<Module> get(final ModuleCategory category) {
        return this.modules.stream()
                .filter(module -> module.getCategory() == category)
                .collect(Collectors.toList());
    }

    @Setter
    private float speedAcceleration;

    @Setter
    private float randomness;

    @Subscribe
    private void onGameUpdate(EventGameUpdate e) {
        if (mc.player == null) return;

        if (!SlownessManager.slowTasksIsEmpty()) {
            SlownessManager.updateSlowTasks();
        }

        if (!SlownessManager.timeTasksIsEmpty()) {
            SlownessManager.updateTimeTasks(false);
        }

        var aura = get(KillAura.class);

        if (!aura.isEnabled() || aura.getTarget() == null) {
            if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
                aura.lastYaw = (mc.gameRenderer.getCamera().getYaw() - 180);
                aura.lastPitch = -mc.gameRenderer.getCamera().getPitch();
            } else {
                aura.lastYaw = mc.gameRenderer.getCamera().getYaw();
                aura.lastPitch = mc.gameRenderer.getCamera().getPitch();
            }

            if (aura.rotation.is("Vanilla") || Instance.get(FreeLook.class).isActive()) return;

            updateBackwardsOther();
        }
    }

    @Subscribe
    private void onRender(EventHUD ignored) {
        for (var module : getModules()) {
            module.getAnimation().run(module.isEnabled());

            for (var setting : module.getSettings()) {
                if (setting instanceof BooleanSetting b) {
                    b.getAnimation().run(b.getValue());
                }

                if (setting instanceof ThemeSetting t) {
                    t.getValue().animation.run(1);
                }
            }
        }
    }

    @Subscribe
    private void onKey(EventKeyInput e) {
        if (e.getAction() == 0) return;

        for (var module : getModules()) {
            module.getSettings()
                    .stream()
                    .filter(setting ->
                            setting instanceof BooleanSetting b &&
                            e.getKey() == b.getKey()
                    )
                    .forEach(setting ->
                            ((BooleanSetting) setting).toggle()
                    );
        }
    }

    @Subscribe
    private void onUpdate(EventTick ignored) {
        if (!SlownessManager.timeTasksIsEmpty()) {
            SlownessManager.updateTimeTasks(true);
        }

        if (mc.player == null || true) return;

        PartyApiClient.fetchPartyStateAsync();
        PartyApiClient.fetchInvitesAsync();

        JsonObject j = new JsonObject();
        j.addProperty("player", mc.player.getNameForScoreboard());
        j.addProperty("x", mc.player.getX());
        j.addProperty("y", mc.player.getY());
        j.addProperty("z", mc.player.getZ());

        PartyApiClient.postAsync("/party/pos", j, json -> {});
    }

    private void updateBackwardsOther() {
        if (mc.player.isGliding()) {
            speedAcceleration += 0.06f;
        } else {
            speedAcceleration += 0.006f;
        }

        get(KillAura.class).speedAcceleration = 0;

        var angle = new Rotation(
                mc.gameRenderer.getCamera().getYaw(),
                mc.gameRenderer.getCamera().getPitch()
        );

        var deltaYaw = MathHelper.wrapDegrees(
                angle.getYaw() - mc.player.getYaw()
        );

        var deltaPitch = angle.getPitch() - mc.player.getPitch();

        if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            deltaYaw = MathHelper.wrapDegrees(
                    (angle.getYaw() - 180) - mc.player.getYaw()
            );

            deltaPitch = MathHelper.wrapDegrees(
                    -angle.getPitch() - mc.player.getPitch()
            );
        }

        var smooth = Math.max(speedAcceleration, 0);

        var newYaw = mc.player.getYaw()
                + deltaYaw * (Math.min(Math.max(smooth, 0), 1));

        var newPitch = mc.player.getPitch()
                + deltaPitch * (Math.min(Math.max(smooth / 2, 0), 1));

        var smoothRot = new Rotation(newYaw, newPitch);

        RotationComponent.update(
                smoothRot,
                360,
                360,
                360,
                360,
                0,
                2,
                false
        );
    }
}