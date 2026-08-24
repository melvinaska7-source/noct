package zov.alphadlc.util.commands.defaults;

import zov.alphadlc.util.chat.ChatUtil;
import zov.alphadlc.util.commands.Command;
import zov.alphadlc.util.neuro.rotation.AIRotationManager;

public class AICommand extends Command {

    private final AIRotationManager manager = new AIRotationManager();

    public AICommand() {
        super("ai", ".ai <start|stop|save|load|train|predict>", "Управление AI ротациями");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            ChatUtil.send("§cИспользование: §f" + getSyntax());
            return;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                manager.startRecording();
            }
            case "stop" -> {
                manager.stopRecording();
            }
            case "save" -> {
                if (args.length < 2) {
                    ChatUtil.send("§cУкажите имя датасета: §f.ai save <name>");
                    return;
                }
                manager.saveDataset(args[1]);
            }
            case "load" -> {
                if (args.length < 2) {
                    ChatUtil.send("§cУкажите имя датасета: §f.ai load <name>");
                    return;
                }
                manager.loadDataset(args[1]);
            }
            case "train" -> {
                if (args.length < 2) {
                    ChatUtil.send("§cУкажите имя модели: §f.ai train <name>");
                    return;
                }
                manager.trainModel(args[1]);
            }
            case "predict" -> {
                manager.predict(mc.player);
            }
            default -> ChatUtil.send("§cНеизвестная команда. Использование: §f" + getSyntax());
        }
    }
}
