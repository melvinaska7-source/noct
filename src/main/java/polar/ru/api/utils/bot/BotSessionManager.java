package polar.ru.api.utils.bot;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.session.Session;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import polar.ru.mixin.IMinecraftClientAccessor;

public final class BotSessionManager {
    private static final double BOT_ATTACK_RANGE = 4.5;
    private static final long BOT_CONNECT_RESOURCE_PACK_GRACE_MS = 10000L;
    private static final List<BotConnection> connections = new CopyOnWriteArrayList<BotConnection>();
    private static volatile boolean ignoreBotMessages;
    private static volatile boolean bypassResourcePacksDuringBotConnect;
    private static volatile long bypassResourcePacksUntilMs;
    private static volatile PendingBotConnect pendingBotConnect;

    public static List<BotConnection> getConnections() {
        BotSessionManager.pruneDeadConnections();
        return new ArrayList<BotConnection>(connections);
    }

    public static boolean shouldBypassResourcePacks() {
        return bypassResourcePacksDuringBotConnect || System.currentTimeMillis() < bypassResourcePacksUntilMs;
    }

    public static void finishBotConnectStage() {
        bypassResourcePacksDuringBotConnect = false;
        bypassResourcePacksUntilMs = System.currentTimeMillis() + 10000L;
        PendingBotConnect pending = pendingBotConnect;
        if (pending == null) {
            return;
        }
        MinecraftClient.getInstance().execute(() -> BotSessionManager.completePendingBotConnect(pending));
    }

