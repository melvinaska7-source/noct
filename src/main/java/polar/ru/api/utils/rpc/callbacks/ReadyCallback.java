package polar.ru.api.utils.rpc.callbacks;

import com.sun.jna.Callback;
import polar.ru.api.utils.rpc.utils.DiscordUser;

public interface ReadyCallback
extends Callback {
    public void apply(DiscordUser var1);
}

