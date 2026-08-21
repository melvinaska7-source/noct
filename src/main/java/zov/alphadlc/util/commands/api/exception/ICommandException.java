package zov.alphadlc.util.commands.api.exception;

import net.minecraft.util.Formatting;
import zov.alphadlc.util.QuickLogger;
import zov.alphadlc.util.commands.api.ICommand;
import zov.alphadlc.util.commands.api.argument.ICommandArgument;

import java.util.List;

public interface ICommandException extends QuickLogger {

    String getMessage();

    default void handle(ICommand command, List<ICommandArgument> args) {
        logDirect(
                this.getMessage(),
                Formatting.RED
        );
    }
}
