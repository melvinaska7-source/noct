package zov.alphadlc.util.commands.api;

import zov.alphadlc.util.commands.api.argparser.IArgParserManager;

public interface ICommandSystem {
    IArgParserManager getParserManager();
}
