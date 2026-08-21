package zov.alphadlc.util.commands.api.datatypes;

import zov.alphadlc.util.commands.api.exception.CommandException;

public interface IDatatypeFor<T> extends IDatatype  {
    T get(IDatatypeContext datatypeContext) throws CommandException;
}
