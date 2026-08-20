package polar.ru.client.ui.clickgui;

public enum ClickGuiType {
    DROPDOWN("Dropdown");

    private final String displayName;

    private ClickGuiType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}

