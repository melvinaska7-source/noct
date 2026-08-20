package polar.ru.client.modules.impl.combat.components.rotations.neuro;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.client.modules.impl.combat.components.rotations.neuro.NeuroPatternData;

public class NeuroPatternRecorder
implements QClient {
    private static final String PATTERNS_DIR = "neuro_patterns";
    private boolean recording = false;
    private NeuroPatternData currentPattern;
    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
    private boolean initialized = false;
    private LivingEntity currentTarget = null;

    public void startRecording(String patternName) {
        try {
            if (this.recording) {
                ChatUtils.sendMessage("§cУже идёт запись паттерна!");
                return;
            }
            if (NeuroPatternRecorder.mc.player == null) {
                ChatUtils.sendMessage("§cНельзя начать запись вне игры!");
                return;
            }
            this.currentPattern = new NeuroPatternData(patternName);
            this.recording = true;
            this.initialized = false;
            this.currentTarget = null;
            ChatUtils.sendMessage("§aЗапись паттерна §e" + patternName + "§a началась!");
            ChatUtils.sendMessage("§7Двигайте головой как обычно в бою. Для остановки: §e.neuro stop");
        }
        catch (Exception e2) {
            ChatUtils.sendMessage("§cОшибка при запуске записи: " + e2.getMessage());
            e2.printStackTrace();
            this.recording = false;
            this.currentPattern = null;
        }
    }

    public void stopRecording() {
        if (!this.recording) {
            ChatUtils.sendMessage("§cЗапись не идёт!");
            return;
        }
        this.recording = false;
        this.currentPattern.finishRecording();
        if (this.currentPattern.getSnapshotCount() < 10) {
            ChatUtils.sendMessage("§cСлишком мало данных для сохранения! Записано тиков: " + this.currentPattern.getSnapshotCount());
            this.currentPattern = null;
            return;
        }
        this.savePattern(this.currentPattern);
        ChatUtils.sendMessage("§aПаттерн §e" + this.currentPattern.getName() + "§a сохранён!");
        ChatUtils.sendMessage("§7Записано тиков: §e" + this.currentPattern.getSnapshotCount() + "§7, длительность: §e" + (float)this.currentPattern.getRecordDuration() / 1000.0f + "s");
        ChatUtils.sendMessage("§7Средняя скорость Yaw: §e" + String.format("%.2f", Float.valueOf(this.currentPattern.getAvgYawSpeed())) + "§7, Pitch: §e" + String.format("%.2f", Float.valueOf(this.currentPattern.getAvgPitchSpeed())));
        this.currentPattern = null;
        this.initialized = false;
    }

    public void update(LivingEntity target) {
        if (!this.recording || NeuroPatternRecorder.mc.player == null || this.currentPattern == null) {
            return;
        }
        try {
            float currentYaw = NeuroPatternRecorder.mc.player.getYaw();
            float currentPitch = NeuroPatternRecorder.mc.player.getPitch();
            if (!this.initialized) {
                this.lastYaw = currentYaw;
                this.lastPitch = currentPitch;
                this.initialized = true;
                return;
            }
            float deltaYaw = MathHelper.wrapDegrees((float)(currentYaw - this.lastYaw));
            float deltaPitch = currentPitch - this.lastPitch;
            String context = this.detectContext(deltaYaw, deltaPitch, target);
            boolean isAttacking = target != null && target == this.currentTarget;
            this.currentTarget = target;
            float distanceToTarget = 999.0f;
            if (target != null) {
                distanceToTarget = this.calculateDistanceToHitbox(target);
            }
            this.currentPattern.addSnapshot(currentYaw, currentPitch, deltaYaw, deltaPitch, isAttacking, distanceToTarget, context);
            this.lastYaw = currentYaw;
            this.lastPitch = currentPitch;
        }
        catch (Exception e2) {
            ChatUtils.sendMessage("§cОшибка во время записи: " + e2.getMessage());
            e2.printStackTrace();
            this.stopRecording();
        }
    }

    private String detectContext(float deltaYaw, float deltaPitch, LivingEntity target) {
        float totalDelta = Math.abs(deltaYaw) + Math.abs(deltaPitch);
        if (target == null) {
            return "idle";
        }
        if (totalDelta < 0.5f) {
            return "tracking";
        }
        if (totalDelta < 5.0f) {
            return "aiming";
        }
        return "jerking";
    }

    private float calculateDistanceToHitbox(LivingEntity target) {
        if (NeuroPatternRecorder.mc.player == null) {
            return 999.0f;
        }
        Vec3d eyePos = NeuroPatternRecorder.mc.player.getCameraPosVec(1.0f);
        Box box = target.getBoundingBox();
        Vec3d closest = new Vec3d(MathHelper.clamp((double)eyePos.x, (double)box.minX, (double)box.maxX), MathHelper.clamp((double)eyePos.y, (double)box.minY, (double)box.maxY), MathHelper.clamp((double)eyePos.z, (double)box.minZ, (double)box.maxZ));
        return (float)eyePos.distanceTo(closest);
    }

    private void savePattern(NeuroPatternData pattern) {
        try {
            Path dirPath = Paths.get(PATTERNS_DIR, new String[0]);
            if (!Files.exists(dirPath, new LinkOption[0])) {
                Files.createDirectories(dirPath, new FileAttribute[0]);
            }
            String fileName = pattern.getName() + ".neuropat";
            Path filePath = dirPath.resolve(fileName);
            try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(filePath, new OpenOption[0])));){
                oos.writeObject(pattern);
            }
        }
        catch (IOException e2) {
            ChatUtils.sendMessage("§cОшибка сохранения паттерна: " + e2.getMessage());
            e2.printStackTrace();
        }
    }

    public static NeuroPatternData loadPattern(String patternName) {
        String fileName = patternName + ".neuropat";
        Path filePath = Paths.get(PATTERNS_DIR, fileName);
        if (!Files.exists(filePath, new LinkOption[0])) {
            ChatUtils.sendMessage("§cПаттерн §e" + patternName + "§c не найден!");
            return null;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(filePath, new OpenOption[0])))) {
            NeuroPatternData pattern = (NeuroPatternData)ois.readObject();
            pattern.finishRecording();
            ChatUtils.sendMessage("§aПаттерн §e" + patternName + "§a загружен!");
            ChatUtils.sendMessage("§7Тиков: §e" + pattern.getSnapshotCount() + "§7, длительность: §e" + (float)pattern.getRecordDuration() / 1000.0f + "s");
            return pattern;
        } catch (IOException | ClassNotFoundException e2) {
            ChatUtils.sendMessage("§cОшибка загружения паттерна: " + e2.getMessage());
            e2.printStackTrace();
            return null;
        }
    }

    public static void listPatterns() {
        try {
            Path dirPath = Paths.get(PATTERNS_DIR, new String[0]);
            if (!Files.exists(dirPath, new LinkOption[0])) {
                ChatUtils.sendMessage("§7Паттерны не найдены. Создайте первый паттерн с помощью §e.neuro start <name>");
                return;
            }
            File[] files = dirPath.toFile().listFiles((dir, name) -> name.endsWith(".neuropat"));
            if (files == null || files.length == 0) {
                ChatUtils.sendMessage("§7Паттерны не найдены.");
                return;
            }
            ChatUtils.sendMessage("§aДоступные паттерны:");
            for (File file : files) {
                String name2 = file.getName().replace(".neuropat", "");
                long sizeKB = file.length() / 1024L;
                ChatUtils.sendMessage("§7- §e" + name2 + "§7 (" + sizeKB + " KB)");
            }
        }
        catch (Exception e2) {
            ChatUtils.sendMessage("§cОшибка чтения паттернов: " + e2.getMessage());
        }
    }
    public boolean isRecording() {
        return this.recording;
    }
}

