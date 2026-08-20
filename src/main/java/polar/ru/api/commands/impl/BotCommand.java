package polar.ru.api.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import polar.ru.api.commands.Command;
import polar.ru.api.utils.bot.BotSessionManager;
import polar.ru.api.utils.chat.ChatUtils;

public class BotCommand
extends Command {
    public BotCommand() {
        super("bot");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)builder.then(this.literal("connect").then(this.arg("name", StringArgumentType.string()).then(this.arg("ip", StringArgumentType.string()).executes(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
                return 0;
            }
            String name = StringArgumentType.getString((CommandContext)context, (String)"name");
            String ip = StringArgumentType.getString((CommandContext)context, (String)"ip");
            BotSessionManager.connect(name, ip);
            ChatUtils.sendMessage("§7[Bot] §fПодключение выполнено: " + name + " -> " + ip);
            return 1;
        }))))).then(this.literal("remove").then(this.arg("name", StringArgumentType.string()).suggests((context, suggestions) -> {
            BotSessionManager.getSessionNames(false).forEach(arg_0 -> ((SuggestionsBuilder)suggestions).suggest(arg_0));
            return suggestions.buildFuture();
        }).executes(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
                return 0;
            }
            String name = StringArgumentType.getString((CommandContext)context, (String)"name");
            if (BotSessionManager.remove(name)) {
                ChatUtils.sendMessage("§7[Bot] §fСессия отключена и удалена: " + name);
            } else {
                ChatUtils.sendMessage("§7[Bot] §fСессия не найдена: " + name);
            }
            return 1;
        })))).then(this.literal("control").then(this.arg("name", StringArgumentType.string()).suggests((context, suggestions) -> {
            BotSessionManager.getSessionNames(false).forEach(arg_0 -> ((SuggestionsBuilder)suggestions).suggest(arg_0));
            return suggestions.buildFuture();
        }).executes(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
                return 0;
            }
            String name = StringArgumentType.getString((CommandContext)context, (String)"name");
            if (name.equalsIgnoreCase(BotSessionManager.getCurrentSessionName())) {
                ChatUtils.sendMessage("§7[Bot] §fТы уже управляешь этой сессией: " + name);
                return 1;
            }
            if (BotSessionManager.control(name)) {
                ChatUtils.sendMessage("§7[Bot] §fПереключаю на сессию: " + name);
            } else {
                ChatUtils.sendMessage("§7[Bot] §fСессия не найдена: " + name);
            }
            return 1;
        })))).then(this.literal("say").then(this.arg("name", StringArgumentType.string()).suggests((context, suggestions) -> {
            BotSessionManager.getSessionNames(false).forEach(arg_0 -> ((SuggestionsBuilder)suggestions).suggest(arg_0));
            return suggestions.buildFuture();
        }).then(this.arg("message", StringArgumentType.greedyString()).executes(context -> {
            String message;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
                return 0;
            }
            String name = StringArgumentType.getString((CommandContext)context, (String)"name");
            if (BotSessionManager.say(name, message = StringArgumentType.getString((CommandContext)context, (String)"message"))) {
                ChatUtils.sendMessage("§7[Bot] §fСообщение отправлено от сессии " + name);
            } else {
                ChatUtils.sendMessage("§7[Bot] §fСессия не найдена: " + name);
            }
            return 1;
        }))))).then(this.literal("sayall").then(this.arg("message", StringArgumentType.greedyString()).executes(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
                return 0;
            }
            String message = StringArgumentType.getString((CommandContext)context, (String)"message");
            BotSessionManager.sayAll(message);
            ChatUtils.sendMessage("§7[Bot] §fСообщение отправлено от всех ботов.");
            return 1;
        })))).then(((LiteralArgumentBuilder)this.literal("return").executes(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
                return 0;
            }
            if (BotSessionManager.restore()) {
                ChatUtils.sendMessage("§7[Bot] §fВозвращаю предыдущую сессию");
            } else {
                ChatUtils.sendMessage("§7[Bot] §fНет сохранённой сессии для возврата");
            }
            return 1;
        })).then(this.arg("name", StringArgumentType.string()).suggests((context, suggestions) -> {
            BotSessionManager.getSessionNames(true).forEach(arg_0 -> ((SuggestionsBuilder)suggestions).suggest(arg_0));
            return suggestions.buildFuture();
        }).executes(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
                return 0;
            }
            String name = StringArgumentType.getString((CommandContext)context, (String)"name");
            if (name.equalsIgnoreCase(BotSessionManager.getCurrentSessionName())) {
                ChatUtils.sendMessage("§7[Bot] §fТы уже управляешь этой сессией: " + name);
                return 1;
            }
            if (BotSessionManager.restore(name)) {
                ChatUtils.sendMessage("§7[Bot] §fПереключаю на сессию: " + name);
            } else {
                ChatUtils.sendMessage("§7[Bot] §fСессия не найдена: " + name);
            }
            return 1;
        })))).then(this.literal("ignore").executes(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
                return 0;
            }
            boolean enabled = BotSessionManager.toggleIgnoreBotMessages();
            ChatUtils.sendMessage("§7[Bot] §fИгнор сообщений ботов: " + (enabled ? "§aвключен" : "§cвыключен"));
            return 1;
        }))).then(this.literal("list").executes(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) {
                return 0;
            }
            List<BotSessionManager.BotConnection> connections = BotSessionManager.getConnections();
            ChatUtils.sendMessage("§7[Bot] §fТекущая сессия: " + BotSessionManager.getCurrentSessionName());
            if (connections.isEmpty()) {
                ChatUtils.sendMessage("§7[Bot] §fСписок сохранённых сессий пуст");
            } else {
                ChatUtils.sendMessage("§7[Bot] §fСохранённые сессии:");
                for (BotSessionManager.BotConnection bot : connections) {
                    ChatUtils.sendMessage("§7- §f" + bot.name() + " (§7" + bot.address() + "§f)");
                }
            }
            return 1;
        }));
    }
}

