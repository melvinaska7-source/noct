package zov.alphadlc;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import zov.alphadlc.event.list.EventKeyInput;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleStorage;
import zov.alphadlc.util.commands.CommandDispatcher;
import zov.alphadlc.util.commands.manager.CommandRepository;
import zov.alphadlc.util.config.ConfigManager;
import zov.alphadlc.util.draggable.DragManager;
import zov.alphadlc.util.friend.FriendRepository;
import zov.alphadlc.util.macro.MacroRepository;
import zov.alphadlc.util.math.TPSGetter;
import zov.alphadlc.util.player.combat.IdealHitUtils;
import zov.alphadlc.util.player.other.ServerManager;
import zov.alphadlc.util.rotation.ComponentManager;
import zov.alphadlc.util.script.ScriptManager;
import zov.alphadlc.util.staff.StaffManager;

import java.io.File;

public class AlphaDLC implements ModInitializer {

    private static AlphaDLC instance;

    @Getter
    private final EventBus eventBus;

    @Getter
    private final ModuleStorage moduleStorage;
    @Getter
    private final ComponentManager componentManager;
    @Getter
    private final DragManager dragManager;
    @Getter
    private final CommandRepository commandRepository;
    @Getter
    private final MacroRepository macroRepository;
    @Getter
    private final ConfigManager configManager;
    @Getter
    private final CommandDispatcher commandDispatcher;
    @Getter
    private final StaffManager staffManager;
    @Getter
    private final ServerManager serverManager;
    @Getter
    private final TPSGetter tpsGetter;
    @Getter
    private final IdealHitUtils idealHitUtils;
    @Getter
    private final ScriptManager scriptManager;

    public AlphaDLC() {
        instance = this;

        eventBus = new EventBus();
        eventBus.register(this);

        moduleStorage = new ModuleStorage();
        componentManager = new ComponentManager();
        dragManager = new DragManager();
        macroRepository = new MacroRepository();
        configManager = new ConfigManager();
        staffManager = new StaffManager();
        staffManager.load();
        commandRepository = new CommandRepository();
        commandDispatcher = new CommandDispatcher();
        serverManager = new ServerManager();
        tpsGetter = new TPSGetter();
        idealHitUtils = new IdealHitUtils();
        scriptManager = new ScriptManager();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConfigManager.save("autocfg");
            getDragManager().saveDraggables();
            getMacroRepository().save();
            FriendRepository.save();
            staffManager.save();
        }));
        File dir = new File("alphadlc/configs/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static AlphaDLC getInstance() {
        return instance == null ? new AlphaDLC() : instance;
    }

    @Override
    public void onInitialize() {
        getModuleStorage().injectRegisterModules();
        componentManager.init();
        dragManager.load();
        macroRepository.load();
        FriendRepository.load();
        configManager.load("autocfg");
        zov.alphadlc.ui.ThemeEditor.applyStartupTheme();
    }

    @Subscribe
    private void onModuleKeyPressed(EventKeyInput event) {
        for (Module module : getModuleStorage().getModules()) {
            if (event.getAction() == 1 && MinecraftClient.getInstance().currentScreen == null) {
                if (module.getKey() == event.getKey()) {
                    module.toggle();
                }
            }
        }
    }
}