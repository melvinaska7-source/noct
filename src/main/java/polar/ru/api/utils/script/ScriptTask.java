package polar.ru.api.utils.script;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import polar.ru.api.events.implement.EventUpdate;

public class ScriptTask {
    private final List<Function<EventUpdate, Boolean>> steps = new ArrayList<Function<EventUpdate, Boolean>>();
    private int currentStep;

    public ScriptTask schedule(Function<EventUpdate, Boolean> step) {
        this.steps.add(step);
        return this;
    }

    public boolean tick(EventUpdate event) {
        if (this.currentStep >= this.steps.size()) {
            return true;
        }
        if (Boolean.TRUE.equals(this.steps.get(this.currentStep).apply(event))) {
            ++this.currentStep;
        }
        return this.currentStep >= this.steps.size();
    }
}

