package zov.alphadlc.module.list.combat;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

final class AutoSwapRecognition {
    private AutoSwapRecognition() {
    }

    static boolean isSphere(ItemStack stack) {
        if (stack == null) return false;

        boolean playerHead = stack.isOf(Items.PLAYER_HEAD);
        var attributes = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        boolean customSphere = playerHead && attributes != null && !attributes.modifiers().isEmpty();
        return isSphereCandidate(customSphere, playerHead);
    }

    static boolean isSphereCandidate(boolean customSphere, boolean playerHead) {
        return customSphere || playerHead;
    }
}
