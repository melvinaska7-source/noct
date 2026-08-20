package polar.ru.api.events.implement;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import polar.ru.api.events.Event;

public class EventRender
extends Event {

    public static class Game
    extends Event {
        private final WorldRenderer context;
        private final MatrixStack matrix;
        private final Matrix4f projectionMatrix;
        private final Camera camera;
        private final float partialTicks;
        private final long finishTimeNano;
        public WorldRenderer getContext() {
            return this.context;
        }
        public MatrixStack getMatrix() {
            return this.matrix;
        }
        public Matrix4f getProjectionMatrix() {
            return this.projectionMatrix;
        }
        public Camera getCamera() {
            return this.camera;
        }
        public float getPartialTicks() {
            return this.partialTicks;
        }
        public long getFinishTimeNano() {
            return this.finishTimeNano;
        }
        public Game(WorldRenderer context, MatrixStack matrix, Matrix4f projectionMatrix, Camera camera, float partialTicks, long finishTimeNano) {
            this.context = context;
            this.matrix = matrix;
            this.projectionMatrix = projectionMatrix;
            this.camera = camera;
            this.partialTicks = partialTicks;
            this.finishTimeNano = finishTimeNano;
        }
    }

    public static class World
    extends Event {
        private final Window scaledResolution;
        private final float partialTicks;
        private final Matrix4f matrix;
        private final MatrixStack matrixStack;
        public Window getScaledResolution() {
            return this.scaledResolution;
        }
        public float getPartialTicks() {
            return this.partialTicks;
        }
        public Matrix4f getMatrix() {
            return this.matrix;
        }
        public MatrixStack getMatrixStack() {
            return this.matrixStack;
        }
        public World(Window scaledResolution, float partialTicks, Matrix4f matrix, MatrixStack matrixStack) {
            this.scaledResolution = scaledResolution;
            this.partialTicks = partialTicks;
            this.matrix = matrix;
            this.matrixStack = matrixStack;
        }
    }

    public static class Default
    extends Event {
        private final DrawContext context;
        private final float partialTicks;
        public DrawContext getContext() {
            return this.context;
        }
        public float getPartialTicks() {
            return this.partialTicks;
        }
        public Default(DrawContext context, float partialTicks) {
            this.context = context;
            this.partialTicks = partialTicks;
        }
    }
}

