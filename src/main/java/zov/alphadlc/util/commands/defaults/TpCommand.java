package zov.alphadlc.util.commands.defaults;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import zov.alphadlc.util.commands.api.Command;
import zov.alphadlc.util.commands.api.argument.IArgConsumer;
import zov.alphadlc.util.commands.api.exception.CommandException;

import java.util.List;
import java.util.stream.Stream;

public class TpCommand extends Command {

    public TpCommand() {
        super("tp");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String name = args.getString();

        MinecraftClient mc = MinecraftClient.getInstance();
        
        if (mc.world == null || mc.player == null) {
            logDirect(Formatting.RED + "Невозможно выполнить команду.");
            return;
        }

        PlayerEntity entityPlayer = mc.world.getPlayers().stream()
                .filter(player -> player.getName().getString().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);

        if (entityPlayer == null) {
            logDirect(Formatting.RED + "Игрок " + name + " не найден.");
            return;
        }

        double x = entityPlayer.getX();
        double y = entityPlayer.getY();
        double z = entityPlayer.getZ();
        
        mc.player.setPosition(x, y, z);
        
        logDirect("Телепортировано к игроку " + entityPlayer.getName().getString());
    }

    @Override
    public String getShortDesc() {
        return "Телепорт к игроку";
    }

    @Override
    public List<String> getLongDesc() {
        return List.of(
                "Телепортирует вас к указанному игроку",
                "",
                "> tp <имя> — телепорт к игроку"
        );
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        if (args.hasExactlyOne()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world != null) {
                return mc.world.getPlayers().stream()
                        .map(player -> player.getName().getString());
            }
        }
        return Stream.empty();
    }
}
