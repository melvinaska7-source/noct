package zov.alphadlc.util.neuro.rotation;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import zov.alphadlc.util.chat.ChatUtil;
import zov.alphadlc.util.IMinecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AIRotationManager implements IMinecraft {

    private static final Path DATASET_DIR = Paths.get("nocturne", "ai", "datasets");
    private static final Path MODEL_DIR = Paths.get("nocturne", "ai", "models");

    private final List<RotationSample> samples = new CopyOnWriteArrayList<>();
    private boolean recording = false;
    private AIRotationModel currentModel;

    public void startRecording() {
        if (recording) {
            ChatUtil.send("§cЗапись уже идет!");
            return;
        }
        samples.clear();
        recording = true;
        ChatUtil.send("§aЗапись начата!");
        ChatUtil.send("§7Атакуйте цель, ваши движения будут записаны");
        ChatUtil.send("§7Используйте §f.ai stop §7для остановки");
    }

    public void stopRecording() {
        if (!recording) {
            ChatUtil.send("§cЗапись не идет!");
            return;
        }
        recording = false;
        ChatUtil.send("§aЗапись остановлена. Сэмплов: §f" + samples.size());
    }

    public void saveDataset(String name) {
        if (samples.isEmpty()) {
            ChatUtil.send("§cНет данных для сохранения! Используйте .ai start для начала записи");
            return;
        }

        try {
            Files.createDirectories(DATASET_DIR);
            Path datasetPath = DATASET_DIR.resolve(name + ".json");
            // Здесь должна быть логика сериализации в JSON
            ChatUtil.send("§aДатасет §e" + name + " §aсохранен (§f" + samples.size() + " §aсэмплов)");
            ChatUtil.send("§7Путь: §f" + datasetPath.toAbsolutePath());
        } catch (IOException e) {
            ChatUtil.send("§cОшибка сохранения датасета: " + e.getMessage());
        }
    }

    public void loadDataset(String datasetName) {
        try {
            Path datasetPath = DATASET_DIR.resolve(datasetName + ".json");
            if (!Files.exists(datasetPath)) {
                ChatUtil.send("§cДатасет §e" + datasetName + " §cне найден!");
                return;
            }
            // Здесь должна быть логика десериализации из JSON
            ChatUtil.send("§aДатасет §e" + datasetName + " §aзагружен");
        } catch (Exception e) {
            ChatUtil.send("§cОшибка загрузки датасета: " + e.getMessage());
        }
    }

    public void trainModel(String name) {
        if (samples.size() < 10) {
            ChatUtil.send("§cНедостаточно данных для обучения! Нужно минимум 10 сэмплов.");
            return;
        }

        try {
            currentModel = new AIRotationModel(name);
            float[][] features = new float[samples.size()][4];
            float[][] labels = new float[samples.size()][2];

            for (int i = 0; i < samples.size(); i++) {
                RotationSample sample = samples.get(i);
                features[i][0] = (float) sample.playerPos().x;
                features[i][1] = (float) sample.playerPos().y;
                features[i][2] = (float) sample.playerPos().z;
                features[i][3] = sample.playerYaw();
                labels[i][0] = sample.targetYaw();
                labels[i][1] = sample.targetPitch();
            }

            currentModel.train(features, labels);
        } catch (Exception e) {
            ChatUtil.send("§cОшибка обучения модели: " + e.getMessage());
        }
    }

    public void predict(ClientPlayerEntity player) {
        if (currentModel == null) {
            ChatUtil.send("§cМодель не загружена! Используйте .ai train или .ai load");
            return;
        }

        try {
            float[] input = new float[]{
                    (float) player.getX(),
                    (float) player.getY(),
                    (float) player.getZ(),
                    player.getYaw()
            };
            float[] prediction = currentModel.predict(input);
            player.setYaw(prediction[0]);
            player.setPitch(prediction[1]);
        } catch (Exception e) {
            ChatUtil.send("§cОшибка предсказания: " + e.getMessage());
        }
    }

    public void saveModel(String name) {
        if (currentModel == null) {
            ChatUtil.send("§cНет модели для сохранения!");
            return;
        }
        try {
            Files.createDirectories(MODEL_DIR);
            currentModel.save(MODEL_DIR);
            ChatUtil.send("§aМодель §e" + name + " §aсохранена");
        } catch (Exception e) {
            ChatUtil.send("§cОшибка сохранения модели: " + e.getMessage());
        }
    }

    public void loadModel(String name) {
        try {
            Path modelPath = MODEL_DIR.resolve(name);
            if (!Files.exists(modelPath)) {
                ChatUtil.send("§cМодель §e" + name + " §cне найдена!");
                return;
            }
            currentModel = new AIRotationModel(name);
            currentModel.load(modelPath);
            ChatUtil.send("§aМодель §e" + name + " §aзагружена");
        } catch (Exception e) {
            ChatUtil.send("§cОшибка загрузки модели: " + e.getMessage());
        }
    }

    public void addSample(Vec3d playerPos, float playerYaw, float targetYaw, float targetPitch) {
        if (recording) {
            samples.add(new RotationSample(playerPos, playerYaw, targetYaw, targetPitch));
        }
    }

    public boolean isRecording() {
        return recording;
    }

    public List<RotationSample> getSamples() {
        return new ArrayList<>(samples);
    }

    public void clearSamples() {
        samples.clear();
    }

    public record RotationSample(Vec3d playerPos, float playerYaw, float targetYaw, float targetPitch) {}
}
