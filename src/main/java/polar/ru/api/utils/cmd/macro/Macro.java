package polar.ru.api.utils.cmd.macro;

import polar.ru.client.modules.settings.implement.BindSetting;

public class Macro {
    private String name;
    private String command;
    private BindSetting bind;
    public Macro(String name, String command, BindSetting bind) {
        this.name = name;
        this.command = command;
        this.bind = bind;
    }
    public String getName() {
        return this.name;
    }
    public String getCommand() {
        return this.command;
    }
    public BindSetting getBind() {
        return this.bind;
    }
}

