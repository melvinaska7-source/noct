package polar.ru.api.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.command.CommandSource;
import polar.ru.api.QClient;

public abstract class Command
implements QClient {
    private final String command;

    public Command(String command) {
        this.command = command;
    }

    public abstract void execute(LiteralArgumentBuilder<CommandSource> var1);

    public void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder builder = LiteralArgumentBuilder.literal((String)this.command);
        this.execute((LiteralArgumentBuilder<CommandSource>)builder);
        dispatcher.register(builder);
    }

    protected <T> RequiredArgumentBuilder<CommandSource, T> arg(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument((String)name, type);
    }

    protected LiteralArgumentBuilder<CommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal((String)name);
    }
    public String getCommand() {
        return this.command;
    }
}

