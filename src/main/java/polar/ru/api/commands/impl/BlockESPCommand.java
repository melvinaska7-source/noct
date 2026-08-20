package polar.ru.api.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import polar.ru.api.commands.Command;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.client.modules.impl.render.BlockESP;

public class BlockESPCommand
extends Command {
    public BlockESPCommand() {
        super("blockesp");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)builder.then(this.literal("add").then(this.arg("block", StringArgumentType.word()).suggests((context, builder1) -> {
            String input = builder1.getRemaining().toLowerCase();
            Registries.BLOCK.stream().map(arg_0 -> ((DefaultedRegistry)Registries.BLOCK).getId(arg_0)).map(Identifier::getPath).filter(name -> name.startsWith(input)).limit(20L).forEach(arg_0 -> ((SuggestionsBuilder)builder1).suggest(arg_0));
            return builder1.buildFuture();
        }).executes(context -> {
            String blockName = (String)context.getArgument("block", String.class);
            if (BlockESP.INSTANCE.isTracking(blockName)) {
                ChatUtils.sendMessage("§cБлок §e" + blockName + "§c уже отслеживается!");
                return 1;
            }
            boolean exists = Registries.BLOCK.stream().anyMatch(block -> {
                String name = Registries.BLOCK.getId(block).getPath();
                return name.equalsIgnoreCase(blockName);
            });
            if (!exists) {
                ChatUtils.sendMessage("§cБлок §e" + blockName + "§c не найден!");
                return 1;
            }
            BlockESP.INSTANCE.addBlock(blockName);
            ChatUtils.sendMessage("§aБлок §e" + blockName + "§a добавлен в отслеживание!");
            return 1;
        })))).then(this.literal("remove").then(this.arg("block", StringArgumentType.word()).suggests((context, builder1) -> {
            BlockESP.INSTANCE.getTrackedBlocks().stream().sorted(String::compareTo).filter(name -> name.startsWith(builder1.getRemaining().toLowerCase())).forEach(arg_0 -> ((SuggestionsBuilder)builder1).suggest(arg_0));
            return builder1.buildFuture();
        }).executes(context -> {
            String blockName = (String)context.getArgument("block", String.class);
            if (!BlockESP.INSTANCE.isTracking(blockName)) {
                ChatUtils.sendMessage("§cБлок §e" + blockName + "§c не отслеживается!");
                return 1;
            }
            BlockESP.INSTANCE.removeBlock(blockName);
            ChatUtils.sendMessage("§aБлок §e" + blockName + "§a удалён из отслеживания!");
            return 1;
        })))).then(this.literal("list").executes(context -> {
            Set<String> blocks = BlockESP.INSTANCE.getTrackedBlocks();
            if (blocks.isEmpty()) {
                ChatUtils.sendMessage("§cСписок отслеживаемых блоков пуст!");
                return 1;
            }
            String blockList = blocks.stream().sorted().collect(Collectors.joining("§7, §e"));
            ChatUtils.sendMessage("§aОтслеживаемые блоки §7(§e" + blocks.size() + "§7)§a: §e" + blockList);
            return 1;
        }))).then(this.literal("clear").executes(context -> {
            if (BlockESP.INSTANCE.getTrackedBlocks().isEmpty()) {
                ChatUtils.sendMessage("§cСписок отслеживаемых блоков уже пуст!");
                return 1;
            }
            BlockESP.INSTANCE.clearBlocks();
            ChatUtils.sendMessage("§aСписок отслеживаемых блоков очищен!");
            return 1;
        }));
    }
}

