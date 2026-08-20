package polar.ru.api.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import net.minecraft.command.CommandSource;
import org.lwjgl.glfw.GLFW;
import polar.ru.api.commands.Command;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.client.modules.Module;
import polar.ru.polar;

public class BindCommand
extends Command {
    public BindCommand() {
        super("bind");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(this.literal("add").then(this.arg("module", StringArgumentType.word()).suggests((context, suggestionsBuilder) -> {
            String remaining = suggestionsBuilder.getRemaining().toLowerCase();
            ModuleClass.INSTANCE.getObject().stream().map(Module::getName).filter(name -> name.toLowerCase().startsWith(remaining)).forEach(arg_0 -> ((SuggestionsBuilder)suggestionsBuilder).suggest(arg_0));
            return suggestionsBuilder.buildFuture();
        }).then(this.arg("key", StringArgumentType.word()).suggests((context, suggestionsBuilder) -> {
            String remaining = suggestionsBuilder.getRemaining().toUpperCase();
            for (Field field : GLFW.class.getDeclaredFields()) {
                String keyName;
                String fieldName = field.getName();
                if (!fieldName.startsWith("GLFW_KEY_") || !(keyName = fieldName.replace("GLFW_KEY_", "")).startsWith(remaining)) continue;
                suggestionsBuilder.suggest(keyName);
            }
            if ("NONE".startsWith(remaining)) {
                suggestionsBuilder.suggest("NONE");
            }
            return suggestionsBuilder.buildFuture();
        }).executes(ctx -> {
            String moduleName = (String)ctx.getArgument("module", String.class);
            Optional<Module> optionalModule = this.findModuleByName(moduleName);
            if (optionalModule.isEmpty()) {
                ChatUtils.sendMessage("Модуль " + moduleName + " не найден");
                return 1;
            }
            Module module = optionalModule.get();
            String keyName = ((String)ctx.getArgument("key", String.class)).toUpperCase();
            int keyCode = this.getKeyCode(keyName);
            if (keyCode == -1) {
                ChatUtils.sendMessage("Клавиша " + keyName + " не найдена");
            } else {
                module.setKey(keyCode);
                ChatUtils.sendMessage("Модуль " + module.getName() + " привязан к клавише " + keyName);
                this.saveConfig();
            }
            return 1;
        }))));
        builder.then(this.literal("remove").then(this.arg("module", StringArgumentType.word()).executes(ctx -> {
            String moduleName = (String)ctx.getArgument("module", String.class);
            Optional<Module> optionalModule = this.findModuleByName(moduleName);
            if (optionalModule.isEmpty()) {
                ChatUtils.sendMessage("Модуль " + moduleName + " не найден");
                return 1;
            }
            Module module = optionalModule.get();
            module.setKey(-1);
            ChatUtils.sendMessage("Привязка клавиши для модуля " + module.getName() + " удалена");
            return 1;
        })));
        builder.then(this.literal("clear").executes(ctx -> {
            ModuleClass.INSTANCE.getObject().forEach(module -> module.setKey(-1));
            ChatUtils.sendMessage("Все привязки клавиш удалены");
            return 1;
        }));
        builder.then(this.literal("list").executes(ctx -> {
            List<Module> boundModules = ModuleClass.INSTANCE.getObject().stream().filter(m2 -> m2.getKey() != -1).toList();
            if (boundModules.isEmpty()) {
                ChatUtils.sendMessage("Нет модулей с привязками");
                return 1;
            }
            for (Module module : boundModules) {
                String keyName = this.getKeyName(module.getKey());
                ChatUtils.sendMessage("Модуль: " + module.getName() + " -> " + keyName);
            }
            return 1;
        }));
    }

    private Optional<Module> findModuleByName(String moduleName) {
        return ModuleClass.INSTANCE.getObject().stream().filter(module -> module.getName().equalsIgnoreCase(moduleName)).findFirst();
    }

    private int getKeyCode(String keyName) {
        if ("NONE".equalsIgnoreCase(keyName)) {
            return -1;
        }
        try {
            return GLFW.class.getField("GLFW_KEY_" + keyName).getInt(null);
        }
        catch (IllegalAccessException | NoSuchFieldException ignored) {
            return -1;
        }
    }

    private String getKeyName(int keyCode) {
        for (Field field : GLFW.class.getDeclaredFields()) {
            String fieldName = field.getName();
            if (!fieldName.startsWith("GLFW_KEY_")) continue;
            try {
                if (field.getInt(null) != keyCode) continue;
                return fieldName.replace("GLFW_KEY_", "");
            }
            catch (IllegalAccessException illegalAccessException) {
                // empty catch block
            }
        }
        return "UNKNOWN";
    }

    private void saveConfig() {
        try {
            polar.INSTANCE.configStorage.saveConfig(polar.INSTANCE.configStorage.currentConfig);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

