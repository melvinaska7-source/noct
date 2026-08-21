package zov.alphadlc.util.math;

public final class TimerUtil {
    public static volatile float speed = 1.0f;

    public static void setTimer(float s2) {
        speed = s2;
    }

    public static void reset() {
        speed = 1.0f;
    }
}
