package zov.alphadlc.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import zov.alphadlc.event.Event;

@Getter
@AllArgsConstructor
public class EventPreHUD extends Event {
    private final DrawContext drawContext;
    private final RenderTickCounter renderTickCounter;
}
