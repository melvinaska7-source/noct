package polar.ru.api.utils.rpc;

import java.util.Arrays;
import net.minecraft.client.network.ServerInfo;
import polar.ru.api.QClient;
import polar.ru.api.utils.rpc.DiscordProfileCache;
import polar.ru.api.utils.rpc.utils.DiscordEventHandlers;
import polar.ru.api.utils.rpc.utils.DiscordRPC;
import polar.ru.api.utils.rpc.utils.DiscordRichPresence;

public class DiscordManager
implements QClient {
    private DiscordDaemonThread discordDaemonThread;
    private long APPLICATION_ID;
    private boolean running;
    private String image;
    private String site;
    private String discord;
    public static DiscordRichPresence discordRichPresence = new DiscordRichPresence();
    public static DiscordRPC discordRPC = DiscordRPC.INSTANCE;

    private void cppInit() {
        this.discordDaemonThread = new DiscordDaemonThread();
        this.APPLICATION_ID = 1518324607998885908L;
        this.running = true;
        this.image = "";
        this.site = "";
        this.discord = "";
    }

    public void init() {
        this.cppInit();
        if (DiscordRPC.INSTANCE == null) {
            System.out.println("Discord RPC native library not found. Discord integration disabled.");
            this.running = false;
            return;
        }
        try {
            DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().ready(user -> {
                if (user == null) {
                    return;
                }
                String userId = user.userId;
                String name = user.username;
                String avatar = user.avatar;
                DiscordProfileCache.onReady(userId, name, avatar);
            }).build();
            DiscordRPC.INSTANCE.Discord_Initialize(String.valueOf(this.APPLICATION_ID), handlers, true, "");
            DiscordManager.discordRichPresence.startTimestamp = System.currentTimeMillis() / 1000L;
            discordRPC.Discord_UpdatePresence(discordRichPresence);
            new Thread(() -> {
                while (this.running) {
                    try {
                        String playerName = DiscordProfileCache.getDisplayUsername();
                        DiscordManager.discordRichPresence.details = "Name - " + playerName;
                        DiscordManager.discordRichPresence.state = this.getServerDisplayName();
                        DiscordManager.discordRichPresence.largeImageKey = this.image;
                        DiscordManager.discordRichPresence.button_label_1 = "Купить";
                        DiscordManager.discordRichPresence.button_url_1 = this.site;
                        DiscordManager.discordRichPresence.button_label_2 = "Дискорд";
                        DiscordManager.discordRichPresence.button_url_2 = this.discord;
                        DiscordRPC.INSTANCE.Discord_UpdatePresence(discordRichPresence);
                        Thread.sleep(2000L);
                    }
                    catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }, "Discord-RPC-Updater").start();
            this.discordDaemonThread.start();
        }
        catch (UnsatisfiedLinkError e2) {
            System.out.println("Discord RPC native library not found. Discord integration disabled.");
            this.running = false;
        }
    }

    public DiscordManager start() {
        this.init();
        return this;
    }

    private String getServerDisplayName() {
        String[] parts;
        if (mc == null) {
            return "Idle";
        }
        ServerInfo info = mc.getCurrentServerEntry();
        if (info == null || info.address == null || info.address.isEmpty()) {
            return "Singleplayer";
        }
        String host = info.address;
        int portIndex = host.indexOf(58);
        if (portIndex > 0) {
            host = host.substring(0, portIndex);
        }
        if ((parts = host.split("\\.")).length >= 3) {
            return String.join((CharSequence)".", Arrays.copyOfRange(parts, 1, parts.length));
        }
        return host;
    }

    public void stopRPC() {
        this.running = false;
        DiscordRPC.INSTANCE.Discord_Shutdown();
        if (this.discordDaemonThread != null) {
            this.discordDaemonThread.interrupt();
        }
    }
    public DiscordDaemonThread getDiscordDaemonThread() {
        return this.discordDaemonThread;
    }
    public long getAPPLICATION_ID() {
        return this.APPLICATION_ID;
    }
    public boolean isRunning() {
        return this.running;
    }
    public String getImage() {
        return this.image;
    }
    public String getSite() {
        return this.site;
    }
    public String getDiscord() {
        return this.discord;
    }

    private class DiscordDaemonThread
    extends Thread {
        private DiscordDaemonThread() {
        }

        @Override
        public void run() {
            this.setName("Discord-RPC");
            try {
                while (DiscordManager.this.running) {
                    DiscordRPC.INSTANCE.Discord_RunCallbacks();
                    Thread.sleep(2000L);
                }
            }
            catch (Exception exception) {
                DiscordManager.this.stopRPC();
            }
            super.run();
        }
    }
}

