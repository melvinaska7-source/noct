package zov.alphadlc.module.list.player;

import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeListSetting;

@ModuleInformation(moduleName = "No Push", moduleDesc = "Убирает толкание от игроков и блоков", moduleCategory = ModuleCategory.PLAYER)
public class NoPush extends Module {
    public final ModeListSetting objects = new ModeListSetting("Обьекты",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Блоки", true)
    );
}