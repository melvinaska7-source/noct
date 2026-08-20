package polar.ru.api.utils.player;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ViaProtocolUtils {
    private static final int MC_1_19_PROTOCOL = 759;
    private static final long CACHE_TIME_MS = 1500L;
    private static final Pattern VERSION_PATTERN = Pattern.compile("1\\.(\\d+)");
    private static long nextRefreshAt;
    private static boolean belowOneNineteen;

    private ViaProtocolUtils() {
    }

    public static boolean isTargetProtocolBelowOneNineteen() {
        long now = System.currentTimeMillis();
        if (now < nextRefreshAt) {
            return belowOneNineteen;
        }
        belowOneNineteen = ViaProtocolUtils.resolveBelowOneNineteen();
        nextRefreshAt = now + 1500L;
        return belowOneNineteen;
    }

    private static boolean resolveBelowOneNineteen() {
        try {
            Class<?> viaFabricPlusClass = Class.forName("com.viaversion.viafabricplus.ViaFabricPlus");
            Object impl = viaFabricPlusClass.getMethod("getImpl", new Class[0]).invoke(null, new Object[0]);
            if (impl == null) {
                return false;
            }
            Object targetVersion = ViaProtocolUtils.invokeNoArg(impl, "getTargetVersion");
            if (targetVersion == null) {
                return false;
            }
            Integer protocolId = ViaProtocolUtils.readProtocolId(targetVersion);
            return protocolId != null && protocolId < 759;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private static Object invokeNoArg(Object instance, String methodName) {
        try {
            Method method = instance.getClass().getMethod(methodName, new Class[0]);
            if (!Modifier.isPublic(method.getModifiers()) || method.getParameterCount() != 0) {
                return null;
            }
            return method.invoke(instance, new Object[0]);
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private static Integer readProtocolId(Object targetVersion) {
        Matcher matcher;
        try {
            Method getVersion = targetVersion.getClass().getMethod("getVersion", new Class[0]);
            Object value = getVersion.invoke(targetVersion, new Object[0]);
            if (value instanceof Number) {
                Number number = (Number)value;
                return number.intValue();
            }
        }
        catch (Throwable getVersion) {
            // empty catch block
        }
        try {
            for (Method method : targetVersion.getClass().getMethods()) {
                Object value;
                String name;
                Class<?> returnType;
                if (!Modifier.isPublic(method.getModifiers()) || method.getParameterCount() != 0 || (returnType = method.getReturnType()) != Integer.TYPE && returnType != Integer.class || !(name = method.getName().toLowerCase()).contains("version") && !name.contains("protocol") && !name.contains("id") || !((value = method.invoke(targetVersion, new Object[0])) instanceof Number)) continue;
                Number number = (Number)value;
                return number.intValue();
            }
        }
        catch (Throwable getVersion) {
            // empty catch block
        }
        if ((matcher = VERSION_PATTERN.matcher(String.valueOf(targetVersion))).find()) {
            int minor = Integer.parseInt(matcher.group(1));
            return minor >= 19 ? 759 : 758;
        }
        return null;
    }
}

