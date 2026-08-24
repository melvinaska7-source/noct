package zov.alphadlc.util.neuro.rotation;

import ai.djl.Model;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.nn.norm.BatchNorm;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.training.initializer.XavierInitializer;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Adam;
import ai.djl.training.tracker.Tracker;
import ai.djl.translate.TranslateException;
import zov.alphadlc.util.chat.ChatUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

public class AIRotationModel implements Closeable {

    private static final int NUM_EPOCH = 500;
    private static final int BATCH_SIZE = 32;
    private static final int INPUT_SIZE = 4;
    private static final int OUTPUT_SIZE = 2;

    private final Model model;
    private final Predictor<float[], float[]> predictor;
    private final String name;

    public AIRotationModel(String name) {
        this.name = name;
        this.model = Model.newInstance(name);
        this.model.setBlock(createMlpBlock());
        this.predictor = model.newPredictor(new FloatArrayTranslator());
    }

    public float[] predict(float[] input) throws TranslateException {
        return predictor.predict(input);
    }

    public void train(float[][] features, float[][] labels) throws ModelException, IOException, TranslateException {
        if (features.length != labels.length || features.length == 0) {
            throw new IllegalArgumentException("Features and labels must have the same size and be non-empty");
        }

        ChatUtil.send("§aНачинаю обучение модели §e" + name + "§a...");
        ChatUtil.send("§7Сэмплов: §f" + features.length + " §7| Эпох: §f" + NUM_EPOCH);

        TrainingConfig trainingConfig = new DefaultTrainingConfig(Loss.l2Loss())
                .optInitializer(new XavierInitializer(), "weight")
                .optOptimizer(Adam.builder().optLearningRateTracker(Tracker.fixed(0.001f)).build())
                .addTrainingListeners(TrainingListener.Defaults.logging());

        try (Trainer trainer = model.newTrainer(trainingConfig)) {
            trainer.initialize(new Shape(BATCH_SIZE, INPUT_SIZE));

            float[] flatFeatures = new float[features.length * INPUT_SIZE];
            float[] flatLabels = new float[labels.length * OUTPUT_SIZE];
            for (int i = 0; i < features.length; i++) {
                System.arraycopy(features[i], 0, flatFeatures, i * INPUT_SIZE, INPUT_SIZE);
                System.arraycopy(labels[i], 0, flatLabels, i * OUTPUT_SIZE, OUTPUT_SIZE);
            }

            ArrayDataset dataset = new ArrayDataset.Builder()
                    .setData(flatFeatures)
                    .setLabels(flatLabels)
                    .optLabels(new Shape(OUTPUT_SIZE))
                    .setSampling(BATCH_SIZE, true)
                    .build();

            EasyTrain.fit(trainer, NUM_EPOCH, dataset, null);
        }

        ChatUtil.send("§aОбучение завершено!");
    }

    public void load(Path path) throws ModelException, IOException {
        model.load(path);
        ChatUtil.send("§aМодель §e" + name + " §aзагружена");
    }

    public void save(Path path) throws IOException {
        model.save(path, name);
        ChatUtil.send("§aМодель §e" + name + " §aсохранена");
    }

    private static ai.djl.nn.Block createMlpBlock() {
        return new SequentialBlock()
                .add(Linear.builder().setUnits(64).build())
                .add(BatchNorm.builder().build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(32).build())
                .add(BatchNorm.builder().build())
                .add(Activation::relu)
                .add(Linear.builder().setUnits(OUTPUT_SIZE).build());
    }

    @Override
    public void close() {
        predictor.close();
        model.close();
    }
}
