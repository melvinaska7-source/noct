package zov.alphadlc.module.list.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoSwapRecognitionTest {
    @Test
    void recognizesPlainPlayerHeadsAsSphereCandidates() {
        assertTrue(AutoSwapRecognition.isSphereCandidate(false, true));
    }

    @Test
    void recognizesServerCustomSpheresAsSphereCandidates() {
        assertTrue(AutoSwapRecognition.isSphereCandidate(true, false));
    }

    @Test
    void rejectsOtherItems() {
        assertFalse(AutoSwapRecognition.isSphereCandidate(false, false));
    }
}
