package polar.ru.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.client.modules.impl.misc.AhHelper;
import polar.ru.client.modules.impl.player.ItemScroller;
import polar.ru.client.modules.impl.render.ShulkerPreview;

@Mixin(value={HandledScreen.class})
public abstract class HandledScreenMixin {
    @Shadow
    @Nullable
    protected abstract Slot getSlotAt(double var1, double var3);

    @Shadow
    protected abstract void onMouseClick(@Nullable Slot var1, int var2, int var3, SlotActionType var4);

    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        boolean shiftPressed;
        MinecraftClient mc = MinecraftClient.getInstance();
        ItemScroller itemScroller = ItemScroller.INSTANCE;
        if (!itemScroller.isEnable() || mc.player == null || mc.interactionManager == null) {
            return;
        }
        long window = mc.getWindow().getHandle();
        boolean leftMousePressed = GLFW.glfwGetMouseButton((long)window, (int)0) == 1;
        boolean bl = shiftPressed = GLFW.glfwGetKey((long)window, (int)340) == 1 || GLFW.glfwGetKey((long)window, (int)344) == 1;
        if (!leftMousePressed || !shiftPressed) {
            itemScroller.resetTimer();
            return;
        }
        Slot slot = this.getSlotAt(mouseX, mouseY);
        if (slot == null || !slot.hasStack()) {
            return;
        }
        if (!itemScroller.canQuickMove()) {
            return;
        }
        this.onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
    }

    @Inject(method={"render"}, at={@At(value="RETURN")})
    private void polar$afterRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AhHelper.INSTANCE.renderFromMixin(context, mouseX, mouseY);
        ShulkerPreview shulkerPreview = ShulkerPreview.getInstance();
        if (shulkerPreview != null) {
            shulkerPreview.renderFromMixin(context, mouseX, mouseY);
        }
    }
}

