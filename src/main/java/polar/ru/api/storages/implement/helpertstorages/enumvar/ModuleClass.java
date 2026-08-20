package polar.ru.api.storages.implement.helpertstorages.enumvar;

import java.util.List;
import polar.ru.api.storages.implement.helpertstorages.enumvar.GlobalObject;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleRewords;
import polar.ru.client.modules.Module;

public class ModuleClass
extends GlobalObject<Module>
implements ModuleRewords {
    public static ModuleClass INSTANCE = new ModuleClass();

    public void initialize() {
        this.add(antibot, antithorns, aimBot, airstuck, arrows, aura, autoAccept, ahHelper, searchHelper, autoBuy, autoDuel, autoLeave, autoExplosion, nameProtect, autoSwap, autoTool, autoTotem, blockesp, blockOverlay, cape, chams, clientSounds, clickPearl, AutoWarden, cosmetics, cubes, elytraBoost, elytraMotion, elytraSwap, elytraTarget, entityESP, fireworkESP, fastExp, freeCam, friendMarkers, fullBright, hitBubbles, hitMarker, interfaceModule, itemRadius, interpolateF5, inventoryWalk, itemAim, itemRelease, itemScroller, jumpCircle, trails, killEffect, lockSlot, lootTracker, noJumpDelay, noPush, noControllerWeb, pets, packetCriticals, projectile, potionTracker, scoreboardHP, hitSound, removals, rPSpoofer, seeInvisibles, shaderEsp, shaderHands, serverHelper, shulkerPreview, sonar, sprint, swingAnimations, targetStrafe, targetESP, trapTimer, totemAngel, tpsSync, trajectories, viewModel, worldTweaks, autoEzz, clickFriend, noFriendDamage, maceExploit, mace, coords, emotes, ghost, chestes, triggerBot, aimAssist, ktLeave, invSort);
    }

    private void add(Module ... mod) {
        this.getObject().addAll(List.of(mod));
    }
}

