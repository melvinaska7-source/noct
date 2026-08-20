package polar.ru.api.utils.client;

public class UserInfo {
    private final String username;
    private final int uid;
    private final String role;
    private final String hwid;
    private final String expireDate;

    public UserInfo(String username, int uid, String role, String hwid, String expireDate) {
        this.username = username;
        this.uid = uid;
        this.role = role;
        this.hwid = hwid;
        this.expireDate = expireDate;
    }

    public static UserInfo empty() {
        return new UserInfo("Unknown", 0, "Unknown", "", "");
    }

    public String getUsername() {
        return this.username;
    }

    public int getUid() {
        return this.uid;
    }

    public String getRole() {
        return this.role;
    }

    public String getHwid() {
        return this.hwid;
    }

    public String getExpireDate() {
        return this.expireDate;
    }
}

