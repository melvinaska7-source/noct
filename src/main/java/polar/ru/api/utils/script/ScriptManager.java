package polar.ru.api.utils.script;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.script.ScriptTask;

public class ScriptManager {
    private final List<ScriptTask> tasks = new CopyOnWriteArrayList<ScriptTask>();

    public void addTask(ScriptTask task) {
        this.tasks.add(task);
    }

    public void tick(EventUpdate event) {
        this.tasks.removeIf(task -> task.tick(event));
    }

    public void clear() {
        this.tasks.clear();
    }
}

