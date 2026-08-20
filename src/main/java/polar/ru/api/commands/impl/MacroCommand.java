package polar.ru.api.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.lang.reflect.Field;
import net.minecraft.command.CommandSource;
import org.lwjgl.glfw.GLFW;
import polar.ru.api.commands.Command;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.api.utils.cmd.macro.Macro;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.polar;

public class MacroCommand
extends Command {
    public MacroCommand() {
        super("macro");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)builder.then(this.literal("add").then(this.arg("name", StringArgumentType.word()).then(this.arg("bind", StringArgumentType.word()).suggests((context, builder1) -> {
            for (Field field : GLFW.class.getDeclaredFields()) {
                String bind;
                String name = field.getName();
                if (!name.startsWith("GLFW_KEY_") || !(bind = name.replace("GLFW_KEY_", "")).startsWith(builder1.getRemaining())) continue;
                builder1.suggest(bind);
            }
            if ("NONE".startsWith(builder1.getRemaining().toUpperCase())) {
                builder1.suggest("NONE");
            }
            return builder1.buildFuture();
        }).then(this.arg("command", StringArgumentType.greedyString()).executes(context -> {
            String name = (String)context.getArgument("name", String.class);
            String bind = ((String)context.getArgument("bind", String.class)).toUpperCase();
            String command = (String)context.getArgument("command", String.class);
            if (polar.INSTANCE.macroStorage.getMacro(name) != null) {
                ChatUtils.sendMessage("Макрос " + name + " уже существует!");
                return 1;
            }
            try {
                int key = "NONE".equals(bind) ? -1 : GLFW.class.getField("GLFW_KEY_" + bind).getInt(null);
                polar.INSTANCE.macroStorage.add(new Macro(name, command, new BindSetting("bind", key)));
                ChatUtils.sendMessage("Макрос " + name + " был добавлен!");
            }
            catch (Exception ignored) {
                ChatUtils.sendMessage("Неверный бинд: " + bind);
            }
            return 1;
        })))))).then(this.literal("remove").then(this.arg("name", StringArgumentType.word()).suggests((context, builder1) -> {
            polar.INSTANCE.macroStorage.getNames().stream().filter(name -> name.startsWith(builder1.getRemaining())).forEach(arg_0 -> ((SuggestionsBuilder)builder1).suggest(arg_0));
            return builder1.buildFuture();
        }).executes(context -> {
            String name = (String)context.getArgument("name", String.class);
            if (polar.INSTANCE.macroStorage.isEmpty()) {
                ChatUtils.sendMessage("Список макросов пуст!");
                return 1;
            }
            Macro macro = polar.INSTANCE.macroStorage.getMacro(name);
            if (macro == null) {
                ChatUtils.sendMessage("Макрос " + name + " не найден!");
                return 1;
            }
            polar.INSTANCE.macroStorage.remove(macro);
            ChatUtils.sendMessage("Макрос " + name + " был удалён!");
            return 1;
        })))).then(this.literal("list").executes(context -> {
            StringBuilder builder1 = new StringBuilder();
            if (polar.INSTANCE.macroStorage.getNames().isEmpty()) {
                ChatUtils.sendMessage("Список макросов пуст!");
            } else {
                for (int i2 = 0; i2 < polar.INSTANCE.macroStorage.getNames().size(); ++i2) {
                    builder1.append(polar.INSTANCE.macroStorage.getNames().get(i2));
                    if (i2 >= polar.INSTANCE.macroStorage.getNames().size() - 1) continue;
                    builder1.append(", ");
                }
                builder1.append(".");
                ChatUtils.sendMessage("Макросы: " + String.valueOf(builder1));
            }
            return 1;
        }))).then(this.literal("clear").executes(context -> {
            if (!polar.INSTANCE.macroStorage.isEmpty()) {
                polar.INSTANCE.macroStorage.clear();
                ChatUtils.sendMessage("Все макросы были удалены!");
            } else {
                ChatUtils.sendMessage("Список макросов пуст!");
            }
            return 1;
        }));
    }
}

