package zov.alphadlc.util.friend;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.entity.player.PlayerEntity;
import zov.alphadlc.util.QuickLogger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class FriendRepository implements QuickLogger {

    public static final int FRIEND_COLOR = 0xFF00FF00;

    private static final File file = new File("alphadlc/friends.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final CaseInsensitiveNameIndex<Friend> friends = new CaseInsensitiveNameIndex<>(Friend::name);

    public static void addFriend(String name) {
        friends.add(new Friend(name));
    }

    public static void removeFriend(String name) {
        friends.remove(name);
    }

    public static boolean shouldAttack(PlayerEntity player) {
        return !isFriend(player.getNameForScoreboard());
    }

    public static boolean isFriend(String friend) {
        return friends.contains(friend);
    }

    public static Friend getFriend(String name) {
        return friends.get(name);
    }

    public static List<Friend> getFriends() {
        return friends.values();
    }

    public static void clear() {
        friends.clear();
    }

    public static void save() {
        try {
            file.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(friends.values(), writer);
            }
        } catch (IOException e) {
        }
    }

    public static void load() {
        if (!file.exists()) return;

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<Friend>>() {}.getType();
            List<Friend> loaded = gson.fromJson(reader, listType);
            if (loaded != null) {
                friends.replaceAll(loaded);
            }
        } catch (IOException e) {
        }
    }
}