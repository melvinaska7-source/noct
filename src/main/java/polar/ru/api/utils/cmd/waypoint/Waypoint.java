package polar.ru.api.utils.cmd.waypoint;


public class Waypoint {
    private double x;
    private double z;
    public Waypoint(double x2, double z2) {
        this.x = x2;
        this.z = z2;
    }
    public double getX() {
        return this.x;
    }
    public double getZ() {
        return this.z;
    }
}

