package polar.ru.api.storages.implement;

import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;
import polar.ru.api.commands.Command;
import polar.ru.api.commands.impl.BindCommand;
import polar.ru.api.commands.impl.BlockESPCommand;
import polar.ru.api.commands.impl.BotCommand;
import polar.ru.api.commands.impl.ConfigCommand;
import polar.ru.api.commands.impl.DataCommand;
import polar.ru.api.commands.impl.FriendCommand;
import polar.ru.api.commands.impl.GPSCommand;
import polar.ru.api.commands.impl.MacroCommand;
import polar.ru.api.commands.impl.MaxDamageCommand;
import polar.ru.api.commands.impl.NeuroCommand;
import polar.ru.api.commands.impl.StaffCommand;
import polar.ru.api.commands.impl.VClipCommand;

public class CommandStorage {
    private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher();
    private final List<Command> commands = new ArrayList<Command>();
    private String prefix = ".";

    public CommandStorage() {
        this.registry();
    }

    private void registry() {
        this.addCommands(new FriendCommand(), new ConfigCommand(), new MacroCommand(), new BotCommand(), new MaxDamageCommand(), new BlockESPCommand(), new GPSCommand(), new BindCommand(), new StaffCommand(), new VClipCommand(), new DataCommand(), new MaxDamageCommand(), new NeuroCommand());
    }

    public CommandSource getSource() {
        return new ClientCommandSource(null, MinecraftClient.getInstance());
    }

    private void addCommands(Command ... command) {
        for (Command cmd : command) {
            cmd.register(this.dispatcher);
            this.commands.add(cmd);
        }
    }
    public CommandDispatcher<CommandSource> getDispatcher() {
        return this.dispatcher;
    }
    public List<Command> getCommands() {
        return this.commands;
    }
    public String getPrefix() {
        return this.prefix;
    }
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}

