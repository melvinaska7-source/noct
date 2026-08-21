package zov.alphadlc.util.commands.api.datatypes;

import zov.alphadlc.util.IMinecraft;
import zov.alphadlc.util.commands.api.exception.CommandException;

import java.util.stream.Stream;

public interface IDatatype extends IMinecraft {
    Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException;
}
