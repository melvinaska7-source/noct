package zov.alphadlc.module.list.render;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Identifier;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.ItemModelSetting;

@ModuleInformation(moduleName = "Item Replacer", moduleDesc = "???????? ?????? ???? ?????", moduleCategory = ModuleCategory.RENDER)
public class ItemReplacer extends Module {
    private static final String[] MODELS = {
            "abominableblade",
            "abominablegreatsaber",
            "abominablescythe",
            "aciddemon",
            "amethyst_shuriken",
            "ancient_royal_great_sword",
            "aquantic_sacred_blade",
            "aquantictrident",
            "arcanethyst",
            "ashura_blade",
            "awakened_lichblade",
            "bloodedge",
            "bloodydeath",
            "bramblethorn",
            "brimstone_claymore",
            "cariansword",
            "chrono_blade",
            "corruptedmythicblade",
            "creationsplitter",
            "crescentrose",
            "cyberkatana",
            "cybermantisblade",
            "cybernetickatana",
            "cyberneticknife",
            "cyberneticsawblade",
            "cybersword",
            "dainsleif",
            "dark_blade",
            "dark_cleaver",
            "death_knight_dagger",
            "death_knight_sword",
            "demigodsunholyblade",
            "demigodsunholyhalberd",
            "demonicblade",
            "demoniccleaver",
            "demonlordsgreataxe",
            "demonlordsword",
            "divine_justice",
            "divine_reaper",
            "divineaxerhitta",
            "divinepunisher",
            "dragonslayingblade",
            "edgeoftheastralplane",
            "emberblade",
            "enigma",
            "epicsword",
            "estoc",
            "excalibur",
            "fallengodspear",
            "fallengodsword",
            "floral_longsword",
            "floral_sabre",
            "forest_guardian_glaive",
            "frostaxe",
            "frostblade",
            "frostscythe",
            "greenscythe",
            "hearthflame",
            "herosword",
            "holymoonlightsword",
            "hornetsneedle",
            "icewhisper",
            "jadehalberd",
            "katana",
            "legendarysword",
            "longsword",
            "magiscythe",
            "masamune",
            "mjolnir",
            "moltenblade",
            "moltensword",
            "muramasa",
            "mysticalspellblade",
            "mythicblade",
            "partisan",
            "pharaohs_treasure",
            "pheonixgrace",
            "powerfusehammer",
            "powerfusesword",
            "requiem_of_hell",
            "ribboncleaver",
            "righteous_relic",
            "riversofblood",
            "royalchakram",
            "royalrapier",
            "sabre",
            "scissorblade",
            "sculkcleaver",
            "sculkscythe",
            "sculksword",
            "sentinels_will",
            "silverine_blade",
            "soul_collector",
            "soul_devourer",
            "soulclaws",
            "souledge",
            "soulharvester",
            "soulrender",
            "soulstealer",
            "stars_edge",
            "steelsword",
            "stop_sign",
            "stormbringer",
            "storms_edge",
            "sunbreak",
            "tengensblade",
            "terrablade",
            "thousanddemondaggers",
            "thunderbrand",
            "thunderbringer",
            "toxic_longsword",
            "vampiricneedle",
            "wakizashi",
            "watcher_claymore",
            "watching_warglaive",
            "waxweaver",
            "whisperwind",
            "wickpiercer",
            "yoru"
    };

    public final ItemModelSetting model = new ItemModelSetting("??????", MODELS[0], MODELS);

    public Identifier getSelectedModelId() {
        return model.getModelId(model.getValue());
    }

    public ItemStack apply(ItemStack original) {
        if (!isEnabled() || original.isEmpty() || !(original.getItem() instanceof SwordItem)) return original;
        ItemStack replacement = original.copy();
        replacement.set(DataComponentTypes.ITEM_MODEL, getSelectedModelId());
        return replacement;
    }
}
