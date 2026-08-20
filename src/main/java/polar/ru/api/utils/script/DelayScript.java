package polar.ru.api.utils.script;

import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.script.ScriptManager;
import polar.ru.api.utils.script.ScriptTask;

public class DelayScript {
    private final ScriptManager scriptManager = new ScriptManager();
    private ScriptTask currentTask;

    public void update(EventUpdate event) {
        this.scriptManager.tick(event);
    }

    public DelayScript cleanup() {
        this.scriptManager.clear();
        this.currentTask = null;
        return this;
    }

    public DelayScript addTickStep(int delayTicks, Runnable action) {
        if (this.currentTask == null) {
            this.currentTask = new ScriptTask();
            this.scriptManager.addTask(this.currentTask);
        }
        int[] remaining = new int[]{Math.max(0, delayTicks)};
        this.currentTask.schedule(event -> {
            if (remaining[0] > 0) {
                remaining[0] = remaining[0] - 1;
                return false;
            }
            action.run();
            return true;
        });
        return this;
    }
}

