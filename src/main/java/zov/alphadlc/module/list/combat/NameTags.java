package zov.alphadlc.module.list.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import zov.alphadlc.AlphaDLC;
import zov.alphadlc.event.list.EventHUD;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.list.misc.NameProtect;
import zov.alphadlc.module.list.render.hud.Interface;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeListSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.friend.FriendRepository;
import zov.alphadlc.util.render.builders.Builder;
import zov.alphadlc.util.render.builders.states.QuadColorState;
import zov.alphadlc.util.render.builders.states.QuadRadiusState;
import zov.alphadlc.util.render.builders.states.SizeState;
import zov.alphadlc.util.render.math.ProjectionUtil;
import zov.alphadlc.util.render.renderers.IRenderer;
import zov.alphadlc.util.render.msdf.Fonts;
import zov.alphadlc.util.render.msdf.MsdfFont;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;
import zov.alphadlc.util.replace.ReplaceUtil;
import org.joml.Vector4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;



@ModuleInformation(moduleName = "NameTags", moduleDesc = "Теги над игроками", moduleCategory = ModuleCategory.RENDER)
public class NameTags extends Module {

    private final ModeListSetting entityTypes = new ModeListSetting("Типы",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Предметы", true)
    );
    
    private final SliderSetting size = new SliderSetting("Размер", 7.5f, 5.0f, 15.0f, 0.5f);
    private final BooleanSetting blur = new BooleanSetting("Блюр", true);
    private final SliderSetting alpha = new SliderSetting("Прозрачность", 100, 0, 100, 1);

    private final Map<UUID, Text> normalizedNames = new ConcurrentHashMap<>();
    private int clearCacheTicker = 0;

    private final List<ItemStack> equipmentCache = new ArrayList<>();

