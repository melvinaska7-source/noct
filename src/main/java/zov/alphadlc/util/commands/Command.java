package zov.alphadlc.util.commands;

import zov.alphadlc.util.IMinecraft;

public abstract class Command implements IMinecraft {
    private final String name;
    private final String syntax;
    private final String description;

    public Command(String name, String syntax, String description) {
        this.name = name;
        this.syntax = syntax;
        this.description = description;
    }

    public abstract void execute(String[] args);

    public String getName() {
        return name;
    }

    public String getSyntax() {
        return syntax;
    }

    public String getDescription() {
        return description;
    }
}
