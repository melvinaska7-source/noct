package polar.ru.api.commands.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import polar.ru.api.commands.Command;

public class VClipCommand
extends Command {
    public VClipCommand() {
        super("vclip");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(this.arg("Y", IntegerArgumentType.integer()).executes(context -> {
            int y2 = (Integer)context.getArgument("Y", Integer.class);
            VClipCommand.mc.player.setPosition(VClipCommand.mc.player.getX(), VClipCommand.mc.player.getY() + (double)y2, VClipCommand.mc.player.getZ());
            return 1;
        }));
        builder.then(this.literal("up").executes(context -> {
            this.clipToSafeBlock(true);
            return 1;
        }));
        builder.then(this.literal("down").executes(context -> {
            this.clipToSafeBlock(false);
            return 1;
        }));
    }

    private void clipToSafeBlock(boolean up) {
        if (VClipCommand.mc.player == null || VClipCommand.mc.world == null) {
            return;
        }
        int startY = VClipCommand.mc.player.getBlockY();
        int minY = VClipCommand.mc.world.getBottomY();
        int maxY = VClipCommand.mc.world.getTopYInclusive() - 2;
        int step = up ? 1 : -1;
        int from = up ? startY + 1 : startY - 1;
        int to = up ? maxY : minY;
        int y2 = from;
        while (up ? y2 <= to : y2 >= to) {
            if (this.isSafeStandPosition(y2)) {
                VoxelShape shape = VClipCommand.mc.world.getBlockState(new BlockPos(VClipCommand.mc.player.getBlockX(), y2 - 1, VClipCommand.mc.player.getBlockZ())).getCollisionShape((BlockView)VClipCommand.mc.world, new BlockPos(VClipCommand.mc.player.getBlockX(), y2 - 1, VClipCommand.mc.player.getBlockZ()));
                double offsetY = shape.isEmpty() ? 0.0 : shape.getMax(Direction.Axis.Y);
                VClipCommand.mc.player.setPosition(VClipCommand.mc.player.getX(), (double)y2 + offsetY, VClipCommand.mc.player.getZ());
                return;
            }
            y2 += step;
        }
    }

    private boolean isSafeStandPosition(int y2) {
        BlockPos floorPos = new BlockPos(VClipCommand.mc.player.getBlockX(), y2 - 1, VClipCommand.mc.player.getBlockZ());
        BlockPos feetPos = floorPos.up();
        BlockPos headPos = feetPos.up();
        BlockState floorState = VClipCommand.mc.world.getBlockState(floorPos);
        if (floorState.getCollisionShape((BlockView)VClipCommand.mc.world, floorPos).isEmpty()) {
            return false;
        }
        return VClipCommand.mc.world.getBlockState(feetPos).getCollisionShape((BlockView)VClipCommand.mc.world, feetPos).isEmpty() && VClipCommand.mc.world.getBlockState(headPos).getCollisionShape((BlockView)VClipCommand.mc.world, headPos).isEmpty();
    }
}

