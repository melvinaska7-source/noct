package polar.ru.client.modules.impl.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventTickPre;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayDeque;
import java.util.Deque;

public class InvSort extends Module {

    public static InvSort INSTANCE = new InvSort();

    public final BindSetting saveBind = new BindSetting("Save", 0);
    public final BindSetting loadBind = new BindSetting("Load", 0);
    public final FloatSetting delaySetting = new FloatSetting("Delay", 50f, 500f, 1f, 120f);

    private final Item[] savedLayout = new Item[36];
    private boolean hasSavedLayout = false;

    private final Deque<Runnable> queue = new ArrayDeque<>();
    private long lastActionTime = 0;

    private boolean saveWasPressed = false;
    private boolean loadWasPressed = false;

    public InvSort() {
        super("InvSort", "Сортировка инвентаря", ModuleCategory.PLAYER);
        this.addSettings(this.saveBind, this.loadBind, this.delaySetting);
        loadFromDisk();
    }

    @Override
    public void onDisable() {
        queue.clear();
    }

    @EventLink
    public void onTick(EventTickPre event) {
        if (mc.player == null) return;

        boolean savePressed = isBindPressed(this.saveBind.getKey());
        if (savePressed && !saveWasPressed) {
            saveInventory();
        }
        saveWasPressed = savePressed;

        boolean loadPressed = isBindPressed(this.loadBind.getKey());
        if (loadPressed && !loadWasPressed) {
            loadInventory();
        }
        loadWasPressed = loadPressed;

        if (queue.isEmpty()) return;
        long delay = (long) this.delaySetting.getValue();
        if (System.currentTimeMillis() - lastActionTime < delay) return;
        queue.poll().run();
        lastActionTime = System.currentTimeMillis();
    }

    private boolean isBindPressed(int keyCode) {
        if (keyCode <= 0) return false;
        long handle = mc.getWindow().getHandle();
        return InputUtil.isKeyPressed(handle, keyCode);
    }

    private void saveInventory() {
        if (mc.player == null) return;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            savedLayout[i] = stack.isEmpty() ? null : stack.getItem();
        }
        hasSavedLayout = true;
        saveToDisk();
        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal("§a[InvSort] §fРасклад сохранён"), false);
        }
    }

    private void loadInventory() {
        if (mc.player == null) return;
        if (!hasSavedLayout) {
            if (mc.player != null) {
                mc.player.sendMessage(net.minecraft.text.Text.literal("§c[InvSort] §fНет сохранённого расклада"), false);
            }
            return;
        }

        queue.clear();

        Item[] current = new Item[36];
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            current[i] = stack.isEmpty() ? null : stack.getItem();
        }

        for (int target = 0; target < 36; target++) {
            Item wanted = savedLayout[target];
            if (wanted == null) continue;
            if (current[target] == wanted) continue;

            int source = -1;
            for (int j = 0; j < 36; j++) {
                if (j == target) continue;
                if (current[j] == wanted) {
                    source = j;
                    break;
                }
            }
            if (source == -1) continue;

            int a = target, b = source;
            queue.add(() -> swapSlots(toScreenSlot(a), toScreenSlot(b)));

            Item tmp = current[target];
            current[target] = current[source];
            current[source] = tmp;
        }

        for (int i = 0; i < 36; i++) {
            Item item = current[i];
            if (item == null) continue;
            if (isPartOfSavedLayout(item)) continue;

            int slot = i;
            queue.add(() -> dropSlot(toScreenSlot(slot)));
            current[i] = null;
        }

        if (mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal("§a[InvSort] §fСортировка запущена..."), false);
        }
    }

    private boolean isPartOfSavedLayout(Item item) {
        for (Item saved : savedLayout) {
            if (saved == item) return true;
        }
        return false;
    }

    private int toScreenSlot(int invIndex) {
        return invIndex < 9 ? 36 + invIndex : invIndex;
    }

    private void swapSlots(int slotA, int slotB) {
        int syncId = mc.player.playerScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slotA, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(syncId, slotB, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(syncId, slotA, 0, SlotActionType.SWAP, mc.player);
    }

    private void dropSlot(int slot) {
        int syncId = mc.player.playerScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slot, 1, SlotActionType.THROW, mc.player);
    }

    private File getSaveDirectory() {
        File dir = new File(MinecraftClient.getInstance().runDirectory, "polar" + File.separator + "inv");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void saveToDisk() {
        try {
            String[] ids = new String[36];
            for (int i = 0; i < 36; i++) {
                Item item = savedLayout[i];
                ids[i] = item == null ? null : Registries.ITEM.getId(item).toString();
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            File file = new File(getSaveDirectory(), "layout.json");
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(ids, writer);
            }
        } catch (Exception ignored) {
        }
    }

    private void loadFromDisk() {
        try {
            File file = new File(getSaveDirectory(), "layout.json");
            if (!file.exists()) return;

            Gson gson = new Gson();
            try (FileReader reader = new FileReader(file)) {
                String[] ids = gson.fromJson(reader, String[].class);
                if (ids == null || ids.length != 36) return;
                for (int i = 0; i < 36; i++) {
                    savedLayout[i] = ids[i] == null ? null : Registries.ITEM.get(Identifier.of(ids[i]));
                }
                hasSavedLayout = true;
            }
        } catch (Exception ignored) {
        }
    }
}
