package zov.alphadlc.module.settings;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A mode-compatible setting whose values are item model identifiers.
 *
 * Keeping this as a {@link ModeSetting} preserves the existing config format,
 * while the preview stacks let the ClickGui render models without touching the
 * player's held item.
 */
public class ItemModelSetting extends ModeSetting {
    private final Map<String, ItemStack> previewStacks = new LinkedHashMap<>();

    public ItemModelSetting(String name, String defaultValue, String... models) {
        super(name, defaultValue, models);
        for (String model : models) {
            ItemStack preview = new ItemStack(Items.STICK);
            preview.set(DataComponentTypes.ITEM_MODEL, getModelId(model));
            previewStacks.put(model, preview);
        }
    }

    public Identifier getModelId(String model) {
        return Identifier.of("mre", "item_replacer/sword/" + model);
    }

    public ItemStack getPreviewStack(String model) {
        return previewStacks.get(model);
    }
}
