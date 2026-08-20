package polar.ru.api.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import polar.ru.api.commands.Command;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.polar;

public class StaffCommand
extends Command {
    public StaffCommand() {
        super("staff");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)builder.then(this.literal("add").then(this.arg("player", StringArgumentType.word()).suggests((context, builder1) -> {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                String name = entry.getProfile().getName();
                if (!name.toLowerCase().startsWith(builder1.getRemaining().toLowerCase())) continue;
                builder1.suggest(name);
            }
            return builder1.buildFuture();
        }).executes(context -> {
            String player = (String)context.getArgument("player", String.class);
            if (!polar.INSTANCE.staffStorage.isStaff(player)) {
                polar.INSTANCE.staffStorage.add(player);
                ChatUtils.sendMessage("Игрок " + player + " добавлен в список стаффов!");
            } else {
                ChatUtils.sendMessage("Игрок " + player + " уже в списке стаффов!");
            }
            return 1;
        })))).then(this.literal("remove").then(this.arg("player", StringArgumentType.word()).suggests((context, builder1) -> {
            polar.INSTANCE.staffStorage.getStaffs().stream().sorted(String::compareTo).filter(name -> name.startsWith(builder1.getRemaining())).forEach(arg_0 -> ((SuggestionsBuilder)builder1).suggest(arg_0));
            return builder1.buildFuture();
        }).executes(context -> {
            String player = (String)context.getArgument("player", String.class);
            if (polar.INSTANCE.staffStorage.isStaff(player)) {
                polar.INSTANCE.staffStorage.remove(player);
                ChatUtils.sendMessage("Игрок " + player + " удалён из списка стаффов!");
            } else {
                ChatUtils.sendMessage("Игрок " + player + " не найден в списке стаффов!");
            }
            return 1;
        })))).then(this.literal("list").executes(context -> {
            StringBuilder builder1 = new StringBuilder();
            if (polar.INSTANCE.staffStorage.getStaffs().isEmpty()) {
                ChatUtils.sendMessage("Список стаффов пуст!");
            } else {
                for (int i2 = 0; i2 < polar.INSTANCE.staffStorage.getStaffs().size(); ++i2) {
                    builder1.append(polar.INSTANCE.staffStorage.getStaffs().get(i2));
                    if (i2 >= polar.INSTANCE.staffStorage.getStaffs().size() - 1) continue;
                    builder1.append(", ");
                }
                builder1.append(".");
                ChatUtils.sendMessage("Стаффы: " + String.valueOf(builder1));
            }
            return 1;
        }))).then(this.literal("clear").executes(context -> {
            if (!polar.INSTANCE.staffStorage.isEmpty()) {
                polar.INSTANCE.staffStorage.clear();
                ChatUtils.sendMessage("Список стаффов очищен!");
            } else {
                ChatUtils.sendMessage("Список стаффов пуст!");
            }
            return 1;
        }));
    }
}

