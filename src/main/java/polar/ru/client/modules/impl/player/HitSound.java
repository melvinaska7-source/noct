package polar.ru.client.modules.impl.player;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventAttackEntity;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class HitSound
extends Module {
    public static HitSound INSTANCE = new HitSound();
    private final BooleanSetting hitSoundEnabled = new BooleanSetting("Звук удара", false);
    private final BooleanSetting critOnly = new BooleanSetting("Только крит", true).visible(() -> this.hitSoundEnabled.isState());
    private final ModeSetting hitSoundType = new ModeSetting("Звук", "Mita Stones", "Mita Stones").visible(() -> this.hitSoundEnabled.isState());
    private static final int SOUND_VARIANTS = 5;
    private final Map<String, byte[]> audioCache = new ConcurrentHashMap<String, byte[]>();
    private final AtomicInteger playToken = new AtomicInteger(0);
    private final AtomicBoolean workerBusy = new AtomicBoolean(false);
    private volatile Clip liveClip;
    private volatile int pendingToken = -1;
    private volatile Thread workerThread;

    public HitSound() {
        super("HitSound", "Звук удара", Module.ModuleCategory.PLAYER);
        this.addSettings(this.hitSoundEnabled, this.critOnly, this.hitSoundType);
    }

    @EventLink
    public void onAttack(EventAttackEntity event) {
        int token;
        if (!this.hitSoundEnabled.isState()) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity)) {
            return;
        }
        if (this.critOnly.isState() && !this.isCritical()) {
            return;
        }
        this.pendingToken = token = this.playToken.incrementAndGet();
        this.killLiveClipAsync();
        this.dispatchPlay(token);
    }

    private boolean isCritical() {
        if (HitSound.mc.player == null) {
            return false;
        }
        return HitSound.mc.player.fallDistance > 0.0f && !HitSound.mc.player.isOnGround() && !HitSound.mc.player.isClimbing() && !HitSound.mc.player.isTouchingWater() && !HitSound.mc.player.hasStatusEffect(StatusEffects.BLINDNESS);
    }

    private void killLiveClipAsync() {
        Clip clip = this.liveClip;
        if (clip == null) {
            return;
        }
        this.liveClip = null;
        Thread killer = new Thread(() -> {
            try {
                if (clip.isRunning()) {
                    clip.stop();
                }
                clip.close();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }, "HitSound-Kill");
        killer.setDaemon(true);
        killer.start();
    }

    private void dispatchPlay(int token) {
        if (!this.workerBusy.compareAndSet(false, true)) {
            return;
        }
        Thread t2 = new Thread(() -> {
            try {
                this.runPlaybackLoop();
            }
            finally {
                this.workerBusy.set(false);
            }
        }, "HitSound-Player");
        t2.setDaemon(true);
        this.workerThread = t2;
        t2.start();
    }

    private void runPlaybackLoop() {
        int token;
        do {
            if ((token = this.pendingToken) != this.playToken.get()) {
                return;
            }
            if (!this.hitSoundEnabled.isState()) {
                return;
            }
            this.playOneSound(token);
        } while (token != this.playToken.get());
    }

    private void playOneSound(int token) {
        try {
            int randomSound = (int)(Math.random() * 5.0) + 1;
            String resourcePath = "/assets/polar/sounds/hit_mita-moan-" + randomSound + ".wav";
            byte[] data = this.loadAudioBytes(resourcePath);
            if (data == null) {
                System.out.println("Hit sound resource not found: " + resourcePath);
                return;
            }
            if (token != this.playToken.get() || !this.hitSoundEnabled.isState()) {
                return;
            }
            try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(data)));){
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                if (token != this.playToken.get() || !this.hitSoundEnabled.isState()) {
                    clip.close();
                    return;
                }
                this.liveClip = clip;
                clip.setFramePosition(0);
                clip.start();
            }
        }
        catch (Exception e2) {
            System.out.println("Error playing hit sound: " + e2.getMessage());
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private byte[] loadAudioBytes(String resourcePath) {
        byte[] cached = this.audioCache.get(resourcePath);
        if (cached != null) {
            return cached;
        }
        try (InputStream audioSrc = this.getClass().getResourceAsStream(resourcePath);){
            if (audioSrc == null) {
                byte[] byArray2 = null;
                return byArray2;
            }
            byte[] bytes = audioSrc.readAllBytes();
            this.audioCache.put(resourcePath, bytes);
            byte[] byArray = bytes;
            return byArray;
        }
        catch (Exception e2) {
            System.out.println("Error loading hit sound: " + e2.getMessage());
            return null;
        }
    }

    @Override
    public void onDisable() {
        this.playToken.incrementAndGet();
        this.killLiveClipAsync();
    }
}

