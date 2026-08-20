package polar.ru.api.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import polar.ru.api.commands.Command;
import polar.ru.client.modules.impl.combat.Aura;

public class MaxDamageCommand
extends Command {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public MaxDamageCommand() {
        super("maxdamage");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)builder.then(this.literal("set").then(this.arg("itemName", StringArgumentType.greedyString()).executes(this::setItemFilter)))).then(this.literal("clear").executes(this::clearItemFilter))).then(this.literal("info").executes(this::showInfo))).executes(this::showHelp);
    }

    private int setItemFilter(CommandContext<CommandSource> context) {
        block3: {
            try {
                String itemName = (String)context.getArgument("itemName", String.class);
                Aura.INSTANCE.setMaxDamageFilter(itemName);
                if (MaxDamageCommand.mc.player != null) {
                    MaxDamageCommand.mc.player.sendMessage((Text)Text.literal((String)("§aФильтр установлен: §f" + itemName)), false);
                }
            }
            catch (Exception e2) {
                if (MaxDamageCommand.mc.player == null) break block3;
                MaxDamageCommand.mc.player.sendMessage((Text)Text.literal((String)("§cОшибка: " + e2.getMessage())), false);
            }
        }
        return 1;
    }

    private int clearItemFilter(CommandContext<CommandSource> context) {
        Aura.INSTANCE.clearMaxDamageFilter();
        if (MaxDamageCommand.mc.player != null) {
            MaxDamageCommand.mc.player.sendMessage((Text)Text.literal((String)"§aФильтр сброшен"), false);
        }
        return 1;
    }

    private int showInfo(CommandContext<CommandSource> context) {
        String filter = Aura.INSTANCE.getMaxDamageFilter();
        if (MaxDamageCommand.mc.player != null) {
            if (filter == null || filter.isEmpty()) {
                MaxDamageCommand.mc.player.sendMessage((Text)Text.literal((String)"§7Фильтр не установлен"), false);
            } else {
                MaxDamageCommand.mc.player.sendMessage((Text)Text.literal((String)("§7Фильтр: §f" + filter)), false);
            }
        }
        return 1;
    }

    private int showHelp(CommandContext<CommandSource> context) {
        if (MaxDamageCommand.mc.player != null) {
            MaxDamageCommand.mc.player.sendMessage((Text)Text.literal((String)"§e.maxdamage set <название>"), false);
            MaxDamageCommand.mc.player.sendMessage((Text)Text.literal((String)"§e.maxdamage clear"), false);
            MaxDamageCommand.mc.player.sendMessage((Text)Text.literal((String)"§e.maxdamage info"), false);
        }
        return 1;
    }
}

