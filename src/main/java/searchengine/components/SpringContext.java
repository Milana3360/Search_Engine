package searchengine.components;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class SpringContext implements ApplicationListener<ContextClosedEvent> {
    private static volatile boolean shuttingDown = false;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        shuttingDown = true;
    }

    public static boolean isShuttingDown() {
        return shuttingDown;
    }
}

