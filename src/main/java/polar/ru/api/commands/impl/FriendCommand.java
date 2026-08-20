package polar.ru.api.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import polar.ru.api.commands.Command;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.polar;

public class FriendCommand
extends Command {
    public FriendCommand() {
        super("friend");
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
            if (!polar.INSTANCE.friendStorage.isFriend(player)) {
                polar.INSTANCE.friendStorage.add(player);
                ChatUtils.sendMessage("Игрок " + player + " добавлен в друзья!");
            } else {
                ChatUtils.sendMessage("Игрок " + player + " уже в списке друзей!");
            }
            return 1;
        })))).then(this.literal("remove").then(this.arg("player", StringArgumentType.word()).suggests((context, builder1) -> {
            polar.INSTANCE.friendStorage.getFriends().stream().sorted(String::compareTo).filter(name -> name.startsWith(builder1.getRemaining())).forEach(arg_0 -> ((SuggestionsBuilder)builder1).suggest(arg_0));
            return builder1.buildFuture();
        }).executes(context -> {
            String player = (String)context.getArgument("player", String.class);
            if (polar.INSTANCE.friendStorage.isFriend(player)) {
                polar.INSTANCE.friendStorage.remove(player);
                ChatUtils.sendMessage("Игрок " + player + " удалён из друзей!");
            } else {
                ChatUtils.sendMessage("Игрок " + player + " не найден в списке друзей!");
            }
            return 1;
        })))).then(this.literal("list").executes(context -> {
            if (polar.INSTANCE.friendStorage.getFriends().isEmpty()) {
                ChatUtils.sendMessage("Список друзей пуст!");
            } else {
                StringBuilder builder1 = new StringBuilder();
                for (int i2 = 0; i2 < polar.INSTANCE.friendStorage.getFriends().size(); ++i2) {
                    builder1.append(polar.INSTANCE.friendStorage.getFriends().get(i2));
                    if (i2 >= polar.INSTANCE.friendStorage.getFriends().size() - 1) continue;
                    builder1.append(", ");
                }
                ChatUtils.sendMessage("Друзья: " + String.valueOf(builder1));
            }
            return 1;
        }))).then(this.literal("clear").executes(context -> {
            if (!polar.INSTANCE.friendStorage.isEmpty()) {
                polar.INSTANCE.friendStorage.clear();
                ChatUtils.sendMessage("Список друзей очищен!");
            } else {
                ChatUtils.sendMessage("Список друзей пуст!");
            }
            return 1;
        }));
    }
}

