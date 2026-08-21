package zov.alphadlc.util.commands;

import zov.alphadlc.util.commands.api.ICommandSystem;
import zov.alphadlc.util.commands.api.argparser.IArgParserManager;
import zov.alphadlc.util.commands.argparser.ArgParserManager;

public enum CommandSystem implements ICommandSystem {
    INSTANCE;

    @Override
    public IArgParserManager getParserManager() {
        return ArgParserManager.INSTANCE;
    }
}
