package zov.alphadlc.util.commands.api.exception;

import zov.alphadlc.util.QuickLogger;
import zov.alphadlc.util.commands.api.ICommand;
import zov.alphadlc.util.commands.api.argument.ICommandArgument;

import java.util.List;

public class CommandUnhandledException extends RuntimeException implements ICommandException, QuickLogger {

    public CommandUnhandledException(String message) {
        super(message);
    }

    public CommandUnhandledException(Throwable cause) {
        super(cause);
    }

    @Override
    public void handle(ICommand command, List<ICommandArgument> args) {
    }
}
