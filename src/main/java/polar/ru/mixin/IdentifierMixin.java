package polar.ru.mixin;

import java.util.Locale;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Identifier.class})
public abstract class IdentifierMixin {
    private static final String SAFE_JOIN_PATH = "invalid_join_id";

    @Inject(method={"validatePath"}, at={@At(value="HEAD")}, cancellable=true)
    private static void polar$sanitizeJoinPath(String namespace, String path, CallbackInfoReturnable<String> cir) {
        if (!IdentifierMixin.shouldSanitizePath(path)) {
            return;
        }
        cir.setReturnValue(IdentifierMixin.sanitizePath(path));
    }

    @Inject(method={"validateNamespace"}, at={@At(value="HEAD")}, cancellable=true)
    private static void polar$sanitizeJoinNamespace(String namespace, String path, CallbackInfoReturnable<String> cir) {
        if (!IdentifierMixin.shouldSanitizeNamespace(namespace)) {
            return;
        }
        cir.setReturnValue(IdentifierMixin.sanitizeNamespace(namespace));
    }

    private static boolean shouldSanitizeNamespace(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        for (int i2 = 0; i2 < value.length(); ++i2) {
            char c2 = value.charAt(i2);
            if (IdentifierMixin.isAllowedNamespaceChar(c2)) continue;
            return true;
        }
        return false;
    }

    private static boolean shouldSanitizePath(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        for (int i2 = 0; i2 < value.length(); ++i2) {
            char c2 = value.charAt(i2);
            if (IdentifierMixin.isAllowedPathChar(c2)) continue;
            return true;
        }
        return false;
    }

    private static boolean isAllowedNamespaceChar(char c2) {
        return c2 >= 'a' && c2 <= 'z' || c2 >= '0' && c2 <= '9' || c2 == '_' || c2 == '.' || c2 == '-';
    }

    private static boolean isAllowedPathChar(char c2) {
        return IdentifierMixin.isAllowedNamespaceChar(c2) || c2 == '/';
    }

    private static String sanitizeNamespace(String namespace) {
        StringBuilder builder = new StringBuilder(namespace.length());
        String lower = namespace.toLowerCase(Locale.ROOT);
        for (int i2 = 0; i2 < lower.length(); ++i2) {
            char c2 = lower.charAt(i2);
            if (IdentifierMixin.isAllowedNamespaceChar(c2)) {
                builder.append(c2);
                continue;
            }
            builder.append('_');
        }
        String sanitized = builder.toString();
        return sanitized.isBlank() ? "minecraft" : sanitized;
    }

    private static String sanitizePath(String path) {
        StringBuilder builder = new StringBuilder(path.length());
        String lower = path.toLowerCase(Locale.ROOT);
        for (int i2 = 0; i2 < lower.length(); ++i2) {
            char c2 = lower.charAt(i2);
            if (IdentifierMixin.isAllowedPathChar(c2)) {
                builder.append(c2);
                continue;
            }
            builder.append('_');
        }
        String sanitized = builder.toString();
        return sanitized.isBlank() ? SAFE_JOIN_PATH : sanitized;
    }
}

