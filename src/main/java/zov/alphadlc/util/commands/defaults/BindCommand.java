package zov.alphadlc.util.commands.defaults;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import zov.alphadlc.AlphaDLC;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleStorage;
import zov.alphadlc.util.commands.api.Command;
import zov.alphadlc.util.commands.api.argument.IArgConsumer;
import zov.alphadlc.util.commands.api.datatypes.KeyDataType;
import zov.alphadlc.util.commands.api.exception.CommandException;
import zov.alphadlc.util.commands.api.exception.CommandNotEnoughArgumentsException;
import zov.alphadlc.util.commands.api.helpers.Paginator;
import zov.alphadlc.util.commands.api.helpers.TabCompleteHelper;
import zov.alphadlc.util.keyboard.KeyStorage;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static zov.alphadlc.util.commands.api.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BindCommand extends Command {

    final ModuleStorage moduleStorage;

    public BindCommand(AlphaDLC onetap) {
        super("bind");
        this.moduleStorage = onetap.getModuleStorage();
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        switch (action) {
            case "add" ->
                handleAddBind(label, args);
            case "remove" ->
                handleRemoveBind(args);
            case "list" ->
                handleListBinds(args, label);
            case "clear" ->
                handleClearBinds(args);
            default -> logDirect("Неизвестная подкоманда. Используй add/remove/list/clear.", Formatting.GRAY);
        }
    }

    // === НОВОЕ: Собираем имя модуля из нескольких аргументов (поддержка пробелов) ===
    // Используем peekString() для просмотра без съедания, getString() только после нахождения
    private Module consumeModule(IArgConsumer args) throws CommandException {
        if (!args.hasAny()) return null;
        
        int argCount = args.getArgs().size();
        
        // Перебираем от самого длинного к самому короткому
        for (int i = argCount; i >= 1; i--) {
            StringBuilder nameBuilder = new StringBuilder();
            for (int j = 0; j < i; j++) {
                if (j > 0) nameBuilder.append(" ");
                try {
                    nameBuilder.append(args.peekString(j));
                } catch (CommandNotEnoughArgumentsException e) {
                    break;
                }
            }
            String name = nameBuilder.toString();
            Module module = moduleStorage.get(name);
            if (module != null) {
                // Съедаем использованные аргументы
                for (int j = 0; j < i; j++) {
                    if (args.hasAny()) {
                        args.getString();
                    }
                }
                return module;
            }
        }
        
        return null;
    }

    private void handleAddBind(String label, IArgConsumer args) throws CommandException {
        args.requireMin(2);
        
        Module module = consumeModule(args);
        if (module == null) {
            String attemptedName = args.hasAny() ? args.getString() : "???";
            logDirect(Formatting.GRAY + "Модуль с названием " + Formatting.WHITE + attemptedName + Formatting.GRAY + " не найден");
            return;
        }
        
        int key = args.getDatatypeFor(KeyDataType.INSTANCE).getValue();
        module.setKey(key);
        logDirect(Formatting.GRAY + "Модуль " + Formatting.WHITE + module.getName() + Formatting.GRAY + " успешно привязан к клавише " + Formatting.WHITE + KeyStorage.getKey(key).toUpperCase());
    }

    private void handleRemoveBind(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        
        Module module = consumeModule(args);
        if (module == null) {
            String attemptedName = args.hasAny() ? args.getString() : "???";
            logDirect(Formatting.GRAY + "Модуль с названием " + Formatting.WHITE + attemptedName + Formatting.GRAY + " не найден");
            return;
        }
        
        module.setKey(-1);
        logDirect(Formatting.GRAY + "Модуль " + Formatting.WHITE + module.getName() + Formatting.GRAY + " больше не имеет привязку к клавише");
    }

    private void handleListBinds(IArgConsumer args, String label) throws CommandException {
        args.requireMax(1);

        List<Module> boundModules = moduleStorage.getModules()
                .stream()
                .filter(module -> module.getKey() != -1)
                .collect(Collectors.toList());

        Paginator.paginate(
                args,
                new Paginator<>(boundModules),
                () -> logDirect("Привязанные модули:"),
                module -> {
                    int key = module.getKey();
                    String keyName = KeyStorage.getKey(key).toUpperCase();
                    return Text.literal(Formatting.GRAY + module.getName() + ": " + Formatting.WHITE + keyName);
                },
                FORCE_COMMAND_PREFIX + label
        );
    }

    private void handleClearBinds(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        for (Module m : moduleStorage.getModules()) {
            m.setKey(-1);
        }
        logDirect(Formatting.GRAY + "Все модули успешно отвязаны и больше не имеют привязку к клавише");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        var raw = args.getArgs();
        int size = raw.size();

        if (size <= 1) {
            String prefix = size == 1 ? args.peekString(0) : "";
            return new TabCompleteHelper()
                    .prepend("add", "remove", "list", "clear")
                    .filterPrefix(prefix)
                    .sortAlphabetically()
                    .stream();
        }

        String action = args.peekString(0).toLowerCase(Locale.ROOT);

        if ((action.equals("add") || action.equals("remove")) && size >= 2) {
            StringBuilder prefixBuilder = new StringBuilder();
            for (int i = 1; i < size; i++) {
                if (i > 1) prefixBuilder.append(" ");
                prefixBuilder.append(args.peekString(i));
            }
            String modulePrefix = prefixBuilder.toString().toLowerCase(Locale.ROOT);
            
            Stream<Module> modules = action.equals("remove") 
                ? moduleStorage.getModules().stream().filter(m -> m.getKey() != -1)
                : moduleStorage.getModules().stream();
                
            return modules.map(Module::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(modulePrefix))
                    .sorted()
                    .distinct();
        }

        if (action.equals("add") && size >= 3) {
            StringBuilder nameBuilder = new StringBuilder();
            for (int i = 1; i < size - 1; i++) {
                if (i > 1) nameBuilder.append(" ");
                nameBuilder.append(args.peekString(i));
            }
            String potentialModule = nameBuilder.toString();
            Module module = moduleStorage.get(potentialModule);
            
            if (module != null) {
                String keyPrefix = args.peekString(size - 1);
                return KeyStorage.keyMap.keySet().stream()
                        .filter(k -> k.toLowerCase(Locale.ROOT).startsWith(keyPrefix.toLowerCase(Locale.ROOT)))
                        .sorted()
                        .distinct();
            }
        }

        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление привязкой модуля к клавише";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
            "Команда для установки и удаления привязки модуля к клавише",
            "",
            "Использование:",
            "> bind add <модуль> <клавиша> — привязать модуль к клавише",
            "> bind remove <модуль> — отвязать модуль от клавиши",
            "> bind list — показать все привязанные модули",
            "> bind clear — отвязать все модули от своих клавиш"
        );
    }
}
