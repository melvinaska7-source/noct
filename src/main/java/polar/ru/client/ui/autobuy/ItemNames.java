package polar.ru.client.ui.autobuy;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

public final class ItemNames {
    private static final Map<String, String> RU_TO_ID = new LinkedHashMap<String, String>();
    private static final Map<String, String> ID_TO_RU = new HashMap<String, String>();

    private ItemNames() {
    }

    private static void put(String id, String ru) {
        ID_TO_RU.put(id, ru);
        RU_TO_ID.put(ru.toLowerCase(), id);
    }

    public static String toRussian(Item item) {
        String id = Registries.ITEM.getId(item).getPath();
        String ru = ID_TO_RU.get(id);
        return ru != null ? ru : item.getName().getString();
    }

    public static String toEnglishId(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        String q2 = query.toLowerCase();
        String exact = RU_TO_ID.get(q2);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, String> e2 : RU_TO_ID.entrySet()) {
            if (!q2.contains(e2.getKey())) continue;
            return e2.getValue();
        }
        return "";
    }

    static {
        ItemNames.put("stone_pickaxe", "Каменная кирка");
        ItemNames.put("iron_pickaxe", "Железная кирка");
        ItemNames.put("diamond_pickaxe", "Алмазная кирка");
        ItemNames.put("netherite_pickaxe", "Незеритовая кирка");
        ItemNames.put("golden_pickaxe", "Золотая кирка");
        ItemNames.put("wooden_pickaxe", "Деревянная кирка");
        ItemNames.put("stone_axe", "Каменный топор");
        ItemNames.put("iron_axe", "Железный топор");
        ItemNames.put("diamond_axe", "Алмазный топор");
        ItemNames.put("netherite_axe", "Незеритовый топор");
        ItemNames.put("golden_axe", "Золотой топор");
        ItemNames.put("wooden_axe", "Деревянный топор");
        ItemNames.put("stone_sword", "Каменный меч");
        ItemNames.put("iron_sword", "Железный меч");
        ItemNames.put("diamond_sword", "Алмазный меч");
        ItemNames.put("netherite_sword", "Незеритовый меч");
        ItemNames.put("golden_sword", "Золотой меч");
        ItemNames.put("wooden_sword", "Деревянный меч");
        ItemNames.put("stone_shovel", "Каменная лопата");
        ItemNames.put("iron_shovel", "Железная лопата");
        ItemNames.put("diamond_shovel", "Алмазная лопата");
        ItemNames.put("netherite_shovel", "Незеритовая лопата");
        ItemNames.put("diamond_helmet", "Алмазный шлем");
        ItemNames.put("diamond_chestplate", "Алмазный нагрудник");
        ItemNames.put("diamond_leggings", "Алмазные поножи");
        ItemNames.put("diamond_boots", "Алмазные ботинки");
        ItemNames.put("netherite_helmet", "Незеритовый шлем");
        ItemNames.put("netherite_chestplate", "Незеритовый нагрудник");
        ItemNames.put("netherite_leggings", "Незеритовые поножи");
        ItemNames.put("netherite_boots", "Незеритовые ботинки");
        ItemNames.put("diamond", "Алмаз");
        ItemNames.put("diamond_block", "Алмазный блок");
        ItemNames.put("iron_ingot", "Железный слиток");
        ItemNames.put("gold_ingot", "Золотой слиток");
        ItemNames.put("netherite_ingot", "Незеритовый слиток");
        ItemNames.put("ancient_debris", "Древние обломки");
        ItemNames.put("coal", "Уголь");
        ItemNames.put("stone", "Камень");
        ItemNames.put("dirt", "Земля");
        ItemNames.put("oak_log", "Дубовые брёвна");
        ItemNames.put("oak_planks", "Дубовые доски");
        ItemNames.put("cobblestone", "Булыжник");
        ItemNames.put("sand", "Песок");
        ItemNames.put("gravel", "Гравий");
        ItemNames.put("redstone", "Редстоун");
        ItemNames.put("lapis_lazuli", "Лазурит");
        ItemNames.put("emerald", "Изумруд");
        ItemNames.put("obsidian", "Обсидиан");
        ItemNames.put("bedrock", "Бедрок");
        ItemNames.put("netherrack", "Незерак");
        ItemNames.put("end_stone", "Камень Края");
        ItemNames.put("shield", "Щит");
        ItemNames.put("bow", "Лук");
        ItemNames.put("crossbow", "Арбалет");
        ItemNames.put("trident", "Трезубец");
        ItemNames.put("mace", "Булава");
        ItemNames.put("fishing_rod", "Удочка");
        ItemNames.put("shears", "Ножницы");
        ItemNames.put("book", "Книга");
        ItemNames.put("enchanted_book", "Зачарованная книга");
        ItemNames.put("map", "Карта");
        ItemNames.put("compass", "Компас");
        ItemNames.put("clock", "Часы");
        ItemNames.put("potion", "Зелье");
        ItemNames.put("bread", "Хлеб");
        ItemNames.put("apple", "Яблоко");
        ItemNames.put("golden_apple", "Золотое яблоко");
        ItemNames.put("enchanted_golden_apple", "Зачарованное золотое яблоко");
        ItemNames.put("ender_pearl", "Жемчуг Края");
        ItemNames.put("totem_of_undying", "Тотем бессмертия");
        ItemNames.put("elytra", "Элитры");
        RU_TO_ID.put("кирка", "pickaxe");
        RU_TO_ID.put("топор", "axe");
        RU_TO_ID.put("меч", "sword");
        RU_TO_ID.put("лопата", "shovel");
        RU_TO_ID.put("мотыга", "hoe");
        RU_TO_ID.put("шлем", "helmet");
        RU_TO_ID.put("нагрудник", "chestplate");
        RU_TO_ID.put("броня", "chestplate");
        RU_TO_ID.put("поножи", "leggings");
        RU_TO_ID.put("штаны", "leggings");
        RU_TO_ID.put("ботинки", "boots");
        RU_TO_ID.put("зелье", "potion");
        RU_TO_ID.put("блок", "block");
    }
}

