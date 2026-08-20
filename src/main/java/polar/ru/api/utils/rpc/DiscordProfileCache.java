package polar.ru.api.utils.rpc;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import polar.ru.api.QClient;

public final class DiscordProfileCache
implements QClient {
    public static final Identifier AVATAR_TEXTURE_ID = Identifier.of((String)"polar", (String)"discord_avatar");
    private static volatile String username = "";
    private static volatile boolean avatarReady;
    private static volatile String lastAvatarUrl;

    private DiscordProfileCache() {
    }

    public static void onReady(String userId, String discordUsername, String avatarHash) {
        String avatarUrl;
        if (discordUsername != null && !discordUsername.isEmpty()) {
            username = discordUsername;
        }
        if ((avatarUrl = DiscordProfileCache.buildAvatarUrl(userId, avatarHash)) == null || avatarUrl.isEmpty()) {
            return;
        }
        if (avatarUrl.equals(lastAvatarUrl) && avatarReady) {
            return;
        }
        lastAvatarUrl = avatarUrl;
        avatarReady = false;
        new Thread(() -> DiscordProfileCache.loadAvatar(avatarUrl), "Discord-Avatar-Loader").start();
    }

    public static String getUsername() {
        return username;
    }

    public static String getDisplayUsername() {
        String sessionName;
        if (username != null && !username.isEmpty()) {
            return username;
        }
        if (mc != null && mc.getSession() != null && (sessionName = mc.getSession().getUsername()) != null && !sessionName.isEmpty()) {
            return sessionName;
        }
        return "Player";
    }

    public static boolean hasAvatar() {
        return avatarReady;
    }

    public static Identifier getAvatarTexture() {
        return AVATAR_TEXTURE_ID;
    }

    private static String buildAvatarUrl(String userId, String avatarHash) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        if (avatarHash != null && !avatarHash.isEmpty()) {
            return "https://cdn.discordapp.com/avatars/" + userId + "/" + avatarHash + ".png?size=64";
        }
        int index = 0;
        try {
            index = (int)(Long.parseLong(userId) % 5L);
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
        return "https://cdn.discordapp.com/embed/avatars/" + index + ".png";
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static void loadAvatar(String avatarUrl) { if (true) return; // Disabled HTTP avatar fetch
        try {
            URL url = URI.create(avatarUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "PolarClient/1.0");
            try (InputStream input = connection.getInputStream();){
                NativeImage image = NativeImage.read((InputStream)input);
                if (image == null) {
                    return;
                }
                if (mc == null) {
                    image.close();
                    return;
                }
                mc.execute(() -> DiscordProfileCache.registerAvatarTexture(image));
                return;
            }
            finally {
                connection.disconnect();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static void registerAvatarTexture(NativeImage image) {
        if (mc == null) {
            image.close();
            return;
        }
        try {
            if (mc.getTextureManager().getTexture(AVATAR_TEXTURE_ID) != null) {
                mc.getTextureManager().destroyTexture(AVATAR_TEXTURE_ID);
            }
            mc.getTextureManager().registerTexture(AVATAR_TEXTURE_ID, (AbstractTexture)new NativeImageBackedTexture(image));
            avatarReady = true;
        }
        catch (Exception ignored) {
            image.close();
            avatarReady = false;
        }
    }

    static {
        lastAvatarUrl = "";
    }
}

