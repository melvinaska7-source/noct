package zov.alphadlc.module.list.player;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TpRequestRecognizer {
    private static final Pattern REQUEST = Pattern.compile(
            "^\\s*(?:Игрок\\s+)?([A-Za-z0-9_]{1,16})\\s+(?:" +
                    "просит к вам телепортироваться|" +
                    "запрашивает телепорт к вам|" +
                    "хочет телепортироваться к вам|" +
                    "отправил вам запрос на телепортацию|" +
                    "has requested teleport(?: to you)?|" +
                    "has requested to teleport to you|" +
                    "sent you a teleport request" +
                    ")[.!]?\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private TpRequestRecognizer() {
    }

    static Optional<String> requester(String message) {
        if (message == null) return Optional.empty();
        Matcher matcher = REQUEST.matcher(message);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    static boolean shouldAccept(String message, boolean friendsOnly, Predicate<String> isFriend) {
        Optional<String> requester = requester(message);
        if (requester.isEmpty()) return false;
        return !friendsOnly || isFriend.test(requester.get());
    }
}
