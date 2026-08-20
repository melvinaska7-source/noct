package polar.ru.client.modules.impl.misc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.Box;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.client.modules.Module;

public class PotionTracker
extends Module {
    public static PotionTracker INSTANCE = new PotionTracker();
    private static final double TRACK_RADIUS = 50.0;
    private static final double SPLASH_RADIUS = 4.0;
    private static final double SPLASH_HEIGHT = 2.0;
    private static final int MAX_MESSAGES = 4;
    private static final int GRAY = new Color(200, 200, 200).getRGB();
    private static final int PLAYER = new Color(235, 235, 235).getRGB();
    private final Map<Integer, PotionData> trackedPotions = new HashMap<Integer, PotionData>();
    private ClientWorld lastWorld;

    public PotionTracker() {
        super("PotionTracker", "Показывает попадание выкинутых зелий по игрокам", Module.ModuleCategory.MISC);
    }

    @Override
    public void onDisable() {
        this.trackedPotions.clear();
        this.lastWorld = null;
        super.onDisable();
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (PotionTracker.mc.player == null || PotionTracker.mc.world == null) {
            this.trackedPotions.clear();
            this.lastWorld = null;
            return;
        }
        if (this.lastWorld != PotionTracker.mc.world) {
            this.trackedPotions.clear();
            this.lastWorld = PotionTracker.mc.world;
        }
        HashSet<Integer> currentPotions = new HashSet<Integer>();
        double trackRadiusSq = 2500.0;
        Box searchBox = PotionTracker.mc.player.getBoundingBox().expand(50.0);
        for (PotionEntity potionEntity : PotionTracker.mc.world.getEntitiesByClass(PotionEntity.class, searchBox, Entity::isAlive)) {
            PotionInfo potionInfo;
            if (PotionTracker.mc.player.squaredDistanceTo((Entity)potionEntity) > trackRadiusSq || (potionInfo = this.getPotionInfo(potionEntity)) == null) continue;
            int entityId = potionEntity.getId();
            currentPotions.add(entityId);
            PotionData data = this.trackedPotions.get(entityId);
            if (data == null) {
                this.trackedPotions.put(entityId, new PotionData(potionInfo, potionEntity.getX(), potionEntity.getY(), potionEntity.getZ()));
                continue;
            }
            data.lastX = potionEntity.getX();
            data.lastY = potionEntity.getY();
            data.lastZ = potionEntity.getZ();
            data.potionInfo = potionInfo;
        }
        HashSet<Integer> removedPotions = new HashSet<Integer>(this.trackedPotions.keySet());
        removedPotions.removeAll(currentPotions);
        Iterator iterator = removedPotions.iterator();
        while (iterator.hasNext()) {
            int entityId = (Integer)iterator.next();
            PotionData data = this.trackedPotions.remove(entityId);
            if (data == null) continue;
            this.printSplash(data);
        }
    }

    private void printSplash(PotionData data) {
        Box potionBox = new Box(data.lastX - 4.0, data.lastY - 2.0, data.lastZ - 4.0, data.lastX + 4.0, data.lastY + 2.0, data.lastZ + 4.0);
        ArrayList<PlayerHit> hits = new ArrayList<PlayerHit>();
        for (PlayerEntity player : PotionTracker.mc.world.getPlayers()) {
            double dz;
            double dx;
            double distance;
            if (player == null || !player.isAlive() || !potionBox.contains(player.getPos()) || (distance = Math.sqrt((dx = player.getX() - data.lastX) * dx + (dz = player.getZ() - data.lastZ) * dz)) > 4.0) continue;
            double proximity = Math.max(0.0, 1.0 - distance / 4.0);
            int percent = Math.max(1, Math.min(100, (int)Math.round(proximity * 100.0)));
            hits.add(new PlayerHit(player.getName().getString(), percent, distance));
        }
        hits.sort(Comparator.comparingDouble(PlayerHit::distance));
        for (int i2 = 0; i2 < Math.min(4, hits.size()); ++i2) {
            PlayerHit hit = (PlayerHit)hits.get(i2);
            this.sendPotionMessage(hit.playerName(), data.potionInfo, hit.percent());
        }
    }

    private PotionInfo getPotionInfo(PotionEntity potionEntity) {
        PotionContentsComponent contents = (PotionContentsComponent)potionEntity.getStack().get(DataComponentTypes.POTION_CONTENTS);
        PotionInfo byEffects = this.getPotionInfo(contents);
        if (byEffects != null) {
            return byEffects;
        }
        return this.getPotionInfo(potionEntity.getStack().getName().getString());
    }

    private PotionInfo getPotionInfo(PotionContentsComponent contents) {
        if (contents == null || !contents.hasEffects()) {
            return null;
        }
        boolean regenerationTwo = this.hasEffect(contents, (RegistryEntry<StatusEffect>)StatusEffects.REGENERATION, 1);
        boolean strengthFive = this.hasEffect(contents, (RegistryEntry<StatusEffect>)StatusEffects.STRENGTH, 4);
        boolean healthBoostThree = this.hasEffect(contents, (RegistryEntry<StatusEffect>)StatusEffects.HEALTH_BOOST, 2);
        boolean strengthFour = this.hasEffect(contents, (RegistryEntry<StatusEffect>)StatusEffects.STRENGTH, 3);
        boolean speedThree = this.hasEffect(contents, (RegistryEntry<StatusEffect>)StatusEffects.SPEED, 2);
        if (regenerationTwo) {
            return PotionInfo.HOLY_WATER;
        }
        if (strengthFive) {
            return PotionInfo.WRATH;
        }
        if (healthBoostThree) {
            return PotionInfo.PALADIN;
        }
        if (strengthFour && speedThree) {
            return PotionInfo.ASSASSIN;
        }
        if (strengthFour) {
            return PotionInfo.ASSASSIN;
        }
        return null;
    }

    private boolean hasEffect(PotionContentsComponent contents, RegistryEntry<StatusEffect> effect, int amplifier) {
        for (StatusEffectInstance instance : contents.getEffects()) {
            if (!instance.getEffectType().equals(effect) || instance.getAmplifier() != amplifier) continue;
            return true;
        }
        return false;
    }

    private PotionInfo getPotionInfo(String itemName) {
        String normalizedName = this.normalize(itemName);
        for (PotionInfo potionInfo : PotionInfo.values()) {
            if (!normalizedName.contains(this.normalize(potionInfo.plainName()))) continue;
            return potionInfo;
        }
        return null;
    }

    private String normalize(String text) {
        return text.replaceAll("§.", "").replace("[", "").replace("]", "").replace("✦", "").toLowerCase(Locale.ROOT).trim();
    }

    private void sendPotionMessage(String playerName, PotionInfo potionInfo, int percent) {
        if (PotionTracker.mc.player == null) {
            return;
        }
        MutableText text = Text.literal((String)"");
        text.append((Text)this.gradientText("polar", ColorUtils.getThemeColor(0), ColorUtils.getThemeColor(90), true));
        text.append((Text)Text.literal((String)" ⇒ ").setStyle(this.grayStyle()));
        text.append((Text)Text.literal((String)playerName).setStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)PLAYER))));
        text.append((Text)Text.literal((String)" получил ").setStyle(this.grayStyle()));
        text.append((Text)this.gradientText(potionInfo.displayName, potionInfo.startColor, potionInfo.endColor, true));
        text.append((Text)Text.literal((String)(" " + percent + "%")).setStyle(this.grayStyle()));
        PotionTracker.mc.player.sendMessage((Text)text, false);
    }

    private MutableText gradientText(String text, int startColor, int endColor, boolean bold) {
        MutableText result = Text.literal((String)"");
        for (int i2 = 0; i2 < text.length(); ++i2) {
            float progress = text.length() <= 1 ? 0.0f : (float)i2 / (float)(text.length() - 1);
            int color = ColorUtils.gradient(startColor, endColor, progress);
            result.append((Text)Text.literal((String)String.valueOf(text.charAt(i2))).setStyle(Style.EMPTY.withBold(Boolean.valueOf(bold)).withColor(TextColor.fromRgb((int)color))));
        }
        return result;
    }

    private Style grayStyle() {
        return Style.EMPTY.withColor(TextColor.fromRgb((int)GRAY));
    }

    private static enum PotionInfo {
        HOLY_WATER("[✦] Святая вода", 16774507, 12123970),
        WRATH("[✦] Зелье Гнева", 12849682, 16757051),
        PALADIN("[✦] Зелье Паладина", 12123970, 0xFFF0A0),
        ASSASSIN("[✦] Зелье Ассасина", 0x555555, 11545130);

        private final String displayName;
        private final int startColor;
        private final int endColor;

        private PotionInfo(String displayName, int startColor, int endColor) {
            this.displayName = displayName;
            this.startColor = startColor;
            this.endColor = endColor;
        }

        private String plainName() {
            int index = this.displayName.indexOf("] ");
            return index >= 0 ? this.displayName.substring(index + 2) : this.displayName;
        }
    }

    private static class PotionData {
        private PotionInfo potionInfo;
        private double lastX;
        private double lastY;
        private double lastZ;

        private PotionData(PotionInfo potionInfo, double lastX, double lastY, double lastZ) {
            this.potionInfo = potionInfo;
            this.lastX = lastX;
            this.lastY = lastY;
            this.lastZ = lastZ;
        }
    }

    private record PlayerHit(String playerName, int percent, double distance) {
    }
}

