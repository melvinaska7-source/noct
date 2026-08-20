package polar.ru.api.commands.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.command.CommandSource;
import polar.ru.api.commands.Command;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.api.utils.cmd.waypoint.Waypoint;
import polar.ru.polar;

public class GPSCommand
extends Command {
    public GPSCommand() {
        super("gps");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        ((LiteralArgumentBuilder)builder.then(this.arg("X", IntegerArgumentType.integer()).then(this.arg("Z", IntegerArgumentType.integer()).executes(context -> {
            int x2 = (Integer)context.getArgument("X", Integer.class);
            int z2 = (Integer)context.getArgument("Z", Integer.class);
            Waypoint waypoint = new Waypoint(x2, z2);
            polar.INSTANCE.waypointStorage.set(waypoint);
            ChatUtils.sendMessage(I18n.translate((String)"Метка поставлена: ", (Object[])new Object[]{x2, z2}));
            return 1;
        })))).then(this.literal("remove").executes(context -> {
            if (!polar.INSTANCE.waypointStorage.isEmpty()) {
                polar.INSTANCE.waypointStorage.clear();
                ChatUtils.sendMessage(I18n.translate((String)"Метка удалена!", (Object[])new Object[0]));
            } else {
                ChatUtils.sendMessage(I18n.translate((String)"Метки не было", (Object[])new Object[0]));
            }
            return 1;
        }));
    }
}

