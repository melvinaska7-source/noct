package polar.ru.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import polar.ru.api.storages.implement.DragStorage;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.polar;

@Mixin(value={ChatScreen.class})
public class ChatScreenMixin {
    @Unique
    private boolean polar$leftPressed;

    @Inject(method={"mouseClicked"}, at={@At(value="HEAD")}, cancellable=true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (ModuleClass.interfaceModule.handleHudContextClick(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }
        for (Draggable draggable : DragStorage.draggables.values()) {
            if (!draggable.getModule().isEnable() || !draggable.onClick(mouseX, mouseY, button)) continue;
            cir.setReturnValue(true);
            return;
        }
    }

    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        boolean leftPressed;
        MinecraftClient mc = MinecraftClient.getInstance();
        Window window = mc.getWindow();
        boolean bl = leftPressed = GLFW.glfwGetMouseButton((long)mc.getWindow().getHandle(), (int)0) == 1;
        if (this.polar$leftPressed && !leftPressed) {
            for (Draggable draggable : DragStorage.draggables.values()) {
                draggable.onRelease(0);
            }
        }
        this.polar$leftPressed = leftPressed;
        for (Draggable draggable : DragStorage.draggables.values()) {
            if (!draggable.getModule().isEnable()) continue;
            draggable.onDraw(mouseX, mouseY, window, context.getMatrices());
        }
        ModuleClass.interfaceModule.renderHudContextMenu(context, mouseX, mouseY);
    }

    @Inject(method={"removed"}, at={@At(value="HEAD")})
    private void onRemoved(CallbackInfo ci) {
        this.polar$leftPressed = false;
        for (Draggable draggable : DragStorage.draggables.values()) {
            draggable.onRelease(0);
        }
        try {
            polar.INSTANCE.configStorage.saveConfig(polar.INSTANCE.configStorage.currentConfig);
        }
        catch (Exception e2) {
            e2.printStackTrace();
        }
    }
}

