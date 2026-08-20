package polar.ru.api.storages.implement;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Formatting;
import polar.ru.api.QClient;
import polar.ru.api.events.EventInvoker;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventBinding;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.api.utils.cmd.macro.Macro;
import polar.ru.polar;

public class MacroStorage
implements QClient {
    private final List<Macro> macros = new ArrayList<Macro>();
    private final List<String> names = new ArrayList<String>();

    public MacroStorage() {
        EventInvoker.register(this);
    }

    public void add(Macro macro) {
        if (macro == null || macro.getName() == null || macro.getName().isBlank() || this.getMacro(macro.getName()) != null) {
            return;
        }
        this.macros.add(macro);
        this.names.add(macro.getName());
        this.save();
    }

    public void remove(Macro macro) {
        if (macro == null) {
            return;
        }
        this.macros.remove(macro);
        this.names.remove(macro.getName());
        this.save();
    }

    public void clear() {
        if (!this.macros.isEmpty()) {
            this.macros.clear();
        }
        if (!this.names.isEmpty()) {
            this.names.clear();
        }
        this.save();
    }

    private void save() {
        try {
            polar.INSTANCE.configStorage.saveGlobals();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public boolean isEmpty() {
        return this.macros.isEmpty();
    }

    public Macro getMacro(String name) {
        for (Macro macro : this.macros) {
            if (!macro.getName().equalsIgnoreCase(name)) continue;
            return macro;
        }
        return null;
    }

    @EventLink
    public void onKey(EventBinding e2) {
        if (MacroStorage.mc.player == null || MacroStorage.mc.world == null || MacroStorage.mc.currentScreen != null || MacroStorage.mc.player.networkHandler == null || this.macros.isEmpty()) {
            return;
        }
        for (Macro macro : this.macros) {
            if (macro == null || macro.getBind() == null || macro.getBind().getKey() != e2.getKey()) continue;
            this.executeMacro(macro);
        }
    }

    private void executeMacro(Macro macro) {
        String command = macro.getCommand();
        if (command == null || command.isBlank()) {
            return;
        }
        if (command.startsWith("/")) {
            MacroStorage.mc.player.networkHandler.sendChatCommand(command.substring(1));
            return;
        }
        String prefix = polar.INSTANCE.commandStorage.getPrefix();
        if (prefix != null && !prefix.isEmpty() && command.startsWith(prefix)) {
            try {
                polar.INSTANCE.commandStorage.getDispatcher().execute(command.substring(prefix.length()), (net.minecraft.command.CommandSource)polar.INSTANCE.commandStorage.getSource());
            }
            catch (CommandSyntaxException ignored) {
                ChatUtils.sendMessage(String.valueOf(Formatting.RED) + "Ошибка в использовании макроса " + macro.getName() + "!");
            }
            return;
        }
        MacroStorage.mc.player.networkHandler.sendChatMessage(command);
    }
    public List<Macro> getMacros() {
        return this.macros;
    }
    public List<String> getNames() {
        return this.names;
    }
}

