package polar.ru.api.events.implement;

import polar.ru.api.events.Event;

public class EventChunkUpdate
extends Event {
    private final int chunkX;
    private final int chunkZ;
    public int getChunkX() {
        return this.chunkX;
    }
    public int getChunkZ() {
        return this.chunkZ;
    }
    public EventChunkUpdate(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }
}

