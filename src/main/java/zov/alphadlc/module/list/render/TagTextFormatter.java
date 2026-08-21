package zov.alphadlc.module.list.render;

final class TagTextFormatter {
    private TagTextFormatter() {
    }

    static String potionRow(String effectName, int amplifierLevel, String duration) {
        return effectName + " " + amplifierLevel + " " + duration;
    }
}
