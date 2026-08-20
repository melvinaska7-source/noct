package polar.ru.client.modules.impl.render;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import polar.ru.api.QClient;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.ui.emotes.EmotesScreen;

public class Emotes
extends Module
implements QClient {
    public static Emotes INSTANCE = new Emotes();
    private final BindSetting openKey = new BindSetting("Клавиша открытия", 66);
    private boolean lastKeyState = false;
    private EmoteType currentEmote = null;
    private float emoteProgress = 0.0f;
    private long emoteStartTime = 0L;

    public Emotes() {
        super("Emotes", "Эмоции и танцы для игрока", Module.ModuleCategory.RENDER);
        this.addSettings(this.openKey);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.currentEmote = null;
        this.emoteProgress = 0.0f;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.currentEmote = null;
        this.emoteProgress = 0.0f;
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (Emotes.mc.player == null) {
            return;
        }
        boolean keyPressed = this.isKeyPressed(this.openKey.getKey());
        if (keyPressed && !this.lastKeyState && Emotes.mc.currentScreen == null) {
            mc.setScreen((Screen)new EmotesScreen());
        }
        this.lastKeyState = keyPressed;
        if (this.currentEmote != null) {
            long elapsedTime = System.currentTimeMillis() - this.emoteStartTime;
            float duration = this.currentEmote.getDuration();
            this.emoteProgress = Math.min((float)elapsedTime / duration, 1.0f);
            this.applyEmoteAnimation(this.currentEmote, this.emoteProgress);
            if (this.emoteProgress >= 1.0f) {
                if (this.currentEmote.isLooping()) {
                    this.emoteStartTime = System.currentTimeMillis();
                    this.emoteProgress = 0.0f;
                } else {
                    this.currentEmote = null;
                    this.emoteProgress = 0.0f;
                }
            }
        }
    }

    private boolean isKeyPressed(int key) {
        if (key == -1) {
            return false;
        }
        long window = mc.getWindow().getHandle();
        if (key >= 1000) {
            int mouseButton = key - 1000;
            return GLFW.glfwGetMouseButton((long)window, (int)mouseButton) == 1;
        }
        return GLFW.glfwGetKey((long)window, (int)key) == 1;
    }

    public void playEmote(EmoteType emote) {
        this.currentEmote = emote;
        this.emoteProgress = 0.0f;
        this.emoteStartTime = System.currentTimeMillis();
    }

    public EmoteType getCurrentEmote() {
        return this.currentEmote;
    }

    public float getEmoteProgress() {
        return this.emoteProgress;
    }

    private void applyEmoteAnimation(EmoteType emote, float progress) {
    }

    public void applyEmoteToModel(BipedEntityModel<?> model, float tickDelta) {
        if (this.currentEmote == null || !this.isEnable()) {
            return;
        }
        float progress = this.smoothProgress(this.emoteProgress);
        switch (this.currentEmote.ordinal()) {
            case 0: {
                this.applyCryAnimation(model, progress);
                break;
            }
            case 1: {
                this.applyRussianSquatAnimation(model, progress);
                break;
            }
            case 2: {
                this.applyFlossAnimation(model, progress);
                break;
            }
            case 3: {
                this.applyTakeTheLAnimation(model, progress);
                break;
            }
            case 4: {
                this.applyOrangeJusticeAnimation(model, progress);
                break;
            }
            case 5: {
                this.applyGetGriddyAnimation(model, progress);
                break;
            }
            case 6: {
                this.applyWaveAnimation(model, progress);
                break;
            }
            case 7: {
                this.applyDabAnimation(model, progress);
            }
        }
    }

    private float smoothProgress(float t2) {
        return t2 * t2 * (3.0f - 2.0f * t2);
    }

    private float smoothSin(float progress, float frequency) {
        return MathHelper.sin((float)(progress * (float)Math.PI * frequency));
    }

    private float smoothCos(float progress, float frequency) {
        return MathHelper.cos((float)(progress * (float)Math.PI * frequency));
    }

    private void applyCryAnimation(BipedEntityModel<?> model, float progress) {
        float wave = this.smoothSin(progress, 4.0f) * 0.08f;
        model.rightArm.pitch = -2.3f + wave;
        model.leftArm.pitch = -2.3f + wave;
        model.rightArm.yaw = -0.25f;
        model.leftArm.yaw = 0.25f;
        model.rightArm.roll = 0.15f;
        model.leftArm.roll = -0.15f;
        model.head.pitch = 0.25f + wave * 0.5f;
        model.body.pitch = this.smoothSin(progress, 2.0f) * 0.04f;
        model.body.yaw = this.smoothCos(progress, 1.5f) * 0.03f;
    }

    private void applyRussianSquatAnimation(BipedEntityModel<?> model, float progress) {
        float squatCycle = this.smoothSin(progress, 2.5f);
        float legKick = this.smoothSin(progress, 5.0f);
        float squatAmount = (1.0f - squatCycle) * 0.5f;
        model.rightLeg.pitch = -squatAmount * 1.2f;
        model.leftLeg.pitch = -squatAmount * 1.2f;
        model.body.pitch = -squatAmount * 0.4f;
        model.rightArm.pitch = -1.1f;
        model.leftArm.pitch = -1.1f;
        model.rightArm.yaw = 0.5f;
        model.leftArm.yaw = -0.5f;
        model.rightArm.roll = -0.25f;
        model.leftArm.roll = 0.25f;
        float kickStrength = Math.max(0.0f, -squatCycle) * 0.8f;
        if (legKick > 0.0f) {
            model.rightLeg.pitch += legKick * kickStrength * 1.3f;
            model.rightLeg.yaw = 0.15f;
        } else {
            model.leftLeg.pitch += -legKick * kickStrength * 1.3f;
            model.leftLeg.yaw = -0.15f;
        }
        model.body.yaw = legKick * 0.1f;
    }

    private void applyFlossAnimation(BipedEntityModel<?> model, float progress) {
        float swing = this.smoothSin(progress, 8.0f);
        model.body.yaw = swing * 0.4f;
        float armSwing = -swing;
        model.rightArm.pitch = 0.15f;
        model.leftArm.pitch = 0.15f;
        model.rightArm.yaw = armSwing * 1.0f - 0.2f;
        model.leftArm.yaw = armSwing * 1.0f + 0.2f;
        model.rightArm.roll = armSwing * 0.6f;
        model.leftArm.roll = armSwing * 0.6f;
        model.body.pitch = Math.abs(swing) * -0.05f;
    }

    private void applyTakeTheLAnimation(BipedEntityModel<?> model, float progress) {
        float bounce = this.smoothSin(progress, 6.0f);
        float bounceHeight = Math.abs(bounce) * 0.15f;
        model.rightArm.pitch = -2.1f;
        model.rightArm.yaw = -0.7f;
        model.rightArm.roll = 0.3f;
        model.leftArm.pitch = -0.4f;
        model.leftArm.yaw = 0.6f;
        model.leftArm.roll = -0.15f;
        model.body.yaw = bounce * 0.15f;
        model.body.roll = bounce * 0.1f;
        model.body.pitch = -bounceHeight * 0.3f;
        model.head.yaw = -bounce * 0.2f;
        model.head.pitch = -0.15f;
        if (bounce > 0.0f) {
            model.rightLeg.pitch = bounceHeight * 2.0f;
            model.leftLeg.pitch = -0.1f;
        } else {
            model.leftLeg.pitch = bounceHeight * 2.0f;
            model.rightLeg.pitch = -0.1f;
        }
    }

    private void applyOrangeJusticeAnimation(BipedEntityModel<?> model, float progress) {
        float chaos1 = this.smoothSin(progress, 10.0f);
        float chaos2 = this.smoothCos(progress, 8.0f);
        float legSwing = this.smoothSin(progress, 5.0f);
        model.body.yaw = legSwing * 0.5f;
        model.body.roll = chaos2 * 0.2f;
        model.body.pitch = Math.abs(chaos1) * -0.08f;
        model.rightArm.pitch = chaos1 * 1.4f;
        model.rightArm.yaw = chaos2 * 0.9f;
        model.rightArm.roll = chaos1 * 0.7f;
        model.leftArm.pitch = chaos2 * 1.4f;
        model.leftArm.yaw = chaos1 * 0.9f;
        model.leftArm.roll = chaos2 * 0.7f;
        if (legSwing > 0.0f) {
            model.rightLeg.yaw = 0.4f;
            model.leftLeg.yaw = 0.2f;
            model.rightLeg.pitch = legSwing * 0.3f;
        } else {
            model.rightLeg.yaw = -0.2f;
            model.leftLeg.yaw = -0.4f;
            model.leftLeg.pitch = -legSwing * 0.3f;
        }
        model.head.yaw = chaos1 * 0.3f;
        model.head.roll = chaos2 * 0.15f;
    }

    private void applyGetGriddyAnimation(BipedEntityModel<?> model, float progress) {
        boolean finalPhase;
        float step = this.smoothSin(progress, 5.0f);
        float beat = this.smoothSin(progress, 10.0f);
        boolean bl = finalPhase = progress > 0.75f;
        if (step > 0.0f) {
            model.rightLeg.pitch = -step * 0.6f;
            model.rightLeg.yaw = 0.2f;
            model.leftLeg.pitch = 0.15f;
            model.leftLeg.yaw = 0.0f;
        } else {
            model.leftLeg.pitch = step * 0.6f;
            model.leftLeg.yaw = -0.2f;
            model.rightLeg.pitch = 0.15f;
            model.rightLeg.yaw = 0.0f;
        }
        model.body.pitch = 0.12f;
        model.body.yaw = step * 0.15f;
        if (finalPhase) {
            float finalProgress = (progress - 0.75f) / 0.25f;
            model.rightArm.pitch = MathHelper.lerp((float)finalProgress, (float)-0.7f, (float)-2.2f);
            model.leftArm.pitch = MathHelper.lerp((float)finalProgress, (float)-0.7f, (float)-2.2f);
            model.rightArm.yaw = MathHelper.lerp((float)finalProgress, (float)0.3f, (float)-0.5f);
            model.leftArm.yaw = MathHelper.lerp((float)finalProgress, (float)-0.3f, (float)0.5f);
        } else {
            model.rightArm.pitch = -0.7f + beat * 0.4f;
            model.leftArm.pitch = -0.7f - beat * 0.4f;
            model.rightArm.yaw = 0.3f;
            model.leftArm.yaw = -0.3f;
        }
        model.head.yaw = step * 0.2f;
        model.head.pitch = 0.05f;
    }

    private void applyWaveAnimation(BipedEntityModel<?> model, float progress) {
        float wave = this.smoothSin(progress, 5.0f);
        model.rightArm.pitch = -2.3f;
        model.rightArm.yaw = -0.4f + wave * 0.4f;
        model.rightArm.roll = wave * 0.25f;
        model.leftArm.pitch = 0.1f;
        model.leftArm.yaw = 0.05f;
    }

    private void applyDabAnimation(BipedEntityModel<?> model, float progress) {
        float dabProgress = this.smoothProgress(progress);
        model.rightArm.pitch = MathHelper.lerp((float)dabProgress, (float)0.0f, (float)-0.4f);
        model.rightArm.yaw = MathHelper.lerp((float)dabProgress, (float)0.0f, (float)-1.8f);
        model.rightArm.roll = MathHelper.lerp((float)dabProgress, (float)0.0f, (float)0.4f);
        model.leftArm.pitch = MathHelper.lerp((float)dabProgress, (float)0.0f, (float)-1.9f);
        model.leftArm.yaw = MathHelper.lerp((float)dabProgress, (float)0.0f, (float)0.7f);
        model.leftArm.roll = MathHelper.lerp((float)dabProgress, (float)0.0f, (float)-0.2f);
        model.head.pitch = MathHelper.lerp((float)dabProgress, (float)0.0f, (float)0.25f);
        model.head.yaw = MathHelper.lerp((float)dabProgress, (float)0.0f, (float)0.4f);
        model.head.roll = MathHelper.lerp((float)dabProgress, (float)0.0f, (float)0.15f);
        model.body.pitch = MathHelper.lerp((float)dabProgress, (float)0.0f, (float)0.08f);
        model.body.yaw = MathHelper.lerp((float)dabProgress, (float)0.0f, (float)0.15f);
    }

    public static enum EmoteType {
        CRY("Плач", 3000.0f, false),
        RUSSIAN_SQUAT("Присядка", 4000.0f, true),
        FLOSS("Floss", 3000.0f, true),
        TAKE_THE_L("Take the L", 3500.0f, true),
        ORANGE_JUSTICE("Orange Justice", 4000.0f, true),
        GET_GRIDDY("Get Griddy", 3500.0f, true),
        WAVE("Помахать", 2000.0f, false),
        DAB("Дэб", 1500.0f, false);

        private final String displayName;
        private final float duration;
        private final boolean looping;

        private EmoteType(String displayName, float duration, boolean looping) {
            this.displayName = displayName;
            this.duration = duration;
            this.looping = looping;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public float getDuration() {
            return this.duration;
        }

        public boolean isLooping() {
            return this.looping;
        }
    }
}

