package polar.ru.api.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import polar.ru.api.commands.Command;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.client.modules.impl.combat.components.rotations.NeuroRotation;
import polar.ru.client.modules.impl.combat.components.rotations.neuro.NeuroPatternData;
import polar.ru.client.modules.impl.combat.components.rotations.neuro.NeuroPatternRecorder;

public class NeuroCommand
extends Command {
    public NeuroCommand() {
        super("neuro");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)builder.then(((LiteralArgumentBuilder)this.literal("start").executes(context -> {
            ChatUtils.sendMessage("§cИспользование: §e.neuro start <название>");
            ChatUtils.sendMessage("§7Пример: §e.neuro start mypattern");
            return 1;
        })).then(this.arg("name", StringArgumentType.word()).executes(context -> {
            String name = (String)context.getArgument("name", String.class);
            if (NeuroRotation.recorder.isRecording()) {
                ChatUtils.sendMessage("§cУже идёт запись паттерна! Остановите её командой §e.neuro stop");
                return 1;
            }
            NeuroRotation.recorder.startRecording(name);
            return 1;
        })))).then(this.literal("stop").executes(context -> {
            if (!NeuroRotation.recorder.isRecording()) {
                ChatUtils.sendMessage("§cЗапись не активна! Начните запись командой §e.neuro start <name>");
                return 1;
            }
            NeuroRotation.recorder.stopRecording();
            return 1;
        }))).then(this.literal("load").then(this.arg("name", StringArgumentType.word()).executes(context -> {
            String name = (String)context.getArgument("name", String.class);
            if (NeuroRotation.recorder.isRecording()) {
                ChatUtils.sendMessage("§cСначала остановите запись паттерна командой §e.neuro stop");
                return 1;
            }
            NeuroPatternData pattern = NeuroPatternRecorder.loadPattern(name);
            if (pattern != null) {
                NeuroRotation.neuroAI.loadPattern(pattern);
                ChatUtils.sendMessage("§aПаттерн §e" + name + "§a загружен и активен!");
                ChatUtils.sendMessage("§7Контексты: §e" + NeuroRotation.neuroAI.getContextStats());
            }
            return 1;
        })))).then(this.literal("unload").executes(context -> {
            if (!NeuroRotation.neuroAI.hasPattern()) {
                ChatUtils.sendMessage("§cНет загруженного паттерна!");
                return 1;
            }
            String patternName = NeuroRotation.neuroAI.getPatternName();
            NeuroRotation.neuroAI.clearPattern();
            ChatUtils.sendMessage("§aПаттерн §e" + patternName + "§a выгружен!");
            return 1;
        }))).then(this.literal("list").executes(context -> {
            NeuroPatternRecorder.listPatterns();
            return 1;
        }))).then(this.literal("info").executes(context -> {
            if (NeuroRotation.recorder.isRecording()) {
                ChatUtils.sendMessage("§eТекущий статус: §aЗАПИСЬ АКТИВНА");
                ChatUtils.sendMessage("§7Остановите запись командой §e.neuro stop");
            } else if (NeuroRotation.neuroAI.hasPattern()) {
                String patternName = NeuroRotation.neuroAI.getPatternName();
                ChatUtils.sendMessage("§eТекущий статус: §aПАТТЕРН АКТИВЕН");
                ChatUtils.sendMessage("§7Паттерн: §e" + patternName);
                ChatUtils.sendMessage("§7Контексты: §e" + NeuroRotation.neuroAI.getContextStats());
            } else {
                ChatUtils.sendMessage("§eТекущий статус: §7НЕАКТИВЕН");
                ChatUtils.sendMessage("§7Используйте §e.neuro start <name>§7 для записи");
                ChatUtils.sendMessage("§7Используйте §e.neuro load <name>§7 для загрузки");
            }
            return 1;
        }))).executes(context -> {
            ChatUtils.sendMessage("§e§lNeuro Rotation System");
            ChatUtils.sendMessage("§7AI система ротации с обучением на ваших паттернах");
            ChatUtils.sendMessage("");
            ChatUtils.sendMessage("§e.neuro start <name> §7- Начать запись паттерна");
            ChatUtils.sendMessage("§e.neuro stop §7- Остановить запись");
            ChatUtils.sendMessage("§e.neuro load <name> §7- Загрузить паттерн");
            ChatUtils.sendMessage("§e.neuro unload §7- Выгрузить паттерн");
            ChatUtils.sendMessage("§e.neuro list §7- Список паттернов");
            ChatUtils.sendMessage("§e.neuro info §7- Текущий статус");
            ChatUtils.sendMessage("");
            ChatUtils.sendMessage("§7Как использовать:");
            ChatUtils.sendMessage("§71. §e.neuro start mypattern§7 - начать запись");
            ChatUtils.sendMessage("§72. Двигайте головой в бою как обычно");
            ChatUtils.sendMessage("§73. §e.neuro stop§7 - сохранить паттерн");
            ChatUtils.sendMessage("§74. §e.neuro load mypattern§7 - загрузить для использования");
            return 1;
        });
    }
}

