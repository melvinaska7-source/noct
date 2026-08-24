package zov.alphadlc.util.commands.defaults;

import zov.alphadlc.AlphaDLC;
import zov.alphadlc.util.commands.api.ICommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DefaultCommands {

    public static List<ICommand> createAll() {
        // PartyCommand и AICommand наследуют Command (не ICommand),
        // поэтому их нельзя добавить в этот список.
        List<ICommand> commands = new ArrayList<ICommand>(Arrays.<ICommand>asList(
                new CfgCommand(),
                new RotationCommand(),
                new HelpCommand(AlphaDLC.getInstance()),
                new MacroCommand(AlphaDLC.getInstance()),
                new BindCommand(AlphaDLC.getInstance()),
                new FriendCommand(AlphaDLC.getInstance()),
                new StaffCommand(AlphaDLC.getInstance()),
                new VClipCommand(),
                new TpCommand(),
                new GpsCommand(),
                new BotCommand()
        ));
        return Collections.unmodifiableList(commands);
    }
}
