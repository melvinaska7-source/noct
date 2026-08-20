package polar.ru.api.storages;

import polar.ru.api.QClient;
import polar.ru.api.events.EventInvoker;
import polar.ru.api.storages.implement.CommandStorage;
import polar.ru.api.storages.implement.ConfigStorage;
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
import polar.ru.api.utils.render.fonts.ttf.Fonts;
import polar.ru.api.utils.tps.TPSCalc;
import polar.ru.mods.maseffects.MaseffectsParticleTypes;
import polar.ru.mods.particular.ParticularParticleTypes;
import polar.ru.polar;

public class InitializeStorage
implements QClient {
    public void onInitialize() {
        EventInvoker.register(this);
        this.initStorages();
    }

    public void initStorages() {
        MaseffectsParticleTypes.register();
        ParticularParticleTypes.register();
        polar.INSTANCE.moduleStorage = new ModuleStorage();
        polar.INSTANCE.themeStorage = new ThemeStorage();
        polar.INSTANCE.tpsCalc = new TPSCalc();
        EventInvoker.register(polar.INSTANCE.tpsCalc);
        polar.INSTANCE.localizationStorage = new LocalizationStorage();
        polar.INSTANCE.freeLookStorage = new FreeLookStorage();
        polar.INSTANCE.rotationStorage = new RotationStorage();
        polar.INSTANCE.serverStorage = new ServerStorage();
        polar.INSTANCE.serverStorage.ServerManager();
        polar.INSTANCE.friendStorage = new FriendStorage();
        polar.INSTANCE.macroStorage = new MacroStorage();
        polar.INSTANCE.staffStorage = new StaffStorage();
        polar.INSTANCE.waypointStorage = new WaypointStorage();
        polar.INSTANCE.commandStorage = new CommandStorage();
        polar.INSTANCE.configStorage = new ConfigStorage();
    }
}

