package zov.alphadlc.module;

import org.junit.jupiter.api.Test;
import zov.alphadlc.module.list.movement.DogFly;
import zov.alphadlc.module.list.movement.NoJumpDelay;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleMetadataTest {
    @Test
    void dogFlyKeepsClassButUsesBibFlyDisplayName() {
        var information = DogFly.class.getAnnotation(ModuleInformation.class);

        assertEquals("Bib Fly", information.moduleName());
    }

    @Test
    void noJumpDelayIsMiscModule() {
        var information = NoJumpDelay.class.getAnnotation(ModuleInformation.class);

        assertEquals(ModuleCategory.MISC, information.moduleCategory());
    }
}
