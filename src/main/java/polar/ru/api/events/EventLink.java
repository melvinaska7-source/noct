package polar.ru.api.events;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value=RetentionPolicy.RUNTIME)
public @interface EventLink {
    public int priority() default 0;
}