    public static Text normalizeSmallCaps(Text text) {
        String[] from = {"ᴀ", "ʙ", "ᴄ", "ᴅ", "ᴇ", "ꜰ", "ɢ", "ʜ", "ɪ", "ᴊ", "ᴋ", "ʟ", "ᴍ", "ɴ", "ᴏ", "ᴘ", "ǫ", "ʀ", "ꜱ", "ᴛ", "ᴜ", "ᴠ", "ᴡ", "x", "ʏ", "ᴢ", "◆", "┃ "};
        String[] to = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "●", ""};
        for (int i = 0; i < from.length; i++) {
            text = ReplaceUtil.replace(text, from[i], to[i]);
        }
        return text;
    }

    public static Text processName(PlayerEntity entity) {
        Text name = entity.getDisplayName();

        MinecraftClient mc = MinecraftClient.getInstance();
        NameProtect nameProtect = AlphaDLC.getInstance().getModuleStorage().get(NameProtect.class);
        if (nameProtect != null && nameProtect.isEnabled() && mc.player != null) {
            String scoreName = entity.getNameForScoreboard();
            String myName = mc.player.getNameForScoreboard();
            String customName = nameProtect.getCustomName();

            if (scoreName.equals(myName) || entity.getUuid().equals(mc.player.getUuid())) {
                name = ReplaceUtil.replace(name, myName, customName);
            } else if (nameProtect.hideFriends.getValue() && FriendRepository.isFriend(scoreName)) {
                name = ReplaceUtil.replace(name, scoreName, customName);
            }
        }

        String s = name.getString();
        if (s.contains("ꔀ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔀ", Formatting.GRAY, "ИГРОК");
        if (s.contains("ꔄ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔄ", Formatting.BLUE, "HERO");
        if (s.contains("ꔈ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔈ", Formatting.GOLD, "TITAN");
        if (s.contains("ꔒ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔒ", Formatting.GREEN, "AVENGER");
        if (s.contains("ꔖ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔖ", Formatting.AQUA, "OVERLORD");
        if (s.contains("ꔠ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔠ", Formatting.GOLD, "MAGISTER");
        if (s.contains("ꔤ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔤ", Formatting.RED, "IMPERATOR");
        if (s.contains("ꔲ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔲ", Formatting.DARK_PURPLE, "BULL");
        if (s.contains("ꕓ")) name = ReplaceUtil.replaceWithFormatted(name, "ꕓ", Formatting.DARK_GRAY, "GHOST");
        if (s.contains("ꔨ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔨ", Formatting.LIGHT_PURPLE, "DRAGON");
        if (s.contains("ꔂ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔂ", Formatting.GOLD, "GOD");
        if (s.contains("ꔸ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔸ", Formatting.GOLD, "GOD");
        if (s.contains("ꔦ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔦ", Formatting.BLUE, "D.ML.ADMIN");
        if (s.contains("ꕀ")) name = ReplaceUtil.replaceWithFormatted(name, "ꕀ", Formatting.DARK_GREEN, "HYDRA");
        if (s.contains("ꕖ")) name = ReplaceUtil.replaceWithFormatted(name, "ꕖ", Formatting.GRAY, "BUNNY");
        if (s.contains("ꕒ")) name = ReplaceUtil.replaceWithFormatted(name, "ꕒ", Formatting.WHITE, "RABBIT");
        if (s.contains("ꕈ")) name = ReplaceUtil.replaceWithFormatted(name, "ꕈ", Formatting.GREEN, "COBRA");
        if (s.contains("ꔶ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔶ", Formatting.GOLD, "TIGER");
        if (s.contains("ꕠ")) name = ReplaceUtil.replaceWithFormatted(name, "ꕠ", Formatting.YELLOW, "D.HELPER");
        if (s.contains("ꔉ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔉ", Formatting.YELLOW, "HELPER");
        if (s.contains("ꔆ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔆ", Formatting.GRAY, "D.MODER");
        if (s.contains("ꕄ")) name = ReplaceUtil.replaceWithFormatted(name, "ꕄ", Formatting.DARK_RED, "VAMPIRE");
        if (s.contains("ꔰ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔰ", Formatting.GRAY, "D.ML.ADMIN");
        if (s.contains("ꔐ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔐ", Formatting.DARK_BLUE, "D.GL.MODER");
        if (s.contains("ꔔ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔔ", Formatting.GRAY, "D.GL.MODER");
        if (s.contains("ꔢ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔢ", Formatting.GRAY, "D.ST.MODER");
        if (s.contains("ꕡ")) name = ReplaceUtil.replaceWithFormatted(name, "ꕡ", Formatting.GOLD, "ST.HELPER");
        if (s.contains("ꕅ")) name = ReplaceUtil.replaceWithFormatted(name, "ꕅ", Formatting.DARK_PURPLE, "MEDIA+");
        if (s.contains("ꔓ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔓ", Formatting.BLUE, "ML.MODER");
        if (s.contains("ꔗ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔗ", Formatting.BLUE, "MODER");
        if (s.contains("ꔡ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔡ", Formatting.DARK_PURPLE, "MODER+");
        if (s.contains("ꔥ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔥ", Formatting.BLUE, "ST.MODER");
        if (s.contains("ꔩ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔩ", Formatting.DARK_PURPLE, "GL.MODER");
        if (s.contains("ꕗ")) name = ReplaceUtil.replaceWithFormatted(name, "ꕗ", Formatting.DARK_RED, "D.ADMIN");
        if (s.contains("ꔘ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔘ", Formatting.BLUE, "D.ST.MODER");
        if (s.contains("ꔳ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔳ", Formatting.AQUA, "ML.ADMIN");
        if (s.contains("ꔷ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔷ", Formatting.RED, "ADMIN");
        if (s.contains("ꔁ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔁ", Formatting.DARK_PURPLE, "MEDIA");
        if (s.contains("ꔅ")) name = ReplaceUtil.replaceWithFormatted(name, "ꔅ", Formatting.RED, "YT");
        if (s.contains("ꕉ")) name = ReplaceUtil.replaceWithFormatted(name, "ꕉ", Formatting.GOLD, "PEGAS");
        if (s.contains("ꕆ")) name = ReplaceUtil.replaceWithFormatted(name, "ꕆ", Formatting.GOLD, "PEGAS");

        return normalizeSmallCaps(name);
    }

    private Text getNormalizedName(PlayerEntity entity) {
        return normalizedNames.computeIfAbsent(entity.getUuid(), uuid -> processName(entity));
    }

    @Subscribe
    private void onRender(EventHUD e) {
        if (clearCacheTicker++ > 100) {
            normalizedNames.clear();
            clearCacheTicker = 0;
        }

        if (mc.world == null || mc.player == null) return;

        MsdfFont font = Fonts.SFBOLD.get();
        float tickDelta = e.getRenderTickCounter().getTickDelta(true);

        if (entityTypes.isEnabled("Игроки")) {
            renderPlayerTags(font, tickDelta, e);
        }

        if (entityTypes.isEnabled("Предметы")) {
            renderItemTags(font, tickDelta, e);
        }
    }

    private void renderPlayerTags(MsdfFont font, float tickDelta, EventHUD e) {
        List<AbstractClientPlayerEntity> worldPlayers = mc.world.getPlayers();
        Interface iface = AlphaDLC.getInstance().getModuleStorage().get(Interface.class);
        if (iface == null) return;

        for (PlayerEntity entity : worldPlayers) {
            if (entity == mc.player && !mc.getEntityRenderDispatcher().camera.isThirdPerson()) continue;

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) + entity.getHeight() + 0.5;
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

            Vector2f pos = ProjectionUtil.project(x, y, z);
            if (pos.getX() == Float.MAX_VALUE || pos.getY() == Float.MAX_VALUE) continue;

            final float screenYOffset = 5.7f;
            float posY = pos.getY() - screenYOffset;

            Text baseName = getNormalizedName(entity);

            float currentHp = zov.alphadlc.util.server.Server.getHealth(entity, false);
            int hpColor = 0xFF5555;

            MutableText name = baseName.copy();

            boolean isFriend = FriendRepository.isFriend(entity.getNameForScoreboard());

            ItemStack offHandStack = entity.getOffHandStack();
            if (offHandStack.getItem() == net.minecraft.item.Items.PLAYER_HEAD
                    && offHandStack.getName().getString().length() <= 18) {
                name.append(Text.literal(" - ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                        .append(offHandStack.getName())
                        .append(Text.literal("").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
            }

            name.append(Text.literal(" - ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)))
                    .append(Text.literal(String.format("%.1f", currentHp)).setStyle(Style.EMPTY.withColor(hpColor)))
                    .append(Text.literal("HP").setStyle(Style.EMPTY.withColor(hpColor)));

            float fontSize = (float) size.getValue();
            float textWidth = font.getWidth(name.getString(), fontSize);
            float paddingX = 3f;
            float headIconSize = 9.5f;
            float headIconPadding = 1f;
            float friendPrefixWidth = isFriend ? font.getWidth("[F]", fontSize) + 3f : 0f;
            float totalWidth = textWidth + (paddingX * 2) + headIconSize + headIconPadding + friendPrefixWidth;
            float tagHeight = 12.5f;
            float tagY = posY - 2;

            float centerX = pos.getX();
            float bgX = centerX - totalWidth / 2.0f;
            float bgY = tagY;

            Vector4f radii = new Vector4f(4, 4, 4, 4);
            if (isFriend) {
                if (blur.getValue()) {
                    DrawUtil.drawRoundBlur(bgX, bgY, totalWidth, tagHeight + 1, radii, 0xFF5DBF6E, 12);
                }
                DrawUtil.drawRound(bgX, bgY, totalWidth, tagHeight + 1, radii, ColorProvider.setAlpha(0x4A1E6B3A, (int)(alpha.getValue() * 2.55)));
            } else {
                if (blur.getValue()) {
                    DrawUtil.drawRoundBlur(bgX, bgY, totalWidth, tagHeight + 1, radii, ColorProvider.rgba(200, 200, 200, 255), 12);
                }
                DrawUtil.drawRound(bgX, bgY, totalWidth, tagHeight + 1, radii, ColorProvider.setAlpha(ColorProvider.rgba(25, 25, 25, (int)(255 * 0.1f)), (int)(alpha.getValue() * 2.55)));
            }

            if (isFriend) {
                DrawUtil.drawText(font, "[F]", bgX + paddingX, posY - 0.8f, 0xFF5DBF6E, fontSize);
            }

            float headSize = 9.5f;
            float headX = bgX + 2.8f + friendPrefixWidth;
            float headY = tagY + (tagHeight - headSize) / 2f;

            if (entity instanceof AbstractClientPlayerEntity clientPlayer) {
                net.minecraft.client.texture.AbstractTexture skinTex = mc.getTextureManager().getTexture(clientPlayer.getSkinTextures().texture());
                int texId = skinTex.getGlId();
                Builder.texture()
                        .size(new SizeState(headSize, headSize))
                        .radius(new QuadRadiusState(1f))
                        .color(new QuadColorState(0xFFFFFFFF))
                        .texture(0.125f, 0.125f, 0.125f, 0.125f, texId)
                        .build()
                        .render(IRenderer.DEFAULT_MATRIX, headX, headY, 0);
            }

            DrawUtil.drawText(font, name, bgX + paddingX + 1.0f + headIconSize + headIconPadding + friendPrefixWidth, posY - 0.25f, fontSize, 220);

            equipmentCache.clear();
            equipmentCache.add(entity.getEquippedStack(EquipmentSlot.HEAD));
            equipmentCache.add(entity.getEquippedStack(EquipmentSlot.CHEST));
            equipmentCache.add(entity.getEquippedStack(EquipmentSlot.LEGS));
            equipmentCache.add(entity.getEquippedStack(EquipmentSlot.FEET));
            equipmentCache.add(entity.getMainHandStack());
            equipmentCache.add(entity.getOffHandStack());
            equipmentCache.removeIf(ItemStack::isEmpty);

            if (!equipmentCache.isEmpty()) {
                float iconSize = 16;
                float spacing = 0;
                float itemsTotalWidth = equipmentCache.size() * iconSize + (equipmentCache.size() - 1) * spacing;
                float startX = pos.getX() - itemsTotalWidth / 2.0f + 13.5f;
                float iconY = posY - 10;

                MatrixStack matrices = e.getDrawContext().getMatrices();

                for (int i = 0; i < equipmentCache.size(); i++) {
                    ItemStack stack = equipmentCache.get(i);
                    float x2 = startX + i * (iconSize + spacing - 2);
                    float scale = 0.7f;
                    float half = -18;

                    matrices.push();
                    matrices.translate(x2 + half, iconY + half, 0);
                    matrices.scale(scale, scale, 1);
                    e.getDrawContext().drawItem(stack, (int) (-half), (int) (-half));
                    e.getDrawContext().drawStackOverlay(mc.textRenderer, stack, (int) (-half), (int) (-half));
                    matrices.pop();
                }
            }
        }
    }

    private void renderItemTags(MsdfFont font, float tickDelta, EventHUD e) {
        MsdfFont sfBold = Fonts.SFBOLD.get();
        Interface iface = AlphaDLC.getInstance().getModuleStorage().get(Interface.class);
        if (iface == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) continue;

            if (itemEntity.isInvisible() || itemEntity.hasNoGravity()) {
                continue;
            }

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) + entity.getHeight() + 0.5;
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

            Vector2f pos = ProjectionUtil.project(x, y, z);
            if (pos.getX() == Float.MAX_VALUE || pos.getY() == Float.MAX_VALUE) continue;

            ItemStack stack = itemEntity.getStack();
            if (stack.isEmpty()) continue;

            int rarityOrdinal = stack.getRarity().ordinal();
            Formatting rarityColor = switch (rarityOrdinal) {
                case 0 -> Formatting.WHITE;
                case 1 -> Formatting.YELLOW;
                case 2 -> Formatting.AQUA;
                case 3 -> Formatting.LIGHT_PURPLE;
                default -> Formatting.WHITE;
            };

            String itemName = stack.getName().getString();
            if (itemName.length() > 20) itemName = itemName.substring(0, 20);
            Text nameText = Text.literal(itemName).setStyle(Style.EMPTY.withColor(rarityColor));
            if (!stack.getName().getSiblings().isEmpty()) {
                String siblingName = stack.getName().getString();
                if (siblingName.length() > 20) siblingName = siblingName.substring(0, 20);
                nameText = Text.literal(siblingName).setStyle(stack.getName().getStyle());
            }

            Text countComponent = stack.getCount() > 1
                    ? Text.literal(" - ").setStyle(Style.EMPTY.withColor(Formatting.GRAY))
                    .append(Text.literal(String.valueOf(stack.getCount()) + "x").setStyle(Style.EMPTY.withColor(Formatting.RED)))
                    : Text.empty();

            Text textComponent = nameText.copy().append(countComponent);
            Text normalized = normalizeSmallCaps(textComponent);

            float textWidth = sfBold.getWidth(normalized.getString(), (float) size.getValue());
            float totalWidth = textWidth + 4;
            float bgX = pos.getX() - (totalWidth / 2.0f);

            Vector4f radii = new Vector4f(3, 3, 3, 3);
            if (blur.getValue()) {
                DrawUtil.drawRoundBlur(bgX, pos.getY() - 2f, totalWidth - 1.0f, 11f, radii, ColorProvider.rgba(200, 200, 200, 255), 12);
            }
            DrawUtil.drawRound(bgX, pos.getY() - 2f, totalWidth - 1.0f, 11f, radii, ColorProvider.setAlpha(ColorProvider.rgba(25, 25, 25, (int)(255 * 0.1f)), (int)(alpha.getValue() * 2.55)));
            
            DrawUtil.drawText(sfBold, normalized, bgX + 2, pos.getY() - 2.0f, (float) size.getValue(), 255);
        }
    }
}

