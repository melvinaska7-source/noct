package polar.ru.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.api.events.implement.EventPacket;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.baritone.BaritoneAntiStuck;
import polar.ru.api.utils.bot.BotSessionManager;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.client.modules.impl.render.WorldTweaks;
import polar.ru.client.modules.impl.render.base.implement.Cooldowns;
import polar.ru.polar;

@Mixin(value={ClientPlayNetworkHandler.class})
public abstract class ClientPlayNetworkHandlerMixin {
    @Shadow
    private ClientWorld world;

    @Inject(method={"sendChatMessage"}, at={@At(value="HEAD")}, cancellable=true)
    public void sendChatMessage(@NotNull String message, CallbackInfo ci) {
        if (message.startsWith(polar.INSTANCE.commandStorage.getPrefix())) {
            try {
                polar.INSTANCE.commandStorage.getDispatcher().execute(message.substring(polar.INSTANCE.commandStorage.getPrefix().length()), (CommandSource)polar.INSTANCE.commandStorage.getSource());
            }
            catch (CommandSyntaxException e2) {
                ChatUtils.sendMessage(String.valueOf(Formatting.RED) + "Ошибка в использовании!");
            }
            ci.cancel();
            return;
        }
    }

    @Inject(method={"onEntityVelocityUpdate"}, at={@At(value="HEAD")}, cancellable=true)
    private void onVelocityUpdate(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        EventPacket event = new EventPacket((Packet<?>)packet, EventPacket.Type.RECEIVE);
        event.call();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method={"onExplosion"}, at={@At(value="HEAD")}, cancellable=true)
    private void onExplosion(ExplosionS2CPacket packet, CallbackInfo ci) {
        EventPacket event = new EventPacket((Packet<?>)packet, EventPacket.Type.RECEIVE);
        event.call();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method={"onEntityPositionSync"}, at={@At(value="HEAD")}, cancellable=true)
    private void onEntityPositionSync(EntityPositionSyncS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (this.world == null || mc.player == null || mc.world == null) {
            ci.cancel();
        }
    }

    @Inject(method={"onGameMessage"}, at={@At(value="HEAD")})
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        BaritoneAntiStuck.onGameMessage(packet.content().getString());
        Cooldowns.onGameMessage(packet.content().getString());
    }

    @Inject(method={"onGameJoin"}, at={@At(value="TAIL")})
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        BotSessionManager.finishBotConnectStage();
    }

    @Inject(method={"onWorldTimeUpdate"}, at={@At(value="TAIL")})
    private void polar$onWorldTimeUpdate(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        if (this.world == null || ModuleClass.INSTANCE == null) {
            return;
        }
        WorldTweaks tweaks = ModuleClass.worldTweaks;
        if (tweaks != null && tweaks.isTimeEnabled()) {
            this.world.getLevelProperties().setTimeOfDay(tweaks.getForcedTime());
        }
    }
}

