package polar.ru.api.commands.impl;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import polar.ru.api.commands.Command;
import polar.ru.api.utils.chat.ChatUtils;

public class TPCommand
extends Command {
    private static Thread activeTpThread = null;

    public TPCommand() {
        super("tp");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(this.arg("x", DoubleArgumentType.doubleArg()).then(this.arg("y", DoubleArgumentType.doubleArg()).then(this.arg("z", DoubleArgumentType.doubleArg()).executes(ctx -> {
            double x2 = DoubleArgumentType.getDouble((CommandContext)ctx, (String)"x");
            double y2 = DoubleArgumentType.getDouble((CommandContext)ctx, (String)"y");
            double z2 = DoubleArgumentType.getDouble((CommandContext)ctx, (String)"z");
            this.startTeleport(x2, y2, z2);
            return 1;
        }))));
    }

    private void startTeleport(double targetX, double targetY, double targetZ) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        if (activeTpThread != null && activeTpThread.isAlive()) {
            activeTpThread.interrupt();
        }
        activeTpThread = new Thread(() -> {
            try {
                double startX = mc.player.getX();
                double startY = mc.player.getY();
                double startZ = mc.player.getZ();
                double dx = targetX - startX;
                double dy = targetY - startY;
                double dz = targetZ - startZ;
                double totalDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (totalDistance == 0.0) {
                    return;
                }
                double stepSize = 0.25;
                int totalSteps = (int)Math.ceil(totalDistance / stepSize);
                double stepX = dx / (double)totalSteps;
                double stepY = dy / (double)totalSteps;
                double stepZ = dz / (double)totalSteps;
                double currentX = startX;
                double currentY = startY;
                double currentZ = startZ;
                for (int i2 = 1; i2 <= totalSteps; ++i2) {
                    if (Thread.currentThread().isInterrupted() || mc.player == null) {
                        return;
                    }
                    double px = currentX += stepX;
                    double py = currentY += stepY;
                    double pz = currentZ += stepZ;
                    mc.execute(() -> {
                        if (mc.player != null && mc.getNetworkHandler() != null) {
                            mc.player.setPosition(px, py, pz);
                            mc.getNetworkHandler().sendPacket((Packet)new PlayerMoveC2SPacket.PositionAndOnGround(px, py, pz, false, false));
                        }
                    });
                    if (i2 % 4 != 0) continue;
                    Thread.sleep(10L);
                }
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.player.setPosition(targetX, targetY, targetZ);
                        ChatUtils.sendMessage("Телепорт завершен!");
                    }
                });
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
        });
        activeTpThread.start();
        ChatUtils.sendMessage("Попытка перемещения к: " + targetX + ", " + targetY + ", " + targetZ);
    }
}

