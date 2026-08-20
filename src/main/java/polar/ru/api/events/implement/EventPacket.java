package polar.ru.api.events.implement;

import net.minecraft.network.packet.Packet;
import polar.ru.api.events.Event;

public class EventPacket
extends Event {
    private final Packet<?> packet;
    private final Type type;
    public EventPacket(Packet<?> packet, Type type) {
        this.packet = packet;
        this.type = type;
    }
    public Packet<?> getPacket() {
        return this.packet;
    }
    public Type getType() {
        return this.type;
    }

    public static enum Type {
        SEND,
        RECEIVE;

    }
}

