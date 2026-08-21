package zov.alphadlc.module.list.player;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpRequestRecognizerTest {
    @Test
    void extractsRequesterFromSupportedRussianRequest() {
        assertEquals(Optional.of("Alex_7"), TpRequestRecognizer.requester("Alex_7 просит к вам телепортироваться"));
    }

    @Test
    void extractsRequesterFromSupportedEnglishRequest() {
        assertEquals(Optional.of("Steve"), TpRequestRecognizer.requester("Steve has requested teleport to you."));
    }

    @Test
    void rejectsOrdinaryChatContainingRequestWords() {
        assertEquals(Optional.empty(), TpRequestRecognizer.requester("<Alex_7> Steve has requested teleport to you"));
        assertEquals(Optional.empty(), TpRequestRecognizer.requester("Alex_7: просит к вам телепортироваться"));
    }

    @Test
    void friendsOnlyRequiresExactRecognizedFriend() {
        assertTrue(TpRequestRecognizer.shouldAccept(
                "Alex_7 просит к вам телепортироваться",
                true,
                name -> name.equalsIgnoreCase("alex_7")
        ));
        assertFalse(TpRequestRecognizer.shouldAccept(
                "NotAlex_7 просит к вам телепортироваться",
                true,
                name -> name.equalsIgnoreCase("alex_7")
        ));
    }

    @Test
    void disabledFriendsOnlyAcceptsAnyValidRequest() {
        assertTrue(TpRequestRecognizer.shouldAccept(
                "Unknown запрашивает телепорт к вам",
                false,
                name -> false
        ));
    }
}
