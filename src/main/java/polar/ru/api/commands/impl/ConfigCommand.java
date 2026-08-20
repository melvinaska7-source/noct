package polar.ru.api.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.io.File;
import java.util.Arrays;
import net.minecraft.command.CommandSource;
import polar.ru.api.commands.Command;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.polar;

public class ConfigCommand
extends Command {
    public ConfigCommand() {
        super("config");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)builder.then(this.literal("save").then(this.arg("config", StringArgumentType.word()).suggests((context, builder1) -> {
            File[] files;
            if (polar.INSTANCE.configsDir.exists() && polar.INSTANCE.configsDir.isDirectory() && (files = polar.INSTANCE.configsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".polar"))) != null) {
                Arrays.stream(files).map(File::getName).map(name -> name.replace(".polar", "")).forEach(arg_0 -> ((SuggestionsBuilder)builder1).suggest(arg_0));
            }
            return builder1.buildFuture();
        }).executes(context -> {
            String config = (String)context.getArgument("config", String.class);
            try {
                polar.INSTANCE.configStorage.saveConfig(config);
                ChatUtils.sendMessage("Конфиг " + config + " успешно сохранён!");
            }
            catch (Exception e2) {
                ChatUtils.sendMessage("Ошибка при сохранении конфига " + config + "!");
                e2.printStackTrace();
            }
            return 1;
        })))).then(this.literal("load").then(this.arg("config", StringArgumentType.word()).suggests((context, builder1) -> {
            File[] files;
            if (polar.INSTANCE.configsDir.exists() && polar.INSTANCE.configsDir.isDirectory() && (files = polar.INSTANCE.configsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".polar"))) != null) {
                Arrays.stream(files).map(File::getName).map(name -> name.replace(".polar", "")).forEach(arg_0 -> ((SuggestionsBuilder)builder1).suggest(arg_0));
            }
            return builder1.buildFuture();
        }).executes(context -> {
            String config = (String)context.getArgument("config", String.class);
            try {
                polar.INSTANCE.configStorage.loadConfig(config);
                ChatUtils.sendMessage("Конфиг " + config + " успешно загружен!");
            }
            catch (Exception e2) {
                ChatUtils.sendMessage("Ошибка при загрузке конфига " + config + "!");
                e2.printStackTrace();
            }
            return 1;
        })))).then(this.literal("list").executes(context -> {
            File[] files = polar.INSTANCE.configsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".polar"));
            if (files == null || files.length == 0) {
                ChatUtils.sendMessage("Список конфигов пуст!");
            } else {
                StringBuilder builder1 = new StringBuilder();
                for (int i2 = 0; i2 < files.length; ++i2) {
                    String fileName = files[i2].getName().replace(".polar", "");
                    builder1.append(fileName);
                    if (i2 >= files.length - 1) continue;
                    builder1.append(", ");
                }
                ChatUtils.sendMessage("Конфиги: " + String.valueOf(builder1));
            }
            return 1;
        }))).then(this.literal("dir").executes(context -> {
            try {
                File configsDir = new File(polar.INSTANCE.globalsDir, "configs");
                if (!configsDir.exists()) {
                    configsDir.mkdirs();
                }
                new ProcessBuilder("explorer.exe", configsDir.getAbsolutePath()).start();
                ChatUtils.sendMessage("Папка с конфигами открыта!");
            }
            catch (Exception e2) {
                ChatUtils.sendMessage("Ошибка при открытии папки с конфигами!");
                e2.printStackTrace();
            }
            return 1;
        }));
    }
}

