package polar.ru;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.lwjgl.glfw.GLFW;
import polar.ru.api.QClient;
import polar.ru.api.events.EventInvoker;
import polar.ru.api.storages.InitializeStorage;
import polar.ru.api.storages.implement.CommandStorage;
import polar.ru.api.storages.implement.ConfigStorage;
import polar.ru.api.storages.implement.DragStorage;
import polar.ru.api.storages.implement.FreeLookStorage;
import polar.ru.api.storages.implement.FriendStorage;
import polar.ru.api.storages.implement.LocalizationStorage;
import polar.ru.api.storages.implement.MacroStorage;
import polar.ru.api.storages.implement.ModuleStorage;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.storages.implement.ServerStorage;
import polar.ru.api.storages.implement.StaffStorage;
import polar.ru.api.storages.implement.ThemeStorage;
import polar.ru.api.storages.implement.WaypointStorage;
import polar.ru.api.utils.client.UserInfo;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.api.utils.rpc.DiscordManager;
import polar.ru.api.utils.tps.TPSCalc;
import polar.ru.client.modules.Module;

public class polar
implements ModInitializer,
QClient {
    public static polar INSTANCE;
    public boolean isServer;
    private static double prevTime;
    public static double deltaTime;
    public InitializeStorage initializer;
    public ModuleStorage moduleStorage;
    public ThemeStorage themeStorage;
    public TPSCalc tpsCalc;
    public ServerStorage serverStorage;
    public RotationStorage rotationStorage;
    public FreeLookStorage freeLookStorage;
    public CommandStorage commandStorage;
    public LocalizationStorage localizationStorage;
    public ConfigStorage configStorage;
    public FriendStorage friendStorage;
    public MacroStorage macroStorage;
    public StaffStorage staffStorage;
    public WaypointStorage waypointStorage;
    public DiscordManager discordManager;
    public UserInfo userInfo = UserInfo.empty();
    public File globalsDir;
    public File configsDir;
    public File abItemsDir;

    public polar() {
        INSTANCE = this;
    }

        public void onInitialize() {
        this.initStorage();
        WorldRenderEvents.START.register(client -> {
            double currentTime = GLFW.glfwGetTime();
            deltaTime = currentTime - prevTime;
            prevTime = currentTime;
            deltaTime = mc.isPaused() ? 0.0 : Math.min(0.05, deltaTime);
        });
    }

        private void initStorage() {
        this.globalsDir = new File("C:\\lavanda", "lavanda");
        this.configsDir = new File(this.globalsDir, "configs");
        this.abItemsDir = new File(this.globalsDir, "abitems");
        EventInvoker.register(this);
        this.createDirs(this.globalsDir, this.configsDir, this.abItemsDir);
        this.initializer = new InitializeStorage();
        this.initializer.onInitialize();
        try {
            this.discordManager = null; // Removed RPC
        }
        catch (UnsatisfiedLinkError e2) {
            System.out.println("Discord RPC native library not found. Discord integration disabled.");
            this.discordManager = null;
        }
        this.playWelcomeSound();
    }

    private void playWelcomeSound() {
        // disabled
    }

    private void createDirs(File ... file) {
        for (File f2 : file) {
            f2.mkdirs();
        }
    }

    public void closeMinecraft() {
        try {
            this.configStorage.saveConfig(this.configStorage.currentConfig);
        }
        catch (Exception e2) {
            e2.printStackTrace();
        }
        if (this.discordManager != null) {
            this.discordManager.stopRPC();
        }
    }

    public static Draggable draggable(Module module, String name, float x2, float y2) {
        DragStorage.draggables.put(name, new Draggable(module, name, x2, y2));
        return DragStorage.draggables.get(name);
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo == null ? UserInfo.empty() : userInfo;
    }
    public UserInfo getUserInfo() {
        return this.userInfo;
    }

    static {
        prevTime = 0.0;
        deltaTime = 0.0;
    }
}