    public static String getCurrentSessionName() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.getSession() == null ? "" : mc.getSession().getUsername();
    }

    public static List<String> getSessionNames(boolean includeCurrent) {
        String currentName;
        BotSessionManager.pruneDeadConnections();
        LinkedHashSet<String> names = new LinkedHashSet<String>();
        if (includeCurrent && !(currentName = BotSessionManager.getCurrentSessionName()).isBlank()) {
            names.add(currentName);
        }
        for (BotConnection bot : connections) {
            if (bot.name() == null || bot.name().isBlank()) continue;
            names.add(bot.name());
        }
        return new ArrayList<String>(names);
    }

    public static boolean toggleIgnoreBotMessages() {
        ignoreBotMessages = !ignoreBotMessages;
        return ignoreBotMessages;
    }

    public static boolean isIgnoreBotMessages() {
        return ignoreBotMessages;
    }

    public static void connect(String name, String address) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSession() == null || name == null || name.isBlank() || address == null || address.isBlank()) {
            return;
        }
        Session originalSession = mc.getSession();
        ServerInfo originalServerInfo = mc.getCurrentServerEntry();
        BotSessionManager.pruneDeadConnections();
        BotSessionManager.disconnectSessionsByName(name, (Text)Text.literal((String)"Replaced"));
        BotConnection previous = BotSessionManager.freezeCurrentSession();
        ((IMinecraftClientAccessor)mc).setSession(BotSessionManager.createSessionWithName(mc.getSession(), name));
        bypassResourcePacksDuringBotConnect = true;
        bypassResourcePacksUntilMs = System.currentTimeMillis() + 60000L;
        pendingBotConnect = new PendingBotConnect(previous, originalSession, originalServerInfo);
        mc.execute(() -> {
            try {
                ConnectScreen.connect((Screen)new MultiplayerScreen((Screen)new TitleScreen()), (MinecraftClient)mc, (ServerAddress)ServerAddress.parse((String)address), (ServerInfo)new ServerInfo(address, address, ServerInfo.ServerType.OTHER), (boolean)false, null);
            }
            catch (Exception ignored) {
                bypassResourcePacksDuringBotConnect = false;
                bypassResourcePacksUntilMs = 0L;
                pendingBotConnect = null;
                BotSessionManager.restoreAfterConnectFailure(mc, previous, originalSession, originalServerInfo);
            }
        });
    }

    public static boolean pulseBots(boolean rightClick) {
        boolean pulsed = false;
        for (BotConnection bot : connections) {
            if (!BotSessionManager.isConnectionUsable(bot)) continue;
            if (rightClick) {
                bot.handler().sendPacket((Packet)new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, bot.player().getYaw(), bot.player().getPitch()));
            } else {
                BotSessionManager.attackBotTarget(bot);
            }
            pulsed = true;
        }
        return pulsed;
    }

    public static void sayAll(String message) {
        for (BotConnection bot : connections) {
            if (!BotSessionManager.isConnectionUsable(bot)) continue;
            if (message.startsWith("/")) {
                bot.handler().sendChatCommand(message.substring(1));
                continue;
            }
            bot.handler().sendChatMessage(message);
        }
    }

    public static boolean control(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        BotSessionManager.pruneDeadConnections();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.world != null && name.equalsIgnoreCase(BotSessionManager.getCurrentSessionName())) {
            return true;
        }
        return connections.stream().filter(bot -> BotSessionManager.matchesName(bot.name(), name)).findFirst().map(bot -> {
            if (!BotSessionManager.isConnectionUsable(bot)) {
                connections.remove(bot);
                return false;
            }
            BotConnection previous = BotSessionManager.freezeCurrentSession();
            if (!BotSessionManager.activateSession(bot)) {
                if (previous != null && BotSessionManager.activateSession(previous)) {
                    connections.remove(previous);
                }
                return false;
            }
            connections.remove(bot);
            return true;
        }).orElse(false);
    }

    public static boolean say(String name, String message) {
        BotSessionManager.pruneDeadConnections();
        return connections.stream().filter(bot -> BotSessionManager.matchesName(bot.name(), name)).findFirst().map(bot -> {
            if (!BotSessionManager.isConnectionUsable(bot)) {
                connections.remove(bot);
                return false;
            }
            if (message.startsWith("/")) {
                bot.handler().sendChatCommand(message.substring(1));
            } else {
                bot.handler().sendChatMessage(message);
            }
            return true;
        }).orElse(false);
    }

    public static boolean remove(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return BotSessionManager.disconnectSessionsByName(name, (Text)Text.literal((String)"Removed")) > 0;
    }

    public static boolean restore() {
        return BotSessionManager.restore(null);
    }

    public static boolean restore(String name) {
        BotSessionManager.pruneDeadConnections();
        String targetName = name == null || name.isBlank() ? (connections.isEmpty() ? "" : connections.get(connections.size() - 1).name()) : name;
        return !targetName.isBlank() && BotSessionManager.control(targetName);
    }

    private static BotConnection freezeCurrentSession() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null || mc.world == null || mc.player == null) {
            return null;
        }
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        BotSessionManager.makeNettyBot(handler, mc.getSession().getUsername(), mc.player);
        BotConnection connection = new BotConnection(mc.getSession().getUsername(), mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "", handler.getConnection(), handler, mc.world, mc.player, mc.interactionManager, mc.getSession(), mc.getCurrentServerEntry());
        BotSessionManager.replaceConnection(connection);
        BotSessionManager.clearActiveSession(mc);
        return connection;
    }

    private static boolean activateSession(BotConnection bot) {
        if (!BotSessionManager.isConnectionUsable(bot)) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        IMinecraftClientAccessor accessor = (IMinecraftClientAccessor)mc;
        Channel channel = BotSessionManager.getChannel(bot.connection());
        if (channel != null && channel.pipeline().get("bot_filter") != null) {
            channel.pipeline().remove("bot_filter");
        }
        try {
            BotSessionManager.setMinecraftClientField(mc, ClientPlayNetworkHandler.class, bot.handler());
            accessor.setSession(bot.session() != null ? bot.session() : BotSessionManager.createSessionWithName(mc.getSession(), bot.name()));
            BotSessionManager.setMinecraftClientField(mc, ServerInfo.class, bot.serverInfo() != null ? bot.serverInfo() : BotSessionManager.createServerInfo(bot.name(), bot.address()));
            accessor.setItemUseCooldown(0);
            mc.world = bot.world();
            mc.player = bot.player();
            mc.cameraEntity = bot.player();
            mc.interactionManager = bot.interactionManager();
            if (mc.worldRenderer != null) {
                mc.worldRenderer.setWorld(bot.world());
            }
            bot.handler().sendPacket((Packet)new PlayerMoveC2SPacket.Full(bot.player().getX(), bot.player().getY(), bot.player().getZ(), bot.player().getYaw(), bot.player().getPitch(), bot.player().isOnGround(), bot.player().horizontalCollision));
            mc.setScreen(null);
            return true;
        }
        catch (Exception ignored) {
            return false;
        }
    }

    private static void completePendingBotConnect(PendingBotConnect pending) {
        if (pendingBotConnect != pending) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        BotConnection connectedBot = null;
        try {
            if (mc.getNetworkHandler() != null && mc.world != null && mc.player != null) {
                connectedBot = BotSessionManager.freezeCurrentSession();
            }
            if (pending.previous() != null && BotSessionManager.activateSession(pending.previous())) {
                connections.remove(pending.previous());
                pendingBotConnect = null;
                return;
            }
            if (connectedBot != null && BotSessionManager.activateSession(connectedBot)) {
                connections.remove(connectedBot);
                pendingBotConnect = null;
                return;
            }
            BotSessionManager.restoreAfterConnectFailure(mc, pending.previous(), pending.originalSession(), pending.originalServerInfo());
        }
        finally {
            pendingBotConnect = null;
        }
    }

    private static void attackBotTarget(BotConnection bot) {
        Entity target = BotSessionManager.findBotAttackTarget(bot);
        if (target != null) {
            if (bot.interactionManager() != null) {
                bot.interactionManager().attackEntity((PlayerEntity)bot.player(), target);
            } else {
                bot.handler().sendPacket((Packet)PlayerInteractEntityC2SPacket.attack((Entity)target, (boolean)false));
            }
        }
        bot.handler().sendPacket((Packet)new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    private static Entity findBotAttackTarget(BotConnection bot) {
        if (bot == null || bot.player() == null || bot.world() == null) {
            return null;
        }
        Vec3d eyePos = bot.player().getCameraPosVec(1.0f);
        Vec3d lookVec = bot.player().getRotationVec(1.0f);
        Vec3d reachVec = eyePos.add(lookVec.multiply(4.5));
        EntityHitResult result = ProjectileUtil.raycast((Entity)bot.player(), (Vec3d)eyePos, (Vec3d)reachVec, (Box)bot.player().getBoundingBox().expand(4.5), entity -> entity != bot.player() && entity.isAlive() && !entity.isRemoved(), (double)20.25);
        return result == null ? null : result.getEntity();
    }

    private static void clearActiveSession(MinecraftClient mc) {
        IMinecraftClientAccessor accessor = (IMinecraftClientAccessor)mc;
        BotSessionManager.setMinecraftClientField(mc, ClientPlayNetworkHandler.class, null);
        accessor.setItemUseCooldown(0);
        mc.world = null;
        mc.player = null;
        mc.cameraEntity = null;
        mc.interactionManager = null;
        if (mc.worldRenderer != null) {
            mc.worldRenderer.setWorld(null);
        }
    }

    private static void replaceConnection(BotConnection connection) {
        BotSessionManager.disconnectSessionsByName(connection.name(), (Text)Text.literal((String)"Replaced"));
        connections.add(connection);
    }

    private static void makeNettyBot(final ClientPlayNetworkHandler handler, final String name, final ClientPlayerEntity botPlayer) {
        Channel channel = BotSessionManager.getChannel(handler.getConnection());
        if (channel == null) {
            return;
        }
        if (channel.pipeline().get("bot_filter") != null) {
            channel.pipeline().remove("bot_filter");
        }
        if (channel.pipeline().get("packet_handler") == null) {
            return;
        }
        channel.pipeline().addBefore("packet_handler", "bot_filter", (ChannelHandler)new ChannelDuplexHandler(){

            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof KeepAliveS2CPacket) {
                    KeepAliveS2CPacket packet = (KeepAliveS2CPacket)msg;
                    handler.getConnection().send((Packet)new KeepAliveC2SPacket(packet.getId()));
                    if (botPlayer != null) {
                        handler.sendPacket((Packet)new PlayerMoveC2SPacket.OnGroundOnly(botPlayer.isOnGround(), botPlayer.horizontalCollision));
                    }
                    ReferenceCountUtil.release((Object)msg);
                    return;
                }
                if (msg instanceof CommonPingS2CPacket) {
                    CommonPingS2CPacket packet = (CommonPingS2CPacket)msg;
                    handler.getConnection().send((Packet)new CommonPongC2SPacket(packet.getParameter()));
                    ReferenceCountUtil.release((Object)msg);
                    return;
                }
                if (msg instanceof ResourcePackSendS2CPacket) {
                    ResourcePackSendS2CPacket packet = (ResourcePackSendS2CPacket)msg;
                    handler.sendPacket((Packet)new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.ACCEPTED));
                    handler.sendPacket((Packet)new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
                    ReferenceCountUtil.release((Object)msg);
                    return;
                }
                if (msg instanceof PlayerPositionLookS2CPacket) {
                    PlayerPositionLookS2CPacket packet = (PlayerPositionLookS2CPacket)msg;
                    BotSessionManager.applyFrozenPositionLook(botPlayer, packet);
                    handler.sendPacket((Packet)new TeleportConfirmC2SPacket(packet.teleportId()));
                    if (botPlayer != null) {
                        handler.sendPacket((Packet)new PlayerMoveC2SPacket.Full(botPlayer.getX(), botPlayer.getY(), botPlayer.getZ(), botPlayer.getYaw(), botPlayer.getPitch(), botPlayer.isOnGround(), botPlayer.horizontalCollision));
                    }
                    ReferenceCountUtil.release((Object)msg);
                    return;
                }
                if (msg instanceof EntityPositionSyncS2CPacket) {
                    EntityPositionSyncS2CPacket packet = (EntityPositionSyncS2CPacket)msg;
                    BotSessionManager.applyFrozenEntityPositionSync(botPlayer, packet);
                    ReferenceCountUtil.release((Object)msg);
                    return;
                }
                if (msg instanceof HealthUpdateS2CPacket) {
                    HealthUpdateS2CPacket packet = (HealthUpdateS2CPacket)msg;
                    if (botPlayer != null) {
                        botPlayer.setHealth(packet.getHealth());
                    }
                    ReferenceCountUtil.release((Object)msg);
                    return;
                }
                if (msg instanceof DisconnectS2CPacket) {
                    connections.removeIf(bot -> BotSessionManager.matchesName(bot.name(), name));
                    ctx.close();
                    ReferenceCountUtil.release((Object)msg);
                    return;
                }
                String packetName = msg.getClass().getSimpleName();
                if (packetName.contains("Sound") || packetName.contains("Particle") || packetName.contains("Screen") || ignoreBotMessages && BotSessionManager.isBotMessagePacket(packetName) || packetName.contains("Explosion") || packetName.contains("BossBar") || packetName.contains("Scoreboard") || packetName.contains("OverlayMessage")) {
                    ReferenceCountUtil.release((Object)msg);
                    return;
                }
                super.channelRead(ctx, msg);
            }

            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                connections.removeIf(bot -> BotSessionManager.matchesName(bot.name(), name));
                super.channelInactive(ctx);
            }

            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                connections.removeIf(bot -> BotSessionManager.matchesName(bot.name(), name));
                ctx.close();
            }
        });
    }

    private static boolean isBotMessagePacket(String packetName) {
        return packetName.contains("Chat") || packetName.contains("Message") || packetName.contains("Title") || packetName.contains("Overlay");
    }

    private static Channel getChannel(ClientConnection connection) {
        try {
            for (Field field : ClientConnection.class.getDeclaredFields()) {
                if (!Channel.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                return (Channel)field.get(connection);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private static void applyFrozenPositionLook(ClientPlayerEntity botPlayer, PlayerPositionLookS2CPacket packet) {
        if (botPlayer == null || packet == null) {
            return;
        }
        double x2 = BotSessionManager.readPacketDouble(packet, "x", botPlayer.getX());
        double y2 = BotSessionManager.readPacketDouble(packet, "y", botPlayer.getY());
        double z2 = BotSessionManager.readPacketDouble(packet, "z", botPlayer.getZ());
        float yaw = (float)BotSessionManager.readPacketDouble(packet, "yaw", botPlayer.getYaw());
        float pitch = (float)BotSessionManager.readPacketDouble(packet, "pitch", botPlayer.getPitch());
        Object change = BotSessionManager.readPacketComponent(packet, "change");
        if (change == null) {
            change = BotSessionManager.readPacketComponent(packet, "flags");
        }
        if (BotSessionManager.hasRelativeFlag(change, "X")) {
            x2 += botPlayer.getX();
        }
        if (BotSessionManager.hasRelativeFlag(change, "Y")) {
            y2 += botPlayer.getY();
        }
        if (BotSessionManager.hasRelativeFlag(change, "Z")) {
            z2 += botPlayer.getZ();
        }
        if (BotSessionManager.hasRelativeFlag(change, "Y_ROT")) {
            yaw += botPlayer.getYaw();
        }
        if (BotSessionManager.hasRelativeFlag(change, "X_ROT")) {
            pitch += botPlayer.getPitch();
        }
        pitch = MathHelper.clamp((float)pitch, (float)-90.0f, (float)90.0f);
        botPlayer.refreshPositionAndAngles(x2, y2, z2, yaw, pitch);
        botPlayer.setYaw(yaw);
        botPlayer.setPitch(pitch);
    }

    private static void applyFrozenEntityPositionSync(ClientPlayerEntity botPlayer, EntityPositionSyncS2CPacket packet) {
        if (botPlayer == null || packet == null || packet.id() != botPlayer.getId() || packet.values() == null) {
            return;
        }
        Vec3d position = packet.values().position();
        if (position == null) {
            return;
        }
        float yaw = packet.values().yaw();
        float pitch = MathHelper.clamp((float)packet.values().pitch(), (float)-90.0f, (float)90.0f);
        botPlayer.refreshPositionAndAngles(position.x, position.y, position.z, yaw, pitch);
        botPlayer.setYaw(yaw);
        botPlayer.setPitch(pitch);
        if (packet.values().deltaMovement() != null) {
            botPlayer.setVelocity(packet.values().deltaMovement());
        }
        botPlayer.setOnGround(packet.onGround());
    }

    private static double readPacketDouble(Object packet, String name, double fallback) {
        double d2;
        Object value = BotSessionManager.readPacketComponent(packet, name);
        if (value instanceof Number) {
            Number number = (Number)value;
            d2 = number.doubleValue();
        } else {
            d2 = fallback;
        }
        return d2;
    }

    private static Object readPacketComponent(Object packet, String name) {
        if (packet == null || name == null || name.isBlank()) {
            return null;
        }
        try {
            Method method = packet.getClass().getMethod(name, new Class[0]);
            method.setAccessible(true);
            return method.invoke(packet, new Object[0]);
        }
        catch (Exception method) {
            try {
                Method method2 = packet.getClass().getMethod("get" + Character.toUpperCase(name.charAt(0)) + name.substring(1), new Class[0]);
                method2.setAccessible(true);
                return method2.invoke(packet, new Object[0]);
            }
            catch (Exception method2) {
                try {
                    RecordComponent[] components = packet.getClass().getRecordComponents();
                    if (components != null) {
                        RecordComponent[] recordComponentArray = components;
                        int n2 = recordComponentArray.length;
                        for (int i2 = 0; i2 < n2; ++i2) {
                            RecordComponent component = recordComponentArray[i2];
                            if (!name.equals(component.getName())) continue;
                            return component.getAccessor().invoke(packet, new Object[0]);
                        }
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
                try {
                    for (Field field : packet.getClass().getDeclaredFields()) {
                        if (!name.equalsIgnoreCase(field.getName())) continue;
                        field.setAccessible(true);
                        return field.get(packet);
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
                return null;
            }
        }
    }

    private static boolean hasRelativeFlag(Object flags, String flagName) {
        Iterable iterable;
        block4: {
            block3: {
                if (!(flags instanceof Iterable)) break block3;
                iterable = (Iterable)flags;
                if (flagName != null) break block4;
            }
            return false;
        }
        for (Object flag : iterable) {
            Enum enumFlag;
            if (!(flag instanceof Enum) || !flagName.equals((enumFlag = (Enum)flag).name())) continue;
            return true;
        }
        return false;
    }

    private static Session createSessionWithName(Session current, String name) {
        try {
            Constructor constructor = Session.class.getDeclaredConstructor(String.class, UUID.class, String.class, Optional.class, Optional.class, Session.AccountType.class);
            constructor.setAccessible(true);
            return (Session)constructor.newInstance(name, UUID.randomUUID(), current == null ? "" : current.getAccessToken(), Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
        }
        catch (Exception e2) {
            throw new RuntimeException(e2);
        }
    }

    private static void setMinecraftClientField(MinecraftClient mc, Class<?> fieldType, Object value) {
        try {
            for (Field field : MinecraftClient.class.getDeclaredFields()) {
                if (field.getType() != fieldType) continue;
                field.setAccessible(true);
                field.set(mc, value);
                return;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static ServerInfo createServerInfo(String name, String address) {
        String safeAddress = address == null ? "" : address;
        String safeName = name == null || name.isBlank() ? safeAddress : name;
        return new ServerInfo(safeName, safeAddress, ServerInfo.ServerType.OTHER);
    }

    private static void restoreAfterConnectFailure(MinecraftClient mc, BotConnection previous, Session originalSession, ServerInfo originalServerInfo) {
        try {
            bypassResourcePacksDuringBotConnect = false;
            bypassResourcePacksUntilMs = 0L;
            if (previous != null && BotSessionManager.activateSession(previous)) {
                connections.remove(previous);
                return;
            }
            IMinecraftClientAccessor accessor = (IMinecraftClientAccessor)mc;
            accessor.setSession(originalSession);
            BotSessionManager.setMinecraftClientField(mc, ServerInfo.class, originalServerInfo);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static int disconnectSessionsByName(String name, Text reason) {
        if (name == null || name.isBlank()) {
            return 0;
        }
        int removed = 0;
        for (BotConnection bot : new ArrayList<BotConnection>(connections)) {
            if (!BotSessionManager.matchesName(bot.name(), name)) continue;
            connections.remove(bot);
            ++removed;
            try {
                if (bot.connection() == null) continue;
                bot.connection().disconnect(reason);
            }
            catch (Exception exception) {}
        }
        return removed;
    }

    private static void pruneDeadConnections() {
        connections.removeIf(bot -> !BotSessionManager.isConnectionUsable(bot));
    }

    private static boolean isConnectionUsable(BotConnection bot) {
        if (bot == null || bot.name() == null || bot.name().isBlank()) {
            return false;
        }
        if (bot.connection() == null || bot.handler() == null || bot.world() == null || bot.player() == null) {
            return false;
        }
        if (bot.player().networkHandler != bot.handler()) {
            return false;
        }
        Channel channel = BotSessionManager.getChannel(bot.connection());
        return channel == null || channel.isOpen();
    }

    private static boolean matchesName(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private record PendingBotConnect(BotConnection previous, Session originalSession, ServerInfo originalServerInfo) {
    }

    public static final class BotConnection {
        private final String name;
        private final String address;
        private final ClientConnection connection;
        private final ClientPlayNetworkHandler handler;
        private final ClientWorld world;
        private final ClientPlayerEntity player;
        private final ClientPlayerInteractionManager interactionManager;
        private final Session session;
        private final ServerInfo serverInfo;

        public BotConnection(String name, String address, ClientConnection connection, ClientPlayNetworkHandler handler, ClientWorld world, ClientPlayerEntity player, ClientPlayerInteractionManager interactionManager, Session session, ServerInfo serverInfo) {
            this.name = name;
            this.address = address;
            this.connection = connection;
            this.handler = handler;
            this.world = world;
            this.player = player;
            this.interactionManager = interactionManager;
            this.session = session;
            this.serverInfo = serverInfo;
        }

        public String name() {
            return this.name;
        }

        public String address() {
            return this.address;
        }

        public ClientConnection connection() {
            return this.connection;
        }

        public ClientPlayNetworkHandler handler() {
            return this.handler;
        }

        public ClientWorld world() {
            return this.world;
        }

        public ClientPlayerEntity player() {
            return this.player;
        }

        public ClientPlayerInteractionManager interactionManager() {
            return this.interactionManager;
        }

        public Session session() {
            return this.session;
        }

        public ServerInfo serverInfo() {
            return this.serverInfo;
        }
    }
}

