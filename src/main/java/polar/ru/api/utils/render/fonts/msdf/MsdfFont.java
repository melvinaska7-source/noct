package polar.ru.api.utils.render.fonts.msdf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.stream.Collectors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import polar.ru.api.QClient;
import polar.ru.api.utils.render.fonts.msdf.MsdfGlyph;

public final class MsdfFont
implements QClient {
    private final String name;
    private AbstractTexture texture;
    private final Identifier atlasIdentifier;
    private final float atlasWidth;
    private final float atlasHeight;
    private final float range;
    private final float lineHeight;
    private final float ascender;
    private final float descender;
    private final HashMap<Integer, MsdfGlyph> glyphs;
    private boolean filtered = false;

    private MsdfFont(String name, AbstractTexture texture, Identifier atlasIdentifier, float atlasWidth, float atlasHeight, float range, float lineHeight, float ascender, float descender, HashMap<Integer, MsdfGlyph> glyphs) {
        this.name = name;
        this.texture = texture;
        this.atlasIdentifier = atlasIdentifier;
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.range = range;
        this.lineHeight = lineHeight;
        this.ascender = ascender;
        this.descender = descender;
        this.glyphs = glyphs;
    }

    public void setFiltered() {
        if (!this.filtered) {
            if (this.texture == null && QClient.mc != null && QClient.mc.getTextureManager() != null) {
                this.texture = QClient.mc.getTextureManager().getTexture(this.atlasIdentifier);
            }
            if (this.texture != null) {
                this.texture.setFilter(true, false);
                this.filtered = true;
            }
        }
    }

    public Identifier getAtlasIdentifier() {
        return this.atlasIdentifier;
    }

    public int getTextureId() {
        if (this.texture == null && QClient.mc != null && QClient.mc.getTextureManager() != null) {
            this.texture = QClient.mc.getTextureManager().getTexture(this.atlasIdentifier);
        }
        return this.texture != null ? this.texture.getGlId() : 0;
    }

    public float getAtlasWidth() {
        return this.atlasWidth;
    }

    public float getAtlasHeight() {
        return this.atlasHeight;
    }

    public float getRange() {
        return this.range;
    }

    public float getLineHeight() {
        return this.lineHeight;
    }

    public float getBaselineHeight() {
        return this.ascender;
    }

    public float getAscender() {
        return this.ascender;
    }

    public float getDescender() {
        return this.descender;
    }

    public String getName() {
        return this.name;
    }

    public MsdfGlyph getGlyph(char c2) {
        int code = (int) c2;
        MsdfGlyph glyph = this.glyphs.get(code);
        if (glyph == null) {
            glyph = this.glyphs.get((int) Character.toUpperCase(c2));
        }
        if (glyph == null) {
            glyph = this.glyphs.get((int) Character.toLowerCase(c2));
        }
        return glyph;
    }

    public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, float size, String text, float thickness, float x2, float y2, float z2, int red, int green, int blue, int alpha) {
        text = MsdfFont.replaceSymbols(text);
        for (int i2 = 0; i2 < text.length(); ++i2) {
            char c2 = text.charAt(i2);
            if (c2 == '§' && i2 + 1 < text.length()) {
                ++i2;
                continue;
            }
            MsdfGlyph glyph = this.getGlyph(c2);
            if (glyph == null) continue;
            x2 += glyph.apply(matrix, consumer, size, x2, y2, z2, red, green, blue, alpha) + thickness;
        }
    }

    public float getWidth(String text, float size) {
        text = MsdfFont.replaceSymbols(text);
        float width = 0.0f;
        for (int i2 = 0; i2 < text.length(); ++i2) {
            char c2 = text.charAt(i2);
            if (c2 == '§' && i2 + 1 < text.length()) {
                ++i2;
                continue;
            }
            MsdfGlyph glyph = this.getGlyph(c2);
            if (glyph == null) continue;
            width += glyph.getWidth(size);
        }
        return width;
    }

    private static String replaceSymbols(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("ᴀ", "A").replace("ʙ", "B").replace("ᴄ", "C").replace("ᴅ", "D").replace("ᴇ", "E").replace("ғ", "F").replace("ɢ", "G").replace("ʜ", "H").replace("ɪ", "I").replace("ᴊ", "J").replace("ᴋ", "K").replace("ʟ", "L").replace("ᴍ", "M").replace("ɴ", "N").replace("ᴏ", "O").replace("ᴘ", "P").replace("ǫ", "Q").replace("ʀ", "R").replace("ꜱ", "S").replace("ᴛ", "T").replace("ᴜ", "U").replace("ᴠ", "V").replace("ᴡ", "W").replace("ʏ", "Y").replace("ᴢ", "Z").replace("ꜰ", "F");
    }

    private static String readResource(Identifier identifier) {
        try {
            InputStream inputStream = null;
            if (mc != null && mc.getResourceManager() != null) {
                try {
                    inputStream = mc.getResourceManager().open(identifier);
                } catch (Throwable ignored) {}
            }
            if (inputStream == null) {
                String path = "/assets/" + identifier.getNamespace() + "/" + identifier.getPath();
                inputStream = MsdfFont.class.getResourceAsStream(path);
            }
            if (inputStream == null) {
                String path = "assets/" + identifier.getNamespace() + "/" + identifier.getPath();
                inputStream = MsdfFont.class.getClassLoader().getResourceAsStream(path);
            }
            if (inputStream == null) {
                throw new RuntimeException("Failed to find font resource: " + String.valueOf(identifier));
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8));
            String result = reader.lines().collect(Collectors.joining("\n"));
            reader.close();
            inputStream.close();
            return result;
        }
        catch (Exception e2) {
            throw new RuntimeException("Failed to read resource: " + String.valueOf(identifier), e2);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name = "?";
        private Identifier dataIdentifier;
        private Identifier atlasIdentifier;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder data(String dataFileName) {
            this.dataIdentifier = Identifier.of((String)"polar", (String)("fonts/msdf/" + dataFileName + "/font.json"));
            return this;
        }

        public Builder atlas(String atlasFileName) {
            this.atlasIdentifier = Identifier.of((String)"polar", (String)("fonts/msdf/" + atlasFileName + "/font.png"));
            return this;
        }

        public MsdfFont build() {
            String json = MsdfFont.readResource(this.dataIdentifier);
            JsonObject root = JsonParser.parseString((String)json).getAsJsonObject();
            JsonObject atlasObj = root.getAsJsonObject("atlas");
            float atlasWidth = atlasObj.get("width").getAsFloat();
            float atlasHeight = atlasObj.get("height").getAsFloat();
            float range = atlasObj.get("distanceRange").getAsFloat();
            JsonObject metricsObj = root.getAsJsonObject("metrics");
            float lineHeight = metricsObj.get("lineHeight").getAsFloat();
            float ascender = metricsObj.get("ascender").getAsFloat();
            float descender = metricsObj.get("descender").getAsFloat();
            HashMap<Integer, MsdfGlyph> glyphs = new HashMap<Integer, MsdfGlyph>();
            JsonArray glyphsArray = root.getAsJsonArray("glyphs");
            for (JsonElement element : glyphsArray) {
                JsonObject glyphObj = element.getAsJsonObject();
                int unicode = glyphObj.get("unicode").getAsInt();
                float advance = glyphObj.get("advance").getAsFloat();
                float planeLeft = 0.0f;
                float planeTop = 0.0f;
                float planeRight = 0.0f;
                float planeBottom = 0.0f;
                if (glyphObj.has("planeBounds") && !glyphObj.get("planeBounds").isJsonNull()) {
                    JsonObject plane = glyphObj.getAsJsonObject("planeBounds");
                    planeLeft = plane.get("left").getAsFloat();
                    planeTop = plane.get("top").getAsFloat();
                    planeRight = plane.get("right").getAsFloat();
                    planeBottom = plane.get("bottom").getAsFloat();
                }
                float atlasLeft = 0.0f;
                float atlasTop = 0.0f;
                float atlasRight = 0.0f;
                float atlasBottom = 0.0f;
                if (glyphObj.has("atlasBounds") && !glyphObj.get("atlasBounds").isJsonNull()) {
                    JsonObject atlas = glyphObj.getAsJsonObject("atlasBounds");
                    atlasLeft = atlas.get("left").getAsFloat();
                    atlasTop = atlas.get("top").getAsFloat();
                    atlasRight = atlas.get("right").getAsFloat();
                    atlasBottom = atlas.get("bottom").getAsFloat();
                }
                MsdfGlyph glyph = new MsdfGlyph(unicode, advance, planeLeft, planeTop, planeRight, planeBottom, atlasLeft, atlasTop, atlasRight, atlasBottom, atlasWidth, atlasHeight);
                glyphs.put(unicode, glyph);
            }
            AbstractTexture texture = null;
            if (QClient.mc != null && QClient.mc.getTextureManager() != null) {
                try {
                    texture = QClient.mc.getTextureManager().getTexture(this.atlasIdentifier);
                } catch (Throwable ignored) {}
            }
            return new MsdfFont(this.name, texture, this.atlasIdentifier, atlasWidth, atlasHeight, range, lineHeight, ascender, descender, glyphs);
        }
    }
}

