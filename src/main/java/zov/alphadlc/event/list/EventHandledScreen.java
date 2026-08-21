package zov.alphadlc.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;
import zov.alphadlc.event.Event;

@Getter
@AllArgsConstructor
public class EventHandledScreen extends Event {
    private final Slot slotHover;
    private final DrawContext drawContext;
    private final int mouseX;
    private final int mouseY;
}
