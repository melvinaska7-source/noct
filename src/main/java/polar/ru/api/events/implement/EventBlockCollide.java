package polar.ru.api.events.implement;

import net.minecraft.util.math.BlockPos;
import polar.ru.api.events.Event;

public class EventBlockCollide
extends Event {
    private final BlockPos pos;

    public EventBlockCollide(BlockPos pos) {
        this.pos = pos;
    }
    public BlockPos getPos() {
        return this.pos;
    }
}

