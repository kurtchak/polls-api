package org.blackbell.polls.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SyncEventBroadcaster {
    private static final Logger log = LoggerFactory.getLogger(SyncEventBroadcaster.class);
    private static final int MAX_EVENTS = 200;
    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000L; // 5 minutes

    private final List<SyncEvent> events = Collections.synchronizedList(new ArrayList<>());
    private final List<SseEmitter> emitters = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong idCounter = new AtomicLong(0);

    public void emit(String level, String town, String season, String phase, String message) {
        SyncEvent event = new SyncEvent(
                idCounter.incrementAndGet(),
                Instant.now(),
                level,
                town,
                season,
                phase,
                message
        );

        synchronized (events) {
            events.add(event);
            if (events.size() > MAX_EVENTS) {
                events.remove(0);
            }
        }

        sendToEmitters(event);
    }

    public void emit(String level, String town, String phase, String message) {
        emit(level, town, null, phase, message);
    }

    public void emit(String level, String phase, String message) {
        emit(level, null, null, phase, message);
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        // Send recent events to new subscriber
        List<SyncEvent> recent = getRecentEvents();
        for (SyncEvent event : recent) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(event.id()))
                        .name("sync")
                        .data(toMap(event)));
            } catch (IOException e) {
                emitters.remove(emitter);
                return emitter;
            }
        }

        return emitter;
    }

    public List<SyncEvent> getRecentEvents() {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }

    public List<SyncEvent> getLastRunEvents() {
        synchronized (events) {
            int lastStartIdx = -1;
            for (int i = events.size() - 1; i >= 0; i--) {
                if ("start".equals(events.get(i).phase())) {
                    lastStartIdx = i;
                    break;
                }
            }
            if (lastStartIdx < 0) {
                return new ArrayList<>(events);
            }
            return new ArrayList<>(events.subList(lastStartIdx, events.size()));
        }
    }

    private void sendToEmitters(SyncEvent event) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        synchronized (emitters) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .id(String.valueOf(event.id()))
                            .name("sync")
                            .data(toMap(event)));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }
            emitters.removeAll(deadEmitters);
        }
    }

    private Map<String, Object> toMap(SyncEvent event) {
        return Map.of(
                "id", event.id(),
                "timestamp", event.timestamp().toString(),
                "level", event.level(),
                "town", event.town() != null ? event.town() : "",
                "season", event.season() != null ? event.season() : "",
                "phase", event.phase(),
                "message", event.message()
        );
    }
}
