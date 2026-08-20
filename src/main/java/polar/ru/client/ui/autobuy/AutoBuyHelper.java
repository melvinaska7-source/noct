package polar.ru.client.ui.autobuy;


public class AutoBuyHelper {
    private Group group;

    public AutoBuyHelper(Group group) {
        this.group = group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }
    public Group getGroup() {
        return this.group;
    }

    public static enum Group {
        RW("RW"),
        HW("HW"),
        FT("FT"),
        SP("SP");

        private final String server;

        public static Group fromString(String s2) {
            if (s2 == null) {
                return RW;
            }
            for (Group g2 : Group.values()) {
                if (!g2.server.equalsIgnoreCase(s2)) continue;
                return g2;
            }
            return RW;
        }
        private Group(String server) {
            this.server = server;
        }
        public String getServer() {
            return this.server;
        }
    }
}

