package polar.ru.api.utils.render.fonts.ttf;

import java.awt.Font;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

public class FontUtil {
    public static Font getFontFromTTF(Identifier loc, float fontSize, int fontType) {
        try {
            InputStream inputStream = null;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getResourceManager() != null) {
                try {
                    Optional resource = client.getResourceManager().getResource(loc);
                    if (resource.isPresent()) {
                        inputStream = ((Resource)resource.get()).getInputStream();
                    }
                } catch (Throwable ignored) {}
            }
            if (inputStream == null) {
                String path = "/assets/" + loc.getNamespace() + "/" + loc.getPath();
                inputStream = FontUtil.class.getResourceAsStream(path);
            }
            if (inputStream == null) {
                String path = "assets/" + loc.getNamespace() + "/" + loc.getPath();
                inputStream = FontUtil.class.getClassLoader().getResourceAsStream(path);
            }
            if (inputStream != null) {
                Font output = Font.createFont(fontType, inputStream);
                output = output.deriveFont(fontSize);
                inputStream.close();
                return output;
            }
            return null;
        }
        catch (Exception e2) {
            e2.printStackTrace();
            return null;
        }
    }
}

