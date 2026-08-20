package polar.ru.api.storages.implement;

import java.util.ArrayList;
import java.util.List;
import polar.ru.polar;

public class FriendStorage {
    private final List<String> friends = new ArrayList<String>();

    public void add(String friend) {
        if (!friend.isEmpty()) {
            this.friends.add(friend);
            this.save();
        }
    }

    public void remove(String friend) {
        this.friends.remove(friend);
        this.save();
    }

    public void clear() {
        this.friends.clear();
        this.save();
    }

    public boolean isFriend(String friend) {
        return this.friends.contains(friend);
    }

    public boolean isEmpty() {
        return this.friends.isEmpty();
    }

    private void save() {
        try {
            polar.INSTANCE.configStorage.saveGlobals();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
    public List<String> getFriends() {
        return this.friends;
    }
}

