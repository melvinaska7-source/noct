package zov.alphadlc.util.commands.defaults;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import zov.alphadlc.util.commands.api.Command;
import zov.alphadlc.util.commands.api.exception.CommandException;
import zov.alphadlc.util.commands.api.argument.IArgConsumer;
import zov.alphadlc.util.gps.GpsRenderer;

public class GpsCommand extends Command {
    public GpsCommand() {
        super("gps");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        double z2;
        double x2;
        GpsRenderer gps = GpsRenderer.get();
        if (args.peekString().equalsIgnoreCase("off")) {
            gps.setEnabled(false);
            return;
        }
        try {
            x2 = Double.parseDouble(args.getString());
            z2 = Double.parseDouble(args.getString());
        }
        catch (NumberFormatException e2) {
            return;
        }
        gps.setTarget(x2, z2);
        gps.setEnabled(true);
    }

    @Override
    public String getShortDesc() {
        return "Устанавливает стрелку на указанные координаты";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList("С помощью этой команды можно установить стрелку указывающую на указанные координаты", "", "Использование:", "> gps <x> <z> - Ставит стрелку на указанные координаты.", "> gps off - Отключает стрелку.");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        if (!args.hasAny() || args.hasExactlyOne() && args.getArgs().getFirst().getValue().isEmpty()) {
            ClientPlayerEntity p2 = MinecraftClient.getInstance().player;
            if (p2 == null) {
                return Stream.of("off");
            }
            return Stream.of("off", (int)p2.getX() + " " + (int)p2.getZ());
        }
        return Stream.empty();
    }
}
