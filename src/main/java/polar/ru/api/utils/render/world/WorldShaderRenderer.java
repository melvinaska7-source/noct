package polar.ru.api.utils.render.world;

import polar.ru.api.QClient;

public class WorldShaderRenderer
implements QClient {
    private static WorldShaderRenderer instance;

    public static WorldShaderRenderer getInstance() {
        if (instance == null) {
            instance = new WorldShaderRenderer();
        }
        return instance;
    }

    public void render() {
    }
}

